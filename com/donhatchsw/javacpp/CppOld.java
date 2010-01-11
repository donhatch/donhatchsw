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
    private static void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

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
        }

        public int read()
            throws java.io.IOException // since lineNumberReader.read() does
        {
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
            return c;
        }
        // Return what read() will return next.
        public int peek()
            throws java.io.IOException
        {
            if (!lookedAhead)
            {
                lookedAheadChar = read();
                lookedAhead = true;
            }
            return lookedAheadChar;
        }
        public int getLineNumber()
        {
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
        public static final int IDENTIFIER = 0;
        public static final int STRING_LITERAL = 1;
        public static final int CHAR_LITERAL = 2;
        public static final int INT_LITERAL = 3;
        public static final int FLOAT_LITERAL = 4;
        public static final int DOUBLE_LITERAL = 5;
        public static final int SYMBOL = 6;
        public static final int COMMENT = 7;
        public static final int SPACES = 8;
        public static final int PREPROCESSOR_DIRECTIVE = 9;
        public static final int MACRO_ARG = 10;
        public static final int NUMTYPES = 11;
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
            Assert(type >= 0 && type < NUMTYPES);
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
                                Assert(typeToNameCache[value] == null);
                                typeToNameCache[value] = field.getName();
                            }
                        }
                    }
                }
                for (int i = 0; i < NUMTYPES; ++i)
                    Assert(typeToNameCache[i] != null);
            }
            Assert(typeToNameCache[type] != null);
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

    static class Macro
    {
        // Doesn't include the name
        int numParams; // -1 means no parens even
        Token[] contents; // args denoted by token type MACRO_ARG with line number containing the index of the argument to be substituted
    } // static class Macro


    /**
    * This class turns a Reader into a reader
    * that reads a token at a time.
    */
    private static class TokenReader
    {
        public LineAndColumnNumberReaderWithLookahead reader;
        StringBuffer scratch = new StringBuffer();

        public TokenReader(java.io.Reader in)
        {
            this.reader = new LineAndColumnNumberReaderWithLookahead(in);
        }
        public Token readToken()
            throws java.io.IOException // since reader.read() does
        {
            // clear scratch...
            scratch.delete(0, scratch.length());

            // grab the line number and column number at the beginning of the token
            int lineNumber = reader.getLineNumber();
            int columnNumber = reader.getColumnNumber();

            int c = reader.read();
            if (c == -1)
                return null;
            if (Character.isWhitespace((char)c))
            {
                scratch.append((char)c);
                int d; // TODO: do this for most of the reader.peek()s in here, for speed
                while ((d = reader.peek()) != -1
                    && Character.isWhitespace((char)d))
                    scratch.append((char)reader.read());
                // XXX uh oh, "\\\n" should be included,
                // but does it need two chars of lookahead??
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
                    Assert(c != -1); // by guard above
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
                    Assert(c == '.'); // by guard above
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
            if (!lookAheadBuffer.isEmpty())
                return (Token)lookAheadBuffer.removeFirst();
            return tokenReader.readToken();
        }
        public Token peekToken(int index)
            throws java.io.IOException // since tokenReader.readToken() does
        {
            while (lookAheadBuffer.size() <= index)
            {
                Token token = tokenReader.readToken();
                if (token == null)
                    return null;
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
            if (token == null)
                return null;
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
                while (in.peekToken(0) != null
                    && in.peekToken(0).type == Token.SPACES)
                    in.readToken();
                Token shouldBeLeftParen = in.readToken();
                if (shouldBeLeftParen == null
                 || shouldBeLeftParen.type != Token.SYMBOL
                 || !shouldBeLeftParen.text.equals("("))
                    throw new Error(inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": invocation of macro "+token.text+" not followed by arg list");
                // each arg will be a list of tokens
                Token args[][] = new Token[macro.numParams][];
                for (int iArg = 0; iArg < macro.numParams; ++iArg)
                {
                    java.util.Vector thisArgTokensVector = new java.util.Vector();
                    // read tokens up to a comma (if not last arg)
                    // or right paren (if last arg)
                    int parenLevel = 1;
                    while (true)
                    {
                        Token anotherToken = in.readToken();
                        if (anotherToken == null)
                            throw new Error(inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": EOF in middle of arg list for macro "+token.text+"");
                        // TODO: this isn't quite right for throwing a coherent error if wrong number of args
                        if (parenLevel > 1)
                        {
                            if (anotherToken.type == Token.SYMBOL
                             && anotherToken.text.equals(")"))
                                parenLevel--;
                        }
                        else // parenLevel == 1
                        {
                            if (anotherToken.type == Token.SYMBOL
                             && anotherToken.text.equals(iArg==macro.numParams-1 ? ")" : ","))
                                break; // without appending
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
            int lineNumber = (in.peekToken(0) != null ? in.peekToken(0).lineNumber : -1);
            int columnNumber = (in.peekToken(0) != null ? in.peekToken(0).columnNumber : -1);

            // XXX TODO: argh, should NOT honor stuff like #define INCLUDE #include, I mistakenly thought I should honor it. but should be able to substitute for the filename though
            Token token = readTokenWithMacroSubstitution(in, inFileName, macros);
            if (token == null
             && !tokenReaderStack.isEmpty())
            {
                in = (TokenReaderWithLookahead)tokenReaderStack.pop();
                inFileName = (String)inFileNameStack.pop();
                needToPrintLineNumber = true;
                continue; // still need to read a token
            }

            if (token == null)
                break;

            if (false)
            {
                out.flush();
                System.out.println("    "+token);
                System.out.flush();
            }

            if (token.type == Token.PREPROCESSOR_DIRECTIVE)
            {
                Assert(token.text.startsWith("#"));
                if (token.text.equals("#include"))
                {
                    if (in.peekToken(0) != null
                     && in.peekToken(0).type == Token.SPACES)
                    {
                        Token spaces = in.readToken();
                        if (spaces.text.indexOf('\n') != -1)
                        {
                            throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": #include expects \"FILENAME\"");
                        }
                    }
                    Token fileNameToken = readTokenWithMacroSubstitution(in, inFileName, macros);
                    if (fileNameToken == null)
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
                    // special case for now:
                    // #define REVERSE(A,B) B,A\n"
                    {
                        Token nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SPACES);
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.IDENTIFIER && nextToken.text.equals("REVERSE"));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SYMBOL && nextToken.text.equals("("));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.IDENTIFIER && nextToken.text.equals("A"));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SYMBOL && nextToken.text.equals(","));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.IDENTIFIER && nextToken.text.equals("B"));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SYMBOL && nextToken.text.equals(")"));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SPACES);
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.IDENTIFIER && nextToken.text.equals("B"));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SYMBOL && nextToken.text.equals(","));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.IDENTIFIER && nextToken.text.equals("A"));
                        nextToken = in.readToken();
                        Assert(nextToken != null && nextToken.type == Token.SPACES);
                    }
                    Macro macro = new Macro();
                    macro.numParams = 2;
                    macro.contents = new Token[] {
                        new Token(Token.MACRO_ARG, "", 1, -1), // arg 1: B
                        new Token(Token.SYMBOL, ",", -1, -1), // arg 1: B
                        new Token(Token.MACRO_ARG, "", 0, -1), // arg 0: A
                    };
                    macros.put("REVERSE", macro);
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

    /*
    // XXX TODO: defunct
    public static void filter(java.io.LineNumberReader in, java.io.PrintWriter out)
        throws java.io.IOException
    {
        java.util.Stack inStack = new java.util.Stack();
        java.util.Stack inFileNameStack = new java.util.Stack();

        String inFileName = "<stdin>"; // XXX TODO: what to do here?
        boolean needToPrintLineNumber = true;
        while (true)
        {
            String line = in.readLine();
            if (line == null)
            {
                if (!inStack.empty())
                {
                    in = (java.io.LineNumberReader)inStack.pop();
                    inFileName = (String)inFileNameStack.pop();
                    needToPrintLineNumber = true;
                    continue;
                }
                break;
            }
            int lineNumber = in.getLineNumber();

            while (line.endsWith("\\"))
            {
                String moreLine = in.readLine();
                if (moreLine == null)
                {
                    System.err.println(inFileName+":"+lineNumber+": warning: backslash-newline at end of file");
                    break;
                }
                line += moreLine;
            }

            if (false)
            {
                out.println("    Input line "+lineNumber+": "+line);
                out.flush();
            }

            if (needToPrintLineNumber)
            {
                out.println("# "+lineNumber+" \""+inFileName+"\"");
                needToPrintLineNumber = false;
            }

            if (line.startsWith("#define ")) // XXX TODO: more lenient
            {
                // TODO: define the macro
                out.println(""); // XXX multiple lines if input was multiple lines
            }
            else if (line.startsWith("#include \"")
             && line.endsWith("\"")) // XXX TODO: more lenient
            {
                int i0 = 10; // XXX TODO: more lenient
                int i1 = line.length()-1;
                String newInFileName = line.substring(i0, i1);
                java.io.LineNumberReader newIn = null;
                try
                {
                    newIn = new java.io.LineNumberReader(
                            new java.io.FileReader(newInFileName));
                }
                catch (java.io.FileNotFoundException e)
                {

                    System.err.println(inFileName+":"+lineNumber+": \""+newInFileName+"\": No such file or directory");
                    System.exit(1);
                }

                inStack.push(in);
                inFileNameStack.push(inFileName);

                in = newIn;
                inFileName = newInFileName;

                needToPrintLineNumber = true;
            }
            // TODO: # by itself is allowed
            else if (line.startsWith("#"))
            {
                System.err.println(inFileName+":"+lineNumber+": invalid preprocessing directive "+line);
            }
            else
                out.println(line);
        }
    } // Cpp ctor
    */

    public static void test()
    {
        final String testFileNamesAndContents[][] = {
            {
                "test0.java", ""
                    +"hello from test0.java\n"
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
                    //+"REVERSE(a) // should be error\n"
                    +"REVERSE(a,b,c) // should be error\n"
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
                     +"#include trivialinclude.h\n"
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
        FileOpener fileOpener = new FileOpener() {
            public java.io.Reader newFileReader(String fileName)
                throws java.io.FileNotFoundException
            {
                for (int i = 0; i < testFileNamesAndContents.length; ++i)
                    if (testFileNamesAndContents[i][0].equals(fileName))
                        return new java.io.StringReader(testFileNamesAndContents[i][1]);
                throw new java.io.FileNotFoundException("Couldn't find test file string for \""+escapify(fileName)+"\"");
            }
        };
        String inFileName = "test0.java";
        java.io.Reader in = null;
        try
        {
            in = fileOpener.newFileReader(inFileName);
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
                   fileOpener,
                   new java.io.PrintWriter(System.out),
                   macros);
        }
        catch (java.io.IOException e)
        {
            System.err.println("Well damn: "+e);
            System.exit(1);
        }
    }

    public static void main(String args[])
    {
        test();

        if (false)
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
    }

} // Cpp
