package com.donhatchsw.mc4d;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
* <pre>
*       Roadmap:
*           model/view stuff uses glue now (not any more!)
*           make model/view stuff do its own clean thing instead (done mostly)
*           then make the glue use model/view stuff and get rid of the corresponding stuff from glue
*           then make MC4DSwing call the model/view stuff instead of the glue stuff where possible
*           yow!
* </pre>
* 
*  <p>
* 
*  This file has stuff that should eventually be moved to
*  various more permanent homes.
*  <p>
*  It was an attempt to quickly glue the good new classes:
* <pre>
*       GenericPuzzleDescription (interface)
*       PolytopePuzzleDescription (implements GenericPuzzleDescription)
*       GenericPipelineUtils
* </pre>
*  onto MC4DSwing/MC4DView with as minimal impact on Melinda's existing code
*  as possible, prior to Melinda getting a look at it
*  and figuring out where it should really go.
* <p>
*  Although, there is some stuff in here that Don
*  needs to move down into the generic utilities.
* <p>
*  Functions currently in here: XXX changing all the time, out of date
* <pre>
*     - GenericGlue() constructor
*             MC4DSwing creates one of these on startup and stores it
*             in its member genericGlue;
*             it also points the MC4DView's genericGlue member
*             to the same GenericGlue object.
*     - addMoreItemsToPuzzleMenu()
*             MC4DSwing should call this after it adds its
*             standard items to the puzzle menu.
*             This function installs callbacks that activate
*             and deactivate the genericGlue object
*             in response to the relevant PuzzleMenu items.
*     - isActive()
*             Whether this GenericGlue object is active
*             (i.e. contains a valid puzzle).
*             This will be true whenever the MC4DView's "current puzzle"
*             is a generic puzzle (rather than a standard one).
* 
*     - undoAction()
*           use in place of the usual undo action when isActive() is true
*     - redoAction()
*           use in place of the usual redo action when isActive() is true
*     - cheatAction()
*           use in place of the usual cheat action when isActive() is true
*     - scrambleAction()
*           use in place of the usual cheat action when isActive() is true
* 
*     - isAnimating()
*           use in place of the usual isAnimating when isActive() is true
* </pre>
*/
public class GenericGlue
{
    // XXX bogus variables that currently must be kept in sync with the corresponding ones in MagicCube.java
    public int MagicCube_NFRAMES_90 = 15; // XXX should be a param that whoever initiates a rotation passes in, we should not need to know this
    public int MagicCube_FULL_SCRAMBLE = 15; // XXX should be a param that whoever initiates a scramble passes in, we should not need to know this

    public static int verboseLevel = 0; // set to >= 1 to debug

    //
    // State.
    //
    public MC4DModel model = null;

    //
    // A rotation is currently in progress if iRotation < nRotation.
    // XXX this is moving to the View and should go away
    //
    public int nRotation = 0; // total number of rotation frames in progress // XXX need to make this variable, it sucks when the speed isn't responsive to the slider!
    public int iRotation = 0; // number of frames done so far
     public float rotationFrom[]; // where rotation is rotating from, in 4space
     public float rotationTo[]; // where rotation is rotating to, in 4space

    //
    // A twist is currently in progress if iTwist < nTwist.
    // XXX this is moving to the Model and should go away
    //
    public int nTwist = 0; // total number of twist frames in progress
    public int iTwist = 0; // number of twist frames done so far
     public int iTwistGrip;     // of twist in progress, if any
     public int twistDir_field;      // of twist in progress, if any  (called _field because a local was shadowing it and it was confusing me)
     public int twistSliceMask; // of twist in progress, if any
     public boolean twistFuttIfPossible; // of twist in progress, if any
     public int twistIsUndo; // of twist in progress, if any-- when finished, don't put on undo stack
    //
    // A cheat is in progress if cheating == true.
    //
    public boolean cheating;

    //
    // Most recently chosen zero-roll pole.
    // It's a 4d vector but the w component is zero, generally.
    //
    public float zeroRollPoleAfterRot3d[] = null;

    //
    // The sticker and polygon-within-sticker
    // that the mouse is currently hovering over.
    //
    public int iStickerUnderMouse = -1;
    public int iPolyUnderMouse = -1;
    public boolean highlightByGrip = true;


    //
    // Rudimentary undo queue.
    // XXX this needs to go away now that we have an undo tree
    //
    public static class HistoryNode
    {
        public int iGrip;
        public int dir;
        public int slicemask;
        public boolean futtIfPossible;
        public HistoryNode(int iGrip, int dir, int slicemask, boolean futtIfPossible)
        {
            this.iGrip = iGrip;
            this.dir = dir;
            this.slicemask = slicemask;
            this.futtIfPossible = futtIfPossible;
        }
    }
    public java.util.ArrayList<HistoryNode> undoq = new java.util.ArrayList<HistoryNode>();
    public int undoPartSize = 0; // undoq has undo part followed by redo part

    //
    // Two scratch Frames to use for computing and painting.
    //
    public GenericPipelineUtils.Frame untwistedFrame = new GenericPipelineUtils.Frame();
    public GenericPipelineUtils.Frame twistingFrame = new GenericPipelineUtils.Frame();
        { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something


    //
    // Debugging state variables.
    // Most of these are settable using secret ctrl-alt key combinations.
    // XXX not any more.  all that is moved over to the View now.
    //
    public boolean useTopsort = true;
    public int jitterRadius = 0;
    public boolean drawLabels = false;
    public boolean showPartialOrder = false;
    public boolean frozenForDebugging = false;
        public int frozenPartialOrderForDebugging[][] = null;


    static private void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }

    public interface Callback { public void call(); }



    public GenericGlue(MC4DModel model)
    {
        this.model = model;
    }

    public boolean isActive()
    {
        return model != null;
    }
    public boolean isAnimating()
    {
        return !frozenForDebugging
            && (iRotation < nRotation
             || iTwist < nTwist);
    }

    private void setText(Component label, String text)
    {
        if (label instanceof JLabel)
            ((JLabel)label).setText(text);
        else
            ((Label)label).setText(text);
    }

    private interface MenuFactoryInterface {
        public Object newMenu(String name);  // returns Menu or JMenu
        public Object newMenuItem(String name, ActionListener actionListener);  // returns MenuItem or JMenuItem
        public int getItemCount(Object menu);  // takes Menu or JMenu
        public Object getItem(Object menu, int i);  // takes Menu or JMenu, returns MenuItem or JMenuItem
        public void add(Object menu, Object item);  // takes menu = Menu or JMenu, item = MenuItem or JMenuItem
        public void addSeparator(Object menu);  // takes menu = Menu or JMenu
        public void addActionListener(Object menuItem, ActionListener actionListener);
    };  // MenuFactoryInterface
    private static class MenuFactory implements MenuFactoryInterface {
        @Override public Object newMenu(String name) {
            return new Menu(name);
        }
        @SuppressWarnings("serial")
        @Override public Object newMenuItem(String name, final ActionListener actionListener) {
            return new MenuItem(name) {{
                addActionListener(actionListener);
            }};
        }
        @Override public int getItemCount(Object menu) {
            return ((Menu)menu).getItemCount();
        }
        @Override public Object getItem(Object menu, int i) {
            return ((Menu)menu).getItem(i);
        }
        @Override public void add(Object menu, Object item) {
            ((Menu)menu).add((MenuItem)item);
        }
        @Override public void addSeparator(Object menu) {
            ((Menu)menu).addSeparator();
        }
        @Override public void addActionListener(Object menuItem, ActionListener actionListener) {
          ((MenuItem)menuItem).addActionListener(actionListener);
        }
    };  // MenuFactory
    private static class JMenuFactory implements MenuFactoryInterface {
        @Override public Object newMenu(String name) {
            return new JMenu(name);
        }
        @SuppressWarnings("serial")
        @Override public Object newMenuItem(String name, ActionListener actionListener) {
            if (false)
            {
              // TODO: why on earth doesn't this work???  it gives a stack overflow when invoked!!
              // TODO: and why does the override in the other class require "final" in 1.7, but this one doesn't?? related?
              return new JMenuItem(name) {{
                  addActionListener(actionListener);
              }};
            }
            else
            {
              // workaround
              JMenuItem answer = new JMenuItem(name);
              answer.addActionListener(actionListener);
              return answer;
            }
        }
        @Override public int getItemCount(Object menu) {
            return ((JMenu)menu).getItemCount();
        }
        @Override public Object getItem(Object menu, int i) {
            return ((JMenu)menu).getItem(i);
        }
        @Override public void add(Object menu, Object item) {
            ((JMenu)menu).add((JMenuItem)item);
        }
        @Override public void addSeparator(Object menu) {
            ((JMenu)menu).addSeparator();
        }
        @Override public void addActionListener(Object menuItem, ActionListener actionListener) {
          ((JMenuItem)menuItem).addActionListener(actionListener);
        }
    };  // JMenuFactory

    // Call this from MC4DSwing ctor right after all
    // the other menu items are added
    public void addMoreItemsToPuzzleMenu(Object puzzlemenu,  // Menu or JMenu
                                         final Component statusLabel, // Label or JLabel
                                         final Callback initPuzzleCallback)
    {
        if (verboseLevel >= 1) System.out.println("in GenericGlue.addMoreItemsToPuzzleMenu");

        MenuFactoryInterface menuFactory = (puzzlemenu instanceof JMenu ? new JMenuFactory() : new MenuFactory());

        // Used for reported progress during puzzle creation,
        // which can take a long time.
        // Currently just goes to System.err.
        final java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                                   new java.io.BufferedWriter(
                                                   new java.io.OutputStreamWriter(
                                                   System.err)));

        // Selecting any of the previously existing menu items
        // should have the side effect of setting
        // model to null-- that's the indicator
        // that the glue overlay mechanism is no longer active.
        for (int i = 0; i < menuFactory.getItemCount(puzzlemenu); ++i)
        {
            menuFactory.addActionListener(menuFactory.getItem(puzzlemenu,i), new ActionListener() {
                @Override public void actionPerformed(ActionEvent ae)
                {
                    if (verboseLevel >= 1) System.out.println("GenericGlue: deactivating");
                    model = null;
                }
            });
        }

        String menuScheme[][] = {
          //{"-"}, // separator
          //{"Generic puzzles (no saving or macros)"},
          {"2d puzzles"},
          {"    {3} Triangle",        "1,2,3,4,5"},
          {"    {4} Square",          "1,2,3,4,5"},
          {"    {5} Pentagon",        "1,2,3,4,5"},
          {"    {6} Hexagon",         "1,2,3,4,5"},
          {"    {7} Heptagon",        "1,2,3,4,5"},
          {"    {8} Octagon",         "1,2,3,4,5"},
          {"    {9} Nonagon",         "1,2,3,4,5"},
          {"    {10} Decagon",        "1,2,3,4,5"},
          {"    {11} Hendecagon",     "1,2,3,4,5"},
          {"    {12} Dodecagon",      "1,2,3,4,5"},
          {"3d puzzles"},
          {"    3d regular"},
          {"        {3,3} Tetrahedron (Meier-Halpern Pyramid (tm))",  "1,3(4.0),5(7.0),7(10.0),9(13.0),11(16.0)"},
          {"        {4,3} Cube (Rubik's Cube (tm))",                  "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21"},
          {"        {3,4} Octahedron",                                "1,2,3,4,5"},
          {"        {5,3} Dodecahedron (Megaminx)",                   "1,2,3(2.5),3,5,7,9"},
          {"    3d wythoff"},
          {"        Tetrahedron based"},
          {"            (1)---(0)---(0) Tetrahedron",                       "1,3(4.0)"},
          {"            (1)---(1)---(0) Truncated tetrahedron",             "1,3(4.0)"},
          {"            (0)---(1)---(0) Octahedron",                        "1,2,3,4,5"}, // vertex figure not simplex but it works anyway
          {"            (0)---(1)---(1) Truncated tetrahedron (dual)",      "1,3(4.0)"},
          {"            (0)---(0)---(1) Tetrahedron(dual)",                 "1,3(4.0)"},
          {"            (1)---(0)---(1) Cuboctahedron",                     "1,3(4.0)"}, // vertex figure not simplex but it's fine
          {"            (1)---(1)---(1) Omnitruncated tetrahedron (truncated octahedron)",         "1,3"}, 
          {"        Cube/Octahedron based"},
          {"            (1)-4-(0)---(0) Cube",                              "1,3"},
          {"            (1)-4-(1)---(0) Truncated cube",                    "1,3(4.0)"},
          {"            (0)-4-(1)---(0) Cuboctahedron",                     "1,3"}, // vertex figure not simplex but it's fine
          {"            (0)-4-(1)---(1) Truncated octahedron",              "1,3"},
          {"            (0)-4-(0)---(1) Octahedron",                        "1,2,3,4,5"}, // vertex figure not simplex but it works anyway
          {"            (1)-4-(0)---(1) Rhombicuboctahedron",               "1"}, // vertex figure not simplex
          {"            (1)-4-(1)---(1) Omnitruncated cuboctahedron",       "1,3"},
          {"        Dodecahedron/Icosahedron based"},
          {"            (1)-5-(0)---(0) Dodecahedron",                      "1,2,3(2.5),3,5,7,9"},
          {"            (1)-5-(1)---(0) Truncated dodecahedron",            "1,3(4.0)"},
          {"            (0)-5-(1)---(0) Icosidodecahedron",                 "1,3(4.0)"}, // vertex figure not simplex but it's fine
          {"            (0)-5-(1)---(1) Truncated icosahedron (soccer ball)", "1,3"},
          {"            (0)-5-(0)---(1) Icosahedron",                       "1"}, // vertex figure not simplex
          {"            (1)-5-(0)---(1) Rhombicosidodecahedron",            "1"}, // vertex figure not simplex
          {"            (1)-5-(1)---(1) Omnitruncated dodecahedron",        "1,3"},
          {"    2d x 1d prisms"},
          {"        {3}x{} Triangular prism",    "1,3(4.0),5(7.0),7(10.0)"},
          {"        {4}x{} Cube",                "1,2,3,4,5"},
          {"        {5}x{} Pentagonal prism",    "1,2,3,4,5"},
          {"        {6}x{} Hexagonal prism",     "1,2,3,4,5"},
          {"        {7}x{} Heptagonal prism",    "1,2,3,4,5"},
          {"        {8}x{} Octagonal prism",     "1,2,3,4,5"},
          {"        {9}x{} Nonagonal prism",     "1,2,3,4,5"},
          {"        {10}x{} Decagonal prism",    "1,2,3,4,5"},
          {"        {11}x{} Hendecagonal prism", "1,2,3,4,5"},
          {"        {12}x{} Dodecagonal prism",  "1,2,3,4,5"},
          {"    -"},
          {"    3d nonuniform boxes"},  // note, the parser treats \(\d+\)(x\(\d+\))* as a special case, in which it doesn't split the lengths spec
          {"        (1)x(1)x(2)", "1,1,2"},
          {"        (2)x(2)x(1)", "2,2,1"},
          {"        (3)x(2)x(1)", "3,2,1"},
          {"        (3)x(3)x(1)", "3,3,1"},
          {"        -"},
          {"        (2)x(2)x(3)", "2,2,3"},
          {"        (3)x(3)x(2)", "3,3,2"},
          {"        (4)x(3)x(2)", "4,3,2"},
          {"    3d highly irregular"},
          {"        Frucht",    "1,3(4.0),5(7.0),7(10.0),9(13.0)"},
          {"        Not-Frucht (other minimal asymmetric trivalant)",    "1,3(4.0),5(7.0),7(10.0),9(13.0)"},
          {"4d puzzles"},
          {"    4d regular"},
          {"        {3,3,3} Simplex (5-cell)",          "1,3(5.0),5(9.0),7(13.0)"},
          {"        {4,3,3} Hypercube (8-cell)",        "1,2,3,4,5,6,7,8,9,3(2.1),3(10.0)"},
          {"        {3,3,4} Cross (16-cell)",           "1,2,3,4,5"}, // vertex figure not simplex... but it works beautifully... closest incident cells get face cuts, next closest get edge cuts, farthest get vertex cuts
          {"        {3,4,3} 24-cell",                   "1,2,3,4,5"}, // vertex figure not simplex... but it works fine anyway, the far incident one gets a vertex cut
          {"        {5,3,3} 120-cell (hypermegaminx)",  "1,2,3(2.5),3"},
          {"        {3,3,5} 600-cell",                  "1"}, // vertex figure not simplex
          {"    4d uniform wythoff"}, // XXX should be at bottom of menu, so when someone is shooting for the moon they always go to the bottom of each cascading menu?
          {"        Simplex based"},
          {"            (1)---(0)---(0)---(0) Simplex",                         "1,3(5.0),5(9.0),7(13.0)"},
          {"            (1)---(.5)---(0)---(0) Barely truncated simplex",      "1,3(5.0),3(9.0)"},
          {"            (1)---(1)---(0)---(0) Truncated simplex (truncated tets and tets)",               "1,3(5.0),3(9.0)"},
          {"            (0)---(1)---(0)---(0) Rectified simplex (octas and tets)", "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1)---(0) Bitruncated simplex (truncated tets)",              "1,3(4.0),3(9.0)"},
          {"            (0)---(0)---(1)---(0) Rectified simplex (dual) (tets and octas)", "1"}, // vertex figure not simplex
          {"            (0)---(0)---(1)---(1) Truncated simplex (dual)",        "1,3(5.0),3(9.0)"},
          {"            (0)---(0)---(.5)---(1) Barely truncated simplex (dual)","1,3(5.0),3(9.0)"},
          {"            (0)---(0)---(0)---(1) Simplex (dual)",                  "1,3(5.0),3(9.0)"},
          {"            (1)---(0)---(0)---(1) Runcinated simplex (tets and triprisms)", "1,2,3"}, // vertex figure not simplex, it's an octahedron! still sort of okay, maybe
          {"            (1)---(0)---(1)---(0) Cantellated simplex",             "1"}, // vertex figure not simplex
          {"            (0)---(1)---(0)---(1) Cantellated simplex (dual)",      "1"}, // vertex figure not simplex
          {"            (1)---(1)---(1)---(0) Cantitruncated simplex",          "1,3(4.0),3(9.0)"},
          {"            (1)---(1)---(0)---(1) Runcitruncated simplex",          "1"}, // vertex figure not simplex
          {"            (1)---(0)---(1)---(1) Runcitruncated simplex (dual)",   "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1)---(1) Cantitruncated simplex (dual)",   "1,3(4.0),3(9.0)"},
          {"            (1)---(1)---(1)---(1) Omnitruncated simplex",           "1,3,3(9.0)"}, // XXX get rid of these 9s? not sure, they're kind of interesting
          {"        Hypercube/Cross based"},
          {"            (1)-4-(0)---(0)---(0) Hypercube",                       "1,3"},
          {"            (1)-4-(.5)---(0)---(0) Barely truncated hypercube",    "1,3(5.0)"},
          {"            (1)-4-(1)---(0)---(0) Truncated hypercube (truncated cubes and tets)", "1,3(5.0)"},
          {"            (0)-4-(1)---(0)---(0) Rectified hypercube (cuboctas and tets)", "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1)---(0) Bitruncated hypercube/cross (truncated octas and truncated tets)", "1,3(4.0)"},
          {"            (0)-4-(0)---(1)---(0) Rectified cross (same as 24-cell)", "1"}, // vertex figure not simplex
          {"            (0)-4-(0)---(1)---(1) Truncated cross (octas and truncated tets)", "1"}, // vertex figure not simplex
          {"            (0)-4-(0)---(.5)---(1) Barely truncated cross",         "1"}, // vertex figure not simplex
          {"            (0)-4-(0)---(0)---(1) Cross",                           "1"}, // vertex figure not simplex
          {"            (1)-4-(0)---(0)---(1) Runcinated hypercube/cross (cubes, tets, and tri prisms)", "1,2,3"}, // vertex figure not simplex, it's an octahedron! still sort of okay, maybe
          {"            (1)-4-(0)---(1)---(0) Cantellated hypercube",           "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(0)---(1) Cantellated cross (same as rectified 24-cell) (cuboctas and cubes)", "1"},  // vertex figure not simplex
          {"            (1)-4-(1)---(1)---(0) Cantitruncated hypercube",        "1,3(4.0)"},
          {"            (1)-4-(1)---(0)---(1) Runcitruncated hypercube",        "1"}, // vertex figure not simplex
          {"            (1)-4-(0)---(1)---(1) Runcitruncated cross",            "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1)---(1) Cantitrucated cross (same as truncated 24-cell) (truncated octas and cubes)", "1,3"},
          {"            (1)-4-(1)---(1)---(1) Omnitruncated hypercube/cross",   "1,3"},
          {"        24-cell based"},
          {"            (1)---(0)-4-(0)---(0) 24-cell",                         "1,2,3,4,5"}, // vertex figure not a simplex, but it works anyway
          {"            (1)---(.5)-4-(0)---(0) Barely truncated 24-cell",       "1,3"},
          {"            (1)---(1)-4-(0)---(0) Truncated 24-cell (same as cantritrucated 16-cell) (truncated octas and cubes)", "1,3"},
          {"            (0)---(1)-4-(0)---(0) Rectified 24-cell (same as cantellated cross) (cuboctas and cubes)", "1"}, // vertex figure not simplex
          {"            (0)---(1)-4-(1)---(0) Bitruncated 24-cell (truncated cubes)", "1,3(4.0)"},
          {"            (0)---(0)-4-(1)---(0) Rectified 24-cell (dual) (cubes and cuboctas)", "1"}, // vertex figure not simplex
          {"            (0)---(0)-4-(1)---(1) Truncated 24-cell (dual)",        "1,3"},
          {"            (0)---(0)-4-(.5)---(1) Barely truncated 24-cell (dual)","1,3"},
          {"            (0)---(0)-4-(0)---(1) 24-cell (dual)",                  "1"}, // vertex figure not simplex
          {"            (1)---(0)-4-(0)---(1) Runcinated 24-cell (octas and triprisms)", "1,2,3"}, // vertex figure not simplex, it's an octahedron! still sort of okay, maybe
          {"            (1)---(0)-4-(1)---(0) Cantellated 24-cell",             "1"}, // vertex figure not simplex
          {"            (0)---(1)-4-(0)---(1) Cantellated 24-cell (dual)",      "1"},  // vertex figure not simplex
          {"            (1)---(1)-4-(1)---(0) Cantitruncated 24-cell",          "1,3(4.0)"},
          {"            (1)---(1)-4-(0)---(1) Runcitruncated 24-cell",          "1"}, // vertex figure not simplex
          {"            (1)---(0)-4-(1)---(1) Runcitruncated 24-cell (dual)",   "1"}, // vertex figure not simplex
          {"            (0)---(1)-4-(1)---(1) Cantitruncated 24-cell (dual)",   "1,3(4.0)"},
          {"            (1)---(1)-4-(1)---(1) Omnitruncated 24-cell (omnitrunced cubes and hexprisms)", "1,3"},
          {"        120-cell/600-cell based"},
          {"            (1)-5-(0)---(0)---(0) 120-cell (hypermegaminx)",        "1,3(2.5),3"},
          {"            (1)-5-(.5)---(0)---(0) Barely truncated 120-cell",      "1,3(5.0)"},
          {"            (1)-5-(1)---(0)---(0) Truncated 120-cell (truncated dodecas and tets)", "1,3(5.0)"},
          {"            (0)-5-(1)---(0)---(0) Rectified 120 (icosidodecas and tets)", "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1)---(0) Bitruncated 120-cell/600-cell (truncated icosas and truncated tets)", "1,3(4.0)"},
          {"            (0)-5-(0)---(1)---(0) Rectified 600-cell (icosas and octas)", "1"}, // vertex figure not simplex
          {"            (0)-5-(0)---(1)---(1) Truncated 600-cell (icosas and truncated tets)", "1"}, // vertex figure not simplex
          {"            (0)-5-(0)---(.5)---(1) Barely truncated 600-cell",      "1"}, // vertex figure not simplex
          {"            (0)-5-(0)---(0)---(1) 600-cell",                        "1"}, //vertex figure not simplex
          {"            (1)-5-(0)---(0)---(1) Runcinated 120/600-cell",         "1,2,3"}, // vertex figure not simplex, it's an octahedron! still sort of okay, maybe
          {"            (1)-5-(0)---(1)---(0) Cantellated 120-cell",            "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(0)---(1) Cantellated 600-cell",            "1"},  // vertex figure not simplex
          {"            (1)-5-(1)---(1)---(0) Cantitruncated 120-cell",         "1,3(4)"},
          {"            (1)-5-(1)---(0)---(1) Runcitruncated 120-cell",         "1"}, // vertex figure not simplex
          {"            (1)-5-(0)---(1)---(1) Runcitruncated 600-cell",         "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1)---(1) Cantitruncated 600-cell",         "1,3"},
          {"            (1)-5-(1)---(1)---(1) Omnitruncated 120-cell/600-cell", "1,3"},
          {"    4d uniform anomalous"},
          {"        Grand Antiprism", "1"},
          {"    3d regular x 1d  hyperprisms"},
          {"        {3,3}x{} Tetrahedral prism",                "1,3(5.0),5(9.0),7(13.0)"},
          {"        {4,3}x{} Hypercube",                        "1,2,3,4,5,6,7,8,9,3(2.1),3(10.0)"},
          {"        {5,3}x{} Dodecahedral prism",               "1,2,3(2.5),3,4,5,6,7"},
          {"        {}x{5,3} Dodecahedral prism (alt)",         "1,2,3(2.5),3,4,5,6,7"},
          {"    3d wythoff x 1d  hyperprisms"},
          {"        Tetrahedron based"},
          {"            (1)---(0)---(0)x{} Tetrahedral prism",                  "1,3(5.0)"},
          {"            (1)---(1)---(0)x{} Truncated-tetrahedron prism",        "1,3(4.0)"},
          {"            (0)---(1)---(0)x{} Octahedral prism",                   "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1)x{} Truncated-tetrahedron(dual) prism",  "1,3(4.0)"},
          {"            (0)---(0)---(1)x{} Tetrahedral(dual) prism",            "1,3(5.0)"},
          {"            (1)---(0)---(1)x{} Cuboctahedral prism",                "1"}, // vertex figure not simplex
          {"            (1)---(1)---(1)x{} Omnitruncated-tetrahedron prism",    "1,3"}, 
          {"        Cube/Octahedron based"},
          {"            (1)-4-(0)---(0)x{} Cube prism (hypercube)",             "1,3"},
          {"            (1)-4-(1)---(0)x{} Truncated-cube prism",               "1,3(4.0)"},
          {"            (0)-4-(1)---(0)x{} Cuboctahedral prism",                "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1)x{} Truncated-octahedron prism",         "1,3"},
          {"            (0)-4-(0)---(1)x{} Octahedral prism",                   "1"}, // vertex figure not simplex
          {"            (1)-4-(0)---(1)x{} Rhombicuboctahedral prism",          "1"}, // vertex figure not simplex
          {"            (1)-4-(1)---(1)x{} Omnitruncated-cube prism",           "1,3"},
          {"        Dodecahedron/Icosahedron based"},
          {"            (1)-5-(0)---(0)x{} Dodecahedral prism",                 "1,3"},
          {"            (1)-5-(1)---(0)x{} Truncated-dodecahedron prism",       "1,3(4.0)"},
          {"            (0)-5-(1)---(0)x{} Icosidodecahedral prism",            "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1)x{} Truncated-icosahedron (soccer ball) prism",        "1,3"},
          {"            (0)-5-(0)---(1)x{} Icosahedral prism",                  "1"}, // vertex figure not simplex
          {"            (1)-5-(0)---(1)x{} Rhombicosidodecahedral prism",       "1"}, // vertex figure not simplex
          {"            (1)-5-(1)---(1)x{} Omnitruncated-dodecahedron prism",   "1,3"},
          {"    2d x 2d  duoprisms"},
          {"        {3}x{4} Triangular prism prism",            "1,3(4.0),5(7.0),7(10.0)"},
          {"        {4}x{4} Hypercube",                         "1,2,3,4,5,6,7,8,9,3(2.1),3(10.0)"},
          {"        {5}x{4} Pentagonal prism prism",            "1,2,3(2.5),3,4,5,6,7"},
          {"        {4}x{5} Pentagonal prism prism (alt)",      "1,2,3(2.5),3,4,5,6,7"},
          {"        {6}x{4} Hexagonal prism prism",             "1,2,3(2.5),3,4,5,6,7"},
          {"        {7}x{4} Heptagonal prism prism",            "1,2,3(2.5),3,4,5,6,7"},
          {"        {8}x{4} Octagonal prism prism",             "1,2,3(2.5),3,4,5,6,7"},
          {"        {9}x{4} Nonagonal prism prism",             "1,2,3(2.5),3,4,5,6,7"},
          {"        {10}x{4} Decagonal prism prism",            "1,2,3(2.5),3,4,5,6,7"},
          {"        {50}x{4} Fiftyagonal prism prism",          "1,3"},
          {"        {100}x{4} Onehundredagonal prism prism",    "1,3"},
          {"        {3}x{3}",                                   "1,3(4.0),5(7.0),7(10.0)"},
          {"        {3}x{5}",                                   "1,3(4.0),5(7.0),7(10.0)"},
          {"        {5}x{5}",                                   "1,2,3(2.5),3,4,5,6,7"},
          {"        {5}x{10}",                                  "1,3(2.5),3"},
          {"        {10}x{5}",                                  "1,3(2.5),3"},
          {"        {10}x{10}",                                 "1,3(2.5),3"},
          {"    -"},
          {"    4d nonuniform boxes"},  // note, the parser treats \(\d+\)(x\(\d+\))* as a special case, in which it doesn't split the lengths spec
          {"        (1)x(1)x(1)x(2)", "1,1,1,2"},
          {"        (2)x(2)x(2)x(1)", "2,2,2,1"},
          {"        (3)x(3)x(3)x(1)", "2,2,2,1"},
          {"        (4)x(3)x(2)x(1)", "4,3,2,1"},
          {"        -"},
          {"        (2)x(2)x(2)x(3)", "2,2,2,3"},
          {"        (3)x(3)x(3)x(2)", "3,3,3,2"},
          {"        (5)x(4)x(3)x(2)", "5,4,3,2"},
          {"    4d highly irregular"},
          {"        Fruity (work in progress, currently sucks)",    "1,3(9.0)"},
          {"5d puzzles"},
          {"    {3,3,3,3} Simplex",        "1,3(6.0),5(11.0)"},
          {"    {4,3,3,3} Hypercube",      "1,2,3,4,5"},
          {"    {3,3,3,4} Cross",          "1,2,3"},
          {"6d puzzles"},
          {"    {3,3,3,3,3} Simplex",      "1,3(7.0)"},
          {"    {4,3,3,3,3} Hypercube",    "1,2,3"},
          {"    {3,3,3,3,4} Cross",        "1,2,3"},
          {"-"}, // separator
          //{"Invent my own!",}, // XXX currently done by the older code down below-- need to port and get rid of that
        }; // menuScheme

        java.util.Stack<Object> menuStack = new java.util.Stack<Object>();  // of Menu or JMenu
        menuStack.push(puzzlemenu);
        for (int i = 0; i < menuScheme.length; ++i)
        {
            CHECK(menuScheme[i].length <= 2);
            String item0 = menuScheme[i][0];
            String item1 = (menuScheme[i].length==1 ? null : menuScheme[i][1]);

            String item0Trimmed = item0.trim();
            int nLeadingSpaces = item0.length() - item0Trimmed.length();
            CHECK(nLeadingSpaces % 4 == 0);
            int depth = nLeadingSpaces/4 + 1; // our whole scheme is at depth 1 already
            item0 = item0.substring(nLeadingSpaces);
            int nLeadingSpacesInNext = i+1==menuScheme.length ? 0 : menuScheme[i+1][0].length()-menuScheme[i+1][0].trim().length();
            boolean isSubmenu = nLeadingSpacesInNext > nLeadingSpaces || item1 != null;

            //System.out.println("item0 = "+item0);
            //System.out.println("    depth = "+depth);
            //System.out.println("    menuStack.size() = "+menuStack.size());
            while (depth < menuStack.size())
            {
                //System.out.println("    popping");
                menuStack.pop();
            }
            CHECK(depth == menuStack.size());
            if (item0.equals("-"))
            {
                CHECK(!isSubmenu);
                menuFactory.addSeparator(menuStack.peek());
            }
            else if (!isSubmenu)
            {
                // Note, this isn't very useful yet.
                // Need some more notation for it to take the role of a subcategory title within a menu, or something.
                // E.g. all within one menu:
                //      3d puzzles
                //        this >
                //        that >
                //        theother >
                CHECK(false);  // Note: this never happens. should I just remove the case?
                menuFactory.add(menuStack.peek(), item0);  // this won't work! second arg must be Menu or JMenu; String isn't implemented (because I'm not sure there's a legit use case for it)
            }
            else
            {
                Object submenu = menuFactory.newMenu(item0+"    "); // padding so the > doesn't clobber the end of the longest names!? lame

                menuFactory.add(menuStack.peek(), submenu);

                //System.out.println("    pushing");
                menuStack.push(submenu);
                if (item1 != null)
                {
                    final String finalName = item0; // including the schlafli symbol
                    final String finalSchlafli = (item0.equalsIgnoreCase("Grand Antiprism") ? item0 : item0.split(" ")[0]);
                    // Special case: if finalSchlafli is a nonuniform (or not) box such as (4)x(3)x(2), then don't split up item1, just treat it as one length specification
                    String lengthStrings[] = finalSchlafli.matches("\\(\\d+\\)(x\\(\\d+\\))*") ? new String[] {item1} : item1.split(",");

                    boolean sanityCheckMenuScheme = false; // XXX make option for this?  hardcoding for now
                    if (sanityCheckMenuScheme)
                    {
                        // Convert the polytope product string
                        // to a spec, and replace 5's with 4's
                        // (to make a simplified proxy, that has triangles
                        // iff the original has triangles,
                        // and has tets iff the original has tets),
                        // and make the polytope product.
                        // Then do sanity checks on it with the different lengths.
                        progressWriter.println("    Checking sanity on schlafli \""+finalSchlafli+"\"");
                        progressWriter.flush();
                        Object[][] schlaflisAndWythoffs = com.donhatchsw.util.CSG.makeRegularStarPolytopeProductSchlaflisAndWythoffsFromString(finalSchlafli);
                        int schlaflis[][] = (int[][])schlaflisAndWythoffs[0];
                        int schlaflisDenoms[][] = (int[][])schlaflisAndWythoffs[1];
                        double wythoffs[][] = (double[][])schlaflisAndWythoffs[2];
                        CHECK(schlaflis.length == schlaflisDenoms.length);
                        CHECK(schlaflis.length == wythoffs.length);
                        for (int iFactor = 0; iFactor < schlaflis.length; ++iFactor)
                        {
                            int schlafli[] = schlaflis[iFactor];
                            int schlafliDenoms[] = schlaflisDenoms[iFactor];
                            double wythoff[] = wythoffs[iFactor];
                            CHECK(schlafli.length == schlafliDenoms.length);
                            CHECK(schlafli.length == wythoff.length-1);
                            for (int iSchlafli = 0; iSchlafli < schlafli.length; ++iSchlafli)
                            {
                                if (schlafli[iSchlafli] == 5 && schlafliDenoms[iSchlafli] == 1
                                 && !(iSchlafli+1 < schlafli.length && schlafli[iSchlafli+1] == 0)) // don't do it if grand antiprism hack, or it will barf on {4,0,3}
                                {
                                    progressWriter.println("        proxying 4 for 5");
                                    schlafli[iSchlafli] = 4;
                                }
                            }
                        }
                        progressWriter.println("        making the non-subdivided polytope");
                        progressWriter.flush();
                        com.donhatchsw.util.CSG.SPolytope proxy = com.donhatchsw.util.CSG.makeRegularStarPolytopeProduct(schlaflis, schlaflisDenoms, wythoffs);
                        progressWriter.println("        getting all elements");
                        progressWriter.flush();
                        com.donhatchsw.util.CSG.Polytope allElts[][] = proxy.p.getAllElements();
                        for (int j = 0; j < lengthStrings.length; ++j)
                        {
                            String lengthString = lengthStrings[j];

                            progressWriter.println("            "+lengthString+":");
                            progressWriter.flush();

                            int intLength = 0;
                            double doubleLength = 0.;
                            // XXX duplicated from elsewhere... need to make a function I think
                            {
                                lengthString = lengthString.trim();

                                try {
                                    //System.out.println("lengthString = "+lengthString);

                                    java.util.regex.Matcher matcher =
                                    java.util.regex.Pattern.compile(
                                        "(\\d+)\\((.*)\\)"
                                    ).matcher(lengthString);
                                    if (matcher.matches())
                                    {
                                        String intPart = matcher.group(1);
                                        String doublePart = matcher.group(2);
                                        //System.out.println("intPart = "+intPart);
                                        //System.out.println("doublePart = "+doublePart);

                                        intLength = Integer.parseInt(intPart);
                                        doubleLength = Double.parseDouble(doublePart);
                                    }
                                    else
                                    {
                                        doubleLength = Double.parseDouble(lengthString);
                                        intLength = (int)Math.ceil(doubleLength);
                                    }
                                }
                                catch (java.lang.NumberFormatException e)
                                {
                                    //System.err.println("Your invention sucks! \""+lengthString+"\" is not a number! (or comma-separated list of numbers, with optional overrides, one for each dimension)");
                                    //initPuzzleCallback.call(); // XXX really just want a repaint I think
                                    //return;
                                    CHECK(false);
                                }
                                //System.out.println("intLength = "+intLength);
                                //System.out.println("doubleLength = "+doubleLength);
                            }
                            if (intLength % 2 == 0)
                            {
                                CHECK(doubleLength == (double)intLength);
                                intLength++;
                                doubleLength += .01;  // make a very thin slice in the middle, just to avoid degeneracies during this sanity check.  we're not actually making anything here.
                            }
                            CHECK(intLength % 2 == 1);
                            for (int iDim = 2; iDim < allElts.length-1; ++iDim) // triangle, tetrahedron, up to but not including the whole polytope
                            {
                                // XXX huh? what's the logic here? revisit this, it fails!  On "{3,4} with edge length 2"
                                // If it has a triangle, doubleLength must be > (3*intLength-1)/2-1.
                                // If it has a tetrahedron, doubleLength must be > (4*intLength-2)/2-1
                                // etc.
                                boolean hasASimplexOfThisDimension = false; // until proven otherwise
                                for (int iElt = 0; iElt < allElts[iDim].length; ++iElt)
                                {
                                    if (allElts[iDim][iElt].getAllElements()[0].length == iDim+1)
                                    {
                                        hasASimplexOfThisDimension = true;
                                        break;
                                    }
                                }
                                if (hasASimplexOfThisDimension)
                                {
                                    if (!(doubleLength > ((iDim+1)*intLength-(iDim-1))/2 - 1))
                                    {
                                        progressWriter.println("            WARNING: "+
                                                               (iDim==2 ? "triangle" : iDim==3 ? "tetrahedron" : "simplex of dimension "+iDim)
                                                               +" with length "+intLength+"("+doubleLength+")");
                                        progressWriter.flush();
                                    }
                                    CHECK(doubleLength > ((iDim+1)*intLength-(iDim-1))/2 - 1);
                                }
                            }
                        } // for each lengthString

                        // Some other consistency checks on the list of lengths for this polytope...
                        boolean vertexFigureIsSimplex;
                        {
                            progressWriter.println("        getting all incidences");
                            progressWriter.flush();
                            int allIncidences[][][][] = proxy.p.getAllIncidences();
                            for (int iVert = 0; iVert < allIncidences[0].length; ++iVert)
                            {
                                // allIncidences[0][iVert][1] is the list of all edges incident
                                // on this vertex
                                CHECK(allIncidences[0][iVert][1].length
                                    == allIncidences[0][0][1].length);
                            }
                            vertexFigureIsSimplex = (allIncidences[0][0][1].length == proxy.p.dim);
                        }

                        if (!vertexFigureIsSimplex)
                        {
                            CHECK(lengthStrings.length == 1);
                            CHECK(lengthStrings[0].equals("1"));
                        }
                        else
                        {
                            CHECK(lengthStrings.length > 1);
                            // TODO: if doesn't have a tet, there should be a 3(4)
                            // TODO: if doesn't have a triangle, there should be a 3(3)
                            // TODO: if has a penta, should be a 3(2.5) (if other conditions allow it)
                        }

                        progressWriter.flush(); // XXX HEY! at least one flush around here was necessary or I don't see any output at all!?  that makes me think something bogus is going on
                    }

                    for (int j = 0; j < lengthStrings.length; ++j)
                    {
                        final String finalLengthString = lengthStrings[j];
                        // CHECK(false);  // XXX coverage -- this is the one that is used
                        menuFactory.add(submenu, menuFactory.newMenuItem(finalLengthString, new ActionListener() {
                            @Override public void actionPerformed(ActionEvent ae)
                            {
                                String name = finalName;
                                String schlafli = finalSchlafli;
                                String lengthString = finalLengthString;

                                //System.out.println("    name = "+name);
                                //System.out.println("    schlafli = "+schlafli);
                                //System.out.println("    lengthString = "+lengthString);

                                if (schlafli == null)
                                {
                                    String prompt = "Enter your invention:";
                                    String initialInput = "{4,3,3} 3";

                                    while (true)
                                    {
                                        // The version that just takes the message and initial text doesn't exist before 1.4
                                        //String reply = JOptionPane.showInputDialog(prompt, initialInput);
                                        String reply = (String)JOptionPane.showInputDialog(
                                                null,
                                                prompt,
                                                null, // title
                                                JOptionPane.QUESTION_MESSAGE,
                                                null, // icon
                                                null, // selectionValues
                                                initialInput);
                                        if (reply == null)
                                        {
                                            initPuzzleCallback.call(); // XXX really just want a repaint I think
                                            return; // cancelled
                                        }
                                        String schlafliAndLength[] = reply.trim().split("\\s+");
                                        if (schlafliAndLength.length != 2)
                                        {
                                            prompt = "Your invention sucks!\nYou must specify the schlafli product symbol (with no spaces),\nfollowed by a space, followed by the puzzle length (or comma-separated list of lengths, with optional overrides, one for each dimension). Try again! (during sanity check)";
                                            initialInput = reply;
                                            continue;
                                        }
                                        schlafli = schlafliAndLength[0];
                                        lengthString = schlafliAndLength[1];
                                        name = "My own invention!  "+schlafli;
                                        break; // got it
                                    }
                                }
                                GenericPuzzleDescription newPuzzle = null;
                                try
                                {
                                    newPuzzle = new PolytopePuzzleDescription(schlafli+" "+lengthString, progressWriter, /*progressCallbacks=*/null);
                                }
                                catch (Throwable t)
                                {
                                    //t.printStacktrace();
                                    String explanation = t.toString();
                                    // yes, this is lame... AND the user
                                    // can't even cut and paste it to mail it to me
                                    if (explanation.equals("java.lang.Error: CHECK failed"))
                                    {
                                        java.io.StringWriter sw = new java.io.StringWriter();
                                        t.printStackTrace(new java.io.PrintWriter(sw));
                                        explanation = "\n" + sw.toString();
                                    }
                                    JOptionPane.showMessageDialog(null,
                                        "Something went horribly wrong when trying to build your invention \""+schlafli+"  "+lengthString+"\":\n"+explanation,
                                        "Your Invention Sucks",
                                        JOptionPane.ERROR_MESSAGE);
                                    return;
                                }

                                int nDims = newPuzzle.nDims();
                                if (nDims > 4)
                                {
                                    JOptionPane.showMessageDialog(null,
                                        "Re: Your invention \""+schlafli+"  "+lengthString+"\"\n"+
                                        "\n"+
                                        "That is a truly BRILLIANT "+nDims+"-dimensional invention.\n"+
                                        "It has:\n"+
                                        "        "+newPuzzle.nFaces()+" faces\n"+
                                        "        "+newPuzzle.nStickers()+" stickers\n"+
                                        "        "+newPuzzle.nCubies()+" visible cubie"+(newPuzzle.nCubies()==1?"":"s")+"\n"+
                                        "        "+newPuzzle.nVerts()+" sticker vertices\n"+
                                        "However, we are accepting only 4-dimensional inventions at this time.",
                                        "Invention Rejection Form Letter",
                                        JOptionPane.ERROR_MESSAGE);
                                    // XXX Lame, should try to get back in the loop and prompt again instead
                                    return;
                                }
                                model = new MC4DModel(newPuzzle);

                                undoq.clear();
                                undoPartSize = 0;

                                model.controllerUndoTreeSquirrel.Clear();
                                model.animationUndoTreeSquirrel.setCurrentNodeIndex(model.controllerUndoTreeSquirrel.getCurrentNodeIndex());

                                // PropertyManager.userprefs.setProperty("genericSchlafli", schlafli); // XXX not yet
                                // PropertyManager.userprefs.setProperty("genericLength", ""+len); // XXX not yet
                                initPuzzleCallback.call(); // really just want a repaint I think
                                String statuslabel = name + "  length="+lengthString;
                                setText(statusLabel, statuslabel); // XXX BUG - hey, it's not set right on program startup!

                                untwistedFrame = new GenericPipelineUtils.Frame();
                                twistingFrame = new GenericPipelineUtils.Frame();
                                    { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something
                            }
                        }));
                    } // for each length
                } // if item1 != null
            }
        } // for each pair in menuScheme



        if (true)
        {
            // XXX left over from previous implementation of the table...
            // XXX need to port the "invent my own" item and get rid of this!
            String table[][] = {
                {null,       "",                  "Invent my own!"},
            };
            for (int i = 0; i < table.length; ++i)
            {
                if (table[i][0] != null && table[i][0].equals("-"))
                {
                    menuFactory.add(puzzlemenu, "-"); // separator
                    continue;
                }

                final String finalSchlafli = table[i][0];
                String lengthStrings[] = table[i][1].split(",");
                final String finalName = (finalSchlafli==null ? table[i][2] :
                                          finalSchlafli + "  " + table[i][2]);

                // Puzzles with triangles kind of suck so far,
                // so we might want to leave them out of the menu...
                boolean allowPuzzlesWithTriangles = true;
                //boolean allowPuzzlesWithTriangles = false;
                if (!allowPuzzlesWithTriangles)
                {
                    if (finalSchlafli != null && finalSchlafli.indexOf("{3") != -1)
                        continue;
                }

                Object submenu;  // Menu or JMenu
                if (finalSchlafli != null)
                {
                    submenu = menuFactory.newMenu(finalName+"    "); // XXX padding so the > doesn't clobber the end of the longest names!? lame
                    menuFactory.add(puzzlemenu, submenu);
                }
                else
                    submenu = puzzlemenu;
                for (int j = 0; j < lengthStrings.length; ++j)
                {
                    final String finalLengthString = lengthStrings[j];
                    // CHECK(false);  // XXX coverage - this is used too
                    menuFactory.add(submenu, menuFactory.newMenuItem(finalSchlafli==null ? finalName : ""+finalLengthString, new ActionListener() {
                        @Override public void actionPerformed(ActionEvent ae)
                        {
                            String schlafli = finalSchlafli;
                            String lengthString = finalLengthString;
                            String name = finalName;
                            if (schlafli == null)
                            {
                                String prompt = "Enter your invention:";
                                String initialInput = "{4,3,3} 3";

                                while (true)
                                {
                                    // The version that just takes the message and initial text doesn't exist before 1.4
                                    //String reply = JOptionPane.showInputDialog(prompt, initialInput);
                                    String reply = (String)JOptionPane.showInputDialog(
                                            null,
                                            prompt,
                                            null, // title
                                            JOptionPane.QUESTION_MESSAGE,
                                            null, // icon
                                            null, // selectionValues
                                            initialInput);
                                    if (reply == null)
                                    {
                                        initPuzzleCallback.call(); // XXX really just want a repaint I think
                                        return; // cancelled
                                    }
                                    String schlafliAndLength[] = reply.trim().split("\\s+");
                                    if (schlafliAndLength.length != 2)
                                    {
                                        prompt = "Your invention sucks!\nYou must specify the schlafli product symbol (with no spaces),\nfollowed by a space, followed by the puzzle length (or comma-separated list of lengths, with optional overrides, one for each dimension). Try again!";
                                        initialInput = reply;
                                        continue;
                                    }
                                    schlafli = schlafliAndLength[0];
                                    lengthString = schlafliAndLength[1];
                                    name = "My own invention!  "+schlafli;
                                    break; // got it
                                }
                            }
                            GenericPuzzleDescription newPuzzle = null;
                            try
                            {
                                newPuzzle = new PolytopePuzzleDescription(schlafli+" "+lengthString, progressWriter, /*progressCallbacks=*/null);
                            }
                            catch (Throwable t)
                            {
                                //t.printStacktrace();
                                String explanation = t.toString();
                                // yes, this is lame... AND the user
                                // can't even cut and paste it to mail it to me
                                if (explanation.equals("java.lang.Error: CHECK failed"))
                                {
                                    java.io.StringWriter sw = new java.io.StringWriter();
                                    t.printStackTrace(new java.io.PrintWriter(sw));
                                    explanation = "\n" + sw.toString();
                                }
                                JOptionPane.showMessageDialog(null,
                                    "Something went horribly wrong when trying to build your invention \""+schlafli+"  "+lengthString+"\":\n"+explanation,
                                    "Your Invention Sucks",
                                    JOptionPane.ERROR_MESSAGE);
                                initPuzzleCallback.call(); // XXX really just want a repaint I think
                                return;
                            }

                            int nDims = newPuzzle.nDims();
                            if (nDims > 4)
                            {
                                JOptionPane.showMessageDialog(null,
                                    "Re: Your invention \""+schlafli+"  "+lengthString+"\"\n"+
                                    "\n"+
                                    "That is a truly BRILLIANT "+nDims+"-dimensional invention.\n"+
                                    "It has:\n"+
                                    "        "+newPuzzle.nFaces()+" faces\n"+
                                    "        "+newPuzzle.nStickers()+" stickers\n"+
                                    "        "+newPuzzle.nCubies()+" visible cubie"+(newPuzzle.nCubies()==1?"":"s")+"\n"+
                                    "        "+newPuzzle.nVerts()+" sticker vertices\n"+
                                    "However, we are accepting only 4-dimensional inventions at this time.",
                                    "Invention Rejection Form Letter",
                                    JOptionPane.ERROR_MESSAGE);
                                // XXX Lame, should try to get back in the loop and prompt again instead
                                return;
                            }
                            model = new MC4DModel(newPuzzle);

                            undoq.clear();
                            undoPartSize = 0;

                            model.controllerUndoTreeSquirrel.Clear();
                            model.animationUndoTreeSquirrel.setCurrentNodeIndex(model.controllerUndoTreeSquirrel.getCurrentNodeIndex());

                            // PropertyManager.userprefs.setProperty("genericSchlafli", schlafli); // XXX not yet
                            // PropertyManager.userprefs.setProperty("genericLength", ""+len); // XXX not yet
                            initPuzzleCallback.call(); // really just want a repaint I think
                            String statuslabel = name + "  length="+lengthString;
                            setText(statusLabel, statuslabel); // XXX BUG - hey, it's not set right on program startup!

                            untwistedFrame = new GenericPipelineUtils.Frame();
                            twistingFrame = new GenericPipelineUtils.Frame();
                                { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something
                        }
                    }));
                }
            }
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue.addMoreItemsToPuzzleMenu");
    } // addMoreItemsToPuzzleMenu

    public void undoAction(Component view, JLabel statusLabel, float nFrames90)
    {
        GenericGlue glue = this;
        if (glue.undoPartSize > 0)
        {
            if (glue.iTwist < glue.nTwist)
            {
                // Twist already in progress.
                // XXX should add this one to a queue.
                // XXX for now, just prohibit it.
                System.err.println("    BEEP! SLOW DOWN!"); // XXX obnoxious
                return;
            }

            GenericGlue.HistoryNode node = glue.undoq.get(--glue.undoPartSize);

            //
            // Initiate the undo twist (opposite dir from original)
            //
            int order = model.genericPuzzleDescription.getGripSymmetryOrders(node.futtIfPossible)[node.iGrip];
            double totalRotationAngle = 2*Math.PI/order*Math.abs(node.dir);
            glue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
            if (glue.nTwist == 0) glue.nTwist = 1;
            glue.iTwist = 0;
            glue.iTwistGrip = node.iGrip;
            glue.twistDir_field = -node.dir;
            glue.twistSliceMask = node.slicemask;
            glue.twistFuttIfPossible = node.futtIfPossible;

            view.repaint();
        }
        else
            statusLabel.setText("Nothing to undo.");

        if (model.controllerUndoTreeSquirrel.undo() == null)
            statusLabel.setText("Nothing to undo.");

    } // undoAction

    public void redoAction(Component view, JLabel statusLabel, float nFrames90)
    {
        GenericGlue glue = this;
        if (glue.undoq.size()-glue.undoPartSize > 0)
        {
            if (glue.iTwist < glue.nTwist)
            {
                // Twist already in progress.
                // XXX should add this one to a queue.
                // XXX for now, just prohibit it.
                System.err.println("    BEEP! SLOW DOWN!"); // XXX obnoxious
                return;
            }

            GenericGlue.HistoryNode node = glue.undoq.get(glue.undoPartSize++);

            //
            // Initiate the redo twist (same dir as original)
            //
            int order = model.genericPuzzleDescription.getGripSymmetryOrders(node.futtIfPossible)[node.iGrip];
            double totalRotationAngle = 2*Math.PI/order*Math.abs(node.dir);
            glue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
            if (glue.nTwist == 0) glue.nTwist = 1;
            glue.iTwist = 0;
            glue.iTwistGrip = node.iGrip;
            glue.twistDir_field = node.dir;
            glue.twistSliceMask = node.slicemask;
            glue.twistFuttIfPossible = node.futtIfPossible;

            view.repaint();
        }
        else
            statusLabel.setText("Nothing to redo.");

        if (model.controllerUndoTreeSquirrel.redo() == null)
            statusLabel.setText("Nothing to redo.");
    } // redoAction

    public void cheatAction(Component view, JLabel statusLabel)
    {
        GenericGlue glue = this;
        glue.cheating = true; // each repaint will trigger another til done
        view.repaint();
        statusLabel.setText("");
    } // cheatAction

    public void scrambleAction(Component view, // Canvas or JPanel, probably
                               Component statusLabel, // Label or JLabel
                               int scramblechenfrengensen,
                               boolean futtIfPossible)
    {
        GenericGlue glue = this;
        int nDims = model.genericPuzzleDescription.nDims();

        // Are all grip orders trivial?  If so, we can't really scramble,
        // and if we try, the code below will endless loop.
        {
            int nGrips = model.genericPuzzleDescription.nGrips();
            boolean foundOne = false;
            for (int iGrip = 0; iGrip < nGrips; ++iGrip) {
                int iFace = model.genericPuzzleDescription.getGrip2Face()[iGrip];
                int order = model.genericPuzzleDescription.getGripSymmetryOrders(futtIfPossible)[iGrip];
                if (!(order < 2 || (nDims==3 && order==2) || nDims==2 && order==4))
                {
                    foundOne = true;
                    break;
                }
            }
            if (!foundOne) {
                System.out.println("There are no scrambling twists!");
                return;
            }
        }

        java.util.Random rand = new java.util.Random();
        int previous_face = -1;
        for(int s = 0; s < scramblechenfrengensen; s++) {
            // select a random grip that is unrelated to the last one (if any)
            int iGrip, iFace, order;
            do {
                iGrip = rand.nextInt(model.genericPuzzleDescription.nGrips());
                iFace = model.genericPuzzleDescription.getGrip2Face()[iGrip];
                order = model.genericPuzzleDescription.getGripSymmetryOrders(futtIfPossible)[iGrip];
            }
            while (
                order < 2 || // don't use trivial ones
                (nDims==3 && order==2) || // don't use the cute flips, in 3d
                (nDims==2 && order==4) || // don't use the cute twirls, in 2d
                iFace == previous_face || // mixing it up
                (previous_face!=-1 && model.genericPuzzleDescription.getFace2OppositeFace()[previous_face] == iFace));
            previous_face = iFace;
            int slicemask = 1<<rand.nextInt(2); // XXX there's no getLength()! oh I think it's because I didn't think that was a generic enough concept to put in GenericPuzzleDescription, but I might have to rethink that.  for now, we just pick the first or second slice... this is fine for up to 4x, and even 5x (sort of)
            int dir = rand.nextBoolean() ? -1 : 1;

            // XXX let the model do this!!!!!
            model.genericPuzzleDescription.applyTwistToState(
                    model.genericPuzzleState,
                    iGrip,
                    dir,
                    slicemask,
                    futtIfPossible);

            // clear redo part
            //glue.undoq.setSize(glue.undoPartSize); // argh, setSize doesn't exist
            //glue.undoq.removeRange(glue.undoPartSize, glue.undoq.size()); // argh, removeRange is protected
            while (glue.undoq.size() > glue.undoPartSize) glue.undoq.remove(glue.undoq.size()-1);

            glue.undoq.add(new GenericGlue.HistoryNode(
                                            iGrip,
                                            dir,
                                            slicemask,
                                            futtIfPossible));
            glue.undoPartSize++;

            // Do it in the undo tree too...
            // The undoq will be removed eventually
            MC4DModel.Twist twist = new MC4DModel.Twist(iGrip, dir, slicemask, futtIfPossible);
            model.controllerUndoTreeSquirrel.Do(twist);
        }
        model.animationUndoTreeSquirrel.setCurrentNodeIndex(model.controllerUndoTreeSquirrel.getCurrentNodeIndex());

        view.repaint();
        boolean fully = scramblechenfrengensen == MagicCube_FULL_SCRAMBLE;
        // scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL; XXX do we need to do this here?
        String labelText = fully ? "Fully Scrambled" : scramblechenfrengensen + " Random Twist" + (scramblechenfrengensen==1?"":"s");
        if (statusLabel instanceof JLabel)
            ((JLabel)statusLabel).setText(labelText);
        else
            ((Label)statusLabel).setText(labelText);
    } // scrambleAction





    public void mouseMovedAction(MouseEvent e,
                                 Component view)
    {
        GenericGlue genericGlue = this;
        int pickedStickerPoly[] = GenericPipelineUtils.pick(
                                        e.getX(), e.getY(),
                                        genericGlue.untwistedFrame);
        int newSticker = pickedStickerPoly!=null ? pickedStickerPoly[0] : -1;
        int newPoly = pickedStickerPoly!=null ? pickedStickerPoly[1] : -1;
        if (newSticker != genericGlue.iStickerUnderMouse
         || newPoly != genericGlue.iPolyUnderMouse)
        {
            genericGlue.iStickerUnderMouse = newSticker;
            genericGlue.iPolyUnderMouse = newPoly;
            view.repaint();
        }
    } // mouseMovedAction

    public void mouseClickedAction(MouseEvent e,
                                   float viewMat4d[/*4*/][/*4*/],
                                   float nFrames90,
                                   int slicemask,
                                   boolean futtIfPossible,

                                   Component viewForViewChanges,
                                   Component viewForModelChanges)
    {
        GenericGlue genericGlue = this;
        boolean isRotate = isMiddleMouseButton(e);
        if (false) // make this true to debug the pick
        {
            int hit[] = GenericPipelineUtils.pick(e.getX(), e.getY(),
                                                  genericGlue.untwistedFrame);
            if (hit != null)
            {
                int iSticker = hit[0];
                int iFace = model.genericPuzzleDescription.getSticker2Face()[iSticker];
                int iCubie = model.genericPuzzleDescription.getSticker2Cubie()[iSticker];
                System.err.println("    Hit sticker "+iSticker+"(polygon "+hit[1]+")");
                System.err.println("        face "+iFace);
                System.err.println("        cubie "+iCubie);
            }
        }

        if (isRotate)
        {
            boolean allowArbitraryElements = e.isControlDown();
            float nicePoint[] = GenericPipelineUtils.pickNicePointToRotateToCenter(
                             e.getX(), e.getY(),
                             allowArbitraryElements,
                             genericGlue.untwistedFrame);

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
                    viewMat4dD[i][j] = (double)viewMat4d[i][j];
                for (int i = 0; i < 4; ++i)
                    nicePointD[i] = (double)nicePoint[i];

                double nicePointOnScreen[] = com.donhatchsw.util.VecMath.vxm(nicePointD, viewMat4dD);
                com.donhatchsw.util.VecMath.normalize(nicePointOnScreen, nicePointOnScreen); // if it's not already
                float minusWAxis[] = {0,0,0,-1};
                genericGlue.rotationFrom = com.donhatchsw.util.VecMath.doubleToFloat(nicePointOnScreen);
                genericGlue.rotationTo = minusWAxis;

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
                    if (com.donhatchsw.util.VecMath.distsqrd(genericGlue.rotationFrom, genericGlue.rotationTo) <= 1e-4*1e-4)
                    {
                        float polyAndStickerAndFaceCenter[][] = GenericPipelineUtils.pickPolyAndStickerAndFaceCenter(
                             e.getX(), e.getY(),
                             genericGlue.untwistedFrame);
                        CHECK(polyAndStickerAndFaceCenter != null); // hit once, should hit again
                        float polyCenter[] = polyAndStickerAndFaceCenter[0];

                        // Interested in only the w component
                        // (and the z component if the puzzle is 2d).
                        // So zero out the first nDims dimensions...
                        polyCenter = com.donhatchsw.util.VecMath.copyvec(polyCenter);
                        com.donhatchsw.util.VecMath.zerovec(model.genericPuzzleDescription.nDims(),
                                                            polyCenter);
                        if (com.donhatchsw.util.VecMath.normsqrd(polyCenter) < 1e-4*1e-4)
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
                        float polyCenterOnScreen[] = com.donhatchsw.util.VecMath.vxm(polyCenter, viewMat4d);
                        genericGlue.rotationFrom = polyCenterOnScreen;
                        com.donhatchsw.util.VecMath.normalize(genericGlue.rotationFrom, genericGlue.rotationFrom);
                    }
                }

                double totalRotationAngle = com.donhatchsw.util.VecMath.angleBetweenUnitVectors(
                                    genericGlue.rotationFrom,
                                    genericGlue.rotationTo);

                genericGlue.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
                if (genericGlue.nRotation == 0) genericGlue.nRotation = 1;
                // XXX ARGH! we'd like the speed to vary as the user changes the slider,
                // XXX but the above essentially locks in the speed for this rotation
                genericGlue.iRotation = 0; // we are iRotation frames into nRotation
                viewForViewChanges.repaint();

                if (genericGlue.iRotation == genericGlue.nRotation)
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
                            e.getX(), e.getY(),
                            genericGlue.untwistedFrame);
            if (iGrip != -1)
            {
                int order = model.genericPuzzleDescription.getGripSymmetryOrders(futtIfPossible)[iGrip];

                if (false)
                {
                    System.err.println("    Grip "+iGrip+"");
                    System.err.println("        order "+order);
                }

                if (genericGlue.iTwist < genericGlue.nTwist)
                {
                    // Twist already in progress.
                    // XXX should add this one to a queue.
                    // XXX for now, just prohibit it.
                    System.err.println("    BEEP! SLOW DOWN!"); // XXX obnoxious
                    return;
                }


                if (order <= 0)
                {
                    System.err.println("Can't twist that.\n");
                    return;
                }

                int dir = (isLeftMouseButton(e) || isMiddleMouseButton(e)) ? 1 : -1; // ccw is 1, cw is -1

                if(e.isShiftDown()) // experimental control to allow double twists but also requires speed control.
                    dir *= 2;

                double totalRotationAngle = 2*Math.PI/order*Math.abs(dir);
                genericGlue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
                if (genericGlue.nTwist == 0) genericGlue.nTwist = 1;
                genericGlue.iTwist = 0;
                genericGlue.iTwistGrip = iGrip;
                genericGlue.twistDir_field = dir;
                genericGlue.twistSliceMask = slicemask;
                genericGlue.twistFuttIfPossible = futtIfPossible;

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

                // clear redo part
                //genericGlue.undoq.setSize(genericGlue.undoPartSize); // argh, setSize doesn't exist
                //genericGlue.undoq.removeRange(genericGlue.undoPartSize, genericGlue.undoq.size()); // argh, removeRange is protected
                while (genericGlue.undoq.size() > genericGlue.undoPartSize) genericGlue.undoq.remove(genericGlue.undoq.size()-1);

                genericGlue.undoq.add(new GenericGlue.HistoryNode(
                                                    genericGlue.iTwistGrip,
                                                    genericGlue.twistDir_field,
                                                    genericGlue.twistSliceMask,
                                                    genericGlue.twistFuttIfPossible));
                genericGlue.undoPartSize++;

                viewForModelChanges.repaint();
            }
            else
                System.out.println("missed");
        }
    } // mouseClickedAction

    //
    // Attempt to implement roll correction.
    //
        public static int findFaceCenterClosestToYZArc(float faceCenters[][],
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

        public void initiateZeroRoll(float viewMat4d[][],
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

            float faceCenters[][] = model.genericPuzzleDescription.getFaceCentersAtRest();
            float pointOnYZArc[] = new float[4]; // zeros... and [3] is left zero
            int iFace = findFaceCenterClosestToYZArc(faceCenters,
                                                     viewMat4d,
                                                     viewMat3d,
                                                     pointOnYZArc);
            this.rotationFrom = com.donhatchsw.util.VecMath.vxm(faceCenters[iFace], viewMat4d);
            this.rotationFrom[3] = 0.f;
            com.donhatchsw.util.VecMath.normalize(this.rotationFrom,
                                                  this.rotationFrom);
            // pointOnYZArc is now in screen space...
            // to get the point we want to rotate to,
            // we undo the viewMat3d on it,
            // i.e. apply viewMat3d's transpose, i.e. its inverse,
            // i.e. multiply by it on the opposite side as usual
            this.rotationTo = com.donhatchsw.util.VecMath.mxv(viewMat3d, pointOnYZArc);

            double totalRotationAngle = com.donhatchsw.util.VecMath.angleBetweenUnitVectors(
                                this.rotationFrom,
                                this.rotationTo);
            this.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * nFrames90); // XXX unscientific rounding
            if (this.nRotation == 0) this.nRotation = 1;
            this.iRotation = 0;

            // Remember the zero roll pole
            // for subsequent calls to zeroOutRollOnSpinDelta
            this.zeroRollPoleAfterRot3d = pointOnYZArc;

            //System.out.println("this.rotationFrom = "+com.donhatchsw.util.VecMath.toString(this.rotationFrom));
            //System.out.println("this.rotationTo = "+com.donhatchsw.util.VecMath.toString(this.rotationTo));

            view.repaint();
        } // initiateZeroRoll

        // Uses the zeroRollPole from the most recent call
        // to initiatezeroRoll.
        public float[][] zeroOutRollAndMaybeTiltOnSpinDelta(float spindelta[][],
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

            CHECK(zeroRollPoleAfterRot3d != null); // initiateZeroRoll must have been called previously
            zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.copyvec(3, zeroRollPoleAfterRot3d); // XXX sigh... because vxm and other stuff freaks if I don't
            // Clamp tilt to [0..pi/2]...
            double currentTilt = Math.atan2(zeroRollPoleAfterRot3d[2],
                                            zeroRollPoleAfterRot3d[1]);
            //System.out.println("tiltDeltaAngle before = "+tiltDeltaAngle*180/Math.PI);
            if (tiltDeltaAngle > Math.PI/2 - currentTilt)
                tiltDeltaAngle = Math.PI/2 - currentTilt;
            if (tiltDeltaAngle < 0 - currentTilt)
                tiltDeltaAngle = 0 - currentTilt;
            //System.out.println("tiltDeltaAngle after = "+tiltDeltaAngle*180/Math.PI);

            double twirlDelta[][] = com.donhatchsw.util.VecMath.makeRowRotMat(twirlDeltaAngle, new double[][]{com.donhatchsw.util.VecMath.floatToDouble(zeroRollPoleAfterRot3d)});
            double tiltDelta[][] = com.donhatchsw.util.VecMath.makeRowRotMat(tiltDeltaAngle, new double[][]{{1,0,0}});
            float adjustedSpinDelta[][] = com.donhatchsw.util.VecMath.doubleToFloat(com.donhatchsw.util.VecMath.mxm(twirlDelta, tiltDelta));

            // Gram-schmidt so we don't drift to non-orthogonal
            // XXX wasn't there a nicer more symmetric way of doing this?
            com.donhatchsw.util.VecMath.gramschmidt(adjustedSpinDelta,
                                                    adjustedSpinDelta);

            // need to apply it to the pole...
            zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.vxm(zeroRollPoleAfterRot3d,
                                                                     adjustedSpinDelta);
            return adjustedSpinDelta;
        } // zeroOutRollAndMaybeTiltOnSpinDelta

        // when dragging, we allow tilt changes
        public float[][] zeroOutRollOnSpinDelta(float spindelta[][])
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(spindelta, false);
        }
        // when autospinning, we don't allow tilt changes,
        // or it would just drift to the min or max tilt, which looks dumb
        public float[][] zeroOutRollAndTiltOnSpinDelta(float spindelta[][])
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(spindelta, true);
        }

    private static boolean isMiddleMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == java.awt.event.MouseEvent.BUTTON2;
    }
    private static boolean isLeftMouseButton(MouseEvent anEvent) {
        return anEvent.getButton() == java.awt.event.MouseEvent.BUTTON1;
    }


    //
    // Little test program that tries every puzzle description in the menu,
    // and makes sure no exceptions are thrown
    //
    public static void main(String args[])
    {
        System.out.println("in GenericGlue.main");
        //GenericGlue.verboseLevel = 1;
        GenericGlue glue = new GenericGlue(null);
        java.awt.Menu puzzlemenu = new Menu();
        Label statusLabel = new Label(); // Label or JLabel
        Callback initPuzzleCallback = new Callback() { @Override public void call() {} };
        int skip = (args.length > 0 ? Integer.parseInt(args[0]) : 0);
        glue.addMoreItemsToPuzzleMenu(puzzlemenu,
                                      statusLabel,
                                      initPuzzleCallback);
        int nDone = traverse(puzzlemenu, 0, 0, skip);
        System.out.println("nDone = "+nDone);
        System.out.println("out GenericGlue.main");
    } // main
    private static int traverse(java.awt.Menu menu, int level, int nDone, int skip)
    {
        for (int i = 0; i < level; ++i) System.out.print("    ");
        System.out.println("in traverse, level = "+level+", nDone="+nDone);

        for (int i = 0; i < level; ++i) System.out.print("    ");
        System.out.println("\""+menu.getLabel()+"\"");

        int nItems = menu.getItemCount();
        for (int iItem = 0; iItem < nItems; ++iItem)
        {
            MenuItem child = menu.getItem(iItem);
            if (child instanceof Menu)
            {
                nDone = traverse((Menu)child, level+1, nDone, skip);
            }
            else
            {
                for (int i = 0; i < level; ++i) System.out.print("    ");
                System.out.println("    "+child.getClass().getName()+": \""+child.getLabel()+"\"");



                // Bleah, in >= 1.3 there's getListeners()... what do I do in <= 1.2?  For now, we just use reflection so it will compile, and it will bomb if the runtime is <= 1.2
                //ActionListener[] listeners = (ActionListener[])child.getListeners(ActionListener.class);

                ActionListener[] listeners = null;
                {
                    Class<?> childClass = child.getClass();
                    java.lang.reflect.Method getListenersMethod = null;
                    try {
                        getListenersMethod = childClass.getMethod("getListeners", Class.class);
                    } catch (NoSuchMethodException e) {
                        System.err.println("NoSuchMethodException trying to get method getListeners on "+childClass);
                        System.err.println("I think that method was introduced in java 1.3, are you using <= 1.2?");

                        System.err.println("Sorry :-(");
                        System.exit(1);
                    };
                    try {
                        listeners = (ActionListener[])getListenersMethod.invoke(child, new Object[]{ActionListener.class});

                    } catch (IllegalAccessException e) {
                        System.err.println("IllegalAccessException trying to invoke getListeners!?");
                        System.exit(1);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        System.err.println("InvocationTargetException trying to invoke getListeners: "+e.getTargetException());
                        System.exit(1);
                    }
                }


                for (int i = 0; i < level; ++i) System.out.print("    ");
                System.out.println("    "+listeners.length+" action listener");
                CHECK(listeners.length <= 1);
                for (int iListener = 0; iListener < listeners.length; ++iListener)
                {
                    if (nDone >= skip)
                    {
                        for (int i = 0; i < level; ++i) System.out.print("    ");
                        System.out.println("    calling the listener action");
                        System.out.println("nDone = "+nDone);

                        ActionListener listener = listeners[iListener];
                        ActionEvent actionEvent = null;
                        listener.actionPerformed(actionEvent);
                    }
                    nDone++;
                }
            }
        }

        for (int i = 0; i < level; ++i) System.out.print("    ");
        System.out.println("out traverse, level = "+level+", nDone="+nDone);
        return nDone;
    }


} // class GenericGlue
