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
    com.donhatchsw.util.CSG.SPolytope polytope;

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

    public PolytopePuzzleDescription(String schlafliProduct, int length)
    {
        com.donhatchsw.util.CSG.SPolytope ptope = com.donhatchsw.util.CSG.makeRegularStarPolytopeCrossProductFromString(schlafliProduct);
        if (length % 2 == 1)
        {
            // Odd length
            int nCutsPerFace = (length-1)/2;

            com.donhatchsw.util.CSG.SPolytope sliced = ptope;
            for (int iFacet = 0; iFacet < ptope.p.facets.length; ++iFacet)
            {
                double oneCutDepth = 2./length/2; // XXX FIX THIS-- get it right... and it's not the same for all facets! and needs to be smaller if there are triangles around!! 2/length is right for a hypercube but not other stuff
                //if (iFacet >= 1) break; // XXX for debugging, only this number of faces
                //if (iFacet%2 == 1) continue; // XXX for debugging, only doing even numbered facets

                com.donhatchsw.util.CSG.Hyperplane faceHyperplane = ptope.p.facets[iFacet].p.contributingHyperplanes[0];
                double faceNormal[] = com.donhatchsw.util.VecMath.copyvec(faceHyperplane.normal);
                double faceOffset = faceHyperplane.offset;
                // make it so normal pointing away from the origin
                // XXX this won't be necessary when we do it right by looking at the edge we are going to subdivide
                if (faceOffset < 0.)
                {
                    faceOffset *= -1.;
                    com.donhatchsw.util.VecMath.vxs(faceNormal, faceNormal, -1.);
                }

                System.out.println("face hyperplane = "+faceHyperplane);
                for (int iCut = 0; iCut < nCutsPerFace; ++iCut)
                {
                    com.donhatchsw.util.CSG.Hyperplane cutHyperplane = new com.donhatchsw.util.CSG.Hyperplane(faceNormal, faceOffset - (nCutsPerFace-iCut)*oneCutDepth); // from inward to outward for efficiency, so each successive cut looks at smaller part of previous result  XXX argh, actually looks at everything anyway, need to micromanage more to get it right
                    sliced = com.donhatchsw.util.CSG.sliceFacets(sliced, cutHyperplane);
                }
            }
            this.polytope = sliced;
        }
        else
        {
            // Even length
            throw new RuntimeException("can't do generic puzzles of even length yet"); // XXX
        }
    } // ctor from schlafli and length

    public String toString()
    {
        com.donhatchsw.util.CSG.Polytope[][] allElements = polytope.p.getAllElements();
        int sizes[] = new int[allElements.length];
        for (int iDim = 0; iDim < sizes.length; ++iDim)
            sizes[iDim] = allElements[iDim].length;
        return "polytope counts per dim = "+com.donhatchsw.util.Arrays.toStringCompact(sizes)+", polytope = "+polytope.toString();
    } // toString



    //
    // The rest of the methods
    // implement the GenericPuzzleDescription interface.
    //

    /**
    * Get the vertices of the geometry that gets drawn
    * (or picked when selecting a sticker rather than a grip) at rest.
    */
    public float[/*nVerts*/][/*nDims*/] getStickerVertsAtRest(float faceShrink,
                                                              float stickerShrink)
    {
       throw new RuntimeException("unimplemented");
    }
    /**
    * Get the indices (into the vertices returned by getDrawVertsAtRest()
    * or getDrawVertsPartiallyTwisted())
    * of the polygons which make up the stickers.
    */
    public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/] getStickerInds()
    {
       throw new RuntimeException("unimplemented");
    }

    /**
    * Get the vertices of the geometry to be picked
    * when selecting a grip for twisting.
    */
    public float[/*nVerts*/][/*nDims*/] getGripVertsAtRest(float faceShrink,
                                                           float stickerShrink)
    {
       throw new RuntimeException("unimplemented");
    }
    /**
    * Get the indices (into the vertices returned by getPickVertsAtRest())
    * of the geometry to be picked when selecting a grip for twisting.
    */
    public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/] getGripInds()
    {
       throw new RuntimeException("unimplemented");
    }

    /**
    * Get the vertices of the geometry that gets drawn
    * partway through a twist.
    */
    public float[/*nVerts*/][/*nDims*/] getStickerVertsPartiallyTwisted(float faceShrink,
                                                                        float stickerShrink,
                                                                        int gripIndex,
                                                                        int dir,
                                                                        float frac,
                                                                        int slicemask)
    {
       throw new RuntimeException("unimplemented");
    }

    /**
    * Get a table mapping sticker index to face index.
    * The resulting array can also be used as the initial puzzle state.
    */
    public int[/*nStickers*/] getSticker2Face()
    {
       throw new RuntimeException("unimplemented");
    }
    /**
    * Get a table mapping sticker index to cubie.
    * This can be used to highlight all the stickers on a given cubie.
    */
    public int[/*nStickers*/] getSticker2Cubie()
    {
       throw new RuntimeException("unimplemented");
    }

    /**
    * Apply a move to an array of colors (face indices)
    * representing the current puzzle state.
    */
    public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                int gripIndex,
                                                int dir,
                                                int slicemask)
    {
       throw new RuntimeException("unimplemented");
    }

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

        String schlafliProduct = args[0];
        int length = Integer.parseInt(args[1]);
        GenericPuzzleDescription descr = new PolytopePuzzleDescription(schlafliProduct, length);
        System.out.println("description = "+descr);

        System.out.println("out main");
    } // main

} // class PolytopePuzzleDescription
