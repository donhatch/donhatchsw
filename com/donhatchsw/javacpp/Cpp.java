/*
 maybe another implementation, maybe cleaner
*/

package com.donhatchsw.javacpp;

public class Cpp
{
    // TODO: If this really doesn't give us anything, should just remove
    // the whole mechanism.
    public static final boolean useTokenPool = true;

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
    private static int tokenDebugLevel  = 2;
    private static int outputDebugLevel = 2;


    // Logical assertions, always compiled in. Ungracefully bail if violated.
    private static void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }

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
            CHECK(lookedAheadChar == -2);
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
            CHECK(i0 <= i1);
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

        CHECK(lineBuffer.nTokensReferringToMe == 0); // otherwise not safe to clear!

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

            CHECK(lineBuffer.length > physicalLineStart);
            CHECK(lineBuffer.chars[lineBuffer.length-1] == '\n');
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

            CHECK(!correctedEOF);
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
        public static final int NUMBER_LITERAL = NUMTYPES++;    // "\\.?[0-9]([0-9a-zA-Z_\\.]|[eEpP][+-])*" note that it doesn't include initial '-'! rather bizarre definition, see http://gcc.gnu.org/onlinedocs/cpp/Tokenization.html
        public static final int SYMBOL = NUMTYPES++;
        public static final int SPACES = NUMTYPES++;
        public static final int COMMENT = NUMTYPES++;
        public static final int COMMENT_START = NUMTYPES++; // XXX TODO: do we want this?
        public static final int COMMENT_MIDDLE = NUMTYPES++; // XXX TODO: do we want this?
        public static final int COMMENT_END = NUMTYPES++; // XXX TODO: do we want this?
        public static final int NEWLINE = NUMTYPES++;
        public static final int PREPROCESSOR_DIRECTIVE = NUMTYPES++;

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

        public boolean textIsEmpty()
        {
            return i1 == i0;
        }
        public boolean textStartsWithDigit()
        {
            if (i1-i0 == 0)
                return false;
            char c = textUnderlyingString[i0];
            return c >= '0' && c <= '9';
        }
        // returns 0 on empty, -1 if not all digits
        public int textToNonNegativeInt()
        {
            int n = 0;
            for (int i = i0; i < i1; ++i)
            {
                char c = textUnderlyingString[i];
                if (c < '0' || c > '9')
                    return -1;
                n = n*10 + (c-'0');
            }
            return n;
        }


        private static String typeToNameCache[] = null;
        public static String typeToName(int type)
        {
            CHECK(type >= 0 && type < NUMTYPES);

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

            CHECK(typeToNameCache[type] != null);
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
                  +", "
                  +(this.inFileName==null ? "null"
                                          : "\""+escapify(this.inFileName))+"\""
                  +", "
                  +this.inLineNumber
                  +", "
                  +this.inColumnNumber
                  +")";
        }
    } // class Token

    private static class Macro
    {
        // Doesn't include the name
        public int numParams; // -1 means no parens even
        public Token[] contents; // args denoted by token type MACRO_ARG with line number containing the index of the argument to be substituted
        public String inFileName;
        public int inLineNumber;
        public int inColumnNumber; // XXX maybe don't need... I think cpp's messages pertaining to macros always say column 1
        public Macro(int numParams, Token[] contents,
                     String inFileName, int inLineNumber, int inColumnNumber)
        {
            this.numParams = numParams;
            this.contents = contents;
            this.inFileName = inFileName;
            this.inLineNumber = inLineNumber;
            this.inColumnNumber = inColumnNumber;
        }

        // For debug printing
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("new Macro("
                     +this.numParams
                     +", ");
            if (this.contents == null)
                sb.append("null");
            else
            {
                sb.append("{\n");
                for (int iContent = 0; iContent < this.contents.length; ++iContent)
                {
                    sb.append("    "+iContent+": ");
                    sb.append(this.contents[iContent]);
                    sb.append("\n");
                }
                sb.append("}");
            }
            sb.append(")");
            return sb.toString();
        }

        // tell whether one macro is the same as another,
        // for deciding whether to warn about it being redefined.
        // note this is semantic comparison, it doesn't care
        // if the param names are different.
        public boolean sameContents(Macro other)
        {
            if (numParams != other.numParams)
                return false;
            if ((contents==null) != (other.contents==null))
                return false;
            if (contents != null)
            {
                if (contents.length != other.contents.length)
                    return false;
                for (int i = 0; i < contents.length; ++i)
                {
                    if (contents[i].type != other.contents[i].type)
                        return false;
                    if (!contents[i].textToString().equals(other.contents[i].textToString())) // not very efficient, but this isn't going to be used much
                        return false;
                }
            }
            return true;
        }
    } // private static class Macro


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
        private int nPrivateBuffersAllocated = 0; // # of times it called new char[]
        private Token freeListHead = null; // we use the parent member to form a linked list


        public Token newRefedToken(int type,
                                   char textUnderlyingString[],
                                   int i0, int i1,
                                   String inFileName, int inLineNumber, int inColumnNumber)
        {
            CHECK(nInUse + nFree == nPhysicalAllocations); // logical invariant

            Token token;
            if (useTokenPool && freeListHead != null)
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
            CHECK(token.refCount == 0);
            token.refCount = 1;

            nInUse++;

            CHECK(nInUse + nFree == nPhysicalAllocations); // logical invariant
            //System.err.println("TOKEN LOGICAL ALLOCATION: "+token);
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
            CHECK(token.lineBufferOwningTextUnderlyingString == null);
            token.lineBufferOwningTextUnderlyingString = lineBuffer;
            lineBuffer.nTokensReferringToMe++;
            return token;
        }

        // clone existing token so that we own our own memory
        public Token newRefedTokenCloned(Token token)
        {
            int i0 = token.i0, i1 = token.i1;

            char textUnderlyingString[];
            if (token.lineBufferOwningTextUnderlyingString != null)
            {
                // Need to copy the buffer, since line buffers are volatile
                textUnderlyingString = new char[i1-i0];
                for (int i = 0; i < textUnderlyingString.length; ++i)
                    textUnderlyingString[i] = token.textUnderlyingString[i0+i];
                nPrivateBuffersAllocated++;
            }
            else
            {
                // can share the buffer
                textUnderlyingString = token.textUnderlyingString;
            }

            return newRefedToken(token.type,
                                 textUnderlyingString,
                                 0,
                                 textUnderlyingString.length,
                                 token.inFileName,
                                 token.inLineNumber,
                                 token.inColumnNumber);
        }

        public Token refToken(Token token)
        {
            CHECK(token.refCount > 0); // tokens can't exist out in the world with ref count 0
            //System.err.println("TOKEN REF COUNT "+token.refCount+" -> "+(token.refCount+1)+" : "+token);
            token.refCount++;
            return token;
        }

        // best practice is for the caller to always set whatever variable
        // was holding token to null immediately after calling this function.
        public void unrefToken(Token token)
        {
            //System.err.println("TOKEN REF COUNT "+token.refCount+" -> "+(token.refCount-1)+" : "+token);
            CHECK(token.refCount > 0);
            if (--token.refCount <= 0)
            {
                CHECK(token.nextInStack == null);
                CHECK(token.refCount == 0);
                CHECK(nInUse > 0);
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



                if (useTokenPool)
                {
                    // We've carefully unrefed and nulled out the Token's members.
                    // Make sure token is not holding on to any other pointers
                    // either...
                    token.textUnderlyingString = null;
                    token.inFileName = null;

                    token.nextInStack = freeListHead;
                    freeListHead = token;
                }
                nFree++;

                CHECK(nInUse + nFree == nPhysicalAllocations); // logical invariant
            }
        } // unrefToken

        public void unrefTokensInMacro(Macro macro)
        {
            Token contents[] = macro.contents;
            if (contents != null)
                for (int i = 0; i < contents.length; ++i)
                {
                    this.unrefToken(contents[i]);
                    contents[i] = null;
                }
        }

        // At all times, nInUse() + nFree() equals nPhysicalAllocations
        // (it's an invariant of all our methods;
        // they always either increment nInUse and decrement nFree, or vice versa,
        // or they increment both nInUse and nPhysicalAllocations).
        // And in a well behaved app
        // (if it doesn't throw past cleanup), 
        // nInUse should be zero at the end.
        public int nInUse()
        {
            CHECK(nInUse + nFree == nPhysicalAllocations);
            return nInUse;
        }
        public int nFree()
        {
            CHECK(nInUse + nFree == nPhysicalAllocations);
            return nFree;
        }
    } // class TokenAllocator



    private static class TokenStack
    {
        private Token first = null;
        private Token last = null;
        private int size = 0;
        public boolean isEmpty()
        {
            return first == null;
        }
        public void pushAndKeepRef(Token token)
        {
            CHECK(token.nextInStack == null);
            token.nextInStack = first;
            first = token;
            if (last == null)
                last = first;

            // transfered ownership of old first from first member
            // to new first.nextInStack, so no need to adjust its ref count

            size++;
        }
        public void pushAndRef(Token token)
        {
            pushAndKeepRef(token);
            token.refCount++; // since it's now referred to by first member (last doesn't refcount anything)
        }
        // TODO: not sure we want this... not sure we want to keep track of last either
        public void addOnBottomAndRef(Token token)
        {
            CHECK(token.nextInStack == null);
            if (last != null)
            {
                CHECK(last.nextInStack == null);
                last.nextInStack = token;
            }
            else
                first = token;
            last = token;
            token.refCount++; // since it's now referred to by either first or oldLast.nextInStack (last doesn't refcount anything). i.e. it's now referred to by the stack.

            size++;
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
            size--;
            return token;
        }
        public void popAndUnref(TokenAllocator tokenAllocator)
        {
            Token token = popAndKeepRef();
            tokenAllocator.unrefToken(token);
            token = null; // following best practice
            size--;
        }
        public Token top()
        {
            return first;
        }
        public int size()
        {
            return size;
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
                System.err.println("            in tokenStream.readToken");
            CHECK(!returnedEOF);

            char chars[] = lineBuffer.chars;
            CHECK(chars[endIndex-1] == '\n'); // so don't need to check endIndex all the time, can just use '\n' as a terminator

            int spacesEndIndex = currentIndex;
            while (spacesEndIndex < endIndex // XXX TODO: can remove this if caller stops calling us after newline
                && chars[spacesEndIndex] != '\n'
                && Character.isWhitespace(chars[spacesEndIndex]))
                spacesEndIndex++;


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
            else if (currentIndex == 0
                  && chars[spacesEndIndex] == '#')
            {
                int tokenStartIndex = spacesEndIndex + 1;
                while (chars[tokenStartIndex] != '\n'
                    && Character.isWhitespace(chars[tokenStartIndex]))
                    tokenStartIndex++;
                int tokenEndIndex = tokenStartIndex;
                while (Character.isJavaIdentifierPart(chars[tokenEndIndex]))
                    tokenEndIndex++;
                currentIndex = tokenStartIndex; // so we get line and col right below
                token = tokenAllocator.newRefedToken(Token.PREPROCESSOR_DIRECTIVE,
                                                     lineBuffer,
                                                     tokenStartIndex, tokenEndIndex,
                                                     lineBuffer.fileName,
                                                     lineBuffer.lineNumbers[currentIndex],
                                                     lineBuffer.columnNumbers[currentIndex]);
                currentIndex = tokenEndIndex;
            }
            else if (spacesEndIndex != currentIndex) // this test must come after the one for PREPROCESSOR_DIRECTIVE
            {
                int tokenEndIndex = spacesEndIndex;
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
                        CHECK(chars[tokenEndIndex] != '\n');
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
                // At this point we should recognize symbols
                // used by the C compiler,
                // but it's fine to just assume they are all single chars.
                // (For example, "&&" should be a token,
                // but we just treat it as two tokens "&" "&" instead.)
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
                System.err.println("                token = "+token);
                System.err.println("            out tokenStream.readToken");
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
            CHECK(stack.isEmpty());
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
            CHECK(token != null);
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

        private boolean keepSynced = true; // kind of hacky way for caller to turn on and off syncing (there should be no syncing or asserting synced if inside comments)

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
        public boolean softSyncToInLineNumber(int inLineNumber)
        {
            outLineNumberPromised += inLineNumber - this.inLineNumber;
            if (outLineNumberPromised == outLineNumberDelivered)
            {
                this.inLineNumber = inLineNumber;
                return true; // success
            }

            CHECK(columnNumber == 0); // TODO: do we want this?
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
            {
                // failed, caller must issue line number directive.
                // It's okay that we messed up outLineNumberPromised,
                // it's about to be reset anyway.
                return false;
            }
        }
        public void hardSyncToInLineNumber(int inLineNumber,
                                           String inFileName,
                                           boolean commentOutLineDirectives,
                                           String extraCrap)
        {
            outLineNumberPromised = outLineNumberDelivered; // release from any promises
            println((commentOutLineDirectives?"// "+(outLineNumberDelivered+1+1)+" ":"")+"# "+(inLineNumber+1)+" \""+inFileName+"\""+extraCrap); // increments outLineNumberPromised and outLineNumberDelivered
            setInLineNumber(inLineNumber);
        }
        // XXX I think this is only called from hardSync, it can maybe just be removed
        public void setInLineNumber(int inLineNumber)
        {
            CHECK(columnNumber == 0);
            this.inLineNumber = inLineNumber;
        }


        public void println()
        {
            if (outputDebugLevel >= DEBUG_PER_LINE)
                System.err.println("                in LazyPrintWriter.println()");
            if (!keepSynced || columnNumber != 0)
            {
                super.println();
                columnNumber = 0;
                outLineNumberDelivered++;
            }
            outLineNumberPromised++;
            inLineNumber++;
            if (outputDebugLevel >= DEBUG_PER_LINE)
                System.err.println("                out LazyPrintWriter.println()");
        }

        public void print(char s[], int i0, int i1)
        {
            if (outputDebugLevel >= DEBUG_PER_TOKEN)
                System.err.println("            in LazyPrintWriter.print(char[])");
            for (int i = i0; i < i1; ++i)
            {
                char c = s[i];
                if (c == '\n')
                    println(); // above; fixes line and column numbers
                else
                {
                    if (keepSynced && columnNumber == 0)
                    {
                        if (outLineNumberDelivered != outLineNumberPromised)
                        {
                            throw new Error("INTERNAL ERROR: delivered line number "+outLineNumberDelivered+", promised line number "+outLineNumberPromised+"");
                        }
                        CHECK(outLineNumberDelivered == outLineNumberPromised);
                    }
                    super.print(c);
                    columnNumber++;
                }
            }
            if (outputDebugLevel >= DEBUG_PER_TOKEN)
                System.err.println("            out LazyPrintWriter.print(char[])");
        }
        public void print(String s)
        {
            // This doesn't get called very often-- only on syncs, I think.
            // so do something simple and maybe not that efficient...
            print(s.toCharArray(), 0, s.length());
        }

        public void println(String s)
        {
            if (outputDebugLevel >= DEBUG_PER_TOKEN)
                System.err.println("                in LazyPrintWriter.println(s=\""+escapify(s)+"\")");
            print(s); // above
            println(); // the other one
            if (outputDebugLevel >= DEBUG_PER_TOKEN)
                System.err.println("                out LazyPrintWriter.println(s=\""+escapify(s)+"\")");
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
        {
            System.err.println("    in Cpp.filter(\""+escapify(in.fileName)+"\")");
        }

        // Stack of #ifwhatever tokens whose scope we are in,
        // for the file,line,column information
        // that we'll need to emit if an error occurs.
        //
        // In the case of #else, we push #else on the stack
        // along with the #if, since we'll need both
        // for an error message like gcc's
        // (it's a bit odd, the text is #else but the line/column info
        // is that of the original #if).
        // When popping, if we see an #else, we pop both the #else
        // and the #if that's underneath it.
        // XXX alternative: push a synthetic token with text #else but line/column from original #if
        //
        // The short-circuiting logic becomes incomprehensible for #elif,
        // so internally we treat #elif as #else #if, e.g.
        //      #if A
        //      #elif B
        //      #else
        //      #endif
        // becomes:
        //      #if A
        //      #else
        //          #if B
        //          #else
        //          #endif
        //      #endif
        // and:
        //      #if A
        //      #elif B
        //      #elif C
        //      #else
        //      #endif
        // becomes:
        //      #if A
        //      #else
        //          #if B
        //          #else
        //              #if C
        //              #else
        //              #endif
        //          #endif
        //      #endif
        // so each time we hit an #elif, we push it on the stack once
        // as if it was an #else,
        // and then again as if it was an #if.
        // (Don't think too hard about this-- it works.)
        // When it's time to pop (on an #endif), we keep popping #else's and #elif's
        // until we pop the original #if (or #ifdef or #ifndef).
        //
        TokenStack ifStack = new TokenStack(); // XXX caller should provide, maybe
        int highestTrueIfStackLevel = 0;

        out.hardSyncToInLineNumber(in.lineNumber, in.fileName, commentOutLineDirectives, in.extraCrap);

        boolean inComment = false;

        while (true)
        {
            getNextLogicalLine(in, lineBuffer);
            if (lineBuffer.length == 0)
                break; // end of file

            // TODO: also assert it's the only one
            CHECK(lineBuffer.chars[lineBuffer.length-1] == '\n');

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
                {
                    // don't print anything--
                    // newlines only get printed when necessary
                    // to sync up other text to the desired line numbers
                    if (true) // XXX wait a minute, why is this needed??
                        out.println();
                }
                else if (token.type == Token.PREPROCESSOR_DIRECTIVE)
                {
                    CHECK(!inComment);

                    // When inside a false #if,
                    // the only preprocessor directives we recognize are:
                    //     #if*
                    //     #endif
                    //     #elif
                    //     #else
                    // I.e. we do not recognize:
                    //     #define
                    //     #undef
                    //     #include
                    //     and anything else, including unrecognized directives
                    boolean inFalseIf = ifStack.size() > highestTrueIfStackLevel;
                    if (!inFalseIf
                     || token.textEquals("if")
                     || token.textEquals("ifdef")
                     || token.textEquals("ifndef")
                     || token.textEquals("endif")
                     || token.textEquals("elif")
                     || token.textEquals("else"))
                    {
                        // In all cases, move past spaces and comments
                        // TODO: for #ifdef, #ifndef, #undef, we need to do this WITHOUT macro substitution, so that we don't expand the expected macro name.  Others do it with macro substitution, I think.
                        Token nextToken = tokenStream.readToken(inComment);
                        while (nextToken.type == Token.SPACES
                            || nextToken.type == Token.COMMENT
                            || nextToken.type == Token.COMMENT_START)
                        {
                            if (nextToken.type == Token.COMMENT_START)
                            {
                                // have to print it so we don't end up outputting the end without the start.
                                // gcc just doesn't output such comments at all, but we aren't in a position to be able to imitate it since we don't have much coherency between lines.
                                out.print(nextToken.textUnderlyingString, nextToken.i0, nextToken.i1);
                                inComment = true;
                                out.keepSynced = !inComment;
                                // next token is guaranteed to be a NEWLINE
                            }
                            tokenAllocator.unrefToken(nextToken);
                            nextToken = tokenStream.readToken(inComment); // XXX need macro substitution?
                        }

                        if (false) ;

                        // ones that don't take anything
                        else if (token.textEquals("else")   // #else
                              || token.textEquals("endif")) // #endif
                        {
                            if (ifStack.isEmpty())
                                throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": #"+token.textToString()+" without #if");

                            if (token.textEquals("else"))
                            {
                                if (ifStack.top().textEquals("else"))
                                {
                                    // find the original #if token
                                    Token originalIfToken = ifStack.popAndKeepRef();
                                    while (!originalIfToken.textEquals("if"))
                                    {
                                        tokenAllocator.unrefToken(originalIfToken);
                                        originalIfToken = ifStack.popAndKeepRef();
                                    }
                                    throw new Error(
                                        token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": #"+token.textToString()+" after #else"
                                      + "\n" // XXX \r\n on windows?
                                      + originalIfToken.inFileName+":"+(originalIfToken.inLineNumber+1)+":"+(originalIfToken.inColumnNumber+1)+": the conditional began here");
                                }
                                // Only consider changing state
                                // if parent state was true...
                                if (highestTrueIfStackLevel >= ifStack.size()-1)
                                {
                                    if (highestTrueIfStackLevel >= ifStack.size()) // was true
                                        highestTrueIfStackLevel = ifStack.size(); // change from true to false as we push
                                    else // was false
                                        highestTrueIfStackLevel = ifStack.size()+1; // change from false to true as we push

                                }
                                ifStack.pushAndKeepRef(tokenAllocator.newRefedTokenCloned(token));
                            }
                            else // #endif
                            {
                                // pop til we pop an #if*
                                for (boolean gotIf = false; !gotIf;)
                                {
                                    CHECK(!ifStack.isEmpty()); // stack was nonempty before, so there's got to be an actual #if* at the bottom of the stack
                                    Token poppedToken = ifStack.popAndKeepRef();
                                    gotIf = poppedToken.textStartsWith("if");
                                    tokenAllocator.unrefToken(poppedToken);
                                    poppedToken = null;
                                }
                            }
                        }

                        // ones that take a macro name
                        else if (token.textEquals("ifdef")   // #ifdef
                              || token.textEquals("ifndef")  // #ifndef
                              || token.textEquals("undef")   // #undef
                              || token.textEquals("define")) // #define
                        {
                            // XXX oops, already ate up the space above, and did it wrong
                            // move past spaces between directive and macro name
                            while (nextToken.type == Token.SPACES
                                || nextToken.type == Token.COMMENT)
                            {
                                tokenAllocator.unrefToken(nextToken);
                                nextToken = tokenStream.readToken(inComment); // WITHOUT macro substitution, so we don't expand the expected macro name
                            }

                            if (nextToken.type == Token.NEWLINE)
                            {
                                throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": no macro name given in #"+token.textToString()+" directive");
                            }

                            if (nextToken.type == Token.COMMENT_START)
                            {
                                // Got something like "#ifdef /*\n*/ foo"
                                // Could try to implement this, but it's not that important.
                                throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": unimplemented: multi-line comment in "+token.textToString()+" directive");
                            }

                            if (nextToken.type != Token.IDENTIFIER)
                                throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": macro names must be identifiers");
                            String macroName = nextToken.textToString();
                            tokenAllocator.unrefToken(nextToken);
                            nextToken = tokenStream.readToken(inComment); // XXX needs to be WITHOUT macro expansion (in case it's #define, we don't want to expand the definition while it's being defined)

                            if (token.textEquals("define")) // #define
                            {
                                int verboseLevel = 0; // XXX turn this into one of the debug things
                                if (verboseLevel >= 2)
                                    System.err.println("        filter: found #define");
                                CHECK(!inFalseIf); // we checked above

                                // must be either whitespace or left paren after macro name... it makes a difference

                                String paramNames[] = null;
                                if (nextToken.type == Token.SYMBOL
                                 && nextToken.textEquals("("))
                                {
                                    if (verboseLevel >= 2)
                                        System.err.println("        filter:     and there's a macro param list");
                                    // There's a macro param list.
                                    java.util.Vector paramNamesVector = new java.util.Vector();

                                    tokenAllocator.unrefToken(nextToken);
                                    nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily

                                    // move past spaces
                                    while (nextToken.type == Token.SPACES
                                        || nextToken.type == Token.COMMENT)
                                    {
                                        tokenAllocator.unrefToken(nextToken);
                                        nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily
                                    }

                                    if (nextToken.type == Token.SYMBOL
                                     && nextToken.textEquals(")"))
                                    {
                                        // zero params
                                    }
                                    else
                                    {
                                        // must be one or more param names,
                                        // separated by commas,
                                        // followed by close paren

                                        if (nextToken.type != Token.IDENTIFIER)
                                            throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever

                                        paramNamesVector.add(nextToken.textToString());
                                        tokenAllocator.unrefToken(nextToken);
                                        nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily

                                        while (true)
                                        {
                                            // move past spaces
                                            while (nextToken.type == Token.SPACES
                                                || nextToken.type == Token.COMMENT)
                                            {

                                                tokenAllocator.unrefToken(nextToken);
                                                nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily
                                            }

                                            if (nextToken.type == Token.SYMBOL)
                                            {
                                                if (nextToken.textEquals(")"))
                                                    break;
                                                else if (nextToken.textEquals(","))
                                                {
                                                    tokenAllocator.unrefToken(nextToken);
                                                    nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily

                                                    // move past spaces
                                                    while (nextToken.type == Token.SPACES
                                                        || nextToken.type == Token.COMMENT)
                                                    {
                                                        tokenAllocator.unrefToken(nextToken);
                                                        nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily
                                                    }

                                                    if (nextToken.type == Token.IDENTIFIER)
                                                    {
                                                        paramNamesVector.add(nextToken.textToString());
                                                        tokenAllocator.unrefToken(nextToken);
                                                        nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily
                                                        continue;
                                                    }
                                                    // otherwise drop into error case
                                                }
                                            }
                                            throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever
                                        }
                                    }
                                    CHECK(nextToken.type == Token.SYMBOL
                                              && nextToken.textEquals(")"));
                                    tokenAllocator.unrefToken(nextToken);
                                    nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily

                                    paramNames = (String[])paramNamesVector.toArray(new String[0]);
                                }
                                else if (nextToken.type == Token.SPACES
                                      || nextToken.type == Token.COMMENT
                                      || nextToken.type == Token.NEWLINE)
                                {
                                    if (verboseLevel >= 2)
                                        System.err.println("        filter:     and there's no macro param list");
                                    ;
                                }
                                else
                                {
                                    // macro name was not followed by a macro param list
                                    // nor spaces
                                    throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever
                                }

                                // we are now in the #define, past the macro name and optional arg list.
                                // next comes the content, up to an unescaped newline
                                // or eof.
                                // still using nextToken to hold the next token we are about to look at.

                                java.util.Vector contentsVector = new java.util.Vector();

                                while (nextToken.type != Token.NEWLINE)
                                {
                                    if (paramNames != null)
                                    {
                                        if (nextToken.type == Token.IDENTIFIER)
                                        {
                                            for (int i = 0; i < paramNames.length; ++i)
                                                if (nextToken.textEquals(paramNames[i]))
                                                {
                                                    tokenAllocator.unrefToken(nextToken);
                                                    nextToken = tokenAllocator.newRefedToken(Token.MACRO_ARG, new char[0], 0, 0, null, i, -1); // smuggle in param index through line number
                                                    break;
                                                }
                                            // if not found, it stays identifier
                                        }
                                        else if (nextToken.type == Token.PREPROCESSOR_DIRECTIVE)
                                        {
                                            // XXX what the fuck is this? this is all messed up... in the first version, I recognized preprocessor directives anywhere in the line, so I could hijack that here.  maybe should do that again? not sure
                                            if (true) throw new Error("XXX hijacking! argh!");

                                            String paramNameMaybe = nextToken.textToString(); // spaces got crunched out already during token lexical scanning
                                            if (paramNameMaybe.equals("#"))
                                            {
                                                //System.err.println(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": hey! "+macroName+" is a token pasting macro!");
                                                tokenAllocator.unrefToken(nextToken);
                                                nextToken = tokenAllocator.newRefedToken(Token.TOKEN_PASTE, new char[]{'#','#'}, 0,2, null, -1, -1);
                                            }
                                            else
                                            {
                                                for (int i = 0; i < paramNames.length; ++i)
                                                    if (paramNameMaybe.equals(paramNames[i]))
                                                    {
                                                        tokenAllocator.unrefToken(nextToken);
                                                        nextToken = tokenAllocator.newRefedToken(Token.MACRO_ARG_QUOTED, (char[])null, 0, 0, null, i, -1); // smuggle in param index through line number
                                                        break;
                                                    }
                                                // if not found, it's an error
                                                if (nextToken.type == Token.PREPROCESSOR_DIRECTIVE)
                                                {
                                                    System.err.println(nextToken);
                                                    throw new Error(nextToken.inFileName+":"+(nextToken.inLineNumber+1)+":"+(nextToken.inColumnNumber+1)+": '#' is not followed by a macro parameter");
                                                }
                                            }
                                        }
                                    }

                                    //System.err.println("(CLONING TOKEN): "+nextToken);
                                    contentsVector.add(tokenAllocator.newRefedTokenCloned(nextToken));
                                    //System.err.println("(UNREFING ORIGINAL TOKEN): "+nextToken);
                                    tokenAllocator.unrefToken(nextToken);
                                    nextToken = tokenStream.readToken(inComment); // XXX WITHOUT macro substitution, so that macros get expanded lazily
                                }
                                Token contents[] = new Token[contentsVector.size()];
                                for (int i = 0; i < contents.length; ++i)
                                    contents[i] = (Token)contentsVector.get(i);
                                {
                                    // in place, compress all consecutive comments and spaces
                                    // into a single space, and
                                    // discard spaces and comments at the beginning.
                                    int nOut = 0;
                                    for (int iIn = 0; iIn < contents.length; ++iIn)
                                    {
                                        Token tokenIn = contents[iIn];
                                        contents[iIn] = null;
                                        if (tokenIn.type == Token.SPACES
                                         || tokenIn.type == Token.COMMENT)
                                        {
                                            if (nOut != 0
                                             && contents[nOut-1].type != Token.SPACES
                                             && !contents[nOut-1].textEquals("##")) // spaces after '##' disappear
                                            {
                                                CHECK(contents[nOut] == null);
                                                contents[nOut++] = tokenAllocator.newRefedToken(Token.SPACES, new char[]{' '}, 0,1, tokenIn.inFileName, tokenIn.inLineNumber, tokenIn.inColumnNumber);
                                            }
                                        }
                                        else
                                        {
                                            if (tokenIn.textEquals("##")
                                             && nOut > 0
                                             && contents[nOut-1].type == Token.SPACES)
                                            {
                                                tokenAllocator.unrefToken(contents[nOut-1]);
                                                contents[nOut-1] = null;
                                                nOut--; // spaces before '##' disappear
                                            }

                                            CHECK(contents[nOut] == null);
                                            contents[nOut++] = tokenAllocator.refToken(tokenIn);
                                        }
                                        tokenAllocator.unrefToken(tokenIn);
                                    }
                                    // and discard spaces and comments at the end too
                                    if (nOut > 0
                                     && contents[nOut-1].type == Token.SPACES)
                                    {
                                        tokenAllocator.unrefToken(contents[nOut-1]);
                                        contents[nOut-1] = null;
                                        nOut--;
                                    }
                                    CHECK(!(nOut > 0
                                                && contents[nOut-1].type == Token.SPACES));

                                    if (nOut != contents.length)
                                    {
                                        Token smallerContents[] = new Token[nOut];
                                        for (int i = 0; i < nOut; ++i)
                                            smallerContents[i] = contents[i];
                                        contents = smallerContents;
                                    }
                                }
                                Macro macro = new Macro(paramNames==null ? -1 : paramNames.length,
                                                        contents,
                                                        token.inFileName,
                                                        token.inLineNumber,
                                                        token.inColumnNumber);
                                if (verboseLevel >= 2)
                                    System.err.println("        filter:     defining macro \""+macroName+"\": "+macro);
                                Macro previousMacro = (Macro)macros.get(macroName);
                                if (previousMacro != null)
                                {
                                    if (macroName == "__LINE__"
                                     || macroName == "__FILE__")
                                        throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": can't redefine \""+macroName+"\"");


                                    if (!macro.sameContents(previousMacro))
                                    {
                                        // note, cpp's notion of sameness is more strict than ours... for example, we consider #define FOO(a,b) a##b the same as #define FOO(x,y) x##y
                                        System.err.println(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": warning: \""+macroName+"\" redefined");
                                        System.err.println(previousMacro.inFileName+":"+(previousMacro.inLineNumber+1)+":"+(previousMacro.inColumnNumber+1)+": warning: this is the location of the previous definition");
                                        // XXX NOTE: cpp says they are at column 1... is that what I should do? maybe don't need to store column number for macro at all?
                                    }

                                    tokenAllocator.unrefTokensInMacro(previousMacro);
                                }

                                macros.put(macroName, macro);

                                /* TODO: this was in old version... do I want it?
                                if (nextToken.type == Token.EOF)
                                {
                                    in.pushBackToken(nextToken);
                                    continue;
                                }
                                */
                                CHECK(nextToken.type == Token.NEWLINE);
                                // don't bother outputting it, we'll output it lazily on next non-newline
                            }
                            else
                            {
                                // The others (#ifdef, #ifndef, #undef)
                                // just take the single macro name and nothing else.

                                // move past spaces between macro name and newline.
                                // It's okay to start a c-style comment here.
                                while (nextToken.type == Token.SPACES
                                    || nextToken.type == Token.COMMENT
                                    || nextToken.type == Token.COMMENT_START)
                                {
                                    if (nextToken.type == Token.COMMENT_START)
                                    {
                                        CHECK(inComment == false);
                                        // have to print it so we don't end up outputting the end without the start.
                                        // gcc just doesn't output such comments at all, but we aren't in a position to be able to imitate it.
                                        out.print(nextToken.textUnderlyingString, nextToken.i0, nextToken.i1);
                                        inComment = true;
                                        out.keepSynced = !inComment;
                                        // next token is guaranteed to be a NEWLINE
                                    }
                                    tokenAllocator.unrefToken(nextToken);
                                    nextToken = tokenStream.readToken(inComment);
                                }

                                if (nextToken.type != Token.NEWLINE)
                                {
                                    // in cpp this is just a warning; we make it an error
                                    throw new Error(nextToken.inFileName+":"+(nextToken.inLineNumber+1)+":"+(nextToken.inColumnNumber+1)+": extra tokens at end of "+token.textToString()+" directive");
                                }

                                if (token.textEquals("undef")) // #undef
                                {
                                    CHECK(!inFalseIf); // we checked above
                                    if (macroName == "__LINE__"
                                     || macroName == "__FILE__")
                                        throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": can't undefine \""+macroName+"\""); // gcc just gives a warning
                                    Macro macro = (Macro)macros.get(macroName);
                                    if (macro != null)
                                        tokenAllocator.unrefTokensInMacro(macro);
                                    macros.remove(macroName);
                                }
                                else // #ifdef or #ifndef
                                {
                                    ifStack.pushAndKeepRef(tokenAllocator.newRefedTokenCloned(token));
                                    if (!inFalseIf)
                                    {
                                        boolean defined = (macros.get(macroName) != null);
                                        boolean answer = (defined == token.textEquals("ifdef"));

                                        if (answer == true)
                                            highestTrueIfStackLevel = ifStack.size(); // set to true as we push
                                        else
                                            highestTrueIfStackLevel = ifStack.size()-1; // change from true to false as we push
                                    }
                                }
                            } // #ifdef,#ifndef,#undef
                        }


                        // ones that take an integer expression
                        else if (token.textEquals("if")    // #if
                              || token.textEquals("elif")) // #elif
                        {
                            Token expressionStartToken = tokenAllocator.refToken(nextToken);
                            // gather rest of line (with macro substitution and defined() evaluation)
                            // into a string...
                            StringBuffer sb = new StringBuffer();
                            while (nextToken.type != Token.NEWLINE)
                            {
                                if (nextToken.type == Token.COMMENT_START)
                                {
                                    CHECK(inComment == false);
                                    // have to print it so we don't end up outputting the end without the start.
                                    // gcc just doesn't output such comments at all, but we aren't in a position to be able to imitate it since we don't have much coherency between lines.
                                    out.print(nextToken.textUnderlyingString, nextToken.i0, nextToken.i1);
                                    CHECK(!inComment); // we can't get PREPROCESSOR_DIRECTIVE tokens when in comment
                                    inComment = true;
                                    out.keepSynced = !inComment;
                                    // next token is guaranteed to be a NEWLINE

                                    throw new Error("BUG: not handling COMMENT_BEGIN after expression right (should emit it iff the #if is going to be true)"); // XXX same bug for other #ifs?  yeah I think so maybe
                                }
                                else if (nextToken.type == Token.COMMENT)
                                    sb.append(" ");
                                else if (nextToken.type == Token.IDENTIFIER)
                                {
                                    if (nextToken.textEquals("defined"))
                                    {
                                        // must be followed by an identifier or exactly the following:
                                        // '(', identifier, ')'.
                                        tokenAllocator.unrefToken(nextToken);
                                        nextToken = tokenStream.readToken(inComment); // NOT expanding macros
                                        while (nextToken.type == Token.SPACES
                                            || nextToken.type == Token.COMMENT)
                                        {
                                            tokenAllocator.unrefToken(nextToken);
                                            nextToken = tokenStream.readToken(inComment); // NOT expanding macros
                                        }

                                        String macroName;
                                        if (nextToken.type == Token.SYMBOL
                                         && nextToken.textEquals("("))
                                        {
                                            tokenAllocator.unrefToken(nextToken);
                                            nextToken = tokenStream.readToken(inComment); // NOT expanding macros
                                            if (nextToken.type != Token.IDENTIFIER)
                                                throw new Error(nextToken.inFileName+":"+(nextToken.inLineNumber+1)+":"+(nextToken.inColumnNumber+1)+": operator \"defined\" requires an identifier");
                                            macroName = nextToken.textToString();
                                            tokenAllocator.unrefToken(nextToken);
                                            nextToken = tokenStream.readToken(inComment); // NOT expanding macros
                                            if (nextToken.type != Token.SYMBOL
                                             || !nextToken.textEquals(")"))
                                                throw new Error(nextToken.inFileName+":"+(nextToken.inLineNumber+1)+":"+(nextToken.inColumnNumber+1)+": missing ')' after \"defined\"");
                                        }
                                        else if (nextToken.type == Token.IDENTIFIER)
                                            macroName = nextToken.textToString();
                                        else
                                            throw new Error(nextToken.inFileName+":"+(nextToken.inLineNumber+1)+":"+(nextToken.inColumnNumber+1)+": operator \"defined\" requires an identifier");

                                        if (macros.get(macroName) != null)
                                            sb.append(" 1 ");
                                        else
                                            sb.append(" 0 ");
                                    }
                                    else
                                    {
                                        // remaining identifiers
                                        // evaluate to 0
                                        // XXX or recursivemacro expansion 
                                        // XXX got stopped? not clear what
                                        // XXX the semantics are in that case
                                        sb.append(" 0 ");
                                    }
                                }
                                else
                                    sb.append(nextToken.textUnderlyingString,
                                              nextToken.i0,
                                              nextToken.i1-nextToken.i0);
                                tokenAllocator.unrefToken(nextToken);
                                nextToken = tokenStream.readToken(inComment); // XXX need to do it with macro substitution and defined() evaluation

                            }


                            if (token.textEquals("elif")) // #elif
                            {
                                // Simulate #elif by doing the #else thing
                                // followed by the #if thing.

                                //
                                // do the #else thing... XXX dup code
                                //
                                if (ifStack.top().textEquals("else"))
                                {
                                    // find the original #if token
                                    Token originalIfToken = ifStack.popAndKeepRef();
                                    while (!originalIfToken.textEquals("if"))
                                    {
                                        tokenAllocator.unrefToken(originalIfToken);
                                        originalIfToken = ifStack.popAndKeepRef();
                                    }
                                    throw new Error(
                                        token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": #"+token.textToString()+" after #else"
                                      + "\n" // XXX \r\n on windows?
                                      + originalIfToken.inFileName+":"+(originalIfToken.inLineNumber+1)+":"+(originalIfToken.inColumnNumber+1)+": the conditional began here");
                                }
                                // Only consider changing state
                                // if parent state was true...
                                if (highestTrueIfStackLevel >= ifStack.size()-1)
                                {
                                    if (highestTrueIfStackLevel >= ifStack.size()) // was true
                                        highestTrueIfStackLevel = ifStack.size(); // change from true to false as we push
                                    else // was false
                                        highestTrueIfStackLevel = ifStack.size()+1; // change from false to true as we push

                                }
                                ifStack.pushAndKeepRef(tokenAllocator.newRefedTokenCloned(token));
                                // do the #if thing,
                                // which is to push the token again.
                                // it's the same token we just pushed,
                                // and the char buffer is considered immutable,
                                // so theoretically we could just push a ref to it
                                // instead of cloning another one,
                                // however in the current implementation,
                                // a given token can only appear once
                                // on any TokenStack, so we really do need to
                                // clone another one.
                                // However, we clone the clone instead of the
                                // original, since that is cheaper
                                // (the allocator will notice it's a clone
                                // of a non-LineBuffer-owned token,
                                // and it will share the internal char buffer).
                                //
                                ifStack.pushAndKeepRef(tokenAllocator.newRefedTokenCloned(ifStack.top()));
                            }
                            else
                            {
                                // do the #if thing...
                                ifStack.pushAndKeepRef(tokenAllocator.newRefedTokenCloned(token));
                            }

                            // we need to evaluate the expression
                            // iff, before the #if was pushed,
                            // we were in a true.
                            boolean needToEvaluate = highestTrueIfStackLevel >= ifStack.size()-1;
                            if (needToEvaluate)
                            {
                                if (sb.length() == 0)
                                    throw new Error(expressionStartToken.inFileName+":"+(expressionStartToken.inLineNumber+1)+":"+(expressionStartToken.inColumnNumber+1)+": #"+token.textToString()+" with no expression"); // note, cpp says #if even if it was #elif; we do better

                                int expressionValue = 0;

                                try
                                {
                                    expressionValue = expressionParser.evaluateIntExpression(sb.toString());
                                }
                                catch (Exception e)
                                {
                                    // ad-hoc error message, different from what gcc emits
                                    throw new Error(expressionStartToken.inFileName+":"+(expressionStartToken.inLineNumber+1)+":"+(expressionStartToken.inColumnNumber+1)+": "+e.getMessage()+" in "+token.textToString()+", expression was \""+escapify(sb.toString())+"\"");
                                }
                                boolean answer = (expressionValue != 0);
                                highestTrueIfStackLevel = (answer ? ifStack.size()
                                                                  : ifStack.size()-1);
                            }
                            tokenAllocator.unrefToken(expressionStartToken);
                        }

                        else if (token.textEquals("include")) // #include
                        {
                            CHECK(!inFalseIf); // we checked above
                            /*
                            get the file name and stuff, including end of line
                            output any newlines and/or line directives needed to get in sync
                            recurse
                            output any newlines and/or line directives needed to get in sync
                            */
                            System.err.println(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": warning: #include unimplemented");
                            while (nextToken.type != Token.NEWLINE)
                            {
                                if (nextToken.type == Token.COMMENT_START)
                                {
                                    CHECK(inComment == false);
                                    // have to print it so we don't end up outputting the end without the start.
                                    // gcc just doesn't output such comments at all, but we aren't in a position to be able to imitate it since we don't have much coherency between lines.
                                    out.print(nextToken.textUnderlyingString, nextToken.i0, nextToken.i1);
                                    CHECK(!inComment); // we can't get PREPROCESSOR_DIRECTIVE tokens when in comment
                                    inComment = true;
                                    out.keepSynced = !inComment;
                                    // next token is guaranteed to be a NEWLINE
                                }
                                tokenAllocator.unrefToken(nextToken);
                                nextToken = tokenStream.readToken(inComment); // XXX need to do it with macro substitution and defined() evaluation
                            }
                        }


                        else if (token.textStartsWithDigit())
                        {
                            int theInt = token.textToNonNegativeInt();
                            if (theInt == -1)
                                throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": \""+token.textToString()+"\" after # is not a positive integer"); // really should say "not a non-negative integer" but we imitate cpp's misnomer
                            //set the input file and line number 
                            CHECK(false); // IMPLEMENT ME
                        }
                        else if (token.textIsEmpty())
                        {
                            // nothing!
                            CHECK(nextToken.type == Token.NEWLINE);
                        }
                        else
                        {
                            throw new Error(token.inFileName+":"+(token.inLineNumber+1)+":"+(token.inColumnNumber+1)+": invalid preprocessor directive #"+token.textToString());
                        }
                        // in all of the above cases,
                        // we processed the whole line til NEWLINE
                        CHECK(nextToken.type == Token.NEWLINE);
                        tokenAllocator.unrefToken(nextToken);
                        nextToken = null;
                        out.println(); // XXX why is this necessary?
                    }
                    else
                    {
                        // in false #if, and it's not a control-flow directive.
                        // discard tokens til end of line,
                        // and don't print a newline.
                        // okay to start a c-style comment here.
                        // XXX TODO: do we need to do this at all? nothing gets output during false #if's anyway
                        Token nextToken = tokenStream.readToken(inComment);
                        while (nextToken.type != Token.NEWLINE)
                        {
                            if (nextToken.type == Token.COMMENT_START)
                            {
                                // have to print it so we don't end up outputting the end without the start.
                                // gcc just doesn't output such comments at all, but we aren't in a position to be able to imitate it since we don't have much coherency between lines.
                                out.print(nextToken.textUnderlyingString, nextToken.i0, nextToken.i1);
                                CHECK(!inComment); // we can't get PREPROCESSOR_DIRECTIVE tokens when in comment
                                inComment = true;
                                out.keepSynced = !inComment;
                                // next token is guaranteed to be a NEWLINE
                            }
                            tokenAllocator.unrefToken(nextToken);
                            nextToken = tokenStream.readToken(inComment);
                        }
                        tokenAllocator.unrefToken(nextToken);
                        nextToken = null;
                        out.println(); // XXX why is this necessary?
                    }
                } // active preprocessor directive

                else
                {
                    // if not in a false #if...
                    if (ifStack.size() <= highestTrueIfStackLevel)
                    {
                        // Actually output something.
                        if (inComment)
                        {
                            out.print(token.textUnderlyingString, token.i0, token.i1);
                        }
                        else
                        {
                            // First make sure output line number is synced up...
                            // (if NOT in comment... line directives in comments
                            // would get ignored!)
                            if (out.columnNumber == 0)
                            {
                                if (!out.softSyncToInLineNumber(token.inLineNumber))
                                    out.hardSyncToInLineNumber(token.inLineNumber,
                                                               token.inFileName,
                                                               commentOutLineDirectives,
                                                               in.extraCrap);
                                CHECK(out.inLineNumber == token.inLineNumber);
                                CHECK(out.outLineNumberDelivered == out.outLineNumberPromised);
                            }
                            if (outputDebugLevel >= DEBUG_PER_TOKEN)
                                System.err.println("        (passing through token)");
                            out.print(token.textUnderlyingString, token.i0, token.i1);
                        }
                    }
                    else
                    {
                        if (outputDebugLevel >= DEBUG_PER_TOKEN)
                            System.err.println("        (suppressing token because in false #if)");
                    }
                }

                // XXX could actually assert that COMMENT_START only happens when not in comment, and COMMENT_END only happens when in comment
                if (token.type == (inComment ? Token.COMMENT_END
                                             : Token.COMMENT_START))
                {
                    inComment = !inComment;
                    out.keepSynced = !inComment;
                }


                tokenAllocator.unrefToken(token);
                token = null;
            }


            // Done with the tokens on this line,
            // including the newline.
        }

        CHECK(tokenStream.isEmpty());

        if (!ifStack.isEmpty())
        {
            // don't bother unrefing, we'll just leak refs, for these and any others on stack
            Token current = ifStack.popAndKeepRef();
            Token original = current;
            while (!original.textStartsWith("if"))
                original = ifStack.popAndKeepRef();
            throw new Error(original.inFileName+":"+(original.inLineNumber+1)+":1: unterminated #"+current.textToString());
        }


        if (inputDebugLevel >= DEBUG_PER_FILE)
            System.err.println("    out Cpp.filter(\""+escapify(in.fileName)+"\")");
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

    private static String millisToSecsString(long millis)
    {
        String answer = "";
        if (millis < 0)
        {
            answer += "-";
            millis = -millis;
        }
        answer += millis/1000
                + "."
                + millis / 100 % 10
                + millis / 10 % 10
                + millis % 10;
        return answer;
    }

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


        // For some reason the real cpp does this at the beginning
        // before the built-ins and command line... so we do it too
        writer.hardSyncToInLineNumber(0, inFileName, parsedArgs.commentOutLineDirectives, "");

        try
        {
            String builtinInput = "#define __LINE__ __LINE__\n" // stub, handled specially
                                + "#define __FILE__ __FILE__\n"; // stub, handled specially
            String language = parsedArgs.language;
            if (language.equals("java"))
            {
                builtinInput += "#define __java 1\n";
            }
            else if (language.equals("c")
                  || language.equals("c++"))
            {
                if (language.equals("c++"))
                {
                    // cpp -x c++ -dM /dev/null
                    // and remove what's in the c output
                    builtinInput += ""
                        + "#define __GXX_WEAK__ 1\n"
                        + "#define __cplusplus 1\n"
                        + "#define __DEPRECATED 1\n"
                        + "#define __GNUG__ 3\n"
                        + "#define __EXCEPTIONS 1\n"
                        + "#define _GNU_SOURCE 1\n"
                        ;
                }
                // cpp -x c -dM /dev/null
                builtinInput += "#define __STDC__ 1\n"; // why the heck is this not in the output of cpp -dM??? it's definitely defined
                builtinInput += ""
                    + "#define __DBL_MIN_EXP__ (-1021)\n"
                    + "#define __FLT_MIN__ 1.17549435e-38F\n"
                    + "#define __CHAR_BIT__ 8\n"
                    + "#define __WCHAR_MAX__ 2147483647\n"
                    + "#define __DBL_DENORM_MIN__ 4.9406564584124654e-324\n"
                    + "#define __FLT_EVAL_METHOD__ 2\n"
                    + "#define __DBL_MIN_10_EXP__ (-307)\n"
                    + "#define __FINITE_MATH_ONLY__ 0\n"
                    + "#define __GNUC_PATCHLEVEL__ 6\n"
                    + "#define __SHRT_MAX__ 32767\n"
                    + "#define __LDBL_MAX__ 1.18973149535723176502e+4932L\n"
                    + "#define __linux 1\n"
                    + "#define __unix 1\n"
                    + "#define __LDBL_MAX_EXP__ 16384\n"
                    + "#define __linux__ 1\n"
                    + "#define __SCHAR_MAX__ 127\n"
                    + "#define __USER_LABEL_PREFIX__ \n"
                    + "#define __STDC_HOSTED__ 1\n"
                    + "#define __LDBL_HAS_INFINITY__ 1\n"
                    + "#define __DBL_DIG__ 15\n"
                    + "#define __FLT_EPSILON__ 1.19209290e-7F\n"
                    + "#define __LDBL_MIN__ 3.36210314311209350626e-4932L\n"
                    + "#define __unix__ 1\n"
                    + "#define __DECIMAL_DIG__ 21\n"
                    + "#define __gnu_linux__ 1\n"
                    + "#define __LDBL_HAS_QUIET_NAN__ 1\n"
                    + "#define __GNUC__ 3\n"
                    + "#define __DBL_MAX__ 1.7976931348623157e+308\n"
                    + "#define __DBL_HAS_INFINITY__ 1\n"
                    + "#define __DBL_MAX_EXP__ 1024\n"
                    + "#define __LONG_LONG_MAX__ 9223372036854775807LL\n"
                    + "#define __GXX_ABI_VERSION 1002\n"
                    + "#define __FLT_MIN_EXP__ (-125)\n"
                    + "#define __DBL_MIN__ 2.2250738585072014e-308\n"
                    + "#define __DBL_HAS_QUIET_NAN__ 1\n"
                    + "#define __tune_i386__ 1\n"
                    + "#define __REGISTER_PREFIX__ \n"
                    + "#define __NO_INLINE__ 1\n"
                    + "#define __i386 1\n"
                    + "#define __FLT_MANT_DIG__ 24\n"
                    + "#define __VERSION__ \"3.4.6 20060404 (Red Hat 3.4.6-9)\"\n"
                    + "#define i386 1\n"
                    + "#define unix 1\n"
                    + "#define __i386__ 1\n"
                    + "#define __SIZE_TYPE__ unsigned int\n"
                    + "#define __ELF__ 1\n"
                    + "#define __FLT_RADIX__ 2\n"
                    + "#define __LDBL_EPSILON__ 1.08420217248550443401e-19L\n"
                    + "#define __GNUC_RH_RELEASE__ 9\n"
                    + "#define __FLT_HAS_QUIET_NAN__ 1\n"
                    + "#define __FLT_MAX_10_EXP__ 38\n"
                    + "#define __LONG_MAX__ 2147483647L\n"
                    + "#define __FLT_HAS_INFINITY__ 1\n"
                    + "#define linux 1\n"
                    + "#define __LDBL_MANT_DIG__ 64\n"
                    + "#define __WCHAR_TYPE__ long int\n"
                    + "#define __FLT_DIG__ 6\n"
                    + "#define __INT_MAX__ 2147483647\n"
                    + "#define __FLT_MAX_EXP__ 128\n"
                    + "#define __DBL_MANT_DIG__ 53\n"
                    + "#define __WINT_TYPE__ unsigned int\n"
                    + "#define __LDBL_MIN_EXP__ (-16381)\n"
                    + "#define __LDBL_MAX_10_EXP__ 4932\n"
                    + "#define __DBL_EPSILON__ 2.2204460492503131e-16\n"
                    + "#define __FLT_DENORM_MIN__ 1.40129846e-45F\n"
                    + "#define __FLT_MAX__ 3.40282347e+38F\n"
                    + "#define __FLT_MIN_10_EXP__ (-37)\n"
                    + "#define __GNUC_MINOR__ 4\n"
                    + "#define __DBL_MAX_10_EXP__ 308\n"
                    + "#define __LDBL_DENORM_MIN__ 3.64519953188247460253e-4951L\n"
                    + "#define __PTRDIFF_TYPE__ int\n"
                    + "#define __LDBL_MIN_10_EXP__ (-4931)\n"
                    + "#define __LDBL_DIG__ 18\n"
                    ;
            }
            else 
            {
                System.err.println("language "+language+" not recognized");
                System.exit(1);
            }

            java.io.Reader builtinFakeInputReader = new java.io.StringReader(builtinInput);
            filter(new LineAndColumnNumberReader(builtinFakeInputReader, "<built-in>"),
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
        catch (java.io.IOException e)
        {
            writer.flush();
            System.err.println("Well damn: "+e);
            System.exit(1);
        }


        try
        {
            if (inputDebugLevel >= DEBUG_PER_LINE)
                System.err.println("command line fake input = \""+escapify(parsedArgs.commandLineFakeInput)+"\"");
            java.io.Reader commandLineFakeInputReader = new java.io.StringReader(parsedArgs.commandLineFakeInput);
            filter(new LineAndColumnNumberReader(commandLineFakeInputReader, "<command line>"),
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

        //
        // Destroy the macros, freeing any tokens therein.
        //
        if (inputDebugLevel >= DEBUG_OVERALL)
            System.err.println("    freeing macros");
        for (java.util.Enumeration e = macros.keys(); e.hasMoreElements(); )
        {
            String name = (String)e.nextElement();
            Macro macro = (Macro)macros.get(name); // XXX TODO: weird, is it not possible to iterate through the name/value pairs without calling the hash function?
            // XXX maybe need macroDebugLevel
            //System.err.println("        "+name+" : "+macro+"");
            tokenAllocator.unrefTokensInMacro(macro);
        }


        if (inputDebugLevel >= DEBUG_OVERALL)
        {
            long t1Millis = System.currentTimeMillis();

            System.err.println("    "+lineBufferScratch.nTokensReferringToMe+" tokens referring to lineBufferScratch");
            System.err.println("    "+tokenAllocator.nInUse+" tokens still in use");
            System.err.println("    "+tokenAllocator.nFree+" tokens in free list");
            System.err.println("    "+tokenAllocator.nPhysicalAllocations+" physical token allocations");
            System.err.println("    "+tokenAllocator.nLogicalAllocations+" logical token allocations");
            System.err.println("    "+tokenAllocator.nPrivateBuffersAllocated+" private char buffers allocated");
            System.err.println("    line buffer max capacity = "+lineBufferScratch.chars.length);

            System.err.println("    "+millisToSecsString(t1Millis-t0Millis)+" seconds");
        }

        CHECK(lineBufferScratch.nTokensReferringToMe == 0);
        CHECK(tokenAllocator.nInUse == 0);

        if (inputDebugLevel >= DEBUG_OVERALL)
            System.err.println("out Cpp.main");
        System.exit(0);
    } // main

} // public class Cpp








