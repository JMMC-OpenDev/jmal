/*******************************************************************************
* JMMC project
* 
* "@(#) $Id: modcProc.c,v 1.1 2004-06-29 15:21:19 gluck Exp $"
*
*
* who       when         what
* --------  -----------  -------------------------------------------------------
* gluck     09-Jun-2004  Created
*
*
* IMPORTANT:
* To make AUTOMATIC DOCUMENTATION GENERATION by doxygen, you have to insert
* your code documentation (about file, functions, define, enumeration, ...) as
* shown below, in the special documentation blocks, adding or deleting markers
* as needed.
* Nevertheless, you also have to comment the code as usually.  For more
* informations, you can report to Programming Standards (JRA4-PRO-2000-0001),
* or doxygen documentation.
* 
* IMPORTANT
* Each time (except in certain case) there is a brief and a detailed
* description, THE BRIEF DESCRIPTION IS A UNIQUE SENTENCE, WHICH ENDS AT THE
* FIRST DOT FOLLOWED BY A SPACE OR A NEWLINE.
*
* REMARKS
* The documentation below, shows some possibilities of doxygen. The general
* format of this documentation is recommended to make the documentation
* easily. Some documentation lines are strongly recommended to get rapidly a
* quite good documentation. Some others are optinonal, depending on the need.
* They will be pointed out with the word OPTIONAL.
*
*******************************************************************************/

/**
 * \file
 * Brief description of the header file, which ends at this dot.
 * 
 * OPTIONAL detailed description of the c main file follows here.
 *
 * \b Files:
 * \n
 * OPTIONAL. If files are used, for each one, name, and usage
 * description.
 * \li \e \<fileName1\> :  usage description of fileName1
 * \li \e \<fileName2\> :  usage description of fileName2
 *
 * \n
 * \b Environment:
 * \n
 * OPTIONAL. If needed, environmental variables accessed by the program. For each
 * variable, name, and usage description, as below.
 * \li \e \<envVar1\> :  usage description of envVar1
 * \li \e \<envVar2\> :  usage description of envVar2
 * 
 * \warning OPTIONAL. Warning if any (software requirements, ...)
 *
 * \n
 * \b Code \b Example:
 * \n
 * OPTIONAL. Code example if needed
 *
 * \code
 * Insert your code example here
 * \endcode
 *
 * \sa OPTIONAL. See also section, in which you can refer other documented
 * entities. Doxygen will create the link automatically.
 * 
 * \bug OPTIONAL. Bugs list if it exists. You can make a list with the \li
 * marker, like below.
 * \bug \li For example, description of the first bug
 * \bug \li For example, description of the second bug
 * 
 * \todo \li  OPTIONAL. Things to forsee list, if needed. You can make a list
 * with the \li marker, like in the Files section above. For example, add
 * modcProc3.
 * 
 * \n
 */

/* 
 * System Headers 
 */
#include <stdio.h>
#include <string.h>

/*
 * MCS Headers 
 */
#include "mcs.h"
#include "log.h"

/*
 * Local Headers 
 */
#include "modcPrivate.h"
#include "modc.h"


/* 
 * Local variables
 */

static mcsBYTES8 string;    /**< Brief description of the variable, ends at 
                              this dot. OPTIONAL detailed description of 
                              the variable follows here. */


/* 
 * Local functions declaration 
 */

/* IMPORTANT : doxygen extracted documentation for local functions is located
 * just below in this file. It's why a normal documentation block is used here
 * with a brief description (just to know a little about the function) and NOT
 * A DOXYGEN DOCUMENTATION BLOCK 
 */

/* Brief description of the procedure */
static mcsCOMPL_STAT modcSub(mcsINT8 x, mcsINT8 y);


/* 
 * Local functions definition
 */

/**
 * Brief description of the function, which ends at this dot.
 *
 * OPTIONAL detailed description of the function follows here.
 *
 * \param x description of parameter x. In the example, a number.
 * \param y description of parameter y. In the example, a number.
 * 
 * \n
 * \return Description of the return value. In the example, SUCCESS or FAILURE. 
 *
 * \n
 */
static mcsCOMPL_STAT modcSub(mcsINT8 x, mcsINT8 y)
{
    mcsINT8 z;
    z=x-y;
    printf("%d - %d = %d\n", x, y, z);

    return SUCCESS;
}


/*
 * Public functions definition
 */

/**
 * Brief description of the function, which ends at this dot.
 *
 * OPTIONAL detailed description of the function follows here.
 *
 * \param a description of parameter a. In the example, a string.
 * \param b description of parameter b. In the example, an integer.
 * 
 * \n
 * \return Description of the return value. In the example, SUCCESS or FAILURE. 
 *
 * \b Files:
 * \n
 * OPTIONAL. If files are used, for each one, name, and usage
 * description.
 * \li \e \<fileName1\> :  usage description of fileName1
 * \li \e \<fileName2\> :  usage description of fileName2
 *
 * \b Environment:
 * \n
 * OPTIONAL. If needed, environmental variables accessed by the program. For each
 * variable, name, and usage description, as below.
 * \li \e \<envVar1\> :  usage description of envVar1
 * \li \e \<envVar2\> :  usage description of envVar2
 * 
 * \warning OPTIONAL. Warning if any (software requirements, ...). For example
 * parameter b is a 8 bit integer.
 *
 * \n
 * \b Code \b Example: 
 * \n
 * OPTIONAL. Code example if needed
 *
 * \code
 * Insert your code example here
 * \endcode
 *
 * \sa OPTIONAL. See also section, in witch you can refer other documented
 * entities. Doxygen will create the link automatically. For example,
 * modcProc2, modcColor
 * 
 * \bug OPTIONAL. Bugs list if it exists. You can make a list with the \li
 * marker, like below.
 * \bug \li For example, the function doesn't deal with special characters.
 * \bug \li For example, the function crashes if the buffer size is greater
 * than 1024.
 *
 * \todo OPTIONAL. Things to forsee list, if needed. You can make a list with
 * the \li marker, like below.
 * \todo \li For example, correct bugs.
 * \todo \li For example, extend the function with file1 and file 2.
 *
 * \n
 */
mcsCOMPL_STAT modcProc1(mcsBYTES32 a, mcsINT8 b)
{
    /* Print out parameters */
    printf("a = %s and b = %i\n", a, b);
    /* Use the local function modcSub */
    mcsINT8 integer = 3;
    printf ("\t=> call modcSub local function : ");
    if (modcSub(b, integer) == FAILURE)
    {
        printf ("ERROR modcSub\n");
    }
        
    return SUCCESS;
}


/**
 * Brief description of the function, which ends at this dot.
 *
 * OPTIONAL detailed description of the function follows here.
 *
 * \param c description of parameter c. In the example, a string.
 * 
 * \n
 * \return Description of the return value. In the example, SUCCESS or FAILURE. 
 *
 * \sa OPTIONAL. See also section, in witch you can refer other documented
 * entities. Doxygen will create the link automatically. For example
 * modcProc1.
 * 
 * \n
 */
mcsCOMPL_STAT modcProc2(mcsBYTES8 c)
{
    /* Print out the parameter */
    printf("\t- c = %s\n", c);
    /* Print out the local variable string */
    strcpy (string, "modcProc2");
    printf ("\t- modcProc.c local variable string = %s\n", string);
        
    return SUCCESS;
}


/*___oOo___*/
