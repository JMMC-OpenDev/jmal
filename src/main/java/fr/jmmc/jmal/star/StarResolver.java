/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.star;

import ch.qos.logback.classic.Level;
import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.logging.LoggingService;
import fr.jmmc.jmcs.util.CollectionUtils;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Store informations relative to a star.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES.
 */
public final class StarResolver {

    /** Use -DSimbad.resolver.cache=true (dev) to use local query/response caching */
    public static final boolean USE_CACHE_DEV = Boolean.getBoolean("Simbad.resolver.cache");

    /** semicolon separator (multiple identifier separator) */
    public static final String SEPARATOR_SEMI_COLON = ";";
    /** special flag to skip fixing Star name */
    public static final String FLAG_SKIP_FIX_NAME = "skipFixName";

    /** Logger - register on the current class to collect local logs */
    private static final Logger _logger = LoggerFactory.getLogger(StarResolver.class.getName());
    /** The collection of resolver mirrors */
    private static final Map<String, String> _resolverServiceMirrors;
    /** The collection of resolver types */
    private static final Map<String, ServiceType> _resolverServiceTypes;
    /** The collection of active resolver mirrors at runtime */
    private static final Set<String> _resolverServiceMirrorSet;
    /** SIMBAD selected mirror (selected using setResolverServiceMirror()) */
    private static String _resolverServiceMirror = null;
    /** RegExp expression to match underscore character */
    private final static Pattern PATTERN_UNDERSCORE = Pattern.compile("_");
    /** RegExp expression to match white spaces arround semicolon separator */
    private final static Pattern PATTERN_WHITE_SPACE_ARROUND_SEMI_COLON = Pattern.compile("\\s*;\\s*");
    /** Simbad base URL (main CDS host) */
    public static final String SIMBAD_MAIN_URL = "http://simbad.u-strasbg.fr/simbad/";
    /** GetStar URL (query by identifier) */
    public static final String GETSTAR_QUERY_ID = "http://sclws.jmmc.fr/sclwsGetStarProxy.php?star=";

    public enum ServiceType {
        SIMBAD,
        GETSTAR
    };

    public final static String SERVICE_GET_STAR_PUBLIC = "JMMC GetStar, FR";
    public final static String SERVICE_GET_STAR_BETA = "JMMC GetStar (beta), FR";

    public final static String SERVICE_SIMBAD_PUBLIC = "SIMBAD Strasbourg, FR";
    public final static String SERVICE_SIMBAD_IP = "SIMBAD Strasbourg, FR [IP]";
    public final static String SERVICE_SIMBAD_HARVARD = "SIMBAD Harvard, US";

    static {
        _resolverServiceMirrors = new HashMap<>(8);
        _resolverServiceMirrors.put(SERVICE_GET_STAR_PUBLIC, "https://www.jmmc.fr/~sclws/getstar/sclwsGetStarProxy.php?star=");
        _resolverServiceMirrors.put(SERVICE_GET_STAR_BETA, GETSTAR_QUERY_ID);
        _resolverServiceMirrors.put(SERVICE_SIMBAD_PUBLIC, SIMBAD_MAIN_URL + "sim-script");
        _resolverServiceMirrors.put(SERVICE_SIMBAD_IP, "http://130.79.128.4/simbad/sim-script");
        _resolverServiceMirrors.put(SERVICE_SIMBAD_HARVARD, "http://simbad.harvard.edu/simbad/sim-script");

        _resolverServiceTypes = new HashMap<>(8);
        _resolverServiceTypes.put(SERVICE_GET_STAR_PUBLIC, ServiceType.GETSTAR);
        _resolverServiceTypes.put(SERVICE_GET_STAR_BETA, ServiceType.GETSTAR);
        _resolverServiceTypes.put(SERVICE_SIMBAD_PUBLIC, ServiceType.SIMBAD);
        _resolverServiceTypes.put(SERVICE_SIMBAD_IP, ServiceType.SIMBAD);
        _resolverServiceTypes.put(SERVICE_SIMBAD_HARVARD, ServiceType.SIMBAD);

        _resolverServiceMirrorSet = new LinkedHashSet<>(8);
        enableGetStar(false);
    }

    public static void enableGetStar(final boolean enabled) {
        _resolverServiceMirrorSet.clear();
        if (enabled) {
            _resolverServiceMirrorSet.add(SERVICE_GET_STAR_BETA);
            _resolverServiceMirrorSet.add(SERVICE_GET_STAR_PUBLIC);
        }
        _resolverServiceMirrorSet.add(SERVICE_SIMBAD_PUBLIC);
        _resolverServiceMirrorSet.add(SERVICE_SIMBAD_IP);
        _resolverServiceMirrorSet.add(SERVICE_SIMBAD_HARVARD);
    }

    /**
     * Get the list of available resolver service mirrors.
     * @return one set of available resolver service mirror names.
     */
    public static Set<String> getResolverServiceMirrors() {
        return _resolverServiceMirrorSet;
    }

    /**
     * Return the current resolver service mirror
     * @return resolver service mirror name
     */
    public static String getResolverServiceMirror() {
        if (_resolverServiceMirror == null) {
            setResolverServiceMirror(getResolverServiceMirrors().iterator().next());
        }
        return _resolverServiceMirror;
    }

    /**
     * Return the resolver service URL from the current mirror or the first one
     * @return resolver service URL
     */
    public static String getResolverServiceUrl() {
        return _resolverServiceMirrors.get(getResolverServiceMirror());
    }

    public static ServiceType getResolverServiceType(final String mirrorName) {
        return _resolverServiceTypes.get(mirrorName);
    }

    /**
     * Choose one mirror giving its name chosen from available ones.
     * @param mirrorName value chosen from getResolverServiceMirrors().
     */
    public static void setResolverServiceMirror(final String mirrorName) {
        // prevent bad cases for bad mirror names
        if (_resolverServiceMirrors.get(mirrorName) == null) {
            _resolverServiceMirror = getResolverServiceMirrors().iterator().next();
        } else {
            _resolverServiceMirror = mirrorName;
        }
    }

    /**
     * Return the next mirror which URL is not in the failed URL Set
     * @param failedUrl failed URL(s)
     * @param type
     * @return next SIMBAD Mirror or null if none is still available
     */
    static String getNextResolverServiceMirror(final Set<String> failedUrl, final ServiceType type) {

        // TODO: fix
        for (Map.Entry<String, String> e : _resolverServiceMirrors.entrySet()) {
            final String mirrorName = e.getKey();
            if ((getResolverServiceType(mirrorName) == type) && !failedUrl.contains(e.getValue())) {
                // change mirror:
                setResolverServiceMirror(e.getKey());
                return _resolverServiceMirror;
            }
        }
        return null;
    }

    /**
     * Wait for the given future to be ready (
     * @param future Future instance to use for synchronous mode (wait for)
     * @return StarResolverResult; null if the future was cancelled or not executed
     */
    public static <K> K waitFor(final Future<K> future) {
        try {
            // Wait for StarResolver task to be done (and listener called) :
            return future.get();
        } catch (InterruptedException ie) {
            _logger.debug("waitFor: interrupted", ie);
        } catch (ExecutionException ee) {
            _logger.info("waitFor: execution error", ee);
        }
        return null;
    }

    /* members */
    /** callback listener with progress */
    private final StarResolverProgressListener _progressListener;
    /** callback listener with results */
    private final StarResolverListener<Object> _listener;
    /** Dedicated thread executor (single thread) */
    private final ThreadExecutors _executor = ThreadExecutors.getSingleExecutor("StarResolverThreadPool");

    /**
     * Constructor without listener (synchronous mode)
     */
    public StarResolver() {
        this(null, null);
    }

    /**
     * Constructor with listener (asynchronous mode)
     *
     * @param progressListener callback listener with progress
     * @param listener callback listener with results
     */
    public StarResolver(final StarResolverProgressListener progressListener,
                        final StarResolverListener<Object> listener) {
        _progressListener = progressListener;
        _listener = listener;
    }

    /**
     * Asynchronously query CDS SIMBAD to retrieve a given star information according to its name.
     * @param flags optional flags associated with the query
     * @param name the name of the star to resolve.
     * @return Future instance to use for synchronous mode (wait for)
     * @throws IllegalArgumentException if the given name is empty
     */
    public Future<Object> resolve(final Set<String> flags, final String name) throws IllegalArgumentException {
        _logger.debug("Searching data for star '{}'.", name);

        if (isMultiple(name)) {
            throw new IllegalArgumentException("Multiple names: use multipleResolve() directly.");
        }

        final String cleanedName = cleanNames(name);

        if (StringUtils.isEmpty(cleanedName)) {
            throw new IllegalArgumentException("Empty star name !");
        }

        return multipleResolve(flags, Arrays.asList(cleanedName));
    }

    /**
     * Asynchronously query CDS SIMBAD to retrieve multiple stars information according to their names.
     * @param flags optional flags associated with the query
     * @param names the names of the star to resolve, separated by semi-colons.
     * @return Future instance to use for synchronous mode (wait for)
     * @throws IllegalArgumentException if the given names are empty
     */
    public Future<Object> multipleResolve(final Set<String> flags, final String names) throws IllegalArgumentException {
        return multipleResolve(flags, prepareNames(names));
    }

    /**
     * Asynchronously query CDS SIMBAD to retrieve multiple stars information according to their names.
     * @param nameList the names of the star to resolve (clean ie no semicolon separator) nor empty strings
     * @return Future instance to use for synchronous mode (wait for)
     * @throws IllegalArgumentException if the given names are empty
     */
    public Future<Object> multipleResolve(final List<String> nameList) throws IllegalArgumentException {
        return multipleResolve(null, nameList);
    }

    /**
     * Asynchronously query CDS SIMBAD to retrieve multiple stars information according to their names.
     * @param flags optional flags associated with the query
     * @param nameList the names of the star to resolve (clean ie no semicolon separator) nor empty strings
     * @return Future instance to use for synchronous mode (wait for)
     * @throws IllegalArgumentException if the given names are empty
     */
    @SuppressWarnings("unchecked")
    public Future<Object> multipleResolve(final Set<String> flags, final List<String> nameList) throws IllegalArgumentException {
        _logger.debug("Searching data for stars '{}'.", nameList);

        if (CollectionUtils.isEmpty(nameList)) {
            throw new IllegalArgumentException("Empty star names !");
        }

        final ServiceType type = getResolverServiceType(getResolverServiceMirror());

        // Launch the query in the background in order to keep GUI updated
        switch (type) {
            default:
            case SIMBAD:
                return submitJob(new SimbadResolveJob(flags, nameList, _progressListener, _listener));
            case GETSTAR:
                return submitJob(new GetStarResolveJob(flags, nameList, _progressListener, _listener));
        }
    }

    private Future<Object> submitJob(final ResolverJob resolveStarJob) {
        // Intercept cancel calls to first abort HTTP method:
        @SuppressWarnings("unchecked")
        final FutureTask<Object> task = new FutureTask<Object>(resolveStarJob) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (resolveStarJob instanceof SimbadResolveJob) {
                    ((SimbadResolveJob) resolveStarJob).cancel();
                }
                return super.cancel(mayInterruptIfRunning);
            }
        };

        // Launch query and return its Future wrapper
        _executor.execute(task);
        return task;
    }

    /**
     * 
     * @param names
     * @return 
     * @throws IllegalArgumentException if the given names are empty
     */
    public static List<String> prepareNames(final String names) {
        _logger.debug("prepareNames '{}'.", names);

        final String cleanedNames = cleanNames(names);

        if (StringUtils.isEmpty(cleanedNames)) {
            throw new IllegalArgumentException("Empty star names !");
        }

        // Split names properly:
        final String[] cleanedNameArray = cleanedNames.split(SEPARATOR_SEMI_COLON);
        final List<String> nameList = new ArrayList<String>(cleanedNameArray.length);

        for (String cleanedName : cleanedNameArray) {
            // Skip empty names
            if (!cleanedName.isEmpty()) {
                nameList.add(cleanedName);
            }
        }
        return nameList;
    }

    /**
     * Return true if the given value contains the semicolon separator (multiple identifier separator)
     @param value
     @return 
     */
    public static boolean isMultiple(final String value) {
        return (value != null) && value.contains(SEPARATOR_SEMI_COLON);
    }

    /**
     * Trim and remove redundant white space characters arround the semicolon separator
     * @param value input value
     * @return string value
     */
    public static String cleanNames(final String value) {
        if (StringUtils.isEmpty(value)) {
            return StringUtils.STRING_EMPTY;
        }
        // replace underscore character by space character:
        final String cleanedUnderscore = PATTERN_UNDERSCORE.matcher(value).replaceAll(StringUtils.STRING_SPACE);
        // replace useless white spaces arround the semicolon separator:
        final String cleanedSemiColon = PATTERN_WHITE_SPACE_ARROUND_SEMI_COLON.matcher(cleanedUnderscore).replaceAll(SEPARATOR_SEMI_COLON);
        return StringUtils.cleanWhiteSpaces(cleanedSemiColon);
    }

    /**
     * Command-line tool that tries to resolve the star name given as first parameter.
     * @param args first argument is the star name
     */
    public static void main(String[] args) {

        // invoke Bootstrapper method to initialize logback now:
        Bootstrapper.getState();

        if (false) {
            LoggingService.setLoggerLevel("fr.jmmc.jmal.star", Level.ALL);
        }

        if (true) {
            StarResolver.setResolverServiceMirror(SERVICE_GET_STAR_BETA);
        } else {
            StarResolver.setResolverServiceMirror(SERVICE_SIMBAD_PUBLIC);
        }

        final String names;

        if (args != null && args.length != 0) {
            names = args[0];
        } else {
// single:            
//            names = "  eps aur";
//            names = "car";
//            names = "aasioi";
// multiple:            
//            names = "    l car  ; L car  ;";
//            names = "aasioi;vega;bad; AK_SCO  ";
            names = "GJ876";
        }

        final StarResolverProgressListener progressListener = new StarResolverProgressListener() {
            @Override
            public void handleProgressMessage(final String message) {
                _logger.info(message);
            }
        };
        final StarResolverListener<Object> listener = new StarResolverListener<Object>() {
            /**
             * Handle the star resolver result as String (raw http response) or StarResolverResult instance (status, error messages, stars) ...
             * @param result star resolver result
             */
            @Override
            public void handleResult(final Object result) {
                _logger.info("ASYNC Star resolver result:\n{}", result);
            }
        };

        // Seek data about the given star name (first arg on command line)
        // Wait for StarResolver task done (and listener calls) :
        final Object result = waitFor(new StarResolver(progressListener, listener).multipleResolve(null, names));

        _logger.info("SYNC star resolver result:\n{}", result);

        _logger.info("Exit.");
        System.exit(0);
    }

}
/*___oOo___*/
