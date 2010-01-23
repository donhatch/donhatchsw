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
*       #if
#       #endif
* and the following command-line options:
*       -I
*       -D
*       -U
*       -C (ignores this option, never strips comments anyway)
*

TODO:
    - #if (I guess that means integer expressions, but can start with just 1, 0, and empty)
    - -I
    - understand <> around file names as well as ""'s?  maybe not worth the trouble
    - ##
    - omit blank lines at end of files like cpp does
    - turn more than 7 consecutive blank lines into just a line number directive like cpp does
    - after return from include, don't emit that blank line (and adjust line number accordingly), like cpp does
    - understand # line numbers and file number on input (masquerade)
    - put "In file included from whatever:3:" or whatever in warnings and errors
    - handle escaped newlines like cpp does -- really as nothing, i.e. can be in the middle of a token or string-- it omits it.  also need to emit proper number of newlines to sync up
    - get the right filename in the "unterminated if" message
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

    private static class DebugLineNumberReader extends java.io.LineNumberReader
    {
        public DebugLineNumberReader(java.io.Reader reader)
        {
            super(reader);
        }
        public int read()
            throws java.io.IOException
        {
            System.out.println("        before read(): line "+super.getLineNumber());
            int c = super.read();
            System.out.println("        read() returning '"+escapify((char)c,'\'')+"'");
            System.out.println("        after read(): line "+super.getLineNumber());
            return c;
        }
        public int getLineNumber()
        {
            System.out.println("        getLineNumber() returning "+super.getLineNumber());
            return super.getLineNumber();
        }
    }

    // Line number reader with 1 char of lookahead.
    // Tells the column number as well as the line number.
    // Only implemented the methods I need.
    private static class LineAndColumnNumberReaderWithLookahead
    {
        private java.io.LineNumberReader lineNumberReader;
        private int lineNumber = 0;
        private int columnNumber = 0;
        private boolean lookedAhead = false;
        private int lookedAheadChar;

        // To avoid dismal performance, caller should make sure
        // that reader is either a BufferedReader or has one as an ancestor.
        public LineAndColumnNumberReaderWithLookahead(java.io.Reader reader)
        {
            this.lineNumberReader = new java.io.LineNumberReader(reader);
            //this.lineNumberReader = new DebugLineNumberReader(reader);
            AssertAlways(this.lineNumberReader.getLineNumber() == 0);
        }

        public int read()
            throws java.io.IOException // since lineNumberReader.read() does
        {
            //System.out.println("            with lookahead before read(): line "+lineNumber);
            //System.out.println("                ("+(lookedAhead?"":"not ")+"looking ahead)");
            int c = lookedAhead ? lookedAheadChar : lineNumberReader.read();
            lookedAhead = false;
            if (c != -1)
            {
                // since lookedAhead is false,
                // lineNumberReader has correct line number
                int newLineNumber = lineNumberReader.getLineNumber();
                if (newLineNumber != lineNumber)
                    columnNumber = 0;
                else
                    columnNumber++;
                lineNumber = newLineNumber;
            }
            //System.out.println("            with lookahead read() returning '"+escapify((char)c,'\'')+"'");
            //System.out.println("            with lookahead after read(): line "+lineNumber);
            return c;
        }
        // Return what read() will return next.
        public int peek()
            throws java.io.IOException
        {
            if (!lookedAhead)
            {
                lookedAheadChar = lineNumberReader.read();
                lookedAhead = true;
            }
            return lookedAheadChar;
        }
        public int getLineNumber()
        {
            //System.out.println("            with lookahead getLineNumber returning "+lineNumber);
            return lineNumber;
        }
        public int getColumnNumber()
        {
            return columnNumber;
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
        public static final int NEWLINE_ESCAPED = 10; // XXX TODO: decide whether this should be included in SPACES, I think it might simplify some things
        public static final int PREPROCESSOR_DIRECTIVE = 11;
        public static final int MACRO_ARG = 12;
        public static final int MACRO_ARG_QUOTED = 13;
        public static final int EOF = 14;
        public static final int NUMTYPES = 15; // one more than last value
        // TODO: long
        // TODO: absorb backslash-newline into spaces

        public int type;
        public String text;
        public String fileName;
        public int lineNumber; // 0 based
        public int columnNumber; // 0 based

        public Token(int type, String text, String fileName, int lineNumber, int columnNumber)
        {
            this.type = type;
            this.text = text;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }
        // copy constructor but changing file name ane line number
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
                  +this.fileName
                  +", "
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

            // grab the line number and column number at the beginning of the token
            int lineNumber = reader.getLineNumber();
            int columnNumber = reader.getColumnNumber();

            int c = reader.read();
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
            else if (c == '\\' && reader.peek()  == '\n')
            {
                scratch.append((char)c);
                scratch.append(reader.read());
                token = new Token(Token.NEWLINE_ESCAPED, scratch.toString(), fileName, lineNumber, columnNumber);
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
                    scratch.append((char)reader.read());
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
            else
            {
                scratch.append((char)c);
                token = new Token(Token.SYMBOL, scratch.toString(), fileName, lineNumber, columnNumber);
            }
            //System.out.println("            TokenReader  returning "+token);
            return token;
        } // readToken
    } // private static class TokenReader

    // Guaranteed to return non-null,
    // but peeking past EOF is an error.
    private static class TokenReaderWithLookahead
    {
        private TokenReader tokenReader;
        String inFileName;
        private java.util.LinkedList lookAheadBuffer = new java.util.LinkedList();

        public boolean hasLookahead()
        {
            return !lookAheadBuffer.isEmpty();
        }

        public TokenReaderWithLookahead(java.io.Reader in, String inFileName)
        {
            this.tokenReader = new TokenReader(in, inFileName);
            this.inFileName = inFileName; // maybe not necessary, just provide an accessor to tokenReader's?
        }

        public Token readToken()
            throws java.io.IOException // since tokenReader.readToken() does
        {
            Token token;
            boolean lookedahead;
            if (!lookAheadBuffer.isEmpty())
            {
                token = (Token)lookAheadBuffer.removeFirst();
                lookedahead = true;
            }
            else
            {
                token = tokenReader.readToken();
                lookedahead = false;
            }
            //System.out.println("            TokenReaderWithLookahead returning ("+(lookedahead ? "lookedahead" : "nolookedahead")+"): "+token);
            return token;
        }
        public Token peekToken(int index)
            throws java.io.IOException // since tokenReader.readToken() does
        {
            while (lookAheadBuffer.size() <= index)
            {
                Token token = tokenReader.readToken();
                lookAheadBuffer.add(token);
            }
            return (Token)lookAheadBuffer.get(index);
        }
        public void pushBackToken(Token token)
        {
            lookAheadBuffer.add(0, token);
        }
        public void pushBackTokens(Token tokens[])
        {
            for (int i = tokens.length-1; i >= 0; --i)
                lookAheadBuffer.add(0, tokens[i]);

        }
        public void pushBackTokens(java.util.Vector tokensVector)
        {
            for (int i = tokensVector.size()-1; i >= 0; --i)
                lookAheadBuffer.add(0, (Token)tokensVector.get(i));

        }
    } // private static class TokenReaderWithLookahead

    private static Token readTokenWithMacroSubstitution(TokenReaderWithLookahead in,
                                                        int lineNumber,
                                                        java.util.Hashtable macros)
        throws java.io.IOException
    {
        while (true)
        {
            Token token = in.readToken();
            if (token.type != Token.IDENTIFIER)
                return token;
            Macro macro = (Macro)macros.get(token.text);
            if (macro == null)
                return token;
            if (macro.numParams == -1) // if it's an invocation of a simple macro without an arg list
            {
                // special cases...
                if (token.text.equals("__LINE__"))
                    in.pushBackToken(new Token(Token.INT_LITERAL,  ""+(lineNumber+1), in.inFileName, lineNumber, -1));
                else if (token.text.equals("__FILE__"))
                    in.pushBackToken(new Token(Token.STRING_LITERAL, "\""+escapify(in.inFileName)+"\"", in.inFileName, lineNumber, -1));
                else
                {
                    /* can't just push back the tokens, we need to change the line numbers too */
                    if (false)
                        in.pushBackTokens(macro.contents);
                    else
                    {
                        Token macroContentsCopy[] = new Token[macro.contents.length];
                        for (int i = 0; i < macro.contents.length; ++i)
                            macroContentsCopy[i] = new Token(macro.contents[i], in.inFileName, lineNumber);
                        in.pushBackTokens(macroContentsCopy);
                    }
                }
                continue;
            }
            else // it's an invocation of a macro with an arg list
            {
                // move past spaces
                while (in.peekToken(0).type == Token.SPACES
                    || in.peekToken(0).type == Token.NEWLINE_ESCAPED
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
                        resultsVector.add(new Token(contentToken, in.inFileName, lineNumber));
                }
                in.pushBackTokens(resultsVector);
            }
        }
    } // readTokenWithMacroSubstitution
    public static void filter(TokenReaderWithLookahead in,
                              FileOpener fileOpener,
                              java.io.PrintWriter out,
                              java.util.Hashtable macros) // gets updated as we go

        throws java.io.IOException
    {
        int verboseLevel = 0; // 0: nothing, 1: print enter and exit function, 2: print more

        if (verboseLevel >= 1)
            System.err.println("    in filter");

        java.util.Stack tokenReaderStack = new java.util.Stack();
        java.util.Stack ifStack = new java.util.Stack(); // of #ifwhatever tokens, for the file,line,column information
        int highestTrueIfStackLevel = 0;
        

        int lineNumber = 0;
        int columnNumber = 0;
        out.println("# "+(lineNumber+1)+" \""+in.inFileName+"\"");

        while (true)
        {
            // The following assumes that every token
            // is marked with the line number of the top-level token
            // that produced it, before macro substitution.
            // That requires making a lot of new tokens on the fly whenever macros
            // are expanded, just for the line numbers, but that's how it's currently done.
            lineNumber = in.peekToken(0).lineNumber;
            columnNumber = in.peekToken(0).columnNumber;

            // XXX TODO: argh, should NOT honor stuff like #define INCLUDE #include, I mistakenly thought I should honor it. but should be able to substitute for the filename though
            Token token = readTokenWithMacroSubstitution(in, lineNumber, macros);
            if (token.type == Token.EOF)
            {
                if (!ifStack.empty())
                {
                    Token unterminatedIfToken = (Token)ifStack.peek();
                    throw new Error(unterminatedIfToken.fileName+":"+(unterminatedIfToken.lineNumber+1)+":"+(unterminatedIfToken.columnNumber+1)+": unterminated "+unterminatedIfToken.text);
                }
                if (!tokenReaderStack.isEmpty())
                {
                    // discard the EOF token, and pop the reader stack
                    in = (TokenReaderWithLookahead)tokenReaderStack.pop();

                    lineNumber = in.peekToken(0).lineNumber;
                    columnNumber = in.peekToken(0).columnNumber;
                    out.println("# "+(lineNumber+1)+" \""+in.inFileName+"\" 2"); // cpp puts a 2 there, don't know why but imitating it

                    continue; // still need to read a token
                }
                else // EOF at top level
                    break;
            }

            if (false)
            {
                out.flush();
                System.out.println("    "+token);
                System.out.flush();
            }

            // when inside a false #if,
            // the only preprocessor directives we recognize are #if* and #endif
            if (token.type == Token.PREPROCESSOR_DIRECTIVE
             && (ifStack.size() <= highestTrueIfStackLevel || token.text.startsWith("#if")
                                                           || token.text.equals("#endif")))
            {
                AssertAlways(token.text.startsWith("#"));
                if (token.text.equals("#endif"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found #endif");

                    if (ifStack.empty())
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #endif without #if");
                    ifStack.pop();
                }
                else if (token.text.equals("#ifdef")
                      || token.text.equals("#ifndef")
                      || token.text.equals("#undef"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found "+token.text);

                    Token nextToken = in.readToken();

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.NEWLINE_ESCAPED
                        || nextToken.type == Token.COMMENT)
                        nextToken = in.readToken();

                    if (nextToken.type == Token.EOF
                     || nextToken.type == Token.NEWLINE_UNESCAPED)
                    {
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": no macro name given in "+token.text+" directive");
                    }

                    if (nextToken.type != Token.IDENTIFIER)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro names must be identifiers");
                    String macroName = nextToken.text;
                    nextToken = in.readToken();

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.NEWLINE_ESCAPED
                        || nextToken.type == Token.COMMENT)
                        nextToken = in.readToken();


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
                                highestTrueIfStackLevel = ifStack.size()+1;
                            else
                                highestTrueIfStackLevel = ifStack.size();
                        }
                        ifStack.push(token);
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
                    out.print(nextToken.text);
                }
                else if (token.text.equals("#define"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found #define");

                    // we'll be doing a lot of lookahead of one token,
                    // so use a local variable nextToken
                    Token nextToken = in.readToken();

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.NEWLINE_ESCAPED
                        || nextToken.type == Token.COMMENT)
                        nextToken = in.readToken();

                    if (nextToken.type == Token.EOF
                     || nextToken.type == Token.NEWLINE_UNESCAPED)
                    {
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": no macro name given in #define directive");
                    }

                    if (nextToken.type != Token.IDENTIFIER)
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro names must be identifiers");
                    String macroName = nextToken.text;

                    // must be either whitespace or left paren after macro name... it makes a difference
                    nextToken = in.readToken();
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

                        nextToken = in.readToken();

                        // move past spaces
                        while (nextToken.type == Token.SPACES
                            || nextToken.type == Token.NEWLINE_ESCAPED
                            || nextToken.type == Token.COMMENT)
                            nextToken = in.readToken();

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
                            nextToken = in.readToken();

                            while (true)
                            {
                                // move past spaces
                                while (nextToken.type == Token.SPACES
                                    || nextToken.type == Token.NEWLINE_ESCAPED
                                    || nextToken.type == Token.COMMENT)

                                    nextToken = in.readToken();

                                if (nextToken.type == Token.SYMBOL)
                                {
                                    if (nextToken.text.equals(")"))
                                        break;
                                    else if (nextToken.text.equals(","))
                                    {
                                        nextToken = in.readToken();

                                        // move past spaces
                                        while (nextToken.type == Token.SPACES
                                            || nextToken.type == Token.NEWLINE_ESCAPED)
                                            nextToken = in.readToken();

                                        if (nextToken.type == Token.IDENTIFIER)
                                        {
                                            paramNamesVector.add(nextToken.text);
                                            nextToken = in.readToken();
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
                        nextToken = in.readToken();

                        paramNames = (String[])paramNamesVector.toArray(new String[0]);
                    }
                    else if (nextToken.type == Token.SPACES
                          || nextToken.type == Token.NEWLINE_ESCAPED
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
                                for (int i = 0; i < paramNames.length; ++i)
                                    if (paramNameMaybe.equals(paramNames[i]))
                                    {
                                        nextToken = new Token(Token.MACRO_ARG_QUOTED, "", null, i, -1); // smuggle in param index through line number
                                        break;
                                    }
                                // if not found, it's an error
                                if (nextToken.type == Token.PREPROCESSOR_DIRECTIVE)
                                {
                                    System.out.println(nextToken);
                                    throw new Error(in.inFileName+":"+(nextToken.lineNumber+1)+":"+(nextToken.columnNumber+1)+": '#' is not followed by a macro parameter");
                                }
                            }
                        }

                        contentsVector.add(nextToken);
                        nextToken = in.readToken();
                    }
                    Token contents[] = new Token[contentsVector.size()];
                    for (int i = 0; i < contents.length; ++i)
                        contents[i] = (Token)contentsVector.get(i);
                    {
                        // in place, compress all consecutive comments and spaces
                        // into a single space, and
                        // discard spaces and comments at the beginning.
                        int nOut = 0;
                        for (int iIn = 0; iIn < contentsVector.size(); ++iIn)
                        {
                            if (contents[iIn].type == Token.SPACES
                             || contents[iIn].type == Token.NEWLINE_ESCAPED
                             || contents[iIn].type == Token.COMMENT)
                            {
                                if (nOut != 0
                                 && contents[nOut-1].type != Token.SPACES)
                                    contents[nOut++] = new Token(Token.SPACES, " ", contents[iIn].fileName, contents[iIn].lineNumber, contents[iIn].columnNumber);
                            }
                            else
                                contents[nOut++] = contents[iIn];
                        }
                        // and discard spaces and comments at the end too
                        if (nOut > 0
                         && contents[nOut-1].type == Token.SPACES)
                            nOut--;
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
                        // TODO: The real cpp doesn't complain if the new definition is exactly the same as the old one.  do we care?? it's sloppy programming anyway
                        System.err.println(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": warning: \""+macroName+"\" redefined");
                        System.err.println(previousMacro.inFileName+":"+(previousMacro.lineNumber+1)+":"+(previousMacro.columnNumber+1)+": warning: this is the location of the previous definition");
                    }

                    macros.put(macroName, macro);

                    if (nextToken.type == Token.EOF)
                    {
                        in.pushBackToken(nextToken);
                        continue;
                    }
                    AssertAlways(nextToken.type == Token.NEWLINE_UNESCAPED);
                    out.print(nextToken.text);
                }
                else if (token.text.equals("#include"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found #include");
                    while (in.peekToken(0).type == Token.SPACES
                        || in.peekToken(0).type == Token.NEWLINE_ESCAPED
                        || in.peekToken(0).type == Token.COMMENT)
                        in.readToken();
                    Token fileNameToken = readTokenWithMacroSubstitution(in, lineNumber, macros);
                    if (fileNameToken.type != Token.STRING_LITERAL)
                        throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\"");

                    String newInFileName = fileNameToken.text.substring(1, fileNameToken.text.length()-1);
                    TokenReaderWithLookahead newIn = null;
                    try
                    {
                        newIn = new TokenReaderWithLookahead(
                                fileOpener.newFileReader(newInFileName),
                                newInFileName);
                    }
                    catch (java.io.FileNotFoundException e)
                    {

                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": \""+newInFileName+"\": No such file or directory");
                    }

                    if (in.hasLookahead()) // uh oh, I'm afraid this will be triggered when the file is a result of simple macro substition like #define FOO "/dev/null" and then #include FOO since 1 char of lookahead was required to detect the end of the FOO token
                    {
                        // TODO: test this
                        throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": extra stuff confusing the #include "+newInFileName);
                    }
                    tokenReaderStack.push(in);

                    in = newIn;

                    lineNumber = in.peekToken(0).lineNumber;
                    columnNumber = in.peekToken(0).columnNumber;
                    out.println("# "+(lineNumber+1)+" \""+in.inFileName+"\" 1"); // cpp puts a 1 there, don't know why but imitating it
                }
                else
                {
                    throw new Error(in.inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": invalid preprocessor directive "+token.text);
                }
            }
            else
            {
                if (token.type == Token.NEWLINE_UNESCAPED)
                {
                    // print newlines whether or not inside a false #if
                    out.println();
                }
                else
                {
                    // other tokens get suppressed if inside a false #if
                    if (highestTrueIfStackLevel >= ifStack.size())
                        out.print(token.text);
                }
            }
        }
        out.flush();
        if (verboseLevel >= 1)
            System.err.println("    out filter");
    } // filter

    // TODO: make a way to test for the errors too
    private static final String testFileNamesAndContents[][] = {
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
            "masqueradeTest.h", ""
                 +"hello from masqueradeTest.h\n"
                +"    file __FILE__ line __LINE__\n"
                 +"# 100 \"someoneelse.h\""
                 +"hello again from masqueradeTest.h\n"
                 +"#include \"moo.h\"\n"
                +"    file __FILE__ line __LINE__\n"
                 +"goodbye from masqueradeTest.h\n"
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
            // XXX hmm this doesn't work in real xpp, don't know what I was thinking
            "tricky.h", ""
                +"hello from tricky.h\n"
                +"    file __FILE__ line __LINE__\n"
                +"#define COMMA ,"
                +"#define LPAREN ("
                +"#define RPAREN )"
                +"#define REVERSE(A,B) B A"
                +"REVERSE LPAREN x COMMA y RPAREN"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from tricky.h\n"
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
        public java.io.Reader newFileReader(String fileName)
            throws java.io.FileNotFoundException
        {
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
            filter(new TokenReaderWithLookahead(builtinFakeInputReader,"<built-in>"),
                   new FileOpener(),
                   writer,
                   macros);
        }
        catch (java.io.IOException e)
        {
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
        try
        {
            filter(new TokenReaderWithLookahead(in, inFileName),
                   testFileOpener,
                   writer,
                   macros);
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
        if (false)
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
        }

        if (false)
        {
            test0("assertTest.prejava");
            test1("assertTest.prejava");
        }

        if (true)
        {
            String inFileName = null;
            StringBuffer commandLineFakeInput = new StringBuffer();
            String includePath[] = {};
            java.util.Hashtable macros = new java.util.Hashtable();

            for (int iArg = 0; iArg < args.length; ++iArg)
            {
                String arg = args[iArg];
                if (arg.startsWith("-I"))
                {
                    AssertAlways(false); // XXX implement me
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

            java.io.PrintWriter writer = new java.io.PrintWriter(System.out);


            try
            {
                String builtinInput = "#define __LINE__ __LINE__\n" // stub, handled specially
                                    + "#define __FILE__ __FILE__\n" // stub, handled specially
                                    + "#define __java 1\n";
                java.io.Reader builtinFakeInputReader = new java.io.StringReader(builtinInput);
                filter(new TokenReaderWithLookahead(builtinFakeInputReader, "<built-in>"),
                       new FileOpener(),
                       writer,
                       macros);
            }
            catch (java.io.IOException e)
            {
                System.err.println("Well damn: "+e);
                System.exit(1);
            }
            try
            {
                java.io.Reader commandLineFakeInputReader = new java.io.StringReader(commandLineFakeInput.toString());
                filter(new TokenReaderWithLookahead(commandLineFakeInputReader, "<command line>"),
                       new FileOpener(),
                       writer,
                       macros);
            }
            catch (java.io.IOException e)
            {
                System.err.println("Well damn: "+e);
                System.exit(1);
            }


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
                reader = new java.io.InputStreamReader(System.in);
            try
            {
                filter(new TokenReaderWithLookahead(reader, "<stdin>"),
                       new FileOpener(),
                       writer,
                       macros);
            }
            catch (java.io.IOException e)
            {
                System.err.println("Well damn: "+e);
                System.exit(1);
            }
        }

        System.exit(0);
    } // main

} // Cpp
