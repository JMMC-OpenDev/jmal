/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific lanSguage governing rights and limitations under the
 * License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package edu.emory.mathcs.jtransforms.fft;

import java.util.concurrent.Future;

import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Computes 2D Discrete Fourier Transform (DFT) of complex and real, single
 * precision data. The sizes of both dimensions can be arbitrary numbers. This
 * is a parallel implementation of split-radix and mixed-radix algorithms
 * optimized for SMP systems. <br>
 * <br>
 * Part of the code is derived from General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html)
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
// Laurent BOURGES: remove strictfp keyword
public final class FloatFFT_2D {

    private final FloatFFT_1D fftColumns;
    private final FloatFFT_1D fftRows;
    private final boolean isPowerOfTwo;
    private final boolean useThreads;
    private final int rows;
    private int columns;
    private float[] t;
    private int oldNthreads;
    private int nt;
    // LBO: empty array to quickly clear temporary arrays in realForwardSubset()
    private float[] emptyArray = null;

    /**
     * Creates new instance of FloatFFT_2D.
     * 
     * @param rows
     *            number of rows
     * @param columns
     *            number of columns
     */
    public FloatFFT_2D(final int rows, final int columns) {
        this(rows, columns, false);
    }

    /**
     * Creates new instance of FloatFFT_2D.
     * 
     * @param rows
     *            number of rows
     * @param columns
     *            number of columns
     * @param useSubSet true to allocate only memory for realForwardSubset
     */
    public FloatFFT_2D(final int rows, final int columns, final boolean useSubSet) {
        if (rows <= 1 || columns <= 1) {
            throw new IllegalArgumentException("rows and columns must be greater than 1");
        }
        this.rows = rows;
        this.columns = columns;

        // fix integer capacity for rows = columns = 65536 !
        this.useThreads = (((long) rows * columns) >= ConcurrencyUtils.getThreadsBeginN_2D());

        if (ConcurrencyUtils.isPowerOf2(rows) && ConcurrencyUtils.isPowerOf2(columns)) {
            isPowerOfTwo = true;

            // subset allocation:
            if (useSubSet) {
                oldNthreads = -1;
                nt = ConcurrencyUtils.getNumberOfThreads() * 2 * rows;
                t = new float[nt];
            } else {
                oldNthreads = ConcurrencyUtils.getNumberOfThreads();
                nt = 8 * oldNthreads * rows;
                if (2 * columns == 4 * oldNthreads) {
                    nt >>= 1;
                } else if (2 * columns < 4 * oldNthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
            }
        } else {
            isPowerOfTwo = false;
        }
        fftRows = new FloatFFT_1D(rows, false);
        if (rows == columns) {
            fftColumns = fftRows;
        } else {
            fftColumns = new FloatFFT_1D(columns, false);
        }
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    /**
     * Computes 2D forward DFT of complex data leaving the result in
     * <code>a</code>. The data is stored in 1D array in row-major order.
     * Complex number is stored as two float values in sequence: the real and
     * imaginary part, i.e. the input array must be of size rows*2*columns. The
     * physical layout of the input data has to be as follows:<br>
     * 
     * <pre>
     * a[k1*2*columns+2*k2] = Re[k1][k2], 
     * a[k1*2*columns+2*k2+1] = Im[k1][k2], 0&lt;=k1&lt;rows, 0&lt;=k2&lt;columns,
     * </pre>
     * 
     * @param a
     *            data to transform
     */
    public void complexForward(final float[] a) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            int oldn2 = columns;
            columns = 2 * columns;
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(0, -1, a, true);
                cdft2d_subth(-1, a, true);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.complexForward(a, r * columns);
                }
                cdft2d_sub(-1, a, true);
            }
            columns = oldn2;
        } else {
            final int rowStride = 2 * columns;
            if ((nthreads > 1) && useThreads && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            for (int r = firstRow; r < lastRow; r++) {
                                fftColumns.complexForward(a, r * rowStride);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = (l == (nthreads - 1)) ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            float[] temp = new float[2 * rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                int idx0 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    int idx1 = 2 * r;
                                    int idx2 = r * rowStride + idx0;
                                    temp[idx1] = a[idx2];
                                    temp[idx1 + 1] = a[idx2 + 1];
                                }
                                fftRows.complexForward(temp);
                                for (int r = 0; r < rows; r++) {
                                    int idx1 = 2 * r;
                                    int idx2 = r * rowStride + idx0;
                                    a[idx2] = temp[idx1];
                                    a[idx2 + 1] = temp[idx1 + 1];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.complexForward(a, r * rowStride);
                }
                float[] temp = new float[2 * rows];
                for (int c = 0; c < columns; c++) {
                    int idx0 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        int idx1 = 2 * r;
                        int idx2 = r * rowStride + idx0;
                        temp[idx1] = a[idx2];
                        temp[idx1 + 1] = a[idx2 + 1];
                    }
                    fftRows.complexForward(temp);
                    for (int r = 0; r < rows; r++) {
                        int idx1 = 2 * r;
                        int idx2 = r * rowStride + idx0;
                        a[idx2] = temp[idx1];
                        a[idx2 + 1] = temp[idx1 + 1];
                    }
                }
            }
        }
    }

    /**
     * Computes 2D forward DFT of complex data leaving the result in
     * <code>a</code>. The data is stored in 2D array. Complex data is
     * represented by 2 float values in sequence: the real and imaginary part,
     * i.e. the input array must be of size rows by 2*columns. The physical
     * layout of the input data has to be as follows:<br>
     * 
     * <pre>
     * a[k1][2*k2] = Re[k1][k2], 
     * a[k1][2*k2+1] = Im[k1][k2], 0&lt;=k1&lt;rows, 0&lt;=k2&lt;columns,
     * </pre>
     * 
     * @param a
     *            data to transform
     */
    public void complexForward(final float[][] a) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            int oldn2 = columns;
            columns = 2 * columns;
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(0, -1, a, true);
                cdft2d_subth(-1, a, true);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.complexForward(a[r]);
                }
                cdft2d_sub(-1, a, true);
            }
            columns = oldn2;
        } else {
            if ((nthreads > 1) && useThreads && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            for (int r = firstRow; r < lastRow; r++) {
                                fftColumns.complexForward(a[r]);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = (l == (nthreads - 1)) ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            float[] temp = new float[2 * rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                int idx1 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    int idx2 = 2 * r;
                                    temp[idx2] = a[r][idx1];
                                    temp[idx2 + 1] = a[r][idx1 + 1];
                                }
                                fftRows.complexForward(temp);
                                for (int r = 0; r < rows; r++) {
                                    int idx2 = 2 * r;
                                    a[r][idx1] = temp[idx2];
                                    a[r][idx1 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.complexForward(a[r]);
                }
                float[] temp = new float[2 * rows];
                for (int c = 0; c < columns; c++) {
                    int idx1 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        int idx2 = 2 * r;
                        temp[idx2] = a[r][idx1];
                        temp[idx2 + 1] = a[r][idx1 + 1];
                    }
                    fftRows.complexForward(temp);
                    for (int r = 0; r < rows; r++) {
                        int idx2 = 2 * r;
                        a[r][idx1] = temp[idx2];
                        a[r][idx1 + 1] = temp[idx2 + 1];
                    }
                }
            }
        }
    }

    /**
     * Computes 2D inverse DFT of complex data leaving the result in
     * <code>a</code>. The data is stored in 1D array in row-major order.
     * Complex number is stored as two float values in sequence: the real and
     * imaginary part, i.e. the input array must be of size rows*2*columns. The
     * physical layout of the input data has to be as follows:<br>
     * 
     * <pre>
     * a[k1*2*columns+2*k2] = Re[k1][k2], 
     * a[k1*2*columns+2*k2+1] = Im[k1][k2], 0&lt;=k1&lt;rows, 0&lt;=k2&lt;columns,
     * </pre>
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     * 
     */
    public void complexInverse(final float[] a, final boolean scale) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            int oldn2 = columns;
            columns = 2 * columns;
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(0, 1, a, scale);
                cdft2d_subth(1, a, scale);
            } else {

                for (int r = 0; r < rows; r++) {
                    fftColumns.complexInverse(a, r * columns, scale);
                }
                cdft2d_sub(1, a, scale);
            }
            columns = oldn2;
        } else {
            final int rowspan = 2 * columns;
            if ((nthreads > 1) && useThreads && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            for (int r = firstRow; r < lastRow; r++) {
                                fftColumns.complexInverse(a, r * rowspan, scale);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = (l == (nthreads - 1)) ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            float[] temp = new float[2 * rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                int idx1 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    int idx2 = 2 * r;
                                    int idx3 = r * rowspan + idx1;
                                    temp[idx2] = a[idx3];
                                    temp[idx2 + 1] = a[idx3 + 1];
                                }
                                fftRows.complexInverse(temp, scale);
                                for (int r = 0; r < rows; r++) {
                                    int idx2 = 2 * r;
                                    int idx3 = r * rowspan + idx1;
                                    a[idx3] = temp[idx2];
                                    a[idx3 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.complexInverse(a, r * rowspan, scale);
                }
                float[] temp = new float[2 * rows];
                for (int c = 0; c < columns; c++) {
                    int idx1 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        int idx2 = 2 * r;
                        int idx3 = r * rowspan + idx1;
                        temp[idx2] = a[idx3];
                        temp[idx2 + 1] = a[idx3 + 1];
                    }
                    fftRows.complexInverse(temp, scale);
                    for (int r = 0; r < rows; r++) {
                        int idx2 = 2 * r;
                        int idx3 = r * rowspan + idx1;
                        a[idx3] = temp[idx2];
                        a[idx3 + 1] = temp[idx2 + 1];
                    }
                }
            }
        }
    }

    /**
     * Computes 2D inverse DFT of complex data leaving the result in
     * <code>a</code>. The data is stored in 2D array. Complex data is
     * represented by 2 float values in sequence: the real and imaginary part,
     * i.e. the input array must be of size rows by 2*columns. The physical
     * layout of the input data has to be as follows:<br>
     * 
     * <pre>
     * a[k1][2*k2] = Re[k1][k2], 
     * a[k1][2*k2+1] = Im[k1][k2], 0&lt;=k1&lt;rows, 0&lt;=k2&lt;columns,
     * </pre>
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     * 
     */
    public void complexInverse(final float[][] a, final boolean scale) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            int oldn2 = columns;
            columns = 2 * columns;

            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(0, 1, a, scale);
                cdft2d_subth(1, a, scale);
            } else {

                for (int r = 0; r < rows; r++) {
                    fftColumns.complexInverse(a[r], scale);
                }
                cdft2d_sub(1, a, scale);
            }
            columns = oldn2;
        } else {
            if ((nthreads > 1) && useThreads && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            for (int r = firstRow; r < lastRow; r++) {
                                fftColumns.complexInverse(a[r], scale);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = (l == (nthreads - 1)) ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {

                        public void run() {
                            float[] temp = new float[2 * rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                int idx1 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    int idx2 = 2 * r;
                                    temp[idx2] = a[r][idx1];
                                    temp[idx2 + 1] = a[r][idx1 + 1];
                                }
                                fftRows.complexInverse(temp, scale);
                                for (int r = 0; r < rows; r++) {
                                    int idx2 = 2 * r;
                                    a[r][idx1] = temp[idx2];
                                    a[r][idx1 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.complexInverse(a[r], scale);
                }
                float[] temp = new float[2 * rows];
                for (int c = 0; c < columns; c++) {
                    int idx1 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        int idx2 = 2 * r;
                        temp[idx2] = a[r][idx1];
                        temp[idx2 + 1] = a[r][idx1 + 1];
                    }
                    fftRows.complexInverse(temp, scale);
                    for (int r = 0; r < rows; r++) {
                        int idx2 = 2 * r;
                        a[r][idx1] = temp[idx2];
                        a[r][idx1 + 1] = temp[idx2 + 1];
                    }
                }
            }
        }
    }

    /**
     * Computes 2D forward DFT of real data leaving the result in <code>a</code>
     * . This method only works when the sizes of both dimensions are
     * power-of-two numbers. The physical layout of the output data is as
     * follows:
     * 
     * <pre>
     * a[k1*columns+2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2], 
     * a[k1*columns+2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2], 
     *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2, 
     * a[2*k2] = Re[0][k2] = Re[0][columns-k2], 
     * a[2*k2+1] = Im[0][k2] = -Im[0][columns-k2], 
     *       0&lt;k2&lt;columns/2, 
     * a[k1*columns] = Re[k1][0] = Re[rows-k1][0], 
     * a[k1*columns+1] = Im[k1][0] = -Im[rows-k1][0], 
     * a[(rows-k1)*columns+1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
     * a[(rows-k1)*columns] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
     *       0&lt;k1&lt;rows/2, 
     * a[0] = Re[0][0], 
     * a[1] = Re[0][columns/2], 
     * a[(rows/2)*columns] = Re[rows/2][0], 
     * a[(rows/2)*columns+1] = Re[rows/2][columns/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     */
    public void realForward(float[] a) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("rows and columns must be power of two numbers");
        } else {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(1, 1, a, true);
                cdft2d_subth(-1, a, true);
                rdft2d_sub(1, a);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realForward(a, r * columns);
                }
                cdft2d_sub(-1, a, true);
                rdft2d_sub(1, a);
            }
        }
    }

    /**
     * Computes 2D forward DFT of real data leaving the result in <code>a</code>
     * . This method only works when the sizes of both dimensions are
     * power-of-two numbers. The physical layout of the output data is as
     * follows:
     * 
     * <pre>
     * a[k1][2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2], 
     * a[k1][2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2], 
     *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2, 
     * a[0][2*k2] = Re[0][k2] = Re[0][columns-k2], 
     * a[0][2*k2+1] = Im[0][k2] = -Im[0][columns-k2], 
     *       0&lt;k2&lt;columns/2, 
     * a[k1][0] = Re[k1][0] = Re[rows-k1][0], 
     * a[k1][1] = Im[k1][0] = -Im[rows-k1][0], 
     * a[rows-k1][1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
     * a[rows-k1][0] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
     *       0&lt;k1&lt;rows/2, 
     * a[0][0] = Re[0][0], 
     * a[0][1] = Re[0][columns/2], 
     * a[rows/2][0] = Re[rows/2][0], 
     * a[rows/2][1] = Re[rows/2][columns/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     */
    public void realForward(float[][] a) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("rows and columns must be power of two numbers");
        } else {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(1, 1, a, true);
                cdft2d_subth(-1, a, true);
                rdft2d_sub(1, a);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realForward(a[r]);
                }
                cdft2d_sub(-1, a, true);
                rdft2d_sub(1, a);
            }
        }
    }

    /**
     * Computes only a subset of the 2D forward DFT of real data (without leaving
     * the result in <code>a</code>).
     * This method only works when the sizes of both dimensions are
     * power-of-two numbers. The physical layout of the output data is as follows:
     * 
     * <pre>
     * a[k1][2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2], 
     * a[k1][2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2], 
     *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2, 
     * a[0][2*k2] = Re[0][k2] = Re[0][columns-k2], 
     * a[0][2*k2+1] = Im[0][k2] = -Im[0][columns-k2], 
     *       0&lt;k2&lt;columns/2, 
     * a[k1][0] = Re[k1][0] = Re[rows-k1][0], 
     * a[k1][1] = Im[k1][0] = -Im[rows-k1][0], 
     * a[rows-k1][1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
     * a[rows-k1][0] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
     *       0&lt;k1&lt;rows/2, 
     * a[0][0] = Re[0][0], 
     * a[0][1] = Re[0][columns/2], 
     * a[rows/2][0] = Re[rows/2][0], 
     * a[rows/2][1] = Re[rows/2][columns/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     * 
     * @param subSize size = rows = columns of the output array containing the subset of the 2D forward DFT (must be an even number)
     * @param inputSize size = rows = columns of the input array a (must be an even number)
     * @param data data to transform
     * @return subset of the 2D forward DFT (power of two) of the given size subSize = rows = columns
     */
    public float[][] realForwardSubset(final int subSize, final int inputSize,
                                       final float[][] data) {
        return realForwardSubset(subSize, inputSize, data, null);
    }

    /**
     * Computes only a subset of the 2D forward DFT of real data (without leaving
     * the result in <code>a</code>).
     * This method only works when the sizes of both dimensions are
     * power-of-two numbers. The physical layout of the output data is as follows:
     * 
     * <pre>
     * a[k1][2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2], 
     * a[k1][2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2], 
     *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2, 
     * a[0][2*k2] = Re[0][k2] = Re[0][columns-k2], 
     * a[0][2*k2+1] = Im[0][k2] = -Im[0][columns-k2], 
     *       0&lt;k2&lt;columns/2, 
     * a[k1][0] = Re[k1][0] = Re[rows-k1][0], 
     * a[k1][1] = Im[k1][0] = -Im[rows-k1][0], 
     * a[rows-k1][1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
     * a[rows-k1][0] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
     *       0&lt;k1&lt;rows/2, 
     * a[0][0] = Re[0][0], 
     * a[0][1] = Re[0][columns/2], 
     * a[rows/2][0] = Re[rows/2][0], 
     * a[rows/2][1] = Re[rows/2][columns/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     * 
     * @param subSize size = rows = columns of the output array containing the subset of the 2D forward DFT (must be an even number)
     * @param inputSize size = rows = columns of the input array a (must be an even number)
     * @param data data to transform
     * @param outputData optional output data[N][M] (N >= subSize & M >= subSize + 2)
     * @return subset of the 2D forward DFT (power of two) of the given size subSize = rows = columns
     */
    public float[][] realForwardSubset(final int subSize, final int inputSize,
                                       final float[][] data,
                                       final float[][] outputData) {
//        System.out.println("realForwardSubset: inputSize = " + inputSize + " - subSize = " + subSize + " - fft rows = " + rows + " - cols = " + columns);

        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("rows and columns must be power of two numbers.");
        }
        if (subSize % 2 != 0) {
            throw new IllegalArgumentException("sub size must be an even number because subSize = number of rows = 2 * (columns / 2).");
        }
        if (inputSize % 2 != 0) {
            throw new IllegalArgumentException("input size must be an even number because input size / 2 = center of image.");
        }
        if (inputSize > subSize) {
            throw new IllegalArgumentException("output size must be larger than input size.");
        }
        if (subSize > rows) {
            throw new IllegalArgumentException("sub size must be larger than fft size.");
        }

        // Test useThreads flag to use then only 1 thread:
        final int nthreads = (!useThreads) ? 1 : ConcurrencyUtils.getNumberOfThreads();

        // fix t array capacity:
        if (oldNthreads != -1) {
            nt = nthreads * 2 * rows;
            t = new float[nt];
            oldNthreads = -1; // to ensure correct allocation next time
        }

        // add 1 column more to compute and store columns/2 values:
        final int subSizeColumns = (subSize < columns) ? subSize + 2 : subSize;

        // Check the given output data:
        final boolean isOutputDataValid = ((outputData != null)
                && (outputData.length >= subSize)
                && (outputData[0] != null)
                && (outputData[0].length >= subSizeColumns));

        // Create new output array (could be given):
        final float[][] output;
        if (isOutputDataValid) {
//            System.out.println("realForwardSubset: reuse outputData ...");
            output = outputData;
        } else {
//            System.out.println("realForwardSubset: output = " + subSize + " x " + subSizeColumns);
            output = new float[subSize][subSizeColumns];
        }

//        System.out.println("realForwardSubset: nthreads = " + nthreads);
//        System.out.println("realForwardSubset: 1D array t[] size = " + t.length);

        
        final int rmul2 = 2 * rows;
        final int idiv2 = inputSize / 2;
        final int rowInOffset = (columns - idiv2);
        final int sdiv2 = subSize / 2;
        final int scdiv2 = subSizeColumns / 2;
        final int rowOutOffset = (columns - sdiv2);

        // emptyData contain 2 * rows or columns used by both rows and columns:
        final float[] emptyData;
        if (emptyArray == null) {
            emptyArray = emptyData = new float[Math.max(rmul2, columns)]; // fixed
        } else {
            emptyData = emptyArray;
        }

        // computation tasks:
        final Runnable[] tasks = new Runnable[nthreads];


        // 1 - Process rows:
//        System.out.println("realForwardSubset: process rows ...");
        /*
         * xdft2d0_subth1(1, 1, a, true);
         */

        // create tasks:
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = columns * i;

            tasks[i] = new Runnable() {

                @Override
                public void run() {

                    /** Get the current thread to check if the computation is interrupted */
                    final Thread currentThread = Thread.currentThread();

                    for (int r = n0; r < inputSize; r += nthreads) {

                        // A - clear complete row:
                        System.arraycopy(emptyData, 0, t, startt, columns);

                        // B - copy input data in t:
                        // NOTE: the fft reference is not the center of the image but (0,0):
                        // it implies to split input image in quadrants and shift them 
                        // to have image at each corners and compute a correct phase i.e. atan2(imaginary/real)
                        
                        if (r < idiv2) {
                            // quadrants 3 and 4 from input image:
                            System.arraycopy(data[r + idiv2], idiv2, t, startt, idiv2);
                            System.arraycopy(data[r + idiv2], 0, t, startt + rowInOffset, idiv2);
                        } else {
                            // quadrants 2 and 1 from input image (inverted):
                            System.arraycopy(data[r - idiv2], idiv2, t, startt, idiv2);
                            System.arraycopy(data[r - idiv2], 0, t, startt + rowInOffset, idiv2);
                        }

                        // C - compute real forward as t contains real data:
                        /*
                         * <pre>
                         * a[2*k] = Re[k], 0&lt;=k&lt;n/2
                         * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
                         * a[1] = Re[n/2]
                         * </pre>
                         */
                        fftColumns.realForward(t, startt);

                        // fast interrupt:
                        if (currentThread.isInterrupted()) {
                            return;
                        }

                        // D - copy data from t to the beginning of output (complex data ie 2*columns):
                        // NOTE: output data contains:
                        // - fft data for quadrants 3 and 4 for 0 < r < idiv2
                        // - fft data for quadrants 2 and 1 for idiv2 < r < inputSize
                        System.arraycopy(t, startt, output[r], 0, subSizeColumns);
                    }
                }
            };
        }

        if (nthreads > 1) {
            // fork and join tasks:
            ConcurrencyUtils.forkAndJoin(tasks);

        } else {
            // execute the single task using the current thread:
            tasks[0].run();
        }

        

        // 2 - Process columns (complex data):
//        System.out.println("realForwardSubset: process columns ...");
        /*
         * cdft2d_subth(-1, a, true);
         */

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = rmul2 * i;

            tasks[i] = new Runnable() {

                @Override
                public void run() {

                    /** Get the current thread to check if the computation is interrupted */
                    final Thread currentThread = Thread.currentThread();

                    int idx2, reIdx, imIdx;
                    float[] oRow;
                    float re, im;

                    // include column/2:
                    for (int c = n0; c < scdiv2; c += nthreads) {
                        reIdx = 2 * c;
                        imIdx = reIdx + 1;
                        /*
                         * <pre>
                         * a[2*k] = Re[k], 0&lt;=k&lt;n/2
                         * a[2*k+1] = Im[k], 0&lt;k&lt;n/2
                         * a[1] = Re[n/2]
                         * </pre>
                         */

                        // A - clear complete column:
                        System.arraycopy(emptyData, 0, t, startt, rmul2);

                        
                        
                        // B - copy input data in t:
                        // NOTE: output data contains:
                        // - fft data for quadrants 3 and 4 for 0 < r < idiv2
                        // - fft data for quadrants 2 and 1 for idiv2 < r < inputSize
                        // copy these data at the beginning and end of each column:
                        
                        // B - copy row data and center rows in t (complex data ie 2*columns):
                        for (int r = 0; r < idiv2; r++) {
                            // copy fft data for quadrants 3 and 4:
                            oRow = output[r];

                            // process array one by one (cache efficiency):
                            re = oRow[reIdx];
                            im = oRow[imIdx];

                            // put fft data at the beginning of the t array:
                            idx2 = startt + 2 * r;

                            t[idx2] = re;
                            t[idx2 + 1] = im;

                            // copy fft data for quadrants 2 and 1:
                            oRow = output[r + idiv2];

                            // process array one by one (cache efficiency):
                            re = oRow[reIdx];
                            im = oRow[imIdx];

                            // put fft data at the end of the t array:
                            idx2 = startt + 2 * (r + rowInOffset);

                            t[idx2] = re;
                            t[idx2 + 1] = im;
                        }

                        // C - compute complex forward as t contains complex data:
                        fftRows.complexForward(t, startt);

                        // fast interrupt:
                        if (currentThread.isInterrupted()) {
                            return;
                        }

                        // D - Fix column 0 directly on t:
                        if (c == 0) {
                            /*
                             * rdft2d_sub(1, a);
                             */

                            // process only subSize / 2 rows:

                            for (int i = 1, idxI, idxJ; i < sdiv2; i++) {
                                idxI = startt + 2 * i;
                                idxJ = startt + rmul2 - 2 * i;

                                t[idxJ] = 0.5f * (t[idxI] - t[idxJ]);
                                t[idxI] -= t[idxJ]; // ie : 0.5f * (t[idxI] + t[idxJ])

                                t[idxJ + 1] = 0.5f * (t[idxI + 1] + t[idxJ + 1]);
                                t[idxI + 1] -= t[idxJ + 1];
                            }
                        }

                        // E - copy data from t(start and end) to output (complex data ie 2*columns):
                        for (int r = 0; r < sdiv2; r++) {
                            idx2 = startt + 2 * r;

                            // process array one by one (cache efficiency):
                            re = t[idx2];
                            im = t[idx2 + 1];

                            oRow = output[r];
                            oRow[reIdx] = re;
                            oRow[imIdx] = im;

                            idx2 = startt + 2 * (r + rowOutOffset);

                            // process array one by one (cache efficiency):
                            re = t[idx2];
                            im = t[idx2 + 1];

                            oRow = output[r + sdiv2];
                            oRow[reIdx] = re;
                            oRow[imIdx] = im;
                        }
                    }
                }
            };
        }

        if (nthreads > 1) {
            // fork and join tasks:
            ConcurrencyUtils.forkAndJoin(tasks);

        } else {
            // execute the single task using the current thread:
            tasks[0].run();
        }


        if (subSize < columns) {
            // fix column zero:
            // Known problem on values at row=rows/2 and col=columns/2 (data symetry on boundaries and missing imaginary part) = acceptable.
            // Solution: fix output[rows][0/1] when rows > 1
            /*
             * a[rows-k1][1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
             * a[rows-k1][0] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
             *       0&lt;k1&lt;rows/2, 
             */
            for (int r = 1, j; r < sdiv2; r++) {
                j = subSize - r;

                output[j][1] = output[r][subSize];
                output[j][0] = -output[r][subSize + 1];
            }

            /*
             * a[0][1] = Re[0][columns/2], 
             */
            output[0][1] = output[0][subSize];

            /*
             * a[rows/2][1] = Re[rows/2][columns/2]
             */
            output[sdiv2][1] = output[sdiv2][subSize]; // quadrant 3


            // Clear column/2 values:
            for (int r = 0; r < subSize; r++) {
                output[r][subSize] = 0f;
                output[r][subSize + 1] = 0f;
            }
        }

        return output;
    }

    /**
     * Computes 2D forward DFT of real data leaving the result in <code>a</code>
     * . This method computes full real forward transform, i.e. you will get the
     * same result as from <code>complexForward</code> called with all imaginary
     * part equal 0. Because the result is stored in <code>a</code>, the input
     * array must be of size rows*2*columns, with only the first rows*columns
     * elements filled with real data. To get back the original data, use
     * <code>complexInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     */
    public void realForwardFull(float[] a) {
        if (isPowerOfTwo) {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(1, 1, a, true);
                cdft2d_subth(-1, a, true);
                rdft2d_sub(1, a);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realForward(a, r * columns);
                }
                cdft2d_sub(-1, a, true);
                rdft2d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealForwardFull(a);
        }
    }

    /**
     * Computes 2D forward DFT of real data leaving the result in <code>a</code>
     * . This method computes full real forward transform, i.e. you will get the
     * same result as from <code>complexForward</code> called with all imaginary
     * part equal 0. Because the result is stored in <code>a</code>, the input
     * array must be of size rows by 2*columns, with only the first rows by
     * columns elements filled with real data. To get back the original data,
     * use <code>complexInverse</code> on the output of this method.
     * 
     * @param a
     *            data to transform
     */
    public void realForwardFull(float[][] a) {
        if (isPowerOfTwo) {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth1(1, 1, a, true);
                cdft2d_subth(-1, a, true);
                rdft2d_sub(1, a);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realForward(a[r]);
                }
                cdft2d_sub(-1, a, true);
                rdft2d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealForwardFull(a);
        }
    }

    /**
     * Computes 2D inverse DFT of real data leaving the result in <code>a</code>
     * . This method only works when the sizes of both dimensions are
     * power-of-two numbers. The physical layout of the input data has to be as
     * follows:
     * 
     * <pre>
     * a[k1*columns+2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2], 
     * a[k1*columns+2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2], 
     *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2, 
     * a[2*k2] = Re[0][k2] = Re[0][columns-k2], 
     * a[2*k2+1] = Im[0][k2] = -Im[0][columns-k2], 
     *       0&lt;k2&lt;columns/2, 
     * a[k1*columns] = Re[k1][0] = Re[rows-k1][0], 
     * a[k1*columns+1] = Im[k1][0] = -Im[rows-k1][0], 
     * a[(rows-k1)*columns+1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
     * a[(rows-k1)*columns] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
     *       0&lt;k1&lt;rows/2, 
     * a[0] = Re[0][0], 
     * a[1] = Re[0][columns/2], 
     * a[(rows/2)*columns] = Re[rows/2][0], 
     * a[(rows/2)*columns+1] = Re[rows/2][columns/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use <code>realInverseFull</code>.
     * 
     * @param a
     *            data to transform
     * 
     * @param scale
     *            if true then scaling is performed
     */
    public void realInverse(float[] a, boolean scale) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("rows and columns must be power of two numbers");
        } else {
            int nthreads;
            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                rdft2d_sub(-1, a);
                cdft2d_subth(1, a, scale);
                xdft2d0_subth1(1, -1, a, scale);
            } else {
                rdft2d_sub(-1, a);
                cdft2d_sub(1, a, scale);
                for (int r = 0; r < rows; r++) {
                    fftColumns.realInverse(a, r * columns, scale);
                }
            }
        }
    }

    /**
     * Computes 2D inverse DFT of real data leaving the result in <code>a</code>
     * . This method only works when the sizes of both dimensions are
     * power-of-two numbers. The physical layout of the input data has to be as
     * follows:
     * 
     * <pre>
     * a[k1][2*k2] = Re[k1][k2] = Re[rows-k1][columns-k2], 
     * a[k1][2*k2+1] = Im[k1][k2] = -Im[rows-k1][columns-k2], 
     *       0&lt;k1&lt;rows, 0&lt;k2&lt;columns/2, 
     * a[0][2*k2] = Re[0][k2] = Re[0][columns-k2], 
     * a[0][2*k2+1] = Im[0][k2] = -Im[0][columns-k2], 
     *       0&lt;k2&lt;columns/2, 
     * a[k1][0] = Re[k1][0] = Re[rows-k1][0], 
     * a[k1][1] = Im[k1][0] = -Im[rows-k1][0], 
     * a[rows-k1][1] = Re[k1][columns/2] = Re[rows-k1][columns/2], 
     * a[rows-k1][0] = -Im[k1][columns/2] = Im[rows-k1][columns/2], 
     *       0&lt;k1&lt;rows/2, 
     * a[0][0] = Re[0][0], 
     * a[0][1] = Re[0][columns/2], 
     * a[rows/2][0] = Re[rows/2][0], 
     * a[rows/2][1] = Re[rows/2][columns/2]
     * </pre>
     * 
     * This method computes only half of the elements of the real transform. The
     * other half satisfies the symmetry condition. If you want the full real
     * inverse transform, use <code>realInverseFull</code>.
     * 
     * @param a
     *            data to transform
     * 
     * @param scale
     *            if true then scaling is performed
     */
    public void realInverse(float[][] a, boolean scale) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("rows and columns must be power of two numbers");
        } else {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                rdft2d_sub(-1, a);
                cdft2d_subth(1, a, scale);
                xdft2d0_subth1(1, -1, a, scale);
            } else {
                rdft2d_sub(-1, a);
                cdft2d_sub(1, a, scale);
                for (int r = 0; r < rows; r++) {
                    fftColumns.realInverse(a[r], scale);
                }
            }
        }
    }

    /**
     * Computes 2D inverse DFT of real data leaving the result in <code>a</code>
     * . This method computes full real inverse transform, i.e. you will get the
     * same result as from <code>complexInverse</code> called with all imaginary
     * part equal 0. Because the result is stored in <code>a</code>, the input
     * array must be of size rows*2*columns, with only the first rows*columns
     * elements filled with real data.
     * 
     * @param a
     *            data to transform
     * 
     * @param scale
     *            if true then scaling is performed
     */
    public void realInverseFull(float[] a, boolean scale) {
        if (isPowerOfTwo) {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth2(1, -1, a, scale);
                cdft2d_subth(1, a, scale);
                rdft2d_sub(1, a);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realInverse2(a, r * columns, scale);
                }
                cdft2d_sub(1, a, scale);
                rdft2d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealInverseFull(a, scale);
        }
    }

    /**
     * Computes 2D inverse DFT of real data leaving the result in <code>a</code>
     * . This method computes full real inverse transform, i.e. you will get the
     * same result as from <code>complexInverse</code> called with all imaginary
     * part equal 0. Because the result is stored in <code>a</code>, the input
     * array must be of size rows by 2*columns, with only the first rows by
     * columns elements filled with real data.
     * 
     * @param a
     *            data to transform
     * 
     * @param scale
     *            if true then scaling is performed
     */
    public void realInverseFull(float[][] a, boolean scale) {
        if (isPowerOfTwo) {
            int nthreads;

            nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = 8 * nthreads * rows;
                if (columns == 4 * nthreads) {
                    nt >>= 1;
                } else if (columns < 4 * nthreads) {
                    nt >>= 2;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                xdft2d0_subth2(1, -1, a, scale);
                cdft2d_subth(1, a, scale);
                rdft2d_sub(1, a);
            } else {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realInverse2(a[r], 0, scale);
                }
                cdft2d_sub(1, a, scale);
                rdft2d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealInverseFull(a, scale);
        }
    }

    private void mixedRadixRealForwardFull(final float[][] a) {
        final int n2d2 = columns / 2 + 1;
        final float[][] temp = new float[n2d2][2 * rows];

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && useThreads && (rows >= nthreads) && (n2d2 - 2 >= nthreads)) {
            Future<?>[] futures = new Future[nthreads];
            int p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int i = firstRow; i < lastRow; i++) {
                            fftColumns.realForward(a[i]);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r][0]; //first column is always real
            }
            fftRows.realForwardFull(temp[0]);

            p = (n2d2 - 2) / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstColumn = 1 + l * p;
                final int lastColumn = (l == (nthreads - 1)) ? n2d2 - 1 : firstColumn + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int c = firstColumn; c < lastColumn; c++) {
                            int idx2 = 2 * c;
                            for (int r = 0; r < rows; r++) {
                                int idx1 = 2 * r;
                                temp[c][idx1] = a[r][idx2];
                                temp[c][idx1 + 1] = a[r][idx2 + 1];
                            }
                            fftRows.complexForward(temp[c]);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r][1];
                    //imaginary part = 0;
                }
                fftRows.realForwardFull(temp[n2d2 - 1]);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = n2d2 - 1;
                    temp[idx2][idx1] = a[r][2 * idx2];
                    temp[idx2][idx1 + 1] = a[r][1];
                }
                fftRows.complexForward(temp[n2d2 - 1]);

            }

            p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx1 = 2 * r;
                            for (int c = 0; c < n2d2; c++) {
                                int idx2 = 2 * c;
                                a[r][idx2] = temp[c][idx1];
                                a[r][idx2 + 1] = temp[c][idx1 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstRow = 1 + l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx3 = rows - r;
                            for (int c = n2d2; c < columns; c++) {
                                int idx1 = 2 * c;
                                int idx2 = 2 * (columns - c);
                                a[0][idx1] = a[0][idx2];
                                a[0][idx1 + 1] = -a[0][idx2 + 1];
                                a[r][idx1] = a[idx3][idx2];
                                a[r][idx1 + 1] = -a[idx3][idx2 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {
            for (int r = 0; r < rows; r++) {
                fftColumns.realForward(a[r]);
            }

            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r][0]; //first column is always real
            }
            fftRows.realForwardFull(temp[0]);

            for (int c = 1; c < n2d2 - 1; c++) {
                int idx2 = 2 * c;
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    temp[c][idx1] = a[r][idx2];
                    temp[c][idx1 + 1] = a[r][idx2 + 1];
                }
                fftRows.complexForward(temp[c]);
            }

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r][1];
                    //imaginary part = 0;
                }
                fftRows.realForwardFull(temp[n2d2 - 1]);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = n2d2 - 1;
                    temp[idx2][idx1] = a[r][2 * idx2];
                    temp[idx2][idx1 + 1] = a[r][1];
                }
                fftRows.complexForward(temp[n2d2 - 1]);

            }

            for (int r = 0; r < rows; r++) {
                int idx1 = 2 * r;
                for (int c = 0; c < n2d2; c++) {
                    int idx2 = 2 * c;
                    a[r][idx2] = temp[c][idx1];
                    a[r][idx2 + 1] = temp[c][idx1 + 1];
                }
            }

            //fill symmetric
            for (int r = 1; r < rows; r++) {
                int idx3 = rows - r;
                for (int c = n2d2; c < columns; c++) {
                    int idx1 = 2 * c;
                    int idx2 = 2 * (columns - c);
                    a[0][idx1] = a[0][idx2];
                    a[0][idx1 + 1] = -a[0][idx2 + 1];
                    a[r][idx1] = a[idx3][idx2];
                    a[r][idx1 + 1] = -a[idx3][idx2 + 1];
                }
            }
        }
    }

    private void mixedRadixRealForwardFull(final float[] a) {
        final int rowStride = 2 * columns;
        final int n2d2 = columns / 2 + 1;
        final float[][] temp = new float[n2d2][2 * rows];

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && useThreads && (rows >= nthreads) && (n2d2 - 2 >= nthreads)) {
            Future<?>[] futures = new Future[nthreads];
            int p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int i = firstRow; i < lastRow; i++) {
                            fftColumns.realForward(a, i * columns);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r * columns]; //first column is always real
            }
            fftRows.realForwardFull(temp[0]);

            p = (n2d2 - 2) / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstColumn = 1 + l * p;
                final int lastColumn = (l == (nthreads - 1)) ? n2d2 - 1 : firstColumn + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int c = firstColumn; c < lastColumn; c++) {
                            int idx0 = 2 * c;
                            for (int r = 0; r < rows; r++) {
                                int idx1 = 2 * r;
                                int idx2 = r * columns + idx0;
                                temp[c][idx1] = a[idx2];
                                temp[c][idx1 + 1] = a[idx2 + 1];
                            }
                            fftRows.complexForward(temp[c]);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r * columns + 1];
                    //imaginary part = 0;
                }
                fftRows.realForwardFull(temp[n2d2 - 1]);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = r * columns;
                    int idx3 = n2d2 - 1;
                    temp[idx3][idx1] = a[idx2 + 2 * idx3];
                    temp[idx3][idx1 + 1] = a[idx2 + 1];
                }
                fftRows.complexForward(temp[n2d2 - 1]);
            }

            p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx1 = 2 * r;
                            for (int c = 0; c < n2d2; c++) {
                                int idx0 = 2 * c;
                                int idx2 = r * rowStride + idx0;
                                a[idx2] = temp[c][idx1];
                                a[idx2 + 1] = temp[c][idx1 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstRow = 1 + l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx5 = r * rowStride;
                            int idx6 = (rows - r + 1) * rowStride;
                            for (int c = n2d2; c < columns; c++) {
                                int idx1 = 2 * c;
                                int idx2 = 2 * (columns - c);
                                a[idx1] = a[idx2];
                                a[idx1 + 1] = -a[idx2 + 1];
                                int idx3 = idx5 + idx1;
                                int idx4 = idx6 - idx1;
                                a[idx3] = a[idx4];
                                a[idx3 + 1] = -a[idx4 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            for (int r = 0; r < rows; r++) {
                fftColumns.realForward(a, r * columns);
            }
            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r * columns]; //first column is always real
            }
            fftRows.realForwardFull(temp[0]);

            for (int c = 1; c < n2d2 - 1; c++) {
                int idx0 = 2 * c;
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = r * columns + idx0;
                    temp[c][idx1] = a[idx2];
                    temp[c][idx1 + 1] = a[idx2 + 1];
                }
                fftRows.complexForward(temp[c]);
            }

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r * columns + 1];
                    //imaginary part = 0;
                }
                fftRows.realForwardFull(temp[n2d2 - 1]);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = r * columns;
                    int idx3 = n2d2 - 1;
                    temp[idx3][idx1] = a[idx2 + 2 * idx3];
                    temp[idx3][idx1 + 1] = a[idx2 + 1];
                }
                fftRows.complexForward(temp[n2d2 - 1]);
            }

            for (int r = 0; r < rows; r++) {
                int idx1 = 2 * r;
                for (int c = 0; c < n2d2; c++) {
                    int idx0 = 2 * c;
                    int idx2 = r * rowStride + idx0;
                    a[idx2] = temp[c][idx1];
                    a[idx2 + 1] = temp[c][idx1 + 1];
                }
            }

            //fill symmetric
            for (int r = 1; r < rows; r++) {
                int idx5 = r * rowStride;
                int idx6 = (rows - r + 1) * rowStride;
                for (int c = n2d2; c < columns; c++) {
                    int idx1 = 2 * c;
                    int idx2 = 2 * (columns - c);
                    a[idx1] = a[idx2];
                    a[idx1 + 1] = -a[idx2 + 1];
                    int idx3 = idx5 + idx1;
                    int idx4 = idx6 - idx1;
                    a[idx3] = a[idx4];
                    a[idx3 + 1] = -a[idx4 + 1];
                }
            }
        }
    }

    private void mixedRadixRealInverseFull(final float[][] a, final boolean scale) {
        final int n2d2 = columns / 2 + 1;
        final float[][] temp = new float[n2d2][2 * rows];

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && useThreads && (rows >= nthreads) && (n2d2 - 2 >= nthreads)) {
            Future<?>[] futures = new Future[nthreads];
            int p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int i = firstRow; i < lastRow; i++) {
                            fftColumns.realInverse2(a[i], 0, scale);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r][0]; //first column is always real
            }
            fftRows.realInverseFull(temp[0], scale);

            p = (n2d2 - 2) / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstColumn = 1 + l * p;
                final int lastColumn = (l == (nthreads - 1)) ? n2d2 - 1 : firstColumn + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int c = firstColumn; c < lastColumn; c++) {
                            int idx2 = 2 * c;
                            for (int r = 0; r < rows; r++) {
                                int idx1 = 2 * r;
                                temp[c][idx1] = a[r][idx2];
                                temp[c][idx1 + 1] = a[r][idx2 + 1];
                            }
                            fftRows.complexInverse(temp[c], scale);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r][1];
                    //imaginary part = 0;
                }
                fftRows.realInverseFull(temp[n2d2 - 1], scale);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = n2d2 - 1;
                    temp[idx2][idx1] = a[r][2 * idx2];
                    temp[idx2][idx1 + 1] = a[r][1];
                }
                fftRows.complexInverse(temp[n2d2 - 1], scale);

            }

            p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx1 = 2 * r;
                            for (int c = 0; c < n2d2; c++) {
                                int idx2 = 2 * c;
                                a[r][idx2] = temp[c][idx1];
                                a[r][idx2 + 1] = temp[c][idx1 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstRow = 1 + l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx3 = rows - r;
                            for (int c = n2d2; c < columns; c++) {
                                int idx1 = 2 * c;
                                int idx2 = 2 * (columns - c);
                                a[0][idx1] = a[0][idx2];
                                a[0][idx1 + 1] = -a[0][idx2 + 1];
                                a[r][idx1] = a[idx3][idx2];
                                a[r][idx1 + 1] = -a[idx3][idx2 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {
            for (int r = 0; r < rows; r++) {
                fftColumns.realInverse2(a[r], 0, scale);
            }

            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r][0]; //first column is always real
            }
            fftRows.realInverseFull(temp[0], scale);

            for (int c = 1; c < n2d2 - 1; c++) {
                int idx2 = 2 * c;
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    temp[c][idx1] = a[r][idx2];
                    temp[c][idx1 + 1] = a[r][idx2 + 1];
                }
                fftRows.complexInverse(temp[c], scale);
            }

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r][1];
                    //imaginary part = 0;
                }
                fftRows.realInverseFull(temp[n2d2 - 1], scale);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = n2d2 - 1;
                    temp[idx2][idx1] = a[r][2 * idx2];
                    temp[idx2][idx1 + 1] = a[r][1];
                }
                fftRows.complexInverse(temp[n2d2 - 1], scale);

            }

            for (int r = 0; r < rows; r++) {
                int idx1 = 2 * r;
                for (int c = 0; c < n2d2; c++) {
                    int idx2 = 2 * c;
                    a[r][idx2] = temp[c][idx1];
                    a[r][idx2 + 1] = temp[c][idx1 + 1];
                }
            }

            //fill symmetric
            for (int r = 1; r < rows; r++) {
                int idx3 = rows - r;
                for (int c = n2d2; c < columns; c++) {
                    int idx1 = 2 * c;
                    int idx2 = 2 * (columns - c);
                    a[0][idx1] = a[0][idx2];
                    a[0][idx1 + 1] = -a[0][idx2 + 1];
                    a[r][idx1] = a[idx3][idx2];
                    a[r][idx1 + 1] = -a[idx3][idx2 + 1];
                }
            }
        }
    }

    private void mixedRadixRealInverseFull(final float[] a, final boolean scale) {
        final int rowStride = 2 * columns;
        final int n2d2 = columns / 2 + 1;
        final float[][] temp = new float[n2d2][2 * rows];

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && useThreads && (rows >= nthreads) && (n2d2 - 2 >= nthreads)) {
            Future<?>[] futures = new Future[nthreads];
            int p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int i = firstRow; i < lastRow; i++) {
                            fftColumns.realInverse2(a, i * columns, scale);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r * columns]; //first column is always real
            }
            fftRows.realInverseFull(temp[0], scale);

            p = (n2d2 - 2) / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstColumn = 1 + l * p;
                final int lastColumn = (l == (nthreads - 1)) ? n2d2 - 1 : firstColumn + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int c = firstColumn; c < lastColumn; c++) {
                            int idx0 = 2 * c;
                            for (int r = 0; r < rows; r++) {
                                int idx1 = 2 * r;
                                int idx2 = r * columns + idx0;
                                temp[c][idx1] = a[idx2];
                                temp[c][idx1 + 1] = a[idx2 + 1];
                            }
                            fftRows.complexInverse(temp[c], scale);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r * columns + 1];
                    //imaginary part = 0;
                }
                fftRows.realInverseFull(temp[n2d2 - 1], scale);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = r * columns;
                    int idx3 = n2d2 - 1;
                    temp[idx3][idx1] = a[idx2 + 2 * idx3];
                    temp[idx3][idx1 + 1] = a[idx2 + 1];
                }
                fftRows.complexInverse(temp[n2d2 - 1], scale);
            }

            p = rows / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx1 = 2 * r;
                            for (int c = 0; c < n2d2; c++) {
                                int idx0 = 2 * c;
                                int idx2 = r * rowStride + idx0;
                                a[idx2] = temp[c][idx1];
                                a[idx2 + 1] = temp[c][idx1 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstRow = 1 + l * p;
                final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        for (int r = firstRow; r < lastRow; r++) {
                            int idx5 = r * rowStride;
                            int idx6 = (rows - r + 1) * rowStride;
                            for (int c = n2d2; c < columns; c++) {
                                int idx1 = 2 * c;
                                int idx2 = 2 * (columns - c);
                                a[idx1] = a[idx2];
                                a[idx1 + 1] = -a[idx2 + 1];
                                int idx3 = idx5 + idx1;
                                int idx4 = idx6 - idx1;
                                a[idx3] = a[idx4];
                                a[idx3 + 1] = -a[idx4 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            for (int r = 0; r < rows; r++) {
                fftColumns.realInverse2(a, r * columns, scale);
            }
            for (int r = 0; r < rows; r++) {
                temp[0][r] = a[r * columns]; //first column is always real
            }
            fftRows.realInverseFull(temp[0], scale);

            for (int c = 1; c < n2d2 - 1; c++) {
                int idx0 = 2 * c;
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = r * columns + idx0;
                    temp[c][idx1] = a[idx2];
                    temp[c][idx1 + 1] = a[idx2 + 1];
                }
                fftRows.complexInverse(temp[c], scale);
            }

            if ((columns % 2) == 0) {
                for (int r = 0; r < rows; r++) {
                    temp[n2d2 - 1][r] = a[r * columns + 1];
                    //imaginary part = 0;
                }
                fftRows.realInverseFull(temp[n2d2 - 1], scale);

            } else {
                for (int r = 0; r < rows; r++) {
                    int idx1 = 2 * r;
                    int idx2 = r * columns;
                    int idx3 = n2d2 - 1;
                    temp[idx3][idx1] = a[idx2 + 2 * idx3];
                    temp[idx3][idx1 + 1] = a[idx2 + 1];
                }
                fftRows.complexInverse(temp[n2d2 - 1], scale);
            }

            for (int r = 0; r < rows; r++) {
                int idx1 = 2 * r;
                for (int c = 0; c < n2d2; c++) {
                    int idx0 = 2 * c;
                    int idx2 = r * rowStride + idx0;
                    a[idx2] = temp[c][idx1];
                    a[idx2 + 1] = temp[c][idx1 + 1];
                }
            }

            //fill symmetric
            for (int r = 1; r < rows; r++) {
                int idx5 = r * rowStride;
                int idx6 = (rows - r + 1) * rowStride;
                for (int c = n2d2; c < columns; c++) {
                    int idx1 = 2 * c;
                    int idx2 = 2 * (columns - c);
                    a[idx1] = a[idx2];
                    a[idx1 + 1] = -a[idx2 + 1];
                    int idx3 = idx5 + idx1;
                    int idx4 = idx6 - idx1;
                    a[idx3] = a[idx4];
                    a[idx3 + 1] = -a[idx4 + 1];
                }
            }
        }
    }

    private void rdft2d_sub(int isgn, float[] a) {
        int n1h, j;
        float xi;
        int idx1, idx2;

        n1h = rows >> 1;
        if (isgn < 0) {
            for (int i = 1; i < n1h; i++) {
                j = rows - i;
                idx1 = i * columns;
                idx2 = j * columns;
                xi = a[idx1] - a[idx2];
                a[idx1] += a[idx2];
                a[idx2] = xi;
                xi = a[idx2 + 1] - a[idx1 + 1];
                a[idx1 + 1] += a[idx2 + 1];
                a[idx2 + 1] = xi;
            }
        } else {
            for (int i = 1; i < n1h; i++) {
                j = rows - i;
                idx1 = i * columns;
                idx2 = j * columns;
                a[idx2] = 0.5f * (a[idx1] - a[idx2]);
                a[idx1] -= a[idx2];
                a[idx2 + 1] = 0.5f * (a[idx1 + 1] + a[idx2 + 1]);
                a[idx1 + 1] -= a[idx2 + 1];
            }
        }
    }

    private void rdft2d_sub(int isgn, float[][] a) {
        int n1h, j;
        float xi;

        n1h = rows >> 1;
        if (isgn < 0) {
            for (int i = 1; i < n1h; i++) {
                j = rows - i;
                xi = a[i][0] - a[j][0];
                a[i][0] += a[j][0];
                a[j][0] = xi;
                xi = a[j][1] - a[i][1];
                a[i][1] += a[j][1];
                a[j][1] = xi;
            }
        } else {
            for (int i = 1; i < n1h; i++) {
                j = rows - i;
                a[j][0] = 0.5f * (a[i][0] - a[j][0]);
                a[i][0] -= a[j][0];
                a[j][1] = 0.5f * (a[i][1] + a[j][1]);
                a[i][1] -= a[j][1];
            }
        }
    }

    private void cdft2d_sub(int isgn, float[] a, boolean scale) {
        int idx1, idx2, idx3, idx4, idx5;
        if (isgn == -1) {
            if (columns > 4) {
                for (int c = 0; c < columns; c += 8) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        idx4 = idx3 + 2 * rows;
                        idx5 = idx4 + 2 * rows;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                        t[idx4] = a[idx1 + 4];
                        t[idx4 + 1] = a[idx1 + 5];
                        t[idx5] = a[idx1 + 6];
                        t[idx5 + 1] = a[idx1 + 7];
                    }
                    fftRows.complexForward(t, 0);
                    fftRows.complexForward(t, 2 * rows);
                    fftRows.complexForward(t, 4 * rows);
                    fftRows.complexForward(t, 6 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        idx4 = idx3 + 2 * rows;
                        idx5 = idx4 + 2 * rows;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                        a[idx1 + 4] = t[idx4];
                        a[idx1 + 5] = t[idx4 + 1];
                        a[idx1 + 6] = t[idx5];
                        a[idx1 + 7] = t[idx5 + 1];
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    idx3 = 2 * rows + 2 * r;
                    t[idx2] = a[idx1];
                    t[idx2 + 1] = a[idx1 + 1];
                    t[idx3] = a[idx1 + 2];
                    t[idx3 + 1] = a[idx1 + 3];
                }
                fftRows.complexForward(t, 0);
                fftRows.complexForward(t, 2 * rows);
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    idx3 = 2 * rows + 2 * r;
                    a[idx1] = t[idx2];
                    a[idx1 + 1] = t[idx2 + 1];
                    a[idx1 + 2] = t[idx3];
                    a[idx1 + 3] = t[idx3 + 1];
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    t[idx2] = a[idx1];
                    t[idx2 + 1] = a[idx1 + 1];
                }
                fftRows.complexForward(t, 0);
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    a[idx1] = t[idx2];
                    a[idx1 + 1] = t[idx2 + 1];
                }
            }
        } else {
            if (columns > 4) {
                for (int c = 0; c < columns; c += 8) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        idx4 = idx3 + 2 * rows;
                        idx5 = idx4 + 2 * rows;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                        t[idx4] = a[idx1 + 4];
                        t[idx4 + 1] = a[idx1 + 5];
                        t[idx5] = a[idx1 + 6];
                        t[idx5 + 1] = a[idx1 + 7];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    fftRows.complexInverse(t, 2 * rows, scale);
                    fftRows.complexInverse(t, 4 * rows, scale);
                    fftRows.complexInverse(t, 6 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        idx4 = idx3 + 2 * rows;
                        idx5 = idx4 + 2 * rows;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                        a[idx1 + 4] = t[idx4];
                        a[idx1 + 5] = t[idx4 + 1];
                        a[idx1 + 6] = t[idx5];
                        a[idx1 + 7] = t[idx5 + 1];
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    idx3 = 2 * rows + 2 * r;
                    t[idx2] = a[idx1];
                    t[idx2 + 1] = a[idx1 + 1];
                    t[idx3] = a[idx1 + 2];
                    t[idx3 + 1] = a[idx1 + 3];
                }
                fftRows.complexInverse(t, 0, scale);
                fftRows.complexInverse(t, 2 * rows, scale);
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    idx3 = 2 * rows + 2 * r;
                    a[idx1] = t[idx2];
                    a[idx1 + 1] = t[idx2 + 1];
                    a[idx1 + 2] = t[idx3];
                    a[idx1 + 3] = t[idx3 + 1];
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    t[idx2] = a[idx1];
                    t[idx2 + 1] = a[idx1 + 1];
                }
                fftRows.complexInverse(t, 0, scale);
                for (int r = 0; r < rows; r++) {
                    idx1 = r * columns;
                    idx2 = 2 * r;
                    a[idx1] = t[idx2];
                    a[idx1 + 1] = t[idx2 + 1];
                }
            }
        }
    }

    private void cdft2d_sub(int isgn, float[][] a, boolean scale) {
        int idx2, idx3, idx4, idx5;
        final int rmul2 = 2 * rows;

        if (isgn == -1) {
            if (columns > 4) {
                for (int c = 0; c < columns; c += 8) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = idx2 + rmul2;
                        idx4 = idx3 + rmul2;
                        idx5 = idx4 + rmul2;
                        t[idx2] = a[r][c];
                        t[idx2 + 1] = a[r][c + 1];
                        t[idx3] = a[r][c + 2];
                        t[idx3 + 1] = a[r][c + 3];
                        t[idx4] = a[r][c + 4];
                        t[idx4 + 1] = a[r][c + 5];
                        t[idx5] = a[r][c + 6];
                        t[idx5 + 1] = a[r][c + 7];
                    }
                    fftRows.complexForward(t, 0);
                    fftRows.complexForward(t, rmul2);
                    fftRows.complexForward(t, 2 * rmul2);
                    fftRows.complexForward(t, 3 * rmul2);

                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = idx2 + rmul2;
                        idx4 = idx3 + rmul2;
                        idx5 = idx4 + rmul2;
                        a[r][c] = t[idx2];
                        a[r][c + 1] = t[idx2 + 1];
                        a[r][c + 2] = t[idx3];
                        a[r][c + 3] = t[idx3 + 1];
                        a[r][c + 4] = t[idx4];
                        a[r][c + 5] = t[idx4 + 1];
                        a[r][c + 6] = t[idx5];
                        a[r][c + 7] = t[idx5 + 1];
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    idx3 = idx2 + rmul2;
                    t[idx2] = a[r][0];
                    t[idx2 + 1] = a[r][1];
                    t[idx3] = a[r][2];
                    t[idx3 + 1] = a[r][3];
                }
                fftRows.complexForward(t, 0);
                fftRows.complexForward(t, rmul2);

                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    idx3 = idx2 + rmul2;
                    a[r][0] = t[idx2];
                    a[r][1] = t[idx2 + 1];
                    a[r][2] = t[idx3];
                    a[r][3] = t[idx3 + 1];
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    t[idx2] = a[r][0];
                    t[idx2 + 1] = a[r][1];
                }
                fftRows.complexForward(t, 0);

                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    a[r][0] = t[idx2];
                    a[r][1] = t[idx2 + 1];
                }
            }
        } else {
            if (columns > 4) {
                for (int c = 0; c < columns; c += 8) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = idx2 + rmul2;
                        idx4 = idx3 + rmul2;
                        idx5 = idx4 + rmul2;
                        t[idx2] = a[r][c];
                        t[idx2 + 1] = a[r][c + 1];
                        t[idx3] = a[r][c + 2];
                        t[idx3 + 1] = a[r][c + 3];
                        t[idx4] = a[r][c + 4];
                        t[idx4 + 1] = a[r][c + 5];
                        t[idx5] = a[r][c + 6];
                        t[idx5 + 1] = a[r][c + 7];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    fftRows.complexInverse(t, rmul2, scale);
                    fftRows.complexInverse(t, 2 * rmul2, scale);
                    fftRows.complexInverse(t, 3 * rmul2, scale);

                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = idx2 + rmul2;
                        idx4 = idx3 + rmul2;
                        idx5 = idx4 + rmul2;
                        a[r][c] = t[idx2];
                        a[r][c + 1] = t[idx2 + 1];
                        a[r][c + 2] = t[idx3];
                        a[r][c + 3] = t[idx3 + 1];
                        a[r][c + 4] = t[idx4];
                        a[r][c + 5] = t[idx4 + 1];
                        a[r][c + 6] = t[idx5];
                        a[r][c + 7] = t[idx5 + 1];
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    idx3 = idx2 + rmul2;
                    t[idx2] = a[r][0];
                    t[idx2 + 1] = a[r][1];
                    t[idx3] = a[r][2];
                    t[idx3 + 1] = a[r][3];
                }
                fftRows.complexInverse(t, 0, scale);
                fftRows.complexInverse(t, rmul2, scale);

                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    idx3 = idx2 + rmul2;
                    a[r][0] = t[idx2];
                    a[r][1] = t[idx2 + 1];
                    a[r][2] = t[idx3];
                    a[r][3] = t[idx3 + 1];
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    t[idx2] = a[r][0];
                    t[idx2 + 1] = a[r][1];
                }
                fftRows.complexInverse(t, 0, scale);

                for (int r = 0; r < rows; r++) {
                    idx2 = 2 * r;
                    a[r][0] = t[idx2];
                    a[r][1] = t[idx2 + 1];
                }
            }
        }
    }

    private void xdft2d0_subth1(final int icr, final int isgn, final float[] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows : ConcurrencyUtils.getNumberOfThreads();

        Future<?>[] futures = new Future[nthreads];
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    if (icr == 0) {
                        if (isgn == -1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexForward(a, r * columns);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexInverse(a, r * columns, scale);
                            }
                        }
                    } else {
                        if (isgn == 1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realForward(a, r * columns);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realInverse(a, r * columns, scale);
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void xdft2d0_subth2(final int icr, final int isgn, final float[] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows : ConcurrencyUtils.getNumberOfThreads();

        Future<?>[] futures = new Future[nthreads];
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    if (icr == 0) {
                        if (isgn == -1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexForward(a, r * columns);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexInverse(a, r * columns, scale);
                            }
                        }
                    } else {
                        if (isgn == 1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realForward(a, r * columns);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realInverse2(a, r * columns, scale);
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void xdft2d0_subth1(final int icr, final int isgn, final float[][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows : ConcurrencyUtils.getNumberOfThreads();

        Future<?>[] futures = new Future[nthreads];
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    if (icr == 0) {
                        if (isgn == -1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexForward(a[r]);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexInverse(a[r], scale);
                            }
                        }
                    } else {
                        if (isgn == 1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realForward(a[r]);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realInverse(a[r], scale);
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void xdft2d0_subth2(final int icr, final int isgn, final float[][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows : ConcurrencyUtils.getNumberOfThreads();

        Future<?>[] futures = new Future[nthreads];
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    if (icr == 0) {
                        if (isgn == -1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexForward(a[r]);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.complexInverse(a[r], scale);
                            }
                        }
                    } else {
                        if (isgn == 1) {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realForward(a[r]);
                            }
                        } else {
                            for (int r = n0; r < rows; r += nthreads) {
                                fftColumns.realInverse2(a[r], 0, scale);
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void cdft2d_subth(final int isgn, final float[] a, final boolean scale) {
        int nthread = ConcurrencyUtils.getNumberOfThreads();
        int nt = 8 * rows;
        if (columns == 4 * nthread) {
            nt >>= 1;
        } else if (columns < 4 * nthread) {
            nthread = columns >> 1;
            nt >>= 2;
        }
        Future<?>[] futures = new Future[nthread];
        final int nthreads = nthread;
        for (int i = 0; i < nthread; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    int idx1, idx2, idx3, idx4, idx5;
                    if (isgn == -1) {
                        if (columns > 4 * nthreads) {
                            for (int c = 8 * n0; c < columns; c += 8 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    idx4 = idx3 + 2 * rows;
                                    idx5 = idx4 + 2 * rows;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                    t[idx4] = a[idx1 + 4];
                                    t[idx4 + 1] = a[idx1 + 5];
                                    t[idx5] = a[idx1 + 6];
                                    t[idx5 + 1] = a[idx1 + 7];
                                }
                                fftRows.complexForward(t, startt);
                                fftRows.complexForward(t, startt + 2 * rows);
                                fftRows.complexForward(t, startt + 4 * rows);
                                fftRows.complexForward(t, startt + 6 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    idx4 = idx3 + 2 * rows;
                                    idx5 = idx4 + 2 * rows;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                    a[idx1 + 4] = t[idx4];
                                    a[idx1 + 5] = t[idx4 + 1];
                                    a[idx1 + 6] = t[idx5];
                                    a[idx1 + 7] = t[idx5 + 1];
                                }
                            }
                        } else if (columns == 4 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 4 * n0;
                                idx2 = startt + 2 * r;
                                idx3 = startt + 2 * rows + 2 * r;
                                t[idx2] = a[idx1];
                                t[idx2 + 1] = a[idx1 + 1];
                                t[idx3] = a[idx1 + 2];
                                t[idx3 + 1] = a[idx1 + 3];
                            }
                            fftRows.complexForward(t, startt);
                            fftRows.complexForward(t, startt + 2 * rows);
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 4 * n0;
                                idx2 = startt + 2 * r;
                                idx3 = startt + 2 * rows + 2 * r;
                                a[idx1] = t[idx2];
                                a[idx1 + 1] = t[idx2 + 1];
                                a[idx1 + 2] = t[idx3];
                                a[idx1 + 3] = t[idx3 + 1];
                            }
                        } else if (columns == 2 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 2 * n0;
                                idx2 = startt + 2 * r;
                                t[idx2] = a[idx1];
                                t[idx2 + 1] = a[idx1 + 1];
                            }
                            fftRows.complexForward(t, startt);
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 2 * n0;
                                idx2 = startt + 2 * r;
                                a[idx1] = t[idx2];
                                a[idx1 + 1] = t[idx2 + 1];
                            }
                        }
                    } else {
                        if (columns > 4 * nthreads) {
                            for (int c = 8 * n0; c < columns; c += 8 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    idx4 = idx3 + 2 * rows;
                                    idx5 = idx4 + 2 * rows;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                    t[idx4] = a[idx1 + 4];
                                    t[idx4 + 1] = a[idx1 + 5];
                                    t[idx5] = a[idx1 + 6];
                                    t[idx5 + 1] = a[idx1 + 7];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                fftRows.complexInverse(t, startt + 2 * rows, scale);
                                fftRows.complexInverse(t, startt + 4 * rows, scale);
                                fftRows.complexInverse(t, startt + 6 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    idx4 = idx3 + 2 * rows;
                                    idx5 = idx4 + 2 * rows;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                    a[idx1 + 4] = t[idx4];
                                    a[idx1 + 5] = t[idx4 + 1];
                                    a[idx1 + 6] = t[idx5];
                                    a[idx1 + 7] = t[idx5 + 1];
                                }
                            }
                        } else if (columns == 4 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 4 * n0;
                                idx2 = startt + 2 * r;
                                idx3 = startt + 2 * rows + 2 * r;
                                t[idx2] = a[idx1];
                                t[idx2 + 1] = a[idx1 + 1];
                                t[idx3] = a[idx1 + 2];
                                t[idx3 + 1] = a[idx1 + 3];
                            }
                            fftRows.complexInverse(t, startt, scale);
                            fftRows.complexInverse(t, startt + 2 * rows, scale);
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 4 * n0;
                                idx2 = startt + 2 * r;
                                idx3 = startt + 2 * rows + 2 * r;
                                a[idx1] = t[idx2];
                                a[idx1 + 1] = t[idx2 + 1];
                                a[idx1 + 2] = t[idx3];
                                a[idx1 + 3] = t[idx3 + 1];
                            }
                        } else if (columns == 2 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 2 * n0;
                                idx2 = startt + 2 * r;
                                t[idx2] = a[idx1];
                                t[idx2 + 1] = a[idx1 + 1];
                            }
                            fftRows.complexInverse(t, startt, scale);
                            for (int r = 0; r < rows; r++) {
                                idx1 = r * columns + 2 * n0;
                                idx2 = startt + 2 * r;
                                a[idx1] = t[idx2];
                                a[idx1 + 1] = t[idx2 + 1];
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void cdft2d_subth(final int isgn, final float[][] a, final boolean scale) {
        int nthread = ConcurrencyUtils.getNumberOfThreads();
        int nt = 8 * rows;
        if (columns == 4 * nthread) {
            nt >>= 1;
        } else if (columns < 4 * nthread) {
            nthread = columns >> 1;
            nt >>= 2;
        }
        Future<?>[] futures = new Future[nthread];
        final int nthreads = nthread;
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    int idx2, idx3, idx4, idx5;
                    final int rmul2 = 2 * rows;
                    if (isgn == -1) {
                        if (columns > 4 * nthreads) {
                            for (int c = 8 * n0; c < columns; c += 8 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = idx2 + rmul2;
                                    idx4 = idx3 + rmul2;
                                    idx5 = idx4 + rmul2;
                                    t[idx2] = a[r][c];
                                    t[idx2 + 1] = a[r][c + 1];
                                    t[idx3] = a[r][c + 2];
                                    t[idx3 + 1] = a[r][c + 3];
                                    t[idx4] = a[r][c + 4];
                                    t[idx4 + 1] = a[r][c + 5];
                                    t[idx5] = a[r][c + 6];
                                    t[idx5 + 1] = a[r][c + 7];
                                }
                                fftRows.complexForward(t, startt);
                                fftRows.complexForward(t, startt + rmul2);
                                fftRows.complexForward(t, startt + 2 * rmul2);
                                fftRows.complexForward(t, startt + 3 * rmul2);

                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = idx2 + rmul2;
                                    idx4 = idx3 + rmul2;
                                    idx5 = idx4 + rmul2;
                                    a[r][c] = t[idx2];
                                    a[r][c + 1] = t[idx2 + 1];
                                    a[r][c + 2] = t[idx3];
                                    a[r][c + 3] = t[idx3 + 1];
                                    a[r][c + 4] = t[idx4];
                                    a[r][c + 5] = t[idx4 + 1];
                                    a[r][c + 6] = t[idx5];
                                    a[r][c + 7] = t[idx5 + 1];
                                }
                            }
                        } else if (columns == 4 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                idx3 = idx2 + rmul2;
                                t[idx2] = a[r][4 * n0];
                                t[idx2 + 1] = a[r][4 * n0 + 1];
                                t[idx3] = a[r][4 * n0 + 2];
                                t[idx3 + 1] = a[r][4 * n0 + 3];
                            }
                            fftRows.complexForward(t, startt);
                            fftRows.complexForward(t, startt + rmul2);

                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                idx3 = idx2 + rmul2;
                                a[r][4 * n0] = t[idx2];
                                a[r][4 * n0 + 1] = t[idx2 + 1];
                                a[r][4 * n0 + 2] = t[idx3];
                                a[r][4 * n0 + 3] = t[idx3 + 1];
                            }
                        } else if (columns == 2 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                t[idx2] = a[r][2 * n0];
                                t[idx2 + 1] = a[r][2 * n0 + 1];
                            }
                            fftRows.complexForward(t, startt);

                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                a[r][2 * n0] = t[idx2];
                                a[r][2 * n0 + 1] = t[idx2 + 1];
                            }
                        }
                    } else {
                        if (columns > 4 * nthreads) {
                            for (int c = 8 * n0; c < columns; c += 8 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = idx2 + rmul2;
                                    idx4 = idx3 + rmul2;
                                    idx5 = idx4 + rmul2;
                                    t[idx2] = a[r][c];
                                    t[idx2 + 1] = a[r][c + 1];
                                    t[idx3] = a[r][c + 2];
                                    t[idx3 + 1] = a[r][c + 3];
                                    t[idx4] = a[r][c + 4];
                                    t[idx4 + 1] = a[r][c + 5];
                                    t[idx5] = a[r][c + 6];
                                    t[idx5 + 1] = a[r][c + 7];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                fftRows.complexInverse(t, startt + rmul2, scale);
                                fftRows.complexInverse(t, startt + 2 * rmul2, scale);
                                fftRows.complexInverse(t, startt + 3 * rmul2, scale);

                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = idx2 + rmul2;
                                    idx4 = idx3 + rmul2;
                                    idx5 = idx4 + rmul2;
                                    a[r][c] = t[idx2];
                                    a[r][c + 1] = t[idx2 + 1];
                                    a[r][c + 2] = t[idx3];
                                    a[r][c + 3] = t[idx3 + 1];
                                    a[r][c + 4] = t[idx4];
                                    a[r][c + 5] = t[idx4 + 1];
                                    a[r][c + 6] = t[idx5];
                                    a[r][c + 7] = t[idx5 + 1];
                                }
                            }
                        } else if (columns == 4 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                idx3 = idx2 + rmul2;
                                t[idx2] = a[r][4 * n0];
                                t[idx2 + 1] = a[r][4 * n0 + 1];
                                t[idx3] = a[r][4 * n0 + 2];
                                t[idx3 + 1] = a[r][4 * n0 + 3];
                            }
                            fftRows.complexInverse(t, startt, scale);
                            fftRows.complexInverse(t, startt + rmul2, scale);

                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                idx3 = idx2 + rmul2;
                                a[r][4 * n0] = t[idx2];
                                a[r][4 * n0 + 1] = t[idx2 + 1];
                                a[r][4 * n0 + 2] = t[idx3];
                                a[r][4 * n0 + 3] = t[idx3 + 1];
                            }
                        } else if (columns == 2 * nthreads) {
                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                t[idx2] = a[r][2 * n0];
                                t[idx2 + 1] = a[r][2 * n0 + 1];
                            }
                            fftRows.complexInverse(t, startt, scale);
                            for (int r = 0; r < rows; r++) {
                                idx2 = startt + 2 * r;
                                a[r][2 * n0] = t[idx2];
                                a[r][2 * n0 + 1] = t[idx2 + 1];
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void fillSymmetric(final float[] a) {
        final int twon2 = 2 * columns;
        int idx1, idx2, idx3, idx4;
        int n1d2 = rows / 2;

        for (int r = (rows - 1); r >= 1; r--) {
            idx1 = r * columns;
            idx2 = 2 * idx1;
            for (int c = 0; c < columns; c += 2) {
                a[idx2 + c] = a[idx1 + c];
                a[idx1 + c] = 0;
                a[idx2 + c + 1] = a[idx1 + c + 1];
                a[idx1 + c + 1] = 0;
            }
        }
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && useThreads && (n1d2 >= nthreads)) {
            Future<?>[] futures = new Future[nthreads];
            int l1k = n1d2 / nthreads;
            final int newn2 = 2 * columns;
            for (int i = 0; i < nthreads; i++) {
                final int l1offa, l1stopa, l2offa, l2stopa;
                if (i == 0) {
                    l1offa = i * l1k + 1;
                } else {
                    l1offa = i * l1k;
                }
                l1stopa = i * l1k + l1k;
                l2offa = i * l1k;
                if (i == nthreads - 1) {
                    l2stopa = i * l1k + l1k + 1;
                } else {
                    l2stopa = i * l1k + l1k;
                }
                futures[i] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        int idx1, idx2, idx3, idx4;

                        for (int r = l1offa; r < l1stopa; r++) {
                            idx1 = r * newn2;
                            idx2 = (rows - r) * newn2;
                            idx3 = idx1 + columns;
                            a[idx3] = a[idx2 + 1];
                            a[idx3 + 1] = -a[idx2];
                        }
                        for (int r = l1offa; r < l1stopa; r++) {
                            idx1 = r * newn2;
                            idx3 = (rows - r + 1) * newn2;
                            for (int c = columns + 2; c < newn2; c += 2) {
                                idx2 = idx3 - c;
                                idx4 = idx1 + c;
                                a[idx4] = a[idx2];
                                a[idx4 + 1] = -a[idx2 + 1];

                            }
                        }
                        for (int r = l2offa; r < l2stopa; r++) {
                            idx3 = ((rows - r) % rows) * newn2;
                            idx4 = r * newn2;
                            for (int c = 0; c < newn2; c += 2) {
                                idx1 = idx3 + (newn2 - c) % newn2;
                                idx2 = idx4 + c;
                                a[idx1] = a[idx2];
                                a[idx1 + 1] = -a[idx2 + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (int r = 1; r < n1d2; r++) {
                idx2 = r * twon2;
                idx3 = (rows - r) * twon2;
                a[idx2 + columns] = a[idx3 + 1];
                a[idx2 + columns + 1] = -a[idx3];
            }

            for (int r = 1; r < n1d2; r++) {
                idx2 = r * twon2;
                idx3 = (rows - r + 1) * twon2;
                for (int c = columns + 2; c < twon2; c += 2) {
                    a[idx2 + c] = a[idx3 - c];
                    a[idx2 + c + 1] = -a[idx3 - c + 1];

                }
            }
            for (int r = 0; r <= rows / 2; r++) {
                idx1 = r * twon2;
                idx4 = ((rows - r) % rows) * twon2;
                for (int c = 0; c < twon2; c += 2) {
                    idx2 = idx1 + c;
                    idx3 = idx4 + (twon2 - c) % twon2;
                    a[idx3] = a[idx2];
                    a[idx3 + 1] = -a[idx2 + 1];
                }
            }
        }
        a[columns] = -a[1];
        a[1] = 0;
        idx1 = n1d2 * twon2;
        a[idx1 + columns] = -a[idx1 + 1];
        a[idx1 + 1] = 0;
        a[idx1 + columns + 1] = 0;
    }

    private void fillSymmetric(final float[][] a) {
        final int newn2 = 2 * columns;
        int n1d2 = rows / 2;

        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if ((nthreads > 1) && useThreads && (n1d2 >= nthreads)) {
            Future<?>[] futures = new Future[nthreads];
            int l1k = n1d2 / nthreads;
            for (int i = 0; i < nthreads; i++) {
                final int l1offa, l1stopa, l2offa, l2stopa;
                if (i == 0) {
                    l1offa = i * l1k + 1;
                } else {
                    l1offa = i * l1k;
                }
                l1stopa = i * l1k + l1k;
                l2offa = i * l1k;
                if (i == nthreads - 1) {
                    l2stopa = i * l1k + l1k + 1;
                } else {
                    l2stopa = i * l1k + l1k;
                }
                futures[i] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        int idx1, idx2;
                        for (int r = l1offa; r < l1stopa; r++) {
                            idx1 = rows - r;
                            a[r][columns] = a[idx1][1];
                            a[r][columns + 1] = -a[idx1][0];
                        }
                        for (int r = l1offa; r < l1stopa; r++) {
                            idx1 = rows - r;
                            for (int c = columns + 2; c < newn2; c += 2) {
                                idx2 = newn2 - c;
                                a[r][c] = a[idx1][idx2];
                                a[r][c + 1] = -a[idx1][idx2 + 1];

                            }
                        }
                        for (int r = l2offa; r < l2stopa; r++) {
                            idx1 = (rows - r) % rows;
                            for (int c = 0; c < newn2; c = c + 2) {
                                idx2 = (newn2 - c) % newn2;
                                a[idx1][idx2] = a[r][c];
                                a[idx1][idx2 + 1] = -a[r][c + 1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {

            for (int r = 1; r < n1d2; r++) {
                int idx1 = rows - r;
                a[r][columns] = a[idx1][1];
                a[r][columns + 1] = -a[idx1][0];
            }
            for (int r = 1; r < n1d2; r++) {
                int idx1 = rows - r;
                for (int c = columns + 2; c < newn2; c += 2) {
                    int idx2 = newn2 - c;
                    a[r][c] = a[idx1][idx2];
                    a[r][c + 1] = -a[idx1][idx2 + 1];
                }
            }
            for (int r = 0; r <= rows / 2; r++) {
                int idx1 = (rows - r) % rows;
                for (int c = 0; c < newn2; c += 2) {
                    int idx2 = (newn2 - c) % newn2;
                    a[idx1][idx2] = a[r][c];
                    a[idx1][idx2 + 1] = -a[r][c + 1];
                }
            }
        }
        a[0][columns] = -a[0][1];
        a[0][1] = 0;
        a[n1d2][columns] = -a[n1d2][1];
        a[n1d2][1] = 0;
        a[n1d2][columns + 1] = 0;
    }
}
