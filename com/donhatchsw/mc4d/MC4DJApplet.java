// TODO: make the initial window fully swing (it's currently based on AppletViewer which puts it in a frame)
// TODO: make the control panels swing
// TODO: @Overrides everywhere  (TODO: can I automatically detect violations?)
package com.donhatchsw.mc4d;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.donhatchsw.awt.Row;
import com.donhatchsw.awt.Col;
import com.donhatchsw.awt.RowLayout;



@SuppressWarnings("serial")
public class MC4DJApplet
    extends com.donhatchsw.shims_for_deprecated.javax_swing_JApplet
{
    static private void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }

    //
    // Note, all public fields are settable as params
    // from the web page (e.g. <PARAM NAME='puzzleDescription' VALUE='{4,3,3} 3'>)
    // or command line (e.g. "puzzleDescription='{4,3,3} 3'")
    //
    public String puzzleDescription = "{4,3,3} 3";
    public int x = 50, y = 50; // for spawned viewers
    public int w = 300, h = 300; // for spawned viewers
    public boolean doDoubleBuffer = true; // XXX get this from viewing params? currently this must match viewing params' default value
    public boolean futtIfPossible = false; // XXX get this from viewing params? currently this must match viewing params' default value
    public boolean forceFuttableXXX = false;
    public int nControlPanelsAtStartup = 0; // can set this to more, to experiment... they should all stay in sync
    public String modelStateString = null;
    private final static String parameterInfo[][] = {
        {"puzzleDescription", "string", "puzzle description, e.g. \"{4,3,3} 3\""},
        {"x", "integer", "x position for initial and spawned viewers"},  // XXX does this work for spawned?
        {"y", "integer", "y position for initial and spawned viewers"},  // XXX does this work for spawned?
        {"w", "integer", "width of initial and spawned viewers"},  // XXX does this work for spawned?
        {"h", "integer", "height of initial and spawned viewers"},  // XXX does this work for spawned?
        {"doDoubleBuffer", "boolean", "whether to double buffer"},
        {"futtIfPossible", "boolean", "whether to try to futt (i.e. allow topologically valid twists that may require morphing)"},
        {"forceFuttableXXX", "boolean", "whether to force puzzle to think it's futtable.  for development."},
        {"nControlPanelsAtStartup", "integer", "number of control panels to open at startup.  they should all stay in sync.  even numbered are swing; odd numbered are legacy."},
        {"modelStateString", "string", "full description of puzzle / state / history, as previously dumped by \"Test to/from string\""},
    };
    public String[][] getParameterInfo()  // XXX TODO: no one ever uses this??
    {
        return parameterInfo;
    }


    public MC4DJApplet()
    {
        System.out.println("    in MC4DJApplet ctor");
        System.out.println("        java version " + System.getProperty("java.version"));

        if (false)
        {
            // Experiment with to redirecting System.out and System.err.
            // In a browser, it throws this, which is understandable:
/*
    java.security.AccessControlException: access denied (java.lang.RuntimePermission setIO)
        at java.security.AccessControlContext.checkPermission(AccessControlContext.java:323)
        at java.security.AccessController.checkPermission(AccessController.java:546)
        at java.lang.SecurityManager.checkPermission(SecurityManager.java:532)
        at java.lang.System.checkIO(System.java:226)
        at java.lang.System.setOut(System.java:148)
        at com.donhatchsw.mc4d.MC4DJApplet.<init>(MC4DJApplet.java:59)
*/

            // So.. should we do this if not in a browser?
            // and how do we tell?  Just try and give up
            // if it throws an exception?
            {
                final java.io.PrintStream origOut = System.out;
                java.io.PrintStream newOut = new java.io.PrintStream(new java.io.OutputStream() {
                    @Override public void write(int b) throws java.io.IOException {
                        origOut.print("["+(char)b+"]");
                        origOut.flush();
                    }
                });
                System.setOut(newOut);
            }
            {
                final java.io.PrintStream origErr = System.err;
                java.io.PrintStream newErr = new java.io.PrintStream(new java.io.OutputStream() {
                    @Override public void write(int b) throws java.io.IOException {
                        origErr.print("{"+(char)b+"}");
                        origErr.flush();
                    }
                });
                System.setErr(newErr);
            }
        }

        System.out.println("    out MC4DJApplet ctor");
    }

    private static Component makeNewMC4DViewCanvas(final MC4DViewGuts viewGuts,
                                                final boolean doDoubleBuffer,
                                                final Component menuBarForWidth[/*1*/],
                                                final PuzzlesAndWindows allPuzzlesAndWindows) // XXX should really be local to this view window so we can change it I think
    {
        final JComponent canvas = new JComponent() {
            @Override public void paintComponent(Graphics g)
            {
                viewGuts.paint(this, g);
            }
            // XXX lame hack... how should I really make the canvas square and same width as menu bar?
            @Override public Dimension getPreferredSize()
            {
                if (menuBarForWidth != null)
                {
                    Dimension menuBarPreferredSize = menuBarForWidth[0].getPreferredSize();
                    return new Dimension(menuBarPreferredSize.width,
                                         menuBarPreferredSize.width); // width, not height.  so, square
                    // XXX or can I reuse it?  seems like everyone's being overly cautious
                }
                else
                    return super.getPreferredSize();
            }

            // So we can type immediately in it
            // (note, it would also work to call requestFocus() in mouseEntered(), I believe)
            @Override public boolean isFocusable()
            {
                return true;
            }
        };

        viewGuts.setControllerComponent(canvas, true);
        viewGuts.setViewComponent(canvas);

        canvas.addKeyListener(new java.awt.event.KeyListener() {
            @Override public void keyPressed(KeyEvent ke)
            {
                if (ke.isAltDown())
                {
                    // ViewGuts has a lot of secret key combinations
                    // involving ctrl-alt, e.g. ctrl-alt-t,
                    // so we need to be careful not to think it's ctrl-t,
                    // which we will if we don't make sure alt isn't down.
                    // In fact we don't want to do any of the stuff below
                    // if alt is down, so instead of checking that everywhere,
                    // just return early without doing anything if it's down.
                    return;
                }

                char c = ke.getKeyChar();

                // In java 1.6, apparently ctrl-letter
                // started coming out as just the letter
                // (with ke.isControlDown() true).
                // Detect this and change it to the old behavior...
                // (actually I think it was just some early 1.6's, maybe)
                // XXX need to do something else, or nothing at all here, for old javas (1.1) in which isControlDown doesn't exist... or else just stop trying to support 1.1 at all
                if (c >= 'a' && c <= 'z' && ke.isControlDown())
                {
                    c -= ('a'-1);
                }

                if (false) {}
                else if (c == 'r'-'a'+1 // ctrl-r -- reset
                      || c == 'c')
                {
                    // XXX duplicated code in menu and key... should be model method
                    com.donhatchsw.util.VecMath.copyvec(
                        viewGuts.model.genericPuzzleState,
                        viewGuts.model.genericPuzzleDescription.getSticker2Face());
                    viewGuts.model.controllerUndoTreeSquirrel.Clear();
                    viewGuts.model.animationUndoTreeSquirrel.setCurrentNodeIndex(viewGuts.model.controllerUndoTreeSquirrel.getCurrentNodeIndex());
                    canvas.repaint();
                }
                else if (c == 'z'-'a'+1 // ctrl-z -- undo
                      || c == 'u')      // u -- undo
                {
                    if (viewGuts.model.controllerUndoTreeSquirrel.undo() == null)
                        System.out.println("Nothing to undo.");
                }
                else if (c == 'y'-'a'+1 // ctrl-y -- redo
                      || c == 'U'       // U -- redo
                      || c == 'r')      // U -- redo
                {
                    if (viewGuts.model.controllerUndoTreeSquirrel.redo() == null)
                        System.out.println("Nothing to redo.");
                }
                else if (c == 't'-'a'+1) // ctrl-t -- cheat
                {
                    while (viewGuts.model.controllerUndoTreeSquirrel.undo() != null)
                        ;
                }
                else if (c >= '1' && c <= '9' // ctrl-1 .. ctrl-9 -- scramble
                      && ke.isControlDown()) // XXX need to use something else for old javas
                {
                    int scramblechenfrengensen = c - '0';
                    System.out.println("Scramble "+scramblechenfrengensen);
                    GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                    glue.scrambleAction(canvas, new Label(), scramblechenfrengensen, viewGuts.viewParams.futtIfPossible.get());
                }
                else if (c == 'f'-'a'+1) // ctrl-f -- full scramble
                {
                    // XXX dup code in menu
                    // XXX Maybe 6 times number of faces?  not sure
                    int scramblechenfrengensen = Math.random() < .5 ? 40 : 41;
                    System.out.println("Fully scrambling");
                    GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                    glue.scrambleAction(canvas, new Label(), scramblechenfrengensen, viewGuts.viewParams.futtIfPossible.get());
                }
                else if (c == 'c'-'a'+1) // ctrl-c -- new control panel window
                {
                    openOrMakeNewControlPanelWindow(viewGuts,
                                                    allPuzzlesAndWindows);
                }
                else if (c == 'u'-'a'+1) // ctrl-u -- new undo tree window
                {
                    makeNewUndoTreeWindow(viewGuts);
                }
                else
                {
                }
            }
            @Override public void keyTyped(KeyEvent ke)
            {
            }
            @Override public void keyReleased(KeyEvent ke)
            {
            }
        });

        return canvas;
    } // makeNewMC4DViewCanvas

        // Define some on-the-fly convenience component classes...
        // XXX move these out into awt, maybe?

        // A JMenuItem whose actionPerformed method gets called on action.
        // Just makes it so we don't have to call addActionListener every
        // friggin time we create a MenuItem.
        @SuppressWarnings("serial")
        private static abstract class MyJMenuItem
            extends JMenuItem
            implements ActionListener
        {
            public MyJMenuItem(String labelText)
            {
                super(labelText);
                addActionListener(this); // so my actionPerformed will get called
            }
        } // MyJMenuItem

    // new menu bar and new view canvas, inside a new panel.
    // TODO: the panel really serves no purpose any more; this is just returning a canvas,menubar pair
    @SuppressWarnings("serial")
    private static class MC4DViewerPanel
        extends JPanel
    {
        private String name;
        @Override public String getName()
        {
            return name;
        }
        private MC4DViewGuts viewGuts;
        public MC4DViewGuts getViewGuts()
        {
            return viewGuts;
        }

        public MC4DViewerPanel(final String name,
                               final MC4DViewGuts viewGuts,
                               final boolean doDoubleBuffer,
                               final com.donhatchsw.shims_for_deprecated.javax_swing_JApplet applet, // for context for cookie
                               final PuzzlesAndWindows allPuzzlesAndWindows) // for save
        {
            this.name = name;
            this.viewGuts = viewGuts;

            allPuzzlesAndWindows.addViewerPanel(this);

            Component menuBarHolder[] = new Component[1]; // so that the canvas can access the menuBar later when it needs to for getPreferredSize, even though we haven't created the menu bar yet
            final Component canvas = makeNewMC4DViewCanvas(viewGuts,
                                                           doDoubleBuffer,
                                                           menuBarHolder, // canvas wants to be square and same size as menu bar
                                                           allPuzzlesAndWindows);

            Component menuBar = new JMenuBar() {{
                add(new JMenu("File") {{
                    add(new MyJMenuItem("Save to browser cookie") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            com.donhatchsw.applet.CookieUtils.setCookie(applet, "mc4dmodelstate", viewGuts.model.toString());
                        }
                    });
                    add(new MyJMenuItem("Save to browser cookie #2") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            com.donhatchsw.applet.CookieUtils.setCookie(applet, "mc4dmodelstate2", viewGuts.model.toString());
                        }
                    });
                    add(new MyJMenuItem("Load from browser cookie") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            String modelStateString = com.donhatchsw.applet.CookieUtils.getCookie(applet, "mc4dmodelstate");
                            MC4DModel newModel = MC4DModel.fromString(modelStateString);
                            if (newModel != null)
                                viewGuts.setModel(newModel);
                        }
                    });
                    add(new MyJMenuItem("Load from browser cookie #2") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            String modelStateString = com.donhatchsw.applet.CookieUtils.getCookie(applet, "mc4dmodelstate2");
                            MC4DModel newModel = MC4DModel.fromString(modelStateString);
                            if (newModel != null)
                                viewGuts.setModel(newModel);
                        }
                    });
                    if (true)
                        add(new MyJMenuItem("Test to/from string") {
                          @Override public void actionPerformed(java.awt.event.ActionEvent e)
                          {
                              MC4DModel m0 = viewGuts.model;
                              String s1 = m0.toString();
                              System.out.println("model = "+s1);
                              MC4DModel m2 = MC4DModel.fromString(s1);
                              String s3 = m2.toString();
                              CHECK(s3.equals(s1));
                              System.out.println("Good!");
                              viewGuts.setModel(m2);
                          }
                        });
                    addSeparator();
                    add(new MyJMenuItem("Experimental print app to terminal") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            System.out.println(allPuzzlesAndWindows.toString());
                        }
                    });
                    add(new MyJMenuItem("Debug dump ui component hierarchies") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            allPuzzlesAndWindows.dumpComponentHierarchies();
                        }
                    });
                    addSeparator();
                    add("Quit");  // TODO: does nothing.  get rid?  what should it do?
                }});
                add(new JMenu("Edit") {{
                    add(new MyJMenuItem("Reset            Ctrl-R") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            // XXX duplicated code in menu and key... should be model method
                            com.donhatchsw.util.VecMath.copyvec(
                                viewGuts.model.genericPuzzleState,
                                viewGuts.model.genericPuzzleDescription.getSticker2Face());
                            viewGuts.model.controllerUndoTreeSquirrel.Clear();
                            viewGuts.model.animationUndoTreeSquirrel.setCurrentNodeIndex(viewGuts.model.controllerUndoTreeSquirrel.getCurrentNodeIndex());
                            canvas.repaint();
                        }
                    });
                    add(new MyJMenuItem("Undo             Ctrl-Z") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            if (viewGuts.model.controllerUndoTreeSquirrel.undo() == null)
                                System.out.println("Nothing to undo.");
                        }
                    });
                    add(new MyJMenuItem("Redo             Ctrl-Y") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            if (viewGuts.model.controllerUndoTreeSquirrel.redo() == null)
                                System.out.println("Nothing to redo.");
                        }
                    });
                    addSeparator();
                    add(new MyJMenuItem("Solve (cheat)  Ctrl-T") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            while (viewGuts.model.controllerUndoTreeSquirrel.undo() != null)
                                ;
                        }
                    });
                    add(new MyJMenuItem("Solve (for real)") {
                        {setEnabled(false);}
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            System.out.println("Sorry, not smart enough for that.");
                        }
                    });
                }});
                add(new JMenu("Scramble") {{
                    for (int i = 1; i <= 8; ++i)
                    {
                        final int scramblechenfrengensen = i;
                        add(new MyJMenuItem(""+i+"      Ctrl-"+i) {
                            @Override public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                System.out.println("Scramble "+scramblechenfrengensen);
                                GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                                glue.scrambleAction(canvas, new Label(), scramblechenfrengensen, viewGuts.viewParams.futtIfPossible.get());
                            }
                        });
                    }
                    addSeparator();
                    add(new MyJMenuItem("Full   Ctrl-F") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            // XXX dup code in key listener
                            // XXX Maybe 6 times number of faces?  not sure
                            int scramblechenfrengensen = Math.random() < .5 ? 40 : 41;
                            System.out.println("Fully scrambling");
                            GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                            glue.scrambleAction(canvas, new Label(), scramblechenfrengensen, viewGuts.viewParams.futtIfPossible.get());
                        }
                    });
                }});
                add(new JMenu("Puzzle") {{
                    final GenericGlue glue = new GenericGlue(viewGuts.model);  // XXX lame! need to not do this, make it call something more legit... glue needs to go away!
                    glue.addMoreItemsToPuzzleMenu(
                        this,
                        new Label("dum dum"),
                        new GenericGlue.Callback() {
                            @Override public void call()
                            {
                                viewGuts.setModel(glue.model);
                            }
                        });
                }});
                add(new JMenu("Windows") {{
                    add(new MyJMenuItem("Control Panel                       Ctrl-C") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            openOrMakeNewControlPanelWindow(viewGuts,
                                                            allPuzzlesAndWindows);
                        }
                    });
                    add(new MyJMenuItem("Expert Control Panel") {
                        {setEnabled(false);}
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            // XXX implement me
                        }
                    });
                    addSeparator();
                    add(new MyJMenuItem("Macros") {
                        {setEnabled(false);}
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                        }
                    });
                    addSeparator();
                    add(new MyJMenuItem("Undo Tree                           Ctrl-U") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            makeNewUndoTreeWindow(viewGuts);
                        }
                    });
                    addSeparator();
                    add(new MyJMenuItem("Shared view of shared puzzle state") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            makeNewViewWindow(viewGuts,
                                              false, // don't clone view, share it
                                              false, // don't clone puzzle state, share it
                                              doDoubleBuffer,
                                              applet,
                                              allPuzzlesAndWindows);
                        }
                    });
                    if (false) // this is a weird one, I don't know if it's useful
                        add(new MyJMenuItem("Shared view of cloned puzzle state") {
                            @Override public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                makeNewViewWindow(viewGuts,
                                                  false, // don't clone view, share it
                                                  true, // clone puzzle state
                                                  doDoubleBuffer,
                                                  applet,
                                                  allPuzzlesAndWindows);
                            }
                        });
                    add(new MyJMenuItem("Cloned view of shared puzzle state ") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            makeNewViewWindow(viewGuts,
                                              true, // clone view
                                              false, // don't clone puzzle state, share it
                                              doDoubleBuffer,
                                              applet,
                                              allPuzzlesAndWindows);
                        }
                    });
                    add(new MyJMenuItem("Cloned view of cloned puzzle state ") {
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                            makeNewViewWindow(viewGuts,
                                              true, // clone view
                                              true, // clone puzzle state
                                              doDoubleBuffer,
                                              applet,
                                              allPuzzlesAndWindows);
                        }
                    });
                    addSeparator();
                    add(new MyJMenuItem("Progress/diagnostics/debug") {
                        {setEnabled(false);}
                        @Override public void actionPerformed(java.awt.event.ActionEvent e)
                        {
                        }
                    });
                }});
                add(new JMenu("Help") {{
                    add("About...");
                }});
            }};  // menuBar

            menuBarHolder[0] = menuBar;

            this.setLayout(new BorderLayout());
            if (menuBar != null)
                this.add("North", menuBar);
            this.add("Center", canvas);
        } // MC4DViewerPanel ctor
    } // class MC4DViewerPanel


    // This gets called when spawning a new window... not the first one.
    private static void makeNewViewWindow(final MC4DViewGuts oldViewGuts,
                                          final boolean cloneView,
                                          final boolean cloneState,
                                          final boolean doDoubleBuffer,
                                          final com.donhatchsw.shims_for_deprecated.javax_swing_JApplet applet,
                                          final PuzzlesAndWindows allPuzzlesAndWindows)
    {
        final MC4DViewGuts newViewGuts = new MC4DViewGuts(oldViewGuts.viewParams,
                                                          cloneView);
        if (cloneState)
        {
            newViewGuts.setModel(new MC4DModel(oldViewGuts.model.genericPuzzleDescription) {{
                this.controllerUndoTreeSquirrel = new com.donhatchsw.util.UndoTreeSquirrel(oldViewGuts.model.controllerUndoTreeSquirrel); // same tree
                this.animationUndoTreeSquirrel = new com.donhatchsw.util.UndoTreeSquirrel(this.controllerUndoTreeSquirrel);
            }});
        }
        else
        {
            newViewGuts.setModel(oldViewGuts.model);
        }

        final String viewName = "View "+(allPuzzlesAndWindows.nextViewerNumber++);
        JFrame frame = new JFrame() {{
            add(new MC4DViewerPanel(viewName,
                                    newViewGuts,  
                                    doDoubleBuffer,
                                    applet,
                                    allPuzzlesAndWindows));
            pack();

            com.donhatchsw.awt.MainWindowCount.increment();
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent we)
                {
                    System.out.println("in windowClosing from makeNewViewWindow");
                    dispose();
                }

                // ARGH! this gets called twice when in browswer:
                // once when user closes it (due to the dispose() above)
                // then again when applet is destroyed.  So,
                // we keep track of whether we are already closed.
                private boolean closedAlreadyYouMoron = false;
                @Override public void windowClosed(java.awt.event.WindowEvent we)
                {
                    if (!closedAlreadyYouMoron)
                    {
                        System.out.println("ciao!");
                        newViewGuts.setModel(null);
                        newViewGuts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
                        newViewGuts.setViewComponent(null); // XXX make this not necessary. with weak ref I think
                        com.donhatchsw.awt.MainWindowCount.decrementAndExitIfImTheLastOne();
                        closedAlreadyYouMoron = true;
                    }
                    else
                    {
                        System.out.println("Got duplicate window close event, sigh.");
                    }
                }
            }); // addWindowListener
        }}; // new Frame

        frame.setTitle("MC4D "+viewName);
        frame.setVisible(true);  // available in java 1.5, replaces deprecated show()
    } // makeNewViewWindow

    //
    // The applet state's toString is a dump of the entire
    // applet state including all window positions.
    // It is suitable for saving to and loading from a file or cookie.
    //
    /*
        Example of what I think it will look like:

        applet = {
            puzzleDescriptionsAndUndoTrees = {
                {genericPuzzleDescription = new PolytopePuzzleDescription("{4,3,3} 3"), undoTree={233 343 624 384 923 923}},
                {genericPuzzleDescription = new PolytopePuzzleDescription("{5,3,3} 3"), undoTree={233 343 624 384 923 923}},
            }
            macros = {
                # Heirarchical tree, they can arrange however
                {
                    "My Solving Macros For Hypercube",
                    "{4,3,3} 3",
                    {
                        "Cross", {123,123,233},{433,655,345,56,7,4454,5456}
                        "DoubleCross", {123,123,233},{433,655,"Cross"(345,456,567),56,7,"Cross",5456}
                    }
                },
                {
                    "Pretty Patterns",
                },
            },
            controlPanelWindows = {
                {
                    name = "Control Panel 0",
                    windowState = closed@100x100+20+20,
                    viewParams = {twistDuration=10,bounce=20,...}
                },
                {
                    name = "Control Panel 1",
                    windowState = open@100x100+20+20,
                    viewParams = {twistDuration=10,bounce=20,...},
                },
                {
                    name = "Control Panel 2",
                    windowState = open@100x100+20+20,
                    viewParams = {twistDuration=10,bounce=20,...},
                },
            }
            undoTreeWindows = {
                {
                    name = "Animation Undo Tree Squirrel 0",
                    windowState = applet@100x100+20+20,
                    undoTree = "{4,3,3} 3"
                    undoTreePos=3
                    puzzleState={0,0,0,1,1,1,2,2,2,...}
                },
                {
                    name = "Animation Undo Tree Squirrel 1",
                    ...
                }
            }
            viewerWindows = {
                {
                    name = "View 0",
                    windowState = applet@100x100+20+20,
                    controlPanelWindowName = "Control Panel 0"
                    undoTreeWindowName = "Animation Undo Tree Squirrel 0"
                },
                {
                    name = "View 1 left",
                    windowState = open@400x300+70+32
                    controlPanelWindowName = "Control Panel 1"
                    undoTreeWindowName = "Animation Undo Tree Squirrel 1"
                },
                {
                    name = "View 1 right",
                    windowState = open@400x300+70+32
                    controlPanelWindowName = "Control Panel 1"
                    undoTreeWindowName = "Animation Undo Tree Squirrel 1"
                }
            }
            nextViewerNumber = 2,
            nextControlPanelNumber = 3,
            nextUndoTreeWindowNumber = 3,
        }
    */



    private static class PuzzlesAndWindows
    {
        private static class NamedObject
        {
            String name;
            Object object;
        }

        //public java.util.ArrayList puzzleDescriptionsAndUndoTrees = new java.util.ArrayList();
        private java.util.ArrayList<MC4DViewerPanel> viewerPanels = new java.util.ArrayList<MC4DViewerPanel>();
        private java.util.ArrayList<Component> controlPanels = new java.util.ArrayList<Component>();
        public java.util.ArrayList<Component> undoTreeSquirrelPanels = new java.util.ArrayList<Component>();
        public int nextViewerNumber = 0;
        public int nextControlPanelNumber = 0;
        public int nextUndoTreeWindowNumber = 0;

        private String windowStateToString(Component component)
        {
            Component frameOrApplet = getTopLevelFrameOrApplet(component);
            java.awt.Rectangle bounds = frameOrApplet.getBounds();
            String s = (frameOrApplet instanceof com.donhatchsw.shims_for_deprecated.javax_swing_JApplet ? "applet" :
                        !frameOrApplet.isVisible() ? "closed" :
                        ((Frame)frameOrApplet).getState() == Frame.ICONIFIED ? "iconified" : "open");
            s += "@" + bounds.width
               + "x" + bounds.height
               + "+" + bounds.x
               + "+" + bounds.y;
            return s;
        }

        // XXX should use a hash table, probably
        private Component findFirstControlPanelOfViewParams(MC4DViewGuts.ViewParams viewParams)
        {
            int n = controlPanels.size();
            for (int i = 0; i < n; ++i)
            {
                Component controlPanel = controlPanels.get(i);
                if (((MC4DControlPanelInterface)controlPanel).getViewParams() == viewParams)
                    return controlPanel;
            }
            return null;
        } // findFirstControlPanelOfViewParams
        private Component findUndoTreeSquirrelPanelOfSquirrel(com.donhatchsw.util.UndoTreeSquirrel squirrel)
        {
            int n = undoTreeSquirrelPanels.size();
            for (int i = 0; i < n; ++i)
            {
                CHECK(false); // XXX do me
            }
            return null;
        } // findUndoTreeSquirrelPanelOfSquirrel

        public void addControlPanel(Component controlPanel)
        {
            {
                // Make sure name doesn't exist...
                String name = controlPanel.getName();
                int n = controlPanels.size();
                for (int i = 0; i < n; ++i)
                    if ((controlPanels.get(i)).getName().equals(name))
                    {
                        throw new IllegalStateException("Tried to add a control panel named "+com.donhatchsw.util.Arrays.toStringCompact(name)+" but there is already one with that name!?");
                    }
            }
            controlPanels.add(controlPanel);
            updateControlPanelWindowTitles();
        }
        public void addViewerPanel(MC4DViewerPanel viewerPanel)
        {
            {
                // Make sure name doesn't exist...
                String name = viewerPanel.getName();
                int n = viewerPanels.size();
                for (int i = 0; i < n; ++i)
                    if ((viewerPanels.get(i)).getName().equals(name))
                    {
                        throw new IllegalStateException("Tried to add a viewer panel named "+com.donhatchsw.util.Arrays.toStringCompact(name)+" but there is already one with that name!?");
                    }
            }
            viewerPanels.add(viewerPanel);
            updateControlPanelWindowTitles();
        }

        // Turn "View 1,View 2" into "Views 1,2".
        // Doesn't necessarily complete in one application-- run it
        // until length doesn't change.
        private final static java.util.regex.Pattern viewStringCompressionPattern = java.util.regex.Pattern.compile("Views? (\\d+(,\\d+)*),\\s*Views? (\\d+(,\\d+)*)");
        private final static String viewStringCompressionReplacement = "Views $1,$3";

        // Title will be something like "MC4D Control Panel for Views 0,1,47)"
        private void updateControlPanelWindowTitles()
        {
            int nControlPanels = controlPanels.size();
            int nViewerPanels = viewerPanels.size();
            for (int iControlPanel = 0; iControlPanel < nControlPanels; ++iControlPanel)
            {
                Component controlPanel = controlPanels.get(iControlPanel);
                String title = controlPanel instanceof MC4DLegacyControlPanel ? "MC4D Legacy Control Panel for "
                                                                              : "MC4D Control Panel for ";
                int nViewsFound = 0;
                for (int iViewerPanel = 0; iViewerPanel < nViewerPanels; ++iViewerPanel)
                {
                    MC4DViewerPanel viewerPanel = viewerPanels.get(iViewerPanel);
                    if (viewerPanel.getViewGuts().viewParams
                     == ((MC4DControlPanelInterface)controlPanel).getViewParams())
                    {
                        if (nViewsFound > 0)
                            title += ",";
                        title += viewerPanel.getName();
                        nViewsFound++;
                    }
                }
                if (nViewsFound == 0)
                {
                    title = title.substring(0,title.length()-3);
                    title += " (orphaned!)";
                }

                if (true)
                {
                    System.out.println("Before: "+title);
                    while (true)
                    {
                        int oldLength = title.length();
                        title = viewStringCompressionPattern.matcher(title).replaceAll(viewStringCompressionReplacement);
                        int newLength = title.length();
                        if (newLength == oldLength)
                            break;
                    }
                    System.out.println("After: "+title);
                }

                ((Frame)getTopLevelFrameOrApplet(controlPanel)).setTitle(title);
            }

        } // updateControlPanelWindowTitles

        public void dumpComponentHierarchies() {
            System.out.println("================================================");
            {
                int n = controlPanels.size();
                System.out.println("    "+n+" control panel"+(n==1?"":"s")+":");
                for (int i = 0; i < n; ++i)
                {
                    Component controlPanel = controlPanels.get(i);
                    Component topLevelFrameOrApplet = getTopLevelFrameOrApplet(controlPanel);
                    MC4DLegacyControlPanel.dumpComponentHierarchy(topLevelFrameOrApplet, 9,i,n);
                }
            }
            {
                int n = viewerPanels.size();
                System.out.println("    "+n+" viewer panel"+(n==1?"":"s")+":");
                for (int i = 0; i < n; ++i)
                {
                    MC4DViewerPanel viewerPanel = viewerPanels.get(i);
                    Component topLevelFrameOrApplet = getTopLevelFrameOrApplet(viewerPanel);
                    MC4DLegacyControlPanel.dumpComponentHierarchy(topLevelFrameOrApplet, 9,i,n);
                }
            }
            System.out.println("================================================");
        }

        @Override public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("applet = {\n");
            {
                sb.append("    puzzleDescriptionsAndUndoTrees = {\n");
                /*
                for (int i = 0; i < puzzleDescriptionsAndUndoTrees.length; ++i)
                {
                    String undoTreeString = new com.donhatchsw.util.UndoTreeSquirrel(puzzleDescriptionsAndUndoTrees[i].undoTree).toString(); // XXX lame-- UndoTree should have its own toString now, without the (you are here)
                    sb.append("        {puzzleDescription = \""+puzzleDescriptionsAndUndoTrees[i].puzzlePrescription+"\", undoTree="+undoTreeString+"},\n");
                }
                */
                sb.append("    },\n");
            }
            {
                sb.append("    macros = {\n");
                // some day
                sb.append("    },\n");
            }
            {
                sb.append("    controlPanelWindows = {\n");
                int n = controlPanels.size();
                for (int i = 0; i < n; ++i)
                {
                    Component controlPanel = controlPanels.get(i);
                    sb.append("        {\n");
                    sb.append("            name = "+com.donhatchsw.util.Arrays.toStringCompact(controlPanel.getName())+"\n");
                    sb.append("            state = "+windowStateToString(controlPanel)+"\n");
                    sb.append("            viewParams = "+((MC4DControlPanelInterface)controlPanel).getViewParams().toString()+"\n");
                    sb.append("        }\n");
                }
                sb.append("    },\n");
            }
            {
                sb.append("    undoTreeWindows = {\n");
                sb.append("    },\n");
            }
            {
                sb.append("    viewerWindows = {\n");
                int n = viewerPanels.size();
                for (int i = 0; i < n; ++i)
                {
                    MC4DViewerPanel viewerPanel = viewerPanels.get(i);
                    sb.append("        {\n");
                    sb.append("            name = "+com.donhatchsw.util.Arrays.toStringCompact(viewerPanel.getName())+",\n");
                    sb.append("            state = "+windowStateToString(viewerPanel)+",\n");
                    Component controlPanel = findFirstControlPanelOfViewParams(viewerPanel.getViewGuts().viewParams);
                    if (controlPanel != null)
                        sb.append("            controlPanelName = "+com.donhatchsw.util.Arrays.toStringCompact(controlPanel.getName())+",\n");
                    // XXX same for undo tree window

                    sb.append("        },\n");
                }
                sb.append("    },\n");
            }
            sb.append("}");
            return sb.toString();
        } // toString
    } // class PuzzlesAndWindows
    private PuzzlesAndWindows allPuzzlesAndWindows = new PuzzlesAndWindows();

    // Walk up the component hierarchy to the root,
    // which better be a Frame or com.donhatchsw.shims_for_deprecated.javax_swing_JApplet.
    // XXX hmm, empirically, when in mozilla, the applet is
    // XXX inside a class sun.plugin.viewer.frame.XNetscapeEmbeddedFrame.
    // XXX how to deal with this, do the detach/attach thing, and still be able to restore
    // XXX position just like any other Frame when in the AppletViewer? hmm
    private static Component getTopLevelFrameOrApplet(Component comp)
    {
        //System.out.println("in getTopLevelFrameOrApplet");
        {
            Container parent;
            while ((parent = comp.getParent()) != null)
                comp = parent;
        }
        //System.out.println("out getTopLevelFrameOrApplet ("+comp.getClass()+")");
        CHECK(comp instanceof Frame
           || comp instanceof com.donhatchsw.shims_for_deprecated.javax_swing_JApplet);
        return comp;
    } // getTopLevelFrameOrApplet


    private MC4DViewGuts mainViewGuts;
    @Override public void init()
    {
        System.out.println("    in MC4DJApplet init");

        com.donhatchsw.applet.AppletUtils.getParametersIntoPublicFields(this, /*verboseLevel=*/0);

        if (forceFuttableXXX)  // must do this before constructing any polytope puzzles, since it affects construction
        {
            PolytopePuzzleDescription.forceFuttableXXX = true;
        }
        mainViewGuts = new MC4DViewGuts();
        if (modelStateString != null) {
            mainViewGuts.setModel(MC4DModel.fromString(modelStateString));
        } else {
            mainViewGuts.setModel(new MC4DModel(puzzleDescription));
        }
        mainViewGuts.viewParams.futtIfPossible.set(futtIfPossible);

        //
        // Initial control panel window(s)
        //
        {
            for (int i = 0; i < this.nControlPanelsAtStartup; ++i)
            {
                // not openOrMake!
                if (i % 2 == 0)
                {
                    makeNewSwingControlPanelWindow(mainViewGuts,
                                                   allPuzzlesAndWindows);
                }
                else
                {
                    makeNewLegacyControlPanelWindow(mainViewGuts,
                                                    allPuzzlesAndWindows);
                }
            }
        }

        //
        // Initial undo tree windows
        //
        {
            int nUndoTreeWindowsAtStartup = 0; // can manually set this to more, for debugging
            for (int i = 0; i < nUndoTreeWindowsAtStartup; ++i)
                makeNewUndoTreeWindow(mainViewGuts);
        }


        String viewName = "View "+(allPuzzlesAndWindows.nextViewerNumber++);
        JPanel mainWindowPanel = new MC4DViewerPanel(viewName,
                                                     mainViewGuts,
                                                     doDoubleBuffer,
                                                     MC4DJApplet.this,
                                                     allPuzzlesAndWindows);

        setLayout(new BorderLayout());
        add(mainWindowPanel);

        System.out.println("    out MC4DJApplet init");
    } // init

    @Override public void start()
    {
        System.out.println("    in MC4DJApplet start");
        System.out.println("    out MC4DJApplet start");
    } // start
    @Override public void stop()
    {
        System.out.println("    in MC4DJApplet stop");
        System.out.println("    out MC4DJApplet stop");
    } // stop
    @Override public void destroy()
    {
        System.out.println("    in MC4DJApplet destroy");
        mainViewGuts.setModel(null);
        mainViewGuts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
        mainViewGuts.setViewComponent(null); // XXX make this not necessary. with weak ref I think
        System.out.println("    out MC4DJApplet destroy");
    } // destroy


    //
    // Common code...
    //
        private static void openOrMakeNewControlPanelWindow(MC4DViewGuts viewGuts,
                                                            PuzzlesAndWindows allPuzzlesAndWindows)
        {
            Component controlPanel = allPuzzlesAndWindows.findFirstControlPanelOfViewParams(viewGuts.viewParams);
            if (controlPanel != null)
            {
                Frame controlPanelFrame = (Frame)getTopLevelFrameOrApplet(controlPanel);
                controlPanelFrame.pack(); // XXX should I?
                controlPanelFrame.setState(Frame.NORMAL);
                controlPanelFrame.setVisible(true);  // available in java 1.5, replaces deprecated show()
                return;
            }
            makeNewLegacyControlPanelWindow(viewGuts, allPuzzlesAndWindows);
        }
        private static void makeNewSwingControlPanelWindow(MC4DViewGuts viewGuts,
                                                            PuzzlesAndWindows allPuzzlesAndWindows)
        {
            System.out.println("Making the swing control panel component...");
            String controlPanelName = "Settings "+(allPuzzlesAndWindows.nextControlPanelNumber++);
            Component controlPanel = new MC4DSwingControlPanel(controlPanelName,
                                                               viewGuts.viewParams,
                                                               viewGuts.viewState);

            System.out.println("Making the window...");
            JFrame frame = new JFrame("MC4D Swing Control Panel");
            frame.getContentPane().add(new JScrollPane(controlPanel) {{
              // from ShephardsPlayApplet:
              // "if we don't do the following, it will screw up and pick a width that forces a horizontal scrollbar... lame!"
              // TODO: figure out what I meant.  is it relevant?  the scrollbar doesn't appear in that applet anyway, so I don't understand what's happening
              //this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            }});

            allPuzzlesAndWindows.addControlPanel(controlPanel); // needs the frame before doing this, so it can set window titles

            System.out.println("Packing the window...");
            frame.pack();
            System.out.println("Showing the window...");
            frame.setVisible(true);
            System.out.println("Done.");
        }  // makeNewSwingControlPanelWindow
        private static void makeNewLegacyControlPanelWindow(MC4DViewGuts viewGuts,
                                                            PuzzlesAndWindows allPuzzlesAndWindows)
        {
            System.out.println("Making the legacy control panel component...");
            String controlPanelName = "Settings "+(allPuzzlesAndWindows.nextControlPanelNumber++);
            Component controlPanel = new MC4DLegacyControlPanel(controlPanelName,
                                                                viewGuts.viewParams,
                                                                viewGuts.viewState); // for "Frame Picture", kind of hacky, violates the idea that control panels are 1-to-1 with viewParams

            java.awt.ScrollPane controlPanelScrollPane = new java.awt.ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
            controlPanelScrollPane.setSize(controlPanel.getPreferredSize());  // why doesn't this work?  makes it too small.  we adjust for it later, when packing.
            controlPanelScrollPane.add(controlPanel);

            System.out.println("Making the window...");
            final java.awt.Frame controlPanelFrame = new java.awt.Frame("MC4D Legacy Control Panel");
            // XXX the following is probably not what I want
            controlPanelFrame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent we) {
                    //controlPanelFrame.dispose();
                    controlPanelFrame.setVisible(false);
                    // no exit, this isn't a main window
                }
            });

            controlPanelFrame.add(controlPanelScrollPane);

            allPuzzlesAndWindows.addControlPanel(controlPanel); // needs the frame before doing this, so it can set window titles

            controlPanelFrame.setLocation(675,0);
            System.out.println("Packing the window...");
            controlPanelFrame.pack();
            if (true)
            {
                // I don't know why, but the earlier controlPanel.getPreferredSize
                // gave a size that's too small.  Now that we've packed, get it again,
                // and it will be correct.
                // And get it right with respect to added scroll bar sizes, too.
                Dimension controlPanelSize = controlPanel.getPreferredSize();
                controlPanelScrollPane.setSize(
                  controlPanelSize.width + controlPanelScrollPane.getVScrollbarWidth(),
                  controlPanelSize.height + controlPanelScrollPane.getHScrollbarHeight());
                controlPanelFrame.pack();
            }
            System.out.println("Showing the window...");
            controlPanelFrame.setVisible(true);  // available in java 1.5, replaces deprecated show()
            System.out.println("Done.");
        }  // makeNewLegacyControlPanelWindow

        @SuppressWarnings("serial")
        private class MC4DUndoTreeWindow
            extends Frame
        {
        } // MC4DUndoTreeWindow

        private static void makeNewUndoTreeWindow(final MC4DViewGuts viewGuts)
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
                    int order = viewGuts.model.genericPuzzleDescription.getGripSymmetryOrders(twist.futtIfPossible)[twist.grip];
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
                    int face = viewGuts.model.genericPuzzleDescription.getGrip2Face()[grip];
                    return faceColor[face % faceColor.length];
                }
                @Override public String leftLabel(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    int grip = twist.grip;
                    int order = viewGuts.model.genericPuzzleDescription.getGripSymmetryOrders(twist.futtIfPossible)[grip];
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
            {
                com.donhatchsw.util.UndoTreeViewer controllerUndoTreeViewer =
                com.donhatchsw.util.UndoTreeViewer.makeExampleUndoTreeViewer(
                        //"Controller's view of the undo tree",
                        "MC4D Undo Tree",
                        viewGuts.model.controllerUndoTreeSquirrel,
                        new com.donhatchsw.util.UndoTreeSquirrel[]{viewGuts.model.animationUndoTreeSquirrel},
                        null, null, 500, 20, 350, 600,
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
            }

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
    *         <li> futtIfPossible
    *         <li> forceFuttableXXX
    *         <li> nControlPanelsAtStartup
    *         <li> modelStateString
    *     </ul>
    */
    public static void main(String args[])
    {
        // Check to make sure each of the command line args
        // is of the form param=value for some valid param name,
        // and that one of them is puzzleDescription (TODO: or modelStateString, but not both).
        // XXX is there a way we can make AppletViewer do this automatically?
        {
            boolean requirePuzzleDescriptionArg = false; // XXX ARGH! I want this to be true when run from the command line, but it has to be false when launching through a jar's manifest.mf because you can't pass args through there :-(
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
                    System.err.println("MC4DJApplet: ERROR: Unrecognized command line argument \""+args[iArg]+"\"");
                    foundBadArg = true;
                }
                if (args[iArg].startsWith("puzzleDescription="))
                    foundPuzzleDescriptionArg = true;
            }
            if (foundBadArg
             || (requirePuzzleDescriptionArg && !foundPuzzleDescriptionArg))
            {
                System.err.println();
                System.err.println("Usage: MC4DJApplet puzzleDescription=\"<puzzleDescription>\" [<otherparam>=<othervalue> ... ]");
                System.err.println("Example: MC4DJApplet puzzleDescription=\"{4,3,3} 3\"");
                System.exit(1);
            }
        }

        String appletViewerArgs[] = new String[args.length+1];
        appletViewerArgs[0] = "com.donhatchsw.mc4d.MC4DJApplet";
        for (int i = 0; i < args.length; ++i)
            appletViewerArgs[i+1] = args[i];
        com.donhatchsw.shims_for_deprecated.com_donhatchsw_applet_AppletViewer.main(appletViewerArgs);
    } // main

} // class MC4DJApplet
