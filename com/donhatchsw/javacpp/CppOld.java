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
            if (c == '\007') return "\\a";
            if (c == '\033') return "\\e";
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
    } // LineAndColumnNumberReaderWithLookahead

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
        public static final int NUMTYPES = 9;
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
    } // private class Token

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
                return new Token(Token.SPACES, scratch.toString(), lineNumber, columnNumber);
            }
            else if (Character.isJavaIdentifierStart((char)c))
            {
                scratch.append((char)c);
                while (reader.peek() != -1
                    && Character.isJavaIdentifierStart((char)reader.peek()))
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
            else
            {
                scratch.append((char)c);
                return new Token(Token.SYMBOL, scratch.toString(), lineNumber, columnNumber);
            }
        } // readToken
    } // private static class TokenReader

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

    public static void main(String args[])
    {
        TokenReader tokenReader = new TokenReader(new java.io.InputStreamReader(
                                                  System.in));
        try
        {
            Token token;
            while ((token = tokenReader.readToken()) != null)
            {
                System.out.println("   "+token.toString());
            }
        }
        catch (java.io.IOException e)
        {
            System.out.println("Well damn: "+e);
        }
    }
} // Cpp
