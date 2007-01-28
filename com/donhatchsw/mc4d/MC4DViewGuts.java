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
* so we just let the caller do that and let it call us from inside its paint().
*
* For example:
* <pre>
*       class ModernMC4DView
*           extends JPanel
*       {
*           public MC4DViewGuts guts; // has-a, not is-a
*
*           public ModernMC4DView()
*           {
*               guts = new MC4DViewGuts(this); // adds listeners to this
*           }
*           public void paintComponent(java.awt.Graphics g)
*           {
*               g.fillRect(0,0,getWidth(),getHeight());
*               guts.paint(this, g);
*           }
*       }
*
*       class AncientMC4DView
*           extends java.awt.Canvas
*       {
*           public MC4DViewGuts guts; // has-a, not is-a
*
*           public AncientMC4DView()
*           {
*               guts = new MC4DViewGuts(this); // adds listeners to this
*           }
*           public void paint(java.awt.Graphics g)
*           {
*               g.fillRect(0,0,getWidth(),getHeight());
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
import com.donhatchsw.util.Listenable;

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
    // shared by all views.  We do NOT own this.
    // Note, we need to be careful to be robust
    // in the case that the model is swapped with a different model--
    // e.g. if we're storing the index
    // of the current sticker, that index can suddenly become out of bounds.
    //
        public MC4DModel model = null;

    //
    // Viewing parameters, specific to this particular view.
    // These could be persistent (saved to a prefs file, etc.).
    //
    public static class ViewParams
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
        public Listenable.Int eventVerboseLevel = new Listenable.Int(0, 5, 0);


        //
        // Geometric / transform appearance params
        //
        public Listenable.Float faceShrink4d = new Listenable.Float(0.f, 1.f, .4f);
        public Listenable.Float stickerShrink4d = new Listenable.Float(0.f, 1.f, .5f);
        public Listenable.FloatMatrix viewMat4d = new Listenable.FloatMatrix(new float[][]{
            {1,0,0,0},
            {0,1,0,0},
            {0,0,1,0},
            {0,0,0,1},
        });
        public Listenable.Float eyeW = new Listenable.Float(0.f, 20.f, 5.2f);
        public Listenable.FloatMatrix viewMat3d = new Listenable.FloatMatrix(
            VecMath.mxm(VecMath.makeRowRotMat(3, 2,0, -42*(float)Math.PI/180), // twirl
                        VecMath.makeRowRotMat(3, 1,2,  30*(float)Math.PI/180))); // tilt
        public Listenable.Float faceShrink3d = new Listenable.Float(0.f, 1.f, .5f);
        public Listenable.Float stickerShrink3d = new Listenable.Float(0.f, 1.f, .5f);
        public Listenable.Float eyeZ = new Listenable.Float(0.f, 20.f, 10.f);
        public Listenable.Float viewScale2d = new Listenable.Float(0.f, 10.f, 1.f);
        public Listenable.Float stickersShrinkTowardsFaceBoundaries = new Listenable.Float(0.f, 1.f, 0.f);
        public Listenable.FloatVector towardsSunVec = new Listenable.FloatVector(new float[]{.82f, 1.55f, 3.3f});

        public Listenable.Color faceColors[] = new Listenable.Color[2640]; // num faces in omnitruncated 120-cell
        {
            System.out.println("Generating default colors...");
            for (int iFace = 0; iFace < faceColors.length; ++iFace)
            {
                faceColors[iFace] = new Listenable.Color(autoGenerateColor(iFace));
            }
            System.out.println("done.");
        }
        // XXX need to put in a general color scheme
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



        //public Listenable.Double nFrames90(15);
        public Listenable.Double nFrames90 = new Listenable.Double(0., 100., 30.);

        //public Listenable.Double criticalDampingFraction = new Listenable.Double(0., 1., 1.); // 1 means critically damped, less than 1 gives it a bit of bounce, more than 1 just makes it slower
        public Listenable.Double bounce = new Listenable.Double(0., 1., 1.); // 1 minus critical damping frac-- probably more comprehensible to the user

        public InterpFunc interp = sine_interp;
        //InterpFunc interp = linear_interp;
        public Listenable.Boolean requireCtrlTo3dRotate = new Listenable.Boolean(false);
        public Listenable.Boolean restrictRoll = new Listenable.Boolean(false);
            public boolean getRestrictRoll() { return restrictRoll.get(); }
            public void setRestrictRoll(MC4DModel model, Component viewComponent, ViewState viewState, boolean newRestrictRoll) // XXX shouldn't take model as a param I don't think, revisit this
            {
                // Do this even if it looks like we were already
                // restricting roll, in case we are initializing
                // a new model or something XXX revisit whether this is the cleanest way to do this
                if (newRestrictRoll && model != null)
                    initiateZeroRoll(viewState,
                                     model.genericPuzzleDescription.getFaceCentersAtRest(),
                                     viewMat4d.get(),
                                     viewMat3d.get(),
                                     nFrames90.get(),
                                     viewComponent);
                restrictRoll.set(newRestrictRoll);
            }
        public Listenable.Boolean stopBetweenMoves = new Listenable.Boolean(true);

        //
        // Ornamentational appearance params
        //
        public Listenable.Boolean highlightByCubie = new Listenable.Boolean(false);
        public Listenable.Boolean highlightByGrip = new Listenable.Boolean(true); // if possible; it gets turned into highlight by sticker automatically if there is no grip info, which there isn't unless it's a 2x2x2x2 currently.  XXX get rid of this?
        public Listenable.Boolean showShadows = new Listenable.Boolean(true);
        public Listenable.Boolean antialiasWhenStill = new Listenable.Boolean(true);
        public Listenable.Boolean drawNonShrunkFaceOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawShrunkFaceOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawNonShrunkStickerOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawShrunkStickerOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawGround = new Listenable.Boolean(true);

        public Listenable.Color shrunkFaceOutlineColor = new Listenable.Color(Color.black);
        public Listenable.Color nonShrunkFaceOutlineColor = new Listenable.Color(Color.black);
        public Listenable.Color shrunkStickerOutlineColor = new Listenable.Color(Color.black);
        public Listenable.Color nonShrunkStickerOutlineColor = new Listenable.Color(Color.black);
        public Listenable.Color groundColor = new Listenable.Color(new Color(20, 130, 20));
        public Listenable.Color backgroundColor = new Listenable.Color(new Color(20, 170, 235));


        //
        // Debugging params.
        // Most of these are settable using secret ctrl-alt key combinations.
        //
        public Listenable.Boolean useTopsort = new Listenable.Boolean(true);
        public Listenable.Int jitterRadius = new Listenable.Int(0,9,0);
        public Listenable.Boolean drawLabels = new Listenable.Boolean(false);
        public Listenable.Boolean showPartialOrder = new Listenable.Boolean(false);
        public Listenable.Boolean frozenForDebugging = new Listenable.Boolean(false); // XXX is this state rather than params?  never want to save it out, do we? but we do set it using the ctrl-alt keys... hmm
            public int frozenPartialOrderForDebugging[][] = null; // XXX this is state you dingbat
    } // class ViewParams

    //
    // Volatile view state.
    // It probably makes no sense to save any of these to a prefs file.
    //
    public static class ViewState
    {
        //
        // Mouse and keyboard state...
        //
        private int slicemask = 0; // bitmask representing which number keys are down
        private int nShiftsDown = 0; // keep track of whether both shift keys are being held down.  This isn't completely reliable (e.g. when mouse exits window while shift is down) but we always set it to 0 when a non-shifted mouse event occurs so it's not too dangerous.
        private int lastDrag[] = null; // non-null == dragging.  always tracks mouse drag regardless of ctrl
        private long time0 = System.currentTimeMillis();
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
            { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something.  really need to stop doing this though, since it messes up double-clicking if something obscures the sticker during the click

    } // class ViewState


    //
    // The controller and view components are usually the same,
    // but they don't logically have to be.
    // The controller component is what we listen to for key and mouse events.
    // The view component is what we call repaint() on when anything changes
    // (and what will probably call our paint() method, although we don't
    // require it).
    //
    private Component controllerComponent = null;
    private Component viewComponent = null;
    public ViewParams viewParams = new ViewParams(); // XXX not sure if it should be public or what
    private ViewState viewState = new ViewState();
        {
            Listenable listenables[] = Listenable.allListenablesInObject(viewParams);
            for (int i = 0; i < listenables.length; ++i)
                listenables[i].addListener(new Listenable.Listener() {
                    public void valueChanged()
                    {
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                });
        }

    //
    // The listener I use to listen to the model
    // for changes (increments in animation which are
    // initiated by other views or by myself).
    // My response to a change is simply to call repaint on the view Component;
    // then when paint() is called it will query the animation progress
    // from the model (which pumps the animation).
    //
    MC4DModel.Listener modelListener = new MC4DModel.Listener() {
        public void movingNotify()
        {
            if (viewComponent != null)
                viewComponent.repaint();
        }
    };

    //
    // The listeners I use
    // to listen to the controller Component
    //
    private KeyListener keyListener;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;

    // To restore when I detach from this controller Component
    private com.donhatchsw.compat.ArrayList savedKeyListeners;
    private com.donhatchsw.compat.ArrayList savedMouseListeners;
    private com.donhatchsw.compat.ArrayList savedMouseMotionListeners;


    /** Detaches this view guts from its old model (if any), and attaches to the new one (if any). */
    public void setModel(MC4DModel newModel)
    {
        if (model != null)
        {
            // Disconnect our listener from the current model...
            model.removeListener(modelListener);
        }
        // Change the model...
        model = newModel;
        if (model != null)
        {
            // Connect all our listeners to the new model
            model.addListener(modelListener);
            viewParams.setRestrictRoll(model, viewComponent, viewState, viewParams.getRestrictRoll()); // initiates the zero roll animation if appropriate
        }
        // Goose the view component to make sure it repaints
        if (viewComponent != null)
            viewComponent.repaint();
    } // setModel


    /** Detaches this view guts from the old controller component (if any) and attaches to the new controller component (if any). */
    public void setControllerComponent(final Component newControllerComponent,
                                       boolean suppressExistingListeners) // XXX implement this!
    {
        if (controllerComponent != null)
        {
            controllerComponent.removeKeyListener(keyListener);
            controllerComponent.removeMouseListener(mouseListener);
            controllerComponent.removeMouseMotionListener(mouseMotionListener);
            // XXX restore suppressed listeners if any
        }
        controllerComponent = newControllerComponent;
        if (controllerComponent != null)
        {
            if (suppressExistingListeners)
            {
                // XXX get the listeners into the saved listener lists
                // XXX and remove them from the component
            }

            controllerComponent.addKeyListener(this.keyListener = new KeyListener() {
                public void keyPressed(KeyEvent ke)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("keyPressed "+ke);
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("    isShiftDown = "+ke.isShiftDown());
                    int keyCode = ke.getKeyCode();
                    int numkey = keyCode - KeyEvent.VK_0;
                    if(1 <= numkey && numkey <= 9)
                        viewState.slicemask |= 1<<(numkey-1); // turn on the specified bit
                    if (keyCode == KeyEvent.VK_SHIFT)
                    {
                        viewState.nShiftsDown++;
                        if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("    nShiftsDown = "+viewState.nShiftsDown);
                    }
                } // keyPressed
                public void keyReleased(KeyEvent ke)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("keyReleased "+ke);
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("    isShiftDown = "+ke.isShiftDown());
                    int keyCode = ke.getKeyCode();
                    int numkey = keyCode - KeyEvent.VK_0;
                    if(1 <= numkey && numkey <= 9)
                        viewState.slicemask &= ~(1<<(numkey-1)); // turn off the specified bit
                    if (keyCode == KeyEvent.VK_SHIFT)
                    {
                        // If nShiftsDown becomes >= 2,
                        // don't decrease it when we get the shift key release.
                        // This lets the user hold down one shift key
                        // and pump up the power with the other!
                        // This is not too dangerous because it always
                        // deflates to zero as any mouse event occurs
                        // with no shift key down.
                        if (viewState.nShiftsDown == 1)
                            viewState.nShiftsDown = 0;
                        if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("    nShiftsDown = "+viewState.nShiftsDown);
                    }
                } // keyReleased
                public void keyTyped(KeyEvent ke)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("keyTyped");
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
                        System.out.println("useTopsort "+viewParams.useTopsort.get()+" -> "+!viewParams.useTopsort.get()+"");
                        viewParams.useTopsort.set(!viewParams.useTopsort.get());
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                    else if (c == 'j'-'a'+1
                     && ke.isAltDown()) // ctrl-alt-j
                    {
                        int jitterRadius = viewParams.jitterRadius.get();
                        jitterRadius++;
                        if (jitterRadius > viewParams.jitterRadius.max())
                            jitterRadius = viewParams.jitterRadius.min();
                        System.out.println("jitterRadius "+viewParams.jitterRadius.get()+" -> "+jitterRadius+"");
                        viewParams.jitterRadius.set(jitterRadius);
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                    else if (c == 'l'-'a'+1
                     && ke.isAltDown()) // ctrl-alt-l
                    {
                        System.out.println("drawLabels "+viewParams.drawLabels.get()+" -> "+!viewParams.drawLabels.get()+"");
                        viewParams.drawLabels.set(!viewParams.drawLabels.get());
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                    else if (c == 'p'-'a'+1
                     && ke.isAltDown()) // ctrl-alt-p
                    {
                        System.out.println("showPartialOrder "+viewParams.showPartialOrder.get()+" -> "+!viewParams.showPartialOrder.get()+"");
                        viewParams.showPartialOrder.set(!viewParams.showPartialOrder.get());
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                    else if (c == ' ' && ke.isControlDown()
                     && ke.isAltDown()) // ctrl-alt-space
                    {
                        System.out.println("frozenForDebugging "+viewParams.frozenForDebugging.get()+" -> "+!viewParams.frozenForDebugging.get()+"");
                        viewParams.frozenForDebugging.set(!viewParams.frozenForDebugging.get());
                        viewParams.frozenPartialOrderForDebugging = null;
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                    else if (c == 'v'-'a'+1
                     && ke.isAltDown()) // ctrl-alt-v -- cycle eventVerboseLevel
                    {
                        System.out.print("eventVerboseLevel "+viewParams.eventVerboseLevel.get());
                        viewParams.eventVerboseLevel.set((viewParams.eventVerboseLevel.get()+1) % (viewParams.eventVerboseLevel.max()-viewParams.eventVerboseLevel.min()) + viewParams.eventVerboseLevel.min());
                        System.out.println(" -> "+viewParams.eventVerboseLevel.get());
                        ke.consume(); // XXX does this make it so subsequent handlers don't see it? dammit, no it doesn't. damn damn damn fuck fuck fuck. what does it mean?
                    }
                    else if (ke.isControlDown()
                          && ke.isAltDown())
                    {
                        System.out.println("Unrecognized key sequence ctrl-alt-"+(char)(c|64)+"");
                    }
                } // keyTyped
            }); // key listener
            controllerComponent.addMouseListener(this.mouseListener = new MouseListener() {
                public void mouseClicked(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mouseClicked on a "+controllerComponent.getClass().getSuperclass().getName());
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down

                    if (viewParams.requireCtrlTo3dRotate.get())
                    {
                        if (me.isControlDown())
                            return; // restricted mode, so ctrled mouse only affects the spin... which is implemented in the press,drag,release callbacks, not this one
                        if (viewComponent != null)
                            viewComponent.repaint(); // so it keeps spinning if it was spinning
                    }

                    boolean isRotate = isMiddleMouseButton(me);
                    if (false) // make this true to debug the pick
                    {
                        int hit[] = GenericPipelineUtils.pick(me.getX(), me.getY(),
                                                              viewState.untwistedFrame,
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
                                         viewState.untwistedFrame, // XXX move that into here, it's view specific!
                                         model.genericPuzzleDescription);

                        if (nicePoint != null)
                        {
                            //
                            // Initiate a rotation
                            // that takes the nice point to the center
                            // (i.e. to the -W axis)
                            // 
                            // XXX do all this in float since there are now float methods in VecMath

                            float viewMat4dF[][] = viewParams.viewMat4d.get();
                            double viewMat4dD[][] = new double[4][4];
                            double nicePointD[] = new double[4];
                            for (int i = 0; i < 4; ++i)
                            for (int j = 0; j < 4; ++j)
                                viewMat4dD[i][j] = (double)viewMat4dF[i][j];
                            for (int i = 0; i < 4; ++i)
                                nicePointD[i] = (double)nicePoint[i];

                            double nicePointOnScreen[] = VecMath.vxm(nicePointD, viewMat4dD);
                            VecMath.normalize(nicePointOnScreen, nicePointOnScreen); // if it's not already
                            float minusWAxis[] = {0,0,0,-1};
                            viewState.rotationFrom = VecMath.doubleToFloat(nicePointOnScreen);
                            viewState.rotationTo = minusWAxis;

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
                                if (VecMath.distsqrd(viewState.rotationFrom, viewState.rotationTo) <= 1e-4*1e-4)
                                {
                                    float polyAndStickerAndFaceCenter[][] = GenericPipelineUtils.pickPolyAndStickerAndFaceCenter(
                                         me.getX(), me.getY(),
                                         viewState.untwistedFrame,
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
                                    float polyCenterOnScreen[] = VecMath.vxm(polyCenter, viewParams.viewMat4d.get());
                                    viewState.rotationFrom = polyCenterOnScreen;
                                    VecMath.normalize(viewState.rotationFrom, viewState.rotationFrom);
                                }
                            }

                            double totalRotationAngle = VecMath.angleBetweenUnitVectors(
                                                viewState.rotationFrom,
                                                viewState.rotationTo);

                            viewState.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * viewParams.nFrames90.get());
                            if (viewState.nRotation == 0) viewState.nRotation = 1;
                            // XXX ARGH! we'd like the speed to vary as the user changes the slider,
                            // XXX but the above essentially locks in the speed for this rotation
                            viewState.iRotation = 0; // we are iRotation frames into nRotation
                            if (viewComponent != null)
                                viewComponent.repaint(); // start it going

                            if (viewState.iRotation == viewState.nRotation)
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
                                        viewState.untwistedFrame,
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
                                for (int i = 0; i < viewState.nShiftsDown-1; ++i)
                                    dir *= 2; // quadruple mega-power-twist!
                            }
                            model.initiateTwist(iGrip, dir, viewState.slicemask);
                            // do NOT call repaint here!
                            // the model will notify us when
                            // we need to repaint, when it's our turn.
                        }
                        else
                            System.out.println("missed");
                    }
                } // mouseClicked
                public void mousePressed(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mousePressed on a "+controllerComponent.getClass().getSuperclass().getName());
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down
                    viewState.lastDrag = new int[]{me.getX(), me.getY()};
                    viewState.lastDragTime = me.getWhen();
                    if (viewParams.requireCtrlTo3dRotate.get() == me.isControlDown())
                    {
                        viewState.spinDelta = null;
                        viewState.dragDelta = null;
                    }
                } // mousePressed
                public void mouseReleased(MouseEvent me)
                {
                    long timedelta = me.getWhen() - viewState.lastDragTime;
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mouseReleased on a "+controllerComponent.getClass().getSuperclass().getName()+", time = "+(me.getWhen()-viewState.time0)/1000.+", timedelta = "+timedelta);
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down
                    viewState.lastDrag = null;
                    viewState.lastDragTime = -1L;
                    if (viewParams.requireCtrlTo3dRotate.get() == me.isControlDown())
                    {
                        if (timedelta == 0)
                        {
                            // Released at same time as previous drag-- lift off!
                            // What was the drag delta now becomes the spin delta.
                            viewState.spinDelta = viewState.dragDelta;
                            // We don't really need a repaint here
                            // since we just saw a drag at the same time
                            // and it did a repaint...
                            // however we do know we want to repaint
                            // and it doesn't hurt.
                            if (viewComponent != null)
                                viewComponent.repaint();
                        }
                        else
                        {
                            // Failure to lift off.
                            viewState.spinDelta = null;
                            if (viewComponent != null)
                                viewComponent.repaint(); // so it can use higher quality paint
                        }
                        viewState.dragDelta = null; // no longer dragging
                    }
                } // mouseReleased
                public void mouseEntered(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 4) System.out.println("mouseExited on a "+controllerComponent.getClass().getSuperclass().getName());
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down
                }
                public void mouseExited(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 4) System.out.println("mouseExited on a "+controllerComponent.getClass().getSuperclass().getName());
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down
                }
            }); // mouse listener
            // watch for dragging gestures to rotate the 3D view
            controllerComponent.addMouseMotionListener(this.mouseMotionListener = new MouseMotionListener() {
                public void mouseDragged(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mouseDragged on a "+controllerComponent.getClass().getSuperclass().getName()+", time = "+(me.getWhen()-viewState.time0)/1000.);
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down
                    if (viewState.lastDrag == null)
                        return;
                    int thisDrag[] = {me.getX(), me.getY()};
                    if (viewParams.requireCtrlTo3dRotate.get() == me.isControlDown())
                    {
                        int pixelsMovedSqrd = VecMath.distsqrd(viewState.lastDrag, thisDrag);
                        if (pixelsMovedSqrd > 0) // do nothing if ended where we started
                        {
                            int dragDir[] = VecMath.vmv(thisDrag, viewState.lastDrag);
                            dragDir[1] *= -1; // in java, y is down, so invert it
                            float axis[] = {-dragDir[1],dragDir[0],0.f};
                            float radians = (float)Math.sqrt(pixelsMovedSqrd) / 300f;
                            viewState.dragDelta = VecMath.makeRowRotMat(radians, new float[][]{axis});
                            if (viewParams.restrictRoll.get())
                                viewParams.viewMat3d.set(VecMath.mxm(viewParams.viewMat3d.get(), zeroOutRollOnSpinDelta(viewState, viewState.dragDelta))); // XXX lame, fix the prototype so it can be static maybe
                            else
                                viewParams.viewMat3d.set(VecMath.mxm(viewParams.viewMat3d.get(), viewState.dragDelta));
                            if (pixelsMovedSqrd < 2*2)
                                viewState.dragDelta = null;
                        }
                    }
                    viewState.lastDrag = thisDrag;
                    viewState.lastDragTime = me.getWhen();
                    if (viewComponent != null)
                        viewComponent.repaint();
                } // mouseDragged
                public void mouseMoved(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 5) System.out.println("        mouseMoved on a "+controllerComponent.getClass().getSuperclass().getName());
                    if (!me.isShiftDown())
                        viewState.nShiftsDown = 0; // kill drift if we know no shifts down

                    int pickedStickerPoly[] = GenericPipelineUtils.pick(
                                                    me.getX(), me.getY(),
                                                    viewState.untwistedFrame,
                                                    model.genericPuzzleDescription);
                    int newSticker = pickedStickerPoly!=null ? pickedStickerPoly[0] : -1;
                    int newPoly = pickedStickerPoly!=null ? pickedStickerPoly[1] : -1;
                    if (newSticker != viewState.iStickerUnderMouse
                     || newPoly != viewState.iPolyUnderMouse)
                    {
                        viewState.iStickerUnderMouse = newSticker;
                        viewState.iPolyUnderMouse = newPoly;
                        if (viewComponent != null)
                            viewComponent.repaint();
                    }
                } // mouseMoved
            }); // mouse motion listener

        } // if newControllerComponent != null
    } // setControllerComponent

    /** Detaches this view guts from the old view component (if any) and attaches to the new view component (if any). */
    public void setViewComponent(Component viewComponent)
    {
        this.viewComponent = viewComponent;
        viewParams.setRestrictRoll(model, viewComponent, viewState, viewParams.getRestrictRoll()); // initiates the zero roll animation if appropriate
    }


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
        if (viewParams.eventVerboseLevel.get() >= 3) System.out.println("            painting on a "+view.getClass().getSuperclass().getName());

        // stole from MC4DView.updateViewFactors
        int
            W = view.getWidth(),
            H = view.getHeight(),
            min = W>H ? H : W;
        if(W*H == 0)
            return;

        g.setColor(viewParams.backgroundColor.get());
        g.fillRect(0, 0, W, H);
        if (viewParams.drawGround.get())
        {
            g.setColor(viewParams.groundColor.get());
            g.fillRect(0, H*6/9, W, W);
        }

        float pixels2polySF = 1f / Math.min(W, H) / viewParams.viewScale2d.get();
        int xOff = ((W>H) ? (W-H)/2 : 0) + min/2;
        int yOff = ((H>W) ? (H-W)/2 : 0) + min/2;

        if (model.isMoving())
        {
            //System.out.println("model says it's moving beforehand");
        }
        else
        {
            //System.out.println("model says it's still beforehand");
        }
        // XXX query whether animation is actually in progress
        model.advanceAnimation(modelListener,
                               viewParams.nFrames90.get(),
                               //viewParams.criticalDampingFraction.get()
                               1-viewParams.bounce.get()
                               );
        if (model.isMoving())
        {
            //System.out.println("model says it's moving afterwards");
            // do NOT call repaint... the animation advance automatically
            // notified the next guy already.  (That's why we passed
            // it our listener, so it could identify who the next guy is.)
            // If we call repaint here and we are a JPanel,
            // it will hog all the draw time
            // from the next guy if it's a Canvas.  Hey, just call me
            // Protector Of The Innocent.
        }
        else
        {
            //System.out.println("model says it's still afterwards");
        }

        MC4DModel.Twist twist = new MC4DModel.Twist(-1,-1,-1);
        int puzzleState[] = new int[model.genericPuzzleDescription.nStickers()];
        // XXX getAnimationState should reaturn a float
        float fractionOfWayThroughTwist = (float)model.getAnimationState(modelListener,
                                                                puzzleState, // fills this
                                                                twist);      // fills this
        //System.out.println("fraction of way through twist = "+fractionOfWayThroughTwist);


        if (viewState.iRotation < viewState.nRotation)
        {
            //
            // 4d rotation in progress
            //
            float incFrac = viewParams.interp.func((viewState.iRotation+1)/(float)viewState.nRotation)
                          - viewParams.interp.func(viewState.iRotation/(float)viewState.nRotation);
            float incmat[][] = com.donhatchsw.util.VecMath.makeRowRotMatThatSlerps(viewState.rotationFrom, viewState.rotationTo, incFrac);
            float newViewMat4d[][] = com.donhatchsw.util.VecMath.mxm(viewParams.viewMat4d.get(), incmat);
            com.donhatchsw.util.VecMath.gramschmidt(newViewMat4d, newViewMat4d);
            viewParams.viewMat4d.set(newViewMat4d);
            //System.out.println("    "+viewState.iRotation+"/"+viewState.nRotation+" -> "+(viewState.iRotation+1)+"/"+viewState.nRotation+"");
            if (!viewParams.frozenForDebugging.get())
            {
                viewState.iRotation++;
                view.repaint(); // make sure we keep drawing while there's more to do

                if (viewState.iRotation == viewState.nRotation
                 && viewParams.getRestrictRoll())
                {
                    // If we are finishing a rotate-to-center
                    // and we are in restricted roll mode,
                    // what we were using as a twirl axis is probably not
                    // very good any more.  Choose another.
                    if (viewState.rotationTo[3] < -.9999) // i.e. if it was a rot to center
                    {
                        viewParams.setRestrictRoll(model, view, viewState, viewParams.getRestrictRoll()); // even though it is already XXX make this cleaner & clearer if possible
                    }
                }
            }
        }

        if (viewState.spinDelta != null) // note, the old applet had an additional test "and not dragging" but we don't need it because spinDelta is never set during dragging now, dragDelta is instead
        {
            if (viewParams.restrictRoll.get())
                viewParams.viewMat3d.set(VecMath.mxm(viewParams.viewMat3d.get(), zeroOutRollOnSpinDelta(viewState, viewState.spinDelta)));
            else
                viewParams.viewMat3d.set(VecMath.mxm(viewParams.viewMat3d.get(), viewState.spinDelta));
            view.repaint();
        }

        GenericPipelineUtils.Frame frameToDrawInto = viewState.untwistedFrame;

        // XXX these numbers need to go up into the viewParams structure
        // XXX and FIX the FUDGE dag nab it

        // old params... but I don't think it was doing it right
        //float[] groundNormal = showShadows ? new float[] {0,1,.1f} : null;
        //float groundOffset = -1.f;

        // XXX why is this a bit diff from old?  well I don't think it was being done right for one thing
        float[] groundNormal = viewParams.showShadows.get() ? new float[] {0,1,.05f} : null;
        float groundOffset = -1.f;

        // XXX I don't seem to be quite the same as the original... unless I correct it here
        float scaleFudge4d = 1.f;
        float scaleFudge3d = 1.f;
        float scaleFudge2d = 4.7f;

        // XXX probably doing this more than necessary... when it's a rest frame that hasn't changed
        GenericPipelineUtils.computeFrame(
            frameToDrawInto,

            model.genericPuzzleDescription,
            viewParams.faceShrink4d.get(),
            viewParams.stickerShrink4d.get(),

            twist.grip,
            twist.dir,
            twist.slicemask,
            (float)fractionOfWayThroughTwist,

            VecMath.mxs(viewParams.viewMat4d.get(), scaleFudge4d),
            viewParams.eyeW.get(),
            VecMath.mxm(
                VecMath.makeRowRotMat(3, 2, 1, (float)Math.PI/2), // XXX FUDGE that makes it nicer for the pentagonal prismprism... what do we need, a preferred viewing orientation for each puzzle as part of the model description?
                VecMath.mxs(viewParams.viewMat3d.get(), scaleFudge3d)),
            viewParams.eyeZ.get(),
            new float[][]{{scaleFudge2d*viewParams.viewScale2d.get()/pixels2polySF, 0},
                          {0, -scaleFudge2d*viewParams.viewScale2d.get()/pixels2polySF},
                          {(float)xOff, (float)yOff}},

            VecMath.normalize(viewParams.towardsSunVec.get()),
            groundNormal,
            groundOffset,
            
            viewParams.useTopsort.get(),
            viewParams.showPartialOrder.get());

        /*
        if (frozenForDebugging)
        {
            if (frozenPartialOrderForDebugging != null)
                glueFrameToDrawInto.partialOrder = frozenPartialOrderForDebugging;
            else
                frozenPartialOrderForDebugging = glueFrameToDrawInto.partialOrder;
        }
        */

        // XXX figure out cleaner way to manage this
        float faceRGB[][] = new float[model.genericPuzzleDescription.nFaces()][3];
        for (int i = 0; i < faceRGB.length; ++i)
        {
            Color color = viewParams.faceColors[i%viewParams.faceColors.length].get();
            color.getColorComponents(faceRGB[i]);
        }

        GenericPipelineUtils.paintFrame(
                frameToDrawInto,
                model.genericPuzzleDescription,
                model.genericPuzzleState,
                viewParams.showShadows.get(),
                viewParams.drawGround.get() ? viewParams.groundColor.get() : null,
                //viewParams.faceRGB,
                faceRGB,
                viewState.iStickerUnderMouse,
                viewState.iPolyUnderMouse,
                viewParams.highlightByCubie.get(),
                !viewParams.highlightByCubie.get() && viewParams.highlightByGrip.get(), // XXX mess, see if I can make this cleaner
                viewParams.drawShrunkStickerOutlines.get() ? viewParams.shrunkStickerOutlineColor.get() : null,
                g,

                viewParams.jitterRadius.get(),
                viewParams.drawLabels.get(),
                viewParams.showPartialOrder.get());
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

        public static void initiateZeroRoll(ViewState viewState,
                                            float faceCenters[][],
                                            float viewMat4d[][],
                                            float viewMat3d[][],
                                            double nFrames90,
                                            Component viewComponent)
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
            viewState.rotationFrom = com.donhatchsw.util.VecMath.vxm(faceCenters[iFace], viewMat4d);
            viewState.rotationFrom[3] = 0.f;
            com.donhatchsw.util.VecMath.normalize(viewState.rotationFrom,
                                                  viewState.rotationFrom);
            // pointOnYZArc is now in screen space...
            // to get the point we want to rotate to,
            // we undo the viewMat3d on it,
            // i.e. apply viewMat3d's transpose, i.e. its inverse,
            // i.e. multiply by it on the opposite side as usual
            viewState.rotationTo = com.donhatchsw.util.VecMath.mxv(viewMat3d, pointOnYZArc);

            double totalRotationAngle = com.donhatchsw.util.VecMath.angleBetweenUnitVectors(
                                viewState.rotationFrom,
                                viewState.rotationTo);
            viewState.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
            if (viewState.nRotation == 0) viewState.nRotation = 1;
            viewState.iRotation = 0;

            // Remember the zero roll pole
            // for subsequent calls to zeroOutRollOnSpinDelta
            viewState.zeroRollPoleAfterRot3d = pointOnYZArc;

            //System.out.println("this.rotationFrom = "+com.donhatchsw.util.VecMath.toString(this.rotationFrom));
            //System.out.println("this.rotationTo = "+com.donhatchsw.util.VecMath.toString(this.rotationTo));

            viewComponent.repaint();
        } // initiateZeroRoll

        // Uses the zeroRollPole from the most recent call
        // to initiatezeroRoll. // XXX should not
        public static float[][] zeroOutRollAndMaybeTiltOnSpinDelta(ViewState viewState,
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

            Assert(viewState.zeroRollPoleAfterRot3d != null); // initiateZeroRoll must have been called previously
            viewState.zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.copyvec(3, viewState.zeroRollPoleAfterRot3d); // XXX sigh... because vxm and other stuff freaks if I don't
            // Clamp tilt to [0..pi/2]...
            double currentTilt = Math.atan2(viewState.zeroRollPoleAfterRot3d[2],
                                            viewState.zeroRollPoleAfterRot3d[1]);
            //System.out.println("tiltDeltaAngle before = "+tiltDeltaAngle*180/Math.PI);
            if (tiltDeltaAngle > Math.PI/2 - currentTilt)
                tiltDeltaAngle = Math.PI/2 - currentTilt;
            if (tiltDeltaAngle < 0 - currentTilt)
                tiltDeltaAngle = 0 - currentTilt;
            //System.out.println("tiltDeltaAngle after = "+tiltDeltaAngle*180/Math.PI);

            double twirlDelta[][] = com.donhatchsw.util.VecMath.makeRowRotMat(twirlDeltaAngle, new double[][]{com.donhatchsw.util.VecMath.floatToDouble(viewState.zeroRollPoleAfterRot3d)});
            double tiltDelta[][] = com.donhatchsw.util.VecMath.makeRowRotMat(tiltDeltaAngle, new double[][]{{1,0,0}});
            float adjustedSpinDelta[][] = com.donhatchsw.util.VecMath.doubleToFloat(com.donhatchsw.util.VecMath.mxm(twirlDelta, tiltDelta));

            // Gram-schmidt so we don't drift to non-orthogonal
            // XXX wasn't there a nicer more symmetric way of doing this?
            com.donhatchsw.util.VecMath.gramschmidt(adjustedSpinDelta,
                                                    adjustedSpinDelta);

            // need to apply it to the pole...
            viewState.zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.vxm(viewState.zeroRollPoleAfterRot3d,
                                                                     adjustedSpinDelta);
            return adjustedSpinDelta;
        } // zeroOutRollAndMaybeTiltOnSpinDelta

        // when dragging, we allow tilt changes
        public static float[][] zeroOutRollOnSpinDelta(ViewState viewState,
                                                       float spindelta[][])
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(viewState, spindelta, false);
        }
        // when autospinning, we don't allow tilt changes,
        // or it would just drift to the min or max tilt, which looks dumb
        public static float[][] zeroOutRollAndTiltOnSpinDelta(ViewState viewState,
                                                              float spindelta[][])
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(viewState, spindelta, true);
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
    // Default color scheme utilities.
    // Still needs lots of work.
    //
        // First observation:
        //  If you look at the standard hue wheel,
        //  perceptually it changes very slowly
        //  near the primary colors r,g,b
        //  and very quickly near the secondary colors c,m,y.
        //  So if we want to evenly spread out colors around the wheel
        //  in terms of perception, we should crowd lots of samples
        //  around the secondary colors and not so many
        //  around the primary colors.
        //  Eyeballing it, it looks like the perceptual
        //  halfway point between a primary color
        //  and an adjacent secondary color
        //  is about 3/4 of the way towards the secondary color,
        //  so I'll use that as the basis for everything.
        // Second observation:
        //  This may be completely subjective,
        //  but it seems to me that there are 8 perceptually
        //  distinct hues:  the 6 usual primary&secondary hues,
        //  plus orange and violet.
        private static double linearizeHue(double perceptualHue)
        {
            double hue = perceptualHue - Math.floor(perceptualHue);
            if (true)
            {
                // The original perceptual hue is 1/8-oriented,
                // i.e. it thinks in terms of r,o,y.g,c,b,v,m.
                // Convert this into something that's 1/6-oriented,
                // via the piecewise linear mapping:
                //      0/8 -> 0/6  red
                //      1/8 ->  1/12  orange
                //      2/8 -> 1/6  yellow
                //      3/8 -> 2/6  green
                //      4/8 -> 3/6  cyan
                //      5/8 -> 4/6  blue
                //      6/8 ->  3/4   violet
                //      7/8 -> 5/6  magenta
                //      8/8 -> 6/6  red again
                final double perceptualHues[] = {
                    0/6.,   // red
                    1/12.,  //   orange
                    1/6.,   // yellow
                    2/6.,   // green
                    3/6.,   // cyan
                    4/6.,   // blue
                    3/4.,   //   violet
                    5/6.,   // magenta
                    6/6.,   // red again
                };
                int i = (int)(hue*8);
                double frac = hue*8 - i;
                hue = perceptualHues[i]*(1-frac)
                    + perceptualHues[i+1]*frac;
            }
            // Now, consider the fraction of the way
            // we are from the nearest secondary color
            // to the nearest primary color,
            // and square that fraction.
            // E.g. halfway from cyan to blue
            // turns into 1/4 of the way from cyan to blue.
            // That keeps us closer to the secondary color
            // longer, which is what we want,
            // because that's where the hue is varying fastest
            // perceptually.
            if (true)
            {
                int i = (int)(hue*6);
                double frac = hue*6 - i;
                if (i % 2 == 1)
                {
                    // y->g or c->b or m->r
                    frac = frac*frac;
                }
                else
                {
                    // y->r or c->g or m->b
                    frac = 1-(1-frac)*(1-frac);
                }
                hue = (i+frac)/6;
            }
            return hue;
        } // linearizeHue

        //
        // Saturations vary fastest perceptually
        // near zero.  So just square the saturation
        // so it doesn't vary as fast in linear space near zero.
        private static double linearizeSat(double perceptualSat)
        {
            return perceptualSat*perceptualSat;
        }


        //
        // Attempt to autogenerate some nice colors.
        // The sequence will be:
        //     1. Fully saturated 8 colors:
        //             red
        //             orange
        //             yellow
        //             green
        //             cyan
        //             blue
        //             violet
        //             magenta
        //             red
        //    2. Same 8 colors with saturation = .6
        //    3. Same 8 colors with saturation = .8
        //    4. Same 8 colors with saturation = .4
        //    Then repeat all of the above with in-between hues
        //    Then repeat all of the above with in-between saturations
        //        .9,.5,.7,.3
        // Eh, on second thought, use only those 4 saturations,
        // then keep doing in-between hues only;
        // that makes something that's easier to comprehend
        // when laying it out in rows of 32.
        // 
        private static void autoGenerateHueAndSat(int iFace,
                                                  double hueAndSat[/*2*/])
        {
            Assert(iFace >= 0);

            double hue = iFace/8.;
            double sat = 1.;
            iFace /= 8;

            double satDecrement = .4;
            double hueIncrement = 1/16.;

            for (int i = 0; i < 2; ++i)
            {
                if (iFace > 0)
                {
                    if (iFace % 2 == 1)
                        sat -= satDecrement;
                    satDecrement /= 2;
                    iFace /= 2;
                }
            }
            while (iFace > 0)
            {
                if (iFace % 2 == 1)
                    hue += hueIncrement;
                hueIncrement /= 2;
                iFace /= 2;
                /*
                if (iFace > 0)
                {
                    if (iFace % 2 == 1)
                        sat -= satDecrement;
                    satDecrement /= 2;
                    iFace /= 2;
                }
                */
            }
            hueAndSat[0] = hue;
            hueAndSat[1] = sat;
        } // autoGenerateHueAndSat

        private static Color autoGenerateColor(int iFace)
        {
            double hueAndSat[] = new double[2];
            autoGenerateHueAndSat(iFace, hueAndSat);
            double hue = hueAndSat[0];
            double sat = hueAndSat[1];
            hue = linearizeHue(hue);
            sat = linearizeSat(sat);
            if (false)
                return new Color((float)Math.random(), (float)Math.random(), (float)Math.random());
            return Color.getHSBColor((float)hue, (float)sat, 1.f);
        } // autoGenerateColor



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
        guts.setControllerComponent(myPanel, false);
        guts.setViewComponent(myPanel);
        //myPanel.setPreferredSize(new java.awt.Dimension(w,h)); // set size bottom up


        JFrame jframe = new JFrame("A spiffy new JPanel");

        jframe.setForeground(java.awt.Color.white);
        jframe.setBackground(java.awt.Color.black);

        jframe.setContentPane(myPanel);
        jframe.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent event)
            {
                System.out.println("Chow!");
                guts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
                guts.setViewComponent(null); // XXX make this not necessary, with weak ref I think
                if (model.nListeners() == 0)
                    System.exit(0); // asinine way of doing things
            }
        });

        //jframe.pack();
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
        guts.setControllerComponent(myCanvas, false);
        guts.setViewComponent(myCanvas);
        //myCanvas.setSize(new java.awt.Dimension(w,h)); // set size bottom up


        final Frame frame = new Frame("A sucky old Canvas") { 
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
                        guts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
                        guts.setViewComponent(null); // XXX make this not necessary. with weak ref I think
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
                    guts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
                    guts.setViewComponent(null); // XXX make this not necessary. with weak ref I think
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

        final MC4DModel model = new MC4DModel(puzzleDescription);

        makeExampleModernViewer(model, 50,50, 300,300);

        boolean doDoubleBuffer = true; // make it even more sucky than necessary
        makeExampleAncientViewer(model, 350,50, 300,300, doDoubleBuffer);


        {
            // XXX dup code!!! figure out how to get it properly... and puzzle description might change!
            float faceRGB[][] = { {0, 0, 1}, {0.5f, 0, 0}, {.4f, 1, 1}, {1, 0, .5f}, {.9f, .5f, 1}, {1, .5f, 0}, {1, 1, .5f}, {0, 1, .5f}, };
            final java.awt.Color faceColor[] = new java.awt.Color[faceRGB.length];
            for (int i = 0; i < faceRGB.length; ++i)
                faceColor[i] = new java.awt.Color(faceRGB[i][0],faceRGB[i][1],faceRGB[i][2]);

            com.donhatchsw.util.UndoTree.ItemLengthizer lengthizer = new com.donhatchsw.util.UndoTree.ItemLengthizer() {
                // XXX this is duplicated in MC4DModel
                public double length(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    Assert(twist != null);
                    Assert(twist.grip != -1);
                    int order = model.genericPuzzleDescription.getGripSymmetryOrders()[twist.grip];
                    if (order <= 0)
                        return 1.; // XXX can this happen, and why?
                    double nQuarterTurns = 4./order
                                         * Math.abs(twist.dir); // power multiplier
                    return nQuarterTurns;
                }
            };
            com.donhatchsw.util.UndoTreeViewer.ItemColorizer colorizer = new com.donhatchsw.util.UndoTreeViewer.ItemColorizer() {
                public java.awt.Color color(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    int grip = twist.grip;
                    Assert(grip != -1);
                    int face = model.genericPuzzleDescription.getGrip2Face()[grip];
                    return faceColor[face % faceColor.length];
                }
                public String leftLabel(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    int grip = twist.grip;
                    int order = model.genericPuzzleDescription.getGripSymmetryOrders()[grip];
                    String degrees = "\u00B0"; // XXX not sure this magic works everywhere, got it from http://www.fileformat.info/info/unicode/char/00b0/index.htm

                    if (order <= 0)
                        return "WTF?"; // XXX can this happen, and why?
                    else
                        return ""+(360/order)+degrees; // XXX this does integer, is that okay?  don't want it to take forever
                }
                public String rightLabel(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    return twist.toString();
                }
            };

            //
            // Make a viewer that shows the undo tree views--
            // the controller view which updates immediately,
            // and the animation view which lags behind.
            //
            com.donhatchsw.util.UndoTreeViewer controllerUndoTreeViewer =
            com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer("Controller's view of the undo tree", model.controllerUndoTree, null, null, 500, 20, 350, 600,
                    // XXX oh gag
                    new int[1],
                    new int[]{1}, // nViewersAlive-- set this to a positive number so the viewer won't exit the program when it's closed (XXX in fact we could also use the same mechanism, that would be even better)
                    new int[1],
                    new int[1],
                    new int[1],
                    false, // don't allow the example "Do" which would put dummy strings in the tree
                    true, // but do allow undo/redo
                    lengthizer,
                    colorizer);

            // XXX need accessors for these instead of making them public I think
            controllerUndoTreeViewer.showLabels = false; // XXX need an accessofr for this
            controllerUndoTreeViewer.centerCurrentNode.set(0.); // false


            com.donhatchsw.util.UndoTreeViewer animationUndoTreeViewer = 
            com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer("Animation's view of the undo tree", model.animationUndoTree, null, null, 850, 20, 350, 600,
                    // XXX oh gag
                    new int[1],
                    new int[]{1}, // nViewersAlive-- set this to a positive number so the viewer won't exit the program when it's closed (XXX in fact we could also use the same mechanism, that would be even better)
                    new int[1],
                    new int[1],
                    new int[1],
                    false, // don't allow the example "Do" which would put dummy strings in the tree
                    false, // and don't allow undo/redo from this view either, since instantaneous changes would make it get out of sync with the permutation array. undo/redo must be done from the controller window, this one is just for watching.
                    lengthizer,
                    colorizer);

            // XXX need accessors for these instead of making them public I think
            animationUndoTreeViewer.showLabels = false;
            animationUndoTreeViewer.centerCurrentNode.set(0.); // false
        }

    } // main

} // MC4DViewGuts



