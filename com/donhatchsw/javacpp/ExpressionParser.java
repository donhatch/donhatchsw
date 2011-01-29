// From an old reentrant no-memory-allocations
// C expression parser I had lying around...

// TODO: support caller-supplied variables and functions
// TODO: support binary math functions (pow, atan2)

package com.donhatchsw.javacpp;

/**
* This class defines a recursive-descent C-like numeric expression parser
* with minimal overhead.
* <p>
* All C arithmetic, boolean, and conditional operators are recognized,
* as is binary ** (power) and post-unary ! (factorial).
* <p>
* The constants e and pi are recognized,
* as are the following unary functions from java.lang.Math:
* sin, cos, tan, asin, acos, atan, exp, log, sqrt, ceil, floor, abs.
* <p>
* Short-circuit evaluation is done properly in conditional and boolean operations.
* <p>
* On syntax error, a RuntimeException will be thrown, containing the index at which the error occurred in the input string.
* <p>
* Integer expressions will throw an ArithmeticException
* if an overflow, divide-by-zero, mod-by-zero, NaN, or Inf is encountered.
* <p>
* Limitations:
* <ul>
*     <li> only unary math functions are supported, no binary ones
*          (note however that pow can be expressed using the ** operator)
*     <li> caller-supplied functions and variables would be useful, but are not supported
* </ul>
*/
public class ExpressionParser
{
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private abstract static class Operator
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
    private abstract static class UnaryOperator extends Operator
    {
        abstract public double fun(double x);
        public UnaryOperator(int assoc, int prec, String name)
        {
            super(assoc, prec, name);
        }
    }
    private abstract static class BinaryOperator extends Operator
    {
        abstract public double fun(double x, double y);
        public BinaryOperator(int assoc, int prec, String name)
        {
            super(assoc, prec, name);
        }
    }

    private static UnaryOperator unops[] = {
        new UnaryOperator(RIGHT, 15, "~") {public double fun(double x) { return ~(int)x; }},
        new UnaryOperator(RIGHT, 15, "!") {public double fun(double x) { return (x==0 ? 1 : 0); }},
        new UnaryOperator(RIGHT, 15, "-") {public double fun(double x) { return -x; }},

        // Hack to get unary math functions in quickly:
        // treat them exactly like unary ops.
        // So, "sqrt 4" and "cos 1" and "log cos 2" will be allowed.
        // This actually isn't so bad.
        new UnaryOperator(RIGHT, 15, "sin")   {public double fun(double x) { return Math.sin(x); }},
        new UnaryOperator(RIGHT, 15, "cos")   {public double fun(double x) { return Math.cos(x); }},
        new UnaryOperator(RIGHT, 15, "tan")   {public double fun(double x) { return Math.tan(x); }},
        new UnaryOperator(RIGHT, 15, "asin")  {public double fun(double x) { return Math.asin(x); }},
        new UnaryOperator(RIGHT, 15, "acos")  {public double fun(double x) { return Math.acos(x); }},
        new UnaryOperator(RIGHT, 15, "atan")  {public double fun(double x) { return Math.atan(x); }},
        new UnaryOperator(RIGHT, 15, "exp")   {public double fun(double x) { return Math.exp(x); }},
        new UnaryOperator(RIGHT, 15, "log")   {public double fun(double x) { return Math.log(x); }},
        new UnaryOperator(RIGHT, 15, "sqrt")  {public double fun(double x) { return Math.sqrt(x); }},
        new UnaryOperator(RIGHT, 15, "ceil")  {public double fun(double x) { return Math.ceil(x); }},
        new UnaryOperator(RIGHT, 15, "floor") {public double fun(double x) { return Math.floor(x); }},
        new UnaryOperator(RIGHT, 15, "abs")   {public double fun(double x) { return Math.abs(x); }},

    };
    private static BinaryOperator binops[] = {
        new BinaryOperator(RIGHT, 14, "**") {public double fun(double x, double y) { return Math.pow(x, y); }},
        new BinaryOperator(LEFT,  13, "*")  {public double fun(double x, double y) { return x * y; }},
        new BinaryOperator(LEFT,  13, "/")  {public double fun(double x, double y) { return x / y; }}, // special case in parse(), does integer division if we're evaluating integer expressions
        new BinaryOperator(LEFT,  13, "%")  {public double fun(double x, double y) { return x % y; }},
        new BinaryOperator(LEFT,  12, "+")  {public double fun(double x, double y) { return x + y; }},
        new BinaryOperator(LEFT,  12, "-")  {public double fun(double x, double y) { return x - y; }},
        new BinaryOperator(LEFT,  11, "<<") {public double fun(double x, double y) { return (int)x << (int)y; }},
        new BinaryOperator(LEFT,  11, ">>") {public double fun(double x, double y) { return (int)x >> (int)y; }},
        new BinaryOperator(LEFT,  10, "<=") {public double fun(double x, double y) { return x <= y ? 1 : 0; }},
        new BinaryOperator(LEFT,  10, ">=") {public double fun(double x, double y) { return x >= y ? 1 : 0; }},
        new BinaryOperator(LEFT,  10, "<")  {public double fun(double x, double y) { return x < y ? 1 : 0; }},
        new BinaryOperator(LEFT,  10, ">")  {public double fun(double x, double y) { return x > y ? 1 : 0; }},
        new BinaryOperator(LEFT,   9, "==") {public double fun(double x, double y) { return x == y ? 1 : 0; }},
        new BinaryOperator(LEFT,   9, "!=") {public double fun(double x, double y) { return x != y ? 1 : 0; }}, // must come before factorial "!"
        new BinaryOperator(LEFT,   5, "&&") {public double fun(double x, double y) { return x!=0 && y!=0 ? 1 : 0; }},       // must come before "&"
        new BinaryOperator(LEFT,   4, "||") {public double fun(double x, double y) { return x!=0 || y!=0 ? 1 : 0; }},        // must come before "|"
        new BinaryOperator(LEFT,   8, "&")  {public double fun(double x, double y) { return (int)x & (int)y; }},
        new BinaryOperator(LEFT,   7, "^")  {public double fun(double x, double y) { return (int)x ^ (int)y; }},
        new BinaryOperator(LEFT,   6, "|")  {public double fun(double x, double y) { return (int)x | (int)y; }},
        new BinaryOperator(RIGHT,  3, "?")  {public double fun(double x, double y) { throw new Error(); }},    // special case in parse(), the function never gets called
        new BinaryOperator(LEFT,   1, ",")  {public double fun(double x, double y) { return y; }},
        new BinaryOperator(LEFT,  16, "!")  {public double fun(double x, double dummy) { return x>0 ? x*fun(x-1,0) : 1; }},     // factorial-- special case in expr_parse(), there's no RHS
    };

    //
    // Primitive string reader operations, with no memory allocation overhead.
    // This encapsulates all state.
    // Member functions other than these shouldn't access these very-private
    // variables.
    //
        private String _s = null;
        private int _sLength = 0;
        private int _pos = -1;

        private final void init(String s)
        {
            _s = s;
            _sLength = s.length();
            _pos = 0;
        }
        private final int tell()
        {
            return _pos;
        }
        private final void seek(int pos)
        {
            _pos = pos;
        }
        private final int peekChar()
        {
            return _pos == _sLength ? -1 : _s.charAt(_pos); // throws indexing error if out of bounds or reading past EOF
        }
        private final void advanceChar()
        {
            _pos++;
        }

        private final int getChar()
        {
            int c = peekChar();
            advanceChar();
            return c;
        }
        private final void discardSpaces()
        {
            int c;
            while ((c = peekChar()) != -1 && Character.isWhitespace((char)c))
                advanceChar();
        }


    private boolean getLiteral(String s)
    {
        int pos = this.tell();
        this.discardSpaces();
        int sLength = s.length();
        for (int i = 0; i < sLength; ++i)
            if (this.getChar() != s.charAt(i))
            {
                this.seek(pos);
                return false; // failure
            }
        return true; // success
    }

    private Operator getOp(Operator ops[],
                           int lowestPrecAllowed)
    {
        int pos = this.tell();
        for (int iOp = 0; iOp < ops.length; ++iOp)
        {
            if (getLiteral(ops[iOp].name))
            {
                if (ops[iOp].prec >= lowestPrecAllowed)
                    return ops[iOp];
                else
                {
                    // Put it back
                    this.seek(pos);
                    // And don't continue;
                    // e.g. if && is on the input
                    // but its precedence is too low to be recognized,
                    // we want to leave the thole thing on the input
                    // rather than reading the '&',
                    // so that the '&&' will be there later
                    // when the stack has been popped and we are ready
                    // for the lower precedence.
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
    // ascii hex character to value
    // XXX eek, this was buggy in the original source
    private static int ctoa(char c)
    {
        if ('0' <= c && c <= '9') return c - '0';
        if ('a' <= c && c <= 'z') return 10 + c - 'a';
        if ('A' <= c && c <= 'Z') return 10 + c - 'A';
        return 0;
    }

    // throws on failure
    private double getConstant(boolean intsOnly)
    {
        int base = 10;
        boolean negate = false;

        int pos = this.tell();

        this.discardSpaces();

        int c;
        if ((c = this.peekChar()) == -1)
        {
            throw new RuntimeException("unexpected end-of-expression trying to read constant");
        }

        if (c == '-')
        {
            negate = true;
            if ((c = this.peekChar()) == -1)
            {
                this.seek(pos);
                throw new RuntimeException("unexpected end-of-expression trying to read constant");
            }
        }

        if (Character.isDigit((char)c) || c == '.')
        {
            if (c == '0')
                base = 8;
            double returnVal = 0;
            while ((c = this.peekChar()) != -1
                && (isHexDigit((char)c) || c == 'x' || c == 'b'))
            {
                if (c == 'x')
                    base = 16;
                else if (c == 'b' && base != 16)
                    base = 2;
                else
                    returnVal = returnVal * base + ctoa((char)c);
                this.advanceChar();
            }
            if (!intsOnly)
            {
                if (this.peekChar() == '.')
                {
                    this.advanceChar();
                    double scale = 1;
                    while ((c = this.peekChar()) != -1 && Character.isDigit((char)c))
                    {
                        scale /= base;
                        returnVal += scale * ctoa((char)c);
                        this.advanceChar();
                    }
                }
                // TODO if I ever care: exponent!
            }
            if (negate)
                returnVal = -returnVal;
            return returnVal;
        }
        else
            throw new RuntimeException("expression parse error trying to read constant at position "+this.tell());
    } // getConstant


    // throws on failure
    private double parse(int lowestPrecAllowed,
                         boolean evaluate,
                         boolean intsOnly)
    {
        double returnVal = 0.;

        UnaryOperator unop;
        if ((unop = (UnaryOperator)getOp(unops, lowestPrecAllowed)) != null)
        {
            // expr -> unop expr
            returnVal = parse(unop.prec, evaluate, intsOnly);
            if (evaluate)
            {
                double newReturnVal = unop.fun(returnVal);

                if (intsOnly)
                {
                    if ((double)(int)newReturnVal != newReturnVal)
                    {
                        // on overflow or NaN or Inf, throw instead of letting it get clamped or turned into 0
                        throw new ArithmeticException(""+unop.name+"("+returnVal+") returned "+newReturnVal+" which is not expressible as an int");
                    }
                    newReturnVal = (double)(int)newReturnVal;
                }

                returnVal = newReturnVal;
            }
        }
        else if (getLiteral("("))
        {
            // expr -> '(' expr ')'
            returnVal = parse(0, evaluate, intsOnly);
            if (!getLiteral(")"))
            {
                // XXX TODO: define the kind of exception we're going to throw, be able to return the index in it?
                throw new RuntimeException(this.peekChar() == -1
                    ? "unexpected end-of-expression"
                    : ("syntax error near '"+(char)this.peekChar()+"'"
                     + " at index "+this.tell()));
            }
        }
        else if (getLiteral("pi"))
            returnVal = Math.PI;
        else if (getLiteral("e"))
            returnVal = Math.E;
        else
            returnVal = getConstant(intsOnly); // throws on failure

        BinaryOperator binop;
        while ((binop = (BinaryOperator)getOp(binops, lowestPrecAllowed)) != null)
        {
            double RHS = Double.NaN;
            if (binop.name.equals("!"))
            {
                // '!' in this context is the right-unary factorial operator,
                // in which case no RHS is needed...
            }
            else if (binop.name.equals("?"))
            {
                double ifTrue = parse(binop.prec,
                                      evaluate && returnVal!=0,
                                      intsOnly);
                if (!getLiteral(":"))
                    throw new RuntimeException(this.peekChar() == -1
                        ? "unexpected end-of-expression"
                        : ("syntax error near '"+(char)this.peekChar()+"'"
                         + " at index "+this.tell()));
                double ifFalse = parse(binop.prec,
                                       evaluate && returnVal==0,
                                       intsOnly);
                RHS = (returnVal!=0) ? ifTrue : ifFalse;
            }
            else
            {
                RHS = parse(binop.assoc == RIGHT ? binop.prec
                                                         : binop.prec+1,
                            evaluate
                         && !binop.name.equals(returnVal!=0 ? "||" : "&&"),
                            intsOnly);
            }
            if (evaluate)
            {
                double newReturnVal;
                if (intsOnly && binop.name.equals("/"))
                    newReturnVal = (double)((int)returnVal / (int)RHS); // throws ArithmeticException if RHS is zero
                else if (intsOnly && binop.name.equals("%"))
                    newReturnVal = (double)((int)returnVal % (int)RHS); // throws ArithmeticException if RHS is zero
                else
                    newReturnVal = binop.fun(returnVal, RHS);

                if (intsOnly)
                {
                    if ((double)(int)newReturnVal != newReturnVal)
                    {
                        // on overflow or NaN or Inf, throw instead of letting it get clamped or turned into 0
                        if (binop.name.equals("!"))
                            throw new ArithmeticException(""+returnVal+binop.name+" returned "+newReturnVal+" which is not expressible as an int");
                        else
                            throw new ArithmeticException(""+returnVal+" "+binop.name+" "+RHS+" returned "+newReturnVal+" which is not expressible as an int");
                    }
                    newReturnVal = (double)(int)newReturnVal;
                }

                returnVal = newReturnVal;
            }
        }
        return returnVal;
    } // parse

    /**
    * Constructor; the purpose of the class object is simply to hold
    * the state variables needed during parsing;
    * no memory allocations are made during parsing.
    */
    public ExpressionParser()
    {}

    /**
    * Evaluate an expression, using double-precision floating-point numbers
    * for the return value and all intermediate expressions,
    * so that, for example, "3/2" evaluates to 1.5.
    */
    public double evaluateDoubleExpression(String s)
    {
        this.init(s);
        double returnVal = parse(0, // recursionLevel
                                 true, // evaluate
                                 false); // intsOnly
        this.discardSpaces();
        if (this.peekChar() != -1)
            throw new RuntimeException("syntax error in double expression at position "+this.tell());
        return returnVal;
    }

    /**
    * Evaluate an expression, using integers
    * for the return value and all intermediate expressions,
    * so that, for example, "3/2" evaluates to 1.
    */
    public int evaluateIntExpression(String s)
    {
        this.init(s);
        int returnVal = (int)parse(0, // recursionLevel
                                   true, // evaluate
                                   true); // intsOnly
        this.discardSpaces();
        if (this.peekChar() != -1)
            throw new RuntimeException("syntax error in int expression at position "+this.tell());
        return returnVal;
    }

    /** little test program */
    public static void main(String args[])
    {
        if (args.length < 1)
        {
            System.err.println("Usage: ExpressionParser \"<expression>\"");
            System.exit(1);
        }
        String s = args[0];
        for (int i = 1; i < args.length; ++i) // starting at 1
            s += " " + args[i];

        ExpressionParser parser = new ExpressionParser();

        System.out.println("As double expression:");
        double d = parser.evaluateDoubleExpression(s);
        System.out.println(d);

        System.out.println("As int expression:");
        int i = parser.evaluateIntExpression(s);
        System.out.println(i);

    } // main


} // ExpressionParser

