package com.donhatchsw.mc4d;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import com.donhatchsw.awt.Row;
import com.donhatchsw.awt.Col;
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
    public boolean doDoubleBuffer = true; // XXX get this from viewing params? currently this must match viewing params' default value
    public boolean futtIfPossible = false; // XXX get this from viewing params? currently this must match viewing params' default value
    private final static String parameterInfo[][] = {
        {"puzzleDescription", "string", "puzzle description, e.g. \"{4,3,3} 3\""},
        {"x", "integer", "x position for spawned viewers"},  // XXX does this work?
        {"y", "integer", "y position for spawned viewers"},  // XXX does this work?
        {"w", "integer", "width of spawned viewers"},  // XXX does this work?
        {"h", "integer", "height of spawned viewers"},  // XXX does this work?
        {"doDoubleBuffer", "boolean", "whether to double buffer"},
        {"futtIfPossible", "boolean", "whether to try to futt (i.e. allow topologically valid twists that may require morphing)"},
    };
    public String[][] getParameterInfo()
    {
        return parameterInfo;
    }


    public MC4DApplet()
    {
        System.out.println("    in MC4DApplet ctor");
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
        at com.donhatchsw.mc4d.MC4DApplet.<init>(MC4DApplet.java:59)
*/

            // So.. should we do this if not in a browser?
            // and how do we tell?  Just try and give up
            // if it throws an exception?
            {
                final java.io.PrintStream origOut = System.out;
                java.io.PrintStream newOut = new java.io.PrintStream(new java.io.OutputStream() {
                    public void write(int b) throws java.io.IOException {
                        origOut.print("["+(char)b+"]");
                        origOut.flush();
                    }
                });
                System.setOut(newOut);
            }
            {
                final java.io.PrintStream origErr = System.err;
                java.io.PrintStream newErr = new java.io.PrintStream(new java.io.OutputStream() {
                    public void write(int b) throws java.io.IOException {
                        origErr.print("{"+(char)b+"}");
                        origErr.flush();
                    }
                });
                System.setErr(newErr);
            }
        }

        System.out.println("    out MC4DApplet ctor");
    }

    private static Canvas makeNewMC4DViewCanvas(final MC4DViewGuts viewGuts,
                                                final boolean doDoubleBuffer,
                                                final Component menuBarForWidth[/*1*/],
                                                final PuzzlesAndWindows allPuzzlesAndWindows) // XXX should really be local to this view window so we can change it I think
    {
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

                if (false)
                {
                    g.setColor(Color.white);
                    g.drawString("ctrl-n for another ancient view", 10, h-50);
                    g.drawString("ctrl-s to save to the cookie", 10, h-30);
                    g.drawString("ctrl-l to load from the cookie", 10, h-10);
                }

                if (doDoubleBuffer)
                    frontBufferGraphics.drawImage(backBuffer, 0, 0, this);
                //System.out.println("out canvas paint");
            }
            // XXX lame hack... how should I really make the canvas square and same width as menu bar?
            public Dimension getPreferredSize()
            {
                if (menuBarForWidth != null)
                {
                    Dimension menuBarPreferredSize = menuBarForWidth[0].getPreferredSize();
                    return new Dimension(menuBarPreferredSize.width,
                                         menuBarPreferredSize.width); // width, not height
                    // XXX or can I reuse it?
                }
                else
                    return super.getPreferredSize();
            }
            // So we can type immediately in it
            public boolean isFocusTraversable()
            {
                return true;
            }
        };
        viewGuts.setControllerComponent(canvas, true);
        viewGuts.setViewComponent(canvas);

        canvas.addKeyListener(new java.awt.event.KeyListener() {
            public void keyPressed(KeyEvent ke)
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
            public void keyTyped(KeyEvent ke)
            {
            }
            public void keyReleased(KeyEvent ke)
            {
            }
        });

        return canvas;
    } // makeNewMC4DViewCanvas

        // Define some on-the-fly convenience component classes...
        // XXX move these out into awt, maybe?

        // A MenuBar has to be associated with a Frame?? wtf!?
        // I mean, how friggin hard is this???
        // All right, making my own that need not be associated with a frame
        private static class MyMenuBar
            extends Row
        {
            private int eventVerbose = 0;
            private boolean doHighlighting = false; // not ready for prime time-- sometimes background is white
            private boolean someMenuIsShowing = false; // XXX bleah! this isn't working right at all... how the hell can I tell when it pops up and down???
            public MyMenuBar()
            {
            }
            public void add(final String labelText, final PopupMenu menu)
            {
                add(new Label(labelText) {{
                    this.add(menu); // necessary, but doesn't really do anything except make the menu feel loved
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
                            if (doHighlighting)
                            {
                                if (savedForeground == null)
                                    savedForeground = getForeground();
                                setForeground(Color.white);
                            }

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
                            if (doHighlighting)
                            {
                                if (savedForeground != null)
                                {
                                    setForeground(savedForeground);
                                    savedForeground = null;
                                }
                            }
                        } // mouseExited
                    });
                }});
                // XXX how to tell when a menu has disappeared?  this isn't the way-- it comes up in only some of the cases of what can happen
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
        private static abstract class MyMenuItem
            extends MenuItem
            implements ActionListener
        {
            public MyMenuItem(String labelText)
            {
                super(labelText);
                addActionListener(this); // so my actionPerformed will get called
            }
        } // MyMenuItem

    // new menu bar and new view canvas, inside a new panel.
    // XXX should use a real menu bar if there's a frame, and a MyMenuBar otherwise
    private static class MC4DViewerPanel
        extends Panel
    {
        private String name;
        public String getName()
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
                               final Applet applet, // for context for cookie
                               final PuzzlesAndWindows allPuzzlesAndWindows) // for save
        {
            this.name = name;
            this.viewGuts = viewGuts;

            allPuzzlesAndWindows.addViewerPanel(this);

            final boolean isInSandbox = true; // XXX figure this out for real

            Component menuBarHolder[] = new Component[1]; // so that the canvas can access the menuBar later when it needs to for getPreferredSize, even though we haven't created the menu bar yet
            final Canvas canvas = makeNewMC4DViewCanvas(viewGuts,
                                                        doDoubleBuffer,
                                                        menuBarHolder, // canvas wants to be square and same size as menu bar
                                                        allPuzzlesAndWindows);

            Component menuBar = null;
            if (System.getProperty("java.version").startsWith("1.1."))
                System.out.println("ARGH, no menubar supported at all in java 1.1!");
            else
                menuBar = new MyMenuBar() {{
                    add("File", new PopupMenu() {{ // XXX argh, this gives under 1.1: java.lang.IncompatibleClassChangeError: Unimplemented interface method   -- what does that mean?  did this exist under 1.1 or not?
                        if (isInSandbox)
                        {
                            add(new MyMenuItem("Save to browser cookie") {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    com.donhatchsw.applet.CookieUtils.setCookie(applet, "mc4dmodelstate", viewGuts.model.toString());
                                }
                            });
                            add(new MyMenuItem("Save to browser cookie #2") {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    com.donhatchsw.applet.CookieUtils.setCookie(applet, "mc4dmodelstate2", viewGuts.model.toString());
                                }
                            });
                            add(new MyMenuItem("Load from browser cookie") {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    String modelStateString = com.donhatchsw.applet.CookieUtils.getCookie(applet, "mc4dmodelstate");
                                    MC4DModel newModel = MC4DModel.fromString(modelStateString);
                                    if (newModel != null)
                                        viewGuts.setModel(newModel);
                                }
                            });
                            add(new MyMenuItem("Load from browser cookie #2") {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    String modelStateString = com.donhatchsw.applet.CookieUtils.getCookie(applet, "mc4dmodelstate2");
                                    MC4DModel newModel = MC4DModel.fromString(modelStateString);
                                    if (newModel != null)
                                        viewGuts.setModel(newModel);
                                }
                            });
                            if (true)
                                add(new MyMenuItem("Test to/from string") {
                                    public void actionPerformed(java.awt.event.ActionEvent e)
                                    {
                                        MC4DModel m0 = viewGuts.model;
                                        String s1 = m0.toString();
                                        System.out.println("model = "+s1);
                                        MC4DModel m2 = MC4DModel.fromString(s1);
                                        String s3 = m2.toString();
                                        Assert(s3.equals(s1));
                                        System.out.println("Good!");
                                        viewGuts.setModel(m2);
                                    }
                                });
                            addSeparator();
                            add(new MyMenuItem("Experimental print app to terminal") {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    System.out.println(allPuzzlesAndWindows.toString());
                                }
                            });
                        }
                        else
                        {
                            add("Open...");
                            add("Save");
                            add("Save to...");
                        }
                        addSeparator();
                        add("Quit");
                    }});
                    add("Edit", new PopupMenu() {{
                        add(new MyMenuItem("Reset            Ctrl-R") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
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
                        add(new MyMenuItem("Undo             Ctrl-Z") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                if (viewGuts.model.controllerUndoTreeSquirrel.undo() == null)
                                    System.out.println("Nothing to undo.");
                            }
                        });
                        add(new MyMenuItem("Redo             Ctrl-Y") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                if (viewGuts.model.controllerUndoTreeSquirrel.redo() == null)
                                    System.out.println("Nothing to redo.");
                            }
                        });
                        addSeparator();
                        add(new MyMenuItem("Solve (cheat)  Ctrl-T") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                while (viewGuts.model.controllerUndoTreeSquirrel.undo() != null)
                                    ;
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
                            add(new MyMenuItem(""+i+"      Ctrl-"+i) {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    System.out.println("Scramble "+scramblechenfrengensen);
                                    GenericGlue glue = new GenericGlue(viewGuts.model); // XX lame! need to not do this, make it call something more legit... glue needs to go away!
                                    glue.scrambleAction(canvas, new Label(), scramblechenfrengensen, viewGuts.viewParams.futtIfPossible.get());
                                }
                            });
                        }
                        addSeparator();
                        add(new MyMenuItem("Full   Ctrl-F") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
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
                    add("Windows", new PopupMenu() {{
                        add(new MyMenuItem("Control Panel                       Ctrl-C") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                openOrMakeNewControlPanelWindow(viewGuts,
                                                                allPuzzlesAndWindows);
                            }
                        });
                        add(new MyMenuItem("Expert Control Panel") {
                            {setEnabled(false);}
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                // XXX implement me
                            }
                        });
                        addSeparator();
                        add(new MyMenuItem("Macros") {
                            {setEnabled(false);}
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                            }
                        });
                        addSeparator();
                        add(new MyMenuItem("Undo Tree                           Ctrl-U") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                makeNewUndoTreeWindow(viewGuts);
                            }
                        });
                        addSeparator();
                        add(new MyMenuItem("Shared view of shared puzzle state") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
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
                            add(new MyMenuItem("Shared view of cloned puzzle state") {
                                public void actionPerformed(java.awt.event.ActionEvent e)
                                {
                                    makeNewViewWindow(viewGuts,
                                                      false, // don't clone view, share it
                                                      true, // clone puzzle state
                                                      doDoubleBuffer,
                                                      applet,
                                                      allPuzzlesAndWindows);
                                }
                            });
                        add(new MyMenuItem("Cloned view of shared puzzle state ") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                                makeNewViewWindow(viewGuts,
                                                  true, // clone view
                                                  false, // don't clone puzzle state, share it
                                                  doDoubleBuffer,
                                                  applet,
                                                  allPuzzlesAndWindows);
                            }
                        });
                        add(new MyMenuItem("Cloned view of cloned puzzle state ") {
                            public void actionPerformed(java.awt.event.ActionEvent e)
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
                        add(new MyMenuItem("Progress/diagnostics/debug") {
                            {setEnabled(false);}
                            public void actionPerformed(java.awt.event.ActionEvent e)
                            {
                            }
                        });
                    }});
                    add("Help", new PopupMenu() {{
                        add("About...");
                    }});
                    // XXX MyMenuBar should do this automatically-- what is the cleanest way?
                    add(new Label(""),
                        new GridBagConstraints(){{fill=HORIZONTAL;weightx=1.;}}); // stretch
                }}; // menuBar

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
                                          final Applet applet,
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
        Frame frame = new Frame() {{
            add(new MC4DViewerPanel(viewName,
                                    newViewGuts,  
                                    doDoubleBuffer,
                                    applet,
                                    allPuzzlesAndWindows));
            pack();

            com.donhatchsw.awt.MainWindowCount.increment();
            addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent we)
                {
                    System.out.println("in windowClosing from makeNewViewWindow");
                    dispose();
                }

                // ARGH! this gets called twice when in browswer:
                // once when user closes it (due to the dispose() above)
                // then again when applet is destroyed.  So,
                // we keep track of whether we are already closed.
                private boolean closedAlreadyYouMoron = false;
                public void windowClosed(java.awt.event.WindowEvent we)
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
        frame.show();
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

        // Kind of a general purpose utility, could be put in util
        private static class OrderedHashTable
        {
            private com.donhatchsw.compat.ArrayList orderedKeys = new com.donhatchsw.compat.ArrayList();
            private java.util.Hashtable hashTable = new java.util.Hashtable();
            public void add(Object key, Object value)
            {
                Assert(hashTable.get(key) == null); // XXX throw something more legit
                orderedKeys.add(key);
                hashTable.put(key, value);
            }
            public void remove(Object key)
            {
                Assert(hashTable.get(key) != null); // XXX throw something more legit
                orderedKeys.remove(key); // takes O(n) time
                hashTable.remove(key);
            }
            public Object get(int i)
            {
                return orderedKeys.get(i);
            }
            public Object get(Object o)
            {
                return hashTable.get(o);
            }
            public int size()
            {
                return orderedKeys.size();
            }
        } // OrderedHashTable

        OrderedHashTable puzzlePrescriptionToUndoTree = new OrderedHashTable();
        OrderedHashTable nameToUndoTreePanel = new OrderedHashTable();
        OrderedHashTable nameToControlPanelPanel = new OrderedHashTable();
        OrderedHashTable nameToViewerPanel = new OrderedHashTable();

        OrderedHashTable viewerPanelToControlPanel = new OrderedHashTable();
        OrderedHashTable viewerPanelToPuzzlePrescription = new OrderedHashTable();
        // XXX do I even need any of the above?

        public com.donhatchsw.compat.ArrayList puzzleDescriptionsAndUndoTrees = new com.donhatchsw.compat.ArrayList();
        private com.donhatchsw.compat.ArrayList viewerPanels = new com.donhatchsw.compat.ArrayList();
        private com.donhatchsw.compat.ArrayList controlPanels = new com.donhatchsw.compat.ArrayList();
        public com.donhatchsw.compat.ArrayList undoTreeSquirrelPanels = new com.donhatchsw.compat.ArrayList();
        public int nextViewerNumber = 0;
        public int nextControlPanelNumber = 0;
        public int nextUndoTreeWindowNumber = 0;

        private String windowStateToString(Component component)
        {
            Component frameOrApplet = getTopLevelFrameOrApplet(component);
            java.awt.Rectangle bounds = frameOrApplet.getBounds();
            String s = (frameOrApplet instanceof Applet ? "applet" :
                        !frameOrApplet.isVisible() ? "closed" :
                        ((Frame)frameOrApplet).getState() == Frame.ICONIFIED ? "iconified" : "open");
            s += "@" + bounds.width
               + "x" + bounds.height
               + "+" + bounds.x
               + "+" + bounds.y;
            return s;
        }

        // XXX should use a hash table, probably
        private MC4DControlPanel findControlPanelOfViewParams(MC4DViewGuts.ViewParams viewParams)
        {
            int n = controlPanels.size();
            for (int i = 0; i < n; ++i)
            {
                MC4DControlPanel controlPanel = (MC4DControlPanel)controlPanels.get(i);
                if (controlPanel.getViewParams() == viewParams)
                    return controlPanel;
            }
            return null;
        } // findControlPanelOfViewParams
        private MC4DControlPanel findUndoTreeSquirrelPanelOfSquirrel(com.donhatchsw.util.UndoTreeSquirrel squirrel)
        {
            int n = undoTreeSquirrelPanels.size();
            for (int i = 0; i < n; ++i)
            {
                Assert(false); // XXX do me
            }
            return null;
        } // findUndoTreeSquirrelPanelOfSquirrel

        public void addControlPanel(MC4DControlPanel controlPanel)
        {
            {
                // Make sure name doesn't exist...
                String name = controlPanel.getName();
                int n = controlPanels.size();
                for (int i = 0; i < n; ++i)
                    if (((MC4DControlPanel)controlPanels.get(i)).getName().equals(name))
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
                    if (((MC4DViewerPanel)viewerPanels.get(i)).getName().equals(name))
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
        private final static com.donhatchsw.compat.regex.Pattern viewStringCompressionPattern = com.donhatchsw.compat.regex.Pattern.compile("Views? (\\d+(,\\d+)*),\\s*Views? (\\d+(,\\d+)*)");
        private final static String viewStringCompressionReplacement = "Views $1,$3";

        // Title will be something like "MC4D Control Panel for Views 0,1,47)"
        private void updateControlPanelWindowTitles()
        {
            int nControlPanels = controlPanels.size();
            int nViewerPanels = viewerPanels.size();
            for (int iControlPanel = 0; iControlPanel < nControlPanels; ++iControlPanel)
            {
                MC4DControlPanel controlPanel = (MC4DControlPanel)controlPanels.get(iControlPanel);
                String title = "MC4D Control Panel for ";
                int nViewsFound = 0;
                for (int iViewerPanel = 0; iViewerPanel < nViewerPanels; ++iViewerPanel)
                {
                    MC4DViewerPanel viewerPanel = (MC4DViewerPanel)viewerPanels.get(iViewerPanel);
                    if (viewerPanel.getViewGuts().viewParams
                     == controlPanel.getViewParams())
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


        public String toString()
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
                    MC4DControlPanel controlPanel = (MC4DControlPanel)controlPanels.get(i);
                    sb.append("        {\n");
                    sb.append("            name = "+com.donhatchsw.util.Arrays.toStringCompact(controlPanel.getName())+"\n");
                    sb.append("            state = "+windowStateToString(controlPanel)+"\n");
                    sb.append("            viewParams = "+controlPanel.getViewParams().toString()+"\n");
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
                    MC4DViewerPanel viewerPanel = (MC4DViewerPanel)viewerPanels.get(i);
                    sb.append("        {\n");
                    sb.append("            name = "+com.donhatchsw.util.Arrays.toStringCompact(viewerPanel.getName())+",\n");
                    sb.append("            state = "+windowStateToString(viewerPanel)+",\n");
                    MC4DControlPanel controlPanel = findControlPanelOfViewParams(viewerPanel.getViewGuts().viewParams);
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
    // which better be a Frame or Applet.
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
        Assert(comp instanceof Frame
            || comp instanceof Applet);
        return comp;
    } // getTopLevelFrameOrApplet


    private MC4DViewGuts mainViewGuts;
    public void init()
    {
        System.out.println("    in MC4DApplet init");

        com.donhatchsw.applet.AppletUtils.getParametersIntoPublicFields(this, 0);

        mainViewGuts = new MC4DViewGuts();
        mainViewGuts.setModel(new MC4DModel(puzzleDescription));
        mainViewGuts.viewParams.futtIfPossible.set(futtIfPossible);

        //
        // Initial control panel window(s)
        //
        {
            int nControlPanelsAtStartup = 0; // can set this to more, to experiment... they should all stay in sync
            for (int i = 0; i < nControlPanelsAtStartup; ++i)
                openOrMakeNewControlPanelWindow(mainViewGuts,
                                                allPuzzlesAndWindows);
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
        Panel mainWindowPanel = new MC4DViewerPanel(viewName,
                                                    mainViewGuts,
                                                    doDoubleBuffer,
                                                    MC4DApplet.this,
                                                    allPuzzlesAndWindows);

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
        mainViewGuts.setModel(null);
        mainViewGuts.setControllerComponent(null, false); // XXX make this not necessary, with weak ref I think
        mainViewGuts.setViewComponent(null); // XXX make this not necessary. with weak ref I think
        System.out.println("    out MC4DApplet destroy");
    } // stop


    //
    // Common code...
    //
        private static void openOrMakeNewControlPanelWindow(MC4DViewGuts viewGuts,
                                                            PuzzlesAndWindows allPuzzlesAndWindows)
        {
            String controlPanelName = "Settings "+(allPuzzlesAndWindows.nextControlPanelNumber++);
            MC4DControlPanel controlPanel = allPuzzlesAndWindows.findControlPanelOfViewParams(viewGuts.viewParams);
            if (controlPanel != null)
            {
                Frame controlPanelFrame = (Frame)getTopLevelFrameOrApplet(controlPanel);
                controlPanelFrame.pack(); // XXX should I?
                controlPanelFrame.setState(Frame.NORMAL);
                controlPanelFrame.show();
                return;
            }

            System.out.println("Making the panel...");
            controlPanel = new MC4DControlPanel(controlPanelName,
                                                viewGuts.viewParams,
                                                viewGuts.viewState); // for "Frame Picture", kind of hacky, violates the idea that control panels are 1-to-1 with viewParams

            java.awt.ScrollPane controlPanelScrollPane = new java.awt.ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
            controlPanelScrollPane.setSize(controlPanel.getPreferredSize());  // why doesn't this work?  makes it too small.  we adjust for it later, when packing.
            controlPanelScrollPane.add(controlPanel);

            System.out.println("Making the window...");
            final java.awt.Frame controlPanelFrame = new java.awt.Frame("MC4D Control Panel");
            // XXX the following is probably not what I want
            controlPanelFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    //controlPanelFrame.dispose();
                    controlPanelFrame.hide();
                    // no exit, this isn't a main window
                }
            });

            controlPanelFrame.add(controlPanelScrollPane);

            allPuzzlesAndWindows.addControlPanel(controlPanel); // needs the frame before doing this, so it can set window titles

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
            controlPanelFrame.show();
            System.out.println("Done.");
        } // openOrMakeNewControlPanelWindow

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
                public double length(Object item)
                {
                    MC4DModel.Twist twist = (MC4DModel.Twist)item;
                    Assert(twist != null);
                    Assert(twist.grip != -1);
                    int order = viewGuts.model.genericPuzzleDescription.getGripSymmetryOrders(twist.futtIfPossible)[twist.grip];
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
                    int order = viewGuts.model.genericPuzzleDescription.getGripSymmetryOrders(twist.futtIfPossible)[grip];
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
    *     </ul>
    */
    public static void main(String args[])
    {
        // Check to make sure each of the command line args
        // is of the form param=value for some valid param name,
        // and that one of them is puzzleDescription.
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
                    System.err.println("MC4DApplet: ERROR: Unrecognized command line argument \""+args[iArg]+"\"");
                    foundBadArg = true;
                }
                if (args[iArg].startsWith("puzzleDescription="))
                    foundPuzzleDescriptionArg = true;
            }
            if (foundBadArg
             || (requirePuzzleDescriptionArg && !foundPuzzleDescriptionArg))
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
