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
    private static class Token
    {
        // The token types
        public final static int IDENTIFIER = 0;
        public final static int STRING_LITERAL = 1;
        public final static int CHAR_LITERAL = 2;
        public final static int FLOAT_LITERAL = 3;
        public final static int DOUBLE_LITERAL = 4;
        public final static int SYMBOL = 5;
        public final static int SPACES = 6;

        public int type;
        public String text;
        public String inFile;
        public int inFileLine; // 1 based
        public int inFileLineChar; // 1 based

        public Token(int type, String text)
        {
            this.type = type;
            this.text = text;
        }
    } // private class Token

    // Java has a PushBackReader and a LineNumberReader,
    // but if I want both capabilities, I seem to be screwed.
    // So implement it here (just the methods I need,
    // including getColumnNumber().
    private class PushBackLineNumberReader()
    {
        private java.io.PushBackReader pushBackReader;
        // To avoid dismal performance, caller should make sure
        // that reader is either a BufferedReader or has one as an ancestor.
        public PushBackLineNumberReader(java.io.Reader reader)
        {
            this.pushBackReader = new java.io.PushBackReader(reader);
        }
        // Get the current line number, starting at 0.
        // The line number is advanced after each line terminator
        // ("\n" or "\r" or "\r\n") is read.
        public int getLineNumber()
        {
            ...
        }
        // Get the current column within the line, starting at 0
        public int getColumnNumber()
        {
        }
        // Read a single char, or -1 on EOF.
        // A line terminator is compressed into a single '\n'.
        public int read()
        {
            int c = pushBackReader.read();
            if (c == -1)
                return c;
            else if (c == '\n')
            {
                lineNumber++;
                columnNumber = 0;
                return '\n';
            }
            else if (c == '\r')
            {
                // absorb an optional following '\n'
                int d = pushBackReader.read();
                if (d != -1 && d != '\n')
                    pushBackReader.unread(d);
                lineNumber++;
                columnNumber = 0;
                return '\n';
            }
            else
            {
                columnNumber++;
                return c;
            }
        }
        public void unread(int c)
        {
            // Make sure c is an actual char, no pushing back EOF
            if ((int)(char)c != c)
                throw new Error(); // XXX more specific
            // And make sure it's something read() could have returned, not '\r'
            if (c == '\r')
                throw new Error(); // XXX more specific
            //XXX argh! how to find out the column???
        }
    } // PushBackLineNumberReader

    /**
    * This class turns a BufferedReader into a reader
    * that reads a token at a time.
    */
    private static class TokenReader
    {
        public java.io.BufferedReader reader;
        StringBuffer lookAheadBuffer = new StringBuffer();

        //-----------------------------------------------
        // XXX maybe should make a class to encapsulate a reader with unlimited pushback and lookahead?
        // For internal use
        private char readChar()
        {
            int lookAheadBufferLength = lookAheadBuffer.length();
            if (lookAheadBufferLength > 0)
            {
                char c = lookAheadBuffer.charAt(lookAheadBufferLength-1);
                lookAheadBuffer.deleteCharAt(lookAheadBufferLength-1);
                return c;
            }
            return '\0';
        }
        // For internal use
        private void pushBackChar(char c)
        {
            lookAheadBuffer.append(c);
        }
        // For internal use
        private char peekChar()
        {
            char c = readChar();
            pushBackChar(c);
            return c;
        }
        //-----------------------------------------------------------


        public TokenReader(java.io.BufferedReader reader)
        {
            this.reader = reader;
        }
        public Token readToken()
        {

            return null;
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
        java.io.LineNumberReader in = new java.io.LineNumberReader(
                                      new java.io.InputStreamReader(
                                      System.in));
        java.io.PrintWriter out = new java.io.PrintWriter(System.out);
        try
        {
            Cpp.filter(in, out);
            out.flush();
        }
        catch (java.io.IOException e)
        {
            throw new Error("caught IOException "+e);
        }
    }
} // Cpp
