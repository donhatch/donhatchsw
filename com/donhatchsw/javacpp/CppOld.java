/**
*
* WARNING: WORK IN PROGRESS, NOTHING WORKS HERE!
*
* This class implements cpp (the C preprocessor),
* actually a subset of its functionality.
*
* Understands the following directives:
*       #define
*       #include "filename"  (not <filename>)
* and the following command-line options:
*       -D
*       -I
*       -C (ignores this option, never strips comments anyway)
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
        public static final int NEWLINE_UNESCAPED = 9;
        public static final int NEWLINE_ESCAPED = 10;
        public static final int PREPROCESSOR_DIRECTIVE = 11;
        public static final int MACRO_ARG = 12;
        public static final int EOF = 13;
        public static final int NUMTYPES = 14; // one more than last value
        // TODO: long
        // TODO: absorb backslash-newline into spaces

        public int type;
        public String text;
        public String fileName;
        public int lineNumber; // 0 based
        public int columnNumber; // 0 based

        public Token(int type, String text, int lineNumber, int columnNumber)
        {
            this.type = type;
            this.text = text;
            this.fileName = null; // XXX ?
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
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
        public Macro(int numParams, Token[] contents)
        {
            this.numParams = numParams;
            this.contents = contents;
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
        private boolean returnedEOF;
        private StringBuffer scratch = new StringBuffer();

        public TokenReader(java.io.Reader in)
        {
            this.reader = new LineAndColumnNumberReaderWithLookahead(in);
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
            if (c == -1)
            {
                returnedEOF = true;
                return new Token(Token.EOF, "", lineNumber, columnNumber);
            }
            // TODO: order these in order of likelihood?
            if (c == '\n')
            {
                scratch.append((char)c);
                return new Token(Token.NEWLINE_UNESCAPED, scratch.toString(), lineNumber, columnNumber);
            }
            else if (c == '\\' && reader.peek()  == '\n')
            {
                scratch.append((char)c);
                scratch.append(reader.read());
                return new Token(Token.NEWLINE_ESCAPED, scratch.toString(), lineNumber, columnNumber);
            }
            else if (Character.isWhitespace((char)c))
            {
                scratch.append((char)c);
                int d; // TODO: do this for most of the reader.peek()s in here, for speed
                while ((d = reader.peek()) != -1
                    && d != '\n'
                    && Character.isWhitespace((char)d))
                    scratch.append((char)reader.read());
                return new Token(Token.SPACES, scratch.toString(), lineNumber, columnNumber);
            }
            else if (Character.isJavaIdentifierStart((char)c))
            {
                scratch.append((char)c);
                while (reader.peek() != -1
                    && Character.isJavaIdentifierPart((char)reader.peek()))
                    scratch.append((char)reader.read());
                return new Token(Token.IDENTIFIER, scratch.toString(), lineNumber, columnNumber);
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
                    return new Token(Token.FLOAT_LITERAL, scratch.toString(), lineNumber, columnNumber);
                }

                // Okay, what have we got?
                String string = scratch.toString();
                if (string.indexOf('.') != -1
                 || string.indexOf('e') != -1
                 || string.indexOf('E') != -1)
                    return new Token(Token.DOUBLE_LITERAL, string, lineNumber, columnNumber);
                else
                    return new Token(Token.INT_LITERAL, string, lineNumber, columnNumber);
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
                return new Token(quoteChar=='"' ? Token.STRING_LITERAL : Token.CHAR_LITERAL, scratch.toString(), lineNumber, columnNumber);
            }
            else if (c == '/' && reader.peek() == '/')
            {
                scratch.append((char)c);             // the first '/'
                scratch.append((char)reader.read()); // the second '/'
                // Read rest of line, including end of line
                while (true)
                {
                    c = reader.read();
                    if (c == -1)
                    {
                        // XXX warn unterminated line?
                        break;
                    }
                    scratch.append((char)c);
                    if (c == '\n')
                        break;
                }
                return new Token(Token.COMMENT, scratch.toString(), lineNumber, columnNumber);
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
                return new Token(Token.COMMENT, scratch.toString(), lineNumber, columnNumber);
            }
            else if (c == '#')
            {
                // TODO: this should really only be done at the beginning of a line, but I think # is an illegal token in java anyway so it shouldn't hurt to do it anywhere
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
                return new Token(Token.PREPROCESSOR_DIRECTIVE, scratch.toString(), lineNumber, columnNumber);
            }
            else
            {
                scratch.append((char)c);
                return new Token(Token.SYMBOL, scratch.toString(), lineNumber, columnNumber);
            }
        } // readToken
    } // private static class TokenReader

    // Guaranteed to return non-null,
    // but peeking past EOF is an error.
    private static class TokenReaderWithLookahead
    {
        private TokenReader tokenReader;
        private java.util.LinkedList lookAheadBuffer = new java.util.LinkedList();

        public boolean hasLookahead()
        {
            return !lookAheadBuffer.isEmpty();
        }

        public TokenReaderWithLookahead(java.io.Reader in)
        {
            this.tokenReader = new TokenReader(in);
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
            //System.out.println("    TokenReaderWithLookahead returning ("+(lookedahead ? "lookedahead" : "nolookedahead")+"): "+token);
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

    // XXX TODO: substituted line and column numbers aren't right
    private static Token readTokenWithMacroSubstitution(TokenReaderWithLookahead in,
                                                        String inFileName,
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
            if (macro.numParams == -1)
            {
                in.pushBackTokens(macro.contents);
                continue;
            }
            else
            {
                // discard spaces
                while (in.peekToken(0).type == Token.SPACES)
                    in.readToken();
                Token shouldBeLeftParen = in.readToken();
                if (!(shouldBeLeftParen.type == Token.SYMBOL
                  && shouldBeLeftParen.text.equals("(")))
                    throw new Error(inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": invocation of macro "+token.text+" not followed by arg list");
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
                            throw new Error(inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": EOF in middle of arg list for macro "+token.text+"");
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
                                        throw new Error(inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro \""+token.text+"\" requires "+macro.numParams+" arguments, but only "+(iArg+1)+" given");
                                    else
                                        throw new Error(inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": macro \""+token.text+"\" requires "+macro.numParams+" arguments, but more given");
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

                if (true)
                {
                    System.out.println("    macro "+token.text+" contents:");
                    for (int iContent = 0; iContent < macro.contents.length; ++iContent)
                    {
                        System.out.println("        "+iContent+": "+macro.contents[iContent]);
                    }

                    System.out.println("    "+args.length+" args for macro "+token.text);
                    for (int iArg = 0; iArg < args.length; ++iArg)
                    {
                        System.out.println("        "+iArg+": "+args[iArg].length+" tokens");
                        for (int j = 0; j < args[iArg].length; ++j)
                            System.out.println("            "+args[iArg][j]);

                    }
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
                            resultsVector.add(args[iArg][j]);
                    }
                    else
                        resultsVector.add(contentToken);
                }
                in.pushBackTokens(resultsVector);
            }
        }
    } // readTokenWithMacroSubstitution
    public static void filter(TokenReaderWithLookahead in,
                              String inFileName,
                              FileOpener fileOpener,
                              java.io.PrintWriter out,
                              java.util.Hashtable macros) // gets updated as we go

        throws java.io.IOException
    {

        java.util.Stack tokenReaderStack = new java.util.Stack();
        java.util.Stack inFileNameStack = new java.util.Stack();
        boolean needToPrintLineNumber = true;

        while (true)
        {
            // XXX TODO: this will be wrong if the next token is from a macro substitution and/or there is lookahed... get this straight
            int lineNumber = in.peekToken(0).lineNumber;
            int columnNumber = in.peekToken(0).columnNumber;

            // XXX TODO: argh, should NOT honor stuff like #define INCLUDE #include, I mistakenly thought I should honor it. but should be able to substitute for the filename though
            Token token = readTokenWithMacroSubstitution(in, inFileName, macros);
            if (token.type == Token.EOF)
            {
                if (!tokenReaderStack.isEmpty())
                {
                    // discard the EOF token, and pop the reader stack
                    in = (TokenReaderWithLookahead)tokenReaderStack.pop();
                    inFileName = (String)inFileNameStack.pop();
                    needToPrintLineNumber = true;
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

            if (token.type == Token.PREPROCESSOR_DIRECTIVE)
            {
                AssertAlways(token.text.startsWith("#"));
                if (token.text.equals("#include"))
                {
                    if (in.peekToken(0).type == Token.SPACES)
                    {
                        Token spaces = in.readToken();
                        if (spaces.text.indexOf('\n') != -1)
                        {
                            throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\"");
                        }
                    }
                    Token fileNameToken = readTokenWithMacroSubstitution(in, inFileName, macros);
                    if (fileNameToken.type == Token.EOF)
                        throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\"");
                    if (fileNameToken.type != Token.STRING_LITERAL)
                        throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\"");

                    String newInFileName = fileNameToken.text.substring(1, fileNameToken.text.length()-1);
                    TokenReaderWithLookahead newIn = null;
                    try
                    {
                        newIn = new TokenReaderWithLookahead(
                                fileOpener.newFileReader(newInFileName));
                    }
                    catch (java.io.FileNotFoundException e)
                    {

                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": \""+newInFileName+"\": No such file or directory");
                    }

                    if (in.hasLookahead())
                    {
                        // TODO: test this
                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": extra stuff confusing the #include "+newInFileName);
                    }
                    tokenReaderStack.push(in);
                    inFileNameStack.push(inFileName);

                    in = newIn;
                    inFileName = newInFileName;

                    needToPrintLineNumber = true;

                }
                else if (token.text.equals("#define"))
                {
                    Token nextToken = in.readToken();
                    if (nextToken.type == Token.EOF
                     || (nextToken.type == Token.SPACES && nextToken.text.indexOf('\n') != -1))
                    {
                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": no macro name given in #define directive");
                    }
                    if (nextToken.type != Token.SPACES)
                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": macro names must be identifiers");
                    nextToken = in.readToken();
                    if (nextToken.type == Token.EOF)
                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": no macro name given in #define directive");
                    if (nextToken.type != Token.IDENTIFIER)
                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": macro names must be identifiers");
                    String macroName = nextToken.text;

                    // must be either whitespace or left paren after macro name
                    nextToken = in.readToken();
                    if (nextToken.type == Token.EOF)
                        throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": no newline at end of file"); // in cpp it's a warning but we don't tolerate it

                    if (nextToken.type == Token.SYMBOL
                     && nextToken.text.equals("("))
                    {
                        // There's a macro param list.
                        java.util.Vector paramNamesVector = new java.util.Vector();

                        // Discard spaces
                        if (in.peekToken(0).type == Token.SPACES
                         && in.peekToken(0).text.indexOf('\n') == -1)
                            in.readToken();

                        if (in.peekToken(0).type == Token.SYMBOL
                         && in.peekToken(0).text.equals(")"))
                        {
                            // zero params
                            in.readToken();
                        }
                        else
                        {
                            // must be one or more param names,
                            // separated by commas,
                            // followed by close paren

                            nextToken = in.readToken();

                            // move past spaces
                            if (nextToken.type == Token.SPACES
                             && nextToken.text.indexOf('\n') == -1)
                                nextToken = in.readToken();


                            if (nextToken.type != Token.IDENTIFIER)
                                throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever

                            paramNamesVector.add(nextToken.text);

                            while (true)
                            {
                                nextToken = in.readToken();

                                // move past spaces
                                if (nextToken.type == Token.SPACES
                                 && nextToken.text.indexOf('\n') == -1)
                                    nextToken = in.readToken();

                                if (nextToken.type == Token.SYMBOL)
                                {
                                    if (nextToken.text.equals(")"))
                                        break;
                                    else if (nextToken.text.equals(","))
                                    {
                                        nextToken = in.readToken();

                                        // move past spaces
                                        if (nextToken.type == Token.SPACES
                                         && nextToken.text.indexOf('\n') == -1)
                                            nextToken = in.readToken();

                                        if (nextToken.type == Token.IDENTIFIER)
                                        {
                                            paramNamesVector.add(nextToken.text);
                                            continue;
                                        }
                                    }
                                }
                                throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever
                            }
                        }

                        String paramNames[] = (String[])paramNamesVector.toArray(new String[0]);

                        AssertAlways(nextToken.type == Token.SYMBOL
                                  && nextToken.text.equals(")"));

                        if (in.peekToken(0).type == Token.SPACES
                         && in.peekToken(0).text.indexOf('\n') == -1)
                        {
                            AssertAlways(false);
                            /*
                            ...
                            think think think
                            */
                        }
                    }
                    else
                    {
                        // There's no macro param list.
                        // Must be spaces.
                        if (nextToken.type != Token.SPACES)
                            throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": malformed parameter list for macro "+macroName+""); // cpp gives lots of different kind of errors but whatever

                        AssertAlways(false);
                        /*
                        ...
                        think think think
                        */
                    }



                    if (nextToken.type == Token.SPACES) // XXX needed?  had this before we made the above stuff
                    {
                        if (nextToken.text.indexOf('\n') != -1)
                        {
                            System.out.println();
                            System.out.print(nextToken.text.substring(nextToken.text.indexOf('\n')+1)); // TODO: turn \n into println, probably turn it into a separate token, I think
                            macros.put(macroName,
                                       new Macro(0, new Token[0]));
                        }
                        else
                        {
                            nextToken = in.readToken();
                            if (nextToken.type == Token.EOF)
                                throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": no newline at end of file"); // in cpp it's a warning but we don't tolerate it
                            java.util.Vector contentsVector = new java.util.Vector();
                            AssertAlways(nextToken.type != Token.SPACES);
                            contentsVector.add(nextToken);
                            while (true)
                            {
                                nextToken = in.readToken();
                                if (nextToken.type == Token.EOF)
                                    throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": no newline at end of file"); // in cpp it's a warning but we don't tolerate it
                                if (nextToken.type == Token.COMMENT)
                                    throw new Error(inFileName+":"+(token.columnNumber+1)+":"+(token.columnNumber+1)+": can't handle comment on line of #define yet"); // XXX TODO: handle it
                                if (nextToken.type == Token.SPACES
                                 && nextToken.text.indexOf('\n') != -1)
                                    break;
                                contentsVector.add(nextToken);
                            }
                            System.out.println();
                            System.out.print(nextToken.text.substring(nextToken.text.indexOf('\n')+1)); // TODO: turn \n into println, probably turn it into a separate token, I think

                            Token contents[] = new Token[contentsVector.size()];
                            for (int i = 0; i < contents.length; ++i)
                                contents[i] = (Token)contentsVector.get(i);
                            macros.put(macroName,
                                       new Macro(-1, // numParams
                                                 contents));

                        }
                    }


                    if (true)
                    {
                        // special case for now:
                        // #define REVERSE(A,B) B,A\n"
                        /*Token*/ nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SPACES);
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.IDENTIFIER && nextToken.text.equals("REVERSE"));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SYMBOL && nextToken.text.equals("("));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.IDENTIFIER && nextToken.text.equals("A"));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SYMBOL && nextToken.text.equals(","));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.IDENTIFIER && nextToken.text.equals("B"));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SYMBOL && nextToken.text.equals(")"));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SPACES);
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.IDENTIFIER && nextToken.text.equals("B"));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SYMBOL && nextToken.text.equals(","));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.IDENTIFIER && nextToken.text.equals("A"));
                        nextToken = in.readToken();
                        AssertAlways(nextToken.type == Token.SPACES);
                        Macro macro = new Macro(2, // numParams,
                            new Token[] {
                                new Token(Token.MACRO_ARG, "", 1, -1), // arg 1: B
                                new Token(Token.SYMBOL, ",", -1, -1), // arg 1: B
                                new Token(Token.MACRO_ARG, "", 0, -1), // arg 0: A
                            });
                        macros.put("REVERSE", macro);
                    }
                }
                else
                {
                    throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": invalid preprocessor directive "+token.text);
                }
            }
            else
            {
                if (needToPrintLineNumber)
                {
                    // XXX TODO: is the line number right, or should it be +1 or what?
                    out.println("# "+(lineNumber+1)+" \""+inFileName+"\"");
                    needToPrintLineNumber = false;
                }
                out.print(token.text); // XXX TODO: turn \n into println... probably make \n be a separate token, I think
            }
        }
        out.flush();
    } // filter

    // TODO: make a way to test for the errors too
    private static final String testFileNamesAndContents[][] = {
        {
            "test0.java", ""
                +"hello from test0.java\n"
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
                //+"REVERSE(a) // should be error\n" // TODO: test this
                //+"REVERSE(a,b,c) // should be error\n" // TODO: test this
                +"goodbye from test0.java\n"
        },
        {
            "test1.java", ""
                +"hello from test1.java\n"
                +"#include \"macros.h\"\n"
                +"here is another line\n"
                +"#define foo \"trivialinclude.h\"\n"
                +"#include foo\n"
                +"goodbye from test1.java\n"
        },
        {
            "macros.h", ""
                 +"hello from macros.h\n"
                 +"#define foo bar\n"
                 +"blah blah\n"
                 +"#include \"trivialinclude.h\"\n"
                 +"blah blah blah\n"
                 +"goodbye from macros.h\n"
        },
        {
            "trivialinclude.h", ""
                 +"hello from trivialinclude.h\n"
                 +"goodbye from trivialinclude.h\n"
        },
        {
            "masqueradeTest.h", ""
                 +"hello from masqueradeTest.h\n"
                 +"# 100 \"someoneelse.h\""
                 +"hello again from masqueradeTest.h\n"
                 +"#include \"moo.h\"\n"
                 +"goodbye from masqueradeTest.h\n"
        },
        {
            "/dev/null", ""
        },
        {
            // XXX hmm this doesn't work in real xpp, don't know what I was thinking
            "tricky.h", ""
                +"#define COMMA ,"
                +"#define LPAREN ("
                +"#define RPAREN )"
                +"#define REVERSE(A,B) B A"
                +"REVERSE LPAREN x COMMA y RPAREN"
        },
        {
            "error0.java", ""
                +"hello from error0.java\n"
                +"#include\n"
                +"goodbye from error0.java\n"
        },
        {
            "error1.java", ""
                +"hello from error1.java\n"
                +"#include    \n"
                +"goodbye from error1.java\n"
        },
        {
            "error2.java", ""
                +"hello from error2.java\n"
                +"#include\n"
        },
        {
            "error3.java", ""
                +"hello from error3.java\n"
                +"#include \n"
        },
        {
            "error4.java", ""
                +"hello from error4.java\n"
                +"#include " // unterminated line
        },
        {
            "error5.java", ""
                +"hello from error5.java\n"
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

    public static void test0()
    {
        String inFileName = "test0.java";
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
        TokenReader tokenReader = new TokenReader(in);
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
    } // test0

    public static void test1()
    {
        String inFileName = "test0.java";
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
        java.util.Hashtable macros = new java.util.Hashtable();
        try
        {
            filter(new TokenReaderWithLookahead(in),
                   inFileName,
                   testFileOpener,
                   new java.io.PrintWriter(System.out),
                   macros);
        }
        catch (java.io.IOException e)
        {
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
    } // test1()

    public static void main(String args[])
    {
        test0();
        test1();

        if (false) // real program might look like this
        {
            java.util.Hashtable macros = new java.util.Hashtable();
            try
            {
                filter(new TokenReaderWithLookahead(new java.io.InputStreamReader(System.in)),
                       "<stdin>",
                       new FileOpener(),
                       new java.io.PrintWriter(System.out),
                       macros);
            }
            catch (java.io.IOException e)
            {
                System.err.println("Well damn: "+e);
                System.exit(1);
            }
        }
    } // main

} // Cpp
