/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.function.math;

/**
 * This class is the base class of flux functions computing the flux at wavelength of an object.
 *
 * @author Laurent BOURGES.
 */
public class FluxFunction {

    /* members */
    /** intensity coefficient of the object */
    protected double flux_weight;

    /**
     * Public constructor
     */
    public FluxFunction() {
        super();
    }

    /**
     * Define the intensity coefficient of the object
     *
     * @param fluxWeight intensity coefficient of the object
     */
    public final void setFluxWeight(final double fluxWeight) {
        this.flux_weight = fluxWeight;
    }

    /**
     * Compute the flux at wavelength of this object
     *
     * @param wavelength wavelength (m)
     * @return flux (no unit)
     */
    public double computeFlux(final double wavelength) {
        return flux_weight;
    }

    @Override
    public String toString() {
        return "FluxFunction{" + "flux_weight=" + flux_weight + '}';
    }

}
