/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.image.job;

import fr.jmmc.jmcs.util.concurrent.InterruptedJobException;
import fr.jmmc.jmcs.util.concurrent.ParallelJobExecutor;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Job dedicated to image i.e. float[][] processing
 * 
 * @param <V> the result type of method <tt>call</tt>
 *
 * @author bourgesl
 */
public abstract class AbstractImageJob<V> implements Callable<V> {

    /** Class logger */
    protected static final Logger logger = LoggerFactory.getLogger(AbstractImageJob.class.getName());
    /** default threshold = 65536 */
    public final static int DEFAULT_THRESHOLD = 256 * 256;
    /** Jmcs Parallel Job executor */
    private static final ParallelJobExecutor jobExecutor = ParallelJobExecutor.getInstance();

    /* members */
    /** job name */
    protected final String _jobName;
    /* input */
    /** data array (2D) [rows][cols] */
    protected final float[][] _array2D;
    /** image width */
    protected final int _width;
    /** image height */
    protected final int _height;
    /* job boundaries */
    /** job index */
    private final int _jobIndex;
    /** total number of concurrent jobs */
    private final int _jobCount;
    /* output */
    /** result object */
    protected final V _result;

    /**
     * Create the image Job
     *
     * @param jobName job name used when throwing an exception
     * @param array data array (2D)
     * @param width image width
     * @param height image height
     */
    public AbstractImageJob(final String jobName, final float[][] array, final int width, final int height) {
        this._jobName = jobName;
        this._array2D = array;
        this._width = width;
        this._height = height;
        // job boundaries for single thread:
        this._jobIndex = 0;
        this._jobCount = 1;
        // define result object:
        this._result = initializeResult();
    }

    /**
     * Create the image Job given a parent job
     *
     * @param parentJob parent Job producing same result
     * @param jobIndex job index used to process data interlaced
     * @param jobCount total number of concurrent jobs
     */
    protected AbstractImageJob(final AbstractImageJob<V> parentJob, final int jobIndex, final int jobCount) {
        this._jobName = parentJob._jobName;
        this._array2D = parentJob._array2D;
        this._width = parentJob._width;
        this._height = parentJob._height;
        this._jobIndex = jobIndex;
        this._jobCount = jobCount;
        // define result object:
        this._result = initializeResult();
    }

    /**
     * 
     * @return result object
     * @throws InterruptedJobException if the current thread is interrupted (cancelled)
     */
    @SuppressWarnings("unchecked")
    public final V forkAndJoin() throws InterruptedJobException {

        V result;

        // Start the computations :
        final long start = System.nanoTime();

        // Should split the computation in parts ?
        // i.e. enough big compute task ?

        if (jobExecutor.isEnabled() && shouldForkJobs()) {
            // process image using parallel threads working interlaced :
            final int nJobs = jobExecutor.getMaxParallelJob();

            final AbstractImageJob<V>[] jobs = new AbstractImageJob[nJobs];

            for (int i = 0; i < nJobs; i++) {
                jobs[i] = initializeChildJob(i, nJobs);
            }

            // execute jobs in parallel:
            final List<V> partialResults = (List<V>) jobExecutor.forkAndJoin(_jobName, jobs);

            merge(partialResults);

            result = _result;

        } else {
            // single processor: use this thread to compute the complete model image:
            result = call();
        }

        // fast interrupt :
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedJobException(_jobName + ": interrupted");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("compute : duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }

        return result;
    }

    /**
     * Execute the task i.e. performs computations
     * 
     * @return result object or null if interrupted
     */
    @Override
    public final V call() {
        if (logger.isDebugEnabled()) {
            logger.debug("AbstractImageJob: start [{}]", _jobIndex);
        }
        // Copy members to local variables:
        /* input */
        final float[][] array2D = _array2D;
        final int width = _width;
        final int height = _height;
        /* job boundaries */
        final int jobIndex = _jobIndex;
        final int jobCount = _jobCount;

        /** Get the current thread to check if the computation is interrupted */
        final Thread currentThread = Thread.currentThread();

        float[] row;

        // iterate on rows starting at jobIndex and skip jobCount rows at each iteration:
        for (int i, j = jobIndex; j < height; j += jobCount) {
            row = array2D[j];

            // iterate on cols:
            for (i = 0; i < width; i++) {
                processValue(i, j, row[i]);
            } // column

            // fast interrupt:
            if (currentThread.isInterrupted()) {
                logger.debug("AbstractImageJob: cancelled (vis)");
                return null;
            }
        } // row

        // Compute done.
        if (logger.isDebugEnabled()) {
            logger.debug("AbstractImageJob: end   [{}]", _jobIndex);
        }
        return _result;
    }

    /**
     * Initialize a new child job for the given job index
     * @param jobIndex job index used to process data interlaced
     * @param jobCount total number of concurrent jobs
     * @return child job
     */
    protected abstract AbstractImageJob<V> initializeChildJob(final int jobIndex, final int jobCount);

    /**
     * Initialize the result object (one per job)
     * @return result Object
     */
    protected abstract V initializeResult();

    /**
     * Merge partial result objects to produce the final result object
     * @param partialResults partial result objects
     */
    protected abstract void merge(final List<V> partialResults);

    /**
     * Process the given value at the given row and column index
     * 
     * @param col row index
     * @param row column index
     * @param value value at the given row and column
     */
    protected abstract void processValue(final int col, final int row, final float value);

    /**
     * Return true if the job should be forked in smaller jobs
     * @return true if the job should be forked in smaller jobs 
     */
    public boolean shouldForkJobs() {
        return _width * _height > DEFAULT_THRESHOLD;
    }
}
