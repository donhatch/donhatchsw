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
        // 3: and on picks, and dump arrays at each step

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

        public int[][/*2*/] partialOrder; // optional, for debugging
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

                                    float faceShrink4d,
                                    float stickerShrink4d,
                                    float faceShrink3d,
                                    float stickerShrink3d,
                                    float stickersShrinkTowardsFaceBoundaries,

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
                                    float groundOffset,

                                    boolean useTopsort,
                                        boolean showPartialOrder)
    {
        if (verboseLevel >= 2) System.out.println("    in GenericPipelineUtils.computeFrame");

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
        float perStickerFaceCenters[][] = new float[nStickers][nDisplayDims]; // XXX MEM ALLOCATION

        //
        // Get the non-shrunk 4d verts and shrink-to points
        // from the puzzle description.
        //
        if (iGripOfTwist == -1)
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
                fracIntoTwist);
        }

        //
        // Shrink the vertices towards the shrink-to points in 4d.
        // And, shrink the sticker centers towards the face centers,
        // so they will be appropriate for subsequent shrink passes.
        //
        {
            float stickerShrinkPoints[][] = new float[nStickers][nDisplayDims]; // XXX mem allocation
            int sticker2face[] = puzzleDescription.getSticker2Face();

            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                VecMath.lerp(stickerShrinkPoints[iSticker],
                             stickerCenters[iSticker], stickerAltCenters[iSticker], stickersShrinkTowardsFaceBoundaries); // BEFORE shrinking towards face center
                // shrink the sticker shrink-to points towards the face center,
                // for the 3d shrink pass later
                VecMath.lerp(stickerCenters[iSticker],
                             perStickerFaceCenters[iSticker], stickerCenters[iSticker], faceShrink4d);
                VecMath.lerp(stickerAltCenters[iSticker],
                             perStickerFaceCenters[iSticker], stickerAltCenters[iSticker], faceShrink4d);
            }

            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                int iSticker = vert2sticker[iVert];
                VecMath.lerp(verts[iVert],
                             stickerShrinkPoints[iSticker], verts[iVert], stickerShrink4d);
                VecMath.lerp(verts[iVert],
                             perStickerFaceCenters[iSticker], verts[iVert], faceShrink4d);
            }
        }
        
        //
        // Rotate/scale in 4d.
        // Not just the verts, but also the shrink-to points,
        // since we'll need to shrink towards them again
        // for the 3d part of the shrink.
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
                if (!doFrontCellCull || volume < 0.f) // only draw *back* cells; cull front ones
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
        if (verboseLevel >= 3) System.out.println("        after front-cell cull: drawList = "+com.donhatchsw.util.Arrays.toStringCompact(com.donhatchsw.util.Arrays.subarray(drawList,0,drawListSize)));

        //
        // 3d face shrink and sticker shrink
        // XXX could try to only do this on vertices that passed the culls
        // XXX need to do this with the xformed and projected shrink-to point,
        // XXX not calculate screwy centers on the fly here.
        // XXX Q: should the sticker shrink-to point always be shrunk towards the face shrink-to point in 4d?  Or does 3d make sense?  Well, 4d would be more robust, since that shrinking could prevent having to do with projected original points that could end up behind the eye.
        //
        if (faceShrink3d != 1.f
         || stickerShrink3d != 1.f)
        {
            int sticker2face[] = puzzleDescription.getSticker2Face();
            int nFaces = puzzleDescription.nFaces();

            if (stickerShrink3d != 1.f)
            {
                float stickerCenters3d[][] = new float[stickerInds.length][3]; // XXX MEM ALLOCATION -- XXX get rid of this, use the projected centers instead
                int nStickerCenterContributors[] = new int[stickerInds.length]; // XXX MEM ALLOCATION
                for (int iVert = 0; iVert < verts.length; ++iVert)
                {
                    int iSticker = vert2sticker[iVert];
                    VecMath.vpv(stickerCenters3d[iSticker], stickerCenters3d[iSticker], verts[iVert]);
                    nStickerCenterContributors[iSticker]++;
                }
                for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
                    VecMath.vxs(stickerCenters3d[iSticker], stickerCenters3d[iSticker], 1.f/nStickerCenterContributors[iSticker]);
                for (int iVert = 0; iVert < verts.length; ++iVert)
                    VecMath.lerp(verts[iVert], stickerCenters3d[vert2sticker[iVert]], verts[iVert], stickerShrink3d);
            }
            if (faceShrink3d != 1.f)
            {
                float faceCenters[][] = new float[nFaces][3]; // XXX MEM ALLOCATION
                int nFaceCenterContributors[] = new int[nFaces]; // XXX MEM ALLOCATION

                for (int iVert = 0; iVert < verts.length; ++iVert)
                {
                    int iFace = sticker2face[vert2sticker[iVert]];
                    VecMath.vpv(faceCenters[iFace], faceCenters[iFace], verts[iVert]);
                    nFaceCenterContributors[iFace]++;
                }
                for (int iFace = 0; iFace < nFaces; ++iFace)
                    VecMath.vxs(faceCenters[iFace], faceCenters[iFace], 1.f/nFaceCenterContributors[iFace]);

                for (int iVert = 0; iVert < verts.length; ++iVert)
                    VecMath.lerp(verts[iVert], faceCenters[sticker2face[vert2sticker[iVert]]], verts[iVert], faceShrink3d);
            }
        }

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
        //if (verboseLevel >= 3) System.out.println("        after 3d clip: verts = "+com.donhatchsw.util.Arrays.toStringCompact(verts));

        //
        // Compute and save the polygon normals in 3d.
        // These will be needed later for the topsort.
        // XXX since we're doing this anyway, could do the backface culling here too
        // XXX instead of later on the 2d polygons
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
        if (verboseLevel >= 2) if (shadowVerts != null) System.out.println("        after 3d->3d project: shadowVerts[0] = "+com.donhatchsw.util.Arrays.toStringCompact(shadowVerts[0]));

        boolean stickerPolyIsStrictlyBackfacing[][] = new boolean[nStickers][];
        for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            stickerPolyIsStrictlyBackfacing[iSticker] = new boolean[stickerInds[iSticker].length];

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
            drawListSize = nFrontFacing;

            if (groundNormal != null)
            {
                // Put the culled ones back at the end, for shadows later
                for (int i = 0; i < nBackfacing; ++i)
                    drawList[drawListSize+i] = shadowExtraDrawList[i];
                shadowDrawListSize = nFrontFacing+nBackfacing;
            }
        }
        if (verboseLevel >= 3) System.out.println("        after back-face cull: drawList = "+com.donhatchsw.util.Arrays.toStringCompact(com.donhatchsw.util.Arrays.subarray(drawList,0,drawListSize)));

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
                Assert(nVertsThisSticker != 0);
                stickerCentersZ[iSticker] = sum / nVertsThisSticker;
            }

            int stickerSortOrder[] = new int[nStickers]; // XXX allocation
            int partialOrderAddress[][][] = (showPartialOrder ? new int[1][][] : null);
            int nSortedStickers = VeryCleverPaintersSortingOfStickers.sortStickersBackToFront(
                    nStickersToSort,
                    puzzleDescription.getAdjacentStickerPairs(),
                    stickerVisibilities,
                    stickerPolyIsStrictlyBackfacing,
                    VecMath.mxv(rot4d, new float[]{0,0,0,eyeW}), // in opposite order so we multiply the eye by the *inverse* of the matrix, to get it into object space  XXX put this elsewhere
                    cutNormal,
                    cutOffsets,
                    sticker2Slice,
                    stickerSortOrder,
                    partialOrderAddress,
                    stickerCentersZ,
                    polyCenters3d,
                    polyNormals3d);

            if (showPartialOrder)
                frame.partialOrder = partialOrderAddress[0];

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
                com.donhatchsw.util.SortStuff.sort(drawList, 0, drawListSize, new com.donhatchsw.util.SortStuff.Comparator() { // XXX ALLOCATION! (need to make sort smarter)
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
        }

        if (verboseLevel >= 3) System.out.println("        after z-sort: stickerInds = "+com.donhatchsw.util.Arrays.toStringCompact(stickerInds));

        frame.drawListSize = drawListSize;
        frame.shadowDrawListSize = groundNormal!=null ? shadowDrawListSize : 0;

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
                             Frame frame,
                             GenericPuzzleDescription puzzleDescription)
    {
        if (verboseLevel >= 3) System.out.println("    in GenericPipelineUtils.pick");
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
        if (verboseLevel >= 3) System.out.println("    out GenericPipelineUtils.pick, returning "+(pickedItem==null?"null":("{iSticker="+pickedItem[0]+",iPolyWithinSticker="+pickedItem[1]+"}")));
        return pickedItem;
    }

    public static int pickSticker(float x, float y,
                                  Frame frame,
                                  GenericPuzzleDescription puzzleDescription)
    {
        int iStickerAndPoly[] = pick(x, y, frame, puzzleDescription);
        return iStickerAndPoly != null ? iStickerAndPoly[0] : -1;
    }

    public static float[][] pickPolyAndStickerAndFaceCenter(float x, float y,
                                                     Frame frame,
                                                     GenericPuzzleDescription puzzleDescription)
    {
        int hit[] = pick(x, y, frame, puzzleDescription);
        if (hit == null)
            return null;
        // XXX would really like to map the pick point back to 4d...
        // XXX for now, map the polygon center back.

        // XXX argh, this is sure overkill here...
        float verts[][] = new float[puzzleDescription.nVerts()][puzzleDescription.nDisplayDims()];
        puzzleDescription.computeVertsAndShrinkToPointsAtRest(
                verts,
                null, // sticker centers
                null, // alt sticker centers,
                null); // per sticker face centers

        int stickerInds[][][] = puzzleDescription.getStickerInds();
        // XXX not sure which of the following are better if either-- maybe poly for 2x, sticker otherwise? it's definitely disconcerting when different parts of the sticker do diff things...
        int sticker[][] = stickerInds[hit[0]];
        int poly[] = sticker[hit[1]];
        float polyCenter[] = VecMath.averageIndexed(poly, verts);
        float stickerCenter[] = VecMath.averageIndexed(sticker, verts);
        float faceCenter[] = puzzleDescription.getFaceCentersAtRest()[puzzleDescription.getSticker2Face()[hit[0]]];

        //System.out.println("        poly center = "+VecMath.toString(polyCenter));
        //System.out.println("        sticker center = "+VecMath.toString(stickerCenter));
        return new float[][]{polyCenter, stickerCenter, faceCenter};
    } // pickPolyAndStickerCenter

    // Pick poly center if it's a 2x2x2x2, sticker center otherwise.
    // XXX not sure this is used any more? well it's used when rotating to center, but I'm not sure it should be... have to check
    public static float[] pickPolyOrStickerCenter(float x, float y,
                                                  Frame frame,
                                                  GenericPuzzleDescription puzzleDescription)
    {
        float polyAndStickerCenter[][] = pickPolyAndStickerAndFaceCenter(x, y, frame, puzzleDescription);
        if (polyAndStickerCenter == null)
            return null;
        float polyCenter[] = polyAndStickerCenter[0];
        float stickerCenter[] = polyAndStickerCenter[1];

        // XXX total hack-- use poly center if we think it's the 2x2x2x2 puzzle
        // XXX and the sticker center otherwise.
        //boolean itsProbablyThe2 = VecMath.normsqrd(stickerCenter) == 1.75
        //                     && (VecMath.normsqrd(polyCenter) == 1.5
        //                      || VecMath.normsqrd(polyCenter) == 2.5);
        boolean itsProbablyThe2 = puzzleDescription.getStickerPoly2Grip() != null;
        if (verboseLevel >= 3) System.out.println("itsProbablyThe2 = "+itsProbablyThe2);

        return itsProbablyThe2 ? polyCenter : stickerCenter;
    } // pickPolyOrStickerCenter

    public static int pickGrip(float x, float y,
                               Frame frame,
                               GenericPuzzleDescription puzzleDescription)
    {
        if (puzzleDescription.getStickerPoly2Grip() != null)
        {
            int stickerAndPoly[] = pick(x, y, frame, puzzleDescription);
            if (stickerAndPoly == null)
                return -1;
            return puzzleDescription.getStickerPoly2Grip()[stickerAndPoly[0]][stickerAndPoly[1]];
        }

        float polyAndStickerAndFaceCenter[][] = pickPolyAndStickerAndFaceCenter(x, y, frame, puzzleDescription);
        if (polyAndStickerAndFaceCenter == null)
            return -1;

        float polyCenter[] = polyAndStickerAndFaceCenter[0];
        float stickerCenter[] = polyAndStickerAndFaceCenter[1];
        float faceCenter[] = polyAndStickerAndFaceCenter[2];
        if (puzzleDescription.nDims() < 4)
        {
            int iGrip = puzzleDescription.getClosestGrip(VecMath.vmv(polyCenter, stickerCenter),
                                                         faceCenter);
            return iGrip;
        }
        else
        {
            int iGrip = puzzleDescription.getClosestGrip(stickerCenter);
            //System.out.println("    the closest grip to "+VecMath.toString(stickerCenter)+" is "+iGrip);
            return iGrip;
        }
    } // pickGrip

    public static float[] pickNicePointToRotateToCenter(float x, float y,
                                                        boolean allowArbitraryElements,
                                                        Frame frame,
                                                        GenericPuzzleDescription puzzleDescription)
    {
        float polyAndStickerAndFaceCenter[][] = pickPolyAndStickerAndFaceCenter(x, y, frame, puzzleDescription);
        if (polyAndStickerAndFaceCenter == null)
            return null;
        float polyCenter[] = polyAndStickerAndFaceCenter[0];
        float stickerCenter[] = polyAndStickerAndFaceCenter[1];
        float faceCenter[] = polyAndStickerAndFaceCenter[2];
        if (allowArbitraryElements)
            return puzzleDescription.getClosestNicePointToRotateToCenter(stickerCenter);
        else
            return faceCenter;
    }


    // XXX figure out where to put this, if anywhere
    private static java.util.Random jitterGenerator = new java.util.Random();
    private static int jitterRadius = 0; // haha, for debugging, but cool effect, should publicize it

    // PAINT
    public static void paintFrame(Frame frame,
                                  GenericPuzzleDescription puzzleDescription,
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
                                  Color shrunkStickerOutlineColor,
                                  Graphics g,
                                  
                                  int jitterRadius,
                                  boolean drawLabels,
                                  boolean showPartialOrder)
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
            if (showPartialOrder)
            {
                int partialOrderSize = frame.partialOrder.length;
                int nStickers = puzzleDescription.nStickers();
                int nNodes = VecMath.max((int[])com.donhatchsw.util.Arrays.flatten(frame.partialOrder, 0, 2))+1;
                nNodes = Math.max(nNodes,puzzleDescription.nStickers());

                predecessors = new int[nNodes][0];
                for (int i = 0; i < partialOrderSize; ++i)
                {
                    predecessors[frame.partialOrder[i][1]] = (int[])com.donhatchsw.util.Arrays.append(predecessors[frame.partialOrder[i][1]], frame.partialOrder[i][0]);
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
                for (int iNode = nStickers; iNode < nNodes; ++iNode)
                {
                    for (int iPred = 0; iPred < predecessors[iNode].length; ++iPred)
                    {
                        int jSticker = predecessors[iNode][iPred];
                        Assert(jSticker < nStickers);
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

                if (showPartialOrder
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
                                continue; // only draw to other-group end tokens

                        if (true)
                            if (jSticker >= nStickers
                             && (jSticker-nStickers)%2 != 0)
                                continue; // only draw to this-group start tokens

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
                if(!isShadows && shrunkStickerOutlineColor != null) {
                    g.setColor(shrunkStickerOutlineColor);
                    // uncomment the following line for an alternate outlining idea -MG
                    // g.setColor(new Color(faceRGB[cs][0], faceRGB[cs][1], faceRGB[cs][2]));
                    g.drawPolygon(xs, ys, poly.length);
                }
//if (iSticker == 63 || iSticker == 219) // XXX get rid
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




    private static class VeryCleverPaintersSortingOfStickers
    {
        // Function return value is number of stickers to draw
        public static int sortStickersBackToFront(
                final int nStickers, // can be less than actual number, for debugging
                int adjacentStickerPairs[][/*2*/][/*2*/],
                final boolean stickerVisibilities[/*nStickers*/],
                boolean stickerPolyIsStrictlyBackfacing[/*nStickers*/][/*nPolysThisSticker*/],
                float eye[],
                float cutNormal[],
                float cutOffsets[], // in increasing order
                int sticker2Slice[],
                int returnStickerSortOrder[/*nStickers*/],
                int returnPartialOrderOptionalForDebugging[][][], // null if caller doesn't care, otherwise it's a singleton array that gets filled in with the partial order
                final float stickerCentersZ[],
                float polyCenters3d[][][],
                float polyNormals3d[][][])
        {
            int nSlices = cutOffsets.length + 1;
            int nCompressedSlices = nSlices; // XXX should combine adjacent slices that are moving together... but maybe it doesn't hurt to just pretend all the slices are twisting separately, it keeps things simple?  Not sure.
            int nNodes = nStickers + 2*nCompressedSlices;
            int parents[] = new int[nNodes];
            int depths[] = new int[nNodes]; // XXX this array is not really necessary, but it simplifies the code

            int maxPartialOrderSize = nStickers*2 // for group inclusion
                                    + adjacentStickerPairs.length;
            int partialOrder[][] = new int[maxPartialOrderSize][2];
            int partialOrderSize = 0;

            // Initialize parents and depths...
            {
                //
                // Figure out which compressed slice the eye is in.
                // That slice will be the root of a simple tree,
                // with two branches of groups:
                //                whichSliceEyeIsIn
                //                 /             \
                //        nextShallowerSlice   nextDeeperSlice
                //            |                  ...
                //   nextNextShallowerSlice 
                //           ...
                //
                int eyeSlice; // which compressed slice eye is in
                {
                    float eyeOffset = VecMath.dot(eye, cutNormal);
                    eyeSlice = 0;
                    while (eyeSlice < cutOffsets.length
                        && eyeOffset > cutOffsets[eyeSlice])
                        eyeSlice++;
                }

                for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice)
                {
                    int iNode = nStickers + 2*iSlice;
                    if (iSlice < eyeSlice)
                    {
                        parents[iNode] = nStickers + 2*(iSlice+1);
                        depths[iNode] = eyeSlice - iSlice;
                    }
                    else if (iSlice > eyeSlice)
                    {
                        parents[iNode] = nStickers + 2*(iSlice-1);
                        depths[iNode] = iSlice - eyeSlice;
                    }
                    else
                    {
                        parents[iNode] = -1;
                        depths[iNode] = 0;
                    }
                    // The node position nSticker+2*iSlice+1 is not used;
                    // it is just an end token for the group,
                    // so that when we need a sticker to be > an entire group,
                    // we only have to specify one inequality instead
                    // of one for each element of the group.
                }

                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    int iSlice = sticker2Slice[iSticker];
                    int iGroup = nStickers + 2*iSlice;
                    int iGroupStartToken = nStickers + 2*iSlice;
                    int iGroupEndToken = nStickers + 2*iSlice+1;

                    parents[iSticker] = iGroup;
                    depths[iSticker] = depths[parents[iSticker]] + 1;

                    if (stickerVisibilities[iSticker])
                    {
                        // add "iGroupStartToken < iSticker"
                        partialOrder[partialOrderSize][0] = iGroupStartToken;
                        partialOrder[partialOrderSize][1] = iSticker;
                        partialOrderSize++;
                        // add "iSticker < iGroupEndToken"
                        partialOrder[partialOrderSize][0] = iSticker;
                        partialOrder[partialOrderSize][1] = iGroupEndToken;
                        partialOrderSize++;
                    }
                }
            }

            {
                float jPolyCenterMinusIPolyCenter[] = new float[3]; // scratch
                for (int iPoly = 0; iPoly < adjacentStickerPairs.length; ++iPoly)
                {
                    int stickersThisPoly[][] = adjacentStickerPairs[iPoly];
                    int iSticker =         stickersThisPoly[0][0];
                    int iPolyThisSticker = stickersThisPoly[0][1];
                    int jSticker =         stickersThisPoly[1][0];
                    int jPolyThisSticker = stickersThisPoly[1][1];
                    if (iSticker >= nStickers
                     || jSticker >= nStickers)
                        continue; // caller probably set nStickers < actaul number of stickers for debugging

                    if (jSticker < iSticker)
                        continue; // already did it
                    boolean iStickerIsVisible = stickerVisibilities[iSticker];
                    boolean jStickerIsVisible = stickerVisibilities[jSticker];
                    if (!iStickerIsVisible && !jStickerIsVisible)
                        continue;
                    int iGroup = iSticker;
                    int jGroup = jSticker;
                    // Walk upwards until iGroup,jGroup are siblings...
                    {
                        while (depths[iGroup] > depths[jGroup])
                            iGroup = parents[iGroup];
                        while (depths[jGroup] > depths[iGroup])
                            jGroup = parents[jGroup];
                        while (parents[iGroup] != parents[jGroup])
                        {
                            iGroup = parents[iGroup];
                            jGroup = parents[jGroup];
                        }
                    }

                    //
                    // See whether things are so inside out
                    // that the polygons are facing away from each other...
                    // If so, then this polygon should not restrict anything.
                    //
                    if (true)
                    {
                        if (iStickerIsVisible && jStickerIsVisible
                         && parents[iSticker] == parents[jSticker]) // XXX floundering
                        {
                            VecMath.vmv(jPolyCenterMinusIPolyCenter,
                                        polyCenters3d[jSticker][jPolyThisSticker],
                                        polyCenters3d[iSticker][iPolyThisSticker]);
                            // we add a tiny bit of slop to make sure we consider
                            // the adjacency valid if the faces are coincident
                            if (VecMath.dot(polyNormals3d[iSticker][iPolyThisSticker], jPolyCenterMinusIPolyCenter) < -1e-3
                             || VecMath.dot(polyNormals3d[jSticker][jPolyThisSticker], jPolyCenterMinusIPolyCenter) > 1e-3)
                            {
                                if (false)
                                {
                                    System.out.println("HA!  I don't CARE because it's SO WARPED! stickers "+iSticker+"("+iPolyThisSticker+") "+jSticker+"("+jPolyThisSticker+")");
                                    System.out.println("    inormal = "+com.donhatchsw.util.Arrays.toStringCompact(polyNormals3d[iSticker][iPolyThisSticker]));
                                    System.out.println("    jnormal = "+com.donhatchsw.util.Arrays.toStringCompact(polyNormals3d[jSticker][jPolyThisSticker]));
                                    System.out.println("    j-i = "+com.donhatchsw.util.Arrays.toStringCompact(jPolyCenterMinusIPolyCenter));
                                    System.out.println("    inormal dot j-i = "+VecMath.dot(polyNormals3d[iSticker][iPolyThisSticker], jPolyCenterMinusIPolyCenter));
                                    System.out.println("    jnormal dot j-i = "+VecMath.dot(polyNormals3d[jSticker][jPolyThisSticker], jPolyCenterMinusIPolyCenter));
                                }

                                continue;
                            }
                        }
                    }


                    if (iGroup == iSticker && jGroup == jSticker)
                    {
                        // The two stickers are immediate siblings,
                        // i.e. both in the same (compressed) slice.
                        // This relationship only matters if they are both visible.
                        if (!iStickerIsVisible || !jStickerIsVisible)
                            continue;
                        //System.out.println("    stickers "+iSticker+","+jSticker+" in same slice "+sticker2Slice[iSticker]+"");
                        boolean iStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                        boolean jStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                        if (iStickerHasPolyBackfacing && jStickerHasPolyBackfacing)
                        {
                            // XXX ARGH!  This wouldn't happen if we were
                            // XXX projecting the original non-shrunken
                            // XXX hyperfaces, but unfortunately we shrink them
                            // XXX so it makes the two polygons not parallel
                            // XXX in 3-space.  I *think* if both are backfacing
                            // XXX it should mean either draw order is okay though.

                            // System.out.println("WARNING: sticker "+iSticker+"("+iPolyThisSticker+") and "+jSticker+"("+jPolyThisSticker+") both have poly backfacing!!");
                            continue;
                        }
                        else
                        {
                            //System.out.println("phew.");
                        }

                        if (iStickerHasPolyBackfacing)
                        {
                            //add "jSticker < iSticker"
                            partialOrder[partialOrderSize][0] = jSticker;
                            partialOrder[partialOrderSize][1] = iSticker;
                            partialOrderSize++;
                        }
                        else if (jStickerHasPolyBackfacing)
                        {
                            //add "iSticker < jSticker"
                            partialOrder[partialOrderSize][0] = iSticker;
                            partialOrder[partialOrderSize][1] = jSticker;
                            partialOrderSize++;
                        }
                    }
                    else if (iGroup == iSticker) // && jGroup != jSticker
                    {
                        // iSticker is adjacent to a group containing jSticker.
                        // The group is twisted, so jSticker's
                        // orientation of the poly is not relevant
                        // (in fact jSticker might not even be visible);
                        // only iSticker's orientation of the poly is relevant.
                        // XXX I thought that was true but then I got cycles in the 2x hypercube... think about this
                        if (!iStickerIsVisible)
                            continue;
                        //System.out.println("    sticker "+iSticker+" is adjacent to sticker "+jSticker+"'s slice "+sticker2Slice[jSticker]+"");
                        boolean iStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                        if (iStickerHasPolyBackfacing)
                        {
                            int jIndGroupEndToken = jGroup+1;
                            //add "jIndGroupEndToken < iSticker";
                            partialOrder[partialOrderSize][0] = jIndGroupEndToken;
                            partialOrder[partialOrderSize][1] = iSticker;
                            partialOrderSize++;
                        }
                        else
                        {
                            int jIndGroupStartToken = jGroup;
                            //add "iSticker < jIndGroupStartToken;
                            partialOrder[partialOrderSize][0] = iSticker;
                            partialOrder[partialOrderSize][1] = jIndGroupStartToken;
                            partialOrderSize++;
                        }
                    }
                    else if (jGroup == jSticker) // && iGroup != iSticker
                    {
                        // same as previous case but reversed
                        if (!jStickerIsVisible)
                            continue;
                        //System.out.println("    sticker "+iSticker+"'s slice "+sticker2Slice[iSticker]+" is adjacent to sticker "+jSticker+"");
                        boolean jStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                        if (jStickerHasPolyBackfacing)
                        {
                            int iIndGroupEndToken = iGroup+1;
                            //add "iIndGroupEndToken < jSticker";
                            partialOrder[partialOrderSize][0] = iIndGroupEndToken;
                            partialOrder[partialOrderSize][1] = jSticker;
                            partialOrderSize++;
                        }
                        else
                        {
                            int iIndGroupStartToken = iGroup;
                            //add "jSticker < iIndGroupStartToken;
                            partialOrder[partialOrderSize][0] = jSticker;
                            partialOrder[partialOrderSize][1] = iIndGroupStartToken;
                            partialOrderSize++;
                        }
                    }
                    else
                    {
                        // This would mean the two stickers are adjacent
                        // but the two different groups they are in are not.
                        // This can't happen.
                        Assert(false);
                    }
                }
            }

            //System.out.println("partial order size = "+partialOrderSize);
            //System.out.println("partial order = "+com.donhatchsw.util.Arrays.toStringCompact(com.donhatchsw.util.Arrays.subarray(partialOrder, 0, partialOrderSize)));

            // can turn this on to get a sense of
            // what it cares about and what it doesn't, if it's screwing up
            boolean doScramble = false;

            //
            // Okay, now we have the partial order.
            // Topsort it into a total order.
            //
            com.donhatchsw.util.TopSorter topsorter = new com.donhatchsw.util.TopSorter(nNodes, maxPartialOrderSize); // XXX allocation
            int nodeSortOrder[] = new int[nNodes]; // XXX allocation
            //System.out.println("nStickers = "+nStickers);
            //System.out.println("nNodes = "+nNodes);
            int componentStarts[] = new int[nNodes+1]; // one extra for end of last one
            int nComponents = doScramble ? topsorter.topsortRandomized(nNodes, nodeSortOrder,
                                                             partialOrderSize, partialOrder,
                                                             componentStarts,
                                                             jitterGenerator)
                                         : topsorter.topsort(nNodes, nodeSortOrder,
                                                             partialOrderSize, partialOrder,
                                                             componentStarts);
            int cycleVerboseLevel = 0;
            if (nComponents == nNodes)
            {
                if (cycleVerboseLevel >= 2) System.out.println("  no cycles!");
            }
            else
            {
                int nNontrivialComponents = 0;
                for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                {
                    int componentSize = componentStarts[iComponent+1]
                                      - componentStarts[iComponent];
                    if (componentSize >= 2)
                        nNontrivialComponents++;
                }
                if (cycleVerboseLevel >= 1) System.out.println("  ARGH! "+nNontrivialComponents+" cycle"+(nNontrivialComponents==1 ? "" : "s")+" in topological depth sort!");

                for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                {
                    int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                    if (componentSize >= 2)
                    {
                        if (cycleVerboseLevel >= 1) System.out.println("    there's a cycle of length "+componentSize+"");
                        //
                        // z-sort within the strongly connected component
                        //
                        if (true)
                        {
                            com.donhatchsw.util.SortStuff.sort(nodeSortOrder, componentStarts[iComponent], componentSize,
                                new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                                    public int compare(int i, int j)
                                    {
                                        // will be too big if it's a group begin or end token;
                                        // just stick those at the end for now, they
                                        // will get deleted soon
                                        boolean iTooBig = i >= nStickers;
                                        boolean jTooBig = j >= nStickers;
                                        if (iTooBig)
                                            if (jTooBig)
                                                return 0;
                                            else
                                                return 1;
                                        else
                                            if (jTooBig)
                                                return -1;

                                        //System.out.println("stickerVisibilities[i="+i+"] = "+stickerVisibilities[i]);
                                        //System.out.println("stickerVisibilities[j="+j+"] = "+stickerVisibilities[j]);
                                        Assert(stickerVisibilities[i]);
                                        Assert(stickerVisibilities[j]);

                                        float iZ = stickerCentersZ[i];
                                        float jZ = stickerCentersZ[j];
                                        // sort from increasing z to decreasing! that is because the z's got negated just before the projection!
                                        return iZ > jZ ? -1 :
                                               iZ < jZ ? 1 : 0;
                                    }
                            });
                        }
                    }
                }

                // The only further use of partialOrder is for rendering it (if in debug mode).
                // Prune out everything except one cycle from each component.
                if (returnPartialOrderOptionalForDebugging != null)
                {
                    int origToSorted[] = VecMath.invertperm(nodeSortOrder, nNodes);
                    int successors[][] = new int[nNodes][0];
                    for (int iPair = 0; iPair < partialOrder.length; ++iPair)
                    {
                        int i = partialOrder[iPair][0];
                        int j = partialOrder[iPair][1];
                        successors[i] = (int[])com.donhatchsw.util.Arrays.append(successors[i], j); // potentially O(n^2) but number of successors is usually at most 3 so whatever
                    }
                    int justTheCycles[][] = new int[partialOrderSize][];
                    int justTheCyclesSize = 0;
                    for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                    {
                        int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                        if (componentSize >= 2)
                        {
                            // Okay to be verbose since they are debugging
                            System.out.println("    found a cycle (well at least a connected component) of length "+componentSize+"");

                            int iNode0 = nodeSortOrder[componentStarts[iComponent]];
                            // Progress forward componentSize times,
                            // to make sure we are within where it's going to loop
                            for (int i = 0; i < componentSize; ++i)
                            {
                                int jNode = -1;
                                for (int iSucc = 0; iSucc < successors[iNode0].length; ++iSucc)
                                    if (origToSorted[successors[iNode0][iSucc]] >= componentStarts[iComponent]
                                     && origToSorted[successors[iNode0][iSucc]] < componentStarts[iComponent+1])
                                    {
                                        jNode = successors[iNode0][iSucc];
                                        break;
                                    }
                                Assert(jNode != -1);
                                iNode0 = jNode;
                            }
                            System.out.print("        "+iNode0+"");
                            for (int iNode = iNode0;;)
                            {
                                int jNode = -1;
                                for (int iSucc = 0; iSucc < successors[iNode].length; ++iSucc)
                                    if (origToSorted[successors[iNode][iSucc]] >= componentStarts[iComponent]
                                     && origToSorted[successors[iNode][iSucc]] < componentStarts[iComponent+1])
                                    {
                                        jNode = successors[iNode][iSucc];
                                        break;
                                    }
                                Assert(jNode != -1);
                                justTheCycles[justTheCyclesSize++] = new int[]{iNode,jNode};
                                System.out.print(" -> "+jNode+"");

                                iNode = jNode;
                                if (jNode == iNode0)
                                    break;
                            }
                            System.out.println();
                        }
                    }
                    partialOrder = justTheCycles;
                    partialOrderSize = justTheCyclesSize;
                    //System.out.println("justTheCyclesSize = "+justTheCyclesSize);
                    //System.out.println("justTheCycles = "+com.donhatchsw.util.Arrays.toStringCompact(justTheCycles));
                }
            }



            //
            // Compress out the group start and end tokens
            // which we no longer care about
            //
            int nCompressedSorted = 0;
            for (int iSorted = 0; iSorted < nNodes; ++iSorted)
            {
                int iNode = nodeSortOrder[iSorted];
                if (iNode < nStickers)
                {
                    int iSticker = iNode;
                    if (stickerVisibilities[iSticker]) // XXX hmm, this is checked in the caller too... maybe we don't need to check it here and in fact we don't need to return a count
                        returnStickerSortOrder[nCompressedSorted++] = iSticker;
                }
            }

            if (returnPartialOrderOptionalForDebugging != null)
            {
                returnPartialOrderOptionalForDebugging[0] = (int[][])com.donhatchsw.util.Arrays.subarray(partialOrder, 0, partialOrderSize);
            }

            return nCompressedSorted;
        } // sortStickersBackToFront
    } // class VeryCleverPaintersSortingOfStickers


    private static float tmpTWAf1[] = new float[2], tmpTWAf2[] = new float[2]; // scratch vars
    private static float twice_triangle_area(float v0[], float v1[], float v2[])
    {
        //float tmpTNf1[] = new float[2], tmpTNf2[] = new float[2];
        VecMath.vmv(2, tmpTWAf1, v1, v0);
        VecMath.vmv(2, tmpTWAf2, v2, v0);
        return VecMath.vxv2(tmpTWAf1, tmpTWAf2);
    }

} // class GenericPipelineUtils
