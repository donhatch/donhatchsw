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
    private static int verboseLevel = 3; // maybe 0 = nothing, 1 = overall, 2 = file, 3 = line, 4 = char


    // Logical assertions, always compiled in. Ungracefully bail if violated.
    private static void AssertAlways(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    private static void warning(String fileName, int lineNumber, int columnNumber, String message)
    {
        System.err.println(fileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": warning: "+message);
    }

    // From com.donhatchsw.util.Arrays...
        private static String escapify(char c, char quoteChar)
        {
            if (c == quoteChar) return "\\"+c;
            if (c == '\\') return "\\\\";
            if (c == '\n') return "\\n";
            if (c == '\r') return "\\r";
            if (c == '\t') return "\\t";
            if (c == '\f') return "\\f";
            if (c == '\b') return "\\b";
            if (c < 32 || c >= 127)
                return "\\"+((((int)c)>>6)&7)
                           +((((int)c)>>3)&7)
                           +((((int)c)>>0)&7);
            return ""+c;
        }
        private static String escapifyCharOrEOF(int c)
        {
            if (c < 0)
                return ""+c;
            else
                return escapify((char)c, '\'');
        }
        private static String escapify(String s)
        {
            StringBuffer sb = new StringBuffer();
            int n = s.length();
            for (int i = 0; i < n; ++i)
                sb.append(escapify(s.charAt(i), '"'));
            return sb.toString();
        }


    // Wrapper around new FileReader,
    // whose behavior can be overridden in subclasses
    // to, say, open an in-memory string instead, for testing.
    private static class FileOpener
    {
        public java.io.Reader newFileReader(String fileName)
            throws java.io.FileNotFoundException
        {
            return new java.io.FileReader(fileName);
        }
    } // private static class FileOpener


    private static class LineAndColumnNumberReader
    {
        private java.io.LineNumberReader reader;
        private String fileName;
        private int lineNumber = 0;
        private int columnNumber = 0;
        private int lookedAheadChar = -2; // -2 means not looked ahead, -1 means EOF

        // To avoid dismal performance, caller should make sure
        // that reader is either a BufferedReader or has one as an ancestor.
        public LineAndColumnNumberReader(java.io.Reader reader, String fileName)
        {
            this.reader = new java.io.LineNumberReader(reader);
            this.fileName = fileName;
        }

        // turns \r\n into \n
        public int read()
            throws java.io.IOException // since newlineSimplifyingReader.read() does
        {
            if (verboseLevel >= 4)
            {
                System.err.println("            in LineAndColumnNumberReader.read()");
                System.err.println("                lineNumber = "+lineNumber);
                System.err.println("                columnNumber = "+columnNumber);
            }
            int c;
            if (lookedAheadChar != -2) // if there is a looked ahead char...
            {
                c = lookedAheadChar;
                lookedAheadChar = -2;
            }
            else
                c = reader.read();

            if (c == -1)
            {
                // EOF... no line or column number adjusting, I don't think? not sure. maybe it should be illegal to ask for them?  not sure.
            }
            else if (c == '\n')
            {
                lineNumber++;
                columnNumber = 0;
            }
            else
                columnNumber++;

            if (verboseLevel >= 4)
            {
                System.err.println("                c = '"+escapifyCharOrEOF(c)+"'");
                System.err.println("                lineNumber = "+lineNumber);
                System.err.println("                columnNumber = "+columnNumber);
                System.err.println("            out LineAndColumnNumberReader.read()");
            }
            return c;
        } // read

        // return what read() will return next.
        // doesn't do anything to lineNumber and columnNumber.
        // XXX TODO: is this even needed?  maybe not
        public int peek()
            throws java.io.IOException
        {
            if (lookedAheadChar == -2)
                lookedAheadChar = reader.read();
            return lookedAheadChar=='\r' ? '\n' : lookedAheadChar;
        }

        public String getFileName()
        {
            return fileName;
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



    private static class LineBuffer
    {
        // anyone outside this class should consider the members read-only
        public int length = 0;
        public char chars[] = new char[1];       // these arrays are same size
        public int lineNumbers[] = new int[1];   // these arrays are same size
        public int columnNumbers[] = new int[1]; // these arrays are same size
        public String fileName = null; // XXX TODO: do we want this?

        public void clear()
        {
            length = 0;
        }
        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }

        public void append(char c, int lineNumber, int columnNumber)
        {
            // make sure line buffer is big enough to hold another char...
            if (length == chars.length)
            {
                // expand line buffer and aux arrays
                char newChars[] = new char[2*chars.length];
                int newLineNumbers[] = new int[2*chars.length];
                int newColumnNumbers[] = new int[2*chars.length];
                for (int i = 0; i < chars.length; ++i)
                {
                    newChars[i] = chars[i];
                    newLineNumbers[i] = lineNumbers[i];
                    newColumnNumbers[i] = columnNumbers[i];
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
                columnNumbers[i0] = columnNumbers[i1];
                i0++;
                i1++;
            }
            length = i0;
        }
    } // class LineBuffer


    // Gets a line from in into lineBuffer,
    // pasting together physical lines joined by escaped newlines,
    // If there are no more lines, returns a line of length 0.
    // Otherwise, the result is guaranteed to have exactly one '\n',
    // and it's guaranteed to be at the end.
    private static void getNextLogicalLine(LineAndColumnNumberReader in,
                                           LineBuffer lineBuffer)
        throws java.io.IOException
    {
        if (verboseLevel >= 3)
            System.err.println("    in getNextLogicalLine");

        lineBuffer.setFileName(in.getFileName());
        lineBuffer.clear();
        while (true)
        {
            int physicalLineStart = lineBuffer.length;
            boolean atEOF = false;

            if (verboseLevel >= 3)
                System.err.println("        reading a physical line");
            // append next physical line...
            while (true)
            {
                int lineNumber = in.getLineNumber();    // before reading a char
                int columnNumber = in.getColumnNumber(); // before reading a char
                int c = in.read();
                if (c == -1) // EOF
                {
                    atEOF = true;
                    break;
                }
                lineBuffer.append((char)c, lineNumber, columnNumber);
                if (c == '\n')
                    break;
            }
            if (verboseLevel >= 3)
                System.err.println("        done reading a physical line");

            if (lineBuffer.length == physicalLineStart)
            {
                // woops, there was no physical line
                if (lineBuffer.length > 0)
                {
                    // there was a previous line that asked for termination
                    warning(in.getFileName(), in.getLineNumber(), in.getColumnNumber(),
                            "backslash-newline at end of file");
                    lineBuffer.append('\n', in.getLineNumber(), in.getColumnNumber()); // XXX really should be from before EOF was read, querying anything after EOF should be error I think
                }
                // otherwise we are returning empty line which caller knows means EOF
                break;
            }
            AssertAlways(lineBuffer.length > physicalLineStart);

            AssertAlways(atEOF == (lineBuffer.chars[lineBuffer.length-1] != '\n'));
            if (atEOF)
            {
                warning(in.getFileName(), in.getLineNumber(), in.getColumnNumber(),
                        "no newline at end of file");
                lineBuffer.append('\n', in.getLineNumber(), in.getColumnNumber()); // XXX really should be from before EOF was read, querying anything after EOF should be error I think
            }

            // okay now the line is terminated by \n,
            // which simplifies things

            // Analyze the stuff at the end of the line...
            int trailingSpacesIncludingOptionalBackslashStartIndex = lineBuffer.length; // and counting
            int backslashIndex = -1;
            while (trailingSpacesIncludingOptionalBackslashStartIndex > physicalLineStart
                && (Character.isWhitespace(lineBuffer.chars[trailingSpacesIncludingOptionalBackslashStartIndex-1])
                 || (backslashIndex==-1 && lineBuffer.chars[trailingSpacesIncludingOptionalBackslashStartIndex-1] == '\\')))
            {
                trailingSpacesIncludingOptionalBackslashStartIndex--;
                if (lineBuffer.chars[trailingSpacesIncludingOptionalBackslashStartIndex] == '\\')
                {
                    AssertAlways(backslashIndex == -1); // by above test
                    backslashIndex = trailingSpacesIncludingOptionalBackslashStartIndex;
                }
            }

            if (backslashIndex == -1)
            {
                // No backslash continuation.
                // remove trailing whitespace, leaving the terminator if any, and return
                lineBuffer.deleteRange(trailingSpacesIncludingOptionalBackslashStartIndex,
                                       lineBuffer.length-1);
                break;
            }
            else
            {
                // there's a backslash continuation.
                // remove the backslash and any surrounding whitespace and line terminator,
                // and continue on to the next loop iteration,
                // to get another physical line
                lineBuffer.deleteRange(trailingSpacesIncludingOptionalBackslashStartIndex, lineBuffer.length);
                if (atEOF)
                {
                    warning(in.getFileName(), in.getLineNumber(), in.getColumnNumber(),
                            "backslash at end of file");
                    lineBuffer.append('\n', in.getLineNumber(), in.getColumnNumber());
                    break;
                }
                continue;
            }
        }

        // well that was way more frickin complicated than it should have been

        if (verboseLevel >= 3)
            System.err.println("    out getNextLogicalLine");
    } // getNextLogicalLine








    private static class Token
    {
        // The token types.
        // See http://gcc.gnu.org/onlinedocs/cpp/Tokenization.html
        private static int NUMTYPES = 0;
        public static final int IDENTIFIER = NUMTYPES++;     // [_a-zA-Z][_a-zA-Z0-9]*
        public static final int STRING_LITERAL = NUMTYPES++; // "([^\"]|\.)*"
        public static final int CHAR_LITERAL = NUMTYPES++;   // '([^\']|\.)*'" lenient, that's okay
        public static final int NUMBER_LITERAL = NUMTYPES++;    // "\\.?[0-9]([0-9a-zA-Z_\\.]|[eEpP][+-])*" note that it doesn't include initial '-'! rather bizarre definition,
        public static final int SYMBOL = NUMTYPES++;
        public static final int SPACES = NUMTYPES++;
        public static final int PREPROCESSOR_DIRECTIVE = NUMTYPES++;
        public static final int COMMENT = NUMTYPES++;
        public static final int COMMENT_START = NUMTYPES++; // XXX TODO: do we want this?
        public static final int COMMENT_MIDDLE = NUMTYPES++; // XXX TODO: do we want this?
        public static final int COMMENT_END = NUMTYPES++; // XXX TODO: do we want this?
        public static final int NEWLINE = NUMTYPES++;

        public static final int MACRO_ARG = NUMTYPES++;
        public static final int MACRO_ARG_QUOTED = NUMTYPES++;
        public static final int TOKEN_PASTE = NUMTYPES++; // temporary form that "##" takes during macro evaluation... NOT during initial tokenizing

        // NUMTYPES is now one more than the last type value


        public int type;
        public char textUnderlyingString[]; // can be lineBuf.chars, or can own its own (in case of macro args or synthetic pasted-together tokens
        public int i0, i1; // start and end indices in underlyingString
        public String inFileName;
        public int inLineNumber;
        public int inColumnNumber;
        public Token parentInMacroExpansion;
        public Token nextInStack; // can only live in one stack at a time
        public int refCount;

        // No constructor, we use an init function instead,
        // to make sure no members are forgotten.
        public void init(int type,
                         char[] textUnderlyingString,
                         int i0,
                         int i1,
                         String inFileName,
                         int inLineNumber,
                         int inColumnNumber)
        {
            this.type = type;
            this.textUnderlyingString = textUnderlyingString;
            this.i0 = i0;
            this.i1 = i1;
            this.inFileName = inFileName;
            this.inLineNumber = inLineNumber;
            this.inColumnNumber = inColumnNumber;

            this.parentInMacroExpansion = null;
            this.nextInStack = null;
            this.refCount = 0; // we don't do ref counting, caller does
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
                if (textUnderlyingString[i0+i] != s.charAt(i))
                    return false;
            return true;
        }
        public boolean textStartsWith(String s)
        {
            int sLength = s.length();
            if (sLength > i1-i0)
                return false;
            for (int i = 0; i < sLength; ++i)
                if (textUnderlyingString[i0+i] != s.charAt(i))
                    return false;
            return true;
        }
        public boolean textEndsWith(String s)
        {
            int sLength = s.length();
            if (sLength > i1-i0)
                return false;
            for (int i = 0; i < sLength; ++i)
                if (textUnderlyingString[i1-sLength+i] != s.charAt(i))
                    return false;
            return true;
        }


        private static String typeToNameCache[] = null;
        public static String typeToName(int type)
        {
            AssertAlways(type >= 0 && type < NUMTYPES);

            if (typeToNameCache == null)
            {
                typeToNameCache = new String[NUMTYPES];

                // Introspect looking for all the public final static ints...
                java.lang.reflect.Field fields[] = Token.class.getDeclaredFields();
                for (int iField = 0; iField < fields.length; ++iField)
                {
                    java.lang.reflect.Field field = fields[iField];
                    // System.out.println("    "+field+": "+field.getName());
                    int modifiers = field.getModifiers();
                    if (java.lang.reflect.Modifier.isPublic(modifiers)
                     && java.lang.reflect.Modifier.isStatic(modifiers)
                     && java.lang.reflect.Modifier.isFinal(modifiers))
                    {
                        Integer valueObject = null;
                        try
                        {
                            valueObject = (Integer)field.get(null);
                        }
                        catch (IllegalArgumentException e) {}
                        catch (IllegalAccessException e) {}
                        if (valueObject != null)
                        {
                            int value = valueObject.intValue();
                            if (value >= 0 && value < NUMTYPES)
                            {
                                if (typeToNameCache[value] != null)
                                {
                                    throw new Error("Token types "+typeToNameCache[value]+" and "+field.getName()+" have the same value "+value+"??");
                                }
                                typeToNameCache[value] = field.getName();
                            }
                        }
                    }
                }
                for (int i = 0; i < NUMTYPES; ++i)
                {
                    if (typeToNameCache[i] == null)
                        throw new Error("No token with value "+i+"??");
                }
            }

            AssertAlways(typeToNameCache[type] != null);
            return typeToNameCache[type];
        } // typeToName
        // For debug printing
        public String toString()
        {
            return "new Token("
                  +typeToName(this.type)
                  +", \""
                  +escapify(this.textToString())
                  +"\", "
                  +"                         "
                  +", \""
                  +escapify(this.inFileName)
                  +"\", "
                  +this.inLineNumber
                  +", "
                  +this.inLineNumber
                  +")";
        }
    } // class Token


    // Tokens are the thing we are going to create and destroy
    // zillions of during parsing.
    // So, optimize by keeping old discarded tokens for re-use so we don't
    // have to rely on the garbage collector and allocator.
    private static class TokenAllocator
    {
        private int nInUse = 0;
        private int nFree = 0;
        private int nLogicalAllocations = 0; // numbe of times newRefedToken was called
        private int nPhysicalAllocations = 0; // # of times it called new Token()
        private Token freeListHead = null; // we use the parent member to form a linked list


        public Token newRefedToken(int type,
                                   char textUnderlyingString[],
                                   int i0, int i1,
                                   String inFileName, int inLineNumber, int inColumnNumber)
        {
            AssertAlways(nInUse + nFree == nPhysicalAllocations); // logical invariant

            Token token;
            if (freeListHead != null)
            {
                nFree--;
                token = freeListHead;
                freeListHead = token.nextInStack;
                token.nextInStack = null;
            }
            else
            {
                token = new Token();
                nPhysicalAllocations++;
            }

            token.init(type,
                       textUnderlyingString,
                       i0,
                       i1,
                       inFileName,
                       inLineNumber,
                       inColumnNumber);
            AssertAlways(token.refCount == 0);
            token.refCount = 1;

            nInUse++;

            AssertAlways(nInUse + nFree == nPhysicalAllocations); // logical invariant
            nLogicalAllocations++;

            return token;
        } // newRefedToken

        // XXX TODO: make sure someone uses this, I assume they do
        public Token refToken(Token token)
        {
            AssertAlways(token.refCount > 0); // tokens can't exist out in the world with ref count 0
            token.refCount++;
            return token;
        }

        // best practice is for the caller to always set whatever variable
        // was holding token to null immediately after calling this function.
        public void unrefToken(Token token)
        {
            if (--token.refCount <= 0)
            {
                AssertAlways(token.nextInStack == null);
                AssertAlways(token.refCount == 0);
                AssertAlways(nInUse > 0);
                --nInUse;

                unrefToken(token.parentInMacroExpansion); // recursively
                token.parentInMacroExpansion = null;

                // We've carefully unrefed and nulled out the Token members.
                // Make sure token is not holding on to any other pointers...
                token.textUnderlyingString = null;
                token.inFileName = null;

                token.nextInStack = freeListHead;
                freeListHead = token;
                nFree++;

                AssertAlways(nInUse + nFree == nPhysicalAllocations); // logical invariant
            }
        } // unrefToken

        // At all times, nInUse() + nFree() equals nPhysicalAllocations
        // (it's an invariant of all our methods;
        // they always either increment nInUse and decrement nFree, or vice versa,
        // or they increment both nInUse and nPhysicalAllocations).
        // And in a well behaved app
        // (if it doesn't throw past cleanup), 
        // nInUse should be zero at the end.
        public int nInUse()
        {
            AssertAlways(nInUse + nFree == nPhysicalAllocations);
            return nInUse;
        }
        public int nFree()
        {
            AssertAlways(nInUse + nFree == nPhysicalAllocations);
            return nFree;
        }
    } // class TokenAllocator


    private static class TokenStack
    {
        private Token head = null;
        public boolean isEmpty()
        {
            return head == null;
        }
        public void pushAndRef(Token token)
        {
            AssertAlways(token.nextInStack == null);
            token.nextInStack = head;
            head = token;
            token.refCount++; // since it's now referred to by head member

            // transfered ownership of old head from head member
            // to new head.nextInStack, so no need to adjust its ref count
        }

        public Token popAndKeepRef()
        {
            Token token = head;
            head = token.nextInStack;
            token.nextInStack = null;
            // transfered ownership of new head from token.nextInStack to head member,
            // so no need to adjust its ref count

            // transfering ownership of token to the caller,
            // so no need to adjust its ref count either
            return token;
        }
        public void popAndUnref(TokenAllocator tokenAllocator)
        {
            Token token = head;
            head = token.nextInStack;
            token.nextInStack = null;
            // transfered ownership of new head from token.nextInStack to head member,
            // so no need to adjust its ref count

            tokenAllocator.unrefToken(token);
            token = null;
        }
    } // class TokenStack



    private static class TokenStreamFromLineBuffer
    {
        private LineBuffer lineBuffer;
        private int endIndex;
        private int currentIndex;
        private TokenAllocator tokenAllocator;
        private boolean returnedEOF = true; // XXX maybe bad name for this... really means empty... should we just call it isEmpty? hmm... I'm confused
        public void init(LineBuffer lineBuffer, int startIndex, int endIndex,
                         TokenAllocator tokenAllocator)
        {
            this.lineBuffer = lineBuffer;
            this.endIndex = endIndex;
            this.currentIndex = startIndex;
            this.tokenAllocator = tokenAllocator;
            this.returnedEOF = false;
        }
        // keeps ref. if you don't want it, use tokenAllocator.unrefToken(readToken());
        public Token readToken()
        {
            System.err.println("    in tokenStream.readToken");
            AssertAlways(!returnedEOF);

            Token token;
            if (currentIndex == endIndex)
            {
                returnedEOF = true;
                token = null; // XXX maybe caller should guard all readTokens with isEmpty(), then can have simpler semantics?
            }
            else if (lineBuffer.chars[currentIndex] == '\n')
            {
                token = tokenAllocator.newRefedToken(Token.NEWLINE,
                                                     lineBuffer.chars,
                                                     currentIndex, currentIndex+1,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex++;
            }
            else
            {
                token = tokenAllocator.newRefedToken(Token.SYMBOL,
                                                     lineBuffer.chars,
                                                     currentIndex, currentIndex+1,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex++;
            }
            System.err.println("        token = "+token);
            System.err.println("    out tokenStream.readToken");
            return token;
        }
        public boolean isEmpty()
        {
            return returnedEOF;
        }
    } // class TokenStreamFromLineBuffer

    private static class TokenStreamFromLineBufferWithPushBack extends TokenStreamFromLineBuffer
    {
        private TokenStack stack = new TokenStack();

        public void init(LineBuffer lineBuffer, int startIndex, int endIndex,
                         TokenAllocator tokenAllocator)
        {
            AssertAlways(stack.isEmpty());
            super.init(lineBuffer, startIndex, endIndex, tokenAllocator);
        }

        // keeps ref. if you don't want it, use tokenAllocator.unrefToken(readToken());
        public Token readToken()
        {
            return !stack.isEmpty() ? stack.popAndKeepRef()
                                    : super.readToken();
        }
        // it's an error to push back the terminating null
        public void pushBackToken(Token token)
        {
            AssertAlways(token != null);
            stack.pushAndRef(token);
        }
        public boolean isEmpty()
        {
            return stack.isEmpty() && super.isEmpty();
        }
    } // class TokenStreamFromLineBufferWithPushBack



    // Doesn't actually output newlines unless it has to.
    // And then only outputs at most 7 in a row.
    private static class LazyPrintWriter extends java.io.PrintWriter
    {
        private int lineNumberPhysical = 0;
        private int lineNumberLogical = 0;
        private int columnNumber =0; // just for making sure we don't sync when not at end of line... or does caller need to be able to query it?

        // To avoid dismal performance, caller should make sure
        // that reader is either a BufferedWriter  or has one as an ancestor.
        public LazyPrintWriter(java.io.Writer writer)
        {
            super(writer);
        }

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
                super.println();
                lineNumberPhysical++;
                columnNumber = 0;
            }

            if (lineNumberLogical >= lineNumberPhysical
             && lineNumberLogical <= lineNumberPhysical+7)
            {
                while (lineNumberPhysical < lineNumberLogical)
                {
                    super.println();
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
                super.println();
                columnNumber = 0;
                lineNumberPhysical++;
            }
            lineNumberLogical++;
        }
        public void print(String s)
        {
            int sLength = s.length();
            for (int i = 0; i < sLength; ++i)
            {
                char c = s.charAt(i);
                if (c == '\n')
                    println(); // above; fixes line and column numbers
                else
                {
                    AssertAlways(lineNumberPhysical == lineNumberLogical);
                    super.print(c);
                    columnNumber++;
                }
            }
        }
        public void print(char s[], int i0, int i1)
        {
            for (int i = i0; i < i1; ++i)
            {
                char c = s[i];
                if (c == '\n')
                    println(); // above; fixes line and column numbers
                else
                {
                    AssertAlways(lineNumberPhysical == lineNumberLogical);
                    super.print(c);
                    columnNumber++;
                }
            }
        }
        public void println(String s)
        {
            print(s);
            println(); // the other one
        }
    } // class LazyPrintWriter


    public static void filter(LineAndColumnNumberReader in,
                              LazyPrintWriter out,
                              FileOpener fileOpener,
                              String includePath[],
                              java.util.Hashtable macros,
                   
                              LineBuffer lineBuffer, // logically local to loop iteration but we only want to allocate it once
                              TokenStreamFromLineBufferWithPushBack tokenStream, // logically local to loop iteration but we only want to allocate it once
                              TokenAllocator tokenAllocator,
                              ExpressionParser expressionParser,
                   
                              boolean commentOutLineDirectives,
                              int recursionLevel)
        throws java.io.IOException
    {
        while (true)
        {
            getNextLogicalLine(in, lineBuffer);
            if (lineBuffer.length == 0)
                break; // end of file

            tokenStream.init(lineBuffer, 0, lineBuffer.length, tokenAllocator);

            /*
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
            */

            while (true)
            {
                Token token = tokenStream.readToken();
                if (token == null)
                    break;

                /*
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
                */

                if (token.type == Token.NEWLINE)
                    out.println();
                else
                    out.print(token.textUnderlyingString, token.i0, token.i1);
            }


            // Done with the tokens on this line,
            // including the newline.
        }

        System.err.println("in.columnNumber = "+in.columnNumber);
        System.err.println("out.columnNumber = "+out.columnNumber);
        AssertAlways(in.columnNumber == 0);
        AssertAlways(out.columnNumber == 0);
        AssertAlways(tokenStream.isEmpty());
    } // filter

    public static void main(String args[])
    {
        if (verboseLevel >= 1)
        {
            System.err.println("in Cpp.main");
        }
        long t0Millis = System.currentTimeMillis();


        String inFileName = null;

        java.io.Reader reader = null;
        if (inFileName != null)
        {
            try
            {
                reader = new java.io.BufferedReader(
                         new java.io.FileReader(inFileName));
            }
            catch (java.io.FileNotFoundException e)
            {
                System.err.println("javacpp: "+inFileName+": No such file or directory");
                System.exit(1);
            }
        }
        else
        {
            reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(System.in));
            inFileName = "<stdin>";
        }

        LazyPrintWriter writer = new LazyPrintWriter(
                                 new java.io.BufferedWriter( // is this recommended??
                                 new java.io.OutputStreamWriter(System.out)));
        String includePath[] = {};
        java.util.Hashtable macros = new java.util.Hashtable();
        LineBuffer lineBufferScratch = new LineBuffer();
        TokenStreamFromLineBufferWithPushBack tokenStreamScratch = new TokenStreamFromLineBufferWithPushBack();
        TokenAllocator tokenAllocator = new TokenAllocator();
        ExpressionParser expressionParser = new ExpressionParser();
        boolean commentOutLineDirectives = true;

        try
        {
            filter(new LineAndColumnNumberReader(reader, inFileName),
                   writer,
                   new FileOpener(),
                   includePath,
                   macros,
                   lineBufferScratch,
                   tokenStreamScratch,
                   tokenAllocator,
                   expressionParser,
                   commentOutLineDirectives,
                   0); // recursionLevel
        }
        catch (Error e)
        {
            //System.err.println("(Caught error, flushing then rethrowing)");
            writer.flush();
            //System.err.println("(Caught error, rethrowing after flushing)");
            throw e;
        }
        catch (java.io.IOException e)
        {
            writer.flush();
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
        writer.flush();

        if (verboseLevel >= 1)
        {
            long t1Millis = System.currentTimeMillis();
            double totalSeconds = (t1Millis-t0Millis)*1e-3;
            System.err.println("    "+totalSeconds+" seconds");
            System.err.println("out Cpp.main");
        }
        System.exit(0);
    } // main

} // public class Cpp1








