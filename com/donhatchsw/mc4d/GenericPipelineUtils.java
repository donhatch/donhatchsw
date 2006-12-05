/**
* XXX does this class name suck?  well it sucks less than PolygonManager.
*
* Utilities for drawing and picking on a generic puzzle.
*
* This is a replacement for much of what PolygonManager did
* in the old implementation; however this class has NO STATE
* and is non-instantiatable, because state in something
* so vaguely named as a "polygon manager" is confusing and impossible
* to remember and DRIVES ME NUTS!
*
* However, there is a utility subclass called a Frame,
* which does hold state-- it is essentially a drawlist
* of 2d polygons, which is used by the three
* primary functions in this file:
*    computeFrame - computes an animation (or rest) Frame
*                   from the puzzle description and viewing parameters
*    paintFrame - draws the Frame
*    pick       - picks what is at a given point in the Frame
*/

// XXX blindly using same imports as MC4DSwing
// XXX these are the imports from MC4DSwing, with the ones we don't need commented out
import java.awt.*;
//import java.awt.event.*;
//import java.io.*;
//import java.util.Enumeration;
//import java.util.Stack;

//import javax.swing.*;
//import javax.swing.border.*;
//import javax.swing.filechooser.FileSystemView;

import com.donhatchsw.util.VecMath;

public class GenericPipelineUtils
{
    private GenericPipelineUtils() {} // non-instantiatable

    public static int verboseLevel = 0; // set to something else to debug
        // 0: nothing
        // 1: print on picks // XXX argh, that's OBNOXIOUS now that picks happen on hover
        // 2: and on computes and paints
        // 3: and dump arrays at each step

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
        public float shadowVerts[][/*3*/];

        public int drawListSize;
        public int shadowDrawListSize;
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
                                    
                                    float unitTowardsSunVec[/*3*/],
                                    float groundNormal[/*3*/], // null if no shadows
                                    float groundOffset)
    {
        if (verboseLevel >= 2) System.out.println("    in GenericPipelineUtils.computeFrame");

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
            if (groundNormal != null)
            {
                if (frame.shadowVerts == null
                 || frame.shadowVerts.length != nVerts
                 || nVerts>0 && frame.shadowVerts[0].length != nDims)
                    frame.shadowVerts = new float[nVerts][nDims-1];
            }
        }

        float verts[][] = frame.verts;
        float shadowVerts[][] = frame.shadowVerts;
        int drawList[][] = frame.drawList;
        int drawListSize = 0; // we'll set frame.drawListSize to this at end
        int shadowDrawListSize = 0; // we'll set frame.shadowDrawListSize to this at end

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
            // Make it so circumradius is 6.
            // XXX I have no basis for this except that empirically it makes
            // XXX the 3^4 hypercube match what the puzzle usually does
            float scale4d = 6.f/puzzleDescription.circumRadius();
            float rotScale4d[][] = VecMath.mxs(rot4d, scale4d); // XXX MEMORY ALLOCATION
            float temp[] = new float[4]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                VecMath.vxm(temp, verts[iVert], rotScale4d);
                VecMath.copyvec(verts[iVert], temp);
            }
        }
        if (verboseLevel >= 3) System.out.println("        after 4d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Clip to the 4d eye's front clipping plane
        //
        {
            // XXX DO ME?
        }
        //if (verboseLevel >= 3) System.out.println("        after 4d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

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
        if (verboseLevel >= 3) System.out.println("        after 4d->3d project: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

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
            shadowDrawListSize = groundNormal != null ? nBackFacing : 0;
        }
        if (verboseLevel >= 3) System.out.println("        after front-cell cull: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Rotate/scale in 3d
        // XXX could try to only do this on vertices that passed the culls
        //
        {
            if (verboseLevel >= 3) System.out.println("rot3d = "+com.donhatchsw.util.Arrays.toStringCompact(rot3d));
            float tempIn[] = new float[3]; // XXX MEMORY ALLOCATION
            float tempOut[] = new float[3]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    tempIn[i] = verts[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot3d); // only first 3... however the matrix can be 3x3 or 4x3
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    verts[iVert][i] = tempOut[i];
            }
        }
        if (verboseLevel >= 3) System.out.println("        after 3d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // If doing shadows,
        // project the shadows onto the ground plane.
        // Note, towardsSunVec doesn't really need to be normalized for this.
        //
        if (groundNormal != null)
        {
            // XXX explain this magic!
            float column[/*4*/][/*1*/] = {
                {-groundNormal[0]},
                {-groundNormal[1]},
                {-groundNormal[2]},
                {groundOffset},
            };
            float row[/*1*/][/*3*/] = {unitTowardsSunVec};
            float shadowMat[/*4*/][/*3*/] = VecMath.mxm(column, row);
            VecMath.mxs(shadowMat, shadowMat, 1.f/VecMath.dot(groundNormal, unitTowardsSunVec));
            for (int i = 0; i < 3; ++i)
                shadowMat[i][i] += 1.f;
            float tempIn[] = new float[3]; // XXX MEMORY ALLOCATION
            float tempOut[] = new float[3]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    tempIn[i] = verts[iVert][i];
                VecMath.vxm(tempOut, tempIn, shadowMat); // only first 3... however the matrix can be 3x3 or 4x3
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    shadowVerts[iVert][i] = tempOut[i];
            }

            if (verboseLevel >= 2) System.out.println("        after 3d shadow projection: verts[0] = "+com.donhatchsw.util.Arrays.toStringCompact(verts[0]));
            if (verboseLevel >= 2) System.out.println("        after 3d shadow projection: shadowVerts[0] = "+com.donhatchsw.util.Arrays.toStringCompact(shadowVerts[0]));
            if (verboseLevel >= 3) System.out.println("        after 3d shadow projection: shadowVerts = "+com.donhatchsw.util.Arrays.toStringCompact(shadowVerts));
        }



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

                if (false) // hard code to true to make it all max intensity
                    brightness = 1.f;

                frame.brightnesses[i0i1[0]][i0i1[1]] = brightness;
                //System.out.println("brightness = "+brightness);
            }
        }

        //
        // Clip to the 3d eye's front clipping plane
        //
        {
            // XXX DO ME?
        }
        //if (verboseLevel >= 3) System.out.println("        after 3d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));


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
        // XXX the following is dup code, lame
        if (groundNormal != null)
        {
            for (int i = 0; i < shadowVerts.length; ++i)
            {
                float z = eyeZ - shadowVerts[i][2];
                float invZ = 1.f/z;
                for (int j = 0; j < 2; ++j)
                    shadowVerts[i][j] *= invZ;
                shadowVerts[i][2] = z; // keep this for future reference
            }
        }

        if (verboseLevel >= 3) System.out.println("        after 3d->2d project: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));
        if (verboseLevel >= 2) System.out.println("        after 3d->3d project: shadowVerts[0] = "+com.donhatchsw.util.Arrays.toStringCompact(shadowVerts[0]));

        //
        // Back-face cull
        //
        boolean doBackfaceCull = true;
        if (doBackfaceCull)
        {
            // XXX ARGH! Need to NOT back-face cull the shadows!
            // XXX for now, just keep track of the culled polygons
            // XXX and put them back at the end, between
            // XXX drawListSize and shadowDrawListSize.
            int shadowExtraDrawList[][] = new int[drawListSize][]; // XXX MEMORY ALLOCATION
            int nBackFacing = 0;


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
                if (area > 0.f) // retain *front* facing polygons-- not we haven't inverted Y yet so this test looks as expected
                    drawList[nFrontFacing++] = i0i1;
                else
                    shadowExtraDrawList[nBackFacing++] = i0i1;
            }
            drawListSize = nFrontFacing;

            if (groundNormal != null)
            {
                // Put the culled ones back at the end, for shadows later
                for (int i = 0; i < nBackFacing; ++i)
                    drawList[drawListSize+i] = shadowExtraDrawList[i];
                shadowDrawListSize = nFrontFacing+nBackFacing;
            }
        }
        if (verboseLevel >= 3) System.out.println("        after back-face cull: drawList = "+com.donhatchsw.util.Arrays.toStringCompact(drawList));

        //
        // Rotate/scale in 2d
        // XXX could try to only do this on vertices that passed both culls
        //
        {
            if (verboseLevel >= 3) System.out.println("rot2d = "+com.donhatchsw.util.Arrays.toStringCompact(rot2d));
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
        // XXX the following is dup code, lame
        if (groundNormal != null)
        {
            if (verboseLevel >= 3) System.out.println("rot2d = "+com.donhatchsw.util.Arrays.toStringCompact(rot2d));
            float tempIn[] = new float[2]; // XXX MEMORY ALLOCATION
            float tempOut[] = new float[2]; // XXX MEMORY ALLOCATION
            for (int iVert = 0; iVert < shadowVerts.length; ++iVert)
            {
                for (int i = 0; i < 2; ++i) // 2 out of 4
                    tempIn[i] = shadowVerts[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot2d); // only first 2... however rot2d can be 2x2 or 3x2
                for (int i = 0; i < 2; ++i) // 2 out of 4
                    shadowVerts[iVert][i] = tempOut[i];
            }
        }

        if (verboseLevel >= 3) System.out.println("        after 2d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));


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
            com.donhatchsw.util.SortStuff.sortRange(drawList, 0, drawListSize-1, new com.donhatchsw.util.SortStuff.Comparator() { // XXX ALLOCATION! (need to make sort smarter)
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
        if (verboseLevel >= 3) System.out.println("        after z-sort: stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));

        frame.drawListSize = drawListSize;
        frame.shadowDrawListSize = groundNormal!=null ? shadowDrawListSize : 0;

        if (verboseLevel >= 2) System.out.println("    out GenericPipelineUtils.computeFrame");
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
        if (verboseLevel >= 1) System.out.println("    in GenericPipelineUtils.pick");
        float thispoint[] = {x, y};
        // From front to back, returning the first hit
        float verts[][] = frame.verts;
        int drawList[][] = frame.drawList;
        int stickerInds[][][] = puzzleDescription.getStickerInds();
        int pickedItem[] = null;
        for (int i = frame.drawListSize-1; i >= 0; --i) // front to back
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
            {
                pickedItem = item; // = {iSticker, iPolyWithinSticker}
                break;
            }
        }
        if (verboseLevel >= 1) System.out.println("    out GenericPipelineUtils.pick, returning "+(pickedItem==null?"null":("{iSticker="+pickedItem[0]+",iPolyWithinSticker="+pickedItem[1]+"}")));
        return pickedItem;
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

        //System.out.println("        poly center = "+VecMath.toString(polyCenter));
        //System.out.println("        sticker center = "+VecMath.toString(stickerCenter));

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


    // XXX figure out where to put this, if anywhere
    private static java.util.Random jitterGenerator = new java.util.Random();
    private static int jitterRadius = 0; // haha, for debugging, but cool effect, should publicize it

    public static void paintFrame(Frame frame,
                                  GenericPuzzleDescription puzzleDescription,
                                  int puzzleState[],

                                  boolean showShadows, // XXX or isShadows? haven't decided whether this should get called again for shadows or if we should do both here
                                  Color ground,
                                  float faceRGB[][],
                                  int iStickerUnderMouse,
                                  boolean highlightByCubie,
                                  Color outlineColor,
                                  Graphics g)
    {
        if (verboseLevel >= 2) System.out.println("    in GenericPipelineUtils.paintFrame");
        if (verboseLevel >= 2) System.out.println("        iStickerUnderMouse = "+iStickerUnderMouse);

        int drawList[][/*2*/] = frame.drawList;
        float brightnesses[][] = frame.brightnesses;
        int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][] = puzzleDescription.getStickerInds();
        int sticker2cubie[] = puzzleDescription.getSticker2Cubie();
        // Note, the range check protects against wild values of iStickerUnderMouse
        // (e.g. if left over from a previous larger puzzle).
        int iCubieUnderMouse = (iStickerUnderMouse < 0
                             || iStickerUnderMouse >= sticker2cubie.length) ? -1 : sticker2cubie[iStickerUnderMouse];

        int xs[] = new int[0], // XXX ALLOCATION
            ys[] = new int[0]; // XXX ALLOCATION
        Color shadowcolor = ground == null ? Color.black : ground.darker().darker().darker().darker();
        for (int iPass = 0; iPass < 2; ++iPass)
        {
            boolean isShadows = iPass == 0;
            float verts[][] = isShadows ? frame.shadowVerts : frame.verts;
            int drawListSize = isShadows ? frame.shadowDrawListSize : frame.drawListSize;
            //System.out.println("isShadows="+isShadows);
            //System.out.println("drawListSize="+drawListSize);
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
                    if (jitterRadius > 0)
                    {
                        xs[i] += jitterGenerator.nextInt(2*jitterRadius+1)-jitterRadius;
                        ys[i] += jitterGenerator.nextInt(2*jitterRadius+1)-jitterRadius;
                    }
                }
                Color stickercolor = new Color(
                    brightness*faceRGBThisSticker[0],
                    brightness*faceRGBThisSticker[1],
                    brightness*faceRGBThisSticker[2]);
                boolean highlight = highlightByCubie ? sticker2cubie[iSticker]==iCubieUnderMouse
                                                     : iSticker==iStickerUnderMouse;
                if(highlight)
                    stickercolor = stickercolor.brighter().brighter();

                g.setColor(isShadows ? shadowcolor : stickercolor);
                g.fillPolygon(xs, ys, poly.length);
                if(!isShadows && outlineColor != null) {
                    g.setColor(outlineColor);
                    // uncomment the following line for an alternate outlining idea -MG
                    // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                    g.drawPolygon(xs, ys, poly.length);
                }
            }
        }

        if (verboseLevel >= 2) System.out.println("    out GenericPipelineUtils.paintFrame");
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
