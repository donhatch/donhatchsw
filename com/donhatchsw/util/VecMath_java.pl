#!/usr/bin/perl -w
use strict;

#
# Generate VecMath.java
#

# XXX here doc! what's the syntax?
print(
"/*
* Copyright (c) 2005 Don Hatch Software
*/
//
// VecMath.prejava
//
// Author: Don Hatch (hatch@plunk.org)
// This code may be used for any purpose as long as it is good and not evil.
//

package com.donhatchsw.util;


#define ROWLENGTH(M) ((M).length==0 ? 0 : (M)[0].length) // XXX still lots of places we should use this and don't

/**
 * Some common operations on vectors and matrices,
 * using an array representation.
 * <p>
 * Most of the vector and matrix arithmetic operations
 * have two forms: a fast version in which the caller supplies
 * the result vector or matrix, and a more convenient but slower version
 * which returns a newly allocated result.
 */

/* not in javadoc, for now */
/*
 * XXX JAVADOC there are some parameter comments that should be converted to javadoc
 */
public final class VecMath
{
    private VecMath() {} // uninstantiatable

");


# 12 versions:
# For each type:  int, float, double
# With and without size argument
# Return a new object, or not

(
    ["vector plus vector", "vpv", "+"],
    ["vector minus vector", "vmv", "-"],
    ["vector times scalar", "vxs", "*"],
    ["scalar times vector", "sxv", "*"],
    ["matrix times scalar", "mxs", "*"],
    ["scalar times matrix", "sxm", "*"],
    ["matrix plus matrix", "mpm", "+"],
    ["matrix minus matrix", "mmm", "-"],
    ["add v to every row of M.", "vpm", "+"],
    ["add v to every row of M.", "mpv", "+"],
    ["add v to every row of M. M can be SCALAR[][], or SCALAR[][][], ...", "vpm", "+"],
    ["add v to every row of M. M can be SCALAR[][], or SCALAR[][][], ...", "mpv", "+"],
    ["subtract v from every row of M.", "mmv", "-"],
    ["subtract v from every row of M. M can be double[][], or double[][][], ...", "mmv", "-"],

    ["copy vector", "copyvec"], # convenience version requires size
    ["copy matrix", "copymat"],
    ["set column of a matrix", "setcolumn"],
    ["get column of a matrix", "getcolumn"],
    ["fill vector with a constant scalar", "fillvec"], # convenience version requires size
    ["fill matrix with a constant scalar", "fillmat"], # convenience version requires size
    ["zero a matrix", "zeromat"], # convenience version requires size
    ["zero a vector", "zerovec"], # convenience version requires size
    ["set matrix to identity", "identitymat"], # convenience version requires size XXX sizes! "return newly allocated identity matrix of given dimension", "return newly allocated identity matrix of given dimensions"
    ["transpose matrix", "transpose"],

    makeRowRotMat XXX YOU ARE HERE

    ["vector dot product", "dot"],
    ["vector distance squared", "distsqrd"],
    ["vector distance", "dist"],
    ["vector norm squared", "normsqrd"],
    ["vector norm", "norm"],
    ["returns index of least element", "mini"],
    ["returns index of greatest element", "maxi"],
    ["returns minimum element", "min"],
    ["returns maximum element", "max"],
    ["sum of elements", "sum"],
    ["average of elements", "average"],
    ["product of elements, not using log (fast)", "productNotUsingLog"],
    ["product of elements, using log to avoid over/underflow (slow)", "productUsingLog"],
    ["geometric average of elements", "geomAverage"],
    ["vector sum of vectors (array rows)", "sum"],
    ["vector average of vectors (array rows)", "average"],
    ["vector sum of indexed list into array of vectors", "sumIndexed"],
    ["vector average of indexed list into array of vectors", "averageIndexed"],
    ["Compute the array of sums-- each element of result\nis the sum of the box starting at index 0,0,...0\nup to and including the given index.\nresult is allowed to be the same as from.", "integrate"],
    ["Inverse of the integrate function.\nresult is allowed to be the same as from.\n"],
    ["bounding box of multidimensional indexed list into an array of points", "bboxIndexed"],
    ["bounding box of an array of points", "bbox"],
    ["uniform (i.e. square, cube, etc.) bounding box of an array of points", "bboxUniform"],
    ["intersect two boxes", "bboxIntersect"],
    ["bounding box of union of two boxes", "bboxUnion"],
    ["whether bbox (including boundary) contains point, within tolerance", "closedBBoxContains"],
    ["whether bbox (excluding boundary) contains point, within tolerance", "bboxInteriorContains"],

    ["row vector times matrix", "vxm"],
    ["matrix times column vector", "vxm"],
    ["matrix times matrix", "mxm"],
    ["row vectors times matrix", "vsxm"],
    ["matrix times column vectors", "mxvs"],

    ["return a random vector with length <= 1, using Math.random()", "random"] # convenience version requires size
    ["return a random vector with length <= 1, using a generator", "random"] # convenience version requires size


    ["perp dot", "xv2"],
    ["determinant of matrix having the two vectors as rows", "vxv2"],
    ["3-dimensional cross product", "vxv3"],
    ["determinant of matrix having the three vectors as rows", "vxvxv3"],
    ["n-dimensional cross product of n-1 n-dimensional vectors", "crossprod"],
    ["vector plus (scalar times vector)", "vpsxv"],
    ["vector plus vector minus vector", "vpvmv"],
    ["(scalar times vector) plus (scalar times vector)", "sxvpsxv"],
    ["(scalar times vector) plus (scalar times vector)", "sxvpsxv"],
    ["||b-a||^2 ||c-a||^2 - (b-a)dot(c-a)", "sqrdTwiceTriangleArea"],
    ["linear interpolation between vectors", "lerp"],
    ["barycentric interpolation between vectors", "bary"],
    ["cubic interpolation, given boundary positions and velocities", "cerp"],
    ["quintic interpolation, given boundary positions, velocities, and\naccelerations.", "quinterp"],

    ["XXX", "luDecompose"],
    ["XXX", "luBackSubstitute"],
    ["invert matrix", "invertmat"],
    ["destructive matrix determinant (destroys the contents of M in the process)", "detDestructive"],
    ["matrix determinant", "det"],
    ["XXX", "invmxv"],
    ["XXX", "invmxm"],
    ["XXX", "vxinvm"],
    ["XXX", "mxinvm"],
    ["square submatrix", "submat"],
)

    /** vector plus vector, initial part */
    public static void vpv(SCALAR result[], SCALAR v0[], SCALAR v1[], int size)
    {
        for (int i = 0; i < size; ++i)
            result[i] = v0[i] + v1[i];
    }
    /** vector plus vector */
    public static void vpv(double result[], double v0[], double v1[])
    {
        vpv(result, v0, result.length)
    }
    /** vector plus vector, initial part, returning newly allocated result */
    public static double[] vpv(double v0[], double v1[], size)
    {
        double result[] = new double[size];
        vpv(result, v0, v1, size);
        return result;
    }
    /** vector plus vector, returning newly allocated result */
    public static double[] vpv(double v0[], double v1[])
    {
        return vpv(v0, v1, v0.length);
    }
