/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: FileFilterRepository.java,v 1.2 2009-04-30 12:59:21 lafrasse Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2009/04/30 09:03:47  lafrasse
 * Creation.
 *
 ******************************************************************************/
package fr.jmmc.mcs.util;

import java.io.File;

import java.util.Hashtable;
import java.util.logging.*;

import javax.swing.filechooser.FileFilter;


/**
 * FileFilterRepository singleton class.
 */
public class FileFilterRepository
{
    /** Logger */
    private static final Logger _logger = Logger.getLogger(
            "fr.jmmc.mcs.util.FileFilterRepository");

    /** Singleton instance */
    private static FileFilterRepository _instance = null;

    /**
     * Hastable to associate string keys like
     * "application-x/scvot-file" to FileFilterRepository instances.
     */
    private Hashtable<String, FileFilter> _repository = new Hashtable();

    /** Hidden constructor */
    protected FileFilterRepository()
    {
        _logger.entering("FileFilterRepository", "FileFilterRepository");
    }

    /** Return the singleton instance */
    public static final synchronized FileFilterRepository getInstance()
    {
        _logger.entering("FileFilterRepository", "getInstance");

        // DO NOT MODIFY !!!
        if (_instance == null)
        {
            _instance = new FileFilterRepository();
        }

        return _instance;

        // DO NOT MODIFY !!!
    }

    /**
     * Register a file filter in the repository.
     *
     * @param mimeType the mime type of the file, like "application-x/scvot-file".
     * @param fileExtension the file extensions associated to the mime type.
     * @param description the humanly readable description for the mime type.
     *
     * @return the previous registered file filter, null otherwise.
     */
    public FileFilter put(String mimeType, String fileExtension,
        String description)
    {
        _logger.entering("FileFilterRepository", "put");

        _logger.finer("FileFilterRepository - put(mimeType = '" + mimeType +
            "', fileExtension = '" + fileExtension + "', description = '" +
            description + "')");

        String[] fileExtensions = new String[1];
        fileExtensions[0] = fileExtension;

        return put(mimeType, fileExtensions, description);
    }

    /**
     * Register a file filter in the repository.
     *
     * @param mimeType the mime type of the file, like "application-x/scvot-file".
     * @param fileExtensions an array of file extensions associated to the mime type.
     * @param description the humanly readable description for the mime type.
     *
     * @return the previous registered file filter, null otherwise.
     */
    public FileFilter put(String mimeType, String[] fileExtensions,
        String description)
    {
        _logger.entering("FileFilterRepository", "put[]");

        _logger.finer("FileFilterRepository - put(mimeType = '" + mimeType +
            "', fileExtensions[] = '" + fileExtensions + "', description = '" +
            description + "')");

        FileFilter filter         = new GenericFileFilter(fileExtensions,
                description);

        FileFilter previousFilter = _repository.put(mimeType, filter);

        if (previousFilter == null)
        {
            _logger.finest("Registered '" + mimeType +
                "' filter for the first time.");
        }
        else if (previousFilter != filter)
        {
            _logger.warning("Overwritten the previously registered '" +
                mimeType + "' file filter.");
        }
        else
        {
            _logger.fine("Registered '" + mimeType +
                "' mimeType associated with file extension '" + fileExtensions +
                "'  (" + description + ") succesfully.");
        }

        return previousFilter;
    }

    /**
     * Return the previously registered file filter for the given mime type.
     *
     * @param mimeType the mime type of the file filter, like "application-x/scvot-file".
     *
     * @return the retrieved registered file filter, null otherwise.
     */
    public FileFilter get(String mimeType)
    {
        _logger.entering("FileFilterRepository", "get");

        FileFilter retrievedFilter = _repository.get(mimeType);

        if (retrievedFilter == null)
        {
            _logger.severe("Cannot find '" + mimeType + "' file filter.");
        }
        else
        {
            _logger.fine("Retrieved '" + mimeType +
                "' file filter succesfully.");
        }

        return retrievedFilter;
    }

    /**
     * Return the content of the object as a String for output.
     *
     * @return the content of the object as a String for output.
     */
    public String toString()
    {
        System.out.println("FileFilterRepository::toString()");

        if (_repository == null)
        {
            return "No file filter registered yet.";
        }

        return _repository.toString();
    }
}


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
 */
class GenericFileFilter extends FileFilter
{
    /** Logger */
    private static final Logger _logger = Logger.getLogger(
            "fr.jmmc.mcs.util.GenericFileFilter");

    /** Hold each file extensions */
    private Hashtable<String, String> _fileExtensions = new Hashtable();

    /** Filter description */
    private String _description;

    /**
     * Creates a new GenericFileFilter object.
     *
     * @param fileExtensions an array of file extensions associated to the mime type.
     * @param description the humanly readable description for the mime type.
     */
    public GenericFileFilter(String[] fileExtensions, String description)
    {
        super();

        _logger.entering("GenericFileFilter", "GenericFileFilter");

        _logger.finer("GenericFileFilter(fileExtensions = '" + fileExtensions +
            "', description = '" + description + "')");

        int nbOfFileExtensions = fileExtensions.length;

        for (int i = 0; i < nbOfFileExtensions; i++)
        {
            // Add filters one by one
            String fileExtension = fileExtensions[i].toLowerCase();

            _fileExtensions.put(fileExtension, description);

            _logger.finest("GenericFileFilter(...) - Added fileExtensions[" +
                (i + 1) + "/" + nbOfFileExtensions + "] = '" + fileExtension +
                "'.");
        }

        _description = description;
    }

    /**
     * Return whether the given file is accepted by this filter, or not.
     *
     * @param f the file to test
     *
     * @return true if file is accepted, false otherwise.
     */
    public boolean accept(File currentFile)
    {
        _logger.entering("GenericFileFilter", "accept");

        if (currentFile != null)
        {
            String fileName = currentFile.getName();

            // If current file is not reguler (e.g directory, links, ...)
            if (currentFile.isFile() == false)
            {
                _logger.finest("Accepting non-regular file '" + fileName +
                    "'.");

                return true; // Accept it to ensure navigation through directory and so
            }

            // If the file has no extension
            String fileExtension = getExtension(currentFile);

            if (fileExtension == null)
            {
                return false; // Discard it
            }

            // If corresponding mime-type is handled
            String fileType = _fileExtensions.get(fileExtension);

            if (fileType != null)
            {
                _logger.finer("Accepting file '" + fileName + "' of type '" +
                    fileType + "'.");

                return true; // Accept it
            }
        }

        return false;
    }

    /**
     * Return the description of this filter.
     *
     * @return the description of this filter.
     */
    public String getDescription()
    {
        _logger.entering("GenericFileFilter", "getDescription");

        return _description;
    }

    /**
     * Return the extension portion of the file's name .
     *
     * @return the file extension if any, null otherwise.
     */
    public static String getExtension(File file)
    {
        _logger.entering("GenericFileFilter", "getExtension");

        String fileExtension = null;

        if (file != null)
        {
            String fileName       = file.getName();
            int    indexOfLastDot = fileName.lastIndexOf('.');

            if ((indexOfLastDot > 0) &&
                    (indexOfLastDot < (fileName.length() - 1)))
            {
                fileExtension = fileName.substring(indexOfLastDot + 1)
                                        .toLowerCase();
            }

            _logger.finest("Extension of file '" + fileName + "' is '" +
                fileExtension + "'.");
        }

        return fileExtension;
    }

    /**
     * Return the content of the object as a String for output.
     *
     * @return the content of the object as a String for output.
     */
    public String toString()
    {
        _logger.entering("GenericFileFilter", "toString");

        String fileExtensions = "NONE";

        if (_fileExtensions != null)
        {
            fileExtensions = _fileExtensions.toString();
        }

        return "File extensions registered for '" + _description + "' : " +
        fileExtensions;
    }
}
/*___oOo___*/
