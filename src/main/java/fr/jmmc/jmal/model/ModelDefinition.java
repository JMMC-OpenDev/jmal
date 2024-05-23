/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.model;

/**
 * This interface gathers all model and parameter types
 * 
 * @author Laurent BOURGES.
 */
public interface ModelDefinition {

    /** suffix for Black-Body variants */
    public String SUFFIX_BB = "_BB";

    /* Model types */
    /** punct model type */
    public String MODEL_PUNCT = "punct";
    /** punct_BB model type */
    public String MODEL_PUNCT_BB = "punct" + SUFFIX_BB;
    /** disk model type */
    public String MODEL_DISK = "disk";
    /** disk_BB model type */
    public String MODEL_DISK_BB = "disk" + SUFFIX_BB;
    /** elongated disk model type */
    public String MODEL_EDISK = "elong_disk";
    /** elongated disk_BB model type */
    public String MODEL_EDISK_BB = "elong_disk" + SUFFIX_BB;
    /** flattened disk model type */
    public String MODEL_FDISK = "flatten_disk";
    /** flattened disk_BB model type */
    public String MODEL_FDISK_BB = "flatten_disk" + SUFFIX_BB;
    /** disk model type */
    public String MODEL_CIRCLE = "circle";
    /** ring model type */
    public String MODEL_RING = "ring";
    /** ring_BB model type */
    public String MODEL_RING_BB = "ring" + SUFFIX_BB;
    /** elongated ring model type */
    public String MODEL_ERING = "elong_ring";
    /** elongated ring_BB model type */
    public String MODEL_ERING_BB = "elong_ring" + SUFFIX_BB;
    /** flattened ring model type */
    public String MODEL_FRING = "flatten_ring";
    /** flattened ring_BB model type */
    public String MODEL_FRING_BB = "flatten_ring" + SUFFIX_BB;
    /** gaussian model type */
    public String MODEL_GAUSS = "gaussian";
    /** gaussian model type */
    public String MODEL_GAUSS_BB = "gaussian" + SUFFIX_BB;
    /** elongated gaussian model type */
    public String MODEL_EGAUSS = "elong_gaussian";
    /** elongated gaussian model type */
    public String MODEL_EGAUSS_BB = "elong_gaussian" + SUFFIX_BB;
    /** flattened gaussian model type */
    public String MODEL_FGAUSS = "flatten_gaussian";
    /** flattened gaussian model type */
    public String MODEL_FGAUSS_BB = "flatten_gaussian" + SUFFIX_BB;
    /** limb darkened disk model type */
    public String MODEL_LDDISK = "limb_quadratic";

    /* Units */
    /** milli arc second Unit */
    public final static String UNIT_MAS = "mas";
    /** degrees Unit */
    public final static String UNIT_DEG = "degrees";
    /** Kelvin Unit */
    public final static String UNIT_KELVIN = "Kelvin";

    /* Parameter types */

 /* common parameters */
    /** Parameter type for the parameter flux_weight */
    public static String PARAM_FLUX_WEIGHT = "flux_weight";
    /** Parameter type for the parameter x */
    public static String PARAM_X = "x";
    /** Parameter type for the parameter y */
    public static String PARAM_Y = "y";

    /* black-body flux */
    /** Parameter type for the parameter Temperature */
    public static String PARAM_TEMPERATURE = "temperature";

    /* specific parameters */
    /** Parameter type for the parameter diameter */
    public static String PARAM_DIAMETER = "diameter";
}
