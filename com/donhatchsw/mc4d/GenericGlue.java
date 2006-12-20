//
// This file is mostly throwaway--
// it is an attempt to quickly glue the good new classes:
//      GenericPuzzleDescription (interface)
//      PolytopePuzzleDescription (implements GenericPuzzleDescription)
//      GenericPipelineUtils
// onto MC4DSwing/MC4DView with as minimal impact on the existing code
// as possible, prior to Melinda getting a look at it
// and figuring out where it should really go.
//
// Functions currently in here:
//
//    - GenericGlue() constructor
//            MC4DSwing creates one of these on startup and stores it
//            in its member genericGlue;
//            it also points the MC4DView's genericGlue member
//            to the same GenericGlue object.
//    - addMoreItemsToPuzzleMenu()
//            MC4DSwing should call this after it adds its
//            standard items to the puzzle menu.
//            This function installs callbacks that activate
//            and deactivate the genericGlue object
//            in response to the relevant PuzzleMenu items.
//    - isActive()
//            Whether this GenericGlue object is active
//            (i.e. contains a valid puzzle).
//            This will be true whenever the MC4DView's "current puzzle"
//            is a generic puzzle (rather than a standard one).
//
//    - undoAction()
//          use in place if the usual undo action when isActive() is true
//    - redoAction()
//          use in place if the usual redo action when isActive() is true
//    - cheatAction()
//          use in place if the usual cheat action when isActive() is true
//    - scrambleAction()
//          use in place if the usual cheat action when isActive() is true
//
//    - isAnimating()
//          use in place of the usual isAnimating when isActive() is true
//
//    - compute
//    - computeAndPaintFrame()
//          MC4DView calls this from its repaint() method
//          in place of its usual calls to paintFrame(),
//          if isActive() is true
//

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GenericGlue
{
    public static int verboseLevel = 0; // set to something else to debug

    //
    // State.  And not much of it!
    //
    public GenericPuzzleDescription genericPuzzleDescription = null;
    public int genericPuzzleState[] = null;

    //
    // A rotation is currently in progress if iRotation < nRotation.
    //
    public int nRotation = 0; // total number of rotation frames in progress
    public int iRotation = 0; // number of frames done so far
     public float rotationFrom[]; // where rotation is rotating from, in 4space
     public float rotationTo[]; // where rotation is rotating to, in 4space

    //
    // A twist is currently in progress if iTwist < nTwist.
    //
    public int nTwist = 0; // total number of twist frames in progress
    public int iTwist = 0; // number of twist frames done so far
     public int iTwistGrip;     // of twist in progress, if any
     public int twistDir;      // of twist in progress, if any
     public int twistSliceMask; // of twist in progress, if any
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
    // The sticker and cubie that the mouse is currently hovering over.
    //
    public int iStickerUnderMouse = -1;


    //
    // Rudimentaty undo queue.
    //
    public static class HistoryNode
    {
        public int iGrip;
        public int dir;
        public int slicemask;
        public HistoryNode(int iGrip, int dir, int slicemask)
        {
            this.iGrip = iGrip;
            this.dir = dir;
            this.slicemask = slicemask;
        }
    }
    public java.util.Vector undoq = new java.util.Vector(); // of HistoryNode
    public int undoPartSize = 0; // undoq has undo part followed by redo part

    //
    // Two scratch Frames to use for computing and painting.
    //
    public GenericPipelineUtils.Frame untwistedFrame = new GenericPipelineUtils.Frame();
    public GenericPipelineUtils.Frame twistingFrame = new GenericPipelineUtils.Frame();
        { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something


    //
    // Debugging state variables
    //
    public boolean useTopsort = true;
    public int jitterRadius = 0;
    public boolean drawLabels = false;
    public boolean showPartialOrder = false;
    public boolean frozenForDebugging = false;
        public int frozenPartialOrderForDebugging[][] = null;


    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    public interface Callback { public void call(); }



    public GenericGlue(String initialSchlafli, int initialLength)
    {
        super();
        if (verboseLevel >= 1) System.out.println("in GenericGlue ctor");
        if (initialSchlafli != null)
        {
            java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                                 new java.io.BufferedWriter(
                                                 new java.io.OutputStreamWriter(
                                                 System.err)));
            genericPuzzleDescription = new PolytopePuzzleDescription(
                initialSchlafli,
                initialLength, initialLength,
                progressWriter);
            genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue ctor");
    }

    public boolean isActive()
    {
        return genericPuzzleDescription != null;
    }
    public boolean isAnimating()
    {
        return !frozenForDebugging
            && (iRotation < nRotation
             || iTwist < nTwist);
    }

    // Call this from MC4DSwing ctor right after all
    // the other menu items are added
    public void addMoreItemsToPuzzleMenu(Menu puzzlemenu,
                                         final JLabel statusLabel,
                                         final Callback initPuzzleCallback)
    {
        if (verboseLevel >= 1) System.out.println("in GenericGlue.addMoreItemsToPuzzleMenu");

        // Used for reported progress during puzzle creation,
        // which can take a long time.
        // Currently just goes to System.err.
        final java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                                   new java.io.BufferedWriter(
                                                   new java.io.OutputStreamWriter(
                                                   System.err)));

        // Selecting any of the previously existing menu items
        // should have the side effect of setting
        // genericPuzzleDescription to null-- that's the indicator
        // that the glue overlay mechanism is no longer active.
        for (int i = 0; i < puzzlemenu.getItemCount(); ++i)
        {
            puzzlemenu.getItem(i).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    if (verboseLevel >= 1) System.out.println("GenericGlue: deactivating");
                    genericPuzzleDescription = null;
                    genericPuzzleState = null;
                }
            });
        }

        String menuScheme[][] = {
          {"-"}, // separator
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
          {"        {3,3} Tetrahedron (Meier-Halpern Pyramid (tm))",             "1,3(4.0),5(7.0),7(10.0),9(13.0),11(16.0)"},
          {"        {4,3} Cube (Rubik's Cube (tm))",                    "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21"},
          {"        {5,3} Dodecahedron (Megaminx)", "1,2,3(2.5),3,5,7,9"},
          {"    3d wythoff"},
          {"        Tetrahedron based"},
          {"            (1)---(0)---(0) Tetrahedron",                       "1,3"},
          {"            (1)---(1)---(0) Truncated tetrahedron",             "1,3"},
          {"            (0)---(1)---(0) Octahedron",                        "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1) Truncated tetrahedron (dual)",      "1,3"},
          {"            (0)---(0)---(1) Tetrahedron(dual)",                 "1,3"},
          {"            (1)---(0)---(1) Cantellated tetrahedron",       "1"}, // vertex figure not simplex
          {"            (1)---(1)---(1) Omnitruncated tetrahedron",         "1,3"}, 
          {"        Cube/Octahedron based"},
          {"            (1)-4-(0)---(0) Cube",                              "1,3"},
          {"            (1)-4-(1)---(0) Truncated cube",                    "1,3"},
          {"            (0)-4-(1)---(0) Cuboctahedron",                     "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1) Truncated octahedron",              "1,3"},
          {"            (0)-4-(0)---(1) Octahedron",                        "1"}, // vertex figure not simplex
          {"            (1)-4-(0)---(1) Cantellated cuboctahedron",         "1"}, // vertex figure not simplex
          {"            (1)-4-(1)---(1) Omnitruncated cuboctahedron",       "1,3"},
          {"        Dodecahedron/Icosahedron based"},
          {"            (1)-5-(0)---(0) Dodecahedron",                      "1,3"},
          {"            (1)-5-(1)---(0) Truncated dodecahedron",            "1,3"},
          {"            (0)-5-(1)---(0) Icosadodecahedron",                 "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1) Truncated icosahedron (soccer ball)", "1,3"},
          {"            (0)-5-(0)---(1) Icosahedron",                       "1"}, // vertex figure not simplex
          {"            (1)-5-(0)---(1) Cantellated icosadodecahedron",     "1"}, // vertex figure not simplex
          {"            (1)-5-(1)---(1) Omnitruncated dodecahedron",        "1,3"},
          {"    2d x 1d prisms"},
          {"        {3}x{} Triangular prism",    "1,2,3,4,5"},
          {"        {4}x{} Cube",                "1,2,3,4,5"},
          {"        {5}x{} Pentagonal prism",    "1,2,3,4,5"},
          {"        {6}x{} Hexagonal prism",     "1,2,3,4,5"},
          {"        {7}x{} Heptagonal prism",    "1,2,3,4,5"},
          {"        {8}x{} Octagonal prism",     "1,2,3,4,5"},
          {"        {9}x{} Nonagonal prism",     "1,2,3,4,5"},
          {"        {10}x{} Decagonal prism",    "1,2,3,4,5"},
          {"        {11}x{} Hendecagonal prism", "1,2,3,4,5"},
          {"        {12}x{} Dodecagonal prism",  "1,2,3,4,5"},
          {"4d puzzles"},
          {"    4d regular"},
          {"        {3,3,3} Simplex (5-cell)",          "1,3(5.0),5(9.0),7(13.0)"},
          {"        {4,3,3} Hypercube (8-cell)",        "1,2,3,4,5,6,7,8,9,3(2.1),3(10.0)"},
          {"        {3,3,4} Cross (16-cell)",           "1"}, // vertex figure not simplex
          {"        {3,4,3} 24-cell",                   "1"}, // vertex figure not simplex
          {"        {5,3,3} 120-cell (hypermegaminx)",  "1,2,3(2.5),3"},
          {"        {3,3,5} 600-cell",                  "1"}, // vertex figure not simplex
          {"    4d uniform wythoff"}, // XXX should be at bottom of menu, so when someone is shooting for the moon they always to to the bottom of each cascading menu?
          {"        Simplex based"},
          {"            (1)---(0)---(0)---(0) Simplex",                         "1,3(5.0),5(9.0),7(13.0)"},
          {"            (1)---(.5)---(0)---(0) Barely truncated simplex",      "1,3(9.0)"},
          {"            (1)---(1)---(0)---(0) Truncated simplex",               "1,3(5.0)"},
          {"            (0)---(1)---(0)---(0)   (octas and tets)",              "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1)---(0)   (truncated tets)",              "1,3,3(9.0)"},
          {"            (0)---(0)---(1)---(0)   (tets and octas)",              "1"}, // vertex figure not simplex
          {"            (0)---(0)---(1)---(1) Truncated simplex (dual)",        "1,3,3(9.0)"},
          {"            (0)---(0)---(.5)---(1) Barely truncated simplex (dual)","1,3,3(9.0)"},
          {"            (0)---(0)---(0)---(1) Simplex (dual)",                  "1,3,3(9.0)"},
          {"            (1)---(0)---(0)---(1)   (tets and triprisms)",          "1"}, // vertex figure not simplex, it's an octahedron! so all cells look okay but it's still not good
          {"            (1)---(0)---(1)---(0)",                                 "1"}, // vertex figure not simplex
          {"            (0)---(1)---(0)---(1)",                                 "1,3,3(9.0)"},
          {"            (1)---(1)---(1)---(0)",                                 "1,3,3(9.0)"},
          {"            (1)---(1)---(0)---(1)",                                 "1"}, // vertex figure not simplex
          {"            (1)---(0)---(1)---(1)",                                 "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1)---(1)",                                 "1,3,3(9.0)"},
          {"            (1)---(1)---(1)---(1) Omnitruncated simplex",           "1,3,3(9.0)"}, // XXX get rid of these 9s?
          {"        Hypercube/Cross based"},
          {"            (1)-4-(0)---(0)---(0) Hypercube",                       "1,3"},
          {"            (1)-4-(.5)---(0)---(0) Barely truncated hypercube",    "1,3(5.0)"},
          {"            (1)-4-(1)---(0)---(0) Truncated hypercube",             "1,3(5.0)"},
          {"            (0)-4-(1)---(0)---(0)   (cuboctas and tets)",           "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1)---(0)   (truncated octas and truncated tets)", "1,3"},
          {"            (0)-4-(0)---(1)---(0)   (octas and tets)",              "1"}, // vertex figure not simplex
          {"            (0)-4-(0)---(1)---(1) Truncated cross",                 "1,3"},
          {"            (0)-4-(0)---(.5)---(1) Barely truncated cross",        "1,3"},
          {"            (0)-4-(0)---(0)---(1) Cross",                           "1,3"},
          {"            (1)-4-(0)---(0)---(1)   (cubes, tets, and tri prisms)", "1"}, // vertex figure not simplex, it's an octahedron! so all cells look okay but it's still not good
          {"            (1)-4-(0)---(1)---(0)",                                 "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(0)---(1)   (cuboctas and cubes)",          "1"},  // vertex figure not simplex
          {"            (1)-4-(1)---(1)---(0)",                                 "1,3"},
          {"            (1)-4-(1)---(0)---(1)",                                 "1"}, // vertex figure not simplex
          {"            (1)-4-(0)---(1)---(1)",                                 "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1)---(1)   (truncated octas and cubes)",   "1,3"},
          {"            (1)-4-(1)---(1)---(1) Omnitruncated hypercube/cross",   "1,3"},
          {"        24-cell based"},
          {"            (1)---(0)-4-(0)---(0) 24-cell",                         "1,3(2.5),3"},
          {"            (1)---(.5)-4-(0)---(0) Barely truncated 24-cell",      "1,3"},
          {"            (1)---(1)-4-(0)---(0) Truncated 24-cell",               "1,3"},
          {"            (0)---(1)-4-(0)---(0)   (cuboctas and cubes)",          "1"}, // vertex figure not simplex
          {"            (0)---(1)-4-(1)---(0)   (truncated cubes)",             "1,3"},
          {"            (0)---(0)-4-(1)---(0)   (cubes and cuboctas)",          "1"}, // vertex figure not simplex
          {"            (0)---(0)-4-(1)---(1) Truncated 24-cell (dual)",        "1,3"},
          {"            (0)---(0)-4-(.5)---(1) Barely truncated 24-cell (dual)","1,3"},
          {"            (0)---(0)-4-(0)---(1) 24-cell (dual)",                  "1,3"},
          {"            (1)---(0)-4-(0)---(1)",                                 "1"}, // vertex figure not simplex, it's an octahedron! so all cells look okay but it's still not good
          {"            (1)---(0)-4-(1)---(0)",                                 "1"}, // vertex figure not simplex
          {"            (0)---(1)-4-(0)---(1)",                                 "1"},  // vertex figure not simplex
          {"            (1)---(1)-4-(1)---(0)",                                 "1,3"},
          {"            (1)---(1)-4-(0)---(1)",                                 "1"}, // vertex figure not simplex
          {"            (1)---(0)-4-(1)---(1)",                                 "1"}, // vertex figure not simplex
          {"            (0)---(1)-4-(1)---(1)",                                 "1,3"},
          {"            (1)---(1)-4-(1)---(1) Omnitruncated 24-cell",           "1,3"},
          {"        120-cell/600-cell based"},
          {"            (1)-5-(0)---(0)---(0) 120-cell (hypermegaminx)",        "1,3(2.5),3"},
          {"            (1)-5-(.5)---(0)---(0) Barely truncated 120-cell",    "1,3"},
          {"            (1)-5-(1)---(0)---(0) Truncated 120-cell",             "1,3(5.0)"},
          {"            (0)-5-(1)---(0)---(0) Rectified 120/600-cell (icosadodecas and tets)",           "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1)---(0) Bitruncated 120-cell (truncated icosas and truncated tets)", "1,3"},
          {"            (0)-5-(0)---(1)---(0) Rectified 600-cell (icosas and octas)",            "1"}, // vertex figure not simplex
          {"            (0)-5-(0)---(1)---(1) Truncated 600-cell",              "1,3"},
          {"            (0)-5-(0)---(.5)---(1) Barely truncated 600-cell",      "1,3"},
          {"            (0)-5-(0)---(0)---(1) 600-cell",                        "1,3"},
          {"            (1)-5-(0)---(0)---(1) Runcinated 120/600-cell",                                 "1"}, // vertex figure not simplex, it's an octahedron! so all cells look okay but it's still not good
          {"            (1)-5-(0)---(1)---(0) Cantellated 120-cell",            "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(0)---(1) Cantellated 600-cell",            "1"},  // vertex figure not simplex
          {"            (1)-5-(1)---(1)---(0) Cantitruncated 120-cell",         "1,3"},
          {"            (1)-5-(1)---(0)---(1) Runcitruncated 120-cell",         "1"}, // vertex figure not simplex
          {"            (1)-5-(0)---(1)---(1) Runcitruncated 600-cell",         "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1)---(1) Cantitruncated 600-cell",         "1,3"},
          {"            (1)-5-(1)---(1)---(1) Omnitruncated 120-cell/600-cell", "1,3"},
          {"    4d uniform anomolous"},
          {"        Grand Antiprism", "1"}, // XXX not supported yet
          {"    3d regular x 1d  hyperprisms"},
          {"        {3,3}x{} Tetrahedral prism",                "1,2,3(5.0),5(9.0),7(13.0)"},
          {"        {4,3}x{} Hypercube",                        "1,2,3,4,5,6,7,8,9,3(2.1),3(10.0)"},
          {"        {5,3}x{} Dodecahedral prism",               "1,2,3(2.5),3,4,5,6,7"},
          {"        {}x{5,3} Dodecahedral prism (alt)",         "1,2,3(2.5),3,4,5,6,7"},
          {"    3d wythoff x 1d  hyperprisms"},
          {"        Tetrahedron based"},
          {"            (1)---(0)---(0)x{} Tetrahedral prism",                  "1,3"},
          {"            (1)---(1)---(0)x{} Truncated-tetrahedron prism",        "1,3"},
          {"            (0)---(1)---(0)x{} Octahedral prism",                   "1"}, // vertex figure not simplex
          {"            (0)---(1)---(1)x{} Truncated-tetrahedron(dual) prism",  "1,3"},
          {"            (0)---(0)---(1)x{} Tetrahedral(dual) prism",            "1,3"},
          {"            (1)---(0)---(1)x{} Cantellated-tetrahedral prism",       "1"}, // vertex figure not simplex
          {"            (1)---(1)---(1)x{} Omnitruncated-tetrahedron prism",    "1,3"}, 
          {"        Cube/Octahedron based"},
          {"            (1)-4-(0)---(0)x{} Cube prism (hypercube)",             "1,3"},
          {"            (1)-4-(1)---(0)x{} Truncated-cube prism",               "1,3"},
          {"            (0)-4-(1)---(0)x{} Cuboctahedral prism",                "1"}, // vertex figure not simplex
          {"            (0)-4-(1)---(1)x{} Truncated-octahedron prism",         "1,3"},
          {"            (0)-4-(0)---(1)x{} Octahedral prism",                   "1"}, // vertex figure not simplex
          {"            (1)-4-(0)---(1)x{} Cantellated-cuboctahedral prism",    "1"}, // vertex figure not simplex
          {"            (1)-4-(1)---(1)x{} Omnitruncated-cube prism",           "1,3"},
          {"        Dodecahedron/Icosahedron based"},
          {"            (1)-5-(0)---(0)x{} Dodecahedral prism",                 "1,3"},
          {"            (1)-5-(1)---(0)x{} Truncated-dodecahedron prism",       "1,3"},
          {"            (0)-5-(1)---(0)x{} Icosadodecahedral prism",            "1"}, // vertex figure not simplex
          {"            (0)-5-(1)---(1)x{} Truncated-icosahedron (soccer ball) prism",        "1,3"},
          {"            (0)-5-(0)---(1)x{} Icosahedral prism",                  "1"}, // vertex figure not simplex
          {"            (1)-5-(0)---(1)x{} Cantellated-icosadodecahedral prism","1"}, // vertex figure not simplex
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
          {"        {3}x{3}",                                   "1,2,3(4.0),4,5,6,7"},
          {"        {3}x{5}",                                   "1,2,3(2.5),3(4.0),4,5,6,7"},
          {"        {5}x{5}",                                   "1,2,3(2.5),3,4,5,6,7"}, // XXX 2 is ugly, has slivers
          {"        {5}x{10}",                                  "1,3(2.5),3"}, // XXX 2 is ugly, has slivers
          {"        {10}x{5}",                                  "1,3(2.5),3"}, // XXX 2 is ugly, has slivers
          {"        {10}x{10}",                                 "1,3(2.5),3"}, // XXX 2 is ugly, has slivers
          {"5d puzzles"},
          {"    {3,3,3,3} Simplex",        "1,2,3,4,5"},
          {"    {4,3,3,3} Hypercube",      "1,2,3,4,5"},
          {"    {3,3,3,4} Cross",          "1"}, // vertex figure not simplex
          {"6d puzzles"},
          {"    {3,3,3,3,3} Simplex",      "1,2,3"},
          {"    {4,3,3,3,3} Hypercube",    "1,2,3"},
          {"    {3,3,3,3,4} Cross",        "1"}, // vertex figure not simplex
          //{"-"}, // separator
          //{"Invent my own!",}, // XXX currently done by the older code down below-- need to port and get rid of that
        }; // menuScheme

        java.util.Stack menuStack = new java.util.Stack();
        menuStack.push(puzzlemenu);
        for (int i = 0; i < menuScheme.length; ++i)
        {
            Assert(menuScheme[i].length <= 2);
            String item0 = menuScheme[i][0];
            String item1 = (menuScheme[i].length==1 ? null : menuScheme[i][1]);

            String item0Trimmed = item0.trim();
            int nLeadingSpaces = item0.length() - item0Trimmed.length();
            Assert(nLeadingSpaces % 4 == 0);
            int depth = nLeadingSpaces/4 + 1; // our whole scheme is at depth 1 already
            item0 = item0.substring(nLeadingSpaces);

            //System.out.println("item0 = "+item0);
            //System.out.println("    depth = "+depth);
            //System.out.println("    menuStack.size() = "+menuStack.size());
            while (depth < menuStack.size())
            {
                //System.out.println("    popping");
                menuStack.pop();
            }
            Assert(depth == menuStack.size());
            if (item0.equals("-"))
            {
                ((Menu)menuStack.peek()).add(new MenuItem("-")); // separator
            }
            else
            {
                Menu submenu = new Menu(item0+"    "); // padding so the > doesn't clobber the end of the longest names!? lame
                ((Menu)menuStack.peek()).add(submenu);
                //System.out.println("    pushing");
                menuStack.push(submenu);
                if (item1 != null)
                {
                    final String finalName = item0; // including the schlafli symbol
                    final String finalSchlafli = (item0.equalsIgnoreCase("Grand Antiprism") ? item0 : item0.split(" ")[0]);
                    String lengthStrings[] = item1.split(",");
                    for (int j = 0; j < lengthStrings.length; ++j)
                    {
                        final String finalLengthString = lengthStrings[j];
                        submenu.add(new MenuItem(finalLengthString)).addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae)
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
                                        String reply = JOptionPane.showInputDialog(prompt, initialInput);
                                        if (reply == null)
                                        {
                                            initPuzzleCallback.call(); // XXX really just want a repaint I think
                                            return; // cancelled
                                        }
                                        String schlafliAndLength[] = reply.trim().split("\\s+");
                                        if (schlafliAndLength.length != 2)
                                        {
                                            prompt = "Your invention sucks!\nYou must specify the schlafli product symbol (with no spaces),\nfollowed by a space, followed by the puzzle length. Try again!";
                                            initialInput = reply;
                                            continue;
                                        }
                                        schlafli = schlafliAndLength[0];
                                        lengthString = schlafliAndLength[1];
                                        name = "My own invention!  "+schlafli;
                                        break; // got it
                                    }
                                }
                                int intLength = 0;
                                double doubleLength = 0.;
                                {
                                    lengthString = lengthString.trim();

                                    try {
                                        //System.out.println("lengthString = "+lengthString);
                                        if (lengthString.length() >= 4
                                         && lengthString.charAt(1) == '(' // XXX assumes intLength < 9
                                         && lengthString.endsWith(")"))
                                        {
                                            String intPart = lengthString.substring(0,1);
                                            String doublePart = lengthString.substring(2, lengthString.length()-1);
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
                                        System.err.println("Your invention sucks! \""+lengthString+"\" is not a number!");
                                        initPuzzleCallback.call(); // XXX really just want a repaint I think
                                        return;
                                    }
                                    //System.out.println("intLength = "+intLength);
                                    //System.out.println("doubleLength = "+doubleLength);
                                }

                                GenericPuzzleDescription newPuzzle = null;
                                try
                                {
                                    newPuzzle = new PolytopePuzzleDescription(schlafli, intLength, doubleLength, progressWriter);
                                }
                                catch (Throwable t)
                                {
                                    //t.printStacktrace();
                                    String explanation = t.toString();
                                    // yes, this is lame... AND the user
                                    // can't even cut and paste it to mail it to me
                                    if (explanation.equals("java.lang.Error: Assertion failed"))
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
                                        "However, we are only accepting 4-dimensional inventions at this time.",
                                        "Invention Rejection Form Letter",
                                        JOptionPane.ERROR_MESSAGE);
                                    // XXX Lame, should try to get back in the loop and prompt again instead
                                    return;
                                }
                                genericPuzzleDescription = newPuzzle;
                                genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());

                                undoq.setSize(0);
                                undoPartSize = 0;

                                // PropertyManager.userprefs.setProperty("genericSchlafli", schlafli); // XXX not yet
                                // PropertyManager.userprefs.setProperty("genericLength", ""+len); // XXX not yet
                                initPuzzleCallback.call(); // really just want a repaint I think
                                String statuslabel = name + "  length="+lengthString;
                                statusLabel.setText(statuslabel); // XXX BUG - hey, it's not set right on program startup!

                                untwistedFrame = new GenericPipelineUtils.Frame();
                                twistingFrame = new GenericPipelineUtils.Frame();
                                    { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something
                            }
                        });
                    }
                }
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
                    puzzlemenu.add(new MenuItem("-")); // separator
                    continue;
                }

                final String schlafli = table[i][0];
                String lengthStrings[] = table[i][1].split(",");
                final String name = (schlafli==null ? table[i][2] :
                                     schlafli + "  " + table[i][2]);

                // Puzzles with triangles kind of suck so far,
                // so we might want to leave them out of the menu...
                boolean allowPuzzlesWithTriangles = true;
                //boolean allowPuzzlesWithTriangles = false;
                if (!allowPuzzlesWithTriangles)
                {
                    if (schlafli != null && schlafli.indexOf("{3") != -1)
                        continue;
                }

                Menu submenu;
                if (schlafli != null)
                {
                    submenu = new Menu(name+"    "); // XXX padding so the > doesn't clobber the end of the longest names!? lame
                    puzzlemenu.add(submenu);
                }
                else
                    submenu = puzzlemenu;
                final String finalSchlafli = schlafli;
                final String finalName = name;
                for (int j = 0; j < lengthStrings.length; ++j)
                {
                    final String lengthString = lengthStrings[j];
                    final String finalLengthString = lengthString;
                    submenu.add(new MenuItem(schlafli==null ? name : ""+lengthString)).addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae)
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
                                    String reply = JOptionPane.showInputDialog(prompt, initialInput);
                                    if (reply == null)
                                    {
                                        initPuzzleCallback.call(); // XXX really just want a repaint I think
                                        return; // cancelled
                                    }
                                    String schlafliAndLength[] = reply.trim().split("\\s+");
                                    if (schlafliAndLength.length != 2)
                                    {
                                        prompt = "Your invention sucks!\nYou must specify the schlafli product symbol (with no spaces),\nfollowed by a space, followed by the puzzle length. Try again!";
                                        initialInput = reply;
                                        continue;
                                    }
                                    schlafli = schlafliAndLength[0];
                                    lengthString = schlafliAndLength[1];
                                    name = "My own invention!  "+schlafli;
                                    break; // got it
                                }
                            }
                            int intLength = 0;
                            double doubleLength = 0.;
                            {
                                lengthString = lengthString.trim();

                                try {
                                    System.out.println("lengthString = "+lengthString);
                                    if (lengthString.length() >= 4
                                     && lengthString.charAt(1) == '(' // XXX assumes intLength < 9
                                     && lengthString.endsWith(")"))
                                    {
                                        String intPart = lengthString.substring(0,1);
                                        String doublePart = lengthString.substring(2, lengthString.length()-1);
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
                                    System.err.println("Your invention sucks! \""+lengthString+"\" is not a number!");
                                    initPuzzleCallback.call(); // XXX really just want a repaint I think
                                    return;
                                }
                                //System.out.println("intLength = "+intLength);
                                //System.out.println("doubleLength = "+doubleLength);
                            }

                            GenericPuzzleDescription newPuzzle = null;
                            try
                            {
                                newPuzzle = new PolytopePuzzleDescription(schlafli, intLength, doubleLength, progressWriter);
                            }
                            catch (Throwable t)
                            {
                                //t.printStacktrace();
                                String explanation = t.toString();
                                // yes, this is lame... AND the user
                                // can't even cut and paste it to mail it to me
                                if (explanation.equals("java.lang.Error: Assertion failed"))
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
                            if (nDims != 4)
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
                                    "However, we are only accepting 4-dimensional inventions at this time.",
                                    "Invention Rejection Form Letter",
                                    JOptionPane.ERROR_MESSAGE);
                                // XXX Lame, should try to get back in the loop and prompt again instead
                                return;
                            }
                            genericPuzzleDescription = newPuzzle;
                            genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());

                            undoq.setSize(0);
                            undoPartSize = 0;

                            // PropertyManager.userprefs.setProperty("genericSchlafli", schlafli); // XXX not yet
                            // PropertyManager.userprefs.setProperty("genericLength", ""+len); // XXX not yet
                            initPuzzleCallback.call(); // really just want a repaint I think
                            String statuslabel = name + "  length="+lengthString;
                            statusLabel.setText(statuslabel); // XXX BUG - hey, it's not set right on program startup!

                            untwistedFrame = new GenericPipelineUtils.Frame();
                            twistingFrame = new GenericPipelineUtils.Frame();
                                { twistingFrame = untwistedFrame; } // XXX HACK for now, avoid any issue about clicking in the wrong one or something
                        }
                    });
                }
            }
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue.addMoreItemsToPuzzleMenu");
    } // addMoreItemsToPuzzleMenu


    // Add a key listener for debugging.
    // All of the key sequences it listens to are ctrl-alt-something
    // so it should be difficult for the user to stumble on these
    // by accident.
    // Currently I call this automatically from inside mouseMovedAction
    // since I don't have handy access to the view before that...
    // So MC4DSwing doesn't really need to worry about calling this.
    public void addAnotherKeyListenerToView(final Canvas view)
    {
        view.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke)
            {
                char c = ke.getKeyChar();
                //System.out.println("generic key listener got key '"+c+"'("+(int)c+")");
                if (c == 't'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-t
                {
                    System.out.println("useTopsort "+useTopsort+" -> "+!useTopsort+"");
                    useTopsort = !useTopsort;
                    view.repaint();
                }
                if (c == 'j'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-j
                {
                    jitterRadius++;
                    if (jitterRadius == 10)
                        jitterRadius = 0;
                    System.out.println("jitterRadius -> "+jitterRadius+"");
                    view.repaint();
                }
                if (c == 'l'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-l
                {
                    System.out.println("drawLabels "+drawLabels+" -> "+!drawLabels+"");
                    drawLabels = !drawLabels;
                    view.repaint();
                }
                if (c == 'p'-'a'+1
                 && ke.isAltDown()) // ctrl-alt-p
                {
                    System.out.println("showPartialOrder "+showPartialOrder+" -> "+!showPartialOrder+"");
                    showPartialOrder = !showPartialOrder;
                    view.repaint();
                }
                if (c == ' ' && ke.isControlDown()
                 && ke.isAltDown()) // ctrl-alt-space
                {
                    System.out.println("frozenForDebugging "+frozenForDebugging+" -> "+!frozenForDebugging+"");
                    frozenForDebugging = !frozenForDebugging;
                    frozenPartialOrderForDebugging = null;
                    view.repaint();
                }
            }
        });
    } // addAnotherKeyListenerToView



    public void undoAction(Canvas view, JLabel statusLabel, float twistFactor)
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

            GenericGlue.HistoryNode node = (GenericGlue.HistoryNode)glue.undoq.get(--glue.undoPartSize);

            //
            // Initiate the undo twist (opposite dir from original)
            //
            int order = glue.genericPuzzleDescription.getGripSymmetryOrders()[node.iGrip];
            double totalRotationAngle = 2*Math.PI/order;
            glue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * MagicCube.NFRAMES_90 * twistFactor); // XXX unscientific rounding
            if (glue.nTwist == 0) glue.nTwist = 1;
            glue.iTwist = 0;
            glue.iTwistGrip = node.iGrip;
            glue.twistDir = -node.dir;
            glue.twistSliceMask = node.slicemask;

            view.repaint();
        }
        else
            statusLabel.setText("Nothing to undo.");
    } // undoAction

    public void redoAction(Canvas view, JLabel statusLabel, float twistFactor)
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

            GenericGlue.HistoryNode node = (GenericGlue.HistoryNode)glue.undoq.get(glue.undoPartSize++);

            //
            // Initiate the redo twist (same dir as original)
            //
            int order = glue.genericPuzzleDescription.getGripSymmetryOrders()[node.iGrip];
            double totalRotationAngle = 2*Math.PI/order;
            glue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * MagicCube.NFRAMES_90 * twistFactor); // XXX unscientific rounding
            if (glue.nTwist == 0) glue.nTwist = 1;
            glue.iTwist = 0;
            glue.iTwistGrip = node.iGrip;
            glue.twistDir = node.dir;
            glue.twistSliceMask = node.slicemask;

            view.repaint();
        }
        else
            statusLabel.setText("Nothing to redo.");
    } // redoAction

    public void cheatAction(Canvas view, JLabel statusLabel)
    {
        GenericGlue glue = this;
        glue.cheating = true; // each repaint will trigger another til done
        view.repaint();
        statusLabel.setText("");
    } // cheatAction

    public void scrambleAction(Canvas view, JLabel statusLabel, int scramblechenfrengensen)
    {
        GenericGlue glue = this;
        int nDims = glue.genericPuzzleDescription.nDims();
        java.util.Random rand = new java.util.Random();
        int previous_face = -1;
        for(int s = 0; s < scramblechenfrengensen; s++) {
            // select a random grip that is unrelated to the last one (if any)
            int iGrip, iFace, order;
            do {
                iGrip = rand.nextInt(glue.genericPuzzleDescription.nGrips());
                iFace = glue.genericPuzzleDescription.getGrip2Face()[iGrip];
                order = glue.genericPuzzleDescription.getGripSymmetryOrders()[iGrip];
            }
            while (
                order < 2 || // don't use trivial ones
                (nDims==3 && order==2) || // don't use the cute flips, in 3d
                (nDims==2 && order==4) || // don't use the cute twirls, in 2d
                iFace == previous_face || // mixing it up
                (previous_face!=-1 && glue.genericPuzzleDescription.getFace2OppositeFace()[previous_face] == iFace));
            previous_face = iFace;
            int slicemask = 1<<rand.nextInt(2); // XXX there's no getLength()! oh I think it's because I didn't think that was a generic enough concept to put in GenericPuzzleDescription, but I might have to rethink that.  for now, we just pick the first or second slice... this is fine for up to 4x, and even 5x (sort of)
            int dir = rand.nextBoolean() ? -1 : 1;

            glue.genericPuzzleDescription.applyTwistToState(
                    glue.genericPuzzleState,
                    iGrip,
                    dir,
                    slicemask);

            glue.undoq.setSize(glue.undoPartSize); // clear redo part
            glue.undoq.addElement(new GenericGlue.HistoryNode(
                                            iGrip,
                                            dir,
                                            slicemask));
            glue.undoPartSize++;

        }
        view.repaint();
        boolean fully = scramblechenfrengensen == MagicCube.FULL_SCRAMBLE;
        // scrambleState = fully ? SCRAMBLE_FULL : SCRAMBLE_PARTIAL; XXX do we need to do this here?
        statusLabel.setText(fully ? "Fully Scrambled" : scramblechenfrengensen + " Random Twist" + (scramblechenfrengensen==1?"":"s"));
    } // scrambleAction





    public void mouseMovedAction(MouseEvent e,
                                 Canvas view)
    {
        GenericGlue genericGlue = this;
        int pickedSticker = GenericPipelineUtils.pickSticker(
                                    e.getX(), e.getY(),
                                    genericGlue.untwistedFrame,
                                    genericGlue.genericPuzzleDescription);
        //System.out.println("    hover sticker "+genericGlue.iStickerUnderMouse+" -> "+pickedSticker+"");
        if (pickedSticker != genericGlue.iStickerUnderMouse)
            view.repaint(); // highlight changed (or turned on or off)
        genericGlue.iStickerUnderMouse = pickedSticker;


        // Kind of hacky way to add a back door key listener for debugging...
        if (view != mostRecentViewIAddedListenerTo)
        {
            addAnotherKeyListenerToView(view);

            mostRecentViewIAddedListenerTo = view;
        }
    } // mouseMovedAction

    private Canvas mostRecentViewIAddedListenerTo = null;


    public void mouseClickedAction(MouseEvent e,
                                   float viewMat4d[/*4*/][/*4*/],
                                   float twistFactor,
                                   int slicemask,

                                   Canvas view)
    {
        GenericGlue genericGlue = this;
        boolean isRotate = e.isControlDown() || isMiddleMouseButton(e);
        if (false) // make this true to debug the pick
        {
            int hit[] = GenericPipelineUtils.pick(e.getX(), e.getY(),
                                                  genericGlue.untwistedFrame,
                                                  genericGlue.genericPuzzleDescription);
            if (hit != null)
            {
                int iSticker = hit[0];
                int iFace = genericGlue.genericPuzzleDescription.getSticker2Face()[iSticker];
                int iCubie = genericGlue.genericPuzzleDescription.getSticker2Cubie()[iSticker];
                System.err.println("    Hit sticker "+iSticker+"(polygon "+hit[1]+")");
                System.err.println("        face "+iFace);
                System.err.println("        cubie "+iCubie);
            }
        }

        if (isRotate)
        {
            float nicePoint[] = GenericPipelineUtils.pickNicePointToRotateToCenter(
                             e.getX(), e.getY(),
                             genericGlue.untwistedFrame,
                             genericGlue.genericPuzzleDescription);

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

                if (genericGlue.genericPuzzleDescription.nDims() < 4)
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
                             genericGlue.untwistedFrame,
                             genericGlue.genericPuzzleDescription);
                        Assert(polyAndStickerAndFaceCenter != null); // hit once, should hit again
                        float polyCenter[] = polyAndStickerAndFaceCenter[0];

                        // Only interested in the w component
                        // (and the z component if the puzzle is 2d).
                        // So zero out the first nDims dimensions...
                        polyCenter = com.donhatchsw.util.VecMath.copyvec(polyCenter);
                        com.donhatchsw.util.VecMath.zerovec(genericGlue.genericPuzzleDescription.nDims(),
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

                genericGlue.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * MagicCube.NFRAMES_90 * twistFactor); // XXX unscientific rounding
                if (genericGlue.nRotation == 0) genericGlue.nRotation = 1;
                // XXX ARGH! we'd like the speed to vary as the user changes the slider,
                // XXX but the above essentially locks in the speed for this rotation
                genericGlue.iRotation = 0; // we are iRotation frames into nRotation
                view.repaint();

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
                            genericGlue.untwistedFrame,
                            genericGlue.genericPuzzleDescription);
            if (iGrip != -1)
            {
                int order = genericGlue.genericPuzzleDescription.getGripSymmetryOrders()[iGrip];

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

                int dir = (isLeftMouseButton(e) || isMiddleMouseButton(e)) ? MagicCube.CCW : MagicCube.CW;

                //if(e.isShiftDown()) // experimental control to allow double twists but also requires speed control.
                //    dir *= 2;

                double totalRotationAngle = 2*Math.PI/order;
                genericGlue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * MagicCube.NFRAMES_90 * twistFactor); // XXX unscientific rounding
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

                view.repaint();
            }
            else
                System.out.println("missed");
        }
    } // mouseClickedAction


    // XXX Could maybe separate this out
    // XXX into a compute part and a paint part,
    // XXX since they seem to be doing logically separated
    // XXX things with almost entirely disjoint subsets of the parameters
    public void computeAndPaintFrame(
        // These are used by the compute part only
        float faceShrink,
        float stickerShrink,
        float viewMat4d[/*4*/][/*4*/], // contents of this get incremented if animating!
        float eyeW,
        float viewMat3d[/*3*/][/*3*/],
        float eyeZ, // MagicCube.EYEZ
        float scale, // whatever the fuck that means
        float pixels2polySF, // whatever the fuck that means
        int xOff,
        int yOff,

        float towardsSunVec[], // used by compute part if showShadows is true
        boolean showShadows, // used by both compute and paint parts

        // All the rest are for paint the paint part only
        Color ground,
        float faceRGB[][],
        boolean highlightByCubie,
        Color outlineColor,
        Graphics g,
        float twistFactor,
        boolean restrictRoll,
        Canvas view)
    {
        GenericGlue genericGlue = this;

        // steal PolygonManager's stuff-- this should be an interface but that's not allowed here apparently
        abstract class InterpFunc { public abstract float func(float f); }
        InterpFunc sine_interp = new InterpFunc() {
            public float func(float x) { return (float)(Math.sin((x - .5) * Math.PI) + 1) / 2; }
        };
        InterpFunc linear_interp = new InterpFunc() {
            public float func(float x) { return x; }
        };
        InterpFunc interp = sine_interp;
        //InterpFunc interp = linear_interp;

        if (genericGlue.iRotation < genericGlue.nRotation)
        {
            //
            // 4d rotation in progress
            //
            float incFrac = interp.func((genericGlue.iRotation+1)/(float)genericGlue.nRotation)
                           - interp.func(genericGlue.iRotation/(float)genericGlue.nRotation);
            float incmat[][] = com.donhatchsw.util.VecMath.makeRowRotMatThatSlerps(genericGlue.rotationFrom, genericGlue.rotationTo, incFrac);
            float newViewMat4d[][] = com.donhatchsw.util.VecMath.mxm(viewMat4d, incmat);
            com.donhatchsw.util.VecMath.gramschmidt(newViewMat4d, newViewMat4d);
            com.donhatchsw.util.VecMath.copymat(viewMat4d, newViewMat4d);
            //System.out.println("    "+genericGlue.iRotation+"/"+genericGlue.nRotation+" -> "+(genericGlue.iRotation+1)+"/"+genericGlue.nRotation+"");
            if (!frozenForDebugging)
            {
                genericGlue.iRotation++;
                view.repaint(); // make sure we keep drawing while there's more to do

                if (genericGlue.iRotation == genericGlue.nRotation
                 && restrictRoll)
                {
                    // If we are finishing a rotate-to-center
                    // and we are in restricted roll mode,
                    // what we were using as a twirl axis is probably not
                    // very good any more.  Choose another.
                    if (genericGlue.rotationTo[3] < -.99999) // i.e. if it was a rot to center
                    {
                        initiateZeroRoll(viewMat4d,
                                         viewMat3d,
                                         twistFactor,
                                         view);
                    }
                }
            }
        }

        int iGripOfTwist = -1;
        int twistDir = 0;
        int slicemask = 0;
        float fracIntoTwist = 0.f;
        GenericPipelineUtils.Frame glueFrameToDrawInto = genericGlue.untwistedFrame;

        if (genericGlue.iTwist < genericGlue.nTwist)
        {
            //
            // Twist in progress (and maybe a 4d rot too at the same time)
            //
            glueFrameToDrawInto = genericGlue.twistingFrame;

            iGripOfTwist = genericGlue.iTwistGrip;
            twistDir = genericGlue.twistDir;
            slicemask = genericGlue.twistSliceMask;

            fracIntoTwist = (float)interp.func((genericGlue.iTwist+1)/(float)genericGlue.nTwist);
            //System.out.println("    "+genericGlue.iTwist+"/"+genericGlue.nTwist+" -> "+(genericGlue.iTwist+1)+"/"+genericGlue.nTwist+"");

            if (!frozenForDebugging)
                view.repaint(); // make sure we keep drawing while there's more to do
        }

        // old params... but I don't think it was doing it right
        //float[] groundNormal = showShadows ? new float[] {0,1,.1f} : null;
        //float groundOffset = -1.f;

        // XXX why is this a bit diff from old?  well I don't think it was being done right for one thing
        float[] groundNormal = showShadows ? new float[] {0,1,.05f} : null;
        float groundOffset = -1.f;

        // XXX I don't seem to be quite the same as the original... unless I correct it here
        float scaleFudge4d = 1.f;
        float scaleFudge3d = 1.f;
        float scaleFudge2d = 4.7f;

        // XXX probably doing this more than necessary... when it's a rest frame that hasn't changed
        GenericPipelineUtils.computeFrame(
            glueFrameToDrawInto,

            genericGlue.genericPuzzleDescription,
            faceShrink,
            stickerShrink,

            iGripOfTwist,
              twistDir,
              slicemask,
              fracIntoTwist,

            com.donhatchsw.util.VecMath.mxs(viewMat4d, scaleFudge4d),
            eyeW,
            com.donhatchsw.util.VecMath.mxm(
                com.donhatchsw.util.VecMath.makeRowRotMat(3, 2, 1, (float)Math.PI/2), // XXX FUDGE that makes it nicer for the pentagonal prismprism... what do we need, a preferred viewing orientation for each puzzle as part of the model description?
                com.donhatchsw.util.VecMath.mxs(viewMat3d, scaleFudge3d)),
            eyeZ,
            new float[][]{{scaleFudge2d*scale/pixels2polySF, 0},
                          {0, -scaleFudge2d*scale/pixels2polySF},
                          {(float)xOff, (float)yOff}},

            com.donhatchsw.util.VecMath.normalize(towardsSunVec),
            groundNormal,
            groundOffset,
            
            useTopsort,
            showPartialOrder);

        if (frozenForDebugging)
        {
            if (frozenPartialOrderForDebugging != null)
                glueFrameToDrawInto.partialOrder = frozenPartialOrderForDebugging;
            else
                frozenPartialOrderForDebugging = glueFrameToDrawInto.partialOrder;
        }

        // THE COMPUTE PART ENDS HERE
        // THE PAINT PART STARTS HERE (maybe should be a separate function)

        GenericPipelineUtils.paintFrame(
                glueFrameToDrawInto,
                genericGlue.genericPuzzleDescription,
                genericGlue.genericPuzzleState,
                showShadows,
                ground,
                faceRGB,
                genericGlue.iStickerUnderMouse,
                highlightByCubie,
                outlineColor,
                g,

                jitterRadius,
                drawLabels,
                showPartialOrder);

        if (frozenForDebugging)
        {
            glueFrameToDrawInto.partialOrder = null; // so we don't get stuck
        }


        if (genericGlue.iTwist < genericGlue.nTwist)
        {
            if (!frozenForDebugging)
                genericGlue.iTwist++;
            if (genericGlue.iTwist == genericGlue.nTwist)
            {
                // End of twist animation-- apply the twist to the state.
                // The move has already been recorded in the undo queue
                // (if it's a forward move and not an undo).
                genericGlue.genericPuzzleDescription.applyTwistToState(
                            genericGlue.genericPuzzleState,
                            genericGlue.iTwistGrip,
                            genericGlue.twistDir,
                            genericGlue.twistSliceMask);
                // XXX need to update the hovered-over sticker! I think.
                // XXX it would suffice to just call the mouseMoved callback... but maybe we don't want to do that after every frame in the cheat
            }
        }
        if (genericGlue.iTwist == genericGlue.nTwist
         && genericGlue.cheating)
        {
            // End of a twist that's part of a cheat
            if (genericGlue.undoPartSize == 0)
            {
                // End of the cheat.
                genericGlue.cheating = false;
            }
            else
            {
                // Initiate the next undo twist in the cheat.
                // XXX duplicate code from the undo menu item-- make a function out of this

                GenericGlue.HistoryNode node = (GenericGlue.HistoryNode)genericGlue.undoq.get(--genericGlue.undoPartSize);

                //
                // Initiate the undo twist (opposite dir from original)
                //
                int order = genericGlue.genericPuzzleDescription.getGripSymmetryOrders()[node.iGrip];
                double totalRotationAngle = 2*Math.PI/order;
                genericGlue.nTwist = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * MagicCube.NFRAMES_90 * twistFactor); // XXX unscientific rounding
                if (genericGlue.nTwist == 0) genericGlue.nTwist = 1;
                genericGlue.iTwist = 0;
                genericGlue.iTwistGrip = node.iGrip;
                genericGlue.twistDir = -node.dir;
                genericGlue.twistSliceMask = node.slicemask;

                view.repaint();
            }
        }
    } // computeAndPaintFrame


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
            Assert(bestIFace != -1);
            com.donhatchsw.util.VecMath.normalize(bestClosestPointOnPositiveYZSector,
                                                  bestClosestPointOnPositiveYZSector);
            com.donhatchsw.util.VecMath.copyvec(3, returnPointOnYZArc,
                                                   bestClosestPointOnPositiveYZSector);
            return bestIFace;
        } // findFaceCenterClosestToYZArc

        public void initiateZeroRoll(float viewMat4d[][],
                                     float viewMat3d[][],
                                     float twistFactor,
                                     Canvas view)
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
                Vec_h._SETMAT3(paddedViewMat3d, viewMat3d);
                paddedViewMat3d[3][3] = 1.f;
                viewMat3d = paddedViewMat3d;
            }

            float faceCenters[][] = this.genericPuzzleDescription.getFaceCentersAtRest();
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
            this.nRotation = (int)(Math.sqrt(totalRotationAngle/(Math.PI/2)) * MagicCube.NFRAMES_90 * twistFactor); // XXX unscientific rounding
            if (this.nRotation == 0) this.nRotation = 1;
            this.iRotation = 0;

            // Remember the zero roll pole
            // for subsequent calls to zeroOutRollOnSpinDelta
            this.zeroRollPoleAfterRot3d = pointOnYZArc;

            //System.out.println("this.rotationFrom = "+com.donhatchsw.util.VecMath.toString(this.rotationFrom));
            //System.out.println("this.rotationTo = "+com.donhatchsw.util.VecMath.toString(this.rotationTo));

            view.repaint();
        } // initiateZeroRoll

        public SQuat zeroOutRollAndMaybeTiltOnSpinDelta(SQuat spindelta,        
                                                        boolean zeroOutTilt)
        {
            // Use the zeroRollPoll from the most recent call
            // to initiateZeroRoll.
            Assert(spindelta.getHomoRotation() >= 0.f);

            SQuat.Vector3 temp = new SQuat.Vector3();
            spindelta.getHomoAxis(temp);
            float homoAxis[] = temp.asArray();

            float sinHalfTiltDeltaAngle = -homoAxis[0]; // XXX why minus? dammit
            float sinHalfTwirlDeltaAngle = -homoAxis[1]; // XXX why minus? dammit
            float tiltDeltaAngle = 2*(float)Math.asin(sinHalfTiltDeltaAngle);
            float twirlDeltaAngle = 2*(float)Math.asin(sinHalfTwirlDeltaAngle);
            if (zeroOutTilt)
                tiltDeltaAngle = 0.f; // before clamping-- we do let the clamp do a tilt if it wants

            Assert(zeroRollPoleAfterRot3d != null); // initiateZeroRoll must have been called previously
            zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.copyvec(3, zeroRollPoleAfterRot3d); // XXX sigh... because vxm and other stuff freaks if I don't
            // Clamp tilt to [0..pi/2]...
            float currentTilt = (float)Math.atan2(zeroRollPoleAfterRot3d[2],
                                                  zeroRollPoleAfterRot3d[1]);
            //System.out.println("tiltDeltaAngle before = "+tiltDeltaAngle*180/Math.PI);
            if (tiltDeltaAngle > (float)Math.PI/2 - currentTilt)
                tiltDeltaAngle = (float)Math.PI/2 - currentTilt;
            if (tiltDeltaAngle < 0 - currentTilt)
                tiltDeltaAngle = 0 - currentTilt;
            //System.out.println("tiltDeltaAngle after = "+tiltDeltaAngle*180/Math.PI);

            SQuat twirlDelta = new SQuat(zeroRollPoleAfterRot3d,-twirlDeltaAngle); // XXX why minus? dammit
            SQuat tiltDelta = new SQuat(1,0,0,-tiltDeltaAngle,false); // XXX why minus? dammit

            //System.out.println("zeroRollPoleAfterRot3d = "+com.donhatchsw.util.VecMath.toString(zeroRollPoleAfterRot3d));

            //return tiltDelta;
            //return twirlDelta;
            //return twirlDelta.mult(tiltDelta);
            SQuat adjustedSpinDelta = twirlDelta.mult(tiltDelta);
            // need to apply it to the pole...

            zeroRollPoleAfterRot3d = com.donhatchsw.util.VecMath.vxm(zeroRollPoleAfterRot3d,
                                                                     new SQuat.Matrix3(adjustedSpinDelta).asArray());
            return adjustedSpinDelta;
        } // zeroOutRollOnSpinDelta

        // when dragging, we allow tilt changes
        public SQuat zeroOutRollOnSpinDelta(SQuat spindelta)
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(spindelta, false);
        }
        // when autospinning, we don't allow tilt changes,
        // or it would just drift to the min or max tilt, which looks dumb
        public SQuat zeroOutRollAndTiltOnSpinDelta(SQuat spindelta)
        {
            return zeroOutRollAndMaybeTiltOnSpinDelta(spindelta, true);
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


} // class GenericGlue
