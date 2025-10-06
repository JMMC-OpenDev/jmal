/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.star;

import ch.qos.logback.classic.Level;
import static fr.jmmc.jmal.star.StarResolver.SERVICE_GET_STAR_BETA;
import static fr.jmmc.jmal.star.StarResolver.SERVICE_SIMBAD_PUBLIC;
import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.gui.component.SearchField;
import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.logging.LoggingService;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * Store informations relative to a star.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES.
 */
public class StarResolverWidget extends SearchField implements StarResolverProgressListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Menu to choose resolver mirrors */
    private transient static JPopupMenu _mirrorPopupMenu = null;

    // Static initialization
    static synchronized JPopupMenu getPopupMenu() {
        if (_mirrorPopupMenu == null) {
            _mirrorPopupMenu = new JPopupMenu();
            // Add title
            JMenuItem menuItem = new JMenuItem("Choose the resolver service:");
            menuItem.setEnabled(false);
            _mirrorPopupMenu.add(menuItem);

            // And populate with StarResolver mirrors
            final Set<String> mirrors = StarResolver.getResolverServiceMirrors();
            for (final String mirror : mirrors) {
                menuItem = new JMenuItem(mirror);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        StarResolver.selectResolverServiceMirror(mirror);
                    }
                });
                _mirrorPopupMenu.add(menuItem);
            }
        }
        return _mirrorPopupMenu;
    }

    /* members */
    /** widget listener to get star resolver result */
    private transient final Map<Object, StarResolverListener> _childListeners = new HashMap<>(4);
    /** flag indicating if the resolver can resolve multiple identifiers */
    private final boolean _supportMultiple;
    /** optional flags associated with the query (atomic updates) */
    private transient Set<String> _flags = null;
    /** star resolver instance */
    private transient final StarResolver _resolver;
    /** Single future instance used to cancel background requests */
    private transient Future<Object> _future = null;

    /**
     * Creates a new StarResolverWidget object that only supports one single identifier
     */
    public StarResolverWidget() {
        this(false);
    }

    /**
     * Creates a new StarResolverWidget object
     * @param supportMultiple flag indicating if the resolver can resolve multiple identifiers
     */
    @SuppressWarnings("unchecked")
    public StarResolverWidget(final boolean supportMultiple) {
        super("Target", getPopupMenu());
        this._supportMultiple = supportMultiple;

        this._resolver = new StarResolver(this,
                new StarResolverListener<Object>() {
            /**
             * Handle the star resolver result (status, error messages, stars):
             * - show error meassages
             * - enable the text field / focus if any error
             * - anyway: propagate the result to the child listener (EDT)
             * @param result star resolver result
             */
            @Override
            public void handleResult(final Object result) {
                _logger.debug("star resolver result:\n{}", result);

                // reset the future instance:
                _future = null;

                SwingUtils.invokeEDT(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (result != null) {
                                if (result instanceof StarResolverResult) {
                                    // Handle status & error messages:
                                    showResultMessage((StarResolverResult) result);
                                }

                                // Propagate the result to the child listener
                                fireResultToChildListener(result);
                            }
                        } finally {
                            // Enable search field after request processing done :
                            setEnabled(true);

                            if (result != null) {
                                if ((result instanceof StarResolverResult)
                                        && ((StarResolverResult) result).isErrorStatus()) {
                                    requestFocus();
                                }
                            }
                        }
                    }
                });
            }
        });

        if (supportMultiple) {
            // fix newline replacement character for copy/paste operations:
            this.setNewLineReplacement(StarResolver.SEPARATOR_SEMI_COLON.charAt(0));
        }

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // note: the action command value is already cleaned by SearchField#cleanText(String)
                final String names = e.getActionCommand();

                final boolean isMultiple = StarResolver.isMultiple(names);

                // Check for multiple identifier support:
                if (!_supportMultiple && isMultiple) {
                    MessagePane.showErrorMessage("Only one identifier expected (remove the ';' character)", "Star resolver problem");
                    return;
                }

                try {
                    // Keep future instance to possibly cancel the job:
                    _future = (isMultiple) ? _resolver.multipleResolve(_flags, names)
                            : _resolver.resolve(_flags, names);

                    // Disable search field while request processing to avoid concurrent requests:
                    setEnabled(false);

                } catch (IllegalArgumentException iae) {
                    MessagePane.showErrorMessage(iae.getMessage());
                }
            }
        });
    }

    @Override
    public String getSelectedOption() {
        return StarResolver.getResolverServiceMirror();
    }

    @Override
    protected void performCancel() {
        _logger.debug("performCancel invoked.");
        if (_future != null) {
            // do cancel background requests:
            _future.cancel(true);
        }
    }

    /**
     * @param resultClass> Resolver result class
     * @return the widget listener to get star resolver result
     */
    public final StarResolverListener<?> getListener(final Class<?> resultClass) {
        return _childListeners.get(resultClass);
    }

    /**
     * @param resultClass> Resolver result class
     * @param listener the widget listener to get star resolver result
     */
    @SuppressWarnings("unchecked")
    public final void setListener(final Class<?> resultClass, final StarResolverListener<?> listener) {
        this._childListeners.put(resultClass, listener);
    }

    /**
     * @return flag indicating if the resolver can resolve multiple identifiers
     */
    public boolean isSupportMultiple() {
        return _supportMultiple;
    }

    /**
     * @return optional flags associated with the query (read-only)
     */
    public Set<String> getFlags() {
        return _flags;
    }

    /**
     * Reset the optional flags associated with the query
     */
    public void resetFlags() {
        _flags = null;
    }

    /**
     * Define the optional flags associated with the query
     * Note: atomic updates required for thread safety, use new Set instance
     * @param flags optional flags associated with the query
     */
    public void setFlags(final String... flags) {
        Set<String> set = null;
        if (flags.length != 0) {
            set = new HashSet<String>(flags.length >> 1);
            for (String flag : flags) {
                set.add(flag);
            }
        }
        this._flags = set;
    }

    /**
     * Clean up the current text value before calling action listeners and update the text field.
     * @param text current text value
     * @return cleaned up text value
     */
    @Override
    public String cleanText(final String text) {
        return StarResolver.cleanNames(text);
    }

    /**
     * Handle the given progress message = show it in the StatusBar (EDT)
     * @param message progress message
     */
    @Override
    public void handleProgressMessage(final String message) {
        StatusBar.show(message);
    }

    public static void showResultMessage(final StarResolverResult result) {
        final String errorMessage;

        // TODO: get both error messages (multiple ?)
        switch (result.getStatus()) {
            case ERROR_SERVER:
                errorMessage = result.getServerErrorMessage();
                break;
            case ERROR_IO:
            case ERROR_PARSING:
                errorMessage = result.getErrorMessage();
                break;

            default:
                errorMessage = null;
        }

        final String warningMessage;

        // Handle multiple matches per identifier:
        if (result instanceof StarListResolverResult && result.isMultipleMatches()) {
            final StarListResolverResult starListResult = (StarListResolverResult)result;
            // TODO: display ambiguous results: let the user select the appropriate star ?
            // Show ambiguous ids for now:
            final List<String> multNames = starListResult.getNamesForMultipleMatches();
            _logger.debug("multNames: {}", multNames);

            final StringBuilder sb = new StringBuilder(256);
            sb.append("Multiple objects found (please refine your query):\n\n");
            for (String name : multNames) {
                sb.append("'").append(name).append("': [ ");
                for (Star star : starListResult.getStars(name)) {
                    String id = star.getId();
                    if (id != null) {
                        sb.append(id);
                    }
                    sb.append(", ");
                }
                sb.delete(sb.length() - 2, sb.length());
                sb.append(" ]\n");
            }
            warningMessage = sb.toString();
        } else {
            warningMessage = null;
        }

        // gather error & warning messages into a single one:
        if (errorMessage != null) {
            String msg = errorMessage;
            if (warningMessage != null) {
                // both messages:
                msg += warningMessage;
            }
            MessagePane.showErrorMessage(msg, "Star resolver problem");
        } else if (warningMessage != null) {
            MessagePane.showWarning(warningMessage, "Star resolver problem");
        }
    }

    /**
     * Fire result to child listener (same class) within EDT
     * @param result star resolver result (not null)
     */
    @SuppressWarnings("unchecked")
    void fireResultToChildListener(final Object result) {
        final StarResolverListener listener = getListener(result.getClass());
        if (listener != null) {
            listener.handleResult(result);
        }
    }

    /**
     * Main - for StarResolverWidget demonstration and test only.
     * @param args unused
     */
    public static void main(final String[] args) {

        // invoke Bootstrapper method to initialize logback now:
        Bootstrapper.getState();

        if (false) {
            LoggingService.setLoggerLevel("fr.jmmc.jmal.star", Level.ALL);
            LoggingService.setLoggerLevel("fr.jmmc.jmcs.network.http", Level.ALL);
        }

        if (true) {
            StarResolver.setResolverServiceMirror(SERVICE_GET_STAR_BETA);
        } else {
            StarResolver.setResolverServiceMirror(SERVICE_SIMBAD_PUBLIC);
        }

        StarResolver.enableGetStar(true);

        final AtomicReference<StarResolverWidget> fieldRef = new AtomicReference<>();

        // GUI initialization (EDT)
        SwingUtils.invokeLaterEDT(new Runnable() {

            @Override
            public void run() {

                // GUI initialization
                final JFrame frame = new JFrame("StarResolverWidget Demo");

                // Force to exit when the frame closes :
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Resolver initialization
                final boolean supportMultiple = true;
                final StarResolverWidget searchField = new StarResolverWidget(supportMultiple);

                final StarResolverListener<Object> listener = new StarResolverListener<Object>() {
                    /**
                     * Handle the star resolver result as String (raw http response) or StarListResolverResult instance (status, error messages, stars) ...
                     * @param result star resolver result
                     */
                    @Override
                    public void handleResult(final Object result) {
                        _logger.info("ASYNC Star resolver result:\n{}", result);
                    }
                };

                // register the StarResolverListener for Simbad:
                searchField.setListener(StarListResolverResult.class, listener);
                // register the StarResolverListener for GetStar:
                searchField.setListener(GetStarResolverResult.class, listener);
                
                fieldRef.set(searchField);

                final JPanel panel = new JPanel(new BorderLayout());
                panel.add(searchField, BorderLayout.CENTER);

                frame.getContentPane().add(panel);

                frame.pack();
                frame.setVisible(true);
            }
        });

        SwingUtils.invokeLaterEDT(new Runnable() {

            @Override
            public void run() {
                final StarResolverWidget searchField = fieldRef.get();
                if (searchField != null) {
                    System.out.println("Set searchField: " + searchField);

                    searchField.setText("HD 1234567890");
                }
            }
        });
    }
}
/*___oOo___*/
