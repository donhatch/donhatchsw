//
// This file is mostly throwaway--
// It is an attempt to glue the new GenericPuzzleDescription thing
// onto MC4DView with as minimal impact to the existing code as possible,
// prior to Melinda getting a look at it
// and figuring out where it should really go.
//

// XXX blindly using same imports as MC4DSwing
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Stack;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

import com.donhatchsw.util.*; // XXX get rid

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
        if (true) // whether to do this on start
        {
            genericPuzzleDescription = new PolytopePuzzleDescription(initialSchlafli, initialLength,
                                               new java.io.PrintWriter(
                                               new java.io.BufferedWriter(
                                               new java.io.OutputStreamWriter(
                                               System.err))));
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
            {"{3}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{5}x{5}",  "1,2,3,4,5,6,7",     ""}, // XXX 2 is ugly, has slivers
            {"{10}x{10}",  "1,3",             ""}, // XXX 2 is ugly, has slivers
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism"},
            {"{}x{5,3}", "1,2,2.5,3,4,5,6,7", "Dodecahedral Prism (alt)"},
            {"{5,3,3}",  "1,2,3",             "Hypermegaminx (BIG!)"},
            {null,       "0", "Invent my own!"},
        };
        puzzlemenu.add(new MenuItem("-")); // separator
        for (int i = 0; i < table.length; ++i)
        {
            final String schlafli = table[i][0];
            String lengthStrings[] = table[i][1].split(",");
            final String name = (schlafli==null ? table[i][2] :
                                 schlafli + "  " + table[i][2]);

            Menu submenu;
            if (schlafli != null)
            {
                submenu = new Menu(name+"    "); // XXX padding so the > doesn't clobber the end of the longest names!? lame
                puzzlemenu.add(submenu);
            }
            else
                submenu = puzzlemenu;
            for (int j = 0; j < lengthStrings.length; ++j)
            {
                final String lengthString = lengthStrings[j];
                final double len = Double.parseDouble(lengthString);
                final String statuslabel = name + "  length="+lengthString;
                submenu.add(new MenuItem(len==0 ? name : ""+lengthString)).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                        if (schlafli != null)
                        {
                            genericPuzzleDescription = new PolytopePuzzleDescription(schlafli, len, progressWriter);
                            genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
                            undoq.setSize(0);
                            undoPartSize = 0;
                            PropertyManager.userprefs.setProperty("genericSchlafli", schlafli);
                            PropertyManager.userprefs.setProperty("genericLength", ""+len);
                            initPuzzleCallback.call(); // apparently necessary in order for repaint to happen
                            statusLabel.setText(statuslabel); // XXX BUG - hey, it's not set right on program startup!
                        }
                        else
                        {
                            System.out.println("Sorry, you can't invent your own yet!");
                        }
                    }
                });
                // XXX add a "pick my own"!
            }
        }
        if (verboseLevel >= 1) System.out.println("out GenericGlue.addMoreItemsToPuzzleMenu");
    } // addMoreItemsToPuzzleMenu

} // class GenericGlue
