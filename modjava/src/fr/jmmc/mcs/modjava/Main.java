/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: Main.java,v 1.10 2011-02-11 15:11:43 lafrasse Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.9  2010/10/11 14:27:31  lafrasse
 * API update and cleanup.
 *
 * Revision 1.8  2010/10/05 07:57:21  lafrasse
 * To reflect fr.jmmc.mcs.interop API changes.
 *
 * Revision 1.7  2010/09/17 09:45:28  lafrasse
 * Added SampMessageHandler usage.
 *
 * Revision 1.6  2010/09/13 15:57:29  lafrasse
 * First SAMP manager implementation.
 *
 * Revision 1.5  2010/09/02 09:34:03  mella
 * Add preference instantiation into init because the menu building need some internal actions of Preferences object
 * Add button to test the dismissable message pane
 *
 * Revision 1.4  2008/10/27 15:26:13  lafrasse
 * REflected JMCS help view action API change.
 *
 * Revision 1.3  2008/10/15 12:02:53  mella
 * rename finnish method into finish
 *
 * Revision 1.2  2008/09/22 16:53:50  lafrasse
 * Moved to new JMCS APIs.
 *
 * Revision 1.1  2008/07/01 08:58:13  lafrasse
 * Added jmcs test application from bcolucci.
 *
 ******************************************************************************/
package fr.jmmc.mcs.modjava;

import fr.jmmc.mcs.gui.App;
import fr.jmmc.mcs.gui.DismissableMessagePane;
import fr.jmmc.mcs.interop.SampCapability;
import fr.jmmc.mcs.interop.SampMessageHandler;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;

/**
 * Class for tests
 *
 * @author $author$
 * @version $Revision: 1.10 $
 */
public class Main extends App {

    /** Logger */
    private static final Logger _logger = Logger.getLogger(Main.class.getName());
    /** Button to launch about box window */
    private JButton _aboutBoxButton = null;
    /** Button to launch feedback report window */
    private JButton _feedbackReportButton = null;
    /** Button to launch helpview window */
    private JButton _helpViewButton = null;
    /** Actions class */
    Actions _actions = null;
    /** Test button */
    private JButton _testDismissableMessagePane = null;

    /** Constructor
     * @param args
     */
    public Main(String[] args) {
        super(args, false, true);
    }

    /** Initialize application objects */
    @Override
    protected void init(String[] args) {
        _logger.warning("Initialize application objects");
        _logger.info(args.length + " arguments have been taken");

        _actions = new Actions();

        // .. buttons
        _aboutBoxButton = new JButton(aboutBoxAction());
        _feedbackReportButton = new JButton(feedbackReportAction());
        _helpViewButton = new JButton(openHelpFrame());
        _testDismissableMessagePane = new JButton(dismissableMessagePaneAction());

        // Set borderlayout
        getFramePanel().setLayout(new BorderLayout());

        // Add buttons to panel
        getFramePanel().add(_aboutBoxButton, BorderLayout.NORTH);
        getFramePanel().add(_feedbackReportButton, BorderLayout.CENTER);
        getFramePanel().add(_testDismissableMessagePane, BorderLayout.SOUTH);

        // Set others preferences
        try {
            Preferences.getInstance().setPreference("MAIN", "maim");
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "Failed setting preference.", ex);
        }
    }

    /** Execute application body */
    @Override
    protected void execute() {
        _logger.info("Execute application body");

        // Show the frame
        getFrame().setVisible(true);

        try {
            SampMessageHandler handler = new SampMessageHandler("stuff.do") {

                public void processMessage(String senderId, Message msg) {
                    // do stuff
                    System.out.println("\tReceived 'stuff.do' message from '" + senderId + "' : '" + msg + "'.");

                    String name = (String) msg.getParam("name");
                    Map result = new HashMap();
                    result.put("name", name);
                    result.put("x", SampUtils.encodeFloat(3.141159));
                    return;
                }
            };
            handler = new SampMessageHandler(SampCapability.LOAD_VO_TABLE) {

                public void processMessage(String senderId, Message msg) {
                    // do stuff
                    System.out.println("\tReceived 'LOAD_VO_TABLE' message from '" + senderId + "' : '" + msg + "'.");
                    return;
                }
            };
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Execute operations before closing application */
    @Override
    protected boolean finish() {
        _logger.warning("Execute operations before closing application");

        // Quit application
        return true;
    }

    /** Open help view action
     * @return
     */
    private Action openHelpFrame() {
        return new AbstractAction("Open Help Frame") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                // Instantiate help JFrame
                JFrame helpFrame = new JFrame("- Help frame -");

                // Add buttons to panel
                helpFrame.getContentPane().add(new JButton(showHelpAction()),
                        BorderLayout.NORTH);
                helpFrame.getContentPane().add(new JButton("CENTER"), BorderLayout.CENTER);
                helpFrame.getContentPane().add(new JButton("WEST"), BorderLayout.WEST);
                helpFrame.getContentPane().add(new JButton("SOUTH"), BorderLayout.SOUTH);

                // Set the frame properties
                helpFrame.pack();
                helpFrame.setLocationRelativeTo(null);
                helpFrame.setVisible(true);
            }
        };
    }

    /** Open help view action
     * @return
     */
    private Action dismissableMessagePaneAction() {
        return new AbstractAction("Show dismissable message pane") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                DismissableMessagePane.show(getFramePanel(),
                        "Try to show a test message\n which can be deactivated by user!!",
                        Preferences.getInstance(), "msgTest");
            }
        };
    }

    /**
     * Main
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new Main(args);
    }
}
/*___oOo___*/