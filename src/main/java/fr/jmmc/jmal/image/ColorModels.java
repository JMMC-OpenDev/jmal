/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.image;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.StringUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Give access to several color models.
 *
 * @author Laurent BOURGES.
 */
public class ColorModels {

    /** Class logger */
    protected static final Logger logger = LoggerFactory.getLogger(ColorModels.class.getName());
    /** Maximum number of colors in a 8 byte palette */
    public static final int MAX_COLORS = 256;
    /* SCM prefix */
    private final static String SUFFIX_ALPHA = "Alpha";
    /** force zero surroundings to be black */
    public final static boolean FORCE_ZERO = true;
    /** Color model Aspro */
    public final static String COLOR_MODEL_ASPRO = "aspro";
    /** Color model Aspro Isophot*/
    public final static String COLOR_MODEL_ASPRO_ISOPHOT = "aspro-isophot";
    /** Color model Heat */
    public final static String COLOR_MODEL_HEAT = "heat";
    /** Color model Gray */
    public final static String COLOR_MODEL_GRAY = "Gray";
    /** Color model Gray */
    public final static String COLOR_MODEL_GRAY_LINEAR = "Gray_L";
    /** Color model Earth */
    public final static String COLOR_MODEL_EARTH = "Earth";
    /** Color model Rainbow */
    public final static String COLOR_MODEL_RAINBOW = "Rainbow";
    /** Color model Aspro Isophot*/
    public final static String COLOR_MODEL_RAINBOW_ISOPHOT = "RAINBOW-isophot";
    /** Color model RainbowAlpha */
    public final static String COLOR_MODEL_RAINBOW_ALPHA = "Rainbow" + SUFFIX_ALPHA;
    /** Default color model (Earth) */
    public final static String DEFAULT_COLOR_MODEL = COLOR_MODEL_EARTH;
    /** Color model names (sorted) */
    private final static Vector<String> colorModelNames = new Vector<String>(64);
    /** Color models keyed by names */
    private final static Map<String, IndexColorModel> colorModels = new HashMap<String, IndexColorModel>(64);
    /** Images of each Color model keyed by names */
    private final static Map<String, BufferedImage> colorModelImages = new HashMap<String, BufferedImage>(64);
    /** Cyclic Color models keyed by names */
    private final static Map<String, IndexColorModel> cyclicColorModels = new HashMap<String, IndexColorModel>(64);
    /* default LUT separator */
    private final static String SEPARATOR_LUT = " ";
    /* ColorCET separator */
    private final static String SEPARATOR_CSV = ",";
    /* ColorCET prefix */
    private final static String PREFIX_CET = "CET";
    /* SCM prefix */
    private final static String PREFIX_SCM = "scm7";

    /**
     * Generated array of lut file names in the jmcs folder fr/jmmc/jmal/image/lut/
     */
    private final static String[] LUT_FILES = {
        "aspro.lut",
        "backgr.lut",
        "blue.lut",
        "blulut.lut",
        "green.lut",
        "heat.lut",
        "idl11.lut",
        "idl14.lut",
        "idl15.lut",
        "idl2.lut",
        "idl4.lut",
        "idl5.lut",
        "idl6.lut",
        "isophot.lut",
        "light.lut",
        "mousse.lut",
        "neg.lut",
        "pastel.lut",
        "pseudo1.lut",
        "pseudo2.lut",
        "rainbow.lut",
        "rainbow1.lut",
        "rainbow2.lut",
        "rainbow3.lut",
        "rainbow4.lut",
        "ramp.lut",
        "real.lut",
        "red.lut",
        "smooth.lut",
        /* CET */
        "CET/CET-C1.csv",
        "CET/CET-C1s.csv",
        "CET/CET-C2.csv",
        "CET/CET-C2s.csv",
        "CET/CET-C3.csv",
        "CET/CET-C3s.csv",
        "CET/CET-C4.csv",
        "CET/CET-C4s.csv",
        "CET/CET-C5.csv",
        "CET/CET-C5s.csv",
        "CET/CET-C6.csv",
        "CET/CET-C6s.csv",
        "CET/CET-C7.csv",
        "CET/CET-C7s.csv",
        "CET/CET-CBC1.csv",
        "CET/CET-CBC2.csv",
        "CET/CET-CBD1.csv",
        "CET/CET-CBL1.csv",
        "CET/CET-CBL2.csv",
        "CET/CET-D01.csv",
        "CET/CET-D01A.csv",
        "CET/CET-D02.csv",
        "CET/CET-D03.csv",
        "CET/CET-D04.csv",
        "CET/CET-D06.csv",
        "CET/CET-D07.csv",
        "CET/CET-D08.csv",
        "CET/CET-D09.csv",
        "CET/CET-D10.csv",
        "CET/CET-D11.csv",
        "CET/CET-D12.csv",
        "CET/CET-D13.csv",
        "CET/CET-I1.csv",
        "CET/CET-I2.csv",
        "CET/CET-I3.csv",
        "CET/CET-L01.csv",
        "CET/CET-L02.csv",
        "CET/CET-L03.csv",
        "CET/CET-L04.csv",
        "CET/CET-L05.csv",
        "CET/CET-L06.csv",
        "CET/CET-L07.csv",
        "CET/CET-L08.csv",
        "CET/CET-L09.csv",
        "CET/CET-L10.csv",
        "CET/CET-L11.csv",
        "CET/CET-L12.csv",
        "CET/CET-L13.csv",
        "CET/CET-L14.csv",
        "CET/CET-L15.csv",
        "CET/CET-L16.csv",
        "CET/CET-L17.csv",
        "CET/CET-L18.csv",
        "CET/CET-L19.csv",
        "CET/CET-L20.csv",
        "CET/CET-R1.csv",
        "CET/CET-R2.csv",
        "CET/CET-R3.csv",
        "CET/CET-R4.csv",
        /* SCM7 */
        "scm7/acton.lut",
        "scm7/bam.lut",
        "scm7/bamako.lut",
        "scm7/batlow.lut",
        "scm7/batlowK.lut",
        "scm7/batlowW.lut",
        "scm7/berlin.lut",
        "scm7/bilbao.lut",
        "scm7/broc.lut",
        "scm7/buda.lut",
        "scm7/bukavu.lut",
        "scm7/cork.lut",
        "scm7/davos.lut",
        "scm7/devon.lut",
        "scm7/fes.lut",
        "scm7/grayC.lut",
        "scm7/hawaii.lut",
        "scm7/imola.lut",
        "scm7/lajolla.lut",
        "scm7/lapaz.lut",
        "scm7/lisbon.lut",
        "scm7/nuuk.lut",
        "scm7/oleron.lut",
        "scm7/oslo.lut",
        "scm7/roma.lut",
        "scm7/tofino.lut",
        "scm7/tokyo.lut",
        "scm7/turku.lut",
        "scm7/vanimo.lut",
        "scm7/vik.lut"
    };

    /**
     * Static initialization to prepare the color models (load lut files)
     */
    static {
        final long start = System.nanoTime();

        // color models from lut files :
        for (String name : LUT_FILES) {
            final IndexColorModel colorModel = loadFromFile(name);
            if (colorModel != null) {
                String label = StringUtils.replaceNonFileNameCharsByUnderscore(name.substring(0, name.indexOf('.')));
                if (label.startsWith(PREFIX_CET)) {
                    label = "xt_" + label.substring("CET/".length());
                } else if (label.startsWith(PREFIX_SCM)) {
                    label = "xt_" + label;
                }
                addColorModel(label, colorModel);
            }
        }

        // hard coded color models :
        addColorModel(COLOR_MODEL_EARTH, getColorModel("xt_scm7_batlowW")); // DEFAULT (Earth ~ batlowW)

        addColorModel(COLOR_MODEL_GRAY, getGrayColorModel(false));
        addColorModel(COLOR_MODEL_GRAY_LINEAR, getGrayColorModel(true));

        addColorModel(COLOR_MODEL_RAINBOW, getColorModel("xt_CET-R2")); // or xt_CET-R1/R2 or xt_CET-R4 (vivid)

        // post process color models (variants):
        postProcess();

        Collections.sort(colorModelNames);

        if (logger.isInfoEnabled()) {
            logger.info("ColorModels [{} available] : duration = {} ms.", colorModelNames.size(), 1e-6d * (System.nanoTime() - start));
        }
    }

    private static void postProcess() {
        addColorModel(COLOR_MODEL_ASPRO_ISOPHOT, computeIsoPhotLut(colorModels.get(COLOR_MODEL_ASPRO)));
        addColorModel(COLOR_MODEL_RAINBOW_ISOPHOT, computeIsoPhotLut(colorModels.get(COLOR_MODEL_RAINBOW)));

        addColorModel(COLOR_MODEL_RAINBOW_ALPHA, getColorModelAlpha(getColorModel(COLOR_MODEL_RAINBOW), 0.8f));

        // compute cyclic color models:
        for (String name : new ArrayList<String>(colorModelNames)) {
            computeCyclicModel(name);
        }
    }

    /**
     * create an isophot variant of the aspro color model
     */
    private static IndexColorModel computeIsoPhotLut(final IndexColorModel colorModel) {
        final byte[] r = new byte[MAX_COLORS];
        final byte[] g = new byte[MAX_COLORS];
        final byte[] b = new byte[MAX_COLORS];

        colorModel.getReds(r);
        colorModel.getGreens(g);
        colorModel.getBlues(b);

        final double inc = 1d / (MAX_COLORS - 1);
        final double halfInc = 0.5d * inc;
        double diff;
        double v = inc; // ]0;1]

        // Avoid zero:
        for (int i = 1; i < MAX_COLORS; i++) {
            diff = Math.abs((10d * v - Math.round(10d * v)) * 0.1d);

            if (diff < halfInc) {
                r[i] = (byte) 0xff;
                g[i] = (byte) 0xff;
                b[i] = (byte) 0xff;
            }
            v += inc;
        }
        return new IndexColorModel(8, MAX_COLORS, r, g, b);
    }

    /**
     * create a cyclic variant of the given color model
     * @param name color model name to use
     */
    private static void computeCyclicModel(final String name) {
        final IndexColorModel colorModel = colorModels.get(name);
        if (colorModel != null) {
            final int nColors = colorModel.getMapSize();

            final byte[] t = new byte[nColors]; // temporary
            final byte[] r = new byte[nColors * 2];
            final byte[] g = new byte[nColors * 2];
            final byte[] b = new byte[nColors * 2];

            colorModel.getReds(t);
            for (int i = 0; i < nColors; i++) {
                r[nColors + i] = t[i];
                r[nColors - 1 - i] = t[i];
            }

            colorModel.getGreens(t);
            for (int i = 0; i < nColors; i++) {
                g[nColors + i] = t[i];
                g[nColors - 1 - i] = t[i];
            }

            colorModel.getBlues(t);
            for (int i = 0; i < nColors; i++) {
                b[nColors + i] = t[i];
                b[nColors - 1 - i] = t[i];
            }

            // 9 means 9bytes => up to 512 colors !
            cyclicColorModels.put(name, new IndexColorModel(9, 2 * nColors, r, g, b));
        }
    }

    private static void addColorModel(final String name, final IndexColorModel colorModel) {
        if (!name.endsWith(SUFFIX_ALPHA)) {
            colorModelNames.add(name);
        }
        colorModels.put(name, colorModel);
        colorModelImages.put(name, createImage(colorModel));
    }

    /**
     * Forbidden constructor
     */
    private ColorModels() {
        // no-op
    }

    /**
     * Return the Color model names (sorted)
     * @return Color model names (sorted)
     */
    public static Vector<String> getColorModelNames() {
        return colorModelNames;
    }

    /**
     * Return the default IndexColorModel
     * @return IndexColorModel
     */
    public static IndexColorModel getDefaultColorModel() {
        return getColorModel(DEFAULT_COLOR_MODEL);
    }

    /**
     * Return the IndexColorModel given its name
     * @param name
     * @return IndexColorModel or the default IndexColorModel if the name was not found
     */
    public static IndexColorModel getColorModel(final String name) {
        IndexColorModel colorModel = colorModels.get(name);
        if (colorModel == null) {
            return getDefaultColorModel();
        }
        return colorModel;
    }

    /**
     * Return the image of the IndexColorModel given its name
     * @param name
     * @return BufferedImage or null if the name was not found
     */
    public static BufferedImage getColorModelImage(final String name) {
        return colorModelImages.get(name);
    }

    /**
     * Return the cyclic IndexColorModel given its name
     * @param name
     * @return IndexColorModel or the default IndexColorModel if the name was not found
     */
    public static IndexColorModel getCyclicColorModel(final String name) {
        IndexColorModel colorModel = cyclicColorModels.get(name);
        if (colorModel == null) {
            return getDefaultColorModel();
        }
        return colorModel;
    }

    /* Private methods */
    /**
     * @return a 'rainbow' color model with given opacity
     * @param alpha opacity
     */
    private static IndexColorModel getColorModelAlpha(final IndexColorModel colorModel, final float alpha) {
        final byte[] r = new byte[MAX_COLORS];
        final byte[] g = new byte[MAX_COLORS];
        final byte[] b = new byte[MAX_COLORS];
        final byte[] a = new byte[MAX_COLORS];

        colorModel.getReds(r);
        colorModel.getGreens(g);
        colorModel.getBlues(b);

        Arrays.fill(a, (byte) Math.round(255 * alpha));

        return new IndexColorModel(8, MAX_COLORS, r, g, b, a);
    }

    /** @return the gray color model */
    private static IndexColorModel getGrayColorModel(final boolean linear) {
        final byte[] r = new byte[MAX_COLORS];
        final byte[] g = new byte[MAX_COLORS];
        final byte[] b = new byte[MAX_COLORS];

        final float inc = 1f / (MAX_COLORS - 1);

        for (int i = 0; i < MAX_COLORS; i++) {
            // linear but sRGB is not linear:
            final byte v = (byte) (linear ? RGB_to_sRGBi(i * inc) : i);
            r[i] = v;
            g[i] = v;
            b[i] = v;
        }
        return new IndexColorModel(8, MAX_COLORS, r, g, b);
    }

    private static byte[][] loadLutFromFile(final String name) {
        BufferedReader reader = null;
        try {
            final String path = "lut/" + name;

            final InputStream in = ColorModels.class.getResourceAsStream(path);

            reader = new BufferedReader(new InputStreamReader(in, "US-ASCII"));

            String line;
            StringTokenizer tok;

            // outputs :
            final float[] rf = new float[MAX_COLORS];
            final float[] gf = new float[MAX_COLORS];
            final float[] bf = new float[MAX_COLORS];

            if (name.startsWith(PREFIX_CET)) {
                final int LEN = 255;
                for (int i = 0; (line = reader.readLine()) != null && i <= LEN; i++) {
                    tok = new StringTokenizer(line, SEPARATOR_CSV);

                    if (tok.countTokens() == 3) {
                        rf[i] = 255f * Float.parseFloat(tok.nextToken());
                        gf[i] = 255f * Float.parseFloat(tok.nextToken());
                        bf[i] = 255f * Float.parseFloat(tok.nextToken());
                    }
                }
            } else if (name.startsWith(PREFIX_SCM)) {
                final int LEN = 255;
                for (int i = 0; (line = reader.readLine()) != null && i <= LEN; i++) {
                    tok = new StringTokenizer(line, SEPARATOR_LUT);

                    if (tok.countTokens() == 3) {
                        rf[i] = 255f * Float.parseFloat(tok.nextToken());
                        gf[i] = 255f * Float.parseFloat(tok.nextToken());
                        bf[i] = 255f * Float.parseFloat(tok.nextToken());
                    }
                }
            } else {
                final int LEN = 128;

                for (int i = 0, n = 0; (line = reader.readLine()) != null && n <= LEN; i += 2, n++) {
                    tok = new StringTokenizer(line, SEPARATOR_LUT);

                    if (tok.countTokens() == 3) {
                        rf[i] = 255f * Float.parseFloat(tok.nextToken());
                        gf[i] = 255f * Float.parseFloat(tok.nextToken());
                        bf[i] = 255f * Float.parseFloat(tok.nextToken());
                    }
                }

                for (int i = 1, j, k, size = MAX_COLORS - 2; i < size; i += 2) {
                    j = i - 1;
                    k = i + 1;
                    rf[i] = 0.5f * (rf[j] + rf[k]);
                    gf[i] = 0.5f * (gf[j] + gf[k]);
                    bf[i] = 0.5f * (bf[j] + bf[k]);
                }

                // special case : color 255 :
                rf[MAX_COLORS - 1] = rf[MAX_COLORS - 2];
                gf[MAX_COLORS - 1] = gf[MAX_COLORS - 2];
                bf[MAX_COLORS - 1] = bf[MAX_COLORS - 2];

                if (FORCE_ZERO) {
                    // force to have black color :
                    final int REF_COLOR = 4;

                    final float rfRef = rf[REF_COLOR];
                    final float gfRef = gf[REF_COLOR];
                    final float bfRef = bf[REF_COLOR];

                    for (int i = REF_COLOR - 1; i >= 0; i--) {
                        rf[i] = rfRef * i / REF_COLOR;
                        gf[i] = gfRef * i / REF_COLOR;
                        bf[i] = bfRef * i / REF_COLOR;
                    }
                }
            }

            final byte[] r = new byte[MAX_COLORS];
            final byte[] g = new byte[MAX_COLORS];
            final byte[] b = new byte[MAX_COLORS];

            for (int i = 0; i < MAX_COLORS; i++) {
                r[i] = (byte) rf[i];
                g[i] = (byte) gf[i];
                b[i] = (byte) bf[i];
            }

            return new byte[][]{r, g, b};

        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException("loadLutFromFile failure: ", uee);
        } catch (IOException ioe) {
            logger.info("loadLutFromFile failure: {}", name, ioe);
        } catch (RuntimeException re) {
            logger.info("loadLutFromFile failure: {}", name, re);
        } finally {
            FileUtils.closeFile(reader);
        }
        return null;
    }

    private static IndexColorModel loadFromFile(final String name) {
        final byte[][] rgb = loadLutFromFile(name);

        if (rgb != null) {
            return new IndexColorModel(8, MAX_COLORS, rgb[0], rgb[1], rgb[2]);
        }
        return null;
    }

    /**
     * List directory contents for a resource folder.
     *
     * @throws URISyntaxException
     * @throws IOException
     */
    public static String[] getResourceListing(final Class<?> clazz, final String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);

        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    /**
     * Test code and generate the array of lut file names in the jmcs folder fr/jmmc/mcs/image/lut/
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // Prepare the lut file list :
        try {
            final StringBuilder sb = new StringBuilder(512);
            sb.append("private final static String[] LUT_FILES = {\n");

            final String[] lutFiles = getResourceListing(ColorModels.class, "fr/jmmc/jmal/image/lut/");
            if (lutFiles != null) {
                Arrays.sort(lutFiles);

                for (String name : lutFiles) {
                    sb.append('"').append(name).append('"').append(", \n");
                }
            }

            final String[] lutFilesCET = getResourceListing(ColorModels.class, "fr/jmmc/jmal/image/lut/CET");
            if (lutFilesCET != null) {
                Arrays.sort(lutFilesCET);

                for (String name : lutFilesCET) {
                    if (name.endsWith("csv")) {
                        sb.append('"').append(PREFIX_CET).append('/').append(name).append('"').append(", \n");
                    }
                }
            }

            final String[] lutFilesSCM7 = getResourceListing(ColorModels.class, "fr/jmmc/jmal/image/lut/scm7");
            if (lutFilesSCM7 != null) {
                Arrays.sort(lutFilesSCM7);

                for (String name : lutFilesSCM7) {
                    if (name.endsWith("lut")) {
                        sb.append('"').append(PREFIX_SCM).append('/').append(name).append('"').append(", \n");
                    }
                }
            }

            final int pos = sb.lastIndexOf(",");
            if (pos != -1) {
                sb.deleteCharAt(pos);
            }
            sb.append("};");

            logger.info("lut files :\n{}", sb.toString());

        } catch (Exception e) { // main (test)
            logger.info("resource listing failure: ", e);
        }

        // test case :
        ColorModels.getDefaultColorModel();
    }

    /** alpha integer mask */
    private static BufferedImage createImage(final IndexColorModel colorModel) {
        final int mapSize = colorModel.getMapSize();
        final int iMaxColor = mapSize - 1;

        final int scaleInt = Math.max(1, SwingUtils.adjustUISizeCeil(1));

        final int width = scaleInt * MAX_COLORS; // always 256px to be consistent among color models
        final int height = scaleInt * 32;

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        try {
            final float ratio = MAX_COLORS / mapSize;

            float x = 0f;
            for (int n = 0; n < mapSize; n++) {
                final int rgb = ImageUtils.getRGB(colorModel, iMaxColor, n, 0);

                // paint color:
                g2d.setColor(new Color(rgb)); // ignore alpha

                final float x2 = x + scaleInt * ratio;

                g2d.fillRect(Math.round(x), 0, Math.round(x2), height);
                x = x2;
            }
        } finally {
            g2d.dispose();
        }
        return image;
    }

    // linear RGB to sRGB (gamma profile):
    private static int RGB_to_sRGBi(final float val) {
        return Math.round(255f * RGB_to_sRGB(val));
    }

    private static float RGB_to_sRGB(final float c) {
        if (c <= 0f) {
            return 0f;
        }
        if (c >= 1f) {
            return 1f;
        }
        if (c >= 0.0031308f) {
            return 1.055f * ((float) Math.pow(c, 1.0 / 2.4)) - 0.055f;
        }
        return c * 12.92f;
    }

}
