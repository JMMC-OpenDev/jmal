/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: jmcsGenerateHelpsetFromHtml.java,v 1.2 2008-05-16 12:40:09 bcolucci Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2008/04/29 14:28:58  bcolucci
 * Added JavaHelp support and automatic documentation generation from HTML.
 *
 ******************************************************************************/
package fr.jmmc.mcs.gui;

import net.sourceforge.jhelpdev.JHelpDevFrame;
import net.sourceforge.jhelpdev.TOCEditorPanel;
import net.sourceforge.jhelpdev.action.CreateAllAction;
import net.sourceforge.jhelpdev.action.CreateMapAction;
import net.sourceforge.jhelpdev.action.OpenConfigAction;
import net.sourceforge.jhelpdev.settings.FileName;

import java.io.File;

import java.util.logging.*;


/** Generates a HelpSet file of a HTML folder */
public class jmcsGenerateHelpsetFromHtml
{
    /** Logger */
    private static final Logger _logger = Logger.getLogger(jmcsGenerateHelpsetFromHtml.class.getName());

    /**
     * Calls jhelpdev software on a HTML folder
     *
     * @param args arg[0] : XML project main file
     */
    public static void main(String[] args)
    {
        // Check if there is only one argument (the jhelpdev project main file)
        if (args.length != 1)
        {
            _logger.severe("No jhelpdev project main file specified ...");
            System.exit(1);
        }

        /* Create the jhelpdev project main file
           to check if is it valid */
        File documentation = new File(args[0]);

        // Does it exists?
        if (! documentation.exists())
        {
            _logger.severe(
                "The jhelpdev project main file specified doesn't exists ...");
            System.exit(1);
        } // Is it a file?
        else if (! documentation.isFile())
        {
            _logger.severe(
                "The jhelpdev project main file specified is not a file ...");
            System.exit(1);
        }

        // Name of this class
        String className = jmcsGenerateHelpsetFromHtml.class.getName();

        // Launch the jhelpdev application
        JHelpDevFrame.main(null);

        // Hide the jhelpdev frame
        JHelpDevFrame.getAJHelpDevToolFrame().setVisible(false);

        // Calls the jhelpdev action to open the project file
        System.out.println(className + " : Opening " + args[0]);
        OpenConfigAction.doIt(new FileName(args[0]));

        // Calls the jhelpdev action to create map files
        System.out.println(className + " : Creating Map...");
        CreateMapAction.doIt();

        // Calls the jhelpdev action to create toc files
        System.out.println(className + " : Creating TOC table...");
        TOCEditorPanel.getTOCTree()
                      .mergeTreeContents(CreateMapAction.getGeneratedRoot());

        // Calls the jhelpdev action to generates helpset (.hs) file
        System.out.println(className + " : Creating HelpSet...");
        CreateAllAction.doIt();

        System.exit(0);
    }
}
/*___oOo___*/
