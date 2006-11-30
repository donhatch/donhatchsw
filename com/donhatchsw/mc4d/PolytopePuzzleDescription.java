/*
    TODO:
        - seed fill for sticker2cubie
        - store slice masks and implement checking to see if a point is in a slice mask
    BUGS:
        - need to get cut depths right
        - I don't think it's oriented correctly at end-- need to send in all planes at once so it can do that automatically with some hope of being efficient
    FIXED:
        - "{3,3}" anything gives {4,6,4,1} so it's not slicing???
        - "{3}x{} 3 gives counts  {42,54,36,1} should be {42,81,41,1} I think
        - "{4,3}" 3  gives counts {32,36,48,1} should be {56,108,54,1}
        - "{4,3}" 3, even cuts only gives counts {20,24,24,1} should be {26,48,24,1}
        - "{4,3}" 3, break after 1 gives counts {12,16,10,1} should be {12,20,10,1}
        - "{4,3}" 3, break after 1 gives counts {16,24,10,1} should be {12,20,10,1}  (failing to do some sharing?)
*/

import com.donhatchsw.util.*; // XXX get rid

class PolytopePuzzleDescription implements GenericPuzzleDescription {
    com.donhatchsw.util.CSG.SPolytope originalPolytope;
    com.donhatchsw.util.CSG.SPolytope slicedPolytope;

    float vertsMinusStickerCenters[][];
    float vertStickerCentersMinusFaceCenters[][];
    float vertFaceCenters[][];
    int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];
    int sticker2face[/*nStickers*/];
    int sticker2faceShadow[/*nStickers*/]; // so we can detect nefariousness
    int sticker2cubie[/*nStickers*/];

    int gripSymmetryOrders[/*nGrips*/];
    double gripUsefulMats[/*nGrips*/][/*nDims*/][/*nDims*/]; // weird name
    double gripSliceNormal[/*nDims*/][/*nDims*/];
    double gripSliceOffsets[]; // slice 0 is bounded by -infinity and offset[0], slice i+1 is bounded by offset[i],offset[i+1], ... slice[nSlices-1] is bounded by offset[nSlices-2]..infinity

    double stickerCentersD[][];
    FuzzyPointHashTable stickerCentersHashTable;

     static private void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }
    

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

    public PolytopePuzzleDescription(String schlafliProduct, int length,
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
        int nFaces = originalPolytope.p.facets.length;

        if (true)
        {
            // Oh, LAME!
            // Facets can appear in a different order in allElements[iDim-1]
            // from the order they appear in the facets list...
            // we will be iterating through them using the facets list,
            // so use those indices instead.
            // We fudge allElements[iDim-1] here to agree with the facets list
            // (even though we are not really supposed to do that)...
            // then every future call to allElements will return
            // the corrected array.
            CSG.Polytope allElements[][] = originalPolytope.p.getAllElements();
            for (int iFacet = 0; iFacet < nFaces; ++iFacet)
                allElements[nDims-1][iFacet] = originalPolytope.p.facets[iFacet].p;
        }

        // Mark each original face with its face index.
        // These marks will persist even aver we slice up into stickers,
        // so that will give us the sticker-to-original-face-index mapping.
        // Also mark each vertex with its vertex index... etc.
        {
            CSG.Polytope allElements[][] = originalPolytope.p.getAllElements();
            for (int iDim = 0; iDim < allElements.length; ++iDim)
            for (int iElt = 0; iElt < allElements[iDim].length; ++iElt)
                allElements[iDim][iElt].aux = new Integer(iElt);
        }

        double faceInwardNormals[][] = new double[nFaces][nDims];
        double faceOffsets[] = new double[nFaces];
        for (int iFacet = 0; iFacet < nFaces; ++iFacet)
        {
            CSG.Polytope facet = originalPolytope.p.facets[iFacet].p;
            CSG.Hyperplane plane = facet.contributingHyperplanes[0];
            VecMath.vxs(faceInwardNormals[iFacet], plane.normal, -1);
            faceOffsets[iFacet] = -plane.offset;
            Assert(faceOffsets[iFacet] < 0.);
            double invNormalLength = 1./VecMath.norm(faceInwardNormals[iFacet]);
            VecMath.vxs(faceInwardNormals[iFacet], faceInwardNormals[iFacet], invNormalLength);
            faceOffsets[iFacet] *= invNormalLength;
        }

        //
        // So we can easily find the opposite face of a given face...
        //
        CSG.Polytope faceToOppositeFace[] = new CSG.Polytope[nFaces];
        {
            FuzzyPointHashTable table = new FuzzyPointHashTable(1e-11, 1e-9, 1./512);
            for (int iFacet = 0; iFacet < nFaces; ++iFacet)
                table.put(faceInwardNormals[iFacet], originalPolytope.p.facets[iFacet].p);
            double oppositeNormalScratch[] = new double[nDims];
            System.err.print("opposites:");
            for (int iFacet = 0; iFacet < nFaces; ++iFacet)
            {
                VecMath.vxs(oppositeNormalScratch, faceInwardNormals[iFacet], -1.);
                faceToOppositeFace[iFacet] = (CSG.Polytope)table.get(oppositeNormalScratch);
                System.err.print("("+iFacet+":"+(faceToOppositeFace[iFacet]!=null ? ""+((Integer)faceToOppositeFace[iFacet].aux).intValue():"null")+")");
            }
        }

        //
        // So we can easily find all neighbor verts of a given vert...
        //
        CSG.Polytope vertToNeighborVerts[][];
        {
            CSG.Polytope allElements[][] = originalPolytope.p.getAllElements();
            CSG.Polytope verts[] = allElements[0];
            CSG.Polytope edges[] = allElements[1];
            vertToNeighborVerts = new CSG.Polytope[verts.length][0];
            for (int iEdge = 0; iEdge < edges.length; ++iEdge)
            {
                CSG.Polytope edge = edges[iEdge];
                Assert(edge.facets.length == 2);
                CSG.Polytope vert0 = edge.facets[0].p;
                CSG.Polytope vert1 = edge.facets[1].p;
                int iVert0 = ((Integer)vert0.aux).intValue();
                int iVert1 = ((Integer)vert1.aux).intValue();
                // This would be inefficient if there were a lot
                // of neighbors expected per vertex, but there aren't
                // (the expected number of neighbors is nDims,
                // since puzzles work best when every vertex
                // is a simplex).
                vertToNeighborVerts[iVert0] = (CSG.Polytope[])Arrays.append(vertToNeighborVerts[iVert0], vert1);
                vertToNeighborVerts[iVert1] = (CSG.Polytope[])Arrays.append(vertToNeighborVerts[iVert1], vert0);
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
        double faceCutOffsets[][] = new double[nFaces][];
        {
            for (int iFacet = 0; iFacet < nFaces; ++iFacet)
            {
                double fullThickness = 0.;
                {
                    CSG.Polytope someVertOnFace = originalPolytope.p.facets[iFacet].p;
                    while (someVertOnFace.dim > 0)
                        someVertOnFace = someVertOnFace.facets[0].p;
                    int iVert = ((Integer)someVertOnFace.aux).intValue();
                    for (int iNeighbor = 0; iNeighbor < vertToNeighborVerts[iVert].length; ++iNeighbor)
                    {
                        CSG.Polytope neighborVert = vertToNeighborVerts[iVert][iNeighbor];
                        double neighborOffset = VecMath.dot(faceInwardNormals[iFacet], neighborVert.getCoords());
                        double thisThickness = neighborOffset - faceOffsets[iFacet];
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
                assert(fullThickness != 0.);

                double sliceThickness = fullThickness / length;

                //System.out.println("    slice thickness "+iFacet+" = "+sliceThickness+"");

                boolean isPrismOfThisFace = Math.abs(-.5 - faceOffsets[iFacet]) > 1e-6;
                // If even length and *not* a prism of this face,
                // then the middle-most cuts will meet,
                // but the slice function can't handle that.
                // So back off a little so they don't meet,
                // so we'll get tiny invisible sliver faces there instead.
                if (length % 2 == 0
                 && !isPrismOfThisFace)
                    sliceThickness *= .1;

                int nNearCuts = length / 2; // (n-1)/2 if odd, n/2 if even
                int nFarCuts = faceToOppositeFace[iFacet]==null ? 0 :
                               length%2==0 && isPrismOfThisFace ? nNearCuts-1 :
                               nNearCuts;
                faceCutOffsets[iFacet] = new double[nNearCuts + nFarCuts];

                for (int iNearCut = 0; iNearCut < nNearCuts; ++iNearCut)
                    faceCutOffsets[iFacet][iNearCut] = faceOffsets[iFacet] + (iNearCut+1)*sliceThickness;
                for (int iFarCut = 0; iFarCut < nFarCuts; ++iFarCut)
                    faceCutOffsets[iFacet][nNearCuts+nFarCuts-1-iFarCut]
                        = -faceOffsets[iFacet] // offset of opposite face
                        - (iFarCut+1)*sliceThickness;
            }
        }

        System.out.println("face inward normals = "+Arrays.toStringCompact(faceInwardNormals));
        System.out.println("cut offsets = "+Arrays.toStringCompact(faceCutOffsets));

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
            for (int iFacet = 0; iFacet < nFaces; ++iFacet)
            {
                if (faceToOppositeFace[iFacet] != null
                 && ((Integer)faceToOppositeFace[iFacet].aux).intValue() < iFacet)
                    continue; // already saw opposite face and made the cuts
                System.out.println("REALLY doing facet "+iFacet);
                for (int iCut = 0; iCut < faceCutOffsets[iFacet].length; ++iCut)
                {
                    com.donhatchsw.util.CSG.Hyperplane cutHyperplane = new com.donhatchsw.util.CSG.Hyperplane(
                        faceInwardNormals[iFacet],
                        faceCutOffsets[iFacet][iCut]);
                    slicedPolytope = com.donhatchsw.util.CSG.sliceFacets(slicedPolytope, cutHyperplane, null);
                    if (progressWriter != null)
                    {
                        progressWriter.print("."); // one dot per cut
                        progressWriter.flush();
                    }
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





/*
        if (length % 2 == 1)
        {
            // Odd length
            int nCutsPerFace = (length-1)/2;

            this.slicedPolytope = originalPolytope;
            if (progressWriter != null)
            {
                progressWriter.print("    Slicing");
                progressWriter.flush();
            }
            for (int iFacet = 0; iFacet < originalPolytope.p.facets.length; ++iFacet)
            {
                double oneCutDepth = 2./length/10; // XXX FIX THIS-- get it right... and it's not the same for all facets! and needs to be even smaller if there are triangles around!! 2/length is right for a hypercube but not other stuff
                //if (iFacet >= 1) break; // XXX for debugging, only this number of faces
                //if (iFacet%2 == 1) continue; // XXX for debugging, only doing even numbered facets

                com.donhatchsw.util.CSG.Hyperplane faceHyperplane = originalPolytope.p.facets[iFacet].p.contributingHyperplanes[0];
                double faceNormal[] = com.donhatchsw.util.VecMath.copyvec(faceHyperplane.normal);
                double faceOffset = faceHyperplane.offset;
                // make it so normal pointing away from the origin
                // XXX this won't be necessary when we do it right by looking at the edge we are going to subdivide
                if (faceOffset < 0.)
                {
                    faceOffset *= -1.;
                    com.donhatchsw.util.VecMath.vxs(faceNormal, faceNormal, -1.);
                }

                for (int iCut = 0; iCut < nCutsPerFace; ++iCut)
                {
                    com.donhatchsw.util.CSG.Hyperplane cutHyperplane = new com.donhatchsw.util.CSG.Hyperplane(faceNormal, faceOffset - (nCutsPerFace-iCut)*oneCutDepth); // from inward to outward for efficiency, so each successive cut looks at smaller part of previous result  XXX argh, actually looks at everything anyway, need to micromanage more to get it right
                    slicedPolytope = com.donhatchsw.util.CSG.sliceFacets(slicedPolytope, cutHyperplane, null);
                    if (progressWriter != null)
                    {
                        progressWriter.print("."); // one dot per cut
                        progressWriter.flush();
                    }
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
        } // odd length
        else
        {
            // Even length
            throw new RuntimeException("can't do generic puzzles of even length yet"); // XXX
        }
*/




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
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2cubie[iSticker] = iSticker; // XXX FIX THIS! need to do a seed fill or something, stopping at slice boundaries but not at original boundaries
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
                com.donhatchsw.util.CSG.cgOfVerts(faceCentersD[iFace], originalPolytope.p.facets[iFace].p);
        }
        this.stickerCentersD = new double[nStickers][nDims];
        this.stickerCentersHashTable = new FuzzyPointHashTable(1e-11, 1e-9, 1./512);
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
            CSG.Polytope verts[] = slicedPolytope.p.getAllElements()[0];
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
                progressWriter.print("    Thinking about grips...");
                progressWriter.flush();
            }

            int nGrips = 0;
            for (int iFacet = 0; iFacet < originalPolytope.p.facets.length; ++iFacet)
            {
                com.donhatchsw.util.CSG.Polytope[][] allElementsOfCell = originalPolytope.p.facets[iFacet].p.getAllElements();
                for (int iDim = 0; iDim <= 3; ++iDim) // yes, even for cell center, which doesn't do anything
                    nGrips += allElementsOfCell[iDim].length;
            }
            this.gripSymmetryOrders = new int[nGrips];
            this.gripUsefulMats = new double[nGrips][nDims][nDims];
            int iGrip = 0;
            for (int iFacet = 0; iFacet < originalPolytope.p.facets.length; ++iFacet)
            {
                CSG.Polytope cell = originalPolytope.p.facets[iFacet].p;
                com.donhatchsw.util.CSG.Polytope[][] allElementsOfCell = cell.getAllElements();
                for (int iDim = 0; iDim <= 3; ++iDim) // XXX should we have a grip for the cell center, which doesn't do anything? maybe!
                {
                    for (int iElt = 0; iElt < allElementsOfCell[iDim].length; ++iElt)
                    {
                        CSG.Polytope elt = allElementsOfCell[iDim][iElt];
                        gripSymmetryOrders[iGrip] = CSG.calcRotationGroupOrder(
                                                originalPolytope.p, cell, elt,
                                                gripUsefulMats[iGrip]);
                        if (progressWriter != null)
                        {
                            progressWriter.print("("+iDim+":"+gripSymmetryOrders[iGrip]+")");
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
            double angle = dir * (2*Math.PI/order);
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
        public int nStickers()
        {
            return slicedPolytope.p.facets.length;
        }
        public int nGrips()
        {
            return nStickers(); // XXX for now
        }

        public float[/*nVerts*/][/*nDims*/]
            getStickerVertsAtRest(float faceShrink,
                                  float stickerShrink)
        {
            float answer[][] = new float[vertsMinusStickerCenters.length][nDims()];
            for (int i = 0; i < answer.length; ++i)
            {
                float faceCenter[] = vertFaceCenters[i];
                float stickerCenterMinusFaceCenter[] = vertStickerCentersMinusFaceCenters[i];
                float vertMinusStickerCenter[] = vertsMinusStickerCenters[i];
                float answeri[] = answer[i];
                for (int j = 0; j < answeri.length; ++j)
                    answeri[j] = (vertMinusStickerCenter[j] * stickerShrink
                                + stickerCenterMinusFaceCenter[j]) * faceShrink
                                + faceCenter[j];
            }
            return answer;
        }
        public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/]
            getStickerInds()
        {
            throw new RuntimeException("unimplemented");
        }
        public float[/*nVerts*/][/*nDims*/]
            getGripVertsAtRest(float faceShrink,
                               float stickerShrink)
        {
            throw new RuntimeException("unimplemented");
        }
        public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/]
            getGripInds()
        {
            throw new RuntimeException("unimplemented");
        }
        public float[/*nVerts*/][/*nDims*/]
            getStickerVertsPartiallyTwisted(float faceShrink,
                                            float stickerShrink,
                                            int gripIndex,
                                            int dir,
                                            float frac,
                                            int slicemask)
        {
            // Note, we purposely go through all the calculation
            // even if dir*frac is 0; we get more consistent timing that way.
            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if (gripSymmetryOrders[gripIndex] == 0)
                throw new IllegalArgumentException("getStickerVertsPartiallyTwisted called on gripIndex "+gripIndex+" which does not rotate!");
            double matD[][] = getTwistMat(gripIndex, dir, frac);
            float matF[][] = doubleToFloat(matD);

            float restVerts[][] = getStickerVertsAtRest(faceShrink, stickerShrink);
            boolean whichVertsGetMoved[] = new boolean[restVerts.length]; // false initially
            for (int iSticker = 0; iSticker < stickerCentersD.length; ++iSticker)
            {
                boolean isInSliceMask = true; // XXX fix
                if (true) throw new RuntimeException("unimplemented");
                {
                    for (int i = 0; i < stickerInds[iSticker].length; ++i)
                    for (int j = 0; j < stickerInds[iSticker][i].length; ++j)
                        whichVertsGetMoved[stickerInds[iSticker][i][j]] = true;
                }
            }

            float verts[][] = new float[restVerts.length][];
            for (int iVert = 0; iVert < verts.length; ++iVert)
            {
                if (whichVertsGetMoved[iVert])
                    verts[iVert] = VecMath.vxm(restVerts[iVert], matF);
                else
                    verts[iVert] = restVerts[iVert];
            }
            return verts;
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

            double scratchVert[] = new double[nDims()];
            double matD[][] = getTwistMat(gripIndex, dir, 1.);
            int newState[] = new int[state.length];
            for (int iSticker = 0; iSticker < state.length; ++iSticker)
            {
                boolean isInSliceMask = true; // XXX fix
                if (true) throw new RuntimeException("unimplemented");
                if (isInSliceMask)
                {
                    VecMath.vxm(scratchVert, stickerCentersD[iSticker], matD);
                    Integer whereIstickerGoes = (Integer)stickerCentersHashTable.get(scratchVert);
                    Assert(whereIstickerGoes != null);
                    newState[whereIstickerGoes.intValue()] = state[iSticker];
                }
                else
                    newState[iSticker] = state[iSticker];
            }
            return newState;
        } // applyTwistToState

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
