#!/usr/bin/gnuplot
# note, some day I think there will be a -c option that will allow args to be passed in more easily, but not today.

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

    png_flag = 1 # if set, output to RMME1.png and RMME2.png instead of terminal

    #velocity0 = -sqrt(.5) + -sqrt(.5)*{0,1}
    #velocity0 = -sqrt(.5) + 1.1 * -sqrt(.5)*{0,1}

    #velocity0 = {-1.5,0}
    #velocity0 = {-1,0}
    #velocity0 = {-.5,0} # symmetric about origin: {-.5,0} to {.5,0}
    velocity0 = {0,0}   # normal, from {0,0} to {1,0}
    #velocity0 = {.5,0}

    # when png_flag is set, both of the following can be set.
    # otherwise it makes sense to set at most one of them.
    plot1_flag = 1  # slack-and-angle to moment
    plot2_flag = 0  # moment to slack-and-angle

    alternate_plot1_flag = 0 # if set, use alternate formulation (non-slack-based invCatScale) for plot1

    # any of the following should work.
    #strategy = "first"
    strategy = "weil" # sets f = wf
    #strategy = "good" # sets f = gf
     

if (png_flag) {
    set terminal png size 600,600
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


if (strategy eq "first") {
    first_a_from_angle_and_invCatScale(angle, invCatScale) = invCatScale*cos(-angle)
    first_b_from_angle_and_invCatScale(angle, invCatScale) = invCatScale*sin(-angle)
    # surprising magic.
    first_xmid_from_a_and_b(a,b) = Asinh(b/2./sinh(a/2.))
    # I wish it simplified further though!
    # Basic case of it is Asinh(1/sinh(x)). Does that simplify?
    #       Asinh(y) = log(y + sqrt(1+y^2))
    #       sinh(x) = (e^x-e^-x)/2
    #       Asinh(1/sinh(x)) = log(2/(e**x-e**-x) + sqrt(1+(2/(e**x-e**-x))**2))
    #                        = log(2/X + sqrt(1+(2/X)^2))
    #                        = log(X + sqrt(1+X^2)) where X = 1/sinh(x).  bleah! not getting anywhere. but wait...
    #                        = log(X*(1 + sqrt(1/X^2 + 1)))
    #                        = log(1/sinh(x) * (1 + sqrt(sinh(x)**2+1)))
    #                        = log((1 + cosh(x)) / sinh(x))
    #                        = log(1./sinh(x) + cosh(x)/sinh(x))
    #                        = log(csch(x) + coth(x))
    # So it does sort of simplify... ?
    # Okay let's do the non-basic case. Let B=b/2, A=a/2.
    #       Asinh(B/sinh(A)) = log(B/sinh(A) + sqrt(1 + (B/sinh(A))^2))
    #                        = log((B + sqrt(sinh(A)^2 + B^2)) / sinh(A))
    # bleah, it's getting messier than the basic case I think :-(
    # That's because sqrt(sinh(x)^2+1) simplifies to cosh(x) but sqrt(sinh(x)^2+c) doesn't?
    # Wait but...
    #       sqrt(sinh(x)**2+2) is sqrt(cosh(x)**2+1)
    # that's just because sinh(x)**2 and cosh(x)**2 differ by 1.
    # Hmph!  I think this will lead nowhere :-(
    #

    #
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
    # In our case, L = 1+slack, a=cos(-angle), b=sin(-angle).
    first_invCatScale_from_slack_and_angle(slack, angle) = first_invCatScale_from_L_and_a_and_b(1.+slack, cos(-angle), sin(-angle))
      first_invCatScale_from_L_and_a_and_b(L, a, b) = asinhc(sqrt(L**2 - b**2) / a) * (2./a)

    first_x0_from_a_and_b(a,b) = first_xmid_from_a_and_b(a,b) - a/2.
    first_x1_from_a_and_b(a,b) = first_xmid_from_a_and_b(a,b) + a/2.
    first_y0_from_a_and_b(a,b) = cosh(first_x0_from_a_and_b(a,b))
    first_y1_from_a_and_b(a,b) = cosh(first_x1_from_a_and_b(a,b))
    first_t0_from_a_and_b(a,b) = sinh(first_x0_from_a_and_b(a,b))  # = sinh(Asinh(b/2./sinh(a/2.)) - a/2.)
    first_t1_from_a_and_b(a,b) = sinh(first_x1_from_a_and_b(a,b))  # = sinh(Asinh(b/2./sinh(a/2.)) + a/2.)




    # analytic version to use when on x axis
    first_moment_from_x(slack,v0) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
    first_moment_from_xy(x,y,v0) = y==0. ? first_moment_from_x(x,v0) : y>0 ? conj(first__moment_from_xy(x,-y,conj(v0))) : first__moment_from_xy(x,y,v0)
    first__moment_from_xy(x,y,v0) = first_moment_from_slack_and_angle(first_slack_from_xy(x,y), first_angle_from_xy(x,y), v0)
      first_slack_from_xy(x,y) = sqrt(x**2+y**2)
      first_angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
      #first_angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing

      first_moment_from_slack_and_angle(slack,angle,v0) = v0*(1.+slack) + first_moment_from_angle_and_invCatScale(angle, first_invCatScale_from_slack_and_angle(slack, angle))
        first_moment_from_angle_and_invCatScale(angle,invCatScale) = first_moment_from_angle_and_invCatScale_and_t0_and_t1(angle,invCatScale, \
                                                                                                               first_t0_from_angle_and_invCatScale(angle,invCatScale), \
                                                                                                               first_t1_from_angle_and_invCatScale(angle,invCatScale))
          first_t0_from_angle_and_invCatScale(angle,invCatScale) = first_t0_from_a_and_b(first_a_from_angle_and_invCatScale(angle,invCatScale), \
                                                                             first_b_from_angle_and_invCatScale(angle,invCatScale))
          first_t1_from_angle_and_invCatScale(angle,invCatScale) = first_t1_from_a_and_b(first_a_from_angle_and_invCatScale(angle,invCatScale), \
                                                                             first_b_from_angle_and_invCatScale(angle,invCatScale))

          # Integrate from t0 to t1 on the canonical catenary with x(t),y(t) translated to the origin,
          # then rotate by -angle and scale by 1/invCatScale^2.
          # That is, integral from t=t0 to t=t1 of:
          #       (x(t)-x(t0),y(t)-y(t0)
          # where:
          #       x(t) = Asinh(t)
          #       y(t) = sqrt(t**2+1)
          first_moment_from_angle_and_invCatScale_and_t0_and_t1(angle,invCatScale,t0,t1) = first_rotate_xy_by_angle(first_x_part_of_integral(t0,t1), \
                                                                                                        first_y_part_of_integral(t0,t1), angle) / invCatScale**2
            first_rotate_xy_by_angle(x,y,angle) = x*cos(angle)-y*sin(angle) \
                                         + (x*sin(angle)+y*cos(angle)) * i
            first_x_part_of_integral(t0,t1) = (t1*Asinh(t1)-sqrt(t1**2+1)) \
                                      - (t0*Asinh(t0)-sqrt(t0**2+1)) \
                                      - (t1-t0)*Asinh(t0)
            first_y_part_of_integral(t0,t1) = .5*(t1*sqrt(t1**2+1)+Asinh(t1)) \
                                      - .5*(t0*sqrt(t0**2+1)+Asinh(t0)) \
                                      - (t1-t0)*sqrt(t0**2+1)
} # strategy eq "first"

{
    #
    # Let's examine the formulation given by Weil and see if it amounts to the same thing.
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
    # XXX Argh! But we still didn't get t0,t1 robustly.  How do we do that??
    # XXX I think maybe we need to get c from first principles, without getting t0,t1 first?  Not sure.
    #     The trick is, need to compute t0,t1,c from y0,y1,a,b, *not* from x0,x1.
    #     Ouch, but wait a minute... it's actually not computable from y0,y1,a,b
    #     in the case when a=0!  In that case we know b = x0==x1 but c,y0,y1 can be anything...
    #     still need L to be in the equation, I think.
    #     Know L = t1-t0

    #       
    #
    # So, the moment will be the integral of x,y from t=t0 to t=t1.
    # According to wolframalpha:
    #       x part of integral = a*t*Asinh(t/a) + b*t - a*sqrt(a^2+t^2)
    #       y part of integral = 1/2 t (sqrt(t^2+a^2) + 2*c)) + 1/2 a^2 log(sqrt(t^2+a^2) + t)
    # but we can turn log(sqrt(t^2+a^2) + t) into hyperbolic trig as follows:
    #         log(sqrt(t**2+a**2) + t)
    #       = log(a*(sqrt((t/a)^2 + 1) + t/a))
    #       = log(a) + log(sqrt((t/a)^2+1) + t/a)
    #       = log(a) + Asinh(t/a)
    # and the log(a) gets absorbed into the integration constant. Yay! So:
    #       x part of integral = a*t*Asinh(t/a) + b*t - a*sqrt(a^2+t^2)
    #       y part of integral = 1/2 t (sqrt(t^2+a^2) + 2*c)) + 1/2 a^2 Asinh(t/a)
    #
    # XXX still simplifying.. and in the end I might just end up with what I had above,
    #     except that I suck at naming things so the above looks messier than it needs to


  weil_moment_from_slack_and_angle(slack,angle,v0) = \
      weil_moment_from_slack_and_angle_helper1(angle,1.+slack, real(v0),imag(v0),real(v0)+cos(-angle),imag(v0)+sin(-angle))
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
    #                          = (x0+x1)/2. - s*Asinh((y1-y0)/((x1-x0)*sinhc((x1-x0)/(2.*s))))
    # (not sure which of the latter two is better if either)
    weil_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,a) =  \
        weil_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a, \
                                            (x0+x1)/2. - a*Asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))) # = b
    weil_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,a) =  \
        weil_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a, \
                                            (x0+x1)/2. - a*Asinh((y1-y0)/((x1-x0)*sinhc((x1-x0)/(2.*a)))))  # = b

    weil_moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a,b) = \
        weil_moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,a,b, \
                                            y0 - a*cosh((x0-b)/a))  # = c
    weil_moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,a,b,c) = \
        weil_moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,a,b,c, \
                                            a*sinh((x0-b)/a), a*sinh((x1-b)/a))  # = t0,t1




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
    weil_moment_from_x(slack,v0) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
    # I suspect the weil version can be demonstrated to behave poorly near the x axis...
    weil_moment_from_xy(x,y,v0) = y==0. ? weil_moment_from_x(x,v0) : y>0 ? conj(_weil_moment_from_xy(x,-y,conj(v0))) : _weil_moment_from_xy(x,y,v0)
    _weil_moment_from_xy(x,y,v0) = weil_moment_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0)
      slack_from_xy(x,y) = sqrt(x**2+y**2)
      angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
      #angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
} # setup for "weil"

{
  # Try a new strategy that I think is more robust than the others:
  # it can handle catScale=0 without a completely special case.
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
  #       y part of integral = 1/2 t (sqrt(t^2+s^2) + 2*c)) + 1/2 s^2 log(sqrt(t^2+s^2) + t)
  # but we can turn log(sqrt(t^2+s^2) + t) into hyperbolic trig as follows:
  #         log(sqrt(t**2+s**2) + t)
  #       = log(s*(sqrt((t/s)^2 + 1) + t/s))
  #       = log(s) + log(sqrt((t/s)^2+1) + t/s)
  #       = log(s) + Asinh(t/s)
  # and the log(s) gets absorbed into the integration constant. Yay! So:
  #       x part of integral = s*t*Asinh(t/s) + b*t - s*sqrt(s^2+t^2)
  #       y part of integral = 1/2 t (sqrt(t^2+s^2) + 2*c)) + 1/2 s^2 Asinh(t/s)

  # XXX but wasn't there a problem near slack=0?  In this case I actually don't think
  # it's possible to compute t unambiguously from y, is it? Because there will be 2 solutions. Maybe.
  # And in any case it's probably more stable to compute it from x in that case anyway.  Maybe.

  good_moment_from_slack_and_angle(slack,angle,v0) = \
      good_moment_from_slack_and_angle_helper1(angle,1.+slack, real(v0),imag(v0),real(v0)+cos(-angle),imag(v0)+sin(-angle))

    good_moment_from_slack_and_angle_helper1(angle,L,x0,y0,x1,y1) = \
        good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1)

    good_moment_from_slack_and_angle_helper1(angle,L,x0,y0,x1,y1) = \
        good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1, \
                                            (x1-x0)/(2*asinhc(sqrt(L**2 - (y1-y0)**2) / (x1-x0))))  # = s  XXX divides by x1-x0, need to reformulate to be well behaved.  should just produce 0 in that case I think?




    good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1,s) =  \
        good_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,s, \
                                            (x0+x1)/2. - s*acosh(L / (s*sqrt(2*(cosh((x1-x0)/s) - 1)))))  # = b    XXX divides by s, need to reformulate to be well behaved.  should just produce (x0+x1)/2. in that case I think?

    # using instead my magic b = (x0+x1)/2. - s*xmid_from_a_and_b((x1-x0)/s, (y1-y0)/s)  (where a_and_b mean different from s and b here)
    #                          = (x0+x1)/2. - s*Asinh((y1-y0)/(2.*s)/sinh((x1-x0)/(2.*s)))
    #                          = (x0+x1)/2. - s*Asinh((y1-y0)/((x1-x0)*sinhc((x1-x0)/(2.*s))))
    # (not sure which of the latter two is better if either)
    good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1,s) =  \
        good_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,s, \
                                            (x0+x1)/2. - s*Asinh((y1-y0)/(2.*s)/sinh((x1-x0)/(2.*s))))  # = b    XXX divides by s, need to reformulate to be well behaved.  should just produce (x0+x1)/2. in that case I think?
    good_moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1,s) =  \
        good_moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,s, \
                                            (x0+x1)/2. - s*Asinh((y1-y0)/((x1-x0)*sinhc((x1-x0)/(2.*s)))))  # = b    XXX divides by s, and by (x1-x0) too, need to reformulate to be well behaved.  should just produce (x0+x1)/2. in that case I think?

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
    good_moment_from_x(slack,v0) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
    good_moment_from_xy(x,y,v0) = y==0. ? good_moment_from_x(x,v0) : y>0 ? conj(_good_moment_from_xy(x,-y,conj(v0))) : _good_moment_from_xy(x,y,v0)

    _good_moment_from_xy(x,y,v0) = good_moment_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0)
      slack_from_xy(x,y) = sqrt(x**2+y**2)
      angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
      #angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
} # setup for "good"

if (strategy eq "first") {
  moment_from_xy(x,y,v0) = first_moment_from_xy(x,y,v0)
}
if (strategy eq "weil") {
  moment_from_xy(x,y,v0) = weil_moment_from_xy(x,y,v0)
}
if (strategy eq "good") {
  moment_from_xy(x,y,v0) = good_moment_from_xy(x,y,v0)
}

#print moment_from_xy(0,-.01)
#print moment_from_xy(0,-.1)
#print moment_from_xy(0,-1)
#print moment_from_xy(0,-2)
#print moment_from_xy(0,-10)

f(z) = moment_from_xy(real(z),imag(z),velocity0)
wf(z) = weil_moment_from_xy(real(z),imag(z),velocity0)
gf(z) = good_moment_from_xy(real(z),imag(z),velocity0)

if (alternate_plot1_flag) {
    # Alternate to look at: a function that can be computed analytically: moment from angle and invCatScale.
    # But unfortunately I think invCatScale must be scaled by something involving asinhc, so we are
    # no better off than before (using the slack formulation) I don't think...
    # Idea: we could use invCatScale directly when far from x axis, which would speed things up maybe....
    # but not too hopeful on this, at this point.

    f(z) = imag(z)>0 ? conj(_f(conj(z))) : _f(z)
      _f(z) = moment_from_angle_and_invCatScale(angle_from_xy(real(z),imag(z)), abs(z))
    # Can we get a simple scaling based on cos(angle) so that, asymptotically, the contours are circles?
    # Well, asymptotically, if we let:
    #   s = catscale
    #   x = x coord on canonical catenary
    #   y = y coord on canonical catenary
    #   t = half-length on canonical catenary
    #   theta = downangle - pi/2
    #
    #   y = t = cosh x = sinh x = 1/2 e^x
    #
    #   T = actual length = s*t = s*y
    #   X = actual width = s*x = cos(theta)
    #
    # So, given theta,T, what is the corresponding s?
    # Well...
    #   s = X/x = cos(theta)/x
    #   T = Y = s*y = cos(theta)/x * y
    #               = cos(theta)/x * sinh(x)
    #               = cos(theta) * sinhc(x)
    #   T/cos(theta) = sinhc(x)
    #   x = asinhc(T/cos(theta))
    #   s = X/x = cos(theta)/asinhc(T/cos(theta))
    #   invCatScale = 1/s = asinhc(T/cos(theta))/cos(theta)
    #
    # Let's try that.
    # Getting thoughts organized:
    # Input: z, v0
    #   angle <- z    angle_from_xy(z)
    #   nominalMagnitude <- z   abs(z)
    #     invCatScale <- angle,nominalMagnitude   invCatScale_from_angle_and_nominalMagnitude
    #       slack <- angle,invCatScale            slack_from_angle_and_invCatScale
    #         moment <- v0,slack,angle,invCatScale      moment_from_slack_and_angle_and_invCatScale_and_v0
    # XXX BLEAH! still pinched at origin!  What the hell?
    # XXX and isn't this a waste of time anyway?  "slack" is the thing I'm looking for, isn't it?  And we have invCatScale_from_slack_and_angle
    # which is of similar complexity as anything I'm doing here, right?
      _f(z) = moment_from_angle_and_nominalMagnitude_and_v0(angle_from_xy(real(z),imag(z)), abs(z),velocity0)
        moment_from_angle_and_nominalMagnitude_and_v0(angle,nominalMagnitude,v0) = \
            moment_from_angle_and_nominalMagnitude_and_v0_helper1(angle,nominalMagnitude,v0,invCatScale_from_angle_and_nominalMagnitude(angle,nominalMagnitude))
        moment_from_angle_and_nominalMagnitude_and_v0_helper1(angle,nominalMagnitude,v0,invCatScale) = \
            moment_from_angle_and_nominalMagnitude_and_v0_helper2(angle,nominalMagnitude,v0,invCatScale,slack_from_angle_and_invCatScale(angle,invCatScale))
        moment_from_angle_and_nominalMagnitude_and_v0_helper2(angle,nominalMagnitude,v0,invCatScale,slack) = \
            v0*(1.+slack) + moment_from_angle_and_invCatScale(angle, invCatScale)

        invCatScale_from_angle_and_nominalMagnitude(angle,nominalMagnitude) =  asinhc(1.+nominalMagnitude/cos(angle))/cos(angle)**1.1
        slack_from_angle_and_invCatScale(angle,invCatScale) = (t1_from_angle_and_invCatScale(angle,invCatScale)-t0_from_angle_and_invCatScale(angle,invCatScale))/invCatScale - 1.
}

unstretched_moment_from_xy(x,y,v0) = squashBy(moment_from_xy(x,y,v0), squash_from_xy(x,y,v0))
  squash_from_xy(x,y,v0) = squash_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0)
    squash_from_slack_and_angle(slack, angle, v0) = slack==0 ? 1. : (moment_from_x(slack,v0)-moment_from_x(-slack,v0))/(-2*imag(moment_from_slack_and_angle(slack,0,v0)))
  squashBy(z,squash) = real(z) + imag(z)*squash*{0,1}
# uncomment this to see the almost-circles
#f(z) = unstretched_moment_from_xy(real(z),imag(z),velocity0)


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
maxMag = (alternate_plot1_flag ? 6 : 4) * magFurtherSubdivisions
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
r = 1
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
set zeroaxis

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
zscale = (alternate_plot1_flag ? .01 : .1)

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
    #####splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with dots linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with pm3d
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with lines linewidth lw, real(exp(u+i*v)),imag(exp(u+i*v)),exp(u) # best-fit circles when symmetric picture

    # weil's in red (with blue slack=1 contour) offset by a little, good in green
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(.01+wf(exp(u+i*v))),imag(wf(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw, \
    #                                          real(gf(exp(u+i*v))),imag(gf(exp(u+i*v))),zscale*exp(u) with lines linewidth lw
    splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(wf(exp(u+i*v))),imag(wf(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw+4, \
                                              real(gf(exp(u+i*v))),imag(gf(exp(u+i*v))),zscale*exp(u) with lines linewidth lw

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

if (!png_flag) {
  pause -1 "Hit Enter or Ctrl-c to exit: " # wait til user hits Enter or ctrl-c
}
