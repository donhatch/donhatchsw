/*
    RELNOTES:
    =========
        This version has the following enhancements:
            - speed of twists and rotations
               have been adjusted to feel more uniform for different angles
               (small angles are slower and large angles are faster
               than before, so that the acceleration is the same
               in all types of moves)
            - "Requre Ctrl Key to Spin Drag" preference
            - "Restrict Roll" preference
               (only works for generic puzzles currently)
            - you can 4d-rotate any element
               (vertex, edge, 2d face, hyperface) of the puzzle
               to the center with the middle mouse, not just hyperface centers
               (only works for generic puzzles currently)
            - Lots of new puzzle types available from the Puzzle menu.
               These are called "generic puzzles" and they are a work
               in progress.
        Generic puzzles have the following limitations currently:
            - no save/load (menus are probably misleading)
            - no macros (menus are probably misleading)
            - can't twist (or undo or redo)
               while a twist or cheat is in progress
               (which means you can't double-click to do a move twice)
            - only 8 colors still, even when more than 8 faces
            - some of the even-length puzzles have spurious extra
              very thin stickers at the halfway planes
            - sometimes the highlighted sticker fails to get updated correctly
              at the end of a twist (jiggle the mouse to fix it)
            - no real solve
            - scramble only affects outer or 2nd slices (you'll
              only notice this if your puzzle length is >= 6)
            - contiguous cubies not implemented (even if gui says otherwise)
            - The frame display routines are not optimized for memory use,
              which means they place a heavy load on the garbage collector.
              This can cause short but noticeable pauses during
              twisting or rotating.  This will be fixed in a future release.
            - exceptions everywhere if you try to do unimplemented stuff
        And the following enhancements:

    ISSUES:
    =======
        - Contiguous cubies.  I would like to do the following:
            1. Get rid of the "Contiguous Cubies" checkbox;
               there will be no magical half-broken
               slider-following-other-slider state any more.
            2. Replace it with either:
                  a) a selector:
                   Stickers Shrink Towards: [Sticker Centers / Face Boundaries]
               or b) a "Stickers shrink towards face boundaries" checkbox
               Then Contiguous Cubies can be obtained
               by turning on "Stickers shrink towards face boundaries"
               and setting "Face Shrink" to 1, and sliding stickerShrink
               up and down.
            3. (Optional): There could be a Button (NOT a checkbox)
               called Contiguous Cubies that:
                   turns on "Stickers shrink towards face boundaries" if
                   not already on, and sets "Face Shrink" to 1.
               I think we can do without it though; it's easy enough
               to do by hand.
            4. Once the above is done, there will be no reason
               to let them go above 1 on either faceshrink or stickershrink
               any more, so both of those sliders's maxes can be set to 1.
            5. (Optional) Actually "Shrink towards face boundaries"
               doesn't need to be boolean, it can be a slider value
               between 0 and 1.
        - It would be nice to have "face shrink 4d", "sticker shrink 4d",
              "face shrink 3d", "sticker shrink 3d".  The 4d and 3d versions
              do qualitatively different things (I was surprised when
              I first saw the 120-cell, until I realized this--
              I was expecting 3d sticker shrink which preserves 3d shape,
              but instead the program does 4d sticker shrink which
              has the effect of regularizing the final 3d shape
              as it gets smaller).

    BUGS / URGENT TODOS:
    ===================

        - side of prefs menu cut off
        - truncated tet is picking inconsistent slices!
        - progress meter on the slice-- just notice when it's taking a long time, and start spewing percentage  (started doing this, need nicer)
        - {5}x{5} 2 has sliver polygons-- I think the isPrismOfThisFace
          hack isn't adequate.  Also it doesnt work for {5}x{} (but that's 3d).
          I think I need to remove the slivers after the fact instead.
          OH hmm... the slivers are kinda cool because they are
          rotation handles!  Think about this... maybe draw them smaller
          and white, or something!

        - 2x2x2x2 does corner twists, should do face (I think)
        - why is scale different before I touch the slider??
        - scale doesn't quite match original
        - it's not oriented correctly at the end after slicing-- so I had to make orientDeep
          public and have the caller call it-- lame! need to send in all planes at once so it can do that automatically with some hope of being efficient
        - need more colors!
        - sometimes exception during picking trying to access too big
          an index right after switching to a smaller puzzle (e.g.
          pentprismprism to hypercube)
        - try to change the puzzle type while it's twisting, it goes into
          an infinite exception loop I think

    NOT HAVING TO DO WITH THIS GENERIC STUFF:
    =====================================================
        - fucking twisting after I let up on the mouse sucks! fix it!!!
        - fucking gui lies... not acceptable.
        - disallow spin dragging gives me the original orientation-- argh.
            - but sometimes that's what I want... bleah.
              need a way to save and restore my "favorite" orientation.
        - hey, I think clicking on a sticker shouldn't kill spin dragging--
            only clicking on boundary should stop it.
            that way can solve while it's spinning!  fun drinking game!
            ooh and make it speed up and slow down and tumble randomly
            while you are trying to solve it!
        - just noticed, Esc doesn't work to cancel immediately, have to click
            at least one thing first

    TODO:
    =====
        SPECIFICATION:
            - be able to specify initial orientation
                  (using which elts to which axes)
            - be able to specify slice thicknesses,
                  orthogonal to puzzle length spec,
                  and allow different for different faces
                  (we don't get slice thicknesses reasonable for anything
                  with triangles in it yet)
                  (and we want the 2.5 thing to work on only the pents,
                   not the squares, of a {5}x{4} and the {5,3}x{})
            - invention form should come up with current puzzle or previous
                  failed attempt
            - should mention Johnson numbers where applicable
        MISC:
            - can't twist while twist is in progress yet-- sucks for usability
            - the cool rotate-arbitrary-element-to-center thing
               should be undoable
            - faceExplode / stickerExplode sliders?
               I'm really confused on what the relationship is
               between those and viewScale, faceShrink, stickerShrink
            - history compression

        POLYTOPE STUFF:
            - getAllIncidences would be faster
              if I did it in two passes, counting first and then filling,
              instead of using a gzillion Vectors one for each element

        NON-IMMEDIATE:
            - Grand Antiprism
            - 3 level cascading menus for {3..12}x{3..12}?
            - nframes proportional to angle actually kind of sucks...
                should be proportionally less frames when rot angle is big,
                otherwise very small rotations get large acceleration
                which looks lame.  maybe the way to think about it is
                that slider should control max acceleration? i.e.
                the acceleration at the start and finish, if we make it
                a cubic function (or even if we leave it sine, I think)
            - nframes should ALWAYS be odd!  even means sometimes
                we get flat faces!
            - ooh, make more slices proportionally slower, would feel more massive!
            - completely general solve?
            - general uniform polytopes! yeah!
            - make slicing faster-- for humongous polytopes, only need to 
                look at neighbor facets (and slices thereof) and no farther,
                that should cut stuff down by a factor of 100 maybe

        PIE IN THE SKY:
            - figure out how to do "contiguous cubies" generically-- is it possible in terms of the others?  probably not... unless I modify the shrink behavior so it always likes up the outer ones?  Hmm, I think this would be a different "shrink mode" that shrinks stickers towards the face boundaries?  YES!  DO IT!

            - hmm... wireframe around the non-shrunk sliced geometry would be nice!
                In fact, it would be nice to have separate buttons for:
                - wirefame around unshrunk faces
                - wireframe around shrunk faces (separate faceShrink for it?)
                - wireframe around unshrunk stickers (separate stickerShrink for it?)
                - wireframe around stickers (that's the current button)

                polygon shrink?
                several different wireframes at once with different styles?
                okay I think this is where I went insane last time I was
                implementing a polytope viewer
            - fade out to transparent instead of suddenly turning inside out?
                This would nicely light up the center,
                And would also help mask the sorting failures
                on faces that are very close to flat
            - ha, for the {5}x{4}, it could be fudged so the cubical facets
                behave like they have full symmetry-- it would allow stickers
                to slide off of the pentprism face and onto a cube face.
                In general this will make the symmetry of a twist
                be dependent on the symmetry of the face,
                which can be more than the symmetry of the whole puzzle.
*/

import com.donhatchsw.util.*; // XXX get rid

class PolytopePuzzleDescription implements GenericPuzzleDescription {
    private com.donhatchsw.util.CSG.SPolytope originalPolytope;
    private com.donhatchsw.util.CSG.SPolytope slicedPolytope;

    private float _circumRadius;
    private float _inRadius;
    private int _nCubies;

    private float vertsMinusStickerCenters[][];
    private float vertStickerCentersMinusFaceCenters[][];
    private float vertFaceCenters[][]; // XXX maybe silly since we hace faceCenters
    private int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];

    private float faceCenters[/*nFaces*/][/*nDims*/];

    private int adjacentStickerPairs[][/*2*/][/*2*/];
    private int face2OppositeFace[/*nFaces*/];
    private int sticker2face[/*nStickers*/];
    private int sticker2faceShadow[/*nStickers*/]; // so we can detect nefariousness
    private int sticker2cubie[/*nStickers*/];

    private float gripCentersF[/*nGrips*/][];
    private int grip2face[/*nGrips*/];
    private int gripSymmetryOrders[/*nGrips*/];
    private double gripUsefulMats[/*nGrips*/][/*nDims*/][/*nDims*/]; // weird name
    private double faceInwardNormals[/*nFaces*/][/*nDims*/];
    private double faceCutOffsets[/*nFaces*/][/*nCutsThisFace*/]; // slice 0 is bounded by -infinity and offset[0], slice i+1 is bounded by offset[i],offset[i+1], ... slice[nSlices-1] is bounded by offset[nSlices-2]..infinity

    private float nicePointsToRotateToCenter[][];

    private double stickerCentersD[][];
    private FuzzyPointHashTable stickerCentersHashTable;

    private static void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }
    private static void Assumpt(boolean condition) { if (!condition) throw new Error("Assumption failed"); }
    

    /**
     * The following schlafli product symbols are supported;
     * note that they are all uniform, and every vertex has exactly
     * nDims incident facets and edges (things would go crazy otherwise).
     *
     *   3-dimensional:
     *     {3,3}
     *     {4,3}
     *     {5,3}
     *     {3}x{} or {}x{3}  (triangular prism)
     *     {4}x{} or {}x{4}  (cube, same as {4,3})
     *     {5}x{} or {}x{5}  (pentagonal prism)
     *     {6}x{} or {}x{6}  (hexagonal prism)
     *     ...
     *
     *   4-dimensional:
     *     {3,3,3}
     *     {4,3,3}
     *     {5,3,3}
     *
     *     {3}x{3}  {3}x{4}  {3}x{5}  {3}x{6}  ...
     *     {4}x{3}  {4}x{4}  {4}x{5}  {4}x{6}  ...
     *     {5}x{3}  {5}x{4}  {5}x{5}  {5}x{6}  ...
     *     {6}x{3}  {6}x{4}  {6}x{5}  {6}x{6}  ...
     *     ...
     *
     *     {3,3}x{} or {}x{3,3} (tetrahedral prism)
     *     {4,3}x{} or {}x{4,3} (hypercube, same as {4,3,3})
     *     {5,3}x{} or {}x{5,3} (dodecahedral prism)
     *
     * Note that {4} can also be expressed as {}x{} in any of the above.
     *
     */

    public PolytopePuzzleDescription(String schlafliProduct,
                                     int intLength, // number of segments per edge
                                     double doubleLength, // edge length / length of first edge segment
                                     java.io.PrintWriter progressWriter)
    {
        if (intLength < 1)
            throw new IllegalArgumentException("PolytopePuzzleDescription called with intLength="+intLength+", min legal intLength is 1");
        if (doubleLength <= 0)
            throw new IllegalArgumentException("PolytopePuzzleDescription called with doubleLength="+intLength+", doubleLength must be positive");

        if (progressWriter != null)
        {
            if (doubleLength == (double)intLength)
                progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+intLength+")...");
            else
                progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+intLength+" ("+doubleLength+")...");
            progressWriter.print("    Constructing polytope...");
            progressWriter.flush();
        }
        this.originalPolytope = com.donhatchsw.util.CSG.makeRegularStarPolytopeProductFromString(schlafliProduct);
        if (progressWriter != null)
        {
            progressWriter.println(" done ("+originalPolytope.p.facets.length+" facets).");
            progressWriter.flush();
        }
        com.donhatchsw.util.CSG.orientDeep(originalPolytope); // XXX shouldn't be necessary!!!!

        int nDims = originalPolytope.p.dim;  // == originalPolytope.fullDim

        CSG.Polytope originalElements[][] = originalPolytope.p.getAllElements();
        CSG.Polytope originalVerts[] = originalElements[0];
        CSG.Polytope originalFaces[] = originalElements[nDims-1];
        int nFaces = originalFaces.length;
        int originalIncidences[][][][] = originalPolytope.p.getAllIncidences();

        // Mark each original face with its face index.
        // These marks will persist even aver we slice up into stickers,
        // so that will give us the sticker-to-original-face-index mapping.
        // Also mark each vertex with its vertex index... etc.
        {
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
            for (int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
                originalElements[iDim][iElt].aux = new Integer(iElt);
        }

        //
        // Figure out the face inward normals and offsets;
        // these will be used for computing where cuts should go.
        //
        this.faceInwardNormals = new double[nFaces][nDims];
        double faceOffsets[] = new double[nFaces];
        for (int iFace = 0; iFace < nFaces; ++iFace)
        {
            CSG.Polytope face = originalFaces[iFace];
            CSG.Hyperplane plane = face.contributingHyperplanes[0];
            VecMath.vxs(faceInwardNormals[iFace], plane.normal, -1);
            faceOffsets[iFace] = -plane.offset;
            Assert(faceOffsets[iFace] < 0.);
            double invNormalLength = 1./VecMath.norm(faceInwardNormals[iFace]);
            VecMath.vxs(faceInwardNormals[iFace], faceInwardNormals[iFace], invNormalLength);
            faceOffsets[iFace] *= invNormalLength;
        }

        //
        // Figure out the circumRadius (farthest vertex from orign)
        // and inRadius (closest face plane to origin)
        // of the original polytope...
        //
        {
            double farthestVertexDistSqrd = 0.;
            for (int iVert = 0; iVert < originalVerts.length; ++iVert)
            {
                double thisDistSqrd = VecMath.normsqrd(originalVerts[iVert].getCoords());
                if (thisDistSqrd > farthestVertexDistSqrd)
                    farthestVertexDistSqrd = thisDistSqrd;
            }
            _circumRadius = (float)Math.sqrt(farthestVertexDistSqrd);

            double nearestFaceDist = 0.;
            for (int iFace = 0; iFace < originalFaces.length; ++iFace)
            {
                double thisFaceDist = -faceOffsets[iFace];
                if (thisFaceDist < nearestFaceDist)
                    nearestFaceDist = thisFaceDist;
            }
            _inRadius = (float)nearestFaceDist;
        }


        //
        // So we can easily find the opposite face of a given face...
        //
        this.face2OppositeFace = new int[nFaces];
        {
            FuzzyPointHashTable table = new FuzzyPointHashTable(1e-9, 1e-8, 1./128);
            for (int iFace = 0; iFace < nFaces; ++iFace)
                table.put(faceInwardNormals[iFace], originalFaces[iFace]);
            double oppositeNormalScratch[] = new double[nDims];
            //System.err.print("opposites:");
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                VecMath.vxs(oppositeNormalScratch, faceInwardNormals[iFace], -1.);
                CSG.Polytope opposite = (CSG.Polytope)table.get(oppositeNormalScratch);
                face2OppositeFace[iFace] = opposite==null ? -1 : ((Integer)opposite.aux).intValue();
                //System.err.print("("+iFace+":"+face2OppositeFace[iFace]+")");
            }
        }

        //
        // Figure out exactly what cuts are wanted
        // for each face.  Cuts parallel to two opposite faces
        // will appear in both faces' cut lists.
        //
        // Note, we store face inward normals rather than outward ones,
        // so that, as we iterate through the slicemask bit indices later,
        // the corresponding cut offsets will be in increasing order,
        // for sanity.
        //
        this.faceCutOffsets = new double[nFaces][];
        {
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                double fullThickness = 0.;
                {
                    // iVert = index of some vertex on face iFace
                    int iVert = originalIncidences[nDims-1][iFace][0][0];
                    // iVertEdges = indices of all edges incident on vert iVert
                    int iVertsEdges[] = originalIncidences[0][iVert][1];
                    // Find an edge incident on vertex iVert
                    // that is NOT incident on face iFace..
                    for (int i = 0; i < iVertsEdges.length; ++i)
                    {
                        int iEdge = iVertsEdges[i];
                        int iEdgesFaces[] = originalIncidences[1][iEdge][nDims-1];
                        int j;
                        for (j = 0; j < iEdgesFaces.length; ++j)
                            if (iEdgesFaces[j] == iFace)
                                break; // iEdge is incident on iFace-- no good
                        if (j == iEdgesFaces.length)
                        {
                            // iEdge is not incident on iFace-- good!
                            int jVert0 = originalIncidences[1][iEdge][0][0];
                            int jVert1 = originalIncidences[1][iEdge][0][1];
                            Assert((jVert0==iVert) != (jVert1==iVert));

                            double edgeVec[] = VecMath.vmv(
                                            originalVerts[jVert1].getCoords(),
                                            originalVerts[jVert0].getCoords());
                            double thisThickness = VecMath.dot(edgeVec, faceInwardNormals[iFace]);
                            if (thisThickness < 0.)
                                thisThickness *= -1.;

                            // If there are more than one neighbor vertex
                            // that's not on this face, pick one that's
                            // closest to the face plane.  This can only
                            // happen if the vertex figure is NOT a simplex
                            // (e.g. it happens for the icosahedron).
                            if (thisThickness > 1e-6
                             && (fullThickness == 0. || thisThickness < fullThickness))
                                fullThickness = thisThickness;
                        }
                    }
                }
                Assert(fullThickness != 0.); // XXX actually this fails if puzzle dimension <= 1, maybe should disallow

                //System.out.println("    slice thickness "+iFace+" = "+sliceThickness+"");

                boolean isPrismOfThisFace = Math.abs(-1. - faceOffsets[iFace]) < 1e-6;

                double sliceThickness = fullThickness / doubleLength;

                // If even length and *not* a prism of this face,
                // then the middle-most cuts will meet,
                // but the slice function can't handle that.
                // So back off a little so they don't meet,
                // so we'll get tiny invisible sliver faces there instead.
                if (intLength == doubleLength
                 && intLength % 2 == 0
                 && !isPrismOfThisFace)
                    sliceThickness *= .99;

                /*
                   Think about what's appropriate for simplex...
                        thickness = 1/3 of full to get upside down tet in middle, 
                                        with its verts poking the faces
                        thickness = 1/4 of full to get nothing in middle
                        thickness = 1/5 of full to get nice rightside up cell in middle
                                        YES, this is what 3 should do I think


                   But for triangular prism prism,
                            1/4 of full is the nice one for 3
                */

                int nNearCuts = intLength / 2; // (n-1)/2 if odd, n/2 if even
                int nFarCuts = face2OppositeFace[iFace]==-1 ? 0 :
                               intLength%2==0 && isPrismOfThisFace ? nNearCuts-1 :
                               nNearCuts;
                faceCutOffsets[iFace] = new double[nNearCuts + nFarCuts];

                for (int iNearCut = 0; iNearCut < nNearCuts; ++iNearCut)
                    faceCutOffsets[iFace][iNearCut] = faceOffsets[iFace] + (iNearCut+1)*sliceThickness;
                for (int iFarCut = 0; iFarCut < nFarCuts; ++iFarCut)
                    faceCutOffsets[iFace][nNearCuts+nFarCuts-1-iFarCut]
                        = -faceOffsets[iFace] // offset of opposite face
                        - (iFarCut+1)*sliceThickness;
            }
        }

        //System.out.println("face inward normals = "+com.donhatchsw.util.Arrays.toStringCompact(faceInwardNormals));
        //System.out.println("cut offsets = "+com.donhatchsw.util.Arrays.toStringCompact(faceCutOffsets));

        //
        // Slice!
        //
        {
            this.slicedPolytope = originalPolytope;
            if (progressWriter != null)
            {
                progressWriter.print("    Slicing");
                progressWriter.flush();
            }
            int maxCuts = -1; // unlimited
            //maxCuts = 6; // set to some desired number for debugging

            //
            // First find out how many cuts we are going to make...
            //
            int nTotalCuts = 0;
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                if (maxCuts >= 0 && nTotalCuts >= maxCuts) break;
                if (face2OppositeFace[iFace] != -1
                 && face2OppositeFace[iFace] < iFace)
                    continue; // already saw opposite face and made the cuts
                for (int iCut = 0; iCut < faceCutOffsets[iFace].length; ++iCut)
                {
                    if (maxCuts >= 0 && nTotalCuts >= maxCuts) break;
                    nTotalCuts++;
                }
            }
            System.out.print("("+nTotalCuts+" cuts)");

            int iTotalCut = 0;
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                if (maxCuts >= 0 && iTotalCut >= maxCuts) break;
                if (face2OppositeFace[iFace] != -1
                 && face2OppositeFace[iFace] < iFace)
                    continue; // already saw opposite face and made the cuts
                //System.out.println("REALLY doing facet "+iFace);
                for (int iCut = 0; iCut < faceCutOffsets[iFace].length; ++iCut)
                {
                    if (maxCuts >= 0 && iTotalCut >= maxCuts) break;
                    com.donhatchsw.util.CSG.Hyperplane cutHyperplane = new com.donhatchsw.util.CSG.Hyperplane(
                        faceInwardNormals[iFace],
                        faceCutOffsets[iFace][iCut]);
                    Object auxOfCut = null; // we don't set any aux on the cut for now
                    slicedPolytope = com.donhatchsw.util.CSG.sliceFacets(slicedPolytope, cutHyperplane, auxOfCut);
                    iTotalCut++;
                    if (progressWriter != null)
                    {
                        progressWriter.print("."); // one dot per cut

                        // We know we are doing an O(n^2) algorithm
                        // so our actual progress fraction is proportional to
                        // the square of the apparent fraction of items done.
                        if ((nTotalCuts-iTotalCut)%10 == 0)
                        {
                            progressWriter.print("("+percent_g_dammit(2,(double)100*iTotalCut*iTotalCut/nTotalCuts/nTotalCuts)+"%)");
                        }

                        progressWriter.flush();
                    }
                }
            }
            Assert(iTotalCut == nTotalCuts);

            if (progressWriter != null)
            {
                progressWriter.println(" done ("+slicedPolytope.p.facets.length+" stickers).");
                progressWriter.flush();
            }

            if (progressWriter != null)
            {
                progressWriter.print("    Fixing orientations (argh!)... ");
                progressWriter.flush();
            }
            com.donhatchsw.util.CSG.orientDeep(slicedPolytope); // XXX shouldn't be necessary!!!!
            if (progressWriter != null)
            {
                progressWriter.println(" done.");
                progressWriter.flush();
            }
        }


        CSG.Polytope stickers[] = slicedPolytope.p.getAllElements()[nDims-1];
        int nStickers = stickers.length;

        //
        // Figure out the mapping from sticker to face.
        //
        this.sticker2face = new int[nStickers];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2face[iSticker] = ((Integer)stickers[iSticker].aux).intValue();
        }
        this.sticker2faceShadow = VecMath.copyvec(sticker2face);

        //
        // Figure out the mapping from sticker to cubie.
        // Cubie indices are arbitrary and not used for anything else;
        // all that is guaranteed is that two stickers are on the same
        // cubie iff they have the same cubie index.
        //
        this.sticker2cubie = new int[nStickers];
        {
            com.donhatchsw.util.MergeFind mf = new com.donhatchsw.util.MergeFind(nStickers);
            // The 4d case:
            //     for each polygon in the sliced puzzle
            //         if it's part of an original polygon (not a cut)
            //             merge the two incident stickers

            CSG.Polytope slicedRidges[] = slicedPolytope.p.getAllElements()[nDims-2];
            int allSlicedIncidences[][][][] = slicedPolytope.p.getAllIncidences();
            for (int iSlicedRidge = 0; iSlicedRidge < slicedRidges.length; ++iSlicedRidge)
            {
                CSG.Polytope ridge = slicedRidges[iSlicedRidge];
                boolean ridgeIsFromOriginal = (ridge.aux != null);
                if (ridgeIsFromOriginal) // if it's not a cut
                {
                    // Find the two stickers that meet at this ridge...
                    int indsOfStickersContainingThisRidge[] = allSlicedIncidences[nDims-2][iSlicedRidge][nDims-1];
                    Assert(indsOfStickersContainingThisRidge.length == 2);
                    int iSticker0 = indsOfStickersContainingThisRidge[0];
                    int iSticker1 = indsOfStickersContainingThisRidge[1];
                    mf.merge(iSticker0, iSticker1);
                }
            }
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2cubie[iSticker] = mf.find(iSticker);

            if (progressWriter != null)
            {
                this._nCubies = 0;
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                    if (sticker2cubie[iSticker] == iSticker)
                        _nCubies++;
                progressWriter.println("    There seem to be "+_nCubies+" accessible cubie(s).");
            }
            // XXX note, we could easily collapse the cubie indicies
            // XXX so that they are consecutive, if we cared
        }

        if (false) // XXX remove if not necessary
        {
            //
            // Now that we've sliced it in its natural number of dimensions,
            // pad up to 4 dimensions for calculation of
            // coordinate stuff, by prismifying
            // by a small segment, square, or cube
            // as necessary.
            //
            CSG.SPolytope slicedPolytope4d;
            if (nDims < 4)
            {
                slicedPolytope = com.donhatchsw.util.CSG.cross(slicedPolytope,
                                                               com.donhatchsw.util.CSG.makeHypercube(new double[4-nDims], .1));
                Assert(slicedPolytope.p.dim == 4);
                stickers = slicedPolytope.p.getAllElements()[3];
            }
            else if (nDims == 4)
            {
                slicedPolytope4d = slicedPolytope;
            }
            else // nDims >= 5
            {
                slicedPolytope4d = null;
            }
        }



        //
        // Find the face centers and sticker centers.
        // The center of mass of the vertices is probably
        // as good as anything, for this
        // (when we get shrinking right, it won't
        // actually use centers, I don't think)
        //
        double faceCentersD[][] = new double[nFaces][nDims];
        {
            for (int iFace = 0; iFace < nFaces; ++iFace)
                com.donhatchsw.util.CSG.cgOfVerts(faceCentersD[iFace], originalFaces[iFace]);
        }
        this.stickerCentersD = new double[nStickers][nDims];
        this.stickerCentersHashTable = new FuzzyPointHashTable(1e-9, 1e-8, 1./128);
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                com.donhatchsw.util.CSG.cgOfVerts(stickerCentersD[iSticker], stickers[iSticker]);
                stickerCentersHashTable.put(stickerCentersD[iSticker], new Integer(iSticker));
            }
        }
        this.faceCenters = doubleToFloat(faceCentersD);

        float stickerCentersMinusFaceCentersF[][] = new float[nStickers][];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                stickerCentersMinusFaceCentersF[iSticker] = doubleToFloat(com.donhatchsw.util.VecMath.vmv(stickerCentersD[iSticker], faceCentersD[sticker2face[iSticker]]));
        }


        //
        // PolyFromPolytope doesn't seem to like the fact that
        // some elements have an aux and some don't... so clear all the vertex
        // auxs.
        // XXX why does this seem to be a problem for nonregular cross products but not for regulars?  figure this out
        //
        if (true)
        {
            CSG.Polytope allElements[][] = slicedPolytope.p.getAllElements();
            for (int iDim = 0; iDim < allElements.length; ++iDim)
            for (int iElt = 0; iElt < allElements[iDim].length; ++iElt)
                allElements[iDim][iElt].aux = null;
        }

        // If less than 4 dimensions, pad up to
        //
        // Get the rest verts (with no shrinkage)
        // and the sticker polygon indices.
        // This is dimension-specific.
        //
        double restVerts[][];
        if (nDims <= 4)
        {
            if (false) // this messes up indexing... XXX maybe should make it not?
            {
                // Start by making a completely separate Poly
                // out of each sticker.  There will be no
                // vertex sharing between stickers.
                com.donhatchsw.util.Poly stickerPolys[] = new com.donhatchsw.util.Poly[nStickers];
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    stickerPolys[iSticker] = com.donhatchsw.util.PolyCSG.PolyFromPolytope(stickers[iSticker]);
                    // So it gets grouped when we concatenate...
                    stickerPolys[iSticker].inds = new int[][][][] {(int[][][])stickerPolys[iSticker].inds};
                }
                //
                // Now concatenate them all together (the verts and the inds,
                // re-indexing inds to point to flattened vert indices)
                //
                {
                    com.donhatchsw.util.Poly slicedPoly = com.donhatchsw.util.Poly.concat(stickerPolys);

                    restVerts = (double[][])slicedPoly.verts;
                    // We assume there is only 1 contour per polygon,
                    // so we can flatten out the contours part of the Poly.
                    this.stickerInds = (int[][][])com.donhatchsw.util.Arrays.flatten(slicedPoly.inds, 2, 2);
                }
            }
            else
            {
                class iVertAux {
                    int iVert;
                    Object savedAux;
                    iVertAux(int iVert, Object savedAux)
                    {
                        this.iVert = iVert;
                        this.savedAux = savedAux;
                    }
                };
                CSG.Polytope allSlicedVerts[] = slicedPolytope.p.getAllElements()[0];
                for (int iVert = 0; iVert < allSlicedVerts.length; ++iVert)
                    allSlicedVerts[iVert].aux = new iVertAux(-1, allSlicedVerts[iVert].aux);

                int nVerts = 0;
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                    nVerts += stickers[iSticker].getAllElements()[0].length;

                restVerts = new double[nVerts][nDims];
                this.stickerInds = new int[nStickers][][];

                nVerts = 0; // reset, count again
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    CSG.Polytope sticker = stickers[iSticker];
                    // XXX note, we MUST step through the polys in the order in which they appear in getAllElements, NOT the order in which they appear in the facets list.  however, we need to get the sign from the facets list!
                    CSG.Polytope polysThisSticker[] = sticker.getAllElements()[2];
                    stickerInds[iSticker] = new int[polysThisSticker.length][];
                    for (int iPolyThisSticker = 0; iPolyThisSticker < polysThisSticker.length; ++iPolyThisSticker)
                    {
                        CSG.Polytope polygon = polysThisSticker[iPolyThisSticker];
                        stickerInds[iSticker][iPolyThisSticker] = new int[polygon.facets.length];
                        for (int iVertThisPoly = 0; iVertThisPoly < polygon.facets.length; ++iVertThisPoly)
                        {
                            // assert this polygon is oriented
                            // and nicely ordered the way we expect...
                            CSG.SPolytope thisEdge = polygon.facets[iVertThisPoly];
                            CSG.SPolytope nextEdge = polygon.facets[(iVertThisPoly+1)%polygon.facets.length];
                            Assert(thisEdge.p.facets.length == 2);
                            Assert(thisEdge.p.facets[0].sign == -1);
                            Assert(thisEdge.p.facets[1].sign == 1);
                            Assert(thisEdge.sign == -1 || thisEdge.sign == 1);
                            Assert(nextEdge.sign == -1 || nextEdge.sign == 1);
                            CSG.Polytope vertex = thisEdge.p.facets[thisEdge.sign==-1?0:1].p;
                            Assert(vertex == nextEdge.p.facets[nextEdge.sign==-1?1:0].p);
                            int iVert = ((iVertAux)vertex.aux).iVert;
                            if (iVert == -1)
                            {
                                iVert = nVerts++;
                                restVerts[iVert] = vertex.getCoords(); // okay to share with it, we aren't going to change it
                                ((iVertAux)vertex.aux).iVert = iVert;
                            }
                            stickerInds[iSticker][iPolyThisSticker][iVertThisPoly] = iVert;
                        }

                        // Figure out this polygon's sign in the sticker.
                        // Since we are iterating through sticker.allElements()[nDims-1] instead of through sticker's facet list (because we need that ordering),
                        // we don't have access to the sign directly.
                        // XXX bleah, this search sucks
                        int indexOfPolyInStickersFacets = 0;
                        while (stickers[iSticker].facets[indexOfPolyInStickersFacets].p != polygon)
                            indexOfPolyInStickersFacets++;
                        if (stickers[iSticker].facets[indexOfPolyInStickersFacets].sign == -1)
                        {
                            //
                            // Reverse the polygon
                            //
                            com.donhatchsw.util.Arrays.reverse(
                                stickerInds[iSticker][iPolyThisSticker],
                                stickerInds[iSticker][iPolyThisSticker]);
                        }
                    }
                    // clear the vertices' aux indices after each sticker,
                    // so that different stickers won't share vertices.
                    for (int iPolyThisSticker = 0; iPolyThisSticker < polysThisSticker.length; ++iPolyThisSticker)
                    {
                        CSG.Polytope polygon = polysThisSticker[iPolyThisSticker];
                        for (int iVertThisPoly = 0; iVertThisPoly < polygon.facets.length; ++iVertThisPoly)
                        {
                            CSG.SPolytope thisEdge = polygon.facets[iVertThisPoly];
                            CSG.Polytope vertex = thisEdge.p.facets[thisEdge.sign==-1?0:1].p;
                            ((iVertAux)vertex.aux).iVert = -1;
                        }
                    }
                }
                Assert(nVerts == restVerts.length);

                for (int iVert = 0; iVert < allSlicedVerts.length; ++iVert)
                    allSlicedVerts[iVert].aux = ((iVertAux)allSlicedVerts[iVert].aux).savedAux;
            }

            //
            // Fix up the indices on each sticker so that
            // the first vertex on the second face
            // does not occur on the first face;
            // that will guarantee that [0][0], [0][1], [0][2], [1][0]
            // form a non-degenerate simplex, as required.
            //
            {
                for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
                {
                    int polygon0[] = stickerInds[iSticker][0];
                    int polygon1[] = stickerInds[iSticker][1];
                    int i;
                    for (i = 0; i < polygon1.length; ++i)
                    {
                        int j;
                        for (j = 0; j < polygon0.length; ++j)
                        {
                            if (polygon1[i] == polygon0[j])
                                break; // this i is no good
                        }
                        if (j == polygon0.length)
                            break; // this i is good
                    }
                    // Cyclic permute polygon1
                    // to put its [i] at [0]
                    if (i != 0)
                    {
                        int cycled[] = new int[polygon1.length];
                        for (int ii = 0; ii < cycled.length; ++ii)
                            cycled[ii] = polygon1[(i+ii)%cycled.length];
                        stickerInds[iSticker][1] = cycled;
                    }
                }
            }

            //
            // Get adjacent sticker pairs into this.adjacentStickerPairs...
            //
            {
                int stickerIncidences[][][] = slicedPolytope.p.getAllIncidences()[nDims-1];
                int nPolygons = slicedPolytope.p.getAllElements()[2].length;
                this.adjacentStickerPairs = new int[nPolygons][2][];
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    int thisStickersIncidentPolygons[] = stickerIncidences[iSticker][nDims-2];
                    for (int iPolyThisSticker = 0; iPolyThisSticker < thisStickersIncidentPolygons.length; ++iPolyThisSticker)
                    {
                        int iPoly = thisStickersIncidentPolygons[iPolyThisSticker];
                        int j = adjacentStickerPairs[iPoly][0]==null ? 0 : 1;
                        Assert(adjacentStickerPairs[iPoly][j] == null);
                        adjacentStickerPairs[iPoly][j] = new int[] {iSticker,iPolyThisSticker};
                    }
                }
                for (int iPoly = 0; iPoly < adjacentStickerPairs.length; ++iPoly)
                    for (int j = 0; j < 2; j++)
                        Assert(adjacentStickerPairs[iPoly][j] != null);
            }

        }
        else // nDims >= 5
        {
            // Make a vertex array of the right size,
            // just so nVerts() will return something sane for curiosity
            int nVerts = 0;
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                nVerts += stickers[iSticker].getAllElements()[0].length;
            restVerts = new double[nVerts][nDims]; // zeros
            this.stickerInds = new int[nStickers][0][];
        }


        //
        // Calculate the three arrays 
        // that will let us quickly calculate the sticker verts
        // at rest for any faceShrink and stickerShrink.
        // Note that vertFaceCenters and vertStickerCentersMinusFaceCenters
        // contain lots of duplicates; these dups all point
        // to the same float[] however, so it's not as horrid as it seems.
        //
        {
            int nVerts = restVerts.length;
            this.vertsMinusStickerCenters = new float[nVerts][];
            this.vertStickerCentersMinusFaceCenters = new float[nVerts][];
            this.vertFaceCenters = new float[nVerts][];
            {
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                for (int j = 0; j < stickerInds[iSticker].length; ++j)
                for (int k = 0; k < stickerInds[iSticker][j].length; ++k)
                {
                    int iFace = sticker2face[iSticker];
                    int iVert = stickerInds[iSticker][j][k];
                    if (vertsMinusStickerCenters[iVert] == null)
                    {
                        vertsMinusStickerCenters[iVert] = doubleToFloat(com.donhatchsw.util.VecMath.vmv(restVerts[iVert], stickerCentersD[iSticker]));
                        vertStickerCentersMinusFaceCenters[iVert] = stickerCentersMinusFaceCentersF[iSticker];
                        vertFaceCenters[iVert] = faceCenters[iFace];
                    }
                }
            }
        }

        //
        // Now think about the twist grips.
        // There will be one grip at each vertex,edge,face center
        // of the original polytope (if 3d)
        // or of each cell of the original polytope (if 4d).
        // XXX woops, I'm retarded, 3d doesn't have that...
        // XXX but actually it wouldn't hurt, could just make that
        // XXX rotate the whole puzzle.
        //
        if (intLength == 1)
        {
            // Don't bother with grips for now, it's taking too long
            // for the big ones
            int nGrips = 0;
            this.gripSymmetryOrders = new int[nGrips];
            this.gripUsefulMats = new double[nGrips][nDims][nDims];
            this.gripCentersF = new float[nGrips][];
            this.grip2face = new int[nGrips];
        }
        else
        {
            if (nDims == 4)
            {
                if (progressWriter != null)
                {
                    progressWriter.print("    Thinking about possible twists...");
                    progressWriter.flush();
                }

                int nGrips = 0;
                for (int iFace = 0; iFace < nFaces; ++iFace)
                {
                    com.donhatchsw.util.CSG.Polytope[][] allElementsOfCell = originalFaces[iFace].getAllElements();
                    for (int iDim = 0; iDim <= 3; ++iDim) // yes, even for cell center, which doesn't do anything
                        nGrips += allElementsOfCell[iDim].length;
                }
                this.gripSymmetryOrders = new int[nGrips];
                this.gripUsefulMats = new double[nGrips][nDims][nDims];
                this.gripCentersF = new float[nGrips][];
                this.grip2face = new int[nGrips];
                double gripCenterD[] = new double[nDims];
                int iGrip = 0;
                for (int iFace = 0; iFace < nFaces; ++iFace)
                {
                    CSG.Polytope cell = originalFaces[iFace];
                    com.donhatchsw.util.CSG.Polytope[][] allElementsOfCell = cell.getAllElements();
                    for (int iDim = 0; iDim <= 3; ++iDim) // XXX should we have a grip for the cell center, which doesn't do anything? maybe!
                    {
                        for (int iElt = 0; iElt < allElementsOfCell[iDim].length; ++iElt)
                        {
                            CSG.Polytope elt = allElementsOfCell[iDim][iElt];
                            gripSymmetryOrders[iGrip] = CSG.calcRotationGroupOrder(
                                                    originalPolytope.p, cell, elt,
                                                    gripUsefulMats[iGrip]);

                            com.donhatchsw.util.CSG.cgOfVerts(gripCenterD, elt);
                            // !! We can't use the element center,
                            // that will end up having the same center
                            // for different stickers on the same cubie!
                            // So fudge it a little towards the cell center.
                            // XXX should try to be more scientific...
                            VecMath.lerp(gripCenterD, gripCenterD, faceCentersD[iFace], .01);

                            gripCentersF[iGrip] = doubleToFloat(gripCenterD);
                            grip2face[iGrip] = iFace;
                            if (progressWriter != null)
                            {
                                //progressWriter.print("("+iDim+":"+gripSymmetryOrders[iGrip]+")");
                                //progressWriter.print(".");

                                progressWriter.flush();
                            }

                            iGrip++;
                        }
                    }
                }
                Assert(iGrip == nGrips);

                /*
                want to know, for each grip:
                    - the grip center coords
                    - its face center coords
                    - its period
                    for each slice using this grip (i.e. iterate through the slices parallel to the face the grip is on):
                        - from indices of a CCW twist of this slice
                        - to indices of a CCW twist of this slice
                */

                if (progressWriter != null)
                {
                    progressWriter.print(" ("+nGrips+" grips)");
                    progressWriter.println(" done.");
                    progressWriter.flush();
                }
            } // nDims == 4
        } // intLength > 1

        //
        // Select points worthy of being rotated to the center (-W axis).
        //
        {
            int nNicePoints = 0;
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
                nNicePoints += originalElements[iDim].length;
            this.nicePointsToRotateToCenter = new float[nNicePoints][nDims];
            double eltCenter[] = new double[nDims];
            int iNicePoint = 0;
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
            for (int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
            {
                com.donhatchsw.util.CSG.cgOfVerts(eltCenter, originalElements[iDim][iElt]);
                nicePointsToRotateToCenter[iNicePoint++] = doubleToFloat(eltCenter);

            }
            Assert(iNicePoint == nNicePoints);
        }


        if (progressWriter != null)
        {
            progressWriter.println("Done.");
            progressWriter.flush();
        }
    } // ctor from schlafli and length

    public String toString(boolean verbose)
    {
        String nl = System.getProperty("line.separator");
        com.donhatchsw.util.CSG.Polytope[][] allElements = slicedPolytope.p.getAllElements();
        String answer = "{sliced polytope counts per dim = "
                      +com.donhatchsw.util.Arrays.toStringCompact(
                       com.donhatchsw.util.CSG.counts(slicedPolytope.p))
                      +", "+nl+"  nDims = "+nDims()
                      +", "+nl+"  nFaces = "+nFaces()
                      +", "+nl+"  nStickers = "+nStickers()
                      +", "+nl+"  nGrips = "+nGrips()
                      +", "+nl+"  nVisibleCubies = "+nCubies()
                      +", "+nl+"  nStickerVerts = "+nVerts();
        if (verbose)
        {
            answer +=
                      ", "+nl+"  slicedPolytope = "+slicedPolytope.toString(true)

                      +", "+nl+"  vertsMinusStickerCenters = "+com.donhatchsw.util.Arrays.toStringNonCompact(vertsMinusStickerCenters, "    ", "    ")
                      +", "+nl+"  vertStickerCentersMinusFaceCenters = "+com.donhatchsw.util.Arrays.toStringNonCompact(vertStickerCentersMinusFaceCenters, "    ", "    ")
                      +", "+nl+"  vertFaceCenters = "+com.donhatchsw.util.Arrays.toStringNonCompact(vertFaceCenters, "    ", "    ")
                      +", "+nl+"  stickerInds = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerInds, "    ", "    ")
                      +", "+nl+"  sticker2face = "+com.donhatchsw.util.Arrays.toStringNonCompact(sticker2face, "    ", "    ");
        }
        answer += "}";
        return answer;
    } // toString

    public String toString()
    {
        return toString(false); // non verbose
    }



    //
    // Utilities...
    //
        private static float[] doubleToFloat(double in[])
        {
            float out[] = new float[in.length];
            for (int i = 0; i < in.length; ++i)
                out[i] = (float)in[i];
            return out;
        }
        private static float[][] doubleToFloat(double in[][])
        {
            float out[][] = new float[in.length][];
            for (int i = 0; i < in.length; ++i)
                out[i] = doubleToFloat(in[i]);
            return out;
        }

        // magic crap used in a couple of methods below
        private double[][] getTwistMat(int gripIndex, int dir, double frac)
        {
            int order = gripSymmetryOrders[gripIndex];
            double angle = dir * (2*Math.PI/order) * frac;
            int nDims = slicedPolytope.p.fullDim;
            return VecMath.mxmxm(VecMath.transpose(gripUsefulMats[gripIndex]),
                                 VecMath.makeRowRotMat(nDims,nDims-2,nDims-1, angle),
                                 gripUsefulMats[gripIndex]);
        } // getTwistMat

        // format x using printf format "%.17g", dammit
        private static String percent_g_dammit(int seventeen, // digits of precision
                                       double x)
        {
            if (x == 0)
                return "0";

            double threshold = Math.round(Math.pow(10, seventeen));
            double scale = 1.;
            while (x < threshold)
            {
                x *= 10;
                scale *= 10;
            }
            while (x >= threshold)
            {
                x /= 10;
                scale /= 10;
            }
            x = Math.floor(x);
            x /= scale;
            if (Math.floor(x) == x)
                return ""+(int)Math.floor(x);
            else
                return ""+x;
        } // percent_g_dammit






    //======================================================================
    // BEGIN GENERICPUZZLEDESCRIPTION INTERFACE METHODS
    //

        public int nDims()
        {
            return slicedPolytope.p.fullDim;
        }
        public int nVerts()
        {
            return vertsMinusStickerCenters.length;
        }
        public int nFaces()
        {
            return originalPolytope.p.facets.length;
        }
        public int nCubies()
        {
            return _nCubies;
        }
        public int nStickers()
        {
            return slicedPolytope.p.facets.length;
        }
        public int nGrips()
        {
            return grip2face.length;
        }
        public float circumRadius()
        {
            return _circumRadius;
        }
        public float inRadius()
        {
            return _inRadius;
        }

        public void computeStickerVertsAtRest(float verts[/*nVerts*/][/*nDims*/],
                                              float faceShrink,
                                              float stickerShrink)
        {
            Assert(verts.length == vertsMinusStickerCenters.length);
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                float faceCenter[] = vertFaceCenters[iVert];
                float stickerCenterMinusFaceCenter[] = vertStickerCentersMinusFaceCenters[iVert];
                float vertMinusStickerCenter[] = vertsMinusStickerCenters[iVert];
                float vert[] = verts[iVert];
                Assert(vert.length == vertMinusStickerCenter.length);
                for (int j = 0; j < vert.length; ++j)
                    vert[j] = (vertMinusStickerCenter[j] * stickerShrink
                             + stickerCenterMinusFaceCenter[j]) * faceShrink
                             + faceCenter[j];
            }
        }
        public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/]
            getStickerInds()
        {
            return stickerInds;
        }
        public void computeGripVertsAtRest(float verts[/*nVerts*/][/*nDims*/],
                                           float faceShrink,
                                           float stickerShrink)
        {
            throw new RuntimeException("unimplemented");
        }
        public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/]
            getGripInds()
        {
            throw new RuntimeException("unimplemented");
        }
        public int[/*nGrips*/]
            getGripSymmetryOrders()
        {
            return gripSymmetryOrders;
        }

        public double[][] getFaceInwardNormals()
        {
            return faceInwardNormals;
        }
        public double[][] getFaceCutOffsets()
        {
            return faceCutOffsets;
        }

        public int getClosestGrip(float pickCoords[/*4*/])
        {
            int bestIndex = -1;
            float bestDistSqrd = Float.MAX_VALUE;
            for (int i = 0; i < gripCentersF.length; ++i)
            {
                float thisDistSqrd = VecMath.distsqrd(gripCentersF[i],
                                                      pickCoords);
                if (thisDistSqrd < bestDistSqrd)
                {
                    bestDistSqrd = thisDistSqrd;
                    bestIndex = i;
                }
            }
            return bestIndex;
        }
        public float[/*nDims*/] getClosestNicePointToRotateToCenter(float pickCoords[])
        {
            int bestIndex = -1;
            float bestDistSqrd = Float.MAX_VALUE;
            for (int i = 0; i < nicePointsToRotateToCenter.length; ++i)
            {
                float thisDistSqrd = VecMath.distsqrd(nicePointsToRotateToCenter[i],
                                                      pickCoords);
                if (thisDistSqrd < bestDistSqrd)
                {
                    bestDistSqrd = thisDistSqrd;
                    bestIndex = i;
                }
            }
            return nicePointsToRotateToCenter[bestIndex];
        }
        public void
            computeStickerVertsPartiallyTwisted(
                                            float verts[/*nVerts*/][/*nDims*/],
                                            float faceShrink,
                                            float stickerShrink,
                                            int gripIndex,
                                            int dir,
                                            int slicemask,
                                            float frac)
        {
            // Note, we purposely go through all the calculation
            // even if dir*frac is 0; we get more consistent timing that way.
            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if (gripSymmetryOrders[gripIndex] == 0)
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on gripIndex "+gripIndex+" which does not rotate!");

            if (slicemask == 0)
                slicemask = 1; // XXX is this the right place for this? lower and it might be time consuming, higher and too many callers will have to remember to do it

            double matD[][] = getTwistMat(gripIndex, dir, frac);
            float matF[][] = doubleToFloat(matD);

            float restVerts[][] = new float[nVerts()][nDims()];
            computeStickerVertsAtRest(restVerts, faceShrink, stickerShrink);
            boolean whichVertsGetMoved[] = new boolean[restVerts.length]; // false initially
            int iFace = grip2face[gripIndex];
            double thisFaceInwardNormal[] = faceInwardNormals[iFace];
            double thisFaceCutOffsets[] = faceCutOffsets[iFace];
            for (int iSticker = 0; iSticker < stickerCentersD.length; ++iSticker)
            {
                if (pointIsInSliceMask(stickerCentersD[iSticker],
                                       slicemask,
                                       thisFaceInwardNormal,
                                       thisFaceCutOffsets))
                {
                    for (int i = 0; i < stickerInds[iSticker].length; ++i)
                    for (int j = 0; j < stickerInds[iSticker][i].length; ++j)
                        whichVertsGetMoved[stickerInds[iSticker][i][j]] = true;
                }
            }

            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                if (whichVertsGetMoved[iVert])
                    verts[iVert] = VecMath.vxm(restVerts[iVert], matF);
                else
                    verts[iVert] = restVerts[iVert];
            }
        } // getStickerVertsPartiallyTwisted
        public int[/*nStickers*/] getSticker2Face()
        {
            // Make sure caller didn't mess it up from last time!!
            if (!VecMath.equals(sticker2face, sticker2faceShadow))
                throw new RuntimeException("PolytopePuzzleDescription.getSticker2Face: caller modified previously returned sticker2face! BAD! BAD! BAD!");
            return sticker2face;
        }
        public int[/*nStickers*/] getSticker2Cubie()
        {
            return sticker2cubie;
        }
        public int[/*nFaces*/] getGrip2Face()
        {
            return grip2face;
        }
        public int[/*nFaces*/] getFace2OppositeFace()
        {
            return face2OppositeFace;
        }
        public int[][/*2*/][/*2*/]
            getAdjacentStickerPairs()
        {
            return adjacentStickerPairs;
        }
        public float[/*nFaces*/][/*nDims*/]
            getFaceCentersAtRest()
        {
            return faceCenters;
        }
        public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                    int gripIndex,
                                                    int dir,
                                                    int slicemask)
        {
            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if (gripSymmetryOrders[gripIndex] == 0)
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on gripIndex "+gripIndex+" which does not rotate!");
            if (state.length != stickerCentersD.length)
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called with wrong size state "+state.length+", expected "+stickerCentersD.length+"!");

            if (slicemask == 0)
                slicemask = 1; // XXX is this the right place for this? lower and it might be time consuming, higher and too many callers will have to remember to do it

            double scratchVert[] = new double[nDims()];
            double matD[][] = getTwistMat(gripIndex, dir, 1.);
            int newState[] = new int[state.length];
            int iFace = grip2face[gripIndex];
            double thisFaceInwardNormal[] = faceInwardNormals[iFace];
            double thisFaceCutOffsets[] = faceCutOffsets[iFace];
            for (int iSticker = 0; iSticker < state.length; ++iSticker)
            {
                if (pointIsInSliceMask(stickerCentersD[iSticker],
                                       slicemask,
                                       thisFaceInwardNormal,
                                       thisFaceCutOffsets))
                {
                    VecMath.vxm(scratchVert, stickerCentersD[iSticker], matD);
                    Integer whereIstickerGoes = (Integer)stickerCentersHashTable.get(scratchVert);
                    Assert(whereIstickerGoes != null);
                    newState[whereIstickerGoes.intValue()] = state[iSticker];
                }
                else
                    newState[iSticker] = state[iSticker];
            }
            VecMath.copyvec(state, newState);
            return newState;
        } // applyTwistToState


        // does NOT do the slicemask 0->1 correction
        private static boolean pointIsInSliceMask(double point[],
                                                  int slicemask,
                                                  double cutNormal[],
                                                  double cutOffsets[])
        {
            // XXX a binary search would work better if num cuts is big.
            // XXX really only need to check offsets between differing
            // XXX bits of slicmask.
            double pointHeight = VecMath.dot(point, cutNormal);
            int iSlice = 0;
            while (iSlice < cutOffsets.length
                && pointHeight > cutOffsets[iSlice])
                iSlice++;
            boolean answer = (slicemask & (1<<iSlice)) != 0;
            return answer;
        }
    //
    // END OF GENERICPUZZLEDESCRIPTION INTERFACE METHODS
    //======================================================================


    //
    // Little test program
    //
    public static void main(String args[])
    {
        if (args.length != 2)
        {
            System.err.println();
            System.err.println("    Usage: PolytopePuzzleDescription \"<schlafliProduct>\" <puzzleLength>");
            System.err.println();
            System.exit(1);
        }
        System.out.println("in main");

        //com.donhatchsw.util.CSG.verboseLevel = 2;

        java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                             new java.io.BufferedWriter(
                                             new java.io.OutputStreamWriter(
                                             System.err)));

        String schlafliProduct = args[0];
        double doubleLength = Double.parseDouble(args[1]);
        GenericPuzzleDescription descr = new PolytopePuzzleDescription(schlafliProduct,
                                                                       (int)Math.ceil(doubleLength),
                                                                       doubleLength,
                                                                       progressWriter);
        System.out.println("description = "+descr.toString());

        System.out.println("out main");
    } // main

} // class PolytopePuzzleDescription