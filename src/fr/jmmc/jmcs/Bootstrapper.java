/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmcs;

import ch.qos.logback.classic.Logger;
import com.apple.eawt.QuitResponse;
import fr.jmmc.jmcs.App.ApplicationState;
import fr.jmmc.jmcs.data.app.ApplicationDescription;
import fr.jmmc.jmcs.data.preference.CommonPreferences;
import fr.jmmc.jmcs.gui.FeedbackReport;
import fr.jmmc.jmcs.gui.MainMenuBar;
import fr.jmmc.jmcs.gui.SplashScreen;
import fr.jmmc.jmcs.gui.action.ActionRegistrar;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.gui.component.ResizableTextViewFactory;
import fr.jmmc.jmcs.gui.task.TaskSwingWorkerExecutor;
import fr.jmmc.jmcs.gui.util.MacOSXAdapter;
import fr.jmmc.jmcs.gui.util.SwingSettings;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.network.NetworkSettings;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.util.IntrospectionUtils;
import fr.jmmc.jmcs.util.concurrent.ParallelJobExecutor;
import fr.jmmc.jmcs.util.logging.LoggingService;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.swing.JFrame;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang.SystemUtils;
import fr.jmmc.jmcs.util.runner.LocalLauncher;

/**
 * This class ordinate App life-cycle.
 *
 * Starting with jMCS initialization (logs, SWING, network ...);
 * Followed in this order:
 * - App.initServices()
 * - App.setupGui()
 * - App.execute()
 * - App.cleanup()
 *
 * @see App
 * @author Sylvain LAFRASSE, Laurent BOURGES.
 */
public final class Bootstrapper {

    /** JMMC Logger */
    private final static Logger _jmmcLogger = LoggingService.getJmmcLogger();
    /** user defined Locale before setting Locale.US */
    private static Locale _userLocale = null;
    /** user defined Timezone before setting Timezone.GMT */
    private static TimeZone _userTimeZone = null;
    /** Flag to avoid reentrance in launch sequence */
    private static boolean _staticBootstrapDone = false;
    /** Store whether application should be quit when main frame close box clicked. */
    private static boolean _exitApplicationWhenClosed = false;
    /** Flag to prevent calls to System.exit() */
    private static boolean _avoidSystemExit = false;
    /** Flag indicating if the application started properly and is ready (visible) */
    private static ApplicationState _applicationState = ApplicationState.JAVA_LIMB;
    /** The application  instance */
    private static App _application = null;

    /**
     * Static Logger initialization and Network settings
     */
    static {
        Bootstrapper.bootstrap();
    }

    /**
     * Static minimal service initialization: logger, swing, network ...
     * @throws IllegalStateException if any exception occurs during initialization.
     * @return true if the initialization sequence succeeds, false otherwise.
     */
    static boolean bootstrap() throws IllegalStateException {
        // avoid reentrance:
        if (_staticBootstrapDone) {
            return true;
        }

        // Disable security checks:
        disableSecurityManager();

        // Set System properties:
        // note: it calls: System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        // Must be set before any call to Collections or Arrays.sort(Object[]) that use that property once
        // ie before initializing Logs because it calls Collections.sort in LoggerContext.getLoggerList:195	
        setSystemProps();

        // Initialize Locale.US:
        initializeLocale();

        // Start the application log singleton:
        LoggingService.getInstance();
        _jmmcLogger.info("jMCS log created at {}. Current level is {}.", new Date(), _jmmcLogger.getEffectiveLevel());

        _jmmcLogger.info("jMCS environment bootstrapping...");
        setState(ApplicationState.ENV_BOOTSTRAP);

        // Define swing settings (laf, defaults...) before any Swing usage if not called at the first line of the main method:
        SwingSettings.setup();

        // Define default network settings:
        NetworkSettings.defineDefaults();

        // Set reentrance flag
        _staticBootstrapDone = true;
        return true;
    }

    /**
     * Defines in code some System.properties to force text anti-aliasing and Mac OS features ...
     * Called by bootstrap() before anything
     * @see MacOSXAdapter
     */
    private static void setSystemProps() {
        // force anti aliasing :
        if (SystemUtils.IS_JAVA_1_5) {
            System.setProperty("swing.aatext", "true");
        } else if (SystemUtils.IS_JAVA_1_6) {
            final String old = System.getProperty("awt.useSystemAAFontSettings");
            if (old == null) {
                System.setProperty("awt.useSystemAAFontSettings", "on");
            }
        }

        if (SystemUtils.IS_OS_MAC_OSX) {
            // always use screen menuBar on MacOS X:
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        // JDK 1.7 settings:
        if (SystemUtils.isJavaVersionAtLeast(1.7f)) {
            // Fix JDK 1.7 - Swing Focus : java.lang.IllegalArgumentException: Comparison method violates its general contract!
            // bug in SortingFocusTraversalPolicy.enumerateAndSortCycle() related to LayoutComparator
            // See also JIDE: @see com.jidesoft.plaf.LookAndFeelFactory#workAroundSwingIssues()
            /*
             * http://stackoverflow.com/questions/13575224/comparison-method-violates-its-general-contract-timsort-and-gridlayout
             * https://forums.oracle.com/forums/thread.jspa?threadID=2455538
             */
            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        }
    }

    /**
     * Disable the security manager to be able to use System.setProperty ...
     */
    private static void disableSecurityManager() {
        try {
            // Disable security checks:
            System.setSecurityManager(null);
        } catch (SecurityException se) {
            // This case occurs with java netx and
            // OpenJDK Runtime Environment (IcedTea6 1.6) (rhel-1.13.b16.el5-x86_64)
            _jmmcLogger.warn("Can't set security manager to null", se);
        }
    }

    /**
     * Initialize default locale and default time zone.
     */
    private static void initializeLocale() {
        // Backup user settings:
        _userLocale = Locale.getDefault();
        _userTimeZone = TimeZone.getDefault();

        // Set the default locale to en-US locale (for Numerical Fields "." ",")
        Locale.setDefault(Locale.US);

        // Set the default timezone to GMT to handle properly the date in UTC:
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Return the user Locale defined before using Locale.US
     * @return user Locale
     */
    public static Locale getUserLocale() {
        return _userLocale;
    }

    /**
     * Return the user TimeZone defined before using GMT
     * @return user TimeZone
     */
    public static TimeZone getUserTimeZone() {
        return _userTimeZone;
    }

    /**
     * Launch an application that will:
     * - execute directly after services initialization and GUI setup;
     * - trap and properly exit on main frame close button click;
     * - show a splash screen during bootstrap sequence.
     * @param application the App object to launch.
     * @return true if all went well, false otherwise.
     * @throws IllegalStateException in case something went really wrong.
     */
    public static boolean launchApp(final App application) throws IllegalStateException {
        return launchApp(application, false);
    }

    /**
     * Launch an application that will:
     * - wait (or not) after services initialization and GUI setup;
     * - trap and properly exit on main frame close button click;
     * - show a splash screen during bootstrap sequence.
     * @param application the App object to launch.
     * @param waitBeforeExecution if true, do not launch App.execute() automatically.
     * @return true if all went well, false otherwise.
     * @throws IllegalStateException in case something went really wrong.
     */
    public static boolean launchApp(final App application, boolean waitBeforeExecution) throws IllegalStateException {
        return launchApp(application, waitBeforeExecution, true);
    }

    /**
     * Launch an application that will:
     * - wait (or not) after services initialization and GUI setup;
     * - trap and properly exit (or not) on main frame close button click;
     * - show a splash screen during bootstrap sequence.
     * @param application the App object to launch.
     * @param waitBeforeExecution if true, do not launch App.execute() automatically.
     * @param exitWhenClosed if true, the application will close when exit method is called.
     * @return true if all went well, false otherwise.
     * @throws IllegalStateException in case something went really wrong.
     */
    public static boolean launchApp(final App application, boolean waitBeforeExecution, boolean exitWhenClosed) throws IllegalStateException {
        final boolean shouldShowSplashScreen = CommonPreferences.getInstance().getPreferenceAsBoolean(CommonPreferences.SHOW_STARTUP_SPLASHSCREEN);
        return launchApp(application, waitBeforeExecution, exitWhenClosed, shouldShowSplashScreen);
    }

    /**
     * Start the application properly:
     * - starts all critical jMCS services;
     * - calls your App.initServices() method to start your services;
     * - calls your App.setupGui() method to setup your graphical interfaces (in EDT);
     * - calls your App.execute() method.
     *
     * @param application your application to start.
     * @param waitBeforeExecution if true, do not launch App.execute() automatically.
     * @param exitWhenClosed if true, the application will close when exit method is called.
     * @param shouldShowSplashScreen show startup splash screen if true, nothing otherwise.
     *
     * @return true on success, false otherwise.
     * @throws IllegalStateException
     */
    public static boolean launchApp(final App application, final boolean waitBeforeExecution, final boolean exitWhenClosed,
            final boolean shouldShowSplashScreen) throws IllegalStateException {

        return ___internalLaunch(application, exitWhenClosed, shouldShowSplashScreen);
    }

    /**
     * Internal: launch given application instance
     * @param application application instance to launch
     * @param exitWhenClosed flag indicating if application should be quit when main frame close box clicked.
     * @param shouldShowSplashScreen true to effectively show the splash screen, false otherwise.
     * @return true if launch succeeded; false otherwise
     */
    private static boolean ___internalLaunch(final App application, final boolean exitWhenClosed, final boolean shouldShowSplashScreen) {

        setState(ApplicationState.ENV_INIT);
        final long startTime = System.nanoTime();
        boolean launchDone = false;

        _application = application;
        _exitApplicationWhenClosed = exitWhenClosed;
        _application.___internalSingletonInitialization();

        try {
            // Load jMCS and application data models
            ApplicationDescription.init();
            _jmmcLogger.debug("Application data loaded.");
            _jmmcLogger.info("jMCS launching '{}' application...", ApplicationDescription.getInstance().getProgramNameWithVersion());

            _application.___internalStart();

            // Build Acknowledgment, ShowRelease and ShowHelp Actions
            // (the creation must be done after applicationModel instanciation)
            ActionRegistrar.getInstance().createAllInternalActions();

            setState(ApplicationState.APP_INIT);

            application.initServices();

            SplashScreen.display(shouldShowSplashScreen);

            ___internalRun();
            launchDone = true;
        } catch (Throwable th) {
            setState(ApplicationState.APP_BROKEN);

            // Show the feedback report (modal)
            SplashScreen.close();
            MessagePane.showErrorMessage("An error occured while initializing the application");
            FeedbackReport.openDialog(true, th);
        } finally {
            final double elapsedTime = 1e-6d * (System.nanoTime() - startTime);
            _jmmcLogger.info("Application startup done (duration = {} ms).", elapsedTime);
        }

        return launchDone;
    }

    /**
     * Describe the life cycle of the application.
     */
    private static void ___internalRun() {

        // Using invokeAndWait to be in sync with this thread :
        // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
        SwingUtils.invokeAndWaitEDT(new Runnable() {
            /**
             * Initializes Splash Screen in EDT
             */
            @Override
            public void run() {

                // If running under Mac OS X
                if (SystemUtils.IS_OS_MAC_OSX) {
                    // Set application name :
                    // system properties must be set before using any Swing component:
                    // Hope nothing as already been done...
                    System.setProperty("com.apple.mrj.application.apple.menu.about.name", ApplicationDescription.getInstance().getProgramName());
                }


                setState(ApplicationState.GUI_SETUP);

                // Delegate initialization to daughter class through abstract setupGui() call
                _application.setupGui();

                // Initialize SampManager as needed by MainMenuBar:
                SampManager.getInstance();

                // Declare SAMP message handlers first:
                _application.declareInteroperability();

                // Perform defered action initialization (SAMP-related actions)
                ActionRegistrar.getInstance().performDeferedInitialization();

                // Define the jframe associated to the application which will get the JMenuBar
                final JFrame frame = App.getFrame();
                // Use OSXAdapter on the frame
                macOSXRegistration(frame);
                // create menus including the Interop menu (SAMP required)
                frame.setJMenuBar(new MainMenuBar());

                // Set application frame common properties
                frame.pack();
            }
        });

        ResizableTextViewFactory.showUnsupportedJdkWarning();

        // Indicate that the application is ready (visible)
        setState(ApplicationState.APP_READY);

        // Delegate execution to daughter class through abstract execute() call
        _application.execute();

        // Optionally Open given File:
        _application.openCommandLineFile();
    }

    /**
     * Generic registration with the Mac OS X application menu (if needed).
     * @param frame application frame
     */
    private static void macOSXRegistration(final JFrame frame) {
        // If running under Mac OS X
        if (SystemUtils.IS_OS_MAC_OSX) {

            // Set the menu bar under Mac OS X
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            final Class<?> osxAdapter = IntrospectionUtils.getClass("fr.jmmc.jmcs.gui.util.MacOSXAdapter");
            if (osxAdapter == null) {
                // This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
                // because OSXAdapter extends ApplicationAdapter in its def
                _jmmcLogger.error("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled.");
            } else {
                final Method registerMethod = IntrospectionUtils.getMethod(osxAdapter, "registerMacOSXApplication", new Class<?>[]{JFrame.class});
                if (registerMethod != null) {
                    IntrospectionUtils.executeMethod(registerMethod, new Object[]{frame});
                }
            }
        }
    }

    /**
     * @return true if the application should exit when frame is closed, false otherwise.
     */
    public static boolean shouldExitAppWhenFrameClosed() {
        return _exitApplicationWhenClosed;
    }

    /**
     * Define the flag to avoid calls to System.exit().
     * @param flag true to avoid calls to System.exit()
     */
    public static void disableSystemExit(final boolean flag) {
        _avoidSystemExit = flag;
    }

    /**
     * Quit the application properly:
     * - warn user of SAMP shutdown if needed;
     * - prompt user of unsaved data loss;
     * - stops application if user is OK.
     * @param evt the triggering event if any, null otherwise.
     */
    public static void quitApp(ActionEvent evt) {

        _jmmcLogger.info("Application quitting.");

        // Mac OS X Quit action handler
        final QuitResponse response;
        if (evt != null && evt.getSource() instanceof QuitResponse) {
            response = (QuitResponse) evt.getSource();
        } else {
            response = null;
        }

        // Check if user is OK to kill SAMP hub (if any)
        if (!SampManager.getInstance().allowHubKilling()) {
            _jmmcLogger.debug("SAMP cancelled application kill.");
            // Otherwise cancel quit
            if (response != null) {
                response.cancelQuit();
            }
            return;
        }

        // If we are ready to stop application execution
        if (_application.canBeTerminatedNow()) {
            _jmmcLogger.debug("Application should be killed.");

            // Verify if we are authorized to kill the application or not
            if (shouldExitAppWhenFrameClosed()) {

                setState(ApplicationState.APP_STOP);

                // Max OS X quit
                if (response != null) {
                    disableSystemExit(true);
                }

                // Exit the application
                stopApp(0);

                // Max OS X quit
                if (response != null) {
                    response.performQuit();
                }
            } else {
                _jmmcLogger.debug("Application frame left opened as required.");
            }
        } else {
            _jmmcLogger.debug("Application quit cancelled.");
        }
        if (response != null) {
            response.cancelQuit();
        }
    }

    /**
     * Stop the application properly without user feedback:
     * - calls your App.cleanup() method;
     * - stops all critical jMCS services;
     * - System.exit(statusCode) if so (@see disableSystemExit()).
     * @param statusCode status code to return
     */
    public static void stopApp(final int statusCode) {

        _jmmcLogger.info("Stopping the application.");
        try {
            if (_application != null) {

                setState(ApplicationState.APP_CLEANUP);
                _application.cleanup();

                setState(ApplicationState.ENV_CLEANUP);
                ___internalStop();
            }
        } finally {
            setState(ApplicationState.APP_DEAD);
            App.___internalSingletonCleanup();
            if (!_avoidSystemExit) {
                _jmmcLogger.info("Exiting with status code '{}'.", statusCode);
                System.exit(statusCode);
            }
        }
        setState(ApplicationState.JAVA_LIMB);
    }

    /**
     * Internal: Stop services
     */
    private static void ___internalStop() {

        // Stop the job runner (if any)
        LocalLauncher.shutdown();

        // Stop the task executor (if any)
        TaskSwingWorkerExecutor.shutdown();

        // Stop the parallel job executor (if any)
        ParallelJobExecutor.shutdown();

        // Disconnect from SAMP Hub (if any)
        SampManager.shutdown();

        // Close all HTTP connections (http client) (if any)
        MultiThreadedHttpConnectionManager.shutdownAll();
    }

    /**
     * @param state the new application state.
     */
    private static void setState(final ApplicationState state) {
        _jmmcLogger.debug("Change state from '{}' to '{}'.", _applicationState, state);
        _applicationState = state;
    }

    /**
     * @return the application current state.
     */
    public static ApplicationState getState() {
        return _applicationState;
    }

    /**
     * @return true if the application is in the given state, false otherwise.
     * @param givenState the state to check against.
     */
    public static boolean isInState(ApplicationState givenState) {
        return (_applicationState == givenState);
    }

    /** Private constructor */
    private Bootstrapper() {
        // no-op
    }
}