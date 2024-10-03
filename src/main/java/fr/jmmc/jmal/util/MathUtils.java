/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.util;

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
