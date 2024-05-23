/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.model;

/**
 * Wavelength variant enumeration (const, black-body)
 * @author bourgesl
 */
public enum WavelengthVariant {

    /** constant model along wavelength */
    Const,
    /** black-body at temperature (planck law) */
    BlackBody
}
