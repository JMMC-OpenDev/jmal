/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmcs.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.filechooser.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A convenience implementation of FileFilter that filters out all files except
 * for those type extensions that it knows about.
 *
 * Extensions are of the type "foo", which is typically found on Windows and
 * Unix boxes, but not on older Macintosh which use ResourceForks (case ignored).
 *
 * Example - create a new filter that filters out all files but gif and jpg files:
 *     GenericFileFilter filter = new GenericFileFilter(
 *                   new String{"gif", "jpg"}, "JPEG & GIF Images")
 *
 * Strongly inspired of ExampleFileFilter class from FileChooserDemo under the
 * demo/jfc directory in the JDK.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES, Guillaume MELLA.
 */
public final class GenericFileFilter extends FileFilter implements FilenameFilter {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(GenericFileFilter.class.getName());
    /** Hold each file extensions */
    private final HashMap<String, String> _fileExtensions = new HashMap<String, String>(4);
    /** Filter description */
    private final String _description;
    /** flag to indicate that one extension contains '.' char */
    private final boolean _extWithDot;

    /**
     * Creates a new GenericFileFilter object.
     *
     * @param fileExtensions an array of file extensions associated to the mime type.
     * @param description the humanly readable description for the mime type.
     */
    GenericFileFilter(final String[] fileExtensions, final String description) {
        super();

        if (_logger.isDebugEnabled()) {
            _logger.debug("GenericFileFilter(fileExtensions = '{}', description = '{}')",
                    Arrays.toString(fileExtensions), description);
        }

        final int nbOfFileExtensions = fileExtensions.length;

        boolean hasDot = false;

        for (int i = 0; i < nbOfFileExtensions; i++) {
            // Add filters one by one
            final String fileExtension = fileExtensions[i].toLowerCase();

            hasDot |= fileExtension.contains(".");

            _fileExtensions.put(fileExtension, description);

            if (_logger.isTraceEnabled()) {
                _logger.trace("GenericFileFilter(...) - Added fileExtensions[{}]/{}] = '{}'.",
                        (i + 1), nbOfFileExtensions, fileExtension);
            }
        }

        _extWithDot = hasDot;
        _description = description;
    }

    /**
     * Return whether the given file is accepted by this filter, or not.
     *
     * @param currentFile the file to test
     *
     * @return true if file is accepted, false otherwise.
     */
    @Override
    public boolean accept(final File currentFile) {
        if (currentFile != null) {
            final String fileName = currentFile.getName();

            // If current file is not regular (e.g directory, links, ...)
            if (!currentFile.isFile()) {
                _logger.trace("Accepting non-regular file '{}'.", fileName);

                // Accept it to ensure navigation through directory and so
                return true; 
            }

            // If the file has no extension
            final String fileExtension = FileUtils.getExtension(fileName);

            if (fileExtension == null) {
                return false; // Discard it
            }

            // If corresponding mime-type is handled
            String fileType = _fileExtensions.get(fileExtension);

            if (fileType != null) {
                _logger.debug("Accepting file '{}' of type '{}'.", fileName, fileType);

                return true; // Accept it
            }

            if (_extWithDot) {
                // retry with extension with dot:
                final String fileExtWithDot = FileUtils.getExtension(fileName, 2);

                if (fileExtWithDot == null) {
                    return false; // Discard it
                }

                // If corresponding mime-type is handled
                fileType = _fileExtensions.get(fileExtWithDot);

                if (fileType != null) {
                    _logger.debug("Accepting file '{}' of type '{}'.", fileName, fileType);

                    return true; // Accept it
                }
            }
        }

        return false;
    }

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param   dir    the directory in which the file was found.
     * @param   name   the name of the file.
     * @return  <code>true</code> if and only if the name should be
     * included in the file list; <code>false</code> otherwise.
     */
    @Override
    public boolean accept(final File dir, final String name) {
        return accept(new File(dir, name));
    }

    /**
     * Return the description of this filter.
     *
     * @return the description of this filter.
     */
    @Override
    public String getDescription() {
        return _description;
    }

    /**
     * Return the content of the object as a String for output.
     *
     * @return the content of the object as a String for output.
     */
    @Override
    public String toString() {
        final String fileExtensions;

        if (_fileExtensions != null) {
            fileExtensions = _fileExtensions.toString();
        } else {
            fileExtensions = "NONE";
        }

        return "File extensions registered for '" + _description + "' : " + fileExtensions;
    }
}