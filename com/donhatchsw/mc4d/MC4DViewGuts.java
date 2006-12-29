/**
* All right, here's how to make everyone happy...
* A completely generic viewer that can be stuck inside
* an ancient Canvas or icky Applet
* or a beautiful modern JPanel or whatever.
*
* We can't derive it from Canvas,
* and we can't derive it from JComponent either...
* Let's not derive it from anything.
* The only reason for deriving from anything anyway
* is so we can overload the paint() method--
* so we just let the caller do that and let it call us.
*
* For example:
* <pre>
*       class ModernMC4DView
*           extends JPanel
*       {
*           MC4DViewGuts guts; // has-a, not is-a
*
*           public ModernMC4DView()
*           {
*               guts = new MC4DViewGuts(this); // adds listeners to this
*           }
*           public void paintComponent(java.awt.Graphics g)
*           {
*               guts.paint(this, g);
*           }
*       }
*
*       class AncientMC4DView
*           extends java.awt.Canvas
*       {
*           MC4DViewGuts guts; // has-a, not is-a
*
*           public AncientMC4DView()
*           {
*               guts = new MC4DViewGuts(this); // adds listeners to this
*           }
*           public void paint(java.awt.Graphics g)
*           {
*               guts.paint(this, g);
*           }
*       }
* </pre>
*/

package com.donhatchsw.mc4d;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*; // XXX this needs to go in a different file

import com.donhatchsw.util.VecMath;

public class MC4DViewGuts
{
    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    //
    // Classes...
    //
        public static interface InterpFunc { public float func(float f); }
        public static InterpFunc sine_interp = new InterpFunc() {
            public float func(float x) { return (float)(Math.sin((x - .5) * Math.PI) + 1) / 2; }
        };
        public static InterpFunc linear_interp = new InterpFunc() {
            public float func(float x) { return x; }
        };

    //
    // Puzzle description and state...
    // shared by all views.  We do NOT own this (and it's immutable
    // anyway, so it can always be shared).
    // Note, the view needs to be careful to be robust in the case that the model
    // is swapped with a different model-- e.g. if it's storing the index
    // of the current sticker, that index can suddenly be out of bounds.
    //
        public MC4DModel model = null;

    //
    // Viewing parameters...
    // each view has its own set of these
    // XXX LAME!  should just be one view per guts!  this works but has no value!
    //
    public static class PerViewState
    {
        /**
        * Anyone can set this at any time to debug the view's activity;
        * possible values are as follows.
        * <pre>
        *      0: nothing (default)
        *      1: key and mouse press/release/click
        *      2: and mouse drags
        *      3: and update/paint
        *      4: and enter/exit (can be obnoxious)
        *      5: and mouse motion (can be obnoxious)
        *</pre>
        */
        public int eventVerboseLevel = 0;

        public float faceShrink = .4f;
        public float stickerShrink = .5f;
        public float viewMat4d[][] = {
            {1,0,0,0},
            {0,1,0,0},
            {0,0,1,0},
            {0,0,0,1},
        };
        public float eyeW = 5.2f;
        public float viewMat3d[][] =
            VecMath.mxm(VecMath.makeRowRotMat(3, 2,0, -42*(float)Math.PI/180), // twirl
                        VecMath.makeRowRotMat(3, 1,2,  30*(float)Math.PI/180)); // tilt
        public float eyeZ = 10.f;
        public float scale = 1.f;
        public float towardsSunVec[] = {.82f, 1.55f, 3.3f};

        public boolean showShadows = true;
        public java.awt.Color ground = new Color(20, 130, 20);
        public float faceRGB[][] = {
            {0, 0, 1},
            {0.5f, 0, 0},
            {.4f, 1, 1},
            {1, 0, .5f},
            {.9f, .5f, 1},
            {1, .5f, 0},
            {1, 1, .5f},
            {0, 1, .5f},
        };
        public boolean highlightByCubie = false;
        public boolean highlightByGrip = false; // XXX need to set this automatically maybe, based on the puzzle description
        //public java.awt.Color outlineColor = java.awt.Color.black;
        public java.awt.Color outlineColor = null;
        //public float nFrames90 = 15;
        public float nFrames90 = 15;
        InterpFunc interp = sine_interp;
        //InterpFunc interp = linear_interp;
        private boolean restrictRoll = false; // this one is private with accessors
            public boolean getRestrictRoll() { return restrictRoll; }
            public void setRestrictRoll(MC4DModel model, Component view, boolean newRestrictRoll) // XXX shouldn't take model as a param I don't think, revisit this
            {
                // Do this even if it looks like we were already
                // restricting roll, in case we are initializing
                // a new model or something XXX revisit whether this is the cleanest way to do this
                if (newRestrictRoll && model != null)
                    initiateZeroRoll(this, // XXX lame-- revisit this
                                     model.genericPuzzleDescription.getFaceCentersAtRest(),
                                     viewMat4d,
                                     viewMat3d,
                                     nFrames90,
                                     view);
                restrictRoll = newRestrictRoll;
            }
        public boolean spinDragRequiresCtrl = false;

        //
        // Debugging state variables.
        // Most of these are settable using secret ctrl-alt key combinations.
        //
        public boolean useTopsort = true;
        public int jitterRadius = 0;
        public boolean drawLabels = false;
        public boolean showPartialOrder = false;
        public boolean frozenForDebugging = false;
            public int frozenPartialOrderForDebugging[][] = null;

        //
        // Mouse and keyboard state...
        //
        private int slicemask = 0; // bitmask representing which number keys are down
        private int nShiftsDown = 0; // keep track of whether both shift keys are being held down.  This isn't completely reliable (e.g. when mouse exits window while shift is down) but we always set it to 0 when a non-shifted mouse event occurs so it's not too dangerous.
        private int lastDrag[] = null; // non-null == dragging.  always tracks mouse drag regardless of ctrl
        private long lastDragTime = -1L; // timestamp of last drag event.  always tracks mouse drag regardless of ctrl
        private float spinDelta[][] = null; // rotation to add for each frame while spinning. null == stopped
        private float dragDelta[][] = null; // while dragging, we keep track of the most recent drag delta, this will grduate into spinDelta when we let go.  (Melinda's applet used spinDelta for both, but that made things complicated in paint when deciding whether to keep spinning or not when mouse was down, especially when combined with spinDragRequiresCtrl.)
        private int iStickerUnderMouse = -1;
        private int iPolyUnderMouse = -1;

        //
        // A rotation is currently in progress if iRotation < nRotation.
        // XXX this is a fucked way to do it, it sucks when the speed isn't responsive to the slider!
        //
        private int nRotation = 0; // total number of rotation frames in progress // XXX need to make this variable, it sucks when the speed isn't responsive to the slider!
        private int iRotation = 0; // number of frames done so far
         private float rotationFrom[]; // where rotation is rotating from, in 4space
         private float rotationTo[]; // where rotation is rotating to, in 4space

        //
        // Most recently chosen zero-roll pole.
        // It's a 4d vector but the w component is zero, generally.
        //
        public float zeroRollPoleAfterRot3d[] = null;

        //
        // Two scratch Frames to use for computing and painting.
        //
        public GenericPipelineUtils.Frame untwistedFrame = new GenericPipelineUtils.Frame();
        public GenericPipelineUtils.Frame twistingFrame = new GenericPipelineUtils.Frame();
            { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something


        //
        // The listener I use to listen to the model
        // for changes (initiated by other views or by myself)
        //
        MC4DModel.Listener modelListener;

        //
        // The listeners I use
        // to listen to the Component I draw on
        //
        private KeyListener keyListener;
        private MouseListener mouseListener;
        private MouseMotionListener mouseMotionListener;

        // To restore when I detach from this Component
        private java.util.Vector savedKeyListeners;
        private java.util.Vector savedMouseListeners;
        private java.util.Vector savedMouseMotionListeners;
    } // class PerViewState

    //
    // Views
    //
    private java.util.Vector/*<Component>*/ views = new java.util.Vector();
    private java.util.Hashtable perViewStates/*<Component, PerViewState>*/ = new java.util.Hashtable();

    /** Detaches this view guts from its old model, and attaches to the new one. */
    public void setModel(MC4DModel newModel)
    {
        int nViews = views.size();
        if (model != null)
        {
            // Disconnect all our listeners from the current model...
            for (int i = 0; i < nViews; ++i)
            {
                Component view = (Component)views.get(i);
                PerViewState perViewState = (PerViewState)perViewStates.get(view);
                model.removeListener(perViewState.modelListener);
            }
        }
        // Change the model...
        model = newModel;
        if (model != null)
        {
            // Connect all our listeners to the new model
            for (int i = 0; i < nViews; ++i)
            {
                Component view = (Component)views.get(i);
                PerViewState perViewState = (PerViewState)perViewStates.get(view);
                model.addListener(perViewState.modelListener);
                perViewState.setRestrictRoll(model, view, perViewState.getRestrictRoll()); // initiates the zero roll animation if appropriate
            }
        }
        // Goose the views to make sure they repaint
        for (int i = 0; i < nViews; ++i)
        {
            Component view = (Component)views.get(i);
            view.repaint();
        }
    } // setModel


    /** Detaches this view from these guts. */
    public void detachListeners(Component view)
    {
        PerViewState perViewState = (PerViewState)perViewStates.get(view);
        if (perViewState == null)
            return; // XXX maybe should throw an error

        model.removeListener(perViewState.modelListener);

        view.removeKeyListener(perViewState.keyListener);
        view.removeMouseListener(perViewState.mouseListener);
        view.removeMouseMotionListener(perViewState.mouseMotionListener);

        perViewStates.remove(view);
        views.removeElement(view);
    } // detachListeners

    /** Attaches the view to these guts, optionally suppressing existing listeners until detached.  More than one view can be attached to a guts. */
    public void attachListeners(final Component view,
                                boolean suppressExistingListeners) // XXX implement this!
    {
        if (perViewStates.get(view) != null)
            return; // XXX maybe should throw an error

        views.addElement(view);
        final PerViewState perViewState = new PerViewState();
        perViewStates.put(view, perViewState);

        // Listen to the model for changes (increment in animation).
        // Our response to a change is simply to call repaint on the view Component;
        // then when paint() is called it will query the animation progress
        // from the model (which pumps the animation).
        model.addListener(perViewState.modelListener = new MC4DModel.Listener() {
            public void movingNotify()
            {
                view.repaint();
            }
        });

        if (suppressExistingListeners)
        {
            // XXX get the listeners into the saved listener lists
            // XXX and remove them from the component
        }

        view.addKeyListener(perViewState.keyListener = new KeyListener() {
            public void keyPressed(KeyEvent ke)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("keyPressed "+ke);
                if (perViewState.eventVerboseLevel >= 1) System.out.println("    isShiftDown = "+ke.isShiftDown());
                int keyCode = ke.getKeyCode();
                int numkey = keyCode - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9)
                    perViewState.slicemask |= 1<<(numkey-1); // turn on the specified bit
                if (keyCode == KeyEvent.VK_SHIFT)
                {
                    perViewState.nShiftsDown++;
                    if (perViewState.eventVerboseLevel >= 1) System.out.println("    nShiftsDown = "+perViewState.nShiftsDown);
                }
            }
            public void keyReleased(KeyEvent ke)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("keyReleased "+ke);
                if (perViewState.eventVerboseLevel >= 1) System.out.println("    isShiftDown = "+ke.isShiftDown());
                int keyCode = ke.getKeyCode();
                int numkey = keyCode - KeyEvent.VK_0;
                if(1 <= numkey && numkey <= 9)
                    perViewState.slicemask &= ~(1<<(numkey-1)); // turn off the specified bit
                if (keyCode == KeyEvent.VK_SHIFT)
                {
                    // If nShiftsDown becomes >= 2,
                    // don't decrease it when we get the shift key release.
                    // This lets the user hold down one shift key
                    // and pump up the power with the other!
                    // This is not too dangerous because it always
                    // deflates to zero as any mouse event occurs
                    // with no shift key down.
                    if (perViewState.nShiftsDown == 1)
                        perViewState.nShiftsDown = 0;
                    if (perViewState.eventVerboseLevel >= 1) System.out.println("    nShiftsDown = "+perViewState.nShiftsDown);
                }
            }
            public void keyTyped(KeyEvent ke)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("keyTyped");
                char c = ke.getKeyChar();
                //System.out.println("generic key listener got key '"+c+"'("+(int)c+")"); // XXX escapify!

                if (false) {}
                else if (c == 's'-'a'+1) // ctrl-s -- save
                {
                    // For starters, just dump the model to stdout.
                    System.out.println("The model is...");
                    System.out.println(model);
                }
                else if (c == 't'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-t
                {
                    System.out.println("useTopsort "+perViewState.useTopsort+" -> "+!perViewState.useTopsort+"");
                    perViewState.useTopsort = !perViewState.useTopsort;
                    view.repaint();
                }
                else if (c == 'j'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-j
                {
                    perViewState.jitterRadius++;
                    if (perViewState.jitterRadius == 10)
                        perViewState.jitterRadius = 0;
                    System.out.println("jitterRadius -> "+perViewState.jitterRadius+"");
                    view.repaint();
                }
                else if (c == 'l'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-l
                {
                    System.out.println("drawLabels "+perViewState.drawLabels+" -> "+!perViewState.drawLabels+"");
                    perViewState.drawLabels = !perViewState.drawLabels;
                    view.repaint();
                }
                else if (c == 'p'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-p
                {
                    System.out.println("showPartialOrder "+perViewState.showPartialOrder+" -> "+!perViewState.showPartialOrder+"");
                    perViewState.showPartialOrder = !perViewState.showPartialOrder;
                    view.repaint();
                }
                else if (c == ' ' && ke.isControlDown()
                 && ke.isAltDown()) // ctrl-alt-space
                {
                    System.out.println("frozenForDebugging "+perViewState.frozenForDebugging+" -> "+!perViewState.frozenForDebugging+"");
                    perViewState.frozenForDebugging = !perViewState.frozenForDebugging;
                    perViewState.frozenPartialOrderForDebugging = null;
                    view.repaint();
                }
                else if (c == 'v'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-v -- cycle eventVerboseLevel
                {
                    System.out.print("eventVerboseLevel "+perViewState.eventVerboseLevel);
                    perViewState.eventVerboseLevel = (perViewState.eventVerboseLevel+1) % 8;
                    System.out.println(" -> "+perViewState.eventVerboseLevel);
                    ke.consume(); // XXX does this make it so subsequent handlers don't see it? dammit, no it doesn't. damn damn damn fuck fuck fuck
                }
                else if (ke.isControlDown()
                      && ke.isAltDown())
                {
                    System.out.println("Unrecognized key sequence ctrl-alt-"+(char)(c|64)+"");
                }
            }
        });
        view.addMouseListener(perViewState.mouseListener = new MouseListener() {
            public void mouseClicked(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mouseClicked on a "+view.getClass().getSuperclass().getName());
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down
                Assert(perViewState != null); // should be no way to make this happen

                if (perViewState.spinDragRequiresCtrl)
                {
                    if (me.isControlDown())
                        return; // restricted mode, so ctrled mouse only affects the spin... which is implemented in the press,drag,release callbacks, not this one
                    view.repaint(); // so it keeps spinning if it was spinning
                }

                boolean isRotate = isMiddleMouseButton(me);
                if (false) // make this true to debug the pick
                {
                    int hit[] = GenericPipelineUtils.pick(me.getX(), me.getY(),
                                                          perViewState.untwistedFrame,
                                                          model.genericPuzzleDescription);
                    if (hit != null)
                    {
                        int iSticker = hit[0];
                        int iFace = model.genericPuzzleDescription.getSticker2Face()[iSticker];
                        int iCubie = model.genericPuzzleDescription.getSticker2Cubie()[iSticker];
                        System.err.println("    Hit sticker "+iSticker+"(polygon "+hit[1]+")");
                        System.err.println("        face "+iFace);
                        System.err.println("        cubie "+iCubie);
                    }
                    else
                    {
                        System.err.println("    Hit nothin'.");
                    }
                }

                if (isRotate)
                {
                    boolean allowArbitraryElements = me.isControlDown();
                    float nicePoint[] = GenericPipelineUtils.pickNicePointToRotateToCenter(
                                     me.getX(), me.getY(),
                                     allowArbitraryElements,
                                     perViewState.untwistedFrame, // XXX move that into here, it's view specific!
                                     model.genericPuzzleDescription);

                    if (nicePoint != null)
                    {
                        //
                        // Initiate a rotation
                        // that takes the nice point to the center
                        // (i.e. to the -W axis)
                        // 
                        // XXX do all this in float since there are now float methods in VecMath

                        double viewMat4dD[][] = new double[4][4];
                        double nicePointD[] = new double[4];
                        for (int i = 0; i < 4; ++i)
                        for (int j = 0; j < 4; ++j)
                            viewMat4dD[i][j] = (double)perViewState.viewMat4d[i][j];
                        for (int i = 0; i < 4; ++i)
                            nicePointD[i] = (double)nicePoint[i];

                        double nicePointOnScreen[] = VecMath.vxm(nicePointD, viewMat4dD);
                        VecMath.normalize(nicePointOnScreen, nicePointOnScreen); // if it's not already
                        float minusWAxis[] = {0,0,0,-1};
                        perViewState.rotationFrom = VecMath.doubleToFloat(nicePointOnScreen);
                        perViewState.rotationTo = minusWAxis;

                        if (model.genericPuzzleDescription.nDims() < 4)
                        {
                            //
                            // In less-than-4d puzzles,
                            // if the projection is flattened
                            // and they clicked on the center sticker,
                            // un-flatten it in such a direction
                            // that it appears that the user is pushing
                            // on the polygon they clicked on.
                            //
                            if (VecMath.distsqrd(perViewState.rotationFrom, perViewState.rotationTo) <= 1e-4*1e-4)
                            {
                                float polyAndStickerAndFaceCenter[][] = GenericPipelineUtils.pickPolyAndStickerAndFaceCenter(
                                     me.getX(), me.getY(),
                                     perViewState.untwistedFrame,
                                     model.genericPuzzleDescription);
                                Assert(polyAndStickerAndFaceCenter != null); // hit once, should hit again
                                float polyCenter[] = polyAndStickerAndFaceCenter[0];

                                // Only interested in the w component
                                // (and the z component if the puzzle is 2d).
                                // So zero out the first nDims dimensions...
                                polyCenter = VecMath.copyvec(polyCenter);
                                VecMath.zerovec(model.genericPuzzleDescription.nDims(),
                                                polyCenter);
                                if (VecMath.normsqrd(polyCenter) < 1e-4*1e-4)
                                {
                                    // They clicked on an *edge* of a sticker
                                    // that's already in the center of the screen--
                                    // we don't know which way to push.
                                    // nothing sensible we can do here, just ignore it.
                                    // (Actually, treat it the same as we treat clicking on a sticker
                                    // that's already in the center in a 4d puzzle, i.e. nothing).
                                    //System.out.println("NICE TRY!");
                                    return;
                                }
                                float polyCenterOnScreen[] = VecMath.vxm(polyCenter, perViewState.viewMat4d);
                                perViewState.rotationFrom = polyCenterOnScreen;
                                VecMath.normalize(perViewState.rotationFrom, perViewState.rotationFrom);
                            }
                        }

                        double totalRotationAngle = VecMath.angleBetweenUnitVectors(
                                            perViewState.rotationFrom,
                                            perViewState.rotationTo);

                        perViewState.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * perViewState.nFrames90);
                        if (perViewState.nRotation == 0) perViewState.nRotation = 1;
                        // XXX ARGH! we'd like the speed to vary as the user changes the slider,
                        // XXX but the above essentially locks in the speed for this rotation
                        perViewState.iRotation = 0; // we are iRotation frames into nRotation
                        view.repaint(); // start it going

                        if (perViewState.iRotation == perViewState.nRotation)
                        {
                            // Already in the center
                            System.err.println("Can't rotate that.\n");
                        }
                    }
                    else
                        System.out.println("missed");
                } // isRotate
                else // !isRotate, it's a twist
                {
                    int iGrip = GenericPipelineUtils.pickGrip(
                                    me.getX(), me.getY(),
                                    perViewState.untwistedFrame,
                                    model.genericPuzzleDescription);
                    if (iGrip != -1)
                    {
                        int order = model.genericPuzzleDescription.getGripSymmetryOrders()[iGrip];

                        if (false)
                        {
                            System.err.println("    Grip "+iGrip+"");
                            System.err.println("        order "+order);
                        }

                        if (order <= 0)
                        {
                            System.err.println("Can't twist that.\n");
                            return;
                        }

                        int dir = (isLeftMouseButton(me) || isMiddleMouseButton(me)) ? 1 : -1; // ccw is 1, cw is -1

                        if(me.isShiftDown())
                        {
                            dir *= 2; // double power-twist!
                            for (int i = 0; i < perViewState.nShiftsDown-1; ++i)
                                dir *= 2; // quadruple mega-power-twist!
                        }
                        model.initiateTwist(iGrip, dir, perViewState.slicemask);
                        // do NOT call repaint here!
                        // the model will notify us when
                        // we need to repaint, when it's our turn.

                        /*
                        double totalRotationAngle = 2*Math.PI/order*Math.abs(dir);
                        genericGlue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * perViewState.nFrames90); // XXX unscientific rounding
                        if (genericGlue.nTwist == 0) genericGlue.nTwist = 1;
                        genericGlue.iTwist = 0;
                        genericGlue.iTwistGrip = iGrip;
                        genericGlue.twistDir = dir;
                        genericGlue.twistSliceMask = slicemask;

                        //
                        // Stick it in the undo queue now, instead
                        // of at the end of the animation.
                        // It's easier for us to do it
                        // here than for the guy at the end to do it,
                        // because he would have to decide to do it or not
                        // depending on whether it was an undo
                        // (and there's currently no mechanism for him
                        // to know that).
                        //
                        // XXX seems like it would be better policy
                        // XXX to add to the undo queue at the same time
                        // XXX as the move is applied to the state...
                        // XXX so we should probably apply the move
                        // XXX to the state here too, which means
                        // XXX we have to modify what gets passed
                        // XXX to the getFrame functions (i.e.
                        // XXX tell it the twist is going towards
                        // XXX the current state array instead of
                        // XXX away from it)
                        // 
                        genericGlue.undoq.setSize(genericGlue.undoPartSize); // clear redo part
                        genericGlue.undoq.addElement(new GenericGlue.HistoryNode(
                                                            genericGlue.iTwistGrip,
                                                            genericGlue.twistDir,
                                                            genericGlue.twistSliceMask));
                        genericGlue.undoPartSize++;
                        */
                    }
                    else
                        System.out.println("missed");
                }
            } // mouseClicked
            public void mousePressed(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mousePressed on a "+view.getClass().getSuperclass().getName());
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down
                perViewState.lastDrag = new int[]{me.getX(), me.getY()};
                perViewState.lastDragTime = me.getWhen();
                if (perViewState.spinDragRequiresCtrl == me.isControlDown())
                {
                    perViewState.spinDelta = null;
                    perViewState.dragDelta = null;
                }
            }
            public void mouseReleased(MouseEvent me)
            {
                long timedelta = me.getWhen() - perViewState.lastDragTime;
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mouseReleased on a "+view.getClass().getSuperclass().getName()+", time = "+me.getWhen()+", timedelta = "+timedelta);
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down
                perViewState.lastDrag = null;
                perViewState.lastDragTime = -1L;
                if (perViewState.spinDragRequiresCtrl == me.isControlDown())
                {
                    if (timedelta == 0)
                    {
                        // Released at same time as previous drag-- lift off!
                        // What was the drag delta now becomes the spin delta.
                        perViewState.spinDelta = perViewState.dragDelta;
                        // We don't really need a repaint here
                        // since we just saw a drag at the same time
                        // and it did a repaint...
                        // however we do know we want to repaint
                        // and it doesn't hurt.
                        view.repaint();
                    }
                    else
                    {
                        // Failure to lift off.
                        perViewState.spinDelta = null;
                        view.repaint(); // so it can use higher quality paint
                    }
                    perViewState.dragDelta = null; // no longer dragging
                }
            }
            public void mouseEntered(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 4) System.out.println("mouseExited on a "+view.getClass().getSuperclass().getName());
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down
            }
            public void mouseExited(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 4) System.out.println("mouseExited on a "+view.getClass().getSuperclass().getName());
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down
            }
        });
        // watch for dragging gestures to rotate the 3D view
        view.addMouseMotionListener(perViewState.mouseMotionListener = new MouseMotionListener() {
            public void mouseDragged(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mouseDragged on a "+view.getClass().getSuperclass().getName()+", time = "+me.getWhen());
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down
                if (perViewState.lastDrag == null)
                    return;
                int thisDrag[] = {me.getX(), me.getY()};
                if (perViewState.spinDragRequiresCtrl == me.isControlDown())
                {
                    int pixelsMovedSqrd = VecMath.distsqrd(perViewState.lastDrag, thisDrag);
                    if (pixelsMovedSqrd > 0) // do nothing if ended where we started
                    {
                        int dragDir[] = VecMath.vmv(thisDrag, perViewState.lastDrag);
                        dragDir[1] *= -1; // in java, y is down, so invert it
                        float axis[] = {-dragDir[1],dragDir[0],0.f};
                        float radians = (float)Math.sqrt(pixelsMovedSqrd) / 300f;
                        perViewState.dragDelta = VecMath.makeRowRotMat(radians, new float[][]{axis});
                        if (perViewState.restrictRoll)
                            perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, zeroOutRollOnSpinDelta(perViewState, perViewState.dragDelta)); // XXX lame, fix the prototype so it can be static maybe
                        else
                            perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, perViewState.dragDelta);
                        if (pixelsMovedSqrd < 2*2)
                            perViewState.dragDelta = null;
                    }
                }
                perViewState.lastDrag = thisDrag;
                perViewState.lastDragTime = me.getWhen();
                view.repaint();
            }
            public void mouseMoved(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 5) System.out.println("        mouseMoved on a "+view.getClass().getSuperclass().getName());
                if (!me.isShiftDown())
                    perViewState.nShiftsDown = 0; // kill drift if we know no shifts down

                int pickedStickerPoly[] = GenericPipelineUtils.pick(
                                                me.getX(), me.getY(),
                                                perViewState.untwistedFrame,
                                                model.genericPuzzleDescription);
                int newSticker = pickedStickerPoly!=null ? pickedStickerPoly[0] : -1;
                int newPoly = pickedStickerPoly!=null ? pickedStickerPoly[1] : -1;
                if (newSticker != perViewState.iStickerUnderMouse
                 || newPoly != perViewState.iPolyUnderMouse)
                {
                    perViewState.iStickerUnderMouse = newSticker;
                    perViewState.iPolyUnderMouse = newPoly;
                    view.repaint();
                }
            }
        });

        perViewState.setRestrictRoll(model, view, perViewState.getRestrictRoll()); // initiates the zero roll animation if appropriate

    } // attachListeners

    /** Constructor. */
    public MC4DViewGuts()
    {
        //
        // Nothing!
        // We don't even take a model in the constructor;
        // the caller has to call setModel(model) instead (or leave it null).
        // This may seem like a pain but it makes it
        // so they are always thinking in terms of attaching and detaching,
        // and they won't have to go rooting through the API
        // looking for setModel later.
        //
    }


    // PAINT
    void paint(Component view, Graphics g)
    {
        PerViewState perViewState = (PerViewState)perViewStates.get(view);
        if (perViewState == null)
            throw new IllegalArgumentException("MC4DViewGuts.paint called on a "+view.getClass().getSuperclass().getName()+" that it's not attached to!?");

        if (perViewState.eventVerboseLevel >= 3) System.out.println("            painting on a "+view.getClass().getSuperclass().getName());

        // stole from MC4DView.updateViewFactors
        int
            W = view.getWidth(),
            H = view.getHeight(),
            min = W>H ? H : W;
        if(W*H == 0)
            return;
        float pixels2polySF = 1f / Math.min(W, H) / perViewState.scale;
        int xOff = ((W>H) ? (W-H)/2 : 0) + min/2;
        int yOff = ((H>W) ? (H-W)/2 : 0) + min/2;

        // XXX query whether animation is actually in progress
        model.advanceAnimation(perViewState.modelListener, perViewState.nFrames90);

        // XXX if model is animating, something...?
        MC4DModel.Twist twist = new MC4DModel.Twist(-1,-1,-1);
        int puzzleState[] = new int[model.genericPuzzleDescription.nStickers()];
        // XXX getAnimationState should reaturn a float
        float timeFractionOfWayThroughTwist = (float)model.getAnimationState(perViewState.modelListener,
                                                                       puzzleState, // fills this
                                                                       twist);      // fills this
        float spaceFractionOfWayThroughTwist = perViewState.interp.func(timeFractionOfWayThroughTwist);


        if (perViewState.iRotation < perViewState.nRotation)
        {
            //
            // 4d rotation in progress
            //
            float incFrac = perViewState.interp.func((perViewState.iRotation+1)/(float)perViewState.nRotation)
                          - perViewState.interp.func(perViewState.iRotation/(float)perViewState.nRotation);
            float incmat[][] = com.donhatchsw.util.VecMath.makeRowRotMatThatSlerps(perViewState.rotationFrom, perViewState.rotationTo, incFrac);
            float newViewMat4d[][] = com.donhatchsw.util.VecMath.mxm(perViewState.viewMat4d, incmat);
            com.donhatchsw.util.VecMath.gramschmidt(newViewMat4d, newViewMat4d);
            com.donhatchsw.util.VecMath.copymat(perViewState.viewMat4d, newViewMat4d);
            //System.out.println("    "+perViewState.iRotation+"/"+perViewState.nRotation+" -> "+(perViewState.iRotation+1)+"/"+perViewState.nRotation+"");
            if (!perViewState.frozenForDebugging)
            {
                perViewState.iRotation++;
                view.repaint(); // make sure we keep drawing while there's more to do

                if (perViewState.iRotation == perViewState.nRotation
                 && perViewState.getRestrictRoll())
                {
                    // If we are finishing a rotate-to-center
                    // and we are in restricted roll mode,
                    // what we were using as a twirl axis is probably not
                    // very good any more.  Choose another.
                    if (perViewState.rotationTo[3] < -.9999) // i.e. if it was a rot to center
                    {
                        perViewState.setRestrictRoll(model, view, perViewState.getRestrictRoll()); // even though it is already XXX make this cleaner & clearer if possible
                    }
                }
            }
        }

        if (perViewState.spinDelta != null) // note, the old applet had an additional test "and not dragging" but we don't need it because spinDelta is never set during dragging now, dragDelta is instead
        {
            if (perViewState.restrictRoll)
                perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, zeroOutRollOnSpinDelta(perViewState, perViewState.spinDelta));
            else
                perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, perViewState.spinDelta);
            view.repaint();
        }

        GenericPipelineUtils.Frame frameToDrawInto = perViewState.untwistedFrame;

        // XXX these numbers need to go up into the perViewState structure
        // XXX and FIX the FUDGE dag nab it

        // old params... but I don't think it was doing it right
        //float[] groundNormal = showShadows ? new float[] {0,1,.1f} : null;
        //float groundOffset = -1.f;

        // XXX why is this a bit diff from old?  well I don't think it was being done right for one thing
        float[] groundNormal = perViewState.showShadows ? new float[] {0,1,.05f} : null;
        float groundOffset = -1.f;

        // XXX I don't seem to be quite the same as the original... unless I correct it here
        float scaleFudge4d = 1.f;
        float scaleFudge3d = 1.f;
        float scaleFudge2d = 4.7f;

        // XXX probably doing this more than necessary... when it's a rest frame that hasn't changed
        GenericPipelineUtils.computeFrame(
            frameToDrawInto,

            model.genericPuzzleDescription,
            perViewState.faceShrink,
            perViewState.stickerShrink,

            twist.grip,
            twist.dir,
            twist.slicemask,
            (float)spaceFractionOfWayThroughTwist,

            VecMath.mxs(perViewState.viewMat4d, scaleFudge4d),
            perViewState.eyeW,
            VecMath.mxm(
                VecMath.makeRowRotMat(3, 2, 1, (float)Math.PI/2), // XXX FUDGE that makes it nicer for the pentagonal prismprism... what do we need, a preferred viewing orientation for each puzzle as part of the model description?
                VecMath.mxs(perViewState.viewMat3d, scaleFudge3d)),
            perViewState.eyeZ,
            new float[][]{{scaleFudge2d*perViewState.scale/pixels2polySF, 0},
                          {0, -scaleFudge2d*perViewState.scale/pixels2polySF},
                          {(float)xOff, (float)yOff}},

            VecMath.normalize(perViewState.towardsSunVec),
            groundNormal,
            groundOffset,
            
            perViewState.useTopsort,
            perViewState.showPartialOrder);

        /*
        if (frozenForDebugging)
        {
            if (frozenPartialOrderForDebugging != null)
                glueFrameToDrawInto.partialOrder = frozenPartialOrderForDebugging;
            else
                frozenPartialOrderForDebugging = glueFrameToDrawInto.partialOrder;
        }
        */
        GenericPipelineUtils.paintFrame(
                frameToDrawInto,
                model.genericPuzzleDescription,
                model.genericPuzzleState,
                perViewState.showShadows,
                perViewState.ground,
                perViewState.faceRGB,
                perViewState.iStickerUnderMouse,
                perViewState.iPolyUnderMouse,
                perViewState.highlightByCubie,
                !perViewState.highlightByCubie && perViewState.highlightByGrip, // XXX mess, see if I can make this cleaner
                perViewState.outlineColor,
                g,

                perViewState.jitterRadius,
                perViewState.drawLabels,
                perViewState.showPartialOrder);
    } // paint


    //
    // Attempt to implement roll correction.
    // XXX in the process of shoehorning this in... params need to be cleaned up severely.
    //
        private static int findFaceCenterClosestToYZArc(float faceCenters[][],
                                                        float viewMat4d[/*4*/][/*4*/],
                                                        float viewMat3d[/*3*/][/*3*/],
                                                        float returnPointOnYZArc[/*>=3*/])
        {
            float viewMat[][] = com.donhatchsw.util.VecMath.mxm(viewMat4d, viewMat3d);
            float thisFaceCenterInWorldSpace[] = new float[4]; // scratch for loop
            float bestClosestPointOnPositiveYZSector[] = new float[4];
            float bestDistSqrd = Float.MAX_VALUE;
            int bestIFace = -1;
            for (int iFace = 0; iFace < faceCenters.length; ++iFace)
            {
                com.donhatchsw.util.VecMath.vxm(thisFaceCenterInWorldSpace, faceCenters[iFace], viewMat);
                // normalize to a unit vector in 4-space...
                com.donhatchsw.util.VecMath.normalize(thisFaceCenterInWorldSpace,
                                                      thisFaceCenterInWorldSpace);
                thisFaceCenterInWorldSpace[3] = 0.f;
                // Reject if the x,y,z part is too small, which means it was
                // too close to the W axis (i.e. it's probably the element
                // we are focused on, or its opposite)
                if (com.donhatchsw.util.VecMath.normsqrd(thisFaceCenterInWorldSpace) < 1e-2*1e-2)
                    continue;
                // normalize to a unit vector in 3-space...
                com.donhatchsw.util.VecMath.normalize(thisFaceCenterInWorldSpace,
                                                      thisFaceCenterInWorldSpace);
                float closestPointOnPositiveYZSector[/*4*/] = {
                    0.f,
                    Math.max(thisFaceCenterInWorldSpace[1], 0.f),
                    Math.max(thisFaceCenterInWorldSpace[2], 0.f),
                    0.f,
                };
                float thisDistSqrd = com.donhatchsw.util.VecMath.distsqrd(
                                                thisFaceCenterInWorldSpace,
                                                closestPointOnPositiveYZSector);
                if (thisDistSqrd < bestDistSqrd)
                {
                    bestDistSqrd = thisDistSqrd;
                    bestIFace = iFace;
                    com.donhatchsw.util.VecMath.copyvec(bestClosestPointOnPositiveYZSector,
                                                        closestPointOnPositiveYZSector);
                }
            }
            Assert(bestIFace != -1);
            com.donhatchsw.util.VecMath.normalize(bestClosestPointOnPositiveYZSector,
                                                  bestClosestPointOnPositiveYZSector);
            com.donhatchsw.util.VecMath.copyvec(3, returnPointOnYZArc,
                                                   bestClosestPointOnPositiveYZSector);
            return bestIFace;
        } // findFaceCenterClosestToYZArc

        public static void initiateZeroRoll(PerViewState perViewState,
                                            float faceCenters[][],
                                            float viewMat4d[][],
                                            float viewMat3d[][],
                                            float nFrames90,
                                            Component view)
        {
            // XXX FUDGE! get rid of this when I get rid of corresponding fudge in display
            {
                viewMat3d = com.donhatchsw.util.VecMath.mxm(
                    com.donhatchsw.util.VecMath.makeRowRotMat(3, 2, 1, (float)Math.PI/2), // XXX FUDGE that makes it nicer for the pentagonal prismprism... what do we need, a preferred viewing orientation for each puzzle as part of the model description?
                    viewMat3d);
            }
            // XXX bleah, should be able to multiply a 4x4 by a 3x3 but it crashes currently, so...
            {
                float paddedViewMat3d[][] = new float[4][4];
                com.donhatchsw.util.VecMath.copymat(paddedViewMat3d, viewMat3d);
                paddedViewMat3d[3][3] = 1.f;
                viewMat3d = paddedViewMat3d;
            }

            float pointOnYZArc[] = new float[4]; // zeros... and [3] is left zero
            int iFace = findFaceCenterClosestToYZArc(faceCenters,
                                                     viewMat4d,
                                                     viewMat3d,
                                                     pointOnYZArc);
            perViewState.rotationFrom = com.donhatchsw.util.VecMath.vxm(faceCenters[iFace], viewMat4d);
            perViewState.rotationFrom[3] = 0.f;
            com.donhatchsw.util.VecMath.normalize(perViewState.rotationFrom,
                                                  perViewState.rotationFrom);
            // pointOnYZArc is now in screen space...
            // to get the point we want to rotate to,
            // we undo the viewMat3d on it,
            // i.e. apply viewMat3d's transpose, i.e. its inverse,
            // i.e. multiply by it on the opposite side as usual
            perViewState.rotationTo = com.donhatchsw.util.VecMath.mxv(viewMat3d, pointOnYZArc);

            double totalRotationAngle = com.donhatchsw.util.VecMath.angleBetweenUnitVectors(
                                perViewState.rotationFrom,
                                perViewState.rotationTo);
            perViewState.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
            if (perViewState.nRotation == 0) perViewState.nRotation = 1;
            perViewState.iRotation = 0;

            // Remember the zero roll pole
            // for subsequent calls to zeroOutRollOnSpinDelta
            perViewState.zeroRollPoleAfterRot3d = pointOnYZArc;

            //System.out.println("this.rotationFrom = "+com.donhatchsw.util.VecMath.toString(this.rotationFrom));
            //System.out.println("this.rotationTo = "+com.donhatchsw.util.VecMath.toString(this.rotationTo));

            view.repaint();
        } // initiateZeroRoll

        // Uses the zeroRollPole from the most recent call
        // to initiatezeroRoll. // XXX should not
        public static float[][] zeroOutRollAndMaybeTiltOnSpinDelta(PerViewState perViewState,
                                                            float spindelta[][],
                                                            boolean zeroOutTiltToo)
        {
            double tiltDeltaAngle, twirlDeltaAngle;
            {
                // ASSUMPTION: spindelta is from a trackball drag,
                // which means its axis is somewhere in the xy plane
                // and can therefore be expressed as pure tilt and twirl.
                tiltDeltaAngle = Math.atan2(spindelta[1][2], spindelta[1][1]);
                twirlDeltaAngle = Math.atan2(spindelta[2][0], spindelta[2][2]);
                //System.out.println("  using mat: tiltDeltaAngle = "+tiltDeltaAngle);
                //System.out.println("  using mat: twirlDeltaAngle = "+twirlDeltaAngle);
            }

            if (zeroOutTiltToo)
                tiltDeltaAngle = 0.f; // before clamping-- we do let the clamp do a tilt if it wants

            Assert(perViewState.zeroRollPoleAfterRot3d != null); // initiateZeroRoll must have been called previously
            perViewState.zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.copyvec(3, perViewState.zeroRollPoleAfterRot3d); // XXX sigh... because vxm and other stuff freaks if I don't
            // Clamp tilt to [0..pi/2]...
            double currentTilt = Math.atan2(perViewState.zeroRollPoleAfterRot3d[2],
                                            perViewState.zeroRollPoleAfterRot3d[1]);
            //System.out.println("tiltDeltaAngle before = "+tiltDeltaAngle*180/Math.PI);
            if (tiltDeltaAngle > Math.PI/2 - currentTilt)
                tiltDeltaAngle = Math.PI/2 - currentTilt;
            if (tiltDeltaAngle < 0 - currentTilt)
                tiltDeltaAngle = 0 - currentTilt;
            //System.out.println("tiltDeltaAngle after = "+tiltDeltaAngle*180/Math.PI);

            double twirlDelta[][] = com.donhatchsw.util.VecMath.makeRowRotMat(twirlDeltaAngle, new double[][]{com.donhatchsw.util.VecMath.floatToDouble(perViewState.zeroRollPoleAfterRot3d)});
            double tiltDelta[][] = com.donhatchsw.util.VecMath.makeRowRotMat(tiltDeltaAngle, new double[][]{{1,0,0}});
            float adjustedSpinDelta[][] = com.donhatchsw.util.VecMath.doubleToFloat(com.donhatchsw.util.VecMath.mxm(twirlDelta, tiltDelta));

            // Gram-schmidt so we don't drift to non-orthogonal
            // XXX wasn't there a nicer more symmetric way of doing this?
            com.donhatchsw.util.VecMath.gramschmidt(adjustedSpinDelta,
                                                    adjustedSpinDelta);

            // need to apply it to the pole...
            perViewState.zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.vxm(perViewState.zeroRollPoleAfterRot3d,
                                                                     adjustedSpinDelta);
            return adjustedSpinDelta;
        } // zeroOutRollAndMaybeTiltOnSpinDelta

        // when dragging, we allow tilt changes
        public static float[][] zeroOutRollOnSpinDelta(PerViewState perViewState,
                                                       float spindelta[][])
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(perViewState, spindelta, false);
        }
        // when autospinning, we don't allow tilt changes,
        // or it would just drift to the min or max tilt, which looks dumb
        public static float[][] zeroOutRollAndTiltOnSpinDelta(PerViewState perViewState,
                                                              float spindelta[][])
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(perViewState, spindelta, true);
        }


    /*
     * Shamelessly copied from someone who shamelessly copied it from SwingUtilities.java since that is in JDK 1.3 and we'd like to keep this to 1.2 and below.
     */
    public static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return ((anEvent.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK);
    }
    public static boolean isLeftMouseButton(MouseEvent anEvent) {
         return ((anEvent.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    }




    //
    // Make a modern viewer based on a JPanel.
    //
    public static void makeExampleModernViewer(final MC4DModel model,
                                               final int x, final int y,
                                               final int w, final int h)
    {
        final MC4DViewGuts guts = new MC4DViewGuts();
        guts.setModel(model);
        final JPanel myPanel = new JPanel() {
            public void paintComponent(Graphics g)
            {
                g.setColor(new Color(20,170,235)); // sky
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(20, 130, 20));
                g.fillRect(0, getHeight()*6/9, getWidth(), getHeight()); // ground
                guts.paint(this, g);

                g.setColor(Color.white);
                g.drawString("ctrl-n for another modern view", 10, getHeight()-10);
            }
            // So we can type immediately in it
            public boolean isFocusTraversable()
            {
                return true;
            }
        };
        guts.attachListeners(myPanel, false);
        //myPanel.setPreferredSize(new java.awt.Dimension(w,h)); // set size bottom up


        JFrame jframe = new JFrame("Spiffy new world");

        jframe.setForeground(java.awt.Color.white);
        jframe.setBackground(java.awt.Color.black);

        jframe.setContentPane(myPanel);
        jframe.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent event)
            {
                System.out.println("Chow!");
                guts.detachListeners(myPanel);
                if (model.nListeners() == 0)
                    System.exit(0); // asinine way of doing things
            }
        });

        jframe.pack();
        jframe.setSize(w,h); // set size top down
        jframe.setLocation(x,y);
        jframe.setVisible(true);
        myPanel.requestFocus(); // seems to be needed initially, if running in <=1.3 on linux, anyway.  weird!

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        myPanel.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke)
            {
                char c = ke.getKeyChar();
                if (c == 'N'-'A'+1) // ctrl-N
                    if (ke.isShiftDown())
                        makeExampleAncientViewer(model,x+20+w,y+20,w,h,false); // ctrl-shift-N
                    else
                        makeExampleModernViewer(model,x+20,y+20,w,h);  // ctrl-n
            }
        });
    } // makeExampleModernViewer


    //
    // Make an ancient viewer based on a canvas.
    //
    public static void makeExampleAncientViewer(final MC4DModel model,
                                                final int x, final int y,
                                                final int w, final int h,
                                                final boolean doDoubleBuffer)
    {
        final MC4DViewGuts guts = new MC4DViewGuts();
        guts.setModel(model);
        final Canvas myCanvas = new Canvas() {
            private Image backBuffer = null;
            int bbw=0, bbh=0;
            public void update(Graphics g) { paint(g); } // don't flash
            public void paint(Graphics frontBufferGraphics)
            {
                if (doDoubleBuffer)
                {
                    if (backBuffer == null
                     || getWidth() != bbw
                     || getHeight() != bbh)
                    {
                        backBuffer = this.createImage(bbw=getWidth(), bbh=getHeight());
                        System.out.println("created back buffer of size "+bbw+"x"+bbh+"");
                    }
                }
                else
                    backBuffer = null;
                Graphics g = backBuffer != null ? backBuffer.getGraphics() : frontBufferGraphics;

                g.setColor(new Color(20,170,235)); // sky
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(20, 130, 20)); // ground
                g.fillRect(0, getHeight()*6/9, getWidth(), getHeight());
                guts.paint(this, g);

                g.setColor(Color.white);
                g.drawString("ctrl-n for another ancient view", 10, getHeight()-10);

                if (backBuffer != null)
                    frontBufferGraphics.drawImage(backBuffer, 0, 0, this);
            }
            // So we can type immediately in it
            public boolean isFocusTraversable()
            {
                return true;
            }
        };
        guts.attachListeners(myCanvas, true);
        //myCanvas.setSize(new java.awt.Dimension(w,h)); // set size bottom up


        final Frame frame = new Frame("Sucky old world") { 
            public boolean handleEvent(java.awt.Event event)
            {
                switch(event.id)
                {
                    case java.awt.Event.WINDOW_DESTROY:
                        System.out.println("bye!");
                        // Empirically, either of the following
                        // cause the app to exit-- do both to be safe!
                        // (XXX I've heard rumors that just doing dispose()
                        //  messes up the debugger)
                        // (XXX but doing exit is evil)
                        dispose(); // hide() doesn't delete the windows
                        guts.detachListeners(myCanvas);
                        if (model.nListeners() == 0)
                            System.exit(0); // asinine way of doing things
                        return true;
                }
                return super.handleEvent(event);
            }
        };
        // The above handleEvent no longer seems to work as of java 1.5.
        // So we have to use a listener.
        // XXX not sure how far back this exists, we'll need to use reflection to get it
        {
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent we) {
                    System.out.println("ciao!");
                    frame.dispose(); // hide() doesn't delete the windows
                    guts.detachListeners(myCanvas);
                    if (model.nListeners() == 0)
                        System.exit(0); // asinine way of doing things
                }
            });
        }

        frame.add(myCanvas);
        frame.pack();
        frame.resize(w, h); // set size top down
        frame.move(x,y);
        frame.show();

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        myCanvas.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke)
            {
                char c = ke.getKeyChar();
                if (c == 'N'-'A'+1) // ctrl-N
                    if (ke.isShiftDown())
                        makeExampleModernViewer(model,x+20-w,y+20,w,h); // ctrl-shift-N
                    else
                        makeExampleAncientViewer(model,x+20,y+20,w,h,doDoubleBuffer);  // ctrl-n
            }
        });
    } // makeExampleAncientViewer



    public static void main(String args[])
    {
        if (args.length != 1)
        {
            System.err.println("Usage: MC4DViewGuts \"<puzzleDescription>\"");
            System.err.println("Example: MC4DViewGuts \"{4,3,3} 3\"");
            System.exit(1);
        }

        String puzzleDescription = args[0];

        MC4DModel model = new MC4DModel(puzzleDescription);

        makeExampleModernViewer(model, 50,50, 300,300);

        boolean doDoubleBuffer = true; // make it even more sucky than necessary
        makeExampleAncientViewer(model, 350,50, 300,300, doDoubleBuffer);

    } // main

} // MC4DViewGuts



