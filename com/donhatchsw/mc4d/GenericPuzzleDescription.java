package com.donhatchsw.mc4d;
/**
 * Description of a generic Rubik's-cube-like puzzle,
 * in any number of dimensions
 * (although the grip concept probably doesn't make sense in higher
 * than 4 dimensions).
 * <p>
 * Note, even derived classes will carry no scrambled state!
 * NOTE: All returned arrays are immutable!!!!!
 * (They would be const if java had a way to express that.)
 * <p>
 * In addition to the interface methods,
 * all derived classes must also define a constructor
 * that takes arguments (String puzzlePrescription, java.io.PrintWriter progressWriter);
 * this is used by GenericPuzzleFactory to deserialize a puzzle description.
 * <p>
 * (Interfaces don't seem to be able to enforce that...
 * nor can they enforce static methods, which would have worked too...
 * nor can I make this an actual class method with a dummy "this" param that accepts null,
 * since java will throw a NullPointerException on such a thing,
 * so subclasses that mess this up just won't be able to be created
 * at the factory, which kinda sucks.)
 * XXX hmm, if I make this be not actually a constructor, I can enforce it through here.
 * XXX hmm, actually I guess not... unless I give it a dummy "this"?  oh this is so stupid
 */
public interface GenericPuzzleDescription {

    /** Number of dimensions of the abstract puzzle. */
    public int nDims();
    /** Number of dimensions of the physical representation (for PolytopePuzzleDescription this is always 4; we make prisms out of smaller dimensional puzzles). */
    public int nDisplayDims();
    public int nVerts();
    public int nFaces();
    public int nCubies();
    public int nStickers();
    public int nGrips();
    /** Distance of farthest vertex from origin. */
    public float circumRadius();
    /** Distance of closest face hyperplane to origin. */
    public float inRadius();

    /**
    * Get the indices (into the vertices returned by computeVertsAndShrinkToPointsAtRest()
    * or computeVertsAndShrinkToPointsPartiallyTwisted())
    * of the polygons which make up the stickers.
    * If 4-dimensional,
    * it is guaranteed that the following form a non-degenerate
    * simplex, the sign of whose projected volume can be tested
    * for inside-out cell culling:
    *       verts[inds[iSticker][0][0]]
    *       verts[inds[iSticker][0][1]]
    *       verts[inds[iSticker][0][2]]
    *       verts[inds[iSticker][1][0]]
    */
    public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/]
        getStickerInds();

    /**
    * Get the vertices of the geometry to be picked
    * when selecting a grip for twisting.
    */
    public void computeGripVertsAtRest(float verts[/*nVerts*/][/*nDims*/],
                                       float faceShrink,
                                       float stickerShrink);
    /**
    * Get the indices (into the vertices returned by getPickVertsAtRest())
    * of the geometry to be picked when selecting a grip for twisting.
    * XXX argh, this won't work, because culling needs to be done
    * XXX on the stickers.
    * XXX so the caller needs to be able to associate the grip geometry
    * XXX with corresponding sticker geometry.  Maybe gripPolygon2Sticker?
    * XXX No, I think the way to do it is... use the sticker
    * XXX geometry for picking.  If it needs to be subdivided for picking,
    * XXX then that's the way it will be drawn.
    * XXX and then have a stickerPolygon2Grip array.
    * XXX Bleah, that will show cracks... especially when drawing outlines :-(
    */
    public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/]
        getGripInds();

    /**
    * Get the rotational symmetry order for each grip.
    * The twist angle associated with each grip
    * will be 2*pi/order.
    */
    public int[/*nGrips*/]
        getGripSymmetryOrders();

    /**
    * XXX floundering here... closest in what sense? normalized vectors on a sphere?
    * XXX to get something robust, I had to do something pretty specific to polytopes, bleah
    */
    public int getClosestGripDEPRECATED(float pickCoords[/*4*/]); // XXX should go away I think... see where it's called, only in the 2x2x2x2 case I think
    public int getClosestGrip(float polyCenter[/*4*/],
                              float stickerCenter[/*4*/]);


    /**
    * Get the unshrunk vertices and various per-sticker shrink-to points
    * of the geometry that gets drawn partway through a twist.
    * @param inGripIndex  grip of the twist
    * @param inDir        direction of the twist (1 is CCW, -1 is CW)
    * @param inSlicemask  slice mask of the twist
    * @param inFrac       fraction of the total angle (which is not the same as the fraction of the total time, if a smoothing function is used).
    * @param outVerts     output vertices before shrinking.
    * @param outStickerCenters  output sticker centers before shrinking.
    * @param outStickerShrinkToPointsOnFaceBoundaries  output alternate sticker centers; these are used instead of the real sticker centers for sticker shrinking when shrinkTowardsFaceBoundaries is true.
    * @param outPerStickerFaceCenters  output face centers.  These are per sticker rather than per face, since a face can get carved up during a twist.
    */
    public void
        computeVertsAndShrinkToPointsPartiallyTwisted(
            float outVerts[/*nVerts*/][/*nDims*/],
            float outStickerCenters[/*nStickers*/][/*nDims*/],
            float outStickerShrinkToPointsOnFaceBoundaries[/*nStickers*/][/*nDims*/],
            float outPerStickerFaceCenters[/*nStickers*/][/*nDims*/],
            int inGripIndex,
            int inDir,
            int inSlicemask,
            float inFrac);

    /**
    * Compute the vertices and shrink-to points of the geometry that gets drawn
    * (or picked when selecting a sticker rather than a grip) at rest.
    */
    public void
        computeVertsAndShrinkToPointsAtRest(
            float outVerts[/*nVerts*/][/*nDisplayDims*/],
            float outStickerCenters[/*nStickers*/][/*nDisplayDims*/],
            float outStickerShrinkToPointsOnFaceBoundaries[/*nStickers*/][/*nDisplayDims*/],
            float outPerStickerFaceCenters[/*nStickers*/][/*nDisplayDims*/]);

                                            

    /* XXX doc me */
    public double[/*nFaces*/][/*nDims*/]
        getFaceInwardNormals();
    public double[/*nFaces*/][/*nCutsThisFace*/]
        getFaceCutOffsets(); // in increasing order

    /**
    * Get a nearby point that would be a nice point to rotate to the center.
    * Nice points are vertices, edge centers, cell centers, ...
    * XXX closest in what sense? do we normalize one or the other? should we? would be the consequences?
    */
    public float[/*nDims*/] getClosestNicePointToRotateToCenter(float point[]);

    /**
    * Get a table mapping sticker index to face index.
    * The returned array should be considered immutable.
    * !!!A COPY OF!!! the returned array can also be used as the initial puzzle state,
    */
    public int[/*nStickers*/] getSticker2Face();
    /**
    * Get a table mapping sticker index to cubie.
    * This can be used to highlight all the stickers on a given cubie.
    */
    public int[/*nStickers*/] getSticker2Cubie();

    /**
    * Get a table mapping face to opposite face (if any).
    */
    public int[/*nFaces*/] getFace2OppositeFace();

    /**
    * Get a table mapping grip index to face index.
    */
    public int[/*nStickers*/] getGrip2Face();

    /**
    * Get a table mapping sticker and polygon index to grip index.
    */
    public int[/*nStickers*/][/*nPolygonsThisSticker*/]
        getStickerPoly2Grip();


    /**
    * Returns a list of pairs-of-pairs {{i,j},{k,l}}
    * such that the polygons stickerInds[i][j] and stickerInds[k][l]
    * are where the two stickers meet (when non-shrunk).
    */
    public int[][/*2*/][/*2*/] getAdjacentStickerPairs();

    /**
    * Get the face centers of the puzzle at rest.
    * The returned array should be considered immutable.
    */
    public float[/*nFaces*/][/*nDims*/] getFaceCentersAtRest();

    /**
    * Apply a move to an array of colors (face indices)
    * representing the current puzzle state.
    */
    public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                int gripIndex,
                                                int dir,
                                                int slicemask);
} // interface GenericPuzzleDescription
