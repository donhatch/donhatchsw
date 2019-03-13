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
                                default mode  "Require Ctrl Key To Spin Drag"
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
               (works for only generic puzzles currently) XXX might not be true by the time I'm through
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
               (works for only generic puzzles currently)
            - Speed of twists and rotations
               have been adjusted to feel more uniform for different
               rotation amounts
               (small rotations are slower and large rotations are faster
               than before, so that the acceleration feels the same
               for all types of moves).
            - Better depth sorting of polygons
               (still doesn't always work though)
               (works for only generic puzzles currently)
               (and only 4d generic puzzles, doesn't work for 3d ones at all)
            - Lots of new puzzle types available from the Puzzle menu,
               of dimension 2,3, and 4.
               These are called "generic puzzles" and they are a work
               in progress.
        Generic puzzles have the following limitations in this release:
            - no save/load (menus are probably misleading)
            - no macros (menus are probably misleading)
            - color choosing is very primitive
            - sometimes the highlighted sticker fails to get updated correctly
              at the end of a twist (jiggle the mouse to fix it)
            - no real solve
            - scramble affects only outer or 2nd slices (you'll
              notice this only if your puzzle length is >= 6)
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

    FIXED I think (test cases):
    ===================
        - "{4,3} 3(10)" isn't making the slice thin
        - "3x3 3(4)" try to click on square, it says can't twist that
        - "{3,3}x{} 5(9)" twists are wrong and says "can't twist that"
        - "(0)---(1)-4-(1)---(0) 3(4.0)"  twists wrong thing
        - "(1)---(1)-4-(0)---(0) 3" twists wrong thing
        - "(0)---(1)--(1)---(0) 3(4.0)" twists are backwards!!!
        - "(0)4(1)(1)(1) 3" twists wrong thing
        - "(0)---(1)---(1)---(1) 3(4.0)" twists wrong thing
        - "5x5 3" and up, twists wrong thing
        - truncated hypercube, twists mostly right except twists something when left-click on center sticker
        - on macbook, can't get to the Twist Duration slider (needs scrolled panel maybe)

    BUGS / URGENT TODOS:
    ===================
        - CHECK fail on 3d puzzle when 1color sticker gonality isn't same as the facet gonality: puzzleDescription="(.25)4(2)3 3(1.4)": CHECK(cutWeight >= -1e-9 && cutWeight <= 1.) (cutWeight is -.75)
        - 5-dimensional puzzles get ArrayIndexOutOfBoundException when trying to view them (should just get rejected, I think)
        - can't fling on laptop (neither macboox nor glinux box)
        - "{4,3,3} 2,3,3" and in fact "{4,3,3} 2" assert-fails now?  (oh, something about further cuts together with new push/pop logic).  currently XXXuseNewPushPopAux is set to false in CSG.prejava because it's not ready yet
	      Exception in thread "main" java.lang.Error: CHECK failed at com/donhatchsw/util/CSG.prejava(331): this.pushedAuxNext != null
		      at com.donhatchsw.util.CSG$Polytope.popAux(com/donhatchsw/util/CSG.prejava:331)
		      at com.donhatchsw.mc4d.PolytopePuzzleDescription.init(PolytopePuzzleDescription.java:1191)
		      at com.donhatchsw.mc4d.PolytopePuzzleDescription.<init>(PolytopePuzzleDescription.java:642)
		      at com.donhatchsw.mc4d.MC4DModel.<init>(MC4DModel.java:192)
		      at com.donhatchsw.mc4d.MC4DApplet.init(MC4DApplet.java:1055)
		      at com.donhatchsw.applet.AppletViewer.main(com/donhatchsw/applet/AppletViewer.prejava:242)
		      at com.donhatchsw.mc4d.MC4DApplet.main(MC4DApplet.java:1316)
        - '{4,3} 3(4)' with nonzero stickers-shrink-to-face-boundaries is asymmetric (due to the one-of-opposite-pairs-doing-all-the-cuts-for-both-of-them thing, I think)
        - make && java -jar donhatchsw.jar puzzleDescription="Fruity 3(9)" shouldn't require such a shallow cut specification!  isn't it supposed to be using the edge that would give the shallowest cut?
        - `java -jar donhatchsw.jar puzzleDescription='{4,3} 2,3,4'`, twisting gives CHECK failure: "CHECK(whereIstickerGoes != null);", 	at com.donhatchsw.mc4d.PolytopePuzzleDescription.applyTwistToState(PolytopePuzzleDescription.java:2402)
        - >=5 dimensional puzzles on command line non-gracefully excepts
        - ctrl-alt-space for debugging... doesn't stop things any more?? (does for rotates, not for twists)
        - with multiple windows, animation doesn't go by itself any more
        - doFurtherCuts issues:
          - in '4,3,3 2', rotate-element-to-center not working right when element is an edge-- it rotates a vert to center instead. (both with old and new poly-to-grip code). ah, I think it's getting confused and assuming stickers, grips, and elements-rotatable-to-center are all the same.
          - the following seem to have pieces with ambiguous inside-outness (maybe just same as the flicker issue already mentioned)
                  '(1)x(1)x(1)x(2) 1,1,1,2'
                  '(1)x(1)x(2)x(2) 1,1,2,2'
                  '(1)x(2)x(2)x(2) 1,2,2,2'
                  '(1)x(2)x(2)x(1) 1,2,2,1'
                  '(2)x(1)x(1)x(1) 2,1,1,1'
                  '{4,3,3} 2(10)'
            - 3x5 2  and  5x5 2  some stickers flicker on and off... thinks they are sort of inside out I guess, damn   (this was true when I was doFurtherCut'sing triangles as well as squares... turn that on to debug this)
            - 3x4 2  still using old closestGrip method, so gets wrong thing when clicking on outer square or edges (fixed now I think?)
            - maybe doFurtherCuts needs to be on if there's a triangle too (not just if there's a square), e.g. 3,3,3 2  or 3,3,4 2   or 3,4,3 2
            - and maybe triangles need a separate scheme?  think about it
            - maybe further-cut only the polygons that need it? (squares, maybe triangles... not sure this is feasible though, since the current method just adds global slice planes)

        - {5,3} 3(1.0001) "stickers shrink to face boundaries" doesn't work

        - why is the progressWriter.flush() needed when doing sanity checking, to see any output at all??  makes me think something bogus is going on otherwise
        - undo tree's colors are wrong!
        - ctrl-c in undo window quits program
        - Quit doesn't quit program
        - "restrict roll" on, set it spinning, reset 3d rotation, it ends up weird
        - contiguous cubies doesn't do anything sensible in Melinda's
        - cascading menus don't cascade well, see if anything I can do (especially when hitting right side, or when trying to do it with clicks but not drags)
        - need good help describing all the controls, in the main help part
        - need to get javacpp fixed and usable before I ship this  (partially fixed now, still lame on jikes output)
        - update date of MANIFEST.INF automatically
        - need simple robust way of guaranteeing loading latest donhatchsw.jar,
             and showing the version/timestamp within a program--
             definitely the help menu, maybe the web page itself too
        - need About menu before shipping
        - make sure our methods for detecting shift/alt/ctrl work in all versions (was it 1.1 -> 1.2 in which things changed?) (also maybe something in different 1.6 versions too?)
        - hotkeys don't work from java 1.6??  e.g. ctrl-c gives only what c gives
           (seems to be okay now... was it just an early release of 1.6?)
        - clicking on the < or > on side of the scrollbars take only about every other time
        - twist speed of generic 2x in melinda's is way too fast
        - gratuitous undo tree animation is really slow and postpones puzzle animation!?
        - status bar and puzzle prescription bar
        - implement expert control panel
            - num paints should go under it, probably... maybe (I like seeing it from very start though)
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
        JOIN:
            - TODONE (mostly: would be nice to be able to express Johnson solids.  Currently can't even express a square pyramid, I don't think? (could allow pyramid to be expressed? maybe join operator, see https://en.wikipedia.org/wiki/Schl%C3%A4fli_symbol) Maybe allow general intersections of half-spaces?
            - succeeds:
                3,4v()
                ()v3,4
                ()v{}
                {}v{}
                {}v3
                3v{}
                ()v3
                ()v()
                ()v()v()
                ()v()v()v()
                ()v()v()v()v()
                etc.
                {}v()
                3v()
                4v()
                etc.
                4,3v()
                5,3v()
                3v3 (gets different ArrayIndexOutOfBoundsException 0 just because it's 5 dimensional, but that's a different bug)

        FUTT:
            - scrambling a small number of scrambles isn't well behaved-- it sometimes does an order=1 move, i.e. nothing (because it allows no-op moves, I think? wait, isn't the code supposed to prevent that?)
            - current limited implementation:
              - make decideWhetherFuttable more reliable (it allows "frucht 3(2.5)" because numbers of incidences match, but it shouldn't)
            - make more general implementation:
              - support other than 3d
              - support other than trivalent
              - allow slicesmask to express more layers than just first wave
              - do the right thing when waves interact
              - do the right thing for length=2 in various cases (cube, triprism, ...)
              - interact properly with heterogeneous intLengths
            - Canonical examples:
              - 3d:
                - frucht: all faces futtable
                  - "frucht 3(4)"
                  - "frucht 9(20)"
                  - frucht such that different waves interact
                - frucht: the square is futtable
                  - tet prism "3x{} 3(4)"
                  - tet prism "3x{} 9(20)"
                  - tet prism such that different waves interact
                - trunc ico: the hexagon is futtable
                  - trunc ico "3(1)5 3(4)"
                  - trunc ico "3(1)5 9(20)"
                  - tet prism such that different waves interact
              - 4d:
                - Fruity
                - tet prism: the 3 cubes are futtable
                  - tet prism prism "3x4 3(5)"
                  - tet prism prism "3x4 9(20)"
                  - tet prism prism such that different waves interact
              TODO: example of 4d 4-valent where a triprism needs futt for every one of its nontrivial local symmetries.
              TODO: example of 4d 4-valent where a triprism needs futt for some but not all of its nontrivial local symmetries.
              TODO: example of 4d 4-valent where a triprism needs futt for a twist on its tri, but some of its twists on squares don't need futt
              TODO: more subtle localities?

        FRUCHT/FRUITY
            - 4d fruity isn't right yet, need to work on it

        NONUNIFORM BOXES:
            - "{4,3} 2,3,4" strangely asymmetric now?  and throws on click.  (oh, that was never the way to do a nonuniform box.  it was "(2)x(3)x(4) 2,3,4"

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
                  (and, now, reconcile this with the multiple-comma-separated length specs that are now supported)
            - invention form should come up with current puzzle or previous
                  failed attempt
            - should mention Johnson numbers and short names where applicable... and hyperlinks would be great.
                  Maybe an "about the shape" page with links.

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
            - the cascading menus for are intolerable.  need a big dialog with stuff all layed out on a page with icons
            - now that circumradius is 1, change eyeW and eyeZ so
              that eyeW > 1 so that it's safe by default, for all puzzles.

        AWT/APPLET/GUI LAYOUT:
            - menu bar not supported at all in 1.1 currently (something about popupmenu api?)
            - MyMenuBar menus don't pop down nicely when others opened
            - clicking twist duration down from 30 to 29.99 takes 8 clicks, back up takes 12???  turn on the block of debugging messages in SliderForFloat to see.  damn, looks like this is a bug in Scrollbar??


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
                we get flat facets!  (XXX is this true any more?)
            - ooh, make more slices proportionally slower, would feel more massive!
            - completely general solve?
            - general uniform polytopes! yeah!
            - make slicing faster-- for humongous polytopes, need to
                look at only neighbor facets (and slices thereof) and no farther,
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
                - wirefame around unshrunk facets
                - wireframe around shrunk facets (separate faceShrink for it?)
                - wireframe around unshrunk stickers (separate stickerShrink for it?)
                - wireframe around stickers (that's the current button)

                polygon shrink?
                several different wireframes at once with different styles?
                okay I think this is where I went insane last time I was
                implementing a polytope viewer
            - fade out to transparent instead of suddenly turning inside out?
                This would nicely light up the center,
                And would also help mask the sorting failures
                on facets that are very close to flat.  I think this would
                look beautiful, especially on the big 120-cell-based puzzles
            - ha, for the {5}x{4}, it could be fudged so the cubical facets
                behave like they have full symmetry-- it would allow stickers
                to slide off of the pentprism face and onto a cube face.
                In general this will make the symmetry of a twist
                be dependent on the symmetry of the face,
                which can be more than the symmetry of the whole puzzle.
                (TODO: this is in progress, it's called Futt.  works for 3d so far)
            - (maybe done?) what should be highlighted is not the sticker, but everything
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
import com.donhatchsw.compat.regex;
import com.donhatchsw.util.CSG;
import com.donhatchsw.util.FuzzyPointHashTable;
import com.donhatchsw.util.SortStuff;
import com.donhatchsw.util.VecMath;

public class PolytopePuzzleDescription implements GenericPuzzleDescription {

    public static boolean forceFuttableXXX = false;  // temporary

    private String prescription; // what was originally passed to the constructor. we are an immutable object that is completely a function of this string, so an identical clone of ourself can be constructed using this string in any future lifetime.

    private CSG.SPolytope originalPolytope;
    private CSG.SPolytope slicedPolytope;

    private int _nDisplayDims = 4; // never tried anything else, it will probably crash
    private float _circumRadius;
    private float _inRadius;
    private int _nCubies;

    private float[/*nVerts*/][/*nDisplayDims*/] vertsF;
    private double[/*nVerts*/][/*nDisplayDims*/] vertsDForFutt; // not needed in general, but currently needed for on-the-fly analysis when futt'ing.
    private int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/] stickerInds;  // indices into vertsF

    private float[/*nFacets*/][/*nDisplayDims*/] facetCentersF;

    private int[][/*2*/][/*2*/] adjacentStickerPairs;
    private int[/*nFacets*/] facet2OppositeFacet;
    private int[/*nStickers*/] sticker2face;
    private int[/*nStickers*/] sticker2faceShadow; // so we can detect nefariousness
    private int[/*nStickers*/] sticker2cubie;

    private float[/*nGrips*/][] gripDirsF;
    private float[/*nGrips*/][] gripOffsF;
    private int[/*nGrips*/] grip2face;
    private int[/*nGrips*/][/*2*/] grip2facetEltForFutt;
    private int[/*nGrips*/] gripSymmetryOrders;
    private int[/*nGrips*/] gripSymmetryOrdersFutted;
    private double[/*nGrips*/][/*nDims*/][/*nDims*/] gripUsefulMats; // weird name
    private double[/*nGrips*/][/*nDims*/] gripTwistRotationFixedPoints;  // a point that should remain fixed by the twist rotation.  for uniform, can be origin, but for frucht, needs to be face center or something.
    private boolean futtable;
    private int[] intLengthsForFutt;  // not needed in general, but currently needed for on-the-fly analysis when futt'ing.
    private int[/*nStickers*/][/*nPolygonsThisSticker*/] stickerPoly2Grip;

    private double[/*nFacets*/][/*nDims*/] facetInwardNormals;
    private double[/*nFacets*/][/*nCutsThisFacet*/] facetCutOffsets; // slice 0 is bounded by -infinity and offset[0], slice i+1 is bounded by offset[i],offset[i+1], ... slice[nSlices-1] is bounded by offset[nSlices-2]..infinity
    private double[/*nFacets*/] facetOffsetsForFutt; // not needed in general, but currently needed for on-the-fly analysis when futt'ing.

    private float[][] nicePointsToRotateToCenter;

    private double[][] stickerCentersD; // for very accurate which-slice determination
    private float[][] stickerCentersF; // for just shoving through display pipeline
    private float[][] stickerAltCentersF; // alternate sticker shrink-to points, on facet boundary
    private FuzzyPointHashTable stickerCentersHashTable;

    private static void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }
    private static void Assumpt(boolean condition) { if (!condition) throw new Error("Assumption failed"); }

    private static String millisToSecsString(long millis)
    {
        // argh, %f not implemented
        //return com.donhatchsw.compat.Format.sprintf("%.3f", millis*.001);
        String answer = "";
        if (millis < 0)
        {
            answer += "-";
            millis = -millis;
        }
        answer += millis/1000
                + "."
                + millis / 100 % 10
                + millis / 10 % 10
                + millis % 10;
        return answer;
    }

    // throws NumberFormatException on failure
    private static double parseDoubleFraction(String s)
    {
      int slashIndex = s.lastIndexOf('/');
      if (slashIndex != -1)
      {
          String numeratorString = s.substring(0, slashIndex);
          String denominatorString = s.substring(slashIndex+1);
          return Double.parseDouble(numeratorString)
               / Double.parseDouble(denominatorString);
      }
      else
          return Double.parseDouble(s);
    }  // parseDoubleFraction


    /**
    * The constructor that is required by the factory.
    *
    * Prescription is a schafli product symbol followed by a space and an integer
    * (specifying the number of cuts parallel to each facet = floor(the integer/2)
    * and an optional parenthesized floating point number (overriding
    * the integer when determining the cut spacing, which is 1/(the number)
    * times edge length.
    * E.g.
    *     Standard 3x3x3 hypercube: "{4,4,3} 3"
    *     Standard 3x3x3 hypercube with very shallow cuts: "{4,3,3} 3(6)"
    *
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
        // TODO: Document this not-yet-documented feature: can be more than one length (each with optional double override), specifying a different cut scheme for each dimension.  The one chosen for a given facet is the one whose index is the index of the coord axis most closely aligned with the facet normal.  Probably makes sense for only axis-aligned boxes.
        com.donhatchsw.compat.regex.Matcher matcher =
        com.donhatchsw.compat.regex.Pattern.compile(
            "\\s*([^ ]+)\\s+((\\d+)(\\((.*)\\))?(,(\\d+)(\\((.*)\\))?)*)"
        ).matcher(prescription);
        if (!matcher.matches())
            throw new IllegalArgumentException("PolytopePuzzleDescription didn't understand prescription string "+com.donhatchsw.util.Arrays.toStringCompact(prescription)+"");

        String schlafliProductString = matcher.group(1);
        String commaSeparatedLengthsString = matcher.group(2);
        String[] lengthStrings = regex.split(commaSeparatedLengthsString, ",");
        int[] intLengths = new int[lengthStrings.length];
        double[] doubleLengths = new double[lengthStrings.length];

        for (int i = 0; i < lengthStrings.length; ++i)
        {
            String lengthString = lengthStrings[i];

            int intLength = 0;
            double doubleLength = 0.;
            // XXX duplicated from elsewhere, twice... need to make a function I think
            {
                lengthString = lengthString.trim();

                try {
                    //System.out.println("lengthString = "+lengthString);

                    com.donhatchsw.compat.regex.Matcher submatcher =
                    com.donhatchsw.compat.regex.Pattern.compile(
                        "(\\d+)\\((.*)\\)"
                    ).matcher(lengthString);
                    if (submatcher.matches())
                    {
                        String intPart = submatcher.group(1);
                        String doublePart = submatcher.group(2);
                        //System.out.println("intPart = "+intPart);
                        //System.out.println("doublePart = "+doublePart);

                        intLength = Integer.parseInt(intPart);
                        doubleLength = parseDoubleFraction(doublePart);
                    }
                    else
                    {
                        // Allow fractions
                        // XXX TODO: allow arbitrary arithmetic expressions, with sqrt,sin,cos,etc.
                        doubleLength = parseDoubleFraction(lengthString);  // XXX should catch parse error and throw illegal arg exception
                        intLength = (int)Math.ceil(doubleLength);
                    }
                }
                catch (java.lang.NumberFormatException e)
                {
                    //System.err.println("Your invention sucks! \""+lengthString+"\" is not a number! (or comma-separated list of numbers, with optional overrides, one for each dimension)");
                    //initPuzzleCallback.call(); // XXX really just want a repaint I think
                    //return;
                    CHECK(false);
                }
                //System.out.println("intLength = "+intLength);
                //System.out.println("doubleLength = "+doubleLength);
            }
            intLengths[i] = intLength;
            doubleLengths[i] = doubleLength;
        }

        init(schlafliProductString, intLengths, doubleLengths, progressWriter);
        this.prescription = prescription;
    } // ctor that takes just a string

    private int intpow(int a, int b) { return b==0 ? 1 : intpow(a,b-1) * a; }  // simple, slow

    private boolean decideWhetherFuttable(int[] intLengths, java.io.PrintWriter progressWriter)
    {
        if (progressWriter != null) progressWriter.println("        is it futtable?");

        if (forceFuttableXXX)
        {
            if (progressWriter != null) progressWriter.println("        deciding futtable because override");
            return true;
        }

        int nDims = this.originalPolytope.p.dim;
        if (nDims != 3)
        {
            if (progressWriter != null) progressWriter.println("        deciding not futtable because nDims="+nDims+" is not 3");
            return false;
        }

        // If intLengths are not all the same, we can't handle it.
        int intLength = intLengths[0];
        for (int i = 0; i < intLengths.length; ++i) {
            if (intLengths[i] != intLength)
            {
                if (progressWriter != null) progressWriter.println("        deciding not futtable because intLengths are not all the same");
                return false;
            }
        }

        if (true)
        {
            // If intLength<3, we can't handle it.
            if (intLength < 3)
            {
                if (progressWriter != null) progressWriter.println("        deciding not futtable because intLength="+intLength+" < 3");
                return false;
            }

            // If intLength isn't odd, we can't handle it.
            if (intLength % 2 == 0)
            {
                if (progressWriter != null) progressWriter.println("        deciding not futtable because intLength="+intLength+" isn't odd");
                return false;
            }
        }

        int nCutsPerFace = (intLength-1)/2;

        CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
        int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();
        int nVerts = originalElements[0].length;
        int nFacets = originalElements[2].length;
        int nEdges = originalElements[1].length;

        if (true)
        {
            // If any vertex figure is not a simplex (i.e. has valence other than nDims),
            // we can't handle it.
            for (int iVert = 0; iVert < nVerts; ++iVert) {
                if (originalIncidences[0][iVert][1].length != nDims)
                {
                    if (progressWriter != null) progressWriter.println("        deciding not futtable because at least one vertex figure is not a simplex");
                    return false;
                }
            }
        }


        // If any non-incident cut sets interact, we can't handle it.

        // We get a pretty good idea of this by counting total number of
        // elements of each dimension on the stickers.
        // XXX this isn't actually the stickers, it's the sliced polytope, without having been separated yet.  so, should rename the vars
        int[] stickerElementCounts = CSG.counts(this.slicedPolytope.p);
        int nStickerVerts = stickerElementCounts[0];
        int nStickerEdges = stickerElementCounts[1];
        int nStickers = stickerElementCounts[nDims-1];
        CHECK(nStickerVerts + nStickers == nStickerEdges + 2);  // Euler's formula
        if (progressWriter != null) progressWriter.println("            sticker element counts = "+VecMath.toString(stickerElementCounts));
        if (true)
        {
            int expectedNumStickers = 0;  // and counting
            for (int iDim = 0; iDim <= nDims-1; ++iDim) {
                int contributionPerElement = intpow(nCutsPerFace, nDims-1 - iDim) * (nDims-iDim);
                expectedNumStickers += originalElements[iDim].length * contributionPerElement;
            }
            if (progressWriter != null) progressWriter.println("            num stickers = "+nStickers+" "+(nStickers==expectedNumStickers?"==":"!=")+" "+expectedNumStickers+" = expected num stickers");
            if (nStickers != expectedNumStickers)
            {
                if (progressWriter != null) progressWriter.println("        deciding not futtable because num stickers is not as expected");
                return false;
            }
        }
        if (true)
        {
            int expectedNumStickerVerts = 0;  // and counting
            expectedNumStickerVerts += nVerts;
            expectedNumStickerVerts += nEdges * (2 * nCutsPerFace);
            expectedNumStickerVerts += nEdges * 2 * nCutsPerFace*nCutsPerFace;
            if (progressWriter != null) progressWriter.println("            num sticker verts = "+nStickerVerts+" "+(nStickerVerts==expectedNumStickerVerts?"==":"!=")+" "+expectedNumStickerVerts+" = expected num sticker verts");
            if (nStickerVerts != expectedNumStickerVerts)
            {
                if (progressWriter != null) progressWriter.println("        deciding not futtable because num sticker verts is not as expected");
                return false;
            }
        }

        // CBB: this actually kinda sucks because it prevents legitimate futting on things
        // whose topology is regular but whose geometry isn't; for example, "{3}v()"  (if that comes out stretched,
        // which it does at the time of this writing).
        // TODO: I think, when I get edge futting working, just remove this check so that topological regulars will be futtable too
        if (true)  // can set this to false if I want to debug futt behavior on, say, a cube.
        {
            // If original polytope is regular, then don't *need* to futt.
            // I think regular means the incidences look as symmetrical as possible.
            // (Though I'm not completely sure of this.
            CHECK(originalIncidences.length == nDims+1);
            boolean topologyIsRegular = true;   // until proven otherwise
            for (int iDim = 0; iDim < originalIncidences.length; ++iDim)
            for (int jDim = 0; jDim < originalIncidences.length; ++jDim)
            {
                for (int ii = 0; ii < originalIncidences[iDim].length; ++ii)
                {
                    if (originalIncidences[iDim][ii][jDim].length != originalIncidences[iDim][0][jDim].length)
                    {
                        topologyIsRegular = false;
                        break;
                    }
                }
                if (!topologyIsRegular) break;
            }
            if (progressWriter != null) progressWriter.println("            topologyIsRegular = "+topologyIsRegular);
            if (topologyIsRegular)
            {
                if (progressWriter != null) progressWriter.println("        deciding not futtable because topology is regular");
                return false;  // don't need to futt, so declare it non-futtable
            }
        }

        // XXX TODO: still no good!  we need to declare "frucht 3(2.5)" non-futtable, but haven't figured out how to detect that yet, since incidence counts are masquerading as the futtable case!  Well at least it rejects "fruct 3".
        if (progressWriter != null) progressWriter.println("        deciding futtable!");

        return true;
    }  // decideWhetherFuttable

    private void init(String schlafliProduct,
                      int[] intLengths, // number of segments per edge, possibly per-face
                      double[] doubleLengths, // edge length / length of first edge segment, possibly per-face
                      java.io.PrintWriter progressWriter)
    {
        CHECK(intLengths.length == doubleLengths.length);
        for (int i = 0; i < intLengths.length; ++i) {
          if (intLengths[i] < 1)
              throw new IllegalArgumentException("PolytopePuzzleDescription called with intLength="+intLengths[i]+", min legal intLength is 1");
          if (doubleLengths[i] <= 0)
              throw new IllegalArgumentException("PolytopePuzzleDescription called with doubleLength="+doubleLengths[i]+", doubleLength must be positive");
        }

        if (progressWriter != null)
        {
            if (VecMath.equalsExactly(doubleLengths, VecMath.intToDouble(intLengths)))
                progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+VecMath.toString(intLengths)+"...");
            else
                progressWriter.println("Attempting to make a puzzle \""+schlafliProduct+"\" of length "+VecMath.toString(intLengths)+" ("+VecMath.toString(doubleLengths)+")...");
            progressWriter.print("    Constructing polytope...");
            progressWriter.flush();
        }
        this.originalPolytope = CSG.makeRegularStarPolytopeProductJoinFromString(schlafliProduct);

        if (this.originalPolytope.p.dim < 2)
        {
            throw new IllegalArgumentException("PolytopePuzzleDescription can't do puzzles of dimension "+this.originalPolytope.p.dim+" (< 2)");
        }

        if (progressWriter != null)
        {
            progressWriter.println(" done ("+originalPolytope.p.facets.length+" facets).");
            progressWriter.flush();
        }

        int nDims = originalPolytope.p.dim;  // == originalPolytope.fullDim

        CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
        CSG.Polytope[] originalVerts = originalElements[0];
        CSG.Polytope[] originalFacets = originalElements[nDims-1];
        int nFacets = originalFacets.length;
        int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();

        // Mark each original facet with its facet index.
        // These marks will persist even aver we slice up into stickers,
        // so that will give us the sticker-to-original-facet-index mapping.
        // Also mark each vertex with its vertex index... etc.
        {
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
            for (int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
            {
                originalElements[iDim][iElt].setAux(new Integer(iElt));
            }
        }

        //
        // Figure out the facet inward normals and offsets;
        // these will be used for computing where cuts should go.
        //
        this.facetInwardNormals = new double[nFacets][nDims];
        double[] facetOffsets = new double[nFacets];
        for (int iFacet = 0; iFacet < nFacets; ++iFacet)
        {
            CSG.Polytope facet = originalFacets[iFacet];
            CSG.Hyperplane plane = facet.contributingHyperplanes[0];
            VecMath.vxs(facetInwardNormals[iFacet], plane.normal, -1);
            facetOffsets[iFacet] = -plane.offset;
            CHECK(facetOffsets[iFacet] < 0.);
            double invNormalLength = 1./VecMath.norm(facetInwardNormals[iFacet]);
            VecMath.vxs(facetInwardNormals[iFacet], facetInwardNormals[iFacet], invNormalLength);
            facetOffsets[iFacet] *= invNormalLength;
        }

        //
        // Figure out the circumRadius (farthest vertex from origin)
        // and inRadius (closest facet plane to origin)
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
            for (int iFacet = 0; iFacet < originalFacets.length; ++iFacet)
            {
                double thisFaceDist = -facetOffsets[iFacet];
                if (thisFaceDist < nearestFaceDist)
                    nearestFaceDist = thisFaceDist;
            }
            _inRadius = (float)nearestFaceDist;
        }


        //
        // So we can easily find the opposite facet of a given facet...
        //
        this.facet2OppositeFacet = new int[nFacets];
        {
            FuzzyPointHashTable table = new FuzzyPointHashTable(1e-9, 1e-8, 1./64);  // 1e-9, 1e-8, 1/128 made something hit a wall on the omnitruncated 120cell
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                table.put(facetInwardNormals[iFacet], originalFacets[iFacet]);
            double[] oppositeNormalScratch = new double[nDims];
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                VecMath.vxs(oppositeNormalScratch, facetInwardNormals[iFacet], -1.);
                CSG.Polytope opposite = (CSG.Polytope)table.get(oppositeNormalScratch);
                facet2OppositeFacet[iFacet] = opposite==null ? -1 : (Integer)opposite.getAux();
                //System.err.print("("+iFacet+":"+facet2OppositeFacet[iFacet]+")");
            }
        }

        //
        // Figure out exactly what cuts are wanted
        // for each facet.  Cuts parallel to two opposite facets
        // will appear in both facets' cut lists.
        //
        // Note, we store facet inward normals rather than outward ones,
        // so that, as we iterate through the slicemask bit indices later,
        // the corresponding cut offsets will be in increasing order,
        // for my sanity.
        //
        this.facetCutOffsets = new double[nFacets][];
        int[] whichLengthToUseForFacet = new int[nFacets];
        {
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                // Which length do we use?
                // Use the one on the axis closest to the normal, for now,
                // and repeat entries if not enough.  This gets it right for the (a)x(b)x(c)x(d), for example.
                whichLengthToUseForFacet[iFacet] = VecMath.maxabsi(facetInwardNormals[iFacet]) % intLengths.length;
                int intLength = intLengths[whichLengthToUseForFacet[iFacet]];
                double doubleLength = doubleLengths[whichLengthToUseForFacet[iFacet]];

                double fullThickness = 0.;
                {
                    // iVert = index of some vertex on facet iFacet
                    int iVert = originalIncidences[nDims-1][iFacet][0][0];
                    // iVertEdges = indices of all edges incident on vert iVert
                    int[] iVertsEdges = originalIncidences[0][iVert][1];
                    // Find an edge incident on vertex iVert
                    // that is NOT incident on facet iFacet..
                    for (int i = 0; i < iVertsEdges.length; ++i)
                    {
                        int iEdge = iVertsEdges[i];
                        int[] iEdgesFacets = originalIncidences[1][iEdge][nDims-1];
                        int j;
                        for (j = 0; j < iEdgesFacets.length; ++j)
                            if (iEdgesFacets[j] == iFacet)
                                break; // iEdge is incident on iFacet-- no good
                        if (j == iEdgesFacets.length)
                        {
                            // iEdge is not incident on iFacet-- good!
                            int jVert0 = originalIncidences[1][iEdge][0][0];
                            int jVert1 = originalIncidences[1][iEdge][0][1];
                            CHECK((jVert0==iVert) != (jVert1==iVert));

                            double[] edgeVec = VecMath.vmv(
                                            originalVerts[jVert1].getCoords(),
                                            originalVerts[jVert0].getCoords());
                            double thisThickness = VecMath.dot(edgeVec, facetInwardNormals[iFacet]);
                            if (thisThickness < 0.)
                                thisThickness *= -1.;

                            // If there are more than one neighbor vertex
                            // that's not on this facet, pick one that's
                            // closest to the facet plane.  This can
                            // happen only if the vertex figure is NOT a simplex
                            // (e.g. it happens for the icosahedron).
                            if (thisThickness > 1e-6
                             && (fullThickness == 0. || thisThickness < fullThickness))
                                fullThickness = thisThickness;
                        }
                    }
                }
                CHECK(fullThickness != 0.); // XXX actually this fails if puzzle dimension <= 1, maybe should disallow

                boolean isPrismOfThisFacet;
                {
                  // We guess it's a prism of this facet if all the number of elements
                  // match.  I suspect that's a sufficient condition, but I haven't proved it.
                  isPrismOfThisFacet = true;  // until proven otherwise;
                  for (int iDim = 0; iDim < nDims; ++iDim)
                  {
                    int nLesserDimensionalEltsThisFacet = iDim==0 ? 0 : originalIncidences[nDims-1][iFacet][iDim-1].length;
                    int nThisDimensionalEltsThisFacet = originalIncidences[nDims-1][iFacet][iDim].length;
                    int nThisDimensionalEltsTotal = originalIncidences[nDims][0][iDim].length;
                    if (nThisDimensionalEltsTotal != 2 * nThisDimensionalEltsThisFacet + nLesserDimensionalEltsThisFacet)
                    {
                      isPrismOfThisFacet = false;
                      break;
                    }
                  }
                }

                double sliceThickness = fullThickness / doubleLength;

                /*
                   Think about what's appropriate for simplex...
                        thickness = 1/3 of full to get upside down tet in middle,
                                        with its verts poking the facets
                        thickness = 1/4 of full to get nothing in middle
                        thickness = 1/5 of full to get nice rightside up cell in middle
                                        YES, this is what 3 should do I think


                   But for triangular prism prism,
                            1/4 of full is the nice one for 3
                */

                int nNearCuts = intLength / 2; // (n-1)/2 if odd, n/2 if even
                int nFarCuts = facet2OppositeFacet[iFacet]==-1 ? 0 :
                               intLength%2==0 && isPrismOfThisFacet ? nNearCuts-1 :
                               nNearCuts;
                facetCutOffsets[iFacet] = new double[nNearCuts + nFarCuts];

                for (int iNearCut = 0; iNearCut < nNearCuts; ++iNearCut)
                    facetCutOffsets[iFacet][iNearCut] = facetOffsets[iFacet] + (iNearCut+1)*sliceThickness;
                // we'll fill in the far cuts in another pass
            } // for iFacet

            // Fill in far cuts of each facet,
            // from near cuts of the opposite facet.
            // Note the opposite facet may have a different
            // offset from the origin, and different slice thickness
            // (e.g. the truncated simplex in 3 or 4 dimensions).
            // CBB: maybe should add these *after* we slice?  Originally I did all the actual slicing from one end, but that messes up the alt shrink to points and futt, so now we do half from one end and half from the other  (or should, anyway, I think?)
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                int iOppositeFacet = facet2OppositeFacet[iFacet];
                int nNearCuts = intLengths[whichLengthToUseForFacet[iFacet]] / 2; // same as in previous pass
                int nFarCuts = facetCutOffsets[iFacet].length - nNearCuts;  // this will be 0 if there's no opposite face
                for (int iFarCut = 0; iFarCut < nFarCuts; ++iFarCut)
                    facetCutOffsets[iFacet][nNearCuts+nFarCuts-1-iFarCut] = -facetCutOffsets[iOppositeFacet][iFarCut];
            }
        }

        //System.out.println("facet inward normals = "+com.donhatchsw.util.Arrays.toStringCompact(facetInwardNormals));
        //System.out.println("cut offsets = "+com.donhatchsw.util.Arrays.toStringCompact(facetCutOffsets));

        // Need further cuts only if there's a square, e.g. {5,3,3} 2 doesn't need it
        // (hmm, do triangles need it?  separate scheme?)
        // And "there's a square" might be equivalent to "the whole thing is a prism of some sort", I'm not sure.
        // These heuristics are a bit wacky; should revisit them.
        // Unfortunately it's not feasible to do further cuts in *all* circumstances in which we technically
        // need them, e.g. on a huge object that's not sliced at all, that we're just viewing;
        // that's why we check for at least one cut.
        boolean doFurtherCuts;
        {
            boolean theresASquare = false;  // weird condition, not entirely sure whether it's appropriate
            boolean theresACut = false;
            boolean theresAFaceWithNoCut = false;
            boolean theresADoubleLength2 = false;
            {
              for (int i = 0; i < originalElements[2].length; ++i)
                  if (originalElements[2][i].facets.length == 4)
                  {
                      theresASquare = true;
                      break;
                  }
              for (int i = 0; i < doubleLengths.length; ++i)
                  if (doubleLengths[i] == 2.)  // XXX make fuzzy?
                  {
                    theresADoubleLength2 = true;
                    break;
                  }
              for (int iFacet = 0; iFacet < nFacets; ++iFacet) {
                  if (facetCutOffsets[iFacet].length == 0)
                      theresAFaceWithNoCut = true;
                  else
                      theresACut = true;
              }
            }
            doFurtherCuts = nDims==4
                         && theresACut
                         && theresASquare
                         && (theresADoubleLength2 || theresAFaceWithNoCut);
        }

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
            long startTimeMillis = System.currentTimeMillis();
            int maxCuts = -1; // unlimited
            //maxCuts = 6; // set to some desired number for debugging

            //
            // First find out how many cuts we are going to make...
            //
            int nTotalCuts = 0;
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                if (maxCuts >= 0 && nTotalCuts >= maxCuts) break;
                if (facet2OppositeFacet[iFacet] != -1
                 && facet2OppositeFacet[iFacet] < iFacet)
                    continue; // already saw opposite facet and made the cuts
                for (int iCut = 0; iCut < facetCutOffsets[iFacet].length; ++iCut)
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
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                if (maxCuts >= 0 && iTotalCut >= maxCuts) break;
                if (facet2OppositeFacet[iFacet] != -1
                 && facet2OppositeFacet[iFacet] < iFacet)
                    continue; // already saw opposite facet and made the cuts
                //System.out.println("REALLY doing facet "+iFacet);
                for (int iCut = 0; iCut < facetCutOffsets[iFacet].length; ++iCut)
                {
                    if (maxCuts >= 0 && iTotalCut >= maxCuts) break;
                    CSG.Hyperplane cutHyperplane = new CSG.Hyperplane(
                        facetInwardNormals[iFacet],
                        facetCutOffsets[iFacet][iCut]);
                    Object auxOfCut = new CutInfo(iFacet,iCut);
                    slicedPolytope = CSG.sliceElements(slicedPolytope, slicedPolytope.p.dim-1, cutHyperplane, auxOfCut, /*sizes=*/null);
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
            CHECK(iTotalCut == nTotalCuts);
            long endTimeMillis = System.currentTimeMillis();

            if (progressWriter != null)
            {
                progressWriter.println(" done ("+slicedPolytope.p.facets.length+" stickers) ("+millisToSecsString(endTimeMillis-startTimeMillis)+" seconds)");
                progressWriter.flush();
            }

            //
            // Have to compute the shrink-to points here
            // before doing further cuts,
            // since it assumes there will be at most two ridges
            // per sticker from a given facet...
            //
            // Calculate the alternate sticker centers;
            // intuitively, these are the shrink-to points
            // on the facet boundaries.
            //
            {
                if (progressWriter != null)
                {
                    progressWriter.print("    Computing alternate sticker shrink-to points on facet boundaries... ");
                    progressWriter.flush();
                }
                this.stickerAltCentersF = computeStickerAltCentersF(
                                              slicedPolytope,
                                              facet2OppositeFacet,
                                              intLengths,
                                              doubleLengths,
                                              whichLengthToUseForFacet);
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
                    CSG.Polytope[] stickers = slicedPolytope.p.getAllElements()[nDims-1];
                    for (int iSticker = 0; iSticker < stickers.length; ++iSticker)
                    {
                        stickers[iSticker].pushAux(new Integer(iSticker));
                    }
                }

                {  // DEBUGGING
                    CSG.Polytope[] stickers = slicedPolytope.p.getAllElements()[nDims-1];
                    for (int iSticker = 0; iSticker < stickers.length; ++iSticker) {
                      stickers[iSticker].pushAux(stickers[iSticker].popAux());
                    }
                }

                if (progressWriter != null)
                {
                    progressWriter.print("    Further slicing for grips("+slicedPolytope.p.getAllElements()[2].length+" polygons)");
                    progressWriter.flush();
                }
                for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                {
                    if (facetCutOffsets[iFacet].length < 2)
                    {
                        // Not enough cuts along the edge perpendicular to this facet.
                        // Make another cut.
                        CSG.Hyperplane cutHyperplane;
                        if (facetCutOffsets[iFacet].length == 0)
                        {
                            // There are no cuts here yet.
                            // Put a cut 1/4 of the way from this facet to the end of the incident edge.
                            // XXX THIS ISNT RIGHT YET-- NEED TO USE FULLTHICKNESSES[iFacet].  (But it's right for boxes)
                            cutHyperplane = new CSG.Hyperplane(
                                facetInwardNormals[iFacet],
                                facetOffsets[iFacet] / 2.);
                        }
                        else
                        {
                            // There's 1 cut already.  Bisect the near piece.
                            cutHyperplane = new CSG.Hyperplane(
                                facetInwardNormals[iFacet],
                                (facetOffsets[iFacet]+facetCutOffsets[iFacet][0])/2.);
                        }


                        Object auxOfCut = null; // note this should not mess up the showFurtherCuts thing, since we are now dividing the ridges of the stickers (e.g. the polygons, in the usual 4d case) so the divided ridges themselves will still have an aux... it's the peaks (i.e. nDims-3 dimensional elements, i.e. edges in the usual 4d case) that will get nulls for auxes, and that's fine
                        slicedPolytope = CSG.sliceElements(slicedPolytope, slicedPolytope.p.dim-2, cutHyperplane, auxOfCut,
                            new int[]{3,4}); // sizes (further-cut only squares and triangles) (XXX that's not quite working like I intended... I wanted to further-cut only when *original* facets were squares. bleah!)
                        if (progressWriter != null)
                        {
                            progressWriter.print("."); // one dot per cut
                            progressWriter.flush();
                        }
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
                    CSG.Polytope[] stickers = slicedPolytope.p.getAllElements()[nDims-1];
                    float[][] oldStickerAltCentersF = this.stickerAltCentersF;
                    float[][] newStickerAltCentersF = new float[stickers.length][];
                    for (int iSticker = 0; iSticker < stickers.length; ++iSticker)
                    {
                        newStickerAltCentersF[iSticker] = oldStickerAltCentersF[(Integer)stickers[iSticker].popAux()];
                    }
                    this.stickerAltCentersF = newStickerAltCentersF;
                }
            } // if doFurtherCuts

            if (true)
            {
                if (progressWriter != null)
                {
                    progressWriter.print("    Fixing facet orderings... ");
                    progressWriter.flush();
                }
                startTimeMillis = System.currentTimeMillis();
                CSG.orientDeepCosmetic(slicedPolytope);
                endTimeMillis = System.currentTimeMillis();
                if (progressWriter != null)
                {
                    progressWriter.println(" done ("+millisToSecsString(endTimeMillis-startTimeMillis)+" seconds)");
                    progressWriter.flush();
                }
            }
        } // slice


        CSG.Polytope[] stickers = slicedPolytope.p.getAllElements()[nDims-1];
        int nStickers = stickers.length;

        //
        // Figure out the mapping from sticker to facet.
        //
        this.sticker2face = new int[nStickers];
        {
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                sticker2face[iSticker] = (Integer)stickers[iSticker].getAux();
            }
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
            //             merge the two incident stickers that meet at this polygon; those stickers are part of a single cubie

            CSG.Polytope[] slicedRidges = slicedPolytope.p.getAllElements()[nDims-2];
            int[][][][] allSlicedIncidences = slicedPolytope.p.getAllIncidences();
            for (int iSlicedRidge = 0; iSlicedRidge < slicedRidges.length; ++iSlicedRidge)
            {
                CSG.Polytope ridge = slicedRidges[iSlicedRidge];  // poly, in the usual 4d case
                // ridge.aux is now either an Integer (the index
                // of the original ridge it was a part of)
                // or a CutInfo (if it was created by one of the primary, i.e. non-further, cuts).
                // The "further" cuts didn't create any ridges (polys), they only subdivided them,
                // so there are no null ridge.aux's.
                CHECK(ridge.getAux() != null);
                boolean ridgeIsFromOriginal = (ridge.getAux() instanceof Integer);
                if (ridgeIsFromOriginal) // if it's not from a cut
                {
                    // Find the two stickers that meet at this ridge...
                    int[] indsOfStickersContainingThisRidge = allSlicedIncidences[nDims-2][iSlicedRidge][nDims-1];
                    CHECK(indsOfStickersContainingThisRidge.length == 2);
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
                progressWriter.flush();
            }
            // XXX note, we could easily collapse the cubie indicies
            // XXX so that they are consecutive, if we cared
        }

        //
        // Find the facet centers and sticker centers.
        // The center of mass of the vertices is probably
        // as good as anything, for this
        // (when we get shrinking right, it won't
        // actually use centers, I don't think)
        //
        double[][] facetCentersD = new double[nFacets][nDims];
        {
            {
                for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                    CSG.cgOfVerts(facetCentersD[iFacet], originalFacets[iFacet]);
            }
            this.stickerCentersD = new double[nStickers][nDims];
            this.stickerCentersHashTable = new FuzzyPointHashTable(1e-9, 1e-8, 1./128);
            {
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    CSG.cgOfVerts(stickerCentersD[iSticker], stickers[iSticker]);
                    stickerCentersHashTable.put(stickerCentersD[iSticker], new Integer(iSticker));
                }
            }
            this.facetCentersF = VecMath.doubleToFloat(facetCentersD);
            this.stickerCentersF = VecMath.doubleToFloat(stickerCentersD);
        }

        //
        // PolyFromPolytope doesn't seem to like the fact that
        // some elements have an aux and some don't... so clear all the vertex
        // auxs.
        // (Actually now they all have aux.)
        // XXX why does this seem to be a problem for nonregular cross products but not for regulars?  figure this out
        //
        if (false)  // XXX tentatively trying setting this to false... I think it's ok.  but should eventually clear them to save memory when done... I think? not sure
        {
            CSG.Polytope[][] allElements = slicedPolytope.p.getAllElements();
            for (int iDim = 0; iDim < allElements.length; ++iDim)
            for (int iElt = 0; iElt < allElements[iDim].length; ++iElt)
                allElements[iDim][iElt].setAux(null);
        }

        // We'll need to keep track of any swaps we do...
        int[][] stickerPolyToOriginalStickerPoly = new int[nStickers][];

        //
        // Get the rest verts (with no shrinkage)
        // and the sticker polygon indices (i.e. mapping from sticker-and-polyThisSticker to vert).
        // This is dimension-specific.
        //
        double restVerts[][];
        if (nDims <= _nDisplayDims) // if 4d or less
        {
            {
                CSG.Polytope[] allSlicedVerts = slicedPolytope.p.getAllElements()[0];
                for (int iVert = 0; iVert < allSlicedVerts.length; ++iVert)
                {
                    allSlicedVerts[iVert].pushAux(-1);
                }

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
                        // This is the thickness in the w direction
                        double padRadius = .1;
                        //double padRadius = .2;
                        //double padRadius = .25;  // good for debugging
                        //double padRadius = .75;  // even better for debugging
                        sticker4d = CSG.cross(new CSG.SPolytope(0,1,sticker),
                                              CSG.makeHypercube(new double[_nDisplayDims-nDims], padRadius)).p;
                        CSG.Polytope[] stickerVerts4d = sticker4d.getAllElements()[0];
                        for (int iVert = 0; iVert < stickerVerts4d.length; ++iVert)
                        {
                            stickerVerts4d[iVert].pushAux(-1);
                            stickerVerts4d[iVert].getCoords()[3] *= -1; // XXX FUDGE-- and this is not really legal... should do this afterwards
                        }
                    }
                    // XXX note, we MUST step through the polys in the order in which they appear in getAllElements, NOT the order in which they appear in the facets list.  however, we need to get the sign from the facets list!
                    CSG.Polytope[] polysThisSticker = sticker4d.getAllElements()[2];
                    stickerInds[iSticker] = new int[polysThisSticker.length][];

                    for (int iPolyThisSticker = 0; iPolyThisSticker < polysThisSticker.length; ++iPolyThisSticker)
                    {
                        CSG.Polytope polygon = polysThisSticker[iPolyThisSticker];

                        //CHECK(polygon == slicedPolytope.p.getAllElements()[2][slicedPolytope.p.getAllIncidences()[nDims-1][iSticker][2][iPolyThisSticker]]);  // sanity check; we're going to need this fact later when deducing grips

                        stickerInds[iSticker][iPolyThisSticker] = new int[polygon.facets.length];
                        for (int iVertThisPoly = 0; iVertThisPoly < polygon.facets.length; ++iVertThisPoly)
                        {
                            // assert this polygon is oriented
                            // and nicely ordered the way we expect...
                            CSG.SPolytope thisEdge = polygon.facets[iVertThisPoly];
                            CSG.SPolytope nextEdge = polygon.facets[(iVertThisPoly+1)%polygon.facets.length];
                            CHECK(thisEdge.p.facets.length == 2);
                            CHECK(thisEdge.p.facets[0].sign == -1);
                            CHECK(thisEdge.p.facets[1].sign == 1);
                            CHECK(thisEdge.sign == -1 || thisEdge.sign == 1);
                            CHECK(nextEdge.sign == -1 || nextEdge.sign == 1);
                            CSG.Polytope vertex = thisEdge.p.facets[thisEdge.sign==-1?0:1].p;
                            CHECK(vertex == nextEdge.p.facets[nextEdge.sign==-1?1:0].p);
                            int iVert = (Integer)vertex.getAux();
                            if (iVert == -1)
                            {
                                iVert = nVerts++;
                                restVerts[iVert] = vertex.getCoords(); // okay to share with it, we aren't going to change it
                                vertex.setAux(iVert);
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
                    // clear the vertices' (pushed) aux indices after each sticker,
                    // so that different stickers won't share vertices.
                    for (int iPolyThisSticker = 0; iPolyThisSticker < polysThisSticker.length; ++iPolyThisSticker)
                    {
                        CSG.Polytope polygon = polysThisSticker[iPolyThisSticker];
                        for (int iVertThisPoly = 0; iVertThisPoly < polygon.facets.length; ++iVertThisPoly)
                        {
                            CSG.SPolytope thisEdge = polygon.facets[iVertThisPoly];
                            CSG.Polytope vertex = thisEdge.p.facets[thisEdge.sign==-1?0:1].p;
                            vertex.setAux(-1);
                        }
                    }
                }
                CHECK(nVerts == restVerts.length);

                for (int iVert = 0; iVert < allSlicedVerts.length; ++iVert)
                {
                    allSlicedVerts[iVert].popAux();
                }
            }

            //
            // Fix up the indices on each sticker so that
            // the first vertex on the second face
            // does not occur on the first facet;
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
                    stickerPolyToOriginalStickerPoly[iSticker] = VecMath.identityperm(stickerInds[iSticker].length);
                    if (doFurtherCuts)
                    {
                        // Find a poly that's not adjacent to polys[0] at all: doesn't even share any verts.
                        int[][] polys = stickerInds[iSticker];
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
                                // WARNING: we know this is correct only because we do at most one swap.  If we did more than one swap, then the permutation would not be self-inverse any more and we'd have to think about whether we're doing it correctly or whether we are backwards.
                                com.donhatchsw.util.Arrays.swap(stickerPolyToOriginalStickerPoly[iSticker], 1,
                                                                stickerPolyToOriginalStickerPoly[iSticker], jPoly);
                                break;
                            }
                        }
                        // Had to find one... hmm, maybe not!
                        // This fails on the runcinateds:
                        //   '(1)---(0)---(0)---(1) 2'
                        //   '(1)-4-(0)---(0)---(1) 2'
                        //   '(1)---(0)-4-(0)---(1) 2'
                        //   '(1)-5-(0)---(0)---(1) 2'
                        // where polys.length is 7 and polys is:
                        // {{716,717,718},{719,718,717},{718,720,716},{717,716,721},{721,722,719,717},{718,719,722,720},{720,722,721,716}}
                        // (4 tris, 3 quads)
                        // Ok, that's, topologically, a cube with one vertex truncated all the way to the three adjacent verts.
                        // I guess it happens.  It's benign in that case, though (I think?)
                        // TODO: make a tighter assertion that's valid.
                        if (false)
                        {
                            System.out.println("polys.length = "+polys.length);
                            System.out.println("polys = "+com.donhatchsw.util.Arrays.toStringCompact(polys));
                            CHECK(polys.length == 4 || jPoly < polys.length);
                        }
                    }

                    int[] polygon0 = stickerInds[iSticker][0];
                    int[] polygon1 = stickerInds[iSticker][1];
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
                        int[] cycled = new int[polygon1.length];
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
                int[][][] stickerIncidences = slicedPolytope.p.getAllIncidences()[nDims-1];
                int nPolygons = slicedPolytope.p.getAllElements()[2].length;
                this.adjacentStickerPairs = new int[nPolygons][2][];
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    int[] thisStickersIncidentPolygons = stickerIncidences[iSticker][nDims-2];
                    for (int iPolyThisSticker = 0; iPolyThisSticker < thisStickersIncidentPolygons.length; ++iPolyThisSticker)
                    {
                        int iPoly = thisStickersIncidentPolygons[iPolyThisSticker];
                        int j = adjacentStickerPairs[iPoly][0]==null ? 0 : 1;
                        CHECK(adjacentStickerPairs[iPoly][j] == null);
                        adjacentStickerPairs[iPoly][j] = new int[] {iSticker,iPolyThisSticker};
                    }
                }
                for (int iPoly = 0; iPoly < adjacentStickerPairs.length; ++iPoly)
                    for (int j = 0; j < 2; j++)
                        CHECK(adjacentStickerPairs[iPoly][j] != null);
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
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                facetCentersF[iFacet] = (float[])com.donhatchsw.util.Arrays.append(facetCentersF[iFacet], 0.f);
                facetInwardNormals[iFacet] = (double[])com.donhatchsw.util.Arrays.append(facetInwardNormals[iFacet], 0.f);
            }
        }

        this.vertsF = VecMath.doubleToFloat(restVerts);

        this.futtable = decideWhetherFuttable(intLengths, progressWriter);
        if (progressWriter != null)
        {
            progressWriter.println("    Polytope is "+(this.futtable ? "futtable! (but need to enable futting in UI as well in order to futt)." : "not futtable."));
        }
        this.intLengthsForFutt = futtable ? VecMath.copyvec(intLengths) : null;  // it's hard for the on-the-fly futt stuff to figure it out otherwise
        this.facetOffsetsForFutt = futtable ? facetOffsets : null;
        this.vertsDForFutt = futtable ? restVerts : null;

        //
        // Now think about the twist grips.
        // There will be one grip at each vertex,edge,facet center
        // of either the original polytope (if 3d)
        // or each cell of the original polytope (if 4d).
        // XXX woops, I'm retarded, 3d doesn't have that...
        // XXX but actually it wouldn't hurt, could just make that
        // XXX rotate the whole puzzle.
        //
        if (nDims == 4 && intLengths.length == 1 && intLengths[0] == 1)
        {
            // Don't bother with grips for now, it's taking too long
            // for the big ones
            int nGrips = 0;
            this.gripDirsF = new float[nGrips][];
            this.gripOffsF = new float[nGrips][];
            this.gripSymmetryOrders = new int[nGrips];
            this.gripUsefulMats = new double[nGrips][nDims][nDims];
            this.gripTwistRotationFixedPoints = new double[nGrips][nDims];
            this.grip2face = new int[nGrips];
        }
        else
        {
            if (progressWriter != null)
            {
                progressWriter.print("    Thinking about possible twists...");
                progressWriter.flush();
            }
            long startTimeMillis = System.currentTimeMillis();
            if (nDims <= 4)
            {
                boolean doTheOddFaceIn3dThing = true;
                int nGrips = 0;
                for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                {
                    CSG.Polytope[][] allElementsOfFacet = originalFacets[iFacet].getAllElements();
                    if (nDims == 4)
                        for (int iDim = 0; iDim <= 3; ++iDim) // yes, even for cell center, which doesn't do anything
                            nGrips += allElementsOfFacet[iDim].length;
                    else if (nDims ==3)
                    {
                        nGrips += 2;
                        nGrips += allElementsOfFacet[1].length;
                        if (doTheOddFaceIn3dThing)
                        {
                            // Note, the actual condition used is slightly more conservative, so
                            // we may end up with fewer grips than this.
                            // If so, we'll adjust at the end.
                            if (allElementsOfFacet[1].length % 2 == 1)
                                nGrips += allElementsOfFacet[1].length;
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
                this.gripSymmetryOrdersFutted = this.futtable ? new int[nGrips] : null;
                this.gripUsefulMats = new double[nGrips][_nDisplayDims][_nDisplayDims];
                this.gripTwistRotationFixedPoints = new double[nGrips][_nDisplayDims];
                this.grip2face = new int[nGrips];
                this.grip2facetEltForFutt = this.futtable ? new int[nGrips][/*2*/] : null;
                int minDim = nDims==4 ? 0 : 2;
                int maxDim = nDims==4 ? 3 : 2; // yes, even for cell center, which doesn't do anything
                int[][][] originalFacetElt2grip = new int[nFacets][maxDim+1][];
                {
                    CSG.SPolytope padHypercube = nDims < 4 ? CSG.makeHypercube(4-nDims) : null;
                    int iGrip = 0;
                    for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                    {
                        CSG.Polytope facet = originalFacets[iFacet];
                        if (padHypercube != null)
                            facet = CSG.cross(new CSG.SPolytope(0,1,facet), padHypercube).p;
                        CSG.Polytope[][] allElementsOfFacet = facet.getAllElements();
                        int[][][][] allIncidencesThisFacet = facet.getAllIncidences();
                        for (int iDim = minDim; iDim <= maxDim; ++iDim)
                        {
                            originalFacetElt2grip[iFacet][iDim] = VecMath.fillvec(allElementsOfFacet[iDim].length, -1);
                            for (int iElt = 0; iElt < allElementsOfFacet[iDim].length; ++iElt)
                            {
                                CSG.Polytope elt = allElementsOfFacet[iDim][iElt];

                                // gripUsefulMats[0] should be (in the direction of)
                                // the point closest to the origin
                                // on the affine subspace containing the face to twist.
                                if (false)
                                {
                                  // Naive-- use facet center.  (actually cg of verts of facet, currently, I think)
                                  // This works for uniform polytopes, but not in general
                                  // (e.g. it's wrong for frucht).
                                  VecMath.copyvec(gripUsefulMats[iGrip][0], facetCentersD[iFacet]);
                                  VecMath.zerovec(gripTwistRotationFixedPoints[iGrip]);
                                }
                                else
                                {
                                  // TODO: seems to be better than naive for frucht, but still not quite right-- the rotation isn't exactly the facet's plane.  Wtf?
                                  // More principled approach, that works for frucht.
                                  double[] facetInwardNormal = this.facetInwardNormals[iFacet];
                                  // it's pointing in the opposite direction from what we want,
                                  // but the following will reverse it, since currentDot>0
                                  // and desiredDot<0.
                                  double currentDot = VecMath.dot(facetInwardNormal, facetInwardNormal);
                                  double desiredDot = VecMath.dot(facetCentersD[iFacet], facetInwardNormal);
                                  VecMath.vxs(gripUsefulMats[iGrip][0], facetInwardNormal, desiredDot/currentDot);
                                  VecMath.copyvec(gripTwistRotationFixedPoints[iGrip], facetCentersD[iFacet]);
                                }

                                // gripUsefulMats[1] should be a vector pointing
                                // from gripUsefulMats[0] in the direction of twist axis,
                                // in the hyperplane being twisted.
                                CSG.cgOfVerts(gripUsefulMats[iGrip][1], elt);
                                VecMath.vmv(gripUsefulMats[iGrip][1],
                                            gripUsefulMats[iGrip][1],
                                            facetCentersD[iFacet]);

                                this.gripDirsF[iGrip] = VecMath.doubleToFloat(gripUsefulMats[iGrip][0]);
                                this.gripOffsF[iGrip] = VecMath.doubleToFloat(gripUsefulMats[iGrip][1]);
                                if (VecMath.normsqrd(this.gripOffsF[iGrip]) <= 1e-4*1e-4)
                                {
                                    // This happens for the center sticker of a facet.
                                    // Make it a "can't twist that" sticker.
                                    VecMath.zeromat(gripUsefulMats[iGrip]);
                                    this.gripSymmetryOrders[iGrip] = 0;
                                }
                                else
                                {
                                    VecMath.extendAndGramSchmidt(2,4,
                                                                 gripUsefulMats[iGrip],
                                                                 gripUsefulMats[iGrip]);
                                    boolean isEdgeFlipIn3d = nDims==3 && Math.abs(gripUsefulMats[iGrip][1][3]) < 1e-6;  // i.e. gripUsefulMats[iGrip] has w=0, i.e. it lies in the xyz space
                                    int maxOrder = (nDims==4 ? (iDim==0 ? allIncidencesThisFacet[0][iElt][1].length : // arity (i.e. number of incident edges) of this vertex on this hyperface
                                                               iDim==1 ? 2 :
                                                               iDim==2 ? elt.facets.length :
                                                               iDim==3 ? 0 : -1) :
                                                    nDims==3 ? (isEdgeFlipIn3d ? 2 : originalFacets[iFacet].facets.length) :  // not the proxy facet!  (XXX what did I mean? elt.facets.length?  I think that would work too, actually could take the gcd of the two)
                                                    nDims==2 ? 4 :
                                                    -1);

                                    //System.out.println("maxOrder = "+maxOrder);
                                    this.gripSymmetryOrders[iGrip] = CSG.calcRotationGroupOrder(
                                                                           originalPolytope.p,
                                                                           maxOrder,
                                                                           gripUsefulMats[iGrip]);
                                    if (this.futtable)
                                    {
                                        if (nDims == 4)
                                        {
                                            // TODO: this isn't right yet-- actually need to analyze to figure it out
                                            this.gripSymmetryOrdersFutted[iGrip] = maxOrder;
                                        }
                                        else if (nDims == 3)
                                        {
                                            CHECK(iDim == 2);
                                            if (intLengths.length == 1 && intLengths[0] == 1) {  // no cuts
                                              this.gripSymmetryOrdersFutted[iGrip] = 1;
                                            } else if (isEdgeFlipIn3d) {
                                              this.gripSymmetryOrdersFutted[iGrip] = 2;
                                            } else {
                                              this.gripSymmetryOrdersFutted[iGrip] = elt.facets.length;  // or originalFacets[iFacet].facets.length?  actually could take the gcd of the two
                                            }
                                        }
                                    }

                                    if (false) {  // set to true to *disable* order-1 rotations, i.e. 360 degree rotations.  This is experimental.
                                        // XXX don't do it like this-- we should store the actual order, 1, regardless, but then make the decision of whether it's functional or not based on a flag.
                                        if (this.gripSymmetryOrders[iGrip] == 1) {
                                            VecMath.zeromat(gripUsefulMats[iGrip]);
                                            this.gripSymmetryOrders[iGrip]  = 0;  // XXX experimental
                                        }
                                    }
                                }
                                // Make sure dirs and offs are normalized (if not zero)
                                this.gripDirsF[iGrip] = VecMath.normalize(this.gripDirsF[iGrip]);
                                if (this.gripSymmetryOrders[iGrip] != 0)
                                    this.gripOffsF[iGrip] = VecMath.normalize(this.gripOffsF[iGrip]);
                                this.grip2face[iGrip] = iFacet;
                                if (elt.getAux() != null && elt.getAux() instanceof Integer)  // XXX it's null sometimes, in 3d, not sure why yet.  in this case we won't be able to look up the grip ... ? but it doesn't matter I don't think, originalFacetElt2grip is used only in 4d
                                {
                                   int iEltGlobal = (Integer)elt.getAux();  // what? we don't use this?  actually I think that's correct
                                   CHECK(originalFacetElt2grip[iFacet][iDim][iElt] == -1);
                                   originalFacetElt2grip[iFacet][iDim][iElt] = iGrip;
                                   if (futtable)
                                   {
                                       grip2facetEltForFutt[iGrip] = new int[] {iDim, iElt};
                                   }
                                }

                                iGrip++;
                                if (doTheOddFaceIn3dThing)
                                {
                                    if (nDims==3 && originalFacets[iFacet].facets.length%2 == 1 && this.gripSymmetryOrders[iGrip-1] == 2)
                                    {
                                        // It's an edge of an odd polygon facet in 3d...
                                        // need the opposite edge too, for adjacent tiles facing it the opposite way.
                                        // (This isn't needed for even-number-of-sided polygons,
                                        // since those grips will be generated by the opposite edge of the polygon)
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
                                        this.gripTwistRotationFixedPoints[iGrip] = this.gripTwistRotationFixedPoints[iGrip-1];
                                        iGrip++;
                                    }
                                }
                            }
                        }
                    }
                    //System.out.println("nGrips = "+nGrips);
                    //System.out.println("iGrip = "+iGrip);

                    // If the "&& this.gripSymmetryOrders[iGrip-1] == 2" clause ever happened, then iGrip will now be less than nGrips.
                    // Correct it.
                    if (iGrip < nGrips) {
                      //System.out.println("CORRECTING nGrips from "+nGrips+" to "+iGrip+"");
                      nGrips = iGrip;
                      this.gripDirsF = (float[][])com.donhatchsw.util.Arrays.subarray(this.gripDirsF, 0, nGrips);
                      this.gripOffsF = (float[][])com.donhatchsw.util.Arrays.subarray(this.gripOffsF, 0, nGrips);
                      this.gripSymmetryOrders = (int[])com.donhatchsw.util.Arrays.subarray(this.gripSymmetryOrders, 0, nGrips);
                      this.gripUsefulMats = (double[][][])com.donhatchsw.util.Arrays.subarray(this.gripUsefulMats, 0, nGrips);
                      this.gripTwistRotationFixedPoints = (double[][])com.donhatchsw.util.Arrays.subarray(this.gripTwistRotationFixedPoints, 0, nGrips);
                      this.grip2face = (int[])com.donhatchsw.util.Arrays.subarray(this.grip2face, 0, nGrips);
                      if (this.grip2facetEltForFutt != null) this.grip2facetEltForFutt = (int[][])com.donhatchsw.util.Arrays.subarray(this.grip2facetEltForFutt, 0, nGrips);
                    }

                    //System.out.println("this.gripSymmetryOrders = "+com.donhatchsw.util.Arrays.toStringCompact(this.gripSymmetryOrders));
                }

                if (doFurtherCuts)
                {
                    // Precompute sticker-and-polygon-to-grip.
                    this.stickerPoly2Grip = new int[nStickers][];
                    if (false)  // XXX THIS METHOD SUCKS; KILL IT!  but keeping it for reference right now because there's something I like about it-- it highlights a bit more polys, allowing some reacharound, and looks more coherent somehow
                    {
                        for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                        {
                            //System.out.println("      iSticker = "+iSticker);
                            int nPolysThisSticker = stickerInds[iSticker].length;
                            stickerPoly2Grip[iSticker] = new int[nPolysThisSticker];
                            for (int iPolyThisSticker = 0; iPolyThisSticker < nPolysThisSticker; ++iPolyThisSticker)
                            {
                                float[] stickerCenter = VecMath.doubleToFloat(stickerCentersD[iSticker]);
                                float[] polyCenter = VecMath.doubleToFloat(VecMath.averageIndexed(stickerInds[iSticker][iPolyThisSticker], restVerts));
                                // So that it doesn't get confused and get
                                // the wrong facet, bump it a little bit towards sticker center
                                VecMath.lerp(polyCenter, polyCenter, stickerCenter, .01f);


                                float[] facetCenterF = facetCentersF[sticker2face[iSticker]];
                                int iGrip = getClosestGrip(facetCenterF,
                                                           VecMath.vmv(polyCenter, facetCenterF));

                                // Don't highlight the one that's going to say "Can't twist that"...
                                // XXX actually we should, if rotate-arbitrary-elements-to-center is on... maybe
                                if (iGrip != -1 && gripSymmetryOrders[iGrip] == 0)
                                    iGrip = -1;

                                stickerPoly2Grip[iSticker][iPolyThisSticker] = iGrip;

                                //System.out.println("stickerPoly2Grip["+iSticker+"]["+iPolyThisSticker+"] = "+stickerPoly2Grip[iSticker][iPolyThisSticker]);
                            }
                        }
                    }
                    else // NEW WAY
                    {
                        // We recorded which original element of the whole polytope
                        // each element of each poly is from.
                        // Within this facet, those original elements will be in 1-1 correspondence with the grips
                        // (in 4d, anyway... which is the only case in which we're actually doing further cuts with grips, I think? not sure).
                        // How do we find the grip that a given poly should be associated with?
                        // 1. If the poly has a vertex that came from a vert of the original polytope,
                        //    then associate it with the grip corresponding to this facets's vertex that
                        //    also came from that original vertex.
                        // 2. Otherwise, if the poly has an edge that came from an edge of the original polytope,
                        //    then associate it with the grip corresponding to this facet's edge
                        //    that also came from that original edge.
                        // 3. Otherwise, if the poly itself came from a poly of the original polytope,
                        //    then associate it with the grip correspoding to this facet's poly
                        //    that also came from that original poly.
                        // We can iterate over the slicedPolytope.p.getAllElements() arrays again, since
                        // they are in the same order as our lists of stickers and polys-per-sticker.

                        // First make it so that we can easily lookup from global element index to element index on a given facet...
                        int maxRelevantDim = 2;  // we're looking at elements of polygons
                        java.util.Hashtable[/*nFacets*/][/*nRelevantDims*/] indexOfOriginalEltOnFacet = new java.util.Hashtable[nFacets][maxRelevantDim+1];
                        for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                        {
                            CSG.Polytope[][] allElementsOfFacet = originalFacets[iFacet].getAllElements();
                            for (int iDim = 0; iDim <= maxRelevantDim; ++iDim) {
                                indexOfOriginalEltOnFacet[iFacet][iDim] = new java.util.Hashtable();
                                int nFacetEltsOfDim = allElementsOfFacet[iDim].length;
                                for (int iFacetEltOfDim = 0; iFacetEltOfDim < nFacetEltsOfDim; ++iFacetEltOfDim) {
                                    CSG.Polytope elt = allElementsOfFacet[iDim][iFacetEltOfDim];
                                    int iElt = (Integer)elt.getAux();
                                    indexOfOriginalEltOnFacet[iFacet][iDim].put(iElt, iFacetEltOfDim);
                                }
                            }
                        }

                        CSG.Polytope[][] allSlicedElements = slicedPolytope.p.getAllElements();
                        int[][][][] allSlicedIncidences = slicedPolytope.p.getAllIncidences();
                        for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                        {
                            int iFacet = sticker2face[iSticker];
                            int nPolysThisSticker = stickerInds[iSticker].length;
                            CHECK(nPolysThisSticker == allSlicedIncidences[nDims-1][iSticker][2].length);
                            stickerPoly2Grip[iSticker] = VecMath.fillvec(nPolysThisSticker, -1);
                            for (int iPolyThisSticker = 0; iPolyThisSticker < nPolysThisSticker; ++iPolyThisSticker)
                            {
                                int iPolyThisStickerOriginal = stickerPolyToOriginalStickerPoly[iSticker][iPolyThisSticker];  // prior to any permuting we did
                                int iPoly = allSlicedIncidences[nDims-1][iSticker][2][iPolyThisStickerOriginal];
                                CSG.Polytope poly = allSlicedElements[2][iPoly];
                                boolean foundGrip = false;
                                for (int iOriginalEltDim = 0; !foundGrip && iOriginalEltDim <= 2; ++iOriginalEltDim)
                                {
                                    int nPolyEltsThisDim = allSlicedIncidences[2][iPoly][iOriginalEltDim].length;

                                    for (int iPolyEltThisDim = 0; !foundGrip && iPolyEltThisDim < nPolyEltsThisDim; ++iPolyEltThisDim)
                                    {
                                        CSG.Polytope polyEltThisDim = allSlicedElements[iOriginalEltDim][
                                            allSlicedIncidences[2][iPoly][iOriginalEltDim][iPolyEltThisDim]];
                                        Object aux = polyEltThisDim.getAux();
                                        // Aux is one of:
                                        // - an Integer (if polyEltThisDim came from an original element)
                                        // - a CutInfo (if it came from a primary cut)
                                        // - null (if it came from a "further" cut) (furthermore, this can't happen if iOriginalEltDim is nDims-2)
                                        boolean eltIsFromOriginal = (aux instanceof Integer);
                                        if (eltIsFromOriginal)
                                        {
                                            int iOriginalElt = (Integer)aux;
                                            CHECK(iOriginalElt != -1);
                                            Integer iOriginalEltOnThisFacet = (Integer)indexOfOriginalEltOnFacet[iFacet][iOriginalEltDim].get(iOriginalElt);
                                            CHECK(iOriginalEltOnThisFacet != null);
                                            int iGrip = originalFacetElt2grip[iFacet][iOriginalEltDim][iOriginalEltOnThisFacet];
                                            CHECK(this.stickerPoly2Grip[iSticker][iPolyThisSticker] == -1);
                                            foundGrip = true;
                                            if (gripSymmetryOrders[iGrip] != 0)
                                            {
                                                this.stickerPoly2Grip[iSticker][iPolyThisSticker] = iGrip;
                                            }
                                            break;
                                        }
                                    }
                                }
                                CHECK(foundGrip == (this.stickerPoly2Grip[iSticker][iPolyThisSticker] != -1));
                            }  // for iPolyThisSticker
                        } // for iSticker
                    }
                }
            }
            else // nDims > 4
            {
                // not thinking very hard
                this.grip2face = new int[0];
            }
            long endTimeMillis = System.currentTimeMillis();
            if (progressWriter != null)
            {
                progressWriter.print(" ("+this.grip2face.length+" grips)");
                progressWriter.println(" done ("+millisToSecsString(endTimeMillis-startTimeMillis)+" seconds)");
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
            double[] eltCenter = new double[nDims]; // in original dimension
            int iNicePoint = 0;
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
            for (int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
            {
                CSG.cgOfVerts(eltCenter, originalElements[iDim][iElt]);
                // all nice points will be on unit sphere...
                VecMath.normalize(eltCenter, eltCenter);
                VecMath.doubleToFloat(nicePointsToRotateToCenter[iNicePoint++],
                                      eltCenter);
            }
            CHECK(iNicePoint == nNicePoints);
        }


        if (progressWriter != null)
        {
            progressWriter.println("Done.");
            progressWriter.flush();
        }
    } // init from schlafli and length

    // XXX figure out a good public interface for something that shows stats
    public String toString(boolean verbose)
    {
        String nl = System.getProperty("line.separator");
        CSG.Polytope[][] allElements = slicedPolytope.p.getAllElements();
        String answer = "{sliced polytope counts per dim = "
                      +com.donhatchsw.util.Arrays.toStringCompact(
                       CSG.counts(slicedPolytope.p))
                      +", "+nl+"  nDims = "+nDims()
                      +", "+nl+"  nFacets = "+nFaces()
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
            public int iFacet;
            public int iCutThisFacet;
            public CutInfo(int iFacet, int iCutThisFacet)
            {
                this.iFacet = iFacet;
                this.iCutThisFacet = iCutThisFacet;
            }
            public String toString()
            {
                return "("+iFacet+":"+iCutThisFacet+")";
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
        // get weight 1, parallel pairs get weights summing to 1
        // according to the relative depths of the cut, so that
        // the closer the cut is to the surface, the higher the weight:
        // weight 1 if the cut is at the surface.
        //
        // CBB: This isn't the greatest for stickers that are
        // triangles or simplices, other than face center stickers:
        // notice that stickers that are simplices don't even move
        // when the "Stickers shrink to face boundaries" slider is moved,
        // even ones that are in fact on a face boundary, and those should
        // certainly move to the face boundary.
        // Should try to come up with an even better scheme.
        //
        private static float[][] computeStickerAltCentersF(
                CSG.SPolytope slicedPolytope,
                int facet2OppositeFacet[],
                int intLengths[],
                double doubleLengths[],
                int whichLengthToUseForFacet[])
        {
            int nDims = slicedPolytope.p.dim;
            CHECK(nDims == slicedPolytope.p.fullDim);

            CSG.Polytope[][] allSlicedElements = slicedPolytope.p.getAllElements();
            int[][][][] allSlicedIncidences = slicedPolytope.p.getAllIncidences();

            CSG.Polytope[] stickers = allSlicedElements[nDims-1];
            CSG.Polytope[] ridges = allSlicedElements[nDims-2];
            CSG.Polytope[] verts = allSlicedElements[0];

            int nFacets = facet2OppositeFacet.length;
            int nStickers = stickers.length;
            int nVerts = verts.length;

            float[][] stickerAltCentersF = new float[nStickers][nDims];


            // Scratch arrays... the size of the whole thing,
            // but we use only the parts that are incident on a particular sticker
            // at a time, and we clear those parts
            // when we are through with that sticker.
            double[] avgDepthOfThisStickerBelowFacet = VecMath.fillvec(nFacets, -1.);
            int[] nCutsParallelToThisFacet = VecMath.fillvec(nFacets, 0);
            double[] vertexWeights = VecMath.fillvec(nVerts, 1.);

            double[] stickerAltCenterD = new double[nDims];

            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                // Figure out avg cut depth of this sticker
                // with respect to each of the facets whose planes
                // contribute to it
                int[] ridgesThisSticker = allSlicedIncidences[nDims-1][iSticker][nDims-2];
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFacet, iCutThisFacet;
                    if (ridge.getAux() instanceof CutInfo)
                    {
                        CutInfo cutInfo = (CutInfo)ridge.getAux();
                        iFacet = cutInfo.iFacet;
                        iCutThisFacet = cutInfo.iCutThisFacet+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        // DUP CODE ALERT (lots of times in this file)
                        // Which original facet?
                        // well, this ridge is on two stickers: iSticker
                        // and some other.  Find that other sticker,
                        // and find which facet that other sticker
                        // was originally from.
                        int[] theTwoStickersSharingThisRidge = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                        CHECK(theTwoStickersSharingThisRidge.length == 2);
                        CHECK(theTwoStickersSharingThisRidge[0] == iSticker
                            || theTwoStickersSharingThisRidge[1] == iSticker);
                        int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                        CSG.Polytope otherSticker = stickers[iOtherSticker];
                        iFacet = (Integer)otherSticker.getAux();
                        iCutThisFacet = 0;
                    }
                    double cutDepth = iCutThisFacet / doubleLengths[whichLengthToUseForFacet[iFacet]];
                    double depth = cutDepth;
                    if (avgDepthOfThisStickerBelowFacet[iFacet] != -1.)
                    {
                        // There are at most two cuts per face
                        // contributing to this sticker, and we already saw
                        // one.  Take the average of that one and this one.
                        depth = .5*(depth + avgDepthOfThisStickerBelowFacet[iFacet]);
                    }
                    avgDepthOfThisStickerBelowFacet[iFacet] = depth;
                    nCutsParallelToThisFacet[iFacet]++;
                    CHECK(nCutsParallelToThisFacet[iFacet] <= 2);
                    int oppFacet = facet2OppositeFacet[iFacet];
                    if (oppFacet != -1)
                    {
                        avgDepthOfThisStickerBelowFacet[oppFacet] = 1.-depth;
                        nCutsParallelToThisFacet[oppFacet]++;
                        CHECK(nCutsParallelToThisFacet[oppFacet] <= 2);
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
                    int iFacet, iCutThisFacet;
                    if (ridge.getAux() instanceof CutInfo)
                    {
                        CutInfo cutInfo = (CutInfo)ridge.getAux();
                        iFacet = cutInfo.iFacet;
                        iCutThisFacet = cutInfo.iCutThisFacet+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        // DUP CODE ALERT (lots of times in this file)
                        // Which original facet?
                        // well, this ridge is on two stickers: iSticker
                        // and some other.  Find that other sticker,
                        // and find which facet that other sticker
                        // was originally from.
                        int[] theTwoStickersSharingThisRidge = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                        CHECK(theTwoStickersSharingThisRidge.length == 2);
                        CHECK(theTwoStickersSharingThisRidge[0] == iSticker
                            || theTwoStickersSharingThisRidge[1] == iSticker);
                        int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                        CSG.Polytope otherSticker = stickers[iOtherSticker];
                        iFacet = (Integer)otherSticker.getAux();
                        iCutThisFacet = 0;
                    }
                    double cutDepth = iCutThisFacet / doubleLengths[whichLengthToUseForFacet[iFacet]];
                    double cutWeight = 1.;
                    if (nCutsParallelToThisFacet[iFacet] == 2)
                    {
                        double avgStickerDepth = avgDepthOfThisStickerBelowFacet[iFacet];

                        double stickerSize = 2*Math.abs(avgStickerDepth-cutDepth);
                        cutWeight =
                            stickerSize==1. ? .5 :
                            cutDepth < avgStickerDepth ? 1. - cutDepth / (1. - stickerSize)
                                                       : (cutDepth-stickerSize) / (1. - stickerSize);
                    }
                    if (!(cutWeight >= -1e-9 && cutWeight <= 1.))
                        System.out.println("uh oh, cutWeight = "+cutWeight);  // fails on "(.25)4(2)3 3(1.4)" : cutWeight is -.75  . note that it's in 3d, and
                    CHECK(cutWeight >= -1e-9 && cutWeight <= 1.); // I've seen 1e-16 on "{4,3} 7" due to floating point roundoff error
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
                    CHECK(vertexWeight >= 0. && vertexWeight <= 1.);
                    totalWeight += vertexWeight;
                    // stickerAltCenterD += vertexWeight * vertexPosition
                    VecMath.vpsxv(stickerAltCenterD,
                                  stickerAltCenterD,
                                  vertexWeight, vert.getCoords());
                    vertexWeights[iVert] = 1.; // clear for next time
                }
                VecMath.vxs(stickerAltCenterD, stickerAltCenterD, 1./totalWeight);
                stickerAltCentersF[iSticker] = VecMath.doubleToFloat(stickerAltCenterD);

                // Clear the parts of avgDepthOfThisStickerBelowFacet
                // that we touched.
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFacet, iCutThisFacet;
                    CHECK(ridge.getAux() != null);
                    if (ridge.getAux() instanceof CutInfo)
                    {
                        CutInfo cutInfo = (CutInfo)ridge.getAux();
                        iFacet = cutInfo.iFacet;
                        iCutThisFacet = cutInfo.iCutThisFacet+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        // DUP CODE ALERT (lots of times in this file)
                        // Which original facet?
                        // well, this ridge is on two stickers: iSticker
                        // and some other.  Find that other sticker,
                        // and find which facet that other sticker
                        // was originally from.
                        int[] theTwoStickersSharingThisRidge = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                        CHECK(theTwoStickersSharingThisRidge.length == 2);
                        CHECK(theTwoStickersSharingThisRidge[0] == iSticker
                            || theTwoStickersSharingThisRidge[1] == iSticker);
                        int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                        CSG.Polytope otherSticker = stickers[iOtherSticker];
                        iFacet = (Integer)otherSticker.getAux();
                        iCutThisFacet = 0;
                    }

                    avgDepthOfThisStickerBelowFacet[iFacet] = -1.;
                    nCutsParallelToThisFacet[iFacet] = 0;
                    int oppFacet = facet2OppositeFacet[iFacet];
                    if (oppFacet != -1)
                    {
                        avgDepthOfThisStickerBelowFacet[oppFacet] = -1.;
                        nCutsParallelToThisFacet[oppFacet] = 0;
                    }
                }
            } // for each sticker

            // Make sure we've been properly cleaning up both
            // avgDepthOfThisStickerBelowFacet and vertexWeights.
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                CHECK(avgDepthOfThisStickerBelowFacet[iFacet] == -1.);
                CHECK(nCutsParallelToThisFacet[iFacet] == 0);
            }
            for (int iVert = 0; iVert < nVerts; ++iVert)
                CHECK(vertexWeights[iVert] == 1.);

            return stickerAltCentersF;
        } // computeStickerAltCentersF

        // magic crap used in a couple of methods below
        private double[][] getTwistMat(int gripIndex, int dir, boolean weWillFutt, double frac)
        {
            int order = (weWillFutt ? this.gripSymmetryOrdersFutted : this.gripSymmetryOrders)[gripIndex];
            double angle = dir * (2*Math.PI/order) * frac;
            double[][] gripUsefulMat = this.gripUsefulMats[gripIndex];
            CHECK(gripUsefulMat.length == _nDisplayDims);
            double[][] rotMat = VecMath.mxmxm(VecMath.transpose(gripUsefulMat),
                                    VecMath.makeRowRotMat(_nDisplayDims,
                                                          _nDisplayDims-2,_nDisplayDims-1,
                                                          angle),
                                    gripUsefulMat);
            double[] gripTwistRotationFixedPoint = gripTwistRotationFixedPoints[gripIndex];
            double[][] translateOriginToFixedPoint = VecMath.makeRowTransMat(gripTwistRotationFixedPoint);
            double[][] translateFixedPointToOrigin = VecMath.makeRowTransMatInv(gripTwistRotationFixedPoint);
            double[][] answer = VecMath.mxmxm(translateFixedPointToOrigin, rotMat, translateOriginToFixedPoint);
            return answer;
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
        public int nFaces()  // GenericPuzzleDescription interface calls it faces, but it's really facets
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
                                           float facetShrink,
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
            getGripSymmetryOrders(boolean futtIfPossible)
        {
            return futtIfPossible && this.futtable ? gripSymmetryOrdersFutted : gripSymmetryOrders;
        }

        public double[][] getFaceInwardNormals()
        {
            return facetInwardNormals;
        }
        public double[][] getFaceCutOffsets()
        {
            return facetCutOffsets;
        }

        // XXX lame, this should be precomputed and looked up by
        // XXX poly and sticker index.
        // XXX (wait, isn't it, now?  stickerPoly2grip())
        // This is called using
        // facetCenter, polyCenter-stickerCenter.
        // XXX NEED TO TOTALLY REPLACE THE MATH TOO, WITH SOMETHING PRINCIPLED
        public int getClosestGrip(float unNormalizedDir[/*4*/],
                                  float unNormalizedOff[/*4*/])
        {
            // should already be orthogonal
            float[] dir = VecMath.normalizeOrZero(unNormalizedDir, 1e-6f);
            float[] off = VecMath.normalizeOrZero(unNormalizedOff, 1e-6f);



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
            // Secondary criterion is closeness to grip off dir (in case of a tie
            // in closeness to grip dir).
            for (int iGrip = 0; iGrip < gripDirsF.length; ++iGrip)
            {
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

        // no need to normalize pickCoords, since all the nice points are normalized-- closest will be same in either case
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
                CHECK(outVerts.length == vertsF.length);
                for (int iVert = 0; iVert < vertsF.length; ++iVert)
                    VecMath.copyvec(outVerts[iVert], vertsF[iVert]);
            }
            if (outStickerCenters != null)
            {
                CHECK(outStickerCenters.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outStickerCenters[iSticker],
                                    stickerCentersF[iSticker]);
            }
            if (outStickerShrinkToPointsOnFaceBoundaries != null)
            {
                CHECK(outStickerShrinkToPointsOnFaceBoundaries.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outStickerShrinkToPointsOnFaceBoundaries[iSticker],
                                    stickerAltCentersF[iSticker]);
            }
            if (outPerStickerFaceCenters != null)
            {
                CHECK(outPerStickerFaceCenters.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outPerStickerFaceCenters[iSticker],
                                    facetCentersF[sticker2face[iSticker]]);
            }
        } // computeVertsAndShrinkToPointsAtRest

        // sort an even-length array so that pairs[0]<=pairs[2]<=...pairs[n-2].
        private void quickDirtySortInPairs(int[] pairs) {
            //System.out.println("  old: pairs="+VecMath.toString(pairs));
            for (int i = 0; i <= pairs.length-4; i += 2) {
                // indices <i are now correct.
                // move the smallest remaining pair into position i.
                for (int j = pairs.length-4; j >= i; j -= 2) {
                    if (pairs[j] > pairs[j+2]) {
                        com.donhatchsw.util.Arrays.swap(pairs, j, pairs, j+2);
                        com.donhatchsw.util.Arrays.swap(pairs, j+1, pairs, j+3);
                    }
                }
                // indices <=i are now correct
            }
            //System.out.println("  new: pairs="+VecMath.toString(pairs));
            for (int i = 0; i+2 < pairs.length; i += 2) {
                //System.out.println("    i="+i+": comparing "+pairs[i]+" with "+pairs[i+2]);
                CHECK(pairs[i] <= pairs[i+2]);
            }
        }

        // Returns pair [edgesThisFaceInOrder, neighborsThisFaceInOrder]
        private int[][] getFaceNeighborsInOrderForFutt(int iFacet)
        {
            int verboseLevel = 0;  // set to something higher than 0 to debug futt stuff
            CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
            int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();
            int gonality = originalIncidences[2][iFacet][1].length;

            boolean[] edgeIsIncidentOnThisFace = new boolean[originalElements[1].length];  // false initially
            for (int iEdgeThisFace = 0; iEdgeThisFace < originalIncidences[2][iFacet][1].length; ++iEdgeThisFace)
            {
                int iEdge = originalIncidences[2][iFacet][1][iEdgeThisFace];
                edgeIsIncidentOnThisFace[iEdge] = true;
            }

            int[] vertsThisFaceInOrder = new int[gonality];
            int[] edgesThisFaceInOrder = new int[gonality];
            int iFirstEdge = originalIncidences[2][iFacet][1][0];
            int iFirstVert = originalIncidences[1][iFirstEdge][0][0];
            // We want to go ccw around the face,
            // so first vertex should be before first edge
            // in ccw order.
            // That is, det(facetcenter, edgecenter, vertcenter) should be <0.
            double det = VecMath.det(new double[/*3*/][/*3*/] {
              CSG.cgOfVerts(originalElements[2][iFacet]),
              CSG.cgOfVerts(originalElements[1][iFirstEdge]),
              CSG.cgOfVerts(originalElements[0][iFirstVert]),
            });
            if (verboseLevel >= 1) System.out.println("      det = "+det);
            if (det > 0)
            {
                iFirstVert = originalIncidences[1][iFirstEdge][0][1];  // the other one
            }
            vertsThisFaceInOrder[0] = iFirstVert;
            edgesThisFaceInOrder[0] = iFirstEdge;
            for (int i = 1; i < gonality; ++i) {  // skipping 0
                // this vert is other vert on prev edge
                int iPrevVert = vertsThisFaceInOrder[i-1];
                int iPrevEdge = edgesThisFaceInOrder[i-1];
                int iThisVert = originalIncidences[1][iPrevEdge][0][0];  // or the other one
                if (iThisVert == iPrevVert) iThisVert = originalIncidences[1][iPrevEdge][0][1];
                vertsThisFaceInOrder[i] = iThisVert;

                // this edge is the other edge incident on this vert
                // that's incident on this face.
                int iThisEdge = -1;
                for (int iEdgeThisVert = 0; iEdgeThisVert < originalIncidences[0][iThisVert][1].length; ++iEdgeThisVert)
                {
                    int iEdge = originalIncidences[0][iThisVert][1][iEdgeThisVert];
                    if (iEdge != iPrevEdge && edgeIsIncidentOnThisFace[iEdge])
                    {
                        // found it!
                        CHECK(iThisEdge == -1);
                        iThisEdge = iEdge;
                        break;
                    }
                }
                CHECK(iThisEdge != -1);
                edgesThisFaceInOrder[i] = iThisEdge;
            }
            int[] neighborsThisFaceInOrder = new int[gonality];
            for (int i = 0; i < gonality; ++i) {
                // this neighbor face is the other face
                // incident on this edge.
                int iThisNeighborFace = originalIncidences[1][edgesThisFaceInOrder[i]][2][0];  // or the other one
                if (iThisNeighborFace == iFacet) iThisNeighborFace = originalIncidences[1][edgesThisFaceInOrder[i]][2][1];
                neighborsThisFaceInOrder[i] = iThisNeighborFace;
            }
            if (verboseLevel >= 1) System.out.println("          vertsThisFaceInOrder = "+VecMath.toString(vertsThisFaceInOrder));
            if (verboseLevel >= 1) System.out.println("          edgesThisFaceInOrder = "+VecMath.toString(edgesThisFaceInOrder));
            if (verboseLevel >= 1) System.out.println("          neighborsThisFaceInOrder = "+VecMath.toString(neighborsThisFaceInOrder));
            return new int[][] {edgesThisFaceInOrder, neighborsThisFaceInOrder};
        }  // getFaceNeighborsInOrderForFutt

        private int[] getFrom2toFacetsForFutt(int gripIndex,
                                              int dir,
                                              int[] edgesThisFaceInOrder,  // used only in 3d. will eventually be removed
                                              int[] neighborsThisFaceInOrder)  // used only in 3d.  will eventually be removed
        {
            int futtVerboseLevel = 1;
            if (futtVerboseLevel >= 1) System.out.println("            in getFrom2toFacetsForFutt");
            int nDims = this.nDims();
            int iFacet = grip2face[gripIndex];
            CSG.Polytope[][] originalElements = this.originalPolytope.p.getAllElements();
            int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();
            int nFacets = originalElements[nDims-1].length;
            int gonality = originalIncidences[2][iFacet][1].length;
            int[] from2toFacet = VecMath.identityperm(nFacets);  // except for...
            if (nDims == 4)
            {
                int eltDim = this.grip2facetEltForFutt[gripIndex][0];
                int iFacetElt = this.grip2facetEltForFutt[gripIndex][1];
                if (futtVerboseLevel >= 1) System.out.println("              iFacet = "+iFacet);
                if (futtVerboseLevel >= 1) System.out.println("              eltDim = "+eltDim);
                if (futtVerboseLevel >= 1) System.out.println("              iFacetElt = "+iFacetElt);
                // TODO: can't just use element size, need to use chosen symmetry order depending on what succeeds
                int iEltGlobal = originalIncidences[nDims-1][iFacet][eltDim][iFacetElt];
                int symmetryOrder = eltDim==2 ? originalIncidences[eltDim][iEltGlobal][1].length :
                                    eltDim==1 ? 2 :
                                    eltDim==0 ? -1 : // we'll correct this below
                                    -1;
                if (eltDim == 0)
                {
                    // What's the valence of the vertex *in the facet* (rather than
                    // in the whole polytope).
                    // We could get this easily by querying facet.getAllIncidences(),
                    // but we don't want to go accumulating those for every facet,
                    // so do a more lightweight search.
                    // Any of the following would work:
                    // - [edge for edge incident on vertex if edge incident on facet]
                    // - [edge for edge incident on facet if edge incident on vertex]
                    // - [edge for edge incident on vertex if facet incident on edge]
                    // - [edge for edge incident on facet if vertex incident on edge]
                    // Ah, the latter is the least amount of searching.
                    int vertexValenceThisFacet = 0;
                    for (int iEdgeThisFacet = 0; iEdgeThisFacet < originalIncidences[3][iFacet][1].length; ++iEdgeThisFacet)
                    {
                        int iEdge = originalIncidences[3][iFacet][1][iEdgeThisFacet];
                        CHECK(originalIncidences[1][iEdge][0].length == 2);
                        if (originalIncidences[1][iEdge][0][0] == iEltGlobal
                         || originalIncidences[1][iEdge][0][1] == iEltGlobal)
                        {
                            vertexValenceThisFacet++;
                        }
                    }
                    symmetryOrder = vertexValenceThisFacet;
                }
                if (futtVerboseLevel >= 1) System.out.println("              symmetryOrder = "+symmetryOrder);
                // To get started, figure out three (or four, if we want to nail down sign initially) fromFacet,toFacet pairs.
                // The first will always be iFacet,iFacet, since that's fixed.
                // Case vertex:
                //   find three neighbor facets, in ccw order around the vertex on the facet;
                //   map the first to the second, and the second to the third.
                // Case edge:
                //   find the two neighbor facets at this edge.
                //   map them to each other.  (this is only three, but in this case either choice of sign gives the same answer)
                // Case 2face:
                //   either:
                //     - map the corresponding neighbor facet to itself,
                //       and find two neighbor facets in ccw order around the 2face, map first to second
                //   or:
                //     - find three neighbor facets, in ccw order around the 2face on the facet;
                //       map the first to the second, and the second to the third.
                // Idea: would it be simpler to mix dims?  That is, specify that the facet and elt
                // stay fixed, then need only one other corrrespondence?  (or two, if we don't want to have to think about the sign afterwards)
            }
            else if (nDims == 3)
            {
                boolean isEdgeFlipIn3d = Math.abs(this.gripUsefulMats[gripIndex][1][3]) < 1e-6;
                if (isEdgeFlipIn3d)
                {
                    // CBB: DUP CODE
                    // Figure out which edge of the facet we're going to flip.
                    // It's the one whose facet-to-the-edge is closest to gripUsefulMats[gripIndex][1].
                    // CBB: this isn't reliable, and may change when I make the frucht useful mat more properly orthogonalized.
                    // CBB: can't we use originalFacetElt2grip?
                    int bestIndex = -1;
                    double bestDot = -1.;
                    for (int i = 0; i < gonality; ++i)
                    {
                        CSG.Polytope ridge = originalElements[nDims-2][edgesThisFaceInOrder[i]];
                        double[] facetToRidge = VecMath.vmv(3, CSG.cgOfVerts(ridge), VecMath.floatToDouble(facetCentersF[iFacet]));  // CBB: floatToDouble is not great.  but this is temporary code anyway, I think
                        double thisDot = VecMath.dot(3, gripUsefulMats[gripIndex][1], facetToRidge);
                        if (futtVerboseLevel >= 1) System.out.println("          looking at i="+i+" -> ridge="+edgesThisFaceInOrder[i]+", neighbor="+neighborsThisFaceInOrder[i]+": thisDot="+thisDot);
                        if (futtVerboseLevel >= 1) System.out.println("              facetToRidge = "+VecMath.toString(facetToRidge));
                        if (futtVerboseLevel >= 1) System.out.println("              gripUsefulMats[gripIndex][1] = "+VecMath.toString(gripUsefulMats[gripIndex][1]));
                        if (thisDot > bestDot) {
                            bestIndex = i;
                            bestDot = thisDot;
                        }
                    }
                    int neighborIndexToFlip = bestIndex;
                    if (futtVerboseLevel >= 1) System.out.println("      neighborIndexToFlip = "+neighborIndexToFlip+" (face "+neighborsThisFaceInOrder[neighborIndexToFlip]+")");
                    for (int i = 0; i < gonality; ++i)
                    {
                        int j = (2*neighborIndexToFlip-i + gonality) % gonality; // so neighborIndex stays fixed, and direction is reversed
                        from2toFacet[neighborsThisFaceInOrder[i]] = neighborsThisFaceInOrder[j];
                    }
                }
                else
                {
                    for (int i = 0; i < gonality; ++i) {
                        // This calculation would make no sense for edge flips, since the vector in question would be zero in w direction
                        int j = (i + (this.gripUsefulMats[gripIndex][1][3] < 0 ? -1 : 1)*dir + gonality) % gonality;
                        from2toFacet[neighborsThisFaceInOrder[i]] = neighborsThisFaceInOrder[j];
                    }
                }
            }
            if (futtVerboseLevel >= 1) System.out.println("            out getFrom2toFacetsForFutt, returning "+VecMath.toString(from2toFacet));
            return from2toFacet;
        }  // getFrom2toFacetsForFutt

        private int[] getFrom2toStickersForFutt(int gripIndex,
                                                int dir,
                                                int slicemask,
                                                int[] from2toFacet)
        {
            int verboseLevel = 0;  // set to something higher than 0 to debug futt stuff
            if (verboseLevel >= 1) System.out.println("        in getFrom2toStickersForFutt");
            int nDims = nDims();
            int iFacet = grip2face[gripIndex];

            CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
            int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();
            CSG.Polytope[][] allSlicedElements = slicedPolytope.p.getAllElements();
            int[][][][] allSlicedIncidences = slicedPolytope.p.getAllIncidences();

            int gonality = originalIncidences[2][iFacet][1].length;
            double[] thisFaceInwardNormal = facetInwardNormals[iFacet];
            double[] thisFaceCutOffsets = facetCutOffsets[iFacet];
            int nStickers = stickerCentersD.length;
            CSG.Polytope[] stickers = allSlicedElements[nDims-1];
            CHECK(stickers.length == nStickers);
            CSG.Polytope[] ridges = allSlicedElements[nDims-2];

            CutInfo[][] sticker2cutInfos = new CutInfo[nStickers][];
            java.util.HashMap cutInfos2sticker = new java.util.HashMap();  // CBB: initial capacity should be number of stickers in slicemask
            SortStuff.Comparator cutInfoCompare = new SortStuff.Comparator() {
                public int compare(Object aObject, Object bObject)
                {
                    CutInfo a = (CutInfo)aObject;
                    CutInfo b = (CutInfo)bObject;
                    if (a.iFacet < b.iFacet) return -1;
                    if (a.iFacet > b.iFacet) return 1;
                    if (a.iCutThisFacet < b.iCutThisFacet) return -1;
                    if (a.iCutThisFacet > b.iCutThisFacet) return 1;
                    return 0;
                }
            };
            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                // CBB: we are testing these too many times!
                if (pointIsInSliceMask(stickerCentersD[iSticker],
                                       slicemask,
                                       thisFaceInwardNormal,
                                       thisFaceCutOffsets))
                {
                    if (verboseLevel >= 2) System.out.println("          looking at sticker "+iSticker);
                    CSG.Polytope sticker = stickers[iSticker];
                    int iFacetThatStickerIsPartOf = (Integer)sticker.getAux();
                    if (verboseLevel >= 2) System.out.println("                  facet that sticker is part of = "+iFacetThatStickerIsPartOf);

                    CutInfo[] stickerCutInfos = new CutInfo[sticker.facets.length + 1];
                    int[] ridgesThisSticker = allSlicedIncidences[nDims-1][iSticker][nDims-2];
                    for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)  // iterating over nDims-2 dimensional elements here
                    {
                        int iRidge = ridgesThisSticker[iRidgeThisSticker];
                        CSG.Polytope ridge = ridges[iRidge];
                        Object aux = ridge.getAux();
                        if (aux instanceof CutInfo)
                        {
                            stickerCutInfos[iRidgeThisSticker] = (CutInfo)aux;
                        }
                        else // it's not from a cut, it's from an original face
                        {
                            // DUP CODE ALERT (lots of times in this file)
                            // Which original facet?
                            // well, this ridge is on two stickers: iSticker
                            // and some other.  Find that other sticker,
                            // and find which facet that other sticker
                            // was originally from.
                            int[] theTwoStickersSharingThisRidge = allSlicedIncidences[nDims-2][iRidge][nDims-1];
                            CHECK(theTwoStickersSharingThisRidge.length == 2);
                            CHECK(theTwoStickersSharingThisRidge[0] == iSticker
                                || theTwoStickersSharingThisRidge[1] == iSticker);
                            int iOtherSticker = theTwoStickersSharingThisRidge[theTwoStickersSharingThisRidge[0]==iSticker ? 1 : 0];
                            CSG.Polytope otherSticker = stickers[iOtherSticker];
                            stickerCutInfos[iRidgeThisSticker] = new CutInfo((Integer)otherSticker.getAux(), -1);
                        }
                        if (verboseLevel >= 3) System.out.println("                      one of this sticker's cut infos: "+stickerCutInfos[iRidgeThisSticker]);
                        CHECK(stickerCutInfos[iRidgeThisSticker].iFacet != iFacetThatStickerIsPartOf);  // XXX why is this failing??
                    }
                    // add one for the facet of which the sticker is a part; that's important too.  call it -2,
                    // to distinguish it from any of the others.
                    stickerCutInfos[sticker.facets.length] = new CutInfo(iFacetThatStickerIsPartOf, -2);

                    // Fix up: currently, when there is a pair of opposite facets,
                    // all the cuts are labeled with the lower-numbered of them,
                    // with up to twice as many cuts.
                    // That would mess us up here, so fix it:
                    // associate the cut with the face it's nearest to.
                    // (This will still assert-fail if an even number of cuts, I'm sure... but currently we mark those unfuttable anyway)
                    for (int i = 0; i < stickerCutInfos.length; ++i)
                    {
                        CutInfo stickerCutInfo = stickerCutInfos[i];
                        if (stickerCutInfo.iCutThisFacet >= 0
                         && this.facet2OppositeFacet[stickerCutInfo.iFacet] != -1
                         && (facetCutOffsets[stickerCutInfo.iFacet].length-1-stickerCutInfo.iCutThisFacet < stickerCutInfo.iCutThisFacet
                          || (facetCutOffsets[stickerCutInfo.iFacet].length-1-stickerCutInfo.iCutThisFacet == stickerCutInfo.iCutThisFacet && this.facet2OppositeFacet[stickerCutInfo.iFacet] < stickerCutInfo.iFacet)))  // this clause is relevant only if there's a cut exactly in the middle, which can happen only if even number of cuts, which we currently mark unfuttable, so it's a no-op currently
                        {
                            stickerCutInfo = new CutInfo(this.facet2OppositeFacet[stickerCutInfo.iFacet],
                                                         facetCutOffsets[stickerCutInfo.iFacet].length-1-stickerCutInfo.iCutThisFacet);
                            if (verboseLevel >= 3) System.out.println("                      CORRECTING "+stickerCutInfos[i]+" to "+stickerCutInfo+"");
                            stickerCutInfos[i] = stickerCutInfo;
                        }
                    }

                    // Sort into canonical order
                    SortStuff.sort(stickerCutInfos, 0, stickerCutInfos.length, cutInfoCompare);
                    String stickerCutInfosString = com.donhatchsw.util.Arrays.toStringCompact(stickerCutInfos);
                    if (verboseLevel >= 2) System.out.println("                  sticker cutInfosString = "+stickerCutInfosString);
                    sticker2cutInfos[iSticker] = stickerCutInfos;
                    CHECK(cutInfos2sticker.put(stickerCutInfosString, iSticker) == null);
                }
            }

            int[] from2toStickerCenters = VecMath.identityperm(nStickers);  // except for...
            for (int fromSticker = 0; fromSticker < nStickers; ++fromSticker)
            {
                CutInfo[] fromStickerCutInfos = sticker2cutInfos[fromSticker];
                if (fromStickerCutInfos != null)  // i.e. if we populated it, i.e. if sticker center is in slicemask
                {
                    if (verboseLevel >= 3) System.out.println("          looking again at sticker "+fromSticker);
                    if (verboseLevel >= 3) System.out.println("              fromStickerCutInfos = "+com.donhatchsw.util.Arrays.toStringCompact(fromStickerCutInfos));
                    CutInfo[] toStickerCutInfos = new CutInfo[fromStickerCutInfos.length];
                    for (int i = 0; i < fromStickerCutInfos.length; ++i)
                    {
                        toStickerCutInfos[i] = new CutInfo(from2toFacet[fromStickerCutInfos[i].iFacet],
                                                           fromStickerCutInfos[i].iCutThisFacet);
                    }
                    if (verboseLevel >= 3) System.out.println("              toStickerCutInfos = "+com.donhatchsw.util.Arrays.toStringCompact(toStickerCutInfos)+" (before sorting)");

                    // Sort into canonical order
                    SortStuff.sort(toStickerCutInfos, 0, toStickerCutInfos.length, cutInfoCompare);
                    if (verboseLevel >= 3) System.out.println("              toStickerCutInfos = "+com.donhatchsw.util.Arrays.toStringCompact(toStickerCutInfos)+" (after sorting)");
                    String toStickerCutInfosString = com.donhatchsw.util.Arrays.toStringCompact(toStickerCutInfos);
                    if (verboseLevel >= 3) System.out.println("              toStickerCutInfosString = "+com.donhatchsw.util.Arrays.toStringCompact(toStickerCutInfosString));
                    Object got = cutInfos2sticker.get(toStickerCutInfosString);
                    CHECK(got != null);
                    int toSticker = (Integer)got;
                    CHECK(from2toStickerCenters[fromSticker] == fromSticker);
                    from2toStickerCenters[fromSticker] = toSticker;
                }
            }
            if (verboseLevel >= 1) System.out.println("        out getFrom2toStickersForFutt");
            return from2toStickerCenters;
        }  // getFrom2toStickersForFutt

        public void
            computeVertsAndShrinkToPointsPartiallyTwisted(
                float outVerts[/*nVerts*/][/*nDisplayDims*/],
                float outStickerCenters[/*nStickers*/][/*nDisplayDims*/],
                float outStickerShrinkToPointsOnFaceBoundaries[/*nStickers*/][/*nDisplayDims*/],
                float outPerStickerFaceCenters[/*nStickers*/][/*nDisplayDims*/],
                int gripIndex,
                int dir,
                int slicemask,
                boolean futtIfPossible,
                float frac)
        {
            int verboseLevel = 0;
            if (verboseLevel >= 1) System.out.println("in computeVertsAndShrinkToPointsPartiallyTwisted(gripIndex="+gripIndex+", dir="+dir+", slicemask="+slicemask+", futtIfPossible="+futtIfPossible+", frac="+frac+")");
            // XXX dup code
            boolean weWillFutt = futtIfPossible
                              && this.futtable
                              && slicemask < (1<<((this.intLengthsForFutt[0]-1)/2))  // e.g. if intLength is 9, then nCuts is 4, so we can handle only up through 1+2+4+8
                              && this.gripSymmetryOrdersFutted[gripIndex] != 1;
            // Note, we purposely go through all the calculation
            // even if dir*frac is 0; we get more consistent timing that way.

            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("computeVertsAndShrinkToPointsPartiallyTwisted called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if ((weWillFutt ? this.gripSymmetryOrdersFutted : this.gripSymmetryOrders)[gripIndex] == 0)
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

            double[][] matD = getTwistMat(gripIndex, dir, weWillFutt, frac);
            float[][] matF = VecMath.doubleToFloat(matD);
            if (verboseLevel >= 1) System.out.println("  matD = "+VecMath.toString(matD));
            if (verboseLevel >= 1) System.out.println("  matF = "+VecMath.toString(matF));

            boolean[] whichVertsGetMoved = new boolean[vertsF.length]; // false initially
            int iFacet = grip2face[gripIndex];
            double[] thisFaceInwardNormal = facetInwardNormals[iFacet];
            double[] thisFaceCutOffsets = facetCutOffsets[iFacet];

            for (int iSticker = 0; iSticker < stickerCentersD.length; ++iSticker)
            {
                if (pointIsInSliceMask(stickerCentersD[iSticker],
                                       slicemask,
                                       thisFaceInwardNormal,
                                       thisFaceCutOffsets))
                {
                    for (int i = 0; i < stickerInds[iSticker].length; ++i)
                    for (int j = 0; j < stickerInds[iSticker][i].length; ++j) // around the polygon
                        whichVertsGetMoved[stickerInds[iSticker][i][j]] = true;

                    VecMath.vxm(outStickerCenters[iSticker], stickerCentersF[iSticker], matF);
                    VecMath.vxm(outStickerShrinkToPointsOnFaceBoundaries[iSticker], stickerAltCentersF[iSticker], matF);
                    VecMath.vxm(outPerStickerFaceCenters[iSticker], facetCentersF[sticker2face[iSticker]], matF);
                }
                else
                {
                    VecMath.copyvec(outStickerCenters[iSticker],
                                    stickerCentersF[iSticker]);
                    VecMath.copyvec(outStickerShrinkToPointsOnFaceBoundaries[iSticker],
                                    stickerAltCentersF[iSticker]);
                    VecMath.copyvec(outPerStickerFaceCenters[iSticker],
                                    facetCentersF[sticker2face[iSticker]]);
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


            if (weWillFutt)
            {
                int futtVerboseLevel = 1;  // set to something higher than 0 to debug futt stuff
                // Whole lotta fudgin goin on.
                // Each "corner region" of the puzzle
                // gets a different actual transform; the verts
                // in that corner region get transformed by that transform.
                if (futtVerboseLevel >= 1) System.out.println("  ==========================");
                if (futtVerboseLevel >= 1) System.out.println("  WHOLE LOTTA FUTTIN GOIN ON");
                if (futtVerboseLevel >= 1) System.out.println("      gripIndex = "+gripIndex);
                if (futtVerboseLevel >= 1) System.out.println("      dir = "+dir);
                if (futtVerboseLevel >= 1) System.out.println("      slicemask = "+slicemask);
                if (futtVerboseLevel >= 1) System.out.println("      frac = "+frac);
                if (futtVerboseLevel >= 1) System.out.println("      iFacet = "+iFacet);
                if (futtVerboseLevel >= 1) System.out.println("      symmetry order = "+this.gripSymmetryOrdersFutted[gripIndex]);

                int nDims = nDims();

                int nStickerLayers = this.intLengthsForFutt[0]/2;  // round down. assumes all intLengths are same.
                if (futtVerboseLevel >= 1) System.out.println("      nStickerLayers = "+nStickerLayers);

                if (nDims == 4)
                {
                    if (futtVerboseLevel >= 1) System.out.println("      (nDims==4 so not doing much yet)");
                    int[] from2toFacet = getFrom2toFacetsForFutt(gripIndex, dir, /*edgesThisFaceInOrder=*/null, /*neighborsThisFaceInOrder=*/null);
                    if (futtVerboseLevel >= 1) System.out.println("          from2toFacet = "+VecMath.toString(from2toFacet));
                }
                else if (nDims == 3)
                {
                    // Find the incident verts and edges, in cyclic order.
                    // This assumes nDims==3.
                    CHECK(nDims == 3);
                    // So, facet=face, ridge=edge, peak=vertex.

                    CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
                    int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();

                    int gonality = originalIncidences[2][iFacet][1].length;

                    int[][] edgesAndNeighborsThisFaceInOrder = getFaceNeighborsInOrderForFutt(iFacet);
                    int[] edgesThisFaceInOrder = edgesAndNeighborsThisFaceInOrder[0];
                    int[] neighborsThisFaceInOrder = edgesAndNeighborsThisFaceInOrder[1];
                    int[] from2toFacet = getFrom2toFacetsForFutt(gripIndex, dir, edgesThisFaceInOrder, neighborsThisFaceInOrder);
                    int[] from2toStickerCenters = getFrom2toStickersForFutt(gripIndex, dir, slicemask, from2toFacet);

                    double[][] fullInvMatD = getTwistMat(gripIndex, -dir, weWillFutt, 1.);  // -dir instead of dir, 1. instead of frac
                    float[][] fullInvMatF = VecMath.doubleToFloat(fullInvMatD);

                    boolean isEdgeFlipIn3d = Math.abs(this.gripUsefulMats[gripIndex][1][3]) < 1e-6;

                    // populate outVerts
                    {
                        double[][][][][] cutIntersectionCoords = new double[gonality][/*nCutsThisFace+1*/][/*nCutsPrevNeighborFace+1*/][/*nCutsNextNeighborFace+1*/][];
                        for (int iCornerRegion = 0; iCornerRegion < gonality; ++iCornerRegion)
                        {
                            if (futtVerboseLevel >= 2) System.out.println("          iCornerRegion = "+iCornerRegion+"/"+gonality);
                            int iPrevNeighborFace = neighborsThisFaceInOrder[(iCornerRegion-1 + gonality) % gonality];
                            int iNextNeighborFace = neighborsThisFaceInOrder[iCornerRegion];
                            if (futtVerboseLevel >= 2) System.out.println("              iFacet,iPrevNeighborFace,iNextNeighborFace = "+iFacet+","+iPrevNeighborFace+","+iNextNeighborFace);

                            // We are going to want to answer questions of the form:
                            // "what is the intersection of the hyperplanes
                            // at these three offsets from the three faces"?
                            // I.e. "what is the point whose dot products with the three hyperplane normals
                            // equals the three hyperplane offsets"?
                            // The face normals stay constant (throughout this corner region)
                            // but the offsets vary, so compute an inverse matrix so we can answer
                            // the questions quickly.
                            double[/*3*/][/*3*/] faceInwardNormalsMat = {
                                (double[])com.donhatchsw.util.Arrays.subarray(facetInwardNormals[iFacet], 0, 3),
                                (double[])com.donhatchsw.util.Arrays.subarray(facetInwardNormals[iPrevNeighborFace], 0, 3),
                                (double[])com.donhatchsw.util.Arrays.subarray(facetInwardNormals[iNextNeighborFace], 0, 3),
                            };
                            if (futtVerboseLevel >= 2) System.out.println("              facetInwardNormalsMat = "+VecMath.toString(faceInwardNormalsMat));
                            double[/*3*/][/*3*/] inverseOfFaceInwardNormalsMat = VecMath.invertmat(faceInwardNormalsMat);
                            if (futtVerboseLevel >= 2) System.out.println("              inverseOfFaceInwardNormalsMat = "+VecMath.toString(inverseOfFaceInwardNormalsMat));


                            cutIntersectionCoords[iCornerRegion] = new double[nStickerLayers+1][/*nCutsPrevNeighborFace+1*/][/*nCutsNextNeighborFace+1*/][];
                            for (int iCutThisFace = 0; iCutThisFace < nStickerLayers+1; ++iCutThisFace)
                            {
                                cutIntersectionCoords[iCornerRegion][iCutThisFace] = new double[nStickerLayers+1+1][/*nCutsNextNeighborFace+1*/][];  // one extra layer in this direction, but we don't populate it yet; we'll copy it from the next corner afterwards    TODO: do we even do that any more?  I don't think so; get rid of that
                                for (int iCutPrevNeighborFace = 0; iCutPrevNeighborFace < nStickerLayers+1+1; ++iCutPrevNeighborFace)
                                {
                                    // Need coords only where at least one of the three cut indices is 0,
                                    // i.e. on the surface of the polyhedron.
                                    // This is an important optimization if number of cuts is large.
                                    cutIntersectionCoords[iCornerRegion][iCutThisFace][iCutPrevNeighborFace] = new double[
                                        iCutThisFace!=0&&iCutPrevNeighborFace!=0 ? 1 : nStickerLayers+1][];
                                    if (iCutPrevNeighborFace == nStickerLayers+1) {
                                        continue;  // this is the extra layer we'll populate afterwards
                                    }
                                    for (int iCutNextNeighborFace = 0; iCutNextNeighborFace < cutIntersectionCoords[iCornerRegion][iCutThisFace][iCutPrevNeighborFace].length; ++iCutNextNeighborFace)
                                    {
                                        double[] desiredOffsets = {
                                            iCutThisFace==0 ? this.facetOffsetsForFutt[iFacet] : facetCutOffsets[iFacet][iCutThisFace-1],
                                            iCutPrevNeighborFace==0 ? this.facetOffsetsForFutt[iPrevNeighborFace] : facetCutOffsets[iPrevNeighborFace][iCutPrevNeighborFace-1],
                                            iCutNextNeighborFace==0 ? this.facetOffsetsForFutt[iNextNeighborFace] : facetCutOffsets[iNextNeighborFace][iCutNextNeighborFace-1],
                                        };
                                        double[] coords3 = VecMath.mxv(inverseOfFaceInwardNormalsMat, desiredOffsets);
                                        //System.out.println("              desiredOffsets = "+VecMath.toString(desiredOffsets)+" -> coords3 = "+VecMath.toString(coords3));
                                        cutIntersectionCoords[iCornerRegion][iCutThisFace][iCutPrevNeighborFace][iCutNextNeighborFace] = coords3;
                                    }
                                }
                            }
                        }

                        // The usual params.
                        // bucket size is chosen by listening to the implementation, which throws if it's too small.
                        FuzzyPointHashTable finalMorphDestinations = new FuzzyPointHashTable(1e-9, 1e-8, 1./128);

                        if (isEdgeFlipIn3d)
                        {
                            if (futtVerboseLevel >= 1) System.out.println("      oh no! it's an edge flip!");
                            // CBB: DUP CODE
                            // Figure out which edge of the facet we're going to flip.
                            // It's the one whose facet-to-the-edge is closest to gripUsefulMats[gripIndex][1].
                            // CBB: this isn't reliable, and may change when I make the frucht useful mat more properly orthogonalized.
                            int bestIndex = -1;
                            double bestDot = -1.;
                            for (int i = 0; i < gonality; ++i)
                            {
                                CSG.Polytope ridge = originalElements[nDims-2][edgesThisFaceInOrder[i]];
                                double[] facetToRidge = VecMath.vmv(3, CSG.cgOfVerts(ridge), VecMath.floatToDouble(facetCentersF[iFacet]));  // CBB: floatToDouble is not great.  but this is temporary code anyway, I think
                                double thisDot = VecMath.dot(3, gripUsefulMats[gripIndex][1], facetToRidge);
                                if (futtVerboseLevel >= 1) System.out.println("          looking at i="+i+" -> ridge="+edgesThisFaceInOrder[i]+", neighbor="+neighborsThisFaceInOrder[i]+": thisDot="+thisDot);
                                if (futtVerboseLevel >= 1) System.out.println("              facetToRidge = "+VecMath.toString(facetToRidge));
                                if (futtVerboseLevel >= 1) System.out.println("              gripUsefulMats[gripIndex][1] = "+VecMath.toString(gripUsefulMats[gripIndex][1]));
                                if (thisDot > bestDot) {
                                    bestIndex = i;
                                    bestDot = thisDot;
                                }
                            }
                            int neighborIndexToFlip = bestIndex;
                            if (futtVerboseLevel >= 1) System.out.println("      neighborIndexToFlip = "+neighborIndexToFlip+" (face "+neighborsThisFaceInOrder[neighborIndexToFlip]+")");

                            for (int fromCornerRegion = 0; fromCornerRegion < cutIntersectionCoords.length; ++fromCornerRegion)
                            {
                                // Given fromCornerRegion, we want toCornerRegion such that:
                                //   fromCornerRegion=neighborIndexToFlip -> toCornerRegion=neighborIndexToFlip+1
                                //   fromCornerRegion=neighborIndexToFlip+1 -> toCornerRegion=neighborIndexToFlip
                                // the formula for that is:
                                //   toCornerRegion = 2*neighborIndexToFlip - fromCornerRegion + 1  (modulo gonality).
                                int toCornerRegion = ((2*neighborIndexToFlip - fromCornerRegion + 1) + gonality) % gonality;
                                if (futtVerboseLevel >= 1) System.out.println("          fromCornerRegion="+fromCornerRegion+" -> toCornerRegion="+toCornerRegion);

                                for (int i = 0; i < cutIntersectionCoords[fromCornerRegion].length; ++i)
                                for (int j = 0; j < cutIntersectionCoords[fromCornerRegion][i].length-1; ++j)  // don't include the extra layer here; it would be redundant
                                for (int k = 0; k < cutIntersectionCoords[fromCornerRegion][i][j].length; ++k) {
                                    for (int wdir = -1; wdir <= 1; wdir += 2) {
                                        double pad = Math.abs(vertsDForFutt[0][3]); // hacky way to retrieve what was used for thickness in w direction
                                        double w = wdir * pad;
                                        double[] from = com.donhatchsw.util.Arrays.append(cutIntersectionCoords[fromCornerRegion][i][j][k], w);

                                        double[] to = com.donhatchsw.util.Arrays.append(cutIntersectionCoords[toCornerRegion][i][k][j], -w);  // interchange k and j because flipping, and negate w

                                        if (futtVerboseLevel >= 3) System.out.println("              setting vert from="+VecMath.toString(from)+" -> to="+VecMath.toString(to));
                                        finalMorphDestinations.put(from, to);
                                    }
                                }
                            }
                        }
                        else
                        {
                            // not an edge flip in 3d
                            for (int fromCornerRegion = 0; fromCornerRegion < cutIntersectionCoords.length; ++fromCornerRegion)
                            {
                                // Note, this is the part of the calculation that would make no sense if it's an edge flip.
                                int toCornerRegion = (fromCornerRegion+(this.gripUsefulMats[gripIndex][1][3]<0?-1:1)*dir + gonality)%gonality;
                                // CBB: all these dimensions better be the same, so don't need to be looking at all different lengths.  same when creating them
                                for (int i = 0; i < cutIntersectionCoords[fromCornerRegion].length; ++i)
                                for (int j = 0; j < cutIntersectionCoords[fromCornerRegion][i].length-1; ++j)  // don't include the extra layer here; it would be redundant
                                for (int k = 0; k < cutIntersectionCoords[fromCornerRegion][i][j].length; ++k) {
                                    for (int wdir = -1; wdir <= 1; wdir += 2) {
                                        double pad = Math.abs(vertsDForFutt[0][3]); // hacky way to retrieve what was used for thickness in w direction
                                        double w = wdir * pad;
                                        double[] from = com.donhatchsw.util.Arrays.append(cutIntersectionCoords[fromCornerRegion][i][j][k], w);
                                        double[] to = com.donhatchsw.util.Arrays.append(cutIntersectionCoords[toCornerRegion][i][j][k], w);
                                        if (futtVerboseLevel >= 3) System.out.println("              setting vert from="+VecMath.toString(from)+" -> to="+VecMath.toString(to));
                                        finalMorphDestinations.put(from, to);
                                    }
                                }
                            }
                        }

                        //System.out.println("      cutIntersectionCoords = "+VecMath.toString(cutIntersectionCoords));
                        //System.out.println("      cutIntersectionCoordsF = "+VecMath.toString((float[][][][][])VecMath.doubleToFloat(cutIntersectionCoords)));
                        // TODO: figure out why does VecMath.toString act lamely (just print addresses) if I don't cast to float[][][][][]?

                        if (false)  // set to true for debugging, to avoid morph altogether and just do the rotate
                        {
                            for (int iVert = 0; iVert < vertsDForFutt.length; ++iVert)
                            {
                                if (whichVertsGetMoved[iVert])
                                    VecMath.vxm(outVerts[iVert], vertsF[iVert], matF);
                                else
                                    VecMath.copyvec(outVerts[iVert], vertsF[iVert]);
                            }
                        }
                        else
                        {
                            float[] toF = new float[4];  // scratch for loop
                            float[] toFinFromSpace = new float[4];  // scratch for loop
                            float[] morphedFromF = new float[4];  // scratch for loop
                            for (int iVert = 0; iVert < vertsDForFutt.length; ++iVert)
                            {
                                if (whichVertsGetMoved[iVert])
                                {
                                    double[] from = vertsDForFutt[iVert];
                                    double[] to = (double[])finalMorphDestinations.get(from);
                                    if (futtVerboseLevel >= 3) System.out.println("          found vert from="+VecMath.toString(from)+" -> to="+VecMath.toString(to));
                                    CHECK(to != null);

                                    // Ok, now what's the right thing to do?
                                    // Need to apply the inverse of the *full* (i.e. frac=1) twist mat to toF
                                    // to get its coords in from space,
                                    // then apply the morph,
                                    // then apply the partial twist mat.
                                    VecMath.doubleToFloat(toF, to);
                                    VecMath.vxm(toFinFromSpace, toF, fullInvMatF);
                                    float[] fromF = vertsF[iVert];
                                    VecMath.lerp(morphedFromF, fromF, toFinFromSpace, frac);
                                    VecMath.vxm(outVerts[iVert], morphedFromF, matF);
                                }
                                else
                                    VecMath.copyvec(outVerts[iVert], vertsF[iVert]);
                            }
                        }
                    }

                    // Populate outStickerCenters, outStickerShrinkToPointsOnFaceBoundaries, and outPerStickerFaceCenters.
                    {
                        // I think the only entries we need to change in outPerStickerFaceCenters are the ones that contribute to the stickers that are moving.
                        // The others wouldn't necessarily make any sense anyway.
                        int nFacets = originalElements[nDims-1].length;
                        float[][] rotatedMorphedFaceCenters = new float[nFacets][/*nDisplayDims=*/4];
                        for (int i = 0; i < gonality; ++i)
                        {
                            float[] fromF = facetCentersF[neighborsThisFaceInOrder[i]];
                            float[] toF = facetCentersF[from2toFacet[neighborsThisFaceInOrder[i]]];

                            float[] toFinFromSpace = VecMath.vxm(toF, fullInvMatF);
                            float[] morphedFrom = VecMath.lerp(fromF, toFinFromSpace, frac);
                            VecMath.vxm(rotatedMorphedFaceCenters[neighborsThisFaceInOrder[i]], morphedFrom, matF);
                        }
                        VecMath.vxm(rotatedMorphedFaceCenters[iFacet], facetCentersF[iFacet], matF);

                        for (int iSticker = 0; iSticker < stickerCentersD.length; ++iSticker)
                        {
                            // CBB: we are testing these too many times!
                            if (pointIsInSliceMask(stickerCentersD[iSticker],
                                                   slicemask,
                                                   thisFaceInwardNormal,
                                                   thisFaceCutOffsets))
                            {
                                int jSticker = from2toStickerCenters[iSticker];
                                if (futtVerboseLevel >= 3) System.out.println("          sticker "+iSticker+" -> "+jSticker);
                                {
                                    float[] fromF = stickerCentersF[iSticker];
                                    float[] toF = stickerCentersF[jSticker];
                                    float[] toFinFromSpace = VecMath.vxm(toF, fullInvMatF);
                                    float[] morphedFrom = VecMath.lerp(fromF, toFinFromSpace, frac);
                                    VecMath.vxm(outStickerCenters[iSticker], morphedFrom, matF);
                                }
                                {
                                    float[] fromF = stickerAltCentersF[iSticker];
                                    float[] toF = stickerAltCentersF[jSticker];
                                    float[] toFinFromSpace = VecMath.vxm(toF, fullInvMatF);
                                    float[] morphedFrom = VecMath.lerp(fromF, toFinFromSpace, frac);
                                    VecMath.vxm(outStickerShrinkToPointsOnFaceBoundaries[iSticker], morphedFrom, matF);
                                }
                                VecMath.copyvec(outPerStickerFaceCenters[iSticker], rotatedMorphedFaceCenters[sticker2face[iSticker]]);
                            }
                        }
                    }
                }  // nDims==3

                if (futtVerboseLevel >= 1) System.out.println("  WHOLE LOTTA FUTTIN WENT ON");
                if (futtVerboseLevel >= 1) System.out.println("  ==========================");
            }
            if (verboseLevel >= 1) System.out.println("out computeVertsAndShrinkToPointsPartiallyTwisted(gripIndex="+gripIndex+", dir="+dir+", slicemask="+slicemask+", futtIfPossible="+futtIfPossible+", frac="+frac+")");
        } // computeVertsAndShrinkToPointsPartiallyTwisted



        public int[/*nStickers*/] getSticker2Face()
        {
            // Make sure caller didn't mess it up from last time!!
            if (!VecMath.equals(sticker2face, sticker2faceShadow))
                throw new RuntimeException("PolytopePuzzleDescription.getSticker2Facet: caller modified previously returned sticker2face! BAD! BAD! BAD!");
            return sticker2face;
        }
        public int[/*nStickers*/] getSticker2Cubie()
        {
            return sticker2cubie;
        }
        public int[/*nFacets*/] getGrip2Face()
        {
            return grip2face;
        }
        public int[/*nStickers*/][/*nPolygonsThisSticker*/] getStickerPoly2Grip()
        {
            return stickerPoly2Grip;
        }
        public int[/*nFacets*/] getFace2OppositeFace()
        {
            return facet2OppositeFacet;
        }
        public int[][/*2*/][/*2*/]
            getAdjacentStickerPairs()
        {
            return adjacentStickerPairs;
        }
        public float[/*nFacets*/][/*nDisplayDims*/]
            getFaceCentersAtRest()
        {
            return facetCentersF;
        }
        public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
                                                    int gripIndex,
                                                    int dir,
                                                    int slicemask,
                                                    boolean futtIfPossible)
        {
            // XXX dup code
            boolean weWillFutt = futtIfPossible
                              && this.futtable
                              && slicemask < (1<<((this.intLengthsForFutt[0]-1)/2))  // e.g. if intLength is 9, then nCuts is 4, so we can handle only up through 1+2+4+8
                              && this.gripSymmetryOrdersFutted[gripIndex] != 1;
            if (gripIndex < 0 || gripIndex >= nGrips())
                throw new IllegalArgumentException("applyTwistToState called on bad gripIndex "+gripIndex+", there are "+nGrips()+" grips!");
            if ((weWillFutt ? this.gripSymmetryOrdersFutted : this.gripSymmetryOrders)[gripIndex] == 0)
                throw new IllegalArgumentException("applyTwistToState called on gripIndex "+gripIndex+" which does not rotate!");
            if (state.length != stickerCentersD.length)
                throw new IllegalArgumentException("applyTwistToState called with wrong size state "+state.length+", expected "+stickerCentersD.length+"!");

            if (slicemask == 0)
                slicemask = 1; // XXX is this the right place for this? lower and it might be time consuming, higher and too many callers will have to remember to do it

            double[] scratchVert = new double[nDims()];
            double[][] matD = getTwistMat(gripIndex, dir, weWillFutt, 1.);
            int[] newState = new int[state.length];
            int iFacet = grip2face[gripIndex];
            double[] thisFaceInwardNormal = facetInwardNormals[iFacet];
            double[] thisFaceCutOffsets = facetCutOffsets[iFacet];

            if (weWillFutt)
            {
                int nDims = nDims();
                if (nDims == 4)
                {
                    VecMath.copyvec(newState, state);  // TODO: remove this when I have something better
                }
                else if (nDims == 3)
                {
                    int[][] edgesAndNeighborsThisFaceInOrder = getFaceNeighborsInOrderForFutt(iFacet);
                    int[] edgesThisFaceInOrder = edgesAndNeighborsThisFaceInOrder[0];
                    int[] neighborsThisFaceInOrder = edgesAndNeighborsThisFaceInOrder[1];
                    int[] from2toFacet = getFrom2toFacetsForFutt(gripIndex, dir, edgesThisFaceInOrder, neighborsThisFaceInOrder);
                    int[] from2toStickers = getFrom2toStickersForFutt(gripIndex, dir, slicemask, from2toFacet);
                    for (int iSticker = 0; iSticker < state.length; ++iSticker)
                    {
                        newState[from2toStickers[iSticker]] = state[iSticker];
                    }
                }
            }
            else
            {
                for (int iSticker = 0; iSticker < state.length; ++iSticker)
                {
                    if (pointIsInSliceMask(stickerCentersD[iSticker],
                                           slicemask,
                                           thisFaceInwardNormal,
                                           thisFaceCutOffsets))
                    {
                        VecMath.vxm(scratchVert, stickerCentersD[iSticker], matD);
                        Integer whereIstickerGoes = (Integer)stickerCentersHashTable.get(scratchVert);
                        CHECK(whereIstickerGoes != null);
                        newState[whereIstickerGoes.intValue()] = state[iSticker];
                    }
                    else
                        newState[iSticker] = state[iSticker];
                }
            }
            VecMath.copyvec(state, newState);
            // XXX we both modify state, and return a different array?  This can't be right, but callers might depend on it
            return newState;
        } // applyTwistToState


        // does NOT do the slicemask 0->1 correction
        private static boolean pointIsInSliceMask(double point[],
                                                  int slicemask,
                                                  double cutNormal[],
                                                  double cutOffsets[])
        {
            // XXX a binary search would work better if num cuts is big.
            // XXX really need to check offsets only between differing
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

        //CSG.verboseLevel = 2;

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
