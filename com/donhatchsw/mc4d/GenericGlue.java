//
// This file is mostly throwaway--
// It is an attempt to glue the new GenericPuzzleDescription thing
// onto MC4DView with as minimal impact as possible,
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

public class Glue
{
    public GenericPuzzleDescription genericPuzzleDescription = null;
    public int genericPuzzleState[] = null;
    public int verboseLevel = 2;

    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    public interface Callback { public void call(); }



    public Glue(String initialSchlafli, int initialLength)
    {
        super();
        if (verboseLevel >= 1) System.out.println("in Glue ctor");
        if (true) // whether to do this on start
        {
            genericPuzzleDescription = new PolytopePuzzleDescription(initialSchlafli, initialLength,
                                               new java.io.PrintWriter(
                                               new java.io.BufferedWriter(
                                               new java.io.OutputStreamWriter(
                                               System.err))));
            genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
        }
        if (verboseLevel >= 1) System.out.println("out Glue ctor");
    }

    public boolean isActive()
    {
        return genericPuzzleDescription != null;
    }

    // Call this from MC4DSwing ctor right after all
    // the other menu items are added
    public void addMoreItemsToPuzzleMenu(Menu puzzlemenu,
                                         final JLabel statusLabel,
                                         final Callback initPuzzleCallback)
    {
        if (verboseLevel >= 1) System.out.println("in Glue.addMoreItemsToPuzzleMenu");

        // Used for reported progress during puzzle creation,
        // which can take a long time.
        // Currently just goes to System.err.
        final java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                                   new java.io.BufferedWriter(
                                                   new java.io.OutputStreamWriter(
                                                   System.err)));

        // Selecting any of the previously existing menu items
        // should have the side effect of setting
        // genericPuzzleDescription to null.
        for (int i = 0; i < puzzlemenu.getItemCount(); ++i)
        {
            puzzlemenu.getItem(i).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    if (verboseLevel >= 1) System.out.println("Glue: deactivating");
                    genericPuzzleDescription = null;
                    genericPuzzleState = null;
                }
            });
        }

        String table[][] = {
            {"{3,3,3}",  "1,2,3,4,5,6,7",     "Simplex"},
            {"{3}x{4}",  "1,2,3,4,5,6,7",     "Triangular Prism Prism"},
            {"{4,3,3}",  "1,2,3,4,5,6,7,8,9", "Hypercube"},
            {"{5}x{4}",  "1,2,3,4,5,6,7",     "Pentagonal Prism Prism"},
            {"{6}x{4}",  "1,2,3,4,5,6,7",     "Hexagonal Prism Prism"},
            {"{3}x{3}",  "1,2,3,4,5,6,7",     ""},
            {"{3}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{5}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,3,4,5,6,7",     "Dodecahedral Prism"},
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
                final int len = Integer.parseInt(lengthStrings[j]);
                final String statuslabel = name + "  length="+len;
                submenu.add(new MenuItem(len==0 ? name : ""+len)).addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                        if (schlafli != null)
                        {
                            genericPuzzleDescription = new PolytopePuzzleDescription(schlafli, len, progressWriter);
                            genericPuzzleState = com.donhatchsw.util.VecMath.copyvec(genericPuzzleDescription.getSticker2Face());
                            PropertyManager.userprefs.setProperty("genericSchlafli", schlafli);
                            PropertyManager.userprefs.setProperty("genericLength", ""+len);
                            initPuzzleCallback.call(); // apparently necessary in order for repaint to happen
                            statusLabel.setText(statuslabel); // XXX BUG - hey, it's not set right on program startup!
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
        if (verboseLevel >= 1) System.out.println("out Glue.addMoreItemsToPuzzleMenu");
    } // addMoreItemsToPuzzleMenu

    // whether given sticker is on the cubie containing the mouse
    // XXX figure out this better based on genericPuzzleDescription
    private boolean partOfCubie(int stickerId)
    {
        return false;
    }

    public void paintFrame(float faceShrink,
                           float stickerShrink,

                           boolean showShadows, // XXX or isShadows?
                           Color ground,
                           int xOff,
                           int yOff,
                           float scale,
                           float faceRGB[][],
                           boolean highlightByCubie,
                           Color outlineColor,
                           Graphics g)
    {
        if (verboseLevel >= 2) System.out.println("in Glue.paintFrame");

        //
        // Get the 4d geometry from the puzzle description
        //
        // XXX what if not at rest?
        float verts4d[][/*4*/];
        int stickerInds[/*nStickers*/]
                       [/*nPolygonsThisSticker*/]
                       [/*nVertsThisPolygon*/];
        {
            verts4d = genericPuzzleDescription.getStickerVertsAtRest(faceShrink,
                                                                     stickerShrink);
            Assert(verts4d.length > 0);
            Assert(verts4d[0].length == 4); // for now, maybe can handle 3d and 2d later
            stickerInds = genericPuzzleDescription.getStickerInds();
        }
        System.out.println("    stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));
        System.out.println("    verts4d = "+com.donhatchsw.util.Arrays.toStringCompact(verts4d));

        //
        // Rotate/scale in 4d
        //
        float verts4dRotScaled[][/*4*/];
        {
            verts4dRotScaled = verts4d;
        }
        System.out.println("    verts4dRotScaled = "+com.donhatchsw.util.Arrays.toStringCompact(verts4dRotScaled));
        verts4d = null;

        //
        // Clip to the 4d eye's front clipping plane
        //
        float verts4dClipped[][/*4*/];
        {
            verts4dClipped = verts4dRotScaled; // XXX FIX ME
        }
        System.out.println("    verts4dClipped = "+com.donhatchsw.util.Arrays.toStringCompact(verts4dClipped));
        verts4dRotScaled = null;

        //
        // Project down to 3d
        //
        float verts3d[][/*3*/];
        {
            // XXX FIX ME
            verts3d = new float[verts4dClipped.length][3];
            for (int i = 0; i < verts3d.length; ++i)
                for (int j = 0; j < 3; ++j)
                    verts3d[i][j] = verts4dClipped[i][j];
        }
        System.out.println("    verts3d = "+com.donhatchsw.util.Arrays.toStringCompact(verts3d));
        verts4dClipped = null;

        //
        // Front-cell cull
        //
        float verts3dCulled[][/*3*/];
        {
            verts3dCulled = verts3d; // XXX FIX ME
        }
        System.out.println("    verts3dCulled = "+com.donhatchsw.util.Arrays.toStringCompact(verts3dCulled));
        verts3d = null;

        //
        // Rotate/scale in 3d
        //
        float verts3dRotScaled[][/*3*/];
        {
            verts3dRotScaled = verts3dCulled;
        }
        System.out.println("    verts3dRotScaled = "+com.donhatchsw.util.Arrays.toStringCompact(verts3dRotScaled));
        verts3d = null;

        //
        // Clip to the 3d eye's front clipping plane
        //
        float verts3dClipped[][/*3*/];
        {
            verts3dClipped = verts3dCulled; // XXX FIX ME
        }
        System.out.println("    verts3dClipped = "+com.donhatchsw.util.Arrays.toStringCompact(verts3dClipped));
        verts3dCulled = null;

        //
        // Project down to 2d
        //
        float verts2d[][/*2*/];
        {
            // XXX FIX ME
            verts2d = new float[verts3dClipped.length][2];
            for (int i = 0; i < verts2d.length; ++i)
                for (int j = 0; j < 2; ++j)
                    verts2d[i][j] = verts3dClipped[i][j];
        }
        System.out.println("    verts2d = "+com.donhatchsw.util.Arrays.toStringCompact(verts2d));
        verts3dClipped = null;

        //
        // Back-face cull?
        //
        float verts2dCulled[][/*2*/];
        {
            verts2dCulled = verts2d; // XXX FIX ME
        }
        System.out.println("    verts2dCulled = "+com.donhatchsw.util.Arrays.toStringCompact(verts2dCulled));
        verts2d = null;

        //
        // Rotate/scale in 2d
        //
        float verts2dRotScaled[][/*2*/];
        {
            verts2dRotScaled = new float[verts2dCulled.length][2];
            float rotScale2dMat[][] = {
                {scale, 0},
                {0,     scale},
                {xOff,  yOff},
            };
            for (int i = 0; i < verts2dCulled.length; ++i)
                VecMath.vxm(verts2dRotScaled[i], verts2dCulled[i], rotScale2dMat);
        }
        System.out.println("    verts2dRotScaled = "+com.donhatchsw.util.Arrays.toStringCompact(verts2dRotScaled));
        verts2dCulled = null;

        //
        // Sort quads back-to-front
        //
        int stickerIndsSorted[/*nStickersRemaining*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];
        {
            stickerIndsSorted = stickerInds; // XXX FIX ME
        }
        System.out.println("    stickerIndsSorted = "+com.donhatchsw.util.Arrays.toStringCompact(stickerIndsSorted));

        //
        // Fudge stuff we don't know yet
        float brightnesses[][];
        {
            brightnesses = new float[stickerIndsSorted.length][];
            for (int i = 0; i < brightnesses.length; ++i)
            {
                brightnesses[i] = new float[stickerIndsSorted[i].length];
                for (int j = 0; j < brightnesses[i].length; ++j)
                {
                    brightnesses[i][j] = 1.f;
                }
            }
        }

        //
        // Render them!
        //
        {
            int xs[] = new int[0],
                ys[] = new int[0];

            for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
            {
                int thisStickerInds[][] = stickerInds[iSticker];
                for (int iPolyThisSticker = 0; iPolyThisSticker < stickerInds[iSticker].length; ++iPolyThisSticker)
                {
                    int thisPolyInds[] = thisStickerInds[iPolyThisSticker];
                    if (thisPolyInds.length > xs.length)
                    {
                        xs = new int[thisPolyInds.length];
                        ys = new int[thisPolyInds.length];
                    }
                    for (int i = 0; i < thisPolyInds.length; ++i)
                    {
                        int thisVertInd = thisPolyInds[i];
                        xs[i] = (int)verts2dRotScaled[thisVertInd][0];
                        ys[i] = (int)verts2dRotScaled[thisVertInd][1];
                        //System.out.println(xs[i] + ", " + ys[i]);
                    }
                    int stickerId = iSticker; // original sticker index
                    int colorOfSticker = genericPuzzleState[stickerId];
                    float b = brightnesses[iSticker][iPolyThisSticker];
                }
            }
        }



        if (false) // body of MC4DSwing.paintFrame, for reference
        {
            // Just declare them so it will compile
            MagicCube.Frame frame = null;
            MagicCube.Frame shadow_frame = null;
            PuzzleState state = null;
            MagicCube.Stickerspec stickerUnderMouse = null;
            boolean isShadows = false;
            float pixels2polySF = 1.f;

            int
                xs[] = new int[4],
                ys[] = new int[4];
            Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();
            for (int q = 0; q < frame.nquads; q++) {
                for (int i = 0; i < 4; i++) {
                    int qi = frame.quads[q][i];
                    xs[i] = (int)(xOff + frame.verts[qi][0]/pixels2polySF + .5);
                    ys[i] = (int)(yOff + frame.verts[qi][1]/pixels2polySF + .5);
                    //System.out.println(xs[i] + ", " + ys[i]);
                }
                int sid = frame.quadids[q]/6;
                int cs = state.idToColor(sid);
                //System.out.println(cs);
                float b = frame.brightnesses[q];
                Color stickercolor = new Color(
                    b*faceRGB[cs][0],
                    b*faceRGB[cs][1],
                    b*faceRGB[cs][2]);
                boolean highlight = stickerUnderMouse != null && (highlightByCubie ? partOfCubie(sid) : stickerUnderMouse.id_within_cube == sid);
                if(highlight)
                    stickercolor = stickercolor.brighter().brighter();
                g.setColor(isShadows ? shadowcolor : stickercolor);
                g.fillPolygon(xs, ys, 4);
                if(!isShadows && outlineColor != null) {
                    g.setColor(outlineColor);
                    // uncomment the following line for an alternate outlining idea -MG
                    // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                    g.drawPolygon(xs, ys, 4);
                }
            }
        }

        if (verboseLevel >= 2) System.out.println("out Glue.paintFrame");
    } // paintFrame

    // XXX THE NORMAL PAINTFRAME THAT WE'RE REPLACING, FOR REFERENCE
/*
    private void paintFrame(MagicCube.Frame frame, boolean isShadows, Graphics g) {
        int
            xs[] = new int[4],
            ys[] = new int[4];
        Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();
        for (int q = 0; q < frame.nquads; q++) {
            for (int i = 0; i < 4; i++) {
                int qi = frame.quads[q][i];
                xs[i] = (int)(xOff + frame.verts[qi][0]/pixels2polySF + .5);
                ys[i] = (int)(yOff + frame.verts[qi][1]/pixels2polySF + .5);
                //System.out.println(xs[i] + ", " + ys[i]);
            }
            int sid = frame.quadids[q]/6;
            int cs = state.idToColor(sid);
            //System.out.println(cs);
            float b = frame.brightnesses[q];
            Color stickercolor = new Color(
                b*faceRGB[cs][0],
                b*faceRGB[cs][1],
                b*faceRGB[cs][2]);
            boolean highlight = stickerUnderMouse != null && (highlightByCubie ? partOfCubie(sid) : stickerUnderMouse.id_within_cube == sid);
            if(highlight)
                stickercolor = stickercolor.brighter().brighter();
            g.setColor(isShadows ? shadowcolor : stickercolor);
            g.fillPolygon(xs, ys, 4);
            if(!isShadows && outlineColor != null) {
                g.setColor(outlineColor);
                // uncomment the following line for an alternate outlining idea -MG
                // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                g.drawPolygon(xs, ys, 4);
            }
        }
    } // THE NORMAL PAINFRAME THAT WE'RE REPLACING
*/


} // class Glue
