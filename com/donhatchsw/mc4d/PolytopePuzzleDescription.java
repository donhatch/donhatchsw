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
    float stickerVertsAtRest_faceShrink1_stickerShrink1[][];
    float stickerVertsAtRest_offsetsPerStickerShrink[][];
    float stickerVertsAtRest_offsetsPerFaceShrink[][];
    int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];

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
            progressWriter.print("Constructing polytope...");
            progressWriter.flush();
        }
        originalPolytope = com.donhatchsw.util.CSG.makeRegularStarPolytopeCrossProductFromString(schlafliProduct);
        if (progressWriter != null)
        {
            progressWriter.println(" done.");
            progressWriter.flush();
        }
        // Mark all original elements as not from a slice
        Object notFromSliceMarker = new Integer(0);
        {
            com.donhatchsw.util.CSG.Polytope[][] allElements = originalPolytope.p.getAllElements();
            for (int i = 0; i < allElements.length; ++i)
                for (int j = 0; j < allElements[i].length; ++j)
                    allElements[i][j].aux = notFromSliceMarker;
        }

        // Mark all new elements as from a slice
        Object fromSliceMarker = new Integer(1);

        if (length % 2 == 1)
        {
            // Odd length
            int nCutsPerFace = (length-1)/2;

            slicedPolytope = originalPolytope;
            if (progressWriter != null)
            {
                progressWriter.print("Slicing");
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
                    slicedPolytope = com.donhatchsw.util.CSG.sliceFacets(slicedPolytope, cutHyperplane, fromSliceMarker);
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

            int nDims = slicedPolytope.p.dim;
            // XXX make PolyCSG more general?
            if (nDims == 3)
            {
                com.donhatchsw.util.Poly slicedPoly = com.donhatchsw.util.PolyCSG.PolyFromPolytope(slicedPolytope.p);
                stickerVertsAtRest_faceShrink1_stickerShrink1 = doubleToFloat((double[][])slicedPoly.verts);

                // slicedPoly.inds is a list of faces, each of which
                // is a list of contours.  We assume there is always
                // one contour per face (i.e. no holes),
                // so we can now re-interpret the one contour per face
                // as one face per sticker.
                stickerInds = (int[][][])slicedPoly.inds;
            }
            else if (nDims == 4)
            {
                // Start by making a completely separate Poly
                // out of each sticker.  There will be no
                // vertex sharing between stickers.
                int nStickers = slicedPolytope.p.facets.length;
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

                stickerVertsAtRest_faceShrink1_stickerShrink1 = doubleToFloat((double[][])slicedPoly.verts);
                // We assume there is only 1 contour per polygon,
                // so we can flatten out the contours part.
                stickerInds = (int[][][])com.donhatchsw.util.Arrays.flatten(slicedPoly.inds, 2, 2);

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
            else
            {
                stickerVertsAtRest_faceShrink1_stickerShrink1 = new float[0][];
                stickerInds = new int[0][][];
            }


            int nVerts = stickerVertsAtRest_faceShrink1_stickerShrink1.length;
            stickerVertsAtRest_offsetsPerFaceShrink = new float[nVerts][nDims]; // XXX all zeros, for now
            stickerVertsAtRest_offsetsPerStickerShrink = new float[nVerts][nDims]; // XXX all zeros, for now
        }
        else
        {
            // Even length
            throw new RuntimeException("can't do generic puzzles of even length yet"); // XXX
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
                      +", "+nl+"  stickerVertsAtRest_faceShrink1_stickerShrink1 = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerVertsAtRest_faceShrink1_stickerShrink1, "    ", "    ")
                      +", "+nl+"  stickerVertsAtRest_offsetsPerFaceShrink = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerVertsAtRest_offsetsPerFaceShrink, "    ", "    ")
                      +", "+nl+"  stickerVertsAtRest_offsetsPerStickerShrink = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerVertsAtRest_offsetsPerStickerShrink, "    ", "    ")
                      +", "+nl+"  stickerInds = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerInds, "    ", "    ")
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
            float answer[][] = new float[stickerVertsAtRest_faceShrink1_stickerShrink1.length][nDims()];
            float oneMinusFaceShrink = 1-faceShrink;
            float oneMinusStickerShrink = 1-stickerShrink;
            for (int i = 0; i < answer.length; ++i)
            for (int j = 0; j < answer[i].length; ++j)
            {
                answer[i][j] = stickerVertsAtRest_faceShrink1_stickerShrink1[i][j]
                   - oneMinusFaceShrink*stickerVertsAtRest_offsetsPerFaceShrink[i][j]
                   - oneMinusStickerShrink*stickerVertsAtRest_offsetsPerStickerShrink[i][j];
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
