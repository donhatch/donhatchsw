/*
    RELNOTES:
    =========
        This version has the following enhancements:
            - Lots of new puzzle types available from the Puzzle menu.
              These are called "generic puzzles" and they are a work
              in progress.
        Generic puzzles have the following limitations currently:
            - no save/load (menus are probably misleading)
            - no real solve
            - no macros (menus are probably misleading)
            - can't twist (or undo or redo)
               while a twist or cheat is in progress
               (which means you can't double-click to do a move twice)
            - only 8 colors still, even when more than 8 faces
            - some of the even-length puzzles have spurious extra
              very thin stickers at the halfway planes
            - trying to make a puzzle based on triangles
               (e.g. simplex, triangular prism)
               will probably produce something ugly at this point, if anything
            - exceptions everywhere if you try to do unimplemented stuff
        And the following enhancements:
            - you can rotate any *cubie* of the length-3 puzzle
              to the center with the middle mouse (not just hyperface centers).

        Maybe:
            - no scramble
            - contiguous cubies not implemented (even if gui says otherwise)
            - shadows not implemented (even if gui says otherwise)
            - sticker highlighting not implemented
            - highlight by cubie not implemented (even if gui says otherwise)

    BUGS / URGENT TODOS:
    ===================

        - needs the sticker2cubie map, using seed fill
        - implement scramble (easy)
        - redo not working? (should be easy)

        - needs shading
        - 120-cell seems messed up... can't get orientation right
          and rotate-to-center seems to rotate it away from center?
          I think it might have to do with failing to push down
          the outermost sign in the polytope when it's
          sign-corrected during creation?

        - {5}x{5} 2 has sliver polygons-- I think the isPrismOfThisFace
          hack isn't adequate.  Also it doesnt work for {5}x{} (but that's 3d).
          I think I need to remove the slivers after the fact instead.
          OH hmm... the slivers are kinda cool because they are
          rotation handles!  Think about this... maybe draw them smaller
          and white, or something!

        - 2x2x2x2 gets in a bad state, because there are more grips than stickers??? is it indexing by grip into an array that is supposed to be indexed by sticker??
        - 2x2x2x2 does corner twists, should do face (I think)
        - why is scale different before I touch the slider??
        - scale doesn't quite match original
        - initial orientation is different-- oh I think the original has tilt and twirl baked in or something
        - it's not oriented correctly at the end-- so I had to make orientDeep
          public and have the caller call it-- lame! need to send in all planes at once so it can do that automatically with some hope of being efficient
        - need more colors!

    ISSUES:
    =======
        - Possible rot-element-to-center behaviors,
          from least to most restrictive
            - don't do it
            - only do it if enabled via checkbox or esoteric modifier combo
            - only do it on stickers that are already on the center face,
              or if there is no center face; 
              if there is a center face, clicking anywhere on a diff face
              just centers that face, you have to click a non-center
              sticker on that face again once the face is in the center
              to focus it
            - do it everywhere-- on one hand this is nice and clean
              and powerful, but on the other hand sometimes it's
              hard to click on the face-center sticker which is
              what is most often wanted
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

    TODO:
    =====
        SPECIFICATION:
            - initial orientation (using which elts to which axes)
                - default should be largest face first
                  (this is especially important for the {5}x{5},
                  which doesn't even come up face first otherwise!)
            - be able to specify slice thicknesses,
                  orthogonal to puzzle length spec,
                  and allow different for different faces
                  (we don't get slice thicknesses reasonable for anything
                  with triangles in it yet)
                  (and we want the 2.5 thing to work on only the pents,
                   not the squares, of a {5}x{4} and the {5,3}x{})
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
            - 3 level cascading menus for {3..12}x{3..12}?
            - make it always come up biggest-face-first by default (actually it seems to)
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
            - fade out to black instead of suddenly turning inside out?
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

    private float vertsMinusStickerCenters[][];
    private float vertStickerCentersMinusFaceCenters[][];
    private float vertFaceCenters[][];
    private int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];
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
     * XXX would also be cool to support other uniform polyhedra or polychora
     *     with simplex vertex figures, e.g.
     *      truncated regular
     *      omnitruncated regular
     */

    public PolytopePuzzleDescription(String schlafliProduct,
                                     double length, // usually int but can experiment with different cut depths
                                     java.io.PrintWriter progressWriter)
    {
        if (length < 1)
            throw new IllegalArgumentException("PolytopePuzzleDescription called with length="+length+", min legal length is 1");

        if (progressWriter != null)
        {
            progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+length+"...");
            progressWriter.print("    Constructing polytope...");
            progressWriter.flush();
        }
        this.originalPolytope = com.donhatchsw.util.CSG.makeRegularStarPolytopeCrossProductFromString(schlafliProduct);
        if (progressWriter != null)
        {
            progressWriter.println(" done ("+originalPolytope.p.facets.length+" facets).");
            progressWriter.flush();
        }

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
        CSG.Polytope faceToOppositeFace[] = new CSG.Polytope[nFaces];
        {
            FuzzyPointHashTable table = new FuzzyPointHashTable(1e-9, 1e-8, 1./128);
            for (int iFace = 0; iFace < nFaces; ++iFace)
                table.put(faceInwardNormals[iFace], originalFaces[iFace]);
            double oppositeNormalScratch[] = new double[nDims];
            //System.err.print("opposites:");
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                VecMath.vxs(oppositeNormalScratch, faceInwardNormals[iFace], -1.);
                faceToOppositeFace[iFace] = (CSG.Polytope)table.get(oppositeNormalScratch);
                //System.err.print("("+iFace+":"+(faceToOppositeFace[iFace]!=null ? ""+((Integer)faceToOppositeFace[iFace].aux).intValue():"null")+")");
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
                Assert(fullThickness != 0.);

                //System.out.println("    slice thickness "+iFace+" = "+sliceThickness+"");

                boolean isPrismOfThisFace = Math.abs(-1. - faceOffsets[iFace]) < 1e-6;
                int ceilLength = (int)Math.ceil(length);

                // Fractional lengths are basically a hack for pentagons
                // and higher gons
                // so that the middle edge width can be controlled
                // by the user; we don't want it to apply
                // to squares though
                if (isPrismOfThisFace)
                    length = ceilLength;

                double sliceThickness = fullThickness / length;

                // If even length and *not* a prism of this face,
                // then the middle-most cuts will meet,
                // but the slice function can't handle that.
                // So back off a little so they don't meet,
                // so we'll get tiny invisible sliver faces there instead.
                if (length == ceilLength
                 && ceilLength % 2 == 0
                 && !isPrismOfThisFace)
                    sliceThickness *= .99;

                //sliceThickness = fullThickness/4;

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

                int nNearCuts = ceilLength / 2; // (n-1)/2 if odd, n/2 if even
                int nFarCuts = faceToOppositeFace[iFace]==null ? 0 :
                               ceilLength%2==0 && isPrismOfThisFace ? nNearCuts-1 :
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
            int totalCuts = 0;
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                if (maxCuts >= 0 && totalCuts >= maxCuts) break;
                if (faceToOppositeFace[iFace] != null
                 && ((Integer)faceToOppositeFace[iFace].aux).intValue() < iFace)
                    continue; // already saw opposite face and made the cuts
                //System.out.println("REALLY doing facet "+iFace);
                for (int iCut = 0; iCut < faceCutOffsets[iFace].length; ++iCut)
                {
                    if (maxCuts >= 0 && totalCuts >= maxCuts) break;
                    com.donhatchsw.util.CSG.Hyperplane cutHyperplane = new com.donhatchsw.util.CSG.Hyperplane(
                        faceInwardNormals[iFace],
                        faceCutOffsets[iFace][iCut]);
                    Object auxOfCut = null; // we don't set any aux on the cut for now
                    slicedPolytope = com.donhatchsw.util.CSG.sliceFacets(slicedPolytope, cutHyperplane, auxOfCut);
                    if (progressWriter != null)
                    {
                        progressWriter.print("."); // one dot per cut
                        progressWriter.flush();
                    }
                    totalCuts++;
                }
            }

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

       int nStickers = slicedPolytope.p.facets.length;

        //
        // Figure out the mapping from sticker to face.
        //
        this.sticker2face = new int[nStickers];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2face[iSticker] = ((Integer)slicedPolytope.p.facets[iSticker].p.aux).intValue();
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
                int nCubies = 0;
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                    if (sticker2cubie[iSticker] == iSticker)
                        nCubies++;
                progressWriter.println("    There seem to be "+nCubies+" accessible cubie(s).");
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
                com.donhatchsw.util.CSG.cgOfVerts(stickerCentersD[iSticker], slicedPolytope.p.facets[iSticker].p);
                stickerCentersHashTable.put(stickerCentersD[iSticker], new Integer(iSticker));
            }
        }

        float faceCentersF[][] = doubleToFloat(faceCentersD);
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

        //
        // Get the rest verts (with no shrinkage)
        // and the sticker polygon indices.
        // This is dimension-specific.
        //
        double restVerts[][];
        if (nDims == 3)
        {
            com.donhatchsw.util.Poly slicedPoly = com.donhatchsw.util.PolyCSG.PolyFromPolytope(slicedPolytope.p);
            restVerts = (double[][])slicedPoly.verts;

            // slicedPoly.inds is a list of faces, each of which
            // is a list of contours.  We assume there is always
            // one contour per face (i.e. no holes),
            // so we can now just re-interpret the one contour per face
            // as one face per sticker (instead of flattening
            // and re-expanding which would just give us back
            // what we started with).
            this.stickerInds = (int[][][])slicedPoly.inds;
        }
        else if (nDims == 4)
        {
            // Start by making a completely separate Poly
            // out of each sticker.  There will be no
            // vertex sharing between stickers.
            com.donhatchsw.util.Poly stickerPolys[] = new com.donhatchsw.util.Poly[nStickers];
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                stickerPolys[iSticker] = com.donhatchsw.util.PolyCSG.PolyFromPolytope(slicedPolytope.p.facets[iSticker].p);
                // So it gets grouped when we concatenate...
                stickerPolys[iSticker].inds = new int[][][][] {(int[][][])stickerPolys[iSticker].inds};
            }
            //
            // Now concatenate them all together (the verts
            // and the inds).
            //
            com.donhatchsw.util.Poly slicedPoly = com.donhatchsw.util.Poly.concat(stickerPolys);

            restVerts = (double[][])slicedPoly.verts;
            // We assume there is only 1 contour per polygon,
            // so we can flatten out the contours part.
            this.stickerInds = (int[][][])com.donhatchsw.util.Arrays.flatten(slicedPoly.inds, 2, 2);

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
        }
        else // nDims is something other than 3 or 4
        {
            restVerts = new double[0][nDims];
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
                        vertFaceCenters[iVert] = faceCentersF[iFace];
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
        if (nDims == 4)
        {
            if (progressWriter != null)
            {
                progressWriter.print("    Thinking about possible twists...");
                progressWriter.flush();
            }

            int nGrips = 0;
            for (int iFace = 0; iFace < originalPolytope.p.facets.length; ++iFace)
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

    public String toString()
    {
        String nl = System.getProperty("line.separator");
        com.donhatchsw.util.CSG.Polytope[][] allElements = slicedPolytope.p.getAllElements();
        int sizes[] = new int[allElements.length];
        for (int iDim = 0; iDim < sizes.length; ++iDim)
            sizes[iDim] = allElements[iDim].length;
        String answer = "{polytope counts per dim = "
                      +com.donhatchsw.util.Arrays.toStringCompact(sizes)
                      +", "+nl+"  nDims = "+nDims()
                      +", "+nl+"  nStickers = "+nStickers()
                      +", "+nl+"  nGrips = "+nGrips()
                      +", "+nl+"  slicedPolytope = "+slicedPolytope.toString(true)
                      +", "+nl+"  vertsMinusStickerCenters = "+com.donhatchsw.util.Arrays.toStringNonCompact(vertsMinusStickerCenters, "    ", "    ")
                      +", "+nl+"  vertStickerCentersMinusFaceCenters = "+com.donhatchsw.util.Arrays.toStringNonCompact(vertStickerCentersMinusFaceCenters, "    ", "    ")
                      +", "+nl+"  vertFaceCenters = "+com.donhatchsw.util.Arrays.toStringNonCompact(vertFaceCenters, "    ", "    ")
                      +", "+nl+"  stickerInds = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerInds, "    ", "    ")
                      +", "+nl+"  sticker2face = "+com.donhatchsw.util.Arrays.toStringNonCompact(sticker2face, "    ", "    ")
                      +"}";
        return answer;
    } // toString


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
            return 1; // XXX fix this!
        }
        public int nStickers()
        {
            return slicedPolytope.p.facets.length;
        }
        public int nGrips()
        {
            return nStickers(); // XXX for now
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
        int length = Integer.parseInt(args[1]);
        GenericPuzzleDescription descr = new PolytopePuzzleDescription(schlafliProduct, length, progressWriter);
        System.out.println("description = "+descr);

        System.out.println("out main");
    } // main

} // class PolytopePuzzleDescription
