/*
 * ***** BEGIN LICENSE BLOCK ***** Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is Piotr Wendykier, Emory
 * University. Portions created by the Initial Developer are Copyright (C)
 * 2007-2009 the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or the
 * GNU Lesser General Public License Version 2.1 or later (the "LGPL"), in which
 * case the provisions of the GPL or the LGPL are applicable instead of those
 * above. If you wish to allow use of your version of this file only under the
 * terms of either the GPL or the LGPL, and not to allow others to use your
 * version of this file under the terms of the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and other
 * provisions required by the GPL or the LGPL. If you do not delete the
 * provisions above, a recipient may use your version of this file under the
 * terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK *****
 */

package edu.emory.mathcs.jtransforms.fft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * <p>
 * This is a test of the class {@link DoubleFFT_2D}. In this test, a very crude
 * 2d FFT method is implemented (see {@link #complexForward(double[][])}),
 * assuming that {@link DoubleFFT_1D} has been fully tested and validated. This
 * crude (unoptimized) method is then used to establish <em>expected</em> values
 * of <em>direct</em> Fourier transforms.
 * </p>
 * <p>
 * For <em>inverse</em> Fourier transforms, the test assumes that the
 * corresponding <em>direct</em> Fourier transform has been tested and
 * validated.
 * </p>
 * <p>
 * In all cases, the test consists in creating a random array of data, and
 * verifying that expected and actual values of its Fourier transform coincide
 * (L2 norm is zero, within a specified accuracy).
 * </p>
 *
 * @author S&eacute;bastien Brisard
 *
 */
@RunWith(value = Parameterized.class)
public class DoubleFFT_2DTest {
    /** Base message of all exceptions. */
    public static final String DEFAULT_MESSAGE = "%d-threaded FFT of size %dx%d: ";

    /** The constant value of the seed of the random generator. */
    public static final int SEED = 20110602;

    @Parameters
    public static Collection<Object[]> getParameters() {
        final int[] size = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 32,
                64, 100, 120, 128, 256, 310, 511, 512, 1024 };

        final ArrayList<Object[]> parameters = new ArrayList<Object[]>();

        for (int i = 0; i < size.length; i++) {
            for (int j = 0; j < size.length; j++) {
                parameters.add(new Object[] { size[i], size[j], 1, SEED });
                parameters.add(new Object[] { size[i], size[j], 4, SEED });
            }
        }
        return parameters;
    }

    /** Fourier transform of the columns. */
    private final DoubleFFT_1D cfft;

    /** The object to be tested. */
    private final DoubleFFT_2D fft;

    /** Number of columns of the data arrays to be Fourier transformed. */
    private final int numCols;

    /** Number of rows of the data arrays to be Fourier transformed. */
    private final int numRows;

    /** Fourier transform of the rows. */
    private final DoubleFFT_1D rfft;

    /** For the generation of the data arrays. */
    private final Random random;

    /**
     * Creates a new instance of this test.
     *
     * @param numRows
     *            number of rows
     * @param numColumns
     *            number of columns
     * @param numThreads
     *            the number of threads to be used
     * @param seed
     *            the seed of the random generator
     */
    public DoubleFFT_2DTest(final int numRows, final int numColumns,
            final int numThreads, final long seed) {
        this.numRows = numRows;
        this.numCols = numColumns;
        this.rfft = new DoubleFFT_1D(numColumns);
        this.cfft = new DoubleFFT_1D(numRows);
        this.fft = new DoubleFFT_2D(numRows, numColumns);
        this.random = new Random(seed);
        ConcurrencyUtils.setNumberOfThreads(numThreads);
        ConcurrencyUtils.setThreadsBeginN_2D(4);
    }

    /**
     * A crude implementation of 2d complex FFT.
     *
     * @param a
     *            the data to be transformed
     */
    public void complexForward(final double[][] a) {
        for (int r = 0; r < numRows; r++) {
            rfft.complexForward(a[r]);
        }
        final double[] buffer = new double[2 * numRows];
        for (int c = 0; c < numCols; c++) {
            for (int r = 0; r < numRows; r++) {
                buffer[2 * r] = a[r][2 * c];
                buffer[2 * r + 1] = a[r][2 * c + 1];
            }
            cfft.complexForward(buffer);
            for (int r = 0; r < numRows; r++) {
                a[r][2 * c] = buffer[2 * r];
                a[r][2 * c + 1] = buffer[2 * r + 1];
            }
        }
    }

    public FloatingPointEqualityChecker createEqualityChecker(final double rel,
            final double abs) {
        final String msg = String.format(DEFAULT_MESSAGE,
                ConcurrencyUtils.getNumberOfThreads(), numRows, numCols);
        return new FloatingPointEqualityChecker(msg, rel, abs, 0f, 0f);
    }

    /** A test of {@link DoubleFFT_2D#complexForward(double[])}. */
    @Test
    public void testComplexForward1dInput() {
        final FloatingPointEqualityChecker checker = createEqualityChecker(
                Math.ulp(1d), 0d);
        final double[] actual = new double[2 * numRows * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double rnd = random.nextDouble();
                actual[2 * r * numCols + c] = rnd;
                expected[r][c] = rnd;
            }
        }
        fft.complexForward(actual);
        complexForward(expected);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[2 * r * numCols + c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /** A test of {@link DoubleFFT_2D#complexForward(double[][])}. */
    @Test
    public void testComplexForward2dInput() {
        final FloatingPointEqualityChecker checker = createEqualityChecker(
                Math.ulp(1d), 0d);
        final double[][] actual = new double[numRows][2 * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][c] = rnd;
            }
        }
        fft.complexForward(actual);
        complexForward(expected);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#complexInverse(double[], boolean)}, with
     * the second parameter set to <code>true</code>.
     */
    @Test
    public void testComplexInverseScaled1dInput() {
        double rel = 1E-8;
        double x0 = 5E-8;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] expected = new double[2 * numRows * numCols];
        final double[] actual = new double[2 * numRows * numCols];
        for (int i = 0; i < actual.length; i++) {
            final double rnd = random.nextDouble();
            actual[i] = rnd;
            expected[i] = rnd;
        }
        fft.complexForward(actual);
        fft.complexInverse(actual, true);
        for (int i = 0; i < actual.length; i++) {
            final double exp = expected[i];
            final double act = actual[i];
            checker.assertEquals("[" + i + "]", exp, act);
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#complexInverse(double[][], boolean)}, with
     * the second parameter set to <code>true</code>.
     */
    @Test
    public void testComplexInverseScaled2dInput() {
        double rel = 1E-8;
        double x0 = 5E-8;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] expected = new double[numRows][2 * numCols];
        final double[][] actual = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][c] = rnd;
            }
        }
        fft.complexForward(actual);
        fft.complexInverse(actual, true);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#complexInverse(double[], boolean)}, with
     * the second parameter set to <code>false</code>.
     */
    @Test
    public void testComplexInverseUnScaled1dInput() {
        double rel = 1E-8;
        double x0 = 5E-8;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] expected = new double[2 * numRows * numCols];
        final double[] actual = new double[2 * numRows * numCols];
        for (int i = 0; i < actual.length; i++) {
            final double rnd = random.nextDouble();
            actual[i] = rnd;
            expected[i] = rnd;
        }
        fft.complexForward(actual);
        fft.complexInverse(actual, false);
        final double s = numRows * numCols;
        for (int i = 0; i < actual.length; i++) {
            final double exp = s * expected[i];
            final double act = actual[i];
            checker.assertEquals("[" + i + "]", exp, act);
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#complexInverse(double[][], boolean)}, with
     * the second parameter set to <code>false</code>.
     */
    @Test
    public void testComplexInverseUnScaled2dInput() {
        double rel = 1E-8;
        double x0 = 5E-8;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] expected = new double[numRows][2 * numCols];
        final double[][] actual = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double rnd = random.nextDouble();
                expected[r][c] = rnd;
                actual[r][c] = rnd;
            }
        }
        fft.complexForward(actual);
        fft.complexInverse(actual, false);
        final double s = numRows * numCols;
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = s * expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /** A test of {@link DoubleFFT_2D#realForward(double[])}. */
    @Test
    public void testRealForward1dInput() {
        if (!ConcurrencyUtils.isPowerOf2(numRows)) {
            return;
        }
        if (!ConcurrencyUtils.isPowerOf2(numCols)) {
            return;
        }
        double rel = 1E-8;
        double x0 = 5E-8;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] actual = new double[numRows * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        final boolean[] checked = new boolean[numRows * numCols];
        Arrays.fill(checked, false);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r * numCols + c] = rnd;
                expected[r][2 * c] = rnd;
                expected[r][2 * c + 1] = 0d;
            }
        }
        fft.realForward(actual);
        complexForward(expected);
        int index;
        double exp, act;
        for (int r = 1; r < numRows; r++) {
            for (int c = 2; c < numCols; c++) {
                exp = expected[r][c];
                index = r * numCols + c;
                act = actual[index];
                checker.assertEquals("[" + index + "]", exp, act);
                checked[index] = true;
            }
        }

        for (int c = 2; c < numCols; c++) {
            exp = expected[0][c];
            act = actual[c];
            checker.assertEquals("[0][" + c + "]", exp, act);
            checked[c] = true;
        }

        for (int r = 1; r < numRows / 2; r++) {
            exp = expected[r][0];
            index = r * numCols;
            act = actual[r * numCols];
            checker.assertEquals("[" + index + "]", exp, act);
            checked[index] = true;

            exp = expected[r][1];
            index = r * numCols + 1;
            act = actual[index];
            checker.assertEquals("[" + index + "]", exp, act);
            checked[index] = true;

            exp = expected[numRows - r][numCols];
            index = (numRows - r) * numCols + 1;
            act = actual[index];
            checker.assertEquals("[" + index + "]", exp, act);
            checked[index] = true;

            exp = expected[numRows - r][numCols + 1];
            index = (numRows - r) * numCols;
            act = actual[index];
            checker.assertEquals("[" + index + "]", exp, act);
            checked[index] = true;
        }

        exp = expected[0][0];
        act = actual[0];
        checker.assertEquals("[0]", exp, act);
        checked[0] = true;

        exp = expected[0][numCols];
        act = actual[1];
        checker.assertEquals("[1]", exp, act);
        checked[1] = true;

        exp = expected[numRows / 2][0];
        index = (numRows / 2) * numCols;
        act = actual[index];
        checker.assertEquals("[" + index + "]", exp, act);
        checked[index] = true;

        exp = expected[numRows / 2][numCols];
        index = (numRows / 2) * numCols + 1;
        act = actual[index];
        checker.assertEquals("[" + (numRows / 2) + "][" + numCols + "]", exp,
                act);
        checked[index] = true;

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                index = r * numCols + c;
                Assert.assertTrue(String.format("[%d]", index), checked[index]);
            }
        }
    }

    /** A test of {@link DoubleFFT_2D#realForward(double[][])}. */
    @Test
    public void testRealForward2dInput() {
        if (!ConcurrencyUtils.isPowerOf2(numRows)) {
            return;
        }
        if (!ConcurrencyUtils.isPowerOf2(numCols)) {
            return;
        }
        double rel = 1E-8;
        double x0 = 5E-8;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] actual = new double[numRows][numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        final boolean[][] checked = new boolean[numRows][numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][2 * c] = rnd;
                expected[r][2 * c + 1] = 0.0;
                checked[r][c] = false;
            }
        }
        fft.realForward(actual);
        complexForward(expected);
        double exp, act;

        for (int r = 1; r < numRows; r++) {
            for (int c = 2; c < numCols; c++) {
                exp = expected[r][c];
                act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
                checked[r][c] = true;
            }
        }

        for (int c = 2; c < numCols; c++) {
            exp = expected[0][c];
            act = actual[0][c];
            checker.assertEquals("[0][" + c + "]", exp, act);
            checked[0][c] = true;
        }

        for (int r = 1; r < numRows / 2; r++) {
            exp = expected[r][0];
            act = actual[r][0];
            checker.assertEquals("[" + r + "][0]", exp, act);
            checked[r][0] = true;

            exp = expected[r][1];
            act = actual[r][1];
            checker.assertEquals("[" + r + "][1]", exp, act);
            checked[r][1] = true;

            exp = expected[numRows - r][numCols];
            act = actual[numRows - r][1];
            checker.assertEquals("[" + (numRows - r) + "][1]", exp, act);
            checked[numRows - r][1] = true;

            exp = expected[numRows - r][numCols + 1];
            act = actual[numRows - r][0];
            checker.assertEquals("[" + (numRows - r) + "][0]", exp, act);
            checked[numRows - r][0] = true;
        }

        exp = expected[0][0];
        act = actual[0][0];
        checker.assertEquals("[0][0]", exp, act);
        checked[0][0] = true;

        exp = expected[0][numCols];
        act = actual[0][1];
        checker.assertEquals("[0][1]", exp, act);
        checked[0][1] = true;

        exp = expected[numRows / 2][0];
        act = actual[numRows / 2][0];
        checker.assertEquals("[" + (numRows / 2) + "][0]", exp, act);
        checked[numRows / 2][0] = true;

        exp = expected[numRows / 2][numCols];
        act = actual[numRows / 2][1];
        checker.assertEquals("[" + (numRows / 2) + "][1]", exp, act);
        checked[numRows / 2][1] = true;

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                Assert.assertTrue(String.format("[%d][%d]", r, c),
                        checked[r][c]);
            }
        }
    }

    /** A test of {@link DoubleFFT_2D#realForwardFull(double[])}. */
    @Test
    public void testRealForwardFull1dInput() {
        double rel = 1E-7;
        double x0 = 5E-6;
        if (numCols == 511) {
            x0 = 5E-5;
        }
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] actual = new double[2 * numRows * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r * numCols + c] = rnd;
                expected[r][2 * c] = rnd;
                expected[r][2 * c + 1] = 0d;
            }
        }
        fft.realForwardFull(actual);
        complexForward(expected);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final int index = 2 * r * numCols + c;
                final double act = actual[index];
                checker.assertEquals("[" + index + "]", exp, act);
            }
        }
    }

    /** A test of {@link DoubleFFT_2D#realForwardFull(double[][])}. */
    @Test
    public void testRealForwardFull2dInput() {
        double rel = 1E-7;
        double x0 = 5E-6;
        if (numCols == 511) {
            x0 = 5E-5;
        }
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] actual = new double[numRows][2 * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][2 * c] = rnd;
                expected[r][2 * c + 1] = 0d;
            }
        }
        fft.realForwardFull(actual);
        complexForward(expected);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#realInverseFull(double[], boolean)}, with
     * the second parameter set to <code>true</code>.
     */
    @Test
    public void testRealInverseFullScaled1dInput() {
        final double rel = 1E-14;
        final double abs = 1E-15;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] actual = new double[2 * numRows * numCols];
        final double[] expected = new double[2 * numRows * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                final int index = r * numCols + c;
                actual[index] = rnd;
                expected[2 * index] = rnd;
                expected[2 * index + 1] = 0d;
            }
        }
        // TODO If the two following lines are permuted, this causes an array
        // index out of bounds exception.
        fft.complexInverse(expected, true);
        fft.realInverseFull(actual, true);
        for (int i = 0; i < actual.length; i++) {
            final double exp = expected[i];
            final double act = actual[i];
            checker.assertEquals("[" + i + "]", exp, act);
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#realInverseFull(double[][], boolean)}, with
     * the second parameter set to <code>true</code>.
     */
    @Test
    public void testRealInverseFullScaled2dInput() {
        final double rel = 1E-14;
        final double abs = 1E-15;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] actual = new double[numRows][2 * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][2 * c] = rnd;
                expected[r][2 * c + 1] = 0d;
            }
        }
        fft.realInverseFull(actual, true);
        fft.complexInverse(expected, true);

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#realInverseFull(double[], boolean)}, with
     * the second parameter set to <code>false</code>.
     */
    @Test
    public void testRealInverseFullUnscaled1dInput() {
        double rel = 1E-7;
        double x0 = 5E-7;
        if ((numRows == 310) || (numCols == 310)) {
            x0 = 1E-6;
        }
        if ((numRows == 511) || (numCols == 511)) {
            x0 = 5E-5;
        }
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] actual = new double[2 * numRows * numCols];
        final double[] expected = new double[2 * numRows * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                final int index = r * numCols + c;
                actual[index] = rnd;
                expected[2 * index] = rnd;
                expected[2 * index + 1] = 0d;
            }
        }
        // TODO If the two following lines are permuted, this causes an array
        // index out of bounds exception.
        fft.complexInverse(expected, false);
        fft.realInverseFull(actual, false);
        for (int i = 0; i < actual.length; i++) {
            final double exp = expected[i];
            final double act = actual[i];
            checker.assertEquals("[" + i + "]", exp, act);
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#realInverseFull(double[][], boolean)}, with
     * the second parameter set to <code>false</code>.
     */
    @Test
    public void testRealInverseFullUnscaled2dInput() {
        double rel = 1E-7;
        double x0 = 5E-7;
        if ((numRows == 310) || (numCols == 310)) {
            x0 = 1E-6;
        }
        if ((numRows == 511) || (numCols == 511)) {
            x0 = 5E-5;
        }
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] actual = new double[numRows][2 * numCols];
        final double[][] expected = new double[numRows][2 * numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][2 * c] = rnd;
                expected[r][2 * c + 1] = 0d;
            }
        }
        fft.realInverseFull(actual, false);
        fft.complexInverse(expected, false);

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < 2 * numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#realInverse(double[], boolean)}, with the
     * second parameter set to <code>true</code>.
     */
    @Test
    public void testRealInverseScaled1dInput() {
        if (!ConcurrencyUtils.isPowerOf2(numRows)) {
            return;
        }
        if (!ConcurrencyUtils.isPowerOf2(numCols)) {
            return;
        }
        double rel = 1E-9;
        double x0 = 1E-14;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[] actual = new double[numRows * numCols];
        final double[] expected = new double[actual.length];
        for (int i = 0; i < actual.length; i++) {
            final double rnd = random.nextDouble();
            actual[i] = rnd;
            expected[i] = rnd;
        }
        fft.realForward(actual);
        fft.realInverse(actual, true);
        for (int i = 0; i < actual.length; i++) {
            final double exp = expected[i];
            final double act = actual[i];
            checker.assertEquals("[" + i + "]", exp, act);
        }
    }

    /**
     * A test of {@link DoubleFFT_2D#realInverse(double[][], boolean)}, with the
     * second parameter set to <code>true</code>.
     */
    @Test
    public void testRealInverseScaled2dInput() {
        if (!ConcurrencyUtils.isPowerOf2(numRows)) {
            return;
        }
        if (!ConcurrencyUtils.isPowerOf2(numCols)) {
            return;
        }
        double rel = 1E-9;
        double x0 = 1E-14;
        final double abs = rel * x0;
        final FloatingPointEqualityChecker checker = createEqualityChecker(rel,
                abs);
        final double[][] actual = new double[numRows][numCols];
        final double[][] expected = new double[numRows][numCols];
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double rnd = random.nextDouble();
                actual[r][c] = rnd;
                expected[r][c] = rnd;
            }
        }
        fft.realForward(actual);
        fft.realInverse(actual, true);
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final double exp = expected[r][c];
                final double act = actual[r][c];
                checker.assertEquals("[" + r + "][" + c + "]", exp, act);
            }
        }
    }
}
