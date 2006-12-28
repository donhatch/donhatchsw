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
    } // Twist

    private static class TwistForAnimationQueue
    {
        public Twist twist; // has-a, not is-a, whenever possible
        public boolean isUndo; // if true, don't put on undo queue when done
        public TwistForAnimationQueue(Twist twist, boolean isUndo)
        {
            this.twist = twist;
            this.isUndo = isUndo;
        }
    }

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

        public java.util.Vector/*<Twist>*/ history = new java.util.Vector();
        public int undoPartSize = 0; // history has undo part followed by redo part
    //
    // VOLATILE NON-SERIALIZABLE PART
    //
        private java.util.LinkedList/*<Twist>*/ pendingTwists = new java.util.LinkedList();
        private double timeFractionOfWayThroughFirstPendingTwist = 0.f;
        private java.util.Vector/*<Listener>*/ listeners = new java.util.Vector();


    //
    // PUBLIC METHODS
    //

        // (No, this one's private... called from fromString once
        // it's parsed everything.  Acquires ownership of the args,
        // which is not the usual way we do things.)
        public MC4DModel(GenericPuzzleDescription genericPuzzleDescription,
                         int genericPuzzleState[],
                         java.util.Vector history,
                         int undoPartSize)
        {
            if (genericPuzzleState.length != genericPuzzleDescription.nStickers())
                throw new IllegalArgumentException("MC4DModel.fromString: puzzle description has "+genericPuzzleDescription.nStickers()+" stickers, but state has size "+genericPuzzleState.length+"!?");
            if (undoPartSize < 0
             || undoPartSize > history.size())
                throw new IllegalArgumentException("MC4DModel.fromString: undoPartSize = "+undoPartSize+" but history only has size "+history.size()+"");

            this.genericPuzzleDescription = genericPuzzleDescription;
            this.genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleState);
            this.history = history;
            this.undoPartSize = undoPartSize;
        }


        // For a caller who already has a puzzle description...
        public MC4DModel(GenericPuzzleDescription genericPuzzleDescription)
        {
            this.genericPuzzleDescription = genericPuzzleDescription;
            this.genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
        }

        public MC4DModel(String prescription)
        {
            init(prescription);
        }

        private void init(String prescription)
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

        /** Initiates a twist. */
        public synchronized void initiateTwist(int grip,
                                               int dir,
                                               int slicemask)
        {
            if (verboseLevel >= 1) System.out.println("MODEL: initiating a twist ("+pendingTwists.size()+" already pending)");
            Twist twist = new Twist(grip, dir, slicemask);
            initiateTwistOrUndoOrRedo(twist, false);
            history.setSize(undoPartSize); // clear redo part
            history.addElement(twist);
            undoPartSize++;
        }
        /** Initiates an undo.  Returns true if successful, false if there was nothing to undo. */
        public synchronized boolean initiateUndo()
        {
            if (verboseLevel >= 1) System.out.println("MODEL: initiating an undo");
            if (undoPartSize == 0)
            {
                if (verboseLevel >= 1) System.out.println("    NOT!!");
                return false; // failed
            }
            Twist twist = (Twist)history.elementAt(--undoPartSize);
            initiateTwistOrUndoOrRedo(new Twist(twist.grip, -twist.dir, twist.slicemask), true);
            return true; // succeeded
        }
        /** Initiates an redo.  Returns true if successful, false if there was nothing to redo. */
        public synchronized boolean initiateRedo()
        {
            if (verboseLevel >= 1) System.out.println("MODEL: initiating a redo");
            if (undoPartSize == history.size())
            {
                if (verboseLevel >= 1) System.out.println("    NOT!!");
                return false; // failed
            }
            Twist twist = (Twist)history.elementAt(undoPartSize++);
            initiateTwistOrUndoOrRedo(twist, false);
            return true; // succeeded
        }

        // common code used by all of the above...
        // caller must synchronize.
        private void initiateTwistOrUndoOrRedo(Twist twist,
                                               boolean isUndo)
        {
            if (!listeners.isEmpty())
            {
                // Just notify the first listener in the chain.
                ((Listener)listeners.elementAt(0)).movingNotify();
            }
            pendingTwists.add(new TwistForAnimationQueue(twist, isUndo));
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
        public synchronized void advanceAnimation(Listener listener, float nFrames90)
        {
            if (verboseLevel >= 1) System.out.println("MODEL: advancing animation");
            if (pendingTwists.isEmpty())
            {
                if (verboseLevel >= 1) System.out.println("    NOT!!!!");
                return; // XXX argh! if an animation just completed, need to make sure each listener gets a final notify-- don't just stop notifying when the animation queue is empty
            }

            // XXX hack-- trying to figure out the nice way to do it when there are like 90 windows each with different nFrames90... go at the speed of the slowest?  for now, since each listener is only getting 1/nListeners of the notifications, multiply by the number of listeners.
            nFrames90 *= listeners.size();

            TwistForAnimationQueue item = (TwistForAnimationQueue)pendingTwists.getFirst();
            Twist twist = item.twist;
            int order = genericPuzzleDescription.getGripSymmetryOrders()[twist.grip];
            double totalRotationAngle = 2*Math.PI/order*Math.abs(twist.dir);
            double nFramesTotal = Math.sqrt(totalRotationAngle/(Math.PI/2.))*nFrames90;

            if (nFramesTotal <= 1.)
                nFramesTotal = 1.; // just make sure it gets there immediately, without risking zero-divide
            timeFractionOfWayThroughFirstPendingTwist += 1./nFramesTotal;
            if (timeFractionOfWayThroughFirstPendingTwist >= 1.)
            {
                pendingTwists.removeFirst(); // it's item
                genericPuzzleDescription.applyTwistToState(
                    genericPuzzleState,
                    item.twist.grip,
                    item.twist.dir,
                    item.twist.slicemask);
                if (!item.isUndo)
                {
                    // XXX should we do this?  remind myself of why I did it in the glue implementation
                    //clear the redo part of the history
                    //history.addElement(item.twist);
                }
                timeFractionOfWayThroughFirstPendingTwist = 0.;
            }

            nextListener(listener).movingNotify(); // typically calls repaint
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
        public synchronized boolean isMoving(Listener listener)
        {
            if (verboseLevel >= 1) System.out.println("MODEL: isMoving returning "+!pendingTwists.isEmpty());
            return !pendingTwists.isEmpty();
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
        public synchronized double getAnimationState(Listener listener,
                                                     int returnPuzzleState[],
                                                     Twist returnTwist)
        {
            if (pendingTwists.isEmpty())
            {
                if (verboseLevel >= 1) System.out.println("MODEL: giving someone animation state: NOT!");
                return 0.; // so all the twist params will be irrelevant
            }
            Twist firstPendingTwist = ((TwistForAnimationQueue)pendingTwists.getFirst()).twist;
            com.donhatchsw.util.VecMath.copyvec(returnPuzzleState, genericPuzzleState);
            returnTwist.grip = firstPendingTwist.grip;
            returnTwist.dir = firstPendingTwist.dir;
            returnTwist.slicemask = firstPendingTwist.slicemask;
            if (verboseLevel >= 1) System.out.println("MODEL: giving someone animation state: "+timeFractionOfWayThroughFirstPendingTwist+" of the way (in time) through a "+returnTwist+"");
            return timeFractionOfWayThroughFirstPendingTwist;
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
            sb.append("    history = "+com.donhatchsw.util.Arrays.toStringCompact(history)+","+nl);
            sb.append("    undoPartSize = "+undoPartSize+","+nl);
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
            String historyTwistStrings[] = com.donhatchsw.compat.regex.split(historyString, ",");
            java.util.Vector history = new java.util.Vector();
            for (int i = 0; i < historyTwistStrings.length; ++i)
                history.addElement(Twist.fromString(historyTwistStrings[i]));
            int undoPartSize = Integer.parseInt(undoPartSizeString);

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
                                            history,
                                            undoPartSize);
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
