#ifndef msgPROCESS_H
#define msgPROCESS_H
/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: msgPROCESS.h,v 1.3 2005-01-29 19:56:16 gzins Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/01/24 15:39:54  gzins
 * Added CVS logs as modification history
 *
 * gzins     06-Dec-2004  Created
 *
 ******************************************************************************/

/**
 * \file
 * msgPROCESS class declaration.
 */

#ifndef __cplusplus
#error This is a C++ include file and cannot be used from plain C
#endif

/*
 * Local header files
 */
#include "msgSOCKET_CLIENT.h"

/*
 * Class declaration
 */

/**
 * Class handling process connected to the MCS message service.
 * 
 * The msgPROCESS class provides methods to set/get informations of a
 * connected process to the MCS message service. 
 */
class msgPROCESS : public msgSOCKET_CLIENT
{

public:
    // Class constructor
    msgPROCESS();

    // Class destructor
    virtual ~msgPROCESS();

    virtual mcsCOMPL_STAT SetName(char *name);
    virtual const char   *GetName() const;

    virtual mcsCOMPL_STAT SetId(mcsINT32 pid);
    virtual mcsINT32      GetId() const;

    virtual mcsCOMPL_STAT SetUnicity(mcsLOGICAL pid);
    virtual mcsLOGICAL    IsUnique() const;

protected:

private:
    // Declaration of copy constructor and assignment operator as private
    // methods, in order to hide them from the users.
    msgPROCESS(const msgPROCESS &process);
    msgPROCESS& operator=(const msgPROCESS&);

    // Name of the process; i.e. MCS registering name
    mcsPROCNAME _name;
   
    // Proces ID
    mcsINT32 _id;

    // Unicity flag
    mcsLOGICAL _unicity;
};

#endif /*!msgPROCESS_H*/

/*___oOo___*/
