/**
A very clever back-to-front painter's sorting of stickers
=========================================================

We start with a simple idea: make a dag, whose nodes are the stickers,
with an edge sticker0->sticker1 if the two stickers (non-shrunk)
are physically adjacent and sticker0 is "behind" sticker1
in the current picture; i.e. if the polygon
they share is frontfacing on sticker0 and backfacing on sticker1.
Topologically sort the dag, producing a good back-to-front rendering order.
This is simple and robust, as long as no twist is in progress.
If a twist *is* in progress, we have to think harder.

Let's say the currently in-progress twist
involves parallel slices 0,1,2,3.
Label slice 0's stickers 0a,0b,0c,... and slice 1's stickers 1a,1b,... etc.
In the most general scenario, each of the slices is being twisted
independently, so imagine each slice having a slightly different twist,
with respect to the following picture.

              +---+ +---+ +-------------+ +---+
          +    \ 0g\ \ 1a\ \     2a    / / 3a/    +
          |\    +---+ +---+ +---------+ +---+    /|
          |0+    +---+ +---+ +-------+ +---+    +3|
          +a| +   \ 0h\ \ 1b\ \  2b / / 3b/   + |k+
          +\| |\   +---+ +---+ +---+ +---+   /| |/+
          |\+ |0+                           +3| +/|
          | + +d|  +---+ +---+ +---+ +---+  |h+ + |
          | | +\|  | 0i| | 1c| | 2c| | 3c|  |/+ | |
          | | |\+  +---+ +---+ +---+ +---+  +/| | |
          | | | +  +---+ +---+ +---+ +---+  + | | |
          |0| |0|  | 0j| | 1d| | 2d| | 3d|  |3| |3|
          |b| |e+  +---+ +---+ +---+ +---+  +i| |l|
          | | |/+  +---+ +---+ +---+ +---+  +\| | |
          | | +/|  | 0k| | 1e| | 2e| | 3e|  |\+ | |
          | + +0|  +---+ +---+ +---+ +---+  |3+ + |
          |/+ |f+                           +j| +\|
          +/| |/   +---+ +---+ +---+ +---+   \| |\+
          +0| +   / 0l/ / 1f/ /  2f \ \ 3f\   + |3+
          |c+    +---+ +---+ +-------+ +---+    +m|
          |/    +---+ +---+ +---------+ +---+    \|
          +    / 0m/ / 1g/ /     2g    \ \ 3g\    +
              +---+ +---+ +-------------+ +---+


If we consider unshrunk faces and stickers, it looks like this:

               +---+---+-----------+---+
               |\ 0g\ 1a\    2a   / 3a/|
               |0+---+---+-------+---+3|
               +a|\ 0h\ 1b\  2b / 3b/|k+
               |\|0+---+---+---+---+3|/|
               | +d| 0i| 1c| 2c| 3c|h+ |
               | |\|   |   |   |   |/| |
               | | +---+---+---+---+ | |
               |0|0| 0j| 1d| 2d| 3d|3|3|
               |b|e|   |   |   |   |i|l|
               | | +---+---+---+---+ | |
               | |/| 0k| 1e| 2e| 3e|\| |
               | +0|   |   |   |   |3+ |
               |/|f+---+---+---+---+j|\|
               +0|/ 0l/ 1f/  2f \ 3f\|3+
               |c+---+--+--------+---+m|
               |/0m/ 1g/     2g   \ 3g\|
               +--+---+------------+---+

We arrange the slices and stickers into a tree,
whose leaves are the stickers,
and whose internal nodes are the slices.
The root node is the slice in which the *4d* eye resides
(although in this picture we are down 1 dimension, so it's the slice
in which the *3d* eye resides, in this case slice 2).
The children of any non-leaf node (slice) are:
  - the stickers in that slice
  - the adjacent slice(s) one step further from the eye.

So the tree structure in this case is as follows:
                          +-------+
                          |Slice 2|
                          +-------+
                         / |  |  | \
                +-------+  |  |  |  +-------+
                |Slice 1|  2a 2b... |Slice 3|
                +-------+           +-------+
               / |  |  |             |  |  |
      +-------+  |  |  |             |  |  |
      |Slice 0|  1a 1b...            3a 3b...
      +-------+
       |  |  |
       0a 0b...

Note that (imagining nothing is twisted, for the moment),
the choice of root and tree structure
guarantees that each subtree is a convex region of the picture (!),
so the tree is a recursive partitioning of space into convex regions and
sub-regions.

The goal is to produce a reasonable "back-to-front" ordering of all the
stickers, with respect to the 3d eye in the real puzzle
(or, in this picture, with respect to a 2d eye; imagine the 2d eye
is anywhere around the perimeter of the above picture).

The rendering order is determined by a recursive tree traversal,
starting at the root:
    RENDER(node) {
        if node is a leaf (sticker):
            render the sticker
        else:
            topologically sort node's children, back to front (see below)
            for each child, back to front:
                RENDER(child)
    }

The "topologically sort node's children back to front" step is done as
follows: a dag is created on node's children, where edge child0->child1
is in the dag iff child0 is physically adjacent to child1
and child0 is *behind* child1 (i.e. the polygon
shared by child0 and child1, in the non-shrunk projected puzzle,
is frontfacing on child0, and backfacing on child1.
In the case that children are on two different slices that have been
twisted differently, it may be that both are frontfacing or both backfacing;
in this case, we base the decision on the polygon's current orientation on the
more rootmost of the two children.

So, in more detail, there are 3 cases:
- child0 is a sticker, child1 is a sticker (both in the same slice).
  E.g. 2a vs. 2b.
  In this case both stickers have been twisted by the same twist matrix; we use
  that orientation of their shared polygon to determine which is in front.
- child0 is a sticker, child1 is a slice.  E.g. 2b vs. Slice 3.
  In this case they may be twisted by two different twist matrices.
  We use the current orientation of the polygon on child0 (the sticker) rather
  than looking at anything currently on child1 (the differently twisted slice).
- child1 is a slice, child0 is a sticker
  This is just the previous case, with the two children reversed.
- (The case where child0 and child1 both slices can't happen:
  the only node that can have two children is the root, and in that case
  the two children are not physically adjacent.)

Further detail: we actually further hierarchicalize by grouping
the sticker children of each slice into convex groups called "slice faces";
each slice face is the intersection of the slice with a face.
This is for two reasons:
  1. better-isolated failure (cycle) detection and fallback.

  2. In the case of faceshrink < 1, just topsorting by sticker adjacency isn't
     enough, but apparently that can be fixed in (almost?) all cases,
     by further grouping stickers by face.
     Canonical example:
              +---+
             /a\b/c\  Face 0 (shrunk) with 3 stickers a,b,c
            +---+---+

            +---+---+
             \d/e\f/  Face 1 (shrunk) with 3 stickers d,e,f
              +---+

              +
             Eye

     The naive dag in this case is on only the stickers
     (with no cognizance of face grouping),
     and consists of the following edges:
            b->a->d->e
            b->c->f->e

     Note that this naive dag does not capture the fact that (part of) c is
     actually behind (part of) d, with respect to the current Eye,
     and therefore c must be rendered before d!

     In more detail, naively topsorting that dag
     can produce any of the following six orderings:
            b a d c f e  BAD!
            b a c d f e
            b a c f d e
            b c a d f e
            b c a f d e
            b c f a d e

     Now consider what happens if we further hierarchicalize first:
     that is, require all of Face 0 to come out consecutively,
     and all of Face 1 to come out consecutively.
     That rules out two of the 6 possible orderings, leaving
     the following four:
            b a c  d f e
            b a c  f d e
            b c a  d f e
            b c a  f d e
     All of which are good, from the given Eye point.

A trace of the full hierarchical traversal order
of the above example, back to front, might look like this:
  Slice(2) {
      SliceFace(2, back face)  {2a 2b}
      Slice(3) {
        SliceFace(3, back face)  {3a 3b}
        SliceFace(3, right face)  {3k 3l 3h 3i 3m 3j}
        SliceFace(3, center face)  {3c 3d 3e}
        SliceFace(3, front face)  {3f 3g}
      }
      SliceFace(2,center face) {2c 2d 2e}
      Slice(1) {
        SliceFace(1,back face)  {1a 1b}
        Slice(0) {
            SliceFace(0,back face)  {0g 0h}
            SliceFace(0,left face)  {0a 0d 0b 0e 0c 0f}
            SliceFace(0,center face) {0i 0j 0k}
            SliceFace(0,front face) {0l 0m}
        }
        SliceFace(1,center face)  {1c 1d 1e}
        SliceFace(1,front face)  {1f 1g}
      }
      SliceFace(2,front) {2f 2g}  {2f 2g}
  }

*/

package com.donhatchsw.mc4d;

import com.donhatchsw.util.VecMath;

public class VeryCleverPaintersSortingOfStickers
{
    // XXX figure out where to put this, if anywhere.  it will probably go away
    private static java.util.Random randomnessGenerator = new java.util.Random();

    private static void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }
    private static String $(Object obj) { return com.donhatchsw.util.Arrays.toStringCompact(obj); }  // convenience
    private static String $(Object obj,int i0, int n) { return $(com.donhatchsw.util.Arrays.subarray(obj,i0,n)); }  // convenience
    private static String repeat(String s, int n) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < n; ++i) sb.append(s); return sb.toString(); }

    // Function return value is number of stickers to draw
    public static int sortStickersBackToFront(
            final int nStickers, // can be less than actual number, for debugging
            final int adjacentStickerPairs[][/*2*/][/*2: iSticker,iPolyThisSticker*/],
            final boolean stickerVisibilities[/*>=nStickers*/],
            final boolean unshrunkStickerPolyIsStrictlyBackfacing[/*>=nStickers*/][/*nPolysThisSticker*/],  // used for comparing stickers on two different faces
            final boolean partiallyShrunkStickerPolyIsStrictlyBackfacing[/*>=nStickers*/][/*nPolysThisSticker*/],  // used for comparing neighboring stickers within a single face
            float eye[/*nDisplayDims*/],
            float cutNormal[/*nDisplayDims*/],
            float cutOffsets[/*nCuts*/], // in increasing order
            final int sticker2Slice[/*>=nStickers*/],
            final int sticker2face[/*>=nStickers*/],
            int returnStickerSortOrder[/*>=nStickers*/],
            final int returnPartialOrderInfoOptionalForDebugging[/*1*/][][/*2*/][/*3*/],  // null if caller doesn't care, otherwise it's a singleton array that gets filled in with the edges in the partial order.  each edge is of the form {{fromStickerIndex,fromStickerIndex0,fromStickerIndex1},{toStickerIndex,toStickerIndex0,toStickerIndex1}}.  These indices are into the returnStickerSortOrder array: fromStickerIndex,toStickerIndex refer to the two stickers that cause the constraint, and the constraint is that the range of output stickers at [fromStickerIndex0,fromStickerIndex1) come before those at [toStickerIndex0,toStickerIndex1).   fromStickerIndex0<=fromStickerIndex<fromStickerIndex1 and toStickerIndex0<=toStickerIndex<toStickerIndex1.
            final String returnSummaryOptionalForDebugging[/*1*/],  // null if caller doesn't care

            final float stickerCentersZ[/*>=nStickers*/],  // for fallback to z-sorting on parts where cycles were found
            float polyCenters3d[/*>=nStickers*/][/*nPolysThisSticker*/][/*3*/],  // for determining if things are so warped that we need to ignore a constraint, e.g. if all faces are visible at the same time
            float polyNormals3d[/*>=nStickers*/][/*nPolysThisSticker*/][/*3*/])  // for determining if things are so warped that we need to ignore a constraint, e.g. if all faces are visible at the same time
    {
        final int localVerboseLevel = 0;  // hard-code to something higher to debug. 0: nothing, 1: in/out and constant time, and nice dump at end 2: more verbose on cycles, 3: fine details
        if (localVerboseLevel >= 1) System.out.println("    in sortStickersBackToFront");
        if (localVerboseLevel >= 1) System.out.println("      nStickers = "+$(nStickers));
        if (localVerboseLevel >= 1) System.out.println("      cutNormal = "+$(cutNormal));
        if (localVerboseLevel >= 1) System.out.println("      cutOffsets = "+$(cutOffsets));
        if (localVerboseLevel >= 1) System.out.println("      eye = "+$(eye)+"  (distance from origin = "+VecMath.norm(eye));
        if (localVerboseLevel >= 3) {
          System.out.println("      sticker2Slice = "+$(sticker2Slice));
          System.out.println("      "+stickerVisibilities.length+" stickerVisibilities = "+$(stickerVisibilities));
          System.out.println("      "+adjacentStickerPairs.length+" adjacentStickerPairs = "+$(adjacentStickerPairs));
        }

        if (returnPartialOrderInfoOptionalForDebugging != null) {
            returnPartialOrderInfoOptionalForDebugging[0] = new int[adjacentStickerPairs.length][][];
        }
        final int[] numZsortsDoneHolder = {0};

        final int[] returnPartialOrderInfoOptionalForDebuggingSizeHolder = (returnPartialOrderInfoOptionalForDebugging != null) ? new int[] {0} : null;

        final int nAllSlices = cutOffsets.length + 1;
        final int nCompressedSlices = nAllSlices; // XXX should combine adjacent slices that are moving together... but maybe it doesn't hurt to just pretend all the slices are twisting separately, it keeps things simple?  Not sure.

        // Sanity check: for each pair of adjacent stickers,
        // they are either in the same slice or in adjacent slices.
        // (This used to fail since there were two cuts too close together;
        // in that case the caller needs to merge those two cuts
        // and kill the zero-thickness slice.)
        for (int iPair = 0; iPair < adjacentStickerPairs.length; ++iPair) {
            int iSlice = sticker2Slice[adjacentStickerPairs[iPair][0][0]];
            int jSlice = sticker2Slice[adjacentStickerPairs[iPair][1][0]];
            CHECK(jSlice == iSlice-1 || jSlice == iSlice || jSlice == iSlice+1);
        }

        //
        // Figure out which compressed slice the (4d) eye is in.
        // That slice will be the root of a simple tree,
        // with two branches of groups:
        //                whichSliceEyeIsIn
        //                 /             \
        //        nextShallowerSlice   nextDeeperSlice
        //            |                  ...
        //   nextNextShallowerSlice 
        //           ...
        //
        int eyeSlice; // which compressed slice eye is in
        {
            float eyeOffset = VecMath.dot(eye, cutNormal);
            eyeSlice = 0;
            while (eyeSlice < cutOffsets.length
                && eyeOffset > cutOffsets[eyeSlice])
                eyeSlice++;
        }
        if (localVerboseLevel >= 1) System.out.println("      eyeSlice = "+eyeSlice+"/"+nCompressedSlices);

        int maxPartialOrderSize = adjacentStickerPairs.length;  // CBB: could think of tighter bound on this as well
        final int[][] partialOrder = new int[maxPartialOrderSize][2];

        int maxSortOrderSizeNeeded = nStickers;  // CBB: tighter bound would be max of stickers-per-face and nFaces+2, or something weird like that
        final com.donhatchsw.util.TopSorter topsorter = new com.donhatchsw.util.TopSorter(/*maxN=*/maxSortOrderSizeNeeded, maxPartialOrderSize); // XXX allocation
        final int sortOrder[] = new int[maxSortOrderSizeNeeded]; // XXX allocation
        final int componentStarts[] = new int[maxSortOrderSizeNeeded+1]; // one extra for end of last one.  XXX allocation


        final int[] visibleStickersSortedBySliceAndFace = new int[nStickers];
        int nVisibleStickersTotal = 0;
        for (int iSticker = 0; iSticker < nStickers; ++iSticker) {
            if (stickerVisibilities[iSticker]) {
                visibleStickersSortedBySliceAndFace[nVisibleStickersTotal++] = iSticker;
            }
        }
        if (localVerboseLevel >= 1) System.out.println("      nVisibleStickersTotal = "+$(nVisibleStickersTotal));

        com.donhatchsw.util.SortStuff.sort(visibleStickersSortedBySliceAndFace, 0, nVisibleStickersTotal,
            new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                @Override public int compare(int i, int j)
                {
                    return sticker2Slice[i] != sticker2Slice[j] ? sticker2Slice[i]-sticker2Slice[j] :
                           sticker2face[i] != sticker2face[j] ? sticker2face[i]-sticker2face[j] :
                           i-j;
                }
            });
        if (localVerboseLevel >= 3) System.out.println("      visibleStickersSortedBySliceAndFace = "+$(visibleStickersSortedBySliceAndFace));

        /*
        Outline of algorithm:
          traverse(slice)
              children of slice are:
                - slicefaces
                - other slices
              topsort the children of this slice
              traverse them in order
          traverse(sliceface)
              topsort the stickers within this sliceface, by immediate adjacencies
              emit them in order
        */

        // figure out nFaces (or rather an upper bound on what we're interested in)
        final int nFaces = sticker2face.length==0 ? 0 : VecMath.max(sticker2face)+1;
        final int[] face2localIndex = VecMath.fillvec(nFaces, -1);
        final int[] slice2localIndex = VecMath.fillvec(nCompressedSlices, -1);
        final int[] sticker2localIndex = VecMath.fillvec(nStickers, -1);

        abstract class Node {
            public abstract int traverse(int answer[], int answerSizeSoFar, StringBuilder tracebuffer, int recursionLevel);
            protected abstract String shortLabel();
            public abstract int totalSize();  // for debugging

            public float getAverageZ() {
                if (!this.cachedAverageZstuffValid) computeCachedAverageZstuff();
                CHECK(this.cachedAverageZstuffValid);
                return cachedAverageZ;
            }

            // Subclasses must provide the following.
            // However, these functions should *not* call each other directly
            // (really these should be private! but private abstract is not allowed for some reason);
            // rather, if they need to recurse, they should call the getAverageZ...()
            // functions, which cache results for efficiency.
            protected abstract double computeAverageZnumerator();
            protected abstract double computeAverageZdenominator();

            // Private vars supporting the caching layer.
            private boolean cachedAverageZstuffValid = false;
            private double cachedAverageZnumerator;
            private double cachedAverageZdenominator;
            private float cachedAverageZ;
            private void computeCachedAverageZstuff() {
                this.cachedAverageZnumerator = computeAverageZnumerator();
                this.cachedAverageZdenominator = computeAverageZdenominator();
                this.cachedAverageZ = (float)(this.cachedAverageZnumerator / this.cachedAverageZdenominator);
                this.cachedAverageZstuffValid = true;
            }
            // Caching layer.  Not public; for use by subclasses'
            // computeAverageZ...() functions to call recursively.
            protected double getAverageZnumerator() {
                if (!this.cachedAverageZstuffValid) computeCachedAverageZstuff();
                CHECK(this.cachedAverageZstuffValid);
                return this.cachedAverageZnumerator;
            }
            protected double getAverageZdenominator() {
                if (!this.cachedAverageZstuffValid) computeCachedAverageZstuff();
                CHECK(this.cachedAverageZstuffValid);
                return cachedAverageZdenominator;
            }

            // CBB: Not sure this is the best data structure for this, but...
            public java.util.ArrayList<Integer> relevantAdjacentStickerPairs = new java.util.ArrayList<Integer>();
        }  // abstract class Node
        class SliceFaceNode extends Node {
            public SliceFaceNode(int iSlice, int iFace) {
                this.iSlice = iSlice;
                this.iFace = iFace;
            }
            public int iSlice;
            public int iFace;

            // Logically, we have an array of visible stickers.
            // To avoid memory allocations, we use a view into visibleStickersSortedBySliceAndFace instead.
            public IntArrayView visibleStickers = new IntArrayView();

            @Override protected double computeAverageZnumerator()
            {
                double answer = 0.;
                for (int i = 0; i < this.visibleStickers.size(); ++i) {
                    answer += stickerCentersZ[this.visibleStickers.get(i)];  // this is what it would look like if visibleStickers was an "array view" object. (java.util.ArrayList.subList() provides such a thing, but I want something reusable instead
                }
                return answer;
            }
            @Override protected double computeAverageZdenominator()
            {
                return (double)this.visibleStickers.size();
            }

            @Override public String toString() {
                return "SliceFaceNode("+iSlice+","+iFace+",visibleStickers="+visibleStickers.toString()+")";
            }
            @Override public String shortLabel() {
                return "SF("+iSlice+","+iFace+")";
            }
            @Override public int totalSize() { return visibleStickers.size(); }
            @Override public int traverse(int answer[], int answerSizeSoFar, StringBuilder tracebuffer, int recursionLevel) {
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"            in SliceFaceNode(iSlice="+iSlice+" iFace="+iFace+").traverse, answerSizeSoFar="+answerSizeSoFar);

                for (int i = 0; i < this.visibleStickers.size(); ++i)
                {
                    CHECK(sticker2localIndex[visibleStickers.get(i)] == -1);
                    sticker2localIndex[visibleStickers.get(i)] = i;
                }
                // topsort the visibleStickers within this sliceface, by immediate adjacencies,
                // and emit them in order.
                int partialOrderSize = 0;
                int nRelevant = this.relevantAdjacentStickerPairs.size();
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"              "+nRelevant+" relevant pairs");
                for (int iRelevant = 0; iRelevant < nRelevant; ++iRelevant) {
                    int iPair = this.relevantAdjacentStickerPairs.get(iRelevant).intValue();

                    int pair[][] = adjacentStickerPairs[iPair];
                    int iSticker =         pair[0][0];
                    int iPolyThisSticker = pair[0][1];
                    int jSticker =         pair[1][0];
                    int jPolyThisSticker = pair[1][1];

                    // double-check that relevance was decided correctly...
                    CHECK(sticker2Slice[iSticker] == this.iSlice);
                    CHECK(sticker2Slice[jSticker] == this.iSlice);
                    CHECK(sticker2face[iSticker] == this.iFace);
                    CHECK(sticker2face[jSticker] == this.iFace);
                    CHECK(stickerVisibilities[iSticker]);
                    CHECK(stickerVisibilities[jSticker]);

                    boolean iStickerHasPolyBackfacing = partiallyShrunkStickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                    boolean jStickerHasPolyBackfacing = partiallyShrunkStickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                    if (iStickerHasPolyBackfacing) {
                        //add "jSticker < iSticker"
                        partialOrder[partialOrderSize][0] = sticker2localIndex[jSticker];
                        partialOrder[partialOrderSize][1] = sticker2localIndex[iSticker];
                        partialOrderSize++;
                        if (returnPartialOrderInfoOptionalForDebugging != null) {

                            // XXX debugging
                            if (returnPartialOrderInfoOptionalForDebuggingSizeHolder[0] == returnPartialOrderInfoOptionalForDebugging[0].length) {
                                System.out.println("XXX UH OH! trying to add new item "+$(new int[][] { {jSticker, jSticker, jSticker+1}, {iSticker, iSticker, iSticker+1}, }));
                                System.out.println("XXX partial order info was "+$(returnPartialOrderInfoOptionalForDebugging[0]));

                            }

                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {jSticker, jSticker, jSticker+1},
                                {iSticker, iSticker, iSticker+1},
                            };
                        }
                    } else if (jStickerHasPolyBackfacing) {
                        //add "iSticker < jSticker"
                        partialOrder[partialOrderSize][0] = sticker2localIndex[iSticker];
                        partialOrder[partialOrderSize][1] = sticker2localIndex[jSticker];
                        partialOrderSize++;
                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {iSticker, iSticker, iSticker+1},
                                {jSticker, jSticker, jSticker+1},
                            };
                        }
                    } else {
                        // this really shouldn't happen, I don't think
                    }
                }

                // restore -1's
                for (int i = 0; i < this.visibleStickers.size(); ++i)
                {
                    CHECK(sticker2localIndex[visibleStickers.get(i)] == i);
                    sticker2localIndex[visibleStickers.get(i)] = -1;
                }

                if (true) {
                    // There will be up to 4 dups of each constraint, here, in 2x case
                    // (in which case each sticker poly is carved up into 4 pieces); de-dup.
                    // CBB: could leave them in, or not generate them to begin with? not sure. clearest for debugging to de-dup, anyway.
                    partialOrderSize = sortAndCompressPartialOrder(partialOrderSize, partialOrder);
                }

                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"              topsort "+this.visibleStickers.size()+" visible stickers with partial order "+$(com.donhatchsw.util.Arrays.subarray(partialOrder, 0, partialOrderSize)));
                int nComponents = topsorter.topsort(this.visibleStickers.size(), sortOrder,
                                                    partialOrderSize, partialOrder,
                                                    componentStarts);
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"              topsort returned "+nComponents+"/"+this.visibleStickers.size()+" components: "+$(com.donhatchsw.util.Arrays.subarray(sortOrder, 0, this.visibleStickers.size())));
                if (nComponents < this.visibleStickers.size()) {
                    if (localVerboseLevel >= 1 || returnPartialOrderInfoOptionalForDebugging!=null) System.out.println("      LOCAL TOPSORT OF "+this.visibleStickers.size()+" STICKERS WITHIN FACE "+iFace+"/"+nFaces+" WITHIN SLICE "+iSlice+"/"+nCompressedSlices+" FAILED - Z-SORTING ONE OR MORE CYCLE OF STICKERS");
                    for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                    {
                        int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                        if (componentSize >= 2)
                        {
                            if (localVerboseLevel >= 1) System.out.println("    sorting a strongly connected component (i.e. snakepit of cycles) of length "+componentSize+"");
                            if (localVerboseLevel >= 2) System.out.println("              before: "+$(com.donhatchsw.util.Arrays.subarray(sortOrder, componentStarts[iComponent], componentSize)));
                            if (true)
                            {
                                com.donhatchsw.util.SortStuff.sort(sortOrder, componentStarts[iComponent], componentSize,
                                    new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                                        @Override public int compare(int i, int j)
                                        {
                                            int iSticker = SliceFaceNode.this.visibleStickers.get(i);
                                            int jSticker = SliceFaceNode.this.visibleStickers.get(j);
                                            CHECK(stickerVisibilities[iSticker]);
                                            CHECK(stickerVisibilities[jSticker]);
                                            float iZ = stickerCentersZ[iSticker];
                                            float jZ = stickerCentersZ[jSticker];
                                            // sort from increasing z to decreasing!
                                            // that is because the z's got negated just before the projection!
                                            return iZ > jZ ? -1 :
                                                   iZ < jZ ? 1 : 0;
                                        }
                                    }
                                );
                            }
                            numZsortsDoneHolder[0]++;
                            if (localVerboseLevel >= 2) System.out.println("              after: "+$(com.donhatchsw.util.Arrays.subarray(sortOrder, componentStarts[iComponent], componentSize)));
                        }
                    }
                }
                for (int i = 0; i < this.visibleStickers.size(); ++i) {
                    int iSticker = visibleStickers.get(sortOrder[i]);
                    answer[answerSizeSoFar++] = iSticker;
                }

                if (tracebuffer != null) {
                    tracebuffer.append(repeat("    ",recursionLevel)+"SliceFace("+this.iSlice+","+this.iFace+")");

                    // Note that all this fancy printing is much ado about nothing,
                    // for SliceFaces, since the topsort never fails at this level
                    // (i.e. a cycle within a face), I don't think.

                    if (false) {
                        // The nodes in final order.
                        // E.g. "{16 18 17 19 20 21 22 23}"
                        tracebuffer.append(" {");
                        for (int i = 0; i < this.visibleStickers.size(); ++i)
                        {
                            if (i > 0) tracebuffer.append(" ");
                            int iSticker = answer[answerSizeSoFar-this.visibleStickers.size()+i];
                            tracebuffer.append(iSticker);
                        }
                        tracebuffer.append("}");
                    }

                    if (true) {
                        // The sticker inds in final order, with strongly connected components highlighted.
                        // E.g. "{(16,18,17,19) 20 21 (22,23)}"
                        tracebuffer.append(" {");
                        for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                        {
                            if (iComponent > 0) tracebuffer.append(" ");
                            int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                            if (componentSize > 1) tracebuffer.append("(");
                            for (int i = componentStarts[iComponent]; i < componentStarts[iComponent+1]; ++i) {
                                if (i > componentStarts[iComponent]) tracebuffer.append(",");
                                int iSticker = answer[answerSizeSoFar-this.visibleStickers.size()+i];
                                CHECK(iSticker == visibleStickers.get(sortOrder[i]));
                                tracebuffer.append(iSticker);
                            }
                            if (componentSize > 1) tracebuffer.append(")");
                        }
                        tracebuffer.append("}");
                    }

                    if (true) {
                        // Experimental: the sticker inds in final order, with predecessors and succesors of each
                        // E.g. "{[19]->16->[17 18 20] [16]->18->[19 22] [16]->17->[19 21] [17 18]->19->[16 23] [16]->20->[21 22] [17 20]->21->[23] [18 20 23]->22->[23] [19 21 22]->23->[22]}";
                        tracebuffer.append(" {");
                        for (int i = 0; i < this.visibleStickers.size(); ++i)
                        {
                            if (i > 0) tracebuffer.append(" ");
                            int iSticker = answer[answerSizeSoFar-this.visibleStickers.size()+i];
                            CHECK(iSticker == visibleStickers.get(sortOrder[i]));

                            // this is wildly inefficient, but whatever; this debugging stuff isn't useful when huge anyway
                            java.util.ArrayList<Integer> preds = new java.util.ArrayList<Integer>();
                            java.util.ArrayList<Integer> succs = new java.util.ArrayList<Integer>();
                            for (int j = 0; j < partialOrderSize; ++j) {
                                if (partialOrder[j][1] == sortOrder[i]) preds.add(visibleStickers.get(partialOrder[j][0]));
                                if (partialOrder[j][0] == sortOrder[i]) succs.add(visibleStickers.get(partialOrder[j][1]));
                            }

                            if (preds.size() > 0) tracebuffer.append(com.donhatchsw.util.Arrays.toString(preds,"["," ","]")+"->");
                            tracebuffer.append(iSticker);
                            if (succs.size() > 0) tracebuffer.append("->"+com.donhatchsw.util.Arrays.toString(succs,"["," ","]"));
                        }
                        tracebuffer.append("}");
                    }

                    if (false) {
                        // The partial order as pairs
                        tracebuffer.append(" {");
                        for (int i = 0; i < partialOrderSize; ++i) {
                            if (i > 0) tracebuffer.append(" ");
                            tracebuffer.append(visibleStickers.get(partialOrder[i][0]));
                            tracebuffer.append("->");
                            tracebuffer.append(visibleStickers.get(partialOrder[i][1]));
                        }
                        tracebuffer.append("}");
                    }

                    tracebuffer.append("\n");
                }

                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"            out SliceFaceNode(iSlice="+iSlice+" iFace="+iFace+").traverse, returning answerSizeSoFar="+answerSizeSoFar);
                return answerSizeSoFar;
            }
        }  // class SliceFaceNode
        class SliceNode extends Node {
            public SliceNode(int iSlice) {
                this.iSlice = iSlice;
            }
            public int iSlice;

            // Logically, we have an array of children.
            // To avoid memory allocations, we use a view into visibleStickersSortedBySliceAndFace instead.
            public ArrayView<Node> children = new ArrayView<Node>();

            @Override protected double computeAverageZnumerator()
            {
                int answer = 0;
                int nChildren = this.children.size();
                for (int i = 0; i < nChildren; ++i) {
                    answer += children.get(i).getAverageZnumerator();
                }
                return answer;
            }
            @Override protected double computeAverageZdenominator()
            {
                int answer = 0;
                int nChildren = this.children.size();
                for (int i = 0; i < nChildren; ++i) {
                    answer += children.get(i).getAverageZdenominator();
                }
                return answer;
            }

            @Override public String toString() {
                return "SliceNode("+iSlice+",children="+children.toString()+")";
            }
            @Override public String shortLabel() {
                return "S("+iSlice+")";
            }
            @Override public int totalSize() {
                int answer = 0;
                int nChildren = this.children.size();
                for (int i = 0; i < nChildren; ++i) {
                    answer += children.get(i).totalSize();
                }
                return answer;
            }
            @Override public int traverse(int answer[], int answerSizeSoFar, StringBuilder tracebuffer, int recursionLevel) {
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"            in SliceNode(iSlice="+iSlice+").traverse, answerSizeSoFar="+answerSizeSoFar);
                int nChildren = this.children.size();
                for (int i = 0; i < nChildren; ++i) {
                    Node child = children.get(i);
                    if (child instanceof SliceNode) {
                        CHECK(slice2localIndex[((SliceNode)child).iSlice] == -1);
                        slice2localIndex[((SliceNode)child).iSlice] = i;
                    } else {
                        CHECK(face2localIndex[((SliceFaceNode)child).iFace] == -1);
                        face2localIndex[((SliceFaceNode)child).iFace] = i;
                    }
                }

                int partialOrderSize = 0;
                int nRelevant = this.relevantAdjacentStickerPairs.size();
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"              "+nRelevant+" relevant pairs");
                for (int iRelevant = 0; iRelevant < nRelevant; ++iRelevant) {
                    int iPair = this.relevantAdjacentStickerPairs.get(iRelevant).intValue();

                    int pair[][] = adjacentStickerPairs[iPair];
                    int iSticker =         pair[0][0];
                    int iPolyThisSticker = pair[0][1];
                    int jSticker =         pair[1][0];
                    int jPolyThisSticker = pair[1][1];

                    // The following should be true since we decided this pair was "relevant"...
                    CHECK(sticker2Slice[iSticker] == iSlice || sticker2Slice[jSticker] == iSlice);
                    if (sticker2Slice[iSticker] == iSlice && sticker2Slice[jSticker] == iSlice) {
                        if (sticker2face[iSticker] == sticker2face[jSticker]) {
                            CHECK(false);
                            continue;  // all within a stickerface; that constraint is handled within that child   XXX could filter these out
                        }
                        // So iSticker,jSticker are both in this slice,
                        // but in two different faces.
                        // In this case, compare using unshrunk.
                        // we care only if both stickers visible.
                        if (!stickerVisibilities[iSticker] || !stickerVisibilities[jSticker]) continue;
                        boolean iStickerHasPolyBackfacing = unshrunkStickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                        boolean jStickerHasPolyBackfacing = unshrunkStickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                        if (iStickerHasPolyBackfacing) {
                            //add "jSticker's sliceface < iSticker's sliceface"
                            partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[jSticker]];
                            partialOrder[partialOrderSize][1] = face2localIndex[sticker2face[iSticker]];
                            partialOrderSize++;
                            if (returnPartialOrderInfoOptionalForDebugging != null) {
                                returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                    {jSticker, jSticker, jSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    {iSticker, iSticker, iSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                };
                            }
                        } else if (jStickerHasPolyBackfacing) {
                            //add "iSticker's sliceface < jSticker's sliceface"
                            partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[iSticker]];
                            partialOrder[partialOrderSize][1] = face2localIndex[sticker2face[jSticker]];
                            partialOrderSize++;
                            if (returnPartialOrderInfoOptionalForDebugging != null) {
                                returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                    {iSticker, iSticker, iSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    {jSticker, jSticker, jSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                };
                            }
                        } else {
                            // this really shouldn't happen, I don't think.
                            // TODO: are we confident enough to CHECK that?
                        }
                    } else {
                        // Exactly one of iSticker,jSticker is in this slice,
                        // and the other is in a different slice.
                        // We care if that other slice is a child of this slice.
                        boolean inSameFace = sticker2face[iSticker] == sticker2face[jSticker];
                        boolean stickerPolyIsStrictlyBackfacing[][] = (inSameFace ? partiallyShrunkStickerPolyIsStrictlyBackfacing : unshrunkStickerPolyIsStrictlyBackfacing);

                        if (sticker2Slice[iSticker] == iSlice) {
                            if (slice2localIndex[sticker2Slice[jSticker]] == -1) {
                                continue;  // its slice is not our child
                            }
                            if (!stickerVisibilities[iSticker]) continue;
                            boolean iStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                            if (iStickerHasPolyBackfacing) {
                                // add "jSticker's slice < iSticker's sliceface"
                                partialOrder[partialOrderSize][0] = slice2localIndex[sticker2Slice[jSticker]];
                                partialOrder[partialOrderSize][1] = face2localIndex[sticker2face[iSticker]];
                                partialOrderSize++;
                                if (returnPartialOrderInfoOptionalForDebugging != null) {
                                    returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                        {jSticker, jSticker, jSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        {iSticker, iSticker, iSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    };
                                }
                            } else {
                                // add "iSticker's sliceface < jSticker's slice"
                                partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[iSticker]];
                                partialOrder[partialOrderSize][1] = slice2localIndex[sticker2Slice[jSticker]];
                                partialOrderSize++;
                                if (returnPartialOrderInfoOptionalForDebugging != null) {
                                    returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                        {iSticker, iSticker, iSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        {jSticker, jSticker, jSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    };
                                }
                            }
                        } else {
                            CHECK(sticker2Slice[jSticker] == iSlice);
                            if (slice2localIndex[sticker2Slice[iSticker]] == -1) {
                                continue;  // its slice is not our child
                            }
                            if (!stickerVisibilities[jSticker]) continue;
                            boolean jStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                            if (jStickerHasPolyBackfacing) {
                                // add "iSticker's slice < jSticker's sliceface"
                                partialOrder[partialOrderSize][0] = slice2localIndex[sticker2Slice[iSticker]];
                                partialOrder[partialOrderSize][1] = face2localIndex[sticker2face[jSticker]];
                                partialOrderSize++;
                                if (returnPartialOrderInfoOptionalForDebugging != null) {
                                    returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                        {iSticker, iSticker, iSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        {jSticker, jSticker, jSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    };
                                }
                            } else {
                                // add "iSticker's sliceface < jSticker's slice"
                                partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[jSticker]];
                                partialOrder[partialOrderSize][1] = slice2localIndex[sticker2Slice[iSticker]];
                                partialOrderSize++;
                                if (returnPartialOrderInfoOptionalForDebugging != null) {
                                    returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                        {jSticker, jSticker, jSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        {iSticker, iSticker, iSticker+0},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    };
                                }
                            }
                        }
                    }
                }

                if (true) {
                    // There will be a lot of dups here (since group constraints are formed in multiple ways); de-dup.
                    // CBB: could leave them in, or not generate them to begin with? not sure. clearest for debugging to de-dup, anyway.
                    partialOrderSize = sortAndCompressPartialOrder(partialOrderSize, partialOrder);
                }

                // restore -1's, before traversing children!
                for (int i = 0; i < nChildren; ++i) {
                    Node child = children.get(i);
                    if (child instanceof SliceNode) {
                        CHECK(slice2localIndex[((SliceNode)child).iSlice] == i);
                        slice2localIndex[((SliceNode)child).iSlice] = -1;
                    } else {
                        CHECK(face2localIndex[((SliceFaceNode)child).iFace] == i);
                        face2localIndex[((SliceFaceNode)child).iFace] = -1;
                    }
                }

                // topsort the children of this slice (other slices, and slicefaces)
                // and emit them in order.
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"              topsort "+nChildren+" items with partial order "+$(com.donhatchsw.util.Arrays.subarray(partialOrder, 0, partialOrderSize)));
                int nComponents = topsorter.topsort(nChildren, sortOrder,
                                                    partialOrderSize, partialOrder,
                                                    componentStarts);
                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"              topsort returned "+nComponents+"/"+nChildren+" components: "+$(com.donhatchsw.util.Arrays.subarray(sortOrder, 0, nChildren)));

                if (nComponents < nChildren) {
                    // Canonical case of this:
                    // standard puzzle, ctrl-middle frontmost vertex on middle face to center.
                    // (There may be some analysis we can do to break such a cycle, but I'm not sure what it is.)
                    if (localVerboseLevel >= 1 || returnPartialOrderInfoOptionalForDebugging!=null) System.out.println("      LOCAL TOPSORT OF "+nChildren+" GROUPS (FACES AND CHILD SLICES) WITHIN SLICE "+iSlice+"/"+nCompressedSlices+" FAILED - Z-SORTING ONE OR MORE CYCLE OF GROUPS");
                    for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                    {
                        int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                        if (componentSize >= 2)
                        {
                            if (localVerboseLevel >= 1) System.out.println("              sorting a strongly connected component (i.e. snakepit of cycles) of length "+componentSize);
                            if (localVerboseLevel >= 2) System.out.println("              before: "+$(com.donhatchsw.util.Arrays.subarray(sortOrder, componentStarts[iComponent], componentSize)));
                            if (true)
                            {
                                com.donhatchsw.util.SortStuff.sort(sortOrder, componentStarts[iComponent], componentSize,
                                    new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                                        @Override public int compare(int i, int j)
                                        {
                                            float iZ = children.get(i).getAverageZ();
                                            float jZ = children.get(j).getAverageZ();
                                            // sort from increasing z to decreasing!
                                            // that is because the z's got negated just before the projection!
                                            return iZ > jZ ? -1 :
                                                   iZ < jZ ? 1 : 0;
                                        }
                                    }
                                );
                            }
                            numZsortsDoneHolder[0]++;
                            if (localVerboseLevel >= 2) System.out.println("              after: "+$(com.donhatchsw.util.Arrays.subarray(sortOrder, componentStarts[iComponent], componentSize)));
                        }
                    }
                }

                StringBuilder tracebufferAux = null;
                if (tracebuffer != null) {
                    // Compose information about the partial order into a local aux tracebuffer, before we recurse and destroy the partial order buffer
                    if (true) {
                        tracebufferAux = new StringBuilder();
                        if (nComponents < nChildren) {
                            tracebufferAux.append("(XXX THERE ARE CYCLES HERE)");
                        }
                        if (true) {
                           // The partial order as pairs.
                           // CBB: could make some nicer presentation of pred/succ lists, like we do for FaceBufferNode.
                           tracebufferAux.append("THE LOCAL PARTIAL ORDER WAS: {");
                           for (int i = 0; i < partialOrderSize; ++i) {
                               if (i > 0) tracebufferAux.append(" ");
                               tracebufferAux.append(children.get(partialOrder[i][0]).shortLabel());
                               tracebufferAux.append("->");
                               tracebufferAux.append(children.get(partialOrder[i][1]).shortLabel());
                           }
                           tracebufferAux.append("}");
                        }
                    }

                }

                if (tracebuffer != null) {
                    tracebuffer.append(repeat("    ",recursionLevel)+"Slice("+this.iSlice+") {\n");
                }
                {
                    // subtle: need to save sortOrder to avoid colliding with sub calls!
                    int[] sortOrderSnapshot = (int[])com.donhatchsw.util.Arrays.subarray(sortOrder, 0, nChildren);  // XXX allocation. Idea: maybe permute the children instead? hmm.  oh, actually, just need one array indexed the same as the allChildren array.  hmm!

                    for (int i = 0; i < nChildren; ++i) {
                        Node child = children.get(sortOrderSnapshot[i]);
                        answerSizeSoFar = child.traverse(answer, answerSizeSoFar, tracebuffer, recursionLevel+1);
                    }
                }

                if (tracebuffer != null) {
                    tracebuffer.append(repeat("    ",recursionLevel+1) + tracebufferAux + "\n");
                }
                if (tracebuffer != null) {
                    tracebuffer.append(repeat("    ",recursionLevel)+"}\n");
                }

                if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"            out SliceNode(iSlice="+iSlice+").traverse, returning answerSizeSoFar="+answerSizeSoFar);
                return answerSizeSoFar;
            }  // traverse
        }  // class SliceNode


        // Allocate a sparse array so we can easily find a given SliceFaceNode from iSlice,iFace.
        // This may seem alarming, but it really isn't that huge
        // (number of entries is commensurate with nStickers).
        SliceFaceNode sliceFaceNodes[][] = new SliceFaceNode[nCompressedSlices][nFaces];  // nulls

        int nSliceFaceNodes = 0;
        for (int iiSticker = 0; iiSticker < nVisibleStickersTotal; ++iiSticker) {
            int iSticker = visibleStickersSortedBySliceAndFace[iiSticker];
            int iSlice = sticker2Slice[iSticker];
            int iFace = sticker2face[iSticker];
            if (sliceFaceNodes[iSlice][iFace] == null)
            {
                nSliceFaceNodes++;
                sliceFaceNodes[iSlice][iFace] = new SliceFaceNode(iSlice, iFace);
                sliceFaceNodes[iSlice][iFace].visibleStickers.init(visibleStickersSortedBySliceAndFace, iiSticker, 0);
            }
            sliceFaceNodes[iSlice][iFace].visibleStickers.init(sliceFaceNodes[iSlice][iFace].visibleStickers.backingStore(),
                                                               sliceFaceNodes[iSlice][iFace].visibleStickers.i0(),
                                                               sliceFaceNodes[iSlice][iFace].visibleStickers.size()+1);
        }
        if (localVerboseLevel >= 1) System.out.println("      nSliceFaceNodes = "+nSliceFaceNodes);

        SliceNode sliceNodes[] = new SliceNode[nCompressedSlices];
        for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice)
        {
            sliceNodes[iSlice] = new SliceNode(iSlice);
        }

        // All non-root nodes, arranged so that the children of any SliceNode are contiguous,
        // so that each SliceNode's children can be an array view into this one big backing store array.
        Node[] allChildNodes = new Node[nCompressedSlices-1 + nSliceFaceNodes];
        if (localVerboseLevel >= 3) System.out.println("      allChildNodes.length = "+allChildNodes.length);
        {
            int iChildNode = 0;
            for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice) {
                if (localVerboseLevel >= 3) System.out.println("              iSlice = "+iSlice);
                // first, count children
                boolean hasLeftChild = iSlice <= eyeSlice && iSlice != 0;
                boolean hasRightChild = iSlice >= eyeSlice && iSlice != nCompressedSlices-1;
                int nChildren = 0;
                if (hasLeftChild) nChildren++;
                if (hasRightChild) nChildren++;
                for (int iFace = 0; iFace < nFaces; ++iFace) {
                    if (sliceFaceNodes[iSlice][iFace] != null) nChildren++;
                }
                if (localVerboseLevel >= 3) System.out.println("                  nChildren = "+nChildren);

                // now, populate children
                int childRangeStart = iChildNode;
                if (hasLeftChild) allChildNodes[iChildNode++] = sliceNodes[iSlice-1];
                if (hasRightChild) allChildNodes[iChildNode++] = sliceNodes[iSlice+1];
                for (int iFace = 0; iFace < nFaces; ++iFace) {
                    SliceFaceNode sliceFaceNode = sliceFaceNodes[iSlice][iFace];
                    if (sliceFaceNode != null) {
                        allChildNodes[iChildNode++] = sliceFaceNode;
                    }
                }
                sliceNodes[iSlice].children.init(allChildNodes, childRangeStart, nChildren);
            }
            CHECK(iChildNode == allChildNodes.length);
        }

        Node root = sliceNodes[eyeSlice];
        if (localVerboseLevel >= 3) System.out.println("      root = sliceNodes[eyeSlice="+eyeSlice+"] = "+root+" totalSize="+root.totalSize());

        // Sanity check that we built the tree correctly.
        if (true)
        {
            if (localVerboseLevel >= 3) {
                for (int iiSticker = 0; iiSticker < nVisibleStickersTotal; ++iiSticker) {
                    int iSticker = visibleStickersSortedBySliceAndFace[iiSticker];
                    if (iiSticker > 0) {
                        if (sticker2Slice[iSticker] != sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]]) System.out.println("          =============================");
                        else if (sticker2face[iSticker] != sticker2face[visibleStickersSortedBySliceAndFace[iiSticker-1]]) System.out.println("          --------------");
                    }
                    System.out.println("          iiSticker="+iiSticker+": iSticker="+iSticker+" iSlice="+sticker2Slice[iSticker]+" iFace="+sticker2face[iSticker]);
                }
            }
            int iiSticker = 0;
            for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice) {
                if (localVerboseLevel >= 3) System.out.println("              iSlice = "+iSlice+" (iiSticker="+iiSticker+")");
                // check we're at a new slice
                CHECK(iiSticker == 0
                   || iiSticker == nVisibleStickersTotal  // there may be a slice with no visible stickers
                   || sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]] < sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker]]);
                SliceNode sliceNode = sliceNodes[iSlice];
                for (int iChild = 0; iChild < sliceNode.children.size(); ++iChild) {
                    Node child = sliceNode.children.get(iChild);
                    if (child instanceof SliceFaceNode) {
                        // check we're at a new sliceface
                        CHECK(iiSticker < nVisibleStickersTotal);  // every SliceFaceNode has at least one visible sticker, or wouldn't have been created
                        CHECK(iiSticker == 0
                           || sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]] < sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker]]
                           || (sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]] == sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker]]
                             && sticker2face[visibleStickersSortedBySliceAndFace[iiSticker-1]] < sticker2face[visibleStickersSortedBySliceAndFace[iiSticker]]));

                        SliceFaceNode sliceFaceNode = (SliceFaceNode)child;
                        for (int i = 0; i < sliceFaceNode.visibleStickers.size(); ++i) {
                            int iSticker = sliceFaceNode.visibleStickers.get(i);
                            CHECK(iSticker == visibleStickersSortedBySliceAndFace[iiSticker++]);
                            if (i != 0)  {
                                // same everything as previous
                                CHECK(sticker2Slice[iSticker] == sticker2Slice[sliceFaceNode.visibleStickers.get(i-1)]);
                                CHECK(sticker2face[iSticker] == sticker2face[sliceFaceNode.visibleStickers.get(i-1)]);
                            }
                        }
                    }
                }
            }
            CHECK(iiSticker == nVisibleStickersTotal);
        }

        {
            float jPolyCenterMinusIPolyCenter[] = new float[3]; // scratch
            int numIgnored = 0;
            for (int iPair = 0; iPair < adjacentStickerPairs.length; ++iPair) {
                int iSticker = adjacentStickerPairs[iPair][0][0];
                int jSticker = adjacentStickerPairs[iPair][1][0];
                int iPolyThisSticker = adjacentStickerPairs[iPair][0][1];
                int jPolyThisSticker = adjacentStickerPairs[iPair][1][1];
                boolean iVisible = stickerVisibilities[iSticker];
                boolean jVisible = stickerVisibilities[jSticker];

                if (!iVisible && !jVisible) continue;

                if (iVisible && jVisible
                 //&& sticker2face[iSticker] != sticker2face[jSticker]  // argh, this might make it worse, but in a good way.  oh! it fixes the problem with canonical puzzle, vert in center, twist front!  I don't quite understand.  VOODOO
                 && sticker2Slice[iSticker] == sticker2Slice[jSticker]  // better voodoo I think.  at least it makes sure the xforms are the same and therefore the geometry we're about to do is really a test of whether warped
                ) {
                    //
                    // See whether things are visible and so inside out
                    // that the polygons are facing away from each other...
                    // If so, then this polygon should not restrict anything.
                    // This can be observed to happen, e.g.:
                    //     - on 4,3,3 in default position, with frontmost vertex ctrl-rotated to center
                    //     - on 5,3,3 in default position, with 4d eye distance increased until nothing behind the 4d eye.
                    // TODO: think about whether an ignoring should also be done in the case when
                    VecMath.vmv(jPolyCenterMinusIPolyCenter,
                                polyCenters3d[jSticker][jPolyThisSticker],
                                polyCenters3d[iSticker][iPolyThisSticker]);
                    // we add a tiny bit of slop to make sure we consider
                    // the adjacency valid if the faces are coincident
                    if (VecMath.dot(polyNormals3d[iSticker][iPolyThisSticker], jPolyCenterMinusIPolyCenter) < -1e-3
                     || VecMath.dot(polyNormals3d[jSticker][jPolyThisSticker], jPolyCenterMinusIPolyCenter) > 1e-3)
                    {
                        if (localVerboseLevel >= 1)
                        {
                            System.out.println("      HA!  I don't CARE because this adjacency is SO WARPED! stickers "+iSticker+"("+iPolyThisSticker+") "+jSticker+"("+jPolyThisSticker+")");
                        }
                        numIgnored++;
                        continue;
                    }
                }

                int iSlice = sticker2Slice[iSticker];
                int jSlice = sticker2Slice[jSticker];
                int iFace = sticker2face[iSticker];
                int jFace = sticker2face[jSticker];

                if (iSlice == jSlice) // i.e. same xform
                {
                    boolean stickerPolyIsStrictlyBackfacing[][] = (iFace==jFace ? partiallyShrunkStickerPolyIsStrictlyBackfacing : unshrunkStickerPolyIsStrictlyBackfacing);
                    boolean iStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                    boolean jStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                    if (iStickerHasPolyBackfacing && jStickerHasPolyBackfacing)
                    {
                        // Note that this cannot happen any more unless
                        // we are viewing the polygon essentially edge-on
                        // and the math got degenerate, since the backfacing flags
                        // were computed preshrunk, which means the two polys
                        // should exactly match.

                        // For posterity, here's a picture of why "either draw order is ok"
                        // is alarming and not ok in general, and why we need to use the unshrunk polys:
                        // A doesn't realize there's anything in front of it,
                        // because it seems like there isn't anything *immediately* in front of it.
                        // That is, we have only B<D C<D.
                        // We also need either A<B or A<C, too, otherwise A might be drawn after D!
                        /*
                                    *
                                   / \
                                  *   *
                                 / \ / \
                              _ *   *   * _
                            *    \ / \ /    *
                             \  _ * A * _  /
                              *B / \ / \ C*
                             /_ *   *   * _\
                            *               *
                                  _ * _
                                * _ D _ *
                                    *
                        */
                        if (localVerboseLevel >= 1 || returnPartialOrderInfoOptionalForDebugging != null) System.out.println("      WARNING: sticker "+iSticker+"("+iPolyThisSticker+") and "+jSticker+"("+jPolyThisSticker+") both have poly backfacing!! Ignoring.  This should be very rare (but evidently happens if stuff behind the eye?)");
                        continue;
                    }
                }

                if (iSlice == jSlice && iFace == jFace) {
                    // inter-node
                    if (iVisible && jVisible) {
                        sliceFaceNodes[iSlice][iFace].relevantAdjacentStickerPairs.add(iPair);
                    }
                } else if (iSlice == jSlice) {
                    // intra-node within same slice
                    // CBB: don't need to do this for some combinations of invisibility I think?
                    sliceNodes[iSlice].relevantAdjacentStickerPairs.add(iPair);
                } else {
                    // intra-node within different slices
                    // CBB: don't need to do this for some combinations of invisibility I think?
                    sliceNodes[iSlice].relevantAdjacentStickerPairs.add(iPair);
                    sliceNodes[jSlice].relevantAdjacentStickerPairs.add(iPair);
                }
            }
            if (numIgnored > 0) {
                if (localVerboseLevel >= 1 || returnPartialOrderInfoOptionalForDebugging != null) System.out.println("      hierarchical painters sort ignored "+numIgnored+" sticker pairs because too warped");
            }
        }

        StringBuilder tracebuffer = localVerboseLevel >= 2 ? new StringBuilder() : null;
        int nStickersEmitted = root.traverse(returnStickerSortOrder, 0, tracebuffer, /*recursionLevel=*/0);

        if (returnPartialOrderInfoOptionalForDebugging != null) {
            returnPartialOrderInfoOptionalForDebugging[0] = (int[][][])com.donhatchsw.util.Arrays.subarray(returnPartialOrderInfoOptionalForDebugging[0], 0, returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]);
        }

        if (returnSummaryOptionalForDebugging != null) {
            if (numZsortsDoneHolder[0] == 0) {
                returnSummaryOptionalForDebugging[0] = "no cycles";
            } else {
                returnSummaryOptionalForDebugging[0] = ""+numZsortsDoneHolder[0]+" z-sort"+(numZsortsDoneHolder[0]==1?"":"s")+" needed to resolve cycles (increasing 4D Eye Distance usually fixes this)";
            }
        }

        if (localVerboseLevel >= 1 && returnPartialOrderInfoOptionalForDebugging != null) System.out.println("      partial order info = "+$(returnPartialOrderInfoOptionalForDebugging[0]));
        if (localVerboseLevel >= 1) System.out.println("      returnStickerSortOrder = "+$(com.donhatchsw.util.Arrays.subarray(returnStickerSortOrder, 0, nStickersEmitted)));
        if (localVerboseLevel >= 1) System.out.println("      tracebuffer = \n"+tracebuffer);
        if (localVerboseLevel >= 1) System.out.println("    out sortStickersBackToFront (bold new way), returning nStickersEmitted="+nStickersEmitted);
        return nStickersEmitted;
    } // sortStickersBackToFront

    // general utility; could go elsewhere
    private static int sortAndCompressPartialOrder(int partialOrderSize, int[][/*2 or 3*/] partialOrder)
    {
        //System.out.println("in sortAndCompressPartialOrder");
        //System.out.println("  before: "+$(partialOrder,0,partialOrderSize));

        com.donhatchsw.util.SortStuff.sort(partialOrder, 0, partialOrderSize,
            new com.donhatchsw.util.SortStuff.Comparator() { // XXX ALLOCATION! (need to make sort smarter)
                @Override public int compare(Object aObj, Object bObj)
                {
                    int[] a = (int[])aObj;
                    int[] b = (int[])bObj;
                    CHECK(a.length == b.length);
                    for (int i = 0; i < a.length; ++i) {
                       if (a[i] < b[i]) return -1;
                       if (a[i] > b[i]) return 1;
                    }
                    return 0;
                }
            }
        );
        {
            int nOut = 0;
            for (int i = 0; i < partialOrderSize; ++i) {
                if (i == 0 || !VecMath.equals(partialOrder[i], partialOrder[nOut-1])) {
                    //partialOrder[nOut++] = partialOrder[i];  // no! have to swap, not set, so we don't wreck the reusable array
                    com.donhatchsw.util.Arrays.swap(partialOrder, nOut++, partialOrder, i);
                }
            }
            partialOrderSize = nOut;
        }
        //System.out.println("  after: "+$(partialOrder,0,partialOrderSize));
        //System.out.println("out sortAndCompressPartialOrder");
        return partialOrderSize;
    }  // sortAndCompressPartialOrder

    // EXPERIMENTAL
    public static class ArrayView<E>
    {
        private E[] backingStore;
        private int i0;
        private int size;
        // Note, there is intentionally no constructor that takes backingStore,i0,size.
        // This is to encourage reuse rather than (possibly accidentally) constructing new instances.
        public void init(E[] backingStore, int i0, int size)
        {
            // note, if size is 0, then anything else is allowed (including backingStore==null), but in that case get() will throw if called
            if (size != 0 && (i0 < 0 || i0+size > backingStore.length)) {
                throw new IndexOutOfBoundsException("ArrayView.init: "+i0+".."+(i0+size)+"-1 out of bounds 0.."+backingStore.length+"-1");
            }
            this.backingStore = (size==0 ? null : backingStore);
            this.i0 = i0;
            this.size = size;
        }
        // CBB: could have an init() that takes another IntArrayView
        // CBB: could have set()
        public E get(int i) {
            if (i < 0 || i >= this.size)
                throw new IndexOutOfBoundsException("ArrayView.get: "+i+" out of bounds 0.."+this.size+"-1");
            return this.backingStore[this.i0 + i];
        }

        public final int size() { return size; }  // read-only
    };  // class ArrayView
    // Argh, and the generic version doesn't work for int[].  Burned again :-(
    // I don't want the overhead of boxing/unboxing,
    // so I'll just write a one-off, with a bare minimum of functionality.
    // The overhead of writing this is worth the clarity of usage in this file alone.
    public static class IntArrayView
    {
        private int[] backingStore = null;
        private int i0 = 0;
        private int size = 0;
        public IntArrayView() {}
        // Note, there is intentionally no constructor that takes backingStore,i0,size.
        // This is to encourage reuse rather than (possibly accidentally) constructing new instances.
        public final void init(int[] backingStore, int i0, int size)
        {
            // note, if size is 0, then anything else is allowed (including backingStore==null), but in that case get() will throw if called
            if (size != 0 && (i0 < 0 || i0+size > backingStore.length)) {
                throw new IndexOutOfBoundsException("IntArrayView.init: "+i0+".."+(i0+size)+"-1 out of bounds 0.."+backingStore.length+"-1");
            }
            this.backingStore = backingStore;
            this.i0 = i0;
            this.size = size;
        }
        // CBB: could have an init() that takes another IntArrayView
        // CBB: could have set()
        public final int get(int i) {
            if (i < 0 || i >= this.size) {
                throw new IndexOutOfBoundsException("ArrayView.get: "+i+" out of bounds 0.."+this.size+"-1");
            }
            return this.backingStore[this.i0 + i];
        }
        public final String toString() {
            return com.donhatchsw.util.Arrays.toStringCompact(
                   com.donhatchsw.util.Arrays.subarray(this.backingStore, this.i0, this.size));
        }

        public final int size() { return size; }  // read-only
        // Full public read-only access, so contents be passed to other utilities like Arrays
        // and Topsorter that don't know about this class.
        public final int[] backingStore() { return backingStore; }
        public final int i0() { return i0; }
    };  // class IntArrayView

} // class VeryCleverPaintersSortingOfStickers

