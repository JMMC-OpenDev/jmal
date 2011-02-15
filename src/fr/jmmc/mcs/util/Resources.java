/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: Resources.java,v 1.15 2010-01-14 13:03:04 bourgesl Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.14  2008/11/06 13:45:37  mella
 * fix logging levels
 *
 * Revision 1.13  2008/10/16 13:16:11  mella
 * Use dedicated warn level param to resources retrieval method
 *
 * Revision 1.12  2008/10/16 09:18:22  lafrasse
 * Jalopization.
 *
 * Revision 1.11  2008/10/15 13:44:01  mella
 * housekeeping
 *
 * Revision 1.10  2008/02/13 05:30:12  mella
 * set right logger name and adjust log level
 *
 * Revision 1.9  2007/06/19 15:18:10  lafrasse
 * Added Mac OS X command key handling for menu short cuts.
 *
 * Revision 1.8  2007/02/15 08:28:46  mella
 * Replace MCSLogger by java.util.logging.Logger
 *
 * Revision 1.7  2007/02/14 10:14:22  mella
 * change jmmc into fr/jmmc
 *
 * Revision 1.6  2007/02/13 13:48:51  lafrasse
 * Moved sources from sclgui/src/jmmc into jmcs/src/fr and rename packages
 *
 * Revision 1.5  2006/11/20 15:41:23  lafrasse
 * Added error handling code.
 *
 * Revision 1.4  2006/11/18 22:58:03  lafrasse
 * Added support for Key Accelerator (keyboard shortcut).
 *
 * Revision 1.3  2006/10/16 14:29:49  lafrasse
 * Updated to reflect MCSLogger API changes.
 *
 * Revision 1.2  2006/08/03 14:47:24  lafrasse
 * Jalopyzation
 *
 * Revision 1.1  2006/07/28 06:36:11  mella
 * First revision
 *
 *
 ******************************************************************************/
package fr.jmmc.mcs.util;

import java.net.URL;

import java.util.*;
import java.util.logging.Level;

import javax.swing.*;


/**
 * Class used to get resources informations from one central point (xml file).
 * Applications must start to set the resource file name before
 * any gui construction.
 */
public abstract class Resources
{
    /** the logger facility */
    protected static java.util.logging.Logger logger_ = java.util.logging.Logger.getLogger(
            "fr.jmmc.mcs.util.Resources");

    /** Contains the class nale for logging */
    static String _loggerClassName = "Resources";

    /** resource filename  that must be overloaded by subclasses */
    protected static String _resourceName = "fr/jmmc/mcs/util/Resources";

    /** Properties */
    private static ResourceBundle _resources = null;

    /** Store whether the execution platform is a Mac or not */
    public static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase()
                                            .startsWith("mac os x"));

    /**
     * Indicates the property file where informations will be exctracted.
     * The property file must end with .properties filename extension. But the
     * given name should omit the extension.
     *
     * @param name Indicates property file to use.
     */
    public static void setResourceName(String name)
    {
        logger_.entering(_loggerClassName, "setResourceName");

        if (logger_.isLoggable(Level.FINE)) {
          logger_.fine("Application will grab resources from '" + name + "'");
        }
        _resourceName = name;
    }

    /**
     * Get content from resource file.
     *
     * @param resourceName name of resource
     *
     * @return the content of the resource or null indicating error
     */
    public static String getResource(String resourceName)
    {
        return getResource(resourceName, Level.WARNING);
    }

    /**
     * Get content from resource file.
     *
     * @param resourceName name of resource
     * @param notFoundLogLevel level to use if resource is not found
     *
     * @return the content of the resource or null indicating error
     */
    public static String getResource(String resourceName, Level notFoundLogLevel)
    {
        logger_.entering(_loggerClassName, "getResource");

        if (_resources == null)
        {
            try
            {
                _resources = java.util.ResourceBundle.getBundle(_resourceName);
            }
            catch (Exception e)
            {
              if (logger_.isLoggable(notFoundLogLevel)) {
                  logger_.log(notFoundLogLevel,
                    "Resource bundle can't be found :" + e.getMessage());
              }

                return null;
            }
        }

        if (logger_.isLoggable(Level.FINE)) {
          logger_.fine("getResource for " + resourceName);
        }

        try
        {
            return _resources.getString(resourceName);
        }
        catch (Exception e)
        {
            logger_.log(notFoundLogLevel, "Entry not found :" + e.getMessage());
        }

        return null;
    }

    /**
     * Get the text of an action.
     *
     * @param actionName the actionInstanceName
     *
     * @return the associated text
     */
    public static String getActionText(String actionName)
    {
        logger_.entering(_loggerClassName, "getActionText");

        return getResource("actions.action." + actionName + ".text", Level.FINE);
    }

    /**
     * Get the description of an action.
     *
     * @param actionName the actionInstanceName
     *
     * @return the associated description
     */
    public static String getActionDescription(String actionName)
    {
        logger_.entering(_loggerClassName, "getActionDescription");

        return getResource("actions.action." + actionName + ".description",
            Level.FINE);
    }

    /**
     * Get the tooltip text of widget related to the common widget group.
     *
     * @param widgetName the widgetInstanceName
     *
     * @return the tooltip text
     */
    public static String getToolTipText(String widgetName)
    {
        logger_.entering(_loggerClassName, "getToolTipText");

        return getResource("widgets.widget." + widgetName + ".tooltip",
            Level.FINE);
    }

    /**
     * Get the accelerator (aka. keyboard short cut) of an action .
     *
     * @param actionName the actionInstanceName
     *
     * @return the associated accelerator
     */
    public static KeyStroke getActionAccelerator(String actionName)
    {
        logger_.entering(_loggerClassName, "getActionAccelerator");

        // Get the accelerator string description from the Resource.properties file
        String keyString = getResource("actions.action." + actionName +
                ".accelerator", Level.FINE);

        if (keyString == null)
        {
            return null;
        }

        // If the execution is on Mac OS X
        if (MAC_OS_X == true)
        {
            // The 'command' key (aka Apple key) is used
            keyString = "meta " + keyString;
        }
        else
        {
            // The 'control' key ise used elsewhere
            keyString = "ctrl " + keyString;
        }

        // Get and return the KeyStroke from the accelerator string description
        KeyStroke accelerator = KeyStroke.getKeyStroke(keyString);

        if (logger_.isLoggable(Level.FINE)) {
          logger_.fine("keyString['" + actionName + "'] = '" + keyString +
            "' -> accelerator = '" + accelerator + "'.");
        }

        return accelerator;
    }

    /**
     * Get the icon of an action .
     *
     * @param actionName the actionInstanceName
     *
     * @return the associated icon
     */
    public static ImageIcon getActionIcon(String actionName)
    {
        logger_.entering(_loggerClassName, "getActionIcon");

        // Get back the icon image path
        String iconPath = getResource("actions.action." + actionName + ".icon",
                Level.FINE);

        if (iconPath == null)
        {
            if (logger_.isLoggable(Level.FINE)) {
              logger_.fine("No icon resource found for action name '" +
                actionName + "'.");
            }

            return null;
        }

        // Get the image from path
        URL imgURL = Resources.class.getResource(iconPath);

        if (imgURL == null)
        {
            if (logger_.isLoggable(Level.FINE)) {
              logger_.fine("Could not load icon '" + iconPath + "'.");
            }

            return null;
        }

        if (logger_.isLoggable(Level.FINE)) {
          logger_.fine("Using imgUrl for icon resource  '" + imgURL);
        }

        return new ImageIcon(imgURL);
    }
}
/*___oOo___*/