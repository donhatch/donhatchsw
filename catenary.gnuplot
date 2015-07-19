#!/usr/bin/gnuplot

# Experimenting fitting a catenary between two points,
# that has a given moment.
# This is the underlying problem for SmoothlyVaryingViewingParameter.


# note, some day I think there will be a -c option that will allow args to be passed in more easily, but not today.

# TODO: 'do' loop inside function?? instead of recursion? hmm
# TODO: implement for arbitrary angle from v0 to v1

# Things that can be tweaked:
#       - change to various values of velocity0 ({-.5,0} for symmetric about origin)
#       - uncomment the "f(z) = unstretched_moment_from_xy(...)" to see the almost-circles (probably only works with velocity0 on x axis)

# TODO: maybe project contours onto the base and sides? Hmm.

# Q: I know the "ellipses" aren't really ellipses.  But are they left-right symmetric at least?
# A: Yes!  Actually this is obvious since they are the same shape as the
#    contours from v0=(-.5,0),v1=(.5,0) which is left-right-symmetric picture,
#    with each contour right-shifted by .5*(1+slack).

# Q: how to get a splot in 2d (currently fudging using "set view" and "unset ztics")
#    (but maybe I don't want it any more? not sure)
# Q: how to reset view to 0,359.99,1.5 from keyboard?
# Q: why the heck isn't the z axis going all the way to the floor?
# Q: is there a way to increase the recursion depth limit? (seems to be 250)

# Q: can I make a visual differentiator at mag=0?
# PA: well, can add a degenerate plot at a different color
# PA: if I was good at color palettes then maybe I could do it?
#     people seem to have demos on the web where they make a simple plot do it,
#     but not an splot? hmm
#     if I google for images "gnuplot splot using palette",
#     I get lots of pictures of pm3d but that's not what I want! argh!
#     oh wait maybe there's some:
#     Oh! see http://www.cs.huji.ac.il/course/2006/76552/Lesson12/
#       splot x*x-y*y with line palette  
#       set parametric
#       splot u,v,v with line palette
#     Easy!
#     Except I don't understand it :-(


#
# Parameters that could logically be taken from command line args if hooked up

    png_flag = 0 # if set, output to RMME1.png and RMME2.png instead of terminal

    if (!png_flag) {
      DISPLAY = "`echo $DISPLAY`"
      #print "DISPLAY = '",DISPLAY,"'"
      if (DISPLAY eq "") {
        print "no DISPLAY, forcing png_flag to 1 (so output goes into RMME1.png, RMME2.png)"
        png_flag = 1
      }
    }

    #velocity0 = sqrt(.5) * {-1,-1}
    #velocity0 = -sqrt(.5) + 1.1 * -sqrt(.5)*{0,1}

    #velocity0 = {-1.5,0}
    #velocity0 = {-1,0}
    #velocity0 = {-.5,0} # symmetric about origin: {-.5,0} to {.5,0}
    velocity0 = {0,0}   # normal, from {0,0} to {1,0}
    #velocity0 = {.5,0}

    velocity1 = velocity0 + {1,0}
    #velocity1 = velocity0 + sqrt(.5)*{1,1}  # XXX not implemented at all yet anywhere I don't think
    #velocity1 = velocity0 + {.5,0}

    # when png_flag is set, both of the following can be set.
    # otherwise it makes sense to set at most one of them.
    plot1_flag = 1  # slack-and-angle to moment
    plot2_flag = 0  # moment to slack-and-angle
    plotf0_flag = 0 # XXX currently doesn't work well
    plotf1_flag = 0 # XXX currently doesn't work well

    plot3_flag = 1 # very experimental. exploring the function in 1d in directions from the origin.

    # either of the following should work.
    #strategy = "weil" # sets f = wf
    strategy = "good" # sets f = gf
     

if (png_flag) {
    # pngcairo antialiases lines, nice when I want it. enhanced gives more chars for labels
    set terminal pngcairo enhanced size 600,600
    set output "RMME0.png"
} else {
    set term wxt size 1000,1000
    #set term x11 size 1000,1000
    set size square
}

set view 0,359.999,1.5 # top down, with a bit of fudge to make y axis labels come out on left instead of right
#set view 85,85 # bad angle for pm3d
#unset ztics


i = {0,1}
debug(s) = system(sprintf("echo \"%s\" >> RMME", s))
assert(cond) = cond || assertion_failed(1) # function that doesn't exist
EXACT(z) = imag(z)==0. ? sprintf("%.17g", z) : sprintf("{%.17g, %.17g}", real(z), imag(z))
max(a,b) = a>=b ? a : b
dtor(d) = d*(pi/180.)
rtod(r) = r*(180./pi)
conj(z) = real(z) - i*imag(z)
xconj(z) = -real(z) + i*imag(z)
leq(a,b,eps) = a-b <= eps
geq(a,b,eps) = b-a <= eps
eq(a,b,eps) = leq(a,b,eps) && geq(a,b,eps)


# Gnuplot provides asinh but it's apparently implemented as log(1. + sqrt(1. + y*y))
# which is crap near 0 and for large negative arguments.
# See notes in MyMath.prejava for the logic here.
Asinh(y) = y<0 ? -Asinh(-y) : log1p(y * (1. + y/(sqrt(y**2+1)+1)))
log1p(x) = log1phelper(x, 1.+x)
  log1phelper(x,u) = log(u) - ((u-1.)-x)/u
#Asinh(y) = asinh(y) # uncomment this to see gaps in the function at the x axis

#
# This method works but tends to be slower than newton
# except for when y>6e23 i.e. x>59.5, or so.
# Newton is a lot better for typical values.
#
sinhc(x) = x==0. ? 1. : sinh(x)/x
asinhc_by_binary_search_lo(y) = y==1. ? 0. : asinhc_binary_search_lo(y, 0., 1e3, (0.+1e3)/2.)
  asinhc_binary_search_lo(y, x0, x1, xMid) = \
      xMid<=x0 || xMid>=x1 ? xMid : \
      sinhc(xMid) < y ? asinhc_binary_search_lo(y, xMid, x1, (xMid+x1)/2.) \
                      : asinhc_binary_search_lo(y, x0, xMid, (x0+xMid)/2.)
asinhc_by_binary_search_hi(y) = y==1. ? 0. : asinhc_binary_search_hi(y, 0., 1e3, (0.+1e3)/2.)
  asinhc_binary_search_hi(y, x0, x1, xMid) = \
      xMid<=x0 || xMid>=x1 ? xMid : \
      sinhc(xMid) <= y ? asinhc_binary_search_hi(y, xMid, x1, (xMid+x1)/2.) \
                      : asinhc_binary_search_hi(y, x0, xMid, (x0+xMid)/2.)

#
# The good news is, Newton seems to work well in all cases:
#   f(x) = sinh(x)/x-y
#   f'(x) = (x*math.cosh(x)-sinh(x))/x**2
#   xNext = x - f(x)/f'(x)
#         = x - (sinh(x)/x-y)/((x*cosh(x)-sinh(x))/x**2)
# But the bad news is, when done, it seems to get in cycles of 5 so we'd need to retain a lot of values
# if our stopping criterion is seeing a repeat.
# But the good news is, after one iteration, we get a value that's definitely too high,
# after which it's all downhill, so we can stop as soon as it didn't decrease.
#
asinhc_by_newton(y) = y<1. ? crash_in_asinhc_by_newton(1) : y==1. ? 0. : asinhc_by_newton_recurse0(y, Asinh(y))
  # this gets called with an initial guess x that may be (or must be?) too small.
  asinhc_by_newton_recurse0(y, x)   = asinhc_by_newton_recurse1(y, x - (sinh(x)/x-y)/((x*cosh(x)-sinh(x))/x**2))
    # this gets called with x that's definitely bigger than the answer.
    asinhc_by_newton_recurse1(y, x) = asinhc_by_newton_recurse (y, x - (sinh(x)/x-y)/((x*cosh(x)-sinh(x))/x**2), x)
      # this gets called with x<xPrev, unless converged.
      asinhc_by_newton_recurse(y, x, xPrev) = x>=xPrev ? (x+xPrev)/2. : asinhc_by_newton_recurse_helper(y, x, cosh(x), sinh(x))
      asinhc_by_newton_recurse_helper(y, x, cosh_x, sinh_x) = asinhc_by_newton_recurse(y, x - (sinh_x/x-y)/((x*cosh_x-sinh_x)/x**2), x)

# XXXTODO: is it really guaranteed that the first time the diff from prev doesn't decrease, it's the answer?
# Actually newton seems a little bit faster than halley.
# Is it because when the 2nd derivative is zero or close to it, newton actually converges cubically? No I don't think so, hmm.
asinhc_by_halley(y) = y<1. ? crash_in_asinhc_by_halley(1) : y==1. ? 0. : asinhc_by_halley_recurse(y, Asinh(y), NaN, NaN, 20)

  asinhc_by_halley_recurse(y, x, xPrev, xPrevPrev, maxRecursions) = maxRecursions==0 ? crash_in_asinhc_by_halley_recurse(1) : (x==xPrev||abs(x-xPrev)>=abs(xPrev-xPrevPrev)) ? (x+xPrev)/2. : asinhc_by_halley_recurse_helper(y, x, xPrev, sinh(x)/x-y, (x*cosh(x)-sinh(x))/x**2, ((x**2+2)*sinh(x)-2*x*cosh(x))/x**3, maxRecursions)
  asinhc_by_halley_recurse_helper(y, x, xPrev, fx, dfx, ddfx, maxRecursions) = asinhc_by_halley_recurse(y, x - 2*fx*dfx/(2*dfx**2 - fx*ddfx), x, xPrev, maxRecursions-1)

#asinhc(y) = asinhc_by_binary_search_lo(y)
asinhc(y) = asinhc_by_newton(y)
#asinhc(y) = asinhc_by_halley(y)


if (0) { # turn this on to exercise and debug asinhc_by_halley. exercises what I think is the worst case, to verify the max recursion depth ever used.  seems to be 20. (found by trying various numbers)
    crashed = 0
    crashedTraceString = ""
    if (1) {
        # version that accumulates a string and sets "crashed" flag instead of actually crashing
        asinhc_by_halley(y) = y<1. ? crash_in_asinhc_by_halley(1) : y==1. ? 0. : asinhc_by_halley_recurse(y, Asinh(y), NaN, NaN, 20)
          asinhc_by_halley_recurse(y, x, xPrev, xPrevPrev, maxRecursions) = ((traceString=traceString.sprintf("        y=%s x=%s\n",EXACT(y),EXACT(x))),maxRecursions==0 ? (crashed=1,crashedTraceString=traceString,NaN) : (x==xPrev||abs(x-xPrev)>=abs(xPrev-xPrevPrev)) ? (x+xPrev)/2. : asinhc_by_halley_recurse_helper(y, x, xPrev, sinh(x)/x-y, (x*cosh(x)-sinh(x))/x**2, ((x**2+2)*sinh(x)-2*x*cosh(x))/x**3, maxRecursions))
          asinhc_by_halley_recurse_helper(y, x, xPrev, fx, dfx, ddfx, maxRecursions) = (asinhc_by_halley_recurse(y, x - 2*fx*dfx/(2*dfx**2 - fx*ddfx), x, xPrev, maxRecursions-1))
    }

    y=2.
    y = (y-1)/1.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
    y=1.1
    y = (y-1)/1.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
    do for [i=1:14] {
        y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
    }
    do for [i=1:3] {
        y = (y-1)/2.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
    }
    print "crashed = ",crashed
    exit
}

# From any of the following references:
#       Barzel/Pixar "Faking Dynamics of Ropes and Springs", EIII Computer Graphics and Applications 17(3), May-June 1997
#       Weil "The synthesis of Cloth Objects", SIGGraph 1986
#       http://en.wikipedia.org/wiki/Catenary#Determining_parameters
# Given L = arc length, a=(x1-x0), b=(y1-y0):
#    sqrt(L^2 - b^2) = 2*catScale*Asinh(a/(2*catScale))
#                    = a * (2*catScale/a) * Asinh(a/(2*catScale))
#                    = a * sinhc(a/(2*catScale))
#    sqrt(L^2 - b^2)/a = sinhc(a/(2*catScale))
#    a/(2*catScale) = asinhc(sqrt(L^2 - b^2)/a)
#    invCatScale = asinhc(sqrt(L^2 - b^2)/a) * (2/a)

{
    #
    # Implement the formulation given by Weil.
    # Given (x0,y0),(x1,y1),
    # he wants to find params a,b,c such that the curve is given by:
    #       y = c + a*cosh((x-b)/a)
    # and has given length L.
    # First solve for a (=catScale) in:
    #       sqrt(L^2 - (y1-y0)^2) = 2*a*sinh((x1-x0)/(2*a))
    #                             = (2*a)/(x1-x0)*sinh((x1-x0)/(2*a))*(x1-x0)
    #                             = sinhc((x1-x0)/(2*a)) * (x1-x0)
    #       (x1-x0)/(2*a) = asinhc(sqrt(L^2 - (y1-y0)^2) / (x1-x0))
    #       2*a = (x1-x0)/asinhc(sqrt(L^2 - (y1-y0)^2) / (x1-x0))
    #         a = (x1-x0)/(2*asinhc(sqrt(L^2 - (y1-y0)^2) / (x1-x0)))
    # Then,
    #         M = sinh(x1/a) - sinh(x0/a)     = t distance on canonical catenary
    #         N = cosh(x1/a) - cosh(x0/a)     = y distance on canonical catenary
    # Wait a minute, isn't it always true that M >= N?  (assuming x0 <= x1)
    # So why are the two papers pretending that's not the case??
    #
    # Anyway,
    #       If N>M:   (actually this never happens, wtf?)
    #           mu = atanh(M/N)
    #            Q = M/sinh(mu) = N/cosh(mu)   (the Barzel paper always uses M/sinh(mu), don't know why)
    #            b = a*(mu - Asinh(L/(Q*a)))
    #       If M>=N:  (this is always true)
    #            mu = atanh(N/M)
    #            Q = M/cosh(mu) = N/sinh(mu)   (the Barzel paper always uses N/sinh(mu), don't know why)
    #            b = a*(mu - acosh(L/(Q*a)))
    #   (NOTE: wolframalpha says Q is M*sqrt(1-N^2/M^2) = sqrt(M^2-N^2) so that might simplify things?)
    # SIMPLIFICATIONS:
    #   - wolframalpha says:
    #       Q = M*sqrt(1-N^2/M^2)
    #         = sqrt(M^2-N^2)
    #         = sqrt(2*(cosh((x1-x0)/a) - 1))
    #   - wolframalpha says:
    #       mu = atanh((cosh(x1)-cosh(x0))/(sinh(x1)-sinh(x0)))
    #          = (x0+x1)/(2*a)
    #            wtf??? is this way way simpler than they made it??
    #   - so:
    #       b = (x0+x1)/2 - a*acosh(L/(a*sqrt(2*(cosh((x1-x0)/a) - 1))))
    #   - actually, I think I can use my surprising magic formula for the midpoint on the canonical catenary:
    #       xmid = Asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))
    #     In other words:
    #       Asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a))) = xmid = ((x0+x1)/2-b)/a
    #       a*Asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a))) = (x0+x1)/2-b
    #       b = (x0+x1)/2 - a*Asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))
    #     So that seems simpler than all of the above.
    #
    # Then solve for c:
    #       y0 = c + a*cosh(x/a-B)
    #       c = y0 - a*cosh(x0/a-B)
    #               (in Barzel paper, x0=y0=0 and its c = -c here, so c = cosh(b/a))

    # Okay so now we know a,B,c, and:
    #       y = c + a*cosh((x-b)/a)
    # But that's a sucky computation of c, in the case that a is small or 0...
    # so we'll compute it more robustly in a moment.
    # Can we parametrize that by arc length?
    #       t0 = a*sinh((x0-b)/a)
    #       t1 = a*sinh((x1-b)/a)
    #       t = a*sinh((x-b)/a)
    # Ok proceeding with parametrization by arc length...
    #       x = b + a*Asinh(t/a)
    #       y = c + a*cosh((x-b)/a)
    #         = c + a*cosh(((b/a + Asinh(t/a))-b/a))
    #         = c + a*cosh(Asinh(t/a))
    #         = c + sqrt(t^2 + a^2)
    #
    # Now let's get c more robustly as promised:
    #       y0 = c + sqrt(t0^2 + a^2)  (or same for y1)
    #       c = y0 - sqrt(t0^2 + a^2)
    #
    # Note that we still didn't get t0,t1 robustly.  How do we do that??
    # I think maybe we need to get c from first principles, without getting t0,t1 first?  Not sure.
    #     The trick is, need to compute t0,t1,c from y0,y1,a,b, *not* from x0,x1.
    #     Ouch, but wait a minute... it's actually not computable from y0,y1,a,b
    #     in the case when a=0!  In that case we know b = x0==x1 but c,y0,y1 can be anything...
    #     still need L to be in the equation, I think.
    # Yeah, actually figured this out, see the "good" alternative formulation later on down
    # in this file.

    #       
    #
    # So, the moment will be the integral of x,y from t=t0 to t=t1.
    # According to wolframalpha:
    #       x part of integral = a*t*Asinh(t/a) + b*t - a*sqrt(a^2+t^2)
    #       y part of integral = 1/2 t (sqrt(t^2+a^2) + 2*c) + 1/2 a^2 log(sqrt(t^2+a^2) + t)
    # but we can turn log(sqrt(t^2+a^2) + t) into hyperbolic trig as follows:
    #         log(sqrt(t**2+a**2) + t)
    #       = log(a*(sqrt((t/a)^2 + 1) + t/a))
    #       = log(a) + log(sqrt((t/a)^2+1) + t/a)
    #       = log(a) + Asinh(t/a)
    # and the log(a) gets absorbed into the integration constant. Yay! So:
    #       x part of integral = a*t*Asinh(t/a) + b*t - a*sqrt(a^2+t^2)
    #       y part of integral = 1/2 t (sqrt(t^2+a^2) + 2*c) + 1/2 a^2 Asinh(t/a)
    # XXX still simplifying... not sure what the point is though, switching horses to the "good" method
    #


  weil_moment_from_slack_and_angle(slack,angle,v0,v1) = \
      weil_moment_from_slack_and_angle_helper1(angle,abs(v1-v0)+slack, \
                                               real(v0 * (cos(-angle) + i*sin(-angle))), \
                                               imag(v0 * (cos(-angle) + i*sin(-angle))), \
                                               real(v1 * (cos(-angle) + i*sin(-angle))), \
                                               imag(v1 * (cos(-angle) + i*sin(-angle))))
    # paper only works if x0,y0 is the *lower* end, for some reason
    weil_moment_from_slack_and_angle_helper1(angle,L,x0,y0,x1,y1) = \
      y1>=y0 ? weil_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1) \
             : xconj(weil_moment_from_slack_and_angle_helper2(-angle,L,-x1,y1,-x0,y0))
    weil_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1) = \
        weil_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1, \
                                            (x1-x0)/(2*asinhc(sqrt(L**2 - (y1-y0)**2) / (x1-x0))))  # = a

    weil_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,a) =  \
        weil_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a, \
                                            (x0+x1)/2. - a*acosh(L / (a*sqrt(2*(cosh((x1-x0)/a) - 1)))))  # = b

    # using instead my magic b = (x0+x1)/2. - a*xmid_from_a_and_b((x1-x0)/a, (y1-y0)/a)  (where a_and_b mean different from a and b here)
    #                          = (x0+x1)/2. - a*Asinh((y1-y0)/a/2./sinh((x1-x0)/a/2.))
    weil_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,a) =  \
        weil_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a, \
                                            (x0+x1)/2. - a*Asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))) # = b

    weil_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a,b) = \
        weil_moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,a,b, \
                                            y0 - a*cosh((x0-b)/a))  # = c
    weil_moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,a,b,c) = \
        weil_moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,a,b,c, \
                                            a*sinh((x0-b)/a), a*sinh((x1-b)/a))  # = t0,t1 ... but this is the inherently unstable part of Weil's and Barzel's method, I think




    weil_moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,a,b,c,t0,t1) = \
        weil_rotate_xy_by_angle(weil_x_part_of_integral(a,b,c,t1) - weil_x_part_of_integral(a,b,c,t0), \
                           weil_y_part_of_integral(a,b,c,t1) - weil_y_part_of_integral(a,b,c,t0), \
                           angle)
      weil_x_part_of_integral(a,b,c_unused,t) = a * (t*(Asinh(t/a) + b/a) - sqrt(t**2 + a**2))
      weil_y_part_of_integral(a,b_unused,c,t) = .5*t*(sqrt(t**2+a**2) + 2*c) + .5*a**2*Asinh(t/a)
      weil_rotate_xy_by_angle(x,y,angle) = x*cos(angle)-y*sin(angle) \
                                        + (x*sin(angle)+y*cos(angle)) * i

    #
    # The rest is the same as in previous method except I might want to change
    # the criterion for using the 1d version depending on how robust this is.
    # If I can make it totally robust, then no need to switch methods at all.
    #

    # analytic version to use when on x axis
    weil_moment_from_x(slack,v0,v1) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
    # I suspect the weil version can be demonstrated to behave poorly near the x axis...
    weil_moment_from_xy(x,y,v0,v1) = y==0. ? weil_moment_from_x(x,v0,v1) : y>0 ? conj(_weil_moment_from_xy(x,-y,conj(v0),conj(v1))) : _weil_moment_from_xy(x,y,v0,v1)
    _weil_moment_from_xy(x,y,v0,v1) = weil_moment_from_slack_and_angle(weil_slack_from_xy(x,y), weil_angle_from_xy(x,y), v0,v1)
      weil_slack_from_xy(x,y) = sqrt(x**2+y**2)
      weil_angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
      #weil_angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
} # setup for "weil"

{
  # Try a new strategy that I think is more robust:
  # it should be able to handle catScale=0 and close to it,
  # without a completely special case.
  # We parametrize the catenary by arc length as follows:
  #   x(t) = s*Asinh(t/s) + b
  #   y(t) = s*sqrt((t/s)^2 + 1) + c
  #        = sqrt(t^2 + s^2) + c
  # for t=t0 to t=t1.
  # Given x0,y0,x1,y1,L=1+slack,
  # we want to find s,b,c,t0,t1
  # such that:
  #     t1-t0 == L
  #     x(t0)=x0,y(t0)=y0
  #     x(t1)=x1,y(t1)=y1
  # We find s and b as in previous methods (where s was called "a" or "catScale").
  # Then we still need to compute t0,t1,c.
  # For t0 and t1, call their midpoint tMid = (t0+t1)/2.
  # then tMid must satisfy:
  #   y0 = y(tMid-L/2) = sqrt((tMid-L/2)^2 + s^2) + c
  #   y1 = y(tMid+L/2) = sqrt((tMid+L/2)^2 + s^2) + c
  # Subtract to get:
  #   y1-y0 = sqrt((tMid+L/2)^2+s^2) - sqrt((tMid-L/2)^2+s^2)
  # Paste into wolframalpha:
  #   "solve (Y == sqrt((t+L/2)^2+s^2) - sqrt((t-L/2)^2+s^2)) for t"
  # That produces:
  #   tMid = .5*(y1-y0)*sqrt((L^2-Y^2+4*s^2)/(L^2-Y^2))
  #        = .5*(y1-y0)*sqrt(1 + 4*s^2/(L^2-Y^2))
  # Then:
  #   t0=tMid-L/2
  #   t1=tMid+L/2.
  # Finally, to find c, solve:
  #   y0 = y(t0) = sqrt(t0^2 + s^2) + c
  #   c = y0 - sqrt(t0^2 + s^2)
  # 
  # So, the moment will be the integral of x,y from t=t0 to t=t1.
  # According to wolframalpha:
  #       x part of integral = s*t*Asinh(t/s) + b*t - s*sqrt(s^2+t^2)
  #       y part of integral = 1/2 t (sqrt(t^2+s^2) + 2*c) + 1/2 s^2 log(sqrt(t^2+s^2) + t)
  # but we can turn log(sqrt(t^2+s^2) + t) into hyperbolic trig as follows:
  #         log(sqrt(t**2+s**2) + t)
  #       = log(s*(sqrt((t/s)^2 + 1) + t/s))
  #       = log(s) + log(sqrt((t/s)^2+1) + t/s)
  #       = log(s) + Asinh(t/s)
  # and the log(s) gets absorbed into the integration constant. Yay! So:
  #       x part of integral = s*t*Asinh(t/s) + b*t - s*sqrt(s^2+t^2)
  #       y part of integral = 1/2 t (sqrt(t^2+s^2) + 2*c) + 1/2 s^2 Asinh(t/s)
  #
  # XXX In the case of slack=0 or near 0, might be more stable to compute t0,t1 from x's instead of from y's

  good_moment_from_slack_and_angle(slack,angle,v0,v1) = \
      good_moment_from_slack_and_angle_helper1(angle,abs(v1-v0)+slack, \
                                               real(v0 * (cos(-angle) + i*sin(-angle))), \
                                               imag(v0 * (cos(-angle) + i*sin(-angle))), \
                                               real(v1 * (cos(-angle) + i*sin(-angle))), \
                                               imag(v1 * (cos(-angle) + i*sin(-angle))))

    good_moment_from_slack_and_angle_helper1(angle,L,x0,y0,x1,y1) = \
        good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1, \
                                            (x1-x0)/(2*asinhc(sqrt(L**2 - (y1-y0)**2) / (x1-x0))))  # = s  XXX divides by x1-x0, need to reformulate to be well behaved.  should just produce 0 in that case I think?




    good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1,s) =  \
        good_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,s, \
                                            (x0+x1)/2. - s*acosh(L / (s*sqrt(2*(cosh((x1-x0)/s) - 1)))))  # = b    XXX divides by s, need to reformulate to be well behaved.  should just produce (x0+x1)/2. in that case I think?

    # using instead my magic b = (x0+x1)/2. - s*xmid_from_a_and_b((x1-x0)/s, (y1-y0)/s)  (where a_and_b mean different from s and b here)
    #                          = (x0+x1)/2. - s*Asinh((y1-y0)/(2.*s)/sinh((x1-x0)/(2.*s)))
    good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1,s) =  \
        good_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,s, \
                                            (x0+x1)/2. - s*Asinh((y1-y0)/(2.*s*sinh((x1-x0)/(2.*s)))))  # = b    XXX divides by s, need to reformulate to be well behaved.  should just produce (x0+x1)/2. in that case I think?

    good_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,s,b) = \
        good_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,s,b, \
                                            .5*(y1-y0)*sqrt(1+4*s**2/(L**2-(y1-y0)**2)))  # = tMid
    good_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,s,b,tMid) = \
        good_moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,s,b, tMid-L/2., tMid+L/2.)  # = t0,t1

    good_moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,s,b,t0,t1) = \
        good_moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,s,b, \
                                            y0 - sqrt(t0**2 + s**2), t0,t1)  # = c,t0,t1


    good_moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,s,b,c,t0,t1) = \
        good_rotate_xy_by_angle(good_x_part_of_integral(s,b,c,t1) - good_x_part_of_integral(s,b,c,t0), \
                           good_y_part_of_integral(s,b,c,t1) - good_y_part_of_integral(s,b,c,t0), \
                           angle)

      # XXX keep thinking about this... it's pretty good now but I'm not sure if it's perfect.
      # XXX actually... why did I need a special case for t==0? won't it just come out right?
      s_times_asinh_t_over_s(t,s) = s==0.||t==0. ? 0. : s*Asinh(t/s)

      good_x_part_of_integral(s,b,c_unused,t) = t*(b + s_times_asinh_t_over_s(t,s)) - s*sqrt(t**2 + s**2)
      good_y_part_of_integral(s,b_unused,c,t) = t*(c + .5*sqrt(t**2+s**2)) + .5*s*s_times_asinh_t_over_s(t,s)
      good_rotate_xy_by_angle(x,y,angle) = x*cos(angle)-y*sin(angle) \
                                   + (x*sin(angle)+y*cos(angle)) * i

    #
    # The rest is the same as in previous method except I might want to change
    # the criterion for using the 1d version depending on how robust this is.
    # If I can make it totally robust, then no need to switch methods at all.
    #

    # analytic version to use when on x axis
    good_moment_from_x(slack,v0,v1) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
    #good_moment_from_xy(x,y,v0,v1) = y==0. ? good_moment_from_x(x,v0,v1) : y>0 ? conj(_good_moment_from_xy(x,-y,conj(v0),conj(v1))) : _good_moment_from_xy(x,y,v0,v1)

    # XXX hmm, actually don't need the 1-d case any more? great!
    # XXX hmm, still need to check x==0&&y==0 for some reason, oh well
    good_moment_from_xy(x,y,v0,v1) = x==0.&&y==0. ? (v0+v1)/2.*abs(v1-v0) : y>0 ? conj(_good_moment_from_xy(x,-y,conj(v0),conj(v1))) : _good_moment_from_xy(x,y,v0,v1)

    _good_moment_from_xy(x,y,v0,v1) = good_moment_from_slack_and_angle(good_slack_from_xy(x,y), good_angle_from_xy(x,y), v0,v1)
      good_slack_from_xy(x,y) = sqrt(x**2+y**2)
      good_angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
      #good_angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
} # setup for "good"

  # note angle is really angle starting from straight down, I think
  slack_and_angle_to_angle0or1(which,slack,angle,v0,v1) = \
      slack_and_angle_to_angle0or1_helper1(which,angle,abs(v1-v0)+slack, \
                                        real(v0 * (cos(-angle) + i*sin(-angle))), \
                                        imag(v0 * (cos(-angle) + i*sin(-angle))), \
                                        real(v1 * (cos(-angle) + i*sin(-angle))), \
                                        imag(v1 * (cos(-angle) + i*sin(-angle))))

  slack_and_angle_to_angle0or1(which,slack,angle,v0,v1) = \
      ( \
      assert(slack >= 0), \
      slack_and_angle_to_angle0or1_helper1(which,angle,abs(v1-v0)+slack, \
                                        real(v0 * (cos(-angle) + i*sin(-angle))), \
                                        imag(v0 * (cos(-angle) + i*sin(-angle))), \
                                        real(v1 * (cos(-angle) + i*sin(-angle))), \
                                        imag(v1 * (cos(-angle) + i*sin(-angle)))))

    slack_and_angle_to_angle0or1_helper1(which,angle,L,x0,y0,x1,y1) = \
        _helper2(which,angle,L,x0,y0,x1,y1, \
                                            (x1-x0)/(2*asinhc(sqrt(L**2 - (y1-y0)**2) / (x1-x0))))  # = s  XXX divides by x1-x0, need to reformulate to be well behaved.  should just produce 0 in that case I think?
    _helper2(which,angle,L,x0,y0,x1,y1,s) =  \
        _helper3(which,angle,L,x0,y0,x1,y1,s, \
                                            (x0+x1)/2. - s*acosh(L / (s*sqrt(2*(cosh((x1-x0)/s) - 1)))))  # = b    XXX divides by s, need to reformulate to be well behaved.  should just produce (x0+x1)/2. in that case I think?
    _helper3(which,angle,L,x0,y0,x1,y1,s,b) = \
        _helper4(which,angle,L,x0,y0,x1,y1,s,b, \
                                            .5*(y1-y0)*sqrt(1+4*s**2/(L**2-(y1-y0)**2)))  # = tMid
    _helper4(which,angle,L,x0,y0,x1,y1,s,b,tMid) = \
        _helper5(which,angle,L,x0,y0,x1,y1,s,b, tMid-L/2., tMid+L/2.)  # = t0,t1
    _helper5(which,angle,L,x0,y0,x1,y1,s,b,t0,t1) = \
      0?asinh(t0/s):\
        angle + (which==0 ? atan(asinh(t0/s)) : atan(asinh(t1/s))-pi)

  slack_and_angle0or1_to_angle(which,slack,angle0or1,v0,v1) = \
      slack_and_angle0or1_to_angle_helper1(which,slack,angle0or1,v0,v1,-pi/2.,pi/2.)
  slack_and_angle0or1_to_angle(which,slack,angle0or1,v0,v1) = \
      ( \
      slack_and_angle0or1_to_angle_helper1(which,slack,angle0or1,v0,v1,-pi/2.,pi/2.))
    slack_and_angle0or1_to_angle_helper1(which,slack,angle0or1,v0,v1,lo,hi) = \
      (lo+hi)/2.<=lo||(lo+hi)/2.>=hi ? (lo+hi)/2. : \
      slack_and_angle_to_angle0or1(which,slack,(lo+hi)/2.,v0,v1) < angle0or1 ? \
      slack_and_angle0or1_to_angle_helper1(which,slack,angle0or1,v0,v1,(lo+hi)/2.,hi) : \
      slack_and_angle0or1_to_angle_helper1(which,slack,angle0or1,v0,v1,lo,(lo+hi)/2.)

  moment_from_xy_through_angle0or1(which,x,y,v0,v1) = y>0 ? conj(moment_from_xy_through_angle0or1_helper1(which,x,-y,conj(v0),conj(v1))) : moment_from_xy_through_angle0or1_helper1(which,x,y,v0,v1)

    moment_from_xy_through_angle0or1_helper1(which,x,y,v0,v1) = moment_from_slack_and_angle0or1(which,sqrt(x**2+y**2),atan2(y,x),v0,v1)
    moment_from_slack_and_angle0or1(which,slack,angle0or1,v0,v1) = good_moment_from_slack_and_angle(slack,slack_and_angle0or1_to_angle(which,slack,angle0or1,v0,v1),v0,v1)


print a=rtod(slack_and_angle_to_angle0or1(0,100.,dtor(9.),velocity0,velocity1))
print b=rtod(slack_and_angle_to_angle0or1(1,100.,dtor(9.),velocity0,velocity1))
print rtod(slack_and_angle0or1_to_angle(0,100.,dtor(a),velocity0,velocity1))
print rtod(slack_and_angle0or1_to_angle(1,100.,dtor(b),velocity0,velocity1))

if (strategy eq "weil") {
  moment_from_xy(x,y,v0,v1) = weil_moment_from_xy(x,y,v0,v1)
}
if (strategy eq "good") {
  moment_from_xy(x,y,v0,v1) = good_moment_from_xy(x,y,v0,v1)
}

#print moment_from_xy(0,-.01)
#print moment_from_xy(0,-.1)
#print moment_from_xy(0,-1)
#print moment_from_xy(0,-2)
#print moment_from_xy(0,-10)

f(z) = moment_from_xy(real(z),imag(z),velocity0,velocity1)
wf(z) = weil_moment_from_xy(real(z),imag(z),velocity0,velocity1)
gf(z) = good_moment_from_xy(real(z),imag(z),velocity0,velocity1)
f0(z) = moment_from_xy_through_angle0or1(0,real(z),imag(z),velocity0,velocity1)
f1(z) = moment_from_xy_through_angle0or1(1,real(z),imag(z),velocity0,velocity1)

unstretched_moment_from_xy(x,y,v0,v1) = squashBy(moment_from_xy(x,y,v0,v1), squash_from_xy(x,y,v0,v1))
  squash_from_xy(x,y,v0,v1) = squash_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0,v1)
    squash_from_slack_and_angle(slack, angle, v0,v1) = slack==0 ? 1. : (moment_from_x(slack,v0,v1)-moment_from_x(-slack,v0,v1))/(-2*imag(moment_from_slack_and_angle(slack,0,v0,v1)))
  squashBy(z,squash) = real(z) + imag(z)*squash*{0,1}
# uncomment this to see the almost-circles
# XXX make this a legit parameter!
#f(z) = unstretched_moment_from_xy(real(z),imag(z),velocity0,velocity1)


# bold experiment: try newton's method in the plane.
# uses hard coded function: f_to_invert. (since unfortunately a function can't be passed
# as a parameter to another function)
# for derivative, uses finite difference with given epsilon.
f_to_invert(z) = f(z)

crashed = 0
invf(y, epsilon) = crashed ? NaN : (traceString="", invf_recurse(y, epsilon, imag(y)>0 ? {0,.01} : {0,.01}, NaN, NaN, 100))
  # crash when at limit
  invf_recurse(y, epsilon, x, xPrev, xPrevPrev, maxRecursions) = ((traceString=traceString.sprintf("    y=%s x=%s progress=%s prevProgress=%s\n", EXACT(y), EXACT(x), EXACT(abs(x-xPrev)), EXACT(abs(xPrev-xPrevPrev)))), x!=x ? NaN : maxRecursions==0 ? (crashed=1,crashedTraceString=traceString,NaN) : x==xPrev||x==xPrevPrev||abs(x-xPrev)>abs(xPrev-xPrevPrev) ? x : invf_recurse_helper(y, epsilon, x, xPrev, f_to_invert(x), maxRecursions))
  # experiment with just returning x when at limit
  invf_recurse(y, epsilon, x, xPrev, xPrevPrev, maxRecursions) = ((traceString=traceString.sprintf("    y=%s x=%s progress=%s prevProgress=%s\n", EXACT(y), EXACT(x), EXACT(abs(x-xPrev)), EXACT(abs(xPrev-xPrevPrev)))), x!=x ? NaN : maxRecursions==0 ? x : x==xPrev||x==xPrevPrev||abs(x-xPrev)>abs(xPrev-xPrevPrev) ? x : invf_recurse_helper(y, epsilon, x, xPrev, f_to_invert(x), maxRecursions))
  #FUDGE = .5 # go only partway there; makes it more stable
  FUDGE = .25 # go only partway there; makes it more stable
  #FUDGE = .125 # go only partway there; makes it more stable
  #FUDGE = 1./16 # go only partway there; makes it more stable
  invf_recurse_helper(y, epsilon, x, xPrev, fx, maxRecursions) = invf_recurse(y, epsilon, x - FUDGE*(fx-y)/((f_to_invert(x+epsilon)-fx)/epsilon), x, xPrev, maxRecursions-1)

if (0) { # turn this on to debug invf
    traceString = ""
    crashedTraceString = ""
    print invf({0,1}, 1e-4)
    print "traceString = ", traceString
}


# Test whether the non-ellipses that look like ellipses are at least left-right symmetric.
# They are! (and actually this is obvious, see comment at top of this file)
if (0) { # turn this on to demonstrate
    print "f({0,1}) = ",f({0,1})
    print "f({1,0})-f({0,1}) = ",f({1,0})-f({0,1})
    print "f({-1,0})-f({0,1}) = ",f({-1,0})-f({0,1})
    print "====="
    print "f({0,2}) = ",f({0,2})
    print "f({2,0})-f({0,2}) = ",f({2,0})-f({0,2})
    print "f({-2,0})-f({0,2}) = ",f({-2,0})-f({0,2})
    print "====="
    print "f({0,5}) = ",f({0,5})
    print "f({3,4})-f({0,5}) = ",f({3,4})-f({0,5})
    print "f({-3,4})-f({0,5}) = ",f({-3,4})-f({0,5})
    print "f({4,3})-f({0,5}) = ",f({4,3})-f({0,5})
    print "f({-4,3})-f({0,5}) = ",f({-4,3})-f({0,5})
}

# Interesting colors but not sure it's completely useful...
# Trouble making it interact well with the lines.
#set pm3d
set pm3d depthorder  # applies to either "set pm3d" if we did it above, or "with pm3d" if we do that below

# Default is no further subdivisions (i.e. divide into 1 part),
# but we tweak this below if using hidden3d.
magFurtherSubdivisions = 1
angleFurtherSubdivisions = 1

# Hidden3d is beautiful, however it makes it so that samples are ignored and only isosamples are used, so it sucks :-(
# If we want to use it, probably want to uncomment all three of the following lines at once.
#set hidden3d
#magFurtherSubdivisions = 8
#angleFurtherSubdivisions = 2

magBase = 2**(1./magFurtherSubdivisions) # each mag level is an integer power of magBase
minMag = -8 * magFurtherSubdivisions
maxMag = 4 * magFurtherSubdivisions
nAngles = 16 * angleFurtherSubdivisions # divide 360 degrees into this many parts

u0 = minMag * log(magBase)
u1 = maxMag * log(magBase)
v0 = 0
v1 = 2*pi

#r = 200
#r = 100
#r = 50
#r = 20
#r = 10
#r = 8.5 # 4 on the right
r = 3.5 # 2 on the right, 4 on the left (this is a good one)
#r = 1.75 # 1 on the right
#r = 1
#r = .5
#r = .25
#r = .125
#r = 1./16
#r = 1./32
#r = 1./64
#r = 1./128
#r = 1./256
#r = 1./512
#r = 1./1024
#r = 1./2048

x0 = (r <= 1 ? real(velocity0)+.5 : 0) - r
x1 = (r <= 1 ? real(velocity0)+.5 : 0) + r
y0 = (r <= 1 ? imag(velocity0) : 0) -r
y1 = (r <= 1 ? imag(velocity0) : 0) + r
#z0 = 0
#z1 = 10*r # XXX need to figure out the right adjustment here based on r... 2*r seems right usually but it's wrong when r is tiny
#z1 = r # XXX need to figure out the right adjustment here based on r... 2*r seems right usually but it's wrong when r is tiny
z0 = 0
z1 = 1


set samples 4*(maxMag-minMag)+1,10*nAngles+1
set isosamples (maxMag-minMag)+1,nAngles+1
set parametric
set zeroaxis # show axes as dotted lines

#lw = 2 # nice for viewing
#lw = 1
lw = .5  # seems to be optimal for information I think
#lw = .25 # just gets lighter

tics = r<=.5 ? r/4. : r<=1 ? r/4. : r<=1.75 ? 1./8 : r<=3.5 ? 1./2 : r<=8.5 ? 1 : r<=10 ? 1 : r<=20 ? 1 : r/10 # kind of weird
set xtics tics
set ytics tics

# XXX I have no idea what the fuck I'm doing here
set palette defined (0 "red", .09 "red", .1 "blue", .13 "red", 1 "red")


#zscale = 0
#zscale = 10
zscale = .1

if (plotf0_flag) {
    if (png_flag) {
        set output "RMMEf0.png"
    }
    print "doing f0 plot..."
    time0 = time(0.)
    splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f0(exp(u+i*v))),imag(f0(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw
    time1 = time(0.)
    print sprintf("f0 splot took %.6f seconds.", (time1-time0))
}
if (plotf1_flag) {
    if (png_flag) {
        set output "RMMEf1.png"
    }
    print "doing f1 plot..."
    time0 = time(0.)
    splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f1(exp(u+i*v))),imag(f1(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw
    time1 = time(0.)
    print sprintf("f1 splot took %.6f seconds.", (time1-time0))
}

if (plot1_flag) {
    if (png_flag) {
        set output "RMME1.png"
    }

    # change from lines to linespoints to debug
    print "doing first plot..."
    time0 = time(0.)
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with linespoints linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with linespoints linewidth lw, real(f(exp(0*u+i*v))),imag(f(exp(0*u+i*v))),exp(u) with linespoints linewidth lw # hacky way to get green on center contour
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with lines linewidth lw

    # Nice, blue on the 1 contour
    splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw

    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with dots linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with pm3d
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with lines linewidth lw, real(exp(u+i*v)),imag(exp(u+i*v)),exp(u) # best-fit circles when symmetric picture

    # weil's in red (with blue slack=1 contour) offset by a little, good in green
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(.01+wf(exp(u+i*v))),imag(wf(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw, \
    #                                          real(gf(exp(u+i*v))),imag(gf(exp(u+i*v))),zscale*exp(u) with lines linewidth lw
    # weil's in red thick, good in green thin
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(wf(exp(u+i*v))),imag(wf(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw+4, \
    #                                          real(gf(exp(u+i*v))),imag(gf(exp(u+i*v))),zscale*exp(u) with lines linewidth lw

    time1 = time(0.)
    print sprintf("first splot took %.6f seconds.", (time1-time0))
}


#=================================================================================================

if (plot2_flag) {
    if (png_flag) {
        set output "RMME2.png"
    }

    magBase = 2**(1./magFurtherSubdivisions) # each mag level is an integer power of magBase
    minMag = -8 * magFurtherSubdivisions
    maxMag = 4 * magFurtherSubdivisions
    #nAngles = 16 * angleFurtherSubdivisions # divide 360 degrees into this many parts
    nAngles = 8 * angleFurtherSubdivisions # divide 360 degrees into this many parts
    set samples 4*(maxMag-minMag)+1,10*nAngles+1
    set isosamples (maxMag-minMag)+1,nAngles+1

    u0 = minMag * log(magBase)
    u1 = maxMag * log(magBase)
    v0 = 0
    v1 = 2*pi
    #r = 4
    r = 2
    #r = .5

    # Empirically, the center is approximately:
    #       velocity0=-1.5 -> center = 3.25
    #       velocity0=-1 -> center = 1.5
    #       velocity0=-.5 -> center = 0  (symmetric)
    #       velocity0=0 -> center=-1.5   (normal)
    #       velocity0=.5 -> center=-3.25
    # so it's something like: (velocity0+.5)*-3.25
    x0 = (r <= 2 ? real(velocity0+.5)*-3.25 : 0) - r
    x1 = (r <= 2 ? real(velocity0+.5)*-3.25 : 0) + r
    y0 = (r <= 2 ? imag(velocity0+.5)*-3.25 : 0) -r
    y1 = (r <= 2 ? imag(velocity0+.5)*-3.25 : 0) + r

    tics = r<=.5 ? r/4. : r<=1 ? r/8. : r<=1.75 ? 1./8 : r<=3.5 ? 1./2 : r<=8.5 ? 1 : r<=10 ? 1 : r<=20 ? 1 : r/10 # kind of weird
    set xtics tics
    set ytics tics




    g(z) = invf(z, 1e-4)
    print "doing second plot (inverse)..."
    time0 = time(0.)
    crashed = 0
    traceString = ""
    crashedTraceString = ""
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(g(exp(u+i*v))),imag(g(exp(u+i*v))),0 with linespoints linewidth lw
    splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(g(exp(u+i*v))),imag(g(exp(u+i*v))),0 with lines linewidth lw
    time1 = time(0.)
    print sprintf("second splot took %.6f seconds.", (time1-time0))
    print "crashed = ",crashed
    #print "traceString = ",traceString
    print "crashedTraceString = ",crashedTraceString
} # if plot2_flag

if (plot3_flag) {
  # very experimental
  set parametric
  set samples 1001
  set terminal png size 1000,1000
  set output "RMME3.png"

  # bleah! newton crashes, halley crashes, binary search endless loops, wtf?
  #asinhc(y) = asinhc_by_newton(y)
  asinhc(y) = asinhc_by_binary_search_lo(y)
  #asinhc(y) = asinhc_by_halley(y)

  degrees = 90
  degrees = 45
  g(x,degrees) = abs(f(x*exp(i*degrees*pi/180.)) - .5)
  #q = 1e-5
  #q = 1e-4
  q = 1e-1
  #q = 1
  #q = 1e1
  time0 = time(0.)

  #
  # Experimenting with scaling/translating in 2nd quadrant
  #
  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,90), t,g(t**exponent,90), t,g(t,90)**exponent
  ## conclusion: for 90 degrees, square it first or afterwards

  #window = 1
  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,170), t,g((t/window)**exponent*window,170), t,(g(t,170)/window)**exponent*window

  #exponent = 2.
  exponent = 1.5
  angle = 179
  plot [0:q] [-q:q] [-q:q] t,g(t,angle), t,g(t**exponent,angle), t,g(t,angle)**exponent

  # want: square when small, square root when large
  # that's because the function seems to be the sum of a square root and a square... or something?
  # wow, wolframalpha gives a functional inverse of x^2+sqrt(x), but it's very hairy.

  # somehow need to leverage the fact that raising to any power > 1 gives zero deriv
  # (but with small window if close to 1)
  # and raising to any power < 1 gives infinite deriv
  # (but with small window if close to 1.
  # And, for angles close to 180,
  # what I am seeing is: infinite deriv, with small window.
  # In other words, it looks like it's been raised to .99 power or something...
  # so solution is to raise to 1.01 power or something, right???
  # If we raise to too big a power, we'll get zero deriv...
  # if we don't raise high enough, it will still be infinite deriv.
  # But finding the right one is tricky, since window is small.

  # Bleah! for angle 179...
  # Empirically, exponent > 2 gives too flat....

  # Let's see, can I be more principled about it?
  # Approximate the inverse function (i.e. from moment to slack xy) with a polynomial with zero linear term,
  # and invert it?
  # Can do that with a quadratic... and maybe even with a cubic (simpler than general since zero linear term?)





  #
  # Experimenting in 1st quadrant
  #
  #plot [0:q] [-q:q] [-q:q] t,g(t,0)
  # conclusion: for 0 degrees, just take the function

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,90), t,g(t**exponent,90), t,g(t,90)**exponent
  # conclusion: for 90 degrees, square it first or afterwards

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,45), t,g(t**exponent,45), t,g(t,45)**exponent
  # conclusion: for 90 degrees, square it first or afterwards

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,10), t,g(t**exponent,10), t,g(t,10)**exponent
  # conclusion: for 10 degrees, square it first or afterwards.
  # wtf? this can't be right all the way down to 0!
  # oh argh, maybe it actually is, it's just the region of influence gets smaller?

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,5), t,g(t**exponent,5), t,g(t,5)**exponent

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,2), t,g(t**exponent,2), t,g(t,2)**exponent

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,1), t,g(t**exponent,1), t,g(t,1)**exponent

  #exponent = 1.
  #plot [0:q] [-q:q] [-q:q] t,g(t,0), t,g(t**exponent,0), t,g(t,0)**exponent

  #
  # Experimenting in 2nd quadrant
  #

  #exponent = .5
  #plot [0:q] [-q:q] [-q:q] t,g(t,180), t,g(t**exponent,180), t,g(t,180)**exponent
  # conclusion: for 180 degrees, square root first or afterwards

  #exponent = 2.
  #plot [0:q] [-q:q] [-q:q] t,g(t,135), t,g(t**exponent,135), t,g(t,135)**exponent
  # conclusion: for 135 degrees, square it first or afterwards



  # OVERALL CONCLUSION:
  # It *always* works to square it first or afterwards (since slope is infinite)
  # except when exactly 0 degrees (in which case slope is finite-- leave alone)
  # or exactly 180 degrees (in which case slope is 0-- take square root)
  # ARGH can I do better?  Something with scaling and/or translating?
  # For example, the 0 degrees case isn't *really* unit, it actually needs to be square rooted,
  # but with offset.
  # I think the real answer is... the function is some mix of square root (on small scale)
  # and square (on large scale)...
  # so the correction has to be some mix of square root (on large scale) and square (on increasingly
  # small scale).
  #
  # Observation: if we just blindly square it,
  # then it's good in the limit under a microscope,
  # but it's not very useful macroscopically.
  # But, can we just add a linear term, making it good everywhere???
  # That might work for second quadrant...
  # But what about first quadrant, especially near and at 0, where we do *not* want to square, we want
  # to take square root (in some sense)?
  # No wait... maybe it's still all good!?
  # If the thing starts out flat, adding t doesn't hurt... ?
  # Weird.
  # No, doesn't help unless linear term of x is added in moment space... but then impossible to invert.


  #plot [0:q] [-q:q] [-q:q] \
  #    t,g(t,0), \
  #    t,g(t,45), \
  #    t,g(t,90), \
  #    t,g(t,135), \
  #    t,g(t,180.*7/8), \
  #    t,g(t,180.*15/16), \
  #    t,g(t,180.*31/32), \
  #    t,g(t,180), \
  #    0,0 title ''


  time1 = time(0.)
  print sprintf("third plot took %.6f seconds.", (time1-time0))
}

if (!png_flag) {
  pause -1 "Hit Enter or Ctrl-c to exit: " # wait til user hits Enter or ctrl-c
}
