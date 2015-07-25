//
// macros.h
//

#define FOR(i,n) for (i = 0; (i) < (n); ++i)
#define FORI(i,n) for (int i = 0; (i) < (n); ++i)
#define FORDOWN(i,n) for (i = (n)-1; (i) >= 0; --i) // only evaluates n once
#define FORIDOWN(i,n) for (int i = (n)-1; (i) >= 0; --i) // only evaluates n once

#define assert(expr) do { if (!(expr)) throw new Error("Assertion failed at "+__FILE__+"("+__LINE__+"): " + #expr + ""); } while (false)
#define assumpt(expr) do { if (!(expr)) throw new Error("Assumption failed at "+__FILE__+"("+__LINE__+"): " + #expr + ""); } while (false)
#define unimplemented() do {if (true) throw new Error("Unimplemented at "+__FILE__+"("+__LINE__+")"); } while (false)

// NOTE: this causes a,b to be evaluated twice on failure, so is not ideal. might be better to do a block... ?
#define assert_op(a,op,b) do { if (!((a)op(b))) throw new Error("Assertion failed at "+__FILE__+"("+__LINE__+"): (" + #a + ")" + #op + "(" + #b + ") ("+(a)+" vs. "+(b)+")"); } while (false)
#define assert_eq(a,b) assert_op(a,==,b)
#define assert_le(a,b) assert_op(a,<=,b)
#define assert_ge(a,b) assert_op(a,>=,b)
#define assert_lt(a,b) assert_op(a,<,b)
#define assert_gt(a,b) assert_op(a,>,b)
#define assert_ne(a,b) assert_op(a,!=,b)
#define assert_nan(x) do { if (!Double.isNaN(x)) throw new Error("Assertion failed at "+__FILE__+"("+__LINE__+"): " + #x + " is "+(x)+", expected NaN"); } while (false)
// Note: the additional "(a)==(b)||" is to make it work correctly for infinities...
// and causes the args to be evaluated twice.
#define assert_almost_eq(a,b,tol) \
    do { \
        if (!((a)==(b)||Math.abs((a)-(b))<=tol)) \
            throw new Error("Assertion failed at "+__FILE__+"("+__LINE__+"): " \
                           +"(" + #a + ")==(" + #b + ")+-" + #tol + "" \
                           +" ("+(a)+" vs. "+(b)+" +- "+(tol)+")" \
                           +" (error = "+((a)-(b))+")" \
                           ); \
    } while (false)
#define assert_almost_inorder(a,b,c,tol) \
    do { \
        if (!((a)-(b)<=(tol)) || !((b)-(c)<=(tol))) \
            throw new Error("Assertion failed at "+__FILE__+"("+__LINE__+"): " \
                           +"(" + #a + ")<=(" + #b + ")<=(" + #c + ")+-" + #tol + "" \
                           +" ("+(a)+" vs. "+(b)+" vs. "+(c)+" +- "+(tol)+")" \
                           +" (error = "+((a)-(b))+", "+((b)-(c))+")" \
                           ); \
    } while (false)

#define INRANGE(foo,bar,baz) ((foo(bar))&&((bar)baz))

#define OUT(s) System.out.println(""+s)
#define _PRINT(x) System.out.println(#x + " = " + (x))
#define _PRINT_(x) System.out.print(#x + " = " + (x) + ", ")
#define _PRINT2(x0,x1) {_PRINT_(x0);_PRINT(x1);}
#define _PRINT3(x0,x1,x2) {_PRINT_(x0);_PRINT2(x1,x2);}
#define _PRINT4(x0,x1,x2,x3) {_PRINT_(x0);_PRINT3(x1,x2,x3);}
#define _PRINT5(x0,x1,x2,x3,x4) {_PRINT_(x0);_PRINT4(x1,x2,x3,x4);}
#define _PRINT6(x0,x1,x2,x3,x4,x5) {_PRINT_(x0);_PRINT5(x1,x2,x3,x4,x5);}
#define _PRINT7(x0,x1,x2,x3,x4,x5,x6) {_PRINT_(x0);_PRINT6(x1,x2,x3,x4,x5,x6);}
#define PRINTSUB(x,i) System.out.println("        " + #x + "["+(i)+"] = " + (x)[i])
#define PRINT(x) {System.out.print("        "); _PRINT(x);}
#define PRINT2(x0,x1) {System.out.print("        "); _PRINT2(x0,x1);}
#define PRINT3(x0,x1,x2) {System.out.print("        "); _PRINT3(x0,x1,x2);}
#define PRINT4(x0,x1,x2,x3) {System.out.print("        "); _PRINT4(x0,x1,x2,x3);}
// XXX ... what a mess

#define PRINTSUBSUB(x,i,j) System.out.println("        " + #x + "["+(i)+"]["+(j)+"] = " + (x)[i][j])
#define PRINTSUBSUBSUB(x,i,j,k) System.out.println("        " + #x + "["+(i)+"]["+(j)+"]["+(k)+"] = " + (x)[i][j][k])
#define PRINTVEC(x) System.out.println("        " + #x + " = " + VecMath.toString(x))
#define PRINTMATROW(x,i) System.out.println("        " + #x + "["+(i)+"] = " + VecMath.toString((x)[i]))
#define PRINTMAT(x) System.out.println("        " + #x + " =\n" + VecMath.toString(x))
#define PRINTARRAY(x) System.out.println("        " + #x + " = " + Arrays.toStringCompact(x))
#define PRINTARRAY_NONCOMPACT(x) System.out.println("        " + #x + " =\n" + Arrays.toStringNonCompact(x, "        ", "        "))

#define ABS(x) ((x) < 0 ? -(x) : (x))
#define SIGN(x) ((x) < 0 ? -1 : 1)
#define HYPOTSQRD(a,b) (((a)*(a))+((b)*(b)))
#define LERP(a,b,t) ((a) + (t)*((b)-(a)))
#define SMOOTH(timeFrac) ((Math.sin(((timeFrac) - .5) * Math.PI) + 1) / 2)
#define SWAP(a,b,temp) {temp=(a);a=(b);b=(temp);}
#define MIN(a,b) ((a)<=(b)?(a):(b))
#define MAX(a,b) ((a)>=(b)?(a):(b))
#define MIN3(a,b,c) ((a)<=(b)?MIN(a,c):MIN(b,c))
#define MAX3(a,b,c) ((a)>=(b)?MAX(a,c):MAX(b,c))
#define MID3(a,b,c) ((a)<=(b)?((b)<=(c)?(b):(a)>=(c)?(a):(c)) \
                             :((a)<=(c)?(a):(b)>=(c)?(b):(c)))
#define MIN4(a,b,c,d) ((a)<=(b)?MIN3(a,c,d):MIN3(b,c,d))
#define MAX4(a,b,c,d) ((a)>=(b)?MAX3(a,c,d):MAX3(b,c,d))
#define MINI(a,b) ((a)<=(b)?0:1)
#define MAXI(a,b) ((a)>=(b)?0:1)
#define MINI3(a,b,c) ((a)<=(b)?(a)<=(c)?0:2 \
                              :(b)<=(c)?1:2)
#define MAXI3(a,b,c) ((a)>=(b)?(a)>=(c)?0:2 \
                              :(b)>=(c)?1:2)
#define MIDI3(a,b,c) ((a)<=(b)?((b)<=(c)?1:(a)>=(c)?0:2) \
                              :((a)<=(c)?0:(b)>=(c)?1:2))
#define CLAMP(x,a,b) ((x)<=(a)?(a):(x)>=(b)?(b):(x))
#define BIT(x,i) (((x)>>(i))&1)
#define SQR(x) ((x)*(x))
#define TRIANGLED(x) (((x)*((x)+1)) / 2)
#define RTOD(r) ((r)*(180./Math.PI))
#define DTOR(r) ((r)*(Math.PI/180.))
#define LT(a,b,tol) (((a)-(b)) < (tol))
#define GT(a,b,tol) (((b)-(a)) < (tol))
#define LEQ(a,b,tol) (((a)-(b)) <= (tol))
#define GEQ(a,b,tol) (((b)-(a)) <= (tol))
#define EQ(a,b,tol) (LEQ(a,b,tol) && GEQ(a,b,tol))
#define MOD(a,b) (((a)+(b))%(b)) // XXX not general! only use it when a+b is known to be >= 0
