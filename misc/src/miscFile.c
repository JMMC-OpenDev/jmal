/*******************************************************************************
 * JMMC project
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.37  2007/03/15 15:57:12  lafrasse
 * Added handling of the '.wsdl' file type.
 *
 * Revision 1.36  2006/01/10 14:40:39  mella
 * Changed rcsId declaration to perform good gcc4 and gcc3 compilation
 *
 * Revision 1.35  2005/10/10 12:00:11  lafrasse
 * Added miscLocateDir()
 *
 * Revision 1.34  2005/10/06 15:12:46  lafrasse
 * Added miscGetEnvVarIntValue function
 *
 * Revision 1.33  2005/10/06 13:08:44  lafrasse
 * Corrected some orthography mistakes in the documentation
 *
 * Revision 1.32  2005/05/26 16:05:11  lafrasse
 * Corrected a bug re-introduced during code review in miscResolvePath(), that was causing data loss upon call of the function on the result of another call of the same function
 *
 * Revision 1.31  2005/05/20 16:22:50  lafrasse
 * Code review : refined user and developper documentation, functions reordering, and rationnalized miscYankExtension()
 *
 * Revision 1.30  2005/04/06 09:31:50  gluck
 * Code review: minor changes
 *
 * Revision 1.29  2005/03/08 07:17:04  gzins
 * Released dynamic buffer used in miscResolvePath()
 * Added input parameter check in miscResolvePath()
 *
 * Revision 1.28  2005/02/17 14:32:42  gzins
 * Improved intialisation of static dynamic buffers
 *
 * Revision 1.27  2005/02/12 14:46:09  gzins
 * Updated miscLocateFile function:
 *  - added test of file existence before looking for in path list.
 *  - returned a resolved path
 *
 * Revision 1.26  2005/02/09 06:27:35  gzins
 * Fixed minor bug in miscFileExists; some errors was added even if addError was set to false
 *
 * Revision 1.25  2005/02/07 14:41:47  lafrasse
 * Changed miscLocateFileInPath() error management behavior in order to report only one error (and not one for each directory) if the given file was not found in all the path directories
 *
 * Revision 1.24  2005/01/28 18:39:10  gzins
 * Changed FAILURE/SUCCESS to mcsFAILURE/mscSUCCESS
 *
 * gzins     10-Dec-2004  Fixed pathSearchList for .cdf files
 * mella     10-Nov-2004  Added xsd and xsl into pathSearchList
 * lafrasse  07-Oct-2004  Changed miscFileExists API
 * lafrasse  01-Oct-2004  Changed miscResolvePath API for consistency
 * lafrasse  30-Sep-2004  Added miscLocateFile
 * lafrasse  27-Sep-2004  Added miscLocateFileInPath, corrected a bug in the
 *                        miscResolvePath use of misFileExists, and refined the
 *                        doxygen documentation
 * lafrasse  25-Sep-2004  Added miscFileExists
 * lafrasse  23-Aug-2004  Changed miscGetEnvVarValue API
 * lafrasse  03-Aug-2004  Corrected a bug in miscResolvePath that was causing an
 *                        '\' append at the end of the computed path
 * lafrasse  02-Aug-2004  Changed includes to isolate miscFile headers from
 *                        misc.h
 *                        Moved mcs.h include to miscFile.h
 *                        Changed includes due to null-terminated string
 *                        specific functions move from miscDynStr.h to
 *                        miscDynBuf.h
 * lafrasse  23-Jul-2004  Added error management code optimization
 * lafrasse  20-Jul-2004  Added miscResolvePath, miscGetEnvVarValue, and
 *                        miscYankLastPath
 * lafrasse  18-Jun-2004  Debugged miscGetExtension
 *                        Added miscYankExtension
 * lafrasse  17-Jun-2004  Added miscGetExtension
 * gzins     16-Jun-2004  Created from VLT file 'slxUtils.c'
 *
 *----------------------------------------------------------------------------*/

/**
 * @file
 * Function collection related to file path and environment variable.
 *
 * In the following documentation, the expression 'simple path' describes a
 * Unix-like single path (e.g. "$HOME/Dev/misc/src/../doc/index.html").
 *
 * In the following documentation, the expression 'composed path' describes a
 * list of Unix-like path each separated by a colon (':') (e.g.
 * "$MCSROOT/lib:$INTROOT/bin:$HOME/Dev/misc/src/../doc/").
 */

static char *rcsId __attribute__ ((unused)) = "@(#) $Id: miscFile.c,v 1.38 2010-01-15 14:18:44 lafrasse Exp $"; 



/* 
 * System Headers
 */
#include <stdio.h>
#include <unistd.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>


/* 
 * MCS Headers
 */
#include "err.h"


/* 
 * Local Headers
 */
#include "miscFile.h"
#include "miscPrivate.h"
#include "miscErrors.h"
#include "miscDynBuf.h"


/* 
 * Constants definition
 */

/** Index defining the @em extension column in the @em pathSearchList array */
#define miscEXT_IDX   0

/** Index defining the @em path column in the @em pathSearchList array */
#define miscPATH_IDX  1

/** Standard MCS Search Path */
#define MCS_STANDARD_SERACH_PATH "../:$INTROOT/:$MCSROOT/"


/* 
 * Local Variables
 */

/**
 * Global array which associate a search path to a file extension.
 */
static char *pathSearchList[][2] = {
   {"cfg", "../config:$INTROOT/config:$MCSROOT/config"},
   {"cdf", "../config:$INTROOT/config:$MCSROOT/config"},
   {"xsd", "../config:$INTROOT/config:$MCSROOT/config"},
   {"xsl", "../config:$INTROOT/config:$MCSROOT/config"},
   {"xml", "../errors:$INTROOT/errors:$MCSROOT/errors"},
   {"wsdl", "../include:$INTROOT/include:$MCSROOT/include"},
   {NULL, NULL }
};


/*
 * Public functions definition
 */

/**
 * Give back the value of a specified shell environment variable (e.g. "$HOME").
 *
 * @param envVarName a null-terminated string containing the searched
 * environment variable name (with or without the leading '$').
 * @param envVarValueBuffer an already allocated buffer to receive the
 * environment variable value.
 * @param envVarValueBufferLength the length of the already allocated buffer.
 *
 * @return mcsSUCCESS on successful completion. Otherwise mcsFAILURE is
 * returned.
 */
mcsCOMPL_STAT miscGetEnvVarValue (const char      *envVarName,
                                        char      *envVarValueBuffer,
                                        mcsUINT32  envVarValueBufferLength)
{
    char *envVarValue = NULL;

    /* Return if the given env. var. name is null */
    if (envVarName == NULL)
    {
        errAdd(miscERR_NULL_PARAM, "envVarName");
        return mcsFAILURE;
    }

    /* If the given env. var. begins with a '$'... */
    if (*envVarName == '$')
    {
        /* Skip this leading '$' */
        envVarName++;
    }

    /* Get the value associated with the given env. var. */
    envVarValue = getenv(envVarName);
    if (envVarValue == NULL)
    {
        errAdd(miscERR_FILE_ENV_VAR_NOT_DEF, envVarName);
        return mcsFAILURE;
    }

    /* If the value is longer that the available extern memory... */
    if (strlen(envVarValue) >= envVarValueBufferLength)
    {
        /* Raise an error and exit */
        errAdd(miscERR_FILE_ENV_VAR_TOO_LONG, envVarName);
        return mcsFAILURE;
    }

    /* Give back the env. var. value */
    strncpy(envVarValueBuffer, envVarValue, envVarValueBufferLength);
    return mcsSUCCESS;
}


/**
 * Give back the integer value of a specified shell environment variable.
 *
 * @param envVarName a null-terminated string containing the searched
 * environment variable name (with or without the leading '$').
 * @param envVarIntValue an already allocated integer to receive the
 * environment variable integer value.
 *
 * @return mcsSUCCESS on successful completion. Otherwise mcsFAILURE is
 * returned.
 */
mcsCOMPL_STAT miscGetEnvVarIntValue (const char       *envVarName,
                                     mcsINT32         *envVarIntValue)
{
    mcsSTRING64 envVarValue;

    /* Get the string value associated with the given env. var. */
    if (miscGetEnvVarValue(envVarName, envVarValue, sizeof(envVarValue)) ==
        mcsFAILURE)
    {
        return mcsFAILURE;
    }

    /* Convert the string value in an integer value */
    mcsINT32 nbReadValue = sscanf(envVarValue, "%d", envVarIntValue);
    if (nbReadValue != 1)
    {
        /* Raise an error and exit */
        errAdd(miscERR_FILE_ENV_VAR_NOT_INT, envVarName, envVarValue);
        return mcsFAILURE;
    }

    return mcsSUCCESS;
}


/**
 * Return the file name from a given simple path.
 * 
 * @warning This function is @em NOT re-entrant. The returned allocated buffer
 * will be @em DEALLOCATED on next call !\n\n
 *
 * @param fullPath null-terminated string containing the full path.
 *
 * @return a null-terminated string containing the found file name, or NULL if 
 * no file name was found in the given path or if an error occurred.
 *
 * \n
 * @ex 
 * @code
 *  ...
 * miscGetFileName("../data/myFile.fits")
 * ...
 * > myFile.fits
 * @endcode
 */
char *miscGetFileName(const char *fullPath)
{
    static char *buffer   = NULL;
    char        *token    = NULL;
    char        *fileName = NULL;

    /* If full file name is empty */
    if ((fullPath == NULL) || (strlen(fullPath) == 0))
    {
        /* Return NULL string */
        return ((char *)NULL);
    }

    /* De-alloc the previous buffer memory */
    if (buffer != NULL)
    {
        free(buffer);
    }

    /* Allocate memory for the static buffer */
    buffer = malloc(strlen(fullPath) + 1);
    if (buffer == NULL)
    {
        errAdd(miscERR_ALLOC);
        return ((char *)NULL);
    }

    /* Copy full file name into the static buffer */
    strcpy(buffer, fullPath);
    
    /* Establish string and get the first token */
    token = strtok(buffer, "/");

    /* While there are tokens in "string" */       
    while(token != NULL)
    {
        /* Set filename */
        fileName = token;

        /* Get next token */
        token = strtok(NULL, "/");
    }

    /* Return the found file name */
    return fileName;
}


/**
 * Return the file extension (without the dot) of a given simple path.
 * 
 * Return a pointer on the character following the last dot of the file name.
 *
 * @param fullPath a null-terminated string containing the full path.
 *
 * @return a pointer on the file extension, or NULL if no extension was found
 * in the given path.
 *
 * \n
 * @ex 
 * @code
 *  ...
 * miscGetExtension("../data/myFile.fits")
 * ...
 * > fits
 * @endcode
 */
char *miscGetExtension(char *fullPath)
{
    char *lastDotPtr   = NULL;
    char *lastSlashPtr = NULL;

    /* Make lastDotPtr points to the last occurrence of '.' in the path */
    lastDotPtr = strrchr(fullPath, '.');
    if (lastDotPtr == NULL)
    {
        /* Exits if no extension found */
        return ((char*)NULL);
    }

    /* Make lastSlashPtr points to the last occurrence of '/' in the path */
    lastSlashPtr = strrchr(fullPath, '/');

    /* if the extension found is a part of the path : "/dir/.dt/file" */
    if (lastSlashPtr > lastDotPtr)
    {
        return ((char*)NULL);
    }

    /* If the extension found is an invisible file : "/dir/.dt" */
    if (*(lastDotPtr - 1) == '/')
    {
        return ((char*)NULL);
    }

    /* Return a pointer on the first character of the found extension */
    return (lastDotPtr + 1);
}


/**
 * Give back a simple path without its file extension.
 * 
 * Remove the file extension (if any) of the given file name, using the given
 * buffer to store the result.
 *
 * The seeked extension can be given with or without the dot (e.g. : "fits" or
 * ".fits"). If not found, nothing is done.
 *
 * If the given extension is a NULL pointer, this function removes the extension
 * found using miscGetExtension() (if any).
 *
 * @param fullPath a null-terminated string containing a full path.
 * @param extension a null-terminated string containing the file extension to
 * yank, or NULL to yank any extension found.
 *
 * @return mcsSUCCESS on successful completion. Otherwise mcsFAILURE is
 * returned.
 *
 * \n
 * @ex 
 * @code
 *  ...
 * miscYankExtension("../data/myFile.fits", NULL)
 * ...
 * > ../data/myFile
 * @endcode
 * @code
 * ...
 * miscYankExtension("../data/myFile.fitsname.fits", "fits")
 * ...
 * > ../data/myFile.fitsname
 * @endcode
 */
mcsCOMPL_STAT miscYankExtension(char *fullPath, char *extension)
{    
    char *extensionPtr;

    /* Return if the given file name does not exist */
    if (fullPath == NULL)
    {
        return mcsFAILURE;
    }

    /* If an extension was found... */
    extensionPtr = miscGetExtension(fullPath);
    if (extensionPtr != NULL)
    {
        /* If an extension was provided... */
        if (extension != NULL)
        {
            /* If the given extension includes a leading dot... */
            if (*extension == '.')
            {
                /* Skip this dot */
                extension++;
            }

            /* If the provided extension does not match the found one */
            if (strcmp(extension, extensionPtr) != 0)
            {
                /* Do nothing */
                return mcsFAILURE;
            }
        }

        /* Cut the string on the last dot */
        *(extensionPtr - 1) = '\0';
    }

    return mcsSUCCESS;
}


/**
 * Remove the last path or file name (if any) in a given simple path.
 *
 * @param path a null-terminated string containing the path to be yanked.
 *
 * @return always mcsSUCCESS.
 */
mcsCOMPL_STAT miscYankLastPath(char *path)
{
    char *lastSlashPos = NULL;

    /* Find the last '/' occurrence in the given path */
    lastSlashPos = strrchr(path, '/');
    if (lastSlashPos != NULL)
    {
        /* Blank it and the following last path */
        *lastSlashPos = '\0';
    }

    return mcsSUCCESS;
}


/**
 * Resolve any environment variables found in a given simple or composed path.
 *
 * The function can resolve paths like "~/$MY_VAR/MY_DIR/file" or
 * "$HOME/$MY_VAR/MY_DIR/file".
 *
 * A composed path (i.e. a colon (':') separated list of paths) will be
 * resolved as well.
 *
 * Note that this function will not further simplify paths that starts with or
 * includes patterns like'./' or '../'.
 *
 * @warning
 * - This function is @em NOT re-entrant. The returned allocated buffer
 * will be @em DEALLOCATED on next call !
 * - No space is allowed in the given path.
 * - Each directory, file name or environment variable element must @em NOT be
 * longer than 255 characters.\n\n
 *
 * @param unresolvedPath a null-terminated string pointer on the path to be
 * resolved.
 *
 * @return a pointer to the resolved path, or NULL if an error occurred.
 */
char*         miscResolvePath    (const char *unresolvedPath)
{
    static mcsLOGICAL   init = mcsFALSE;
    static miscDYN_BUF  builtPath;

    miscDYN_BUF         unresolvedPathBuffer;
    mcsSTRING256        pathElement;
    mcsSTRING256        envVarValue;
    mcsINT32            pathElementLength;
    mcsUINT32           builtPathLength = 0;
    char               *unresolvedPathCopy = NULL;
    char               *endingChar = NULL;
    char               *nextSlashPtr = NULL;

    /* Check parameter validity */
    if (unresolvedPath == NULL)
    {
        errAdd(miscERR_NULL_PARAM, "unresolvedPath"); 
        return NULL;
    }

    /* Initialize the static Dynamic Buffer (if not already done) */
    if (init == mcsFALSE)
    {
        if (miscDynBufInit(&builtPath) == mcsFAILURE)
        {
            return NULL;
        }

        init = mcsTRUE;
    }

    /*
     * Make a local copy of the given unresolved path before freeing the
     * static buffer, in order to avoid data loss in case the user called
     * miscResolvePath() several times on its result.
     *
     * e.g miscResolvePath(miscResolvePath(path));
     */
    if (miscDynBufInit(&unresolvedPathBuffer) == mcsFAILURE)
    {
        return NULL;
    }
    if (miscDynBufAppendString(&unresolvedPathBuffer, unresolvedPath) ==
        mcsFAILURE)
    {
        miscDynBufDestroy(&unresolvedPathBuffer);
        return NULL;
    }
    unresolvedPathCopy = miscDynBufGetBuffer(&unresolvedPathBuffer);
    if (unresolvedPathCopy == NULL)
    {
        miscDynBufDestroy(&unresolvedPathBuffer);
        return NULL;
    }

    /* Reset the static Dynamic Buffer */
    if (miscDynBufReset(&builtPath) == mcsFAILURE)
    {
        miscDynBufDestroy(&unresolvedPathBuffer);
        return NULL;
    }

    /* Resolve the full path element by element */
    nextSlashPtr = strchr(unresolvedPathCopy, '/');
    do
    {
        /* If the current path element is an environment variable... */
        if (*unresolvedPathCopy == '$')
        {
            /* If the current path element is not the last one... */
            if (nextSlashPtr != NULL)
            {
                /* Its length is equal to : */
                pathElementLength = ((nextSlashPtr - unresolvedPathCopy) - 1);
            }
            else
            {
                /* Otherwise its length is equal to : */
                pathElementLength = strlen(unresolvedPathCopy);
            }

            /* Copy only the current path element in the temporary buffer */
            strncpy(pathElement, (unresolvedPathCopy + 1), pathElementLength);
            *(pathElement + pathElementLength) = '\0';

            /* Resolve the current path element as an env. var */
            if (miscGetEnvVarValue(pathElement, envVarValue,sizeof(envVarValue))
                == mcsFAILURE)
            {
                miscDynBufDestroy(&unresolvedPathBuffer);
                return NULL;
            }

            /* Append the env. var. value to the resolved path */
            if (miscDynBufAppendString(&builtPath, envVarValue) == mcsFAILURE)
            {
                miscDynBufDestroy(&unresolvedPathBuffer);
                return NULL;
            }
        }
        /* Else if the current path element is '~' */
        else if (*unresolvedPathCopy == '~')
        {
            /* Resolve the '~' (aka 'HOME') env. var  value */
            if (miscGetEnvVarValue("HOME", envVarValue, sizeof(envVarValue)) ==
                mcsFAILURE)
            {
                miscDynBufDestroy(&unresolvedPathBuffer);
                return NULL;
            }

            /* Append the 'HOME' env. var. value to the resolved path */
            if (miscDynBufAppendString(&builtPath, envVarValue) == mcsFAILURE)
            {
                miscDynBufDestroy(&unresolvedPathBuffer);
                return NULL;
            }
        }
        /* The current path element is a real directory of file name */
        else
        {
            /* If the current path element is not the last one... */
            if (nextSlashPtr != NULL)
            {
                /* Its length is equal to : */
                pathElementLength = (nextSlashPtr - unresolvedPathCopy);
            }
            else
            {
                /* Otherwise its length is equal to : */
                pathElementLength = strlen(unresolvedPathCopy);
            }

            /* Copy the current path element in a temporary buffer */
            strncpy(pathElement, unresolvedPathCopy, pathElementLength);
            *(pathElement + pathElementLength) = '\0';

            /* Append the path element to the resolved path */
            if (miscDynBufAppendString(&builtPath, pathElement) == mcsFAILURE)
            {
                miscDynBufDestroy(&unresolvedPathBuffer);
                return NULL;
            }
        }

        /* Add a '/' to the resolved path */
        if (miscDynBufAppendString(&builtPath, "/") == mcsFAILURE)
        {
            miscDynBufDestroy(&unresolvedPathBuffer);
            return NULL;
        }

        /* If the current path element is NOT the last one... */
        if (nextSlashPtr != NULL)
        {
            /* If the current path element is NOT the one before the last... */
            if (*nextSlashPtr != '\0')
            {
                /* Point to the next element path to be resolved */
                unresolvedPathCopy = (nextSlashPtr + 1);
            }
            else
            {
                unresolvedPathCopy = nextSlashPtr;
            }
        }
        else
        {
            /* End the resolved path string */
            unresolvedPathCopy = "\0";
        }

        /* If there is one more path after the current one... */
        if (*unresolvedPathCopy == ':')
        {
            /* Append a ':' separator in the resolved path */
            if (miscDynBufAppendString(&builtPath, ":") == mcsFAILURE)
            {
                miscDynBufDestroy(&unresolvedPathBuffer);
                return NULL;
            }

            /* Point to the beginning of the next path */
            unresolvedPathCopy++;
        }
    }
    while (((nextSlashPtr = strchr(unresolvedPathCopy, '/')) != NULL) ||
           (*unresolvedPathCopy != '\0'));

    /*
     * Since we cannot know if a filename is contained in the path, we should
     * not allow slash at the end of the complete path
     */

    /* Get the Dynamic Buffer length */
    if (miscDynBufGetNbStoredBytes(&builtPath, &builtPathLength) == mcsFAILURE)
    {
        miscDynBufDestroy(&unresolvedPathBuffer);
        return NULL;
    }

    /* Get Dynamic Buffer internal buffer pointer */
    endingChar = miscDynBufGetBuffer(&builtPath);
    if (endingChar == NULL)
    {
        miscDynBufDestroy(&unresolvedPathBuffer);
        return NULL;
    }
    
    /* Compute the last path character position */
    endingChar += (builtPathLength - 2);

    /* If the path last character is a '/' */
    if (*endingChar == '/')
    {
        /* Replace the '/' with a '\0' */
        *endingChar = '\0';

        /* Decrease the global path length */
        builtPath.storedBytes--;
    }

    /* Strip the Dynamic Buffer */
    if (miscDynBufStrip(&builtPath) == mcsFAILURE)
    {
        miscDynBufDestroy(&unresolvedPathBuffer);
        return NULL;
    }
    
    miscDynBufDestroy(&unresolvedPathBuffer);
    return miscDynBufGetBuffer(&builtPath);
}


/**
 * Test if a file (or a directory) exists at a given simple path.
 *
 * @param fullPath a null-terminated string containing the path to be tested.
 * @param addError an mcsLOGICAL to specify wether or not this function should
 * raise an error that tries to explain the reason why the file was not found.
 *
 * @return mcsTRUE if the file or directory exists, mcsFALSE otherwise.
 */
mcsLOGICAL    miscFileExists        (const char       *fullPath,
                                     mcsLOGICAL        addError)
{
    char*       resolvedPath = NULL;
    struct stat fileInformationBuffer;

    /* Test the fullPath parameter validity */
    if ((fullPath == NULL) || (strlen(fullPath) == 0))
    {
        /* If an explaining error should be raised */
        if (addError == mcsTRUE)
        {
            /* Raise it */
            errAdd(miscERR_NULL_PARAM, "fullPath");
        }

        return mcsFALSE;
    }

    /* Try to resolve any Env. Var contained in the given path */
    resolvedPath = miscResolvePath(fullPath);
    if (resolvedPath == NULL)
    {
        /* If an explaining error should NOT be raised... */
        if (addError == mcsFALSE)
        {
            /* Erase the error stack */
            errResetStack();
        }

        return mcsFALSE;
    }

    /* Try to get file system informations of the file to be tested */
    if (stat(resolvedPath, &fileInformationBuffer) == -1)
    {
        /* If an explaining error should be raised... */
        if (addError == mcsTRUE)
        {
            /*
             * Raise an error according to the problem detected by the 'stat'
             * function call
             */
            switch (errno)
            {
                case EACCES:
                    /* Permission denied */
                    errAdd(miscERR_FILE_PERMISSION_DENIED, resolvedPath);
                    break;
        
                case ENAMETOOLONG:
                    /* File name too long */
                    errAdd(miscERR_FILE_NAME_TOO_LONG, resolvedPath);
                    break;
        
                case ENOENT:
                    /* A component of the path doesn't exist */
                case ENOTDIR:
                    /* A component of the path is not a directory */
                    errAdd(miscERR_FILE_DOESNT_EXIST, resolvedPath);
                    break;
        
                case ELOOP:
                    /* Too many sym. links encountered while traversing path */
                    errAdd(miscERR_FILE_TOO_MANY_SYM_LINKS, resolvedPath);
                    break;
        
                default : 
                    errAdd(miscERR_FILE_UNDEFINED_ERRNO, resolvedPath, errno);
            }
        }

        return mcsFALSE;
    }

    return mcsTRUE;
}


/**
 * Search for a file (or a directory) in a composed path.
 *
 * @warning This function is @em NOT re-entrant. The returned allocated buffer
 * will be @em DEALLOCATED on next call !\n\n
 *
 * @param path the list of path to be searched, each separated by colons (':').
 * @param fileName the seeked file or directory name.
 *
 * @return a pointer to the @em FIRST path where the file or directory is, or
 * NULL if not found or an error occurred.
 */
char* miscLocateFileInPath(const char *path, const char *fileName)
{
    static mcsLOGICAL  init = mcsFALSE;
    static miscDYN_BUF tmpPath;
    const char* originalPath = path;
    int pathPartLength;
    char *colonPtr;

    /* Initialize buffer (if not already done */
    if (init == mcsFALSE)
    {
        miscDynBufInit(&tmpPath);
        init = mcsTRUE;
    }

    /* Test the path parameter validity */
    if ((path == NULL) || (strlen(path) == 0))
    {
        path = MCS_STANDARD_SERACH_PATH;
    }

    /* Test the fileName parameter validity */
    if ((fileName == NULL) || (strlen(fileName) == 0))
    {
        errAdd(miscERR_NULL_PARAM, "fileName");
        return NULL;
    }

    /* Reset the static Dynamic Buffer */
    if (miscDynBufReset(&tmpPath) == mcsFAILURE)
    {
        return NULL;
    }

    /*
     * For each path part, until all of them were tested or a valid path was
     * found
     */
    char *validPath = NULL;
    do
    {
        /* Compute the length of the current path part */
        pathPartLength = 0;
        colonPtr = strchr(path, ':');
        if (colonPtr == NULL)
        {
            pathPartLength = strlen(path);
        }
        else
        {
            pathPartLength = colonPtr - path;
        }

        /* Construct the to-be-tested temporary path */
        miscDynBufAppendBytes(&tmpPath, (char*)path, pathPartLength);
        miscDynBufAppendBytes(&tmpPath, "/", 1);
        miscDynBufAppendString(&tmpPath, (char*)fileName);

        /* If no file exists at the temporary path */
        validPath = miscDynBufGetBuffer(&tmpPath);
        if (miscFileExists(validPath, mcsFALSE) == mcsFALSE)
        {
            /* Reset the temporary path variable */
            validPath = NULL;

            /* Reset the static Dynamic Buffer */
            if (miscDynBufReset(&tmpPath) == mcsFAILURE)
            {
                return NULL;
            }

            /* If there is any ':' left in the given path */
            path = strchr(path, ':');
            if (path != NULL)
            {
                /* Make path pointer point to the next part beginning */
                path++;
            }
        }
    } while ((path != NULL) && (validPath == NULL));

    /* Minimize allocated memory used by the static Dynamic Buffer */
    if (miscDynBufStrip(&tmpPath) == mcsFAILURE)
    {
        return NULL;
    }

    /* If the file was not found along the path */
    if (validPath == NULL)
    {
        /* Add an error in the Errors Stack */
        errAdd(miscERR_FILE_NOT_FOUND_IN_PATH, fileName, originalPath);
    }

    return miscResolvePath(validPath);
}


/**
 * Search for a file (according to its extension) in the pre-configured @em
 * pathSearchList composed paths list.
 *
 * @warning This function is @em NOT re-entrant. The returned allocated buffer
 * will be @em DEALLOCATED on next call !\n\n
 *
 * @param fileName the name of the searched file with its extension.
 *
 * @return a pointer to the @em FIRST path where the file is, or NULL if not
 * found or an error occurred.
 */
char* miscLocateFile (const char *fileName)
{
    char       *fileExtension = NULL;
    mcsUINT32   i             = 0;
    mcsLOGICAL  found         = mcsFALSE;

    /* Test the fileName parameter validity */
    if ((fileName == NULL) || (strlen(fileName) == 0))
    {
        errAdd(miscERR_NULL_PARAM, "fileName");
        return NULL;
    }
    
    /*
     * Check first if the file exists; i.e if the given file corresponds
     * to an accessible file.
     */
    if (miscFileExists(fileName, mcsFALSE) == mcsTRUE)
    {
        return miscResolvePath(fileName);
    }

    /* Get the file extension */
    fileExtension = miscGetExtension((char*)fileName);
    if (fileExtension == NULL)
    {
        errAdd(miscERR_FILE_EXTENSION_MISSING, fileName);
        return NULL;
    }

    /*
     * For each path of the list, until all of them were tested or a path
     * corresponding to fileExtension was found
     */
    while (pathSearchList[i][miscEXT_IDX] != NULL)
    {
        /* Compare the file extension with the current one in the path list */
        if (strcmp(fileExtension, pathSearchList[i][miscEXT_IDX]) == 0)
        {
            found = mcsTRUE;
            break;
        }

        i++;
    }

    /* Return wether the file is at the path or not */
    if (found == mcsTRUE)
    {
        return miscLocateFileInPath(pathSearchList[i][miscPATH_IDX], fileName);
    }
    else
    {
        errAdd(miscERR_FILE_EXTENSION_UNKNOWN, fileExtension, fileName);
        return NULL;
    }
}


/**
 * Search for a directory in the standard "../:$INTROOT/:$MCSROOT/" MCS path.
 *
 * @warning This function is @em NOT re-entrant. The returned allocated buffer
 * will be @em DEALLOCATED on next call !\n\n
 *
 * @param dirName the name of the searched directory.
 *
 * @return a pointer to the @em FIRST path where the directory is, or NULL if
 * not found or an error occurred.
 */
char* miscLocateDir (const char *dirName)
{
    /* Return wether the directory is at the path or not */
    return miscLocateFileInPath(NULL, dirName);
}


/**
 * Search for an executable in the standard "../bin/:$INTROOT/bin/:$MCSROOT/bin/" MCS path.
 *
 * @warning This function is @em NOT re-entrant. The returned allocated buffer
 * will be @em DEALLOCATED on next call !\n\n
 *
 * @param exeName the name of the searched executable.
 *
 * @return a pointer to the @em FIRST path where the executable is, or NULL if
 * not found or an error occurred.
 */
char* miscLocateExe (const char *exeName)
{
    /* Return wether the executable is at the path or not */
    return miscLocateFileInPath("../bin/:$INTROOT/bin/:$MCSROOT/bin/", exeName);
}


/*___oOo___*/
