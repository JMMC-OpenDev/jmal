/*******************************************************************************
* JMMC project
*
* "@(#) $Id: msgManager.cpp,v 1.2 2004-12-15 15:55:35 lafrasse Exp $"
*
* who       when         what
* --------  -----------  -------------------------------------------------------
* gzins     06-Dec-2004  Created
* lafrasse  15-Dec-2004  Re-added Doxygen documentation from the npw removed
*                        msgManager.c
*
*
*******************************************************************************/

/**
 * \file
 * \e \<msgManager\> - inter-process communication server.
 *
 * \b Synopsis:\n
 * \e \<msgManager\>
 *
 * \b Details:\n
 * \e \<msgManager\> is the communication server allowing message exchange
 * between processes. Each process connected to this server can send message to
 * the other connected processes.
 * 
 */

static char *rcsId="@(#) $Id: msgManager.cpp,v 1.2 2004-12-15 15:55:35 lafrasse Exp $"; 
static void *use_rcsId = ((void)&use_rcsId,(void *) &rcsId);


/* 
 * System Headers 
 */
#include <stdlib.h>
#include <iostream>
#include <signal.h>

/**
 * \namespace std
 * Export standard iostream objects (cin, cout,...).
 */
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
#include "msgMANAGER.h"
#include "msgPrivate.h"

/*
 * Local Variables
 */
static msgMANAGER *msgManager=NULL;

/* 
 * Signal catching functions  
 */

/**
 * Trap certain system signals.
 *
 * Particulary manages 'broken pipe' and 'dead process' signals, plus the end
 * signal (i.e. CTRL-C).
 *
 * \param signalNumber the system signal to be trapped
 */

void msgSignalHandler (int signalNumber)
{
    logInfo("Received %d system signal...", signalNumber);
    if (signalNumber == SIGPIPE)
    {
        return;
    }
    logInfo("%s program aborted.", mcsGetProcName());
    delete (msgManager);
    exit (EXIT_SUCCESS);
}

/* 
 * Main
 */

int main(int argc, char *argv[])
{
    // Message manager instance
    msgManager = new msgMANAGER;
    
    /* Init system signal trapping */
    if (signal(SIGINT, msgSignalHandler) == SIG_ERR)
    {
        logError("signal(SIGINT, ...) function error");
        exit(EXIT_FAILURE);
    }
    if (signal (SIGTERM, msgSignalHandler) == SIG_ERR)
    {
        logError("signal(SIGTERM, ...) function error");
        exit(EXIT_FAILURE);
    }
    if (signal (SIGPIPE, msgSignalHandler) == SIG_ERR)
    {
        logError("signal(SIGPIPE, ...) function error");
        exit(EXIT_FAILURE);
    }

    // Initialization
    if (msgManager->Init(argc, argv) == FAILURE)
    {
        // Close error stack
        errCloseStack();
        exit (EXIT_FAILURE);
    }

    // Enter in main loop
    if (msgManager->MainLoop() == FAILURE)
    {
        // Error handling if necessary
        
        // Exit from the application with FAILURE
        exit (EXIT_FAILURE);
    }

    // Exit from the application with SUCCESS
    delete (msgManager);
    exit (EXIT_SUCCESS);
}


/*___oOo___*/
