// From an old reentrant no-memory-allocations
// C expression parser I had lying around...

package com.donhatchsw.javacpp;

public class ExpressionParser
{
    // Logical assertions, always compiled in. Ungracefully bail if violated.
    private static void AssertAlways(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

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
        public final int discard_spaces()
        {
            int c;
            while ((c = peek()) != -1 && Character.isWhiteSpace((char)c))
                advance();
        }
    } // ZeroOverheadStringReader


    private static boolean getLiteral(ZeroOverheadStringReader reader,
                                      String s)
    {
        int pos = reader.tell();
        reader.discard_spaces();
        int sLength = s.length();
        for (int i = 0; i < sLength; ++i)
            if (reader.getchar() != s.charAt(i))
            {
                seek(pos);
                return false; // failure
            }
        return true; // success
    }

    private static boolean getOp(ZeroOverheadStringReader reader,
                                 Operator ops[],
                                 int lowestOperatorPrecedenceRecognized)
    {
        int pos = reader.tell();
        for (int iOp = 0; iOp < ops.length; ++iOp)
        {
            if (getLiteral(reader, ops[iOp].name))
            {
                if (ops[iOp].prec >= lowestOperatorPrecedenceRecognized)
                    return ops[iOp];
                else
                {
                    // Put it back and don't continue;
                    // e.g. if && is on the input
                    // but its precedence is too low to be recognized,
                    // we want to leave the thole thing on the input
                    // rather than reading the '&'.
                    seek(pos);
                    return null;
                }
            }
        }
        return null;
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
        double scale;
        boolean negate = false;
        double returnVal = Double.NaN;

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

        if (isdigit(c) || c == '.')
        {
            if (c == '0')
                base = 8;
            returnVal = 0;
            while ((c = reader.peek()) != -1
                && (isHexDigit((char)c) || c == 'x' || c == 'b'))
            {
                if (c == 'x')
                    base = 16;
                else if (c == 'b')
                    base = 2;
                else
                    returnVal = returnVal * base + ctoa(c);
                reader.advance();
            }
            if (reader.peek() == '.')
            {
                reader.advance();
                double scale = 1;
                while ((c = peek()) != -1 && Character.isDigit((char)c))
                {
                    scale /= base;
                    returnVal += scale * ctoa(c);
                    reader.advance();
                }
            }
            // TODO if I ever care: exponent!
            if (negate)
                returnVal = -returnVal;
            return returnVal;
        }
        throw new RuntimeExcpetion("expression parse error trying to read constant at position "+reader.tell());
    } // getConstant


    // throws on failure
    private double parse(ZeroOverheadStringReader reader,
                         int lowestOperatorPrecedenceRecognized,
                         boolean evaluate)

    {
        reader.init(s);

        Operator unop, binop;
        double RHS = Double.NaN;
        double returnVal = Double.NaN;

        if ((unop = getOp(reader, unops, numberof(unops), lowestPrecAllowed)) != null)
        {
            // expr -> unop expr
            returnVal = parse(reader, unop.prec);
            if (evaluate)
                returnVal = unop.fun(returnVal);
        }
        else if (expr_get_literal(reader, "("))
        {
            // expr -> '(' expr ')'
            returnVal = parse(reader, 0);
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

        while ((binop = getOp(reader, binops, lowestOperatorPrecedenceRecognezed)) != null)
        {
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
                if (binop.name.equals("%" && RHS == 0)
                    throw new RuntimeExcpetion("mod by zero");
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
    } // main


} // ExpressionParser



