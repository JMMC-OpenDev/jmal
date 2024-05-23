/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

/**
 * This class computes the black-body flux for the temperature (kelvin) at wavelength of an object.
 *
 * @author Laurent BOURGES.
 */
public final class BlackBodyFunction extends FluxFunction {

    /* members */
    /** temperature of the object */
    private double temperature;

    /**
     * Public constructor
     */
    public BlackBodyFunction() {
        super();
    }

    /**
     * Define the temperature of the object
     * @param temperature temperature in kelvin
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * Compute the flux at wavelength of this object
     *
     * @param wavelength wavelength (m)
     * @return flux (no unit)
     */
    @Override
    public double computeFlux(final double wavelength) {
        // Note: bandwidth not used as no integration of the planck law within channel:
        return flux_weight * Functions.computePlanck(wavelength, temperature);
    }

    @Override
    public String toString() {
        return "BlackBodyFunction{" + "temperature=" + temperature + '}';
    }

}
