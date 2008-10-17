/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: HelpView.java,v 1.9 2008-10-16 14:19:34 mella Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.8  2008/06/20 08:41:45  bcolucci
 * Remove unused imports and add class comments.
 *
 * Revision 1.7  2008/06/13 08:16:59  bcolucci
 * Check if a null pointer exception was launched from WindowCenterer.
 *
 * Revision 1.6  2008/06/10 08:25:06  bcolucci
 * Center the frame on the screen.
 *
 * Revision 1.5  2008/05/16 12:53:43  bcolucci
 * Removed unecessary try/catch, and added argument checks.
 *
 * Revision 1.4  2008/04/29 14:28:58  bcolucci
 * Added JavaHelp support and automatic documentation generation from HTML.
 *
 * Revision 1.3  2007/02/13 15:35:39  lafrasse
 * Jalopization.
 *
 * Revision 1.2  2007/02/13 13:48:51  lafrasse
 * Moved sources from sclgui/src/jmmc into jmcs/src/fr and rename packages
 *
 * Revision 1.1  2006/11/18 23:13:06  lafrasse
 * Creation.
 *
 *
 ******************************************************************************/
package fr.jmmc.mcs.gui;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.help.HelpBroker;
import javax.help.HelpSet;


/**
 * This class uses the JavaHelp system to show a
 * help window. The informations of the help window have
 * been taken from a file called <b>[module_name]-doc.jar</b>
 * located into the application lib folder and generated by
 * a bash script called <b>jmcsHTML2HelpSet.sh</b>.
 */
public class HelpView
{
    /** Logger */
    private static final Logger _logger = Logger.getLogger(HelpView.class.getName());

    /** internal reference to the help broker */
    private static HelpBroker _helpBroker;

    /** instance of help view */
    private static HelpView _instance = null;

    /** inited flag */
    private static boolean _alreadyInited = false;

    /** Show the help window */
    public HelpView()
    {
        _instance = this;
    }

    /**
     * Tell if help set can be used
     *
     * @return true if the help set can be used, false otherwise.
     */
    public static boolean isAvailable()
    {
        if (_instance == null)
        {
            _instance = new HelpView();
        }

        if (_alreadyInited)
        {
            return true;
        }

        try
        {
            // Get the helpset file and create the centered help broker 
            URL url = HelpSet.findHelpSet(null, "documentation.hs");
            _logger.fine("using helpset url=" + url);

            HelpSet helpSet = new HelpSet(_instance.getClass().getClassLoader(),
                    url);
            _helpBroker = helpSet.createHelpBroker();
            _helpBroker.setLocation(WindowCenterer.getCenteringPoint(
                    _helpBroker.getSize()));
        }
        catch (Exception ex)
        {
            _logger.log(Level.SEVERE, "Problem during helpset built", ex);

            return false;
        }

        _alreadyInited = true;

        return true;
    }

    /**
     * Show or hide the help view depending on the value of parameter b.
     *
     * @param b if true, shows this component; otherwise, hides this componentShow or hide help view.
     */
    public static void setVisible(boolean b)
    {
        if (isAvailable())
        {
            // Show the window
            _helpBroker.setDisplayed(b);
        }
    }
}
/*___oOo___*/
