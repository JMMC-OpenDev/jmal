#ifndef errPrivate_H
#define errPrivate_H
/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: errPrivate.h,v 1.6 2005-02-09 14:26:03 lafrasse Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2005/01/27 14:14:24  gzins
 * Added declaration of functions/macros related to end-user oriented error message.
 * Changed errERROR to errERROR_STACK
 *
 * gzins     16-Jun-2004  Created
 * lafrasse  14-Dec-2004  Moved errMSG_MAX_LEN to err.H
 *
 *
 ******************************************************************************/

/* The following piece of code alternates the linkage type to C for all 
functions declared within the braces, which is necessary to use the 
functions in C++-code.
*/

#ifdef __cplusplus
extern C {
#endif

#include <stdarg.h>
/* Module name */
#define MODULE_ID "err"

/* Local functions */
mcsCOMPL_STAT errResetLocalStack (errERROR_STACK *error);
mcsCOMPL_STAT errCloseLocalStack (errERROR_STACK *error);
mcsCOMPL_STAT errDisplayLocalStack (errERROR_STACK *error);
mcsLOGICAL    errIsInLocalStack (errERROR_STACK    *error,
                                 const mcsMODULEID moduleId,
                                 mcsINT32          errorId);
mcsLOGICAL    errLocalStackIsEmpty (errERROR_STACK *error);
mcsINT8       errGetLocalStackSize (errERROR_STACK *error);
mcsCOMPL_STAT errPackLocalStack (errERROR_STACK *error,
                                 char           *buffer,
                                 mcsUINT32      bufLen);
mcsCOMPL_STAT errUnpackLocalStack (errERROR_STACK *error,
                                   const char     *buffer,
                                   mcsUINT32      bufLen);
mcsCOMPL_STAT errPushInLocalStack(errERROR_STACK *error,
                                  const char     *timeStamp,
                                  const char     *procName,
                                  const char     *moduleId,
                                  const char     *location,
                                  mcsINT32       errorId,
                                  mcsLOGICAL     isErrUser,
                                  char           severity,
                                  char           *runTimePar);
mcsCOMPL_STAT errAddInLocalStack (errERROR_STACK    *error, 
                                  const mcsMODULEID moduleId,
                                  const char        *fileLine,
                                  mcsINT32          errorId,
                                  mcsLOGICAL        isErrUser,
                                  ... );
mcsCOMPL_STAT errAddInLocalStack_v (errERROR_STACK    *error, 
                                    const mcsMODULEID moduleId,
                                    const char        *fileLine,
                                    mcsINT32          errorId,
                                    mcsLOGICAL        isErrUser,
                                    va_list           argPtr);
char         *errUserGetInLocalStack (errERROR_STACK *error);

/* Global variable */
extern errERROR_STACK errGlobalStack;

#ifdef __cplusplus
}
#endif

#endif /*!errPrivate_H*/
/*___oOo___*/