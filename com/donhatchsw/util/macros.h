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


#define INRANGE(foo,bar,baz) ((foo(bar))&&((bar)baz))

#define OUT(s) System.out.println(""+s);
#define PRINT(x) System.out.println(#x + " = " + (x))
#define PRINT_(x) System.out.print(#x + " = " + (x) + ", ")
#define PRINT2(x0,x1) {PRINT_(x0);PRINT(x1);}
#define PRINT3(x0,x1,x2) {PRINT_(x0);PRINT2(x1,x2);}
#define PRINT4(x0,x1,x2,x3) {PRINT_(x0);PRINT3(x1,x2,x3);}
#define PRINT5(x0,x1,x2,x3,x4) {PRINT_(x0);PRINT4(x1,x2,x3,x4);}
#define PRINT6(x0,x1,x2,x3,x4,x5) {PRINT_(x0);PRINT5(x1,x2,x3,x4,x5);}
#define PRINT7(x0,x1,x2,x3,x4,x5,x6) {PRINT_(x0);PRINT6(x1,x2,x3,x4,x5,x6);}
#define PRINTSUB(x,i) System.out.println("    " + #x + "["+(i)+"] = " + (x)[i])
#define PRINTSUBSUB(x,i,j) System.out.println("    " + #x + "["+(i)+"]["+(j)+"] = " + (x)[i][j])
#define PRINTVEC(x) System.out.println(#x + " = " + VecMath.toString(x))
#define PRINTMATROW(x,i) System.out.println("    " + #x + "["+(i)+"] = " + VecMath.toString((x)[i]))
#define PRINTMAT(x) System.out.println(#x + " =\n" + VecMath.toString(x))
// XXX change the following to PRINTARRAY I think
#define PRINTARRAY(x) System.out.println(#x + " = " + Arrays.toStringCompact(x))
#define PRINTARRAY_NONCOMPACT(x) System.out.println(#x + " =\n" + Arrays.toStringNonCompact(x, "    ", "    "))

#define ABS(x) ((x) < 0 ? -(x) : (x))
#define HYPOTSQRD(a,b) (((a)*(a))+((b)*(b)))
#define LERP(a,b,t) ((a) + (t)*((b)-(a)))
#define SWAP(a,b,temp) {temp=(a);a=(b);b=(temp);}
#define MIN(a,b) ((a)<=(b)?(a):(b))
#define MAX(a,b) ((a)>=(b)?(a):(b))
#define MIN3(a,b,c) ((a)<=(b)?MIN(a,c):MIN(b,c))
#define MAX3(a,b,c) ((a)>=(b)?MAX(a,c):MAX(b,c))
#define MIN4(a,b,c,d) ((a)<=(b)?MIN3(a,c,d):MIN3(b,c,d))
#define MAX4(a,b,c,d) ((a)>=(b)?MAX3(a,c,d):MAX3(b,c,d))
#define MINI(a,b) ((a)<=(b)?0:1)
#define MAXI(a,b) ((a)>=(b)?0:1)
#define MINI3(a,b,c) ((a)<=(b)?(a)<=(c)?0:2 \
                              :(b)<=(c)?1:2)
#define MAXI3(a,b,c) ((a)>=(b)?(a)>=(c)?0:2 \
                              :(b)>=(c)?1:2)
#define CLAMP(x,a,b) ((x)<=(a)?(a):(x)>=(b)?(b):(x))
#define BIT(x,i) (((x)>>(i))&1)
#define SQR(x) ((x)*(x))
#define TRIANGLED(x) (((x)*((x)+1)) / 2)
#define RTOD(r) ((r)*(180./Math.PI))
#define DTOR(r) ((r)*(Math.PI/180.))

#define LT(a,b,eps) (((b)-(a)) > eps)
#define GT(a,b,eps) (((a)-(b)) > eps)
#define LEQ(a,b,eps) (((a)-(b)) <= eps)
#define GEQ(a,b,eps) (((b)-(a)) <= eps)
#define EQ(a,b,eps) (LEQ(a,b,eps) && GEQ(a,b,eps))
#define MOD(a,b) (((a)+(b))%(b)) // XXX not general! only use it when a+b is known to be >= 0
