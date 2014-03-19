/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.star;

import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.MCSExceptionHandler;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.jmcs.util.UrlUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Store informations relative to a star.
 * 
 * TODO LIST: 
 * - use HTTP CLIENT to reuse HTTP connections (multiple queries) and properly handle timeouts
 * - remove any Swing dependency (StatusBar & SwingUtils) to be used by any web application (oidb)
 * - provide a synchronous mode to be used by any web application (oidb)
 * - use jmcs ThreadExecutors.getSingleExecutors("simbad") to use a single thread pool 
 * ie reuse thread and avoid concurrent calls to simbad
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES.
 */
public final class StarResolver {

    /** Logger - register on the current class to collect local logs */
    private static final Logger _logger = LoggerFactory.getLogger(StarResolver.class.getName());
    /** The collection of CDS mirrors (initialized into getMirrors()) */
    private static final Map<String, String> _simbadMirrors;
    /** SIMBAD selected mirror (selected using setSimbadMirror()) */
    private static String _simbadMirror = null;
    /** comma separator */
    public static final String SEPARATOR_COMMA = ",";
    /** semicolon separator */
    public static final String SEPARATOR_SEMI_COLON = ";";
    /** HTTP response encoding use UTF-8 */
    private static final String HTTP_ENCODING = "UTF-8";

    static {
        _simbadMirrors = new LinkedHashMap<String, String>(4);
        _simbadMirrors.put("SIMBAD Strasbourg, FR", "http://simbad.u-strasbg.fr/simbad/sim-script?script=");
        _simbadMirrors.put("SIMBAD Harvard, US", "http://simbad.harvard.edu/simbad/sim-script?script=");
        _simbadMirrors.put("SIMBAD Strasbourg, FR [IP]", "http://130.79.128.4/simbad/sim-script?script=");
        _simbadMirrors.put("SIMBAD Harvard, US [IP]", "http://131.142.185.22/simbad/sim-script?script=");
    }
    /* members */
    /** The sought star name(s) */
    private final String _starNames;
    /** The star data container */
    private final Star _starModel;
    /** The thread executing the CDS SIMBAD query and parsing */
    private ResolveStarThread _resolveStarThread = null;
    /** running job left to complete */
    private int _jobCounter = 0;
    /** Star list */
    private final StarList _starList;

    /**
     * Constructor.
     *
     * @param name the name of the star to resolve.
     * @param star the star to fulfill.
     */
    public StarResolver(final String name, final Star star) {
        _starNames = name;
        _starModel = star;
        _starList = null;
    }

    /**
     * Dedicated constructor for multiple star resolution.
     *
     * @param names the names of the star to resolve, separated by semi-colons.
     */
    public StarResolver(final String names, final StarList stars) {
        _starNames = names;
        _starModel = new Star();
        _starList = stars;
    }

    /**
     * Asynchronously query CDS SIMBAD to retrieve a given star information according to its name.
     */
    public void resolve() {
        _logger.trace("StarResolver.resolve");

        if (_resolveStarThread != null) {
            _logger.warn("A star resolution thread is already running, so doing nothing.");
            return;
        }

        // Launch the query in the background in order to keep GUI updated

        // Define the star name
        Star newStarModel = new Star();
        _jobCounter = 1;
        newStarModel.setName(_starNames);

        _resolveStarThread = new ResolveStarThread(_starNames, newStarModel);

        // Define UncaughtExceptionHandler
        MCSExceptionHandler.installThreadHandler(_resolveStarThread);

        // Launch query
        _resolveStarThread.start();
    }

    /**
     * Synchronously query CDS SIMBAD to retrieve multiple stars information according to their names.
     */
    public void multipleResolve() {
        _logger.trace("StarResolver.multipleResolve");

        if (_starList == null) {
            _logger.warn("No star list provided, so doing nothing.");
            return;
        }

        if (_resolveStarThread != null) {
            _logger.warn("A star resolution thread is already running, so doing nothing.");
            return;
        }

        // Flush current list
        _starList.clear();

        // Launch the query in the background in order to keep GUI updated
        String[] names = _starNames.split(SEPARATOR_SEMI_COLON);
        _jobCounter = names.length;
        for (String name : names) {

            // Skip empty names
            if (name.isEmpty()) {
                decrementJobCounter();
                continue;
            }

            // Define the star name
            Star newStarModel = new Star();
            newStarModel.setName(name);
            _starList.add(newStarModel);

            _resolveStarThread = new ResolveStarThread(_starNames, newStarModel);

            // Define UncaughtExceptionHandler
            MCSExceptionHandler.installThreadHandler(_resolveStarThread);

            // Launch query
            _resolveStarThread.start();
        }
    }

    private synchronized void decrementJobCounter() {
        _jobCounter--;
        if (_jobCounter <= 0) {
            if (_starList != null) {
                // Use EDT to ensure only 1 thread (EDT) updates the model and handles the notification :
                SwingUtils.invokeEDT(new Runnable() {
                    @Override
                    public void run() {
                        // Notify all registered observers that the query went fine :
                        _starList.fireNotification(Star.Notification.QUERY_COMPLETE);
                    }
                });
            }
        }
    }

    /**
     * Get the list of available mirrors.
     * @return one set of available mirror names.
     */
    public static Set<String> getSimbadMirrors() {
        return _simbadMirrors.keySet();
    }

    /**
     * Return the current SIMBAD mirror
     * @return SIMBAD mirror name
     */
    public static String getSimbadMirror() {
        if (_simbadMirror == null) {
            setSimbadMirror(getSimbadMirrors().iterator().next());
        }
        return _simbadMirror;
    }

    /**
     * Return the SIMBAD URL from the current mirror or the first one
     * @return SIMBAD URL
     */
    public static String getSimbadUrl() {
        if (_simbadMirror == null) {
            setSimbadMirror(getSimbadMirrors().iterator().next());
        }

        return _simbadMirrors.get(_simbadMirror);
    }

    /**
     * Choose one mirror giving its name chosen from available ones.
     * @param mirrorName value chosen from getSimbadMirrors().
     */
    public static void setSimbadMirror(final String mirrorName) {
        // prevent bad cases for bad mirror names
        if (_simbadMirrors.get(mirrorName) == null) {
            _simbadMirror = getSimbadMirrors().iterator().next();
        } else {
            _simbadMirror = mirrorName;
        }
    }

    /**
     * Return the next SIMBAD Mirror which URL is not in the failed URL Set
     * @param failedUrl failed URL(s)
     * @return next SIMBAD Mirror or null if none is still available
     */
    private static String getNextSimbadMirror(final Set<String> failedUrl) {
        for (Map.Entry<String, String> e : _simbadMirrors.entrySet()) {
            if (!failedUrl.contains(e.getValue())) {
                // change mirror:
                setSimbadMirror(e.getKey());
                return _simbadMirror;
            }
        }
        return null;
    }

    /**
     * Command-line tool that tries to resolve the star name given as first parameter.
     * @param args first argument is the star name
     */
    public static void main(String[] args) {
        // Context initialization
        final String starName = args[0];
        final Star star = new Star();
        star.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // Output results
                System.out.println("Star '" + starName + "' contains:\n" + star);
            }
        });

        // Seek data about the given star name (first arg on command line)
        StarResolver starResolver = new StarResolver(starName, star);
        starResolver.resolve();
    }

    /**
     * Star resolver thread : launch and handle CDS SIMBAD query
     */
    private final class ResolveStarThread extends Thread {

        /** flag to indicate that an error occurred */
        private boolean _error = false;
        /** SIMBAD querying result */
        private String _result = null;
        /** The sought star name */
        private final String _starName;
        /**
         * The querying result data container, not to overwrite original model with
         * incomplete data in case an error occurs during CDS querying
         */
        private final Star _newStarModel;

        private ResolveStarThread(final String starName, final Star newStarModel) {
            _starName = starName;
            _newStarModel = newStarModel;
        }

        @Override
        public void run() {
            _logger.trace("ResolveStarThread.run");

            querySimbad();

            if (!isError()) {
                parseResult();
            }

            decrementJobCounter();
        }

        /**
         * Set an error message from CDS SIMBAD query execution, and notify
         * registered observers.
         * @param message the error message to store.
         */
        private void raiseCDSimbadErrorMessage(final String message) {
            raiseCDSimbadErrorMessage(message, (Exception) null);
        }

        /**
         * Set an error message from CDS SIMBAD query execution, and notify
         * registered observers.
         * @param message the error message to store.
         * @param exception exception (optional)
         */
        private void raiseCDSimbadErrorMessage(final String message, final Exception exception) {
            _error = true;

            if (exception != null) {
                String msg = message + " :\n" + getExceptionMessage(exception);

                // avoid too long messages:
                if (msg.length() > 128) {
                    msg = msg.substring(0, 128);
                }
                _starModel.raiseCDSimbadErrorMessage(msg);
            } else {
                _starModel.raiseCDSimbadErrorMessage(message);
            }

            decrementJobCounter();
        }

        /**
         * Set an error message from CDS SIMBAD query execution, and notify registered observers.
         * @param message the error message to store.
         * @param messages exception list (optional)
         */
        private void raiseCDSimbadErrorMessage(final String message, final List<String> messages) {
            _error = true;

            if (messages != null && !messages.isEmpty()) {
                String msg = message + " :";

                for (String m : messages) {
                    // avoid too long messages:
                    if (m.length() > 128) {
                        m = m.substring(0, 128);
                    }
                    msg += "\n" + m;
                }

                _starModel.raiseCDSimbadErrorMessage(msg);
            } else {
                _starModel.raiseCDSimbadErrorMessage(message);
            }

            decrementJobCounter();
        }

        /**
         * Format specific exception messages
         * @param e exception
         * @return user message for the given exception
         */
        private String getExceptionMessage(final Exception e) {
            if (e instanceof UnknownHostException) {
                return "Unknown host [" + e.getMessage() + "]";
            }
            return e.getMessage();
        }

        /**
         * Return the flag indicating that an error occurred
         * @return true if an error occurred
         */
        private boolean isError() {
            return _error;
        }

        /**
         * Query SIMBAD using script
         */
        public void querySimbad() {
            _logger.trace("ResolveStarThread.querySimbad");

            // Should never receive an empty scence object name
            if (_starName.length() == 0) {
                raiseCDSimbadErrorMessage("Could not resolve empty star name.");
                return;
            }

            // Reset result before proceeding
            _result = "";

            // buffer used for both script and result :
            final StringBuilder sb = new StringBuilder(1024);

            // Forge Simbad script to execute
            sb.append("output console=off script=off\n"); // Just data
            sb.append("format object form1 \""); // Simbad script preambule
            sb.append("%COO(d;A);%COO(d;D);%COO(A);%COO(D);\\n"); // RA and DEC coordinates as sexagesimal and decimal degree values
            sb.append("%OTYPELIST\\n"); // Object types enumeration
            sb.append("%FLUXLIST(V,I,J,H,K;N=F,)\\n"); // Magnitudes, 'Band=Value' format
            sb.append("%PM(A;D)\\n"); // Proper motion with error
            sb.append("%PLX(V;E)\\n"); // Parallax with error
            sb.append("%SP(S)\\n"); // Spectral types enumeration
            sb.append("%RV(V;W)\\n"); // Radial velocity
            sb.append("%IDLIST[%*,]"); // Simbad identifiers
            sb.append("\"\n"); // Simbad script end
            sb.append("query id ").append(_starName); // Add the object name we are looking for

            final String simbadScript = sb.toString();

            _logger.trace("CDS Simbad script :\n{}", simbadScript);

            // Forge the URL in UTF8 unicode charset
            final String encodedScript = UrlUtils.encode(simbadScript);

            // Use prefered Simbad mirror
            String simbadMirror = getSimbadMirror();
            String simbadURL;

            final Set<String> failedUrl = new HashSet<String>(4);
            List<String> ioMessages = null;
            InputStream inputStream = null;
            BufferedReader bufferedReader = null;

            // Retry other mirrors if needed
            while (simbadMirror != null) {
                // Try to get star data from CDS
                simbadURL = getSimbadUrl();
                try {
                    final String simbadQuery = simbadURL + encodedScript;

                    _logger.debug("Querying CDS Simbad: {}", simbadQuery);

                    // Launch the network query
                    // TODO: use HTTP CLIENT !!
                    inputStream = UrlUtils.parseURL(simbadQuery).openStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream, HTTP_ENCODING));

                    // Read incoming data line by line
                    String currentLine;

                    // reset buffer :
                    sb.setLength(0);

                    while ((currentLine = bufferedReader.readLine()) != null) {
                        sb.append(currentLine).append('\n');
                    }

                    _result = sb.toString();

                    _logger.trace("CDS Simbad raw result :\n{}", _result);

                    // exit from loop:
                    simbadMirror = null;

                } catch (IOException ioe) {
                    _logger.info("Simbad connection failed: {}", getExceptionMessage(ioe));

                    if (ioMessages == null) {
                        ioMessages = new ArrayList<String>(4);
                    }
                    ioMessages.add("[" + simbadMirror + "] " + getExceptionMessage(ioe));

                    failedUrl.add(simbadURL);

                    // get another simbad mirror:
                    simbadMirror = getNextSimbadMirror(failedUrl);

                    if (simbadMirror != null) {
                        _logger.info("Trying another Simbad mirror [{}]", simbadMirror);

                        StatusBar.show("Simbad connection failed: trying another mirror [" + simbadMirror + "] ...");
                    } else {
                        // no more mirror to use (probably bad network settings):
                        ioMessages.add("\nPlease check your network connection !");

                        raiseCDSimbadErrorMessage("Simbad connection failed", ioMessages);
                    }

                } finally {
                    if (bufferedReader != null) {
                        FileUtils.closeFile(bufferedReader);
                    } else {
                        FileUtils.closeStream(inputStream);
                    }
                }
            }
        }

        /**
         * Parse SIMBAD response
         */
        public void parseResult() {
            _logger.trace("ResolveStarThread.parseResult");

            // If the result string is empty
            if (_result.length() < 1) {
                raiseCDSimbadErrorMessage("No data for star '" + _starName + "'.");
                return;
            }

            // If there was an error during query
            if (_result.startsWith("::error")) {
                // sample error (name not found):
                /*
                 ::error:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

                 [3] Identifier not found in the database : NAME TEST
                 */
                // try to get error message:
                String errorMessage = null;
                final int pos = _result.indexOf('\n');
                if (pos != -1) {
                    errorMessage = StringUtils.removeRedundantWhiteSpaces(_result.substring(pos + 1)).trim();
                }

                raiseCDSimbadErrorMessage("Querying script execution failed for star '" + _starName + "' "
                        + ((errorMessage != null) ? ":\n" + errorMessage : "."));
                return;
            }

            // Remove any blanking character (~) :
            _result = _result.replaceAll("~[ ]*", "");

            _logger.trace("CDS Simbad result without blanking values :\n{}", _result);

            try {
                // Parsing result line by line :

                // Special line tokenizer because it is possible to have a blank lines (no flux at all) :
                final StrictStringTokenizer lineTokenizer = new StrictStringTokenizer(_result, "\n");

                // First line should contain star coordinates, separated by ';'
                final String coordinates = lineTokenizer.nextToken();
                parseCoordinates(coordinates);

                // Second line should contain object types, separated by ','
                final String objectTypes = lineTokenizer.nextToken();
                parseObjectTypes(objectTypes);

                // Third line should contain star fluxes, separated by ','
                final String fluxes = lineTokenizer.nextToken();
                parseFluxes(fluxes);

                // Forth line should contain star proper motions, separated by ';'
                final String properMotion = lineTokenizer.nextToken();
                parseProperMotion(properMotion);

                // Fith line should contain star parallax, separated by ';'
                final String parallax = lineTokenizer.nextToken();
                parseParallax(parallax);

                // Sixth line should contain star spectral types
                final String spectralTypes = lineTokenizer.nextToken();
                parseSpectralTypes(spectralTypes);

                // Seventh line should contain radial velocity, separated by ';'
                final String radialVelocity = lineTokenizer.nextToken();
                parseRadialVelocity(radialVelocity);

                // Eigth line should contain SIMBAD identifiers, separated by ','
                final String identifiers = lineTokenizer.nextToken();
                parseIdentifiers(identifiers);

                /*
                 * At this stage parsing went fine.
                 * So copy back the new CDS Simbad result in the original Star object.
                 *
                 * Done only after all data were fetched and parsed successfully, to
                 * always have consistent data in _starModel.
                 *
                 * If anything went wrong while querying or parsing, previous data
                 * remain unchanged.
                 */

                // Use EDT to ensure only 1 thread (EDT) updates the model and handles the notification :
                SwingUtils.invokeEDT(new Runnable() {
                    @Override
                    public void run() {
                        _starModel.copy(_newStarModel);

                        // Notify all registered observers that the query went fine :
                        _starModel.fireNotification(Star.Notification.QUERY_COMPLETE);
                    }
                });

            } catch (NumberFormatException nfe) {
                raiseCDSimbadErrorMessage("Could not parse data for star '" + _starName + "'", nfe);
            } catch (ParseException pe) {
                raiseCDSimbadErrorMessage("Could not parse data for star '" + _starName + "'", pe);
            }
        }

        /**
         * Parse star RA / DEC coordinates
         * @param coordinates SIMBAD RA / DEC coordinates
         * @throws ParseException if parsing SIMBAD RA/DEC failed
         * @throws NumberFormatException if parsing number(s) failed
         */
        private void parseCoordinates(final String coordinates) throws ParseException, NumberFormatException {
            _logger.debug("Coordinates contains '{}'.", coordinates);

            final StringTokenizer coordinatesTokenizer = new StringTokenizer(coordinates, SEPARATOR_SEMI_COLON);

            if (coordinatesTokenizer.countTokens() == 4) {
                final double ra = Double.parseDouble(coordinatesTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("RA_d = '{}'.", ra);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.RA_d, ra);

                final double dec = Double.parseDouble(coordinatesTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("DEC_d = '{}'.", dec);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.DEC_d, dec);

                final String hmsRa = coordinatesTokenizer.nextToken();
                if (_logger.isTraceEnabled()) {
                    _logger.trace("RA = '{}'.", hmsRa);
                }
                _newStarModel.setPropertyAsString(Star.Property.RA, hmsRa);

                final String dmsDec = coordinatesTokenizer.nextToken();
                if (_logger.isTraceEnabled()) {
                    _logger.trace("DEC = '{}'.", dmsDec);
                }
                _newStarModel.setPropertyAsString(Star.Property.DEC, dmsDec);
            } else {
                throw new ParseException("Invalid coordinates '" + coordinates + "'", -1);
            }
        }

        /**
         * Parse object types
         * @param objectTypes SIMBAD object types
         */
        private void parseObjectTypes(final String objectTypes) {
            _logger.debug("Object Types contains '{}'.", objectTypes);

            _newStarModel.setPropertyAsString(Star.Property.OTYPELIST, objectTypes);
        }

        /**
         * Parse magnitudes
         * @param fluxes SIMBAD fluxes
         * @throws NumberFormatException if parsing number(s) failed
         */
        private void parseFluxes(final String fluxes) throws NumberFormatException {
            _logger.debug("Fluxes contains '{}'.", fluxes);

            final StringTokenizer fluxesTokenizer = new StringTokenizer(fluxes, SEPARATOR_COMMA);

            while (fluxesTokenizer.hasMoreTokens()) {
                final String token = fluxesTokenizer.nextToken();
                // The first character is the magnitude band letter :
                final String magnitudeBand = "FLUX_" + token.substring(0, 1).toUpperCase();
                // The second character is "=", followed by the magnitude value in double :
                final String value = token.substring(2);

                if (_logger.isTraceEnabled()) {
                    _logger.trace("{} = '{}'.", magnitudeBand, value);
                }

                _newStarModel.setPropertyAsDouble(Star.Property.fromString(magnitudeBand), Double.parseDouble(value));
            }
        }

        /**
         * Parse optional proper motion
         * @param properMotion
         * @throws NumberFormatException if parsing number(s) failed
         */
        private void parseProperMotion(final String properMotion) throws NumberFormatException {
            _logger.debug("Proper Motion contains '{}'.", properMotion);

            final StringTokenizer properMotionTokenizer = new StringTokenizer(properMotion, SEPARATOR_SEMI_COLON);

            if (properMotionTokenizer.countTokens() == 2) {
                final double pm_ra = Double.parseDouble(properMotionTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("PROPERMOTION_RA = '{}'.", pm_ra);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.PROPERMOTION_RA, pm_ra);

                final double pm_dec = Double.parseDouble(properMotionTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("PROPERMOTION_DEC = '{}'.", pm_dec);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.PROPERMOTION_DEC, pm_dec);
            } else {
                if (_logger.isTraceEnabled()) {
                    _logger.trace("No proper motion data for star '{}'.", _starName);
                }
            }
        }

        /**
         * Parse optional parallax
         * @param parallax SIMBAD parallax
         * @throws NumberFormatException if parsing number(s) failed
         */
        private void parseParallax(final String parallax) throws NumberFormatException {
            _logger.debug("Parallax contains '{}'.", parallax);

            final StringTokenizer parallaxTokenizer = new StringTokenizer(parallax, SEPARATOR_SEMI_COLON);

            if (parallaxTokenizer.countTokens() == 2) {
                final double plx = Double.parseDouble(parallaxTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("PARALLAX = '{}'.", plx);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.PARALLAX, plx);

                final double plx_err = Double.parseDouble(parallaxTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("PARALLAX_err = '{}'.", plx_err);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.PARALLAX_err, plx_err);
            } else {
                if (_logger.isTraceEnabled()) {
                    _logger.trace("No parallax data for star '{}'.", _starName);
                }
            }
        }

        /**
         * Parse spectral types
         * @param spectralTypes SIMBAD spectral types
         */
        private void parseSpectralTypes(final String spectralTypes) {
            _logger.debug("Spectral Types contains '{}'.", spectralTypes);

            _newStarModel.setPropertyAsString(Star.Property.SPECTRALTYPES, spectralTypes);
        }

        /**
         * Parse optional radial velocity
         * @param radialVelocity SIMBAD radial velocity
         * @throws NumberFormatException if parsing number(s) failed
         */
        private void parseRadialVelocity(final String radialVelocity) throws NumberFormatException {
            _logger.debug("Radial velocity contains '{}'.", radialVelocity);

            final StringTokenizer rvTokenizer = new StringTokenizer(radialVelocity, SEPARATOR_SEMI_COLON);

            if (rvTokenizer.countTokens() > 0) {
                final double rv = Double.parseDouble(rvTokenizer.nextToken());
                if (_logger.isTraceEnabled()) {
                    _logger.trace("RV = '{}'.", rv);
                }
                _newStarModel.setPropertyAsDouble(Star.Property.RV, rv);

                if (rvTokenizer.hasMoreTokens()) {
                    final String rv_def = rvTokenizer.nextToken();
                    if (_logger.isTraceEnabled()) {
                        _logger.trace("RV_DEF = '{}'.", rv_def);
                    }
                    _newStarModel.setPropertyAsString(Star.Property.RV_DEF, rv_def);
                }
            } else {
                if (_logger.isTraceEnabled()) {
                    _logger.trace("No radial velocity data for star '{}'.", _starName);
                }
            }
        }

        /**
         * Parse optional identifiers
         * @param identifiers SIMBAD identifiers
         */
        private void parseIdentifiers(final String identifiers) {
            _logger.debug("Identifiers contain '{}'.", identifiers);

            String ids = identifiers;

            if (ids.length() > 0) {
                // remove redundant space characters :
                ids = StringUtils.removeRedundantWhiteSpaces(ids);
            }

            if (ids.length() > 0) {
                // remove last separator :
                ids = ids.substring(0, ids.length() - 1);
            }

            _newStarModel.setPropertyAsString(Star.Property.IDS, ids);
        }
    }

    /**
     * StringTokenizer Hack to return empty token if multiple delimiters found
     *
     * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4140850
     */
    private static final class StrictStringTokenizer {

        /** delimiter */
        private final String delimiter;
        /** internal string tokenizer returning delimiter too */
        private final StringTokenizer st;
        /** last token reminder */
        private String lastToken;

        /**
         * Special StringTokenizer that returns empty token if multiple delimiters encountered
         * @param input input string
         * @param delimiter delimiter
         */
        StrictStringTokenizer(final String input, final String delimiter) {
            this.delimiter = delimiter;
            this.st = new StringTokenizer(input, delimiter, true);
            this.lastToken = delimiter;// if first token is separator
        }

        /**
         * @return the next token from this string tokenizer.
         */
        public String nextToken() {
            String result = null;

            String token;
            while (result == null && this.st.hasMoreTokens()) {
                token = this.st.nextToken();
                if (token.equals(this.delimiter)) {
                    if (this.lastToken.equals(this.delimiter)) {
                        // no value between 2 separators ?
                        result = "";
                    }
                } else {
                    result = token;
                }
                this.lastToken = token;
            } // next token
            if (result == null) {
                result = "";
            }
            return result;
        }
    }
}
/*___oOo___*/