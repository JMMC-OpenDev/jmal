/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model;

import java.util.List;

/**
 * This class holds several variables used during model computations: model functions ...
 *
 * @author bourgesl
 */
public final class ModelFunctionComputeContext extends ModelComputeContext {

    /* members */
    /** list of function contexts to compute */
    private final List<FunctionComputeContext> modelFunctionContexts;

    /**
     * Copy constructor
     *
     * @param context model compute context
     */
    public ModelFunctionComputeContext(final ModelFunctionComputeContext context) {
        this(context.getFreqCount(), context.getModelFunctionContexts());
    }

    /**
     * Protected constructor
     *
     * @param freqCount uv frequency count used to preallocate arrays
     * @param modelFunctionContexts list of function contexts to compute
     */
    ModelFunctionComputeContext(final int freqCount, final List<FunctionComputeContext> modelFunctionContexts) {
        super(freqCount);
        this.modelFunctionContexts = modelFunctionContexts;
    }

    /**
     * Return the list of function contexts to compute
     *
     * @return list of function contexts to compute
     */
    List<FunctionComputeContext> getModelFunctionContexts() {
        return modelFunctionContexts;
    }
}
