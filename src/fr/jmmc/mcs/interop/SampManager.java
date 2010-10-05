/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: SampManager.java,v 1.7 2010-10-05 12:02:39 bourgesl Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.6  2010/10/05 10:17:56  bourgesl
 * fixed warnings / javadoc
 * fixed exception handling / logs
 * fixed member visibility
 *
 * Revision 1.5  2010/10/04 23:37:32  lafrasse
 * Removed unused imports.
 *
 * Revision 1.4  2010/10/04 23:35:44  lafrasse
 * Added "Interop" menu handling.
 *
 * Revision 1.3  2010/09/24 12:07:37  lafrasse
 * Added preliminary support for message sending and broadcasting, plus SampCapability management.
 *
 * Revision 1.2  2010/09/14 14:31:42  lafrasse
 * Added TODOs
 *
 * Revision 1.1  2010/09/13 15:57:18  lafrasse
 * First SAMP manager implementation.
 *
 ******************************************************************************/
package fr.jmmc.mcs.interop;

import fr.jmmc.mcs.gui.App;
import fr.jmmc.mcs.gui.ApplicationDataModel;
import fr.jmmc.mcs.gui.StatusBar;
import java.util.Collections;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.DefaultClientProfile;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.xmlrpc.HubMode;

/**
 * SampManager singleton class.
 *
 * @author lafrasse
 */
public class SampManager {

    /** Logger */
    private static final Logger _logger = Logger.getLogger("fr.jmmc.mcs.interop.SampManager");

    /** Singleton instance */
    private static volatile SampManager _instance = null;

    /** Singleton instance : TODO : encapsulate this inside SampManager  */
    private static GuiHubConnector _connector = null;

    /** Hook to the "Interop" menu */
    private static JMenu _menu = null;

    /** JMenu to Action relations */
    private static final Map<SampCapabilityAction,JMenu> _map = Collections.synchronizedMap(new HashMap<SampCapabilityAction, JMenu>());

    /**
     * Return the singleton instance
     * @return singleton instance
     * @throws SampException if any Samp exception occured
     */
    public static final synchronized SampManager getInstance() throws SampException {
        // DO NOT MODIFY !!!
        if (_instance == null) {
            _instance = new SampManager();
        }

        return _instance;

        // DO NOT MODIFY !!!
    }

    /** 
     * Hidden constructor
     * @throws SampException if any Samp exception occured
     */
    protected SampManager() throws SampException {

        // @TODO : init JSamp env.
        final ClientProfile profile = DefaultClientProfile.getProfile();

        _connector = new GuiHubConnector(profile);

        // Try to start an external SAMP hub if none available

        // TODO : use an internal hub for JNLP issues :
        final boolean external = true;

        final Action act = _connector.createHubAction(external, HubMode.CLIENT_GUI);
        act.actionPerformed(null);

        if (!_connector.isConnected()) {
            StatusBar.show("Could not connect to nor start a SAMP hub.");
        }

        // Build application metadata
        final Metadata meta = new Metadata();

        final ApplicationDataModel applicationDataModel = App.getSharedApplicationDataModel();
        // @TODO : create some JmcsException !!!

        final String applicationName = applicationDataModel.getProgramName();
        meta.setName(applicationName);

        final String applicationURL = applicationDataModel.getMainWebPageURL();
        meta.setDescriptionText("More info at " + applicationURL);

        /* @TODO : Add App metadata (cf. Aladin : icon, url, author, ... )
        String iconURL = applicationDataModel.getLogoURL();
        meta.setIconUrl(iconURL);
         */

        _connector.declareMetadata(meta);
        _connector.addConnectionListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (_logger.isLoggable(Level.INFO)) {
                    _logger.info("SAMP Hub connection status changed: " + e);
                }
                // @TODO : Refresh menu to populate it according to connection
            }
        });
    }

    /**
     * Return the JSamp Gui hub connector providing swing actions
     * @return JSamp Gui hub connector providing swing actions
     * @throws SampException if any Samp exception occured
     */
    public static GuiHubConnector getGuiHubConnector() throws SampException {
        SampManager.getInstance();

        return _connector;
    }

    /**
     * Register an app-specific capability
     * @param handler message handler
     * @throws SampException if any Samp exception occured
     */
    public static void registerCapability(final SampMessageHandler handler) throws SampException {
        SampManager.getInstance();

        _connector.addMessageHandler(handler);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Registered SAMP capability for mType '" + handler.handledMType() + "'.");
        }

        // This step required even if no custom message handlers added.
        _connector.declareSubscriptions(_connector.computeSubscriptions());

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(10);
    }

    /**
     * Link SampManager instance to the "Interop" menu
     * @param menu interop menu container
     */
    public static void hookMenu(final JMenu menu) {

        if (_menu != null) {
          throw new IllegalStateException("the interoperability menu is already hooked by SampManager : \n" + _menu +"\n" + menu);
        }

        _menu = menu;

        // If some capabilities are registered
        if (!_map.isEmpty()) {
            // Make the "Interop" menu visible
            _menu.setVisible(true);
        }
    }

    /**
     * Link a menu entry to its action
     * @param menu menu entry
     * @param action samp capability action
     */
    public static void addMenu(final JMenu menu, final SampCapabilityAction action) {
        _map.put(action, menu);
    }

    /** 
     * Get a menu entry from its action
     * @param action samp capability action
     * @return menu menu entry
     */
    public static JMenu getMenu(final SampCapabilityAction action) {
        return _map.get(action);
    }

    /** 
     * Send the given message to a client
     * @param mType samp message type
     * @param recipient public-id of client to receive message
     * @param parameters message parameters
     *
     * @throws SampException if any Samp exception occured
     */
    public static void sendMessageTo(final String mType, final String recipient, final Map<?,?> parameters) throws SampException {
        SampManager.getInstance();

        final Message msg = new Message(mType, parameters);
        _connector.getConnection().notify(recipient, msg);

        if (_logger.isLoggable(Level.INFO)) {
            _logger.info("Sent '" + mType + "' SAMP message to '" + recipient + "' client.");
        }
    }

    /**
     * Send the given message to all clients supporting the given message type
     * @param mType samp message type
     * @param parameters message parameters
     * 
     * @throws SampException if any Samp exception occured
     */
    public static void broadcastMessage(final String mType, final Map<?,?> parameters) throws SampException {
        SampManager.getInstance();

        final Message msg = new Message(mType, parameters);
        _connector.getConnection().notifyAll(msg);

        if (_logger.isLoggable(Level.INFO)) {
            _logger.info("Broadcasted SAMP message to '" + mType + "' capable clients.");
        }
    }
}
