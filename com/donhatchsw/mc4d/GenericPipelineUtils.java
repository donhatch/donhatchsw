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
*    pick       - picks what is at a given 2d point in the Frame
*/

package com.donhatchsw.mc4d;

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
        // 1: print when there's a cycle in the topsort
        // 2: and on computes and paints
        // 3: and on picks
        // 4: and dump arrays at each step in compute

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

        public float verts[][/*4*/]; // x,y,z,w, not just x,y! see above
        public float shadowVerts[][/*3*/];

        // Each element of drawList is a pair i,j,
        // referring to the polygon stickerInds[i][j]
        // in the original puzzle description.
        // We store these rather than just straight indices into the vertices,
        // for a number of reasons: (1) so we can look up the face easily,
        // for color (2) so we can look up sticker and/or cubie easily,
        // for highlighting (3) we need these indices later for picking,
        // when the user clicks on something.
        public int drawListSize;
        public int shadowDrawListSize;
        public int drawList[][/*2*/];
        public float brightnesses[/*nStickers*/][/*nPolysThisSticker*/];

        // XXX need the various non-shrunk and shrunk edges in here somehow!  and need them to be associated with the faces!  Bleah!

        // Memory used by drawList (before culling and sorting).
        // We keep this around so that a Frame can be reused
        // without having to do any memory allocations.
        public int drawListBuffer[/*nStickers*/][/*nPolysThisSticker*/][/*2*/];

        public int[][/*2*/][/*3*/] partialOrderInfo; // optional, for debugging.  TODO: describe what it means here. currently it's described down in the sort, which generates it

        GenericPuzzleDescription puzzleDescription; // puzzle description used when filling it, to be used later for picking
    } // class Frame

    private static void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }
    private static String $(Object obj) { return com.donhatchsw.util.Arrays.toStringCompact(obj); }  // convenience

    public interface Callback { public void call(); }



    /**
    * Compute a frame of animation.
    * Attempts to avoid doing any new memory allocations
    * when called repeatedly on a given puzzleDescription.
    */
    public static void computeFrame(Frame frame, // return into here

                                    GenericPuzzleDescription puzzleDescription,

                                    float faceShrink4d,
                                    float stickerShrink4d,
                                    float faceShrink3d,
                                    float stickerShrink3d,
                                    float stickersShrinkTowardsFaceBoundaries,

                                    int iGripOfTwist,    // -1 if not twisting
                                    int twistDir,               
                                    int twistSliceMask,
                                    boolean twistFuttIfPossible,
                                    float fracIntoTwist,

                                    float rot4d[/*4*/][/*4 or 5*/],
                                    float eyeW,
                                    float rot3d[/*3*/][/*3 or 4*/],
                                    float eyeZ,
                                    float rot2d[/*2*/][/*2 or 3*/],
                                    
                                    float unitTowardsSunVec[/*3*/],
                                    float groundNormal[/*3*/], // null if no shadows
                                    float groundOffset,

                                    boolean useTopsort,
                                        boolean showPartialOrder)
    {
        if (verboseLevel >= 2) System.out.println("    in GenericPipelineUtils.computeFrame(iGripOfTwist="+iGripOfTwist+", twistDir="+twistDir+", twistSliceMask="+twistSliceMask+", fracIntoTwist="+fracIntoTwist+"");

        int nOriginalDims = puzzleDescription.nDims();
        int nDisplayDims = puzzleDescription.nDisplayDims();
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
             || nVerts>0 && frame.verts[0].length != nDisplayDims)
                frame.verts = new float[nVerts][nDisplayDims];
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
                 || nVerts>0 && frame.shadowVerts[0].length != nDisplayDims)
                    frame.shadowVerts = new float[nVerts][nDisplayDims-1];
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

        int vert2sticker[] = new int[verts.length]; // XXX MEM ALLOCATION
        for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
        {
            int thisStickerInds[][] = stickerInds[iSticker];
            for (int i = 0; i < thisStickerInds.length; ++i)
            for (int j = 0; j < thisStickerInds[i].length; ++j)
                vert2sticker[thisStickerInds[i][j]] = iSticker;
        }


        float stickerCenters[][] = new float[nStickers][nDisplayDims]; // XXX MEM ALLOCATION
        float stickerAltCenters[][] = new float[nStickers][nDisplayDims]; // XXX MEM ALLOCATION
        float stickerShrinkPoints[][] = new float[nStickers][nDisplayDims]; // XXX MEM ALLOCATION
        float perStickerFaceCenters[][] = new float[nStickers][nDisplayDims]; // XXX MEM ALLOCATION

        //
        // Get the non-shrunk 4d verts and shrink-to points
        // from the puzzle description.
        //
        if (iGripOfTwist == -1
         || fracIntoTwist == 0.  // If we don't add this, we'll do extra work doing the partially twisted thing when a twist is over, in which case the model reports it's still twisting but frac is 0 (not sure that should be happening, but it does)
         )
        {
            puzzleDescription.computeVertsAndShrinkToPointsAtRest(
                verts,
                stickerCenters,
                stickerAltCenters,
                perStickerFaceCenters);
        }
        else
        {
            puzzleDescription.computeVertsAndShrinkToPointsPartiallyTwisted(
                verts,
                stickerCenters,
                stickerAltCenters,
                perStickerFaceCenters,
                iGripOfTwist,
                twistDir,
                twistSliceMask,
                twistFuttIfPossible,
                fracIntoTwist);
        }

        float[][] verts_totally_unshrunk = VecMath.copymat(verts);  // XXX MEMORY ALLOCATION
        float[][] verts_yesfaceshrink_nostickershrink = VecMath.copymat(verts);  // XXX MEMORY ALLOCATION

        //
        // Shrink the vertices towards the shrink-to points in 4d.
        // And, shrink the sticker centers towards the face centers,
        // so they will be appropriate for subsequent shrink passes.
        //
        {
            int sticker2face[] = puzzleDescription.getSticker2Face();

            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                VecMath.lerp(stickerShrinkPoints[iSticker],
                             stickerCenters[iSticker], stickerAltCenters[iSticker], stickersShrinkTowardsFaceBoundaries); // BEFORE shrinking towards face center
            }

            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                int iSticker = vert2sticker[iVert];
                VecMath.lerp(verts[iVert],
                             stickerShrinkPoints[iSticker], verts[iVert], stickerShrink4d);
                VecMath.lerp(verts[iVert],
                             perStickerFaceCenters[iSticker], verts[iVert], faceShrink4d);
                VecMath.lerp(verts_yesfaceshrink_nostickershrink[iVert],
                             perStickerFaceCenters[iSticker], verts_yesfaceshrink_nostickershrink[iVert], faceShrink4d);
            }
        }

        // The five arrays we need to rotate and project...
        float arrays[][][] = {
            verts_totally_unshrunk,
            verts_yesfaceshrink_nostickershrink,
            verts,
            stickerShrinkPoints,
            perStickerFaceCenters,
        };
        
        //
        // Rotate/scale in 4d.
        // Not just the verts, but also the shrink-to points,
        // since we'll need to shrink towards them again
        // for the 3d part of the shrink.  And the per-sticker face centers too.
        //
        {
            // Make it so circumradius is 1.
            // That way any 4d eye distance > 1 is safe.
            float scale4d = 1.f/puzzleDescription.circumRadius();
            float rotScale4d[][] = VecMath.mxs(rot4d, scale4d); // XXX MEMORY ALLOCATION
            float temp[] = new float[4]; // XXX MEMORY ALLOCATION
            for (int iArray = 0; iArray < 3; ++iArray)
            {
                float array[][] = arrays[iArray];
                for (int i = 0; i < array.length; ++i)
                {
                    VecMath.vxm(temp, array[i], rotScale4d);
                    VecMath.copyvec(array[i], temp);
                }
            }
        }
        if (verboseLevel >= 4) System.out.println("        after 4d rotscale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Clip to the 4d eye's front clipping plane
        //
        {
            // XXX DO ME?
        }
        //if (verboseLevel >= 4) System.out.println("        after 4d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Project down to 3d.
        // Unshrunk verts, verts, face centers, and sticker shrink-to points.
        //
        {
            for (int iArray = 0; iArray < arrays.length; ++iArray)
            {
                float array[][] = arrays[iArray];
                for (int i = 0; i < array.length; ++i)
                {
                    float w = eyeW - array[i][3];
                    float invW = 1.f/w;
                    for (int j = 0; j < 3; ++j)
                        array[i][j] *= invW;
                    array[i][3] = w; // keep this for future reference
                }
            }
        }
        if (verboseLevel >= 4) System.out.println("        after 4d->3d project: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        boolean stickerVisibilities[] = new boolean[nStickers]; // XXX memory allocation!

        //
        // Front-cell cull
        //
        boolean doFrontCellCull = true;
        if (false) // XXX it's interesting to set this to true! think about it
            if (nOriginalDims != nDisplayDims)
                doFrontCellCull = false;
        {
            int nBackfacing = 0;
            float mat[][] = new float[3][3]; // XXX MEMORY ALLOCATION
            for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
            {
                int thisStickerInds[][] = stickerInds[iSticker];
                float v0[] = verts[thisStickerInds[0][0]];
                float v1[] = verts[thisStickerInds[0][1]];
                float v2[] = verts[thisStickerInds[0][2]];
                float v3[] = verts[thisStickerInds[1][0]];
                VecMath.vmv(3, mat[0], v1, v0); // 3 out of 4
                VecMath.vmv(3, mat[1], v2, v0); // 3 out of 4
                VecMath.vmv(3, mat[2], v3, v0); // 3 out of 4
                float volume = VecMath.vxvxv3(mat[0], mat[1], mat[2]);
                if (!doFrontCellCull || volume < 0.f) // draw only *back* cells; cull front ones
                {
                    // append references to this sticker's polys into drawList
                    for (int iPolyThisSticker = 0; iPolyThisSticker < thisStickerInds.length; ++iPolyThisSticker)
                        drawList[nBackfacing++] = frame.drawListBuffer[iSticker][iPolyThisSticker]; // = {iSticker,iPolyThisSticker}
                    stickerVisibilities[iSticker] = true;
                }
                else
                {
                    stickerVisibilities[iSticker] = false;
                }
            }
            drawListSize = nBackfacing;
            shadowDrawListSize = groundNormal != null ? nBackfacing : 0;
        }
        if (verboseLevel >= 4) System.out.println("        after front-cell cull: drawList = "+com.donhatchsw.util.Arrays.toStringCompact(com.donhatchsw.util.Arrays.subarray(drawList,0,drawListSize)));

        //
        // 3d face shrink and sticker shrink
        // XXX could try to do this on only vertices that passed the culls
        // XXX need to do this with the xformed and projected shrink-to point,
        // XXX not calculate screwy centers on the fly here.
        // Q: should the sticker shrink-to point always be shrunk towards the face shrink-to point in 4d?  Or does 3d make sense?  Well, 4d would be more robust, since that shrinking could prevent having to do with projected original points that could end up behind the eye.  So that's what we do.
        //
        if (stickerShrink3d != 1.f)
            for (int iVert = 0; iVert < verts.length; ++iVert)
                VecMath.lerp(verts[iVert], stickerShrinkPoints[vert2sticker[iVert]], verts[iVert], stickerShrink3d);
        if (faceShrink3d != 1.f)
            for (int iVert = 0; iVert < verts.length; ++iVert)
                VecMath.lerp(verts[iVert], perStickerFaceCenters[vert2sticker[iVert]], verts[iVert], faceShrink3d);

        //
        // Rotate/scale in 3d
        // XXX could try to do this on only vertices that passed the culls
        //
        {
            if (verboseLevel >= 4) System.out.println("rot3d = "+com.donhatchsw.util.Arrays.toStringCompact(rot3d));
            float tempIn[] = new float[3]; // XXX MEMORY ALLOCATION
            float tempOut[] = new float[3]; // XXX MEMORY ALLOCATION
            // TODO: dup code.  maybe should use the "five arrays" trick used above
            for (int iVert = 0; iVert < verts_totally_unshrunk.length; ++iVert)
            {
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    tempIn[i] = verts_totally_unshrunk[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot3d); // only first 3... however the matrix can be 3x3 or 4x3
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    verts_totally_unshrunk[iVert][i] = tempOut[i];
            }
            for (int iVert = 0; iVert < verts_yesfaceshrink_nostickershrink.length; ++iVert)
            {
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    tempIn[i] = verts_yesfaceshrink_nostickershrink[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot3d); // only first 3... however the matrix can be 3x3 or 4x3
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    verts_yesfaceshrink_nostickershrink[iVert][i] = tempOut[i];
            }
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    tempIn[i] = verts[iVert][i];
                VecMath.vxm(tempOut, tempIn, rot3d); // only first 3... however the matrix can be 3x3 or 4x3
                for (int i = 0; i < 3; ++i) // 3 out of 4
                    verts[iVert][i] = tempOut[i];
            }
        }
        if (verboseLevel >= 4) System.out.println("        after 3d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

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
            if (verboseLevel >= 4) System.out.println("        after 3d shadow projection: shadowVerts = "+com.donhatchsw.util.Arrays.toStringCompact(shadowVerts));
        }



        //
        // Compute brightnesses.
        //
        {
            // XXX should do this on only faces that are going
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
                VecMath.vmv(3, e1, v1, v0);
                VecMath.vmv(3, e2, v2, v0);
                VecMath.vxv3(triangleNormal, e1, e2);
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
        //if (verboseLevel >= 4) System.out.println("        after 3d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Compute and save the polygon normals in 3d.
        // These will be needed later for the topsort.
        // XXX since we're doing this anyway, could do the backface culling here too
        // XXX instead of later on the 2d polygons
        //
        // TODO: revisit whether we still need this at all.  it was for a sanity check inside the sort function that is maybe defunct.
        //
        float polyCenters3d[][][] = null;
        float polyNormals3d[][][] = null;
        if (useTopsort)
        {
            polyCenters3d = new float[nStickers][][]; // XXX ALLOCATION
            polyNormals3d = new float[nStickers][][]; // XXX ALLOCATION
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                polyCenters3d[iSticker] = new float[stickerInds[iSticker].length][3];
                polyNormals3d[iSticker] = new float[stickerInds[iSticker].length][3];
            }

            float mat[][] = new float[2][3]; // XXX ALLOCATION

            for (int i = 0; i < drawListSize; ++i)
            {
                int i0i1[] = drawList[i];
                int iSticker = i0i1[0];
                int iPolyThisSticker = i0i1[1];
                int poly[] = stickerInds[iSticker][iPolyThisSticker];
                float v0[] = verts[poly[0]];
                float v1[] = verts[poly[1]];
                float v2[] = verts[poly[2]];
                VecMath.vmv(3, mat[0], v1, v0); // 3 out of 4
                VecMath.vmv(3, mat[1], v2, v0); // 3 out of 4
                VecMath.vxv3(polyNormals3d[iSticker][iPolyThisSticker],
                             mat[0], mat[1]);
                VecMath.normalize(polyNormals3d[iSticker][iPolyThisSticker],
                                  polyNormals3d[iSticker][iPolyThisSticker]);

                float polyCenter3d[] = polyCenters3d[iSticker][iPolyThisSticker];
                for (int iVertThisPoly = 0; iVertThisPoly < poly.length; ++iVertThisPoly)
                    VecMath.vpv(3, polyCenter3d,
                                   polyCenter3d,
                                   verts[poly[iVertThisPoly]]); // 3 out of 4
                VecMath.vxs(polyCenter3d,
                            polyCenter3d,
                            1.f/poly.length);
                // XXX Oh BLEAH! This will mess up if the polygons are coincident!
                // XXX really want a point behind the polygon... i.e. the sticker center?
                // XXX but it seems to me there are cases when that would give
                // XXX the wrong answer... namely the sticker center is on the 
                // XXX inside out side but the polygon isn't!  bleah.
                // XXX okay I know, I'll test for the coincident case with epsilon...
                // XXX if the centers are almost coincident, I'll assume the adjacency
                // XXX is valid.
            }
        }


        //
        // Project down to 2d
        // XXX could try to do this on only vertices that passed the culls
        //
        {
            for (int i = 0; i < verts_totally_unshrunk.length; ++i)
            {
                float z = eyeZ - verts_totally_unshrunk[i][2];
                float invZ = 1.f/z;
                for (int j = 0; j < 2; ++j)
                    verts_totally_unshrunk[i][j] *= invZ;
                verts_totally_unshrunk[i][2] = z; // keep this for future reference
            }
            for (int i = 0; i < verts_yesfaceshrink_nostickershrink.length; ++i)
            {
                float z = eyeZ - verts_yesfaceshrink_nostickershrink[i][2];
                float invZ = 1.f/z;
                for (int j = 0; j < 2; ++j)
                    verts_yesfaceshrink_nostickershrink[i][j] *= invZ;
                verts_yesfaceshrink_nostickershrink[i][2] = z; // keep this for future reference
            }
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
        if (verboseLevel >= 4) System.out.println("        after 3d->2d project: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));
        if (verboseLevel >= 2) if (shadowVerts != null) System.out.println("        after 3d->3d project: shadowVerts[0] = "+com.donhatchsw.util.Arrays.toStringCompact(shadowVerts[0]));

        boolean stickerPolyIsStrictlyBackfacing[][] = new boolean[nStickers][];
        boolean unshrunkStickerPolyIsStrictlyBackfacing[][] = new boolean[nStickers][];
        boolean partiallyShrunkStickerPolyIsStrictlyBackfacing[][] = new boolean[nStickers][];
        for (int iSticker = 0; iSticker < nStickers; ++iSticker)
        {
            stickerPolyIsStrictlyBackfacing[iSticker] = new boolean[stickerInds[iSticker].length];
            unshrunkStickerPolyIsStrictlyBackfacing[iSticker] = new boolean[stickerInds[iSticker].length];
            partiallyShrunkStickerPolyIsStrictlyBackfacing[iSticker] = new boolean[stickerInds[iSticker].length];
        }

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
            int nBackfacing = 0;

            float mat[][] = new float[2][2]; // XXX ALLOCATION

            int nFrontFacing = 0;
            for (int i = 0; i < drawListSize; ++i)
            {
                int i0i1[] = drawList[i];
                int poly[] = stickerInds[i0i1[0]][i0i1[1]];

                {
                    float v0[] = verts[poly[0]];
                    float v1[] = verts[poly[1]];
                    float v2[] = verts[poly[2]];
                    VecMath.vmv(2, mat[0], v1, v0); // 2 out of 4
                    VecMath.vmv(2, mat[1], v2, v0); // 2 out of 4
                    float area = VecMath.vxv2(mat[0], mat[1]);
                    boolean thisStickerPolyIsStrictlyBackfacing = area < 0.f; // retain *front* facing polygons-- note we haven't inverted Y yet so this test looks as expected
                    if (!thisStickerPolyIsStrictlyBackfacing)
                        drawList[nFrontFacing++] = i0i1;
                    stickerPolyIsStrictlyBackfacing[i0i1[0]][i0i1[1]] = thisStickerPolyIsStrictlyBackfacing;
                }
                // same for unshrunk, to compute unshrunkStickerPolyIsStrictlyBackfacing, but do *not* append to drawlist
                {
                    float v0[] = verts_totally_unshrunk[poly[0]];
                    float v1[] = verts_totally_unshrunk[poly[1]];
                    float v2[] = verts_totally_unshrunk[poly[2]];
                    VecMath.vmv(2, mat[0], v1, v0); // 2 out of 4
                    VecMath.vmv(2, mat[1], v2, v0); // 2 out of 4
                    float area = VecMath.vxv2(mat[0], mat[1]);
                    boolean thisStickerPolyIsStrictlyBackfacing = area < 0.f; // retain *front* facing polygons-- note we haven't inverted Y yet so this test looks as expected
                    unshrunkStickerPolyIsStrictlyBackfacing[i0i1[0]][i0i1[1]] = thisStickerPolyIsStrictlyBackfacing;
                }
                // likewise for partially
                {
                    float v0[] = verts_yesfaceshrink_nostickershrink[poly[0]];
                    float v1[] = verts_yesfaceshrink_nostickershrink[poly[1]];
                    float v2[] = verts_yesfaceshrink_nostickershrink[poly[2]];
                    VecMath.vmv(2, mat[0], v1, v0); // 2 out of 4
                    VecMath.vmv(2, mat[1], v2, v0); // 2 out of 4
                    float area = VecMath.vxv2(mat[0], mat[1]);
                    boolean thisStickerPolyIsStrictlyBackfacing = area < 0.f; // retain *front* facing polygons-- note we haven't inverted Y yet so this test looks as expected
                    partiallyShrunkStickerPolyIsStrictlyBackfacing[i0i1[0]][i0i1[1]] = thisStickerPolyIsStrictlyBackfacing;
                }
            }
            drawListSize = nFrontFacing;

            if (groundNormal != null)
            {
                // Put the culled ones back at the end, for shadows later
                for (int i = 0; i < nBackfacing; ++i)
                    drawList[drawListSize+i] = shadowExtraDrawList[i];
                shadowDrawListSize = nFrontFacing+nBackfacing;
            }
        }
        if (verboseLevel >= 4) System.out.println("        after back-face cull: drawList = "+com.donhatchsw.util.Arrays.toStringCompact(com.donhatchsw.util.Arrays.subarray(drawList,0,drawListSize)));

        //
        // Rotate/scale in 2d
        // XXX could try to do this on only vertices that passed both culls
        //
        {
            if (verboseLevel >= 4) System.out.println("rot2d = "+com.donhatchsw.util.Arrays.toStringCompact(rot2d));
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
            if (verboseLevel >= 4) System.out.println("rot2d = "+com.donhatchsw.util.Arrays.toStringCompact(rot2d));
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

        if (verboseLevel >= 4) System.out.println("        after 2d rot/scale/trans: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        if (useTopsort
         && puzzleDescription.getAdjacentStickerPairs() == null)
        {
            if (verboseLevel >= 2)
                System.out.println("        topsort forced off because this puzzle description didn't give any adjacent sticker pairs!");
            useTopsort = false; // XXX bleah! haven't got it implemented for 3d puzzles yet
        }
        if (useTopsort)
        {
            //
            // Try spiffy topsorting
            //
            int nStickersToSort = nStickers; // set to something less to debug
            //int nStickersToSort = 4; // set to something less to debug
            float cutNormal[] = {1,0,0,0};
            float cutOffsets[] = {};
            int iFaceOfTwist = (iGripOfTwist==-1 ? -1 : puzzleDescription.getGrip2Face()[iGripOfTwist]);
            if (iFaceOfTwist == -1) iFaceOfTwist = Math.min(mostRecentFaceOfTwist,puzzleDescription.nFaces()-1); else mostRecentFaceOfTwist = iFaceOfTwist; // XXX get rid!
            if (iFaceOfTwist != -1)
            {
                double cutNormalD[] = puzzleDescription.getFaceInwardNormals()[iFaceOfTwist];
                double cutOffsetsD[] = puzzleDescription.getFaceCutOffsets()[iFaceOfTwist];
                cutNormal = new float[]{(float)cutNormalD[0],(float)cutNormalD[1],(float)cutNormalD[2],(float)cutNormalD[3]};
                cutOffsets = new float[cutOffsetsD.length];
                for (int iOffset = 0; iOffset < cutOffsets.length; ++iOffset)
                    cutOffsets[iOffset] = (float)cutOffsetsD[iOffset];
            }

            // XXX ARGH!  Doing this all over again
            // XXX because we clobbered the 4d verts...
            // XXX we need to classify the stickers by slice earlier!

            // XXX And really need to remove those offsets in cutOffsets whose two slices are moving together!
            int sticker2Slice[] = new int[nStickersToSort];
            {
                float stickerCentersAtRest[][] = new float[nStickers][4];
                puzzleDescription.computeVertsAndShrinkToPointsAtRest(
                    null, // verts
                    stickerCentersAtRest,
                    null, // alt sticker centers
                    null); // per sticker face centers;
                for (int iSticker = 0; iSticker < nStickersToSort; ++iSticker)
                {
                    float stickerCenter[] = stickerCentersAtRest[iSticker];
                    float stickerCenterOffset = VecMath.dot(stickerCenter, cutNormal);
                    int stickerSlice = 0;
                    while (stickerSlice < cutOffsets.length
                        && stickerCenterOffset > cutOffsets[stickerSlice])
                        stickerSlice++;
                    sticker2Slice[iSticker] = stickerSlice;
                }
            }

            // Bleah!  Need to calculate z values after all so that the topsort
            // can resolve cycles.  I.e. within each cycle,
            // topsort doesn't know what to do so we can just make it z-sort
            // within the cycle.
            float stickerCentersZ[/*nStickers*/] = new float[nStickersToSort]; // XXX ALLOCATION!
            for (int iSticker = 0; iSticker < nStickersToSort; ++iSticker)
            {
                if (!stickerVisibilities[iSticker])
                    continue;
                float sum = 0.f;
                int nVertsThisSticker = 0;
                for (int iPolyThisSticker = 0; iPolyThisSticker < stickerInds[iSticker].length; ++iPolyThisSticker)
                {
                    int poly[] = stickerInds[iSticker][iPolyThisSticker];
                    for (int j = 0; j < poly.length; ++j)
                        sum += verts[poly[j]][2];
                    nVertsThisSticker += poly.length;
                }
                CHECK(nVertsThisSticker != 0);
                stickerCentersZ[iSticker] = sum / nVertsThisSticker;
            }

            int stickerSortOrder[] = new int[nStickers]; // XXX allocation
            int partialOrderInfoAddress[][][][] = (showPartialOrder ? new int[1][][][] : null);
            int nSortedStickers = VeryCleverPaintersSortingOfStickers.sortStickersBackToFront(
                    nStickersToSort,
                    puzzleDescription.getAdjacentStickerPairs(),
                    stickerVisibilities,
                    unshrunkStickerPolyIsStrictlyBackfacing,
                    partiallyShrunkStickerPolyIsStrictlyBackfacing,
                    VecMath.mxv(rot4d, new float[]{0,0,0,eyeW}), // in opposite order so we multiply the eye by the *inverse* of the matrix, to get it into object space  XXX put this elsewhere
                    cutNormal,
                    cutOffsets,
                    sticker2Slice,
                    puzzleDescription.getSticker2Face(),
                    stickerSortOrder,
                    partialOrderInfoAddress,
                    stickerCentersZ,
                    polyCenters3d,
                    polyNormals3d);

            if (showPartialOrder) {
                frame.partialOrderInfo = partialOrderInfoAddress[0];
            }

            drawListSize = 0;
            for (int iSorted = 0; iSorted < nSortedStickers; ++iSorted)
            {
                int iSticker = stickerSortOrder[iSorted];
                if (!stickerVisibilities[iSticker])
                    continue;
                for (int iPolyThisSticker = 0; iPolyThisSticker < stickerInds[iSticker].length; ++iPolyThisSticker)
                {
                    if (doBackfaceCull
                     && stickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker])
                        continue;
                    drawList[drawListSize++] = frame.drawListBuffer[iSticker][iPolyThisSticker]; // = {iSticker,iPolyThisSticker}
                }
            }
        }
        else
        {
            //
            // Sort drawlist polygons back-to-front,
            // using the z values that we retained from before the 3d->2d projection
            // (but there's less work to do now that we culled back faces).
            // TODO: should do this per sticker I think; it would solve a lot of the immediate problems on 3d and 2d puzzles!
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
                    CHECK(poly.length != 0);
                    polyCentersZ[i0][i1] = sum / poly.length;
                }

                final float finalPolyCentersZ[][] = polyCentersZ;
                com.donhatchsw.util.SortStuff.sort(drawList, 0, drawListSize, new com.donhatchsw.util.SortStuff.Comparator() { // XXX ALLOCATION! (need to make sort smarter)
                    @Override public int compare(Object i, Object j)
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
        }

        if (verboseLevel >= 4) System.out.println("        after z-sort: stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));

        frame.drawListSize = drawListSize;
        frame.shadowDrawListSize = groundNormal!=null ? shadowDrawListSize : 0;
        frame.puzzleDescription = puzzleDescription;

        if (verboseLevel >= 2) System.out.println("    out GenericPipelineUtils.computeFrame");
    } // computeFrame

    private static int mostRecentFaceOfTwist = -1; // XXX get rid

    /**
    * Return the index of the sticker and polygon within sticker if hit,
    * or null if nothing hit.
    * NOTE: assumes Y is inverted, for the CCW test.
    * XXX I think I want to take out the Y inversion from the Frame?  not sure
    */
    public static int[] pick(float x, float y,
                             Frame frame)
    {
        if (verboseLevel >= 3) System.out.println("    in GenericPipelineUtils.pick");
        if (frame.puzzleDescription == null)
            return null;
        float thispoint[] = {x, y};
        // From front to back, returning the first hit
        float verts[][] = frame.verts;
        int drawList[][] = frame.drawList;
        int stickerInds[][][] = frame.puzzleDescription.getStickerInds();
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
        if (verboseLevel >= 3) System.out.println("    out GenericPipelineUtils.pick, returning "+(pickedItem==null?"null":("{iSticker="+pickedItem[0]+",iPolyWithinSticker="+pickedItem[1]+"}")));
        return pickedItem;
    }

    public static int pickSticker(float x, float y,
                                  Frame frame)
    {
        int iStickerAndPoly[] = pick(x, y, frame);
        return iStickerAndPoly != null ? iStickerAndPoly[0] : -1;
    }

    public static float[][] pickPolyAndStickerAndFaceCenter(float x, float y,
                                                     Frame frame)
    {
        int hit[] = pick(x, y, frame);
        if (hit == null)
            return null;
        // XXX would really like to map the pick point back to 4d...
        // XXX for now, map the polygon center back.

        // XXX argh, this is sure overkill here...
        float verts[][] = new float[frame.puzzleDescription.nVerts()][frame.puzzleDescription.nDisplayDims()];
        frame.puzzleDescription.computeVertsAndShrinkToPointsAtRest(
                verts,
                null, // sticker centers
                null, // alt sticker centers,
                null); // per sticker face centers

        int stickerInds[][][] = frame.puzzleDescription.getStickerInds();
        // XXX not sure which of the following are better if either-- maybe poly for 2x, sticker otherwise? it's definitely disconcerting when different parts of the sticker do diff things...
        int sticker[][] = stickerInds[hit[0]];
        int poly[] = sticker[hit[1]];
        float polyCenter[] = VecMath.averageIndexed(poly, verts);
        float stickerCenter[] = VecMath.averageIndexed(sticker, verts);
        float faceCenter[] = frame.puzzleDescription.getFaceCentersAtRest()[frame.puzzleDescription.getSticker2Face()[hit[0]]];

        //System.out.println("        poly center = "+VecMath.toString(polyCenter));
        //System.out.println("        sticker center = "+VecMath.toString(stickerCenter));
        return new float[][]{polyCenter, stickerCenter, faceCenter};
    } // pickPolyAndStickerCenter

    // Pick poly center if it's a 2x2x2x2, sticker center otherwise.
    // XXX not sure this is used any more? well it's used when rotating to center, but I'm not sure it should be... have to check
    public static float[] pickPolyOrStickerCenter(float x, float y,
                                                  Frame frame)
    {
        float polyAndStickerCenter[][] = pickPolyAndStickerAndFaceCenter(x, y, frame);
        if (polyAndStickerCenter == null)
            return null;
        float polyCenter[] = polyAndStickerCenter[0];
        float stickerCenter[] = polyAndStickerCenter[1];

        // XXX total hack-- use poly center if we think it's the 2x2x2x2 puzzle
        // XXX and the sticker center otherwise.
        //boolean itsProbablyThe2 = VecMath.normsqrd(stickerCenter) == 1.75
        //                     && (VecMath.normsqrd(polyCenter) == 1.5
        //                      || VecMath.normsqrd(polyCenter) == 2.5);
        boolean itsProbablyThe2 = frame.puzzleDescription.getStickerPoly2Grip() != null;
        if (verboseLevel >= 3) System.out.println("itsProbablyThe2 = "+itsProbablyThe2);

        return itsProbablyThe2 ? polyCenter : stickerCenter;
    } // pickPolyOrStickerCenter

    public static int pickGrip(float x, float y,
                               Frame frame)
    {
        if (verboseLevel >= 3) System.out.println("in GenericPipelineUtils.pickGrip");
        // Hmm, apparently for some puzzles, getStickerPoly2Grip()
        // returns non-null and so can be used to get this easily.
        // But, sometimes not, in which case we need to use other logic.
        // XXX should this other logic be placed inside getStickerPoly2Grip()
        // XXX so that it always returns non-null?
        if (frame.puzzleDescription.getStickerPoly2Grip() != null)
        {
            int stickerAndPoly[] = pick(x, y, frame);
            if (stickerAndPoly == null)
                return -1;
            return frame.puzzleDescription.getStickerPoly2Grip()[stickerAndPoly[0]][stickerAndPoly[1]];
        }

        // XXXTODO: get clear on when this is called.  I think it's when it's "non-generic" puzzle that's not further cut, in which case the naive logic is good enough
        //CHECK(false);

        float polyAndStickerAndFaceCenter[][] = pickPolyAndStickerAndFaceCenter(x, y, frame);
        if (polyAndStickerAndFaceCenter == null)
            return -1;

        float polyCenter[] = polyAndStickerAndFaceCenter[0];
        float stickerCenter[] = polyAndStickerAndFaceCenter[1];
        float faceCenter[] = polyAndStickerAndFaceCenter[2];
        if (frame.puzzleDescription.nDims() < 4)
        {

            if (verboseLevel >= 3) System.out.println("    polyCenter = "+com.donhatchsw.util.Arrays.toStringCompact(polyCenter));
            if (verboseLevel >= 3) System.out.println("    stickerCenter = "+com.donhatchsw.util.Arrays.toStringCompact(stickerCenter));
            if (verboseLevel >= 3) System.out.println("    faceCenter = "+com.donhatchsw.util.Arrays.toStringCompact(faceCenter));
            int iGrip = frame.puzzleDescription.getClosestGrip(faceCenter,
                                                               VecMath.vmv(polyCenter, stickerCenter));

            if (verboseLevel >= 3) System.out.println("out GenericPipelineUtils.pickGrip, returning "+iGrip);
            return iGrip;
        }
        else
        {
            int iGrip = frame.puzzleDescription.getClosestGrip(
                            faceCenter,
                            VecMath.vmv(stickerCenter, faceCenter));
            //System.out.println("    the closest grip to "+VecMath.toString(stickerCenter)+" is "+iGrip);
            return iGrip;
        }
    } // pickGrip

    public static float[] pickNicePointToRotateToCenter(float x, float y,
                                                        boolean allowArbitraryElements,
                                                        Frame frame)
    {
        float polyAndStickerAndFaceCenter[][] = pickPolyAndStickerAndFaceCenter(x, y, frame);
        if (polyAndStickerAndFaceCenter == null)
            return null;
        float polyCenter[] = polyAndStickerAndFaceCenter[0];
        float stickerCenter[] = polyAndStickerAndFaceCenter[1];
        float faceCenter[] = polyAndStickerAndFaceCenter[2];
        if (allowArbitraryElements)
            return frame.puzzleDescription.getClosestNicePointToRotateToCenter(stickerCenter);
        else
            return faceCenter;
    }


    // XXX figure out where to put this, if anywhere
    private static java.util.Random jitterGenerator = new java.util.Random();
    private static int jitterRadius = 0; // haha, for debugging, but cool effect, should publicize it

    // PAINT
    public static void paintFrame(Frame frame,
                                  int puzzleState[],

                                  boolean showShadows,
                                  Color ground,
                                  float faceRGB[][],
                                  int iStickerUnderMouse,
                                  int iPolyUnderMouse,
                                  boolean highlightByCubie,
                                  boolean highlightByGrip,
                                  Color nonShrunkFaceOutlineColor,
                                  Color shrunkFaceOutlineColor,
                                  Color nonShrunkStickerOutlineColor,
                                  boolean drawShrunkStickerSurfaces,
                                  Color shrunkStickerOutlineColor,
                                  Graphics g,
                                  
                                  int jitterRadius,
                                  boolean drawLabels,
                                  boolean showPartialOrder)
    {
        if (verboseLevel >= 2) System.out.println("    in GenericPipelineUtils.paintFrame");
        if (verboseLevel >= 2) System.out.println("        iStickerUnderMouse = "+iStickerUnderMouse);

        GenericPuzzleDescription puzzleDescription = frame.puzzleDescription;
        int drawList[][/*2*/] = frame.drawList;
        float brightnesses[][] = frame.brightnesses;
        int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][] = puzzleDescription.getStickerInds();
        int sticker2cubie[] = puzzleDescription.getSticker2Cubie();
        // Note, the range check protects against wild values of iStickerUnderMouse
        // (e.g. if left over from a previous larger puzzle).
        int iCubieUnderMouse = (iStickerUnderMouse < 0
                             || iStickerUnderMouse >= sticker2cubie.length) ? -1 : sticker2cubie[iStickerUnderMouse];
        int stickerPoly2Grip[][] = puzzleDescription.getStickerPoly2Grip();
        int iGripUnderMouse = (stickerPoly2Grip == null
                            || iStickerUnderMouse < 0
                            || iStickerUnderMouse >= stickerPoly2Grip.length
                            || iPolyUnderMouse < 0
                            || iPolyUnderMouse >= stickerPoly2Grip[iStickerUnderMouse].length) ? -1 : stickerPoly2Grip[iStickerUnderMouse][iPolyUnderMouse];
        if (stickerPoly2Grip == null)
            highlightByGrip = false;
        boolean highlightBySticker = !highlightByCubie && !highlightByGrip;
        if (iGripUnderMouse == -1)
            highlightByGrip = false; // otherwise the polys that don't map to grips will light up


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

            // XXX holy shit get this showPartialOrder crap out of here so it's
            // XXX possible to see the loop structure
            int predecessors[][] = null;
            float partialOrderNodeCenters2d[][] = null;
            if (showPartialOrder && frame.partialOrderInfo != null)
            {
                int nStickers = puzzleDescription.nStickers();
                int nNodes = nStickers;
                int partialOrderSize = frame.partialOrderInfo.length;
                predecessors = new int[nNodes][0];
                for (int i = 0; i < partialOrderSize; ++i) {
                    int from = frame.partialOrderInfo[i][0][0];
                    int to = frame.partialOrderInfo[i][1][0];
                    int fromGroupSize = frame.partialOrderInfo[i][0][2] - frame.partialOrderInfo[i][0][1];
                    int toGroupSize = frame.partialOrderInfo[i][1][2] - frame.partialOrderInfo[i][1][1];
                    //System.out.println("          fromGroupSize = "+fromGroupSize);
                    //System.out.println("          toGroupSize = "+toGroupSize);
                    boolean isSimple = (fromGroupSize==1 && toGroupSize==1);
                    //if (isSimple)   // can switch this to show one or the other or both.  TODO: show both differently
                    {
                        // XXX very inefficient, although expected number of predecessors per sticker isn't large.
                        predecessors[to] = com.donhatchsw.util.Arrays.append(predecessors[to], from);
                    }
                }

                partialOrderNodeCenters2d = new float[nNodes][2]; // zeros
                int nContributorsThisNode[] = new int[nNodes]; // zeros
                for (int iItem = 0; iItem < frame.drawListSize; ++iItem)
                {
                    int iSticker = drawList[iItem][0];
                    int iPolyThisSticker = drawList[iItem][1];
                    int poly[] = stickerInds[iSticker][iPolyThisSticker];
                    for (int i = 0; i < poly.length; ++i)
                    {
                        float vert[] = verts[poly[i]];
                        VecMath.vpv(partialOrderNodeCenters2d[iSticker], partialOrderNodeCenters2d[iSticker], vert);
                        nContributorsThisNode[iSticker]++;
                    }
                }
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    if (nContributorsThisNode[iSticker] != 0)
                        VecMath.vxs(partialOrderNodeCenters2d[iSticker], partialOrderNodeCenters2d[iSticker], 1.f/nContributorsThisNode[iSticker]);
                }

                // Okay, now we've figured out 2d centers
                // for all nodes that are stickers...
                // Now figure out some centers for nodes that
                // represent groups.
                // We'll draw those at the average sticker center
                // of each of their component stickers.
                // TODO: resurrect this, maybe?  the current code above didn't make any nodes for non-stickers.
                for (int iNode = nStickers; iNode < nNodes; ++iNode)
                {
                    for (int iPred = 0; iPred < predecessors[iNode].length; ++iPred)
                    {
                        int jSticker = predecessors[iNode][iPred];
                        CHECK(jSticker < nStickers);
                        if (VecMath.normsqrd(partialOrderNodeCenters2d[jSticker]) != 0.)
                        {
                            VecMath.vpv(partialOrderNodeCenters2d[iNode],
                                        partialOrderNodeCenters2d[iNode],
                                        partialOrderNodeCenters2d[jSticker]);
                            nContributorsThisNode[iNode]++;
                        }
                    }
                    if (nContributorsThisNode[iNode] != 0)
                        VecMath.vxs(partialOrderNodeCenters2d[iNode], partialOrderNodeCenters2d[iNode], 1.f/nContributorsThisNode[iNode]);
                }
                for (int iNode = nStickers; iNode < nNodes; ++iNode)
                {
                    if ((iNode-nStickers)%2 == 0)
                    {
                        // it's a start token... actually what we just did
                        // doesnt' make any sense... set it to the same
                        // as the corresponding end token instead.
                        VecMath.copyvec(partialOrderNodeCenters2d[iNode], partialOrderNodeCenters2d[iNode+1]);
                    }
                }
            }


            for (int iItem = 0; iItem < drawListSize; ++iItem)
            {
                int iSticker = drawList[iItem][0];
                int iPolyThisSticker = drawList[iItem][1];

                if (showPartialOrder && frame.partialOrderInfo != null
                        && (iItem==0 || drawList[iItem][0] != drawList[iItem-1][0]))
                {
                    int nStickers = puzzleDescription.nStickers();
                    // Find any partial order items
                    // otherSticker < thisSticker,
                    // and draw them now.
                    for (int iPred = 0; iPred < predecessors[iSticker].length; ++iPred)
                    {
                        int jSticker = predecessors[iSticker][iPred];

                        if (true)
                            if (jSticker >= nStickers
                             && (jSticker-nStickers)%2 != 1)
                                continue; // draw only to other-group end tokens

                        if (true)
                            if (jSticker >= nStickers
                             && (jSticker-nStickers)%2 != 0)
                                continue; // draw only to this-group start tokens

                        float otherStickerCenter[] = partialOrderNodeCenters2d[jSticker];
                        float myStickerCenter[] = partialOrderNodeCenters2d[iSticker];
                        if (jitterRadius > 0)
                        {
                            otherStickerCenter = VecMath.copyvec(otherStickerCenter);
                            myStickerCenter = VecMath.copyvec(myStickerCenter);
                            for (int i = 0; i < 2; ++i)
                            {
                                otherStickerCenter[i] += jitterGenerator.nextInt(2*jitterRadius+1)-jitterRadius;
                                myStickerCenter[i] += jitterGenerator.nextInt(2*jitterRadius+1)-jitterRadius;
                            }
                        }

                        java.awt.Color colors[][] = {
                            {
                                ground.darker(), // lighter than the rest of the shadows
                            },
                            {
                                java.awt.Color.red,
                                java.awt.Color.orange,
                                java.awt.Color.yellow,
                                java.awt.Color.green,
                                //java.awt.Color.cyan,
                                //java.awt.Color.blue,
                            }
                        };
                        int nSegs = colors[iPass].length;
                        float points[][] = new float[nSegs+1][];
                        for (int iPoint = 0; iPoint < points.length; ++iPoint)
                            points[iPoint] = VecMath.lerp(otherStickerCenter, myStickerCenter, (float)iPoint/(float)nSegs); // XXX move this division up
                        for (int iSeg = 0; iSeg < nSegs; ++iSeg)
                        {
                            g.setColor(colors[iPass][iSeg]);
                            g.drawLine((int)points[iSeg][0], (int)points[iSeg][1],
                                       (int)points[iSeg+1][0], (int)points[iSeg+1][1]);
                        }
                    }
                }

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
                if (drawShrunkStickerSurfaces) {
                    Color stickercolor = new Color(
                        brightness*faceRGBThisSticker[0],
                        brightness*faceRGBThisSticker[1],
                        brightness*faceRGBThisSticker[2]);
                    boolean highlight = false;
                    //System.out.println("iGripUnderMouse = "+iGripUnderMouse);
                    if (highlightByCubie && sticker2cubie[iSticker]==iCubieUnderMouse)
                        highlight = true;
                    else if (highlightByGrip && stickerPoly2Grip[iSticker][iPolyThisSticker]==iGripUnderMouse)
                        highlight = true;
                    else if (highlightBySticker && iSticker==iStickerUnderMouse)
                        highlight = true;
                    if(highlight)
                        stickercolor = stickercolor.brighter().brighter();

                    g.setColor(isShadows ? shadowcolor : stickercolor);
                    g.fillPolygon(xs, ys, poly.length);
                }

                if(!isShadows && shrunkStickerOutlineColor != null) {
                    g.setColor(shrunkStickerOutlineColor);
                    // uncomment the following line for an alternate outlining idea -MG
                    //g.setColor(new Color(faceRGBThisSticker[0], faceRGBThisSticker[1], faceRGBThisSticker[2]));
                    g.drawPolygon(xs, ys, poly.length);
                }
// uncomment something like the following for debugging specific interactions
//if (iSticker == 63 || iSticker == 219)
//if (iSticker == 16 || iSticker == 17 || iSticker == 0)
                if (drawLabels)
                {
                    String label = ""+iSticker+"("+iPolyThisSticker+")";
                    int x = 0;
                    int y = 0;
                    for (int i = 0; i < poly.length; ++i)
                    {
                        x += xs[i];
                        y += ys[i];
                    }
                    x /= poly.length;
                    y /= poly.length;
                    g.setColor(brightness > .5f ? Color.black : Color.white);
                    g.drawString(label, x, y);
                }
            }
        }

        if (verboseLevel >= 2) System.out.println("    out GenericPipelineUtils.paintFrame");
    } // paintFrame



    private static float tmpTWAf1[] = new float[2], tmpTWAf2[] = new float[2]; // scratch vars.  not thread safe!
    private static float twice_triangle_area(float v0[], float v1[], float v2[])
    {
        //float tmpTNf1[] = new float[2], tmpTNf2[] = new float[2];
        VecMath.vmv(2, tmpTWAf1, v1, v0);
        VecMath.vmv(2, tmpTWAf2, v2, v0);
        return VecMath.vxv2(tmpTWAf1, tmpTWAf2);
    }

} // class GenericPipelineUtils
