package com.donhatchsw.mc4d;

/**
* The model.  Several views can look at the same model.
* The model consists of:
* <pre>
*       Serializable part:
*           - an immutable puzzle desciption (can be shared among multiple models)
*           - the puzzle state (an array of ints)
*           - a history (undo/redo stack of (grip,dir,slicemask) tuples)
*       Non-serializable part:
*           - a queue of pending twists
*           - time fraction of the way done with the first pending twist
*               (it's the view's responsibility to smooth this into a space fraction,
*               so different viewers viewing the same model animation
*               can have different smoothing functions)
*           - a list of listeners (views) that have attached
* </pre>
*/

public class MC4DModel
{
    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    /**
    * Anyone can set this at any time to debug the model's activity;
    * possible values are as follows.
    * <pre>
    *    0: nothing (default)
    *    1: print something on each basic action
    * </pre>
    */
    public int verboseLevel = 0;

    //
    // CLASSES
    //
    /** A twist (grip, dir, slicemask). */
    public static class Twist
    {
        public int grip;
        public int dir; /** -1 = CCW, 1 = CW, can double or triple etc., for power */
        public int slicemask;
        public Twist(int grip, int dir, int slicemask)
        {
            this.grip = grip;
            this.dir = dir;
            this.slicemask = slicemask;
        }

        public String toString()
        {
            return (dir == 1 ? "" : dir==-1 ? "-" : ""+dir+"*")
                 + grip
                 + (slicemask==1||slicemask==0 ? "" : ":"+slicemask); // XXX strange place to the 0->1 thing
        }
        public static Twist fromString(String s)
        {
            com.donhatchsw.compat.regex.Matcher matcher =
            com.donhatchsw.compat.regex.Pattern.compile(
                "\\s*((-)|(-?\\d+)\\*)?(\\d+)(:(-?\\d+))?\\s*"
            ).matcher(s);
            if (!matcher.matches())
                return null; // XXX this will probably lead to a null pointer exception in the caller which is lame... should throw an IllegalArgumentException instead
            String dirStringJustMinus = matcher.group(2);
            String dirStringNumber = matcher.group(3);
            String gripString = matcher.group(4);
            String slicemaskString = matcher.group(6);

            int dir = dirStringJustMinus != null ? -1 :
                         dirStringNumber != null ?  Integer.parseInt(dirStringNumber) : 1;
            int grip = Integer.parseInt(gripString);
            int slicemask = slicemaskString != null ? Integer.parseInt(slicemaskString) : 1;

            return new Twist(grip, dir, slicemask);
        }
        /** regex describing the possible strings returned by fromString() and parsed by toString(). This is used by UndoTree.fromString() and UndoTree.toString(). */
        public static String regex()
        {
            return "(|-|-?\\d+\\*)(\\d+)(|:-?\\d+)";
        }
        /**
        * equals.
        * This is used by UndoTree to decide whether it can
        * smoothly transition between two consecutive moves
        * without having to decelerate to zero at the transition point.
        * <p>
        * XXX The undo tree decision could be more powerful if it took a comparison functor,
        * then it could know that (grip,dir,slicemask)
        * is the same as (opposite grip, -dir, slicemask)...
        * I could accomplish that by making the Twist class
        * be non-static so that it knows about the model,
        * but I'd hate to make this simple well-contained class
        * turn into something that's suddenly fuzzy and unbounded.
        * hmm, but MC4DModel could use a non-static subclass of Twist
        * that *does* know...  bleah I hate making the class heirarchy
        * more baroque than it needs to be,
        * I think passing an isCombinable functor to the evolve() method
        * would be the cleanest way to do it.
        * But, just doing it using equals for now.
        */
        public boolean equals(Object thatObject)
        {
            Twist that = (Twist)thatObject;
            return this.grip == that.grip
                && this.dir == that.dir
                && this.slicemask == that.slicemask;
        }
    } // Twist


    /**
    *  Sequential (pull) model...
    *  Model notifies the listener there is animation available,
    *  and the listener subsequently queries the model's state
    *  and advances it.
    *  Typically the movingNotify callback calls repaint on some component
    *  whose paint method, when called later,
    *  will advance the model's animation state
    *  and compute and display the frame of animation.
    */
    public interface Listener
    {
        /** typically calls repaint on some component. XXX should maybe just call this repaint to make that clear */
        public void movingNotify();
    }


    //
    // SERIALIZABLE PART
    //
        public GenericPuzzleDescription genericPuzzleDescription;
        public int genericPuzzleState[]; // only accessible via listener notification     XXX make this private!  glue looks at it currently

        public com.donhatchsw.util.UndoTree controllerUndoTree = new com.donhatchsw.util.UndoTree();

    //
    // VOLATILE NON-SERIALIZABLE PART
    //
        public com.donhatchsw.util.UndoTree animationUndoTree = new com.donhatchsw.util.UndoTree(controllerUndoTree); // follows controllerUndoTree, but lags behind at the pace of the animation
        private java.util.Vector/*<Listener>*/ listeners = new java.util.Vector();


    //
    // PUBLIC METHODS
    //

        // (No, this one's private... called from fromString once
        // it's parsed everything.  Acquires ownership of the args,
        // which is not the usual way we do things.)
        private MC4DModel(GenericPuzzleDescription genericPuzzleDescription,
                         int genericPuzzleState[],
                         com.donhatchsw.util.UndoTree controllerUndoTree)
        {
            if (genericPuzzleState.length != genericPuzzleDescription.nStickers())
                throw new IllegalArgumentException("MC4DModel.fromString: puzzle description has "+genericPuzzleDescription.nStickers()+" stickers, but state has size "+genericPuzzleState.length+"!?");

            this.genericPuzzleDescription = genericPuzzleDescription;
            this.genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleState);
            this.controllerUndoTree = controllerUndoTree;
        }


        // For a caller who already has a puzzle description...
        public MC4DModel(GenericPuzzleDescription genericPuzzleDescription)
        {
            this.genericPuzzleDescription = genericPuzzleDescription;
            this.genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
            commonInitCode();
        }

        public MC4DModel(String prescription)
        {
            // XXX probably bogus to do this here-- caller should have the opportunity to redirect the progress messages!
            java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                                 new java.io.BufferedWriter(
                                                 new java.io.OutputStreamWriter(
                                                 System.err)));
            this.genericPuzzleDescription = new PolytopePuzzleDescription(
                                                    prescription,
                                                    progressWriter);
            this.genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
            commonInitCode();
        }

        private void commonInitCode()
        {
            // XXX need to store this listener and remove it when we break down, I think?
            // Make it so the animation gets goosed when
            // the controllerUndoTree changes (e.g. when
            // an undo is initiated, either from the app
            // or the undo tree viewer window or whatever...
            // I haven't even written that part yet!)
            // This will start things pumping.
            controllerUndoTree.addListener(new com.donhatchsw.util.UndoTree.Listener() {
                public void somethingChanged()
                {
                    // controllerUndoTree changed.
                    // actually all we have to do is call repaint;
                    // this will make our paint() eventually get called
                    // which will advance the animation.
                    if (!listeners.isEmpty())
                    {
                        // Just notify the first listener in the chain.
                        ((Listener)listeners.elementAt(0)).movingNotify();
                    }
                }
            });
        }

        /** Adds a listener that will be notified when the puzzle animation progresses. */
        public synchronized void addListener(Listener listener)
        {
            // XXX assert it wasn't there
            listeners.addElement(listener);
        }
        /** Removes a listener. */
        public synchronized void removeListener(Listener listener)
        {
            // XXX assert it was there
            listeners.removeElement(listener);
        }
        public synchronized int nListeners()
        {
            return listeners.size();
        }

        /** Initiates a twist. */
        public synchronized void initiateTwist(int grip,
                                               int dir,
                                               int slicemask)
        {
            Twist twist = new Twist(grip, dir, slicemask);
            controllerUndoTree.Do(twist);
        }
        /** Initiates an undo.  Returns true if successful, false if there was nothing to undo. */
        public synchronized boolean initiateUndo()
        {
            if (verboseLevel >= 1) System.out.println("MODEL: initiating an undo");
            Twist twist = (Twist)controllerUndoTree.undo();
            return twist != null; // whether there was actually something to undo
        }
        /** Initiates an redo.  Returns true if successful, false if there was nothing to redo. */
        public synchronized boolean initiateRedo()
        {
            if (verboseLevel >= 1) System.out.println("MODEL: initiating a redo");
            Twist twist = (Twist)controllerUndoTree.undo();
            if (twist != null)
            {
                if (!listeners.isEmpty())
                {
                    // Just notify the first listener in the chain.
                    ((Listener)listeners.elementAt(0)).movingNotify();
                }
                return true;
            }
            else
                return false;
        }

        public synchronized void initiateCheat()
        {
            if (verboseLevel >= 1) System.out.println("MODEL: initiating a cheat");
            if (controllerUndoTree.getCurrentNodeIndex() != 0)
            {
                controllerUndoTree.setCurrentNodeIndex(0);
                if (!listeners.isEmpty())
                {
                    // Just notify the first listener in the chain.
                    ((Listener)listeners.elementAt(0)).movingNotify();
                }
            }
        }

        /**
        *  Advances the animation for this listener.
        *  Calls movingNotify() on the next listener in the listener chain
        *  (which is just this listener if there is only one).
        *  This sounds pretty weird but it makes sure all the listeners
        *  get a piece of the action if there are
        *  several listeners (viewers) going at once.
        *  The listener should NOT call its own repaint itself
        *  to keep an animation going,
        *  or it will hog all the action.  It needs to wait its turn
        *  and let its repaint get called from inside some other guy's
        *  advanceAnimation.
        *  To be precise, if there are two listeners (viewers),
        *  here's the way it goes:
        *  <pre>
        *      someone initiates a twist by calling model.initiateTwist()
        *          listener0's movingNotify gets called
        *              it calls viewer0.repaint()
        *
        *      viewer0's rendering thread wakes up because of the repaint
        *      viewer0's paint gets called.  inside its paint:
        *          model.advanceAnimation(listener0)
        *              inside there, calls listener1's movingNotify   (next in the chain)
        *                  which calls viewer1.repaint()
        *                  (viewer1's rendering thread can now wake up if it wants)
        *          animationstate = model.getanimationstate(listener0)
        *          compute and render the frame
        *      viewer0's rendering thread now goes back to sleep waiting for a repaint
        *
        *      viewer1's rendering thread wakes up because of the repaint
        *      viewer1's paint gets called. inside its paint:
        *          model.advanceAnimation(listener1)
        *              inside there, calls listener0's movingNotify   (next in the chain)
        *                  which calls viewer0.repaint()
        *                  (viewer0's rendering thread can now wake up if it wants)
        *          ...
        *  </pre>
        *
        *  When the animation is done, someone's advanceAnimation()
        *  won't call movingNotify() on the next guy, and the action stops
        *  (until someone initiates another twist).
        *  <p>
        *  If there is only one viewer, the "next in chain" is itself,
        *  so its advanceAnimation ends up calling its own movingNotify
        *  which calls its own repaint, so it's all good.
        */
        public synchronized void advanceAnimation(Listener listener, double nFrames90, double criticalDampingFraction)
        {
            if (verboseLevel >= 1) System.out.println("MODEL: advancing animation");

            // XXX hack-- trying to figure out the nice way to do it when there are like 90 windows each with different nFrames90... go at the speed of the slowest?  for now, since each listener is only getting 1/nListeners of the notifications, multiply by the number of listeners, otherwise we'll get huge gaps and it sucks.  I tried it.
            nFrames90 *= listeners.size();

            boolean wasMoving = animationUndoTree.isMoving(controllerUndoTree);

            com.donhatchsw.util.UndoTree.ReturnValueOfIncrementViewTowardsOtherView discreteStateChange = animationUndoTree.incrementViewTowardsOtherView(
                controllerUndoTree,
                1.,        // the canonical edge length
                nFrames90, // takes this amount of time
                1.,        // and we are incrementing by this amount of time
                criticalDampingFraction,
                // XXX don't need to make this every time!
                new com.donhatchsw.util.UndoTree.ItemLengthizer() {
                    public double length(Object item)
                    {
                        Twist twist = (Twist)item;
                        Assert(twist != null);
                        int order = genericPuzzleDescription.getGripSymmetryOrders()[twist.grip];
                        if (order <= 0)
                            return 1.; // XXX is this sensible?
                        double nQuarterTurns = 4./order
                                             * Math.abs(twist.dir); // power multiplier
                        return nQuarterTurns;
                    }
            });

            if (discreteStateChange != null)
            {
                Twist twist = (Twist)discreteStateChange.item;
                // Apply the discrete state change
                // to the permutation array.
                // discreteStateChange.dir is 1 for Do/redo and -1 for undo,
                // so we can express the resulting twist direction neatly
                // as discreteStateChange.dir * twist.dir.
                //
                genericPuzzleDescription.applyTwistToState(
                    genericPuzzleState,
                    twist.grip,
                    discreteStateChange.dir * twist.dir,
                    twist.slicemask);
            }

            if (wasMoving)
                nextListener(listener).movingNotify(); // typically calls repaint
                // XXX oh wait... the last few guys won't update!?  bleah
        } // advanceAnimation



        /**
        * Returns true if a call to advanceAnimation() will actually
        * change anything (and notify anyone); i.e. if getAnimationState()
        * would return different results before and after an advanceAnimation() call.
        * This might be used to decide whether to do a high quality
        * render or not.
        * Note, if advanceAnimation actually does something,
        * it will notify the next listener in the chain
        * and the current listener should NOT call repaint....
        * however if the caller wants to repaint for some other reason,
        * then they DO need to call repaint if not moving.
        * So isMoving() can be used to test for that.
        */
        public synchronized boolean isMoving()
        {
            return animationUndoTree.isMoving(controllerUndoTree);
        }


        /**
        *  Gets a prescription for the current frame of animation,
        *  suitable for passing into GenericPipelineUtils.computeFrame.
        *  The function return value is the time fraction
        *  of the progress into the twist.
        *  (Note, the caller needs to convert this time fraction
        *  into a space fraction before passing it to the GenericPipeline
        *  stuff, typically using some smoothing function f
        *  such that f(0)=0, f(1)=1, f'(0)=0, f'(1)=0.)
        */
        public synchronized double getAnimationState(Listener listener, // XXX huh?  is listener used? I don't think so
                                                     int returnPuzzleState[],
                                                     Twist returnTwist)
        {
            Twist twist = (Twist)animationUndoTree.getItemOnEdgeFromParentToCurrentNode();
            double returnFrac = 1.-animationUndoTree.getCurrentNodeFraction();

            if (twist == null) // this happens if it's the root
            {
                returnTwist.grip = 0;
                returnTwist.dir = 0;
                returnTwist.slicemask = 0;
            }
            else
            {
                returnTwist.grip = twist.grip;
                returnTwist.dir = -twist.dir;
                returnTwist.slicemask = twist.slicemask;
            }
            if (verboseLevel >= 1) System.out.println("MODEL: giving someone animation state: "+returnFrac+" of the way (in time) through a "+returnTwist+"");
            return returnFrac;
        }

        /**
        * Convert the model to a string,
        * suitable for saving in a save file or whatever.
        */
        public String toString()
        {
            String nl = System.getProperty("line.separator"); // XXX ACK! this will mess up everything on the mac!!!  I think maybe I should just use '\n'!

            StringBuffer sb = new StringBuffer();
            sb.append("{"+nl);
            sb.append("    genericPuzzleDescription = "+genericPuzzleDescription+","+nl);
            sb.append("    genericPuzzleState = ");
            // XXX need to sort by face and put each on a line by itself
            sb.append(com.donhatchsw.util.Arrays.toStringCompact(genericPuzzleState));
            sb.append(","+nl);
            sb.append("    history = "+com.donhatchsw.util.Arrays.toStringCompact(controllerUndoTree)+","+nl); // XXX not right yet!
            sb.append("}");
            return sb.toString();
        } // toString
        // XXX get clear on who does what... this could be a constructor, but would need to be smart and differentiate it from just a prescription
        public static MC4DModel fromString(String s)
        {
            // First replace anything that looks like it could be
            // a line separator with a single newline, for consistency...
            s = com.donhatchsw.compat.regex.replaceAll(s, "\n|\r\n|\r", "\n"); // XXX hasn't been tested yet
            // XXX doing it all in one bite like this is powerful, but unfortunately it doesn't let us give very good error messages :-(
            // I can write the pattern more clearly if I use a single space
            // whenever I mean \s*, and then convert...
            //com.donhatchsw.compat.regex.verboseLevel = 1;
            String prepattern = " \\{ genericPuzzleDescription = ([^\n]+) , \n genericPuzzleState = ([^\n]+) , \n history = ([^\n]+) , \n undoPartSize = (\\d+) ,? \\} ";
                   //prepattern = " \\{ genericPuzzleDescription = ([^\n]+) ,  (.|\r|\n)*"; // XXX
            String pattern = com.donhatchsw.compat.regex.replaceAll(prepattern, " ", "\\\\s*");
            System.out.println("pattern = "+com.donhatchsw.util.Arrays.toStringCompact(pattern)+"");
            com.donhatchsw.compat.regex.Matcher matcher =
            com.donhatchsw.compat.regex.Pattern.compile(pattern).matcher(s);
            if (!matcher.matches())
                throw new IllegalArgumentException("MC4DModel.fromString called on a bad string of length "+s.length()+": "+com.donhatchsw.util.Arrays.toStringCompact(s)+"");
            Assert(matcher.groupCount() == 4);
            String genericPuzzleDescriptionString = matcher.group(1);
            String genericPuzzleStateString = matcher.group(2);
            String historyString = matcher.group(3);
            String undoPartSizeString = matcher.group(4);
            if (false)
            {
                System.out.println("genericPuzzleDescriptionString = "+genericPuzzleDescriptionString);
                System.out.println("genericPuzzleStateString = "+genericPuzzleStateString);
                System.out.println("historyString = "+historyString);
                System.out.println("undoPartSizeString = "+undoPartSizeString);
            }

            historyString.replaceAll("\\[(.*)\\]", "$1"); // silly way to get rid of the surrounding brackets that Arrays.toString put there when printing the Vector
            com.donhatchsw.util.UndoTree controllerUndoTree = com.donhatchsw.util.UndoTree.fromString(historyString, new com.donhatchsw.util.UndoTree.ItemFromString() {
                public String regex()
                {
                    return Twist.regex();
                }
                public Object itemFromString(String s)
                {
                    return Twist.fromString(s);
                }
            });

            int genericPuzzleState[] = null;
            try {
                genericPuzzleState = (int[])com.donhatchsw.util.Arrays.fromString(genericPuzzleStateString);
                // XXX also can be class cast problem if it looked like the wrong kind of array
            } catch (java.text.ParseException e) {
                throw new IllegalArgumentException("MC4DModel.fromString called on bogus int array string "+com.donhatchsw.util.Arrays.toStringCompact(genericPuzzleStateString)+": "+e);
            }

            GenericPuzzleDescription genericPuzzleDescription = GenericPuzzleFactory.construct(genericPuzzleDescriptionString,
                                                 new java.io.PrintWriter(
                                                 new java.io.BufferedWriter(
                                                 new java.io.OutputStreamWriter(
                                                 System.err))));

            MC4DModel model = new MC4DModel(genericPuzzleDescription,
                                            genericPuzzleState,
                                            controllerUndoTree);
            return model;
        } // fromString


    //
    // PRIVATE UTILITY STUFF
    //

        private Listener nextListener(Listener listener)
        {
            int index = listeners.indexOf(listener);
            if (index == -1)
                throw new IllegalArgumentException("MC4DModel: listener "+listener+" is not attached!? ("+listeners.size()+" listeners attached)");
            return (Listener)listeners.elementAt((index+1)%listeners.size());
        }

} // class MC4DModel
