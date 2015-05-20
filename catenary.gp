#!/usr/bin/gnuplot

# Q: I know the "ellipses" aren't really ellipses.  But are they left-right symmetric at least?
# A: Yes!

# Q: how to get a splot in 2d (currently fudging using "set view" and "unset ztics")
#    (but maybe I don't want it any more? not sure)
# Q: how to reset view to 0,359.99,1.5 from keyboard?
# Q: why the heck isn't the z axis going all the way to the floor?
# Q: is there a way to increase the recursion depth limit? (seems to be 250)
# Q: can I make a visual differentiator at mag=0?
# PA: well, can add a degenerate plot at a different color

# Q: can I print during function recursion?
# Q: how do I debug what values are causing recursion depth limit overflow?
# A: keep track of recursion level and max recursion level, and snapshot params when highest is reached, then print the snapshot at the end

# Q: maybe project contours onto the base and sides? Hmm.

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
# This method works but tends to be a bit slower than newton
#
sinhc(x) = x==0. ? 1. : sinh(x)/x
asinhc_by_binary_search(y) = y==1. ? 0. : asinhc_binary_search(y, 0., 1e3, (0.+1e3)/2.)
  asinhc_binary_search(y, x0, x1, xMid) = \
      xMid<=x0 || xMid>=x1 ? xMid : \
      sinhc(xMid) < y ? asinhc_binary_search(y, xMid, x1, (xMid+x1)/2.) \
                      : asinhc_binary_search(y, x0, xMid, (x0+xMid)/2.)

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
      asinhc_by_newton_recurse(y, x, xPrev) = x>=xPrev ? xPrev : asinhc_by_newton_recurse(y, x - (sinh(x)/x-y)/((x*cosh(x)-sinh(x))/x**2), x)

#asinhc(y) = asinhc_by_binary_search(y)
asinhc(y) = asinhc_by_newton(y)

# TODO: Halley's method?  That seems to be the method of choice for lambertw and glog, maybe for this too?

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
moment_from_x(slack) = slack==0 ? .5 : slack<0 ? .5 - slack*(slack/4.) : .5 + slack*(1+slack/4.)
moment_from_xy(x,y) = x!=0&&abs(y/x)<1e-12 ? moment_from_x(x) : y>0 ? conj(_moment_from_xy(x,-y)) : _moment_from_xy(x,y)
#moment_from_xy(x,y) = y==0. ? moment_from_x(x) : y>0 ? conj(_moment_from_xy(x,-y)) : _moment_from_xy(x,y)
_moment_from_xy(x,y) = moment_from_slack_and_angle(slack_from_xy(x,y), angle_from_xy(x,y))
  slack_from_xy(x,y) = sqrt(x**2+y**2)
  #angle_from_xy(x,y) = atan2(x,-y) # i.e. atan2(y,x) minus -90 degrees
  angle_from_xy(x,y) = atan2(y,x) - (-pi/2) # should be same thing
  moment_from_slack_and_angle(slack,angle) = moment_from_slack_and_angle_and_invCatScale(slack, angle, invCatScale_from_slack_and_angle(slack, angle))
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

f(z) = moment_from_xy(real(z),imag(z))


# Test whether the non-ellipses that look like ellipses are at least left-right symmetric. \
# They are! \
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
# but we may tweak this below if using hidden3d.
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

#xy1 = 200
#xy1 = 100
#xy1 = 50
#xy1 = 10
#xy1 = 8.5 # 4 on the right
xy1 = 3.5 # 2 on the right, 3 on the left
#xy1 = 1.75 # 1 on the right
#xy1 = 1

x0 = y0 = -(x1 = y1 = xy1)
z0 = 0
z1 = 2*xy1


set samples 4*(maxMag-minMag)+1,10*nAngles+1
set isosamples (maxMag-minMag)+1,nAngles+1
set parametric
set zeroaxis

#lw = 2 # nice for viewing
#lw = 1
lw = .5  # seems to be optimal for information I think
#lw = .25 # just gets lighter



# change from lines to linespoints to debug
time0 = time(0.)
splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with linespoints linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with linespoints linewidth lw, real(f(exp(0*u+i*v))),imag(f(exp(0*u+i*v))),exp(u) with linespoints linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with lines linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with dots linewidth lw
#splot [u0:u1][v0:v1][x0:x1][y0:y1][z0:z1] real(f(exp(u+i*v))),imag(f(exp(u+i*v))),exp(u) with pm3d
time1 = time(0.)

print sprintf("that took %.6f seconds.", (time1-time0))

pause -1 "Hit Enter or Ctrl-c to exit: " # wait til user hits Enter or ctrl-c
