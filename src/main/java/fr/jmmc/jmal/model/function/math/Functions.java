/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

import cern.jet.math.Bessel;
import fr.jmmc.jmal.complex.MutableComplex;
import fr.jmmc.jmal.util.MathUtils;
import net.jafama.FastMath;

/**
 * @author Laurent BOURGES.
 */
public final class Functions {

    /* mathematical constants */
    /** _LPB_PI = value of the variable PI, to avoid any corruption */
    public final static double PI = 3.141592653589793238462643383279503D;
    /** _LPB_DEG2RAD = degree to radian conversion factor */
    public final static double DEG2RAD = PI / 180D;
    /** _LPB_MAS2RAD = milliarcsecond to radian conversion factor */
    public final static double MAS2RAD = DEG2RAD / 3600D / 1000D;
    /** constant used to compute the gaussian model */
    private final static double GAUSS_CST_INV = 1d / (4d * Math.log(2d));
    /** 2 x PI x MAS2RAD */
    private final static double TWO_PI_MAS2RAD = 2d * PI * MAS2RAD;
    /** PI x MAS2RAD */
    private final static double PI_MAS2RAD = PI * MAS2RAD;
    private final static double C = 299792458.0;
    private final static double H = 6.62606891e-34;
    private final static double K = 1.380658e-23;

    /**
     * Forbidden constructor
     */
    private Functions() {
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
     * Return the cosinus of the beta angle in degrees
     * beta is the trigonometric angle
     * |y
     * |
     * ---|---> x beta =0 or 180 for y=0, beta = 90 or -90 for x=0)
     * |
     * |
     *
     * The angle ROTATION is the astronomical position angle, |North
     * equal to 0 or 180 for x=0, and 90 or 270 for y=0. |
     * so, ROTATION = 90 - beta ---|--->East
     * the positive x-semi-axis being the Est direction, and |
     * the positive y-semi-axis beeing the North direction. |
     *
     * @param rotation rotation angle in degrees
     * @return cosinus of the beta angle
     */
    public static double getCosBeta(final double rotation) {
        return FastMath.cos(getBeta(rotation));
    }

    /**
     * Return the sinus of the beta angle in degrees
     * beta is the trigonometric angle
     * |y
     * |
     * ---|---> x beta =0 or 180 for y=0, beta = 90 or -90 for x=0)
     * |
     * |
     *
     * The angle ROTATION is the astronomical position angle, |North
     * equal to 0 or 180 for x=0, and 90 or 270 for y=0. |
     * so, ROTATION = 90 - beta ---|--->East
     * the positive x-semi-axis being the Est direction, and |
     * the positive y-semi-axis beeing the North direction. |
     *
     * @param rotation rotation angle in degrees
     * @return sinus of the beta angle
     */
    public static double getSinBeta(final double rotation) {
        return FastMath.sin(getBeta(rotation));
    }

    /**
     * Return the beta angle in degrees
     * beta is the trigonometric angle
     * |y
     * |
     * ---|---> x beta =0 or 180 for y=0, beta = 90 or -90 for x=0)
     * |
     * |
     *
     * The angle ROTATION is the astronomical position angle, |North
     * equal to 0 or 180 for x=0, and 90 or 270 for y=0. |
     * so, ROTATION = 90 - beta ---|--->East
     * the positive x-semi-axis being the Est direction, and |
     * the positive y-semi-axis beeing the North direction. |
     *
     * @param rotation rotation angle in degrees
     * @return beta angle in degrees
     */
    private static double getBeta(final double rotation) {
        return DEG2RAD * (90D - rotation);
    }

    /**
     * Return the new spatial frequency U transform(ufreq, vfreq, t_ana, rotation)
     *
     * Returns the new spatial frequencies when the object has got geometrical
     * transformations, successively a rotation and an anamorphose.
     * (u,v)--> Transpose(Inverse(T))(u,v), with matrix T = HAR;
     * Inverse(R)= |cos(beta) -sin(beta)|
     * |sin(beta) cos(beta)| beta angle in degrees
     * beta is the trigonometric angle
     * |y
     * |
     * ---|---> x beta =0 or 180 for y=0, beta = 90 or -90 for x=0)
     * |
     * |
     *
     * Inverse(A)= |t_ana 0|
     * |0 1| t_ana = ratio of anamorphose, >0
     *
     * The angle ROTATION is the astronomical position angle, |North
     * equal to 0 or 180 for x=0, and 90 or 270 for y=0. |
     * so, ROTATION = 90 - beta ---|--->East
     * the positive x-semi-axis being the Est direction, and |
     * the positive y-semi-axis beeing the North direction. |
     *
     * @see getCosBeta(double)
     * @see getSinBeta(double)
     *
     * @param ufreq UFREQ
     * @param vfreq VFREQ
     * @param anamorphoseRatio t_ana = ratio of anamorphose, >0
     * @param cosBeta cosinus of the beta angle; see getCosBeta(rotation)
     * @param sinBeta sinus of the beta angle; see getSinBeta(rotation)
     * @return new spatial frequency UFREQ
     */
    public static double transformU(final double ufreq, final double vfreq,
                                    final double anamorphoseRatio,
                                    final double cosBeta, final double sinBeta) {

        return ufreq * cosBeta * anamorphoseRatio + vfreq * sinBeta * anamorphoseRatio;
    }

    public static double transformU(final double ufreq, final double vfreq,
                                    final double cosBeta, final double sinBeta) {

        return ufreq * cosBeta + vfreq * sinBeta;
    }

    /**
     * Return the new spatial frequency V
     * transform(ufreq, vfreq, t_ana, rotation)
     *
     * Returns the new spatial frequencies when the object has got geometrical
     * transformations, only a rotation.
     * (u,v)--> Transpose(Inverse(T))(u,v), with matrix T = HAR;
     * Inverse(R)= |cos(beta) -sin(beta)|
     * |sin(beta) cos(beta)| beta angle in degrees
     * beta is the trigonometric angle
     * |y
     * |
     * ---|---> x beta =0 or 180 for y=0, beta = 90 or -90 for x=0)
     * |
     * |
     *
     * The angle ROTATION is the astronomical position angle, |North
     * equal to 0 or 180 for x=0, and 90 or 270 for y=0. |
     * so, ROTATION = 90 - beta ---|--->East
     * the positive x-semi-axis being the Est direction, and |
     * the positive y-semi-axis beeing the North direction. |
     *
     * @see getCosBeta(double)
     * @see getSinBeta(double)
     *
     * @param ufreq UFREQ
     * @param vfreq VFREQ
     * @param cosBeta cosinus of the beta angle; see getCosBeta(rotation)
     * @param sinBeta sinus of the beta angle; see getSinBeta(rotation)
     * @return new spatial frequency VFREQ
     */
    public static double transformV(final double ufreq, final double vfreq,
                                    final double cosBeta, final double sinBeta) {

        return -ufreq * sinBeta + vfreq * cosBeta;
    }

    /* Model functions */
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

    /**
     * Returns the emission curve (spectral radiance) of the black body in
     * W/m^2/sr/m. WAVELENGTH is in meter, and TEMPERATURE is in Kelvin.
     * 
     * B(L,T) = 2 h c^2 / L^5 * 1/(exp(h*c/(k*L*T)-1) ,
     * 
     * where L is the wavelength, T its absolute temperature, k the Boltzmann
     * constant, h the Planck constant, and c the speed of light in the medium.
     * 
     * The spectral radiance must be multiplied by pi to get the emittance
     * (in W/m^2/m), and by 1e-6 to get units in W/m^2/micron.
     * 
     * @param wavelength wavelength to use (m)
     * @param temperature black body temperature (K)
     * @return spectral radiance of a black body (W/m^3/st)
     */
    public static double computePlanck(final double wavelength, final double temperature) {
        final double x = (H * C / K) / (temperature * wavelength);
        return (2.0 * H * C * C) / Math.pow(wavelength, 5.0) / Math.expm1(x);
    }
}
