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
        - argh, auto-4dscale doesn't work well on nonuniform (frucht)
        - 3d shrinks sure seem like they are doing nonsense-- they are not shrinking towards respective centers, they seem like they also include some uncontrolled exploding.  this is even when 4d shrinks are 1.
                - in fact, try "5,3 5", flatten, fiddle with 3d face shrink-- it goes totally crazy! cool effect but wrong
        - "5x5 2" and "4x5 2" strange flickering of some stickers during twists! and in some views (rotate things to center), some wildly inside out stickers showing.  (conjecture:  I didn't get the non-degerate-normalization correct on these further-cuts puzzles)

        - CHECK fail on 3d puzzle when 1color sticker gonality isn't same as the facet gonality: puzzleDescription="(.25)4(2)3 3(1.4)": CHECK(cutWeight >= -1e-9 && cutWeight <= 1.) (cutWeight is -.75)
        - 5-dimensional puzzles get ArrayIndexOutOfBoundException when trying to view them (should just get rejected, I think)
        - can't fling on laptop (neither macboox nor glinux), and it's sucky even with mouse (glinux)
        - '{4,3} 3(4)' with nonzero stickers-shrink-to-face-boundaries is asymmetric (due to the one-of-opposite-pairs-doing-all-the-cuts-for-both-of-them thing, I think)
        - make && java -jar donhatchsw.jar puzzleDescription="Fruity 3(9)" shouldn't require such a shallow cut specification!  isn't it supposed to be using the edge that would give the shallowest cut?
        - `java -jar donhatchsw.jar puzzleDescription='{4,3} 2,3,4'`, twisting gives CHECK failure: "CHECK(whereIstickerGoes != null);", 	at com.donhatchsw.mc4d.PolytopePuzzleDescription.applyTwistToState(PolytopePuzzleDescription.java:2402)
        - >=5 dimensional puzzles on command line non-gracefully excepts
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

        - "{5,3} 3(1.0001)" (maybe just 3(1) now, since slicing more robust) "stickers shrink to face boundaries" doesn't work

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
        - get antialiasing's notion of "still" right, then make it "antialias when still" by default
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
        TOPSORTING:
            - "4,3 3", flatten, turn on topsort viz, do twists... sometimes spazzes out and draws lines to the upper-left of window, wtf?
            - get confidence in new algorithm, maybe retire old?
            - viz would look a lot nicer (and wouldn't jump around) if lines were drawn from the poly center rather than otherwise. would need to store that in the array I guess
            - work more towards getting rid of all per-frame memory allocations (make it a reusable sorter object, with all allocations done once up front?)

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
                {}v()  (triangle)
                3v()  (tetrahedron)
                4v()  (square pyramid!)
                etc.
                4,3v()
                5,3v()
                3v3 (gets different ArrayIndexOutOfBoundsException 0 just because it's 5 dimensional, but that's a different bug)
            - still TODO: naturally bipyramid, too (+ operator)

        FUTT:
            - it's picking the wrong edge sometimes, in frucht, some of the 7-gon edges  (that is, clicking on some 2faces of vert pieces choose some wacky axis)
            - scrambling a small number of scrambles isn't well behaved-- it sometimes does an order=1 move, i.e. nothing (because it allows no-op moves, I think? wait, isn't the code supposed to prevent that?)
            - decideWhetherFuttable decides topological regulars aren't futtable... but it really should only do that if it's *geometrically* regular.  or maybe don't do that check at all (but would have to get futt working properly with arbitrary slicemask first)
            - make more general implementation:
              - support other than trivalent
              - allow slicesmask to express more layers than just first wave  (maybe just make sure exotic slicemask disables futting?  think about it)
              - do the right thing when waves interact
              - do the right thing for length=2 in various cases (cube, triprism, ...)
              - interact properly with heterogeneous intLengths
            - Canonical examples:
              - 3d:
                - frucht: all faces futtable
                  - "frucht 3(4)"
                  - "frucht 9(20)"
                  - frucht such that different waves interact
                - prisms: the square is futtable
                  - tet prism "3x{} 3(4)"
                  - tet prism "3x{} 9(20)"
                  - tet prism such that different waves interact
                - trunc ico: the hexagon is futtable
                  - trunc ico "3(1)5 3(4)"
                  - trunc ico "3(1)5 9(20)"
                  - tet prism such that different waves interact
              - 4d:
                - "4x3 3(4)": the cube is futtable
                - Fruity
                - tet prism: the 3 cubes are futtable
                  - tri prism prism "3x4 3(5)"
                  - tri prism prism "3x4 9(20)"
                  - tri prism prism such that different waves interact
              TODO: example of 4d 4-valent where a triprism needs futt for every one of its nontrivial local symmetries.
              TODO: example of 4d 4-valent where a triprism needs futt for some but not all of its nontrivial local symmetries.
              TODO: example of 4d 4-valent where a triprism needs futt for a twist on its tri, but some of its twists on squares don't need futt
              TODO: more subtle localities?

        FRUCHT/FRUITY
            - 4d fruity isn't right yet, need to work on it

        NONUNIFORM BOXES:
            - "{4,3} 2,3,4" strangely asymmetric now?  and throws on click.  (oh, that was never the way to do a nonuniform box.  it was "(2)x(3)x(4) 2,3,4".  but, should fix the throw)
            - "{4,3,3} 2,3,3" gives irregular stickers :-(  OH this is same as previous-- this is NOT the way to do a nonuniform box!  I keep getting this wrong, need to make this more discoverable!
            - "{4,3,3} 2,3,3" CHECK fail on some twists:  (more cases of the above)
                Exception in thread "AWT-EventQueue-0" java.lang.Error: CHECK failed: whereIstickerGoes != null
                  at com.donhatchsw.mc4d.PolytopePuzzleDescription.CHECK(PolytopePuzzleDescription.java:539)
                  at com.donhatchsw.mc4d.PolytopePuzzleDescription.applyTwistToState(PolytopePuzzleDescription.java:3667)
                  at com.donhatchsw.mc4d.MC4DModel.advanceAnimation(MC4DModel.java:402)
                  at com.donhatchsw.mc4d.MC4DViewGuts.paint(MC4DViewGuts.java:893)

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
import com.donhatchsw.util.CSG;
import com.donhatchsw.util.FuzzyPointHashTable;
import com.donhatchsw.util.SortStuff;
import com.donhatchsw.util.VecMath;

public class PolytopePuzzleDescription implements GenericPuzzleDescription {

    public static boolean forceFuttableXXX = false;  // temporary

    private String prescription; // what was originally passed to the constructor. we are an immutable object that is completely a function of this string, so an identical clone of ourself can be constructed using this string in any future lifetime.
    @Override public String getPrescription() { return prescription; }

    @Override public String getTopologicalFingerprintHumanReadable() { return topologicalFingerprintHumanReadable; }
    @Override public String getTopologicalFingerprintDigest() { return topologicalFingerprintDigest; }

    private CSG.SPolytope originalPolytope;
    private CSG.SPolytope slicedPolytope;

    private String originalPolytopeHumanReadableTopologicalFingerprint;  // currently not exposed
    private String originalPolytopeTopologicalFingerprintDigest;  // currently not exposed
    private String topologicalFingerprintHumanReadable;  // exposed via getTopologicalFingerprintHumanReadable()
    private String topologicalFingerprintDigest;  // exposed via getTopologicalFingerprintDigest()

    private int _nDisplayDims = 4; // never tried anything else, it will probably crash
    private float _circumRadius;
    private float _inRadius;
    private int _nCubies;

    private float[/*nVerts*/][/*nDisplayDims*/] vertsF;
    private double[/*nVerts*/][/*nDisplayDims*/] vertsDForFutt; // not needed in general, but currently needed for on-the-fly analysis when futt'ing.
    private int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/] stickerInds;  // indices into vertsF

    private float[/*nFacets*/][/*nDisplayDims*/] facetCentersF;

    private int[][/*2*/][/*2: iSticker,iPolyThisSticker*/] adjacentStickerPairs;
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
    private FuzzyPointHashTable<Integer> stickerCentersHashTable;

    private static void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }
    private static void Assumpt(boolean condition) { if (!condition) throw new Error("Assumption failed"); }
    private static String $(Object obj) { return com.donhatchsw.util.Arrays.toStringCompact(obj); }  // convenience

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
    * The following schlafli product symbols are supported,
    * using 'x' or '*' for the binary product operator.
    * Note that the following are all uniform, and every vertex has exactly
    * nDims incident facets and edges (things can go a bit crazy otherwise).
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
    * Other uniform polyhedra and polychora are also supported;
    * for details on the notation, see the doc for CSG.makeRegularStarPolytopeProductFromString();
    * however, note that, when used here, the schlafli product part must not contain any spaces.
    * (Using '*' instead of 'x' can help here, in some cases.)
    *
    * TODO: that doc is extensive but not complete.
    *
    * progressWriter and progressCallbacks are two different methods
    * of reporting progress back to the caller; progressCallbacks
    * provides the ability for the caller to cancel the construction,
    * in which case the polytope will be in a bad state and should not be used.
    */
    public PolytopePuzzleDescription(String prescription,
                                     java.io.PrintWriter progressWriter,
                                     ProgressCallbacks progressCallbacks)
    {
        prescription = prescription.replaceAll("Grand Antiprism",
                                               "Grand_Antiprism");

        // TODO: Document this not-yet-documented feature: can be more than one length (each with optional double override), specifying a different cut scheme for each dimension.  The one chosen for a given facet is the one whose index is the index of the coord axis most closely aligned with the facet normal.  Probably makes sense for only axis-aligned boxes.
        java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(
            "\\s*([^ ]+)\\s+((\\d+)(\\((.*)\\))?(,(\\d+)(\\((.*)\\))?)*)"
        ).matcher(prescription);
        if (!matcher.matches())
            throw new IllegalArgumentException("PolytopePuzzleDescription didn't understand prescription string "+com.donhatchsw.util.Arrays.toStringCompact(prescription)+"");

        String schlafliProductString = matcher.group(1);
        String commaSeparatedLengthsString = matcher.group(2);
        String[] lengthStrings = commaSeparatedLengthsString.split(",");
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

                    java.util.regex.Matcher submatcher =
                    java.util.regex.Pattern.compile(
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

        if (init(schlafliProductString, intLengths, doubleLengths, progressWriter, progressCallbacks))
        {
            this.prescription = prescription;
        }
        else
        {
            // CBB: should maybe null out everything?
            this.prescription = "(cancelled)";
        }
    } // ctor that takes just a string

    private int intpow(int a, int b) { return b==0 ? 1 : intpow(a,b-1) * a; }  // simple, slow

    private boolean decideWhetherFuttable(int[] intLengths, java.io.PrintWriter progressWriter)
    {
        if (progressWriter != null) progressWriter.println("        is it futtable?");

        if (forceFuttableXXX)
        {
            if (progressWriter != null) progressWriter.println("        deciding it's futtable because override");
            return true;
        }

        int nDims = this.originalPolytope.p.dim;
        if (nDims != 3 && nDims != 4)
        {
            if (progressWriter != null) progressWriter.println("        deciding it's not futtable because nDims="+nDims+" is not 3 or 4");
            return false;
        }

        // If intLengths are not all the same, we can't handle it.
        int intLength = intLengths[0];
        for (int i = 0; i < intLengths.length; ++i) {
            if (intLengths[i] != intLength)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because intLengths are not all the same");
                return false;
            }
        }

        if (true)
        {
            // If intLength<3, we can't handle it.
            if (intLength < 3)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because intLength="+intLength+" < 3");
                return false;
            }

            // If intLength isn't odd, we can't handle it.
            if (intLength % 2 == 0)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because intLength="+intLength+" isn't odd");
                return false;
            }
        }

        int nCutsPerFacet = (intLength-1)/2;

        CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
        int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();
        int nVerts = originalElements[0].length;
        int nEdges = originalElements[1].length;
        int nPolys = originalElements[2].length;
        int nFacets = originalElements[nDims-1].length;

        if (true)
        {
            // If any vertex figure is not a simplex (i.e. has valence other than nDims),
            // we can't handle it.
            for (int iVert = 0; iVert < nVerts; ++iVert) {
                if (originalIncidences[0][iVert][1].length != nDims)
                {
                    if (progressWriter != null) progressWriter.println("        deciding it's not futtable because at least one vertex figure is not a simplex");
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
        int nStickerPolys = stickerElementCounts[2];
        int nStickers = stickerElementCounts[nDims-1];
        if (nDims==3) CHECK(nStickerVerts + nStickers == nStickerEdges + 2);  // Euler's formula
        if (progressWriter != null) progressWriter.println("            sticker element counts = "+VecMath.toString(stickerElementCounts));
        if (true)
        {
            // Recall this is counting sticker verts *before* separation.
            // E.g. each original vertex contributes only one sticker vert.
            // If I was smart, I'd do this using inclusion-exclusion.
            int expectedNumStickerVerts = 0;  // and counting
            for (int iDim = 0; iDim <= nDims-1; ++iDim)
            {
                int contributionPerElementVertex = intpow(nCutsPerFacet, iDim);
                for (int iElt = 0; iElt < originalIncidences[iDim].length; ++iElt)
                {
                    int nVertsThisElt = originalIncidences[iDim][iElt][0].length;
                    expectedNumStickerVerts += nVertsThisElt * contributionPerElementVertex;
                }
            }
            if (progressWriter != null) progressWriter.println("            num sticker verts = "+nStickerVerts+" "+(nStickerVerts==expectedNumStickerVerts?"==":"!=")+" "+expectedNumStickerVerts+" = expected num sticker verts");
            if (nStickerVerts != expectedNumStickerVerts)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because num sticker verts is not as expected");
                return false;
            }
        }
        if (true)
        {
            int expectedNumStickerEdges = 0;  // and counting
            for (int iDim = 1; iDim <= nDims-1; ++iDim) // skip 0
            {
                int contributionPerElementEdge = (2*nCutsPerFacet+1)*intpow(nCutsPerFacet, iDim-1);
                for (int iElt = 0; iElt < originalIncidences[iDim].length; ++iElt)
                {
                    int nEdgesThisElt = originalIncidences[iDim][iElt][1].length;
                    expectedNumStickerEdges += nEdgesThisElt * contributionPerElementEdge;
                }
            }
            if (progressWriter != null) progressWriter.println("            num sticker edges = "+nStickerEdges+" "+(nStickerEdges==expectedNumStickerEdges?"==":"!=")+" "+expectedNumStickerEdges+" = expected num sticker edges");
            if (nStickerEdges != expectedNumStickerEdges)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because num sticker edges is not as expected");
                return false;
            }
        }
        if (true)
        {
            int expectedNumStickerPolys = 0;  // and counting
            for (int iDim = 2; iDim <= nDims-1; ++iDim) // start at 2
            {
                for (int iElt = 0; iElt < originalIncidences[iDim].length; ++iElt)
                {
                    int nPolysThisElt = originalIncidences[iDim][iElt][2].length;
                    for (int iPolyThisElt = 0; iPolyThisElt < nPolysThisElt; ++iPolyThisElt)
                    {
                        int iPoly = originalIncidences[iDim][iElt][2][iPolyThisElt];
                        int gonality = originalIncidences[2][iPoly][0].length;
                        expectedNumStickerPolys += (1 + gonality*(nCutsPerFacet+1)*nCutsPerFacet) * intpow(nCutsPerFacet, iDim-2);
                    }
                }
            }
            if (progressWriter != null) progressWriter.println("            num sticker polys = "+nStickerPolys+" "+(nStickerPolys==expectedNumStickerPolys?"==":"!=")+" "+expectedNumStickerPolys+" = expected num sticker polys");
            if (nStickerPolys != expectedNumStickerPolys)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because num sticker polys is not as expected");
                return false;
            }
        }
        if (true)
        {
            int expectedNumStickers = 0;  // and counting
            for (int iDim = 0; iDim <= nDims-1; ++iDim)
            {
                int contributionPerElement = intpow(nCutsPerFacet, nDims-1 - iDim) * (nDims-iDim);
                expectedNumStickers += originalElements[iDim].length * contributionPerElement;
            }
            if (progressWriter != null) progressWriter.println("            num stickers = "+nStickers+" "+(nStickers==expectedNumStickers?"==":"!=")+" "+expectedNumStickers+" = expected num stickers");
            if (nStickers != expectedNumStickers)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because num stickers is not as expected");
                return false;
            }
        }

        int[][][][] slicedIncidences = this.slicedPolytope.p.getAllIncidences();

        int maxGonality = 0;
        for (int iPoly = 0; iPoly < nPolys; ++iPoly)
        {
            int gonality = originalIncidences[2][iPoly][0].length;
            maxGonality = Math.max(maxGonality, gonality);
        }
        if (progressWriter != null) progressWriter.println("            maxGonality = "+maxGonality);

        for (int p = 3; p <= maxGonality; ++p)
        {
            if (p == 4) continue;  // squares are special
            // In 3d, num sticker p-gons should be num p-gons in original.
            // In 4d, num sticker p-gons should be num p-gons in original, times 2*nCutsPerFacet+1.
            // Not sure what the general formula is.  Whatever.
            int expectedNumStickerPgons = 0; // and counting
            for (int iPoly = 0; iPoly < nPolys; ++iPoly)
            {
                int gonality = originalIncidences[2][iPoly][0].length;
                if (gonality == p) expectedNumStickerPgons += (nDims==3 ? 1 :
                                                               nDims==4 ? 2*nCutsPerFacet+1 :
                                                               -1000);  // not sure what it should be in general
            }
            int nStickerPgons = 0;
            for (int iStickerPoly = 0; iStickerPoly < nStickerPolys; ++iStickerPoly)
            {
                int gonality = slicedIncidences[2][iStickerPoly][0].length;
                if (gonality == p) nStickerPgons++;
            }
            if (progressWriter != null) progressWriter.println("            num sticker "+p+"-gons = "+nStickerPgons+" "+(nStickerPgons==expectedNumStickerPgons?"==":"!=")+" "+expectedNumStickerPgons+" = expected num sticker "+p+"-gons");
            if (nStickerPgons != expectedNumStickerPgons)
            {
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because num sticker "+p+"-gons is not as expected");
                return false;
            }
        }

        // CBB: this actually kinda sucks because it prevents legitimate futting on things
        // whose topology is regular but whose geometry isn't; for example, "{3}v()"  (if that comes out stretched,
        // which it does at the time of this writing) (but not at the time of *this* writing).
        // TODO: I think, when I get edge futting working, just remove this check so that topological regulars will be futtable too.  Actually first need to think about whether futtable loses other stuff... e.g. it reduces number of things you can do with slicemask.  Maybe just make exotic slicemask disable futtability for that move?
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
                if (progressWriter != null) progressWriter.println("        deciding it's not futtable because topology is regular");
                return false;  // don't need to futt, so declare it non-futtable
            }
        }

        // XXX TODO: still no good!  we need to declare "frucht 3(2.5)" non-futtable, but haven't figured out how to detect that yet, since incidence counts are masquerading as the futtable case!  Well at least it rejects "fruct 3".
        if (progressWriter != null) progressWriter.println("        deciding it's futtable!");

        return true;
    }  // decideWhetherFuttable

    // split into lines, indent each line, and re-join.
    private static String indented(String indent, String text) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; ++i) {
            sb.append(indent);
            sb.append(lines[i]);
            if (i != lines.length-1) sb.append("\n");
        }
        return sb.toString();
    }

    // returns true if succeeded, false if cancelled.
    private boolean init(String schlafliProduct,
                         int[] intLengths, // number of segments per edge, possibly per-face
                         double[] doubleLengths, // edge length / length of first edge segment, possibly per-face
                         java.io.PrintWriter progressWriter,
                         ProgressCallbacks progressCallbacks)
    {
        CHECK(intLengths.length == doubleLengths.length);
        for (int i = 0; i < intLengths.length; ++i) {
          if (intLengths[i] < 1)
              throw new IllegalArgumentException("PolytopePuzzleDescription called with intLength="+intLengths[i]+", min legal intLength is 1");
          if (doubleLengths[i] < 0)
              throw new IllegalArgumentException("PolytopePuzzleDescription called with doubleLength="+doubleLengths[i]+", doubleLength must be nonnegative");
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
        if (progressCallbacks != null && !progressCallbacks.subtaskInit("Constructing polytope")) return false;
        this.originalPolytope = CSG.makeRegularStarPolytopeProductJoinFromString(schlafliProduct);

        if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Constructing polytope"
        if (progressWriter != null)
        {
            progressWriter.println(" done ("+originalPolytope.p.facets.length+" facet"+(originalPolytope.p.facets.length==1?"":"s")+").");
            progressWriter.flush();
        }


        {
            {
                if (progressWriter != null) {
                    progressWriter.print("    Computing fingerprint of polytope... ");
                    progressWriter.flush();
                }
                if (progressCallbacks != null && !progressCallbacks.subtaskInit("Computing fingerprint of polytope")) return false;
                long t0millis = System.currentTimeMillis();
                this.originalPolytopeHumanReadableTopologicalFingerprint = CSG.computeHumanReadableTopologicalFingerprint(this.originalPolytope.p);
                this.originalPolytopeTopologicalFingerprintDigest = CSG.sha1(this.originalPolytopeHumanReadableTopologicalFingerprint);
                long t1millis = System.currentTimeMillis();
                if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Computing fingerprint of polytope"
                if (progressWriter != null) {
                    progressWriter.println(this.originalPolytopeTopologicalFingerprintDigest+" ("+millisToSecsString(t1millis-t0millis)+" seconds)");
                    progressWriter.flush();
                    progressWriter.println("    Human readable topological fingerprint of polytope:");
                    progressWriter.println(indented("        ", this.originalPolytopeHumanReadableTopologicalFingerprint));
                }
            }
        }

        if (this.originalPolytope.p.dim < 2)
        {
            throw new IllegalArgumentException("PolytopePuzzleDescription can't do puzzles of dimension "+this.originalPolytope.p.dim+" (< 2)");
        }

        int nDims = originalPolytope.p.dim;  // == originalPolytope.fullDim

        CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
        CSG.Polytope[] originalVerts = originalElements[0];
        CSG.Polytope[] originalFacets = originalElements[nDims-1];
        int nFacets = originalFacets.length;
        int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();

        // Mark each original facet with its facet index.
        // These marks will persist even after we slice up into stickers,
        // so that will give us the sticker-to-original-facet-index mapping.
        // Also mark each vertex with its vertex index... etc.
        {
            for (int iDim = 0; iDim < originalElements.length; ++iDim)
            for (int iElt = 0; iElt < originalElements[iDim].length; ++iElt)
            {
                originalElements[iDim][iElt].setAux(Integer.valueOf(iElt));
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
            FuzzyPointHashTable<CSG.Polytope> table = new FuzzyPointHashTable<CSG.Polytope>(1e-9, 1e-8, 1./64);  // 1e-9, 1e-8, 1/128 made something hit a wall on the omnitruncated 120cell
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                table.put(facetInwardNormals[iFacet], originalFacets[iFacet]);
            double[] oppositeNormalScratch = new double[nDims];
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                VecMath.vxs(oppositeNormalScratch, facetInwardNormals[iFacet], -1.);
                CSG.Polytope opposite = table.get(oppositeNormalScratch);
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

                // Interpret doubleLength==0 as infinity, i.e. sliceLength==0, because there's nothing else it could mean.
                double sliceThickness = doubleLength==0. ? 0. : fullThickness / doubleLength;

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
                int nFarCuts = facet2OppositeFacet[iFacet]==-1 ? 0 : nNearCuts;

                // In some cases, notably an even length prism or antiprism of this face,
                // the middle cut is the same from either side: it goes through the origin.
                // In that case, we'd like to do it only as a near cut, so nFarCuts
                // should be nNearCuts-1.
                //
                // HOWEVER, there are other cases where the last cut on the two sides are equal,
                // notably truncated tetrahedron with carefully chosen cut depth: "(1)3(1)3(0) 2(1)"
                // (and truncated simplex in 4d, too, probably), and there's no way
                // of predicting when this will happen.
                // In fact, we can cause similar problems even for odd numbers of slices on standard puzzles:
                //      "{4,3,3} 7(6)"
                //      "{4,3,3} 7(4)"
                // Given that, what we do is as follows: make the list of cuts (even though nFarCuts may
                // may be too many at this point), then sort and remove dups (and near dups) at the end.

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
                if (iOppositeFacet != -1) {
                  int nNearCuts = intLengths[whichLengthToUseForFacet[iFacet]] / 2; // same as in previous pass
                  int nFarCuts = facetCutOffsets[iFacet].length - nNearCuts;
                  for (int iFarCut = 0; iFarCut < nFarCuts; ++iFarCut)
                      facetCutOffsets[iFacet][nNearCuts+nFarCuts-1-iFarCut] = -facetCutOffsets[iOppositeFacet][iFarCut];
                }
            }
            // Now sort and de-dup as promised.
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                SortStuff.sort(facetCutOffsets[iFacet]);
                int nCutsRemaining = 0;
                for (int iCut = 0; iCut < facetCutOffsets[iFacet].length; ++iCut)
                {
                    if (nCutsRemaining == 0
                     || facetCutOffsets[iFacet][iCut] > facetCutOffsets[iFacet][nCutsRemaining-1] + 1e-6)
                    {
                        facetCutOffsets[iFacet][nCutsRemaining++] = facetCutOffsets[iFacet][iCut];
                    }
                }
                facetCutOffsets[iFacet] = (double[])com.donhatchsw.util.Arrays.subarray(facetCutOffsets[iFacet], 0, nCutsRemaining);
            }
            // Finally, make sure opposite cut sets are *exactly* opposite.  They might not be,
            // if de-duping made different choices.
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                int iOppositeFacet = facet2OppositeFacet[iFacet];
                if (iOppositeFacet > iFacet)  // so we do this only once per pair, and only if there is an opposite
                {
                    int nCuts = facetCutOffsets[iFacet].length;
                    CHECK(nCuts == facetCutOffsets[iOppositeFacet].length); // careful analysis of the deduping probably would show this can't fail, but check anyway
                    for (int iCut = 0; iCut < nCuts; ++iCut) {
                        CHECK(Math.abs(facetCutOffsets[iOppositeFacet][nCuts-1-iCut] - -facetCutOffsets[iFacet][iCut]) < 1e-3);  // rough sanity check, much coarser than the dedup tolerance that was used
                        facetCutOffsets[iOppositeFacet][nCuts-1-iCut] = -facetCutOffsets[iFacet][iCut];
                    }
                }
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
            if (progressCallbacks != null && !progressCallbacks.subtaskInit("Slicing", nTotalCuts)) return false;

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
                            if (iTotalCut == nTotalCuts)
                                progressWriter.print("(100%)");  // special case to avoid "1e+02"
                            else
                                progressWriter.print("("+String.format("%.2g", (double)100*iTotalCut*iTotalCut/nTotalCuts/nTotalCuts)+"%)");
                        }

                        progressWriter.flush();
                    }
                    if (progressCallbacks != null && !progressCallbacks.updateProgress(iTotalCut+1)) return false;
                }
            }
            CHECK(iTotalCut == nTotalCuts);
            long endTimeMillis = System.currentTimeMillis();

            if (progressWriter != null)
            {
                progressWriter.println(" done ("+slicedPolytope.p.facets.length+" stickers) ("+millisToSecsString(endTimeMillis-startTimeMillis)+" seconds)");
                progressWriter.flush();
            }
            if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Slicing"

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
                if (progressCallbacks != null && !progressCallbacks.subtaskInit("Computing alternate sticker shrink-to points on facet boundaries")) return false;
                this.stickerAltCentersF = computeStickerAltCentersF(
                                              slicedPolytope,
                                              facet2OppositeFacet,
                                              intLengths,
                                              doubleLengths,
                                              whichLengthToUseForFacet);
                if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Computing alternate sticker shrink-to points on facet boundaries"
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
                        stickers[iSticker].pushAux(Integer.valueOf(iSticker));
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
                if (progressCallbacks != null && !progressCallbacks.subtaskInit("Further slicing for grips", nFacets)) return false;
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
                    if (progressCallbacks != null && !progressCallbacks.updateProgress(iFacet+1)) return false;
                }
                if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Further slicing for grips"
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
                if (progressCallbacks != null && !progressCallbacks.subtaskInit("Fixing facet orderings")) return false;
                startTimeMillis = System.currentTimeMillis();
                CSG.orientDeepCosmetic(slicedPolytope);
                endTimeMillis = System.currentTimeMillis();
                if (progressWriter != null)
                {
                    progressWriter.println(" done ("+millisToSecsString(endTimeMillis-startTimeMillis)+" seconds)");
                    progressWriter.flush();
                }
                if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Fixing facet orderings"
            }
        } // slice

        CSG.Polytope[] stickers = slicedPolytope.p.getAllElements()[nDims-1];
        int nStickers = stickers.length;

        {
            this.topologicalFingerprintHumanReadable =
                "original polytope:\n" +
                indented("    ", this.originalPolytopeHumanReadableTopologicalFingerprint) + "\n" +
                "number of stickers: " + nStickers + "\n" +
                "floor(intLength/2) = ";
            for (int i = 0; i < intLengths.length; ++i) {
              if (i > 0) this.topologicalFingerprintHumanReadable += ",";
              this.topologicalFingerprintHumanReadable += intLengths[i]/2;
            }
            this.topologicalFingerprintDigest = CSG.sha1(this.topologicalFingerprintHumanReadable);
        }

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
            this.stickerCentersHashTable = new FuzzyPointHashTable<Integer>(1e-9, 1e-8, 1./128);
            {
                for (int iSticker = 0; iSticker < nStickers; ++iSticker)
                {
                    CSG.cgOfVerts(stickerCentersD[iSticker], stickers[iSticker]);
                    stickerCentersHashTable.put(stickerCentersD[iSticker], Integer.valueOf(iSticker));
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
        // (Note, in this case "original" means after slicing but before
        // swapping; it doesn't refer to pre-sliced)
        int[][] stickerPolyToOriginalStickerPoly = new int[nStickers][];

        //
        // Get the rest verts (with no shrinkage)
        // and the sticker polygon indices stickerInds
        // (i.e. mapping from sticker-and-polyThisSticker to vert indices).
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
                            stickerVerts4d[iVert].getCoords()[3] *= -1; // XXX FUDGE-- and this is totally illegal... should do this afterwards
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
                                vertex.setAux(Integer.valueOf(iVert));
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
            if (nDims == 4)
            {
                // TODO: try to do it this way for nDims==3 and nDims==2 too... or just remove this way
                // and use the "BRAIN DEAD WAY" that should work in general (but is not discrete
                // so is theoretically not as airtight).

                // Hmm, if Polytope provided back index information, we could just iterate over that,
                // but it doesn't.

                int[/*nStickers*/][/*nDims+1 or so*/][/*nEltsThisStickerThisDim*/] stickerIncidences = slicedPolytope.p.getAllIncidences()[nDims-1];
                // The expected number of adjacencies is exactly the number of ridges in the sliced polytope.
                // That is: nPolygons if nDims==4, nEdges if nDims==3, nVerts if nDims==2.
                int nAdjsExpected = slicedPolytope.p.getAllElements()[nDims-2].length;
                this.adjacentStickerPairs = new int[nAdjsExpected][2][];
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

                // We now have adjacentStickerPairs with respect to orderings before we swapped polys around.
                // Fix that.
                {
                    int[][] originalStickerPolyToStickerPoly = new int[nStickers][];
                    for (int iSticker = 0; iSticker < originalStickerPolyToStickerPoly.length; ++iSticker) {
                        originalStickerPolyToStickerPoly[iSticker] = VecMath.invertperm(stickerPolyToOriginalStickerPoly[iSticker]);
                    }
                    for (int iPair = 0; iPair < this.adjacentStickerPairs.length; ++iPair) {
                        for (int iStickerThisPair = 0; iStickerThisPair < 2; ++iStickerThisPair) {
                            int iSticker = this.adjacentStickerPairs[iPair][iStickerThisPair][0];
                            int iPolyThisStickerOld = this.adjacentStickerPairs[iPair][iStickerThisPair][1];
                            int iPolyThisStickerNew = originalStickerPolyToStickerPoly[iSticker][iPolyThisStickerOld];
                            this.adjacentStickerPairs[iPair][iStickerThisPair][1] = iPolyThisStickerNew;
                        }
                    }
                }
            }
            else
            {
                // BRAIN DEAD WAY that would actually work for nDims=4 as well--
                // just consider two polys coincidental if their sticker centers are the same.

                // The expected number of adjacencies is exactly the number of ridges in the sliced polytope.
                // That is: nPolygons if nDims==4, nEdges if nDims==3, nVerts if nDims==2.
                int nAdjsExpected = slicedPolytope.p.getAllElements()[nDims-2].length;
                this.adjacentStickerPairs = new int[nAdjsExpected][][];
                int nAdjsFound = 0;

                FuzzyPointHashTable<int[][]> polyCenter2stickerAndPolyIndices = new FuzzyPointHashTable<int[][]>(1e-9, 1e-8, 1./64);

                double[] polycenter = new double[4];  // scratch for loop
                for (int iSticker = 0; iSticker < stickerInds.length; ++iSticker) {
                    for (int iPolyThisSticker = 0; iPolyThisSticker < stickerInds[iSticker].length; ++iPolyThisSticker) {
                        int poly[] = stickerInds[iSticker][iPolyThisSticker];
                        VecMath.zerovec(polycenter);
                        for (int iVertThisPoly = 0; iVertThisPoly < poly.length; ++iVertThisPoly) {
                            double[] vert = restVerts[poly[iVertThisPoly]];
                            CHECK(vert.length == 4);
                            VecMath.vpv(polycenter, polycenter, vert);
                        }
                        VecMath.vxs(polycenter, polycenter, 1./poly.length);
                        int[][] entry = polyCenter2stickerAndPolyIndices.get(polycenter);
                        //System.out.println("      stickerpoly "+iSticker+"("+iPolyThisSticker+") center: "+com.donhatchsw.util.Arrays.toStringCompact(polycenter));
                        if (entry == null) {
                            entry = new int[][] {{iSticker, iPolyThisSticker}, {-1,-1}};
                            polyCenter2stickerAndPolyIndices.put(VecMath.copyvec(polycenter), entry);
                            //System.out.println("          not found -> "+com.donhatchsw.util.Arrays.toStringCompact(entry));
                        } else {
                            CHECK(entry[0][0] != -1);
                            CHECK(entry[0][1] != -1);
                            CHECK(entry[1][0] == -1);
                            CHECK(entry[1][1] == -1);
                            entry[1][0] = iSticker;
                            entry[1][1] = iPolyThisSticker;
                            //System.out.println("          found -> "+com.donhatchsw.util.Arrays.toStringCompact(entry));
                            this.adjacentStickerPairs[nAdjsFound++] = entry;
                        }
                    }
                }
                CHECK(nAdjsFound == nAdjsExpected);
                // Note that, if nDims<4, there will be a bunch of unmatched faces still in the table;
                // that's fine.
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
                stickerCentersD[iSticker] = com.donhatchsw.util.Arrays.append(stickerCentersD[iSticker], 0.);
                stickerCentersF[iSticker] = com.donhatchsw.util.Arrays.append(stickerCentersF[iSticker], 0.f);
                stickerAltCentersF[iSticker] = com.donhatchsw.util.Arrays.append(stickerAltCentersF[iSticker], 0.f);
            }
            for (int iFacet = 0; iFacet < nFacets; ++iFacet)
            {
                facetCentersF[iFacet] = com.donhatchsw.util.Arrays.append(facetCentersF[iFacet], 0.f);
                facetInwardNormals[iFacet] = com.donhatchsw.util.Arrays.append(facetInwardNormals[iFacet], 0.f);
            }
        }

        this.vertsF = VecMath.doubleToFloat(restVerts);

        if (progressCallbacks != null && !progressCallbacks.subtaskInit("Deciding whether futtable")) return false;
        this.futtable = decideWhetherFuttable(intLengths, progressWriter);
        if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Deciding whether futtable"
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
            if (progressCallbacks != null && !progressCallbacks.subtaskInit("Thinking about possible twists", nFacets)) return false;
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
                                        for (int maybeSymmetryOrder = maxOrder; ; --maybeSymmetryOrder)
                                        {
                                            if (maxOrder % maybeSymmetryOrder != 0) continue;
                                            CHECK(maybeSymmetryOrder >= 1);  // 1 has to succeed
                                            //System.out.println("iFacet="+iFacet+" iGrip="+iGrip+" trying symmetry order "+maybeSymmetryOrder+"/"+maxOrder);
                                            if (getFrom2toFacetsForFutt(iGrip, /*dir=*/1, maybeSymmetryOrder) != null)
                                            {
                                                //System.out.println("iFacet="+iFacet+" iGrip="+iGrip+" symmetry order "+maybeSymmetryOrder+"/"+maxOrder+" succeeded!");
                                                this.gripSymmetryOrdersFutted[iGrip] = maybeSymmetryOrder;
                                                break;
                                            }
                                            //System.out.println("iFacet="+iFacet+" iGrip="+iGrip+" symmetry order "+maybeSymmetryOrder+"/"+maxOrder+" failed");
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
                        if (progressCallbacks != null && !progressCallbacks.updateProgress(iFacet+1)) return false;
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
                        // workaround 3 from https://programming.guide/java/generic-array-creation.html
                        @SuppressWarnings("serial") class HashMapIntegerInteger extends java.util.HashMap<Integer,Integer> {}
                        HashMapIntegerInteger[/*nFacets*/][/*nRelevantDims*/] indexOfOriginalEltOnFacet = new HashMapIntegerInteger[nFacets][maxRelevantDim+1];
                        for (int iFacet = 0; iFacet < nFacets; ++iFacet)
                        {
                            CSG.Polytope[][] allElementsOfFacet = originalFacets[iFacet].getAllElements();
                            for (int iDim = 0; iDim <= maxRelevantDim; ++iDim) {
                                indexOfOriginalEltOnFacet[iFacet][iDim] = new HashMapIntegerInteger();
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
                                            Integer iOriginalEltOnThisFacet = indexOfOriginalEltOnFacet[iFacet][iOriginalEltDim].get(iOriginalElt);
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
            if (progressCallbacks != null && !progressCallbacks.subtaskDone()) return false;  // "Thinking about possible twists"
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
            progressWriter.println("    Puzzle fingerprint human readable =");
            progressWriter.println(indented("        ", getTopologicalFingerprintHumanReadable()));
            progressWriter.println("    Puzzle fingerprint digest = "+getTopologicalFingerprintDigest());
        }


        if (progressWriter != null)
        {
            progressWriter.println("Done.");
            progressWriter.flush();
        }

        return true;  // success (not cancelled)
    } // init from schlafli and length

    // XXX figure out a good public interface for something that shows stats
    public String toString(boolean verbose)
    {
        String nl = System.getProperty("line.separator");
        CSG.Polytope[][] allElements = slicedPolytope.p.getAllElements();
        String answer = "{"
                      +"original polytope counts per dim = "+com.donhatchsw.util.Arrays.toStringCompact(CSG.counts(originalPolytope.p))
            +", "+nl+"  sliced polytope counts per dim = "+com.donhatchsw.util.Arrays.toStringCompact(CSG.counts(slicedPolytope.p))
            +", "+nl+"  nDims = "+nDims()
            +", "+nl+"  nFacets = "+nFaces()
            +", "+nl+"  nStickers = "+nStickers()
            +", "+nl+"  nGrips = "+nGrips()
            +", "+nl+"  nVisibleCubies = "+nCubies()
            +", "+nl+"  nStickerVerts = "+nVerts();
        if (verbose)
        {
            answer +=
                ", "+nl+"  originalPolytope = "+originalPolytope.toString(true)
                //+", "+nl+"  slicedPolytope = "+slicedPolytope.toString(true)
                +", "+nl+"  stickerInds = "+com.donhatchsw.util.Arrays.toStringNonCompact(stickerInds, "    ", "    ")
                +", "+nl+"  sticker2face = "+com.donhatchsw.util.Arrays.toStringNonCompact(sticker2face, "    ", "    ");
        }
        answer += "}";
        return answer;
    } // toString

    // XXX get clear on exactly what is required here...
    // XXX another way of doing it would be to just let each of the subclasses of GenericPuzzleInterface try, with its fromString()... but I don't think that would work with delay loading
    @Override public String toString()
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
            @Override public String toString()
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
        // CBB: it also totally falls down for the octahedral center sticker
        // of a tetrahedron of edge length 2.  In that case the code below
        // completely fails (total weight 0) so we make a special case for that
        // and just make the alt-center the same as the center.
        //
        private static float[][] computeStickerAltCentersF(
                CSG.SPolytope slicedPolytope,
                int facet2OppositeFacet[],
                int intLengths[],
                double doubleLengths[],
                int whichLengthToUseForFacet[])
        {
            int verboseLevel = 0;  // 0: nothing; 1: in/out and constant; 2: and output array; 3: and per-item detail
            if (verboseLevel >= 1) System.out.println("        in computeStickerAltCentersF");
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
                if (verboseLevel >= 2) System.out.println("          iSticker = "+iSticker+"/"+nStickers);
                // Figure out avg cut depth of this sticker
                // with respect to each of the facets whose planes
                // contribute to it.
                // Note that "ridge" for a 4d puzzle means polygon; for a 3d puzzle it means edge.
                int[] ridgesThisSticker = allSlicedIncidences[nDims-1][iSticker][nDims-2];
                if (verboseLevel >= 1) System.out.println("              first pass over ridges ("+(nDims-2)+" dimensional elements");
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    if (verboseLevel >= 2) System.out.println("                  iRidgeThisSticker = "+iRidgeThisSticker+"/"+ridgesThisSticker.length);
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFacet, iCutThisFacet;
                    if (ridge.getAux() instanceof CutInfo)
                    {
                        if (verboseLevel >= 1) System.out.println("                      it's from a cut");
                        CutInfo cutInfo = (CutInfo)ridge.getAux();
                        iFacet = cutInfo.iFacet;
                        iCutThisFacet = cutInfo.iCutThisFacet+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        if (verboseLevel >= 1) System.out.println("                      it's not from a cut; it's from an original face");
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
                if (verboseLevel >= 2) System.out.println("              second pass over ridges ("+(nDims-2)+" dimensional elements");
                for (int iRidgeThisSticker = 0; iRidgeThisSticker < ridgesThisSticker.length; ++iRidgeThisSticker)
                {
                    if (verboseLevel >= 2) System.out.println("                  iRidgeThisSticker = "+iRidgeThisSticker+"/"+ridgesThisSticker.length);
                    int iRidge = ridgesThisSticker[iRidgeThisSticker];
                    CSG.Polytope ridge = ridges[iRidge];
                    int iFacet, iCutThisFacet;
                    if (ridge.getAux() instanceof CutInfo)
                    {
                        if (verboseLevel >= 1) System.out.println("                      it's from a cut");
                        CutInfo cutInfo = (CutInfo)ridge.getAux();
                        iFacet = cutInfo.iFacet;
                        iCutThisFacet = cutInfo.iCutThisFacet+1;
                    }
                    else // it's not from a cut, it's from an original face
                    {
                        if (verboseLevel >= 1) System.out.println("                      it's not from a cut; it's from an original face");
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
                    if (verboseLevel >= 2) System.out.println("                      cutWeight = "+cutWeight);
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
                int nVertsThisSticker = allSlicedIncidences[nDims-1][iSticker][0].length;
                if (verboseLevel >= 2) System.out.println("              num verts this sticker = "+nVertsThisSticker);
                for (int iVertThisSticker = 0; iVertThisSticker < nVertsThisSticker; ++iVertThisSticker)
                {
                    int iVert = allSlicedIncidences[nDims-1][iSticker][0][iVertThisSticker];
                    CSG.Polytope vert = verts[iVert];
                    double vertexWeight = vertexWeights[iVert];
                    if (verboseLevel >= 2) System.out.println("                  iVertThisSticker="+iVertThisSticker+"/"+nVertsThisSticker+": vertexWeight="+vertexWeight);
                    CHECK(vertexWeight >= 0. && vertexWeight <= 1.);
                    totalWeight += vertexWeight;
                    // stickerAltCenterD += vertexWeight * vertexPosition
                    VecMath.vpsxv(stickerAltCenterD,
                                  stickerAltCenterD,
                                  vertexWeight, vert.getCoords());
                    vertexWeights[iVert] = 1.; // clear for next time
                }
                if (verboseLevel >= 2) System.out.println("              totalWeight = "+totalWeight);
                if (totalWeight == 0.)
                {
                    // This happens for the center octahedron sticker of each face
                    // in a "{3,3,3} 2" or "{3,3}x{} 2" etc.
                    // I can't say that I completely recall what we're doing here, but it seems
                    // the only sensible thing to do is set the alt center point to the sticker center.
                    CSG.cgOfVerts(stickerAltCenterD, stickers[iSticker]);
                }
                else
                {
                    VecMath.vxs(stickerAltCenterD, stickerAltCenterD, 1./totalWeight);
                }

                if (verboseLevel >= 2) System.out.println("              stickerAltCenterD = "+$(stickerAltCenterD));
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

            if (verboseLevel >= 2) System.out.println("          stickerAltCentersF = "+$(stickerAltCentersF));
            if (verboseLevel >= 1) System.out.println("        out computeStickerAltCentersF");
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

        @Override public int nDims()
        {
            return slicedPolytope.p.fullDim;
        }
        @Override public int nDisplayDims()
        {
            return _nDisplayDims;
        }
        @Override public int nVerts()
        {
            return vertsF.length;
        }
        @Override public int nFaces()  // GenericPuzzleDescription interface calls it faces, but it's really facets
        {
            return originalPolytope.p.facets.length;
        }
        @Override public int nCubies()
        {
            return _nCubies;
        }
        @Override public int nStickers()
        {
            return slicedPolytope.p.facets.length;
        }
        @Override public int nGrips()
        {
            return grip2face.length;
        }
        @Override public float circumRadius()
        {
            return _circumRadius;
        }
        @Override public float inRadius()
        {
            return _inRadius;
        }

        @Override public int[/*nStickers*/][/*nPolygonsThisSticker*/][/*nVertsThisPolygon*/]
            getStickerInds()
        {
            return stickerInds;
        }
        @Override public void computeGripVertsAtRest(float verts[/*nVerts*/][/*nDims*/],
                                           float facetShrink,
                                           float stickerShrink)
        {
            throw new RuntimeException("unimplemented");
        }
        @Override public int[/*nGrips*/][/*nPolygonsThisGrip*/][/*nVertsThisPolygon*/]
            getGripInds()
        {
            throw new RuntimeException("unimplemented");
        }
        @Override public int[/*nGrips*/]
            getGripSymmetryOrders(boolean futtIfPossible)
        {
            return futtIfPossible && this.futtable ? gripSymmetryOrdersFutted : gripSymmetryOrders;
        }

        @Override public double[][] getFaceInwardNormals()
        {
            return facetInwardNormals;
        }
        @Override public double[][] getFaceCutOffsets()
        {
            return facetCutOffsets;
        }

        // XXX lame, this should be precomputed and looked up by
        // XXX poly and sticker index.
        // XXX (wait, isn't it, now?  stickerPoly2grip())
        // This is called using
        // facetCenter, polyCenter-stickerCenter.
        // XXX NEED TO TOTALLY REPLACE THE MATH TOO, WITH SOMETHING PRINCIPLED
        @Override public int getClosestGrip(float unNormalizedDir[/*4*/],
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
        @Override public float[/*nDims*/] getClosestNicePointToRotateToCenter(float pickCoords[])
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

        @Override public void
            computeVertsAndShrinkToPointsAtRest(
                float outVerts[/*nVerts*/][/*nDisplayDims*/],
                float outStickerCenters[/*nStickers*/][/*nDisplayDims*/],
                float outStickerShrinkToPointsOnFaceBoundaries[/*nStickers*/][/*nDisplayDims*/],
                float outPerStickerFaceCenters[/*nStickers*/][/*nDisplayDims*/])
        {
            int verboseLevel = 0;
            if (verboseLevel >= 1) System.out.println("        in PolytopePuzzleDescription.computeVertsAndShrinkToPointsAtRest");
            if (verboseLevel >= 1) System.out.println("          this.stickerAltCentersF = "+$(this.stickerAltCentersF));
            if (outVerts != null)
            {
                CHECK(outVerts.length == this.vertsF.length);
                for (int iVert = 0; iVert < this.vertsF.length; ++iVert)
                    VecMath.copyvec(outVerts[iVert], this.vertsF[iVert]);
            }
            if (outStickerCenters != null)
            {
                CHECK(outStickerCenters.length == stickerCentersF.length);
                for (int iSticker = 0; iSticker < this.stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outStickerCenters[iSticker],
                                    this.stickerCentersF[iSticker]);
            }
            if (outStickerShrinkToPointsOnFaceBoundaries != null)
            {
                CHECK(outStickerShrinkToPointsOnFaceBoundaries.length == this.stickerCentersF.length);
                for (int iSticker = 0; iSticker < this.stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outStickerShrinkToPointsOnFaceBoundaries[iSticker],
                                    this.stickerAltCentersF[iSticker]);
            }
            if (outPerStickerFaceCenters != null)
            {
                CHECK(outPerStickerFaceCenters.length == this.stickerCentersF.length);
                for (int iSticker = 0; iSticker < this.stickerCentersF.length; ++iSticker)
                    VecMath.copyvec(outPerStickerFaceCenters[iSticker],
                                    this.facetCentersF[sticker2face[iSticker]]);
            }
            if (verboseLevel >= 1) System.out.println("          outVerts = "+$(outVerts));
            if (verboseLevel >= 1) System.out.println("          outStickerCenters = "+$(outStickerCenters));
            if (verboseLevel >= 1) System.out.println("          outStickerShrinkToPointsOnFaceBoundaries = "+$(outStickerShrinkToPointsOnFaceBoundaries));
            if (verboseLevel >= 1) System.out.println("          outPerStickerFaceCenters = "+$(outPerStickerFaceCenters));
            if (verboseLevel >= 1) System.out.println("        out PolytopePuzzleDescription.computeVertsAndShrinkToPointsAtRest");
        } // computeVertsAndShrinkToPointsAtRest

        // Attempt to get it with the given symmetryOrder.
        // If this symmetry doesn't work out, returns null.
        private int[] getFrom2toFacetsForFutt(int gripIndex, int dir, int symmetryOrder)
        {
            int futtVerboseLevel = 0;  // 0: nothing, 1: constant size, 2: big but compactly printed arrays, 3: gory detail
            if (futtVerboseLevel >= 1) System.out.println("            in getFrom2toFacetsForFutt");
            int nDims = this.nDims();
            int iFacet = grip2face[gripIndex];
            CSG.Polytope[][] originalElements = this.originalPolytope.p.getAllElements();
            int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();
            int nFacets = originalElements[nDims-1].length;
            int gonality = originalIncidences[2][iFacet][1].length;
            int[] from2toFacet = VecMath.identityperm(nFacets);  // except for...
            {
                int eltDim = -1;
                int iFacetElt = -1;
                if (nDims == 4)
                {
                    eltDim = this.grip2facetEltForFutt[gripIndex][0];
                    iFacetElt = this.grip2facetEltForFutt[gripIndex][1];
                }
                else if (nDims == 3)
                {
                    // CBB: not sure why this.grip2facetEltForFutt[gripIndex] is sometimes null.  Do lame search for correct element.
                    boolean isEdgeFlipIn3d = Math.abs(this.gripUsefulMats[gripIndex][1][3]) < 1e-6;
                    if (isEdgeFlipIn3d)
                    {
                        eltDim = 1;

                        double bestDot = -1.;
                        int bestFacetElt = -1;
                        for (int iEdgeThisFace = 0; iEdgeThisFace < originalIncidences[nDims-1][iFacet][eltDim].length; ++iEdgeThisFace)
                        {
                            int iEdge = originalIncidences[nDims-1][iFacet][eltDim][iEdgeThisFace];
                            CSG.Polytope edge = originalElements[eltDim][iEdge];
                            double[] faceToEdge = VecMath.vmv(3, CSG.cgOfVerts(edge), VecMath.floatToDouble(facetCentersF[iFacet]));  // CBB: floatToDouble is not great.  but this is temporary code anyway, I think
                            double thisDot = VecMath.dot(3, gripUsefulMats[gripIndex][1], faceToEdge);
                            if (thisDot > bestDot)
                            {
                                bestDot = thisDot;
                                bestFacetElt = iEdgeThisFace;
                            }
                        }
                        iFacetElt = bestFacetElt;
                    }
                    else
                    {
                        eltDim = 2;
                        iFacetElt = 0;  // the facet itself
                    }
                }
                else
                {
                    CHECK(false);
                }

                if (futtVerboseLevel >= 1) System.out.println("              gripIndex = "+gripIndex);
                if (futtVerboseLevel >= 1) System.out.println("              dir = "+dir);
                if (futtVerboseLevel >= 1) System.out.println("              symmetryOrder = "+symmetryOrder);
                if (futtVerboseLevel >= 1) System.out.println("              iFacet = "+iFacet);
                if (futtVerboseLevel >= 1) System.out.println("              eltDim = "+eltDim);
                if (futtVerboseLevel >= 1) System.out.println("              iFacetElt = "+iFacetElt);
                int iEltGlobal = originalIncidences[nDims-1][iFacet][eltDim][iFacetElt];
                if (futtVerboseLevel >= 1) System.out.println("              iEltGlobal = "+iEltGlobal);


                // To figure out facet mapping, we first figure out flag mapping.
                // The flags of interest are all flags containing a vertex incident on this facet.
                int[][] flagsOfInterest;
                {
                    flagsOfInterest = null;
                    for (int iPass = 0; iPass < 2; ++iPass)  // first pass, just count
                    {
                        int nFlagsOfInterest = 0;
                        for (int iVertThisFacet = 0; iVertThisFacet < originalIncidences[nDims-1][iFacet][0].length; ++iVertThisFacet) {
                            int iVert = originalIncidences[nDims-1][iFacet][0][iVertThisFacet];
                            for (int iEdgeThisVert = 0; iEdgeThisVert < originalIncidences[0][iVert][1].length; ++iEdgeThisVert) {
                                int iEdge = originalIncidences[0][iVert][1][iEdgeThisVert];
                                for (int iFaceThisEdge = 0; iFaceThisEdge < originalIncidences[1][iEdge][2].length; ++iFaceThisEdge) {
                                    int iFace = originalIncidences[1][iEdge][2][iFaceThisEdge];
                                    if (nDims == 3)
                                    {
                                        if (iPass == 1) flagsOfInterest[nFlagsOfInterest] = new int[] {iVert, iEdge, iFace};
                                        nFlagsOfInterest++;
                                    }
                                    else if (nDims == 4)
                                    {
                                        for (int iCellThisFace = 0; iCellThisFace < originalIncidences[2][iFace][3].length; ++iCellThisFace) {
                                            int iCell = originalIncidences[2][iFace][3][iCellThisFace];
                                            if (iPass == 1) flagsOfInterest[nFlagsOfInterest] = new int[] {iVert, iEdge, iFace, iCell};
                                            nFlagsOfInterest++;
                                        }
                                    }
                                    else
                                    {
                                        CHECK(false);
                                    }
                                }
                            }
                        }
                        if (iPass == 0)
                        {
                            flagsOfInterest = new int[nFlagsOfInterest][];
                        }
                        else
                        {
                            CHECK(nFlagsOfInterest == flagsOfInterest.length);
                        }
                    }
                }  // initialized flagsOfInterest
                if (futtVerboseLevel >= 2) System.out.println("              flagsOfInterest = "+com.donhatchsw.util.Arrays.toStringCompact(flagsOfInterest));
                if (futtVerboseLevel >= 1) System.out.println("              flagsOfInterest.length = "+flagsOfInterest.length);

                int[/*nFlags*/][/*nDims*/] flagIndex2reflectedFlagIndex = VecMath.fillmat(flagsOfInterest.length, nDims, -1);
                {
                    // TODO: add ability to set initial capacity
                    com.donhatchsw.util.SpecializedHashMap.IntArrayHashMap<Integer> unmatchedPartialFlag2Index = new com.donhatchsw.util.SpecializedHashMap.IntArrayHashMap<Integer>(flagsOfInterest.length);

                    for (int iFlag = 0; iFlag < flagsOfInterest.length; ++iFlag)
                    {
                        int[] key = new int[4];  // scratch for loop
                        for (int iDim = 0; iDim < nDims; ++iDim)  // not including nDims
                        {
                            VecMath.copyvec(key, flagsOfInterest[iFlag]);
                            key[iDim] = -1;
                            Integer neighbor = unmatchedPartialFlag2Index.get(key);
                            if (neighbor == null)
                            {
                                // put requires a more permanent copy of the scratch key
                                unmatchedPartialFlag2Index.put(VecMath.copyvec(key), iFlag);
                            }
                            else
                            {
                                CHECK(neighbor != iFlag);
                                CHECK(flagIndex2reflectedFlagIndex[iFlag][iDim] == -1);
                                CHECK(flagIndex2reflectedFlagIndex[neighbor][iDim] == -1);
                                flagIndex2reflectedFlagIndex[iFlag][iDim] = neighbor;
                                flagIndex2reflectedFlagIndex[neighbor][iDim] = iFlag;
                            }
                        }
                    }
                    if (futtVerboseLevel >= 2) System.out.println("              flagIndex2reflectedFlagIndex = "+com.donhatchsw.util.Arrays.toStringCompact(flagIndex2reflectedFlagIndex));
                }  // initialized flagIndex2reflectedFlagIndex

                int[] from2toFlag = VecMath.fillvec(flagsOfInterest.length, -1);
                int[] to2fromFlag = VecMath.fillvec(flagsOfInterest.length, -1);
                {
                    int seedFromFlagIndex = -1;
                    int seedToFlagIndex = -1;
                    {
                        // fromSeedFlag will be any flag containing facet iFacet and the given elt, with the appropriate sign.
                        // (actually don't worry about sign, if we get it backwards will correct it at the end).
                        // toSeedFlag will be formed by reflecting the flag in the other two element dimensions,
                        // in some order (which will affect the sign, but we're not worrying about that til the end).
                        for (int i = 0; i < flagsOfInterest.length; ++i)
                        {
                            int[] flag = flagsOfInterest[i];
                            if (flag[eltDim] == iEltGlobal && flag[nDims-1] == iFacet)  // don't worry about sign
                            {
                                seedFromFlagIndex = i;
                                break;
                            }
                        }
                        CHECK(seedFromFlagIndex != -1);
                        seedToFlagIndex = seedFromFlagIndex;  // for starters; we'll reflect it twice
                        CHECK(eltDim==0 || eltDim==1 || eltDim==2);
                        int[] reflectDirections = nDims==3 ? (eltDim==0 ? new int[] {1} :
                                                              eltDim==1 ? new int[] {0} :
                                                              eltDim==2 ?  new int[] {0, 1} : null) :
                                                  nDims==4 ? new int[] {(eltDim+1)%3, (eltDim+2)%3} :   // may be in wrong order which will affect sign, who cares
                                                  null;

                        // First, see what the full local symmetry order actually is here.
                        // I.e. how many double-(or whatever)-reflections to get from seedFromFlagIndex back to itself?
                        int fullSymmetryOrder = -1;
                        {
                            int flagIndex = seedFromFlagIndex;
                            for (int i = 0; ; i++)
                            {
                                if (i != 0 && flagIndex == seedFromFlagIndex) {
                                    fullSymmetryOrder = i;
                                    break;
                                }
                                for (int iReflectDirection = 0; iReflectDirection < reflectDirections.length; ++iReflectDirection)
                                {
                                    int reflectDirection = reflectDirections[iReflectDirection];
                                    flagIndex = flagIndex2reflectedFlagIndex[flagIndex][reflectDirection];
                                }
                            }
                        }
                        if (futtVerboseLevel >= 1) System.out.println("              fullSymmetryOrder = "+fullSymmetryOrder);
                        CHECK(fullSymmetryOrder != -1);
                        for (int iReflectionPair = 0; iReflectionPair < fullSymmetryOrder / symmetryOrder; ++iReflectionPair)
                        {
                            for (int iReflectDirection = 0; iReflectDirection < reflectDirections.length; ++iReflectDirection)
                            {
                                int reflectDirection = reflectDirections[iReflectDirection];
                                seedToFlagIndex = flagIndex2reflectedFlagIndex[seedToFlagIndex][reflectDirection];
                            }
                        }
                    }
                    CHECK(seedFromFlagIndex != -1);
                    CHECK(seedToFlagIndex != -1);
                    if (futtVerboseLevel >= 1) System.out.println("              seed from flag = "+VecMath.toString(flagsOfInterest[seedFromFlagIndex]));
                    if (futtVerboseLevel >= 1) System.out.println("              seed to flag =   "+VecMath.toString(flagsOfInterest[seedToFlagIndex]));

                    int[] searched = VecMath.fillvec(flagsOfInterest.length, -1);
                    int nSearched = 0;
                    from2toFlag[seedFromFlagIndex] = seedToFlagIndex;
                    to2fromFlag[seedToFlagIndex] = seedFromFlagIndex;
                    searched[nSearched++] = seedFromFlagIndex;
                    for (int iSearched = 0; iSearched < nSearched; ++iSearched)  // while growing
                    {
                        if (futtVerboseLevel >= 3) System.out.println("              top of loop");
                        int iFromFlag = searched[iSearched];
                        int iToFlag = from2toFlag[iFromFlag];
                        if (futtVerboseLevel >= 3) System.out.println("                  searching at "+VecMath.toString(flagsOfInterest[iFromFlag])+" -> "+VecMath.toString(flagsOfInterest[iToFlag])+"");
                        CHECK(to2fromFlag[iToFlag] == iFromFlag);
                        for (int iDim = 0; iDim < nDims; ++iDim)  // not including nDims
                        {
                            int jFromFlag = flagIndex2reflectedFlagIndex[iFromFlag][iDim];
                            int jToFlag = flagIndex2reflectedFlagIndex[iToFlag][iDim];
                            if ((jFromFlag==-1) != (jToFlag==-1))
                            {
                                // fail.
                                if (futtVerboseLevel >= 1) System.out.println("            out getFrom2toFacetsForFutt, failed! (first case, (jFromFlag==-1) != (jToFlag==-1))");
                                return null;
                            }
                            if (jFromFlag == -1) continue;
                            if (futtVerboseLevel >= 3) System.out.println("                      iDim="+iDim+": reflected to "+VecMath.toString(flagsOfInterest[jFromFlag])+" -> "+VecMath.toString(flagsOfInterest[jToFlag])+"");
                            if (from2toFlag[jFromFlag] == -1)
                            {
                                if (to2fromFlag[jToFlag] != -1)
                                {
                                    // fail.
                                    if (futtVerboseLevel >= 1) System.out.println("            out getFrom2toFacetsForFutt, failed! (second case, to2fromFlag[jToFlag] != -1)");
                                    return null;
                                }
                                from2toFlag[jFromFlag] = jToFlag;
                                to2fromFlag[jToFlag] = jFromFlag;
                                searched[nSearched++] = jFromFlag;
                            }
                            else
                            {
                                if (from2toFlag[jFromFlag] != jToFlag)
                                {
                                    // fail.
                                    if (futtVerboseLevel >= 1) System.out.println("            out getFrom2toFacetsForFutt, failed! (third case, from2toFlag[jFromFlag] != jToFlag)");
                                    return null;
                                }
                                CHECK(to2fromFlag[jToFlag] == jFromFlag);
                            }
                        }
                    }
                    CHECK(nSearched == searched.length);
                    for (int i = 0; i < flagsOfInterest.length; ++i)
                    {
                        CHECK(from2toFlag[i] != -1);
                        CHECK(to2fromFlag[i] != -1);
                        CHECK(from2toFlag[to2fromFlag[i]] == i);
                        CHECK(to2fromFlag[from2toFlag[i]] == i);
                    }
                }  // initialized from2toFlag and to2fromFlag
                for (int i = 0; i < flagsOfInterest.length; ++i)
                {
                    int fromFacet = flagsOfInterest[i][nDims-1];
                    int toFacet = flagsOfInterest[from2toFlag[i]][nDims-1];
                    CHECK(from2toFacet[fromFacet] == fromFacet  // if it hasn't been moved yet
                       || from2toFacet[fromFacet] == toFacet);  // if it has been moved yet
                    from2toFacet[fromFacet] = toFacet;
                }
                // Make sure we got everything...
                for (int i = 0; i < flagsOfInterest.length; ++i)
                {
                    int fromFacet = flagsOfInterest[i][nDims-1];
                    int toFacet = flagsOfInterest[from2toFlag[i]][nDims-1];
                    CHECK(from2toFacet[fromFacet] == toFacet);
                }
            }

            // Rather than try to fix any of the logic above that was not even
            // trying to choose the correct sign, we choose it now.
            // We came up with a permutation that is rotating facets around the axis;
            // if it's in the wrong direction (i.e. its sign is not the same as dir),
            // just reverse the permutation.
            if (true)
            {
                CHECK(VecMath.det(this.gripUsefulMats[gripIndex]) > 0.);

                int fromFacet = -1;
                for (int i = 0; i < from2toFacet.length; ++i)
                {
                    if (from2toFacet[i] != i)
                    {
                        fromFacet = i;
                        break;
                    }
                }
                if (fromFacet != -1  // something moved
                 && from2toFacet[from2toFacet[fromFacet]] != fromFacet)  // and it's not a 180 degree rotation  (in which case inverting the permutation would be a no-op)
                {
                    double[][] m = {
                        this.gripUsefulMats[gripIndex][0],
                        this.gripUsefulMats[gripIndex][1],
                        CSG.cgOfVerts(originalElements[nDims-1][fromFacet]),
                        CSG.cgOfVerts(originalElements[nDims-1][from2toFacet[fromFacet]]),
                    };
                    if (nDims == 3)
                    {
                        CHECK(m[2].length == 3);
                        CHECK(m[3].length == 3);
                        m[2] = com.donhatchsw.util.Arrays.append(m[2], 0.);
                        m[3] = com.donhatchsw.util.Arrays.append(m[3], 0.);
                    }

                    if (futtVerboseLevel >= 1) System.out.println("              m = "+VecMath.toString(m));
                    double det = VecMath.det(m);
                    if (futtVerboseLevel >= 1) System.out.println("              det = "+det);
                    if (det * dir < 0.)
                    {
                        from2toFacet = VecMath.invertperm(from2toFacet);
                    }
                }
            }

            if (futtVerboseLevel >= 1) System.out.println("            out getFrom2toFacetsForFutt, returning "+VecMath.toString(from2toFacet));
            return from2toFacet;
        }  // getFrom2toFacetsForFutt

        private static SortStuff.Comparator cutInfoCompare = new SortStuff.Comparator() {
            @Override public int compare(Object aObject, Object bObject)
            {
                CutInfo a = (CutInfo)aObject;
                CutInfo b = (CutInfo)bObject;
                if (a.iFacet < b.iFacet) return -1;
                if (a.iFacet > b.iFacet) return 1;
                if (a.iCutThisFacet < b.iCutThisFacet) return -1;
                if (a.iCutThisFacet > b.iCutThisFacet) return 1;
                return 0;
            }
        };  // cutInfoCompare

        // Each sticker can be identified by the facet cuts (i.e. facet number and cut number)
        // that bound it.  Therefore a permutation of the facets
        // induces a permutation of the stickers.
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
            java.util.HashMap<String,Integer> cutInfos2sticker = new java.util.HashMap<String,Integer>();  // CBB: initial capacity should be number of stickers in slicemask
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

        @Override public void
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
                int futtVerboseLevel = 0;  // set to something higher than 0 to debug futt stuff
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

                CSG.Polytope[][] originalElements = originalPolytope.p.getAllElements();
                int[][][][] originalIncidences = originalPolytope.p.getAllIncidences();

                int[] from2toFacet = getFrom2toFacetsForFutt(gripIndex, dir, this.gripSymmetryOrdersFutted[gripIndex]);
                int[] from2toStickerCenters = getFrom2toStickersForFutt(gripIndex, dir, slicemask, from2toFacet);
                double[][] fullInvMatD = getTwistMat(gripIndex, -dir, weWillFutt, 1.);  // -dir instead of dir, 1. instead of frac
                float[][] fullInvMatF = VecMath.doubleToFloat(fullInvMatD);

                // populate outVerts
                {
                    FuzzyPointHashTable<double[]> from2toVertCoords = new FuzzyPointHashTable<double[]>(1e-9, 1e-8, 1./128);
                    {
                        // make a map from coords to symbolic, then symbolic to symbolic is easy, then symbolic to coordso

                        java.util.HashMap<String,double[]> cutInfos2coords = new java.util.HashMap<String,double[]>();  // CBB: initial capacity

                        int[] vertsThisFacet = originalIncidences[nDims-1][iFacet][0];
                        int nCoordsPerCorner = 1; for (int i = 0; i < nDims; ++i) nCoordsPerCorner *= nStickerLayers+1;  // nCoordsPerCorner = intpow(nStickerLayers+1, nDims);
                        CutInfo[][] allCutInfoListsWeAreMaking = new CutInfo[vertsThisFacet.length * nCoordsPerCorner][nDims];

                        double[] desiredOffsets = new double[nDims];  // scratch for loop
                        for (int iVertThisFacet = 0; iVertThisFacet < vertsThisFacet.length; ++iVertThisFacet)
                        {
                            int iVert = vertsThisFacet[iVertThisFacet];
                            int[] facetsThisVert = originalIncidences[0][iVert][nDims-1];
                            CHECK(facetsThisVert.length == nDims);

                            int[] sortedFacetsThisVert = VecMath.copyvec(facetsThisVert);
                            SortStuff.sort(sortedFacetsThisVert);

                            // We are going to want to answer questions of the form:
                            // "what is the intersection of the hyperplanes
                            // at these nDims offsets from the nDims facets"?
                            // I.e. "what is the point whose dot products with the nDims hyperplane normals
                            // equals the nDims hyperplane offsets"?
                            // The facet normals stay constant (throughout this corner region)
                            // but the offsets vary, so compute an inverse matrix so we can answer
                            // the questions quickly.
                            double[][] facetInwardNormalsMat = new double[nDims][];
                            for (int iDim = 0; iDim < nDims; ++iDim)
                            {
                                int jFacet = sortedFacetsThisVert[iDim];
                                facetInwardNormalsMat[iDim] = (double[])com.donhatchsw.util.Arrays.subarray(facetInwardNormals[jFacet], 0, nDims);
                            }
                            if (futtVerboseLevel >= 2) System.out.println("              facetInwardNormalsMat = "+VecMath.toString(facetInwardNormalsMat));
                            double[/*3*/][/*3*/] inverseOfFacetInwardNormalsMat = VecMath.invertmat(facetInwardNormalsMat);
                            if (futtVerboseLevel >= 2) System.out.println("              inverseOfFacetInwardNormalsMat = "+VecMath.toString(inverseOfFacetInwardNormalsMat));

                            int nCoordsThisCorner = 1; for (int i = 0; i < nDims; ++i) nCoordsThisCorner *= nStickerLayers+1;  // nCoordsThisCorner = intpow(nStickerLayers+1, nDims);
                            for (int iCoordThisCorner = 0; iCoordThisCorner < nCoordsThisCorner; ++iCoordThisCorner)
                            {
                                int scratch = iCoordThisCorner;
                                CutInfo[] cutInfos = allCutInfoListsWeAreMaking[iVertThisFacet*nCoordsPerCorner + iCoordThisCorner];
                                for (int iDim = 0; iDim < nDims; ++iDim)
                                {
                                    int jFacet = sortedFacetsThisVert[iDim];
                                    int jCutThisFacet = scratch % (nStickerLayers+1);
                                    cutInfos[iDim] = new CutInfo(jFacet, jCutThisFacet);
                                    desiredOffsets[iDim] = jCutThisFacet==0 ? this.facetOffsetsForFutt[jFacet] : facetCutOffsets[jFacet][jCutThisFacet-1];
                                    scratch /= nStickerLayers+1;
                                }
                                double[] coords = VecMath.mxv(inverseOfFacetInwardNormalsMat, desiredOffsets);
                                //System.out.println("              desiredOffsets = "+VecMath.toString(desiredOffsets)+" -> coords3 = "+VecMath.toString(coords3));
                                String cutInfosString = java.util.Arrays.toString(cutInfos);
                                cutInfos2coords.put(cutInfosString, coords);
                            }
                        }

                        CutInfo[] toCutInfos = new CutInfo[nDims];  // scratch for loop
                        for (int iDim = 0; iDim < nDims; ++iDim)
                            toCutInfos[iDim] = new CutInfo(/*iFacet=*/-1, /*iCutThisFacet=*/-1);
                        boolean isEdgeFlipIn3d = Math.abs(this.gripUsefulMats[gripIndex][1][3]) < 1e-6;
                        for (int i = 0; i < allCutInfoListsWeAreMaking.length; ++i)
                        {
                            CutInfo[] fromCutInfos = allCutInfoListsWeAreMaking[i];
                            for (int iDim = 0; iDim < nDims; ++iDim)
                            {
                                toCutInfos[iDim].iFacet = from2toFacet[fromCutInfos[iDim].iFacet];
                                toCutInfos[iDim].iCutThisFacet = fromCutInfos[iDim].iCutThisFacet;
                            }
                            SortStuff.sort(toCutInfos, 0, toCutInfos.length, cutInfoCompare);

                            String fromCutInfosString = java.util.Arrays.toString(fromCutInfos);
                            String toCutInfosString = java.util.Arrays.toString(toCutInfos);

                            double[] fromCoords = cutInfos2coords.get(fromCutInfosString);
                            double[] toCoords = cutInfos2coords.get(toCutInfosString);
                            CHECK(fromCoords != null);
                            CHECK(toCoords != null);
                            if (nDims == 4)
                            {
                                from2toVertCoords.put(fromCoords, toCoords);
                            }
                            else if (nDims == 3)
                            {
                                double pad = Math.abs(vertsDForFutt[0][3]); // hacky way to retrieve what was used for thickness in w direction
                                if (isEdgeFlipIn3d)
                                {
                                    from2toVertCoords.put(com.donhatchsw.util.Arrays.append(fromCoords, pad),
                                                          com.donhatchsw.util.Arrays.append(toCoords, -pad));
                                    from2toVertCoords.put(com.donhatchsw.util.Arrays.append(fromCoords, -pad),
                                                          com.donhatchsw.util.Arrays.append(toCoords, pad));
                                }
                                else
                                {
                                    from2toVertCoords.put(com.donhatchsw.util.Arrays.append(fromCoords, pad),
                                                          com.donhatchsw.util.Arrays.append(toCoords, pad));
                                    from2toVertCoords.put(com.donhatchsw.util.Arrays.append(fromCoords, -pad),
                                                          com.donhatchsw.util.Arrays.append(toCoords, -pad));
                                }
                            }
                        }
                    }  // initialized from2toVertCoords

                    float[] toF = new float[4];  // scratch for loop
                    float[] toFinFromSpace = new float[4];  // scratch for loop
                    float[] morphedFromF = new float[4];  // scratch for loop
                    for (int iVert = 0; iVert < vertsDForFutt.length; ++iVert)
                    {
                        if (whichVertsGetMoved[iVert])
                        {
                            double[] from = vertsDForFutt[iVert];
                            //double[] to = (double[])finalMorphDestinations.get(from);
                            double[] to = from2toVertCoords.get(from);
                            if (futtVerboseLevel >= 3) System.out.println("          found vert from="+VecMath.toString(from)+" -> to="+VecMath.toString(to));
                            if (to == null)
                            {
                                throw new AssertionError("Didn't find vertex from="+VecMath.toString(from));
                            }
                            //CHECK(to != null);

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
                }  // populated outVerts

                // Populate outStickerCenters, outStickerShrinkToPointsOnFaceBoundaries, and outPerStickerFaceCenters.
                {
                    // I think the only entries we need to change in outPerStickerFaceCenters are the ones that contribute to the stickers that are moving.
                    // The others wouldn't necessarily make any sense anyway.
                    // However, a sticker can be moving *without* its facet moving to a different facet, so from2toFacet[fromFacet]==fromFacet is *not* a valid criterion for skipping.
                    int nFacets = originalElements[nDims-1].length;
                    float[][] rotatedMorphedFaceCenters = new float[nFacets][/*nDisplayDims=*/4];
                    {
                        for (int fromFacet = 0; fromFacet < from2toFacet.length; ++fromFacet)
                        {
                            int toFacet = from2toFacet[fromFacet];
                            //if (toFacet == fromFacet) continue; // NOT a valid criterion, see above.

                            float[] fromF = facetCentersF[fromFacet];
                            float[] toF = facetCentersF[toFacet];

                            float[] toFinFromSpace = VecMath.vxm(toF, fullInvMatF);
                            float[] morphedFrom = VecMath.lerp(fromF, toFinFromSpace, frac);

                            VecMath.vxm(rotatedMorphedFaceCenters[fromFacet], morphedFrom, matF);
                        }
                        VecMath.vxm(rotatedMorphedFaceCenters[iFacet], facetCentersF[iFacet], matF);
                    }

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
                }  // populated outStickerCenters, outStickerShrinkToPointsOnFaceBoundaries, and outPerStickerFaceCenters.

                if (futtVerboseLevel >= 1) System.out.println("  WHOLE LOTTA FUTTIN WENT ON");
                if (futtVerboseLevel >= 1) System.out.println("  ==========================");
            }
            if (verboseLevel >= 1) System.out.println("out computeVertsAndShrinkToPointsPartiallyTwisted(gripIndex="+gripIndex+", dir="+dir+", slicemask="+slicemask+", futtIfPossible="+futtIfPossible+", frac="+frac+")");
        } // computeVertsAndShrinkToPointsPartiallyTwisted



        @Override public int[/*nStickers*/] getSticker2Face()
        {
            // Make sure caller didn't mess it up from last time!!
            if (!VecMath.equals(sticker2face, sticker2faceShadow))
                throw new RuntimeException("PolytopePuzzleDescription.getSticker2Facet: caller modified previously returned sticker2face! BAD! BAD! BAD!");
            return sticker2face;
        }
        @Override public int[/*nStickers*/] getSticker2Cubie()
        {
            return sticker2cubie;
        }
        @Override public int[/*nFacets*/] getGrip2Face()
        {
            return grip2face;
        }
        @Override public int[/*nStickers*/][/*nPolygonsThisSticker*/] getStickerPoly2Grip()
        {
            return stickerPoly2Grip;
        }
        @Override public int[/*nFacets*/] getFace2OppositeFace()
        {
            return facet2OppositeFacet;
        }
        @Override public int[][/*2*/][/*2*/]
            getAdjacentStickerPairs()
        {
            return adjacentStickerPairs;
        }
        @Override public float[/*nFacets*/][/*nDisplayDims*/]
            getFaceCentersAtRest()
        {
            return facetCentersF;
        }
        @Override public int[/*nStickers*/] applyTwistToState(int state[/*nStickers*/],
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
                int[] from2toFacet = getFrom2toFacetsForFutt(gripIndex, dir, this.gripSymmetryOrdersFutted[gripIndex]);
                int[] from2toStickers = getFrom2toStickersForFutt(gripIndex, dir, slicemask, from2toFacet);
                for (int iSticker = 0; iSticker < state.length; ++iSticker)
                {
                    newState[from2toStickers[iSticker]] = state[iSticker];
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
                        Integer whereIstickerGoes = stickerCentersHashTable.get(scratchVert);
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
        final boolean[] cancelledHolder = {false};
	ProgressCallbacks progressCallbacks = new ProgressCallbacks() {
	    private long initTimeNanos = 0;
	    @Override public boolean subtaskInit(String string, int max) {
		System.out.print(string+" ("+max+") ...");
		initTimeNanos = System.nanoTime();
		return true;  // keep going
	    }
	    @Override public boolean subtaskInit(String string) {
		System.out.println(string+"...");
		initTimeNanos = System.nanoTime();
		return true;  // keep going
	    }
	    @Override public boolean updateProgress(int progress) {
		System.out.print("..."+progress);
		System.out.flush();
		// Silly and disruptive exercise of the cancellation feature
		if (progress == 1000) {
		    System.out.println();
		    System.out.print("This is taking a while.  Want to keep going? (y/n)[Y] ");
		    System.out.flush();
		    try {
			char c = (char)System.in.read();
			if (c == 'n') {
			    cancelledHolder[0] = true;
			    return false;  // cancel
			}
		    } catch (java.io.IOException e) {
			System.out.println("Caught: "+e);
			cancelledHolder[0] = true;
			return false;  // cancel
		    }
		}
		return true;  // keep going
	    }
	    @Override public boolean subtaskDone() {
		long doneTimeNanos = System.nanoTime();
		System.out.printf("  done (%.4gs).\n", (doneTimeNanos-initTimeNanos)/1e9);
		return true;  // keep going (done with subtask)
	    }
	};

        String puzzleDescriptionString = args[0];
        GenericPuzzleDescription descr = new PolytopePuzzleDescription(puzzleDescriptionString,
                                                                       // CBB: both?  weird output
                                                                       progressWriter,
                                                                       progressCallbacks);
        if (cancelledHolder[0])
        {
            System.out.println("Cancelled!");
        }
        else
        {
            System.out.println("short description (prescription) = "+descr.toString());
            System.out.println("verbose-ish description = "+((PolytopePuzzleDescription)descr).toString(false));
            //System.out.println("verbose description = "+((PolytopePuzzleDescription)descr).toString(true));
        }

        System.out.println("out main");
    } // main

} // class PolytopePuzzleDescription
