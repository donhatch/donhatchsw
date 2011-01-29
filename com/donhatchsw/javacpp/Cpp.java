/*
 maybe another implementation, maybe cleaner
 TODO: I forget the state of this.  Did I hit a dead end?  Should I abort it?
 I think the whole concept of using Readers is maybe too much complication,
 why not just read the whole file into memory
 and have data structures that allow looking up the line and column number
 of any char at any index?
 Then tokens could be essentially type, start and end index
*/

package com.donhatchsw.javacpp;

public class Cpp
{
    private static final int DEBUG_NONE = 0;
    private static final int DEBUG_OVERALL = 1;
    private static final int DEBUG_PER_FILE = 2;
    private static final int DEBUG_PER_LINE = 3;
    private static final int DEBUG_PER_TOKEN = 4;
    private static final int DEBUG_PER_CHAR = 5;
    // I set the following to the values rather than the variable names above,
    // just so they are easy to change instantly.
    // TokenDebugLevel is separate from inputDebugLevel, because sometimes you want to see token debugging but don't care about line debugging.
    private static int inputDebugLevel  = 2;
    private static int tokenDebugLevel  = 5;
    private static int outputDebugLevel = 5;


    // Logical assertions, always compiled in. Ungracefully bail if violated.
    private static void AssertAlways(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    private static void warning(String message,
                                String fileName, int lineNumber, int columnNumber)
    {
        System.err.println(fileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": warning: "+message);
    }
    private static void error(String message,
                              String fileName, int lineNumber, int columnNumber)
    {
        throw new Error(fileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": error: "+message);
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


    // Can't push back arbitrary chars (since it's impossible
    // to determine line and column info in general)
    // but can push back EOF.
    private static class LineAndColumnNumberReader
    {
        private java.io.LineNumberReader reader;
        private String fileName;
        private String extraCrap = ""; // gets put at end of line number directives
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
            if (inputDebugLevel >= DEBUG_PER_CHAR)
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

            if (inputDebugLevel >= DEBUG_PER_CHAR)
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
        public void pushBackEOF()
        {
            AssertAlways(lookedAheadChar == -2);
            lookedAheadChar = -1;
        }
    } // class LineAndColumnNumberReader



    private static class LineBuffer
    {
        private int length = 0;
        private char chars[] = new char[1];       // these arrays are same size
        private int lineNumbers[] = new int[1];   // these arrays are same size
        private int columnNumbers[] = new int[1]; // these arrays are same size
        private String fileName = null; // XXX TODO: do we want this?
        private int nTokensReferringToMe = 0;

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
        if (inputDebugLevel >= DEBUG_PER_LINE)
            System.err.println("    in getNextLogicalLine");

        AssertAlways(lineBuffer.nTokensReferringToMe == 0); // otherwise not safe to clear!

        boolean correctedEOF = false;
        int lastBackslashLineNumber = -1;
        int lastBackslashColumnNumber = -1;

        lineBuffer.setFileName(in.getFileName());
        lineBuffer.clear();
        while (true)
        {
            int physicalLineStart = lineBuffer.length;
            boolean atEOF = false;

            if (inputDebugLevel >= DEBUG_PER_LINE)
                System.err.println("        reading a physical line");
            // append next physical line...
            while (true)
            {
                int lineNumber = in.getLineNumber();    // before reading a char
                int columnNumber = in.getColumnNumber(); // before reading a char
                int c = in.read();
                if (c == -1) // EOF
                {
                    if (lastBackslashLineNumber != -1)
                        warning("backslash-newline at end of file",
                                lineBuffer.fileName, lastBackslashLineNumber, lastBackslashColumnNumber);
                    if (lineBuffer.length == 0)
                        return; // caller knows this means EOF
                    if (lastBackslashLineNumber == -1) // i.e. if didn't print other message
                        warning("no newline at end of file",
                                lineBuffer.fileName, lineNumber, columnNumber);
                    c = '\n';
                    in.pushBackEOF();
                    correctedEOF = true;
                }
                lineBuffer.append((char)c, lineNumber, columnNumber);
                if (c == '\n')
                    break;
            }

            AssertAlways(lineBuffer.length > physicalLineStart);
            AssertAlways(lineBuffer.chars[lineBuffer.length-1] == '\n');
            if (inputDebugLevel >= DEBUG_PER_LINE)
            {
                System.err.println("            physical line = \""+escapify(new String(lineBuffer.chars, physicalLineStart, lineBuffer.length - physicalLineStart))+"\"");
                System.err.println("        done reading a physical line");
            }
            // okay now the line is terminated by \n,
            // which simplifies things

            // See if the physical line we just added ends in
            // a backslash, optional spaces, and a newline
            int index = lineBuffer.length-1; // index of trailing '\n' for starters
            while (index > physicalLineStart
               && Character.isWhitespace(lineBuffer.chars[index-1]))
                index--;
            if (index > physicalLineStart
             && lineBuffer.chars[index-1] == '\\')
            {
                // there's a backslash continuation.
                int backslashIndex = index-1;
                lastBackslashLineNumber = lineBuffer.lineNumbers[backslashIndex];
                lastBackslashColumnNumber = lineBuffer.columnNumbers[backslashIndex];
                // remove the backslash and any surrounding whitespace and line terminator,
                // and continue on to the next loop iteration,
                // to get another physical line
                if (correctedEOF)
                {
                    lineBuffer.deleteRange(backslashIndex, lineBuffer.length-1); // delete except for trailing newline
                    break;
                }
                if (backslashIndex != lineBuffer.length-2)
                {
                    warning("backslash and newline separated by space",
                            lineBuffer.fileName, lineBuffer.lineNumbers[backslashIndex], lineBuffer.columnNumbers[backslashIndex]);
                }
                lineBuffer.deleteRange(backslashIndex, lineBuffer.length); // delete including trailing newline
                // and continue to get another physical line
            }
            else
            {
                // No backslash continuation.
                // remove trailing whitespace, leaving the terminator, and return
                lineBuffer.deleteRange(index,
                                       lineBuffer.length-1);
                break;
            }

            AssertAlways(!correctedEOF);
        }

        // well that was way more frickin complicated than it should have been

        if (inputDebugLevel >= DEBUG_PER_LINE)
        {
            System.err.println("        logical line = \""+escapify(new String(lineBuffer.chars, 0, lineBuffer.length))+"\"");
            System.err.println("    out getNextLogicalLine");
        }
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
        public char textUnderlyingString[]; // can be lineBuf.chars, or can own its own (in case of macro args or synthetic pasted-together tokens)
        public int i0, i1; // start and end indices in underlyingString
        public String inFileName;
        public int inLineNumber;
        public int inColumnNumber;
        public Token parentInMacroExpansion;
        public Token nextInStack; // can only live in one stack at a time
        public int refCount;
        public LineBuffer lineBufferOwningTextUnderlyingString; // null if we own our own textUnderlyingString

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
            this.refCount = 0; // we don't do ref counting, caller does, optionally
            this.lineBufferOwningTextUnderlyingString = null;
        }
        public void init(int type,
                         LineBuffer lineBuffer,
                         int i0,
                         int i1,
                         String inFileName,
                         int inLineNumber,
                         int inColumnNumber)
        {
            init(type,
                 lineBuffer.chars,
                 i0, i1, inFileName, inLineNumber, inColumnNumber);
            this.lineBufferOwningTextUnderlyingString = lineBuffer;
        }

        // should be used sparingly-- in error/warning/debug printing only
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
                  +this.inColumnNumber
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

        public Token newRefedToken(int type,
                                   LineBuffer lineBuffer,
                                   int i0, int i1,
                                   String inFileName, int inLineNumber, int inColumnNumber)
        {
            Token token = newRefedToken(type,
                                        lineBuffer.chars,
                                        i0, i1, inFileName, inLineNumber, inColumnNumber);
            AssertAlways(token.lineBufferOwningTextUnderlyingString == null);
            token.lineBufferOwningTextUnderlyingString = lineBuffer;
            lineBuffer.nTokensReferringToMe++;
            return token;
        }

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

                if (token.parentInMacroExpansion != null)
                {
                    unrefToken(token.parentInMacroExpansion); // recursively
                    token.parentInMacroExpansion = null;
                }

                if (token.lineBufferOwningTextUnderlyingString != null)
                {
                    --token.lineBufferOwningTextUnderlyingString.nTokensReferringToMe;
                    token.lineBufferOwningTextUnderlyingString = null;
                }



                // We've carefully unrefed and nulled out the Token members.
                // Make sure token is not holding on to any other pointers
                // either...
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
        private Token first = null;
        private Token last = null;
        public boolean isEmpty()
        {
            return first == null;
        }
        public void pushAndRef(Token token)
        {
            AssertAlways(token.nextInStack == null);
            token.nextInStack = first;
            first = token;
            if (last == null)
                last = first;
            token.refCount++; // since it's now referred to by first member (last doesn't refcount anything)

            // transfered ownership of old first from first member
            // to new first.nextInStack, so no need to adjust its ref count
        }
        // TODO: not sure we want this... not sure we want to keep track of last either
        public void addOnBottomAndRef(Token token)
        {
            AssertAlways(token.nextInStack == null);
            if (last != null)
            {
                AssertAlways(last.nextInStack == null);
                last.nextInStack = token;
            }
            else
                first = token;
            last = token;
            token.refCount++; // since it's now referred to by either first or oldLast.nextInStack (last doesn't refcount anything). i.e. it's now referred to by the stack.
        }

        public Token popAndKeepRef()
        {
            Token token = first;
            first = token.nextInStack;
            token.nextInStack = null;
            if (first == null)
                last = null;
            // transfered ownership of new first from token.nextInStack to first member,
            // so no need to adjust its ref count

            // transfering ownership of token to the caller,
            // so no need to adjust its ref count either
            return token;
        }
        public void popAndUnref(TokenAllocator tokenAllocator)
        {
            Token token = popAndKeepRef();
            tokenAllocator.unrefToken(token);
            token = null; // following best practice
        }
    } // class TokenStack


    private static class TokenStreamFromLineBuffer
    {
        private LineBuffer lineBuffer;
        private int endIndex; // XXX not sure this is needed... it's always the end of the line buffer, and the line buffer always ends with '\n' which we use as the terminator when parsing it
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
        public Token readToken(boolean inComment)
        {
            if (tokenDebugLevel >= DEBUG_PER_TOKEN)
                System.err.println("    in tokenStream.readToken");
            AssertAlways(!returnedEOF);

            char chars[] = lineBuffer.chars;
            AssertAlways(chars[endIndex-1] == '\n'); // so don't need to check endIndex all the time, can just use '\n' as a terminator

            Token token;
            if (currentIndex == endIndex)
            {
                // XXX should we even be here? caller should stop at the newline, maybe
                returnedEOF = true;
                token = null; // XXX maybe caller should guard all readTokens with isEmpty(), then can have simpler semantics?
            }
            else if (chars[currentIndex] == '\n')
            {
                token = tokenAllocator.newRefedToken(Token.NEWLINE,
                                                     lineBuffer,
                                                     currentIndex, currentIndex+1,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex++;
            }
            else if (inComment
                  || (chars[currentIndex] == '/'
                   && chars[currentIndex+1] == '*'))
            {
                // find index of end of comment or end of line
                int tokenEndIndex = inComment ? currentIndex
                                              : currentIndex + 2;
                int tokenType;
                while (true)
                {
                    if (chars[tokenEndIndex] == '\n')
                    {
                        tokenType = inComment ? Token.COMMENT_MIDDLE
                                              : Token.COMMENT_START;
                        break;
                    }
                    if (chars[tokenEndIndex] == '*'
                     && chars[tokenEndIndex+1] == '/')
                    {
                        tokenEndIndex += 2;
                        tokenType = inComment ? Token.COMMENT_END
                                              : Token.COMMENT;
                        break;
                    }
                    tokenEndIndex++;
                }
                token = tokenAllocator.newRefedToken(tokenType,
                                                     lineBuffer,
                                                     currentIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else if (chars[currentIndex] == '/'
                  && chars[currentIndex+1] == '/')
            {
                // find index of end of line (the newline or EOF)
                int tokenEndIndex = currentIndex + 2;
                while (chars[tokenEndIndex] != '\n')
                    tokenEndIndex++;
                token = tokenAllocator.newRefedToken(Token.COMMENT,
                                                     lineBuffer,
                                                     currentIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else if (Character.isWhitespace(chars[currentIndex]))
            {
                // find whitespace end
                int tokenEndIndex = currentIndex+1;
                while (chars[tokenEndIndex] != '\n'
                    && Character.isWhitespace(chars[tokenEndIndex]))
                    tokenEndIndex++;
                token = tokenAllocator.newRefedToken(Token.SPACES,
                                                     lineBuffer,
                                                     currentIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else if (Character.isJavaIdentifierStart(chars[currentIndex]))
            {
                // find identifier end
                int tokenEndIndex = currentIndex+1;
                while (Character.isJavaIdentifierPart(chars[tokenEndIndex]))
                    tokenEndIndex++;
                token = tokenAllocator.newRefedToken(Token.IDENTIFIER,
                                                     lineBuffer,
                                                     currentIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else if (Character.isDigit(chars[currentIndex])
                  || (chars[currentIndex] == '.'
                   && Character.isDigit(chars[currentIndex+1])))
            {
                // From http://gcc.gnu.org/onlinedocs/cpp/Tokenization.html
                // "A preprocessing number has a rather bizarre definition. The category includes all the normal integer and floating point constants one expects of C, but also a number of other things one might not initially recognize as a number. Formally, preprocessing numbers begin with an optional period, a required decimal digit, and then continue with any sequence of letters, digits, underscores, periods, and exponents. Exponents are the two-character sequences `e+', `e-', `E+', `E-', `p+', `p-', `P+', and `P-'. (The exponents that begin with `p' or `P' are new to C99. They are used for hexadecimal floating-point constants.)"

                // find "number" end
                int tokenEndIndex = currentIndex+1;
                while (true)
                {
                   if ((chars[tokenEndIndex] == 'e'
                     || chars[tokenEndIndex] == 'E'
                     || chars[tokenEndIndex] == 'p'
                     || chars[tokenEndIndex] == 'P')
                    && (chars[tokenEndIndex+1] == '+'
                     || chars[tokenEndIndex+1] == '-'))
                        tokenEndIndex += 2;
                   else if (chars[tokenEndIndex] == '.'
                         || chars[tokenEndIndex] == '_'
                         || Character.isLetterOrDigit(chars[tokenEndIndex]))
                        tokenEndIndex++;
                   else
                        break;
                }
                token = tokenAllocator.newRefedToken(Token.NUMBER_LITERAL,
                                                     lineBuffer,
                                                     currentIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else if (chars[currentIndex] == '"'
                  || chars[currentIndex] == '\'')
            {
                // find char or string end
                int tokenEndIndex = currentIndex+1;
                char quoteChar = chars[currentIndex];
                while (true)
                {
                    char c = chars[tokenEndIndex++];
                    if (c == '\n')
                        throw new Error("unterminated string or char literal"); // TODO: cpp doesn't do this I don't think
                    if (c == '\\')
                    {
                        // It's impossible for us to see a backslash
                        // followed by a newline at this point,
                        // because the line-getting would have joined
                        // such a line to the next line.
                        AssertAlways(chars[tokenEndIndex] != '\n');
                        tokenEndIndex++; // no matter what it is.
                        // backslash can be followed by up to 3 digits,
                        // or various other things, but we don't have to worry
                        // about that, we handled the necessary case
                        // which is an escaped quote or escaped backslash
                    }
                    else if (c == quoteChar)
                    {
                        break;
                    }
                }
                token = tokenAllocator.newRefedToken(quoteChar=='"' ? Token.STRING_LITERAL
                                                                    : Token.CHAR_LITERAL,
                                                     lineBuffer,
                                                     currentIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else
            {
                token = tokenAllocator.newRefedToken(Token.SYMBOL,
                                                     lineBuffer,
                                                     currentIndex, currentIndex+1,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex++;
            }
            if (tokenDebugLevel >= DEBUG_PER_TOKEN)
            {
                System.err.println("        token = "+token);
                System.err.println("    out tokenStream.readToken");
            }
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
        public Token readToken(boolean inComment)
        {
            return !stack.isEmpty() ? stack.popAndKeepRef()
                                    : super.readToken(inComment);
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
        private int inLineNumber = 0; // the input line number corresponding to outLineNumberPromised
        private int outLineNumberDelivered = 0;
        private int outLineNumberPromised = 0;
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
        // TODO: not sure if this is wanted? not sure yet
        public boolean syncToInLineNumber(int inLineNumber)
        {
            if (inLineNumber == this.inLineNumber)
                return true; // success
            outLineNumberPromised += inLineNumber - this.inLineNumber;

            AssertAlways(columnNumber == 0); // TODO: do we want this?
            // TODO: remove this if we decide on the assert
            if (columnNumber > 0)
            {
                super.println();
                outLineNumberDelivered++;
                columnNumber = 0;
            }

            if (outLineNumberPromised >= outLineNumberDelivered
             && outLineNumberPromised <= outLineNumberDelivered+7)
            {
                while (outLineNumberDelivered < outLineNumberPromised)
                {
                    super.println();
                    outLineNumberDelivered++;
                }
                this.inLineNumber = inLineNumber;
                return true; // success
            }
            else
                return false; // failed, caller must issue line number directive
        }
        public void setInLineNumber(int inLineNumber)
        {
            AssertAlways(columnNumber == 0);
            this.inLineNumber = inLineNumber;
        }

        public void println()
        {
            if (columnNumber != 0)
            {
                super.println();
                columnNumber = 0;
                outLineNumberDelivered++;
            }
            outLineNumberPromised++;
            inLineNumber++;
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
                    AssertAlways(outLineNumberDelivered == outLineNumberPromised);
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
                    AssertAlways(outLineNumberDelivered == outLineNumberPromised);
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
                              java.util.Hashtable macros, // gets updated as we go
                   
                              LineBuffer lineBuffer, // logically local to loop iteration but we only want to allocate it once
                              TokenStreamFromLineBufferWithPushBack tokenStream, // logically local to loop iteration but we only want to allocate it once
                              TokenAllocator tokenAllocator,
                              ExpressionParser expressionParser,
                   
                              boolean commentOutLineDirectives,
                              int recursionLevel)
        throws java.io.IOException
    {
        if (inputDebugLevel >= DEBUG_PER_FILE)
            System.err.println("    in Cpp.filter");

        // XXX TODO: should probably be emitLineNumberDirective
        out.println((commentOutLineDirectives?"// "+(out.outLineNumberDelivered+1+1)+" ":"")+"# "+(in.lineNumber+1)+" \""+in.fileName+"\""+in.extraCrap); // increments outLineNumberDelivered
        out.setInLineNumber(in.lineNumber);

        boolean inComment = false;

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
                Token token = tokenStream.readToken(inComment);
                if (token == null)
                    break;

                if (token.type == (inComment ? Token.COMMENT_END
                                             : Token.COMMENT_START))
                    inComment = !inComment;

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
                {
                    if (out.columnNumber == 0)
                    {
                        if (!out.syncToInLineNumber(token.inLineNumber))
                        {
                            out.outLineNumberPromised = out.outLineNumberDelivered; // release from any promises   TODO: this is unclean
                            out.println((commentOutLineDirectives?"// "+(out.outLineNumberDelivered+1+1)+" ":"")+"# "+(token.inLineNumber+1)+" \""+in.fileName+"\""+in.extraCrap); // increments outLineNumberDelivered
                            out.setInLineNumber(token.inLineNumber);
                        }
                    }
                    out.print(token.textUnderlyingString, token.i0, token.i1);
                }

                tokenAllocator.unrefToken(token);
                token = null;
            }


            // Done with the tokens on this line,
            // including the newline.
        }

        AssertAlways(tokenStream.isEmpty());
        if (inputDebugLevel >= DEBUG_PER_FILE)
            System.err.println("    out Cpp.filter");
    } // filter

    // weird that java.util.Vector doesn't have this...
    // I think its toArray api is broken
    private static Object toArray(java.util.Vector vector, Class arrayType)
    {
        int arrayLength = vector.size();
        Object array = java.lang.reflect.Array.newInstance(
            arrayType.getComponentType(), arrayLength);
        for (int i = 0; i < arrayLength; ++i)
            java.lang.reflect.Array.set(array, i, vector.get(i));
        return array;
    }

    public static class ParsedCommandLineArgs
    {
        String inFileNames[];
        String includePath[]; // from -I flags, and language if c or c++
        String commandLineFakeInput; // #defines for -D and #undefs for -U
        String language; // from -x flag
        boolean commentOutLineDirectives;
        public ParsedCommandLineArgs(
            String inFileNames[],
            String includePath[],
            String commandLineFakeInput,
            String language,
            boolean commentOutLineDirectives)
        {
            this.inFileNames = inFileNames;
            this.includePath = includePath;
            this.commandLineFakeInput = commandLineFakeInput;
            this.language = language;
            this.commentOutLineDirectives = commentOutLineDirectives;
        }
    } // class ParsedCommandLineArgs


    public static ParsedCommandLineArgs parseCommandLineArgs(String args[])
    {
        java.util.Vector inFileNamesVector = new java.util.Vector();
        java.util.Vector includePathVector = new java.util.Vector();
        StringBuffer commandLineFakeInputBuffer = new StringBuffer();
        String language = "java";
        for (int iArg = 0; iArg < args.length; ++iArg)
        {
            String arg = args[iArg];
            if (false) ;
            else if (arg.startsWith("-I"))
            {
                String dir;
                if (arg.equals("-I"))
                {
                    if (iArg+1 == args.length)
                    {
                        System.err.println("javacpp: argument to `-I' is missing");
                        System.exit(1);
                    }
                    dir = args[++iArg];
                }
                else
                    dir = arg.substring(2);
                includePathVector.add(dir);
            }
            else if (arg.startsWith("-D"))
            {
                String nameAndValue;
                if (arg.equals("-D"))
                {
                    if (iArg+1 == args.length)
                    {
                        System.err.println("javacpp: argument to `-D' is missing");
                        System.exit(1);
                    }
                    nameAndValue = args[++iArg];
                }
                else
                    nameAndValue = arg.substring(2);
                int indexOfFirstEqualsSign = nameAndValue.indexOf('=');
                String name, value;
                if (indexOfFirstEqualsSign != -1)
                {
                    if (indexOfFirstEqualsSign == 0)
                    {
                        System.err.println("javacpp: `-D' is missing macro name");
                        System.exit(1);
                    }
                    name = nameAndValue.substring(0, indexOfFirstEqualsSign);
                    value = nameAndValue.substring(indexOfFirstEqualsSign+1);
                }
                else
                {
                    name = nameAndValue;
                    value = "1";
                }
                commandLineFakeInputBuffer.append("#define "+name+" "+value+"\n");
            }
            else if (arg.startsWith("-U"))
            {
                String name;
                if (arg.equals("-U"))
                {
                    if (iArg+1 == args.length)
                    {
                        System.err.println("javacpp: argument to `-U' is missing");
                        System.exit(1);
                    }
                    name = args[++iArg];
                }
                else
                    name = arg.substring(2);
                commandLineFakeInputBuffer.append("#undef "+name+"\n");
            }
            else if (arg.startsWith("-x"))
            {
                if (arg.equals("-x"))
                {
                    if (iArg+1 == args.length)
                    {
                        System.err.println("javacpp: argument to `-x' is missing");
                        System.exit(1);
                    }
                    language = args[++iArg];
                }
                else
                    language = arg.substring(2);
            }
            else if (arg.startsWith("-"))
            {
                System.err.println("javacpp: unrecognized option \""+args[iArg]+"\"");
                System.exit(1);
            }
            else
            {
                String inFileName = arg;
                inFileNamesVector.add(inFileName);
            }
        }

        boolean commentOutLineDirectives = false;
        if (language.equals("java"))
        {
            commentOutLineDirectives = true;
        }
        else if (language.equals("c++")
              || language.equals("c"))
        {
            // just imitate what I have on the machine I'm writing this on...

            if (language.equals("c++"))
            {
                includePathVector.add("/usr/lib/gcc/i386-redhat-linux/3.4.6/../../../../include/c++/3.4.6");
                includePathVector.add("/usr/lib/gcc/i386-redhat-linux/3.4.6/../../../../include/c++/3.4.6/i386-redhat-linux");
                includePathVector.add("/usr/lib/gcc/i386-redhat-linux/3.4.6/../../../../include/c++/3.4.6/backward");
            }
            includePathVector.add("/usr/local/include");
            includePathVector.add("/usr/lib/gcc/i386-redhat-linux/3.4.6/include");
            includePathVector.add("/usr/include");
        }
        else
        {
            System.err.println("language "+language+" not recognized");
            System.exit(1);
        }

        return new ParsedCommandLineArgs(
            (String[])toArray(inFileNamesVector, String[].class),
            (String[])toArray(includePathVector, String[].class),
            commandLineFakeInputBuffer.toString(),
            language,
            commentOutLineDirectives);
    } // parseArgs

    public static void main(String args[])
    {
        if (inputDebugLevel >= DEBUG_OVERALL)
        {
            System.err.println("in Cpp.main");
        }
        long t0Millis = System.currentTimeMillis();

        ParsedCommandLineArgs parsedArgs = parseCommandLineArgs(args);

        if (parsedArgs.inFileNames.length > 1)
        {
            System.err.println("javacpp: too many input files");
            System.exit(1);
        }
        String inFileName = parsedArgs.inFileNames.length == 0 ? null :
                            parsedArgs.inFileNames[0];

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


        /*
        doIt(java.io.Reader in,
             java.io.Writer out,


    static void doIt(java.io.Reader in,
                     java.io.Writer out,
        */






        LazyPrintWriter writer = new LazyPrintWriter(
                                 new java.io.BufferedWriter( // is this recommended??
                                 new java.io.OutputStreamWriter(System.out)));
        LineBuffer lineBufferScratch = new LineBuffer();
        TokenStreamFromLineBufferWithPushBack tokenStreamScratch = new TokenStreamFromLineBufferWithPushBack();
        TokenAllocator tokenAllocator = new TokenAllocator();
        ExpressionParser expressionParser = new ExpressionParser();
        java.util.Hashtable macros = new java.util.Hashtable();

        try
        {
            filter(new LineAndColumnNumberReader(reader, inFileName),
                   writer,
                   new FileOpener(),
                   parsedArgs.includePath,
                   macros,
                   lineBufferScratch,
                   tokenStreamScratch,
                   tokenAllocator,
                   expressionParser,
                   parsedArgs.commentOutLineDirectives,
                   0); // recursionLevel
        }
        catch (Error e)
        {
            //System.err.println("(Caught error, flushing then rethrowing)");
            writer.flush();
            //System.err.println("(Caught error, flushed, rethrowing)");
            throw e;
        }
        catch (java.io.IOException e)
        {
            writer.flush();
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
        writer.flush();

        if (inputDebugLevel >= DEBUG_OVERALL)
        {
            long t1Millis = System.currentTimeMillis();
            double totalSeconds = (t1Millis-t0Millis)*1e-3;
            System.err.println("    "+totalSeconds+" seconds");

            System.err.println("    "+lineBufferScratch.nTokensReferringToMe+" tokens referring to lineBufferScratch");
            System.err.println("    "+tokenAllocator.nInUse+" tokens in use");
            System.err.println("    "+tokenAllocator.nFree+" tokens free");
            System.err.println("    "+tokenAllocator.nPhysicalAllocations+" physical allocations");
            System.err.println("    "+tokenAllocator.nLogicalAllocations+" logical allocationa");
            System.err.println("    line buffer max length = "+lineBufferScratch.chars.length);

            System.err.println("out Cpp.main");
        }
        AssertAlways(lineBufferScratch.nTokensReferringToMe == 0);
        AssertAlways(tokenAllocator.nInUse == 0);
        System.exit(0);
    } // main

} // public class Cpp








