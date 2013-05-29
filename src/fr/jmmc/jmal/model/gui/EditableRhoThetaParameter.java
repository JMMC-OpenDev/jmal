/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.model.gui;

import fr.jmmc.jmal.model.ModelDefinition;
import fr.jmmc.jmal.model.ModelManager;
import fr.jmmc.jmal.model.targetmodel.Model;
import fr.jmmc.jmal.model.targetmodel.Parameter;
import fr.jmmc.jmal.util.MathUtils;
import net.jafama.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class overrides the EditableParameter class to define a rho or theta position parameter where only the value is editable
 *
 * @author Laurent BOURGES.
 */
public final class EditableRhoThetaParameter implements Editable {

    /** Class logger */
    private static Logger logger = LoggerFactory.getLogger(EditableRhoThetaParameter.class.getName());
    /** double precision for conversion = 10^-9 */
    public final static double ZERO_THRESHOLD = 1e-9d;

    /**
     * Custom type enum
     */
    public enum Type {

        /** rho parameter */
        RHO("rho", "rho", "mas"),
        /** theta parameter */
        THETA("theta", "theta", "deg");

        /**
         * Custom constructor
         *
         * @param name name of the parameter
         * @param type type of the parameter
         * @param units units of the parameter
         */
        private Type(final String name, final String type, final String units) {
            this.name = name;
            this.type = type;
            this.units = units;
        }
        /** parameter name */
        private final String name;
        /** parameter type */
        private final String type;
        /** parameter units */
        private final String units;

        /**
         * Return the parameter name
         * @return parameter name
         */
        public String getName() {
            return name;
        }

        /**
         * Return the parameter type
         * @return parameter type
         */
        public String getType() {
            return type;
        }

        /**
         * Return the parameter units
         * @return parameter units
         */
        public String getUnits() {
            return units;
        }
    }

    /* members */
    /** model of the parameter */
    private final Model model;
    /** type */
    private final Type type;
    /** name */
    private final String name;
    /** parameter X */
    private final Parameter paramX;
    /** parameter Y */
    private final Parameter paramY;

    /**
     * Constructor for rho and theta editable parameter
     *
     * @param model parent model
     * @param type rho or theta type
     */
    public EditableRhoThetaParameter(final Model model, final Type type) {
        this.model = model;
        this.type = type;

        this.name = this.type.getName() + ModelManager.parseModelUniqueIndex(model);

        this.paramX = model.getParameter(ModelDefinition.PARAM_X);
        this.paramY = model.getParameter(ModelDefinition.PARAM_Y);
    }

    /**
     * Return the model
     *
     * @return model
     */
    @Override
    public Model getModel() {
        return model;
    }

    /**
     * Indicates if the parameter is a shared parameter
     *
     * @return true if the parameter is a shared parameter
     */
    @Override
    public boolean isShared() {
        return false;
    }

    /**
     * Indicates if the parameter is a position
     *
     * @return true if the parameter is a position
     */
    @Override
    public boolean isPosition() {
        return true;
    }

    /* Parameter methods */

    /* --- read-only parameter attributes ------------------------------------- */
    /**
     * Gets the value of the name property.
     *
     * @return value of the name property
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Gets the value of the type property.
     *
     * @return value of the type property
     */
    @Override
    public String getType() {
        return this.type.getType();
    }

    /**
     * Gets the value of the units property.
     *
     * @return value of the units property
     */
    @Override
    public String getUnits() {
        return this.type.getUnits();
    }
    /* --- writable parameter attributes -------------------------------------- */

    /**
     * Gets the value of the value property.
     *
     * @return value property
     */
    @Override
    public double getValue() {
        final double x = paramX.getValue();
        final double y = paramY.getValue();

        if (logger.isDebugEnabled()) {
            logger.debug("getValue[{}] [x,y] = [{}, {}]", type, x, y);
        }

        double value = 0d;
        switch (this.type) {
            case RHO:
                value = getRho(x, y);
                break;
            case THETA:
                value = getTheta(x, y);
                break;
            default:
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getValue[{}] = [{}]", type, value);
        }
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value value to set
     */
    @Override
    public void setValue(final double value) {
        if (logger.isDebugEnabled()) {
            logger.debug("setValue[{}] = [{}]", type, value);
        }
        // old values :
        final double x = paramX.getValue();
        final double y = paramY.getValue();

        if (logger.isDebugEnabled()) {
            logger.debug("setValue[{}] [x,y] = [{}, {}]", type, x, y);
        }

        double x2 = 0d;
        double y2 = 0d;

        switch (this.type) {
            case RHO:
                final double theta = getTheta(x, y);

                x2 = getX(value, theta);
                y2 = getY(value, theta);
                break;
            case THETA:
                final double rho = getRho(x, y);

                x2 = getX(rho, value);
                y2 = getY(rho, value);
                break;
            default:
        }
        paramX.setValue(x2);
        paramY.setValue(y2);

        if (logger.isDebugEnabled()) {
            logger.debug("setValue[{}] [x2,y2] = [{}, {}]", type, x2, y2);
        }
    }

    /**
     * Gets the value of the minValue property.
     *
     * @return null
     */
    @Override
    public Double getMinValue() {
        return null;
    }

    /**
     * Gets the value of the maxValue property.
     *
     * @return null
     */
    @Override
    public Double getMaxValue() {
        return null;
    }

    /**
     * Gets the value of the scale property.
     *
     * @return null
     */
    @Override
    public Double getScale() {
        return null;
    }

    /**
     * Gets the value of the hasFixedValue property.
     *
     * @return false
     */
    @Override
    public boolean isHasFixedValue() {
        return false;
    }

    /**
     * Sets the value of the minValue property.
     *
     * @param value value to set
     */
    @Override
    public void setMinValue(final Double value) {
    }

    /**
     * Sets the value of the maxValue property.
     *
     * @param value value to set
     */
    @Override
    public void setMaxValue(final Double value) {
    }

    /**
     * Sets the value of the scale property.
     *
     * @param value value to set
     */
    @Override
    public void setScale(final Double value) {
    }

    /**
     * Sets the value of the hasFixedValue property.
     *
     * @param value value to set
     */
    @Override
    public void setHasFixedValue(final boolean value) {
    }

    /* --- utility methods */
    /**
     * Return the distance rho
     *
     * @param x coordinate x
     * @param y coordinate y
     * @return distance rho
     */
    public static double getRho(final double x, final double y) {
        return MathUtils.carthesianNorm(x, y);
    }

    /**
     * Return the theta angle in [-180;180]
     *
     * @param x coordinate x
     * @param y coordinate y
     * @return theta angle in [-180;180]
     */
    public static double getTheta(final double x, final double y) {
        return FastMath.toDegrees(FastMath.atan2(y, x));
    }

    /**
     * Return the coordinate x
     *
     * @param rho distance
     * @param theta angle in [-180;180]
     * @return coordinate x
     */
    public static double getX(final double rho, final double theta) {
        final double x = rho * FastMath.cos(FastMath.toRadians(theta));
        /*
         * if (Math.abs(x) < ZERO_THRESHOLD) {
         * return 0d;
         * }
         */
        return x;
    }

    /**
     * Return the coordinate y
     *
     * @param rho distance
     * @param theta angle in [-180;180]
     * @return coordinate y
     */
    public static double getY(final double rho, final double theta) {
        final double y = rho * FastMath.sin(FastMath.toRadians(theta));
        /*
         * if (Math.abs(y) < ZERO_THRESHOLD) {
         * return 0d;
         * }
         */
        return y;
    }
}
