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
    //
    // State.  Intentionally minimal!
    //
    public GenericPuzzleDescription genericPuzzleDescription = null;
    public int genericPuzzleState[] = null;
    public static int verboseLevel = 1;


    /**
     * Geometry data for an animation frame.
     * NOTE: the pre-projected W and Z components
     * are retained; this can be used for mapping 2d pick points
     * back up to 4d if desired.
     */
    public class Frame
    {
        // NOTE: the pre-projected Z and W components
        // are retained; this can be used for mapping 2d pick points
        // back up to 4d if desired.
        public int nVerts;      // may be less than verts.length if some culled
        public float verts[][/*4*/]; // x,y,z,w, not just x,y! see above
        public int nPolys;      // may be less than polys.length is some culled
        public int polys[][];   // sorted back to front
        public int polyIds[]; // map from poly index to unculled poly index
        public float brightnesses[]; // map from poly index to brightness

        public int polysBuffer[][]; // The memory used for polys (before
                                    // culling and sorting).  We store this
                                    // so that a Frame can be reused without
                                    // having to do any memory allocations.
    } // class Frame

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
            {"{3,3,3}",  "1,1.9,2,3,4,5,6,7",     "Simplex"},
            {"{3}x{4}",  "1,2,3,4,5,6,7",     "Triangular Prism Prism"},
            {"{4,3,3}",  "1,2,3,4,5,6,7,8,9", "Hypercube"},
            {"{5}x{4}",  "1,2,2.5,3,4,5,6,7",     "Pentagonal Prism Prism"},
            {"{4}x{5}",  "1,2,2.5,3,4,5,6,7",     "Pentagonal Prism Prism (alt)"},
            {"{6}x{4}",  "1,2,2.5,3,4,5,6,7",     "Hexagonal Prism Prism"},
            {"{3}x{3}",  "1,2,3,4,5,6,7",     ""},
            {"{3}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{5}x{5}",  "1,2,3,4,5,6,7",     ""},
            {"{3,3}x{}", "1,2,3,4,5,6,7",     "Tetrahedral Prism"},
            {"{5,3}x{}", "1,2,2.5,3,4,5,6,7",     "Dodecahedral Prism"},
            {"{}x{5,3}", "1,2,2.5,3,4,5,6,7",     "Dodecahedral Prism (alt)"},
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

    // Computes a frame of animation.
    // Attempts to avoid doing any new memory allocations
    // when called repeatedly on a given puzzleDescription.
    public static void computeFrame(GenericPuzzleDescription puzzleDescription,
                                    float faceShrink,
                                    float stickerShrink,

                                    float rot4d[/*4*/][/*4*/],
                                    float eyeW,
                                    float rot3d[/*3*/][/*3*/],
                                    float eyeZ,
                                    float scale2d,
                                    float xOff, // XXX why is this int in MC4DView?
                                    float yOff, // XXX why is this int in MC4DView?
                                    Frame frame) // output
    {
        if (verboseLevel >= 2) System.out.println("in Glue.computeFrame");

        int nDims = puzzleDescription.nDims();
        Assert(nDims == 4);
        int nVerts = puzzleDescription.nVerts();
        int nPolys = puzzleDescription.nPolygons();

        //
        // Allocate any parts of frame that are null
        // or different from last time...
        //
        {
            if (frame.verts == null
             || frame.verts.length != nVerts
             || nVerts>0 && frame.verts[0].length != nDims)
                frame.verts = new float[nVerts][nDims];
            if (!arrayLengthsMatch(frame.polysBuffer, puzzleDescription.getPolygons()))
                frame.polysBuffer = com.donhatchsw.util.Arrays.copy(puzzleDescription.getPolygons(), 2); // don't care about the contents, just need the sizes to be right
            if (frame.polys == null
             || frame.polys.length != nPolys)
                frame.polys = new int[nPolys][];
            if (frame.polyIds == null
             || frame.polyIds.length != nPolys)
                frame.polyIds = new int[nPolys];
            if (frame.brightnesses == null
             || frame.brightnesses.length != nPolys)
                frame.polyIds = new float[nPolys];
        }

        //
        // There should be no memory allocations from here down.
        // XXX but there are... but they can be fixed.
        //

        //
        // Point polys into polysBuffer...
        //
        for (int i = 0; i < nPolys; ++i)
            frame.polys[i] = frame.polysBuffer[i];

        //
        // Get the 4d geometry from the puzzle description
        //
        {
            puzzleDescription.getStickerVertsAtRest(frame.verts,
                                                    faceShrink,
                                                    stickerShrink);
            Arrays.copy(frame.polys, genericPuzzleDescription.getPolygons());
        }
        Assert(false);

        if (verboseLevel >= 2) System.out.println("out Glue.computeFrame");
    } // computeFrame

    public static void paintFrame(float faceShrink,
                                    float stickerShrink,

                                    float rot4d[/*4*/][/*4*/],
                                    float eyeW,
                                    float rot3d[/*3*/][/*3*/],
                                    float eyeZ,
                                    float scale2d,
                                    float xOff, // XXX why is this int in MC4DView?
                                    float yOff, // XXX why is this int in MC4DView?
                           boolean showShadows, // XXX or isShadows?
                           Color ground,
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

            verts4d = (float[][])com.donhatchsw.util.Arrays.copy(verts4d, 2); // so we own it and can mess it
            stickerInds = (int[][][])com.donhatchsw.util.Arrays.copy(stickerInds, 3); // so we own it and can mess it
        }
        if (verboseLevel >= 3) System.out.println("    initial stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));
        if (verboseLevel >= 3) System.out.println("    initial verts4d = "+com.donhatchsw.util.Arrays.toStringCompact(verts4d));

        //
        // Rotate/scale in 4d
        //
        {
            // Make it so circumradius is 4.
            // XXX I have no basis for this except that empirically it makes
            // XXX the 3^4 hypercube match what the puzzle usually does

            // XXX this is constant per puzzle,
            // XXX so we should really back it into the stored arrays
            // XXX so that we don't have to do it here
            float scale4d = 4.f/genericPuzzleDescription.circumRadius();
            float rotScale4d[][] = VecMath.mxs(rot4d, scale4d);
            verts4d = VecMath.mxm(verts4d, rotScale4d);
        }
        if (verboseLevel >= 3) System.out.println("    after 4d rot/scale/trans: verts4d = "+com.donhatchsw.util.Arrays.toStringCompact(verts4d));

        //
        // Clip to the 4d eye's front clipping plane
        //
        {
            // XXX DO ME
        }
        if (verboseLevel >= 3) System.out.println("    after 4d clip: verts4d = "+com.donhatchsw.util.Arrays.toStringCompact(verts4d));

        //
        // Project down to 3d
        //
        float verts3d[][/*3*/];
        {
            verts3d = new float[verts4d.length][3];
            for (int i = 0; i < verts3d.length; ++i)
            {
                float mult = 1.f/(eyeW - verts4d[i][3]);
                for (int j = 0; j < 3; ++j)
                    verts3d[i][j] = verts4d[i][j] * mult;
            }
        }
        if (verboseLevel >= 3) System.out.println("    after 4d->3d project: verts3d = "+com.donhatchsw.util.Arrays.toStringCompact(verts3d));

        verts4d = null;

        //
        // Front-cell cull
        //
        {
            float mat[][] = new float[3][3];
            for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
            {
                int thisStickerInds[][] = stickerInds[iSticker];
                float v0[] = verts3d[thisStickerInds[0][0]];
                float v1[] = verts3d[thisStickerInds[0][1]];
                float v2[] = verts3d[thisStickerInds[0][2]];
                float v3[] = verts3d[thisStickerInds[1][0]];
                VecMath.vmv(mat[0], v1, v0);
                VecMath.vmv(mat[1], v2, v0);
                VecMath.vmv(mat[2], v3, v0);
                float volume = VecMath.vxvxv3(mat[0], mat[1], mat[2]);
                if (volume > 0.f)
                    stickerInds[iSticker] = null;
            }
        }
        if (verboseLevel >= 3) System.out.println("    after front-cell cull: verts3d = "+com.donhatchsw.util.Arrays.toStringCompact(verts3d));

        //
        // Rotate/scale in 3d
        //
        {
            verts3d = VecMath.mxm(verts3d, rot3d);
        }
        if (verboseLevel >= 3) System.out.println("    after 3d rot/scale/trans: verts3d = "+com.donhatchsw.util.Arrays.toStringCompact(verts3d));

        //
        // If doing shadows,
        // project the shadows onto the ground plane
        //
        {
            // XXX DO ME
        }

        //
        // Clip to the 3d eye's front clipping plane
        //
        {
            // XXX DO ME
        }
        if (verboseLevel >= 3) System.out.println("    after 3d clip: verts3d = "+com.donhatchsw.util.Arrays.toStringCompact(verts3d));

        //
        // Sort quads back-to-front
        //
        int stickerIndIndsBackToFront[] = new int[stickerInds.length];
        {
            // We're not set up to flatten into
            // a flat list of quads yet, so for now,
            // just sort by sticker center.
            float stickerCenters[][] = new float[stickerInds.length][3]; // zeros
            for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
            {
                if (stickerInds[iSticker] == null)
                    continue; // cell was culled
                // XXX counts vertices lots of times.. and sheesh, only [2] is relevant!!
                VecMath.averageIndexed(stickerCenters[iSticker], stickerInds[iSticker], verts3d);
            }
            VecMath.identityperm(stickerIndIndsBackToFront);
            final float finalStickerCenters[][] = stickerCenters;
            SortStuff.sort(stickerIndIndsBackToFront, new SortStuff.IntComparator() {
                public int compare(int i, int j)
                {
                    return finalStickerCenters[i][2] < finalStickerCenters[j][2] ? -1 :
                           finalStickerCenters[i][2] > finalStickerCenters[j][2] ? 1 : 0;
                }
            });
        }
        if (verboseLevel >= 3) System.out.println("    after z-sort: stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));


        //
        // Project down to 2d
        //
        float verts2d[][/*2*/];
        {
            verts2d = new float[verts3d.length][2];
            for (int i = 0; i < verts2d.length; ++i)
                for (int j = 0; j < 2; ++j)
                    verts2d[i][j] = verts3d[i][j]; // XXX FIX ME
        }
        if (verboseLevel >= 3) System.out.println("    after 3d->2d project: verts2d = "+com.donhatchsw.util.Arrays.toStringCompact(verts2d));

        verts3d = null;

        //
        // Back-face cull
        //
        {
            float mat[][] = new float[2][2];
            for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
            {
                if (stickerInds[iSticker] == null)
                    continue; // cell was culled
                for (int iPoly = 0; iPoly < stickerInds[iSticker].length; ++iPoly)
                {
                    int thisPolyInds[] = stickerInds[iSticker][iPoly];
                    float v0[] = verts2d[thisPolyInds[0]];
                    float v1[] = verts2d[thisPolyInds[1]];
                    float v2[] = verts2d[thisPolyInds[2]];
                    VecMath.vmv(mat[0], v1, v0);
                    VecMath.vmv(mat[1], v2, v0);
                    float area = VecMath.vxv2(mat[0], mat[1]);
                    if (area < 0.f)
                        stickerInds[iSticker][iPoly] = null;
                }
            }
        }
        if (verboseLevel >= 3) System.out.println("    after back-face cull: verts2d = "+com.donhatchsw.util.Arrays.toStringCompact(verts2d));

        //
        // Rotate/scale in 2d
        //
        {
            //scale2d *= .1; // XXX FUDGE
            float rotScale2dMat[][] = {
                {scale2d, 0},
                {0,       -scale2d},
                {xOff,    yOff},
            };
            if (verboseLevel >= 3) System.out.println("rotScale2dMat = "+com.donhatchsw.util.VecMath.toString(rotScale2dMat));
            for (int i = 0; i < verts2d.length; ++i)
                verts2d[i] = VecMath.vxm(verts2d[i], rotScale2dMat);
        }
        if (verboseLevel >= 3) System.out.println("    after 2d rot/scale/trans: verts2d = "+com.donhatchsw.util.Arrays.toStringCompact(verts2d));

        //
        // Fudge stuff we don't know yet
        float brightnesses[][];
        {
            brightnesses = new float[stickerInds.length][];
            for (int i = 0; i < brightnesses.length; ++i)
            {
                if (stickerInds[i] == null)
                    continue;
                brightnesses[i] = new float[stickerInds[i].length];
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
            int
                xs[] = new int[0],
                ys[] = new int[0];
            Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();

            for (int _iSticker = 0; _iSticker < stickerInds.length; ++_iSticker)
            {
                int iSticker = stickerIndIndsBackToFront[_iSticker];
                int thisStickerInds[][] = stickerInds[iSticker];
                if (thisStickerInds == null)
                    continue; // front-cell culled

                int colorOfSticker = genericPuzzleState[iSticker];
                float faceRGBThisSticker[] = faceRGB[colorOfSticker % faceRGB.length]; // XXX need to make more colors

                for (int iPolyThisSticker = 0; iPolyThisSticker < stickerInds[iSticker].length; ++iPolyThisSticker)
                {
                    int thisPolyInds[] = thisStickerInds[iPolyThisSticker];
                    if (thisPolyInds == null)
                        continue; // back-face culled
                    if (thisPolyInds.length > xs.length)
                    {
                        xs = new int[thisPolyInds.length];
                        ys = new int[thisPolyInds.length];
                    }
                    for (int i = 0; i < thisPolyInds.length; ++i)
                    {
                        int thisVertInd = thisPolyInds[i];
                        xs[i] = (int)verts2d[thisVertInd][0];
                        ys[i] = (int)verts2d[thisVertInd][1];
                        //System.out.println(xs[i] + ", " + ys[i]);
                    }
                    float brightness = brightnesses[iSticker][iPolyThisSticker];
                    Color stickercolor = new Color(
                        brightness*faceRGBThisSticker[0],
                        brightness*faceRGBThisSticker[1],
                        brightness*faceRGBThisSticker[2]);
                    /*
                    boolean highlight = stickerUnderMouse != null && (highlightByCubie ? partOfCubie(sid) : stickerUnderMouse.id_within_cube == sid);
                    if(highlight)
                        stickercolor = stickercolor.brighter().brighter();
                    */
                    boolean isShadows = false; // for now
                    g.setColor(isShadows ? shadowcolor : stickercolor);
                    g.fillPolygon(xs, ys, thisPolyInds.length);
                    if(!isShadows && outlineColor != null) {
                        g.setColor(outlineColor);
                        // uncomment the following line for an alternate outlining idea -MG
                        // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                        g.drawPolygon(xs, ys, thisPolyInds.length);
                    }
                }
            }
        }



        if (false) // body of MC4DSwing.paintFrame, for reference
        {
            // Just declare the variables so it will compile
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

    //
    // Utilities...
    //
        private static void arrayLengthsMatch(int a[][], int b[][])
        {
            if (a==null && b==null)
                return true;
            if (a==null || b==null)
                return false;
            if (a.length != b.length)
                return false;
            for (int i = 0; i < a.length; ++i)
                if ((a==null) != (b==null)
                 || a!=null && a.length != b.length)
                    return false;
            return true;
        }

} // class Glue
