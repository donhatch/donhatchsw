#!/usr/bin/gnuplot

# Script for producing Figure 4 that I added to
# http://math.stackexchange.com/questions/1345571/what-would-a-planetary-orbit-look-like-if-gravity-had-constant-magnitude

# XXX Interesting fact: if I plot the velocity instead of the position,
# and scale the hypotrochoid given by these a,b,c by exactly 1.75 and rotate it 90 degrees,
# I get an almost-match again-- but in this case the velocity curve is too wide instead of too narrow

# time ./sim.py  0.  1. 0.  0. 1.6626561281062888  1.  '2*pi/36' 10000  '10.11*pi' >| OUT_closed_7
# time ./sim.py  0.  1. 0.  0. 1.6626561281062888  1.  '2*pi/360' 1000  '10.11*pi' >| OUT_closed_7_fine
# bbox = [-1.9183277185439316,2.0548661125202106]x[-2.019910834613428,2.0198654059866596]

EXACT(z) = imag(z)==0. ? sprintf("%.17g", z) : sprintf("{%.17g, %.17g}", real(z), imag(z))

# Find params a,b,h such that:
#       a/b = 7/3
#       a-b+h = xmax = 2.0548661125202106 (from sim)
#       a-b-h = -1
#  =>
#       b = a*3/7
#       a*4/7+h = 2.0548661125202106
#       a*4/7-h = -1
#       2*h = 3.0548661125202106
#       h = (2.0548661125202106 + 1)/2
#         = 1.5274330562601053
#
# Leads to:
#   a = 0.92300784845518424
#   b = 0.39557479219507891
#   h = 1.5274330562601053


# Better I think:
# Function:
#     (a-b)*cos(t)+h*cos((a-b)/b*t), -((a-b)*sin(t)-h*sin((a-b)/b*t))
# First derivative:
#     -(a-b)*sin(t)-h*(a-b)/b*sin((a-b)/b*t), -(a-b)*cos(t)-h*(a-b)/b*cos((a-b)/b*t)
# Second derivative:
#     -(a-b)*cos(t)-h*((a-b)/b)**2*cos((a-b)/b*t), (a-b)*sin(t)-h*((a-b)/b))**2*sin((a-b)/b*t))

# Find params such that:
#     first derivative at t=3*pi is 0,1.6626561281062888
#     second derivative at t=3*pi is 1,0
# i.e.:
#     -(a-b)-h*(a-b)/b == 1.6626561281062888
#     -(a-b)-h*((a-b)/b)**2 = 1

# Find params a,b,h such that:
#       a/b = 7/3   (so it will be a 7-petalled flower)
#         => b = a*3/7, a-b = a*4/7, (a-b)/b = 4/3, b/(a-b) = 3/4
#       a-b-h = -1
#         => a*4/7 - h = -1
#       derivative at t=0 is 0,1.6626561281062888
#       i.e. -((a-b) - h*(b/(a-b))) = 1.6626561281062888
#       i.e. -(a*4/7 - h*3/4) = 1.6626561281062888
# add earlier ->
#       -h + h*3/4 = 1.6626561281062888 - 1
#       -h/4 = 1.6626561281062888
#       
# ARGH not right-- need speed too?
# So really need to add a speed scale factor s.
# Function:
#     (a-b)*cos(t*s)+h*cos((a-b)/b*t*s), -((a-b)*sin(t*s)-h*sin((a-b)/b*t*s))
# First derivative:
#     -(a-b)*s*sin(t*s)-h*(a-b)/b*s**sin((a-b)/b*t*s), -(a-b)*s*cos(t*s)-h*(a-b)/b*s*cos((a-b)/b*t*s)
# Second derivative:
#     -(a-b)*s**2*cos(t/s)-h*((a-b)/b)**2*s**2*cos((a-b)/b*t/s), (a-b)*s**2*sin(t/s)-h*((a-b)/b))**2*s**2*sin((a-b)/b*t/s))
# Find params a,b,h,s such that:
#     function value at t=3*pi/s is 1,0
#     first derivative at t=3*pi/s is 0,1.6626561281062888
#     second derivative at t=3*pi/s is -1,0
#     a/b = 7/3 (so it will be a 7-petalled flower)
#       => b = a*3/7, a-b = a*4/7, (a-b)/b = 4/3, b/(a-b) = 3/4
# i.e. ...

v0 = 1.6626561281062888
xmax = 2.0548661125202106
h = (xmax+1.)/2.
a = (h-1)*7/4
b = a*3/7
print "a = ",EXACT(a)
print "b = ",EXACT(b)
print "h = ",EXACT(h)

size = 700
set term wxt enhanced size size,size
set size square
set parametric
q = 2.5

lerp(a,b,t) = a*(1-t) + b*t

set samples 180 # would be 181 if exactly to end
t0 = 6*pi/2.
set title 'Figure 4: Is it a hypotrochoid?'
set label sprintf('Hypotrochoid params: a = %.6f b=%.6f h=%.6f', a, b, h) at -.75,-2.25
set term pngcairo enhanced size 700,700 font ",9" crop
set output 'Figure4.png'
plot [t0+0:t0+6*pi*(178.5/180.)] [-q:q] [-q:q] \
    (a-b)*cos(t)+h*cos((a-b)/b*t), -((a-b)*sin(t)-h*sin((a-b)/b*t)) with linespoints ls 1 title 'Hypotrochoid (a-b)*cos(t)+h*cos((a-b)/b*t), -((a-b)*sin(t)-h*sin((a-b)/b*t))', \
    'OUT_closed_7' with linespoints linestyle 3 title sprintf('Constant magnitude 1 central force orbit with p(0)=(1,0),v(0)=(0,%.6f)',v0), \
    a*cos(t),a*sin(t) linestyle 7 title '', \
    (a-b+b*cos(t)), b*sin(t) linestyle 7 title '', \
    lerp(a-b,a-b+h,(t-t0)/(6*pi)),0 linestyle 7 title ''

#pause -1 "Hit Enter or Ctrl-c to exit: " # wait til user hits Enter or ctrl-c
