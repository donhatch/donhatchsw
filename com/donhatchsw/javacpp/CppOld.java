/**
*
* WARNING: WORK IN PROGRESS!
*
* This class implements cpp (the C preprocessor),
* actually a subset of its functionality.
*
* Understands the following directives:
*       #include "filename"  (not <filename>)
*       #define
*       #undef
*       #ifdef
*       #ifndef
*       #if     (evaluates C integer expressions, including defined())
*       #elif   (evaluates C integer expressions, including defined())
*       #else
*       #endif
* and the following command-line options:
*       -I
*       -D
*       -U
*       -C  (ignores this option, never strips comments anyway)
*       -x [ java | c++ | c ]  (default language is java)
*
* To imitate the builtin include path and defines from
* cpp -C from gcc version 3.4.6 on redhat 3.4.6-9,
* run it with -x c or -x c++.
* When using the default language (-x java),
* line number directives will be commented out in the output.
*

TODO:
    - test0 and test1 is broken (string file opener I guess)
    - handle escaped newlines like cpp does -- really as nothing, i.e. can be in the middle of a token or string-- it omits it.  also need to emit proper number of newlines to sync up
    - #include_next, sigh
    - make sure line numbers in sync in all cases
    - understand # line numbers and file number on input (masquerade)

    - secret command line option for dumping the test files
    - test that every file in /usr/include gives the same output as cpp -C -D...
        -   cpp -C /usr/include/math.h
        -   cpp -C < /usr/include/math.h
        -   (cd /usr/include; cpp -C math.h)
        -   (cd /usr/include; cpp -C < math.h)
        -   (cd /usr; cpp -C include/math.h)
        -   (cd /usr; cpp -C < include/math.h)
        - all of the above without c++ defines/includes and then with
        - all of the above twice:
            cpp -C /usr/include/math.h | cpp -C
            cpp -C < /usr/include/math.h | cpp -C
            etc., also doing it from a temporary file
    - ##  (concatenates tokens)
    - make #include "filename" look in same directory as current file (I think it's implemented but logic might not be right, it uses File.getParent which is probably retarded)
    - understand <> around file names as well as ""'s -- needed for comparing against cpp on include files in /usr/include which will be the ultimate test I guess
    - hmm, if test output is a bit different... OH it discards spaces at the end of each line!  argh!!
    - put "In file included from whatever:3:" or whatever in warnings and errors
    - named operators?  ARGH! http://gcc.gnu.org/onlinedocs/cpp/C_002b_002b-Named-Operators.html   I think I'll blow this off.  wait, the real cpp doesn't even do it?? "#if 1 and 1" gives same syntax error as "#if 1 andddd 1".  ohh it only happens if processing c++ code I think?  whatever.
    - variadic macros?  ARGH, no don't bother
    - hmm, cpp doc says macro args are macro-expanded before they
      get subtituted in, that might be simpler than what I did
    - #pragma?  e.g. #pragma weak, in /usr/include/bits/libc-lock.h

*/

package com.donhatchsw.javacpp;

public class Cpp
{
    // Logical assertions, always compiled in. Ungracefully bail if violated.
    private static void AssertAlways(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

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


    // Line-and-column number reader with lookahead.
    // Pushback is NOT supported since it would be impossible to determine column number.
    // Only implemented the methods I need.
    // Only 1 character of lookahead is available externally.
    // 1 character of lookahead would be sufficient EXCEPT for the pesky fact that we have to
    // look ahead an arbitrary number of spaces to determine whether we're looking at an escaped newline,
    // oh well.
    // TODO: I think discarding of escaped newlines should be on this level? turned out to be a surprisingly hairy problem.
    private static class LineAndColumnNumberReaderWithLookahead
    {
        private java.io.LineNumberReader newlineSimplifyingReader; // note we do NOT use the line number feature of this guy, we only use the fact that it compresses each line terminator into a single '\n' during read.
        private int lineNumber = 0;
        private int columnNumber = 0;

        private int nLookedAhead = 0;
        private int lookaheadBuffer[] = new int[0]; // expands as needed

        // To avoid dismal performance, caller should make sure
        // that reader is either a BufferedReader or has one as an ancestor.
        public LineAndColumnNumberReaderWithLookahead(java.io.Reader reader)
        {
            this.newlineSimplifyingReader = new java.io.LineNumberReader(reader);
            AssertAlways(this.newlineSimplifyingReader.getLineNumber() == 0);
        }

        public int read()
            throws java.io.IOException // since newlineSimplifyingReader.read() does
        {
            //System.out.println("            with lookahead before read(): line "+lineNumber);
            //System.out.println("                ("+(nLookedAhead>=1?"":"not ")+"looking ahead)");
            int c;
            if (nLookedAhead >= 1)
            {
                c = lookaheadBuffer[0];
                for (int i = 0; i < nLookedAhead-1; ++i)
                    lookaheadBuffer[i] = lookaheadBuffer[i+1];
                --nLookedAhead;
            }
            else
            {
                c = newlineSimplifyingReader.read();
            }
            if (c != -1)
            {
                if (c == '\n')
                {
                    lineNumber++;
                    columnNumber = 0;
                }
                else
                    columnNumber++;
            }
            //System.err.println("            with lookahead read() returning '"+escapify((char)c,'\'')+"'");
            //System.err.println("            with lookahead after read(): line "+lineNumber);
            return c;
        } // private read


        // Return what read() will return next.
        public int peek()
            throws java.io.IOException
        {
            int index = 0;
            while (nLookedAhead <= index)
            {
                if (nLookedAhead == lookaheadBuffer.length)
                {
                    // grow buffer
                    int newLookaheadBuffer[] = new int[lookaheadBuffer.length*2+1]; // 0 1 3 7 15 31 63 ...
                    for (int i = 0; i < nLookedAhead; ++i)
                        newLookaheadBuffer[i] = lookaheadBuffer[i];
                    lookaheadBuffer = newLookaheadBuffer;
                }
                if (nLookedAhead >= 1)
                    AssertAlways(lookaheadBuffer[nLookedAhead-1] != -1); // no peeking past eof!
                lookaheadBuffer[nLookedAhead++] = newlineSimplifyingReader.read();
                // peeking doesn't do anything to lineNumber and columnNumber
            }
            return lookaheadBuffer[index];
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
            this.columnNumber = 0;
        }
    } // private static class LineAndColumnNumberReaderWithLookahead

    private static class Token
    {
        // The token types
        public static final int IDENTIFIER = 0;     // "_[a-zA-Z0-9][a-zA-Z0-9]*"
        public static final int STRING_LITERAL = 1; // "\"([^\\"]|\\.)*\""
        public static final int CHAR_LITERAL = 2;   // "'([^\\']|\\.)*'" // too leniant
        public static final int INT_LITERAL = 3;    // "-?([0-9]+|0x[0-9a-fA-F]*) // and not followed by x, is there lookahead?
        public static final int FLOAT_LITERAL = 4;
        public static final int DOUBLE_LITERAL = 5;
        public static final int SYMBOL = 6;
        public static final int COMMENT = 7;
        public static final int SPACES = 8;
        public static final int NEWLINE_UNESCAPED = 9; // XXX TODO: not handling these right, these should NOT turn into spaces, cpp uses them as nothing (argh, which ends up putting multiple stuff on a line!)
        public static final int PREPROCESSOR_DIRECTIVE = 10;
        public static final int MACRO_ARG = 11;
        public static final int MACRO_ARG_QUOTED = 12;
        public static final int TOKEN_PASTE = 13; // temporary form that "##" takes during macro evaluation... NOT during initial tokenizing
        public static final int EOF = 14;
        public static final int NUMTYPES = 15; // one more than last value
        // TODO: long
        // TODO: absorb backslash-newline into spaces

        public int type;
        public String text;
        public String fileName;
        public int lineNumber; // 0 based
        public int columnNumber; // 0 based
        public Token parentInMacroInvocation = null; // used when expanding macros to stop recursion

        public Token(int type, String text, String fileName, int lineNumber, int columnNumber)
        {
            this.type = type;
            this.text = text;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }
        // copy constructor but changing file name and line number XXX shouldn't we be changing column number too if we do this?  or is this just so __LINE__ and __FILE__ will come out right?
        public Token(Token fromToken, String fileName, int lineNumber)
        {
            this(fromToken.type,
                 fromToken.text,
                 fileName,
                 lineNumber,
                 fromToken.columnNumber);
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
                  +escapify(this.text)
                  +"\", "
                  +"                         "
                  +", \""
                  +escapify(this.fileName)
                  +"\", "
                  +this.lineNumber
                  +", "
                  +this.columnNumber
                  +")";
        }
    } // private static class Token

    private static class Macro
    {
        // Doesn't include the name
        public int numParams; // -1 means no parens even
        public Token[] contents; // args denoted by token type MACRO_ARG with line number containing the index of the argument to be substituted
        public String inFileName;
        public int lineNumber;
        public int columnNumber;
        public Macro(int numParams, Token[] contents,
                     String inFileName, int lineNumber, int columnNumber)
        {
            this.numParams = numParams;
            this.contents = contents;
            this.inFileName = inFileName;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
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
                    if (!contents[i].text.equals(other.contents[i].text))
                        return false;
                }
            }
            return true;
        }
    } // private static class Macro


    /**
    * This class turns a Reader into a reader
    * that reads a token at a time.
    * It's guaranteed to return an EOF token at the end,
    * and if the caller reads past that, it's an error.
    */
    private static class TokenReader
    {
        private LineAndColumnNumberReaderWithLookahead reader;
        String fileName;
        private boolean returnedEOF;
        private StringBuffer scratch = new StringBuffer();

        public TokenReader(java.io.Reader in, String fileName)
        {
            this.reader = new LineAndColumnNumberReaderWithLookahead(in);
            this.fileName = fileName;
            this.returnedEOF = false;
        }
        public Token readToken()
            throws java.io.IOException // since reader.read() does
        {
            if (returnedEOF)
                throw new Error("TokenReader asked to read past EOF");

            // clear scratch...
            scratch.delete(0, scratch.length());

            // grab the (0-based) line number and column number at the beginning of the token
            int lineNumber = reader.getLineNumber();
            int columnNumber = reader.getColumnNumber();

            int c = reader.read();

            while (c == '\\')
            {
                // escaped newlines just disappear,
                // and it's okay to swallow any intervening spaces too.
                // TODO: really this should be done in the stream we're reading from, so that, for example, an escaped newline can appear right in the middle of an identifier
                int d;
                boolean gotSpace = false;
                while ((d = reader.peek()) != -1
                    && d != '\n'
                    && Character.isWhitespace((char)d))
                {
                    gotSpace = true;
                    reader.read(); // discard the space
                }
                if (d == '\n')
                {
                    reader.read(); // discard the newline
                    if (gotSpace)
                        System.err.println(fileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": warning: backslash and newline separated by space");
                    c = reader.read();
                }
                else
                {
                    // TODO: argh! lost the space!  I think this actually requires two characters of lookahead, argh.  but we can fake it by putting a space in the token we return.
                    if (gotSpace)
                        System.err.println("ARGH!! handling backslash-space lamely");
                    return new Token(Token.SYMBOL, "\\"+(gotSpace?" ":""), fileName, lineNumber, columnNumber);
                }
            }

            Token token;
            if (c == -1)
            {
                returnedEOF = true;
                token = new Token(Token.EOF, "", fileName, lineNumber, columnNumber);
            }
            // TODO: order these in order of likelihood?
            else if (c == '\n')
            {
                scratch.append((char)c);
                token = new Token(Token.NEWLINE_UNESCAPED, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            else if (Character.isWhitespace((char)c))
            {
                scratch.append((char)c);
                int d; // TODO: do this for most of the reader.peek()s in here, for speed
                while ((d = reader.peek()) != -1
                    && d != '\n'
                    && Character.isWhitespace((char)d))
                    scratch.append((char)reader.read());
                token = new Token(Token.SPACES, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            else if (Character.isJavaIdentifierStart((char)c))
            {
                scratch.append((char)c);
                while (reader.peek() != -1
                    && Character.isJavaIdentifierPart((char)reader.peek()))
                    scratch.append((char)reader.read());
                token = new Token(Token.IDENTIFIER, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            else if (Character.isDigit((char)c)
                  || (c == '.' && reader.peek() != -1
                               && Character.isDigit((char)reader.peek()))
                  || (c == '-' && reader.peek() != -1
                               && (reader.peek() == '.'
                                || Character.isDigit((char)reader.peek()))))
            {
                // -?[0-9]*.[0-9]*([eE]-?[0-9]+)?
                // but one of the first two [0-9]*'s are required to be
                // non-empty.
                // So really it's
                //      -?([0-9][0-9]*.[0-9]*|.[0-9][0-9]*)([eE]-?[0-9]+)?

                // optional initial '-'
                if (c == '-')
                {
                    scratch.append((char)c);
                    c = reader.read();
                    AssertAlways(c != -1); // by guard above
                }

                if (Character.isDigit((char)c))
                {
                    scratch.append((char)c);
                    while (reader.peek() != -1
                        && Character.isDigit((char)reader.peek()))
                        scratch.append((char)reader.read());
                    if (reader.peek() == '.')
                    {
                        scratch.append((char)reader.read()); // the '.'
                        // eat up *optional* digits after the '.'
                        while (reader.peek() != -1
                            && Character.isDigit((char)reader.peek()))
                            scratch.append((char)reader.read());
                    }
                }
                else // c is '.'
                {
                    AssertAlways(c == '.'); // by guard above
                    scratch.append((char)c);
                    if (reader.peek() == -1)
                    {
                        // Really obscure-- can probably only happen
                        // if there's a "-." at the end of the file
                        throw new Error(); // XXX more detail
                    }
                    if (!Character.isDigit((char)reader.peek()))
                    {
                        // "-." followed by non-digit-- I think that's an illegal construct... really in this case we should return "-" then ".", but we got tricked into eating too much so there's no recovery
                        throw new Error(); // XXX more detail
                    }
                    scratch.append((char)reader.read()); // a digit after the '.'
                    // eat up optional more digits after the '.'
                    while (reader.peek() != -1
                        && Character.isDigit((char)reader.peek()))
                        scratch.append((char)reader.read());
                }

                // Optional [eE][+-]?[0-9]+
                if (reader.peek() == 'e'
                 || reader.peek() == 'E')
                {
                    scratch.append((char)reader.read()); // the 'e' or 'E'
                    // optional - or +
                    if (reader.peek() == '-'
                     || reader.peek() == '+')
                        scratch.append((char)reader.read()); // the '-' or '+'
                    // must be followed by at least one digit
                    if (reader.peek() == -1)
                        throw new Error(); // XXX more detail
                    if (!Character.isDigit((char)reader.peek()))
                        throw new Error(); // XXX more detail
                    scratch.append((char)reader.read()); // the digit
                    // optional more digits
                    while (reader.peek() != -1
                        && Character.isDigit((char)reader.peek()))
                        scratch.append((char)reader.read()); // the digit
                }

                // Optional f, which makes it a float for sure
                if (reader.peek() == 'f')
                {
                    scratch.append((char)reader.read()); // the 'f'
                    token = new Token(Token.FLOAT_LITERAL, scratch.toString(), fileName, lineNumber, columnNumber);
                }
                else
                {
                    // Okay, what have we got?
                    String string = scratch.toString();
                    if (string.indexOf('.') != -1
                     || string.indexOf('e') != -1
                     || string.indexOf('E') != -1)
                        token = new Token(Token.DOUBLE_LITERAL, string, fileName, lineNumber, columnNumber);
                    else
                        token = new Token(Token.INT_LITERAL, string, fileName, lineNumber, columnNumber);
                }
            }
            else if (c == '"' || c == '\'')
            {
                scratch.append((char)c);
                char quoteChar = (char)c;
                while (true)
                {
                    c = reader.read();
                    if (c == -1)
                        throw new Error("EOF in middle of string or char literal");
                    scratch.append((char)c);
                    if (c == '\\')
                    {
                        c = reader.read();
                        if (c == -1)
                            throw new Error("EOF in middle of string or char literal");
                        scratch.append((char)c); // the escaped char
                        // backslash can be followed by up to 3 digits,
                        // or various other things, but we don't have to worry
                        // about that, we handled the necessary case
                        // which is an escaped quote or escaped backslash
                    }
                    else if (c == quoteChar)
                        break;
                }
                token = new Token(quoteChar=='"' ? Token.STRING_LITERAL : Token.CHAR_LITERAL, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            else if (c == '/' && reader.peek() == '/')
            {
                scratch.append((char)c);             // the first '/'
                scratch.append((char)reader.read()); // the second '/'
                // Read rest of line, NOT including newline or EOF
                while (reader.peek() != -1
                    && reader.peek() != '\n')
                {
                    c = reader.read();
                    if (c == '\\')
                    {
                        // TODO: naive, in the case of spaces between the backslash and the newline... doesn't recognize that
                        if (reader.peek() == '\n')
                        {
                            reader.read(); // discard the newline
                            continue;
                        }
                    }
                    scratch.append((char)c);
                }
                token = new Token(Token.COMMENT, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            else if (c == '/' && reader.peek() == '*')
            {
                scratch.append((char)c);             // '/'
                scratch.append((char)reader.read()); // '*'
                // Read until "*/"
                while (true)
                {
                    c = reader.read();
                    if (c == -1)
                        throw new Error("EOF in middle of comment");

                    if (c == '\\')
                    {
                        // TODO: naive, in the case of spaces between the backslash and the newline... doesn't recognize that
                        if (reader.peek() == '\n')
                        {
                            reader.read(); // discard the newline
                            continue;
                        }
                    }

                    scratch.append((char)c);
                    if (c == '*' && reader.peek() == '/')
                    {
                        scratch.append((char)reader.read()); // the '/'
                        break;
                    }
                }
                token = new Token(Token.COMMENT, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            else if (c == '#')
            {
                // do this anywhere in a line, since it also is part of handling #arg in part of a macro definition
                if (reader.peek() == '#')
                {
                    reader.read();
                    token = new Token(Token.PREPROCESSOR_DIRECTIVE, "##", fileName, lineNumber, columnNumber); // not TOKEN_PASTE, that's only created temporarily during macro expansion
                }
                else
                {
                    scratch.append((char)c);
                    while (reader.peek() != -1
                        && Character.isWhitespace((char)reader.peek()))
                    {
                        // Discard spaces between the '#' and the rest
                        // of the directive; do NOT put them in the scratch
                        // buffer.  This will make it easier to identify
                        // the directive later.
                        reader.read();
                    }
                    // Picks up both identifiers and ints (line number directives),
                    // will also pick up something like #1foo but whatever.
                    // TODO maybe guard against that
                    while (reader.peek() != -1
                        && Character.isJavaIdentifierPart((char)reader.peek()))
                        scratch.append((char)reader.read());
                    token = new Token(Token.PREPROCESSOR_DIRECTIVE, scratch.toString(), fileName, lineNumber, columnNumber);
                }
            }
            else
            {
                scratch.append((char)c);
                token = new Token(Token.SYMBOL, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            //System.err.println("            TokenReader  returning "+token);
            return token;
        } // readToken
        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }
        public void setLineNumber(int lineNumber)
        {
            reader.setLineNumber(lineNumber);
        }
    } // private static class TokenReader

    // Guaranteed to return non-null,
    // but peeking past EOF is an error.
    private static class TokenReaderWithLookahead
    {
        private TokenReader tokenReader;
        public String inFileName;
        public String extraCrap; // gets put at the end of # line directives referring to this file, to imitate the real cpp
        private java.util.LinkedList lookaheadBuffer = new java.util.LinkedList();

        public boolean hasLookahead()
        {
            return !lookaheadBuffer.isEmpty();
        }

        public TokenReaderWithLookahead(java.io.Reader in, String inFileName, String extraCrap)
        {
            this.tokenReader = new TokenReader(in, inFileName);
            this.inFileName = inFileName; // maybe not necessary, just provide an accessor to tokenReader's?
            this.extraCrap = extraCrap;
        }

        public Token readToken()
            throws java.io.IOException // since tokenReader.readToken() does
        {
            Token token;
            boolean lookedahead;
            if (!lookaheadBuffer.isEmpty())
            {
                token = (Token)lookaheadBuffer.removeFirst();
                lookedahead = true;
            }
            else
            {
                token = tokenReader.readToken();
                lookedahead = false;
            }
            //System.err.println("            TokenReaderWithLookahead returning ("+(lookedahead ? "lookedahead" : "nolookedahead")+"): "+token);
            return token;
        }
        public Token peekToken(int index)
            throws java.io.IOException // since tokenReader.readToken() does
        {
            while (lookaheadBuffer.size() <= index)
            {
                Token token = tokenReader.readToken();
                lookaheadBuffer.add(token);
            }
            return (Token)lookaheadBuffer.get(index);
        }
        public void pushBackToken(Token token)
        {
            lookaheadBuffer.add(0, token);
        }
        public void pushBackTokens(Token tokens[])
        {
            for (int i = tokens.length-1; i >= 0; --i)
                lookaheadBuffer.add(0, tokens[i]);

        }
        public void pushBackTokens(java.util.Vector tokensVector)
        {
            for (int i = tokensVector.size()-1; i >= 0; --i)
                lookaheadBuffer.add(0, (Token)tokensVector.get(i));

        }
        public void setFileName(String fileName)
        {
            inFileName = fileName;
            tokenReader.setFileName(fileName);

            java.util.ListIterator iter = lookaheadBuffer.listIterator(0);
            while (iter.hasNext())
            {
                Token token = (Token)iter.next();
                token.fileName = fileName;
            }
        }
        public void setLineNumber(int lineNumber)
        {
            if (hasLookahead())
                System.err.println("Uh oh, setting line number to "+lineNumber+" but has lookahead");

            tokenReader.setLineNumber(lineNumber);

            java.util.ListIterator iter = lookaheadBuffer.listIterator(0);
            while (iter.hasNext())
            {
                Token token = (Token)iter.next();
                System.err.println("    changing from "+token.lineNumber+" to "+lineNumber+" on a "+Token.typeToName(token.type));
                token.lineNumber = lineNumber;
                // count number of newlines in text, add that to lineNumber
                int i = 0;
                while ((i = token.text.indexOf('\n', i)) != -1)
                {
                    lineNumber++;
                    i++;
                }
            }
        }
    } // private static class TokenReaderWithLookahead

    private static Token readTokenWithMacroSubstitution(TokenReaderWithLookahead in,
                                                        int lineNumber,
                                                        java.util.Hashtable macros,
                                                        boolean evaluateDefineds)
        throws java.io.IOException
    {
        while (true)
        {
            Token token = _readTokenWithMacroSubstitution(in, lineNumber, macros, evaluateDefineds);

            if (token.type != Token.EOF
             && token.type != Token.NEWLINE_UNESCAPED) // no need to look for tokenpaste past a newline, this avoids big messiness in setLineNumber
            {
                if (token.type == Token.TOKEN_PASTE)
                {
                    //System.err.println("==========HEY! discarding ## since LHS is empty, I think");
                    continue;
                }
                else if (in.peekToken(0).type == Token.TOKEN_PASTE)
                {
                    Token tokenPasteToken = in.readToken();
                    Token anotherToken = _readTokenWithMacroSubstitution(in, lineNumber, macros, evaluateDefineds);
                    String combinedText = token.text + anotherToken.text;
                    if (false)
                        combinedText = "[" + combinedText + "]";
                    // XXX TODO: need to re-tokenize the combined text I think?? hmm what's an example where it makes a difference?
                    Token combinedToken = new Token(token.type,
                                                    combinedText,
                                                    token.fileName,
                                                    token.lineNumber,
                                                    token.columnNumber);
                    //System.err.println("==========HEY! pasting tokens \""+token.text+"\" and \""+anotherToken.text+"\" to get \""+combinedToken.text+"\"");
                    in.pushBackToken(combinedToken);
                    continue;
                }
                else
                {
                    //System.err.println("(peeked a "+Token.typeToName(in.peekToken(0).type)+")");
                }
            }
            //System.err.println("readTokenWithMacroSubstitution (possibly recursive) returning "+token);
            AssertAlways(token.type != Token.TOKEN_PASTE); // those never make it out of this function
            return token;
        }
    } // readTokenWithMacroSubstitution

    // The implementation of readTokenWithMacroSubstitution except for token pasting
    private static Token _readTokenWithMacroSubstitution(TokenReaderWithLookahead in,
                                                         int lineNumber, // XXX TODO: is this needed any more?  I think it's stored in the token itself now, should check to make sure
                                                         java.util.Hashtable macros,
                                                         boolean evaluateDefineds)
        throws java.io.IOException
    {
        while (true)
        {
            Token token = in.readToken();

            if (token.type != Token.IDENTIFIER)
                return token;

            if (evaluateDefineds
             && token.text.equals("defined"))
            {
                // must be followed by an identifier or exactly the following:
                // '(', identifier, ')'.
                Token nextToken = in.readToken();
                while (nextToken.type == Token.SPACES
                    || nextToken.type == Token.COMMENT)
                    nextToken = in.readToken();

                String macroName;
                if (nextToken.type == Token.SYMBOL
                 && nextToken.text.equals("("))
                {
                    nextToken = in.readToken();
                    if (nextToken.type != Token.IDENTIFIER)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": operator \"defined\" requires an identifier");
                    macroName = nextToken.text;
                    nextToken = in.readToken();
                    if (nextToken.type != Token.SYMBOL
                     || !nextToken.text.equals(")"))
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": missing ')' after \"defined\"");
                }
                else if (nextToken.type == Token.IDENTIFIER)
                    macroName = nextToken.text;
                else
                    throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": operator \"defined\" requires an identifier");

                if (macros.get(macroName) != null)
                    return new Token(Token.INT_LITERAL, "1", token.fileName, token.lineNumber, token.columnNumber);
                else
                    return new Token(Token.INT_LITERAL, "0", token.fileName, token.lineNumber, token.columnNumber);
            }

            Macro macro = (Macro)macros.get(token.text);
            if (macro == null)
            {
                if (evaluateDefineds) // XXX assume if we are evaluating defineds, then we are in an #if or #elif which means we also want to turn unrecognized identifiers into 0.  evaluateDefineds may not be a good name for this any more
                {
                    return new Token(Token.INT_LITERAL, "0", token.fileName, token.lineNumber, token.columnNumber);
                }
                return token;
            }

            if (true)
            {
                //System.err.println("Checking for recursion to decide whether to expand macro "+token.text+"");
                for (Token ancestorToken = token.parentInMacroInvocation;
                     ancestorToken != null;
                     ancestorToken = ancestorToken.parentInMacroInvocation)
                {
                    //System.err.println("    -> "+ancestorToken.text);
                    if (ancestorToken.text.equals(token.text))
                    {
                        //System.err.println("HEY! suppressing macro recursion on token "+token.text+"");
                        return token;
                    }
                }
            }

            if (macro.numParams == -1) // if it's an invocation of a simple macro without an arg list
            {
                // special cases...
                if (token.text.equals("__LINE__"))
                    in.pushBackToken(new Token(Token.INT_LITERAL,  ""+(lineNumber+1), in.inFileName, lineNumber, -1));
                else if (token.text.equals("__FILE__"))
                    in.pushBackToken(new Token(Token.STRING_LITERAL, "\""+escapify(in.inFileName)+"\"", in.inFileName, lineNumber, -1));
                else
                {
                    /* can't just push back the tokens, we need to change the line numbers too... and also set parent for recursion avoidance check */
                    if (false)
                        in.pushBackTokens(macro.contents);
                    else
                    {
                        Token macroContentsCopy[] = new Token[macro.contents.length];
                        for (int i = 0; i < macro.contents.length; ++i)
                        {
                            macroContentsCopy[i] = new Token(macro.contents[i], in.inFileName, lineNumber);
                            macroContentsCopy[i].parentInMacroInvocation = token;
                        }
                        in.pushBackTokens(macroContentsCopy);
                    }
                }

                if (false)
                {
                    // argh can't do this, what if it's '##'
                    continue;
                }
                else
                {
                    // do this instead, hope it works, it's tail recursion I guess so it should be possible to do this in a while loop if I was really smart
                    return readTokenWithMacroSubstitution(in, lineNumber, macros, evaluateDefineds);
                }
            }
            else // it's an invocation of a macro with an arg list
            {
                // move past spaces
                while (in.peekToken(0).type == Token.SPACES
                    || in.peekToken(0).type == Token.COMMENT)
                    in.readToken();
                Token shouldBeLeftParen = in.readToken();
                if (!(shouldBeLeftParen.type == Token.SYMBOL
                  && shouldBeLeftParen.text.equals("(")))
                    throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": invocation of macro "+token.text+" not followed by arg list");
                // each arg will be a list of tokens
                Token args[][] = new Token[macro.numParams][];
                for (int iArg = 0; iArg < macro.numParams; ++iArg)
                {
                    java.util.Vector thisArgTokensVector = new java.util.Vector();
                    // read tokens up to a comma or right paren.
                    int parenLevel = 1;
                    while (true)
                    {
                        Token anotherToken = in.readToken();
                        if (anotherToken.type == Token.EOF)
                            throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": EOF in middle of arg list for macro "+token.text+"");
                        if (parenLevel > 1)
                        {
                            if (anotherToken.type == Token.SYMBOL
                             && anotherToken.text.equals(")"))
                                parenLevel--;
                        }
                        else // parenLevel == 1
                        {
                            if (anotherToken.type == Token.SYMBOL
                             && (anotherToken.text.equals(")")
                              || anotherToken.text.equals(",")))
                            {

                                if (!anotherToken.text.equals(iArg==macro.numParams-1 ? ")" : ","))
                                {
                                    if (iArg < macro.numParams-1)
                                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro \""+token.text+"\" requires "+macro.numParams+" arguments, but only "+(iArg+1)+" given");
                                    else
                                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro \""+token.text+"\" requires "+macro.numParams+" arguments, but more given");
                                    // cpp's message is "passed 3 arguments, but takes just 2, but whatever... we aren't set up to receive more than that into the array and it's not important enough
                                }

                                break; // without appending
                            }
                        }
                        if (anotherToken.text.equals("("))
                            parenLevel++;
                        thisArgTokensVector.add(anotherToken);
                    }

                    args[iArg] = new Token[thisArgTokensVector.size()];
                    for (int j = 0; j < args[iArg].length; ++j)
                        args[iArg][j] = (Token)thisArgTokensVector.get(j);
                }

                // Now substitute the args
                // into the macro contents
                java.util.Vector resultsVector = new java.util.Vector();
                for (int iContent = 0; iContent < macro.contents.length; ++iContent)
                {
                    Token contentToken = macro.contents[iContent];
                    if (contentToken.type == Token.MACRO_ARG)
                    {
                        int iArg = contentToken.lineNumber; // for MACRO_ARG tokens, arg index in is smuggled in through line number
                        for (int j = 0; j < args[iArg].length; ++j)
                            resultsVector.add(new Token(args[iArg][j], null, lineNumber));
                    }
                    else if (contentToken.type == Token.MACRO_ARG_QUOTED)
                    {
                        int iArg = contentToken.lineNumber; // for MACRO_ARG tokens, arg index in is smuggled in through line number
                        StringBuffer sb = new StringBuffer();
                        for (int j = 0; j < args[iArg].length; ++j)
                            sb.append(args[iArg][j].text);
                        resultsVector.add(new Token(Token.STRING_LITERAL, "\""+escapify(sb.toString())+"\"", null, -1, -1)); // XXX TODO: do a line and column number make sense here?
                    }
                    else
                    {
                        Token newToken = new Token(contentToken, in.inFileName, lineNumber);
                        newToken.parentInMacroInvocation = token;
                        resultsVector.add(newToken);
                    }
                }
                in.pushBackTokens(resultsVector);

                if (false)
                {
                    System.err.println("pushed back argumentative macro expansion:");
                    for (int i = 0; i < resultsVector.size(); ++i)
                    {
                        System.err.println("    "+(Token)resultsVector.get(i));
                    }
                }

                if (false)
                {
                    // argh can't do this, what if it's '##'
                    continue;
                }
                else
                {
                    // do this instead, hope it works, it's tail recursion I guess so it should be possible to do this in a while loop if I was really smart
                    return readTokenWithMacroSubstitution(in, lineNumber, macros, evaluateDefineds);
                }
            }
        }
    } // _readTokenWithMacroSubstitution

    // Returns a triple {name, Reader, extraCrap}.
    private static Object[/*3*/] findAndNewFileReader(FileOpener fileOpener,
                                                      String fileName,
                                                      String searchPath[],
                                                      String directoryToLookInFirst) // null means don't, "" means "." but don't prepend anything
        throws java.io.FileNotFoundException
    {
        //
        // The logic for the extra crap after line directives is:
        //  3 -> sysp=1
        //  3 4 -> sysp=2
        // And in the cpp source, in internal.h, it says this about sysp:
        //  // One for a system header, two for a C system header file that therefore
        //  // needs to be extern "C" protected in C++, and zero otherwise.
        // We to imitate that logic, just so that we'll have identical
        // output to cpp,
        // although I don't really know what "system header file" means.
        // we just assume it means anything under /usr.
        //
        if (fileName.startsWith("/"))
        {
            java.io.Reader reader = fileOpener.newFileReader(fileName); // if it throws, we throw
            String extraCrap = !fileName.startsWith("/usr/") ? "" :
                               !fileName.endsWith(".h") ? " 3" : " 3 4";
            return new Object[] {fileName, reader, extraCrap};
        }

        if (directoryToLookInFirst != null)
        {
            String pathName = directoryToLookInFirst.equals("")
                                  ? fileName
                                  : directoryToLookInFirst+"/"+fileName;
            try
            {
                java.io.Reader reader = fileOpener.newFileReader(pathName);
                String extraCrap = ""; // cpp doesn't seem to add extra crap when the pathname is constructed this way
                return new Object[] {pathName, reader, extraCrap};
            }
            catch (java.io.FileNotFoundException e)
            {}
        }

        for (int i = 0; i < searchPath.length; ++i)
        {
            String pathName = searchPath[i]+"/"+fileName;
            try
            {
                java.io.Reader reader = fileOpener.newFileReader(pathName);
                String extraCrap = !pathName.startsWith("/usr/") ? "" :
                                   !pathName.endsWith(".h") ? " 3" : " 3 4";
                return new Object[] {pathName, reader, extraCrap};
            }
            catch (java.io.FileNotFoundException e)
            {}
        }
        throw new java.io.FileNotFoundException("No such file or directory");
    } // findAndNewFileReader

    public static void filter(TokenReaderWithLookahead in,
                              FileOpener fileOpener,
                              String includePath[],
                              java.io.PrintWriter out,
                              java.util.Hashtable macros, // gets updated as we go
                              ExpressionParser expressionParser,
                              boolean commentOutLineDirectives,
                              int recursionLevel)

        throws java.io.IOException
    {
        int verboseLevel = 0; // 0: nothing, 1: print enter and exit function, 2: print more

        if (verboseLevel >= 1)
            System.err.println("    in filter");

        java.util.Stack ifStack = new java.util.Stack(); // of #ifwhatever tokens, for the file,line,column information
        int highestTrueIfStackLevel = 0;
        java.util.Stack endifMultiplierStack = new java.util.Stack();

        int lineNumber = 0;
        int columnNumber = 0;
        if (recursionLevel >= 1)
            out.println((commentOutLineDirectives?"// ":"")+"# "+(lineNumber+1)+" \""+in.inFileName+"\" 1"+in.extraCrap); // cpp puts a 1 there when entering recursive levels, imitating it
        else
            out.println((commentOutLineDirectives?"// ":"")+"# "+(lineNumber+1)+" \""+in.inFileName+"\""+in.extraCrap);
        int outputLineNumber = lineNumber;
        int outputTokenColumnNumber = 0;

        Token token = null;
        while (true)
        {
            // The following assumes that every token
            // is marked with the line number of the top-level token
            // that produced it, before macro substitution.
            // That requires making a lot of new tokens on the fly whenever macros
            // are expanded, just for the line numbers, but that's how it's currently done.
            lineNumber = in.peekToken(0).lineNumber; // don't worry about macro expansion
            columnNumber = in.peekToken(0).columnNumber; // don't worry about macro expansion

            // XXX TODO: argh, should NOT honor stuff like #define INCLUDE #include, I mistakenly thought I should honor it. but should be able to substitute for the filename though
            if (ifStack.size() <= highestTrueIfStackLevel)
                token = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
            else
                token = in.readToken(); // don't expand macros, so, for example, we don't choke trying to expand FOO when we see #undef FOO inside #if 0

            if (token.type == Token.EOF)
                break;


            // when inside a false #if,
            // the only preprocessor directives we recognize are:
            //     #if*
            //     #endif
            //     #elif
            //     #else
            // (argh, I guess the only ones we *don't* recognize
            // are #define, #undef, #include)
            if (token.type == Token.PREPROCESSOR_DIRECTIVE
             && (ifStack.size() <= highestTrueIfStackLevel || token.text.startsWith("#if")
                                                           || token.text.equals("#endif")
                                                           || token.text.equals("#elif")
                                                           || token.text.equals("#else")
            ))
            {
                AssertAlways(token.text.startsWith("#"));

                if (false) ;
                // ones that take an integer expression
                else if (token.text.equals("#if")
                      || token.text.equals("#elif"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found "+token.text);

                    Token nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, true);

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, true);

                    Token expressionStartToken = nextToken;

                    // gather rest of line (with macro substitution and defined() evaluation)
                    // into a string...
                    StringBuffer sb = new StringBuffer();
                    while (nextToken.type != Token.NEWLINE_UNESCAPED
                        && nextToken.type != Token.EOF)
                    {
                        if (nextToken.type == Token.COMMENT)
                            sb.append(" ");
                        else
                            sb.append(nextToken.text);
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, true);
                    }


                    boolean needToEvaluate;
                    if (token.text.equals("#elif"))
                    {
                        // Treat this as #else followed by #if.
                        // First do the #else thing...
                        if (ifStack.empty())
                            throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": "+token.text+" without #if");
                        if (highestTrueIfStackLevel >= ifStack.size()-1) // if not suppressing at the moment
                        {
                            if (highestTrueIfStackLevel == ifStack.size()-1)
                                highestTrueIfStackLevel = ifStack.size(); // false to true
                            else
                                highestTrueIfStackLevel = ifStack.size()-1; // true to false
                        }

                        ifStack.push(token);
                        // Increment the prevailing #endif multiplier...
                        endifMultiplierStack.push(new Integer((((Integer)endifMultiplierStack.pop()).intValue()+1)));
                    }
                    else // #if
                    {
                        ifStack.push(token);
                        // Just do the #if thing, pushing a multiplier of 1.
                        endifMultiplierStack.push(new Integer(1));
                    }

                    if (ifStack.size()-1 <= highestTrueIfStackLevel)
                    {
                        // we're not suppressing at the parent level,
                        // so we do in fact need to evaluate the expression
                        // to find out whether to suppress at this level
                        int expressionValue = 0;
                        try
                        {
                            expressionValue = expressionParser.evaluateIntExpression(sb.toString());
                        }
                        catch (Exception e)
                        {
                            throw new Error(expressionStartToken.fileName+":"+(expressionStartToken.lineNumber+1)+":"+(expressionStartToken.columnNumber+1)+": "+e.getMessage()+" in "+token.text+", expression was "+sb.toString()+"");
                        }
                        boolean answer = (expressionValue != 0);

                        if (answer == true)
                        {
                            if (verboseLevel >= 2)
                                System.err.println("    condition evaluated to true");
                            highestTrueIfStackLevel = ifStack.size();
                        }
                        else
                        {
                            if (verboseLevel >= 2)
                                System.err.println("    condition evaluated to false");
                            highestTrueIfStackLevel = ifStack.size()-1;
                        }
                    }
                    else
                    {
                        if (verboseLevel >= 2)
                            System.err.println("    didn't need to evaluate condition since we're already suppressing at a shallower level");
                    }


                    if (nextToken.type == Token.EOF)
                    {
                        in.pushBackToken(nextToken);
                        continue;
                    }
                    AssertAlways(nextToken.type == Token.NEWLINE_UNESCAPED);
                    // don't bother outputting it, we'll output it lazily on next non-newline
                }
                // ones that take no args...
                else if (token.text.equals("#else")
                      || token.text.equals("#endif"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found "+token.text);


                    if (ifStack.empty())
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": "+token.text+" without #if");
                    if (token.text.equals("#else"))
                    {
                        if (highestTrueIfStackLevel >= ifStack.size()-1) // if not suppressing at the moment
                        {
                            if (highestTrueIfStackLevel == ifStack.size()-1)
                                highestTrueIfStackLevel = ifStack.size(); // false to true
                            else
                                highestTrueIfStackLevel = ifStack.size()-1; // true to false
                        }
                    }
                    else // #endif
                    {
                        int endifMultiplier = ((Integer)endifMultiplierStack.pop()).intValue();
                        for (int i = 0; i < endifMultiplier; ++i)
                            ifStack.pop();
                    }


                    Token nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                    if (nextToken.type != Token.EOF
                     && nextToken.type != Token.NEWLINE_UNESCAPED)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": extra tokens at end of "+token.text+" directive");

                    if (nextToken.type == Token.EOF)
                    {
                        in.pushBackToken(nextToken);
                        continue;
                    }
                    AssertAlways(nextToken.type == Token.NEWLINE_UNESCAPED);
                    // don't bother outputting it, we'll output it lazily on next non-newline
                }
                // ones that take one macro name arg and that's all
                else if (token.text.equals("#ifdef")
                      || token.text.equals("#ifndef")
                      || token.text.equals("#undef"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found "+token.text);

                    Token nextToken = in.readToken(); // WITHOUT macro substitution, so we don't expand the expected macro name

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = in.readToken(); // WITHOUT macro substitution, so we don't expand the expected macro name

                    if (nextToken.type == Token.EOF
                     || nextToken.type == Token.NEWLINE_UNESCAPED)
                    {
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": no macro name given in "+token.text+" directive");
                    }

                    if (nextToken.type != Token.IDENTIFIER)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro names must be identifiers");
                    String macroName = nextToken.text;
                    nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);


                    if (token.text.equals("#undef"))
                    {
                        if (macroName == "__LINE__"
                         || macroName == "__FILE__")
                            throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": can't undefine \""+macroName+"\"");
                        macros.remove(macroName);
                    }
                    else // #ifdef or #ifndef
                    {
                        if (highestTrueIfStackLevel >= ifStack.size()) // if currently true, see if this makes the one we are adding true or false
                        {
                            boolean defined = (macros.get(macroName) != null);
                            boolean answer = (defined == token.text.equals("#ifdef"));
                            if (answer == true)
                            {
                                if (verboseLevel >= 2)
                                    System.err.println("    condition evaluated to true");
                                highestTrueIfStackLevel = ifStack.size()+1;
                            }
                            else
                            {
                                if (verboseLevel >= 2)
                                    System.err.println("    condition evaluated to false");
                                highestTrueIfStackLevel = ifStack.size();
                            }
                        }

                        ifStack.push(token);
                        endifMultiplierStack.push(new Integer(1));
                    }


                    if (nextToken.type != Token.EOF
                     && nextToken.type != Token.NEWLINE_UNESCAPED)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": extra tokens at end of "+token.text+" directive");

                    if (nextToken.type == Token.EOF)
                    {
                        in.pushBackToken(nextToken);
                        continue;
                    }
                    AssertAlways(nextToken.type == Token.NEWLINE_UNESCAPED);
                    // don't bother outputting it, we'll output it lazily on next non-newline
                }
                else if (token.text.equals("#define"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found #define");

                    // we'll be doing a lot of lookahead of one token,
                    // so use a local variable nextToken
                    Token nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = in.readToken(); // WITHOUT macro substitution, so we don't expand the expected macro name

                    if (nextToken.type == Token.EOF
                     || nextToken.type == Token.NEWLINE_UNESCAPED)
                    {
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": no macro name given in #define directive");
                    }

                    if (nextToken.type != Token.IDENTIFIER)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro names must be identifiers");
                    String macroName = nextToken.text;

                    // must be either whitespace or left paren after macro name... it makes a difference
                    nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily
                    if (nextToken.type == Token.EOF)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": no newline at end of file"); // in cpp it's a warning but we don't tolerate it

                    String paramNames[] = null;
                    if (nextToken.type == Token.SYMBOL
                     && nextToken.text.equals("("))
                    {
                        if (verboseLevel >= 2)
                            System.err.println("        filter:     and there's a macro param list");
                        // There's a macro param list.
                        java.util.Vector paramNamesVector = new java.util.Vector();

                        nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                        // move past spaces
                        while (nextToken.type == Token.SPACES
                            || nextToken.type == Token.COMMENT)
                            nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                        if (nextToken.type == Token.SYMBOL
                         && nextToken.text.equals(")"))
                        {
                            // zero params
                        }
                        else
                        {
                            // must be one or more param names,
                            // separated by commas,
                            // followed by close paren

                            if (nextToken.type != Token.IDENTIFIER)
                                throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever

                            paramNamesVector.add(nextToken.text);
                            nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                            while (true)
                            {
                                // move past spaces
                                while (nextToken.type == Token.SPACES
                                    || nextToken.type == Token.COMMENT)

                                    nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                                if (nextToken.type == Token.SYMBOL)
                                {
                                    if (nextToken.text.equals(")"))
                                        break;
                                    else if (nextToken.text.equals(","))
                                    {
                                        nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                                        // move past spaces
                                        while (nextToken.type == Token.SPACES
                                            || nextToken.type == Token.COMMENT)
                                            nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                                        if (nextToken.type == Token.IDENTIFIER)
                                        {
                                            paramNamesVector.add(nextToken.text);
                                            nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily
                                            continue;
                                        }
                                        // otherwise drop into error case
                                    }
                                }
                                throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever
                            }
                        }
                        AssertAlways(nextToken.type == Token.SYMBOL
                                  && nextToken.text.equals(")"));
                        nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily

                        paramNames = (String[])paramNamesVector.toArray(new String[0]);
                    }
                    else if (nextToken.type == Token.SPACES
                          || nextToken.type == Token.COMMENT
                          || nextToken.type == Token.NEWLINE_UNESCAPED
                          || nextToken.type == Token.EOF)
                    {
                        if (verboseLevel >= 2)
                            System.err.println("        filter:     and there's no macro param list");
                        ;
                    }
                    else
                    {
                        // macro name was not followed by a macro param list
                        // nor spaces
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever
                    }

                    // we are now in the #define, past the macro name and optional arg list.
                    // next comes the content, up to an unescaped newline
                    // or eof.
                    // still using nextToken to hold the next token we are about to look at.

                    java.util.Vector contentsVector = new java.util.Vector();

                    while (nextToken.type != Token.NEWLINE_UNESCAPED
                        && nextToken.type != Token.EOF)
                    {
                        if (paramNames != null)
                        {
                            if (nextToken.type == Token.IDENTIFIER)
                            {
                                for (int i = 0; i < paramNames.length; ++i)
                                    if (nextToken.text.equals(paramNames[i]))
                                    {
                                        nextToken = new Token(Token.MACRO_ARG, "", null, i, -1); // smuggle in param index through line number
                                        break;
                                    }
                                // if not found, it stays identifier
                            }
                            else if (nextToken.type == Token.PREPROCESSOR_DIRECTIVE)
                            {
                                String paramNameMaybe = nextToken.text.substring(1); // spaces got crunched out already during token lexical scanning
                                if (paramNameMaybe.equals("#"))
                                {
                                    //System.err.println(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": hey! "+macroName+" is a token pasting macro!");
                                    nextToken = new Token(Token.TOKEN_PASTE, "##", nextToken.fileName, nextToken.lineNumber, nextToken.columnNumber);
                                }
                                else
                                {
                                    for (int i = 0; i < paramNames.length; ++i)
                                        if (paramNameMaybe.equals(paramNames[i]))
                                        {
                                            nextToken = new Token(Token.MACRO_ARG_QUOTED, "", null, i, -1); // smuggle in param index through line number
                                            break;
                                        }
                                    // if not found, it's an error
                                    if (nextToken.type == Token.PREPROCESSOR_DIRECTIVE)
                                    {
                                        System.err.println(nextToken);
                                        throw new Error(in.inFileName+":"+(nextToken.lineNumber+1)+":"+(nextToken.columnNumber+1)+": '#' is not followed by a macro parameter");
                                    }
                                }
                            }
                        }

                        contentsVector.add(nextToken);
                        nextToken = in.readToken(); // WITHOUT macro substitution, so that macros get expanded lazily
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
                            if (contents[iIn].type == Token.SPACES
                             || contents[iIn].type == Token.COMMENT)
                            {
                                if (nOut != 0
                                 && contents[nOut-1].type != Token.SPACES
                                 && !contents[nOut-1].text.equals("##")) // spaces after '##' disappear
                                    contents[nOut++] = new Token(Token.SPACES, " ", contents[iIn].fileName, contents[iIn].lineNumber, contents[iIn].columnNumber);
                            }
                            else
                            {
                                if (contents[iIn].text.equals("##")
                                 && nOut > 0
                                 && contents[nOut-1].type == Token.SPACES)
                                    nOut--; // spaces before '##' disappear
                                contents[nOut++] = contents[iIn];
                            }
                        }
                        // and discard spaces and comments at the end too
                        if (nOut > 0
                         && contents[nOut-1].type == Token.SPACES)
                            nOut--;
                        AssertAlways(!(nOut > 0
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
                                            in.inFileName,
                                            token.lineNumber,
                                            token.columnNumber);
                    if (verboseLevel >= 2)
                        System.err.println("        filter:     defining macro \""+macroName+"\": "+macro);
                    Macro previousMacro = (Macro)macros.get(macroName);
                    if (previousMacro != null)
                    {
                        if (macroName == "__LINE__"
                         || macroName == "__FILE__")
                            throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": can't redefine \""+macroName+"\"");


                        if (!macro.sameContents(previousMacro))
                        {
                            // note, cpp's notion of sameness is more strict than ours... for example, we consider #define FOO(a,b) a##b the same as #define FOO(x,y) x##y
                            System.err.println(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": warning: \""+macroName+"\" redefined");
                            System.err.println(previousMacro.inFileName+":"+(previousMacro.lineNumber+1)+":"+(previousMacro.columnNumber+1)+": warning: this is the location of the previous definition");
                        }
                    }

                    macros.put(macroName, macro);

                    if (nextToken.type == Token.EOF)
                    {
                        in.pushBackToken(nextToken);
                        continue;
                    }
                    AssertAlways(nextToken.type == Token.NEWLINE_UNESCAPED);
                    // don't bother outputting it, we'll output it lazily on next non-newline
                }
                else if (token.text.equals("#include"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found #include");

                    // XXX TODO: the real cpp doesn't really parse the filename as a string, e.g. it doesn't recognize backslash as special

                    Token nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                    char delimiter = '"';
                    Token fileNameToken = nextToken;
                    if (fileNameToken.type == Token.SYMBOL
                     && fileNameToken.text.equals("<"))
                    {
                        delimiter = '<';
                        // turn it into a string
                        StringBuffer sb = new StringBuffer();
                        sb.append(nextToken.text);
                        while (true)
                        {
                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                            if (nextToken.type == Token.EOF
                             || nextToken.type == Token.NEWLINE_UNESCAPED)
                                throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\" or <FILENAME>");
                            sb.append(nextToken.text);
                            if (nextToken.type == Token.SYMBOL
                             && nextToken.text.equals(">"))
                                break;
                        }
                        fileNameToken = new Token(Token.STRING_LITERAL, sb.toString(), fileNameToken.fileName, fileNameToken.lineNumber, fileNameToken.columnNumber); // XXX this is a bit weird, we've made a string literal that's not delimited by quotes and that doesn't follow usual backslash stuff, maybe it's misnamed... but actually we shouldn't be parsing as string_literal at all, just use a StringBuffer even when delimited by quotes?
                    }
                    if (fileNameToken.type != Token.STRING_LITERAL)
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\" or <FILENAME>");

                    String newInFileName = fileNameToken.text.substring(1, fileNameToken.text.length()-1);
                    TokenReaderWithLookahead newIn = null;
                    try
                    {
                        String directoryToLookInFirst;
                        if (delimiter == '<')
                            directoryToLookInFirst = null;
                        else
                        {
                            if (in.inFileName.indexOf('/') != -1)
                            {
                                directoryToLookInFirst = new java.io.File(in.inFileName).getParent();
                                System.err.println("file parent of \""+in.inFileName+"\" is \""+directoryToLookInFirst+"\"");
                            }
                            else
                            {
                                directoryToLookInFirst = ""; // means "." except don't actually put that in the filename
                            }
                        }

                        Object newInAndPathName[/*3*/] = findAndNewFileReader(fileOpener, newInFileName, includePath, directoryToLookInFirst);
                        String newInPathName = (String)newInAndPathName[0];
                        java.io.Reader newInReader = (java.io.Reader)newInAndPathName[1];
                        String newExtraCrap = (String)newInAndPathName[2];
                        newIn = new TokenReaderWithLookahead(
                                newInReader,
                                newInPathName,
                                newExtraCrap);
                    }
                    catch (java.io.FileNotFoundException e)
                    {

                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": \""+newInFileName+"\": No such file or directory");
                    }

                    while (in.peekToken(0).type == Token.SPACES
                        || in.peekToken(0).type == Token.COMMENT)
                        in.readToken();
                    if (in.peekToken(0).type != Token.NEWLINE_UNESCAPED)
                    {
                        // TODO: wrong error message if it's EOF
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": extra stuff confusing the #include "+newInFileName);
                    }

                    // Have to get output newlines in sync,
                    // I guess so anyone reading the output
                    // will know which line the included file was included from.
                    AssertAlways(lineNumber == token.lineNumber); // do we even need the lineNumber variable??
                    int desiredOutputLineNumber = token.lineNumber;
                    {
                        if (outputTokenColumnNumber > 0)
                        {
                            //out.print("(a)");
                            out.println();
                            outputLineNumber++;
                            outputTokenColumnNumber = 0;
                        }
                        while (outputLineNumber < desiredOutputLineNumber
                            && desiredOutputLineNumber-outputLineNumber <= 7)
                        {
                            //out.print("(b)");
                            out.println();
                            outputLineNumber++;
                            outputTokenColumnNumber = 0;
                        }
                        if (outputLineNumber != desiredOutputLineNumber)
                        {
                            out.println((commentOutLineDirectives?"// ":"")+"# "+(desiredOutputLineNumber+1)+" \""+in.inFileName+"\""+in.extraCrap);
                            outputLineNumber = desiredOutputLineNumber;
                            outputTokenColumnNumber = 0;
                        }
                    }
                    if (recursionLevel+1 >= 200) // same limit as cpp, apparently
                    {
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include nested too deeply");
                    }

                    filter(newIn,
                           fileOpener,
                           includePath,
                           out,
                           macros,
                           expressionParser,
                           commentOutLineDirectives,
                           recursionLevel+1);
                    out.println((commentOutLineDirectives?"// ":"")+"# "+(lineNumber+2)+" \""+in.inFileName+"\" 2"+in.extraCrap); // cpp puts a 2 there when leaving recursive levels, imitating it

                    outputLineNumber = lineNumber+1;
                    outputTokenColumnNumber = 0;
                }
                else if (token.text.equals("#error")
                      || token.text.equals("#warning")
                      || token.text.equals("#pragma"))
                {
                    // gather rest of line (WITHOUT macro substitution)
                    // into a string...
                    // XXX TODO: comments?
                    Token nextToken = in.readToken();
                    StringBuffer sb = new StringBuffer();
                    sb.append(token.text);
                    while (nextToken.type != Token.NEWLINE_UNESCAPED
                        && nextToken.type != Token.EOF)
                    {
                        if (nextToken.type == Token.COMMENT)
                            sb.append(" ");
                        else
                            sb.append(nextToken.text);
                        nextToken = in.readToken();
                    }
                    if (token.text.equals("#pragma"))
                    {
                        // just try to imitate what cpp does, on the include files in /usr/include... okay if it's not fully general or robust
                        if (sb.toString().equals("#pragma GCC system_header"))
                        {
                            // imitate screwy logic of cpp
                            if (recursionLevel == 0)
                            {
                                if (outputTokenColumnNumber > 0)
                                {
                                    out.println();

                                    outputLineNumber++;
                                    outputTokenColumnNumber = 0;
                                }
                                out.flush(); // XXX TODO: ad-hoc, should put these everywhere maybe
                                System.err.println(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": warning: #pragma system_header ignored outside include file");
                            }
                            else
                            {
                                if (!in.extraCrap.startsWith(" 3"))
                                {
                                    AssertAlways(in.extraCrap.equals(""));
                                    // apparently gcc doesn't add the 4 even if it's a .h file
                                    //if (in.inFileName.endsWith(".h"))
                                    //    in.extraCrap = " 3 4";
                                    //else
                                        in.extraCrap = " 3";
                                    // TODO: argh! if it's top level, doesn't do it
                                }
                                if (outputTokenColumnNumber > 0)
                                {
                                    out.println();

                                    outputLineNumber++;
                                    outputTokenColumnNumber = 0;
                                }
                                out.println((commentOutLineDirectives?"// ":"")+"# "+(lineNumber+1)+" \""+in.inFileName+"\""+in.extraCrap);
                                outputLineNumber = lineNumber;
                                outputTokenColumnNumber = 0;
                            }
                        }
                        else
                            System.err.println(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": warning: I don't understand pragma: "+sb.toString()); // TODO: cpp gives column number where "warning" begins, not where "#warning" begins
                    }
                    else if (token.text.equals("#warning"))
                        System.err.println(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": warning: "+sb.toString()); // TODO: cpp gives column number where "warning" begins, not where "#warning" begins
                    else // token.text.equals("#error")
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": "+sb.toString());
                }
                else if (token.text.length() >= 2
                      && Character.isDigit(token.text.charAt(1)))
                {
                    // Line number directive
                    int oneBasedLineNumber = token.text.charAt(1) - '0';
                    for (int i = 2; i < token.text.length(); ++i)
                    {
                        char c = token.text.charAt(i);
                        if (!Character.isDigit(c))
                            throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": \""+token.text.substring(1)+"\" after # is not a positive integer"); // // TODO: cpp gives column number where "warning" begins, not where "#warning" begins
                        oneBasedLineNumber = oneBasedLineNumber*10 + (c-'0');
                    }

                    Token nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                    Token fileNameToken = nextToken;
                    if (fileNameToken.type != Token.STRING_LITERAL)
                    {
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": \""+escapify(fileNameToken.text)+"\" is not a valid filename");
                    }
                    nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                    java.util.Vector flagsVector = new java.util.Vector();
                    while (true)
                    {
                        while (nextToken.type == Token.SPACES
                            || nextToken.type == Token.COMMENT)
                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                        if (nextToken.type == Token.NEWLINE_UNESCAPED)
                            break;
                        else if (nextToken.type == Token.INT_LITERAL)
                        {
                            flagsVector.add(nextToken.text); // don't bother parsing it into an actual int
                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                        }
                        else
                            throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": invalid flag \""+escapify(nextToken.text)+"\" in line directive");
                    }
                    //System.err.println("got "+flagsVector.size()+" int flags");
                    String flags[] = new String[flagsVector.size()];
                    for (int i = 0; i < flags.length; ++i)
                        flags[i] = (String)flagsVector.get(i);

                    // imitate cpp's screwy logic
                    String reason = "";
                    String extraCrap = "";
                    int iFlag = 0;
                    if (iFlag < flags.length
                     && (flags[iFlag].equals("1")
                      || flags[iFlag].equals("2")))
                        reason = " "+flags[iFlag++];
                    if (iFlag < flags.length
                     && flags[iFlag].equals("3"))
                    {
                        extraCrap += " " + flags[iFlag++];
                        if (iFlag < flags.length
                         && flags[iFlag].equals("4"))
                            extraCrap += " " + flags[iFlag++];
                    }
                    //System.err.println("reason = \""+reason+"\"");
                    //System.err.println("extraCrap = \""+extraCrap+"\"");

                    // jam it into in
                    in.setFileName(fileNameToken.text.substring(1, fileNameToken.text.length()-1));
                    in.setLineNumber(oneBasedLineNumber-1);
                    in.extraCrap = extraCrap;

                    if (outputTokenColumnNumber > 0)
                    {
                        out.println();
                        outputTokenColumnNumber = 0;
                    }
                    out.println((commentOutLineDirectives?"// ":"")+"# "+oneBasedLineNumber+" \""+in.inFileName+"\""+reason+in.extraCrap);
                    outputLineNumber = oneBasedLineNumber-1;
                    outputTokenColumnNumber = 0;

                    AssertAlways(nextToken.type == Token.NEWLINE_UNESCAPED);
                    // don't bother outputting it, we'll output it lazily on next non-newline
                }
                else
                {
                    throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": invalid preprocessor directive "+token.text);
                }
            }
            else // not a preprocessing directive
            {
                if (token.type == Token.NEWLINE_UNESCAPED)
                {
                    // don't bother outputting it, we'll output it lazily on next non-newline
                }
                else
                {
                    // other tokens get suppressed if inside a false #if
                    if (highestTrueIfStackLevel >= ifStack.size()) // i.e. if not suppressing
                    {
                        // Make sure we're on the right line in the
                        // output.  If not, correct it
                        // by spewing newlines and/or a line number
                        // directive.



                        /*
                            we didn't spew a newline after the preceding comment..., we figured we'd do that lazily.
                            on that comment, lineNumber was something small
                            then we got a newline
                            , lineNumber for that was 17

                            0 based:
                                lineNumber 0  : preceding comment
                                ... linenumber 17 : newline
                                linenumber 18 : newline
                                linenumber 19 : next comment

                        */
                        AssertAlways(lineNumber == token.lineNumber);
                        int desiredOutputLineNumber = token.lineNumber;
                        //out.print("(outputLineNumber="+outputLineNumber+")(lineNumber="+lineNumber+")(desired="+desiredOutputLineNumber+"-0)(token.lineNumber="+token.lineNumber+")");
                        //out.print(token);
                        if (outputLineNumber != desiredOutputLineNumber)
                        {
                            if (outputTokenColumnNumber > 0)
                            {
                                //out.print("(c)");
                                out.println();
                                outputLineNumber++;
                                outputTokenColumnNumber = 0;
                            }
                            // if we're close enough, fix by just spewing newlines
                            if (desiredOutputLineNumber-outputLineNumber <= 7)
                            {
                                while (outputLineNumber < desiredOutputLineNumber)
                                {
                                    //out.print("(d)");
                                    out.println();
                                    outputLineNumber++;
                                    outputTokenColumnNumber = 0;
                                }
                            }
                            if (outputLineNumber != desiredOutputLineNumber)
                            {
                                out.println((commentOutLineDirectives?"// ":"")+"# "+(desiredOutputLineNumber+1)+" \""+in.inFileName+"\""+in.extraCrap);
                                outputLineNumber = desiredOutputLineNumber;
                                outputTokenColumnNumber = 0;
                            }
                        }

                        if (token.type == Token.COMMENT)
                        {
                            // argh! comments can contain embedded newlines
                            // which are '\n'
                            // (that's what the Reader gave us).
                            // need to turn those back into println
                            // for output.
                            int i0 = 0;
                            int i1;
                            while ((i1 = token.text.indexOf('\n', i0)) != -1)
                            {
                                out.println(token.text.substring(i0, i1));
                                outputLineNumber++;
                                outputTokenColumnNumber = 0;
                                i0 = i1+1;
                            }
                            AssertAlways(token.text.substring(0) == token.text);
                            out.print(token.text.substring(i0));
                            outputTokenColumnNumber++;
                        }
                        else
                        {
                            // I don't think any other token types can contain embedded newlines. make sure...
                            // XXX TODO: argh, I can trip this with my not-quite-right token pasting, which gets the type of the LHS but the RHS might be a comment:   "#define CAT(a,b) a##b  \n  CAT(x,/*\n\n*/)" argh!
                            AssertAlways(token.text.indexOf('\n') == -1);

                            out.print(token.text);
                            outputTokenColumnNumber++;
                        }
                    } // if not suppressing
                } // token isn't a newline
            }
        } // while next token != EOF

        // Discard pending newlines,
        // except if there was any output, print a newline.
        if (outputTokenColumnNumber > 0)
        {
            // put a newline at the end of the file anyway.
            out.println();
            outputLineNumber++;
            outputTokenColumnNumber = 0;
        }
        if (token.columnNumber > 0)
        {
            out.flush();
            System.err.println(token.fileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": warning: no newline at end of file");
        }

        if (!ifStack.empty())
        {
            Token unterminatedIfToken = (Token)ifStack.peek();
            throw new Error(unterminatedIfToken.fileName+":"+(unterminatedIfToken.lineNumber+1)+":"+(unterminatedIfToken.columnNumber+1)+": unterminated "+unterminatedIfToken.text);
        }
        AssertAlways(endifMultiplierStack.empty()); // always in sync with ifStack

        if (verboseLevel >= 1)
            System.err.println("    out filter");
    } // filter

    // TODO: make a way to test for the errors too
    private static final String testFileNamesAndContents[][] = {
        {
            "testCppGoneMad.prejava", ""
                +"// wtf? this is excerpted from /usr/include/sys/cdefs.h\n"
                +"// why is cpp putting that extra right paren there???\n"
                +"// ohh it's some out of bounds memory read I bet\n"
                +"hello from testCppGoneMad.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"/*\n"
                +"#elif __SOME_OTHER_COMPILER__\n"
                +"\n"
                +"# define __REDIRECT(name, proto, alias) name proto; \\\n"
                +"\\\n"
                +"\\\n"
                +"\\\n"
                +"        _Pragma(\"let \" #name \" = \" #alias)\n"
                +"*/\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from testCppGoneMad.prejava\n"
        },
        {
            "test00.prejava", ""
                +"hello from test00.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#define REVERSE(a,b) b,a\n"
                +"REVERSE(x,y)\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from test00.prejava\n"
        },
        {
            "test0.prejava", ""
                +"hello from test0.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#define FOO foo\n"
                +"#define BAR bar\n"
                +" /* FOO /* FOO */ FOO\n"
                +"\"/* FOO /* FOO */ foo\" expected\n"
                +"#define REVERSE(A,B) B,A\n"
                +"REVERSE(a,b)\n"
                +"REVERSE(\"(\",\")\")\n"
                +"REVERSE(\")\",\"(\")\n"
                +"REVERSE(\",\",\"(\")\n"
                +"REVERSE(\",\",\")\")\n"
                +"REVERSE(\"(\",\",\")\n"
                +"REVERSE(\")\",\",\")\n"
                +"REVERSE((a,b),c)\n"
                +"REVERSE(a,(b,c))\n"
                +"REVERSE((a,b),(c,d))\n"
                +"REVERSE((a,(b,c)),((d,e),f))\n"
                +"REVERSE(FOO,BAR)\n"
                +"REVERSE(REVERSE(a,b),REVERSE(c,d))\n"
                //+"REVERSE(a) // should be error\n" // TODO: test this
                //+"REVERSE(a,b,c) // should be error\n" // TODO: test this
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from test0.prejava\n"
        },
        {
            "test1.prejava", ""
                +"hello from test1.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#include \"macros.h\"\n"
                +"here is another line\n"
                +"#define foo \"trivialinclude.h\"\n"
                +"#include foo\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from test1.prejava\n"
        },
        {
            // this was giving errors before
            "test3.prejava", ""
                +"hello from test3.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"\n"
                +"#define FOO(a)  [a]\n"
                +"#if 0\n"
                +"FOO(x)\n"
                +"    #undef FOO\n"
                +"FOO(x)\n"
                +"#endif\n"
                +"\n"
                +" FOO(x)\n"
                +"\"[x]\" expected\n"
                +"    #undef FOO\n"
                +" FOO(x)\n"
                +"\"FOO(x)\" expected\n"
                +"\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from test3.prejava\n"
        },
        {
            "macros.h", ""
                 +"hello from macros.h\n"
                +"    file __FILE__ line __LINE__\n"
                 +"#define foo bar\n"
                 +"blah blah\n"
                 +"#include \"trivialinclude.h\"\n"
                 +"#include \"trivialinclude.h\"\n"
                 +"blah blah blah blah\n"
                +"    file __FILE__ line __LINE__\n"
                 +"goodbye from macros.h\n"
        },
        {
            "trivialinclude.h", ""
                 +"\n"
                 +"hello from trivialinclude.h\n"
                +"    file __FILE__ line __LINE__\n"
                 +"goodbye from trivialinclude.h\n"
        },
        {
            "masqueradeTest.prejava", ""
                 +"hello from masqueradeTest.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                 +"# 100 \"someoneelse.h\""
                 +"hello again from masqueradeTest.prejava\n"
                 +"#include \"moo.h\"\n"
                +"    file __FILE__ line __LINE__\n"
                 +"goodbye from masqueradeTest.prejava\n"
        },
        {
            "/dev/null", ""
        },
        {
            "assertTest.prejava", ""
                 +"hello from assertTest.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#define assert(expr) do { if (!(expr)) throw new Error(\"Assertion failed at \"+__FILE__+\"(\"+__LINE__+\"): \" + #expr + \"\"); } while (false)\n"
                +"    file __FILE__ line __LINE__\n"
                +"    assert(1+1 == 2);\n"
                +"    assert(1+1 == 1);\n"
                +"    file __FILE__ line __LINE__\n"
                 +"goodbye from assertTest.prejava\n"
        },
        {
            // stuff from cpp docs
            "tricky0.h", ""
                +"hello from tricky0.h\n"
                +"    file __FILE__ line __LINE__\n"
                +"\n"
                +"// http://gcc.gnu.org/onlinedocs/gcc-4.4.3/cpp/Misnesting.html#Misnesting\n"
                +"#define twice(x) (2*(x))\n"
                +"#define call_with_1(x) x(1)\n"
                +" call_with_1 (twice)\n"
                +"\"(2*(1))\" expected\n"
                +"\n"
                +"\n"
                +"#define AFTERX(x) X_ ## x\n"
                +"#define XAFTERX(x) AFTERX(x)\n"
                +"#define TABLESIZE 1024\n"
                +"#define BUFSIZE TABLESIZE\n"
                +"AFTERX(BUFSIZE)\n"
                +"\"X_BUFSIZE\" expected\n"
                +" XAFTERX(BUFSIZE)\n"
                +"\"X_1024\" expected (not X_TABLESIZE)\n"
                +"\n"
                +"// http://gcc.gnu.org/onlinedocs/gcc-4.4.3/cpp/Argument-Prescan.html#Argument-Prescan\n"
                +"#define foo a,b\n"
                +"#define bar(x) lose(x)\n"
                +"#define lose(x) (1 + (x))\n"
                +"bar(foo)\n"
                +"\n"
                +"#define foo(onearg)  #onearg\n"
                +" foo()\n"
                +"\"\"\"\" expected\n"
                +" foo ()\n"
                +"\"\"\"\" expected\n"
                +" foo( )\n"
                +"\"\"\"\" expected (we get this wrong, whatEVER)\n"
                +" foo ( )\n"
                +"\"\"\"\" expected (we get this wrong, whatEVER)\n"
                +"\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from tricky0.h\n"
        },
        {
            // stuff from cpp docs
            "tricky1.h", ""
                +"hello from tricky.h\n"
                +"    file __FILE__ line __LINE__\n"
                +"\n"
                +"//  http://gcc.gnu.org/onlinedocs/gcc-3.0.2/cpp_1.html\n"
                +"/\\\n"
                +"*\n"
                +"*/ # /*\n"
                +"*/ defi\\\n"
                +"ne FO\\\n"
                +"O 10\\\n"
                +"20\n"
                +" FOO\n"
                +"\"1020\" expected (WAIT a minute, it works without -C but not with -C??)\n"
                +"\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from tricky.h\n"
        },
        {
            "stringifyTest.prejava", ""
                +"hello from stringifyTest.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"//  http://gcc.gnu.org/onlinedocs/gcc-4.4.3/cpp/Stringification.html#Stringification\n"
                +"#define WARN_IF(EXP) \\\n"
                +"do { if (EXP) \\\n"
                +"        fprintf (stderr, \"Warning: \" #EXP \"\\n\"); } \\\n"
                +"while (0)\n"
                +" WARN_IF (x == 0);\n"
                +"\"do { if (x == 0) fprintf (stderr, \"Warning: \" \"x == 0\" \"\\n\"); } while (0);\" expected\n"
                +"\n"
                +"#define STRINGIFY(stuff) (#stuff)\n"
                +"STRINGIFY(p = \"foo\\n\";)\n"
                +"#undef STRINGIFY\n"
                +"#define STRINGIFY(stuff) ( # stuff )\n"
                +"STRINGIFY(p = \"foo\\n\";)\n"
                +"\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from stringifyTest.prejava\n"
        },
        {
            // just isolate what I'm debugging
            "ifTest0.prejava", ""
                +"hello from ifTest0.prejava\n"
                /*
                +"#if 0\n"
                +"    this should not be output\n"
                +"#elif 1\n"
                +"    output 24\n"
                +"#endif\n"
                +"#if 1\n"
                +"    output 25\n"
                +"#elif 0\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 29\n"
                +"#elif 0\n"
                +"    this should not be output\n"
                +"#else\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                */
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   output 1\n"
                +"            #if 0\n"
                +"                   this should not be output\n"
                +"            #elif 0\n"
                +"                   this should not be output\n"
                +"            #elif 1\n"
                +"                   output 2\n"
                +"            #elif 0\n"
                +"                   this should not be output\n"
                +"            #elif 1\n"
                +"                   this should not be output\n"
                +"            #else\n"
                +"                   this should not be output\n"
                +"            #endif\n"
                +"                   output 3\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"goodbye from ifTest0.prejava\n"
        },
        {
            // stress test the conditionals
            "ifTest.prejava", ""
                +"hello from ifTest.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"\n"
                +"\n"
                +"#ifdef __LINE__\n"
                +"    output 0\n"
                +"#endif // comment should be fine\n"
                +"\n"
                +"#ifndef __LINE__\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#ifdef NOT_DEFINED\n"
                +"    this should not be output\n"
                +"#endif // comment should be fine\n"
                +"\n"
                +"#ifndef NOT_DEFINED\n"
                +"    output 1\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#ifndef FOO\n"
                +"        output 2\n"
                +"    #define FOO(bar) something\n"
                +"        output 3\n"
                +"    #ifndef FOO\n"
                +"        this should not be output\n"
                +"    #endif\n"
                +"        output 4\n"
                +"    #ifdef FOO\n"
                +"        output 5\n"
                +"    #endif\n"
                +"        output 6\n"
                +"    #undef FOO\n"
                +"        output 7\n"
                +"    #ifndef FOO\n"
                +"        output 8\n"
                +"    #endif\n"
                +"        output 9\n"
                +"    #ifdef FOO\n"
                +"        this should not be output\n"
                +"    #endif\n"
                +"        output 10\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#define FOO 1\n"
                +"#ifdef FOO\n"
                +"        output 11\n"
                +"#endif\n"
                +"#ifndef FOO\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if FOO\n"
                +"        output 12\n"
                +"#endif\n"
                +"#if !FOO\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#undef FOO\n"
                +"#undef FOO\n"
                +"#define FOO 0\n"
                +"#ifdef FOO\n"
                +"        output 13\n"
                +"#endif\n"
                +"#ifndef FOO\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if FOO\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if !FOO\n"
                +"        output 14\n"
                +"#endif\n"
                +"\n"
                +"#define NOT(x) !(x)\n"
                +"#ifdef NOT\n"
                +"        output 15\n"
                +"#endif\n"
                +"#ifndef NOT\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if NOT(0)\n"
                +"        output 16\n"
                +"#endif\n"
                +"#if NOT(1)\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if !NOT(0)\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if !NOT(1)\n"
                +"        output 17\n"
                +"#endif\n"
                +"#if NOT(FOO) // FOO should still be 0\n"
                +"        output 18\n"
                +"#endif\n"
                +"#undef FOO\n"
                +"#define FOO (!!(1)) // should evaluate to 1\n"
                +"#if !NOT(FOO)\n"
                +"        output 19\n"
                +"#endif\n"
                +"#if NOT(FOO)\n"
                +"        this should not be output\n"
                +"#endif\n"
                +"#if !NOT(!(!FOO))\n"
                +"        output 20\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#if 1\n"
                +"    output 21\n"
                +"#endif\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"#else\n"
                +"    output 22\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 23\n"
                +"#else\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"#elif 0\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"#elif 1\n"
                +"    output 24\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 25\n"
                +"#elif 0\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 26\n"
                +"#elif 1\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"#elif 0\n"
                +"    this should not be output\n"
                +"#else\n"
                +"    output 27\n"
                +"#endif\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"#elif 1\n"
                +"    output 28\n"
                +"#else\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 29\n"
                +"#elif 0\n"
                +"    this should not be output\n"
                +"#else\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 30\n"
                +"#elif 1\n"
                +"    this should not be output\n"
                +"#else\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"    #if 0\n"
                +"        this should not be output\n"
                +"    #endif\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 0\n"
                +"    this should not be output\n"
                +"    #if 1\n"
                +"        this should not be output\n"
                +"    #endif\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 31\n"
                +"    #if 0\n"
                +"        this should not be output\n"
                +"    #endif\n"
                +"    output 32\n"
                +"#endif\n"
                +"\n"
                +"#if 1\n"
                +"    output 33\n"
                +"    #if 1\n"
                +"        output 34\n"
                +"    #endif\n"
                +"    output 35\n"
                +"#endif\n"
                +"\n"
                +"#if 0\n"
                +"                   this should not be output\n"
                +"    #if 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #else\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #endif\n"
                +"                   this should not be output\n"
                +"#elif 0\n"
                +"                   this should not be output\n"
                +"    #if 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #else\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #endif\n"
                +"                   this should not be output\n"
                +"#elif 1\n"
                +"                   output 36\n"
                +"    #if 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   output 37\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   output 38\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   output 39\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #else\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #endif\n"
                +"                   output 40\n"
                +"#elif 0\n"
                +"                   this should not be output\n"
                +"    #if 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #else\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #endif\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"#elif 1\n"
                +"                   this should not be output\n"
                +"    #if 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #else\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #endif\n"
                +"                   this should not be output\n"
                +"#else\n"
                +"                   this should not be output\n"
                +"    #if 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 0\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #elif 1\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #else\n"
                +"                   this should not be output\n"
                +"        #if 0\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #elif 0\n"
                +"                   this should not be output\n"
                +"        #elif 1\n"
                +"                   this should not be output\n"
                +"        #else\n"
                +"                   this should not be output\n"
                +"        #endif\n"
                +"                   this should not be output\n"
                +"    #endif\n"
                +"                   this should not be output\n"
                +"#endif\n"
                +"#define DEF0 defined\n"
                +"#if DEF0(__LINE__)\n"
                +"    output 41\n"
                +"#endif\n"
                +"#if !DEF0(__LINE__)\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"#define DEF1(x) defined(x)\n"
                +"#if DEF1(__LINE__)\n"
                +"    output 42\n"
                +"#endif\n"
                +"#if !DEF1(__LINE__)\n"
                +"    this should not be output\n"
                +"#endif\n"
                +"\n"
                +"output 43\n"
                +"there should have been outputs 0 through 43\n"
                +"\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from ifTest.prejava\n"
        },
        {
            "nonRecursionTest.prejava", ""
                +"hello from nonRecursionTest.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"\n"
                +"//\n"
                +"// Make sure macros are expanded lazily (i.e. not expanded during definition of a macro)\n"
                +"//\n"
                +"#define BAR BAZ\n"
                +"#define FOO BAR\n"
                +"#undef BAR\n"
                +" FOO\n"
                +"\"BAR\" expected\n"
                +"#undef FOO\n"
                +"\n"
                +"//\n"
                +"// Non-recursive things to make sure they still work\n"
                +"//\n"
                +"#define SWAP(x,y) y,x\n"
                +" SWAP(a,b)\n"
                +"\"b,a\" expected\n"
                +" SWAP(SWAP(a,b),SWAP(c,d))\n"
                +"\"d,c,b,a\" expected\n"
                +"#undef SWAP\n"
                +"\n"
                +"//\n"
                +"// Simple recursion that endless loops if implemented naively\n"
                +"//\n"
                +"#define FOO FOO\n"
                +" FOO\n"
                +"\"FOO\" expected\n"
                +"#undef FOO\n"
                +"\n"
                +"#define FOO BAR\n"
                +"#define BAR BAR\n"
                +" FOO\n"
                +"\"BAR\" expected\n"
                +"#undef FOO\n"
                +"#undef BAR\n"
                +"\n"
                +"#define FOO BAR\n"
                +"#define BAR FOO\n"
                +" FOO\n"
                +"\"FOO\" expected\n"
                +"#undef FOO\n"
                +"#undef BAR\n"
                +"\n"
                +"#define FOO BAR\n"
                +"#define BAR BAZ\n"
                +"#define BAZ FOO\n"
                +" FOO\n"
                +"\"FOO\" expected\n"
                +"#undef FOO\n"
                +"#undef BAR\n"
                +"#undef BAZ\n"
                +"\n"
                +"#define FOO BAR\n"
                +"#define BAR BAZ\n"
                +"#define BAZ BAR\n"
                +" FOO\n"
                +"\"BAR\" expected\n"
                +"#undef FOO\n"
                +"#undef BAR\n"
                +"#undef BAZ\n"
                +"\n"
                +"#define SWAP(x,y) SWAP(y,x)\n"
                +" SWAP(a,b)\n"
                +"\"SWAP(b,a)\" expected\n"
                +"#undef SWAP\n"
                +"//\n"
                +"// Recursive things that expand and crash if implemented naively\n"
                +"//\n"
                +"#define FOO FOO FOO\n"
                +" FOO\n"
                +"\"FOO FOO\" expected\n"
                +"#undef FOO\n"
                +"\n"
                +"#define FOO FOO BAR\n"
                +" FOO\n"
                +"\"FOO BAR\" expected\n"
                +"#undef FOO\n"
                +"\n"
                +"#define FOO BAR FOO\n"
                +" FOO\n"
                +"\"BAR FOO\" expected\n"
                +"#undef FOO\n"
                +"\n"
                +"#define FOO BAR FOO BAR\n"
                +" FOO\n"
                +"\"BAR FOO BAR\" expected\n"
                +"#undef FOO\n"
                +"\n"
                +"#define FOO BAR\n"
                +"#define BAR(x) BAR((x))\n"
                +" FOO(a)\n"
                +"\"BAR((a))\" expected\n"
                +"#undef FOO\n"
                +"#undef BAR\n"
                +"\n"
                +"#define SWAP0(x,y) SWAP1((y),(x))\n"
                +"#define SWAP1(x,y) SWAP0((y),(x))\n"
                +" SWAP0(a,b)\n"
                +"\"SWAP0(((a)),((b)))\" expected\n"
                +"#undef SWAP0\n"
                +"#undef SWAP1\n"
                +"\n"
                +"#define SWAP0(x,y) SWAP1((x),(y))\n"
                +"#define SWAP1(x,y) SWAP0((y),(x))\n"
                +" SWAP0(a,b)\n"
                +"\"SWAP0(((b)),((a)))\" expected\n"
                +"#undef SWAP0\n"
                +"#undef SWAP1\n"
                +"//\n"
                +"#define SWAP(x,y) SWAP(SWAP(y,x),SWAP((y)(x))\n"
                +" SWAP(a,b)\n"
                +"\"SWAP(SWAP(a,b),SWAP((a),(b))\" expected\n"
                +"#undef SWAP\n"
                +"//\n"
                +"#define S(x,y) S1(S1(y,x),(S1((y),(x))))\n"
                +"#define S1(x,y) S1(S1([y],[x]),[S1([[y]],[[x]]])\n"
                +" S(a,b)\n"
                +" S1(S1(b,a),(S1((b),(a))))\n"
                +"\"S1(S1([(S1(S1([(a)],[(b)]),[S1([[(a)]],[[(b)]]]))],[S1(S1([a],[b]),[S1([[a]],[[b]]])]),[S1([[(S1(S1([(a)],[(b)]),[S1([[(a)]],[[(b)]]]))]],[[S1(S1([a],[b]),[S1([[a]],[[b]]])]]])\" expected\n" // actually I didn't do this by thinking very hard about it, it's just what cpp produces
                +"#undef SWAP0\n"
                +"#undef SWAP1\n"
                +"//\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from nonRecursionTest.prejava\n"
        },
        {
            "tokenPastingTest.prejava", ""
                +"hello from tokenPastingTest.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#define A a\n"
                +"#define B b\n"
                +"#define C c\n"
                +"#define D d\n"
                +"#define AB YO!\n"
                +"#define ab yo!\n"
                +"#define ABCD yahoo!\n"
                +"#define CAT(x,y) x ## y\n"
                +" CAT(A,B)\n"
                +"\"yo!\" expected (why does cpp say YO! ?)\n"
                +" CAT( A , B )\n"
                +"\"yo!\" expected (why does cpp say YO! ?)\n"
                +"#undef CAT\n"
                +"#define CAT(x,y)x##y\n"
                +" CAT(A,B)\n"
                +"\"yo!\" expected (why does cpp say YO! ?)\n"
                +" CAT(A B,C D)\n"
                +"\"a bc d\" expected (why does cpp say a BC d?)\n"
                +" CAT(A B,)\n"
                +"\"a b\" expected\n"
                +"    line __LINE__\n"
                +" CAT(,C D)\n"
                +"\"c d\" expected\n"
                +"    line __LINE__\n"
                +" CAT(,   C    D   )\n"
                +"\"c d\" expected\n"
                +"    line __LINE__\n"
                +" CAT(,)\n"
                +"\"\" expected\n"
                +" CATT(CAT(A,B),CAT(C,D))\n"
                +"\"CATT(yo!,CD)\" expected\n"
                +"#define FOOBAR (x,y) moose\n"
                +" CAT(CAT(A,B)FOO,BAR(C,D))\n"
                +"\"CAT(a,b)(x,y) moose(c,d)\" expected\n"
                +" CAT(A,B)CAT(C,D)\n"
                +" CAT(A,)CAT(C,D)\n"
                +" CAT(A,B)CAT(,D)\n"
                +" CAT(,)CAT(C,D)\n"
                +" CAT(A,B)CAT(,)\n"
                +" CAT(,)CAT(,)\n"
                +" CAT(CAT(,),CAT(,))\n"
                //+"#define WEIRD(a,b) a####b\n" // should give error when used: "pasting "x" and "##" does not give a valid preprocing token
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from tokenPastingTest.prejava\n"
        },
        {
            "error0.prejava", ""
                +"hello from error0.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#include\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from error0.prejava\n"
        },
        {
            "error1.prejava", ""
                +"hello from error1.prejava\n"
                +"    file __FILE__ line __LINE__\n"
                +"#include    \n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from error1.prejava\n"
        },
        {
            "error2.prejava", ""
                +"hello from error2.prejava\n"
                +"#include\n"
        },
        {
            "error3.prejava", ""
                +"hello from error3.prejava\n"
                +"#include \n"
        },
        {
            "error4.prejava", ""
                +"hello from error4.prejava\n"
                +"#include " // unterminated line
        },
        {
            "error5.prejava", ""
                +"hello from error5.prejava\n"
                +"#include foo\n"
        },
    };
    private static FileOpener testFileOpener = new FileOpener() {
        public java.io.Reader newFileReader(String fileName, String includePath[])
            throws java.io.FileNotFoundException
        {
            // XXX TODO: use includePath
            for (int i = 0; i < testFileNamesAndContents.length; ++i)
                if (testFileNamesAndContents[i][0].equals(fileName))
                    return new java.io.StringReader(testFileNamesAndContents[i][1]);
            throw new java.io.FileNotFoundException("Couldn't find test file string for \""+escapify(fileName)+"\"");
        }
    };

    public static void test0(String inFileName)
    {
        System.out.println("in test0(\""+inFileName+"\")");
        java.io.Reader in = null;
        try
        {
            in = testFileOpener.newFileReader(inFileName);
        }
        catch (java.io.FileNotFoundException e)
        {

            System.err.println("woops! "+e);
            System.exit(1);
        }
        TokenReader tokenReader = new TokenReader(in, inFileName);
        try
        {
            Token token;
            do
            {
                token = tokenReader.readToken();
                System.out.println("    "+token);
            } while (token.type != Token.EOF);
        }
        catch (java.io.IOException e)
        {
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
        System.out.println("out test0");
    } // test0

    public static void test1(String inFileName)
    {
        System.out.println("in test1(\""+inFileName+"\")");
        System.out.println("===========================================");
        boolean commentOutLineDirectives = false;
        java.io.Reader in = null;
        try
        {
            in = testFileOpener.newFileReader(inFileName);
        }
        catch (java.io.FileNotFoundException e)
        {

            System.err.println("woops! "+e);
            System.exit(1);
        }
        String includePath[] = {};
        java.util.Hashtable macros = new java.util.Hashtable();
        java.io.PrintWriter writer = new java.io.PrintWriter(System.out);
        try
        {
            String builtinInput = "#define __LINE__ __LINE__\n" // stub, handled specially
                                + "#define __FILE__ __FILE__\n" // stub, handled specially
                                + "#define __java 1\n";
            java.io.Reader builtinFakeInputReader = new java.io.StringReader(builtinInput);
            filter(new TokenReaderWithLookahead(builtinFakeInputReader,"<built-in>",""),
                   new FileOpener(),
                   includePath,
                   writer,
                   macros,
                   new ExpressionParser(),
                   commentOutLineDirectives,
                   0); // recursionLevel
        }
        catch (java.io.IOException e)
        {
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
        try
        {
            filter(new TokenReaderWithLookahead(in, inFileName, ""),
                   testFileOpener,
                   includePath,
                   writer,
                   macros,
                   new ExpressionParser(),
                   commentOutLineDirectives,
                   0); // recursionLevel
        }
        catch (java.io.IOException e)
        {
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
        System.out.println("===========================================");
        System.out.println("out test1");
    } // test1()

    public static void main(String args[])
    {
        System.err.println("in Cpp.main");
        long t0Millis = System.currentTimeMillis();
        ExpressionParser expressionParser = new ExpressionParser();
        if (args.length == 1 && args[0].equals("-dumpTests"))
        {
            // dump the test strings into files in tmp dir
            String tmpDirName = "tmp";
            System.out.println("WARNING: creating directory "+tmpDirName+"");
            boolean created = new java.io.File(tmpDirName).mkdir();
            for (int iTestFile = 0; iTestFile < testFileNamesAndContents.length; ++iTestFile)
            {
                String fileName = testFileNamesAndContents[iTestFile][0];
                String contents = testFileNamesAndContents[iTestFile][1];
                if (fileName.startsWith("/"))
                {
                    AssertAlways(fileName.equals("/dev/null"));
                    AssertAlways(contents.equals(""));
                    continue;
                }
                String filePath = tmpDirName+'/'+fileName;
                System.out.println("    WARNING: creating file "+filePath+"");
                java.io.PrintWriter writer = null;
                try
                {
                    writer = new java.io.PrintWriter(
                             new java.io.BufferedWriter(
                             new java.io.FileWriter(filePath)));
                }
                catch (java.io.IOException e)
                {
                    System.err.println("Couldn't open "+fileName+" for writing: "+e);
                }
                writer.print(contents);
                writer.flush();
            }
            System.exit(0);
        } // -dumpTests option

        if (false)
        {
            test0("assertTest.prejava");
            //test1("assertTest.prejava");

            //test0("ifTest.prejava");
            test1("ifTest.prejava");
        }

        if (true)
        {
            // behave like actual cpp
            String inFileName = null;
            StringBuffer commandLineFakeInput = new StringBuffer();
            java.util.Vector includePathVector = new java.util.Vector();
            java.util.Hashtable macros = new java.util.Hashtable();
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
                    commandLineFakeInput.append("#define "+name+" "+value+"\n");
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
                    commandLineFakeInput.append("#undef "+name+"\n");
                }
                else if (arg.startsWith("-x"))
                {
                    String dir;
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
                    if (inFileName != null)
                    {
                        System.err.println("javacpp: too many input files");
                        System.exit(1);
                    }
                    inFileName = arg;
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

            String includePath[] = (String[])includePathVector.toArray(new String[0]);


            java.io.PrintWriter writer = new java.io.PrintWriter(System.out);

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
                reader = new java.io.InputStreamReader(System.in);
                inFileName = "<stdin>";
            }

            // For some reason the real cpp does this at the beginning
            // before the built-ins and command line... so we do it too
            writer.println((commentOutLineDirectives?"// ":"")+"# 1 \""+inFileName+"\"");

            try
            {
                String builtinInput = "#define __LINE__ __LINE__\n" // stub, handled specially
                                    + "#define __FILE__ __FILE__\n"; // stub, handled specially
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
                filter(new TokenReaderWithLookahead(builtinFakeInputReader, "<built-in>", ""),
                       new FileOpener(),
                       includePath,
                       writer,
                       macros,
                       expressionParser,
                       commentOutLineDirectives,
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
                java.io.Reader commandLineFakeInputReader = new java.io.StringReader(commandLineFakeInput.toString());
                filter(new TokenReaderWithLookahead(commandLineFakeInputReader, "<command line>", ""),
                       new FileOpener(),
                       includePath,
                       writer,
                       macros,
                       expressionParser,
                       commentOutLineDirectives,
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
                filter(new TokenReaderWithLookahead(reader, inFileName, ""),
                       new FileOpener(),
                       includePath,
                       writer,
                       macros,
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
        } // behave like actual cpp

        long t1Millis = System.currentTimeMillis();
        double totalSeconds = (t1Millis-t0Millis)*1e-3;
        System.err.println("    "+totalSeconds+" seconds");
        System.err.println("out Cpp.main");
        System.exit(0);
    } // main

} // Cpp
