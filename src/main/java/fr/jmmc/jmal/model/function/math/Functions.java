/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

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
    /** 2 x PI x MAS2RAD */
    public final static double TWO_PI_MAS2RAD = 2d * PI * MAS2RAD;
    /** PI x MAS2RAD */
    public final static double PI_MAS2RAD = PI * MAS2RAD;
    public final static double C = 299792458.0;
    public final static double H = 6.62606891e-34;
    public final static double K = 1.380658e-23;

    /**
     * Forbidden constructor
     */
    private Functions() {
        super();
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
    
    public static double computeEllipseSurface(final double minorDiameter, final double axisRatio) {
        return (PI_MAS2RAD * 0.25) * axisRatio * Math.pow(minorDiameter, 2.0);
    }
}
