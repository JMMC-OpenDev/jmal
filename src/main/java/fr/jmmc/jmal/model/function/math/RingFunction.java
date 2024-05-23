/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

/**
 * This class computes the Fourier transform at frequencies (UFREQ,VFREQ) of a ring object.
 *
 * @author Laurent BOURGES.
 */
public class RingFunction extends DiskFunction {

    /** ring width (mas) */
    protected double width;

    /**
     * Public constructor
     */
    public RingFunction() {
        super();
    }

    /**
     * Check the function parameters against the given maximum distance.
     *
     * @param maxDist maximum distance in mas
     * @return true if valid; false otherwise
     */
    @Override
    public boolean check(final double maxDist) {
        return super.check(maxDist)
                && check("diameter + width", (diameter + width), maxDist)
                && ((axisRatio <= 1d) || check("(diameter + width) * axisRatio", ((diameter + width) * axisRatio), maxDist));
    }

    /**
     * Define the width (mas)
     *
     * @param width width (mas)
     */
    public final void setWidth(double width) {
        this.width = width;
    }

    /**
     * Compute the Fourier transform at frequencies (UFREQ,VFREQ) of this object
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @return Fourier transform value
     */
    @Override
    public double computeWeight(final double ufreq, final double vfreq) {
        if (isStreched) {
            // transform UV coordinates :
            return Functions.computeRing(
                    Functions.transformU(ufreq, vfreq, axisRatio, cosBeta, sinBeta),
                    Functions.transformV(ufreq, vfreq, cosBeta, sinBeta),
                    diameter, width);
        }
        return Functions.computeRing(ufreq, vfreq, diameter, width);
    }
}
