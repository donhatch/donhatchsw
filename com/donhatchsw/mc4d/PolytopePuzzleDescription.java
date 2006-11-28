/*
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

class PolytopePuzzleDescription implements GenericPuzzleDescription {
    com.donhatchsw.util.CSG.SPolytope originalPolytope;
    com.donhatchsw.util.CSG.SPolytope slicedPolytope;

    float vertsMinusStickerCenters[][];
    float vertStickerCentersMinusFaceCenters[][];
    float vertFaceCenters[][];
    int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];
    int sticker2face[/*nStickers*/];

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
        if (progressWriter != null)
        {
            progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+length+"...");
            progressWriter.print("    Constructing polytope...");
            progressWriter.flush();
        }
        originalPolytope = com.donhatchsw.util.CSG.makeRegularStarPolytopeCrossProductFromString(schlafliProduct);
        if (progressWriter != null)
        {
            progressWriter.println(" done.");
            progressWriter.flush();
        }

        // Mark each original face with its face index.
        // These marks will persist even aver we slice up into stickers,
        // so that will give us the sticker-to-original-face-index mapping.
        {
            for (int iFacet = 0; iFacet < originalPolytope.p.facets.length; ++iFacet)
                originalPolytope.p.facets[iFacet].p.aux = new Integer(iFacet);
        }

        if (length % 2 == 1)
        {
            // Odd length
            int nCutsPerFace = (length-1)/2;

            slicedPolytope = originalPolytope;
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

        int nDims = slicedPolytope.p.dim;
        int nFaces = originalPolytope.p.facets.length;
        int nStickers = slicedPolytope.p.facets.length;

        //
        // Figure out the mapping from sticker to face.
        //
        sticker2face = new int[nStickers];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                sticker2face[iSticker] = ((Integer)slicedPolytope.p.facets[iSticker].p.aux).intValue();
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
        double stickerCentersD[][] = new double[nStickers][nDims];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                com.donhatchsw.util.CSG.cgOfVerts(stickerCentersD[iSticker], slicedPolytope.p.facets[iSticker].p);
        }

        float faceCentersF[][] = doubleToFloat(faceCentersD);
        float stickerCentersMinusFaceCentersF[][] = new float[nStickers][];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                stickerCentersMinusFaceCentersF[iSticker] = doubleToFloat(com.donhatchsw.util.VecMath.vmv(stickerCentersD[iSticker], faceCentersD[sticker2face[iSticker]]));
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
            this.stickerInds = new int[0][][];
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
                for (int iDim = 0; iDim < 3; ++iDim) // XXX should we have a grip for the cell center, which doesn't do anything? maybe!
                    nGrips += allElementsOfCell[iDim].length;
            }
            int iGrip = 0;
            for (int iFacet = 0; iFacet < originalPolytope.p.facets.length; ++iFacet)
            {
                com.donhatchsw.util.CSG.Polytope[][] allElementsOfCell = originalPolytope.p.facets[iFacet].p.getAllElements();
                for (int iDim = 0; iDim < 3; ++iDim) // XXX should we have a grip for the cell center, which doesn't do anything? maybe!
                {
                    for (int iElt = 0; iElt < allElementsOfCell[iDim].length; ++iElt)
                    {
                        if (iDim == 0)
                        {
                            // symmetry is 3 if it works, or 1
                            if (progressWriter != null)
                                progressWriter.print("("+3+")");
                            // Trick to tell (this works since it's uniform):
                            // if the three faces that meet at this edge
                            // all have the same gonality, then it's 3, else 1
                            // XXX that tells the symmetry of the cell
                            // XXX about this point...
                            // XXX but might it be that the symmetry of
                            // XXX the whole puzzle is less?
                            // XXX YES!  E.g. the {3}x{4}.
                            // XXX the symmetry about a cube vert
                            // XXX is NOT 3!
                        }
                        else if (iDim == 1)
                        {
                            // symmetry is 2 if it works, or 1.
                            if (progressWriter != null)
                                progressWriter.print("("+2+")");
                            // Trick to tell (this works since it's uniform):
                            // if the two faces that meet at this edge
                            // have the same gonality, then it's 2, else 1
                            // XXX that tells the symmetry of the cell
                            // XXX about this point...
                            // XXX but might it be that the symmetry of
                            // XXX the whole puzzle is less?
                            // XXX YES!  E.g. the {3}x{4}.
                            // XXX there are some cube edges
                            // XXX with symmetry NOT 2,
                            // XXX if two diff cell neighbor types there!
                        }
                        else if (iDim == 2)
                        {
                            // symmetry is gonality, if it works, or some factor of it, at worst 1
                            int gonality = allElementsOfCell[iDim][iElt].facets.length;
                            if (progressWriter != null)
                                progressWriter.print("("+gonality+")");
                        }
                        else
                        {
                            // symmetry is 0 -- i.e. twist doesn't even make sense
                        }
                    }
                }
            }


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
           throw new RuntimeException("unimplemented");
        }
        public int[/*nStickers*/] getSticker2Face()
        {
           throw new RuntimeException("unimplemented");
        }
        public int[/*nStickers*/] getSticker2Cubie()
        {
           throw new RuntimeException("unimplemented");
        }
        public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                    int gripIndex,
                                                    int dir,
                                                    int slicemask)
        {
           throw new RuntimeException("unimplemented");
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
