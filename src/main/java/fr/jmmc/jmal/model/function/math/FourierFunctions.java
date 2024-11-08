/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.model.function.math;

import cern.jet.math.Bessel;
import fr.jmmc.jmal.complex.MutableComplex;
import static fr.jmmc.jmal.model.function.math.Functions.PI_MAS2RAD;
import static fr.jmmc.jmal.model.function.math.Functions.TWO_PI_MAS2RAD;
import fr.jmmc.jmal.util.MathUtils;
import net.jafama.FastMath;

/**
 * @author Laurent BOURGES.
 */
public final class FourierFunctions {

    /** constant used to compute the gaussian model */
    public final static double GAUSS_CST_INV = 1d / (4d * Math.log(2d));

    /**
     * Forbidden constructor
     */
    private FourierFunctions() {
        super();
    }

    /**
     * shift(ufreq, vfreq, x, y)
     *
     * Returns the complex value applied in the Fourier transform at frequencies
     * (UFREQ,VFREQ) to account for a shift (X,Y) in image space of the given value.
     * X, Y are given in milliarcseconds.
     *
     * @param ufreq UFREQ
     * @param vfreq VFREQ
     * @param zero flag to indicate that x = 0 and y = 0
     * @param x X (mas)
     * @param y Y (mas)
     * @param value value to shift
     * @param output complex Fourier transform value
     */
    public static void shift(final double ufreq, final double vfreq,
                             final boolean zero, final double x, final double y,
                             final double value, final MutableComplex output) {
        if (zero) {
            // update output complex instance (mutable):
            output.updateComplex(value, 0d);
        } else {
            final double phase = TWO_PI_MAS2RAD * (x * ufreq + y * vfreq);
            // update output complex instance (mutable):
            output.updateComplex(value * FastMath.cos(phase), -value * FastMath.sin(phase));
        }
    }

    /**
     * Compute the punct model function for a single UV point
     *
     * @return Fourier transform value
     */
    public static double computePunct() {
        return 1.0;
    }

    /**
     * Compute the circle model function for a single UV point
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @param diameter diameter of the circle model given in milliarcsecond
     * @return Fourier transform value
     */
    public static double computeCircle(final double ufreq, final double vfreq,
                                       final double diameter) {

        final double d = PI_MAS2RAD * diameter * MathUtils.carthesianNorm(ufreq, vfreq);

        double g;
        if (d == 0D) {
            g = 1D;
        } else {
            g = Bessel.j0(d);
        }
        return g;
    }

    /**
     * Compute the disk model function for a single frequency (used by SearchCal GUI)
     *
     * @param freq spatial frequency in rad-1
     * @param diameter diameter of the uniform disk object given in milliarcsecond
     * @return Fourier transform value
     */
    public static double computeDisk(final double freq, final double diameter) {

        final double d = PI_MAS2RAD * diameter * freq;

        double g;
        if (d == 0D) {
            g = 1D;
        } else {
            g = 2D * Bessel.j1(d) / d;
        }

        return g;
    }

    /**
     * Compute the disk model function error for a single frequency (used by SearchCal GUI)
     *
     * @param freq spatial frequency in rad-1
     * @param diameter diameter of the uniform disk object given in milliarcsecond
     * @param diameterError diameter error of the uniform disk object given in milliarcsecond
     * @return Error on Fourier transform value
     */
    public static double computeDiskError(final double freq, final double diameter, final double diameterError) {

        final double d = PI_MAS2RAD * diameter * freq;

        double e;
        if (d == 0D) {
            e = 0D;
        } else {
            e = 2D * Bessel.jn(2, d) * diameterError / diameter;
        }
        return e;
    }

    /**
     * Compute the disk model function for a single UV point
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @param diameter diameter of the uniform disk object given in milliarcsecond
     * @return Fourier transform value
     */
    public static double computeDisk(final double ufreq, final double vfreq,
                                     final double diameter) {

        return computeDisk(MathUtils.carthesianNorm(ufreq, vfreq), diameter);
    }

    /**
     * Compute the ring model function for a single UV point
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @param diameter diameter of the uniform ring object given in milliarcsecond
     * @param width width of the uniform ring object given in milliarcsecond
     * @return Fourier transform value
     */
    public static double computeRing(final double ufreq, final double vfreq,
                                     final double diameter, final double width) {

        if (width == 0d) {
            // infinitely thin ring, i.e. a circle.
            return computeCircle(ufreq, vfreq, diameter);
        }
        if (diameter == 0d) {
            // disk of radius width.
            return computeDisk(ufreq, vfreq, 2d * width);
        }

        final double radius = 0.5d * diameter;
        final double alpha = 1d + width / radius;
        final double r = PI_MAS2RAD * radius * MathUtils.carthesianNorm(ufreq, vfreq);

        double g;
        if (r == 0D) {
            g = 1D;
        } else {
            g = ((alpha * Bessel.j1(2d * alpha * r) / r) - (Bessel.j1(2d * r) / r)) / (alpha * alpha - 1d);
        }
        return g;
    }

    /**
     * Compute the gaussian model function for a single UV point
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @param fwhm full width at half maximum of the gaussian object given in milliarcsecond (diameter like)
     * @return Fourier transform value
     */
    public static double computeGaussian(final double ufreq, final double vfreq,
                                         final double fwhm) {

        final double f = PI_MAS2RAD * fwhm;
        final double d = -f * f * GAUSS_CST_INV * (ufreq * ufreq + vfreq * vfreq);

        double g;
        if (d == 0D) {
            g = 1D;
        } else {
            g = FastMath.exp(d);
        }
        return g;
    }

    /**
     * Compute the center-to-limb darkened disk model function for a single UV point :
     * o(mu) = 1 - A1(1-mu) - A2(1-mu)^2.
     *
     * @param ufreq U frequency in rad-1
     * @param vfreq V frequency in rad-1
     * @param diameter diameter of the disk object given in milliarcsecond
     * @param a1 first coefficient of the quadratic law
     * @param a2 second coefficient of the quadratic law
     * @return Fourier transform value
     */
    public static double computeLimbQuadratic(final double ufreq, final double vfreq,
                                              final double diameter, final double a1, final double a2) {
        /*
         * 11- Limb darkened Disk
         * g(u,v) = ( a*(j1/x) + b* sqrt(PI/2)*(J3over2/x**1.5) + 2*c*(j2/x**2) )/s
         * where
         * BesselJ[3/2, z] == (Sqrt[2/Pi] ((-z) Cos[z] + Sin[z]))/z^(3/2)
         * x=pi*diametre*q
         * j1=J1(x)
         * j2=J2(x)
         * j3over2(x)=sqrt((2/PI)/x)*((sin(x)/x)-cos(x))
         * a=1-cu-cv
         * b=cu+2*cv
         * c=-cv
         * s=a/2+b/3+c/4
         * q**2 = u**2+v**2
         */

        final double d = PI_MAS2RAD * diameter * MathUtils.carthesianNorm(ufreq, vfreq);

        double g;
        if (d == 0D) {
            g = 1D;
        } else {
            final double a = 1d - a1 - a2;
            final double b = a1 + 2d * a2;
            final double c = -a2;
            final double s = a / 2d + b / 3d + c / 4d;

            // Note : BesselJ[3/2, z] == (Sqrt[2/Pi] ((-z) Cos[z] + Sin[z]))/z^(3/2)
            // BesselJ[3/2, d] * Sqrt(Pi/2) :
            final double term2 = (FastMath.sin(d) / d - FastMath.cos(d)) / Math.sqrt(d);

            g = (a * Bessel.j1(d) / d + b * term2 / FastMath.pow(d, 1.5d) + 2d * c * Bessel.jn(2, d) / (d * d)) / s;
        }
        return g;
    }
}
