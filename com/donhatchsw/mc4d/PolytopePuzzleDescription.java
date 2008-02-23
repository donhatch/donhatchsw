/*
    VERSION 4.0 RELNOTES:
    =====================
        Changes:
            - The Ctrl key no longer works to simulate the middle mouse;
              use the Alt key instead.  This is more standard,
              and the Ctrl key is now being used for other things
              ("Require Ctrl Key to Spin Drag", and centering
              arbitrary elements, both described below).
             
        This version has the following enhancements:

            - New "Require Ctrl Key to Spin Drag" preference
                If this box is unchecked (the default),
                then un-ctrled mouse actions (click, drag)
                will affect the 3d orientation
                (in addition to doing twists on the puzzle)
                and ctrled mouse actions will not.
                If the box is checked, then the opposite holds--
                un-ctrled mouse actions will *not* affect the 3d orientation,
                and ctrled mouse actions *will*,
                and furthermore ctrled mouse actions will not do twists.
                To summarize:
                                   default    "Require Ctrl Key To Spin Drag"
                             +----------------+------------------+
                   un-ctrled |    twists yes  |    twists yes    |
                             |    3drot yes   |    3drot no      |
                             +----------------+------------------+
                    ctrled   |    twists yes  |    twists no     |
                             |    3drot no    |    3drot yes     |
                             +----------------+------------------+
                Note the following implications.
                    - The default ("Don't require Ctrl Key to Spin Drag")
                      is good for casual usage-- you can easily 3d rotate,
                      and apply puzzle twists, all with the mouse
                      without touching the keyboard.  However, it has
                      the disadvantage that the mouse functions
                      are overloaded-- for example if you click to do a twist,
                      you might accidentally drag and change the puzzle's
                      orientation; conversely, if you click to stop
                      the spinning, and you happen to land on a sticker,
                      you will also get a puzzle twist that you didn't intend.
                    - "Require Ctrl Key To Spin Drag" mode allows more control
                      since the mouse functions are not overloaded--
                      you press Ctrl when you want to affect
                      the 3d orientation (which means you won't accidentally
                      twist) and you release it when you want to do twists
                      (which means you won't accidentally knock
                      the 3d orientation that you painstakingly created,
                      or stop a nice spin if you have one going).
                    - As just noted, in "Require Ctrl Key To Spin Drag"
                      mode, you can solve the puzzle while it's spinning,
                      since when you click to twist it won't stop the spin.
                      However, even if you are in the default mode,
                      you can still twist without stopping the spin
                      if you want-- simply hold down the ctrl key
                      when you click to twist.

            - "Restrict Roll" preference
               (only works for generic puzzles currently) XXX might not be true by the time I'm through
            - Friendlier interface to the 2x2x2x2 puzzle
               that lets you use the same moves as for the other puzzles,
               (rather than just corner twists).
            - You can now 4d-rotate any element
               (vertex, edge, 2d face, hyperface) of the puzzle
               to the center of the view, not just the hyperface centers.
               To 4d-rotate an arbitrary element to the center,
               ctrl-middle-click on it.  Middle-click without holding down ctrl
               rotates the entire hyperface face to the center,
               as it always did.
               (only works for generic puzzles currently)
            - Speed of twists and rotations
               have been adjusted to feel more uniform for different
               rotation amounts
               (small rotations are slower and large rotations are faster
               than before, so that the acceleration feels the same
               for all types of moves).
            - You can hold down the shift key while twisting
              to get a smooth double twist.
              (Or hold down both shift keys for a quadruple twist!)
              (XXX obsolete?  don't do it? yeah I think it's crack at this point, with the combining feature)
            - Better depth sorting of polygons
               (still doesn't always work though)
               (only works for generic puzzles currently)
            - Lots of new puzzle types available from the Puzzle menu.
               These are called "generic puzzles" and they are a work
               in progress.
        Generic puzzles have the following limitations in this release:
            - no save/load (menus are probably misleading)
            - no macros (menus are probably misleading)
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

    ISSUES:
    =======
        XXX these are done in my version... update these relnotes
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
            3. (Optional) There could be a Button (NOT a checkbox)
               called Contiguous Cubies that:
                   turns on "Stickers shrink towards face boundaries" if
                   not already on, and sets "Face Shrink" to 1.
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
        - the following don't work:
                "(1)---(1)---(0) 3(4.0)"  (truncated tet)   (messes up in slice)
                "(1)---(1)-4-(1)---(0) 3" and opposite too  (messes up in slice)
                "(1)-5-(0)---(1) 1"  assertion failed CSG.prejava(4531): k + nNormals == n
        - "(0)---(1)-4-(1)---(0) 3(4.0)"  twists wrong thing
        - "(1)---(1)-4-(0)---(0) 3" twists wrong thing
        - "(0)---(1)-4-(1)---(1) 3(4.0)" twists wrong thing
        - "(0)---(1)--(1)---(0) 3(4.0)" twists are backwards!!!
        - "(1)---(1)---(0) 3" cuts are all random and messed up!

        - truncated hypercube, both 4d and 3d sticker shrink move stuff off center, so twisting makes it jump
        - why no package help in the javadoc any more??
        - undo tree's colors are wrong!
        - ctrl-c in undo window quits program
        - "restrict roll" on, set it spinning, reset 3d rotation, it ends up weird
        - contiguous cubies doesn't do anything sensible in Melinda's
        - cascading menus don't cascade well, see if anything I can do
        - need good help describing all the controls, in the main help part
        - need to get javacpp fixed and usable before I ship this  (partially fixed now, still lame on jikes output)
        - hotkeys don't work from java 1.6??
        - clicking on the < or > on side of the scrollbars only take about every other time
        - twist speed of generic 2x in melinda's is way too fast
        - gratuitous undo tree animation is really slow and postpones puzzle animation!?
        - status bar and puzzle prescription bar
        - implement expert control panel
            - num paints should go under it, probably... maybe (I like seeing it from very start though)
        - close a view window that's in sync with another view window...
           the remaining one doesn't update any more :-(
        - shared view/cloned puzzle state restrict roll doesn't work right
            -- do I really want shared view?  seems like a weird concept
        - puzzle change or load makes undo tree viewer go stale...
           well that's expected, but should maybe close the window if no more views
           of it?  or have a "launch view" from the undo tree window?
           Oh no wait, the undo tree viewer should maintain focus
           on a particular view window's animation squirrel!
           Its window title should reflect that!
        - label each window, e.g.
                View/controller windows:
                    model 0 view 0
                    model 0 view 1
                    model 0 view 2

                    model 1 view 3
                    model 1 view 4
                Undo tree window:
                    if I can get all the views in the same undo tree window,
                    then just "model 0" or whatever
                    NO, needs to be specific to a view-- 
                    different squirrels in the same tree see the tree differently!
                    but yes, should show all the squirrels in that tree.
        - should there be an overall list of all the models and views,
            that the user can manually attach stuff to,
            and also maybe make different views follow different other views?
            and also maybe share control panels (view params)?

        - doesn't start spinning easily enough when I let go
        - Frame Picture assumes window is 502x485-- bogus!
        - get antialiasing's notion of "still" right, then turn it on by default
        - resetting 4d rotation or rotation should do it smoothly, otherwise it's jarring and disorienting
        - really need to pick from the rest frame, not the intermediate twist frame,
          in case of double-clicks... ARGH but the rest frame will lag behind
          if we are spin dragging!!! fuck fuck fuck... okay maybe the most recent non-twisting one, regardless of whether rotating or not? maybe
        - in the 2x2x2x2, rotate-arbitrary-element-to-center should rotate a grip to center, not a sticker
        - XXX why isn't this working, when clicking when spinDragRequiresCtrl is on XXX wtf was I talking about
        - side of prefs menu cut off XXX wtf was I talking about... melinda's?
        - truncated tet is picking inconsistent slices!
        - progress meter on the slice-- just notice when it's taking a long time, and start spewing percentage  (started doing this, need to make it nicer)
        - {5}x{5} 2 has sliver polygons-- I think the isPrismOfThisFace
          hack isn't adequate.  Also it doesnt work for {5}x{} (but that's 3d).
          I think I need to remove the slivers after the fact instead.
          OH hmm... the slivers are kinda cool because they are
          rotation handles!  Think about this... maybe draw them smaller
          and white, or something!

        - scale doesn't quite match original
           - oh! and the effect of viewScale2d is getting squared! see "Frame Picture" code
        - need more colors!
        - sometimes exception during picking trying to access too big
          an index right after switching to a smaller puzzle (e.g.
          pentprismprism to hypercube)
        - try to change the puzzle type while it's twisting, it goes into
          an infinite exception loop I think
        - if in ctrl-to-spindrag mode, shouldn't hightlight sticker
          when ctrl key is down... oh but fooey, that's still relevant
          when using ctrl for rotate-arbitrary-element-to-center.
        - length 1: polygons should be grips, I think
        - 3d: really shouldn't do rotates except on face center sticker, it's confusing...
          actually edge clicks that perfectly line up with the face center sticker are okay
        - float slider with range 0..1 is mostly right but shows 0.0060, should be 0.006
        - args to MC4DViewApplet need to have clear error and usage message
            including form of the puzzle description,
            and probably if they give <puzzleDescription> it should get changed
            into puzzleDescription=<puzzleDescription>

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
            - should mention Johnson numbers and short names where applicable
        MISC:
            - checkbox "Auto 2d scale"
                When checked, 2d view scale moves automatically
                combined with other params
                Hmm, could have another slider with it...
                "2d view fraction"; scale2d and frac2d move together,
                normally the other sliders affect frac2d
                while leaving scale2d alone...
                but if auto 2d scale is on, they affect scale2d
                while leaving frac2d alone.
            - 'frame' button? it should be on the view window, not the control panel,
                since that is the one wart that is preventing multiple
                view windows from sharing a control panel
            - the cool rotate-arbitrary-element-to-center thing
               should be undoable... somehow
               - preference whether that should be treated as an undo move?
            - faceExplode / stickerExplode sliders?
               I'm really confused on what the relationship is
               between those and viewScale, faceShrink, stickerShrink
               Maybe could have a mode checkbox, that toggles
               between a shrink notion and an explode notion
            - history compression
            - highlight should highlight in all views that share the controller
               squirrel-- that would be a good way to get one's bearings
            - in control panel, checkbox for frontfaces or backfaces
            - I'd sure like to be able to set it spinning more gently
                (maybe controls to speed up / slow down spin when it's spinning?)

        AWT/APPLET/GUI LAYOUT:
            - MyMenuBar menus don't pop down nicely when others opened


        HIGHLIGHT BY GRIP:
            - 2x shows cracks :-(
            - 2x outlines shows outlines on the cracks, too

        POLYTOPE STUFF:
            - it's not oriented correctly at the end after slicing-- so I had
               to make orientDeep public and have the caller call it-- lame!
            -  need to send in all planes at once so it can do that automatically
               with some hope of being efficient

        NON-IMMEDIATE:
            - Grand Antiprism (there's a menu item for it)
            - 3 level cascading menus for {3..12}x{3..12}?
            - nframes should ALWAYS be odd!  even means sometimes
                we get flat faces!  (XXX is this true any more?)
            - ooh, make more slices proportionally slower, would feel more massive!
            - completely general solve?
            - general uniform polytopes! yeah!
            - make slicing faster-- for humongous polytopes, only need to 
                look at neighbor facets (and slices thereof) and no farther,
                that should cut stuff down by a factor of 100 maybe
            - hyperlinks to wikipedia or dinogeorge?

        PIE IN THE SKY:
            - ooh, solve should add stuff to undo queue as it goes, while animation lags behind.
               maybe.
            - ooh, stereo
            - named sets of settings that can be saved and restored
            - copy and paste view params or sets of them
            - mode in which they manipulate the puzzle face like a trackball?
                hmm... they fling it, and it gravitates ?

            - be able to copy one or several viewing params from one view to another-- and/or tie certain ones together!  when params are tied together, hovering over the param control in one view control panel should make it light up everywhere! yeah!
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
                on faces that are very close to flat.  I think this would
                look beautiful, especially on the big 120-cell-based puzzles
            - ha, for the {5}x{4}, it could be fudged so the cubical facets
                behave like they have full symmetry-- it would allow stickers
                to slide off of the pentprism face and onto a cube face.
                In general this will make the symmetry of a twist
                be dependent on the symmetry of the face,
                which can be more than the symmetry of the whole puzzle.
            - what should be highlighted is not the sticker, but everything
                that maps to the same grip as what the mouse is over.
                So for the 3x it should be the sticker like it is now,
                for the 2x and lower dim polygons it should be the polygon,
                for 4x it should be the whole panel of stickers
                that map to the same grip.
                Definitely in 3d, if there is a center tile,
                that is the only one that should map to a grip
                and that is the only one that should light up
            - since puzzle descriptions are immutable
                and functions of only the prescription,
                it would be nice if it could be automatically
                shared... store it in a hash table...
                but we'd need to be able to release when no one
                has a ref to it any more... how to detect this?
                Hmm, maybe it's a reference queue:
                    http://java.sun.com/developer/technicalArticles/ALT/RefObj/
                describes an app that uses it for caching
                    http://builder.com.com/5100-6386-1049546.html
                Ah- I think it's just using a WeakHashMap!
                Hmm, what if a key is in two WeakHashMaps
                and nowhere else-- is the garbage collector
                smart enough to reclaim it?
                And, see:
                    http://www.cs.wisc.edu/~cs368-1/JavaTutorial/jdk1.2/api/java/util/WeakHashMap.html
                warns that the value should not strongly
                refer to the key!  So when the key is a string,
                it should do a new String(string).
*/

package com.donhatchsw.mc4d;
import com.donhatchsw.util.*; // XXX get rid... at least get more specific

public class PolytopePuzzleDescription implements GenericPuzzleDescription {

    private String prescription; // what was originally passed to the constructor. we are an immutable object that is completely a function of this string, so an identical clone of ourself can be constructed using this string in any future lifetime.

    private com.donhatchsw.util.CSG.SPolytope originalPolytope;
    private com.donhatchsw.util.CSG.SPolytope slicedPolytope;

    private int _nDisplayDims = 4; // never tried anything else, it will probably crash
    private float _circumRadius;
    private float _inRadius;
    private int _nCubies;

    private float vertsF[/*nVerts*/][/*nDisplayDims*/];
    private int stickerInds[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/];

    private float faceCentersF[/*nFaces*/][/*nDisplayDims*/];

    private int adjacentStickerPairs[][/*2*/][/*2*/];
    private int face2OppositeFace[/*nFaces*/];
    private int sticker2face[/*nStickers*/];
    private int sticker2faceShadow[/*nStickers*/]; // so we can detect nefariousness
    private int sticker2cubie[/*nStickers*/];

    private float gripDirsF[/*nGrips*/][];
    private float gripOffsF[/*nGrips*/][];
    private int grip2face[/*nGrips*/];
    private int gripSymmetryOrders[/*nGrips*/];
    private double gripUsefulMats[/*nGrips*/][/*nDims*/][/*nDims*/]; // weird name
    private int stickerPoly2Grip[/*nStickers*/][/*nPolygonsThisSticker*/];

    private double faceInwardNormals[/*nFaces*/][/*nDims*/];
    private double faceCutOffsets[/*nFaces*/][/*nCutsThisFace*/]; // slice 0 is bounded by -infinity and offset[0], slice i+1 is bounded by offset[i],offset[i+1], ... slice[nSlices-1] is bounded by offset[nSlices-2]..infinity

    private float nicePointsToRotateToCenter[][];

    private double stickerCentersD[][]; // for very accurate which-slice determination
    private float stickerCentersF[][]; // for just shoving through display pipeline
    private float stickerAltCentersF[][]; // alternate sticker shrink-to points, on face boundary
    private FuzzyPointHashTable stickerCentersHashTable;

    private static void Assert(boolean condition) { if (!condition) throw new Error("Assertion failed"); }
    private static void Assumpt(boolean condition) { if (!condition) throw new Error("Assumption failed"); }
    

    /**
    * The constructor that is required by the factory.
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
    public PolytopePuzzleDescription(String prescription, java.io.PrintWriter progressWriter)
    {
        prescription = com.donhatchsw.compat.regex.replaceAll(prescription,
                                                              "Grand Antiprism",
                                                              "Grand_Antiprism");

        //com.donhatchsw.compat.regex.verboseLevel = 2;
        com.donhatchsw.compat.regex.Matcher matcher =
        com.donhatchsw.compat.regex.Pattern.compile(
            "\\s*([^ ]+)\\s+(\\d+)(\\((.*)\\))?"
        ).matcher(prescription);
        if (!matcher.matches())
            throw new IllegalArgumentException("PolytopePuzzleDescription didn't understand prescription string "+com.donhatchsw.util.Arrays.toStringCompact(prescription)+"");

        String schlafliProductString = matcher.group(1);
        String intLengthString = matcher.group(2);
        String doubleLengthString = matcher.group(4);

        int intLength = Integer.parseInt(intLengthString);
        double doubleLength = doubleLengthString != null ? Double.valueOf(doubleLengthString).doubleValue() : (double)intLength; // XXX should catch parse error and throw illegal arg exception

        init(schlafliProductString, intLength, doubleLength, progressWriter);
        this.prescription = prescription;
    } // ctor that takes just a string

    private void init(String schlafliProduct,
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
                progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+intLength+"...");
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
            FuzzyPointHashTable table = new FuzzyPointHashTable(1e-9, 1e-8, 1./64);  // 1e-9, 1e-8, 1/128 made something hit a wall on the omnitruncated 120cell
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

                boolean isPrismOfThisFace = Math.abs(-1. - faceOffsets[iFace]) < 1e-6;

                double sliceThickness = fullThickness / doubleLength;

                //System.out.println("    slice thickness "+iFace+" = "+sliceThickness+"");

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

        // There are many different inputs that produce the 2x2x2x2,
        // so take a guess based on cut depth and element counts
        boolean itsProbablyThe2 = nDims==4
                               && doubleLength == 2. // XXX make fuzzy?
                               && originalElements[0].length == 16
                               && originalElements[1].length == 32
                               && originalElements[2].length == 24
                               && originalElements[3].length == 8;
        boolean doFurtherCuts = itsProbablyThe2;

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
            if (progressWriter != null)
            {
                progressWriter.print("("+nTotalCuts+" cuts)");
                progressWriter.flush();
            }

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
                    Object auxOfCut = new CutInfo(iFace,iCut);
                    slicedPolytope = com.donhatchsw.util.CSG.sliceElements(slicedPolytope, slicedPolytope.p.dim-1, cutHyperplane, auxOfCut);
                    iTotalCut++;
                    if (progressWriter != null)
                    {
                        progressWriter.print("."); // one dot per cut

                        // We know we are doing an O(n^2) algorithm
                        // so our actual progress fraction is proportional to
                        // the square of the apparent fraction of items done.
                        if ((nTotalCuts-iTotalCut)%10 == 0)
                        {
                            progressWriter.print("("+com.donhatchsw.compat.Format.sprintf("%.2g", (double)100*iTotalCut*iTotalCut/nTotalCuts/nTotalCuts)+"%)");
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

            //
            // Have to compute the shrink-to points here
            // before doing further cuts,
            // since it assumes there will be at most two ridges
            // per sticker from a given face...
            //
            // Calculate the alternate sticker centers;
            // intuitively, these are the shrink-to points
            // on the face boundaries.
            //
            {
                if (progressWriter != null)
                {
                    progressWriter.print("    Computing alternate sticker shrink-to points on face boundaries... ");
                    progressWriter.flush();
                }
                this.stickerAltCentersF = computeStickerAltCentersF(
                                              slicedPolytope,
                                              face2OppositeFace,
                                              intLength,
                                              doubleLength);
                if (progressWriter != null)
                {
                    progressWriter.println(" done.");
                    progressWriter.flush();
                }
            }
            if (doFurtherCuts)
            {
                {
                    // ARGH! The further slicing is going to give a different
                    // order of stickers... can't have that.
                    // Temporarily mark each sticker with its current index;
                    // we will use these marks to re-permute stickerAltCentersF
                    // afterwards.
                    CSG.Polytope stickers[] = slicedPolytope.p.getAllElements()[nDims-1];
                    for (int iSticker = 0; iSticker < stickers.length; ++iSticker)
                        stickers[iSticker].pushAux(new Integer(iSticker));
                }

                if (progressWriter != null)
                {
                    progressWriter.print("    Further slicing for grips("+slicedPolytope.p.getAllElements()[2].length+" polygons)");
                    progressWriter.flush();
                }
                for (int iFace = 0; iFace < nFaces; ++iFace)
                {
                    com.donhatchsw.util.CSG.Hyperplane cutHyperplane = new com.donhatchsw.util.CSG.Hyperplane(
                        faceInwardNormals[iFace],
                        (faceOffsets[iFace]+faceCutOffsets[iFace][0])/2.);
                    Object auxOfCut = null; // note this should not mess up the showFurtherCuts thing, since we are now dividing the ridges of the stickers (e.g. the polygons, in the usual 4d case) so the divided ridges themselves will still have an aux... it's the peaks (i.e. nDims-3 dimensional elements, i.e. edges in the usual case) that will get nulls for auxes, and that's fine
                    slicedPolytope = com.donhatchsw.util.CSG.sliceElements(slicedPolytope, slicedPolytope.p.dim-2, cutHyperplane, auxOfCut);
                    if (progressWriter != null)
                    {
                        progressWriter.print("."); // one dot per cut
                        progressWriter.flush();
                    }
                }
                if (progressWriter != null)
                {
                    progressWriter.println(" done ("+slicedPolytope.p.getAllElements()[2].length+" polygons).");
                    progressWriter.flush();
                }

                {
                    // Permute newStickerAltCentersF
                    // and pop each sticker's original index
                    // from its aux stack.
                    CSG.Polytope stickers[] = slicedPolytope.p.getAllElements()[nDims-1];
                    float oldStickerAltCentersF[][] = this.stickerAltCentersF;
                    float newStickerAltCentersF[][] = new float[stickers.length][];
                    for (int iSticker = 0; iSticker < stickers.length; ++iSticker)
                        newStickerAltCentersF[iSticker] = oldStickerAltCentersF[((Integer)stickers[iSticker].popAux()).intValue()];
                    this.stickerAltCentersF = newStickerAltCentersF;
                }



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
                // ridge.aux is now either an Integer (the index
                // of the original ridge it was a part of)
                // or a CutInfo (if it was created by a cut).
                // XXX hey waitaminute, if there were further cuts for grips, there should be some nulls
                Assert(ridge.aux != null);
                boolean ridgeIsFromOriginal = (ridge.aux instanceof Integer);
                if (ridgeIsFromOriginal) // if it's not from a cut
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

        //
        // Find the face centers and sticker centers.
        // The center of mass of the vertices is probably
        // as good as anything, for this
        // (when we get shrinking right, it won't
        // actually use centers, I don't think)
        //
        double faceCentersD[][] = new double[nFaces][nDims];
        {
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
            this.faceCentersF = VecMath.doubleToFloat(faceCentersD);
            this.stickerCentersF = VecMath.doubleToFloat(stickerCentersD);
        }

        //
        // PolyFromPolytope doesn't seem to like the fact that
        // some elements have an aux and some don't... so clear all the vertex
        // auxs.
        // (Actually now they all have aux.)
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
        if (nDims <= _nDisplayDims) // if 4d or less
        {
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
                    nVerts += stickers[iSticker].getAllElements()[0].length
                            * (1<<(_nDisplayDims-nDims)); // cross with a segment or square if necessary

                restVerts = new double[nVerts][_nDisplayDims];
                this.stickerInds = new int[nStickers][][];

                nVerts = 0; // reset, count again
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    CSG.Polytope sticker = stickers[iSticker];
                    CSG.Polytope sticker4d = sticker;
                    if (nDims < _nDisplayDims)
                    {
                        double padRadius = .1;
                        //double padRadius = .2;
                        //double padRadius = .25;
                        sticker4d = CSG.cross(new CSG.SPolytope(0,1,sticker),
                                              CSG.makeHypercube(new double[_nDisplayDims-nDims], padRadius)).p;
                        CSG.Polytope stickerVerts4d[] = sticker4d.getAllElements()[0];
                        for (int iVert = 0; iVert < stickerVerts4d.length; ++iVert)
                        {
                            stickerVerts4d[iVert].aux = new iVertAux(-1, stickerVerts4d[iVert].aux);
                            stickerVerts4d[iVert].getCoords()[3] *= -1; // XXX FUDGE-- and this is not really legal... should do this afterwards
                        }
                    }
                    // XXX note, we MUST step through the polys in the order in which they appear in getAllElements, NOT the order in which they appear in the facets list.  however, we need to get the sign from the facets list!
                    CSG.Polytope polysThisSticker[] = sticker4d.getAllElements()[2];
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

                        // Figure out this polygon's sign in the sticker4d.
                        // Since we are iterating through sticker4d.allElements()[nDims-1] instead of through sticker4d's facet list (because we need that ordering),
                        // we don't have access to the sign directly.
                        // XXX bleah, this search sucks, should be a way to fast query this!
                        int indexOfPolyInStickersFacets = 0;
                        while (sticker4d.facets[indexOfPolyInStickersFacets].p != polygon)
                            indexOfPolyInStickersFacets++;
                        if (sticker4d.facets[indexOfPolyInStickersFacets].sign == -1)
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
            // There is a subtlety in the case when the sticker polygons
            // are further carved up into grips... in that case
            // we have to do further checking to make sure we don't choose
            // [1] in the same plane as [0].
            //
            {
                for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker)
                {
                    if (doFurtherCuts)
                    {
                        // Find one that's not adjacent to [0] at all.
                        int polys[][] = stickerInds[iSticker];
                        int iPoly = 0;
                        int jPoly;
                        for (jPoly = 1; jPoly < polys.length; ++jPoly)
                        {
                            boolean incident = false; // until proven true
                            for (int i = 0; i < polys[iPoly].length; ++i)
                            {
                                for (int j = 0; j < polys[jPoly].length; ++j)
                                    if (polys[iPoly][i] == polys[jPoly][j])
                                    {
                                        incident = true;
                                        break;
                                    }
                                if (incident)
                                    break;
                            }
                            if (!incident)
                            {
                                com.donhatchsw.util.Arrays.swap(polys, 1,
                                                                polys, jPoly);
                                break;
                            }
                        }
                        Assert(jPoly < polys.length); // had to find one
                    }

                    int polygon0[] = stickerInds[iSticker][0];
                    int polygon1[] = stickerInds[iSticker][1];
                    int i;
                    for (i = 0; i < polygon1.length; ++i)
                    {
                        int j;
                        for (j = 0; j < polygon0.length; ++j)
                        {
                            if (polygon1[i] == polygon0[j])
                                break; // this i is no good, it's on polygon0
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
            if (nDims == 4) // XXX need to figure this out for nDims==3 too!
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
            else
            {
                // XXX stopgap for now
                //this.adjacentStickerPairs = new int[0][2][];
            }
        }
        else // nDims >= 5
        {
            // Make a vertex array of the right size,
            // just so nVerts() will return something sane for curiosity
            int nVerts = 0;
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                nVerts += stickers[iSticker].getAllElements()[0].length;
            restVerts = new double[nVerts][_nDisplayDims]; // zeros
            this.stickerInds = new int[nStickers][0][];
        }

        // Expand out any arrays we have
        // from nDims to 4 dims
        for (int iPad = 0; iPad < _nDisplayDims-nDims; ++iPad)
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                stickerCentersD[iSticker] = (double[])com.donhatchsw.util.Arrays.append(stickerCentersD[iSticker], 0.);
                stickerCentersF[iSticker] = (float[])com.donhatchsw.util.Arrays.append(stickerCentersF[iSticker], 0.f);
                stickerAltCentersF[iSticker] = (float[])com.donhatchsw.util.Arrays.append(stickerAltCentersF[iSticker], 0.f);
            }
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                faceCentersF[iFace] = (float[])com.donhatchsw.util.Arrays.append(faceCentersF[iFace], 0.f);
                faceInwardNormals[iFace] = (double[])com.donhatchsw.util.Arrays.append(faceInwardNormals[iFace], 0.f);
            }
        }

        this.vertsF = VecMath.doubleToFloat(restVerts);


        //
        // Now think about the twist grips.
        // There will be one grip at each vertex,edge,face center
        // of the original polytope (if 3d)
        // or of each cell of the original polytope (if 4d).
        // XXX woops, I'm retarded, 3d doesn't have that...
        // XXX but actually it wouldn't hurt, could just make that
        // XXX rotate the whole puzzle.
        //
        if (nDims == 4 && intLength == 1)
        {
            // Don't bother with grips for now, it's taking too long
            // for the big ones
            int nGrips = 0;
            this.gripDirsF = new float[nGrips][];
            this.gripOffsF = new float[nGrips][];
            this.gripSymmetryOrders = new int[nGrips];
            this.gripUsefulMats = new double[nGrips][nDims][nDims];
            this.grip2face = new int[nGrips];
        }
        else
        {
            if (progressWriter != null)
            {
                progressWriter.print("    Thinking about possible twists...");
                progressWriter.flush();
            }
            if (nDims <= 4)
            {
                boolean doTheOddFaceIn3dThing = true;
                int nGrips = 0;
                for (int iFace = 0; iFace < nFaces; ++iFace)
                {
                    com.donhatchsw.util.CSG.Polytope[][] allElementsOfFace = originalFaces[iFace].getAllElements();
                    if (nDims == 4)
                        for (int iDim = 0; iDim <= 3; ++iDim) // yes, even for cell center, which doesn't do anything
                            nGrips += allElementsOfFace[iDim].length;
                    else if (nDims ==3)
                    {
                        nGrips += 2;
                        nGrips += allElementsOfFace[1].length;
                        if (doTheOddFaceIn3dThing)
                        {
                            if (allElementsOfFace[1].length % 2 == 1)
                                nGrips += allElementsOfFace[1].length;
                        }
                    }
                    else  if (nDims == 2)
                    {
                        nGrips += 6;
                    }
                }
                this.gripDirsF = new float[nGrips][];
                this.gripOffsF = new float[nGrips][];
                this.gripSymmetryOrders = new int[nGrips];
                this.gripUsefulMats = new double[nGrips][_nDisplayDims][_nDisplayDims];
                this.grip2face = new int[nGrips];
                {
                    CSG.SPolytope padHypercube = nDims < 4 ? CSG.makeHypercube(4-nDims) : null;
                    int iGrip = 0;
                    for (int iFace = 0; iFace < nFaces; ++iFace)
                    {
                        CSG.Polytope face = originalFaces[iFace];
                        if (padHypercube != null)
                            face = CSG.cross(new CSG.SPolytope(0,1,face), padHypercube).p;
                        com.donhatchsw.util.CSG.Polytope[][] allElementsOfFace = face.getAllElements();
                        int minDim = nDims==4 ? 0 : 2;
                        int maxDim = nDims==4 ? 3 : 2; // yes, even for cell center, which doesn't do anything
                        int allIncidencesThisFace[][][][] = face.getAllIncidences();
                        for (int iDim = minDim; iDim <= maxDim; ++iDim)
                        {
                            for (int iElt = 0; iElt < allElementsOfFace[iDim].length; ++iElt)
                            {
                                CSG.Polytope elt = allElementsOfFace[iDim][iElt];
                                VecMath.copyvec(gripUsefulMats[iGrip][0], faceCentersD[iFace]);
                                CSG.cgOfVerts(gripUsefulMats[iGrip][1], elt);
                                VecMath.vmv(gripUsefulMats[iGrip][1],
                                            gripUsefulMats[iGrip][1],
                                            gripUsefulMats[iGrip][0]);
                                this.gripDirsF[iGrip] = VecMath.doubleToFloat(gripUsefulMats[iGrip][0]);
                                this.gripOffsF[iGrip] = VecMath.doubleToFloat(gripUsefulMats[iGrip][1]);
                                if (VecMath.normsqrd(this.gripOffsF[iGrip]) <= 1e-4*1e-4)
                                {
                                    // This happens for the center sticker of a face.
                                    // Make it a "can't twist that" sticker.
                                    VecMath.zeromat(gripUsefulMats[iGrip]);
                                    this.gripSymmetryOrders[iGrip] = 0;
                                }
                                else
                                {
                                    VecMath.extendAndGramSchmidt(2,4,
                                                                 gripUsefulMats[iGrip],
                                                                 gripUsefulMats[iGrip]);

                                    int maxOrder = (nDims==4 ? iDim==0 ? allIncidencesThisFace[0][iElt][1].length :
                                                               iDim==1 ? 2 :
                                                               iDim==2 ? allIncidencesThisFace[2][iElt][1].length :
                                                               iDim==3 ? 0 : -1 :
                                                    nDims==3 ? originalFaces[iFace].facets.length%2==0 ? originalFaces[iFace].facets.length : 2*originalFaces[iFace].facets.length : // not the proxy face!  it will be either the face gonality or 2... would be more efficient to use lcm for maxOrder, but this is 3d so whatever, the whole thing is small
                                                    nDims==2 ? 4 : -1);

                                    //System.out.println("maxOrder = "+maxOrder);
                                    this.gripSymmetryOrders[iGrip] = CSG.calcRotationGroupOrder(
                                                                           originalPolytope.p,
                                                                           maxOrder,
                                                                           gripUsefulMats[iGrip]);
                                }
                                // Make sure dirs and offs are normalized
                                this.gripDirsF[iGrip] = VecMath.normalize(this.gripDirsF[iGrip]);
                                this.gripOffsF[iGrip] = VecMath.normalize(this.gripOffsF[iGrip]);
                                grip2face[iGrip] = iFace;
                                //System.out.println("iGrip = "+iGrip);
                                //System.out.println("this.gripSymmetryOrders["+iGrip+"] = "+VecMath.toString(gripUsefulMats[iGrip]));

                                iGrip++;
                                if (doTheOddFaceIn3dThing)
                                {
                                    if (nDims==3 && originalFaces[iFace].facets.length%2 == 1 && this.gripSymmetryOrders[iGrip-1] == 2)
                                    {
                                        // It's an edge of an odd polygon face in 3d...
                                        // need the opposite edge too, for adjacent tiles facing it the opposite way
                                        this.gripSymmetryOrders[iGrip] = this.gripSymmetryOrders[iGrip-1];
                                        this.gripDirsF[iGrip] = this.gripDirsF[iGrip-1];
                                        this.gripOffsF[iGrip] = VecMath.sxv(-1.f, this.gripOffsF[iGrip-1]);
                                        this.grip2face[iGrip] = this.grip2face[iGrip-1];
                                        this.gripUsefulMats[iGrip] = new double[][] {
                                            this.gripUsefulMats[iGrip-1][1],
                                            this.gripUsefulMats[iGrip-1][0],
                                            this.gripUsefulMats[iGrip-1][3],
                                            this.gripUsefulMats[iGrip-1][2],
                                        };
                                        iGrip++;
                                    }
                                }
                            }
                        }
                    }
                    //System.out.println("nGrips = "+nGrips);
                    //System.out.println("iGrip = "+iGrip);
                    Assert(iGrip == nGrips);
                    //System.out.println("this.gripSymmetryOrders = "+com.donhatchsw.util.Arrays.toStringCompact(this.gripSymmetryOrders));
                }

                if (doFurtherCuts)
                {
                    // Precompute sticker-and-polygon-to-face.
                    this.stickerPoly2Grip = new int[nStickers][];
                    for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                    {
                        int nPolysThisSticker = stickerInds[iSticker].length;
                        stickerPoly2Grip[iSticker] = new int[nPolysThisSticker];
                        for (int iPolyThisSticker = 0; iPolyThisSticker < nPolysThisSticker; ++iPolyThisSticker)
                        {
                            float stickerCenter[] = VecMath.doubleToFloat(stickerCentersD[iSticker]);
                            float polyCenter[] = VecMath.doubleToFloat(VecMath.averageIndexed(stickerInds[iSticker][iPolyThisSticker], restVerts));
                            // So that it doesn't get confused and get
                            // the wrong face...
                            VecMath.lerp(polyCenter, polyCenter, stickerCenter, .01f);
                            int iGrip = getClosestGrip(polyCenter);
                            // Don't highlight the one that's going to say "Can't twist that"...
                            // XXX actually we should, if rotate-arbitrary-elements-to-center is on... maybe
                            if (iGrip != -1 && gripSymmetryOrders[iGrip] == 0)
                                iGrip = -1;

                            stickerPoly2Grip[iSticker][iPolyThisSticker] = iGrip;

                            //System.out.println("stickerPoly2Grip["+iSticker+"]["+iPolyThisSticker+"] = "+stickerPoly2Grip[iSticker][iPolyThisSticker]);
                        }
                    }
                }
            }
            else
            {
                // not thinking very hard
                this.grip2face = new int[0];
            }
            if (progressWriter != null)
            {
                progressWriter.print(" ("+this.grip2face.length+" grips)");
                progressWriter.println(" done.");
                progressWriter.flush();
            }
        } // intLength > 1

        //
        // Select points worthy of being rotated to the center (-W axis).
        //
        {
            int nNicePoints = 0;
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
                nNicePoints += originalElements[iDim].length;
            this.nicePointsToRotateToCenter = new float[nNicePoints][_nDisplayDims];
            double eltCenter[] = new double[nDims]; // in original dimension
            int iNicePoint = 0;
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
            for (int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
            {
                com.donhatchsw.util.CSG.cgOfVerts(eltCenter, originalElements[iDim][iElt]);
                VecMath.copyvec(nicePointsToRotateToCenter[iNicePoint++],
                                VecMath.doubleToFloat(eltCenter)); // XXX lame way to do this
            }
            Assert(iNicePoint == nNicePoints);
        }


        if (progressWriter != null)
        {
            progressWriter.println("Done.");
            progressWriter.flush();
        }
    } // ctor from schlafli and length

    // XXX figure out a good public interface for something that shows stats
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

                      +", "+nl+"  stickerInds = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerInds, "    ", "    ")
                      +", "+nl+"  sticker2face = "+com.donhatchsw.util.Arrays.toStringNonCompact(sticker2face, "    ", "    ");
        }
        answer += "}";
        return answer;
    } // toString

    // XXX get clear on exactly what is required here...
    // XXX another way of doing it would be to just let each of the subclasses of GenericPuzzleInterface try, with its fromString()... but I don't think that would work with delay loading
    public String toString()
    {
        return "new PolytopePuzzleDescription("+com.donhatchsw.util.Arrays.toStringCompact(prescription)+")"; // escapifies
    }


    //
    // Utilities...
    //

        private static class CutInfo {
            public int iFace;
            public int iCutThisFace;
            public CutInfo(int iFace, int iCutThisFace)
            {
                this.iFace = iFace;
                this.iCutThisFace = iCutThisFace;
            }
        }

        //
        // Utility function to do the complicated
        // alt sticker center calculation in the ctor.
        //
        // The alt sticker center is a weighted average of the sticker
        // vertices.  The weight of a particular vertex in a sticker
        // is the product of the weights of the planes that contribute
        // to it, in that sticker.
        // The planes that contribute to a sticker
        // come in parallel pairs and singletons.  Singletons
        // get weight 1, parallel pairs get weights according
        // to the relative depths of the cut, so that
        // the closer the cut is to the surface, the higher the weight:
        // weight 1 if the cut is at the surface.
        //
        //
        private static float[][] computeStickerAltCentersF(
                CSG.SPolytope slicedPolytope,
                int face2OppositeFace[],
                int intLength,
                double doubleLength)
        {
            int nDims = slicedPolytope.p.dim;
            Assert(nDims == slicedPolytope.p.fullDim);

            CSG.Polytope allSlicedElements[][] = slicedPolytope.p.getAllElements();
            int allSlicedIncidences[][][][] = slicedPolytope.p.getAllIncidences();

            CSG.Polytope stickers[] = allSlicedElements[nDims-1];
            CSG.Polytope ridges[] = allSlicedElements[nDims-2];
            CSG.Polytope verts[] = allSlicedElements[0];

            int nFaces = face2OppositeFace.length;
            int nStickers = stickers.length;
            int nVerts = verts.length;

            float stickerAltCentersF[][] = new float[nStickers][nDims];


            // Scratch arrays... the size of the whole thing,
            // but we only use the parts that are incident on a particular sticker
            // at a time, and we clear those parts
            // when we are through with that sticker.
            double avgDepthOfThisStickerBelowFace[] = VecMath.fillvec(nFaces, -1.);
            int nCutsParallelToThisFace[] = VecMath.fillvec(nFaces, 0);
            double vertexWeights[] = VecMath.fillvec(nVerts, 1.);

            double stickerAltCenterD[] = new double[nDims];

            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                // Figure out avg cut depth of this sticker
                // with respect to each of the faces whose planes
                // contribute to it
                int ridgesThisSticker[] = allSlicedIncidences[nDims-1][iSticker][nDims-2];
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFace, iCutThisFace;
                    if (ridge.aux instanceof CutInfo)
                    {
                        CutInfo cutInfo = (CutInfo)ridge.aux;
                        iFace = cutInfo.iFace;
                        iCutThisFace = cutInfo.iCutThisFace+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        // Which original face?
                        // well, this ridge is on two stickers: iSticker
                        // and some other.  Find that other sticker,
                        // and find which face that other sticker
                        // was originally from.
                        int theTwoStickersSharingThisRidge[] = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                        Assert(theTwoStickersSharingThisRidge.length == 2);
                        Assert(theTwoStickersSharingThisRidge[0] == iSticker
                            || theTwoStickersSharingThisRidge[1] == iSticker);
                        int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                        CSG.Polytope otherSticker = stickers[iOtherSticker];
                        iFace = ((Integer)otherSticker.aux).intValue();
                        iCutThisFace = 0;
                    }
                    double cutDepth = iCutThisFace / doubleLength;
                    double depth = cutDepth;
                    if (avgDepthOfThisStickerBelowFace[iFace] != -1.)
                    {
                        // There are at most two cuts per face
                        // contributing to this sticker, and we already saw
                        // one.  Take the average of that one and this one.
                        depth = .5*(depth + avgDepthOfThisStickerBelowFace[iFace]);
                    }
                    avgDepthOfThisStickerBelowFace[iFace] = depth;
                    nCutsParallelToThisFace[iFace]++;
                    Assert(nCutsParallelToThisFace[iFace] <= 2);
                    int oppFace = face2OppositeFace[iFace];
                    if (oppFace != -1)
                    {
                        avgDepthOfThisStickerBelowFace[oppFace] = 1.-depth;
                        nCutsParallelToThisFace[oppFace]++;
                        Assert(nCutsParallelToThisFace[oppFace] <= 2);
                    }
                } // for each ridge this sticker

                // Now figure out the weight of each cut in the sticker.
                // Any time there are two parallel cuts contributing
                // to the sticker, those two cuts' weights
                // will add up to 1.  All other cuts contributing to the
                // sticker just get weight 1.
                // As we go, multiply in these cut weights
                // into the sticker weights.
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFace, iCutThisFace;
                    if (ridge.aux instanceof CutInfo)
                    {
                        CutInfo cutInfo = (CutInfo)ridge.aux;
                        iFace = cutInfo.iFace;
                        iCutThisFace = cutInfo.iCutThisFace+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        // Which original face?
                        // well, this ridge is on two stickers: iSticker
                        // and some other.  Find that other sticker,
                        // and find which face that other sticker
                        // was originally from.
                        int theTwoStickersSharingThisRidge[] = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                        Assert(theTwoStickersSharingThisRidge.length == 2);
                        Assert(theTwoStickersSharingThisRidge[0] == iSticker
                            || theTwoStickersSharingThisRidge[1] == iSticker);
                        int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                        CSG.Polytope otherSticker = stickers[iOtherSticker];
                        iFace = ((Integer)otherSticker.aux).intValue();
                        iCutThisFace = 0;
                    }
                    double cutDepth = iCutThisFace / doubleLength;
                    double cutWeight = 1.;
                    if (nCutsParallelToThisFace[iFace] == 2)
                    {
                        double avgStickerDepth = avgDepthOfThisStickerBelowFace[iFace];

                        double stickerSize = 2*Math.abs(avgStickerDepth-cutDepth);
                        cutWeight =
                            stickerSize==1. ? .5 :
                            cutDepth < avgStickerDepth ? 1. - cutDepth / (1. - stickerSize)
                                                       : (cutDepth-stickerSize) / (1. - stickerSize);
                    }
                    Assert(cutWeight >= -1e-9 && cutWeight <= 1.); // I've seen 1e-16 on "{4,3} 7" due to floating point roundoff error
                    if (cutWeight < 0.)
                        cutWeight = 0.;


                    // Each vertex weight is going to be the product
                    // of all the cut weights that contribute to it.
                    // So multiply this cut weight into each vertex
                    // incident on this cut.
                    for (int iVertThisRidge = 0; iVertThisRidge < allSlicedIncidences[nDims-2][iRidge][0].length; ++iVertThisRidge)
                    {
                        int iVert = allSlicedIncidences[nDims-2][iRidge][0][iVertThisRidge];
                        vertexWeights[iVert] *= cutWeight;
                    }
                } // for each ridge this sticker

                VecMath.zerovec(stickerAltCenterD);
                double totalWeight = 0.;
                for (int iVertThisSticker = 0; iVertThisSticker < allSlicedIncidences[nDims-1][iSticker][0].length; ++iVertThisSticker)
                {
                    int iVert = allSlicedIncidences[nDims-1][iSticker][0][iVertThisSticker];
                    CSG.Polytope vert = verts[iVert];
                    double vertexWeight = vertexWeights[iVert];
                    Assert(vertexWeight >= 0. && vertexWeight <= 1.);
                    totalWeight += vertexWeight;
                    // stickerAltCenterD += vertexWeight * vertexPosition
                    VecMath.vpsxv(stickerAltCenterD,
                                  stickerAltCenterD,
                                  vertexWeight, vert.getCoords());
                    vertexWeights[iVert] = 1.; // clear for next time
                }
                VecMath.vxs(stickerAltCenterD, stickerAltCenterD, 1./totalWeight);
                stickerAltCentersF[iSticker] = VecMath.doubleToFloat(stickerAltCenterD);

                // Clear the parts of avgDepthOfThisStickerBelowFace
                // that we touched.
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFace, iCutThisFace;
                    Assert(ridge.aux != null);
                    if (ridge.aux instanceof CutInfo)
                    {
                        CutInfo cutInfo = (CutInfo)ridge.aux;
                        iFace = cutInfo.iFace;
                        iCutThisFace = cutInfo.iCutThisFace+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        // Which original face?
                        // well, this ridge is on two stickers: iSticker
                        // and some other.  Find that other sticker,
                        // and find which face that other sticker
                        // was originally from.
                        int theTwoStickersSharingThisRidge[] = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                        Assert(theTwoStickersSharingThisRidge.length == 2);
                        Assert(theTwoStickersSharingThisRidge[0] == iSticker
                            || theTwoStickersSharingThisRidge[1] == iSticker);
                        int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                        CSG.Polytope otherSticker = stickers[iOtherSticker];
                        iFace = ((Integer)otherSticker.aux).intValue();
                        iCutThisFace = 0;
                    }

                    avgDepthOfThisStickerBelowFace[iFace] = -1.;
                    nCutsParallelToThisFace[iFace] = 0;
                    int oppFace = face2OppositeFace[iFace];
                    if (oppFace != -1)
                    {
                        avgDepthOfThisStickerBelowFace[oppFace] = -1.;
                        nCutsParallelToThisFace[oppFace] = 0;
                    }
                }
            } // for each sticker

            // Make sure we've been properly cleaning up both
            // avgDepthOfThisStickerBelowFace and vertexWeights.
            for (int iFace = 0; iFace < nFaces; ++iFace)
            {
                Assert(avgDepthOfThisStickerBelowFace[iFace] == -1.);
                Assert(nCutsParallelToThisFace[iFace] == 0);
            }
            for (int iVert = 0; iVert < nVerts; ++iVert)
                Assert(vertexWeights[iVert] == 1.);

            return stickerAltCentersF;
        } // computeStickerAltCentersF

        // magic crap used in a couple of methods below
        private double[][] getTwistMat(int gripIndex, int dir, double frac)
        {
            int order = gripSymmetryOrders[gripIndex];
            double angle = dir * (2*Math.PI/order) * frac;
            double gripUsefulMat[][] = gripUsefulMats[gripIndex];
            Assert(gripUsefulMat.length == _nDisplayDims);
            double mat[][] = VecMath.mxmxm(VecMath.transpose(gripUsefulMats[gripIndex]),
                                 VecMath.makeRowRotMat(_nDisplayDims,
                                                       _nDisplayDims-2,_nDisplayDims-1,
                                                       angle),
                                 gripUsefulMats[gripIndex]);
            return mat;
        } // getTwistMat


    //======================================================================
    // BEGIN GENERICPUZZLEDESCRIPTION INTERFACE METHODS
    //

        public int nDims()
        {
            return slicedPolytope.p.fullDim;
        }
        public int nDisplayDims()
        {
            return _nDisplayDims;
        }
        public int nVerts()
        {
            return vertsF.length;
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
            float gripDirPlusGripOffF[] = new float[_nDisplayDims];
            for (int i = 0; i < gripDirsF.length; ++i)
            {
                VecMath.vpsxv(gripDirPlusGripOffF,
                              gripDirsF[i],
                              .99f,
                              gripOffsF[i]);
                float thisDistSqrd = VecMath.distsqrd(gripDirPlusGripOffF,
                                                      pickCoords);
                if (thisDistSqrd < bestDistSqrd)
                {
                    bestDistSqrd = thisDistSqrd;
                    bestIndex = i;
                }
            }
            return bestIndex;
        }
        // XXX lame, this should be precomputed and looked up by
        // XXX poly and sticker index.
        // XXX This only seems to be called
        // XXX for 3d puzzles, and it's called using
        // XXX faceCenter, polyCenter-stickerCenter.
        public int getClosestGrip(float unNormalizedDir[/*4*/],
                                  float unNormalizedOff[/*4*/])
        {
            float mat[][] = {VecMath.copyvec(unNormalizedDir),
                             VecMath.copyvec(unNormalizedOff)};
            VecMath.gramschmidt(mat, mat);
            float dir[] = mat[0];
            float off[] = mat[1];
            //System.out.println("    unNormalizedDir = "+com.donhatchsw.util.Arrays.toStringCompact(unNormalizedDir));
            //System.out.println("    unNormalizedOff = "+com.donhatchsw.util.Arrays.toStringCompact(unNormalizedOff));
            //System.out.println("        dir= "+com.donhatchsw.util.Arrays.toStringCompact(dir));
            //System.out.println("        off= "+com.donhatchsw.util.Arrays.toStringCompact(off));
            //System.out.println("        nGrips = "+gripDirsF.length);
            float eps = 1e-6f; // note, using this to compare squares, but it's all right (the distances are < 2, so the squared distances are < 4, so we are off by at most a factor of 8 here)
            int bestGrip = -1;
            float bestDistSqrd = Float.MAX_VALUE;
            float bestOffDistSqrd = Float.MAX_VALUE;
            // Primary criterion is closeness to grip dir.
            // Secondary criterion is closeness to off dir (in case of a tie
            // in closeness to grip dir).
            for (int iGrip = 0; iGrip < gripDirsF.length; ++iGrip)
            {
                //if (iGrip < 68) System.out.println("    gripDirsF["+iGrip+"] = "+com.donhatchsw.util.Arrays.toStringCompact(gripDirsF[iGrip]));
                //if (iGrip < 68) System.out.println("    gripOffsF["+iGrip+"] = "+com.donhatchsw.util.Arrays.toStringCompact(gripOffsF[iGrip]));
                float thisDistSqrd = VecMath.distsqrd(gripDirsF[iGrip],
                                                      dir);
                if (thisDistSqrd > bestDistSqrd + eps)
                    continue;
                float thisOffDistSqrd = VecMath.distsqrd(gripOffsF[iGrip],
                                                         off);
                if (thisDistSqrd >= bestDistSqrd - eps
                 && thisOffDistSqrd > bestOffDistSqrd + eps)
                        continue;
                bestDistSqrd = thisDistSqrd;
                bestOffDistSqrd = thisOffDistSqrd;
                bestGrip = iGrip;
                //System.out.println("            best grip = "+bestGrip);
                //System.out.println("            bestDistSqrd "+bestDistSqrd);
                //System.out.println("            bestOffDistSqrd "+bestOffDistSqrd);
            }
            //System.out.println("        best grip = "+bestGrip);
            return bestGrip;
        } // getClosestGrip

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
            computeVertsAndShrinkToPointsAtRest(
                float outVerts[/*nVerts*/][/*nDisplayDims*/],
                float outStickerCenters[/*nStickers*/][/*nDisplayDims*/],
                float outStickerShrinkToPointsOnFaceBoundaries[/*nStickers*/][/*nDisplayDims*/],
                float outPerStickerFaceCenters[/*nStickers*/][/*nDisplayDims*/])
        {
            if (outVerts != null)
            {
                Assert(outVerts.length == vertsF.length);
                for (int iVert = 0; iVert < vertsF.length; ++iVert)
                    VecMath.copyvec(outVerts[iVert], vertsF[iVert]);
            }
            if (outStickerCenters != null)
            {
                Assert(outStickerCenters.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outStickerCenters[iSticker],
                                    stickerCentersF[iSticker]);
            }
            if (outStickerShrinkToPointsOnFaceBoundaries != null)
            {
                Assert(outStickerShrinkToPointsOnFaceBoundaries.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outStickerShrinkToPointsOnFaceBoundaries[iSticker],
                                    stickerAltCentersF[iSticker]);
            }
            if (outPerStickerFaceCenters != null)
            {
                Assert(outPerStickerFaceCenters.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outPerStickerFaceCenters[iSticker],
                                    faceCentersF[sticker2face[iSticker]]);
            }
        } // computeVertsAndShrinkToPointsAtRest

        public void
            computeVertsAndShrinkToPointsPartiallyTwisted(
                float outVerts[/*nVerts*/][/*nDisplayDims*/],
                float outStickerCenters[/*nStickers*/][/*nDisplayDims*/],
                float outStickerShrinkToPointsOnFaceBoundaries[/*nStickers*/][/*nDisplayDims*/],
                float outPerStickerFaceCenters[/*nStickers*/][/*nDisplayDims*/],
                int gripIndex,
                int dir,
                int slicemask,
                float frac)
        {
            // Note, we purposely go through all the calculation
            // even if dir*frac is 0; we get more consistent timing that way.

            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("computeVertsAndShrinkToPointsPartiallyTwisted called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if (gripSymmetryOrders[gripIndex] == 0)
                throw new IllegalArgumentException("computeVertsAndShrinkToPointsPartiallyTwisted called on gripIndex "+gripIndex+" which does not rotate!");
            if (outVerts.length != vertsF.length)
                throw new IllegalArgumentException("outVerts length "+outVerts.length+" does not match number of verts "+vertsF.length+"!");
            if (outVerts.length > 0 && outVerts[0].length != vertsF[0].length)
                throw new IllegalArgumentException("outVerts[0] length "+outVerts[0].length+" does not match verts dimension "+vertsF[0].length+"!");
            if (outStickerCenters.length != stickerCentersF.length)
                throw new IllegalArgumentException("outStickerCenters length "+outStickerCenters.length+" does not match number of stickers "+stickerCentersF.length+"!");
            if (outStickerCenters.length > 0 && outStickerCenters[0].length != stickerCentersF[0].length)
                throw new IllegalArgumentException("outStickerCenters[0] length "+outStickerCenters[0].length+" does not match stickers dimension "+stickerCentersF[0].length+"!");
            if (outStickerShrinkToPointsOnFaceBoundaries.length != stickerCentersF.length)
                throw new IllegalArgumentException("outStickerShrinkToPointsOnFaceBoundaries length "+outStickerShrinkToPointsOnFaceBoundaries.length+" does not match number of stickers "+stickerCentersF.length+"!");
            if (outPerStickerFaceCenters.length != stickerCentersF.length)
                throw new IllegalArgumentException("outPerStickerFaceCenters length "+outPerStickerFaceCenters.length+" does not match number of stickers "+stickerCentersF.length+"!");


            if (slicemask == 0)
                slicemask = 1; // XXX is this the right place for this? lower and it might be time consuming, higher and too many callers will have to remember to do it

            double matD[][] = getTwistMat(gripIndex, dir, frac);
            float matF[][] = VecMath.doubleToFloat(matD);

            boolean whichVertsGetMoved[] = new boolean[vertsF.length]; // false initially
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

                    VecMath.vxm(outStickerCenters[iSticker], stickerCentersF[iSticker], matF);
                    VecMath.vxm(outStickerShrinkToPointsOnFaceBoundaries[iSticker], stickerAltCentersF[iSticker], matF);
                    VecMath.vxm(outPerStickerFaceCenters[iSticker], faceCentersF[sticker2face[iSticker]], matF);
                }
                else
                {
                    VecMath.copyvec(outStickerCenters[iSticker],
                                    stickerCentersF[iSticker]);
                    VecMath.copyvec(outStickerShrinkToPointsOnFaceBoundaries[iSticker],
                                    stickerAltCentersF[iSticker]);
                    VecMath.copyvec(outPerStickerFaceCenters[iSticker],
                                    faceCentersF[sticker2face[iSticker]]);
                }
            }

            // We do the following in a separate single pass over the verts,
            // to avoid redundant computation since verts
            // are referenced multiple times per sticker in the
            // stickerInds list.  Another possible way of doing this
            // would be to make lists of sticker-to-verts-on-that-sticker
            // without redundancies, and iterate through those in this function.
            // That would probably actually be better.
            for (int iVert = 0; iVert < vertsF.length; ++iVert)
            {
                if (whichVertsGetMoved[iVert])
                    VecMath.vxm(outVerts[iVert], vertsF[iVert], matF);
                else
                    VecMath.copyvec(outVerts[iVert], vertsF[iVert]);
            }
        } // computeVertsAndShrinkToPointsPartiallyTwisted



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
        public int[/*nStickers*/][/*nPolygonsThisSticker*/] getStickerPoly2Grip()
        {
            return stickerPoly2Grip;
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
        public float[/*nFaces*/][/*nDisplayDims*/]
            getFaceCentersAtRest()
        {
            return faceCentersF;
        }
        public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                    int gripIndex,
                                                    int dir,
                                                    int slicemask)
        {
            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("applyTwistToState called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if (gripSymmetryOrders[gripIndex] == 0)
                throw new IllegalArgumentException("applyTwistToState called on gripIndex "+gripIndex+" which does not rotate!");
            if (state.length != stickerCentersD.length)
                throw new IllegalArgumentException("applyTwistToState called with wrong size state "+state.length+", expected "+stickerCentersD.length+"!");

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
        if (args.length != 1)
        {
            System.err.println();
            System.err.println("    Usage: PolytopePuzzleDescription \"<schlafliProduct> <length>[(<doubleLength>)]\"");
            System.err.println("    Example: PolytopePuzzleDescription \"{4,3,3} 3\"");
            System.err.println("    Example: PolytopePuzzleDescription \"{3,3,3} 3(5.0)\"");
            System.err.println("    Example: PolytopePuzzleDescription \"{5}x{4} 3\"");
            System.err.println();
            System.exit(1);
        }
        System.out.println("in main");

        //com.donhatchsw.util.CSG.verboseLevel = 2;

        java.io.PrintWriter progressWriter = new java.io.PrintWriter(
                                             new java.io.BufferedWriter(
                                             new java.io.OutputStreamWriter(
                                             System.err)));

        String puzzleDescriptionString = args[0];
        GenericPuzzleDescription descr = new PolytopePuzzleDescription(puzzleDescriptionString,
                                                                       progressWriter);
        System.out.println("description = "+descr.toString());

        System.out.println("out main");
    } // main

} // class PolytopePuzzleDescription
