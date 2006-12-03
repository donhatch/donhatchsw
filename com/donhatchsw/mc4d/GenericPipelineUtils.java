/**
* XXX does this class name suck?  well it sucks less than PolygonManager.
*
* Utilities for drawing and picking on a generic puzzle.
*
* This is a replacement for much of what PolygonManager did
* in the old implementation; however this class has NO STATE
* and is non-instantiatable, because state in something
* so generic and vague as a "polygon manager" is confusing and impossible
* to remember and DRIVES ME NUTS!
*
* However, there is a utility subclass called a Frame,
* which does hold state-- it is essentially a drawlist
* of 2d polygons, which is used by the three
* primary functions in this file:
*    computeFrame - computes a Frame from the puzzle description
*                    and viewing parameters
*    paintFrame - draws the Frame
*    pick       - picks what is at a given point in the Frame
*/

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

public class GenericPipelineUtils
{
    private GenericPipelineUtils() {} // non-instantiatable

    public static int verboseLevel = 0; // set to something else to debug

    /**
     * Geometry data for an animation frame.
     * NOTE: the pre-projected W and Z components
     * are retained; this can be used for mapping 2d pick points
     * back up to 4d if desired.
     */
    public static class Frame
    {
        // NOTE: the pre-projected Z and W components
        // are retained; this can be used for mapping 2d pick points
        // back up to 4d if desired.
        // verts[i] refers to the same vertex as vertex i in
        // the puzzle description (although unused indices may end up
        // with arbitrary values).
        // Each element of drawList is a pair i,j,
        // referring to the polygon stickerInds[i][j]
        // in the original puzzle description.

        public float verts[][/*4*/]; // x,y,z,w, not just x,y! see above

        public int drawListSize;
        public int drawList[][/*2*/];
        public float brightnesses[/*nStickers*/][/*nPolysThisSticker*/];

        // Memory used by drawList (before culling and sorting).
        // We keep this around so that a Frame can be reused
        // without having to do any memory allocations.
        public int drawListBuffer[/*nStickers*/][/*nPolysThisSticker*/][/*2*/];
    } // class Frame

    static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }

    public interface Callback { public void call(); }



    /**
    * Compute a frame of animation.
    * Attempts to avoid doing any new memory allocations
    * when called repeatedly on a given puzzleDescription.
    */
    public static void computeFrame(Frame frame, // return into here

                                    GenericPuzzleDescription puzzleDescription,

                                    float faceShrink,
                                    float stickerShrink,

                                    int iGripOfTwist,    // -1 if not twisting
                                    int twistDir,               
                                    int twistSliceMask,
                                    float fracIntoTwist,

                                    float rot4d[/*4*/][/*4 or 5*/],
                                    float eyeW,
                                    float rot3d[/*3*/][/*3 or 4*/],
                                    float eyeZ,
                                    float rot2d[/*2*/][/*2 or 3*/],
                                    
                                    float unitTowardsSunVec[/*3*/])
    {
        if (verboseLevel >= 2) System.out.println("in Glue.computeFrame");

        int nDims = puzzleDescription.nDims();
        Assert(nDims == 4);
        int nVerts = puzzleDescription.nVerts();
        int nStickers = puzzleDescription.nStickers();

        //
        // Allocate any parts of frame that are null
        // or different from last time...
        //
        int stickerInds[][][] = puzzleDescription.getStickerInds();
        {
            if (frame.verts == null
             || frame.verts.length != nVerts
             || nVerts>0 && frame.verts[0].length != nDims)
                frame.verts = new float[nVerts][nDims];
            int nPolys = 0;
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                nPolys += stickerInds[iSticker].length;
            if (!com.donhatchsw.util.Arrays.sizesMatch(frame.drawListBuffer, stickerInds, 2))
            {
                frame.drawListBuffer = new int[stickerInds.length][][];
                for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
                {
                    frame.drawListBuffer[iSticker] = new int[stickerInds[iSticker].length][];
                    for (int iPolyThisSticker = 0; iPolyThisSticker < stickerInds[iSticker].length; ++iPolyThisSticker)
                        frame.drawListBuffer[iSticker][iPolyThisSticker] = new int[] {iSticker, iPolyThisSticker};
                }
            }
            if (!com.donhatchsw.util.Arrays.sizesMatch(frame.brightnesses, stickerInds, 2))
            {
                frame.brightnesses = new float[stickerInds.length][];
                for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
                    frame.brightnesses[iSticker] = new float[stickerInds[iSticker].length];
            }
            if (frame.drawList == null
             || frame.drawList.length != nPolys)
                frame.drawList = new int[nPolys][/*2*/];
        }

        float verts[][] = frame.verts;
        int drawList[][] = frame.drawList;
        int drawListSize = 0; // we'll set frame.drawListSize to this at end

        //
        // There should be no memory allocations from here down.
        // XXX but there are... but they can be fixed.
        //

        //
        // Get the 4d verts from the puzzle description
        //
        if (iGripOfTwist == -1)
            puzzleDescription.computeStickerVertsAtRest(verts,
                                                        faceShrink,
                                                        stickerShrink);
        else
            puzzleDescription.computeStickerVertsPartiallyTwisted(
                                                        verts,
                                                        faceShrink,
                                                        stickerShrink,
                                                        iGripOfTwist,
                                                        twistDir,
                                                        twistSliceMask,
                                                        fracIntoTwist);
        //
        // Rotate/scale in 4d
        //
        {
            // Make it so circumradius is 4.
            // XXX I have no basis for this except that empirically it makes
            // XXX the 3^4 hypercube match what the puzzle usually does
            float scale4d = 4.f/puzzleDescription.circumRadius();
            float rotScale4d[][] = VecMath.mxs(rot4d, scale4d); // XXX MEMORY ALLOCATION
            float temp[] = new float[4]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                VecMath.vxm(temp, verts[iVert], rotScale4d);
                VecMath.copyvec(verts[iVert], temp);
            }
        }
        if (verboseLevel >= 3) System.out.println("    after 4d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Clip to the 4d eye's front clipping plane
        //
        {
            // XXX DO ME?
        }
        //if (verboseLevel >= 3) System.out.println("    after 4d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Project down to 3d
        //
        {
            for (int i = 0; i < verts.length; ++i)
            {
                float w = eyeW - verts[i][3];
                float invW = 1.f/w;
                for (int j = 0; j < 3; ++j)
                    verts[i][j] *= invW;
                verts[i][3] = w; // keep this for future reference
            }
        }
        if (verboseLevel >= 3) System.out.println("    after 4d->3d project: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Front-cell cull
        //
        {
            int nBackFacing = 0;
            float mat[][] = new float[3][3]; // XXX MEMORY ALLOCATION
            for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
            {
                int thisStickerInds[][] = stickerInds[iSticker];
                float v0[] = verts[thisStickerInds[0][0]];
                float v1[] = verts[thisStickerInds[0][1]];
                float v2[] = verts[thisStickerInds[0][2]];
                float v3[] = verts[thisStickerInds[1][0]];
                Vec_h._VMV3(mat[0], v1, v0); // 3 out of 4
                Vec_h._VMV3(mat[1], v2, v0); // 3 out of 4
                Vec_h._VMV3(mat[2], v3, v0); // 3 out of 4
                float volume = VecMath.vxvxv3(mat[0], mat[1], mat[2]);
                if (volume < 0.f) // only draw *back* cells; cull front ones
                {
                    // append references to this sticker's polys into drawList
                    for (int iPolyThisSticker = 0; iPolyThisSticker < thisStickerInds.length; ++iPolyThisSticker)
                        drawList[nBackFacing++] = frame.drawListBuffer[iSticker][iPolyThisSticker]; // = {iSticker,iPolyThisSticker}
                }
            }
            drawListSize = nBackFacing;
        }
        if (verboseLevel >= 3) System.out.println("    after front-cell cull: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Rotate/scale in 3d
        // XXX could try to only do this on vertices that passed the culls
        //
        {
            if (verboseLevel >= 3) System.out.println("rot3d = "+com.donhatchsw.util.VecMath.toString(rot3d));
            float tempIn[] = new float[3]; // XXX MEMORY ALLOCATION
            float tempOut[] = new float[3]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    tempIn[i] = verts[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot3d); // only first 3... however rot3d can be 3x3 or 4x3
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    verts[iVert][i] = tempOut[i];
            }
        }
        if (verboseLevel >= 3) System.out.println("    after 3d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Compute brightnesses.
        //
        {
            // XXX should only do this faces that are going
            // XXX to pass the cull... PolygonManager did this cleverly I think,
            // XXX computing the 2d verts first, culling, and then
            // XXX going back to the 3d verts of the polys that remained.
            // XXX on the other hand, since we are computing
            // XXX all the face normals here, we could use those
            // XXX to backface cull right away, and avoid projecting
            // XXX the vertices that got culled!
            float triangleNormal[] = new float[3]; // XXX MEMORY ALLOCATION
            float e1[] = new float[3]; // XXX MEMORY ALLOCATION
            float e2[] = new float[3]; // XXX MEMORY ALLOCATION
            for (int i = 0; i < drawListSize; ++i)
            {
                int i0i1[] = drawList[i];
                int poly[] = stickerInds[i0i1[0]][i0i1[1]];
                float v0[] = verts[poly[0]];
                float v1[] = verts[poly[1]];
                float v2[] = verts[poly[2]];
                Vec_h._VMV3(e1, v1, v0);
                Vec_h._VMV3(e2, v2, v0);
                Vec_h._VXV3(triangleNormal, e1, e2);
                VecMath.normalize(triangleNormal, triangleNormal);
                float brightness = VecMath.dot(triangleNormal, unitTowardsSunVec);
                if (brightness < 0)
                    brightness = 0;
                frame.brightnesses[i0i1[0]][i0i1[1]] = brightness;
                //System.out.println("brightness = "+brightness);
            }
        }


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
            // XXX DO ME?
        }
        //if (verboseLevel >= 3) System.out.println("    after 3d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));


        //
        // Project down to 2d
        // XXX could try to only do this on vertices that passed the culls
        //
        {
            for (int i = 0; i < verts.length; ++i)
            {
                float z = eyeZ - verts[i][2];
                float invZ = 1.f/z;
                for (int j = 0; j < 2; ++j)
                    verts[i][j] *= invZ;
                verts[i][2] = z; // keep this for future reference
            }
        }
        if (verboseLevel >= 3) System.out.println("    after 3d->2d project: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Back-face cull
        //
        {
            float mat[][] = new float[2][2]; // XXX ALLOCATION
            int nFrontFacing = 0;
            for (int i = 0; i < drawListSize; ++i)
            {
                int i0i1[] = drawList[i];
                int poly[] = stickerInds[i0i1[0]][i0i1[1]];
                float v0[] = verts[poly[0]];
                float v1[] = verts[poly[1]];
                float v2[] = verts[poly[2]];
                Vec_h._VMV2(mat[0], v1, v0); // 2 out of 4
                Vec_h._VMV2(mat[1], v2, v0); // 2 out of 4
                float area = VecMath.vxv2(mat[0], mat[1]);
                if (area > 0.f) // only retain *front*-facing polygons
                    drawList[nFrontFacing++] = i0i1;
            }
            drawListSize = nFrontFacing;
        }
        if (verboseLevel >= 3) System.out.println("    after back-face cull: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Rotate/scale in 2d
        // XXX could try to only do this on vertices that passed both culls
        //
        {
            if (verboseLevel >= 3) System.out.println("rot2d = "+com.donhatchsw.util.VecMath.toString(rot2d));
            float tempIn[] = new float[2]; // XXX MEMORY ALLOCATION
            float tempOut[] = new float[2]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                for (int i = 0; i < 2; ++i) // 2 out of 4
                    tempIn[i] = verts[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot2d); // only first 2... however rot2d can be 2x2 or 3x2
                for (int i = 0; i < 2; ++i) // 2 out of 4
                    verts[iVert][i] = tempOut[i];
            }
        }
        if (verboseLevel >= 3) System.out.println("    after 2d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));


        //
        // Sort drawlist polygons back-to-front,
        // using the z values that we retained from before the 3d->2d projection
        // (but there's less work to do now that we culled back faces).
        //
        {
            float polyCentersZ[/*nStickers*/][/*nPolysThisSticker*/] = new float[nStickers][]; // XXX ALLOCATION!
            for (int i = 0; i < nStickers; ++i)
                polyCentersZ[i] = new float[stickerInds[i].length]; // XXX ALLOCATION!

            for (int i = 0; i < drawListSize; ++i)
            {
                int i0i1[] = drawList[i];
                int i0 = i0i1[0];
                int i1 = i0i1[1];
                int poly[] = stickerInds[i0][i1];
                float sum = 0.f;
                for (int j = 0; j < poly.length; ++j)
                    sum += verts[poly[j]][2];
                Assert(poly.length != 0);
                polyCentersZ[i0][i1] = sum / poly.length;
            }

            final float finalPolyCentersZ[][] = polyCentersZ;
            SortStuff.sortRange(drawList, 0, drawListSize-1, new SortStuff.Comparator() { // XXX ALLOCATION! (need to make sort smarter)
                public int compare(Object i, Object j)
                {
                    int[] i0i1 = (int[])i;
                    int[] j0j1 = (int[])j;
                    float iZ = finalPolyCentersZ[i0i1[0]][i0i1[1]];
                    float jZ = finalPolyCentersZ[j0j1[0]][j0j1[1]];
                    // sort from increasing z to decreasing! that is because the z's got negated just before the projection!
                    return iZ > jZ ? -1 :
                           iZ < jZ ? 1 : 0;
                }
            });
        }
        if (verboseLevel >= 3) System.out.println("    after z-sort: stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));

        frame.drawListSize = drawListSize;

        if (verboseLevel >= 2) System.out.println("out Glue.computeFrame");
    } // computeFrame

    /**
    * Return the index of the sticker and polygon within sticker if hit,
    * or null if nothing hit.
    * NOTE: assumes Y is inverted, for the CCW test.
    * XXX I think I want to take out the Y inversion from the Frame?  not sure
    */
    public static int[] pick(float x, float y,
                             Frame frame,
                             GenericPuzzleDescription puzzleDescription)
    {
        float thispoint[] = {x, y};
        // From front to back, returning the first hit
        float verts[][] = frame.verts;
        int drawList[][] = frame.drawList;
        int stickerInds[][][] = puzzleDescription.getStickerInds();
        for (int i = frame.drawListSize-1; i >= 0; --i)
        {
            int item[] = drawList[i];
            int iSticker = item[0];
            int iPolyWithinSticker = item[1];
            int poly[] = stickerInds[iSticker][iPolyWithinSticker];
            int j;
            for (j = 0; j < poly.length; ++j)
                if (twice_triangle_area(verts[poly[j]], verts[poly[(j+1)%poly.length]], thispoint) > 0)
                    break; // it's CW  (>0 means CW since inverted)
            if (j == poly.length) // they were all CCW, so we hit this poly
                return item; // {iSticker, iPolyWithinSticker}
        }
        return null;
    }

    public static int pickSticker(float x, float y,
                                  Frame frame,
                                  GenericPuzzleDescription puzzleDescription)
    {
        int iStickerAndPoly[] = pick(x, y, frame, puzzleDescription);
        return iStickerAndPoly != null ? iStickerAndPoly[0] : -1;
    }

    // Pick poly center if it's a 2x2x2x2, sticker center otherwise.
    public static float[] pickPolyOrStickerCenter(float x, float y,
                                                  Frame frame,
                                                  GenericPuzzleDescription puzzleDescription)
    {
        int hit[] = pick(x, y, frame, puzzleDescription);
        if (hit == null)
            return null;
        // XXX would really like to map the pick point back to 4d...
        // XXX for now, map the polygon center back.

        // XXX argh, this is sure overkill here...
        float verts[][] = new float[puzzleDescription.nVerts()][puzzleDescription.nDims()];
        puzzleDescription.computeStickerVertsAtRest(verts,
                                                    1.f,  // faceShrink
                                                    1.f); // stickerShrink
        int stickerInds[][][] = puzzleDescription.getStickerInds();
        // XXX not sure which of the following are better if either-- maybe poly for 2x, sticker otherwise? it's definitely disconcerting when different parts of the sticker do diff things...
        int sticker[][] = stickerInds[hit[0]];
        int poly[] = sticker[hit[1]];
        float polyCenter[] = VecMath.averageIndexed(poly, verts);
        float stickerCenter[] = VecMath.averageIndexed(sticker, verts);
        float center[];

        //System.out.println("    poly center = "+VecMath.toString(polyCenter));
        //System.out.println("    sticker center = "+VecMath.toString(stickerCenter));

        // XXX total hack-- use poly center if we think it's the 2x2x2x2 puzzle
        // XXX and the sticker center otherwise.

        boolean itsProbablyThe2 = VecMath.normsqrd(stickerCenter) == 1.75
                               && (VecMath.normsqrd(polyCenter) == 1.5
                                || VecMath.normsqrd(polyCenter) == 2.5);
        if (verboseLevel >= 1) System.out.println("itsProbablyThe2 = "+itsProbablyThe2);

        if (itsProbablyThe2)
            center = polyCenter;
        else
            center = stickerCenter;
        return center;
    } // pickPolyOrStickerCenter

    public static int pickGrip(float x, float y,
                               Frame frame,
                               GenericPuzzleDescription puzzleDescription)
    {
        float polyOrStickerCenter[] = pickPolyOrStickerCenter(x, y, frame, puzzleDescription);
        if (polyOrStickerCenter == null)
            return -1;
        int iGrip = puzzleDescription.getClosestGrip(polyOrStickerCenter);
        return iGrip;
    }

    public static float[] pickNicePointToRotateToCenter(float x, float y,
                                                        Frame frame,
                                                        GenericPuzzleDescription puzzleDescription)
    {
        float polyOrStickerCenter[] = pickPolyOrStickerCenter(x, y, frame, puzzleDescription);
        if (polyOrStickerCenter == null)
            return null;
        float nicePoint[] = puzzleDescription.getClosestNicePointToRotateToCenter(polyOrStickerCenter);
        return nicePoint;
    }


    public static void paintFrame(Frame frame,
                                  GenericPuzzleDescription puzzleDescription,
                                  int puzzleState[],

                                  boolean showShadows, // XXX or isShadows?
                                  Color ground,
                                  float faceRGB[][],
                                  boolean highlightByCubie,
                                  Color outlineColor,
                                  Graphics g)
    {
        if (verboseLevel >= 2) System.out.println("in Glue.paintFrame");

        float verts[][] = frame.verts;
        int drawListSize = frame.drawListSize;
        int drawList[][/*2*/] = frame.drawList;
        float brightnesses[][] = frame.brightnesses;
        int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][] = puzzleDescription.getStickerInds();

        int xs[] = new int[0], // XXX ALLOCATION
            ys[] = new int[0]; // XXX ALLOCATION
        Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();
        for (int iItem = 0; iItem < drawListSize; ++iItem)
        {
            int iSticker = drawList[iItem][0];
            int iPolyThisSticker = drawList[iItem][1];
            int poly[] = stickerInds[iSticker][iPolyThisSticker];
            float brightness = brightnesses[iSticker][iPolyThisSticker];
            int colorOfSticker = puzzleState[iSticker];
            float faceRGBThisSticker[] = faceRGB[colorOfSticker % faceRGB.length]; // XXX need to make more colors

            if (poly.length > xs.length)
            {
                xs = new int[poly.length]; // XXX ALLOCATION
                ys = new int[poly.length]; // XXX ALLOCATION
            }
            for (int i = 0; i < poly.length; ++i)
            {
                float vert[] = verts[poly[i]];
                xs[i] = (int)vert[0];
                ys[i] = (int)vert[1];
                //System.out.println(xs[i] + ", " + ys[i]);
            }
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
            g.fillPolygon(xs, ys, poly.length);
            if(!isShadows && outlineColor != null) {
                g.setColor(outlineColor);
                // uncomment the following line for an alternate outlining idea -MG
                // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                g.drawPolygon(xs, ys, poly.length);
            }
        }



        if (false) // body of MC4DSwing.paintFrame, for reference
        {
            // Just declare the variables so it will compile
            MagicCube.Frame mcframe = null;
            MagicCube.Frame shadow_frame = null;
            PuzzleState state = null;
            MagicCube.Stickerspec stickerUnderMouse = null;
            boolean isShadows = false;
            float pixels2polySF = 1.f;
            int xOff = 0;
            int yOff = 0;

            //int
            //    xs[] = new int[4],
            //    ys[] = new int[4];
            //Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();
            for (int q = 0; q < mcframe.nquads; q++) {
                for (int i = 0; i < 4; i++) {
                    int qi = mcframe.quads[q][i];
                    xs[i] = (int)(xOff + mcframe.verts[qi][0]/pixels2polySF + .5);
                    ys[i] = (int)(yOff + mcframe.verts[qi][1]/pixels2polySF + .5);
                    //System.out.println(xs[i] + ", " + ys[i]);
                }
                int sid = mcframe.quadids[q]/6;
                int cs = state.idToColor(sid);
                //System.out.println(cs);
                float b = mcframe.brightnesses[q];
                Color stickercolor = new Color(
                    b*faceRGB[cs][0],
                    b*faceRGB[cs][1],
                    b*faceRGB[cs][2]);
                //boolean highlight = stickerUnderMouse != null && (highlightByCubie ? partOfCubie(sid) : stickerUnderMouse.id_within_cube == sid);
                //if(highlight)
                //    stickercolor = stickercolor.brighter().brighter();
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


    private static float tmpTWAf1[] = new float[2], tmpTWAf2[] = new float[2]; // scratch vars
    private static float twice_triangle_area(float v0[], float v1[], float v2[])
    {
        //float tmpTNf1[] = new float[2], tmpTNf2[] = new float[2];
        Vec_h._VMV2(tmpTWAf1, v1, v0);
        Vec_h._VMV2(tmpTWAf2, v2, v0);
        return Vec_h._VXV2(tmpTWAf1, tmpTWAf2);
    }

} // class GenericPipelineUtils
