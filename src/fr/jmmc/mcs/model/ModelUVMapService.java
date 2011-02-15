/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: ModelUVMapService.java,v 1.7 2011-02-04 16:41:49 bourgesl Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.6  2010/09/24 15:44:15  bourgesl
 * removed catch RuntimeException to get it at higher level
 *
 * Revision 1.5  2010/05/04 15:40:58  bourgesl
 * javadoc
 *
 * Revision 1.4  2010/02/18 15:51:18  bourgesl
 * added parameter argument validation and propagation (illegal argument exception)
 *
 * Revision 1.3  2010/02/04 14:43:36  bourgesl
 * added UVMapData bean to keep several data related to uv map in order to conserve the value / color mapping and uv area while zooming on uv map
 *
 * Revision 1.2  2010/02/03 16:05:46  bourgesl
 * Added fast thread interruption checks for asynchronous uv map computation
 *
 * Revision 1.1  2010/02/03 09:51:46  bourgesl
 * uv map rendering service
 *
 */
package fr.jmmc.mcs.model;

import fr.jmmc.mcs.image.ColorModels;
import fr.jmmc.mcs.image.ImageUtils;
import fr.jmmc.mcs.model.targetmodel.Model;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.math.complex.Complex;

/**
 * This class generates an UV Map Image for given target Models and UV area
 * @author bourgesl
 */
public final class ModelUVMapService {

  /** Class Name */
  private static final String className_ = "fr.jmmc.mcs.model.ModelUVMapService";
  /** Class logger */
  private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
          className_);
  /** default image width / height */
  private final static int DEFAULT_IMAGE_SIZE = 512;
  /** default color model (aspro - Rainbow) */
  private final static IndexColorModel DEFAULT_COLOR_MODEL = ColorModels.getColorModel("aspro");

  /**
   * Image modes (amplitude, phase)
   */
  public enum ImageMode {

    /** Amplitude */
    AMP,
    /** Phase */
    PHASE
  }

  /**
   * Forbidden constructor
   */
  private ModelUVMapService() {
    // no-op
  }

  /**
   * Compute the UV Map for the given models and UV ranges
   * @param models list of models to use
   * @param uvRect UV frequency area in rad-1
   * @param mode image mode (amplitude or phase)
   * @return UVMapData
   */
  public static UVMapData computeUVMap(final List<Model> models,
                                       final Rectangle2D.Double uvRect,
                                       final ImageMode mode) {
    return computeUVMap(models, uvRect, null, null, mode, DEFAULT_IMAGE_SIZE, DEFAULT_COLOR_MODEL);
  }

  /**
   * Compute the UV Map for the given models and UV ranges
   * @param models list of models to use
   * @param uvRect UV frequency area in rad-1
   * @param refMin minimum reference double value used only for sub images
   * @param refMax maximum reference double value used only for sub images
   * @param mode image mode (amplitude or phase)
   * @return UVMapData
   */
  public static UVMapData computeUVMap(final List<Model> models,
                                       final Rectangle2D.Double uvRect,
                                       final Float refMin, final Float refMax,
                                       final ImageMode mode) {
    return computeUVMap(models, uvRect, refMin, refMax, mode, DEFAULT_IMAGE_SIZE, DEFAULT_COLOR_MODEL);
  }

  /**
   * Compute the UV Map for the given models and UV ranges
   * @param models list of models to use
   * @param uvRect UV frequency area in rad-1
   * @param refMin minimum reference value used only for sub images
   * @param refMax maximum reference value used only for sub images
   * @param mode image mode (amplitude or phase)
   * @param imageSize number of pixels for both width and height of the generated image
   * @param colorModel color model to use
   * @return UVMapData
   */
  public static UVMapData computeUVMap(final List<Model> models,
                                       final Rectangle2D.Double uvRect,
                                       final Float refMin, final Float refMax,
                                       final ImageMode mode,
                                       final int imageSize,
                                       final IndexColorModel colorModel) {

    UVMapData uvMapData = null;

    if (models != null && !models.isEmpty()) {

      /** Get the current thread to check if the computation is interrupted */
      final Thread currentThread = Thread.currentThread();

      // Start the computations :
      final long start = System.nanoTime();

      try {
        // square size :
        final int size = imageSize * imageSize;

        // this step indicates when the thread.isInterrupted() is called in the for loop
        final int stepInterrupt = size / 20;

        // 1 - Prepare UFreq and VFreq arrays :
        double[] u = computeFrequencySamples(imageSize, uvRect.getX(), uvRect.getMaxX());
        double[] v = computeFrequencySamples(imageSize, uvRect.getY(), uvRect.getMaxY());

        // fast interrupt :
        if (currentThread.isInterrupted()) {
          return null;
        }

        double[] ufreq = new double[size];
        double[] vfreq = new double[size];

        // Note : the image is produced from an array where 0,0 corresponds to the upper left corner
        // whereas it corresponds in UV to the lower U and Upper V coordinates => inverse the V axis

        for (int j = 0, l = 0; j < imageSize; j++) {
          // inverse the v axis for the image :
          l = imageSize - j - 1;
          for (int i = 0, k = 0; i < imageSize; i++) {
            k = imageSize * j + i;
            ufreq[k] = u[i];
            vfreq[k] = v[l];
          }
        }
        // force GC :
        u = null;
        v = null;

        // fast interrupt :
        if (currentThread.isInterrupted()) {
          return null;
        }

        // 2 - Compute complex visibility for the given models :

        Complex[] vis = ModelManager.getInstance().computeModels(ufreq, vfreq, models);

        // fast interrupt :
        if (currentThread.isInterrupted()) {
          return null;
        }

        // force GC :
        ufreq = null;
        vfreq = null;

        // 3 - Extract the amplitude/phase to get the uv map :

        // use single precision for performance (image needs not double precision) :
        float[] data = new float[size];

        float val;
        float valMin = Float.POSITIVE_INFINITY;
        float valMax = Float.NEGATIVE_INFINITY;

        switch (mode) {
          case AMP:
            for (int i = 0; i < size; i++) {
              // amplitude = complex modulus (abs in commons-math) :
              val = (float)vis[i].abs();
              data[i] = val;

              if (val < valMin) {
                valMin = val;
              }
              if (val > valMax) {
                valMax = val;
              }
              // fast interrupt :
              if (i % stepInterrupt == 0 && currentThread.isInterrupted()) {
                return null;
              }
            }
            if (logger.isLoggable(Level.FINE)) {
              logger.fine("VIS_AMP in [" + valMin + ", " + valMax + "]");
            }
            break;
          case PHASE:
            for (int i = 0; i < size; i++) {
              // phase [-PI;PI] = complex phase (argument in commons-math) :
              val = (float)vis[i].getArgument();
              data[i] = val;

              if (val < valMin) {
                valMin = val;
              }
              if (val > valMax) {
                valMax = val;
              }
              // fast interrupt :
              if (i % stepInterrupt == 0 && currentThread.isInterrupted()) {
                return null;
              }
            }
            if (logger.isLoggable(Level.FINE)) {
              logger.fine("VIS_PHI in [" + valMin + ", " + valMax + "]");
            }
            break;
          default:
        }

        // force GC :
        vis = null;

        // fast interrupt :
        if (currentThread.isInterrupted()) {
          return null;
        }

        // 4 - Get the image with the given color model :

        // use the given reference extrema to make the value to color conversion :
        final float min = (refMin != null) ? refMin.floatValue() : valMin;
        final float max = (refMax != null) ? refMax.floatValue() : valMax;

        if (logger.isLoggable(Level.FINE)) {
          logger.fine("value range in [" + min + ", " + max + "]");
        }

        final BufferedImage uvMap = ImageUtils.createImage(imageSize, imageSize, data, min, max, colorModel);

        // force gc :
        data = null;

        // fast interrupt :
        if (currentThread.isInterrupted()) {
          return null;
        }

        // results :
        uvMapData = new UVMapData(mode, imageSize, colorModel, uvRect, Float.valueOf(min), Float.valueOf(max), uvMap);

      } catch (IllegalArgumentException iae) {
        // ModelManager.compute throws an IllegalArgumentException if a parameter value is invalid :
        if (logger.isLoggable(Level.WARNING)) {
          logger.log(Level.WARNING, "invalid argument :", iae);
        }
        throw iae;
      }

      if (logger.isLoggable(Level.INFO)) {
        logger.info("compute : duration = " + 1e-6d * (System.nanoTime() - start) + " ms.");
      }
    }

    return uvMapData;
  }

  /**
   * Return the frequencies in rad-1
   * @param nbSamples number of sampled values
   * @param min minimum frequency value
   * @param max maximum frequency value
   * @return sampled frequencies in rad-1
   */
  private static double[] computeFrequencySamples(final int nbSamples, final double min, final double max) {
    final double[] freq = new double[nbSamples];

    final double step = (max - min) / nbSamples;

    freq[0] = min;
    for (int i = 1; i < nbSamples; i++) {
      freq[i] = freq[i - 1] + step;
    }

    return freq;
  }
}