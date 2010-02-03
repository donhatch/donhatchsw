/*
 maybe another implementation, maybe cleaner


 Question: how to do line termination?
     - always the system default?
     - imitate whatever's in the first line of input?
     - try to imitate what's on every line? (then use what for lines we create?)
 The simplest is to use the system default (system property line.separator).
 The next simplest, and maybe cleanest, is to imitate whatever's in the
 first line of input.
 Trying to imitate what's on every line seems friendliest, but
 it's hell to maintain, and it's not clear what to do for lines we create
 (imitate nearby lines?  imitate whatever's in first line of input,
 for those only?)
 What about an empty input file?  I guess in that case
 we should output an empty file, with no directives at all? hmm
 DOING IT SIMPLEST WAY
*/

package com.donhatchsw.javacpp;

public class Cpp1
{
    private LineAndColumnNumberReader in;
    private LazyWriter out;


    private static class LineBuffer
    {
        // anyone outside this class should consider the members read-only
        public int length = 0;
        public char chars[] = new char[1];       // these arrays are same size
        public int lineNumbers[] = new int[1];   // these arrays are same size
        public int columnNumbers[] = new int[1]; // these arrays are same size
        public String fileName = null;

        public void clear()
        {
            length = 0;
        }

        public void append(char c, int lineNumber, int charNumber)
        {
            // make sure line buffer is big enough to hold another char...
            if (length == chars.length)
            {
                // expand line buffer and aux arrays
                char newChars[] = new char[2*chars.length];
                int newLineNumbers[] = new int[2*chars.length];
                int newLolumnNumbers[] = new int[2*chars.length];
                for (int i = 0; i < chars.length; ++i)
                {
                    newChars[i] = chars[i];
                    newLineNumbers[i] = lineBufToInLineNumber[i];
                    newColumnNumbers[i] = lineBufToInColumnNumber[i];
                }
                chars = newChars;
                lineNumbers = newLineNumbers;
                columnNumbers = newColumnNumbers;
            }
            chars[length] = c;
            lineNumbers[length] = lineNumber;
            columnNumbers[length] = columnNumber;
            length++;
        }
        public void deleteRange(int i0, int i1)
        {
            AssertAlways(i0 <= i1);
            while (i1 < length)
            {
                chars[i0] = chars[i1];
                lineNumbers[i0] = lineNumbers[i1];
                columnNumber[i0] = columnNumber[i1];
                i0++;
                i1++;
            }
            length = i0;
        }
    }; // class LineBuffer

    private int inFileName = 0;              // can be changed with line number directives
    private int inLineNumber = 0;            // can be changed with line number directives
    private int inColumnNumber = 0;          // can be changed with line number directives

    private int outLineNumberPhysical = 0;   // excludes pending newlines
    private int outColumnNumberPhysical = 0; // excludes pending newlines
    private int outLineNumberLogical = 0;    // includes pending newlines
    private int outColumnNumberLogical = 0;  // includes pending newlines

    // TODO: do we need this?
    private int tieInLineNumber = 0;  // this in line number
    private int tieOutLineNumber = 0; // maps to this out line number


    // TODO: decide whether to be lazy about column as well as line?
    class LazyWriter()
    {
        private java.io.Writer writer;
        private int lineNumberPhysical;
        private int lineNumberLogical;
        private int columnNumber; // just for making sure we don't sync when not at end of line... or does caller need to be able to query it?

        // this may only be called when columnNumber is 0.
        // returns true if successfully synced
        // using spaces and (up to 7) newlines,
        // false if it couldn't do that in which case caller needs to
        // issue a line number directive.
        public boolean sync(int lineNumber)
        {
            lineNumberLogical = lineNumber;

            AssertAlways(columnNumber == 0); // TODO: do we want this?
            // TODO: remove this if we decide on the assert
            if (columnNumber > 0)
            {
                writer.println();
                lineNumberPhysical++;
                columnNumberPhysical = 0;
            }

            if (lineNumberLogical >= lineNumberPhysical
             && lineNumberLogical <= lineNumberPhysical+7)
            {
                while (lineNumberPhysical < lineNumberLogical)
                {
                    writer.println();
                    lineNumberPhysical++;
                }
                return true;
            }
            else
                return false;
        }
        public void println()
        {
            if (columnNumber != 0)
            {
                writer.println();
                columnNumber = 0;
                lineNumberPhysical++;
            }
            lineNumberLogical++;
        }
        public void print(String s)
        {
            int n;
            for (int i = 0; i < n; ++i)
            {
                char c = s.charAt(i);
                if (c == '\n')
                {
                    println(); // fixes line and column numbers
                }
                else
                {
                    AssertAlways(lineNumberPhysical == lineNumberLogical);
                    writer.print(c);
                    columnNumber++;
                }
            }
        }
        public void println(String s)
        {
            print(s);
            println(); // the other one
        }
    } // class LazyWriter



    private static class Token
    {
        public int type;
        public char textUnderlyingString[]; // can be lineBuf.chars, or can own its own (in case of macro args or synthetic pasted-together tokens
        public int i0, i1; // start and end indices in underlyingString
        public String inFileName;
        public int inLineNumber;
        public int inColumnNumber;
        public Token* parentInMacroExpansion;
        public Token* nextInStack; // can only live in one stack at a time
        public int refCount;

        // No constructor, we use an init function instead,
        // to make sure no members are forgotten.
        public void init(int type,
                         char textUnderlyingString,
                         int i0,
                         int i1,
                         String inFileName,
                         int inLineNumber,
                         int inColumnNumber,
                         Token parentInMacroExpansion,
                         Token nextInStack,
                         int refCount)
        {
            this.type = type;
            this.textUnderlyingString = textUnderlyingString;
            this.i0 = i0;
            this.i1 = i1;
            this.inFileName = inFileName;
            this.inLineNumber = inLineNumber;
            this.inColumnNumber = inColumnNumber;
            this.parentInMacroExpansion = parentInMacroExpansion;
            this.nextInStack = nextInStack;
            this.refCount = refCount;
        }

        // should be used sparingly-- maybe in error/warning/debug printing only?
        public String textToString()
        {
            return new String(textUnderlyingString, i0, i1-i0);
        }
        public boolean textEquals(String s)
        {
            int sLength = s.length();
            if (sLength != i1-i0)
                return false;
            for (int i = 0; i < sLength; ++i)
                if (underlyingString[i0+i] != s.charAt(i))
                    return false;
            return true;
        }
        public boolean textStartsWith(String s)
        {
            int sLength = s.length();
            if (sLength > i1-i0)
                return false;
            for (int i = 0; i < sLength; ++i)
                if (underlyingString[i0+i] != s.charAt(i))
                    return false;
            return true;
        }
        public boolean textEndsWith(String s)
        {
            int sLength = s.length();
            if (sLength > i1-i0)
                return false;
            for (int i = 0; i < sLength; ++i)
                if (underlyingString[i1-sLength+i] != s.charAt(i))
                    return false;
            return true;
        }
    }; // Token

    // Tokens are the thing we are going to create and destroy
    // zillions of during parsing.
    // So, optimize by keeping old discarded tokens for re-use so we don't
    // have to rely on the garbage collector and allocator.
    private static class TokenBarn
    {
        private int nInUse = 0;
        private int nFree = 0;
        private Token freeListHead = null; // we use the parent member to form a linked list


        public Token newRefedToken(int type, char textUnderlyingString[], i0, i1, inFileName, inLineNumber, inColumnNumber)
        {
            Token token;
            if (freeListHead != null)
            {
                nFree--;
                token = freeListHead;
                freeListHead = freeListHead->parent;
            }
            else
                token = new Token();

            token.init(type,
                       underlyingString,
                       i0,
                       i1,
                       inFileName,
                       inLineNumber,
                       inColumnNumber,
                       null, // parent
                       1); // refCount

            nInUse++;

            return token;
        }
        public void unrefToken(Token token)
        {
            if (--token.refCount <= 0)
            {
                AssertAlways(token.refCount == 0);
                AssertAlways(nInUse > 0);
                --nInUse;

                unrefToken(token.parent); // recursively

                // make sure token is not holding on to any pointers
                token.parent = null;
                token.underlyingString = null;
                token.inFileName = null;

                token.parent = freeListHead;
                freeListHead = token;
                nFree++;
            }
        }
        public int nInUse()
        {
            return nInUse;
        }
        public int nFree()
        {
            return nFree;
        }
    }; // TokenBarn

    private static class TokenStack
    {
        private Token head = null;
        public bool isEmpty()
        {
            return head == null;
        }
        public void pushAndRef(Token token)
        {
            token.refCount++;
        }
        public Token popAndKeepRef()
        {
            Token token = head;
            head = token.nextInStack;
            token.nextInStack = null;
            return token;
        }
        public void popAndUnref(TokenBarn barn)
        {
            Token token = head;
            head = token.nextInStack;
            token.nextInStack = null;
            tokenBarn.unrefToken(token);
        }
    }; // TokenStack


    class LineAndColumnNumberReader
    {
        private java.io.Reader reader;
        private int lineNumber = 0;
        private int columnNumber = 0;
        private int lookedAheadChar = -2; // -2 means not looked ahead, -1 means EOF

        private final bool turnLineSeparatorsIntoSingleNewlines = true; // probably, yeah, simplest way to process things


        // To avoid dismal performance, caller should make sure
        // that reader is either a BufferedReader or has one as an ancestor.
        public LineAndColumnNumberReader(java.io.Reader reader)
        {
            this.reader = new java.io.LineNumberReader(reader);
        }

        // turns \r\n into \n
        public int read()
            throws java.io.IOException // since newlineSimplifyingReader.read() does
        {
            int c;
            if (lookedAheadChar != -2) // if there is a looked ahead char...
            {
                c = lookedAheadChar;
                lookedAheadChar = -2;
            }
            else
                c = reader.read();
            if (turnLineSeparatorsIntoSingleNewlines)
            {
                if (c == '\r')
                {
                    if ((lookedAheadChar = reader.read()) == '\n')
                        lookedAheadChar = -2;
                    c = '\n';
                }
            }
            else
            {
                // untested, maybe remove this
                if (c == '\n'
                 || (c == '\r' &&  (lookedAheadChar = reader.read()) != '\n')
                {
                    lineNumber++;
                    columnNumber = 0;
                }
                else if (c != '\r')
                {
                    columnNumber = 0; // it's a carriage return, what can I say
                }
                else
                    columnNumber++;
            }
            return c;
        } // read

        // return what read() will return next.
        // doesn't do anything to lineNumber and columnNumber.
        public int peek()
            throws java.io.IOException
        {
            if (lookedAheadChar == -2)
                lookedAheadChar = reader.read();
            return lookedAheadChar;
        }

        public int getLineNumber()
        {
            //System.err.println("            with lookahead getLineNumber returning "+lineNumber);
            return lineNumber;
        }
        public int getColumnNumber()
        {
            return columnNumber;
        }
        public void setLineNumber(int lineNumber)
        {
            this.lineNumber = lineNumber;
            this.columnNumber = 0; // or just assert it's 0?
        }
    } // class LineAndColumnNumberReader


    // gets a line,
    // pasting together physical lines joined by escaped newlines,
    // incrementing inLineNumber and inColumnNumber appropriately.
    // If there are no more lines, returns a line of length 0.
    private void getNextLogicalLine(LineAndColumnNumberReader in,
                                    LineBuffer lineBuffer)
    {
        lineBuffer.clear();
        while (true)
        {
            int physicalLineStart = lineBuffer.length;

            // append next physical line...
            while (true)
            {
                int lineNumber = in.getLineNumber();    // before reading a char
                int columnNumber = in.getColumnNumber(); // before reading a char
                int c = in.read();
                if (c == -1) // EOF
                    break;
                lineBuffer.append(c, lineNumber, columnNumber);
                if (c == '\n'                         // newline
                 || (c == '\r' && in.peek() != '\n')) // or carriage-return not followed by newline
                    break;
            }

            if (lineBuffer.length == physicalLineStart)
            {
                // woops, there was no physical line
                if (lineBuffer.length > 0)
                {
                    // there was a previous line that asked for termination
                    warning: backslash-newline at end of file;
                    artificially terminate it?
                }
                // otherwise we are returning empty line which caller knows means EOF
                break;
            }
            AssertAlways(lineBuffer.length > physicalLineStart);

            // Analyze the stuff at the end of the line...
            int lineTerminatorIndex = lineBuffer.length; // and counting
            while (lineTerminatorIndex > physicalLineStart
                && (lineBuffer.chars[lineTerminatorIndex-1] == '\n'
                 || lineBuffer.chars[lineTerminatorIndex-1] == '\r'))
                 lineTerminatorIndex--;
            int backslashIndex = -1; // we'll set this if we find one
            int trailingSpacesIncludingOptionalBackslashStartIndex = lineTerminatorIndex; // and counting
            while (trailingSpacesIncludingOptionalBackslashStartIndex > physicalLineStart
                && (Character.isWhitespace(lineBuffer.chars[trailingSpacesIncludingOptionalBackslashStartIndex-1])
                 || (backslashIndex==-1 && lineBuffer.chars[trailingSpacesIncludingOptionalBackslashStartIndex-1] == '\\')))
            {
                trailingSpacesIncludingOptionalBackslashStartIndex--;
                if (lineBuffer.chars[trailingSpacesIncludingOptionalBackslashStartIndex] == '\\')
                    backslashIndex = trailingSpacesIncludingOptionalBackslashStartIndex;
            }

            // Record the first line terminator seen...
            if (lineTerminator == null)
             && lineTerminatorIndex < lineBuffer.length)
                lineTerminator = new String(lineBuffer.chars, lineTerminatorIndex, lineBuffer.length-lineTerminatorIndex);

            if (backslashIndex == -1)
            {
                // No backslash continuation.
                // remove trailing whitespace, leaving the terminator if any, and return
                lineBuffer.deleteRange(trailingSpacesIncludingOptionalBackslashStartIndex,
                                       lineTerminatorIndex);
                break;
            }
            else
            {
                // there's a backslash continuation.
                // remove the backslash and any surrounding whitespace and line terminator,
                // and get another physical line
                if (lineTerminatorIndex == lineBuffer.length)
                {
                    warning: no newline at end of file
                    lineBuffer.length = trailingSpacesIncludingOptionalBackslashStartIndex;
                    return;
                }
                lineBuffer.length = trailingSpacesIncludingOptionalBackslashStartIndex;
                continue;
            }

            ... ARGH! confused and distracted
            if (lineTerminatorIndex == lineBuffer.length)
            {
                warning: no newline at end of file
                artificially terminate it?
            }
        }
    } // getNextLogicalLine

    class TokenStreamFromLineBuffer
    {
        LineBuffer lineBuffer;
        int endIndex;
        int currentIndex;
        public void init(LineBuffer lineBuffer, int startIndex, int endIndex)
        {
            this.lineBuffer = lineBuffer;
            this.endIndex = endIndex;
            this.currentIndex = startIndex;
        }
        // keeps ref. if you don't want it, use tokenBarn.unrefToken(readToken());
        public Token readToken()
        {
            ...
        }
    }; // TokenStreamFromLineBuffer

    class TokenStreamWithPushBack extends TokenStream
    {
        private TokenStack stack;

        public void init(LineBuffer lineBuffer, int startIndex, int endIndex)
        {
            AssertAlways(stackSize == 0);
            super.init(lineBuffer, startIndex, endIndex);
        }

        // keeps ref. if you don't want it, use tokenBarn.unrefToken(readToken());
        public Token readToken()
        {
            return stackSize > 0 ? stack.popAndKeepRef()
                                 : super.readToken();
        }
        public void pushBackToken(Token token)
        {
            stack.pushAndRef(token);
        }
    }; // TokenStreamWithPushBack



    filter(in, out)
    {
        TokenStreamWithPushBack tokenStream = new tokenStream(); // we re-init it at the beginning of each line.  could share it among recursive invocations, too

        while (true)
        {
            getNextLogicalLine();
            if (lineLength == 0)
                break; // end of file

            tokenStream.init(lineBuf, 0, lineLength);

            if line starts with a preprocessor directive
            {
                if it's #include
                {
                    get the file name and stuff, including end of line
                    output any newlines and/or line directives needed to get in sync
                    recurse
                }
                else if it's line number directive
                {
                    set the input file and line number 
                }
                else if it's #ifdef, #if, #elif, #else, #endif
                {
                    do the appropriate thing
                }
                else if it's #define or #undef
                {
                    do the appropriate thing
                }
            }

            while (true)
            {
                Token token = tokenStream.readToken();
                if (token == null)
                    break;
                if token is name of a macro
                {
                    read any args if appropriate
                    subtitute
                    push results on token stack, from end to beginning
                }
                else
                {
                    output any newlines and/or line directives needed to get in sync
                    output the token
                }
            }
        }
        if (outputColumnNumberPhysical != 0)
        {
            out.println(); // ARGH should be using same line terminators that were input.  maybe actually keep up to 7 lines of output?
        }
    }
} // public class Cpp1








