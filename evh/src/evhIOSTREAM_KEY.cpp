/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: evhIOSTREAM_KEY.cpp,v 1.4 2006-05-11 13:04:18 mella Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2005/01/29 15:17:02  gzins
 * Added CVS log as modification history
 *
 * gzins     27-Sep-2004  Created
 *
 ******************************************************************************/

/**
 * \file
 * Definition of the evhIOSTREAM_KEY class.
 */

static char *rcsId __attribute__ ((unused)) ="@(#) $Id: evhIOSTREAM_KEY.cpp,v 1.4 2006-05-11 13:04:18 mella Exp $";

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
#include "evhIOSTREAM_KEY.h"
#include "evhPrivate.h"

/*
 * Class constructor
 */
evhIOSTREAM_KEY::evhIOSTREAM_KEY(const int sd) :
    evhKEY(evhTYPE_IOSTREAM)
{
    SetSd(sd);
}
/**
 * Copy constructor.
 */
evhIOSTREAM_KEY::evhIOSTREAM_KEY(const evhIOSTREAM_KEY &key) : evhKEY(key)
{
    logExtDbg("evhIOSTREAM_KEY::evhIOSTREAM_KEY()"); 
    *this = key;
}


/**
 * Class destructor
 */
evhIOSTREAM_KEY::~evhIOSTREAM_KEY()
{
}

/**
 * Assignment operator
 */
evhIOSTREAM_KEY& evhIOSTREAM_KEY::operator =( const evhIOSTREAM_KEY& key)
{
    logExtDbg("evhIOSTREAM_KEY::operator =()"); 

    SetSd(key._sd);

    return *this;
}

/*
 * Public methods
 */
/**
 * Determines whether the given key is equal to this.
 *
 * \param key element to be compared to this.
 * 
 * \return mcsTRUE if it is equal, mcsFALSE otherwise.
 */
mcsLOGICAL evhIOSTREAM_KEY::IsSame(const evhKEY& key)
{
    logExtDbg("evhIOSTREAM_KEY::IsSame()");

    // If it is the same event type (i.e. command event)
    if (evhKEY::IsSame(key) == mcsTRUE)
    {
        if (_sd == ((evhIOSTREAM_KEY *)&key)->_sd)
        {
            return mcsTRUE;
        }
    }
    return mcsFALSE;
}

/**
 * Determines whether the given key matches to this.
 *
 * \param key element to be compared to this.
 * 
 * \return mcsTRUE if it matches, mcsFALSE otherwise.
 */
mcsLOGICAL evhIOSTREAM_KEY::Match(const evhKEY& key)
{
    logExtDbg("evhIOSTREAM_KEY::Match()");

    if (evhKEY::IsSame(key) == mcsTRUE)
    {
        if (_sd == ((evhIOSTREAM_KEY *)&key)->_sd)
        {
            return mcsTRUE;
        }
    }
    return mcsFALSE;
}

/**
 * Set command name 
 *
 * \return reference to the object itselfu
 *
 * \warning If command name length exceeds mcsCMD_LEN characters, it is
 * truncated
 */
evhIOSTREAM_KEY & evhIOSTREAM_KEY::SetSd(const int sd)
{
    logExtDbg("evhIOSTREAM_KEY::SetSd()");

    _sd = sd;

    return *this;
}

/**
 * Get command name.
 *
 * \return command name type 
 */
int evhIOSTREAM_KEY::GetSd() const
{
    logExtDbg("evhIOSTREAM_KEY::GetSd()");

    return (_sd);
}

/*___oOo___*/
