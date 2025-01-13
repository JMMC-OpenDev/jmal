/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model;

import fr.jmmc.jmal.complex.MutableComplex;
import fr.jmmc.jmal.model.function.math.BlackBodyFunction;
import fr.jmmc.jmal.model.function.math.FluxFunction;
import fr.jmmc.jmal.model.function.math.FourierFunctions;
import fr.jmmc.jmal.model.function.math.PunctFunction;
import fr.jmmc.jmal.model.targetmodel.Model;
import fr.jmmc.jmal.model.targetmodel.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <T> type of the function class
 *
 * @author Laurent BOURGES.
 */
public abstract class AbstractModelFunction<T extends PunctFunction> implements ModelFunction {

    /** Class logger */
    protected final static Logger logger = LoggerFactory.getLogger(AbstractModelFunction.class.getName());
    /** enable description in Model elements */
    private static final boolean ENABLE_DESC = "true".equalsIgnoreCase(System.getProperty("jmal.model.desc", "false"));

    /* specific parameters for elongated models */
    /** Parameter type for the parameter elong_ratio */
    public final static String PARAM_ELONG_RATIO = "elong_ratio";
    /** Parameter type for the parameter major_axis_pos_angle */
    public final static String PARAM_MAJOR_AXIS_ANGLE = "major_axis_pos_angle";

    /* specific parameters for flattened models */
    /** Parameter type for the parameter flatten_ratio */
    public final static String PARAM_FLATTEN_RATIO = "flatten_ratio";
    /** Parameter type for the parameter minor_axis_pos_angle */
    public final static String PARAM_MINOR_AXIS_ANGLE = "minor_axis_pos_angle";

    /* members */
    /** wavelength variant */
    protected final WavelengthVariant wlVariant;

    /**
     * Constructor for the given wavelength variant
     * @param wlVariant the wavelength variant
     */
    public AbstractModelFunction(final WavelengthVariant wlVariant) {
        this.wlVariant = wlVariant;
    }

    /**
     * @return true if the wavelength variant is Const (gray model) i.e. not dependent on wavelength
     */
    @Override
    public boolean isGray() {
        return (this.wlVariant == WavelengthVariant.Const);
    }

    /**
     * @return true if the wavelength variant is BlackBody
     */
    public boolean isBlackBody() {
        return (this.wlVariant == WavelengthVariant.BlackBody);
    }

    /**
     * Return a new template Model instance with its default parameters.
     *
     * This method must be overriden by child classes to define the model type and specific parameters
     *
     * @return new Model instance
     */
    @Override
    public Model newModel() {
        final Model model = new Model();

        model.setNameAndType(getType());
        updateModelDescription(model);

        // common parameters :
        Parameter param;

        param = new Parameter();
        param.setNameAndType(PARAM_FLUX_WEIGHT);
        param.setValue(1D);
        model.getParameters().add(param);

        if (isBlackBody()) {
            param = new Parameter();
            param.setNameAndType(PARAM_TEMPERATURE);
            param.setMinValue(0D);
            param.setValue(1000D); // 1000 K by default
            param.setUnits(UNIT_KELVIN);
            model.getParameters().add(param);
        }

        param = new Parameter();
        param.setNameAndType(PARAM_X);
        param.setValue(0D);
        param.setUnits(UNIT_MAS);
        model.getParameters().add(param);

        param = new Parameter();
        param.setNameAndType(PARAM_Y);
        param.setValue(0D);
        param.setUnits(UNIT_MAS);
        model.getParameters().add(param);

        return model;
    }

    /**
     * Add a parameter supporting only positive values
     *
     * @param model model to update
     * @param name name of the parameter
     */
    protected final void addPositiveParameter(final Model model, final String name) {
        final Parameter param = new Parameter();
        param.setNameAndType(name);
        param.setMinValue(0D);
        param.setValue(0D);
        param.setUnits(UNIT_MAS);
        model.getParameters().add(param);
    }

    /**
     * Add a parameter representing a ratio (value >= 1)
     *
     * @param model model to update
     * @param name name of the parameter
     */
    protected final void addRatioParameter(final Model model, final String name) {
        final Parameter param = new Parameter();
        param.setNameAndType(name);
        param.setMinValue(1D);
        param.setValue(1D);
        model.getParameters().add(param);
    }

    /**
     * Add a parameter supporting only angle values (0 - 180 degrees)
     *
     * @param model model to update
     * @param name name of the parameter
     */
    protected final void addAngleParameter(final Model model, final String name) {
        final Parameter param = new Parameter();
        param.setNameAndType(name);
        param.setMinValue(0D);
        param.setValue(0D);
        param.setMaxValue(180D);
        param.setUnits(UNIT_DEG);
        model.getParameters().add(param);
    }

    /**
     * Update the model description (from code)
     * @param model model to update
     */
    @Override
    public final void updateModelDescription(final Model model) {
        if (ENABLE_DESC) {
            if (model.getDesc() == null) {
                model.setDesc(getDescription());
            }
        } else {
            if (model.getDesc() != null) {
                // trim description anyway:
                model.setDesc(null);
            }
        }
    }

    /**
     * Check the model parameters against their min/max bounds.
     * For now, it uses the parameter user min/max (LITpro) instead using anything else
     *
     * @param model model to check
     * @throws IllegalArgumentException
     */
    @Override
    public final void validate(final Model model) {
        updateModelDescription(model);

        for (Parameter param : model.getParameters()) {
            final double value = param.getValue();

            if (param.getMinValue() != null && value < param.getMinValue().doubleValue()) {
                createParameterException(param.getType(), model, "< " + param.getMinValue().doubleValue());
            }

            if (param.getMaxValue() != null && value > param.getMaxValue().doubleValue()) {
                createParameterException(param.getType(), model, "> " + param.getMaxValue().doubleValue());
            }
        }
    }

    /**
     * Compute the model function for the given Ufreq, Vfreq arrays and model parameters
     *
     * Note : the visibility array is given to add this model contribution to the total visibility
     *
     * @param function model function to compute
     * @param flux_weights normalized flux weights
     * @param ufreq U frequencies in rad-1
     * @param vfreq V frequencies in rad-1
     * @param nVis number of visibility to compute
     * @param vis complex visibility array
     * @param modelVis complex variable to store model complex contribution
     */
    public static void compute(final PunctFunction function, final double[] flux_weights,
                               final double[] ufreq, final double[] vfreq,
                               final int nVis, final MutableComplex[] vis, final MutableComplex modelVis) {
        // Compute :
        final double x = function.getX();
        final double y = function.getY();
        final boolean zero = function.isZero();

        double u, v, flux_weight;

        for (int i = 0; i < nVis; i++) {
            u = ufreq[i];
            v = vfreq[i];
            flux_weight = flux_weights[i];

            FourierFunctions.shift(u, v, zero, x, y, flux_weight * function.computeWeight(u, v), modelVis);

            // mutable complex:
            vis[i].add(modelVis);
        }
    }

    /**
     * Prepare the flux function for the given model
     *
     * @param model model instance
     * @return flux function
     */
    @Override
    public final FluxFunction prepareFluxFunction(final Model model) {
        if (isBlackBody()) {
            final BlackBodyFunction function = new BlackBodyFunction();
            function.setFluxWeight(getParameterValue(model, PARAM_FLUX_WEIGHT));
            function.setTemperature(getParameterValue(model, PARAM_TEMPERATURE));
            return function;
        } else {
            final FluxFunction function = new FluxFunction();
            function.setFluxWeight(getParameterValue(model, PARAM_FLUX_WEIGHT));
            return function;
        }
    }

    /**
     * Compute the flux function for the given wavelengths
     *
     * @param solidAngle solid angle to determine the emitting surface
     * @param function flux function to compute
     * @param wavelengths wavelengths to use (m)
     * @param flux array to store function's flux
     * @param totalFlux array to store total flux
     */
    public static void computeFlux(final double solidAngle,
                                   final FluxFunction function,
                                   final double wavelengths[],
                                   final double[] flux,
                                   final double[] totalFlux) {
        // Compute :
        for (int l = 0; l < wavelengths.length; l++) {
            final double fluxValue = solidAngle * function.computeFlux(wavelengths[l]);
            if (flux != null) {
                flux[l] = fluxValue;
            }
            if (totalFlux != null) {
                totalFlux[l] += fluxValue;
            }
        }
    }

    /**
     * Normalize the given flux array by the total flux to have total flux = 1.0
     * @param flux flux to normalize by total flux
     * @param totalFlux total flux to use
     */
    public static void normalizeFlux(final double[] flux, final double[] totalFlux) {
        // normalize flux by total flux:
        for (int l = 0; l < flux.length; l++) {
            if (totalFlux[l] <= 0.0) {
                flux[l] = 0.0; // discard contribution
            } else {
                flux[l] /= totalFlux[l];
            }
        }
    }

    /**
     * Prepare the computation function for the given model :
     * Get model parameters to fill the function context
     *
     * @param model model instance
     * @return model function
     */
    @Override
    public final PunctFunction prepareFunction(final Model model) {
        return createFunction(model);
    }

    /**
     * Create the computation function for the given model :
     * Get model parameters to fill the function context
     *
     * @param model model instance
     * @return model function
     */
    protected abstract T createFunction(final Model model);

    /**
     * Return the parameter value of the given type among the parameters of the given model
     *
     * @param type type of the parameter
     * @param model model to use
     * @return parameter value
     * @throws IllegalArgumentException if the parameter type is invalid for the given model
     */
    protected static double getParameterValue(final Model model, final String type) {
        final Parameter parameter = model.getParameter(type);
        if (parameter == null) {
            throw new IllegalArgumentException("parameter [" + type + "] not found in the model [" + model.getName() + "] !");
        }
        return parameter.getValue();
    }

    /**
     * Create a parameter validation exception
     *
     * @param type type of the parameter
     * @param model model instance
     * @param message validation message [< 0 for example]
     * @throws IllegalArgumentException
     */
    protected static void createParameterException(final String type, final Model model, final String message) throws IllegalArgumentException {
        // Find the parameter for the given type in the model parameter list :
        final Parameter parameter = model.getParameter(type);

        throw new IllegalArgumentException(parameter.getName() + " [" + parameter.getValue() + "] " + message + " not allowed in the model [" + model.getName() + "] !");
    }
}
