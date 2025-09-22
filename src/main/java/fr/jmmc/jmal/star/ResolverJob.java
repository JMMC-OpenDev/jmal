/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.star;

import fr.jmmc.jmal.star.StarResolver.ServiceType;
import static fr.jmmc.jmal.star.StarResolver.USE_CACHE_DEV;
import fr.jmmc.jmcs.data.preference.SessionSettingsPreferences;
import fr.jmmc.jmcs.network.http.Http;
import fr.jmmc.jmcs.util.CollectionUtils;
import fr.jmmc.jmcs.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Star resolver service
 * @author bourgesl
 */
public abstract class ResolverJob implements Callable<Object> {

    /** Logger */
    protected static final Logger _logger = LoggerFactory.getLogger(ResolverJob.class.getName());
    /** Simbad value for the read timeout in milliseconds (60 seconds) */
    public static final int HTTP_SOCKET_READ_TIMEOUT_SMALL = 60 * 1000;
    /** Simbad value for the read timeout in milliseconds (300 seconds) */
    public static final int HTTP_SOCKET_READ_TIMEOUT_LARGE = 5 * HTTP_SOCKET_READ_TIMEOUT_SMALL;
    /** threshold to consider query is large (100 ids) */
    public static final int HTTP_THREHOLD_LARGE = 100;

    /** empty String array */
    public final static String[] EMPTY_STRING = new String[0];

    /* members */
    /** optional flags associated with the query */
    protected final Set<String> _flags;
    /** list of queried identifiers */
    protected final List<String> _names;

    /** callback listener with progress */
    protected final StarResolverProgressListener _progressListener;
    /** callback listener with results */
    protected final StarResolverListener<Object> _listener;
    /** running thread name (only defined during the background execution; null otherwise) */
    protected String threadName = null;

    /**
     * @param flags optional flags associated with the query
     * @param names list of queried identifiers
     * @param progressListener callback listener with progress
     * @param listener callback listener with results
     */
    ResolverJob(final Set<String> flags, final List<String> names,
                final StarResolverProgressListener progressListener,
                final StarResolverListener<Object> listener) {
        _flags = flags;
        _names = names;
        _progressListener = progressListener;
        _listener = listener;
    }

    /**
     * Cancel any http request in progress (close socket)
     * Called by another thread
     */
    final void cancel() {
        _logger.debug("ResolverJob.cancel");
        if (this.threadName != null) {
            Http.abort(this.threadName);
        }
    }

    public abstract String getResolverName();

    public abstract Object getResolverResult();

    protected abstract String buildQuery();

    protected abstract HttpMethodBase buildHttpMethod(final String serviceURL, final String query);

    /**
     * Parse Resolver response
     * @param response http content as string
     * @return true if successfull (no error); false otherwise
     * @throws IllegalStateException if parsing error
     */
    protected abstract boolean parseResponse(final String response) throws IllegalStateException;

    /**
     * @return true if any error occured
     */
    public abstract boolean isErrorStatus();

    @Override
    public final Object call() {
        _logger.debug("ResolverJob.run");

        if (_progressListener != null) {
            _progressListener.handleProgressMessage("querying " + getResolverName() + " for star(s) "
                    + _names + " ... (please wait, this may take a while)");
        }
        // define the thread name:
        this.threadName = Thread.currentThread().getName();

        String response = null;
        Object result;
        try {
            response = queryResolver();

            if (Thread.currentThread().isInterrupted()) {
                handleError(StarResolverStatus.ERROR_IO, getResolverName() + " star resolution cancelled.");
            } else {
                parseResponse(response);

                // If everything went fine, set status to OK
                if (!isErrorStatus()) {
                    if (_progressListener != null) {
                        _progressListener.handleProgressMessage(getResolverName() + " star resolution done.");
                    }
                }
            }

        } catch (IOException ioe) {
            handleError(StarResolverStatus.ERROR_IO, ioe.getMessage());
        } catch (IllegalStateException ise) {
            _logger.info("Parsing error on the {} response:\n{}", getResolverName(), response);
            handleError(StarResolverStatus.ERROR_PARSING, ise.getMessage());
        } finally {
            result = getResolverResult();
            // anyway: process result
            if (_listener != null) {
                _listener.handleResult(result);
            }
        }
        return result;
    }

    protected void handleError(final StarResolverStatus status, final String errorMessage) {
        if (_progressListener != null) {
            _progressListener.handleProgressMessage(errorMessage);
        }
    }

    /**
     * Handle the given progress message (using server mirror, error, done) ...
     * @param message progress message
     */
    protected final void handleProgressMessage(final String message) {

    }

    /**
     * Query Star resolver
     * @return http query content
     * @throws IllegalStateException if the star name is empty
     * @throws IOException if no resolver mirror is responding
     */
    private final String queryResolver() throws IllegalArgumentException, IOException {
        _logger.trace("ResolverJob.queryResolver");

        final List<String> ids = _names;

        // Should never receive an empty scence object name
        if (CollectionUtils.isEmpty(ids)) {
            throw new IllegalStateException("Could not resolve empty star name.");
        }

        // Reset result before proceeding
        String response = "";

        // In development: load cached query results:
        final File cachedFile;
        if (USE_CACHE_DEV) {
            cachedFile = generateCacheFile(ids);

            if (cachedFile.exists() && cachedFile.length() != 0L) {
                try {
                    response = FileUtils.readFile(cachedFile);

                    // update last modified (consistent cache):
                    cachedFile.setLastModified(System.currentTimeMillis());

                    _logger.info("using cached result: " + cachedFile.getAbsolutePath());
                    return response;
                } catch (IOException ioe) {
                    _logger.info("unable to read cached result: " + cachedFile.getAbsolutePath(), ioe);
                }
            }
        } else {
            cachedFile = null;
        }

        // Build query:
        final String query = buildQuery();
        _logger.trace("query:\n{}", query);

        // Get the shared HTTP client to send queries to Simbad (multithread support)
        final HttpClient client = Http.getHttpClient();

        /** Get the current thread to check if the query is cancelled */
        final Thread currentThread = Thread.currentThread();

        // Use prefered Simbad mirror
        String serviceMirror = StarResolver.getResolverServiceMirror();
        final ServiceType serviceType = StarResolver.getResolverServiceType(serviceMirror);
        String serviceURL;

        final Set<String> failedUrl = new HashSet<String>(4);
        List<String> ioMessages = null;

        // Retry other mirrors if needed
        while ((serviceMirror != null) && (!currentThread.isInterrupted())) {
            // Try to get star data from the default or first resolver service:
            serviceURL = StarResolver.getResolverServiceUrl();

            _logger.debug("Querying service: {}", serviceURL);
            HttpMethodBase method = null;
            try {
                final long start = System.nanoTime();

                // create the HTTP Post method:
                method = buildHttpMethod(serviceURL, query);

                // customize timeouts:
                final int timeout = (ids.size() >= HTTP_THREHOLD_LARGE) ? HTTP_SOCKET_READ_TIMEOUT_LARGE : HTTP_SOCKET_READ_TIMEOUT_SMALL;
                _logger.debug("Timeout: {}", timeout);

                method.getParams().setSoTimeout(timeout);

                // execute query:
                response = Http.execute(client, method);

                _logger.info("ResolverJob.queryResolver: duration = {} ms.", 1e-6d * (System.nanoTime() - start));

                // exit from loop:
                return response;
            } catch (IOException ioe) {
                final String eMsg = getExceptionMessage(ioe);
                _logger.info("Resolver service connection failed: {}", eMsg);
                if (ioMessages == null) {
                    ioMessages = new ArrayList<String>(5);
                }
                ioMessages.add("[" + serviceMirror + "] " + eMsg);
                failedUrl.add(serviceURL);
                // get another mirror:
                serviceMirror = StarResolver.getNextResolverServiceMirror(failedUrl, serviceType);
                if (serviceMirror != null) {
                    _logger.info("Trying another resolver mirror [{}]", serviceMirror);
                    if (_progressListener != null) {
                        _progressListener.handleProgressMessage("Resolver service failed: trying another mirror [" + serviceMirror + "] ...");
                    }
                } else {
                    // no more mirror to use (probably bad network settings):
                    ioMessages.add("\nPlease check your network connection !");

                    // reset buffer:
                    final StringBuilder sb = new StringBuilder(256);
                    sb.append("Resolver service connection failed:");
                    for (String msg : ioMessages) {
                        sb.append('\n').append(msg);
                    }

                    throw new IOException(sb.toString());
                }
            } finally {
                if ((method != null) && method.isAborted()) {
                    currentThread.interrupt();
                }

                // In development: save cached query results:
                if (USE_CACHE_DEV && response.length() != 0) {
                    try {
                        FileUtils.writeFile(cachedFile, response);

                        _logger.info("saving cached result: {}", cachedFile.getAbsolutePath());
                    } catch (IOException ioe) {
                        _logger.info("unable to write cached result: " + cachedFile.getAbsolutePath(), ioe);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Format specific exception messages
     * @param e exception
     * @return user message for the given exception
     */
    protected String getExceptionMessage(final Exception e) {
        if (e instanceof UnknownHostException) {
            return "Unknown host [" + e.getMessage() + "]";
        }
        return (e != null) ? e.getMessage() : "";
    }

    private static File generateCacheFile(final List<String> nameList) {
        final String parentPath = SessionSettingsPreferences.getApplicationFileStorage();
        // assert that parent directory exist
        new File(parentPath).mkdirs();

        final int nIds = nameList.size();
        final String[] names = nameList.toArray(new String[nIds]);

        // Copy and sort ids to have an hashcode independent from ordering:
        Arrays.sort(names);

        final StringBuilder sb = new StringBuilder(2048);

        // loop on identifiers to build cached key: '<ID>,'
        for (String id : names) {
            sb.append(id).append(','); // Add each object name we are looking for
        }
        final int hash_ids = sb.toString().hashCode();

        // Form file name:
        sb.setLength(0);
        sb.append("StarsResolver_").append(nIds).append('_');
        sb.append(names[0]).append('-').append(names[nIds - 1]).append('_');
        sb.append(hash_ids).append(".dat");

        final String fileName = sb.toString();

        return new File(parentPath, fileName);
    }

}
