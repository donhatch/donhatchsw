/**
 * Description of a generic Rubik's-cube-like puzzle,
 * in any number of dimensions
 * (although the grip concept probably doesn't make sense in higher
 * than 4 dimensions).
 *
 * Note, even derived classes will carry no scrambled state!
 * NOTE: All returned arrays are immutable!!!!!
 * (They would be const if java had a way to express that.)
 */
interface GenericPuzzleDescription {

    /**
    * Get the vertices of the geometry that gets drawn
    * (or picked when selecting a sticker rather than a grip) at rest.
    */
    public float[/*nVerts*/][/*nDims*/] getStickerVertsAtRest(float faceShrink,
                                                              float stickerShrink);
    /**
    * Get the indices (into the vertices returned by getDrawVertsAtRest()
    * or getDrawVertsPartiallyTwisted())
    * of the polygons which make up the stickers.
    */
    public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/] getStickerInds();

    /**
    * Get the vertices of the geometry to be picked
    * when selecting a grip for twisting.
    */
    public float[/*nVerts*/][/*nDims*/] getGripVertsAtRest(float faceShrink,
                                                           float stickerShrink);
    /**
    * Get the indices (into the vertices returned by getPickVertsAtRest())
    * of the geometry to be picked when selecting a grip for twisting.
    */
    public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/] getGripInds();

    /**
    * Get the vertices of the geometry that gets drawn
    * partway through a twist.
    */
    public float[/*nVerts*/][/*nDims*/] getStickerVertsPartiallyTwisted(float faceShrink,
                                                                        float stickerShrink,
                                                                        int gripIndex,
                                                                        int dir,
                                                                        float frac,
                                                                        int slicemask);

    /**
    * Get a table mapping sticker index to face index.
    * The resulting array can also be used as the initial puzzle state.
    */
    public int[/*nStickers*/] getSticker2Face();
    /**
    * Get a table mapping sticker index to cubie.
    * This can be used to highlight all the stickers on a given cubie.
    */
    public int[/*nStickers*/] getSticker2Cubie();

    /**
    * Apply a move to an array of colors (face indices)
    * representing the current puzzle state.
    */
    public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                int gripIndex,
                                                int dir,
                                                int slicemask);
} // interface GenericPuzzleDescription
