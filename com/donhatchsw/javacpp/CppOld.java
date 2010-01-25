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
*       -C (ignores this option, never strips comments anyway)
*

TODO:
    - integer expressions (currently does trivial ones)
    - make it not endless loop on #define foo foo and that sort of thing?
    - -I
    - understand <> around file names as well as ""'s?  maybe not worth the trouble
    - ##  (concatenates tokens)
    - omit blank lines at end of files like cpp does
    - turn more than 7 consecutive blank lines into just a line number directive like cpp does
    - after return from include, don't emit that blank line (and adjust line number accordingly), like cpp does
    - understand # line numbers and file number on input (masquerade)
    - put "In file included from whatever:3:" or whatever in warnings and errors
    - handle escaped newlines like cpp does -- really as nothing, i.e. can be in the middle of a token or string-- it omits it.  also need to emit proper number of newlines to sync up
    - make sure line numbers in sync in all cases
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

    private static int evaluateIntExpression(String expr, String inFileName, int lineNumber, int columnNumber)
    {
        String originalExpr = expr;
        //System.err.println("    in evaluateIntExpression(\""+escapify(originalExpr)+"\")");

        // for now, only handle really trivial things

        StringBuffer sb = new StringBuffer();

        // get rid of any whitespace...
        {
            sb.delete(0, sb.length());
            for (int i = 0; i < expr.length(); ++i)
                if (!Character.isWhitespace(expr.charAt(i)))
                    sb.append(expr.charAt(i));
            expr = sb.toString();
        }

        // get rid of any parens, making sure they match
        {
            sb.delete(0, sb.length());
            int parenLevel = 0;
            for (int i = 0; i < expr.length(); ++i)
            {
                char c = expr.charAt(i);
                if (c == '(')
                    parenLevel++;
                else if (c == ')')
                {
                    parenLevel--;
                    if (parenLevel < 0)
                        break; // turns into error
                }
                else
                    sb.append(expr.charAt(i));
            }
            if (parenLevel != 0)
                throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": expression has unmatched parentheses");
            expr = sb.toString();
        }

        {
            boolean negate = false;
            while (expr.startsWith("!"))
            {
                negate = !negate;
                expr = expr.substring(1);
            }
            int value;
            if (expr.equals("0"))
                value = 0;
            else if (expr.equals("1"))
                value = 1;
            else throw new Error(inFileName+":"+(lineNumber+1)+":"+(columnNumber+1)+": can't understand expression \""+escapify(originalExpr)+"\"; I only understand trivial things like 0 and 1");
            if (negate)
                value = (value==0 ? 1 : 0);
            //System.err.println("    out evaluateIntExpression(\""+escapify(originalExpr)+"\"), returning "+value);
            return value;
        }
    } // evaluateIntExpression

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
                // must be followed by exactly the following:
                // '(', identifier, ')'.
                Token nextToken = in.readToken();
                if (nextToken.type != Token.SYMBOL
                 || !nextToken.text.equals("("))
                    throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": defined not followed by (identifier)");
                nextToken = in.readToken();
                if (nextToken.type != Token.IDENTIFIER)
                    throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": defined not followed by (identifier)");
                String macroName = nextToken.text;
                nextToken = in.readToken();
                if (nextToken.type != Token.SYMBOL
                 || !nextToken.text.equals(")"))
                    throw new Error(in.inFileName+":"+(token.lineNumber+1)+":"+(token.columnNumber+1)+": defined not followed by (identifier)");
                if (macros.get(macroName) != null)
                    return new Token(Token.INT_LITERAL, "1", token.fileName, token.lineNumber, token.columnNumber);
                else
                    return new Token(Token.INT_LITERAL, "0", token.fileName, token.lineNumber, token.columnNumber);
            }

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
                              java.util.Hashtable macros, // gets updated as we go
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
            out.println("# "+(lineNumber+1)+" \""+in.inFileName+"\" 1"); // cpp puts a 1 there, don't know why but imitating it
        else
            out.println("# "+(lineNumber+1)+" \""+in.inFileName+"\"");

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
            Token token = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
            if (token.type == Token.EOF)
                break;


            if (false)
            {
                out.flush();
                System.out.println("    "+token);
                System.out.flush();
            }

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
                        || nextToken.type == Token.NEWLINE_ESCAPED
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, true);

                    Token expressionStartToken = nextToken;

                    // gather rest of line (with macro substitution and defined() evaluation)
                    // into a string...
                    StringBuffer sb = new StringBuffer();
                    while (nextToken.type != Token.NEWLINE_UNESCAPED
                        && nextToken.type != Token.EOF)
                    {
                        if (nextToken.type == Token.NEWLINE_ESCAPED)
                            ; // really nothing
                        else if (nextToken.type == Token.COMMENT)
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
                    else
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
                        if (false)
                        {
                            out.print("(flush)"); // so I don't leave this in the shipped version
                            out.flush();
                        }
                        int expressionValue = evaluateIntExpression(sb.toString(),
                                                                    expressionStartToken.fileName,
                                                                    expressionStartToken.lineNumber,
                                                                    expressionStartToken.columnNumber);
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
                    out.print(nextToken.text);
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
                        || nextToken.type == Token.NEWLINE_ESCAPED
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
                    out.print(nextToken.text);
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
                        || nextToken.type == Token.NEWLINE_ESCAPED
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
                        || nextToken.type == Token.NEWLINE_ESCAPED
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
                    out.print(nextToken.text);
                }
                else if (token.text.equals("#define"))
                {
                    if (verboseLevel >= 2)
                        System.err.println("        filter: found #define");

                    // we'll be doing a lot of lookahead of one token,
                    // so use a local variable nextToken
                    Token nextToken = in.readToken(); // WITHOUT macro substitution, so we don't expand the expected macro name

                    // move past spaces
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.NEWLINE_ESCAPED
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
                    nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
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

                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                        // move past spaces
                        while (nextToken.type == Token.SPACES
                            || nextToken.type == Token.NEWLINE_ESCAPED
                            || nextToken.type == Token.COMMENT)
                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

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
                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                            while (true)
                            {
                                // move past spaces
                                while (nextToken.type == Token.SPACES
                                    || nextToken.type == Token.NEWLINE_ESCAPED
                                    || nextToken.type == Token.COMMENT)

                                    nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                                if (nextToken.type == Token.SYMBOL)
                                {
                                    if (nextToken.text.equals(")"))
                                        break;
                                    else if (nextToken.text.equals(","))
                                    {
                                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                                        // move past spaces
                                        while (nextToken.type == Token.SPACES
                                            || nextToken.type == Token.NEWLINE_ESCAPED)
                                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                                        if (nextToken.type == Token.IDENTIFIER)
                                        {
                                            paramNamesVector.add(nextToken.text);
                                            nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
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
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

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
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
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

                    Token nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);
                    while (nextToken.type == Token.SPACES
                        || nextToken.type == Token.NEWLINE_ESCAPED
                        || nextToken.type == Token.COMMENT)
                        nextToken = readTokenWithMacroSubstitution(in, lineNumber, macros, false);

                    Token fileNameToken = nextToken;
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


                    filter(newIn,
                           fileOpener,
                           out,
                           macros,
                           recursionLevel+1);
                    out.println("# "+(lineNumber+2)+" \""+in.inFileName+"\" 2"); // cpp puts a 1 there, don't know why but imitating it
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
        } // while next token != EOF

        if (!ifStack.empty())
        {
            Token unterminatedIfToken = (Token)ifStack.peek();
            throw new Error(unterminatedIfToken.fileName+":"+(unterminatedIfToken.lineNumber+1)+":"+(unterminatedIfToken.columnNumber+1)+": unterminated "+unterminatedIfToken.text);
        }
        AssertAlways(endifMultiplierStack.empty()); // always in sync with ifStack

        if (recursionLevel == 0)
            out.flush(); // do we want this?
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
            // XXX hmm this doesn't work in real cpp, don't know what I was thinking
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
                +"\"S1((b),(a)),S1([[(b)]],[[(a)]])),S1(S1([a],[b]),S1([[a]],[[b]])\" expected\n"
                +"#undef SWAP0\n"
                +"#undef SWAP1\n"
                +"//\n"
                +"    file __FILE__ line __LINE__\n"
                +"goodbye from nonRecursionTest.prejava\n"
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
                   macros,
                   0); // recursionLevel
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
                   macros,
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
        if (true)
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

        if (true)
        {
            //test0("assertTest.prejava");
            //test1("assertTest.prejava");

            //test0("ifTest.prejava");
            test1("ifTest.prejava");
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
                       macros,
                       0); // recursionLevel
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
                       macros,
                       0); // recursionLevel
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
                       macros,
                       0); // recursionLevel
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
