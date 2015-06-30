#!/usr/bin/python

# Simulate bounce=1, i.e. aim as if we had infinite power,
# i.e. always aim straight for the target.

from math import * # for the evals, so you can say sqrt(pi) etc.
import cmath
import sys

if len(sys.argv) != 9:
  print >>sys.stderr, "Usage: ./sim.py px py vx vy a DT dtsPerDT t1"
  print >>sys.stderr, "Example: ./sim.py  1. 0.  0. 1.  1.  '2*pi/360' 10000  '2*pi'"
  print >>sys.stderr, "Example: ./sim.py  1. 0.  0. 1.75  1.  '2*pi/360' 1000  '2*pi*5.18'"
  exit(1)

px = eval(sys.argv[1])
py = eval(sys.argv[2])
vx = eval(sys.argv[3])
vy = eval(sys.argv[4])
aMagnitude = eval(sys.argv[5])
DT = eval(sys.argv[6])
dtsPerDT = eval(sys.argv[7])
t1 = eval(sys.argv[8])

assert type(px) in [float,int]
assert type(py) in [float,int]
assert type(vx) in [float,int]
assert type(vy) in [float,int]
assert type(aMagnitude) in [float,int]
assert type(DT) in [float,int]
assert type(dtsPerDT) == int

p = complex(px,py)
v = complex(vx,vy)
dt = DT/dtsPerDT
print >>sys.stderr, "p = "+`p`
print >>sys.stderr, "v = "+`v`
print >>sys.stderr, "aMagnitude = "+`aMagnitude`
print >>sys.stderr, "dt = "+`dt`
print >>sys.stderr, "t1 = "+`t1`

exponent = 1 # hard-code to something other than 1 here to experiment

i = 0
while i*dt <= t1:
  a = -p/abs(p) * aMagnitude
  if i % dtsPerDT == 0:
    pExaggerated = p
    pExaggerated = p/abs(p) * abs(p)**exponent # hack
    print `pExaggerated.real`+" "+`pExaggerated.imag` +"  "+`v.real`+" "+`v.imag` +"  "+`a.real`+" "+`a.imag`+"  "+`i*dt/t1`
  # evaluate v at the half-step past p
  v += a * (dt*5 if i==0 else dt)
  # evaluate p at the half-step past v
  p += v * dt
  i += 1

# sweet spots for v_y (p=1,0 v=0,v_y aMag=1
#       7/4?          .621   oh I see, it's going in rather than out

#       hmm can't get to 5/2
#       7/4 = 1.75     -> 1.663
#       9/5 = 1.8      -> 2.705
#       11/6 = 1.8333  -> 3.465
#       13/7 = 1.857   -> 4.12
#       15/8 = 1.875   -> 4.705
#       17/9 = 1.889   -> 5.25
#       19/10 = 1.9    -> 5.755
#       21/11 = 1.909  -> 6.236
#       23/12 = 1.9167 -> 6.69
#       25/13 = 1.923  -> 7.125



# Maybe ask on math.stackexchange:

# Subject: what path would a planet take if gravity had constant magnitude?

# What path would a particle take if it is always attracted towards the origin
# with constant-magnitude force?
# In other words, it is experiencing gravity towards the origin,
# except that instead of the usual kind of gravity whose
# magnitude varies inversely with distance squared from the attractor,
# this gravity's magnitude is constant, so that is always
# accelerating towards the origin with a constant magnitude acceleration, say 1.
#
# Stated in terms of differential equations,
# the particle's motion p(t) = (x(t),y(t)) is such that it satisfies:
#     p^..(t) = -p(t)/||p(t)||.
# If we are additionally given initial position p(0) and velocity v(0)=p^.(0),
# then the function p is completely determined, and it can be computed
# numerically to any desired accuracy by simply iterating the following
# with small enough timestep dt:
#     a <= -p/||p||
#     v <= v + a*dt
#     p <= p + v*dt
#
# My question is: does such a particle trace out a well-known function?
#
# Notice that a simple circular orbit of unit radius and speed
# is one example of a function satisfying the given properties.
# More generally, for any radius r, the initial conditions:
#       p(0) = r,0
#       v(0) = 0,sqrt(r)
# yield circular motion:
#       p(t) = r*cos(t/sqrt(r)), r*sin(t/sqrt(r)).
# Check:
#      p.(t) = -sqrt(r)*sin(t/sqrt(r)), sqrt(r)*cos(t/sqrt(r)).
#     p..(t) = -cos(t/sqrt(r)), -sin(t/sqrt(r))
#            = -p(t)/||p(t)||.
#
# But what if the initial conditions are not quite so nicely aligned?
#
# Figure 1 shows 6 different sets of initial conditions,
# evolved from t=0 to t=$2 pi$:
# p(0)=(1,0), v(0)=(0,y) for y=0.5,1,1.5,2,2.5.

'''
time ./sim.py  1. 0.  0. 0.5  1.  '2*pi/36' 1000  '2*pi' >| OUT1
time ./sim.py  1. 0.  0. 1.0  1.  '2*pi/36' 1000  '2*pi' >| OUT2
time ./sim.py  1. 0.  0. 1.5  1.  '2*pi/36' 1000  '2*pi' >| OUT3
time ./sim.py  1. 0.  0. 2.0  1.  '2*pi/36' 1000  '2*pi' >| OUT4
time ./sim.py  1. 0.  0. 2.5  1.  '2*pi/36' 1000  '2*pi' >| OUT5
gnuplot:
  set size square
  set zeroaxis
  set style data lp
  q = 4
  set style line 5 lt rgb '#00cccc' pt 1
  set style line 4 lt rgb 'blue' pt 1
  set style line 3 lt rgb 'magenta' pt 1
  set style line 2 lt rgb 'red' pt 1
  set style line 1 lt rgb '#88cc88' pt 1
  set style line 22 lt rgb 'red' pt 0
  set style line 44 lt rgb 'blue' pt 0
  set title 'Figure 1: p(t) for t=0 to 2*pi'
  plot [-q:q] [-q:q] 'OUT5' ls 5 title 'p(0)=(1,0) v(0)=(0,2.5)', 'OUT4' ls 4 title 'p(0)=(1,0) v(0)=(0,2.0)', 'OUT3' ls 3 title 'p(0)=(1,0) v(0)=(0,1.5)', 'OUT2' ls 2 title 'p(0)=(1,0) v(0)=(0,1.0)', 'OUT1' ls 1 title 'p(0)=(1,0) v(0)=(0,0.5)'

'''
# In Figure 2, we evolve just the one with v(0)=(0,2) farther, to t=$20*pi$:
'''
time ./sim.py  1. 0.  0. 2.0  1.  '2*pi/36' 1000  '20*pi' >| OUT4_20pi
time ./sim.py  1. 0.  0. 2.0  1.  '2*pi/36' 1000  '60*pi' >| OUT4_60pi
gnuplot:
  set title 'Figure 2: p(t) for t=0 to 20*pi'
  plot [-q:q] [-q:q] 'OUT4_20pi' ls 4 title 'p(0)=(1,0) v(0)=(0,2)', 'OUT2' ls 2 title 'p(0)=(1,0) v(0)=(0,1)'
'''
# And in Figure 2, we evolve it even farther, to t=$60*pi$:
'''
  set title 'Figure 3: p(t) for t=0 to 60*pi'
  plot [-q:q] [-q:q] 'OUT4_60pi' ls 44 title 'p(0)=(1,0) v(0)=(0,2)', 'OUT2' ls 22 title 'p(0)=(1,0) v(0)=(0,1)'
'''

