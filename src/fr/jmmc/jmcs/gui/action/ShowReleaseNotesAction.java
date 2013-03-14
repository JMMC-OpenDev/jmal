/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmcs.gui.action;

import fr.jmmc.jmcs.data.app.ApplicationDescription;
import fr.jmmc.jmcs.data.app.model.Change;
import fr.jmmc.jmcs.data.app.model.Prerelease;
import fr.jmmc.jmcs.data.app.model.Release;
import fr.jmmc.jmcs.gui.component.ResizableTextViewFactory;
import fr.jmmc.jmcs.util.StringUtils;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action generates release notes for the given ApplicationDescription.
 * @author Laurent BOURGES, Sylvain LAFRASSE.
 */
public final class ShowReleaseNotesAction extends RegisteredAction {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Class name. This name is used to register to the ActionRegistrar */
    public final static String _className = ShowReleaseNotesAction.class.getName();
    /** Class logger */
    private final static Logger _logger = LoggerFactory.getLogger(_className);
    // Members
    /** Description to extract release notes from */
    private final ApplicationDescription _applicationDescription;
    /** Title */
    private String _windowTitle = null;
    /** HTML content (cached) */
    private String _windowContent = null;

    /**
     * Constructor that use the default ApplicationDescription instance and generate title automatically.
     * @param actionName the name of the action.
     */
    public ShowReleaseNotesAction(final String actionName) {
        super(_className, actionName, "Release Notes");
        _applicationDescription = ApplicationDescription.getInstance();
    }

    /**
     * Constructor that automatically register the action in RegisteredAction.
     * 
     * @param actionName the name of the action.
     * @param titlePrefix title prefix to use in window title and HTML content
     * @param applicationDescription application description to use
     */
    public ShowReleaseNotesAction(final String actionName, final String titlePrefix, final ApplicationDescription applicationDescription) {
        super(_className, actionName);
        _windowTitle = titlePrefix;
        _applicationDescription = applicationDescription;
    }

    /**
     * Handle the action event
     * @param evt action event
     */
    @Override
    public void actionPerformed(final ActionEvent evt) {
        _logger.debug("actionPerformed");

        // Lazily compute content only once
        if (_windowContent == null) {
            _windowContent = generateHtml();
        }

        ResizableTextViewFactory.createHtmlWindow(_windowContent, _windowTitle, false);
    }

    /** 
     * Generate HTML content
     * @return HTML content
     */
    private String generateHtml() {

        // Compute title (if none)
        if (_windowTitle == null) {
            _windowTitle = _applicationDescription.getProgramNameWithVersion();
        }
        _windowTitle += " Release Notes";

        // Compose standard header
        final StringBuilder generatedHtml = new StringBuilder(8 * 1024);
        generatedHtml.append("<html><body>");
        generatedHtml.append("<h1><center><b>").append(_windowTitle).append("</b></center></h1>\n");

        // Extracted changes per type:
        final List<Change> changeList = new ArrayList<Change>(20);
        for (Release r : _applicationDescription.getReleases()) {

            generatedHtml.append("<hr>").append("<h3>").append("Version ").append(r.getVersion());
            String pubDate = r.getPubDate();
            if (pubDate == null) {
                pubDate = "no publication date yet";
            }
            generatedHtml.append(" (<i>").append(pubDate).append("</i>)</h3>\n");

            processChangeType("FEATURE", "Features", r.getPrereleases(), generatedHtml, changeList);
            processChangeType("CHANGE", "Changes", r.getPrereleases(), generatedHtml, changeList);
            processChangeType(null, null, r.getPrereleases(), generatedHtml, changeList); // empty type considered as 'Change'
            processChangeType("BUGFIX", "Bug Fixes", r.getPrereleases(), generatedHtml, changeList);
        }

        generatedHtml.append("</body></html>");

        return generatedHtml.toString();
    }

    /**
     * Generate HTML for the given change type.
     * @see #findChangeByType(java.lang.String, java.util.List, java.util.List) 
     * @param type type to match or null (matches empty type)
     * @param label label to display for the given type
     * @param prereleaseList list of prerelease 
     * @param generatedHtml HTML buffer to fill
     * @param changeList temporary list of Change to fill
     */
    private void processChangeType(final String type, final String label, final List<Prerelease> prereleaseList, final StringBuilder generatedHtml, final List<Change> changeList) {
        if (findChangeByType(type, prereleaseList, changeList)) {
            if (label != null) {
                generatedHtml.append(label).append(":\n");
            }
            generatedHtml.append("<ul>\n");

            for (Change c : changeList) {
                generatedHtml.append("<li>").append(c.getValue()).append("</li>\n");
            }
            generatedHtml.append("</ul>\n");
        }
    }

    /**
     * Extract Change instances according to their type.
     * @param type type to match or null (matches empty type)
     * @param prereleaseList list of prerelease 
     * @param changeList list of Change to fill
     * @return true if Change instances found for the given type, false otherwise.
     */
    private boolean findChangeByType(final String type, final List<Prerelease> prereleaseList, final List<Change> changeList) {
        changeList.clear();

        final boolean noType = StringUtils.isEmpty(type);

        for (Prerelease p : prereleaseList) {
            for (Change c : p.getChanges()) {
                if (noType) {
                    if (StringUtils.isEmpty(c.getType())) {
                        changeList.add(c);
                    }
                } else {
                    if (type.equalsIgnoreCase(c.getType())) {
                        changeList.add(c);
                    }
                }
            }
        }
        return !changeList.isEmpty();
    }
}
