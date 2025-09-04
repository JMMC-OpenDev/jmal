/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal;

import fr.jmmc.jmcs.util.NumberUtils;
import java.util.Arrays;
import net.jafama.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Photometry band related information
 *
 * @author Laurent BOURGES, Sylvain LAFRASSE.
 */
public enum Band {

    /** U (ultra violet) */
    U("U", 0.334, 0.066, -1.4, 0.3),
    /** B (Visible) */
    B("B", 0.461875, 0.08175, -1.2, 0.48),
    /** GAIA G_BP (from vosa) */
    G_BP("G_bp", 0.503575, 0.21575, -1.38945, 0.5),
    /** V (Visible) */
    V("V", 0.556, 0.1105, -1.44, 0.5),
    /** GAIA G (from vosa) */
    G("G", 0.582239, 0.405297, -1.60134, 0.6),
    /** R (Visible) */
    R("R", 0.6625, 0.10651, -1.65, 0.65),
    /** GAIA G_RP (from vosa) */
    G_RP("G_rp", 0.761996, 0.292444, -1.8965, 0.7),
    /** I (Near Infrared) */
    I("I", 0.869625, 0.31176, -1.94, 0.75),
    /** J (Near Infrared) */
    J("J", 1.2365, 0.426, -2.5, 0.77),
    /** H (Near Infrared) */
    H("H", 1.679625, 0.46425, -2.94, 0.84),
    /** K (Near Infrared) */
    K("K", 2.365625, 0.912, -3.4, 0.93),
    /** L (Near Infrared) (MATISSE) [2.8 - 4.2] */
    L("L", 3.5, 1.4, -4.154, 0.972, 286),
    /** M (Mid Infrared) (MATISSE) [4.2 - 7.8] */
    M("M", 6.0, 3.6, -4.568, 0.985, 182, 4.5),
    /** N (Mid Infrared) (MATISSE) [7.9 - 13.1] */
    N("N", 10.5, 5.2, -6.0, 0.996, 37),
    /** Q (Mid Infrared) */
    Q("Q", 16.575, 4.05, -7.17, 0.999);

    /*
    λeff and Weff are used, then logFluxZero = math.log10(zero point * 1e7)
    
Filter ID 	λref 	λmean 	λeff 	λmin 	λmax 	Weff 	ZPν 	ZPλ 	Obs. Facility 	Instrument 	Description
GAIA/GAIA3.Gbp	5109.71	5319.87	5035.75	3301.83	6739.34	2157.50	3552.01	4.08e-9	GAIA	GAIA	GAIA Gbp filter, DR3
GAIA/GAIA3.G	6217.59	6719.55	5822.39	3309.13	10381.71	4052.97	3228.75	2.5e-9	GAIA	GAIA	GAIA G filter, DR3
GAIA/GAIA3.Grp	7769.02	7939.10	7619.96	6200.46	10465.57	2924.44	2554.95	1.27e-9	GAIA	GAIA	GAIA Grp filter, DR3

    From https://svo2.cab.inta-csic.es/theory/fps/index.php?mode=browse&gname=GAIA&gname2=GAIA3&asttype=
     */
    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(Band.class.getName());
    /** Planck's constant in standard units (6.6262e-34) */
    public final static double H_PLANCK = 6.62606896e-34;
    /** Speed of light (2.99792458e8) */
    public final static double C_LIGHT = 2.99792458e8;

    /**
     * Find the band corresponding to the given band name
     *
     * @param bandName wave length in microns
     * @return corresponding band or null if no match
     */
    public static Band findBand(final String bandName) throws IllegalStateException {
        if (bandName != null && bandName.length() != 0) {
            final Band[] bands = values();
            final int len = bands.length;

            for (int i = len - 1; i >= 0; i--) {
                final Band b = bands[i];
                if (b.getName().equals(bandName)) {
                    return b;
                }
            }
        }
        return null;
    }

    /**
     * Find the band corresponding to the given wavelength
     *
     * @param waveLength wave length in microns
     * @return corresponding band
     * @throws IllegalStateException if no band found
     */
    public static Band findBand(final double waveLength) throws IllegalStateException {
        final Band[] bands = values();
        final int len = bands.length;

        Band match = null;
        double minDist = Double.MAX_VALUE;

        for (int i = len - 1; i >= 0; i--) {
            final Band b = bands[i];
            final double dist = Math.abs(waveLength - b.getLambda());
            if (dist <= b.getHalfBandWidth()) {
                if (dist < minDist) {
                    minDist = dist;
                    match = b;
                }
            }
        }
        if (match != null) {
            return match;

        }
        // Return the nearest band:
        if (waveLength <= bands[0].getLambda()) {
            return bands[0];
        }
        if (waveLength >= bands[len - 1].getLambda()) {
            return bands[len - 1];
        }
        // Handle discontinuity between bands:
        Band p = bands[0];
        for (int i = 1; i < len; i++) {
            final Band b = bands[i];
            if ((p.getLambda() <= waveLength) && (waveLength <= b.getLambda())) {
                // wavelength is close to p or b bands:
                if (Math.abs(waveLength - p.getLambda()) <= Math.abs(b.getLambda() - waveLength)) {
                    return p;
                }
                return b;
            }
            p = b;
        }
        return p;
    }

    /** r0 factor = 1.029 * 1E-6/a and a = PI / ( 180 * 3600 ) */
    private static final double R0_FACTOR = ALX.AIRY_DISK_FWHM_FACTOR * 1e-6 * (180.0 * 3600.0) / Math.PI;

    /**
     * Compute the strehl ratio. see le louarn et al (1998, mnras 295, 756), and amb-igr-011 p.5
     *
     * @param aoBand band of the AO system (V by default)
     * @param magnitude object magnitude in AO's band
     * @param waveLengths wave lengths in meters
     * @param diameter telescope diameter in meters
     * @param seeing seeing in arc sec
     * @param nbSubPupils number of sub-pupils (interesting pixels on the camera)
     * @param nbActuators number of actuators (or corrected AO modes)
     * @param td detector time (ms)
     * @param t0 coherence time (ms)
     * @param quantumEfficiency Detector quantum efficiency
     * @param ron Detector readout noise
     * @param elevation target elevation in degrees [0; 90]
     * @param strehlMax optional max strehl for this AO in this band (0 to use band's default)
     * @return strehl ratio
     */
    public static double[] strehl(final Band aoBand, final double magnitude, final double[] waveLengths,
                                  final double diameter, final double seeing, final int nbSubPupils, final int nbActuators,
                                  final double td, final double t0,
                                  final double quantumEfficiency, final double ron,
                                  final double elevation, final double strehlMax) {

        final double logStrehlMax = (strehlMax > 0.0) ? -Math.log(strehlMax) : 0.0;

        // avoid cos(0) so use min elevation = 0.5 deg:
        final double zenithAngle = 90.0 - Math.max(elevation, 0.5);

        // airmass: secant of the zenith angle (1/cos(zenith_angle))
        final double airmass = 1.0 / FastMath.cos(FastMath.toRadians(zenithAngle));

        final double lambdaV = 0.5; // seeing is given at 500 nm

        // r0(e)=cos(90-e)^(3/5) * r0
        final double r0_V = R0_FACTOR * (lambdaV / seeing);

        final double r0_corr = r0_V * FastMath.pow(airmass, -3.0 / 5.0);

        final double lambdaAO = (aoBand != Band.V) ? aoBand.getLambdaFluxZero() : lambdaV;

        final double td_over_t0 = td / t0;

        // size of a square sub pupil:
        final double ds2 = Math.PI * FastMath.pow(0.5 * diameter, 2.0) / nbSubPupils;
        final double ds = Math.sqrt(Math.PI * FastMath.pow(0.5 * diameter, 2.0) / nbActuators);

        if (logger.isDebugEnabled()) {
            logger.debug("zenithAngle   = {}", zenithAngle);
            logger.debug("airmass       = {}", airmass);
            logger.debug("lambdaAO      = {}", lambdaAO);
            logger.debug("magnitude     = {}", magnitude);
            logger.debug("diameter      = {}", diameter);
            logger.debug("seeing        = {}", seeing);
            logger.debug("r0_V          = {}", r0_V);
            logger.debug("(td/t0)       = {}", td_over_t0);
            logger.debug("nbSubPupils   = {}", nbSubPupils);
            logger.debug("ds2           = {}", ds2);
            logger.debug("nbActuators   = {}", nbActuators);
            logger.debug("ds            = {}", ds);
            logger.debug("strehlMax     = {}", strehlMax);
        }

        // NbPhot(AO) per DIT per sub aperture:
        final double n0_per_subPupil = aoBand.getNbPhotZero() * aoBand.getBandWidth() * (1e-6 * 1e-3) * td;

        // flux_per_subap=0.25 * f * 10.^(-0.4 * mag) * ds^2
        // LBO: remove 0.25 => transmission
        final double nphot_per_subPupil = quantumEfficiency * n0_per_subPupil * FastMath.pow(10.0, -0.4 * magnitude) * ds2;

        final int nWLen = waveLengths.length;
        final double[] strehlPerChannel = new double[nWLen];

        double lambdaObs, lambdaRatio;
        double r0, d_over_r0, ds_over_r0;

        double sigmaphi2_alias_fit, sigmaphi2_phot, sigmaphi2_sensor, sigmaphi2_fixed, sigmaphi2, e_sigmaphi2;

        final double sigmaphi2_bw = 0.962 * FastMath.pow(td_over_t0, (5.0 / 3.0));

        for (int i = 0; i < nWLen; i++) {
            lambdaObs = waveLengths[i] * 1e6; // microns

            // explication formule r0:
            // seeing = angular FWHM of seeing in V = 1.029 lambdaV / r0 where r0 = fried coherence length.
            // to have seeing in arcsec and all wavelengths in microns, we have
            // seeing * a = 1.029 * lambdaV * 1E-6 / r0 with a = 1 arcsec in radians = PI / 180 * 3600
            // thus r0 = 1.029 * 1E-6 / a * seeing
            // use lambdaV as seeing is given for V:
            lambdaRatio = (lambdaObs / lambdaV);

            // r0 at lambda obs:
            r0 = r0_corr * FastMath.pow(lambdaRatio, (6.0 / 5.0));
            d_over_r0 = diameter / r0; // Math.max(1.0, diameter / r0);
            ds_over_r0 = ds / r0;

            // constant was 0.87 = AMD-REP 001 p32 (related to AO system)
            // MATISSE uses 0.54, adopted in 2018.11
            sigmaphi2_alias_fit = 0.54 * FastMath.pow(ds_over_r0, (5.0 / 3.0));

            // use lambdaAO as magAO corresponds to this AO band:
            lambdaRatio = (lambdaObs / lambdaAO);

            // photon error:
            // (4.*(!DPI^2)/3.)*(lambda_ao/lambda_sc)^2/Nphot_ao_ds
            sigmaphi2_phot = (4.0 / 3.0 * Math.PI * Math.PI) * FastMath.pow(lambdaRatio, -2.0) / nphot_per_subPupil;

            // sensor error:
            // (4.*(!DPI^2)/3.)*(lambda_ao/lambda_sc)^2/Nphot_ao_ds
            sigmaphi2_sensor = (8.0 / 9.0 * Math.PI * Math.PI) * FastMath.pow(lambdaRatio, -2.0) * FastMath.pow(ron / nphot_per_subPupil, 2.0);
            sigmaphi2_sensor *= FastMath.pow(1.0 + FastMath.pow(lambdaRatio, (12.0 / 5.0)) * FastMath.pow(ds_over_r0, 2.0), 2.0);
            sigmaphi2_sensor *= FastMath.pow(lambdaRatio, -(12.0 / 5.0));
            sigmaphi2_sensor *= FastMath.pow(ds_over_r0, -2.0);

            sigmaphi2_fixed = (logStrehlMax > 0.0) ? logStrehlMax : -Math.log(findBand(lambdaObs).getStrehlMax());

            sigmaphi2 = sigmaphi2_alias_fit + sigmaphi2_bw + sigmaphi2_phot + sigmaphi2_sensor + sigmaphi2_fixed;

            e_sigmaphi2 = FastMath.exp(-sigmaphi2);

            strehlPerChannel[i] = e_sigmaphi2 + (1.0 - e_sigmaphi2) / (1.0 + FastMath.pow(d_over_r0, 2.0));

            if (logger.isDebugEnabled()) {
                logger.debug("lambda          = {}", lambdaObs);
                logger.debug("r0              = {}", r0);
                logger.debug("nphot_per_subPupil = {}", nphot_per_subPupil);
                logger.debug("sigmaphi2_alias = {}", sigmaphi2_alias_fit);
                logger.debug("sigmaphi2_phot  = {}", sigmaphi2_phot);
                logger.debug("sigmaphi2_det   = {}", sigmaphi2_bw);
                logger.debug("sigmaphi2_fixed = {}", sigmaphi2_fixed);
                logger.debug("sigmaphi2       = {}", sigmaphi2);
                logger.debug("strehl          = {}", strehlPerChannel[i]);
            }
        }
        return strehlPerChannel;
    }

    // GRAVITY NGS_VIS: [4.337, 1.864]
    public static final double[] COEFFS_ISO_NGS_VIS = new double[]{4.337, 1.864};
    // GRAVITY NGS_IR: [1.75 , 1.973]
    public static final double[] COEFFS_ISO_NGS_IR = new double[]{1.75, 1.973};

    // GRAVITY LGS VIS: [4.326, 0.39, 1.985]
    public static final double[] COEFFS_ISO_LGS_VIS_LGS = new double[]{4.326, 1.985};
    public static final double[] COEFFS_ISO_LGS_VIS_NGS = new double[]{0.390, 1.985};

    // GRAVITY LGS IR: [4.169, 0.35 , 1.958]
    public static final double[] COEFFS_ISO_LGS_IR__LGS = new double[]{4.169, 1.958};
    public static final double[] COEFFS_ISO_LGS_IR__NGS = new double[]{0.350, 1.958};

    public static double[] strehl_iso_NGS(final Band aoBand, final boolean visWFS, final double[] waveLengths,
                                          final double seeing, final double h0,
                                          final double elevation, final double distAs) {

        return strehl_iso(aoBand, waveLengths, seeing, h0, elevation, distAs,
                (visWFS) ? COEFFS_ISO_NGS_VIS : COEFFS_ISO_NGS_IR);
    }

    public static double[] strehl_iso_LGS(final Band aoBand, final boolean visWFS, final double[] waveLengths,
                                          final double seeing, final double h0,
                                          final double elevation, final double distAs_LGS, final double distAs_NGS) {

        final double[] strehlPerChannel_LGS = strehl_iso(aoBand, waveLengths, seeing, h0, elevation, distAs_LGS,
                (visWFS) ? COEFFS_ISO_LGS_VIS_LGS : COEFFS_ISO_LGS_IR__LGS);

        if (strehlPerChannel_LGS != null) {
            final double[] strehlPerChannel_NGS = strehl_iso(aoBand, waveLengths, seeing, h0, elevation, distAs_NGS,
                    (visWFS) ? COEFFS_ISO_LGS_VIS_NGS : COEFFS_ISO_LGS_IR__NGS);

            if (strehlPerChannel_NGS != null) {
                final int nWLen = waveLengths.length;
                for (int i = 0; i < nWLen; i++) {
                    strehlPerChannel_LGS[i] *= strehlPerChannel_NGS[i];
                }
            }
        }
        return strehlPerChannel_LGS;
    }

    private static double[] strehl_iso(final Band aoBand, final double[] waveLengths,
                                       final double seeing, final double h0,
                                       final double elevation, final double distAs,
                                       final double[] coeffs) {

        if (Double.isNaN(distAs) || (distAs <= 0.0)) {
            // multiply by 1.0 so skip:
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("aoBand          = {}", aoBand);
            logger.debug("waveLengths     = [{} - {}]", waveLengths[0], waveLengths[waveLengths.length - 1]);
            logger.debug("h0              = {}", h0);
            logger.debug("elevation       = {}", elevation);
            logger.debug("distAs          = {}", distAs);
            logger.debug("coeffs          = {}", Arrays.toString(coeffs));
        }

        // avoid cos(0) so use min elevation = 0.5 deg:
        final double zenithAngle = 90.0 - Math.max(elevation, 0.5);

        // airmass: secant of the zenith angle (1/cos(zenith_angle))
        final double airmass = 1.0 / FastMath.cos(FastMath.toRadians(zenithAngle));

        final double lambdaV = 0.5; // seeing is given at 500 nm

        // r0(e)=cos(90-e)^(3/5) * r0
        final double r0_corr = R0_FACTOR * (lambdaV / seeing) * FastMath.pow(airmass, -3.0 / 5.0);

        final int nWLen = waveLengths.length;
        final double[] strehlPerChannel = new double[nWLen];

        double lambdaObs, lambdaRatio, r0;

        for (int i = 0; i < nWLen; i++) {
            lambdaObs = waveLengths[i] * 1e6; // microns

            // use lambdaV as seeing is given for V:
            lambdaRatio = (lambdaObs / lambdaV);

            // r0 at lambda AO:
            r0 = r0_corr * FastMath.pow(lambdaRatio, (6.0 / 5.0));

            final double sigmaphi2 = coeffs[0] * Math.pow(as2rad(distAs) * airmass * h0 / r0, coeffs[1]);

            double sr = FastMath.exp(-sigmaphi2);

            if (sr <= 0.0) {
                sr = 0.0;
            } else if (sr >= 1.0) {
                sr = 1.0;
            }
            strehlPerChannel[i] = sr;
        }
        return strehlPerChannel;
    }

    /* members */
    /** single char band name (upper case) */
    private final String name;
    /** central wave length in microns */
    private final double lambda;
    /** band width in microns */
    private final double bandWidth;
    /** log10 zero magnitude flux at band in W/m^2/m */
    private final double logFluxZero;
    /** maximum strehl ratio */
    private final double strehlMax;
    /** zero point for jansky conversion */
    private final double zeroPoint;
    /** wave length in microns corresponding to the given zero flux */
    private final double lambdaFluxZero;
    /** half band width in microns */
    private final double halfBandWidth;

    /**
     * Custom constructor
     *
     * @param name band name
     * @param lambda central wave length in microns
     * @param bandWidth band width in microns
     * @param logFluxZero log10 zero magnitude flux at band in W/m^2/m
     * @param strehlMax maximum strehl ratio
     */
    private Band(final String name, final double lambda, final double bandWidth, final double logFluxZero,
                 final double strehlMax) {
        this(name, lambda, bandWidth, logFluxZero, strehlMax, Double.NaN);
    }

    /**
     * Custom constructor
     *
     * @param name band name
     * @param lambda central wave length in microns
     * @param bandWidth band width in microns
     * @param logFluxZero log10 zero magnitude flux at band in W/m^2/m
     * @param strehlMax maximum strehl ratio
     * @param zeroPoint zero point for jansky conversion
     */
    private Band(final String name, final double lambda, final double bandWidth, final double logFluxZero,
                 final double strehlMax, final double zeroPoint) {
        this(name, lambda, bandWidth, logFluxZero, strehlMax, zeroPoint, lambda);
    }

    /**
     * Custom constructor
     *
     * @param name band name
     * @param lambda central wave length in microns
     * @param bandWidth band width in microns
     * @param logFluxZero log10 zero magnitude flux at band in W/m^2/m
     * @param strehlMax maximum strehl ratio
     * @param zeroPoint zero point for jansky conversion
     * @param lambdaFluxZero wave length in microns corresponding to the given zero flux
     */
    private Band(final String name, final double lambda, final double bandWidth, final double logFluxZero,
                 final double strehlMax, final double zeroPoint, final double lambdaFluxZero) {
        this.name = name;
        this.lambda = lambda;
        this.bandWidth = bandWidth;
        this.logFluxZero = logFluxZero;
        this.strehlMax = strehlMax;
        this.zeroPoint = zeroPoint;
        this.lambdaFluxZero = lambdaFluxZero;
        this.halfBandWidth = 0.5 * bandWidth;
    }

    /**
     * Return the band name
     *
     * @return band name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the central wave length in microns
     *
     * @return central wave length in microns
     */
    public double getLambda() {
        return lambda;
    }

    public double getLambdaLower() {
        return lambda - halfBandWidth;
    }

    public double getLambdaUpper() {
        return lambda + halfBandWidth;
    }

    /**
     * Return the band width in microns
     *
     * @return band width in microns
     */
    public double getBandWidth() {
        return bandWidth;
    }

    double getHalfBandWidth() {
        return halfBandWidth;
    }

    /**
     * Return the log10 zero magnitude flux at band in W/m^2/m
     *
     * @return log10 zero magnitude flux at band in W/m^2/m
     */
    public double getLogFluxZero() {
        return logFluxZero;
    }

    /**
     * Return the maximum strehl ratio
     *
     * @return maximum strehl ratio
     */
    public double getStrehlMax() {
        return strehlMax;
    }

    /**
     * Return the wave length in microns corresponding to the given zero flux
     *
     * @return wave length in microns corresponding to the given zero flux
     */
    public double getLambdaFluxZero() {
        return lambdaFluxZero;
    }

    /**
     * Return the zero point for jansky conversion
     *
     * @return zero point for jansky conversion
     */
    public double getZeroPoint() {
        return zeroPoint;
    }

    /**
     * Return the number of photons in m^-2.s^-1.m^-1 for an object at magnitude 0
     * @return nb of photons in m^-2.s^-1.m^-1 for an object at magnitude 0
     */
    public double getNbPhotZero() {
        // nb of photons m^-2.s^-1.m^-1 for an object at magnitude 0:
        // note: fzero depends on the spectral band:
        return FastMath.pow(10.0, getLogFluxZero()) * getLambdaFluxZero() * (1e-6 / (H_PLANCK * C_LIGHT));
    }

    /*
            From mag calculator:
            Zeropoint = 290;  Eff. wavelength = 3.45 microns (L)
            Zeropoint = 163;  Eff. wavelength =  4.8 microns (M)       
            Zeropoint = 39.8; Eff. wavelength = 10.1 microns (N)

            From http://svo2.cab.inta-csic.es/theory/fps/index.php?mode=browse&gname=WISE&asttype=
            Filter ID                       λref        λmean       λeff        λmin        λmax        Weff 	ZPν 	ZPλ 	Obs. Facility 	Instrument 	Description
            WISE/WISE.W1                    33526.00	33526.00	33526.00	27540.97	38723.88	6626.42	309.54	8.18e-12	WISE	 	WISE W1 filter
            Generic/Bessell_JHKLM.L         34716.72	34840.75	34517.64	30576.00	38312.00	4595.45	283.94	7.06e-12	 	 	Bessell & Brett 1988 L filter
            WISE/WISE.W2                    46028.00	46028.00	46028.00	39633.26	53413.60	10422.66	171.79	2.42e-12	WISE	 	WISE W2 filter
            Generic/Bessell_JHKLM.M         47272.98	47339.33	47171.11	44415.38	50733.33	3560.00     158.92	2.13e-12	 	 	Bessell & Brett 1988 M filter
            WISE/WISE.W3                    115608.00	115608.00	115608.00	74430.44	172613.43	55055.71	31.67	6.52e-14	WISE	 	WISE W3 filter
            Generic/Johnson_UBVRIJHKL.N     98705.02	103240.68	92055.94	70196.00	136000.00	43744.44	38.58	1.36e-13	 	 	Johnson UBVRIJHKL system, N filter
 
            From alexis (matisse):
            ;reference wavelengths in m
            lambda_phi0 = [3.5e-6,4.5e-6,10.5e-6]
            ;reference flux at zero magnitude in Jy
            phi0_Jy = [286.,182.,37.]
     */
    /**
     * Converts the given magnitude to jansky (if band's zero point is well defined)
     * @param mag magnitude
     * @return jansky value (flux density)
     */
    public double magToJy(final double mag) {
        if (Double.isNaN(mag) || Double.isNaN(zeroPoint)) {
            return Double.NaN;
        }
        if (Double.isNaN(zeroPoint)) {
            return Double.NaN;
        }
        return zeroPoint * Math.pow(10.0, -0.4 * mag); // 1 Jy = 10-26 * W * m-2 * Hz -1
    }

    /**
     * Converts the given flux density (jansky) to magnitude (if band's zero point is well defined)
     * @param flux_density jansky value
     * @return magnitude
     */
    public double jyToMag(final double flux_density) {
        if (Double.isNaN(flux_density) || Double.isNaN(zeroPoint) || (flux_density <= 0.0)) {
            return Double.NaN;
        }
        return -2.5 * (Math.log10(flux_density) - Math.log10(zeroPoint));
    }

    public static void main(String[] args) {
        if (false) {
            System.out.println("as2rad(1): " + as2rad(1.0));
            System.out.println("r0(seeing = 0.5as) : " + getR0(0.5) + " cm");
            System.out.println("r0(seeing = 1.0as) : " + getR0(1.0) + " cm");
            System.out.println("r0(seeing = 1.5as) : " + getR0(1.5) + " cm");

            System.exit(1);
        }

        if (true) {
            /*
            Fix MATISSE bands:
            L: 2.8 - 4.2    f0= 7e-11 W.m − 2.um-1 (3.5um)
            M: 4.2 - 8.0    f0= 2.7e-11 W.m − 2.um-1 (4.5um)
            N: 8.0 - 13.0   f0= 1e-12 W.m − 2.um-1 (10.5um)
             */
            System.out.println("log(f0) L: " + NumberUtils.trimTo3Digits(Math.log10(1e6 * 7e-11)));
            System.out.println("log(f0) M: " + NumberUtils.trimTo3Digits(Math.log10(1e6 * 2.7e-11)));
            System.out.println("log(f0) N: " + NumberUtils.trimTo3Digits(Math.log10(1e6 * 1e-12)));
        }

        for (Band b : values()) {
            double mid = b.getLambda();
            double min = b.getLambdaLower();
            double max = b.getLambdaUpper();
            double lambda0 = b.getLambdaFluxZero();

            final double nzero = b.getNbPhotZero();

            System.out.println("Band: " + b.getName()
                    + " min: " + NumberUtils.trimTo3Digits(min)
                    + " mid: " + NumberUtils.trimTo3Digits(mid)
                    + " max: " + NumberUtils.trimTo3Digits(max)
                    + " l0: " + NumberUtils.trimTo3Digits(lambda0)
                    + " n0 : " + nzero
            );
        }

        if (true) {
            for (double wl = 0.4; wl < 14.0; wl += 0.02) {
                Band b = Band.findBand(wl);
                System.out.println("Band: " + b.getName()
                        + " wl: " + NumberUtils.trimTo3Digits(wl));
            }
        }
        /*
            Band: U min: 0.301 mid: 0.334 max: 0.367 l0: 0.334 n0 : 6.6937549979784128E16
            Band: B min: 0.421 mid: 0.461 max: 0.502 l0: 0.461 n0 : 1.46705974779427744E17
            Band: V min: 0.5 mid: 0.556 max: 0.611 l0: 0.556 n0 : 1.01624433397212688E17
            Band: R min: 0.609 mid: 0.662 max: 0.715 l0: 0.662 n0 : 7.466365193537544E16
            Band: I min: 0.713 mid: 0.869 max: 1.025 l0: 0.869 n0 : 5.0263805017315112E16
            Band: J min: 1.023 mid: 1.236 max: 1.449 l0: 1.236 n0 : 1.9684186281571244E16
            Band: H min: 1.447 mid: 1.679 max: 1.911 l0: 1.679 n0 : 9.708132068674184E15
            Band: K min: 1.909 mid: 2.365 max: 2.821 l0: 2.365 n0 : 4.74099226559661E15
            Band: L min: 2.8 mid: 3.5 max: 4.2 l0: 3.5 n0 : 1.2359229306721938E15
            Band: M min: 4.2 mid: 6.0 max: 7.8 l0: 4.5 n0 : 6.125419819504099E14
            Band: N min: 7.9 mid: 10.5 max: 13.1 l0: 10.5 n0 : 5.285823345220048E13
            Band: Q min: 14.549 mid: 16.575 max: 18.599 l0: 16.575 n0 : 5.64126995424172E12
         */

        // mag to jansky:
        for (Band b : Arrays.asList(Band.L, Band.M, Band.N)) {
            System.out.println("Test Band (" + b + ", zero_point = " + b.getZeroPoint() + ")");

            for (int m = 0; m <= 10; m++) {
                final double mag = m;
                final double flux_density = b.magToJy(mag); // Jy
                final double mag_conv = b.jyToMag(flux_density); // mag
                System.out.println("mag: " + NumberUtils.trimTo3Digits(mag)
                        + " <=> " + NumberUtils.trimTo3Digits(flux_density) + " Jy"
                        + " <=> mag converted: " + NumberUtils.trimTo3Digits(mag_conv));
            }
            for (double j = -1.0; j <= 2; j += 0.2) {
                final double flux_density = (Math.abs(j) < 1e-3) ? 0.0 : j; // Jy
                final double mag = b.jyToMag(flux_density);
                System.out.println("jy: " + NumberUtils.trimTo3Digits(flux_density)
                        + " <=> " + NumberUtils.trimTo3Digits(mag) + " mag"
                );
            }
        }

        if (false) {
            double w = 0.1;

            while (w < 20.0) {
                System.out.println("findBand(" + NumberUtils.trimTo3Digits(w) + "): " + findBand(w));

                w += (w < 1.0) ? 0.02 : 0.05;
            }
        }
    }

    private static double getR0(final double seeing) {
        final double lambdaV = 0.5; // seeing is given at 500 nm

        // r0 at lambda AO:
        final double r0 = R0_FACTOR * (lambdaV / seeing);

        final double r0_grav = 0.98e-6 * lambdaV / as2rad(seeing); // m

        System.out.println("r0:   " + r0);
        System.out.println("r0_g: " + r0_grav);

        System.out.println("ratio: " + (r0 / r0_grav) + " ?= " + (ALX.AIRY_DISK_FWHM_FACTOR / 0.98));

        return r0;
    }

    private static double rad2as(final double angRad) {
        return Math.toDegrees(angRad) * ALX.DEG_IN_ARCSEC;
    }

    private static double as2rad(final double angAs) {
        return Math.toRadians(angAs * ALX.ARCSEC_IN_DEGREES);
    }

}
