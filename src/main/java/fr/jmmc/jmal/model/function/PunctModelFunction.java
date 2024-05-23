/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.model.function;

import fr.jmmc.jmal.model.AbstractModelFunction;
import fr.jmmc.jmal.model.WavelengthVariant;
import fr.jmmc.jmal.model.function.math.PunctFunction;
import fr.jmmc.jmal.model.targetmodel.Model;

/**
 * This ModelFunction implements the punct model
 * 
 * @author Laurent BOURGES.
 */
public final class PunctModelFunction extends AbstractModelFunction<PunctFunction> {

    /* Model constants */
    /** punct model description */
    private final static String MODEL_DESC
                                = "Returns the Fourier transform of a punctual object (Dirac function) at coordinates (X,Y) \n"
            + "(milliarcsecond). \n\n"
            + "FLUX_WEIGHT is the intensity coefficient. FLUX_WEIGHT=1 means total energy is 1.";
    /** punct_BB model description */
    private final static String MODEL_DESC_BB
                                = "Returns the Fourier transform multiplied by the relative flux of a blackbody at TEMPERATURE \n"
            + "(in Kelvin) centered at WAVELENGTH (in meters) of a punctual object (Dirac function) \n"
            + "at coordinates (X,Y) (milliarcsecond). \n\n"
            + "FLUX_WEIGHT is the intensity coefficient to define the relative extent of the blackbody component.";

    /**
     * Constructor
     */
    public PunctModelFunction() {
        this(WavelengthVariant.Const);
    }

    /**
     * Constructor for the given wavelength variant
     * @param wlVariant the wavelength variant
     */
    public PunctModelFunction(final WavelengthVariant wlVariant) {
        super(wlVariant);
    }

    /**
     * Return the model type
     * @return model type
     */
    @Override
    public String getType() {
        if (isBlackBody()) {
            return MODEL_PUNCT_BB;
        }
        return MODEL_PUNCT;
    }

    /**
     * Return the model description
     * @return model description
     */
    @Override
    public String getDescription() {
        if (isBlackBody()) {
            return MODEL_DESC_BB;
        }
        return MODEL_DESC;
    }

    /**
     * Return a new Model instance with its parameters and default values
     * @return new Model instance
     */
    @Override
    public Model newModel() {
        final Model model = super.newModel();

        model.setNameAndType(getType());
        model.setDesc(getDescription());

        return model;
    }

    /**
     * Create the computation function for the given model :
     * Get model parameters to fill the function context
     * @param model model instance
     * @return model function
     */
    @Override
    protected PunctFunction createFunction(final Model model) {
        final PunctFunction function = new PunctFunction();

        // Get parameters to fill the context :
        function.setX(getParameterValue(model, PARAM_X));
        function.setY(getParameterValue(model, PARAM_Y));

        return function;
    }
}
