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
//          the test for whether an animation (rotate or twist)
//          is in progress, when isActive() is true
//
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
     public double rotationFrom[]; // where rotation is rotating from, in 4space
     public double rotationTo[]; // where rotation is rotating to, in 4space

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
    java.util.Vector undoq = new java.util.Vector(); // of HistoryNode
    int undoPartSize = 0; // undoq has undo part followed by redo part


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
                initialLength,
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
        return iRotation < nRotation
            || iTwist < nTwist;
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

        String table[][] = {
            {"{3,3,3}",  "1,1.9,2,3,4,5,6,7", "Simplex"},
            {"{3}x{4}",  "1,2,3,4,5,6,7",     "Triangular Prism Prism"},
            {"{4,3,3}",  "1,2,3,4,5,6,7,8,9", "Hypercube"},
            {"{5}x{4}",  "1,2,2.5,3,4,5,6,7", "Pentagonal Prism Prism"},
            {"{4}x{5}",  "1,2,2.5,3,4,5,6,7", "Pentagonal Prism Prism (alt)"},
            {"{6}x{4}",  "1,2,2.5,3,4,5,6,7", "Hexagonal Prism Prism"},
            {"{7}x{4}",  "1,2,2.5,3,4,5,6,7", "True HEPAgonal Prism Prism"},
            {"{8}x{4}",  "1,2,2.5,3,4,5,6,7", "Octagonal Prism Prism"},
            {"{8}x{4}",  "1,2,2.5,3,4,5,6,7", "Nonagonal Prism Prism"},
            {"{10}x{4}", "1,2,2.5,3,4,5,6,7", "Decagonal Prism Prism"},
            {"{100}x{4}","1,3",               "Onehundredagonal Prism Prism"},
            {"{3}x{3}",  "1,2,3,4,5,6,7",     ""},
            {"{3}x{5}",  "1,2,2.5,3,4,5,6,7", ""},
            {"{5}x{5}",  "1,2,2.5,3,4,5,6,7", ""}, // XXX 2 is ugly, has slivers
            {"{5}x{10}",  "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{10}x{5}",  "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{10}x{10}", "1,2.5,3",          ""}, // XXX 2 is ugly, has slivers
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,2.5,2.5,3,4,5,6,7", "Dodecahedral Prism"},
            {"{}x{5,3}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism (alt)"},
            {"{5,3,3}",  "1,2,2.5,3",         "Hypermegaminx (BIG!)"},
            {null,       "",                  "Invent my own!"},
        };
        puzzlemenu.add(new MenuItem("-")); // separator
        for (int i = 0; i < table.length; ++i)
        {

            final String schlafli = table[i][0];
            String lengthStrings[] = table[i][1].split(",");
            final String name = (schlafli==null ? table[i][2] :
                                 schlafli + "  " + table[i][2]);

            // Puzzles with triangles kind of suck so far,
            // so we might want to leave them out of the menu...
            //boolean allowPuzzlesWithTriangles = true;
            boolean allowPuzzlesWithTriangles = false;
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
                        double len;
                        try { len = Double.parseDouble(lengthString); }
                        catch (java.lang.NumberFormatException e)
                        {
                            System.err.println("Your invention sucks! \""+lengthString+"\" is not a number!");
                            initPuzzleCallback.call(); // XXX really just want a repaint I think
                            return;
                        }

                        GenericPuzzleDescription newPuzzle = null;
                        try
                        {
                            newPuzzle = new PolytopePuzzleDescription(schlafli, len, progressWriter);
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
                                "Something went very wrong when trying to build your invention \""+schlafli+"  "+lengthString+"\":\n"+explanation,
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

                        PropertyManager.userprefs.setProperty("genericSchlafli", schlafli);
                        PropertyManager.userprefs.setProperty("genericLength", ""+len);
                        initPuzzleCallback.call(); // really just want a repaint I think
                        String statuslabel = name + "  length="+lengthString;
                        statusLabel.setText(statuslabel); // XXX BUG - hey, it's not set right on program startup!
                    }
                });
                // XXX add a "pick my own"!
            }
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue.addMoreItemsToPuzzleMenu");
    } // addMoreItemsToPuzzleMenu



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
            glue.nTwist = (int)(totalRotationAngle/(Math.PI/2) * MagicCube.NFRAMES_180 * twistFactor); // XXX unscientific rounding-- and it's too fast for small angles!  It's more noticeable here than for twists because very small angles are possible here.  Really we'd like to bound the max acceleration.
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
            glue.nTwist = (int)(totalRotationAngle/(Math.PI/2) * MagicCube.NFRAMES_180 * twistFactor);
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



} // class GenericGlue
