/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.model;

import fr.jmmc.jmal.model.function.math.PunctFunction;
import fr.jmmc.jmal.model.function.math.Functions;
import fr.jmmc.jmal.model.targetmodel.Model;
import fr.jmmc.jmal.model.targetmodel.Parameter;
import org.apache.commons.math.complex.Complex;

/**
 *
 * @param <T> type of the function class
 * 
 * @author Laurent BOURGES.
 */
public abstract class AbstractModelFunction<T extends PunctFunction> implements ModelFunction {

  /** Class Name */
  private final static String className_ = "fr.jmmc.mcs.model.ModelFunction";
  /** Class logger */
  protected final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
          className_);

  /** variant enumeration (standard, elongated and flattened models) */
  public enum ModelVariant {

    /** default/standard model variant */
    Standard,
    /** elongated model variant */
    Elongated,
    /** flattened model variant */
    Flattened;
  }

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

  /**
   * Constructor
   */
  public AbstractModelFunction() {
    super();
  }

  /**
   * Return a new template Model instance with its default parameters.
   *
   * This method must be overriden by child classes to define the model type and specific parameters
   *
   * @return new Model instance
   */
  public Model newModel() {

    final Model model = new Model();

    // common parameters :
    Parameter param;

    param = new Parameter();
    param.setNameAndType(PARAM_FLUX_WEIGHT);
    param.setValue(1D);
    model.getParameters().add(param);

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
   * @param model model to update
   * @param name name of the parameter
   */
  protected void addPositiveParameter(final Model model, final String name) {
    final Parameter param = new Parameter();
    param.setNameAndType(name);
    param.setMinValue(0D);
    param.setValue(0D);
    param.setUnits(UNIT_MAS);
    model.getParameters().add(param);
  }

  /**
   * Add a parameter representing a ratio (value >= 1)
   * @param model model to update
   * @param name name of the parameter
   */
  protected void addRatioParameter(final Model model, final String name) {
    final Parameter param = new Parameter();
    param.setNameAndType(name);
    param.setMinValue(1D);
    param.setValue(1D);
    model.getParameters().add(param);
  }

  /**
   * Add a parameter supporting only angle values (0 - 180 degrees)
   * @param model model to update
   * @param name name of the parameter
   */
  protected void addAngleParameter(final Model model, final String name) {
    final Parameter param = new Parameter();
    param.setNameAndType(name);
    param.setMinValue(0D);
    param.setValue(0D);
    param.setMaxValue(180D);
    param.setUnits(UNIT_DEG);
    model.getParameters().add(param);
  }

  /**
   * Check the model parameters against their min/max bounds.
   * For now, it uses the parameter user min/max (LITpro) instead using anything else
   * @param model model to check
   * @throws IllegalArgumentException
   */
  public final void validate(final Model model) {
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
   * @param ufreq U frequencies in rad-1
   * @param vfreq V frequencies in rad-1
   * @param model model instance
   * @param vis complex visibility array
   */
  public final void compute(final double[] ufreq, final double[] vfreq, final Model model, final Complex[] vis) {

    // check model parameters :
    validate(model);

    /** Get the current thread to check if the computation is interrupted */
    final Thread currentThread = Thread.currentThread();

    final int size = ufreq.length;

    // this step indicates when the thread.isInterrupted() is called in the for loop
    final int stepInterrupt = 1 + size / 20;

    // Get parameters to fill the function context :
    final T function = createFunction(model);

    // Compute :
    for (int i = 0; i < size; i++) {
      vis[i] = vis[i].add(compute(ufreq[i], vfreq[i], function));

      // fast interrupt :
      if (i % stepInterrupt == 0 && currentThread.isInterrupted()) {
        return;
      }
    }
  }

  /**
   * Compute the model function at a single Ufreq and Vfreq
   *
   * return function.computeWeight(ufreq, vfreq) * shift(ufreq, vfreq, x, y)
   *
   * @param ufreq U frequency in rad-1
   * @param vfreq V frequency in rad-1
   * @param function model function to compute
   * @return complex Fourier transform value
   */
  protected final Complex compute(final double ufreq, final double vfreq, final T function) {
    return Functions.shift(ufreq, vfreq, function.getX(), function.getY(), function.computeWeight(ufreq, vfreq));
  }

  /**
   * Create the computation function for the given model :
   * Get model parameters to fill the function context
   * @param model model instance
   * @return model function
   */
  protected abstract T createFunction(final Model model);

  /**
   * Return the parameter value of the given type among the parameters of the given model
   * @param type type of the parameter
   * @param model model to use
   * @return parameter value
   * @throws IllegalArgumentException if the parameter type is invalid for the given model
   */
  protected final static double getParameterValue(final Model model, final String type) {
    final Parameter parameter = model.getParameter(type);
    if (parameter == null) {
      throw new IllegalArgumentException("parameter [" + type + "] not found in the model [" + model.getName() + "] !");
    }
    return parameter.getValue();
  }

  /**
   * Create a parameter validation exception
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
