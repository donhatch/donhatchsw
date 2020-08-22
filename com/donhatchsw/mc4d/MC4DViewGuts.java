/**
* All right, here's how to make everyone happy...
* A completely generic viewer that can be stuck inside
* an ancient Canvas, or icky Applet,
* or a beautiful modern JComponent.
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
*           extends JComponent
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
    static private void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }

    //
    // Classes...
    //
        public static interface InterpFunc { public float func(float f); }
        public static InterpFunc sine_interp = new InterpFunc() {
            @Override public float func(float x) { return (float)(Math.sin((x - .5) * Math.PI) + 1) / 2; }
        };
        public static InterpFunc linear_interp = new InterpFunc() {
            @Override public float func(float x) { return x; }
        };
        public static InterpFunc quad_interp = new InterpFunc() {
            @Override public float func(float x)
            {
                if (x <= .5f)
                    return x*x*2.f;
                else
                    //return 1-func(1-x);
                    return 1 - (1-x)*(1-x)*2.f;
            }
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

        // CBB:
        // We probably want eyeW to be as small as possible (though > 1),
        // but currently making too close to 1 makes topsort fail.
        // (And, it makes the puzzle look unfamiliar sometimes.)
        // Empirically, here are some thresholds:
        // eyeW=1.1: default puzzle bad on some twists
        // eyeW=1.05: just large enough so topsort fully succeeds in hypermegaminx "{5,3,3} 3" at rest. Too small for 4,3,3 in nonstandard positions though.
        // eyeW=1.5: good for 4,3,3 3 vert first, but edge-first outer faces are exactly flat/flaky (!) so needs to be smaller
        // eyeW=1.45: good (though still very flat) for 4,3,3 3 edge-first, still decent for vertex first
        public Listenable.Float eyeW = new Listenable.Float(0.f, 4.f, 1.45f); // 1 means circumradius after shrinks applied (4d shrinks only, currently).  Theoretically anything >=1 should be safe for rendering, however, currently, too close to 1 makes the topsort fail.

        public Listenable.FloatMatrix viewMat3d = new Listenable.FloatMatrix(
            VecMath.mxm(VecMath.makeRowRotMat(3, 2,0, -42*(float)Math.PI/180), // twirl
                        VecMath.makeRowRotMat(3, 1,2,  30*(float)Math.PI/180))); // tilt
        public Listenable.Float faceShrink3d = new Listenable.Float(0.f, 1.f, 1.f);
        public Listenable.Float stickerShrink3d = new Listenable.Float(0.f, 1.f, 1.f);
        public Listenable.Float eyeZ = new Listenable.Float(0.f, 20.f, 10.f);
        public Listenable.Float viewScale2d = new Listenable.Float(0.f, 4.f, 1.f);
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



        //public Listenable.Double nFrames90(0., 100., 15.);
        //public Listenable.Double nFrames90 = new Listenable.Double(0., 100., 30.);
        public Listenable.Double nFrames90 = new Listenable.Double(0., 300., 150.);
        public Listenable.Double bounce = new Listenable.Double(0., 1., 0.);

        // Currently this is used for rot to center, but not for twists (which are smoothed by the undo tree, which effectively uses a quadratic interpolator).
        // I think I still like the sine one better than the quad one, which is aggravating because it's the quad for twists no matter what, I thought that was the uber solution
        public InterpFunc interp = quad_interp;
        //public InterpFunc interp = sine_interp;
        //public InterpFunc interp = linear_interp;
        public Listenable.Boolean requireCtrlTo3dRotate = new Listenable.Boolean(false);
        public Listenable.Boolean restrictRoll = new Listenable.Boolean(false);
            public boolean getRestrictRoll() { return restrictRoll.get(); }
            public void setRestrictRoll(MC4DModel model, Component viewComponent, ViewState viewState, boolean newRestrictRoll) // XXX shouldn't take model as a param I don't think, revisit this
            {
                // Do this even if it looks like we were already
                // restricting roll, in case we are initializing
                // a new model or something XXX revisit whether this is the cleanest way to do this... note we also have the listener, so this is redundant in the case it actually did change
                // XXX This shouldn't go in here, the listener should handle it completely
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
        public Listenable.Boolean futtIfPossible = new Listenable.Boolean(false);

        //
        // Ornamentational appearance params
        //
        public Listenable.Boolean highlightByCubie = new Listenable.Boolean(false);
        public Listenable.Boolean highlightByGrip = new Listenable.Boolean(true); // if possible; it gets turned into highlight by sticker automatically if there is no grip info, which there isn't unless it's a 2x2x2x2 currently.  XXX get rid of this?
        public Listenable.Boolean showShadows = new Listenable.Boolean(true);

        public Listenable.Int antialias = new Listenable.Int(0,2,0); // 0=never, 1=when still, 2=always  XXX not ready for prime time-- not checking whether still properly, also the transition completely sucks; it moves the picture by 1/2 pixel

        public Listenable.Boolean drawNonShrunkFaceOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawShrunkFaceOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawNonShrunkStickerOutlines = new Listenable.Boolean(false);
        public Listenable.Boolean drawShrunkStickerSurfaces = new Listenable.Boolean(true);
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
        public Listenable.Boolean topsortUsesBoldNewWay = new Listenable.Boolean(true);
        public Listenable.Boolean showNumPaintsDone = new Listenable.Boolean(true);
        public Listenable.Int jitterRadius = new Listenable.Int(0,9,0);
        public Listenable.Boolean drawLabels = new Listenable.Boolean(false);
        public Listenable.Boolean showPartialOrder = new Listenable.Boolean(false);
        public Listenable.Boolean frozenForDebugging = new Listenable.Boolean(false); // XXX is this state rather than params?  never want to save it out, do we? but we do set it using the ctrl-alt keys... hmm
            public int frozenPartialOrderForDebugging[][][] = null; // XXX this is state you dingbat.  was there not supposed to be any?

        // String representation includes all current values of
        // all fields that are Listenables
        // (but not their mins, maxes, or defaults).
        @Override public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("{");

            Class<? extends ViewParams> myClass = this.getClass();
            java.lang.reflect.Field[] fields = myClass.getFields();
            for (int iField = 0; iField < fields.length; iField++)
            {
                java.lang.reflect.Field field = fields[iField];
                Class<?> fieldType = field.getType();
                if (Listenable.class.isAssignableFrom(fieldType))
                {
                    try {
                        Listenable fieldValue = (Listenable)field.get(this);
                        sb.append(field.getName()+"="+fieldValue.toString());
                        sb.append(',');
                    } catch (IllegalAccessException e) {
                        CHECK(false);
                    }
                }
            }
            sb.deleteCharAt(sb.length()-1); // delete the last comma
            sb.append("}");
            return sb.toString();
        } // toString
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

        public int nPaintsDone = 0;
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
    public ViewState viewState = new ViewState(); // XXX made public so that the Frame Picture button can use it to get the bbox of the most recent frame... maybe should be accessors to get this info instead, expecially since there are probably race conditions with it now
    private java.util.ArrayList<Listenable.Listener> keepalive = new java.util.ArrayList<Listenable.Listener>(); // keeps my listeners alive with strong refs for as long as I'm alive



    //
    // The listener I use to listen to the model
    // for changes (increments in animation which are
    // initiated by other views or by myself).
    // My response to a change is simply to call repaint on the view Component;
    // then when paint() is called it will query the animation progress
    // from the model (which pumps the animation).
    // XXX think about doing this automatically, maybe having a Listenable that's just a version number or something
    //
    MC4DModel.Listener modelListener = new MC4DModel.Listener() {
        @Override public void movingNotify()
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

    // To restore when I detach from this controller Component  (XXX apparently not implemented)
    private java.util.ArrayList<java.awt.event.KeyListener> savedKeyListeners;
    private java.util.ArrayList<java.awt.event.MouseListener> savedMouseListeners;
    private java.util.ArrayList<java.awt.event.MouseMotionListener> savedMouseMotionListeners;


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
                @Override public void keyPressed(KeyEvent ke)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("keyPressed "+ke);
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("    isShiftDown = "+ke.isShiftDown());
                    int keyCode = ke.getKeyCode();
                    int numkey = keyCode - KeyEvent.VK_0;
                    if(1 <= numkey && numkey <= 9)
                        viewState.slicemask |= 1<<(numkey-1); // turn on the specified bit
                    else
                    {
                        // Anything with ctrl-alt in it is undetectable
                        // using keyTyped, at least on some platforms
                        // (e.g. HP pavilion laptop running windows vista)
                        // so we have to detect those here in keyPressed instead.

                        // Also, ke.getKeyChar() is useless in older VMs,
                        // for ctrl-alt things...
                        // in 1.6 it returns 1 for ctrl-alt-'a', (good).
                        // but in 1.3 and 1.2 it returns 65535 (unknown).
                        // So, have to use key codes instead.
                        // TODO: modernize since we now support only >=1.7

                        if (ke.isControlDown()
                         && ke.isAltDown())
                        {
                            if (false) {}
                            else if (keyCode == KeyEvent.VK_T) // ctrl-alt-t
                            {
                                System.out.println("useTopsort "+viewParams.useTopsort.get()+" -> "+!viewParams.useTopsort.get()+"");
                                viewParams.useTopsort.set(!viewParams.useTopsort.get());
                                if (viewComponent != null)
                                    viewComponent.repaint();
                            }
                            else if (keyCode == KeyEvent.VK_J) // ctrl-alt-j
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
                            else if (keyCode == KeyEvent.VK_L) // ctrl-alt-l
                            {
                                System.out.println("drawLabels "+viewParams.drawLabels.get()+" -> "+!viewParams.drawLabels.get()+"");
                                viewParams.drawLabels.set(!viewParams.drawLabels.get());
                                if (viewComponent != null)
                                    viewComponent.repaint();
                            }
                            else if (keyCode == KeyEvent.VK_P) // ctrl-alt-p
                            {
                                System.out.println("showPartialOrder "+viewParams.showPartialOrder.get()+" -> "+!viewParams.showPartialOrder.get()+"");
                                viewParams.showPartialOrder.set(!viewParams.showPartialOrder.get());
                                if (viewComponent != null)
                                    viewComponent.repaint();
                            }
                            else if (keyCode == KeyEvent.VK_SPACE) // ctrl-alt-space
                            {
                                System.out.println("frozenForDebugging "+viewParams.frozenForDebugging.get()+" -> "+!viewParams.frozenForDebugging.get()+"");
                                viewParams.frozenForDebugging.set(!viewParams.frozenForDebugging.get());
                                viewParams.frozenPartialOrderForDebugging = null;
                                if (viewComponent != null)
                                    viewComponent.repaint();
                            }
                            else if (keyCode == KeyEvent.VK_V) // ctrl-alt-v
                            {
                                System.out.print("eventVerboseLevel "+viewParams.eventVerboseLevel.get());
                                viewParams.eventVerboseLevel.set((viewParams.eventVerboseLevel.get()+1) % (viewParams.eventVerboseLevel.max()-viewParams.eventVerboseLevel.min()) + viewParams.eventVerboseLevel.min());
                                System.out.println(" -> "+viewParams.eventVerboseLevel.get());
                                ke.consume(); // XXX does this make it so subsequent handlers don't see it? dammit, no it doesn't. damn damn damn fuck fuck fuck. what does it mean?
                            }
                            else if (keyCode == KeyEvent.VK_H) // ctrl-alt-h -- help on the secret ctrl-alt things
                            {
                                System.out.println("======================");
                                System.out.println("Secret ctrl-alt key combinations:");
                                System.out.println("    ctrl-alt-h -- show this help message");
                                System.out.println("    ctrl-alt-t -- toggle useTopSort");
                                System.out.println("    ctrl-alt-j -- cycle jitterRadius");
                                System.out.println("    ctrl-alt-l -- toggle drawLabels");
                                System.out.println("    ctrl-alt-p -- toggle showPartialOrder");
                                System.out.println("    ctrl-alt-v -- cycle eventVerboseLevel");
                                System.out.println("    ctrl-alt-space -- toggle frozenForDebugging");
                                System.out.println("======================");
                            }
                            else if (keyCode != KeyEvent.VK_ALT
                                  && keyCode != KeyEvent.VK_CONTROL
                                  && keyCode != KeyEvent.VK_SHIFT)
                            {
                                System.out.println("Unrecognized key sequence ctrl-alt-(keyCode="+keyCode+"=\""+KeyEvent.getKeyText(keyCode)+"\" keyChar="+(ke.getKeyChar()>=32&&ke.getKeyChar()<=126?String.format("%c",ke.getKeyChar()):String.format("ord %d",0+ke.getKeyChar()))+")");
                            }
                        }
                    }
                } // keyPressed
                @Override public void keyReleased(KeyEvent ke)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("keyReleased "+ke);
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("    isShiftDown = "+ke.isShiftDown());
                    int keyCode = ke.getKeyCode();
                    int numkey = keyCode - KeyEvent.VK_0;
                    if(1 <= numkey && numkey <= 9)
                        viewState.slicemask &= ~(1<<(numkey-1)); // turn off the specified bit
                } // keyReleased

                // NOTE-- on my HP pavilion laptop running vista, keyTyped can't be used to detect things like ctrl-alt-v: once ctrl-alt is down, v no longer generates a keyTyped at all (only a key pressed and key released).  So, I moved all the ctrl-alt stuff to the keyPressed section.
                @Override public void keyTyped(KeyEvent ke)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("keyTyped");
                    char c = ke.getKeyChar();
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("============generic keyTyped listener got key '"+c+"'("+(int)c+")"); // XXX escapify!

                    if (false) {}
                    else if (c == 's'-'a'+1) // ctrl-s -- save
                    {
                        // For starters, just dump the model to stdout.
                        System.out.println("The model is...");
                        System.out.println(model);
                    }



                } // keyTyped
            }); // key listener
            controllerComponent.addMouseListener(this.mouseListener = new MouseListener() {
                @Override public void mouseClicked(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mouseClicked on a "+controllerComponent.getClass().getSuperclass().getName());
                    if (viewParams.requireCtrlTo3dRotate.get())
                    {
                        if (me.isControlDown())
                            return; // restricted mode, so ctrled mouse affects only the spin... which is implemented in the press,drag,release callbacks, not this one
                        if (viewComponent != null)
                            viewComponent.repaint(); // so it keeps spinning if it was spinning
                    }

                    boolean isRotate = isMiddleMouseButton(me);
                    if (false) // make this true to debug the pick
                    {
                        int hit[] = GenericPipelineUtils.pick(me.getX(), me.getY(),
                                                              viewState.untwistedFrame);
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
                                         viewState.untwistedFrame); // XXX move that into here, it's view specific!

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
                                         viewState.untwistedFrame);
                                    CHECK(polyAndStickerAndFaceCenter != null); // hit once, should hit again
                                    float polyCenter[] = polyAndStickerAndFaceCenter[0];

                                    // Interested in only the w component
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
                                        viewState.untwistedFrame);
                        if (iGrip != -1)
                        {
                            boolean futtIfPossible = viewParams.futtIfPossible.get();
                            int order = model.genericPuzzleDescription.getGripSymmetryOrders(futtIfPossible)[iGrip];

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

                            model.initiateTwist(iGrip, dir, viewState.slicemask, futtIfPossible);
                            // do NOT call repaint here!
                            // the model will notify us when
                            // we need to repaint, when it's our turn.
                        }
                        else
                            System.out.println("missed");
                    }
                } // mouseClicked
                @Override public void mousePressed(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mousePressed on a "+controllerComponent.getClass().getSuperclass().getName());
                    viewState.lastDrag = new int[]{me.getX(), me.getY()};
                    viewState.lastDragTime = me.getWhen();
                    if (viewParams.requireCtrlTo3dRotate.get() == me.isControlDown())
                    {
                        viewState.spinDelta = null;
                        viewState.dragDelta = null;
                    }
                } // mousePressed
                @Override public void mouseReleased(MouseEvent me)
                {
                    long timedelta = me.getWhen() - viewState.lastDragTime;
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mouseReleased on a "+controllerComponent.getClass().getSuperclass().getName()+", time = "+(me.getWhen()-viewState.time0)/1000.+", timedelta = "+timedelta);
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
                @Override public void mouseEntered(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 4) System.out.println("mouseExited on a "+controllerComponent.getClass().getSuperclass().getName());
                }
                @Override public void mouseExited(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 4) System.out.println("mouseExited on a "+controllerComponent.getClass().getSuperclass().getName());
                }
            }); // mouse listener
            // watch for dragging gestures to rotate the 3D view
            controllerComponent.addMouseMotionListener(this.mouseMotionListener = new MouseMotionListener() {
                @Override public void mouseDragged(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 1) System.out.println("mouseDragged on a "+controllerComponent.getClass().getSuperclass().getName()+", time = "+(me.getWhen()-viewState.time0)/1000.);
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
                @Override public void mouseMoved(MouseEvent me)
                {
                    if (viewParams.eventVerboseLevel.get() >= 5) System.out.println("        mouseMoved on a "+controllerComponent.getClass().getSuperclass().getName());

                    int pickedStickerPoly[] = GenericPipelineUtils.pick(
                                                    me.getX(), me.getY(),
                                                    viewState.untwistedFrame);
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
        // We don't take a model in the constructor;
        // the caller has to call setModel(model) instead (or leave it null).
        // This may seem like a pain but it makes it
        // so they are always thinking in terms of attaching and detaching,
        // and they won't have to go rooting through the API
        // looking for setModel later.
        //
        viewParams = new ViewParams();
        attachListenersToViewParams();
    } // ctor from nothing

    /** Constructor that shares or clones view parameters from an existing guts. */
    public MC4DViewGuts(ViewParams otherParams,
                        boolean cloneParams) // if not cloneParams, then share
    {
        if (cloneParams)
        {
            // Copy the params
            viewParams = new ViewParams();
            Listenable fromListenables[] = Listenable.allListenablesInObject(otherParams);
            Listenable toListenables[] = Listenable.allListenablesInObject(viewParams);
            for (int i = 0; i < fromListenables.length; ++i)
                toListenables[i].set(fromListenables[i]);
        }
        else
        {
            // Share the params
            viewParams = otherParams;
        }
        attachListenersToViewParams();
    } // ctor from existing params

    // just common init code
    private void attachListenersToViewParams()
    {
        Listenable listenables[] = Listenable.allListenablesInObject(viewParams);
        for (int i = 0; i < listenables.length; ++i)
        {
            Listenable.Listener listener = new Listenable.Listener() {
                @Override public void valueChanged()
                {
                    if (viewComponent != null)
                        viewComponent.repaint();
                }
            };
            listenables[i].addListener(listener);
            keepalive.add(listener);
        }

        {
            Listenable.Listener listener = new Listenable.Listener() {
                @Override public void valueChanged()
                {
                    viewParams.setRestrictRoll(model, viewComponent, viewState, viewParams.restrictRoll.get());
                }
            };
            viewParams.restrictRoll.addListener(listener);
            keepalive.add(listener);
        }
    } // attachListenersToViewParams


    // PAINT
    void paint(Component view, Graphics g)
    {
        if (viewParams.eventVerboseLevel.get() >= 3) System.out.println("            begin painting on a "+view.getClass().getSuperclass().getName());

        // stole from MC4DView.updateViewFactors
        Dimension viewSize = view.getSize(); // getWidth,getHeight don't exist in 1.1
        int
            W = viewSize.width,
            H = viewSize.height,
            min = W>H ? H : W;
        if(W*H == 0)
        {
            if (viewParams.eventVerboseLevel.get() >= 3) System.out.println("            end painting on a "+view.getClass().getSuperclass().getName()+" (W*H==0)");
            return;
        }

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

        boolean wasMoving = model.isMoving();
        if (wasMoving)
        {
            //System.out.println("model says it's moving beforehand");
        }
        else
        {
            //System.out.println("model says it's still beforehand");
        }
        if (!viewParams.frozenForDebugging.get())
        {
            model.advanceAnimation(modelListener,
                                   viewParams.nFrames90.get(),
                                   viewParams.bounce.get()
                                   );
        }
        boolean isMoving = model.isMoving();
        if (isMoving)
        {
            //System.out.println("model says it's moving afterwards");
            // do NOT call repaint... the animation advance automatically
            // notified the next guy already.  (That's why we passed
            // it our listener, so it could identify who the next guy is.)
            // If we call repaint here and we are a JComponent,
            // it will hog all the draw time
            // from the next guy if it's a Canvas.  Hey, just call me
            // Protector Of The Innocent.
        }
        else
        {
            //System.out.println("model says it's still afterwards");
        }

        if (true) // XXX not ready for prime time-- need to figure out whether moving in any way, not just twisting
        {
            if (!System.getProperty("java.version").startsWith("1.1.") // have to check to avoid Graphics2D class not found error under 1.1
             && g instanceof Graphics2D) {
                boolean okToAntialias = true
                                      // && allowAntiAliasing && lastDrag==null && spindelta==null // XXX need to do something like this!
                                      && (viewParams.antialias.get() == 2 ||
                                          (viewParams.antialias.get() == 1 && !wasMoving));
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    okToAntialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
            }
        }

        MC4DModel.Twist twist = new MC4DModel.Twist(-1,-1,-1,false);
        int puzzleState[] = new int[model.genericPuzzleDescription.nStickers()];
        float fractionOfWayThroughTwist = (float)model.getAnimationState(modelListener,
                                                                puzzleState, // fills this
                                                                twist);      // fills this
        //System.out.println("fraction of way through twist = "+fractionOfWayThroughTwist);
        //System.out.println("twist = "+twist);


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
            if (!viewParams.frozenForDebugging.get())
            {
                viewParams.viewMat4d.set(newViewMat4d);
                //System.out.println("    "+viewState.iRotation+"/"+viewState.nRotation+" -> "+(viewState.iRotation+1)+"/"+viewState.nRotation+"");
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
                viewParams.viewMat3d.set(VecMath.mxm(viewParams.viewMat3d.get(), zeroOutRollAndTiltOnSpinDelta(viewState, viewState.spinDelta)));
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
            viewParams.faceShrink3d.get(),
            viewParams.stickerShrink3d.get(),
            viewParams.stickersShrinkTowardsFaceBoundaries.get(),

            twist.grip,
            twist.dir,
            twist.slicemask,
            twist.futtIfPossible,
            fractionOfWayThroughTwist,

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
            viewParams.topsortUsesBoldNewWay.get(),
            viewParams.showPartialOrder.get());

        if (true)  // TODO: make this a mode; it's useful either way
        {
            // Hack to make the shown partial order diagram freeze too
            if (viewParams.frozenForDebugging.get())
            {
                if (viewParams.frozenPartialOrderForDebugging != null)
                    frameToDrawInto.partialOrderInfo = viewParams.frozenPartialOrderForDebugging;
                else
                    viewParams.frozenPartialOrderForDebugging = frameToDrawInto.partialOrderInfo;
            }
        }

        // XXX figure out cleaner way to manage this
        float faceRGB[][] = new float[model.genericPuzzleDescription.nFaces()][3];
        for (int i = 0; i < faceRGB.length; ++i)
        {
            Color color = viewParams.faceColors[i%viewParams.faceColors.length].get();
            // color.getColorComponents(faceRGB[i]); // doesn't exist in 1.1
            {
                int rgb = color.getRGB();
                float red = ((rgb>>16)&255)/255.f;
                float green = ((rgb>>8)&255)/255.f;
                float blue = (rgb&255)/255.f;
                //System.out.println("r="+red+" g="+green+" b="+blue);
                faceRGB[i][0] = red;
                faceRGB[i][1] = green;
                faceRGB[i][2] = blue;
            }
        }

        if (frameToDrawInto.puzzleDescription != null)
            GenericPipelineUtils.paintFrame(
                frameToDrawInto,
                model.genericPuzzleState, // XXX what if model.puzzleDescription is out of sync with frame.puzzleDescription??
                viewParams.showShadows.get(),
                viewParams.drawGround.get() ? viewParams.groundColor.get() : null,
                faceRGB,
                viewState.iStickerUnderMouse,
                viewState.iPolyUnderMouse,
                viewParams.highlightByCubie.get(),
                !viewParams.highlightByCubie.get() && viewParams.highlightByGrip.get(), // XXX mess, see if I can make this cleaner
                viewParams.drawNonShrunkFaceOutlines.get() ? viewParams.nonShrunkFaceOutlineColor.get() : null,
                viewParams.drawShrunkFaceOutlines.get() ? viewParams.shrunkFaceOutlineColor.get() : null,
                viewParams.drawNonShrunkStickerOutlines.get() ? viewParams.nonShrunkStickerOutlineColor.get() : null,
                viewParams.drawShrunkStickerSurfaces.get(),
                viewParams.drawShrunkStickerOutlines.get() ? viewParams.shrunkStickerOutlineColor.get() : null,
                g,

                viewParams.jitterRadius.get(),
                viewParams.drawLabels.get(),
                viewParams.showPartialOrder.get());

        com.donhatchsw.awt.MyGraphics mg = new com.donhatchsw.awt.MyGraphics(g, viewSize, 0,W,H,0);

        ++viewState.nPaintsDone;
        if (viewParams.showNumPaintsDone.get())
        {
            g.setColor(java.awt.Color.BLACK);
            mg.drawString("("+viewState.nPaintsDone+" paint"+(viewState.nPaintsDone==1?"":"s")+")",
                          W-2., 2., 1., -1.);  // upper right of window
        }
        if (viewParams.frozenForDebugging.get()) {
            g.setColor(java.awt.Color.RED);
            mg.drawString("FROZEN FOR DEBUGGING", 0., 0., -1., -1.);  // upper left of window
            mg.drawString("(ctrl-alt-space to unfreeze)", 0., g.getFontMetrics().getHeight(), -1., -1.);
        }
        if (frameToDrawInto.cyclesSummary != null) {
            g.setColor(frameToDrawInto.cyclesSummary.contains("no cycles") ? java.awt.Color.GREEN : java.awt.Color.RED);
            mg.drawString(frameToDrawInto.cyclesSummary,
                          W, H, 1., 1.);  // lower right of window
        }
        if (viewParams.eventVerboseLevel.get() >= 3) System.out.println("            end painting on a "+view.getClass().getSuperclass().getName());
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
            CHECK(bestIFace != -1);
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

            CHECK(viewState.zeroRollPoleAfterRot3d != null); // initiateZeroRoll must have been called previously
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


    private static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == java.awt.event.MouseEvent.BUTTON2;
    }
    private static boolean isLeftMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == java.awt.event.MouseEvent.BUTTON1;
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
                //      6/8 ->  9/12   violet
                //      7/8 -> 5/6  magenta
                //      8/8 -> 6/6  red again
                final double perceptualHues[] = {
                    0/6.,   // red
                    1/12.,  //   orange
                    1/6.,   // yellow
                    2/6.,   // green
                    3/6.,   // cyan
                    4/6.,   // blue

                    //9/12.,   //   violet  (too magenta)
                    //17/24.,  //   violet  (too blue)
                    //35/48.,  //   violet  (still a bit too blue)
                    //71/96.,  //   violet (too magenta again)
                    141/192.,  //   violet (just right on my computer, probably sucks on everyone else's)

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
            CHECK(iFace >= 0);

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
    // Make a modern viewer based on a JComponent.
    //
    public static void makeExampleModernViewer(final MC4DModel model,
                                               final int x, final int y,
                                               final int w, final int h)
    {
        final MC4DViewGuts guts = new MC4DViewGuts();
        guts.setModel(model);
        @SuppressWarnings("serial")
        final JComponent myJComponent = new JComponent() {
            @Override public void paintComponent(Graphics g)
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
            // (note, it would also work to call requestFocus() in mouseEntered(), I believe)
            @Override public boolean isFocusable()
            {
                return true;
            }
        };
        guts.setControllerComponent(myJComponent, false);
        guts.setViewComponent(myJComponent);
        //myJComponent.setPreferredSize(new java.awt.Dimension(w,h)); // set size bottom up


        @SuppressWarnings("serial")
        JFrame jframe = new JFrame("A spiffy new JComponent") {{
            com.donhatchsw.awt.MainWindowCount.increment();
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent event)
                {
                    dispose();
                }
                @Override public void windowClosed(java.awt.event.WindowEvent event)
                {
                    System.out.println("Chow!");
                    guts.setModel(null);
                    guts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
                    guts.setViewComponent(null); // XXX make this not necessary, with weak ref I think
                    com.donhatchsw.awt.MainWindowCount.decrementAndExitIfImTheLastOne();
                }
            });
        }};

        jframe.setForeground(java.awt.Color.white);
        jframe.setBackground(java.awt.Color.black);

        jframe.setContentPane(myJComponent);

        //jframe.pack();
        jframe.setSize(w,h); // set size top down
        jframe.setLocation(x,y);
        jframe.setVisible(true);
        myJComponent.requestFocus(); // seems to be needed initially, if running in <=1.3 on linux, anyway.  weird!

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        myJComponent.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent ke)
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
        @SuppressWarnings("serial")
        final Canvas myCanvas = new Canvas() {
            private Image backBuffer = null;
            private int bbw=0, bbh=0;
            @Override public void update(Graphics g) { paint(g); } // don't flash
            @Override public void paint(Graphics frontBufferGraphics)
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
            // (note, it would also work to call requestFocus() in mouseEntered(), I believe)
            @Override public boolean isFocusable()
            {
                return true;
            }
        };
        guts.setControllerComponent(myCanvas, false);
        guts.setViewComponent(myCanvas);
        //myCanvas.setSize(new java.awt.Dimension(w,h)); // set size bottom up


        final Frame frame = new Frame("A sucky old Canvas");
        {
            com.donhatchsw.awt.MainWindowCount.increment();
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent we)
                {
                    frame.dispose();
                }
                @Override public void windowClosed(java.awt.event.WindowEvent we)
                {
                    System.out.println("ciao!!");
                    guts.setModel(null);
                    guts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
                    guts.setViewComponent(null); // XXX make this not necessary. with weak ref I think
                    com.donhatchsw.awt.MainWindowCount.decrementAndExitIfImTheLastOne();
                }
            });
        }

        frame.add(myCanvas);
        frame.pack();
        frame.setSize(w, h); // set size top down
        frame.setLocation(x,y);
        frame.setVisible(true);  // available in java 1.5, replaces deprecated show()

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        myCanvas.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent ke)
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

        boolean doDoubleBuffer = true; // set to false to make it even more sucky than necessary
        makeExampleAncientViewer(model, 350,50, 300,300, doDoubleBuffer);


        {
            // XXX dup code!!! figure out how to get it properly... and puzzle description might change!
            float faceRGB[][] = { {0, 0, 1}, {0.5f, 0, 0}, {.4f, 1, 1}, {1, 0, .5f}, {.9f, .5f, 1}, {1, .5f, 0}, {1, 1, .5f}, {0, 1, .5f}, };
            final java.awt.Color faceColor[] = new java.awt.Color[faceRGB.length];
            for (int i = 0; i < faceRGB.length; ++i)
                faceColor[i] = new java.awt.Color(faceRGB[i][0],faceRGB[i][1],faceRGB[i][2]);

            com.donhatchsw.util.UndoTreeSquirrel.ItemLengthizer lengthizer = new com.donhatchsw.util.UndoTreeSquirrel.ItemLengthizer() {
                // XXX this is duplicated in MC4DModel
                @Override public double length(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    CHECK(twist != null);
                    CHECK(twist.grip != -1);
                    int order = model.genericPuzzleDescription.getGripSymmetryOrders(twist.futtIfPossible)[twist.grip];
                    if (order <= 0)
                        return 1.; // XXX can this happen, and why?
                    double nQuarterTurns = 4./order
                                         * Math.abs(twist.dir); // power multiplier
                    return nQuarterTurns;
                }
            };
            com.donhatchsw.util.UndoTreeViewer.ItemColorizer colorizer = new com.donhatchsw.util.UndoTreeViewer.ItemColorizer() {
                @Override public java.awt.Color color(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    int grip = twist.grip;
                    CHECK(grip != -1);
                    int face = model.genericPuzzleDescription.getGrip2Face()[grip];
                    return faceColor[face % faceColor.length];
                }
                @Override public String leftLabel(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    int grip = twist.grip;
                    int order = model.genericPuzzleDescription.getGripSymmetryOrders(twist.futtIfPossible)[grip];
                    String degrees = "\u00B0"; // XXX not sure this magic works everywhere, got it from http://www.fileformat.info/info/unicode/char/00b0/index.htm

                    if (order <= 0)
                        return "WTF?"; // XXX can this happen, and why?
                    else
                        return ""+(360/order)+degrees; // XXX this does integer, is that okay?  don't want it to take forever
                }
                @Override public String rightLabel(Object item)
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
            com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer("Controller's view of the undo tree", model.controllerUndoTreeSquirrel, new com.donhatchsw.util.UndoTreeSquirrel[]{}, null, null, 500, 20, 350, 600,
                    // XXX oh gag
                    new int[1],
                    new int[]{1}, // nViewersAlive-- set this to a positive number so the viewer won't exit the program when it's closed (XXX in fact we could also use the same mechanism, that would be even better)
                    new int[1],
                    new int[1],
                    new int[1],
                    false, // don't allow the example "Do" which would put dummy strings in the tree
                    true, // but do allow undo/redo
                    false, // don't allow clear which would mess up everything currently (although maybe should hook up to reset at some time in the future)
                    lengthizer,
                    colorizer);

            // XXX need accessors for these instead of making them public I think
            controllerUndoTreeViewer.showLabels = false; // XXX need an accessor for this
            controllerUndoTreeViewer.centerCurrentNode.setTargetPosition(0.); // false


            com.donhatchsw.util.UndoTreeViewer animationUndoTreeViewer = 
            com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer("Animation's view of the undo tree", model.animationUndoTreeSquirrel, new com.donhatchsw.util.UndoTreeSquirrel[]{}, null, null, 850, 20, 350, 600,
                    // XXX oh gag
                    new int[1],
                    new int[]{1}, // nViewersAlive-- set this to a positive number so the viewer won't exit the program when it's closed (XXX in fact we could also use the same mechanism, that would be even better)
                    new int[1],
                    new int[1],
                    new int[1],
                    false, // don't allow the example "Do" which would put dummy strings in the tree
                    false, // and don't allow undo/redo from this view either, since instantaneous changes would make it get out of sync with the permutation array. undo/redo must be done from the controller window, this one is just for watching.
                    false, // don't allow clear which would mess up everything currently (although maybe should hook up to reset at some time in the future)
                    lengthizer,
                    colorizer);

            // XXX need accessors for these instead of making them public I think
            animationUndoTreeViewer.showLabels = false;
            animationUndoTreeViewer.centerCurrentNode.setTargetPosition(0.); // false
        }

        // release the main token
        com.donhatchsw.awt.MainWindowCount.decrementAndExitIfImTheLastOne();

    } // main

} // MC4DViewGuts



