#ifndef evhTASK_H
#define evhTASK_H
/*******************************************************************************
* JMMC project
*
* "@(#) $Id: evhTASK.h,v 1.2 2004-11-18 17:36:47 gzins Exp $"
*
* who       when		 what
* --------  -----------	 -------------------------------------------------------
* gzins     09-Jun-2004  created
* gzins     18-Nov-2004  splitted parsing and usage methods to separate
*                        options and arguments in command-line parameters
* 
*******************************************************************************/

#ifndef __cplusplus
#error This is a C++ include file and cannot be used from plain C
#endif

#include "mcs.h"

class evhTASK
{
public:
    evhTASK();
    virtual ~evhTASK();

    virtual mcsCOMPL_STAT Init(mcsINT32 argc, char *argv[]);
    virtual mcsCOMPL_STAT AppInit();

    virtual const char *Name();
    virtual mcsCOMPL_STAT Usage();
    virtual mcsCOMPL_STAT PrintSynopsis();
    virtual mcsCOMPL_STAT PrintStdOptions();
    virtual mcsCOMPL_STAT PrintAppOptions();
    virtual mcsCOMPL_STAT PrintArguments();
    virtual mcsCOMPL_STAT ParseOptions(mcsINT32 argc, char *argv[]);
    virtual mcsCOMPL_STAT ParseStdOptions(mcsINT32 argc, char *argv[],
                                          mcsINT32 *optInd,
                                          mcsLOGICAL *optUsed);
    virtual mcsCOMPL_STAT ParseAppOptions(mcsINT32 argc, char *argv[],
                                          mcsINT32 *optInd,
                                          mcsLOGICAL *optUsed);
    virtual mcsCOMPL_STAT ParseArguments(mcsINT32 argc, char *argv[],
                                         mcsINT32 *optInd,
                                         mcsLOGICAL *optUsed);
    virtual mcsLOGICAL IsFileLogOption();
    virtual mcsLOGICAL IsStdoutLogOption();
    virtual mcsLOGICAL IsActionLogOption();
    virtual mcsLOGICAL IsTimerLogOption();

    virtual const char *GetSwVersion();

protected:
private:
    // Declaration of copy constructor and assignment operator as private
    // methods, in order to hide them from the users.
    evhTASK& operator=(const evhTASK&);
    evhTASK (const evhTASK&);

    mcsLOGICAL _fileLogOption;
    mcsLOGICAL _stdoutLogOption;
    mcsLOGICAL _actionLogOption; 
    mcsLOGICAL _timerLogOption;
};

#endif /*!evhTASK_H*/

/*___oOo___*/
