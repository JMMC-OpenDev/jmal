/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.util;

import static fr.jmmc.jmcs.util.NumberUtils.ARG_EPSILON;
import static fr.jmmc.jmcs.util.NumberUtils.PI;
import static fr.jmmc.jmcs.util.NumberUtils.PI_HALF;
import net.jafama.FastMath;

/**
 * Provides several norm functions (2D and 3D)
 * 
 * @author bourgesl
 */
public final class MathUtils {

    private MathUtils() {
        // no-op
    }

    public static double getArgument(final double re, final double im) {
        // check |re| == 0
        final double normRe = Math.abs(re);
        if (normRe == 0.0) {
            return (im >= 0.0) ? PI_HALF : -PI_HALF;
        }
        // check |im| == 0
        final double normIm = Math.abs(im);
        if (normIm == 0.0) {
            return (re >= 0.0) ? 0.0 : PI;
        }
        final double epsilon = ARG_EPSILON * (normRe + normIm);
        // check |re| << |im| or |im| << |re|
        if (normIm <= epsilon) {
            return (re >= 0.0) ? 0.0 : PI;
        }
        // check |im| << |re|
        if (normRe <= epsilon) {
            return (im >= 0.0) ? PI_HALF : -PI_HALF;
        }
        return FastMath.atan2(im, re);
    }

    /**
     * Return the carthesian norm i.e. square root of square sum
     * @param x x value
     * @param y y value
     * @return SQRT(x^2 + y^2)
     */
    public static double carthesianNorm(final double x, final double y) {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Return the carthesian norm i.e. square root of square sum
     * @param x x value
     * @param y y value
     * @param z z value
     * @return SQRT(x^2 + y^2 + z^2)
     */
    public static double carthesianNorm(final double x, final double y, final double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
