//
// Attempt to hack the new GenericPuzzleDescription thing
// into an MC4DView
// with minimal impact on the existing MC4DView code.
//
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Stack;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

public class Glue extends MC4DSwing
{
    private java.io.PrintWriter progressWriter;
    GenericPuzzleDescription genericPuzzleDescription;

    Glue()
    {
        super();
        System.out.println("in Glue ctor");
        progressWriter = new java.io.PrintWriter(new java.io.BufferedWriter(
                                                 new java.io.OutputStreamWriter(
                                                 System.err)));
        System.out.println("out Glue ctor");
    }

    // Call this from MC4DSwing ctor right after all
    // the other menu items are added
    protected void GlueMoreItemsToPuzzleMenu(Menu puzzlemenu, final JPanel viewcontainer, final JLabel statusLabel)
    {
        System.out.println("in GlueMoreItemsToPuzzleMenu");

        // Selecting any of the previously existing menu items
        // should set genericPuzzleDescription to null.
        for (int i = 0; i < puzzlemenu.getItemCount(); ++i)
        {
            puzzlemenu.getItem(i).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    System.out.println("setting genericPuzzleDescription to null");
                    genericPuzzleDescription = null;
                }
            });
        }

        // Lame!  Should actually be two different menus--
        // puzzle, and puzzle size!  Or, cascading!
        String table[][] = {
            {"{3,3,3}",  "1,2,3,4,5,6,7",     "Simplex"},
            {"{3}x{4}",  "1,2,3,4,5,6,7",     "Triangular Prism Prism"},
            {"{4,3,3}",  "1,2,3,4,5,6,7,8,9", "Hypercube"},
            {"{5}x{4}",  "1,2,3,4,5,6,7",     "Pentagonal Prism Prism"},
            {"{3}x{3}",  "1,2,3,4,5,6,7",     ""},
            {"{3}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{5}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,3,4,5,6,7",     "Dodecahedral Prism"},
            {"{5,3,3}",  "1,2,3",             "Hypermegaminx (BIG!)"},
            {null,       "0", "Invent my own!"},
        };
        puzzlemenu.add(new MenuItem("-"));
        //puzzlemenu.add(new MenuItem("-"));
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
                final int len = Integer.parseInt(lengthStrings[j]);
                final String statuslabel = name + "  length="+len;
                submenu.add(new MenuItem(len==0 ? name : ""+len)).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                        if (schlafli != null)
                        {
                            genericPuzzleDescription = new PolytopePuzzleDescription(schlafli, len, progressWriter);
                            PropertyManager.userprefs.setProperty("genericSchlafli", schlafli);
                            PropertyManager.userprefs.setProperty("genericLength", ""+len);
                            UnGlueInitPuzzle(null); // apparently necessary in order for repaint to happen
                            viewcontainer.validate(); // XXX what does this do?
                            statusLabel.setText(statuslabel); // XXX hey, it's not set right at the beginning!
                        }
                        else
                        {
                            System.out.println("Sorry, you can't invent your own yet!");
                        }
                        // XXX need to make the MC4DView disable its usual
                        // XXX listeners! maybe remove them and save them
                        // XXX so we can restore them later!  Otherwise
                        // XXX it keeps saying "missed!"
                    }
                });
                // XXX add a "pick my own"!
            }
        }
        System.out.println("out GlueMoreItemsToPuzzleMenu");
    } // GlueMoreItemsToPuzzleMenu


    // Call this instead of new MC4DView
    protected MC4DView GlueNewMC4DView(PuzzleState puzzle, PolygonManager polymgr, History hist)
    {
        System.out.println("in GlueNewMC4DView");
        MC4DView ret = new GlueMC4DView(puzzle, polymgr, hist);
        System.out.println("out GlueNewMC4DView");
        return ret;
    }


    public class GlueMC4DView extends MC4DView
    {
        public GlueMC4DView(PuzzleState puzzle, PolygonManager polymgr, History hist)
        {
            super(puzzle, polymgr, hist);
        }

        public void paint(Graphics g1)
        {
            //System.out.println("in GlueMC4DView.paint");
            if (genericPuzzleDescription == null)
            {
                super.paint(g1);
                return;
            }
            Graphics g = super.startPaint(g1);

            // paint the background
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
            if(ground != null) {
                g.setColor(ground);
                g.fillRect(0, getHeight()*6/9, getWidth(), getHeight());
            }
/*
            // paint the puzzle
            if(showShadows)
                paintFrame(shadow_frame, true, g);
            paintFrame(frame, false, g);
*/

            super.endPaint(); // blits the back buffer to the front
            //System.out.println("out GlueMC4DView.paint");
        }
    } // GlueMC4DView



    public static void main(String args[])
    {
        MC4DSwing.main(args, new Glue());
    } // main
} // class Glue
