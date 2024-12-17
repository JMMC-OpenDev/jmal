/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

/**
 * This class computes the Fourier transform at frequencies (UFREQ,VFREQ) of a gaussian object.
 *
 * @author Laurent BOURGES.
 */
public class GaussianFunction extends DiskFunction {

    /**
     * Public constructor
     */
    public GaussianFunction() {
        super();
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
            return FourierFunctions.computeGaussian(
                    Functions.transformU(ufreq, vfreq, axisRatio, cosBeta, sinBeta),
                    Functions.transformV(ufreq, vfreq, cosBeta, sinBeta),
                    diameter);
        }
        return FourierFunctions.computeGaussian(ufreq, vfreq, diameter);
    }

    /**
     * Compute the solid angle of this object for black-body variants only.
     * No unit ~ area as unscaled by distance.
     * Solid angle ~ ellipse surface.
     *
     * @return solid angle value ~ ellipse surface
     */
    @Override
    public double computeSolidAngle() {
        // diameter is fwhm:
        return isGray() ? 1.0 : Functions.computeEllipseSurface(diameter, axisRatio);
    }
}
