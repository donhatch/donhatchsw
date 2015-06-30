#!/usr/bin/python

# Simulate bounce=1, i.e. aim as if we had infinite power,
# i.e. always aim straight for the target.

# gnuplot:
#       set size square
#       q = 1.5
#       plot [-q:q] [-q:q] 'OUT'


from math import * # for the evals, so you can say sqrt(pi) etc.
import cmath
import sys

if len(sys.argv) != 9:
  exit("Usage: sim.py px py vx vy a DT dtsPerDT t1")

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

exponent = 1

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

# sweet spots for v:
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

