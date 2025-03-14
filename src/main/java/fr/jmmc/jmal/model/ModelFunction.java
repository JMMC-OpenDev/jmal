/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model;

import fr.jmmc.jmal.model.function.math.FluxFunction;
import fr.jmmc.jmal.model.function.math.PunctFunction;
import fr.jmmc.jmal.model.targetmodel.Model;

/**
 * This interface represents a taregt Model to compute the complex visibility for a given UV coordinates
 *
 * @author Laurent BOURGES.
 */
public interface ModelFunction extends ModelDefinition {

    /**
     * Return the model type
     *
     * @return model type
     */
    public String getType();

    /**
     * Return the model description
     *
     * @return model description
     */
    public String getDescription();

    /**
     * Update the model description (from code)
     * @param model model to update
     */
    public void updateModelDescription(final Model model);

    /**
     * Return a new Model instance with its parameters and default values
     *
     * @return new Model instance
     */
    public Model newModel();

    /**
     * Check the model parameters against their min/max bounds.
     *
     * @param model model to check
     * @throws IllegalArgumentException
     */
    public void validate(final Model model);

    /**
     * @return true if the wavelength variant is Const (gray model) i.e. not dependent on wavelength
     */
    public boolean isGray();

    /**
     * Prepare the flux function for the given model
     *
     * @param model model instance
     * @return flux function
     */
    public FluxFunction prepareFluxFunction(final Model model);

    /**
     * Prepare the computation function for the given model :
     * Get model parameters to fill the function context
     *
     * @param model model instance
     * @return model function
     */
    public PunctFunction prepareFunction(final Model model);
}
