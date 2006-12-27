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

package com.donhatchsw.MagicCube;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*; // XXX this needs to go in a different file

import com.donhatchsw.util.VecMath;

public class MC4DViewGuts
{
    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    //
    // Puzzle description and state...
    // shared by all views
    //
        //
        // All the generic puzzle description and puzzle state and stuff
        // ended up conveniently in here when I was trying
        // to write the generic stuff with minimal impact
        //
        private GenericGlue glue = null;

    //
    // Viewing parameters...
    // each view has its own set of these
    //
    static class PerViewState
    {
        public int eventVerboseLevel = 0;
            //     0: nothing
            //     1: key and mouse press/release/click
            //     2: and mouse drags
            //     3: and update/paint
            //     4: and enter/exit (can be obnoxious)
            //     5: and mouse motion (can be obnoxious)

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
        public float sunvec[] = {.82f, 1.55f, 3.3f};

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
        //public java.awt.Color outlineColor = java.awt.Color.black;
        public java.awt.Color outlineColor = null;
        public float twistFactor = 1.f;
        public boolean restrictRoll = false;
        public boolean spinDragRequiresCtrl = false;

        //
        // Mouse and keyboard state...
        //
        public int slicemask = 0; // bitmask representing which number keys are down
        public int lastDrag[] = null; // non-null == dragging
        public long lastDragTime = -1L; // timestamp of last drag event
        public float spinDelta[][] = null; // rotation to add for each frame while spinning. null == stopped
        public float dragDelta[][] = null; // while dragging, we keep track of the most recent drag delta, this is what will be turned into spinDelta when we let go.  Melinda's applet used spinDelta for both, but that made things complicated in paint when deciding whether to keep spinning or not when mouse was down, especially when combined with spinDragRequiresCtrl.

        private KeyListener keyListener;
        private MouseListener mouseListener;
        private MouseMotionListener mouseMotionListener;

        // To restore when we detach from this view
        private java.util.Vector savedKeyListeners;
        private java.util.Vector savedMouseListeners;
        private java.util.Vector savedMouseMotionListeners;
    } // class PerViewState

    //
    // Views
    //
    private java.util.Vector/*<Component>*/ views = new java.util.Vector();
    private java.util.Hashtable perViewStates/*<Component, PerViewState>*/ = new java.util.Hashtable();

    public int nViews()
    {
        return views.size();
    }

    //
    // Several of the Glue functions
    // want a Component they can call repaint() on.
    // We trick it by always passing in the next
    // view in the chain, so that all the views
    // eventually get updated.
    // XXX this was a quick hack that worked surprisingly well... but need to do it right.  probably they don't all make it to the end of the animation... and the rotate-to-center ends up all confused because it's only partway there at the end.
    //
    private Component viewAfter(Component prev)
    {
        int nViews = views.size();
        for (int i = 0; i < nViews; ++i)
            if (views.get(i) == prev)
            {
                //System.out.println(""+i+" -> "+((i+1)%nViews));
                return (Component)views.get((i+1)%nViews);
            }
        Assert(false);
        return null;
    }

    /** Detaches this view from these guts. */
    public void detachListeners(Component view)
    {
        PerViewState perViewState = (PerViewState)perViewStates.get(view);
        if (perViewState == null)
            return; // XXX maybe should throw an error

        view.removeKeyListener(perViewState.keyListener);
        view.removeMouseListener(perViewState.mouseListener);
        view.removeMouseMotionListener(perViewState.mouseMotionListener);
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

        if (suppressExistingListeners)
        {
            // XXX get the listeners into the saved listener lists
        }

        view.addKeyListener(perViewState.keyListener = new KeyListener() {
            public void keyPressed(KeyEvent ke)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("keyPressed");
            }
            public void keyReleased(KeyEvent ke)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("keyReleased");
            }
            public void keyTyped(KeyEvent ke)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("keyTyped");
            }
        });
        view.addMouseListener(perViewState.mouseListener = new MouseListener() {
            public void mouseClicked(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mouseClicked on a "+view.getClass().getSuperclass().getName());
                PerViewState perViewState = (PerViewState)perViewStates.get(view);
                Assert(perViewState != null); // should be no way to make this happen
                glue.mouseClickedAction(me,
                                        perViewState.viewMat4d,
                                        perViewState.twistFactor,
                                        perViewState.slicemask,
                                        view, // for view changes
                                        viewAfter(view)); // for model changes
            }
            public void mousePressed(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mousePressed on a "+view.getClass().getSuperclass().getName());
                perViewState.lastDrag = new int[]{me.getX(), me.getY()};
                perViewState.lastDragTime = me.getWhen();
                if (!(perViewState.spinDragRequiresCtrl && !me.isControlDown()))
                {
                    perViewState.spinDelta = null;
                    perViewState.dragDelta = null;
                }
            }
            public void mouseReleased(MouseEvent me)
            {
                long timedelta = me.getWhen() - perViewState.lastDragTime;
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mouseReleased on a "+view.getClass().getSuperclass().getName()+", time = "+me.getWhen()+", timedelta = "+timedelta);
                perViewState.lastDrag = null;
                perViewState.lastDragTime = -1L;
                if (!(perViewState.spinDragRequiresCtrl && !me.isControlDown()))
                {
                    if (timedelta == 0)
                    {
                        // Released at same time as previous drag-- lift off.
                        perViewState.spinDelta = perViewState.dragDelta;
                        view.repaint();
                    }
                    else
                    {
                        // Failed to lift off.
                        perViewState.spinDelta = null;
                        view.repaint(); // so it can use higher quality paint
                    }
                    perViewState.dragDelta = null; // no longer dragging
                }
            }
            public void mouseEntered(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 4) System.out.println("mouseExited on a "+view.getClass().getSuperclass().getName());
            }
            public void mouseExited(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 4) System.out.println("mouseExited on a "+view.getClass().getSuperclass().getName());
            }
        });
        // watch for dragging gestures to rotate the 3D view
        view.addMouseMotionListener(perViewState.mouseMotionListener = new MouseMotionListener() {
            public void mouseDragged(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 1) System.out.println("mouseDragged on a "+view.getClass().getSuperclass().getName()+", time = "+me.getWhen());
                if (perViewState.lastDrag == null)
                    return;
                int thisDrag[] = {me.getX(), me.getY()};
                if (!(perViewState.spinDragRequiresCtrl && !me.isControlDown()))
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
                            perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, glue.zeroOutRollOnSpinDelta(perViewState.dragDelta));
                        else
                            perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, perViewState.dragDelta);
                        if (pixelsMovedSqrd < 2*2)
                            perViewState.dragDelta = null;
                    }
                }
                perViewState.lastDrag = thisDrag;
                perViewState.lastDragTime = me.getWhen();
                System.out.println("calling repaint");
                view.repaint();
            }
            public void mouseMoved(MouseEvent me)
            {
                if (perViewState.eventVerboseLevel >= 5) System.out.println("        mouseMoved on a "+view.getClass().getSuperclass().getName());
                glue.mouseMovedAction(me,
                                      view); // this view, not the one after!
            }
        });

    } // attachListeners

    /** Constructor. */
    public MC4DViewGuts(GenericGlue glue)
    {
        this.glue = glue;
    }


    // PAINT
    void paint(Component view, Graphics g)
    {
        PerViewState perViewState = (PerViewState)perViewStates.get(view);
        if (perViewState == null)
            throw new IllegalArgumentException("MC4DViewGuts.paint called on a view.getClass().getSuperclass().getName() that it's not attached to!?");

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

        // XXX if model is animating, something...?

        if (perViewState.spinDelta != null) // note, the old applet had an additional test "and not dragging" but we don't need it because spinDelta is never set during dragging now, dragDelta is instead
        {
            if (perViewState.restrictRoll)
                perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, glue.zeroOutRollOnSpinDelta(perViewState.spinDelta));
            else
                perViewState.viewMat3d = VecMath.mxm(perViewState.viewMat3d, perViewState.spinDelta);
            view.repaint();
        }

        glue.computeAndPaintFrame(
          // used by compute part...
            perViewState.faceShrink,
            perViewState.stickerShrink,
            perViewState.viewMat4d,
            perViewState.eyeW,
            perViewState.viewMat3d,
            perViewState.eyeZ,
            perViewState.scale,
            pixels2polySF,
            xOff,
            yOff,
            perViewState.sunvec,

          // used by compute and paint part...
            perViewState.showShadows,

          // used by paint part only...
            perViewState.ground,
            perViewState.faceRGB,
            perViewState.highlightByCubie,
            !perViewState.highlightByCubie && glue.highlightByGrip,
            perViewState.outlineColor,
            g,
            perViewState.twistFactor,
            perViewState.restrictRoll,
            viewAfter(view));
        //glue.advanceAnimations(viewAfter(view)); // XXX ? what the fuck am I doing
    } // paint



    //
    // Make a modern viewer based on a JPanel.
    //
    public static void makeExampleModernViewer(final MC4DViewGuts guts,
                                               final int x, final int y,
                                               final int w, final int h)
    {
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
                if (guts.nViews() == 0)
                    System.exit(0); // asinine way of doing things
            }
        });

        jframe.pack();
        jframe.setSize(w,h); // set size top down
        jframe.setLocation(x,y);
        jframe.setVisible(true);

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        myPanel.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke)
            {
                char c = ke.getKeyChar();
                if (c == '\016')
                    if (ke.isShiftDown())
                        makeExampleAncientViewer(guts,x+20+w,y+20,w,h,false); // ctrl-shift-N
                    else
                        makeExampleModernViewer(guts,x+20,y+20,w,h);  // ctrl-n
            }
        });
    } // makeExampleModernViewer


    //
    // Make an ancient viewer based on a canvas.
    //
    public static void makeExampleAncientViewer(final MC4DViewGuts guts,
                                                final int x, final int y,
                                                final int w, final int h,
                                                final boolean doDoubleBuffer)
    {
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
                        if (guts.nViews() == 0)
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
                    if (guts.nViews() == 0)
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
                if (c == '\016')
                    if (ke.isShiftDown())
                        makeExampleModernViewer(guts,x+20-w,y+20,w,h); // ctrl-shift-N
                    else
                        makeExampleAncientViewer(guts,x+20,y+20,w,h,doDoubleBuffer);  // ctrl-n
            }
        });
    } // makeExampleAncientViewer



    public static void main(String args[])
    {
        if (args.length != 0 && args.length != 2)
        {
            System.err.println("Usage: MC4DViewGuts <schlafli> <length>");
            System.exit(1);
        }
        String schlafli = args.length > 0 ? args[0] : "{4,3,3}";
        int length = Integer.parseInt(args.length > 1 ? args[1] : "3");

        GenericGlue glue = new GenericGlue(schlafli, length);
        MC4DViewGuts guts = new MC4DViewGuts(glue);

        makeExampleModernViewer(guts, 50,50, 300,300);

        boolean doDoubleBuffer = true; // make it even more sucky than necessary
        makeExampleAncientViewer(guts, 350,50, 300,300, doDoubleBuffer);

    } // main

} // MC4DViewGuts



