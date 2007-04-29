package com.donhatchsw.mc4d;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;



public class MC4DViewApplet
    extends Applet
{
    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    //
    // Note, all public fields are settable as params
    // from the web page (e.g. <PARAM NAME='puzzleDescription' VALUE='{4,3,3} 3'>)
    // or command line (e.g. "puzzleDescription='{4,3,3} 3'")
    //
    public String puzzleDescription = "{4,3,3} 3";
    public int x = 50, y = 50; // for spawned viewers
    public int w = 300, h = 300; // for spawned viewers
    public boolean doDoubleBuffer = true; // XXX get this from viewing params?

    Image backBuffer = null;
    private Dimension backBufferSize = null;

    public MC4DViewApplet()
    {
    }

    public void init()
    {
        //System.out.println("in MC4DViewApplet init");

        com.donhatchsw.applet.AppletUtils.getParametersIntoPublicFields(this, 0);

        final MC4DModel model = new MC4DModel(puzzleDescription);
        final MC4DViewGuts guts = new MC4DViewGuts();
        guts.setModel(model);

        //
        // Control panel window(s)
        //
        int nControlPanels = 2; // XXX just need 1, but can experiment to make sure they stay in sync
        for (int iControlPanel = 0; iControlPanel < nControlPanels; ++iControlPanel)
        {
            final java.awt.Frame controlPanelFrame = new java.awt.Frame("MC4DControlPanel Test");
            // XXX the following is probably not what I want
            controlPanelFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    controlPanelFrame.dispose();
                }
            });
            controlPanelFrame.add(new MC4DControlPanel(guts.viewParams,
                                                       guts.viewState));
            controlPanelFrame.pack();
            controlPanelFrame.show();
        }

        //
        // View window
        //
        Canvas canvas = new Canvas() {
            public void update(Graphics g) { paint(g); } // don't flash
            public void paint(Graphics frontBufferGraphics)
            {
                Dimension size = size();
                int w = size.width, h = size.height;

                if (doDoubleBuffer)
                {
                    if (backBuffer == null
                     || !size.equals(backBufferSize))
                    {
                        System.out.println("    creating back buffer of size "+w+"x"+h+"");
                        backBuffer = this.createImage(w, h);
                        backBufferSize = size;
                    }
                }
                else
                    backBuffer = null;
                Graphics g = doDoubleBuffer ? backBuffer.getGraphics() : frontBufferGraphics;

                guts.paint(this, g);

                g.setColor(Color.white);
                g.drawString("ctrl-n for another ancient view", 10, h-50);
                g.drawString("ctrl-s to save to the cookie", 10, h-30);
                g.drawString("ctrl-l to load from the cookie", 10, h-10);

                if (doDoubleBuffer)
                    frontBufferGraphics.drawImage(backBuffer, 0, 0, this);
            }
        };
        guts.setControllerComponent(canvas, true);
        guts.setViewComponent(canvas);

        // Make it so ctrl-n spawns another view of the same model,
        // and ctrl-shift-N spawns the opposite kind of view of the same model.
        canvas.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke)
            {
                char c = ke.getKeyChar();
                switch (c)
                {
                    case 'n'-'a'+1: // ctrl-n
                        if (ke.isShiftDown())
                            MC4DViewGuts.makeExampleModernViewer(model,x+20-w,y+20,w,h); // ctrl-shift-N
                        else
                            MC4DViewGuts.makeExampleAncientViewer(model,x+20,y+20,w,h,doDoubleBuffer);  // ctrl-n
                        break;
                    case 's'-'a'+1: // ctrl-s -- save to a cookie
                        com.donhatchsw.applet.CookieUtils.setCookie(MC4DViewApplet.this, "mc4dmodelstate", guts.model.toString());
                        break;
                    case 'l'-'a'+1: // ctrl-l -- load from a cookie
                        String modelStateString = com.donhatchsw.applet.CookieUtils.getCookie(MC4DViewApplet.this, "mc4dmodelstate");
                        MC4DModel model = MC4DModel.fromString(modelStateString);
                        if (model != null)
                            guts.setModel(model);
                        break;
                    default:
                        break;
                }
            }
        });

        setLayout(new BorderLayout());
        add("Center", canvas);

        //
        // Undo tree windows
        //
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

        //System.out.println("out MC4DViewApplet init");
    } // init

    /**
    * Invoking this main is the same thing as invoking
    * the applet viewer's main with this class name as the first arg.
    * You can set the parameters on the command line,
    * e.g. puzzleDescription="{4,3,3} 3"
    */
    public static void main(String args[])
    {
        String appletViewerArgs[] = new String[args.length+1];
        appletViewerArgs[0] = "com.donhatchsw.mc4d.MC4DViewApplet";
        for (int i = 0; i < args.length; ++i)
            appletViewerArgs[i+1] = args[i];

        com.donhatchsw.applet.AppletViewer.main(appletViewerArgs);
    } // main

} // class MC4DViewApplet
