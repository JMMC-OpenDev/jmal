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
                && check("diameter + 2.0 * width", (diameter + 2.0 * width), maxDist)
                && ((axisRatio <= 1d) || check("(diameter + 2.0 * width) * axisRatio",
                        ((diameter + 2.0 * width) * axisRatio), maxDist));
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
            return FourierFunctions.computeRing(
                    Functions.transformU(ufreq, vfreq, axisRatio, cosBeta, sinBeta),
                    Functions.transformV(ufreq, vfreq, cosBeta, sinBeta),
                    diameter, width);
        }
        return FourierFunctions.computeRing(ufreq, vfreq, diameter, width);
    }

    /**
     * Compute the solid angle of this object for black-body variants only.
     * No unit ~ area as unscaled by distance.
     * Solid angle ~ difference of ellipse surfaces (larger - minor ones).
     *
     * @return solid angle value ~ (larger - smaller) ellipse surface
     */
    @Override
    public double computeSolidAngle() {
        return isGray() ? 1.0 : Functions.computeEllipseSurface(diameter + 2.0 * width, axisRatio)
                                - Functions.computeEllipseSurface(diameter, axisRatio);
    }
}
