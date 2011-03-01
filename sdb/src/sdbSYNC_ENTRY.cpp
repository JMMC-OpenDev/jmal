/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: sdbSYNC_ENTRY.cpp,v 1.13 2007-10-26 13:25:26 lafrasse Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.12  2007/05/15 11:12:40  gzins
 * Fixed warning
 *
 * Revision 1.11  2007/05/15 11:11:18  gzins
 * Updtated to prevent multiple semaphore creation
 *
 * Revision 1.10  2007/05/15 09:15:10  gzins
 * Fixed minor bug
 *
 * Revision 1.9  2007/05/15 08:17:25  gzins
 * Added IsInit method
 *
 * Revision 1.8  2006/12/21 15:03:08  lafrasse
 * Moved from static-based design to instance-based design.
 *
 * Revision 1.7  2006/05/11 13:04:57  mella
 * Changed rcsId declaration to perform good gcc4 and gcc3 compilation
 *
 * Revision 1.6  2006/04/07 07:51:38  swmgr
 * Changed logTest to logDebug
 *
 * Revision 1.5  2006/03/28 11:08:58  gzins
 * Adjusted log message level
 *
 * Revision 1.4  2006/02/23 16:09:46  lafrasse
 * Removed temporary test code
 *
 * Revision 1.3  2006/02/22 17:05:43  lafrasse
 * Added security check to disable semaphores use in case of  bad initialization
 *
 * Revision 1.2  2005/12/22 14:10:35  lafrasse
 * Added a way to release all the created semaphores used by sdbSYNC_ENTRY
 *
 * Revision 1.1  2005/12/20 13:52:34  lafrasse
 * Added preliminary support for INTRA-process action log
 *
 ******************************************************************************/

/**
 * @file
 * Definition of sdbSYNC_ENTRY class.
 */

static char *rcsId __attribute__ ((unused)) ="@(#) $Id: sdbSYNC_ENTRY.cpp,v 1.13 2007-10-26 13:25:26 lafrasse Exp $";

/* 
 * System Headers 
 */
#include <iostream>
using namespace std;


/*
 * MCS Headers 
 */
#include "mcs.h"
#include "log.h"
#include "err.h"


/*
 * Local Headers 
 */
#include "sdbSYNC_ENTRY.h"
#include "sdbPrivate.h"
#include "sdbErrors.h"


/*
 * Static members dfinition 
 */


/**
 * Class constructor
 */
sdbSYNC_ENTRY::sdbSYNC_ENTRY()
{
    _emptyBufferSemaphore = 0;
    _fullBufferSemaphore  = 0;
    
    _initSucceed = mcsFALSE;
    _lastMessage = mcsFALSE;
}

/**
 * Class destructor
 */
sdbSYNC_ENTRY::~sdbSYNC_ENTRY()
{
}


/*
 * Public methods
 */
/**
 * !!! NOT YET DOCUMENTED cause THIS IMPLEMENTATION IS TEMPORARY !!!
 */
mcsCOMPL_STAT sdbSYNC_ENTRY::Init(void)
{
    // Static member initialization
    memset(_buffer, '\0', sizeof(_buffer));
    _lastMessage = mcsFALSE;

    if (Destroy() == mcsFAILURE)
    {
        return mcsFAILURE;
    }

    /* Semaphores initialisation */
    if (thrdSemaphoreInit(&_emptyBufferSemaphore, 1) == mcsFAILURE)
    {
        _initSucceed = mcsFALSE;
        return mcsFAILURE;
    }
    if (thrdSemaphoreInit(&_fullBufferSemaphore, 0) == mcsFAILURE)
    {
        _initSucceed = mcsFALSE;
        return mcsFAILURE;
    }

    _initSucceed = mcsTRUE;
    return mcsSUCCESS;
}

/**
 * !!! NOT YET DOCUMENTED cause THIS IMPLEMENTATION IS TEMPORARY !!!
 */
mcsCOMPL_STAT sdbSYNC_ENTRY::Destroy(void)
{
    if (_initSucceed == mcsTRUE)
    {
        /* Semaphores destruction */
        if (thrdSemaphoreDestroy(_emptyBufferSemaphore) == mcsFAILURE)
        {
            return mcsFAILURE;
        }
        if (thrdSemaphoreDestroy(_fullBufferSemaphore) == mcsFAILURE)
        {
            return mcsFAILURE;
        }
    }
    _initSucceed = mcsFALSE;

    return mcsSUCCESS;
}

/**
 * !!! NOT YET DOCUMENTED cause THIS IMPLEMENTATION IS TEMPORARY !!!
 */
mcsCOMPL_STAT sdbSYNC_ENTRY::Write(const char* message, const mcsLOGICAL lastMessage)
{
    logTrace("sdbSYNC_ENTRY::Write()");

    /* Verify parameter vailidity */
    if (message == NULL)
    {
        errAdd(sdbERR_NULL_PARAM, "message");
        return mcsFAILURE;
    }
    
    if (_initSucceed == mcsTRUE)
    {
        /* Wait for buffer emptyness */
        logDebug("Waiting for the buffer to be empty.");
        if (thrdSemaphoreWait(_emptyBufferSemaphore) == mcsFAILURE)
        {
            return mcsFAILURE;
        }
        logDebug("The buffer has been emptied.");
    }
        
    logDebug("Storing the new message in the buffer.");
    _lastMessage = lastMessage;
    strncpy(_buffer, message, sizeof(_buffer));
    
    if (_initSucceed == mcsTRUE)
    {
        /* Signal that a new message has been posted */
        logDebug("Signals that the new message has been posted.");
        if (thrdSemaphoreSignal(_fullBufferSemaphore) == mcsFAILURE)
        {
            return mcsFAILURE;
        }
    }

    return mcsSUCCESS;
}

/**
 * !!! NOT YET DOCUMENTED cause THIS IMPLEMENTATION IS TEMPORARY !!!
 */
mcsCOMPL_STAT sdbSYNC_ENTRY::Wait(char* message, mcsLOGICAL* lastMessage)
{
    logTrace("sdbSYNC_ENTRY::Wait()");

    /* Verify parameter vailidity */
    if (message == NULL)
    {
        errAdd(sdbERR_NULL_PARAM, "message");
        return mcsFAILURE;
    }
    if (lastMessage == NULL)
    {
        errAdd(sdbERR_NULL_PARAM, "lastMessage");
        return mcsFAILURE;
    }

    if (_initSucceed == mcsTRUE)
    {
        /* Wait for a new message to be posted */
        logDebug("Waiting for a new message in the buffer.");
        if (thrdSemaphoreWait(_fullBufferSemaphore) == mcsFAILURE)
        {
            return mcsFAILURE;
        }
        logDebug("A new message has been received in the buffer.");
    }
    
    logDebug("Giving back the new message.");
    *lastMessage = _lastMessage;
    strncpy(message, _buffer, sizeof(_buffer));

    if (_initSucceed == mcsTRUE)
    {
        /* Signal buffer emptyness */
        logDebug("Signals that the new message has been used.");
        if (thrdSemaphoreSignal(_emptyBufferSemaphore) == mcsFAILURE)
        {
            return mcsFAILURE;
        }
    }

    return mcsSUCCESS;
}

/**
 * !!! NOT YET DOCUMENTED cause THIS IMPLEMENTATION IS TEMPORARY !!!
 */
mcsLOGICAL sdbSYNC_ENTRY::IsInit()
{
    logTrace("sdbSYNC_ENTRY::IsInit()");

    return _initSucceed;
}

/*
 * Protected methods
 */


/*
 * Private methods
 */


/*___oOo___*/