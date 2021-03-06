/**
 * *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 *****************************************************************************
 */
package fr.jmmc.jmal.image;

import fr.jmmc.jmal.util.GenericWeakCache;
import fr.jmmc.jmcs.util.concurrent.InterruptedJobException;
import fr.jmmc.jmcs.util.concurrent.ParallelJobExecutor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains several utility methods to produce Image objects from raw data
 *
 * @author Laurent BOURGES.
 */
public final class ImageUtils {

    /** Class logger */
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class.getName());
    /** debug rotate flag (red zone) */
    private final static boolean DEBUG_ROTATE = false;
    /** alpha integer mask */
    private final static int ALPHA_MASK = 0xff << 24;
    /** flag to use RGB color interpolation */
    public final static boolean USE_RGB_INTERPOLATION = true;
    /** threshold to use parallel jobs (256 x 256 pixels) */
    private final static int JOB_THRESHOLD = 256 * 256 - 1;
    /** Graphics image interpolation */
    private static Object IMAGE_INTERPOLATION = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
    /** Jmcs Parallel Job executor */
    private final static ParallelJobExecutor jobExecutor = ParallelJobExecutor.getInstance();
    /** weak image cache for createImage()/recycleImage() */
    private final static GenericWeakCache<BufferedImage> imageCache = new GenericWeakCache<BufferedImage>("ImageUtils") {

        @Override
        protected boolean checkSizes(BufferedImage image, int length, int length2) {
            return (image.getWidth() == length && image.getHeight() == length2);
        }

        @Override
        public String getSizes(BufferedImage image) {
            return String.format("%d x %d", image.getWidth(), image.getHeight());
        }
    };

    public enum ImageInterpolation {
        /** No image interpolation */
        None,
        /** Bilinear image interpolation */
        Bilinear,
        /** Bicubic image interpolation */
        Bicubic;
    }

    /**
     * Forbidden constructor
     */
    private ImageUtils() {
        // no-op
    }

    public static Object getImageInterpolationHint() {
        return IMAGE_INTERPOLATION;
    }

    public static ImageInterpolation getImageInterpolation() {
        if (IMAGE_INTERPOLATION == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
            return ImageInterpolation.None;
        } else if (IMAGE_INTERPOLATION == RenderingHints.VALUE_INTERPOLATION_BILINEAR) {
            return ImageInterpolation.Bilinear;
        } else {
            return ImageInterpolation.Bicubic;
        }
    }

    public static void setImageInterpolation(final ImageInterpolation interpolation) {
        switch (interpolation) {
            case None:
                IMAGE_INTERPOLATION = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                break;
            case Bilinear:
                IMAGE_INTERPOLATION = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                break;
            default:
            case Bicubic:
                IMAGE_INTERPOLATION = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
        }
    }

    /**
     * Convert min/max range to the given color scaling method
     * @param min data min value
     * @param max data max value
     * @param colorScale color scaling method
     * @return scaled min / max values
     */
    public static float[] scaleMinMax(final float min, final float max, final ColorScale colorScale) {

        final float scaledMin;
        final float scaledMax;
        switch (colorScale) {
            default:
            case LINEAR:
                scaledMin = min;
                scaledMax = max;
                break;
            case LOGARITHMIC:
                // protect against negative values including zero:
                if (min <= 0f || max <= 0f) {
                    throw new IllegalArgumentException("Negative or zero values in range[" + min + " - " + max + "] !");
                }
                scaledMin = (float) Math.log10(min);
                scaledMax = (float) Math.log10(max);

                if (logger.isDebugEnabled()) {
                    logger.debug("scaleMinMax: new range[{} - {}]", scaledMin, scaledMax);
                }
                break;
        }
        return new float[]{scaledMin, scaledMax};
    }

    /**
     * Return the value to pixel coefficient
     *
     * @param min data min value
     * @param max data max value
     * @param iMaxColor maximum number of colors to use
     * @return data to color linear scaling factor
     */
    public static float computeScalingFactor(final float min, final float max, final int iMaxColor) {
        final int iMax = iMaxColor - 1;

        float factor = iMax / (max - min);

        if (factor == 0f) {
            factor = 1f;
        }

        return factor;
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (1D)
     * @param min lower data value (lower threshold)
     * @param max upper data value (upper threshold)
     * @param colorModel color model
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[] array, final float min, final float max,
                                            final IndexColorModel colorModel) {
        return ImageUtils.createImage(width, height, array, min, max, colorModel, ColorScale.LINEAR);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (1D)
     * @param min lower data value (lower threshold)
     * @param max upper data value (upper threshold)
     * @param colorModel color model
     * @param colorScale color scaling method
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[] array, final float min, final float max,
                                            final IndexColorModel colorModel,
                                            final ColorScale colorScale) {

        final float[] scaledMinMax = scaleMinMax(min, max, colorScale);

        final float scalingFactor = computeScalingFactor(scaledMinMax[0], scaledMinMax[1], colorModel.getMapSize());

        return ImageUtils.createImage(width, height, array, scaledMinMax[0], colorModel, scalingFactor, colorScale);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (1D)
     * @param min lower data value (lower threshold)
     * @param colorModel color model
     * @param scalingFactor value to pixel coefficient
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[] array, final float min,
                                            final IndexColorModel colorModel, final float scalingFactor) {
        return ImageUtils.createImage(width, height, array, min, colorModel, scalingFactor, ColorScale.LINEAR);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (1D)
     * @param scaledMin minimum data value or log10(min) 
     * @param colorModel color model
     * @param scalingFactor value to pixel coefficient
     * @param colorScale color scaling method
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[] array, final float scaledMin,
                                            final IndexColorModel colorModel, final float scalingFactor,
                                            final ColorScale colorScale) {
        if (array == null) {
            throw new IllegalStateException("Undefined data array.");
        }
        if (array.length != (width * height)) {
            throw new IllegalStateException("Invalid data array size: " + array.length + "; expected: " + (width * height) + ".");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("createImage: using array of size {} x {}", width, height);
        }

        // Start the computations :
        final long start = System.nanoTime();

        final BufferedImage image = createImage(width, height, colorModel);
        final WritableRaster imageRaster = image.getRaster();
        final DataBuffer dataBuffer = imageRaster.getDataBuffer();

        // Should split the computation in parts ?
        // i.e. enough big compute task ?
        final int nJobs = (array.length >= JOB_THRESHOLD) ? jobExecutor.getMaxParallelJob() : 1;

        final ComputeImagePart[] jobs = new ComputeImagePart[nJobs];

        for (int i = 0; i < nJobs; i++) {
            // ensure last job goes until lineEnd:
            jobs[i] = new ComputeImagePart(array, scaledMin, colorModel, scalingFactor, colorScale, dataBuffer, i, nJobs);
        }

        // execute jobs in parallel or using current thread if only one job (throws InterruptedJobException if interrupted):
        jobExecutor.forkAndJoin("ImageUtils.createImage", jobs);

        if (logger.isDebugEnabled()) {
            logger.debug("compute : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }

        return image;
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (2D)
     * @param min lower data value (lower threshold)
     * @param max upper data value (upper threshold)
     * @param colorModel color model
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[][] array, final float min, final float max,
                                            final IndexColorModel colorModel) {
        return ImageUtils.createImage(width, height, array, min, max, colorModel, ColorScale.LINEAR);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (2D)
     * @param min lower data value (lower threshold)
     * @param max upper data value (upper threshold)
     * @param colorModel color model
     * @param colorScale color scaling method
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[][] array, final float min, final float max,
                                            final IndexColorModel colorModel, final ColorScale colorScale) {

        final float[] scaledMinMax = scaleMinMax(min, max, colorScale);

        final float scalingFactor = computeScalingFactor(scaledMinMax[0], scaledMinMax[1], colorModel.getMapSize());

        return ImageUtils.createImage(width, height, array, scaledMinMax[0], colorModel, scalingFactor, colorScale);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (2D) [rows][cols]
     * @param min lower data value (lower threshold)
     * @param colorModel color model
     * @param scalingFactor value to pixel coefficient
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[][] array, final float min,
                                            final IndexColorModel colorModel, final float scalingFactor) {
        return ImageUtils.createImage(width, height, array, min, colorModel, scalingFactor, ColorScale.LINEAR);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param array data array (2D) [rows][cols]
     * @param scaledMin minimum data value or log10(min) 
     * @param colorModel color model
     * @param scalingFactor value to pixel coefficient
     * @param colorScale color scaling method
     * @return new BufferedImage or null if interrupted
     * 
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     * @throws RuntimeException if any exception occured during the computation
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final float[][] array, final float scaledMin,
                                            final IndexColorModel colorModel, final float scalingFactor,
                                            final ColorScale colorScale) {
        if (array == null) {
            throw new IllegalStateException("Undefined data array.");
        }
        if (array.length != height) {
            throw new IllegalStateException("Invalid data array size: " + array.length + "; expected: " + height + ".");
        }
        if (array[0].length != width) {
            throw new IllegalStateException("Invalid data array size: " + array[0].length + "; expected: " + width + ".");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("createImage: using array of size {} x {}", width, height);
        }

        // Start the computations :
        final long start = System.nanoTime();

        final BufferedImage image = createImage(width, height, colorModel);
        final WritableRaster imageRaster = image.getRaster();
        final DataBuffer dataBuffer = imageRaster.getDataBuffer();

        // Should split the computation in parts ?
        // i.e. enough big compute task ?
        final int nJobs = ((width * height) >= JOB_THRESHOLD) ? jobExecutor.getMaxParallelJob() : 1;

        final ComputeImagePart[] jobs = new ComputeImagePart[nJobs];

        for (int i = 0; i < nJobs; i++) {
            // ensure last job goes until lineEnd:
            jobs[i] = new ComputeImagePart(array, width, height, scaledMin, colorModel, scalingFactor, colorScale, dataBuffer, i, nJobs);
        }

        // execute jobs in parallel or using current thread if only one job (throws InterruptedJobException if interrupted):
        try {
            jobExecutor.forkAndJoin("ImageUtils.createImage", jobs);
        } catch (RuntimeException re) {
            logger.debug("recycleImage <= interrupted job:");
            // recycle image:
            recycleImage(image);
            // rethrow exception:
            throw re;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("compute : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }

        return image;
    }

    public static BufferedImage transformImage(final BufferedImage image, final IndexColorModel colorModel,
                                               final AffineTransform at, final int w, final int h) {
        return transformImage(image, colorModel, at, w, h, true);
    }

    public static BufferedImage transformImage(final BufferedImage image, final IndexColorModel colorModel,
                                               final AffineTransform at, final int w, final int h,
                                               final boolean recycle) {

        // get the original image's width and height
        final int iw = image.getWidth();
        final int ih = image.getHeight();

        final BufferedImage outImage = createImage(w, h, colorModel);

        final Graphics2D g2d = (Graphics2D) outImage.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g2d.setBackground(
                    DEBUG_ROTATE ? Color.RED : new Color(colorModel.getRGB(0)) // min color means background
            );
            g2d.clearRect(0, 0, w, h);

            // Force rendering hints :
            // set quality flags:
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

            // Use bicubic interpolation (slower) for quality:
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, ImageUtils.getImageInterpolationHint());

            // compute the location to draw the original image on the new image
            final double offX = (w - iw) / 2.0;
            final double offY = (h - ih) / 2.0;

            // move first to the offsetted origin in the new image (larger one):
            g2d.translate(offX, offY);
            // concatenate transforms based on the original image:
            g2d.transform(at);

            g2d.drawImage(image, 0, 0, iw, ih, null);

        } finally {
            g2d.dispose();
        }
        if (recycle) {
            // recycle previous image:
            ImageUtils.recycleImage(image);
        }
        return outImage;
    }

    public static Rectangle2D.Double getBoundingBox(final AffineTransform at, final Rectangle2D box) {
        final double x0 = box.getMinX();
        final double x1 = box.getMaxX();
        final double y0 = box.getMinY();
        final double y1 = box.getMaxY();

        double xmin, xmax, ymin, ymax;
        double x, y;

        final Point2D ptSrc = new Point2D.Double();
        final Point2D ptDst = new Point2D.Double();

        // x0, y0:
        ptSrc.setLocation(x0, y0);
        at.transform(ptSrc, ptDst);
        x = ptDst.getX();
        y = ptDst.getY();

        xmin = xmax = x;
        ymin = ymax = y;

        // x1, y0:
        ptSrc.setLocation(x1, y0);
        at.transform(ptSrc, ptDst);
        x = ptDst.getX();
        y = ptDst.getY();

        if (x < xmin) {
            xmin = x;
        } else if (x > xmax) {
            xmax = x;
        }
        if (y < ymin) {
            ymin = y;
        } else if (y > ymax) {
            ymax = y;
        }

        // x0, y1:
        ptSrc.setLocation(x0, y1);
        at.transform(ptSrc, ptDst);
        x = ptDst.getX();
        y = ptDst.getY();

        if (x < xmin) {
            xmin = x;
        } else if (x > xmax) {
            xmax = x;
        }
        if (y < ymin) {
            ymin = y;
        } else if (y > ymax) {
            ymax = y;
        }

        // x1, y1:
        ptSrc.setLocation(x1, y1);
        at.transform(ptSrc, ptDst);
        x = ptDst.getX();
        y = ptDst.getY();

        if (x < xmin) {
            xmin = x;
        } else if (x > xmax) {
            xmax = x;
        }
        if (y < ymin) {
            ymin = y;
        } else if (y > ymax) {
            ymax = y;
        }

        return new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /**
     * Create an Image from the given data array using the specified Color Model
     *
     * @param width image width
     * @param height image height
     * @param colorModel color model
     * @return new BufferedImage
     */
    public static BufferedImage createImage(final int width, final int height,
                                            final IndexColorModel colorModel) {

        if (logger.isDebugEnabled()) {
            logger.debug("createImage: size {} x {}", width, height);
        }

        BufferedImage image = imageCache.getItem(width, height);
        if (image != null) {
            return image;
        }

        final ColorModel imageColorModel;
        final WritableRaster imageRaster;

        // TODO: memory waste: reuse images if possible !!
        if (USE_RGB_INTERPOLATION) {
            imageColorModel = ColorModel.getRGBdefault();
            imageRaster = imageColorModel.createCompatibleWritableRaster(width, height);
        } else {
            imageColorModel = colorModel;
            imageRaster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE, width, height, new int[]{0xFF}, null);
        }

        // do not initialize raster pixels
        image = new BufferedImage(imageColorModel, imageRaster, false, null);

        if (logger.isDebugEnabled()) {
            logger.debug("create image[{} x {}] @ {}", image.getWidth(), image.getHeight(), image.hashCode());
        }

        return image;
    }

    public static void recycleImage(final BufferedImage image) {
        imageCache.putItem(image);
    }

    /**
     * Return the color index using the indexed color model for the given value (linear scale)
     *
     * @param iMaxColor index of the highest color
     * @param value data value to convert between 0.0 and 255.0
     * @return color index
     */
    public static int getColor(final int iMaxColor, final float value) {
        int colorIdx = Math.round(value);

        if (colorIdx < 0) {
            colorIdx = 0;
        } else if (colorIdx > iMaxColor) {
            colorIdx = iMaxColor;
        }
        return colorIdx;
    }

    /**
     * Return an RGB color (32bits) using the given color model for the given value (linear scale)
     *
     * @param colorModel color model
     * @param iMaxColor index of the highest color
     * @param value data value to convert between 0.0 and colorModel.getMapSize() - 1 (128 or 240 or 255...)
     * @param alphaMask alpha mask (0 - 255) << 24
     * @return RGB color
     */
    public static int getRGB(final IndexColorModel colorModel, final int iMaxColor, final float value, final int alphaMask) {
        int minColorIdx = (int) Math.floor(value);

        final float ratio = value - minColorIdx;

        if (minColorIdx < 0) {
            minColorIdx = 0;
        } else if (minColorIdx > iMaxColor) {
            minColorIdx = iMaxColor;
        }

        int maxColorIdx = minColorIdx + 1;

        if (maxColorIdx > iMaxColor) {
            maxColorIdx = iMaxColor;
        }

        final int minColor = colorModel.getRGB(minColorIdx);
        final int maxColor = colorModel.getRGB(maxColorIdx);

        final int ra = minColor >> 16 & 0xff;
        final int ga = minColor >> 8 & 0xff;
        final int ba = minColor & 0xff;

        final int rb = maxColor >> 16 & 0xff;
        final int gb = maxColor >> 8 & 0xff;
        final int bb = maxColor & 0xff;

        // linear interpolation for color :
        final int r = Math.round(ra + (rb - ra) * ratio);
        final int g = Math.round(ga + (gb - ga) * ratio);
        final int b = Math.round(ba + (bb - ba) * ratio);

        return alphaMask | (r << 16) | (g << 8) | b;
    }

    /**
     * Scale the given value using linear or logarithmic scale
     * 
     * @param doLog10 true to use logarithmic scale
     * @param scaledMin minimum data value or log10(min) 
     * @param scalingFactor data to color linear scaling factor
     * @param value value to convert
     * @return scaled value
     */
    public static float getScaledValue(final boolean doLog10, final float scaledMin, final float scalingFactor, final float value) {
        if (doLog10) {
            if (value <= 0f) {
                // lowest color
                return 0f;
            }
            return ((float) Math.log10(value) - scaledMin) * scalingFactor;
        }

        return (value - scaledMin) * scalingFactor;
    }

    /**
     * Compute image Task that process one image slice in parallel with other tasks working on the same image:
     * Convert the given 1D or 2D data array to RGB color using the given scaling factor
     */
    private static class ComputeImagePart implements Runnable {

        /* input */
        /** data array (1D) */
        private final float[] _array1D;
        /** data array (2D) [rows][cols] */
        private final float[][] _array2D;
        /** image width */
        private final int _width;
        /** image height */
        private final int _height;
        /** lower data value */
        private final float _scaledMin;
        /** indexed color model */
        private final IndexColorModel _colorModel;
        /** color scaling method */
        private final ColorScale _colorScale;
        /** data to color linear scaling factor */
        private final float _scalingFactor;
        /* output */
        /** image raster dataBuffer */
        private final DataBuffer _dataBuffer;
        /* job boundaries */
        /** job index */
        private final int _jobIndex;
        /** total number of concurrent jobs */
        private final int _jobCount;

        /**
         * Create the task
         *
         * @param array data array (1D)
         * @param scaledMin lower data value
         * @param colorModel indexed color model
         * @param scalingFactor data to color linear scaling factor
         * @param colorScale color scaling method
         * @param dataBuffer image raster dataBuffer
         * @param jobIndex job index used to process data interlaced
         * @param jobCount total number of concurrent jobs
         */
        ComputeImagePart(final float[] array, final float scaledMin,
                         final IndexColorModel colorModel, final float scalingFactor, final ColorScale colorScale,
                         final DataBuffer dataBuffer,
                         final int jobIndex, final int jobCount) {

            this._array1D = array;
            this._array2D = null;
            this._width = array.length;
            this._height = 0;
            this._scaledMin = scaledMin;
            this._colorModel = colorModel;
            this._colorScale = colorScale;
            this._scalingFactor = scalingFactor;
            this._dataBuffer = dataBuffer;
            this._jobIndex = jobIndex;
            this._jobCount = jobCount;
        }

        /**
         * Create the task
         *
         * @param array data array (2D)
         * @param width image width
         * @param height image height
         * @param scaledMin lower data value
         * @param colorModel indexed color model
         * @param scalingFactor data to color linear scaling factor
         * @param colorScale color scaling method
         * @param dataBuffer image raster dataBuffer
         * @param jobIndex job index used to process data interlaced
         * @param jobCount total number of concurrent jobs
         */
        ComputeImagePart(final float[][] array, final int width, final int height, final float scaledMin,
                         final IndexColorModel colorModel, final float scalingFactor, final ColorScale colorScale,
                         final DataBuffer dataBuffer,
                         final int jobIndex, final int jobCount) {

            this._array1D = null;
            this._array2D = array;
            this._width = width;
            this._height = height;
            this._scaledMin = scaledMin;
            this._colorModel = colorModel;
            this._colorScale = colorScale;
            this._scalingFactor = scalingFactor;
            this._dataBuffer = dataBuffer;
            this._jobIndex = jobIndex;
            this._jobCount = jobCount;
        }

        /**
         * Execute the task i.e. performs the computations
         */
        @Override
        public void run() {
            if (logger.isDebugEnabled()) {
                logger.debug("ComputeImagePart: start [{}]", _jobIndex);
            }
            // Copy members to local variables:
            /* input */
            final int width = _width;
            final int height = _height;
            final float[] array1D = _array1D;
            final float[][] array2D = _array2D;
            final float scaledMin = _scaledMin;
            final IndexColorModel colorModel = _colorModel;
            final float scalingFactor = _scalingFactor;
            final boolean doLog10 = (_colorScale == ColorScale.LOGARITHMIC);
            /* output */
            final DataBuffer dataBuffer = _dataBuffer;
            /* job boundaries */
            final int jobIndex = _jobIndex;
            final int jobCount = _jobCount;

            // Prepare other variables:
            final int iMaxColor = colorModel.getMapSize() - 1;

            /** Get the current thread to check if the computation is interrupted */
            final Thread currentThread = Thread.currentThread();

            if (USE_RGB_INTERPOLATION) {

                // initialize raster pixels
                if (array1D != null) {
                    for (int i = jobIndex; i < width; i += jobCount) {

                        dataBuffer.setElem(i, getRGB(colorModel, iMaxColor,
                                getScaledValue(doLog10, scaledMin, scalingFactor, array1D[i]), ALPHA_MASK));

                        // fast interrupt:
                        if (currentThread.isInterrupted()) {
                            logger.debug("ComputeImagePart: cancelled (vis)");
                            return;
                        }
                    } // pixel by pixel
                } else if (array2D != null) {
                    float[] row;
                    for (int i, offset, j = jobIndex, lastRow = height - 1; j < height; j += jobCount) {
                        // inverse vertical axis (0 at bottom, height at top):
                        offset = width * (lastRow - j);
                        row = array2D[j];

                        for (i = 0; i < width; i++) {

                            dataBuffer.setElem(offset + i, getRGB(colorModel, iMaxColor,
                                    getScaledValue(doLog10, scaledMin, scalingFactor, row[i]), ALPHA_MASK));
                        }

                        // fast interrupt:
                        if (currentThread.isInterrupted()) {
                            logger.debug("ComputeImagePart: cancelled (vis)");
                            return;
                        }
                    } // line by line
                }

            } else {

                // initialize raster pixels
                if (array1D != null) {
                    for (int i = jobIndex; i < width; i += jobCount) {

                        dataBuffer.setElem(i, getColor(iMaxColor,
                                getScaledValue(doLog10, scaledMin, scalingFactor, array1D[i])));

                        // fast interrupt:
                        if (currentThread.isInterrupted()) {
                            logger.debug("ComputeImagePart: cancelled (vis)");
                            return;
                        }
                    } // pixel by pixel
                } else if (array2D != null) {
                    float[] row;
                    for (int i, offset, j = jobIndex, lastRow = height - 1; j < height; j += jobCount) {
                        // inverse vertical axis (0 at bottom, height at top):
                        offset = width * (lastRow - j);
                        row = array2D[j];

                        for (i = 0; i < width; i++) {

                            dataBuffer.setElem(offset + i, getColor(iMaxColor,
                                    getScaledValue(doLog10, scaledMin, scalingFactor, row[i])));
                        }

                        // fast interrupt:
                        if (currentThread.isInterrupted()) {
                            logger.debug("ComputeImagePart: cancelled (vis)");
                            return;
                        }
                    } // line by line
                }
            }

            // Compute done.
            if (logger.isDebugEnabled()) {
                logger.debug("ComputeImagePart: end   [{}]", _jobIndex);
            }
        }
    }
}
