/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal;

import java.text.DecimalFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Astronomical Library: ra/dec parser & formatter.
 *
 * Class regrouping usefull statics method to convert star coordinates between
 * different formats and units.
 * 
 * @author Sylvain LAFRASSE, Guillaume MELLA, Laurent BOURGES.
 */
public final class ALX {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(ALX.class.getName());
    /** Describe the micrometer (micron, or um) unit */
    public static final double MICRON = 1d;
    /** Describe the meter unit */
    public static final double METER = 1d;
    /** Describe the arcminute unit */
    public static final double ARCMIN = 1d;
    /** Specify the value of one arcminute in degrees */
    public static final double ARCMIN_IN_DEGREES = (1d / 60d);
    /** Describe the arcsecond unit */
    public static final double ARCSEC = 1d;
    /** Specify the value of one arcsecond in degrees */
    public static final double ARCSEC_IN_DEGREES = (1d / 3600d);
    /** Specify the value of one arcsecond in degrees */
    public static final double DEG_IN_ARCSEC = 3600d;
    /** Specify the value of one milli arcsecond in degrees */
    public static final double MILLI_ARCSEC_IN_DEGREES = ARCSEC_IN_DEGREES / 1000d;
    /** Specify the value of one hour in degrees */
    public static final double HOUR_IN_DEGREES = 360d / 24d;
    /** Specify the value of one hour in degrees */
    public static final double DEG_IN_HOUR = 24d / 360d;

    /**
     * Forbidden constructor : utility class
     */
    private ALX() {
        super();
    }

    /**
     * Convert the given Right Ascension (RA).
     *
     * @param raHms the right ascension as a HH:MM:SS.TT or HH MM SS.TT string.
     *
     * @return the right ascension as a double in degrees.
     */
    public static double parseHMS(final String raHms) {

        // RA can be given as HH:MM:SS.TT or HH MM SS.TT. 
        // Replace ':' by ' ', and remove trailing and leading space
        final String input = raHms.replace(':', ' ').trim();

        double hh = 0d;
        double hm = 0d;
        double hs = 0d;

        // Parse the given string
        try {
            final String[] tokens = input.split(" ");

            final int len = tokens.length;

            if (len > 0) {
                hh = Double.parseDouble(tokens[0]);
            }
            if (len > 1) {
                hm = Double.parseDouble(tokens[1]);
            }
            if (len > 2) {
                hs = Double.parseDouble(tokens[2]);
            }

        } catch (NumberFormatException nfe) {
            _logger.debug("format exception: ", nfe);
            hh = 0d;
            hm = 0d;
            hs = 0d;
        }

        // Get sign of hh which has to be propagated to hm and hs
        final double sign = (input.startsWith("-")) ? -1d : 1d;

        // Convert to degrees
        // note : hh already includes the sign :
        final double ra = (hh + sign * (hm * ARCMIN_IN_DEGREES + hs * ARCSEC_IN_DEGREES)) * HOUR_IN_DEGREES;

        if (_logger.isDebugEnabled()) {
            _logger.debug("HMS : ’{}' = '{}'.", raHms, ra);
        }

        return ra;
    }

    /**
     * Convert the given Right Ascension (RA).
     *
     * @param raHms the right ascension as a HH:MM:SS.TT or HH MM SS.TT string.
     *
     * @return the right ascension as a double in degrees  [-180; -180].
     */
    public static double parseRA(final String raHms) {
        double ra = parseHMS(raHms);

        // Set angle range [-180 - 180]
        if (ra > 180d) {
            ra -= 360d;
        }

        if (_logger.isDebugEnabled()) {
            _logger.debug("RA  : ’{}' = '{}'.", raHms, ra);
        }

        return ra;
    }

    /**
     * Convert the given Declinaison (DEC).
     *
     * @param decDms the declinaison as a DD:MM:SS.TT or DD MM SS.TT string.
     *
     * @return the declinaison as a double in degrees.
     */
    public static double parseDEC(final String decDms) {

        // DEC can be given as DD:MM:SS.TT or DD MM SS.TT. 
        // Replace ':' by ' ', and remove trailing and leading space
        final String input = decDms.replace(':', ' ').trim();

        double dd = 0d;
        double dm = 0d;
        double ds = 0d;

        // Parse the given string
        try {
            final String[] tokens = input.split(" ");

            final int len = tokens.length;

            if (len > 0) {
                dd = Double.parseDouble(tokens[0]);
            }
            if (len > 1) {
                dm = Double.parseDouble(tokens[1]);
            }
            if (len > 2) {
                ds = Double.parseDouble(tokens[2]);
            }

        } catch (NumberFormatException nfe) {
            _logger.debug("format exception: ", nfe);
            dd = 0d;
            dm = 0d;
            ds = 0d;
        }

        // Get sign of dd which has to be propagated to dm and ds
        final double sign = (input.startsWith("-")) ? -1d : 1d;

        // Convert to degrees
        // note : dd already includes the sign :
        final double dec = dd + sign * (dm * ARCMIN_IN_DEGREES + ds * ARCSEC_IN_DEGREES);

        if (_logger.isDebugEnabled()) {
            _logger.debug("DEC : ’{}' = '{}'.", decDms, dec);
        }

        return dec;
    }

    /**
     * Return the DMS format of the given angle
     * @param angle angle in degrees > -360.0
     * @return string DMS representation
     */
    public static String toDMS(final double angle) {
        return toDMS(new StringBuilder(16), angle).toString();
    }

    /**
     * Append the DMS format of the given angle to given string builder
     * @param sb string builder to append into
     * @param angle angle in degrees > -360.0
     * @return given string builder
     */
    public static StringBuilder toDMS(final StringBuilder sb, final double angle) {
        if (angle < -360d) {
            return sb;
        }
        /* TODO: use constant = 360 and clean modulo in range[-90;90] */
        final double normalizedAngle = Math.abs(angle) % 360d;

        final int iDeg = (int) Math.floor(normalizedAngle);
        final double rest = normalizedAngle - iDeg;

        if (angle < 0d) {
            sb.append("-");
        }
        sb.append(iDeg);

        return toMS(sb, rest);
    }

    /**
     * Return the HMS format of the given angle
     * @param angle angle in degrees > -360.0
     * @return string HMS representation, null otherwise
     */
    public static String toHMS(final double angle) {
        return toHMS(new StringBuilder(16), angle).toString();
    }

    /**
     * Append the HMS format of the given angle to given string builder
     * @param sb string builder to append into
     * @param angle angle in degrees > -360.0
     * @return given string builder
     */
    public static StringBuilder toHMS(final StringBuilder sb, final double angle) {
        if (angle < -360d) {
            return sb;
        }
        /* TODO: use constant = 360 and clean modulo in range[0;360] */
        final double normalizedAngle = ((angle + 360d) % 360d) * DEG_IN_HOUR; /* convert deg in hours */

        final double fHour = normalizedAngle;
        final int iHour = (int) Math.floor(fHour);

        final double rest = normalizedAngle - iHour;

        sb.append(iHour);

        return toMS(sb, rest);
    }

    private static StringBuilder toMS(final StringBuilder sb, final double angle) {
        final double fMinute = 60d * angle;
        final int iMinute = (int) Math.floor(fMinute);

        final double fSecond = 60d * (fMinute - iMinute);

        /* TODO: avoid using DecimalFormat perform padding manually */
        DecimalFormat formatter = new DecimalFormat(":00");
        sb.append(formatter.format(iMinute));

        formatter = new DecimalFormat(":00.###");
        sb.append(formatter.format(fSecond));

        return sb;
    }

    /**
     * Convert a value in arc-minute to minutes.
     *
     * @param arcmin the arc-minute value to convert.
     *
     * @return a double containing the converted value.
     */
    public static double arcmin2minutes(final double arcmin) {
        final double minutes = (arcmin * DEG_IN_HOUR);

        return minutes;
    }

    /**
     * Convert a value in minutes to arc-minute.
     *
     * @param minutes the value in minutes to convert.
     *
     * @return a double containing the converted value.
     */
    public static double minutes2arcmin(final double minutes) {
        final double arcmin = (minutes * HOUR_IN_DEGREES);

        return arcmin;
    }

    /**
     * Convert a value in arc-minute to degrees.
     *
     * @param arcmin the arc-minute value to convert.
     *
     * @return a double containing the converted value.
     */
    public static double arcmin2degrees(final double arcmin) {
        final double degrees = (arcmin * ARCMIN_IN_DEGREES);

        return degrees;
    }

    /**
     * Convert a value in degrees to arc-minute.
     *
     * @param degrees the value in degrees to convert.
     *
     * @return a double containing the converted value.
     */
    public static double degrees2arcmin(final double degrees) {
        final double arcmin = (degrees * 60d);

        return arcmin;
    }

    /**
     * Convert a minute value to degrees.
     *
     * @param minutes the value in minute to convert.
     *
     * @return a double containing the converted value.
     */
    public static double minutes2degrees(final double minutes) {
        /* TODO: define constant = 60/15 */
        final double degrees = minutes / 4d;

        return degrees;
    }

    /**
     * Convert a value in degrees to minute.
     *
     * @param degrees the value in degrees to convert.
     *
     * @return a double containing the converted value.
     */
    public static double degrees2minutes(final double degrees) {
        /* TODO: define constant = 60/15 */
        final double minutes = degrees * 4d;

        return minutes;
    }

    /**
     * Unit tests
     */
    public static void main(String[] args) {

        // Set the default locale to en-US locale (for Numerical Fields "." ",")
        Locale.setDefault(Locale.US);

        /* HMS */
        System.out.println("HMS(0°) = " + toHMS(0.0));
        System.out.println("HMS(4°) = " + toHMS(4.0));
        System.out.println("HMS(4.x°) = " + toHMS(4.123456789123456789));
        System.out.println("HMS(12°) = " + toHMS(12.0));
        System.out.println("HMS(1h) = " + toHMS(1.0 * HOUR_IN_DEGREES));
        System.out.println("HMS(12h) = " + toHMS(12.0 * HOUR_IN_DEGREES));

        /* DMS */
        System.out.println("DMS(-90°) = " + toDMS(-90.0));
        System.out.println("DMS(-30°) = " + toDMS(-30.0));
        System.out.println("DMS(-5°) = " + toDMS(-5.0));
        System.out.println("DMS(0°) = " + toDMS(0.0));
        System.out.println("DMS(5°) = " + toDMS(5.0));
        System.out.println("DMS(5.x°) = " + toDMS(5.123456789123456789));
        System.out.println("DMS(30°) = " + toDMS(30.0));
        System.out.println("DMS(90°) = " + toDMS(90.0));
    }
}
/*___oOo___*/
