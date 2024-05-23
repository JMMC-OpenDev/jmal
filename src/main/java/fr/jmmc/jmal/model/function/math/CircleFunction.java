/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

/**
 * This class computes the Fourier transform at frequencies (UFREQ,VFREQ) of a circle object.
 *
 * @author Laurent BOURGES.
 */
public class CircleFunction extends PunctFunction {

    /** diameter (mas) */
    protected double diameter;

    /**
     * Public constructor
     */
    public CircleFunction() {
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
        return check("diameter", diameter, maxDist);
    }

    /**
     * Define the diameter (mas)
     *
     * @param diameter diameter (mas)
     */
    public final void setDiameter(final double diameter) {
        this.diameter = diameter;
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
        return Functions.computeCircle(ufreq, vfreq, diameter);
    }
}
