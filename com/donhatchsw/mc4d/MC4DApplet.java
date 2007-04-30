package com.donhatchsw.mc4d;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import com.donhatchsw.awt.NewRow;
import com.donhatchsw.awt.NewCol;
import com.donhatchsw.awt.RowLayout;



public class MC4DApplet
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
    private final static String parameterInfo[][] = {
        {"puzzleDescription", "string", "puzzle description, e.g. \"{4,3,3} 3\""},
        {"x", "integer", "x position for spawned viewers"},
        {"y", "integer", "y position for spawned viewers"},
        {"w", "integer", "width of spawned viewers"},
        {"h", "integer", "height of spawned viewers"},
        {"doDoubleBuffer", "boolean", "whether to double buffer"},
    };
    public String[][] getParameterInfo()
    {
        return parameterInfo;
    }


    public MC4DApplet()
    {
        System.out.println("    in MC4DApplet ctor");
        System.out.println("    out MC4DApplet ctor");
    }

    public void init()
    {
        System.out.println("    in MC4DApplet init");

        com.donhatchsw.applet.AppletUtils.getParametersIntoPublicFields(this, 0);

        final MC4DViewGuts viewGuts = new MC4DViewGuts();
        viewGuts.setModel(new MC4DModel(puzzleDescription));

        //
        // Control panel window(s)
        //
        {
            int nControlPanelsAtStartup = 0; // can set this to more, to experiment... they should all stay in sync
            for (int i = 0; i < nControlPanelsAtStartup; ++i)
                makeNewControlPanelWindow(viewGuts);
        }

        //
        // Undo tree windows
        //
        {
            int nUndoTreeWindowsAtStartup = 0; // can manually set this to more, for debugging
            for (int i = 0; i < nUndoTreeWindowsAtStartup; ++i)
                makeNewUndoTreeWindow(viewGuts);
        }


        //
        // View canvas, that we'll stick inside something
        //
        final Canvas canvas = new Canvas() {
            private Image backBuffer = null;
            private Dimension backBufferSize = null;

            public void update(Graphics g) { paint(g); } // don't flash
            public void paint(Graphics frontBufferGraphics)
            {
                //System.out.println("in canvas paint");
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

                viewGuts.paint(this, g);

                g.setColor(Color.white);
                g.drawString("ctrl-n for another ancient view", 10, h-50);
                g.drawString("ctrl-s to save to the cookie", 10, h-30);
                g.drawString("ctrl-l to load from the cookie", 10, h-10);

                if (doDoubleBuffer)
                    frontBufferGraphics.drawImage(backBuffer, 0, 0, this);
                //System.out.println("out canvas paint");
            }
        };
        viewGuts.setControllerComponent(canvas, true);
        viewGuts.setViewComponent(canvas);

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
                            MC4DViewGuts.makeExampleModernViewer(viewGuts.model,x+20-w,y+20,w,h); // ctrl-shift-N
                        else
                            MC4DViewGuts.makeExampleAncientViewer(viewGuts.model,x+20,y+20,w,h,doDoubleBuffer);  // ctrl-n
                        break;
                    case 's'-'a'+1: // ctrl-s -- save to a cookie
                        com.donhatchsw.applet.CookieUtils.setCookie(MC4DApplet.this, "mc4dmodelstate", viewGuts.model.toString());
                        break;
                    case 'l'-'a'+1: // ctrl-l -- load from a cookie
                        String modelStateString = com.donhatchsw.applet.CookieUtils.getCookie(MC4DApplet.this, "mc4dmodelstate");
                        MC4DModel newModel = MC4DModel.fromString(modelStateString);
                        if (newModel != null)
                            viewGuts.setModel(newModel);
                        break;
                    default:
                        break;
                }
            }
        });

        // Define some on-the-fly convenience component classes...
        // XXX move these out, probably

        // A MenuBar has to be associated with a Frame?? wtf!?
        // I mean, how friggin hard is this???
        // All right, making my own that need not be associated with a frame
        class MyMenuBar
            extends NewRow
        {
            private int eventVerbose = 0;
            private boolean someMenuIsShowing = false; // XXX bleah! this isn't working right at all... how the hell can I tell when it pops up and down???
            public MyMenuBar()
            {
            }
            public void add(final String labelText, final PopupMenu menu)
            {
                add(new Label(labelText) {{
                    add(menu); // necessary, but doesn't really do anything except make the menu feel loved
                    addMouseListener(new MouseListener() {
                        public void mouseClicked(MouseEvent me)
                        {
                            if (eventVerbose >= 1) System.out.println("    "+labelText+": mouseClicked");
                        } // mouseClicked
                        public void mousePressed(MouseEvent me)
                        {
                            if (eventVerbose >= 1) System.out.println("    "+labelText+": mousePressed");
                            // XXX fooey, if another menu is up, we don't even get this event... unfriendly!  how does the real menubar fix this??
                            Component theLabel = me.getComponent();
                            menu.show(theLabel,
                                      0, theLabel.getHeight());
                            someMenuIsShowing = true;
                        } // mousePressed
                        public void mouseReleased(MouseEvent me)
                        {
                            if (eventVerbose >= 1) System.out.println("    "+labelText+": mouseReleased");
                        } // mouseReleased
                        private Color savedForeground = null;
                        public void mouseEntered(MouseEvent me)
                        {
                            if (eventVerbose >= 1) System.out.println("    "+labelText+": mouseEntered");
                            if (eventVerbose >= 2) System.out.println("        "+me);
                            if (savedForeground == null)
                                savedForeground = getForeground();
                            setForeground(Color.white);

                            if (me.getModifiers() != 0)
                            {
                                Component theLabel = me.getComponent();
                                menu.show(theLabel,
                                          0, theLabel.getHeight());
                            }
                        } // mouseEntered
                        public void mouseExited(MouseEvent me)
                        {
                            if (eventVerbose >= 1) System.out.println("    "+labelText+": mouseExited");
                            if (savedForeground != null)
                            {
                                setForeground(savedForeground);
                                savedForeground = null;
                            }
                        } // mouseExited
                    });
                }});
                // XXX how to tell when a menu has disappeared?  this isn't the way-- it only comes up in some of the cases of what can happen
                menu.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        System.out.println("action on a menu");
                        someMenuIsShowing = false;
                    }
                });
            }
        } // MyMenuBar

        // A MenuItem whose actionPerformed method gets called on action.
        // Just makes it so we don't have to call addActionListener every
        // friggin time we create a MenuItem.
        abstract class MyMenuItem
            extends MenuItem
            implements ActionListener
        {
            public MyMenuItem(String labelText)
            {
                super(labelText);
                addActionListener(this); // so my actionPerformed will get called
            }
        } // MyMenuItem

        //
        // Make a panel, containing the canvas
        // and a menu bar and maybe some other stuff, I don't know yet
        //
        Panel mainWindowPanel = new NewCol() {{

            setLayout(new BorderLayout());
            add("Center", canvas);
            add("North", new MyMenuBar() {{
                add("File", new PopupMenu() {{
                    add("Open...");
                    add("Save");
                    add("Save As...");
                    addSeparator();
                    add("Quit");
                }});
                add("Edit", new PopupMenu() {{
                    add(new MyMenuItem("Reset") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            com.donhatchsw.util.VecMath.copyvec(
                                viewGuts.model.genericPuzzleState,
                                viewGuts.model.genericPuzzleDescription.getSticker2Face());
                            viewGuts.model.controllerUndoTree.Clear();
                            canvas.repaint(); // XXX necessary?
                        }
                    });
                    add(new MyMenuItem("Undo") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            if (viewGuts.model.controllerUndoTree.undo() == null)
                                System.out.println("Nothing to undo.");
                            canvas.repaint(); // XXX necessary?
                        }
                    });
                    add(new MyMenuItem("Redo") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            if (viewGuts.model.controllerUndoTree.redo() == null)
                                System.out.println("Nothing to redo.");
                            canvas.repaint(); // XXX necessary?
                        }
                    });
                    addSeparator();
                    add(new MyMenuItem("Solve (cheat)") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            while (viewGuts.model.controllerUndoTree.undo() != null)
                                ;
                            canvas.repaint(); // XXX necessary?
                        }
                    });
                    add(new MyMenuItem("Solve (for real)") {
                        {setEnabled(false);}
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            System.out.println("Sorry, not smart enough for that.");
                        }
                    });
                }});
                add("Scramble", new PopupMenu() {{
                    for (int i = 1; i <= 8; ++i)
                    {
                        final int scramblechenfrengensen = i;
                        add(new MyMenuItem(""+i) {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                System.out.println("Scramble "+scramblechenfrengensen);
                                GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                                glue.scrambleAction(canvas, new Label(), scramblechenfrengensen);
                            }
                        });
                    }
                    addSeparator();
                    add(new MyMenuItem("Full") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            // XXX Maybe 6 times number of faces?  not sure
                            int scramblechenfrengensen = Math.random() < .5 ? 40 : 41;
                            System.out.println("Fully scrambling");
                            GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                            glue.scrambleAction(canvas, new Label(), scramblechenfrengensen);
                        }
                    });
                }});
                add("Windows", new PopupMenu() {{
                    add(new MyMenuItem("Control Panel") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            makeNewControlPanelWindow(viewGuts);
                        }
                    });
                    addSeparator();
                    add(new MyMenuItem("Undo Tree") {
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            makeNewUndoTreeWindow(viewGuts);
                        }
                    });
                    addSeparator();
                    add(new MyMenuItem("Cloned view of same model that follows this one") {
                        {setEnabled(false);}
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                        }
                    });
                    add(new MyMenuItem("Cloned view of same model") {
                        {setEnabled(false);}
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                        }
                    });
                    add(new MyMenuItem("Cloned view of cloned model") {
                        {setEnabled(false);}
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                        }
                    });
                    addSeparator();
                    add(new MyMenuItem("Macros") {
                        {setEnabled(false);}
                        public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                        }
                    });
                }});
                add("Puzzle", new PopupMenu() {{
                    final GenericGlue glue = new GenericGlue(viewGuts.model);  // XXX lame! need to not do this, make it call something more legit... glue needs to go away!
                    glue.addMoreItemsToPuzzleMenu(
                        this,
                        new Label("dum dum"),
                        new GenericGlue.Callback() {
                            public void call()
                            {
                                viewGuts.setModel(glue.model);
                            }
                        });
                }});
                add("Help", new PopupMenu() {{
                    add("About...");
                }});
                // XXX MyMenuBar should do this automatically-- what is the cleanest way?
                add(new Label(""),
                    new GridBagConstraints(){{fill=HORIZONTAL;weightx=1.;}}); // stretch
            }});
        }}; // mainWindowPanel

        // Add the canvas to myself
        setLayout(new BorderLayout());
        add(mainWindowPanel);

        System.out.println("    out MC4DApplet init");
    } // init

    public void start()
    {
        System.out.println("    in MC4DApplet start");
        System.out.println("    out MC4DApplet start");
    } // start
    public void stop()
    {
        System.out.println("    in MC4DApplet stop");
        System.out.println("    out MC4DApplet stop");
    } // stop
    public void destroy()
    {
        System.out.println("    in MC4DApplet destroy");
        System.out.println("    out MC4DApplet destroy");
    } // stop


    //
    // Common code...
    //
        private static void makeNewControlPanelWindow(MC4DViewGuts viewGuts)
        {
            final java.awt.Frame controlPanelFrame = new java.awt.Frame("MC4DControlPanel Test");
            // XXX the following is probably not what I want
            controlPanelFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    controlPanelFrame.dispose();
                    // no exit, this isn't a main window
                }
            });
            controlPanelFrame.add(new MC4DControlPanel(viewGuts.viewParams,
                                                       viewGuts.viewState));
            controlPanelFrame.pack();
            controlPanelFrame.show();
        } // makeNewControlPanelWindow

        private static void makeNewUndoTreeWindow(final MC4DViewGuts viewGuts)
        {
            // XXX dup code!!! figure out how to get it properly... and puzzle description might change!
            float faceRGB[][] = { {0, 0, 1}, {0.5f, 0, 0}, {.4f, 1, 1}, {1, 0, .5f}, {.9f, .5f, 1}, {1, .5f, 0}, {1, 1, .5f}, {0, 1, .5f}, };
            final java.awt.Color faceColor[] = new java.awt.Color[faceRGB.length];
            for (int i = 0; i < faceRGB.length; ++i)
                faceColor[i] = new java.awt.Color(faceRGB[i][0],faceRGB[i][1],faceRGB[i][2]);

            com.donhatchsw.util.UndoTreeSquirrel.ItemLengthizer lengthizer = new com.donhatchsw.util.UndoTreeSquirrel.ItemLengthizer() {
                // XXX this is duplicated in MC4DModel
                public double length(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    Assert(twist != null);
                    Assert(twist.grip != -1);
                    int order = viewGuts.model.genericPuzzleDescription.getGripSymmetryOrders()[twist.grip];
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
                    int face = viewGuts.model.genericPuzzleDescription.getGrip2Face()[grip];
                    return faceColor[face % faceColor.length];
                }
                public String leftLabel(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    int grip = twist.grip;
                    int order = viewGuts.model.genericPuzzleDescription.getGripSymmetryOrders()[grip];
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
            com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer("Controller's view of the undo tree", viewGuts.model.controllerUndoTree, null, null, 500, 20, 350, 600,
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
            com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer("Animation's view of the undo tree", viewGuts.model.animationUndoTree, null, null, 850, 20, 350, 600,
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
        } // makeNewUndoTreeWindow



    /**
    * Invoking this main is the same thing as invoking
    * the applet viewer's main with this class name as the first arg.
    * <p>
    * You can set the parameters on the command line,
    * e.g. puzzleDescription="{4,3,3} 3".
    * Currently the allowable params are:
    *     <ul>
    *         <li> puzzleDescription  (required)
    *         <li> x
    *         <li> y
    *         <li> w
    *         <li> h
    *         <li> doDoubleBuffer
    *     </ul>
    */
    public static void main(String args[])
    {
        // Check to make sure each of the command line args
        // is of the form param=value for some valid param name,
        // and that one of them is puzzleDescription.
        // XXX is there a way we can make AppletViewer do this automatically?
        {
            boolean foundPuzzleDescriptionArg = false;
            boolean foundBadArg = false;
            for (int iArg = 0; iArg < args.length; ++iArg)
            {
                boolean foundParamForThisArg = false;
                for (int iParam = 0; iParam < parameterInfo.length; ++iParam)
                    if (args[iArg].startsWith(parameterInfo[iParam][0]+"="))
                        foundParamForThisArg = true;
                if (!foundParamForThisArg)
                {
                    System.err.println();
                    System.err.println("MC4DApplet: ERROR: Unrecognized command line argument \""+args[iArg]+"\"");
                    foundBadArg = true;
                }
                if (args[iArg].startsWith("puzzleDescription="))
                    foundPuzzleDescriptionArg = true;
            }
            if (!foundPuzzleDescriptionArg || foundBadArg)
            {
                System.err.println();
                System.err.println("Usage: MC4DApplet puzzleDescription=\"<puzzleDescription>\" [<otherparam>=<othervalue> ... ]");
                System.err.println("Example: MC4DApplet puzzleDescription=\"{4,3,3} 3\"");
                System.exit(1);
            }
        }

        String appletViewerArgs[] = new String[args.length+1];
        appletViewerArgs[0] = "com.donhatchsw.mc4d.MC4DApplet";
        for (int i = 0; i < args.length; ++i)
            appletViewerArgs[i+1] = args[i];
        com.donhatchsw.applet.AppletViewer.main(appletViewerArgs);
    } // main

} // class MC4DApplet
