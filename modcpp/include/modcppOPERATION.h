#ifndef modcppOPERATION_H
#define modcppOPERATION_H
/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/

/**
 * \file
 * Brief description of the header file, which ends at this dot.
 */


#ifndef __cplusplus
#error This is a C++ include file and cannot be used from plain C
#endif


/*
 * Class declaration
 */

/**
 * Brief description of the class, which ends at this dot.
 * 
 * OPTIONAL detailed description of the class follows here.
 *
 * \usedfiles
 * OPTIONAL. If files are used, for each one, name, and usage description.
 * \filename fileName1 :  usage description of fileName1
 * \filename fileName2 :  usage description of fileName2
 *
 * \n
 * \env
 * OPTIONAL. If needed, environmental variables accessed by the class. For
 * each variable, name, and usage description, as below.
 * \envvar envVar1 :  usage description of envVar1
 * \envvar envVar2 :  usage description of envVar2
 * 
 * \n
 * \warning OPTIONAL. Warning if any (software requirements, ...)
 *
 * \n
 * \ex
 * OPTIONAL. Code example if needed
 * \n Brief example description.
 * \code
 * Insert your code example here
 * \endcode
 *
 * \sa OPTIONAL. See also section, in which you can refer other documented
 * entities. Doxygen will create the link automatically.
 * \sa modcppMain.C
 * 
 * \bug OPTIONAL. Bugs list if it exists.
 * \bug For example, description of the first bug
 * \bug For example, description of the second bug
 * 
 * \todo OPTIONAL. Things to forsee list, if needed. For example, 
 * \todo add other methods, dealing with operations.
 * 
 */
class modcppOPERATION 
{

    // IMPORTANT : doxygen extracted documentation for public functions is
    // located in the .C file and not in this header file. It's why a normal
    // documentation block (beginning with 2 slashes) is used here with a
    // brief description (just to know a little about the function) and NOT A
    // DOXYGEN DOCUMENTATION BLOCK (beginning with 1 slash and 2 stars).
    
public:
     // Brief description of the constructor
     modcppOPERATION();
     
     // Brief description of the constructor
     modcppOPERATION(char *name);
     
     // Brief description of the destructor
     virtual ~modcppOPERATION();

     // Brief description of the method
     mcsCOMPL_STAT Add(mcsINT8 x, mcsINT8 y);

     // Brief description of the method
     mcsCOMPL_STAT Divide(mcsINT8 x, mcsINT8 y, mcsFLOAT *z);

     // Brief description of the method
     mcsCOMPL_STAT SubAndMultiply(mcsINT8 x, mcsINT8 y);

     // Brief description of the method
     void SetName(char *name);
     
     // Brief description of the method
     char * GetName();

protected:
     // Brief description of the method
     mcsCOMPL_STAT Sub(mcsINT8 x, mcsINT8 y);

private:    
     /** Brief member description */
     char _name[64];
     
     // Brief description of the method
     mcsCOMPL_STAT Multiply(mcsINT8 x, mcsINT8 y);
};


#endif /*!modcppOPERATION_H*/


/*___oOo___*/