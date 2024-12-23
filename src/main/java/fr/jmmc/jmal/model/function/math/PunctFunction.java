/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the base class of all functions computing the Fourier transform at frequencies (UFREQ,VFREQ) of an object.
 *
 * @author Laurent BOURGES.
 */
public class PunctFunction {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(PunctFunction.class.getName());

    /** x coordinate of the object (mas) */
    protected double x = 0d;
    /** y coordinate of the object (mas) */
    protected double y = 0d;
    /** flag to indicate that x = 0 and y = 0 */
    protected boolean zero;
    /** flag to indicate gray model */
    protected boolean isGray;

    /**
     * Public constructor
     */
    public PunctFunction() {
        super();
    }

    /**
     * Check the function parameters against the given maximum distance.
     *
     * @param maxDist maximum distance in mas
     * @return true if valid; false otherwise
     */
    public boolean check(final double maxDist) {
        return true;
    }

    /**
     * Check the given distance against the given maximum distance.
     * @param name name of the parameter
     * @param dist distance in mas to check
     * @param maxDist maximum distance in mas
     * @return true if valid; false otherwise
     */
    public static boolean check(final String name, final double dist, final double maxDist) {
        if (dist < maxDist) {
            if (logger.isDebugEnabled()) {
                logger.debug("check: valid {} = {} < {}", name, dist, maxDist);
            }
            return true;
        }
        logger.info("check: Invalid {} = {} > {} !", name, dist, maxDist);
        return false;
    }

    public boolean isGray() {
        return isGray;
    }

    public void setGray(boolean isGray) {
        this.isGray = isGray;
    }

    /**
     * Return true when x = 0 and y = 0
     *
     * @return true when x = 0 and y = 0
     */
    public final boolean isZero() {
        return zero;
    }

    /**
     * Update the zero flag i.e. true when x = 0 and y = 0
     */
    private void updateZero() {
        zero = (x == 0d) && (y == 0d);
    }

    /**
     * Return the x coordinate of the object (mas)
     *
     * @return x coordinate of the object (mas)
     */
    public final double getX() {
        return x;
    }

    /**
     * Define the x coordinate of the object (mas)
     *
     * @param x x coordinate of the object (mas)
     */
    public final void setX(final double x) {
        this.x = x;
        updateZero();
    }

    /**
     * Return the y coordinate of the object (mas)
     *
     * @return y coordinate of the object (mas)
     */
    public final double getY() {
        return y;
    }

    /**
     * Define the y coordinate of the object (mas)
     *
     * @param y coordinate of the object (mas)
     */
    public final void setY(final double y) {
        this.y = y;
        updateZero();
    }

    /**
     * Compute the Fourier transform at frequencies (UFREQ,VFREQ) of this object
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @return Fourier transform value
     */
    public double computeWeight(final double ufreq, final double vfreq) {
        return FourierFunctions.computePunct();
    }

    /**
     * Compute the solid angle of this object for black-body variants only.
     * No unit ~ area as unscaled by distance.
     * (NaN by default for BB; 1.0 otherwise)
     *
     * @return solid angle value (NaN by default for BB; 1.0 otherwise)
     */
    public double computeSolidAngle() {
        return isGray() ? 1.0 : Double.NaN;
    }
}
