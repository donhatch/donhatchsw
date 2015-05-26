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


     

set term wxt size 1000,1000
#set term x11 size 1000,1000
set size square
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
asinhc_by_newton(y) = y<1. ? crash(1) : y==1. ? 0. : asinhc_by_newton_recurse0(y, asinh(y))
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
# I know however that I'm slowed down by the extra params here (xPrev, maxRecursions)
asinhc_by_halley(y) = y<1. ? crash(1) : y==1. ? 0. : asinhc_by_halley_recurse(y, asinh(y), NaN, NaN, 100)

  asinhc_by_halley_recurse(y, x, xPrev, xPrevPrev, maxRecursions) = maxRecursions==0 ? crash(1) : (x==xPrev||abs(x-xPrev)>=abs(xPrev-xPrevPrev)) ? (x+xPrev)/2. : asinhc_by_halley_recurse_helper(y, x, xPrev, sinh(x)/x-y, (x*cosh(x)-sinh(x))/x**2, ((x**2+2)*sinh(x)-2*x*cosh(x))/x**3, maxRecursions)
  asinhc_by_halley_recurse_helper(y, x, xPrev, fx, dfx, ddfx, maxRecursions) = asinhc_by_halley_recurse(y, x - 2*fx*dfx/(2*dfx**2 - fx*ddfx), x, xPrev, maxRecursions-1)

  # same but only computes sinh(x) and cosh(x) once, at expense of another layer of user-defined function
  asinhc_by_halley_recurse(y, x, xPrev, xPrevPrev, maxRecursions) = maxRecursions==0 ? crash(1) : (x==xPrev||abs(x-xPrev)>=abs(xPrev-xPrevPrev)) ? (x+xPrev)/2. : asinhc_by_halley_recurse_helper1(y, x, xPrev, sinh(x), cosh(x), maxRecursions)
  asinhc_by_halley_recurse_helper1(y, x, xPrev, sinh_x, cosh_x, maxRecursions) = asinhc_by_halley_recurse_helper2(y, x, xPrev, sinh_x/x-y, (x*cosh_x-sinh_x)/x**2, ((x**2+2)*sinh_x-2*x*cosh_x)/x**3, maxRecursions)
  asinhc_by_halley_recurse_helper2(y, x, xPrev, fx, dfx, ddfx, maxRecursions) = asinhc_by_halley_recurse(y, x - 2*fx*dfx/(2*dfx**2 - fx*ddfx), x, xPrev, maxRecursions-1)

#asinhc(y) = asinhc_by_binary_search_lo(y)
asinhc(y) = asinhc_by_newton(y)
#asinhc(y) = asinhc_by_halley(y)



#asinhc_by_halley(y) = y<1. ? crash(1) : y==1. ? 0. : asinhc_by_halley_recurse(y, asinh(y), NaN, NaN, 100)
#  asinhc_by_halley_recurse(y, x, xPrev, xPrevPrev, maxRecursions) = ((traceString=traceString.sprintf("    x=%.17g\n",x)),maxRecursions==0 ? crash(1) : (x==xPrev||abs(x-xPrev)>=abs(xPrev-xPrevPrev)) ? (x+xPrev)/2. : asinhc_by_halley_recurse_helper(y, x, xPrev, sinh(x)/x-y, (x*cosh(x)-sinh(x))/x**2, ((x**2+2)*sinh(x)-2*x*cosh(x))/x**3, maxRecursions))
#  asinhc_by_halley_recurse_helper(y, x, xPrev, fx, dfx, ddfx, maxRecursions) = (asinhc_by_halley_recurse(y, x - 2*fx*dfx/(2*dfx**2 - fx*ddfx), x, xPrev, maxRecursions-1))

#y=2.
#y = (y-1)/1.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y=1.1
#y = (y-1)/1.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/10.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/2.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/2.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString
#y = (y-1)/2.+1.; traceString = ""; print sprintf("asinhc_by_bin_lo(%.17g) = %.17g", y, asinhc_by_binary_search_lo(y)); print sprintf("asinhc_by_bin_hi(%.17g) = %.17g", y, asinhc_by_binary_search_hi(y)); print sprintf("asinhc_by_newton(%.17g) = %.17g", y, asinhc_by_newton(y)); print sprintf("asinhc_by_halley(%.17g) = %.17g", y, asinhc_by_halley(y)); print "traceString = ", traceString


a_from_angle_and_invCatScale(angle, invCatScale) = invCatScale*cos(-angle);
b_from_angle_and_invCatScale(angle, invCatScale) = invCatScale*sin(-angle);
xmid_from_a_and_b(a,b) = asinh(b/2./sinh(a/2.))

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
# In our case, L = 1+slack, a=cos(angle), b=sin(angle).
invCatScale_from_slack_and_angle(slack, angle) = invCatScale_from_L_and_a_and_b(1.+slack, cos(angle), sin(angle))
  invCatScale_from_L_and_a_and_b(L, a, b) = asinhc(sqrt(L**2 - b**2) / a) * (2./a)

x0_from_a_and_b(a,b) = xmid_from_a_and_b(a,b) - a/2.
x1_from_a_and_b(a,b) = xmid_from_a_and_b(a,b) + a/2.
y0_from_a_and_b(a,b) = cosh(x0_from_a_and_b(a,b))
y1_from_a_and_b(a,b) = cosh(x1_from_a_and_b(a,b))
t0_from_a_and_b(a,b) = sinh(x0_from_a_and_b(a,b))
t1_from_a_and_b(a,b) = sinh(x1_from_a_and_b(a,b))

conj(z) = real(z) - i*imag(z)

# analytic version to use when on x axis
moment_from_x(slack,v0) = (slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)) + (1.+abs(slack))*v0
moment_from_xy(x,y,v0) = x!=0&&abs(y/x)<1e-12 ? moment_from_x(x,v0) : y>0 ? conj(_moment_from_xy(x,-y,conj(v0))) : _moment_from_xy(x,y,v0)
#moment_from_xy(x,y,v0) = y==0. ? moment_from_x(x,v0) : y>0 ? conj(_moment_from_xy(x,-y,conj(v0))) : _moment_from_xy(x,y,v0)
_moment_from_xy(x,y,v0) = moment_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0)
  slack_from_xy(x,y) = sqrt(x**2+y**2)
  #angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
  angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
  moment_from_slack_and_angle(slack,angle,v0) = v0*(1.+slack) + moment_from_slack_and_angle_and_invCatScale(slack, angle, invCatScale_from_slack_and_angle(slack, angle))
    moment_from_slack_and_angle_and_invCatScale(slack,angle,invCatScale) = moment_from_slack_and_angle_and_invCatScale_and_t0_and_t1(slack,angle,invCatScale, \
                                                                                                                                     t0_from_slack_and_angle_and_invCatScale(slack,angle,invCatScale), \
                                                                                                                                     t1_from_slack_and_angle_and_invCatScale(slack,angle,invCatScale))
      t0_from_slack_and_angle_and_invCatScale(slack,angle,invCatScale) = t0_from_a_and_b(a_from_angle_and_invCatScale(angle,invCatScale), \
                                                                                         b_from_angle_and_invCatScale(angle,invCatScale))
      t1_from_slack_and_angle_and_invCatScale(slack,angle,invCatScale) = t1_from_a_and_b(a_from_angle_and_invCatScale(angle,invCatScale), \
                                                                                         b_from_angle_and_invCatScale(angle,invCatScale))

      # Integrate from t0 to t1 on the canonical catenary with x(t),y(t) translated to the origin,
      # then rotate by -angle and scale by 1/invCatScale^2.
      # That is, integral from t=t0 to t=t1 of:
      #       (x(t)-x(t0),y(t)-y(t0)
      # where:
      #       x(t) = asinh(t)
      #       y(t) = sqrt(t**2+1)
      moment_from_slack_and_angle_and_invCatScale_and_t0_and_t1(slack,angle,invCatScale,t0,t1) = rotate_xy_by_angle(x_part_of_integral(t0,t1), \
                                                                                                                    y_part_of_integral(t0,t1), angle) / invCatScale**2
        rotate_xy_by_angle(x,y,angle) = x*cos(angle)-y*sin(angle) \
                                     + (x*sin(angle)+y*cos(angle)) * i
        x_part_of_integral(t0,t1) = (t1*asinh(t1)-sqrt(t1**2+1)) \
                                  - (t0*asinh(t0)-sqrt(t0**2+1)) \
                                  - (t1-t0)*asinh(t0)
        y_part_of_integral(t0,t1) = .5*(t1*sqrt(t1**2+1)+asinh(t1)) \
                                  - .5*(t0*sqrt(t0**2+1)+asinh(t0)) \
                                  - (t1-t0)*sqrt(t0**2+1)

#print moment_from_xy(0,-.01)
#print moment_from_xy(0,-.1)
#print moment_from_xy(0,-1)
#print moment_from_xy(0,-2)
#print moment_from_xy(0,-10)

velocity0 = {0,0}   # normal
#velocity0 = {-.5,0} # symmetric about origin
f(z) = moment_from_xy(real(z),imag(z),velocity0)

unstretched_moment_from_xy(x,y,v0) = squashBy(moment_from_xy(x,y,v0), squash_from_xy(x,y,v0))
  squash_from_xy(x,y,v0) = squash_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y), v0)
    squash_from_slack_and_angle(slack, angle, v0) = slack==0 ? 1. : (moment_from_x(slack,v0)-moment_from_x(-slack,v0))/(-2*imag(moment_from_slack_and_angle(slack,0,v0)))
  squashBy(z,squash) = real(z) + imag(z)*squash*{0,1}
# uncomment this to see the almost-circles
#f(z) = unstretched_moment_from_xy(real(z),imag(z),velocity0)


# Test whether the non-ellipses that look like ellipses are at least left-right symmetric. \
# They are! (and actually this is obvious, see comment at top of this file) \
if (1) \
    print "f({0,1}) = ",f({0,1}); \
    print "f({1,0})-f({0,1}) = ",f({1,0})-f({0,1}); \
    print "f({-1,0})-f({0,1}) = ",f({-1,0})-f({0,1}); \
    print "====="; \
    print "f({0,2}) = ",f({0,2}); \
    print "f({2,0})-f({0,2}) = ",f({2,0})-f({0,2}); \
    print "f({-2,0})-f({0,2}) = ",f({-2,0})-f({0,2}); \
    print "====="; \
    print "f({0,5}) = ",f({0,5}); \
    print "f({3,4})-f({0,5}) = ",f({3,4})-f({0,5}); \
    print "f({-3,4})-f({0,5}) = ",f({-3,4})-f({0,5}); \
    print "f({4,3})-f({0,5}) = ",f({4,3})-f({0,5}); \
    print "f({-4,3})-f({0,5}) = ",f({-4,3})-f({0,5})

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
maxMag = 3 * magFurtherSubdivisions
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
r = 3.5 # 2 on the right, 4 on the left
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

x0 = (r < 1 ? real(velocity0)+.5 : 0) - r
x1 = (r < 1 ? real(velocity0)+.5 : 0) + r
y0 = -r
y1 = r
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


tics = r<=.5 ? r/4. : r<=1 ? r/8. : r<=1.75 ? 1./8 : r<=3.5 ? 1./2 : r<=8.5 ? 1 : r<=10 ? 1 : r<=20 ? 1 : r/10 # kind of weird
set xtics tics
set ytics tics

# XXX I have no idea what the fuck I'm doing here
set palette defined (0 "red", .09 "red", .1 "blue", .13 "red", 1 "red")

set terminal png size 600,600
#set terminal png size 100,100
set output "RMME.png"
# XXX argh, the "set size square" isn't taking

# change from lines to linespoints to debug
time0 = time(0.)
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with linespoints linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with linespoints linewidth lw, real(f(exp(0*u+i*v))),imag(f(exp(0*u+i*v))),exp(u) with linespoints linewidth lw # hacky way to get green on center contour
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),10*exp(u) with lines linewidth lw
splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),.1*exp(u) with line palette linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with dots linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with pm3d

#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with lines linewidth lw, real(exp(u+i*v)),imag(exp(u+i*v)),exp(u) # best-fit circles when symmetric picture

time1 = time(0.)

print sprintf("splot took %.6f seconds.", (time1-time0))

#pause -1 "Hit Enter or Ctrl-c to exit: " # wait til user hits Enter or ctrl-c
