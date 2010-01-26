// From an old reentrant no-memory-allocations
// C expression parser I had lying around...

package com.donhatchsw.javacpp;

public class ExpressionParser
{
    // Logical assertions, always compiled in. Ungracefully bail if violated.
    private static void AssertAlways(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    abstract static class Operator
    {
        public int assoc; // LEFT or RIGHT
        public int prec; // precedence (higher number means higher precedence)
        public String name;
        public Operator(int assoc, int prec, String name)
        {
            this.assoc = assoc;
            this.prec = prec;
            this.name = name;
        }
    }
    abstract static class UnaryOperator extends Operator
    {
        abstract public double fun(double x);
        public UnaryOperator(int assoc, int prec, String name)
        {
            super(assoc, prec, name);
        }
    }
    abstract static class BinaryOperator extends Operator
    {
        abstract public double fun(double x, double y);
        public BinaryOperator(int assoc, int prec, String name)
        {
            super(assoc, prec, name);
        }
    }

    private static UnaryOperator unops[] = {
        new UnaryOperator(RIGHT, 15, "~") {public double fun(double x) { return ~(long)x; }},
        new UnaryOperator(RIGHT, 15, "!") {public double fun(double x) { return (x==0 ? 1 : 0); }},
        new UnaryOperator(RIGHT, 15, "-") {public double fun(double x) { return -x; }},
    };
    private static BinaryOperator binops[] = {
        new BinaryOperator(RIGHT, 14, "**") {public double fun(double x, double y) { return Math.pow(x, y); }},
        new BinaryOperator(LEFT,  13, "*")  {public double fun(double x, double y) { return x * y; }},
        new BinaryOperator(LEFT,  13, "/")  {public double fun(double x, double y) { return x / y; }}, // XXX hmm, this could result in wrong answers if these are supposed to be integer expressions
        new BinaryOperator(LEFT,  13, "%")  {public double fun(double x, double y) { return x % y; }},
        new BinaryOperator(LEFT,  12, "+")  {public double fun(double x, double y) { return x + y; }},
        new BinaryOperator(LEFT,  12, "-")  {public double fun(double x, double y) { return x - y; }},
        new BinaryOperator(LEFT,  11, "<<") {public double fun(double x, double y) { return (long)x << (long)y; }},
        new BinaryOperator(LEFT,  11, ">>") {public double fun(double x, double y) { return (long)x >> (long)y; }},
        new BinaryOperator(LEFT,  10, "<=") {public double fun(double x, double y) { return (x <= y) ? 1 : 0; }},
        new BinaryOperator(LEFT,  10, ">=") {public double fun(double x, double y) { return (x >= y) ? 1 : 0; }},
        new BinaryOperator(LEFT,  10, "<")  {public double fun(double x, double y) { return (x < y) ? 1 : 0; }},
        new BinaryOperator(LEFT,  10, ">")  {public double fun(double x, double y) { return (x > y) ? 1 : 0; }},
        new BinaryOperator(LEFT,   9, "==") {public double fun(double x, double y) { return x == y? 1 : 0; }},
        new BinaryOperator(LEFT,   9, "!=") {public double fun(double x, double y) { return x == y? 1 : 0; }}, // must come before factorial "!"
        new BinaryOperator(LEFT,   5, "&&") {public double fun(double x, double y) { return x!=0 && y!=0 ? 1 : 0; }},       // must come before "&"
        new BinaryOperator(LEFT,   4, "||") {public double fun(double x, double y) { return x!=0 || y!=0 ? 1 : 0; }},        // must come before "|"
        new BinaryOperator(LEFT,   8, "&")  {public double fun(double x, double y) { return (long)x & (long)y; }},
        new BinaryOperator(LEFT,   7, "^")  {public double fun(double x, double y) { return (long)x ^ (long)y; }},
        new BinaryOperator(LEFT,   6, "|")  {public double fun(double x, double y) { return (long)x | (long)y; }},
        new BinaryOperator(RIGHT,  3, "?")  {public double fun(double x, double y) { throw new Error(); }},    // special case in parse(), the function never gets called
        new BinaryOperator(LEFT,   1, ",")  {public double fun(double x, double y) { return y; }},
        new BinaryOperator(LEFT,  16, "!")  {public double fun(double x, double dummy) { return x>0 ? x*fun(x-1,0) : 1; }},     // factorial-- special case in expr_parse(), there's no RHS
    };

    private ZeroOverheadStringReader reader = new ZeroOverheadStringReader();
    private static class ZeroOverheadStringReader
    {
        private String s = null;
        private int sLength = 0;
        private int pos = -1;
        public ZeroOverheadStringReader()
        {
            // nothing
        }
        public final void init(String s)
        {
            this.s = s;
            this.sLength = s.length();
            this.pos = 0;
        }
        public final int tell()
        {
            return pos;
        }
        public final void seek(int pos)
        {
            this.pos = pos;
        }
        public final int peek()
        {
            return pos == sLength ? -1 : s.charAt(pos); // throws indexing error if out of bounds or reading past EOF
        }
        public final void advance()
        {
            pos++;
        }
        public final int getchar()
        {
            int c = peek();
            advance();
            return c;
        }
        public final void discardSpaces()
        {
            int c;
            while ((c = peek()) != -1 && Character.isWhitespace((char)c))
                advance();
        }
    } // ZeroOverheadStringReader


    private static boolean getLiteral(ZeroOverheadStringReader reader,
                                      String s)
    {
        int pos = reader.tell();
        reader.discardSpaces();
        int sLength = s.length();
        for (int i = 0; i < sLength; ++i)
            if (reader.getchar() != s.charAt(i))
            {
                reader.seek(pos);
                return false; // failure
            }
        return true; // success
    }

    private static Operator getOp(ZeroOverheadStringReader reader,
                                 Operator ops[],
                                 int lowestPrecAllowed)
    {
        int pos = reader.tell();
        for (int iOp = 0; iOp < ops.length; ++iOp)
        {
            if (getLiteral(reader, ops[iOp].name))
            {
                if (ops[iOp].prec >= lowestPrecAllowed)
                    return ops[iOp];
                else
                {
                    // Put it back and don't continue;
                    // e.g. if && is on the input
                    // but its precedence is too low to be recognized,
                    // we want to leave the thole thing on the input
                    // rather than reading the '&'.
                    reader.seek(pos);
                    return null;
                }
            }
        }
        return null;
    }

    private static boolean isHexDigit(char c)
    {
        return (c >= '0' && c <= '9')
            || (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z');
    }
    /* ascii hex character to value */
    private static int ctoa(char c)
    {
        if ('0' < c && c <= '9') return c - '0';
        if ('a' < c && c <= 'z') return 10 + c - 'a';
        if ('A' < c && c <= 'Z') return 10 + c - 'A';
        return 0;
    }

    // throws on failure
    private static double getConstant(ZeroOverheadStringReader reader)
    {
        int base = 10;
        boolean negate = false;

        int pos = reader.tell();

        reader.discardSpaces();

        int c;
        if ((c = reader.peek()) == -1)
        {
            throw new RuntimeException("unexpected end-of-expression trying to read constant");
        }

        if (c == '-')
        {
            negate = true;
            if ((c = reader.peek()) == -1)
            {
                reader.seek(pos);
                throw new RuntimeException("unexpected end-of-expression trying to read constant");
            }
        }

        if (Character.isDigit((char)c) || c == '.')
        {
            if (c == '0')
                base = 8;
            double returnVal = 0;
            while ((c = reader.peek()) != -1
                && (isHexDigit((char)c) || c == 'x' || c == 'b'))
            {
                if (c == 'x')
                    base = 16;
                else if (c == 'b')
                    base = 2;
                else
                    returnVal = returnVal * base + ctoa((char)c);
                reader.advance();
            }
            if (reader.peek() == '.')
            {
                reader.advance();
                double scale = 1;
                while ((c = reader.peek()) != -1 && Character.isDigit((char)c))
                {
                    scale /= base;
                    returnVal += scale * ctoa((char)c);
                    reader.advance();
                }
            }
            // TODO if I ever care: exponent!
            if (negate)
                returnVal = -returnVal;
            return returnVal;
        }
        else
            throw new RuntimeException("expression parse error trying to read constant at position "+reader.tell());
    } // getConstant


    // throws on failure
    private double parse(ZeroOverheadStringReader reader,
                         int lowestPrecAllowed,
                         boolean evaluate)

    {
        double returnVal = 0.;

        UnaryOperator unop;
        if ((unop = (UnaryOperator)getOp(reader, unops, lowestPrecAllowed)) != null)
        {
            // expr -> unop expr
            returnVal = parse(reader, unop.prec, evaluate);
            if (evaluate)
                returnVal = unop.fun(returnVal);
        }
        else if (getLiteral(reader, "("))
        {
            // expr -> '(' expr ')'
            returnVal = parse(reader, 0, evaluate);
            if (!getLiteral(reader, ")"))
            {
                // XXX TODO: define the kind of exception we're going to throw, be able to return the index in it
                throw new RuntimeException(reader.peek() == -1
                    ? "unexpected end-of-expression"
                    : "syntax error near '"+(char)reader.peek()+"'");
            }
        }
        else
            returnVal = getConstant(reader); // throws on failure

        BinaryOperator binop;
        while ((binop = (BinaryOperator)getOp(reader, binops, lowestPrecAllowed)) != null)
        {
            double RHS = Double.NaN;
            if (binop.name.equals("!"))
            {
                // '!' in this context is the right-unary factorial operator,
                // in which case no RHS is needed...
            }
            else if (binop.name.equals("?"))
            {
                double ifTrue = parse(reader, binop.prec,
                                      evaluate && returnVal!=0);
                if (!getLiteral(reader, ":"))
                    throw new RuntimeException(reader.peek() == -1
                        ? "unexpected end-of-expression"
                        : "syntax error near '"+(char)reader.peek()+"'");
                double ifFalse = parse(reader, binop.prec,
                                       evaluate && returnVal==0);
                RHS = (returnVal!=0) ? ifTrue : ifFalse;
            }
            else
            {
                RHS = parse(reader, binop.assoc == RIGHT ? binop.prec
                                                         : binop.prec+1,
                        evaluate
                     && !binop.name.equals(returnVal!=0 ? "||" : "&&"));
            }
            if (evaluate)
            {
                if (binop.name.equals("/") && RHS == 0)
                    throw new RuntimeException("divide by zero");
                if (binop.name.equals("%") && RHS == 0)
                    throw new RuntimeException("mod by zero");
                returnVal = binop.fun(returnVal, RHS);
            }
        }
        return returnVal;
    } // parse

    public double evaluate(String s)
    {
        reader.init(s);
        return parse(reader, 0, true);
    }

    /** little test program */
    public static void main(String args[])
    {
        if (args.length != 1)
            System.err.println("Usage: ExpressionParser \"<expression>\"");
        String s = args[0];
        ExpressionParser parser = new ExpressionParser();
        double value = parser.evaluate(s);
        System.out.println(value);
    } // main


} // ExpressionParser

