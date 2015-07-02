#!/usr/bin/python

# TODO: actually solve for the v that gives a given shape

# Simulate bounce=1, i.e. aim as if we had infinite power,
# i.e. always aim straight for the target.

from math import * # for the evals, so you can say sqrt(pi) etc.
import cmath
import sys

if len(sys.argv) == 3:
  spirographParameter = eval(sys.argv[1])
  dt = eval(sys.argv[2])

  # TODO: don't use super-fine dt until zoomed in

  # What's to solve?
  # well, given, say, target=7/4,
  # we want the initial velocity that makes:
  #     - position come back to mag 1 for first time at 7/4
  #     - position reach max mag for first time at (7/4)/2
  def solveForSpirographParameter(spirographParameter, dt):
    def solveForAngleFracOfFirstMax(targetAngleFrac,dt):

      # find angle frac at which max mag is reached for first time,
      # i.e. when velocity starts pointing more inwards than outwards
      def angleFracOfFirstMax(initialVelocityMagnitude,dt):
        p = complex(1,0)
        v = complex(0,initialVelocityMagnitude)
        aMagnitude = 1
        i = 0
        while True:
          a = -p/abs(p) * aMagnitude
          # evaluate v at the half-step past p
          v += a * (dt*5 if i==0 else dt)
          if p.real < 0 and v.real*p.real + v.imag*p.imag < 0:
            # Call p the max.
            #print `i`+': dot prod is '+`v.real*p.real + v.imag*p.imag`
            return atan2(p.imag,p.real) / (2.*pi)
          # evaluate p at the half-step past v
          p += v * dt
          i += 1

      print "    targetAngleFrac = "+`targetAngleFrac`
      lo = 1.1
      hi = 20.
      while True:
        mid = (lo+hi)*.5
        if mid==lo or mid==hi:
          return mid
        gotAngleFrac = angleFracOfFirstMax(mid,dt)
        print "    initialVY="+`mid`+" -> angleFrac="+`gotAngleFrac`
        if gotAngleFrac > targetAngleFrac:
          # got angle too big, so mid is too small
          lo = mid
        else:
          hi = mid
    answer = solveForAngleFracOfFirstMax(spirographParameter*.5, dt)
    return answer
     
  print "Solving!"
  answer = solveForSpirographParameter(spirographParameter, dt)
  print "answer = "+`answer`
  exit(0)

elif len(sys.argv) == 10:

  exponent = eval(sys.argv[1])
  px = eval(sys.argv[2])
  py = eval(sys.argv[3])
  vx = eval(sys.argv[4])
  vy = eval(sys.argv[5])
  aMagnitude = eval(sys.argv[6])
  DT = eval(sys.argv[7])
  dtsPerDT = eval(sys.argv[8])
  t1 = eval(sys.argv[9])

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
  print >>sys.stderr, "exponent = "+`exponent`
  print >>sys.stderr, "p = "+`p`
  print >>sys.stderr, "v = "+`v`
  print >>sys.stderr, "aMagnitude = "+`aMagnitude`
  print >>sys.stderr, "DT = "+`DT`+" = "+`DT*180./pi`+" degrees"
  print >>sys.stderr, "dt = (DT="+`DT`+"="+`DT*180./pi`+" degrees)/(dtsPerDT="+`dtsPerDT`+") = "+`dt`+" = "+`dt*180./pi`+" degrees"
  print >>sys.stderr, "t1 = "+`t1`
  print '# exponent='+`exponent`+" p="+`p`+" v="+`v`+" aMagnitude="+`aMagnitude`+" dt = (DT="+`DT`+")/(dtsPerDT="+`dtsPerDT`+") = "+`dt`+" = "+`dt*180./pi`+" degrees  t1="+`t1`

  xmin = xmax = ymin = ymax = 0 # XXX actually should be initialize to inverted

  i = 0
  while i*dt <= t1:
    a = -p/abs(p) * abs(p)**exponent * aMagnitude
    #a = -p/abs(p) * aMagnitude
    if i % dtsPerDT == 0:
      print `p.real`+" "+`p.imag` +"  "+`v.real`+" "+`v.imag` +"  "+`a.real`+" "+`a.imag`+"  "+`i*dt/t1`
    # evaluate v at the half-step past p
    v += a * (dt*5 if i==0 else dt)
    # evaluate p at the half-step past v
    p += v * dt
    i += 1

    xmin = min(xmin,p.real)
    xmax = max(xmax,p.real)
    ymin = min(ymin,p.imag)
    ymax = max(ymax,p.imag)
  print             '# bbox = ['+`xmin`+','+`xmax`+']x['+`ymin`+','+`ymax`+']'
  print >>sys.stderr, 'bbox = ['+`xmin`+','+`xmax`+']x['+`ymin`+','+`ymax`+']'

else:
  print >>sys.stderr, "Usage: ./sim.py exponent px py vx vy a DT dtsPerDT t1"
  print >>sys.stderr, "Example: ./sim.py  0  1. 0.  0. 1.  1.  '2*pi/360' 10000  '2*pi'"
  print >>sys.stderr, "Example: ./sim.py  0  1. 0.  0. 1.75  1.  '2*pi/360' 1000  '2*pi*5.18'"
  exit(1)


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

  # Doing the numeric solve for 7/4:
  #     dt=1e-3 -> 1.7471683996321064
  #     dt=1e-4 -> 1.6720304689883347
  #     dt=1e-5 -> 1.6635521991436382
  #     dt=1e-6 -> 1.662751497650623
  #     dt=1e-7 -> 1.662664288780125
  #     dt=1e-8 -> 1.6626561281062888 (2 hours)



  # Asked on math.stackexchange:
  # http://math.stackexchange.com/questions/1345571/what-would-a-planetary-orbit-look-like-if-gravity-had-constant-magnitude

  # Subject: what would a planetary orbit look like if gravity had constant magnitude?
  '''
  Consider a unit-mass particle that is always experiencing a single unit-magnitude
  force towards the origin.  This is a central force, but it is not one of the
  familiar ones, e.g. gravity whose magnitude is proportional to inverse distance squared,
  or a spring force whose magnitude is proportional to distance.

  So the particle is always accelerating towards the origin
  with constant acceleration magnitude $1$.
  Stated as a differential equation,
  working in the $x$-$y$ plane, the particle's position as a function of time
  $\mathbf{p}(t){=}(x(t),y(t))$ satisfies:
  $$\ddot{\mathbf{p}}(t) = -\mathbf{p}(t)/\Vert\mathbf{p}(t)\Vert.$$
  If we are additionally given initial position $\mathbf{p}(0)$
  and velocity $\mathbf{v}(0)=\dot{\mathbf{p}}(0)$,
  then the function $\mathbf{p}$ is completely determined, and it can be easily computed
  numerically to any desired accuracy by simply iterating the following with small enough timestep $dt$:
  \begin{align}
      \mathbf{a} &\leftarrow -\mathbf{p}/\Vert\mathbf{p}\Vert \\
      \mathbf{v} &\leftarrow \mathbf{v} + \mathbf{a}\,\,dt \\
      \mathbf{p} &\leftarrow \mathbf{p} + \mathbf{v}\,\,dt
  \end{align}

  My question: is $\mathbf{p}(t)$ a well-known function,
  and does it have a closed form?

  Of course one case of this is a simple circular orbit of unit radius and speed:
  $$\mathbf{p}(0){=}(1,0), \,\, \mathbf{v}(0){=}(0,1) \,\,\Rightarrow\,\, \mathbf{p}(t)=\left(\cos t,\sin t\right).$$
  More generally, a uniform circular orbit of any radius $r$ and speed $\sqrt{r}$ can be obtained:
  $$\mathbf{p}(0){=}(r,0), \,\, \mathbf{v}(0){=}(0,\sqrt{r}) \,\,\Rightarrow\,\, \mathbf{p}(t)=\left(r \cos\frac{t}{\sqrt{r}},r \sin\frac{t}{\sqrt{r}}\right).$$
  and we check that the desired equation holds:
  \begin{align}
       \dot{\mathbf{p}}(t) &= \left(-\sqrt{r} \sin \frac{t}{\sqrt{r}}, \sqrt{r} \cos \frac{t}{\sqrt{r}}\right) \\
      \ddot{\mathbf{p}}(t) &= \left(-\cos \frac{t}{\sqrt{r}}, -\sin\frac{t}{\sqrt{r}}\right) \\
                           &= -\mathbf{p}(t)/\Vert\mathbf{p}(t)\Vert.
  \end{align}

  Another simple case is when the initial velocity is zero or collinear with the position and the origin;
  in this case it's a 1-dimensional problem
  and the position can easily be seen to be a simple piecewise quadratic function of time.

  But what if the initial conditions are not so nicely aligned?

  To get an idea of the shapes that are possible,
  I've made some plots, using gnuplot, of simulations
  using the simple evolution algorithm I described earlier, with
  $dt = 1/10000$ degree $\approx .00000175$.

  Figure 1 shows five different initial states,
  each evolved from $t{=}0$ to $t{=}2 \pi$:
  $\,\,\mathbf{p}(0){=}(1,0)$, $\mathbf{v}(0){=}(0,v_{0 y})$ for $v_{0 y}{=}0.5,1,1.5,2,2.5$.

  ![Figure 1: p(t) for t=0 to 2*pi][1]

  Figure 2 shows the one with $\mathbf{v}(0){=}(0,2)$ evolved farther, to $t{=}20\pi$.

  ![Figure 2: p(t) for t=0 to 20*pi][2]

  Figure 3 shows it evolved even farther, to $t{=}60\pi$.

  ![Figure 3: p(t) for t=0 to 60*pi][3]

  [1]: http://i.stack.imgur.com/yRvxF.png
  [2]: http://i.stack.imgur.com/g0IGA.png
  [3]: http://i.stack.imgur.com/0gewL.png



  Cropped and better:
  [1]: http://i.stack.imgur.com/yRvxF.png
  [2]: http://i.stack.imgur.com/g0IGA.png
  [3]: http://i.stack.imgur.com/0gewL.png

  Previous Uncropped:
  [1]: http://i.stack.imgur.com/eKpeB.png
  [2]: http://i.stack.imgur.com/BNhBB.png
  [3]: http://i.stack.imgur.com/djpPI.png
  =============================================

UPDATE 2015/07/02:  
It sure looks like a spirograph hypotrochoid, doesn't it?
http://mathworld.wolfram.com/Spirograph.html .

Exploring this possibility,
I found by binary search an initial velocity (0,1.662656) (probably only accurate to 4 decimal places or so)
yielding a closed orbit in the shape of a 7-petalled flower,
and then compared that sim result with the 7-petalled hypotrochoid
having the same min and max radii; see Figure 4.



Conclusion: It's really close, but it's not a hypotrochoid.
It moves too fast at the fast parts and too slow at the slow parts,
and stays a bit too close to the origin during the in-between parts.

  =============================================



  '''

  '''
  time ./sim.py   0.  1. 0.  0. 0.5  1.  '2*pi/36' 10000  '2*pi' >| OUT1
  time ./sim.py   0.  1. 0.  0. 1.0  1.  '2*pi/36' 10000  '2*pi' >| OUT2
  time ./sim.py   0.  1. 0.  0. 1.5  1.  '2*pi/36' 10000  '2*pi' >| OUT3
  time ./sim.py   0.  1. 0.  0. 2.0  1.  '2*pi/36' 10000  '2*pi' >| OUT4
  time ./sim.py   0.  1. 0.  0. 2.5  1.  '2*pi/36' 10000  '2*pi' >| OUT5
  time ./sim.py   0.  1. 0.  0. 2.0  1.  '2*pi/36' 10000  '20*pi' >| OUT4_20pi
  time ./sim.py   0.  1. 0.  0. 2.0  1.  '2*pi/36' 10000  '60*pi' >| OUT4_60pi

  in gnuplot:


  set size square
  set zeroaxis
  set style data lp
  q = 4
  set style line 5 lt rgb '#00cccc' pt 1
  set style line 4 lt rgb 'blue' pt 1
  set style line 3 lt rgb 'magenta' pt 1
  set style line 2 lt rgb 'red' pt 1
  set style line 1 lt rgb '#00cc00' pt 1
  set style line 22 lt rgb 'red' pt 0
  set style line 44 lt rgb 'blue' pt 0

  set key bottom left at -4.7,-3.85; set term pngcairo enhanced size 375,375 font ",9" crop
  set title 'Figure 1: p(t) for t=0 to 2{/Symbol p}' offset 0,-.5
  set output 'Figure1.png'; plot [-q:q] [-q:q] 'OUT5' ls 5 title 'p(0)=(1,0) v(0)=(0,2.5)', 'OUT4' ls 4 title 'p(0)=(1,0) v(0)=(0,2.0)', 'OUT3' ls 3 title 'p(0)=(1,0) v(0)=(0,1.5)', 'OUT2' ls 2 title 'p(0)=(1,0) v(0)=(0,1.0)', 'OUT1' ls 1 title 'p(0)=(1,0) v(0)=(0,0.5)'

  set key bottom left at -4.5,-3.7; set term pngcairo enhanced size 375,375 font ",9" crop
  set title 'Figure 2: p(t) for t=0 to 20{/Symbol p}' offset 0,-.5
  set output 'Figure2.png'; plot [-q:q] [-q:q] 'OUT4_20pi' ls 4 title 'p(0)=(1,0) v(0)=(0,2)', 'OUT2' ls 2 title 'p(0)=(1,0) v(0)=(0,1)'

  set key bottom left at -4.5,-3.7; set term pngcairo enhanced size 375,375 font ",9" crop
  set title 'Figure 3: p(t) for t=0 to 60{/Symbol p}' offset 0,-.5
  set output 'Figure3.png'; plot [-q:q] [-q:q] 'OUT4_60pi' ls 44 title 'p(0)=(1,0) v(0)=(0,2)', 'OUT2' ls 22 title 'p(0)=(1,0) v(0)=(0,1)'
  '''


'''


'''
