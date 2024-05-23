/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model;

import fr.jmmc.jmal.model.function.math.FluxFunction;
import fr.jmmc.jmal.model.function.math.PunctFunction;

/**
 * This class holds several variables used during model computations per function ...
 *
 * @author bourgesl
 */
public final class FunctionComputeContext {

    /* members */
    /** uv frequency count used to preallocate arrays */
    private final int freqCount;
    /** flux function to compute */
    private final FluxFunction fluxFunction;
    /** model function to compute */
    private final PunctFunction modelFunction;
    /* output */
    /** flux contribution (normalized in prepareModels) */
    private final double[] flux;

    /**
     * Protected constructor
     *
     * @param freqCount uv frequency count used to preallocate arrays
     * @param fluxFunction flux function to compute
     * @param modelFunction model function to compute
     */
    FunctionComputeContext(final int freqCount, final FluxFunction fluxFunction, final PunctFunction modelFunction) {
        this.freqCount = freqCount;

        this.modelFunction = modelFunction;
        this.fluxFunction = fluxFunction;

        flux = new double[freqCount];
    }

    /**
     * Return the uv frequency count
     *
     * @return uv frequency count
     */
    public int getFreqCount() {
        return freqCount;
    }

    /**
     * Return the flux function to compute
     *
     * @return flux function to compute
     */
    public FluxFunction getFluxFunction() {
        return fluxFunction;
    }

    /**
     * Return the model function to compute
     *
     * @return model function to compute
     */
    PunctFunction getModelFunction() {
        return modelFunction;
    }

    /* outputs */
    /**
     * Return the flux contribution
     * @return flux contribution (normalized)
     */
    public double[] getFlux() {
        return flux;
    }

    @Override
    public String toString() {
        return "FunctionComputeContext{" + "fluxFunction=" + fluxFunction + ", modelFunction=" + modelFunction + '}';
    }

}
