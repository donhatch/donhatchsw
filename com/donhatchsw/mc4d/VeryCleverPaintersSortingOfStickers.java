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
involves parallel slices 0,1,2,3,4.
Label slice 0's stickers 0a,0b,0c,... and slice 1's stickers 1a,1b,... etc.
In the most general scenario, each of the slices is being twisted
independently, so imagine each slice having a slightly different twist,
with respect to the following picture.

              ----+ +---+ +-------------+ +---+
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

The goal is to produce a reasonable "back-to-front" ordering of all the
stickers, with respect to the 3d eye in the real puzzle
(or, in this picture, with respect to a 2d eye; imagine the 2d eye
is anywhere around the perimeter of the above picture).

The rendering order is determined by a tree traversal starting at the root:
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
twisted differently, we use the polygon's current orientation on the more
rootmost of the two children.

So, in more detail, there are 3 cases:
- child0 is a sticker, child1 is a sticker (both in the same slice).
  E.g. 2a vs. 2b.
  In this case both stickers have been twisted by the same twist matrix;
  we use that orientation of their shared polygon to determine which is in front.
- child0 is a sticker, child1 is a slice.  E.g. 2b vs. Slice 3.
  In this case they may be twisted by two different twist matrices.
  We use the current orientation of the polygon on child0 (the sticker)
  rather than looking at anything currently on child1 (the differently twisted slice).
- child1 is a slice, child0 is a sticker
  This is just the previous case, with the two children reversed.
- (The case where child0 and child1 both slices can't happen:
  the only node that can have two children is the root, and in that case
  the two children are not physically adjacent.)

Further detail: we actually further hierarchicalize by grouping
the sticker children of each slice into convex groups called "slice faces";
each slice face is the intersection of the slice with a face.
This is for two reasons:
  1. In the case of faceshrink < 1, just topsorting by sticker adjacency isn't enough,
     but apparently that can be fixed in (almost?) all cases by grouping stickers by face.
     Canonical example:
              +---+
             /a\b/c\  Face 0 (shrunk)
            +---+---+

            +---+---+
             \d/e\f/  Face 1 (shrunk)
              +---+

           +
          Eye

     The naive dag in this case is on only the stickers
     (with no cognizance of face grouping),
     and consists of the following edges:
            b->a->d->e
            b->c->f->e

     Note that this naive dag does not capture the fact that c is behind d
     and therefore must be rendered first!
     So topsort can produce any of the following orderings:
            b a d c f e  BAD!
            b a c d f e
            b c a d f e
            b c f a d e

     Now consider what happens if we further hierarchicalize:
     that is, require all of Face 0 to come out consecutively,
     and all of Face 1 to come out consecutively.
     That rules out two of the 4 possible orderings, leaving
     the following two:
            b a c  d f e
            b c a  d f e
     Either of which are good, from the given Eye point.

  2. better-isolated failure (cycle) detection and fallback.

*/

package com.donhatchsw.mc4d;

import com.donhatchsw.util.VecMath;

public class VeryCleverPaintersSortingOfStickers
{
    // XXX figure out where to put this, if anywhere.  it will probably go away
    private static java.util.Random randomnessGenerator = new java.util.Random();

    private static void CHECK(boolean condition) { if (!condition) throw new Error("CHECK failed"); }
    private static String $(Object obj) { return com.donhatchsw.util.Arrays.toStringCompact(obj); }  // convenience
    private static String repeat(String s, int n) { StringBuffer sb = new StringBuffer(); for (int i = 0; i < n; ++i) sb.append(s); return sb.toString(); }

    // Function return value is number of stickers to draw
    public static int sortStickersBackToFront(
            boolean boldNewWay,  // whether to use experimental new algorithm.  it's better, but kinks haven't been worked out yet.
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
            final float stickerCentersZ[/*>=nStickers*/],
            float polyCenters3d[/*>=nStickers*/][/*nPolysThisSticker*/][/*3*/],
            float polyNormals3d[/*>=nStickers*/][/*nPolysThisSticker*/][/*3*/])
    {
        final int localVerboseLevel = 0;  // hard-code to something higher to debug. 0: nothing, 1: in/out and constant time, 2: more verbose on cycles, 3: fine details
        if (localVerboseLevel >= 1) System.out.println("    in sortStickersBackToFront");
        if (localVerboseLevel >= 1) System.out.println("      nStickers = "+$(nStickers));
        if (localVerboseLevel >= 1) System.out.println("      cutNormal = "+$(cutNormal));
        if (localVerboseLevel >= 1) System.out.println("      cutOffsets = "+$(cutOffsets));
        if (localVerboseLevel >= 3) {
            System.out.println("      sticker2Slice = "+$(sticker2Slice));
            System.out.println("      adjacentStickerPairs = "+$(adjacentStickerPairs));
        }

        if (returnPartialOrderInfoOptionalForDebugging != null) {
            returnPartialOrderInfoOptionalForDebugging[0] = new int[adjacentStickerPairs.length][][];
        }
        final int[] returnPartialOrderInfoOptionalForDebuggingSizeHolder = (returnPartialOrderInfoOptionalForDebugging != null) ? new int[] {0} : null;

        final int nSlices = cutOffsets.length + 1;
        int nCompressedSlices = nSlices; // XXX should combine adjacent slices that are moving together... but maybe it doesn't hurt to just pretend all the slices are twisting separately, it keeps things simple?  Not sure.
        int nNodes = nStickers + 2*nCompressedSlices;
        int parents[] = new int[nNodes];
        int depths[] = new int[nNodes]; // XXX this array is not really necessary, but it simplifies the code

        int maxPartialOrderSize = nStickers*2 // for group inclusion
                                + adjacentStickerPairs.length;
        final int partialOrder[][] = new int[maxPartialOrderSize][2];
        int partialOrderSize = 0;
        final com.donhatchsw.util.TopSorter topsorter = new com.donhatchsw.util.TopSorter(/*maxN=*/nNodes, maxPartialOrderSize); // XXX allocation
        final int nodeSortOrder[] = new int[nNodes]; // XXX allocation
        final int componentStarts[] = new int[nNodes+1]; // one extra for end of last one.  XXX allocation

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
        if (localVerboseLevel >= 1) System.out.println("      eyeSlice = "+eyeSlice);

        if (boldNewWay) {
            // BOLD NEW WORK IN PROGRESS: new way of doing things, let's see if it pans out.

            int[] visibleStickersSortedBySliceAndFace = new int[nStickers];
            int nVisibleStickers = 0;
            for (int iSticker = 0; iSticker < nStickers; ++iSticker) {
                if (stickerVisibilities[iSticker]) {
                    visibleStickersSortedBySliceAndFace[nVisibleStickers++] = iSticker;
                }
            }
            if (localVerboseLevel >= 1) System.out.println("      nVisibleStickers = "+$(nVisibleStickers));

            com.donhatchsw.util.SortStuff.sort(visibleStickersSortedBySliceAndFace, 0, nVisibleStickers,
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

            Preparation must proceed as follows:
              for each sticker adjacency
                  if in different slices:
                     add faceslice-slice constraint within this slice
                  else if in different faceslice within same slice:
                     add faceslice-faceslice constraint within this slice
                  else:  // in same faceslice
                     add sticker-sticker constraint within this faceslice
            */

            // figure out nFaces (or rather an upper bound on what we're interested in)
            final int nFaces = sticker2face.length==0 ? 0 : VecMath.max(sticker2face)+1;
            final int[] face2localIndex = VecMath.fillvec(nFaces, -1);
            final int[] slice2localIndex = VecMath.fillvec(nSlices, -1);
            final int[] sticker2localIndex = VecMath.fillvec(nStickers, -1);

            abstract class Node {
                public abstract int traverse(int answer[], int answerSizeSoFar, int recursionLevel);
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
            }  // abstract class Node
            class SliceFaceNode extends Node {
                public SliceFaceNode(int iSlice, int iFace) {
                    this.iSlice = iSlice;
                    this.iFace = iFace;
                }
                public int iSlice;
                public int iFace;
                public int visibleStickers[];

                @Override protected double computeAverageZnumerator()
                {
                    double answer = 0.;
                    for (int i = 0; i < visibleStickers.length; ++i) {
                        answer += stickerCentersZ[this.visibleStickers[i]];
                    }
                    return answer;
                }
                @Override protected double computeAverageZdenominator()
                {
                    return visibleStickers.length;
                }

                @Override public String toString() {
                    return "SliceFaceNode(visibleStickers="+$(visibleStickers)+")";
                }
                @Override public int totalSize() { return visibleStickers.length; }
                @Override public int traverse(int answer[], int answerSizeSoFar, int recursionLevel) {
                    if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"    in SliceFaceNode(iSlice="+iSlice+" iFace="+iFace+").traverse, answerSizeSoFar="+answerSizeSoFar);
                    for (int i = 0; i < visibleStickers.length; ++i)
                    {
                        CHECK(sticker2localIndex[visibleStickers[i]] == -1);
                        sticker2localIndex[visibleStickers[i]] = i;
                    }
                    // topsort the visibleStickers within this sliceface, by immediate adjacencies,
                    // and emit them in order.
                    // TODO: totally inefficient to make another full pass for each SliceFace like this!
                    int partialOrderSize = 0;
                    for (int iPair = 0; iPair < adjacentStickerPairs.length; ++iPair) {
                        int pair[][] = adjacentStickerPairs[iPair];
                        int iSticker =         pair[0][0];
                        int iPolyThisSticker = pair[0][1];
                        int jSticker =         pair[1][0];
                        int jPolyThisSticker = pair[1][1];
                        if (sticker2Slice[iSticker] == iSlice
                         && sticker2face[iSticker] == iFace
                         && sticker2Slice[jSticker] == iSlice
                         && sticker2face[jSticker] == iFace) {
                            // we care only if both stickers visible.
                            if (!stickerVisibilities[iSticker] || !stickerVisibilities[jSticker]) continue;
                            boolean iStickerHasPolyBackfacing = partiallyShrunkStickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                            boolean jStickerHasPolyBackfacing = partiallyShrunkStickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                            if (iStickerHasPolyBackfacing) {
                                //add "jSticker < iSticker"
                                partialOrder[partialOrderSize][0] = sticker2localIndex[jSticker];
                                partialOrder[partialOrderSize][1] = sticker2localIndex[iSticker];
                                partialOrderSize++;
                                if (returnPartialOrderInfoOptionalForDebugging != null) {
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
                                        {jSticker, jSticker, jSticker+1},
                                        {iSticker, iSticker, iSticker+1},
                                    };
                                }
                            } else {
                                // this really shouldn't happen, I don't think
                            }
                        }
                    }
                    int nComponents = topsorter.topsort(this.visibleStickers.length, nodeSortOrder,
                                                        partialOrderSize, partialOrder,
                                                        componentStarts);
                    if (nComponents < this.visibleStickers.length) {
                        System.out.println("      LOCAL TOPSORT OF "+this.visibleStickers.length+" STICKERS WITHIN FACE "+iFace+"/"+nFaces+" WITHIN SLICE "+iSlice+"/"+nSlices+" FAILED - Z-SORTING ONE OR MORE CYCLE OF STICKERS");
                        for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                        {
                            int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                            if (componentSize >= 2)
                            {
                                if (localVerboseLevel >= 1) System.out.println("    sorting a strongly connected component (i.e. snakepit of cycles) of length "+componentSize+"");
                                if (localVerboseLevel >= 2) System.out.println("              after: "+$(com.donhatchsw.util.Arrays.subarray(nodeSortOrder, componentStarts[iComponent], componentSize)));
                                if (true)
                                {
                                    com.donhatchsw.util.SortStuff.sort(nodeSortOrder, componentStarts[iComponent], componentSize,
                                        new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                                            @Override public int compare(int i, int j)
                                            {
                                                CHECK(stickerVisibilities[SliceFaceNode.this.visibleStickers[i]]);
                                                CHECK(stickerVisibilities[SliceFaceNode.this.visibleStickers[j]]);
                                                float iZ = stickerCentersZ[SliceFaceNode.this.visibleStickers[i]];
                                                float jZ = stickerCentersZ[SliceFaceNode.this.visibleStickers[j]];
                                                // sort from increasing z to decreasing!
                                                // that is because the z's got negated just before the projection!
                                                return iZ > jZ ? -1 :
                                                       iZ < jZ ? 1 : 0;
                                            }
                                        }
                                    );
                                }
                                if (localVerboseLevel >= 2) System.out.println("              after: "+$(com.donhatchsw.util.Arrays.subarray(nodeSortOrder, componentStarts[iComponent], componentSize)));
                            }
                        }
                    } else {
                        for (int i = 0; i < visibleStickers.length; ++i) {
                            int iSticker = visibleStickers[nodeSortOrder[i]];
                            answer[answerSizeSoFar++] = iSticker;
                        }
                    }

                    // restore -1's
                    for (int i = 0; i < visibleStickers.length; ++i)
                    {
                        CHECK(sticker2localIndex[visibleStickers[i]] == i);
                        sticker2localIndex[visibleStickers[i]] = -1;
                    }
                    if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"    out SliceFaceNode(iSlice="+iSlice+" iFace="+iFace+").traverse, returning answerSizeSoFar="+answerSizeSoFar);
                    return answerSizeSoFar;
                }
            }  // class SliceFaceNode
            class SliceNode extends Node {
                public SliceNode(int iSlice) {
                    this.iSlice = iSlice;
                }
                public int iSlice;
                public Node children[];

                @Override protected double computeAverageZnumerator()
                {
                    int answer = 0;
                    for (int i = 0; i < children.length; ++i) {
                        answer += children[i].getAverageZnumerator();
                    }
                    return answer;
                }
                @Override protected double computeAverageZdenominator()
                {
                    int answer = 0;
                    for (int i = 0; i < children.length; ++i) {
                        answer += children[i].getAverageZdenominator();
                    }
                    return answer;
                }

                @Override public String toString() {
                    return "SliceNode(children="+$(children)+")";
                }
                @Override public int totalSize() {
                    int answer = 0;
                    for (int i = 0; i < children.length; ++i) {
                        answer += children[i].totalSize();
                    }
                    return answer;
                }
                @Override public int traverse(int answer[], int answerSizeSoFar, int recursionLevel) {
                    if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"    in SliceNode(iSlice="+iSlice+").traverse, answerSizeSoFar="+answerSizeSoFar);
                    for (int i = 0; i < children.length; ++i) {
                        if (children[i] instanceof SliceNode) {
                            CHECK(slice2localIndex[((SliceNode)children[i]).iSlice] == -1);
                            slice2localIndex[((SliceNode)children[i]).iSlice] = i;
                        } else {
                            CHECK(face2localIndex[((SliceFaceNode)children[i]).iFace] == -1);
                            face2localIndex[((SliceFaceNode)children[i]).iFace] = i;
                        }
                    }

                    int partialOrderSize = 0;
                    // TODO: totally inefficient to make another full pass for each SliceFace like this!
                    for (int iPair = 0; iPair < adjacentStickerPairs.length; ++iPair) {
                        int pair[][] = adjacentStickerPairs[iPair];
                        int iSticker =         pair[0][0];
                        int iPolyThisSticker = pair[0][1];
                        int jSticker =         pair[1][0];
                        int jPolyThisSticker = pair[1][1];



                        // cases we care about here:
                        //   - constraint between two different slicefaces of this slice
                        //   - constraint between a sliceface and a slice that's a child of this one
                        if (sticker2Slice[iSticker] != iSlice && sticker2Slice[jSticker] != iSlice) {
                            continue;  // not relevant to this slice at all
                        }
                        if (sticker2Slice[iSticker] == iSlice && sticker2Slice[jSticker] == iSlice) {
                            if (sticker2face[iSticker] == sticker2face[jSticker]) {
                                continue;  // all within a stickerface; that constraint is handled within that child
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
                                        {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                    };
                                }
                            } else if (jStickerHasPolyBackfacing) {
                                //add "iSticker's sliceface < jSticker's sliceface"
                                partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[iSticker]];
                                partialOrder[partialOrderSize][1] = face2localIndex[sticker2face[jSticker]];
                                partialOrderSize++;
                                if (returnPartialOrderInfoOptionalForDebugging != null) {
                                    returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                        {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
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
                                            {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                            {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        };
                                    }
                                } else {
                                    // add "iSticker's sliceface < jSticker's slice"
                                    partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[iSticker]];
                                    partialOrder[partialOrderSize][1] = slice2localIndex[sticker2Slice[jSticker]];
                                    partialOrderSize++;
                                    if (returnPartialOrderInfoOptionalForDebugging != null) {
                                        returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                            {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                            {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
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
                                            {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                            {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        };
                                    }
                                } else {
                                    // add "iSticker's sliceface < jSticker's slice"
                                    partialOrder[partialOrderSize][0] = face2localIndex[sticker2face[jSticker]];
                                    partialOrder[partialOrderSize][1] = slice2localIndex[sticker2Slice[iSticker]];
                                    partialOrderSize++;
                                    if (returnPartialOrderInfoOptionalForDebugging != null) {
                                        returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                            {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                            {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                        };
                                    }
                                }
                            }
                        }
                    }

                    // There will be a lot of dups here; de-dup.
                    // CBB: could leave them in, or not generate them to begin with? not sure. clearest for debugging to de-dup, anyway.
                    partialOrderSize = sortAndCompressPartialOrder(partialOrderSize, partialOrder);

                    // restore -1's, before traversing children!
                    for (int i = 0; i < children.length; ++i) {
                        if (children[i] instanceof SliceNode) {
                            CHECK(slice2localIndex[((SliceNode)children[i]).iSlice] == i);
                            slice2localIndex[((SliceNode)children[i]).iSlice] = -1;
                        } else {
                            CHECK(face2localIndex[((SliceFaceNode)children[i]).iFace] == i);
                            face2localIndex[((SliceFaceNode)children[i]).iFace] = -1;
                        }
                    }

                    // topsort the children of this slice (other slices, and slicefaces)
                    // and emit them in order.
                    if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"      topsort "+this.children.length+" items with partial order "+$(com.donhatchsw.util.Arrays.subarray(partialOrder, 0, partialOrderSize)));
                    int nComponents = topsorter.topsort(this.children.length, nodeSortOrder,
                                                        partialOrderSize, partialOrder,
                                                        componentStarts);
                    if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"      topsort returned "+nComponents+"/"+this.children.length+" components: "+$(com.donhatchsw.util.Arrays.subarray(nodeSortOrder, 0, this.children.length)));

                    if (nComponents < this.children.length) {
                        // Canonical case of this:
                        // standard puzzle, ctrl-middle frontmost vertex on middle face to center.
                        // (There may be some analysis we can do to break such a cycle, but I'm not sure what it is.)
                        System.out.println("      LOCAL TOPSORT OF "+this.children.length+" GROUPS (FACES AND CHILD SLICES) WITHIN SLICE "+iSlice+"/"+nSlices+" FAILED - Z-SORTING ONE OR MORE CYCLE OF GROUPS");
                        for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                        {
                            int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                            if (componentSize >= 2)
                            {
                                if (localVerboseLevel >= 1) System.out.println("              sorting a strongly connected component (i.e. snakepit of cycles) of length "+componentSize);
                                if (localVerboseLevel >= 2) System.out.println("              before: "+$(com.donhatchsw.util.Arrays.subarray(nodeSortOrder, componentStarts[iComponent], componentSize)));
                                if (true)
                                {
                                    com.donhatchsw.util.SortStuff.sort(nodeSortOrder, componentStarts[iComponent], componentSize,
                                        new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                                            @Override public int compare(int i, int j)
                                            {
                                                float iZ = children[i].getAverageZ();
                                                float jZ = children[j].getAverageZ();
                                                // sort from increasing z to decreasing!
                                                // that is because the z's got negated just before the projection!
                                                return iZ > jZ ? -1 :
                                                       iZ < jZ ? 1 : 0;
                                            }
                                        }
                                    );
                                }
                                if (localVerboseLevel >= 2) System.out.println("              after: "+$(com.donhatchsw.util.Arrays.subarray(nodeSortOrder, componentStarts[iComponent], componentSize)));
                            }
                        }
                    }
                    //else // XXX
                    {
                        // subtle: need to save nodeSortOrder to avoid colliding with sub calls!
                        int[] nodeSortOrderSnapshot = (int[])com.donhatchsw.util.Arrays.subarray(nodeSortOrder, 0, this.children.length);  // XXX allocation. Idea: maybe permute the children instead?

                        for (int i = 0; i < children.length; ++i) {
                            Node child = children[nodeSortOrderSnapshot[i]];
                            answerSizeSoFar = child.traverse(answer, answerSizeSoFar, recursionLevel+1);
                        }
                    }


                    if (localVerboseLevel >= 3) System.out.println(repeat("    ",recursionLevel)+"    out SliceNode(iSlice="+iSlice+").traverse, returning answerSizeSoFar="+answerSizeSoFar);
                    return answerSizeSoFar;
                }  // traverse
            }  // class SliceNode
            SliceNode sliceNodes[] = new SliceNode[nCompressedSlices];
            for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice)
            {
                sliceNodes[iSlice] = new SliceNode(iSlice);
            }

            int iiSticker0 = 0;
            for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice)
            {
                if (localVerboseLevel >= 3) System.out.println("          iSlice = "+iSlice);
                boolean hasLeftChild = iSlice <= eyeSlice && iSlice != 0;
                boolean hasRightChild = iSlice >= eyeSlice && iSlice != nCompressedSlices-1;
                int nSliceChildren = (hasLeftChild?1:0) + (hasRightChild?1:0);
                if (localVerboseLevel >= 3) System.out.println("              nSliceChildren = "+nSliceChildren);

                // How many slicefaces on this slice?
                // iiSticker0 is now the beginning...
                int nSliceFacesThisSlice = 0;
                int iiSticker1 = iiSticker0;
                while (iiSticker1 < nVisibleStickers
                    && sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker1]] ==
                       sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker0]]) {
                    if (iiSticker1 == iiSticker0 || sticker2face[visibleStickersSortedBySliceAndFace[iiSticker1]] !=
                                                    sticker2face[visibleStickersSortedBySliceAndFace[iiSticker1-1]]) {
                        nSliceFacesThisSlice++;
                    }
                    iiSticker1++;
                }
                if (localVerboseLevel >= 3) System.out.println("              nSliceFacesThisSlice = "+nSliceFacesThisSlice);

                int nChildren = nSliceChildren + nSliceFacesThisSlice;
                sliceNodes[iSlice].children = new Node[nChildren];

                int iChild = 0;
                if (hasLeftChild) sliceNodes[iSlice].children[iChild++] = sliceNodes[iSlice-1];
                if (hasRightChild) sliceNodes[iSlice].children[iChild++] = sliceNodes[iSlice+1];


                int firstStickerThisSliceFace = iiSticker0;
                // For each sticker in this slice...
                for (int iiSticker = iiSticker0; iiSticker < iiSticker1; ++iiSticker) {
                    int iSticker = visibleStickersSortedBySliceAndFace[iiSticker];
                    CHECK(sticker2Slice[iSticker] == iSlice);
                    int iFace = sticker2face[iSticker];
                    if (iiSticker == iiSticker0
                     || iFace != sticker2face[visibleStickersSortedBySliceAndFace[iiSticker-1]]) {
                        // This is the first sticker in a new sliceface
                        int nStickersThisSliceFace = 1;
                        while (firstStickerThisSliceFace+nStickersThisSliceFace < iiSticker1
                            && sticker2face[visibleStickersSortedBySliceAndFace[firstStickerThisSliceFace+nStickersThisSliceFace]] ==
                               sticker2face[visibleStickersSortedBySliceAndFace[firstStickerThisSliceFace]]) {
                            nStickersThisSliceFace++;
                        }
                        SliceFaceNode sliceFaceNode = new SliceFaceNode(iSlice, iFace);

                        sliceFaceNode.visibleStickers = (int[])com.donhatchsw.util.Arrays.subarray(
                            visibleStickersSortedBySliceAndFace,
                            firstStickerThisSliceFace,
                            nStickersThisSliceFace);

                        sliceNodes[iSlice].children[iChild++] = sliceFaceNode;
                        firstStickerThisSliceFace += nStickersThisSliceFace;
                    }
                }
                CHECK(firstStickerThisSliceFace == iiSticker1);
                CHECK(iChild == nChildren);

                // Advance iiSticker0 to beginning of next slice
                iiSticker0 = iiSticker1;
            }
            CHECK(iiSticker0 == nVisibleStickers);

            Node root = sliceNodes[eyeSlice];
            if (localVerboseLevel >= 3) System.out.println("          root = sliceNodes[eyeSlice="+eyeSlice+"] = "+root+" totalSize="+root.totalSize());

            // Sanity check that we built the tree correctly
            {
                int iiSticker = 0;
                for (int iSlice = 0; iSlice < nSlices; ++iSlice) {
                    // check we're at a new slice
                    CHECK(iiSticker == 0
                       || sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]] < sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker]]);
                    SliceNode sliceNode = sliceNodes[iSlice];
                    for (int iChild = 0; iChild < sliceNode.children.length; ++iChild) {
                        Node child = sliceNode.children[iChild];
                        if (child instanceof SliceFaceNode) {
                            // check we're at a new sliceface
                            CHECK(iiSticker == 0
                               || sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]] < sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker]]
                               || (sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker-1]] == sticker2Slice[visibleStickersSortedBySliceAndFace[iiSticker]]
                                 && sticker2face[visibleStickersSortedBySliceAndFace[iiSticker-1]] < sticker2face[visibleStickersSortedBySliceAndFace[iiSticker]]));

                            SliceFaceNode sliceFaceNode = (SliceFaceNode)child;
                            for (int i = 0; i < sliceFaceNode.visibleStickers.length; ++i) {
                                int iSticker = sliceFaceNode.visibleStickers[i];
                                CHECK(iSticker == visibleStickersSortedBySliceAndFace[iiSticker++]);
                                if (i != 0)  {
                                    // same everything as previous
                                    CHECK(sticker2Slice[iSticker] == sticker2Slice[sliceFaceNode.visibleStickers[i-1]]);
                                    CHECK(sticker2face[iSticker] == sticker2face[sliceFaceNode.visibleStickers[i-1]]);
                                }
                            }
                        }
                    }
                }
                CHECK(iiSticker == nVisibleStickers);
            }

            int nStickersEmitted = root.traverse(returnStickerSortOrder, 0, /*recursionLevel=*/0);

            if (returnPartialOrderInfoOptionalForDebugging != null) {
                returnPartialOrderInfoOptionalForDebugging[0] = (int[][][])com.donhatchsw.util.Arrays.subarray(returnPartialOrderInfoOptionalForDebugging[0], 0, returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]);
            }

            if (localVerboseLevel >= 1) System.out.println("    out sortStickersBackToFront (bold new way), returning nStickersEmitted="+nStickersEmitted);
            return nStickersEmitted;
        }  // end bold new work in progress

        // Initialize parents and depths...
        {
            for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice)
            {
                int iNode = nStickers + 2*iSlice;
                if (iSlice < eyeSlice)
                {
                    parents[iNode] = nStickers + 2*(iSlice+1);
                    depths[iNode] = eyeSlice - iSlice;
                }
                else if (iSlice > eyeSlice)
                {
                    parents[iNode] = nStickers + 2*(iSlice-1);
                    depths[iNode] = iSlice - eyeSlice;
                }
                else
                {
                    parents[iNode] = -1;
                    depths[iNode] = 0;
                }
                // For each iSlice, the odd node position nStickers+2*iSlice+1 is not used;
                // it is just an end token for the group,
                // so that when we need a sticker to be > an entire group,
                // we have to specify only one inequality instead
                // of one for each element of the group.
            }

            for (int iSticker = 0; iSticker < nStickers; ++iSticker)
            {
                int iSlice = sticker2Slice[iSticker];
                int iGroup = nStickers + 2*iSlice;
                int iGroupStartToken = nStickers + 2*iSlice;
                int iGroupEndToken = nStickers + 2*iSlice+1;

                parents[iSticker] = iGroup;
                depths[iSticker] = depths[parents[iSticker]] + 1;

                if (stickerVisibilities[iSticker])
                {
                    // add "iGroupStartToken < iSticker"
                    partialOrder[partialOrderSize][0] = iGroupStartToken;
                    partialOrder[partialOrderSize][1] = iSticker;
                    partialOrderSize++;
                    // add "iSticker < iGroupEndToken"
                    partialOrder[partialOrderSize][0] = iSticker;
                    partialOrder[partialOrderSize][1] = iGroupEndToken;
                    partialOrderSize++;
                }
            }
        }

        {
            float jPolyCenterMinusIPolyCenter[] = new float[3]; // scratch
            for (int iPoly = 0; iPoly < adjacentStickerPairs.length; ++iPoly)
            {
                int stickersThisPoly[][] = adjacentStickerPairs[iPoly];
                int iSticker =         stickersThisPoly[0][0];
                int iPolyThisSticker = stickersThisPoly[0][1];
                int jSticker =         stickersThisPoly[1][0];
                int jPolyThisSticker = stickersThisPoly[1][1];
                if (iSticker >= nStickers
                 || jSticker >= nStickers)
                    continue; // caller probably set nStickers < actaul number of stickers for debugging

                if (jSticker < iSticker)
                    continue; // already did it
                boolean iStickerIsVisible = stickerVisibilities[iSticker];
                boolean jStickerIsVisible = stickerVisibilities[jSticker];
                if (!iStickerIsVisible && !jStickerIsVisible)
                    continue;
                int iGroup = iSticker;
                int jGroup = jSticker;
                // Walk upwards until iGroup,jGroup are siblings...
                {
                    while (depths[iGroup] > depths[jGroup])
                        iGroup = parents[iGroup];
                    while (depths[jGroup] > depths[iGroup])
                        jGroup = parents[jGroup];
                    while (parents[iGroup] != parents[jGroup])
                    {
                        iGroup = parents[iGroup];
                        jGroup = parents[jGroup];
                    }
                }

                //
                // See whether things are so inside out
                // that the polygons are facing away from each other...
                // If so, then this polygon should not restrict anything.
                // This can be observed to happen, e.g. on 5,3,3 in default position, with 4d eye distance increased until nothing behind the 4d eye.
                //
                // TODO: this protection may be no longer necessary since we are now computing backfaces
                //       based on unshrunk; not sure!  And, in fact, it may be screwing
                //       some things up (that is, the only thing keeping a particular
                ///      sticker properly behind others are these warped adjacencies).  Revisit.  How?
                if (true)
                {
                    if (iStickerIsVisible && jStickerIsVisible
                     && parents[iSticker] == parents[jSticker]) // XXX floundering
                    {
                        VecMath.vmv(jPolyCenterMinusIPolyCenter,
                                    polyCenters3d[jSticker][jPolyThisSticker],
                                    polyCenters3d[iSticker][iPolyThisSticker]);

                        // we add a tiny bit of slop to make sure we consider
                        // the adjacency valid if the faces are coincident
                        if (VecMath.dot(polyNormals3d[iSticker][iPolyThisSticker], jPolyCenterMinusIPolyCenter) < -1e-3
                         || VecMath.dot(polyNormals3d[jSticker][jPolyThisSticker], jPolyCenterMinusIPolyCenter) > 1e-3)
                        {
                            if (localVerboseLevel >= 1 || returnPartialOrderInfoOptionalForDebugging != null)
                            {
                                System.out.println("HA!  I don't CARE because it's SO WARPED! stickers "+iSticker+"("+iPolyThisSticker+") "+jSticker+"("+jPolyThisSticker+")");
                                System.out.println("    inormal = "+com.donhatchsw.util.Arrays.toStringCompact(polyNormals3d[iSticker][iPolyThisSticker]));
                                System.out.println("    jnormal = "+com.donhatchsw.util.Arrays.toStringCompact(polyNormals3d[jSticker][jPolyThisSticker]));
                                System.out.println("    j-i = "+com.donhatchsw.util.Arrays.toStringCompact(jPolyCenterMinusIPolyCenter));
                                System.out.println("    inormal dot j-i = "+VecMath.dot(polyNormals3d[iSticker][iPolyThisSticker], jPolyCenterMinusIPolyCenter));
                                System.out.println("    jnormal dot j-i = "+VecMath.dot(polyNormals3d[jSticker][jPolyThisSticker], jPolyCenterMinusIPolyCenter));
                            }

                            continue;
                        }
                    }
                }

                boolean inSameFace = sticker2face[iSticker] == sticker2face[jSticker];
                boolean stickerPolyIsStrictlyBackfacing[][] = (inSameFace ? partiallyShrunkStickerPolyIsStrictlyBackfacing : unshrunkStickerPolyIsStrictlyBackfacing);

                if (iGroup == iSticker && jGroup == jSticker)
                {
                    // The two stickers are immediate siblings,
                    // i.e. both in the same (compressed) slice.
                    // This relationship matters only if they are both visible.
                    if (!iStickerIsVisible || !jStickerIsVisible)
                        continue;
                    //System.out.println("    stickers "+iSticker+","+jSticker+" in same slice "+sticker2Slice[iSticker]+"");
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
                        // is not ok:
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
                        if (localVerboseLevel >= 1 || returnPartialOrderInfoOptionalForDebugging != null) System.out.println("WARNING: sticker "+iSticker+"("+iPolyThisSticker+") and "+jSticker+"("+jPolyThisSticker+") both have poly backfacing!!");
                        continue;
                    }
                    else
                    {
                        //System.out.println("phew.");
                    }

                    if (iStickerHasPolyBackfacing)
                    {
                        //add "jSticker < iSticker"
                        partialOrder[partialOrderSize][0] = jSticker;
                        partialOrder[partialOrderSize][1] = iSticker;
                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {jSticker, jSticker, jSticker+1},
                                {iSticker, iSticker, iSticker+1},
                            };
                        }
                        partialOrderSize++;
                    }
                    else if (jStickerHasPolyBackfacing)
                    {
                        //add "iSticker < jSticker"
                        partialOrder[partialOrderSize][0] = iSticker;
                        partialOrder[partialOrderSize][1] = jSticker;
                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {iSticker, iSticker, iSticker+1},
                                {jSticker, jSticker, jSticker+1},
                            };
                        }
                        partialOrderSize++;
                    }
                }
                else if (iGroup == iSticker) // && jGroup != jSticker
                {
                    // iSticker is adjacent to a group containing jSticker.
                    // The group is twisted, so jSticker's
                    // orientation of the poly is not relevant
                    // (in fact jSticker might not even be visible, even if both are visible when stationary);
                    // only iSticker's orientation of the poly is relevant.
                    // XXX I thought that was true but then I got cycles in the 2x hypercube... think about this

                    // XXX oh, it's not true. it might be that the group (containg jSticker) is stationary and the rest of the puzzle (containing iSticker) is moving, and iSticker might not be visible even if both are visible when stationary!!
                    // XXX otoh maybe it's all relative and this is still the correct thing to do?  in that case, need a better description of it.

                    if (!iStickerIsVisible)
                        continue;
                    boolean iStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[iSticker][iPolyThisSticker];
                    if (localVerboseLevel >= 2) System.out.println("    sticker "+iSticker+"("+iPolyThisSticker+") (which is "+(iStickerHasPolyBackfacing ? "backfacing" : "not backfacing")+") is adjacent to sticker "+jSticker+"("+jPolyThisSticker+")'s slice "+sticker2Slice[jSticker]+"");
                    if (iStickerHasPolyBackfacing)
                    {
                        int jIndGroupEndToken = jGroup+1;
                        //add "jIndGroupEndToken < iSticker";
                        partialOrder[partialOrderSize][0] = jIndGroupEndToken;
                        partialOrder[partialOrderSize][1] = iSticker;
                        if (localVerboseLevel >= 2) System.out.println("        so added "+com.donhatchsw.util.Arrays.toStringCompact(partialOrder[partialOrderSize]));

                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {iSticker, iSticker, iSticker+1},
                                {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                            };
                        }
                        partialOrderSize++;
                    }
                    else
                    {
                        int jIndGroupStartToken = jGroup;
                        //add "iSticker < jIndGroupStartToken;
                        partialOrder[partialOrderSize][0] = iSticker;
                        partialOrder[partialOrderSize][1] = jIndGroupStartToken;
                        if (localVerboseLevel >= 2) System.out.println("        so added "+com.donhatchsw.util.Arrays.toStringCompact(partialOrder[partialOrderSize]));
                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {jSticker, jSticker, jSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                {iSticker, iSticker, iSticker+1},
                            };
                        }
                        partialOrderSize++;
                    }
                }
                else if (jGroup == jSticker) // && iGroup != iSticker
                {
                    // same as previous case but reversed
                    if (!jStickerIsVisible)
                        continue;
                    boolean jStickerHasPolyBackfacing = stickerPolyIsStrictlyBackfacing[jSticker][jPolyThisSticker];
                    if (localVerboseLevel >= 2) System.out.println("    sticker "+iSticker+"("+iPolyThisSticker+")'s slice "+sticker2Slice[iSticker]+" is adjacent to sticker "+jSticker+"("+jPolyThisSticker+") (which is "+(jStickerHasPolyBackfacing ? "backfacing" : "not backfacing")+")");
                    if (jStickerHasPolyBackfacing)
                    {
                        int iIndGroupEndToken = iGroup+1;
                        //add "iIndGroupEndToken < jSticker";
                        partialOrder[partialOrderSize][0] = iIndGroupEndToken;
                        partialOrder[partialOrderSize][1] = jSticker;
                        if (localVerboseLevel >= 2) System.out.println("        so added "+com.donhatchsw.util.Arrays.toStringCompact(partialOrder[partialOrderSize]));
                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                                {jSticker, jSticker, jSticker+1},
                            };
                        }
                        partialOrderSize++;
                    }
                    else
                    {
                        int iIndGroupStartToken = iGroup;
                        //add "jSticker < iIndGroupStartToken;
                        partialOrder[partialOrderSize][0] = jSticker;
                        partialOrder[partialOrderSize][1] = iIndGroupStartToken;
                        if (localVerboseLevel >= 2) System.out.println("        so added "+com.donhatchsw.util.Arrays.toStringCompact(partialOrder[partialOrderSize]));
                        if (returnPartialOrderInfoOptionalForDebugging != null) {
                            returnPartialOrderInfoOptionalForDebugging[0][returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]++] = new int[][] {
                                {jSticker, jSticker, jSticker+1},
                                {iSticker, iSticker, iSticker},  // XXX TODO: fix range, maybe.  make it empty for now, just to distinguish from singleton
                            };
                        }
                        partialOrderSize++;
                    }
                }
                else
                {
                    // This would mean the two stickers are adjacent
                    // but the two different groups they are in are not
                    // (That is, one is the left child of the root,
                    // and the other is the right child of the root).
                    // This can't happen.
                    // (Partially due to de-duping of cuts during puzzle creation.)
                    CHECK(false);
                }
            }
        }

        //System.out.println("partial order size = "+partialOrderSize);
        //System.out.println("partial order = "+com.donhatchsw.util.Arrays.toStringCompact(com.donhatchsw.util.Arrays.subarray(partialOrder, 0, partialOrderSize)));

        // can turn this on to get a sense of
        // what it cares about and what it doesn't, if it's screwing up
        boolean doScramble = false;

        //
        // Okay, now we have the partial order.
        // Topsort it into a total order.
        //
        //System.out.println("nStickers = "+nStickers);
        //System.out.println("nNodes = "+nNodes);
        int nComponents = doScramble ? topsorter.topsortRandomized(nNodes, nodeSortOrder,
                                                         partialOrderSize, partialOrder,
                                                         componentStarts,
                                                         randomnessGenerator)
                                     : topsorter.topsort(nNodes, nodeSortOrder,
                                                         partialOrderSize, partialOrder,
                                                         componentStarts);
        int cycleVerboseLevel = 0;
        if (nComponents == nNodes)
        {
            if (cycleVerboseLevel >= 2) System.out.println("  no cycles!");
        }
        else
        {
            int nNontrivialComponents = 0;
            for (int iComponent = 0; iComponent < nComponents; ++iComponent)
            {
                int componentSize = componentStarts[iComponent+1]
                                  - componentStarts[iComponent];
                if (componentSize >= 2)
                    nNontrivialComponents++;
            }
            if (cycleVerboseLevel >= 1) System.out.println("  ARGH! "+nNontrivialComponents+" cycle"+(nNontrivialComponents==1 ? "" : "s")+" in topological depth sort!");

            for (int iComponent = 0; iComponent < nComponents; ++iComponent)
            {
                int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                if (componentSize >= 2)
                {
                    if (cycleVerboseLevel >= 1) System.out.println("    there's a cycle (actually connected component) of length "+componentSize+"");
                    //
                    // z-sort within the strongly connected component
                    //
                    if (true)
                    {
                        com.donhatchsw.util.SortStuff.sort(nodeSortOrder, componentStarts[iComponent], componentSize,
                            new com.donhatchsw.util.SortStuff.IntComparator() { // XXX ALLOCATION! (need to make sort smarter)
                                @Override public int compare(int i, int j)
                                {
                                    // will be too big if it's a group begin or end token;
                                    // just stick those at the end for now, they
                                    // will get deleted soon
                                    boolean iTooBig = i >= nStickers;
                                    boolean jTooBig = j >= nStickers;
                                    if (iTooBig)
                                        if (jTooBig)
                                            return 0;
                                        else
                                            return 1;
                                    else
                                        if (jTooBig)
                                            return -1;

                                    //System.out.println("stickerVisibilities[i="+i+"] = "+stickerVisibilities[i]);
                                    //System.out.println("stickerVisibilities[j="+j+"] = "+stickerVisibilities[j]);
                                    CHECK(stickerVisibilities[i]);
                                    CHECK(stickerVisibilities[j]);

                                    float iZ = stickerCentersZ[i];
                                    float jZ = stickerCentersZ[j];
                                    // sort from increasing z to decreasing! that is because the z's got negated just before the projection!
                                    return iZ > jZ ? -1 :
                                           iZ < jZ ? 1 : 0;
                                }
                            }
                        );
                    }
                }
            }

            // The only further use of partialOrder is for rendering it (if in debug mode).
            // Prune out everything except one cycle from each component.
            if (returnPartialOrderInfoOptionalForDebugging != null)
            {
                int origToSorted[] = VecMath.invertperm(nodeSortOrder, nNodes);
                int successors[][] = new int[nNodes][0];
                for (int iPair = 0; iPair < partialOrder.length; ++iPair)
                {
                    int i = partialOrder[iPair][0];
                    int j = partialOrder[iPair][1];
                    successors[i] = com.donhatchsw.util.Arrays.append(successors[i], j); // potentially O(n^2) but number of successors is usually at most 3 so whatever
                }
                int justTheCycles[][] = new int[partialOrderSize][];
                int justTheCyclesSize = 0;
                for (int iComponent = 0; iComponent < nComponents; ++iComponent)
                {
                    int componentSize = componentStarts[iComponent+1] - componentStarts[iComponent];
                    if (componentSize > 1)
                    {
                        // Okay to be verbose since user is debugging
                        System.out.println("    found a cycle (well at least a connected component) of length "+componentSize+"");

                        int iNode0 = nodeSortOrder[componentStarts[iComponent]];
                        // Progress forward componentSize times,
                        // to make sure we are within where it's going to loop
                        for (int i = 0; i < componentSize; ++i)
                        {
                            int jNode = -1;
                            for (int iSucc = 0; iSucc < successors[iNode0].length; ++iSucc)
                                if (origToSorted[successors[iNode0][iSucc]] >= componentStarts[iComponent]
                                 && origToSorted[successors[iNode0][iSucc]] < componentStarts[iComponent+1])
                                {
                                    jNode = successors[iNode0][iSucc];
                                    break;
                                }
                            CHECK(jNode != -1);
                            iNode0 = jNode;
                        }
                        System.out.print("        "+iNode0+"");
                        for (int iNode = iNode0;;)
                        {
                            int jNode = -1;
                            for (int iSucc = 0; iSucc < successors[iNode].length; ++iSucc)
                                if (origToSorted[successors[iNode][iSucc]] >= componentStarts[iComponent]
                                 && origToSorted[successors[iNode][iSucc]] < componentStarts[iComponent+1])
                                {
                                    jNode = successors[iNode][iSucc];
                                    break;
                                }
                            CHECK(jNode != -1);
                            justTheCycles[justTheCyclesSize++] = new int[]{iNode,jNode};
                            System.out.print(" -> "+jNode+"");

                            iNode = jNode;
                            if (jNode == iNode0)
                                break;
                        }
                        System.out.println();
                        System.out.println("    that actual cycle has length "+justTheCyclesSize);
                    }
                }
                if (true) {
                    justTheCycles = (int[][])com.donhatchsw.util.Arrays.subarray(justTheCycles, 0, justTheCyclesSize);
                    System.out.println("================================");
                    System.out.println("nStickers = "+nStickers);
                    System.out.println("justTheCyclesSize = "+justTheCyclesSize);
                    System.out.println("justTheCycles = "+com.donhatchsw.util.Arrays.toStringCompact(justTheCycles));

                    System.out.print("firstCycle = ( ");
                    int firstCycleSize = 0;
                    for (int i = 0; i < justTheCycles.length; ++i) {
                        firstCycleSize++;
                        int ii = justTheCycles[i][0];
                        String description = ii<nStickers ? ""+ii : ii%2==0 ? ""+ii+"{" : "}"+ii;
                        System.out.print(""+description+" -> ");
                        if (justTheCycles[i][1] == justTheCycles[0][0]) {
                          int ii0 = justTheCycles[0][0];
                          String description0 = ii0<nStickers ? ""+ii0 : ii0%2==0 ? ""+ii0+"{" : "}"+ii0;
                          System.out.println(""+description0+" ) of length "+firstCycleSize);
                          break;
                        }
                    }

                    System.out.println("    The slices:");
                    for (int iSlice = 0; iSlice < nCompressedSlices; ++iSlice) {
                        int groupBeginTokenIndex = nStickers + 2*iSlice;
                        int groupEndTokenIndex = nStickers + 2*iSlice+1;
                        System.out.print("        slice "+iSlice+"/"+nCompressedSlices+": begin token "+groupBeginTokenIndex+", end token "+groupEndTokenIndex+": ");
                        for (int iSticker = 0; iSticker < nStickers; ++iSticker) {
                            CHECK((parents[iSticker] == groupBeginTokenIndex) == (sticker2Slice[iSticker]==iSlice));
                            if (parents[iSticker] == groupBeginTokenIndex) {
                                System.out.print(" "+iSticker);
                            }
                        }
                        System.out.println();
                        int parentSliceBeginTokenIndex = parents[groupBeginTokenIndex];
                        int parentSlice;
                        if (parentSliceBeginTokenIndex == -1)
                            parentSlice = -1;
                        else {
                            CHECK((parentSliceBeginTokenIndex-nStickers)%2 == 0);
                            parentSlice = (parentSliceBeginTokenIndex-nStickers)/2;
                        }
                        System.out.println("            parent slice = "+parentSlice);
                    }
                    System.out.println("================================");
                }
            }
        }
        if (returnPartialOrderInfoOptionalForDebugging != null) {
            returnPartialOrderInfoOptionalForDebugging[0] = (int[][][])com.donhatchsw.util.Arrays.subarray(returnPartialOrderInfoOptionalForDebugging[0], 0, returnPartialOrderInfoOptionalForDebuggingSizeHolder[0]);
        }

        //
        // Compress out the group start and end tokens
        // which we no longer care about
        //
        int nCompressedSorted = 0;
        for (int iSorted = 0; iSorted < nNodes; ++iSorted)
        {
            int iNode = nodeSortOrder[iSorted];
            if (iNode < nStickers)
            {
                int iSticker = iNode;
                if (stickerVisibilities[iSticker]) // XXX hmm, this is checked in the caller too... maybe we don't need to check it here and in fact we don't need to return a count
                    returnStickerSortOrder[nCompressedSorted++] = iSticker;
            }
        }

        if (localVerboseLevel >= 1) System.out.println("    out sortStickersBackToFront (NOT bold new way), returning nCompressedSorted="+nCompressedSorted);
        return nCompressedSorted;
    } // sortStickersBackToFront

    // general utility; could go elsewhere
    private static int sortAndCompressPartialOrder(int partialOrderSize, int[][/*2*/] partialOrder)
    {
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
                if (i == 0 || !VecMath.equals(partialOrder[i], partialOrder[i-1])) {
                    partialOrder[nOut++] = partialOrder[i];
                }
            }
            partialOrderSize = nOut;
        }
        return partialOrderSize;
    }  // sortAndCompressPartialOrder

} // class VeryCleverPaintersSortingOfStickers

