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

    png_flag = 0 # if set, output to RMME1.png and RMME2.png instead of terminal

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

    weil_flag = 0 # if set, use formulation more like the one in Weil's paper.  it should work either way.
     

if (!png_flag) {
    set term wxt size 1000,1000
    #set term x11 size 1000,1000
    set size square
}

set view 0,359.999,1.5 # top down, with a bit of fudge to make y axis labels come out on left instead of right
#set view 85,85 # bad angle for pm3d
#unset ztics


i = {0,1}

EXACT(z) = imag(z)==0. ? sprintf("%.17g", z) : sprintf("{%.17g, %.17g}", real(z), imag(z))

max(a,b) = a>=b ? a : b

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
asinhc_by_newton(y) = y<1. ? crash_in_asinhc_by_newton(1) : y==1. ? 0. : asinhc_by_newton_recurse0(y, asinh(y))
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
asinhc_by_halley(y) = y<1. ? crash_in_asinhc_by_halley(1) : y==1. ? 0. : asinhc_by_halley_recurse(y, asinh(y), NaN, NaN, 20)

  asinhc_by_halley_recurse(y, x, xPrev, xPrevPrev, maxRecursions) = maxRecursions==0 ? crash_in_asinhc_by_halley_recurse(1) : (x==xPrev||abs(x-xPrev)>=abs(xPrev-xPrevPrev)) ? (x+xPrev)/2. : asinhc_by_halley_recurse_helper(y, x, xPrev, sinh(x)/x-y, (x*cosh(x)-sinh(x))/x**2, ((x**2+2)*sinh(x)-2*x*cosh(x))/x**3, maxRecursions)
  asinhc_by_halley_recurse_helper(y, x, xPrev, fx, dfx, ddfx, maxRecursions) = asinhc_by_halley_recurse(y, x - 2*fx*dfx/(2*dfx**2 - fx*ddfx), x, xPrev, maxRecursions-1)

#asinhc(y) = asinhc_by_binary_search_lo(y)
asinhc(y) = asinhc_by_newton(y)
#asinhc(y) = asinhc_by_halley(y)


if (0) { # turn this on to debug asinhc_by_halley. exercises what I think is the worst case, to verify the max recursion depth ever used.  seems to be 20. (found by trying various numbers)
    crashed = 0
    crashedTraceString = ""
    if (1) {
        # version that accumulates a string and sets "crashed" flag instead of actually crashing
        asinhc_by_halley(y) = y<1. ? crash_in_asinhc_by_halley(1) : y==1. ? 0. : asinhc_by_halley_recurse(y, asinh(y), NaN, NaN, 20)
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


a_from_angle_and_invCatScale(angle, invCatScale) = invCatScale*cos(-angle)
b_from_angle_and_invCatScale(angle, invCatScale) = invCatScale*sin(-angle)
# surprising magic.
xmid_from_a_and_b(a,b) = asinh(b/2./sinh(a/2.))
# I wish it simplified further though!
# Basic case of it is asinh(1/sinh(x)). Does that simplify?
#       asinh(y) = log(y + sqrt(1+y^2))
#       sinh(x) = (e^x-e^-x)/2
#       asinh(1/sinh(x)) = log(2/(e**x-e**-x) + sqrt(1+(2/(e**x-e**-x))**2))
#                        = log(2/X + sqrt(1+(2/X)^2))
#                        = log(X + sqrt(1+X^2)) where X = 1/sinh(x).  bleah! not getting anywhere. but wait...
#                        = log(X*(1 + sqrt(1/X^2 + 1)))
#                        = log(1/sinh(x) * (1 + sqrt(sinh(x)**2+1)))
#                        = log((1 + cosh(x)) / sinh(x))
#                        = log(1./sinh(x) + cosh(x)/sinh(x))
#                        = log(csch(x) + coth(x))
# So it does sort of simplify... ?
# Okay let's do the non-basic case. Let B=b/2, A=a/2.
#       asinh(B/sinh(A)) = log(B/sinh(A) + sqrt(1 + (B/sinh(A))^2))
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
#    sqrt(L^2 - b^2) = 2*catScale*asinh(a/(2*catScale))
#                    = a * (2*catScale/a) * asinh(a/(2*catScale))
#                    = a * sinhc(a/(2*catScale))
#    sqrt(L^2 - b^2)/a = sinhc(a/(2*catScale))
#    a/(2*catScale) = asinhc(sqrt(L^2 - b^2)/a)
#    invCatScale = asinhc(sqrt(L^2 - b^2)/a) * (2/a)
# In our case, L = 1+slack, a=cos(-angle), b=sin(-angle).
invCatScale_from_slack_and_angle(slack, angle) = invCatScale_from_L_and_a_and_b(1.+slack, cos(-angle), sin(-angle))
  invCatScale_from_L_and_a_and_b(L, a, b) = asinhc(sqrt(L**2 - b**2) / a) * (2./a)

x0_from_a_and_b(a,b) = xmid_from_a_and_b(a,b) - a/2.
x1_from_a_and_b(a,b) = xmid_from_a_and_b(a,b) + a/2.
y0_from_a_and_b(a,b) = cosh(x0_from_a_and_b(a,b))
y1_from_a_and_b(a,b) = cosh(x1_from_a_and_b(a,b))
t0_from_a_and_b(a,b) = sinh(x0_from_a_and_b(a,b))  # = sinh(asinh(b/2./sinh(a/2.)) - a/2.)
t1_from_a_and_b(a,b) = sinh(x1_from_a_and_b(a,b))  # = sinh(asinh(b/2./sinh(a/2.)) + a/2.)




conj(z) = real(z) - i*imag(z)
xconj(z) = -real(z) + i*imag(z)

# analytic version to use when on x axis
moment_from_x(slack,v0) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
# XXX what is the right criterion?
moment_from_xy(x,y,v0) = x!=0&&abs(y/x)<1e-12 ? moment_from_x(x,v0) : y>0 ? conj(_moment_from_xy(x,-y,conj(v0))) : _moment_from_xy(x,y,v0)
#moment_from_xy(x,y,v0) = y==0. ? moment_from_x(x,v0) : y>0 ? conj(_moment_from_xy(x,-y,conj(v0))) : _moment_from_xy(x,y,v0)
_moment_from_xy(x,y,v0) = moment_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0)
  slack_from_xy(x,y) = sqrt(x**2+y**2)
  angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
  #angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
  moment_from_slack_and_angle(slack,angle,v0) = v0*(1.+slack) + moment_from_angle_and_invCatScale(angle, invCatScale_from_slack_and_angle(slack, angle))
    moment_from_angle_and_invCatScale(angle,invCatScale) = moment_from_angle_and_invCatScale_and_t0_and_t1(angle,invCatScale, \
                                                                                                           t0_from_angle_and_invCatScale(angle,invCatScale), \
                                                                                                           t1_from_angle_and_invCatScale(angle,invCatScale))
      t0_from_angle_and_invCatScale(angle,invCatScale) = t0_from_a_and_b(a_from_angle_and_invCatScale(angle,invCatScale), \
                                                                         b_from_angle_and_invCatScale(angle,invCatScale))
      t1_from_angle_and_invCatScale(angle,invCatScale) = t1_from_a_and_b(a_from_angle_and_invCatScale(angle,invCatScale), \
                                                                         b_from_angle_and_invCatScale(angle,invCatScale))

      # Integrate from t0 to t1 on the canonical catenary with x(t),y(t) translated to the origin,
      # then rotate by -angle and scale by 1/invCatScale^2.
      # That is, integral from t=t0 to t=t1 of:
      #       (x(t)-x(t0),y(t)-y(t0)
      # where:
      #       x(t) = asinh(t)
      #       y(t) = sqrt(t**2+1)
      moment_from_angle_and_invCatScale_and_t0_and_t1(angle,invCatScale,t0,t1) = rotate_xy_by_angle(x_part_of_integral(t0,t1), \
                                                                                                    y_part_of_integral(t0,t1), angle) / invCatScale**2
        rotate_xy_by_angle(x,y,angle) = x*cos(angle)-y*sin(angle) \
                                     + (x*sin(angle)+y*cos(angle)) * i
        x_part_of_integral(t0,t1) = (t1*asinh(t1)-sqrt(t1**2+1)) \
                                  - (t0*asinh(t0)-sqrt(t0**2+1)) \
                                  - (t1-t0)*asinh(t0)
        y_part_of_integral(t0,t1) = .5*(t1*sqrt(t1**2+1)+asinh(t1)) \
                                  - .5*(t0*sqrt(t0**2+1)+asinh(t0)) \
                                  - (t1-t0)*sqrt(t0**2+1)

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
#            b = a*(mu - asinh(L/(Q*a)))
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
#       xmid = asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))
#     In other words:
#       asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a))) = xmid = ((x0+x1)/2-b)/a
#       a*asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a))) = (x0+x1)/2-b
#       b = (x0+x1)/2 - a*asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))
#     So that seems simpler than all of the above.
#   - also, almost everywhere b is used it's divided by a...
#     so instead of b, use B = (x0+x1)/(2*a) - acosh(L/(a*sqrt(2*(cosh((x1-x0)/a) - 1)))).
#     Then the catenary satisfies:
#       y = c + a*cosh(x/a-B)
#
# Then solve for c:
#       y0 = c + a*cosh(x/a-B)
#       c = y0 - a*cosh(x0/a-B)
#               (in Barzel paper, x0=y0=0 and its c = -c here, so c = cosh(b/a))
# Okay so now we know a,B,c, and:
#       y = c + a*cosh(x/a-B)
# Can we parametrize that by arc length?
#       t0 = a*sinh(x0/a-B)
#       t1 = a*sinh(x1/a-B)
#       t = a*sinh(x/a-B)
#       x = B*a + a*asinh(t/a)
#       y = c + a*cosh(x/a-B)
#         = c + a*cosh(((B + asinh(t/a))-B))
#         = c + a*cosh(asinh(t/a))
#         = c + sqrt(t^2 + a^2)
# So, the moment will be the integral of x,y from t=t0 to t=t1.
# According to wolframalpha:
#       x part of integral = a*t*asinh(t/a) + a*B*t - a*sqrt(a^2+t^2)
#       y part of integral = 1/2 t (sqrt(t^2+a^2) + 2*c)) + 1/2 a^2 log(sqrt(t^2+a^2) + t)
# but we can turn log(sqrt(t^2+a^2) + t) into hyperbolic trig as follows:
#         log(sqrt(t**2+a**2) + t)
#       = log(a*(sqrt((t/a)^2 + 1) + t/a))
#       = log(a) + log(sqrt((t/a)^2+1) + t/a)
#       = log(a) + asinh(t/a)
# and the log(a) gets absorbed into the integration constant. Yay! So:
#       x part of integral = a*t*asinh(t/a) + a*B*t - a*sqrt(a^2+t^2)
#       y part of integral = 1/2 t (sqrt(t^2+a^2) + 2*c)) + 1/2 a^2 asinh(t/a)
#
# XXX still simplifying.. and in the end I might just end up with what I had above,
#     except that I suck at naming things so the above looks messier than it needs to

assert(cond) = cond ? 1 : assertion_failed(1)

if (weil_flag) {
  moment_from_slack_and_angle(slack,angle,v0) = \
      moment_from_slack_and_angle_helper1(angle,1.+slack, real(v0),imag(v0),real(v0)+cos(-angle),imag(v0)+sin(-angle))
    # paper only works if x0,y0 is the *lower* end, for some reason
    moment_from_slack_and_angle_helper1(angle,L,x0,y0,x1,y1) = \
      y1>=y0 ? moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1) \
             : xconj(moment_from_slack_and_angle_helper2(-angle,L,-x1,y1,-x0,y0))
    moment_from_slack_and_angle_helper2(angle,L,x0,y0,x1,y1) = \
        moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1, \
                                            (x1-x0)/(2*asinhc(sqrt(L**2 - (y1-y0)**2) / (x1-x0))))  # = a

    moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,a) =  \
        moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a, \
                                            (x0+x1)/(2.*a) - acosh(L / (a*sqrt(2*(cosh((x1-x0)/a) - 1)))))  # = B

    # using instead my magic b = (x0+x1)/2 - a*asinh((y1-y0)/a/2./sinh((x1-x0)/a/2.))
    #                     so B = (x0+x1)/(2.*a) - asinh((y1-y0)/a/2./sinh((x1-x0)/a/2.))
    moment_from_slack_and_angle_helper3(angle,L,x0,y0,x1,y1,a) =  \
        moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a, \
                                            (x0+x1)/(2.*a) - asinh((y1-y0)/(2.*a)/sinh((x1-x0)/(2.*a)))) # = B

    moment_from_slack_and_angle_helper4(angle,L,x0,y0,x1,y1,a,B) = \
        moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,a,B, \
                                            y0 - a*cosh(x0/a-B))  # = c
    moment_from_slack_and_angle_helper5(angle,L,x0,y0,x1,y1,a,B,c) = \
        moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,a,B,c, \
                                            a*sinh(x0/a-B), a*sinh(x1/a-B))  # = t0,t1
    moment_from_slack_and_angle_helper6(angle,L,x0,y0,x1,y1,a,B,c,t0,t1) = \
        rotate_xy_by_angle(x_part_of_integral(a,B,c,t1) - x_part_of_integral(a,B,c,t0), \
                           y_part_of_integral(a,B,c,t1) - y_part_of_integral(a,B,c,t0), \
                           angle)
      x_part_of_integral(a,B,c_unused,t) = a * (t*(asinh(t/a) + B) - sqrt(t**2 + a**2))
      y_part_of_integral(a,B_unused,c,t) = .5*t*(sqrt(t**2+a**2) + 2*c) + .5*a**2*asinh(t/a)
      rotate_xy_by_angle(x,y,angle) = x*cos(angle)-y*sin(angle) \
                                   + (x*sin(angle)+y*cos(angle)) * i
}

#print moment_from_xy(0,-.01)
#print moment_from_xy(0,-.1)
#print moment_from_xy(0,-1)
#print moment_from_xy(0,-2)
#print moment_from_xy(0,-10)

f(z) = moment_from_xy(real(z),imag(z),velocity0)

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
        set terminal png size 600,600
        set output "RMME1.png"
    }

    # change from lines to linespoints to debug
    print "doing first plot..."
    time0 = time(0.)
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with linespoints linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with linespoints linewidth lw, real(f(exp(0*u+i*v))),imag(f(exp(0*u+i*v))),exp(u) with linespoints linewidth lw # hacky way to get green on center contour
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with lines linewidth lw
    splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with line palette linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with dots linewidth lw
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with pm3d
    #splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),zscale*exp(u) with lines linewidth lw, real(exp(u+i*v)),imag(exp(u+i*v)),exp(u) # best-fit circles when symmetric picture
    time1 = time(0.)
    print sprintf("splot took %.6f seconds.", (time1-time0))
}


#=================================================================================================

if (plot2_flag) {
    if (png_flag) {
        set terminal png size 600,600
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
}

if (!png_flag) {
  pause -1 "Hit Enter or Ctrl-c to exit: " # wait til user hits Enter or ctrl-c
}
