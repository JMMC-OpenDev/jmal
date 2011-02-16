/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: cmdPARAM.cpp,v 1.18 2006-05-11 13:04:09 mella Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.17  2006/01/04 12:36:19  mella
 * Corrected bug with default values
 *
 * Revision 1.16  2005/04/17 16:18:59  mella
 * Improve documentation
 *
 * Revision 1.15  2005/04/11 12:33:25  scetre
 * Revue : changed documentation
 *
 * Revision 1.14  2005/04/11 12:20:08  scetre
 * Revue : changed documentation
 *
 * Revision 1.13  2005/03/04 17:04:11  scetre
 * Defined errors related to parameter out of range as user error
 *
 * Revision 1.12  2005/03/03 16:13:43  lafrasse
 * Removed a repetitove 'unit' cout in cmdParam::GetHelp()
 *
 * Revision 1.11  2005/03/03 10:46:47  gzins
 * Fixed bug in range check of float parameters
 *
 * Revision 1.10  2005/02/28 13:38:18  lafrasse
 * Added minimum and maximum parameter values information output in cmdPARAM:GetHelp()
 *
 * Revision 1.9  2005/02/28 11:05:59  scetre
 * Removed unused printf
 *
 * Revision 1.8  2005/02/27 19:44:17  gzins
 * Implemented parameter value range check
 *
 * Revision 1.7  2005/02/27 09:27:41  gzins
 * Improved error handling
 *
 * Revision 1.6  2005/02/15 11:02:48  gzins
 * Changed SUCCESS/FAILURE to mcsSUCCESS/mcsFAILURE
 *
 * Revision 1.5  2005/02/15 10:58:58  gzins
 * Added CVS log as file modification history
 *
 * mella     15-Nov-2004  Created
 * gzins     15-Dec-2004  Added error handling
 * lafrasse  01-Feb-2005  Refined GetHelp output format and added type
 *                        management
 *
 ******************************************************************************/

/**
 * \file
 * cmdPARAM class definition.
 */

static char *rcsId __attribute__ ((unused)) ="@(#) $Id: cmdPARAM.cpp,v 1.18 2006-05-11 13:04:09 mella Exp $";

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
#include "cmd.h"
#include "cmdPARAM.h"
#include "cmdPrivate.h"
#include "cmdErrors.h"

/**
 * Class constructor
 *
 * \param name the name of the parameter.
 * \param desc description of the parameter 
 * \param type type of the parrameter
 * \param unit unit of the parameter
 * \param optional logical to know if the parameter is optional or not
 */
cmdPARAM::cmdPARAM(string name, string desc, string type, string unit,
                   mcsLOGICAL optional)
{
    logExtDbg("cmdPARAM::cmdPARAM");
    // copy each value in the appropriated object element
    _name = name;
    _desc = desc;
    _type = type;
    _unit = unit;
    _optional = optional;
}


/**
 * Class destructor
 *
 */
cmdPARAM::~cmdPARAM()
{
    logExtDbg("cmdPARAM::~cmdPARAM");
}

/*
 * Public methods
 */

/**
 *  Get the name of the parameter.
 *
 *  \returns the string containing the name.
 */
string cmdPARAM::GetName(void)
{
    logExtDbg("cmdPARAM::GetName()");
    return _name;
}

/**
 *  Get the description of the parameter.
 *
 *  \returns the string containing the description or an empty string if it does
 *  not exist.
 */
string cmdPARAM::GetDesc(void)
{
    logExtDbg("cmdPARAM::GetDesc()");
    return _desc;
}

/**
 *  Get the type of the parameter.
 *
 *  \returns the string containing the type or an empty string if it does not
 *  exist.
 */
string cmdPARAM::GetType(void)
{
    logExtDbg("cmdPARAM::GetType()");
    return _type;
}

/**
 *  Get the unit of the parameter.
 *
 *  \returns the string containing the unit or an empty string if it does not
 *  exist.
 */
string cmdPARAM::GetUnit(void)
{
    logExtDbg("cmdPARAM::GetUnit()");
    return _unit;
}

/**
 *  Return if the parameter is optional.
 *
 *  \returns mcsTRUE or mcsFALSE
 */
mcsLOGICAL cmdPARAM::IsOptional(void)
{
    logExtDbg("cmdPARAM::IsOptional()");
    return _optional;
}

/**
 *  Return the help of the parameter.
 *
 *  \returns the help string
 */
string cmdPARAM::GetHelp(void)
{
    logExtDbg("cmdPARAM::GetHelp()");

    string help;
    help.append("\t-");
    help.append(_name);

    // If there is a given type
    if (! _type.empty())
    {
        // add in the help
        help.append(" <");
        help.append(_type);
        help.append(">");
    }

    // If there is a defaultValue
    if (HasDefaultValue())
    {
        // add in the help
        help.append(" (default = '");
        help.append(_defaultValue);
        help.append("')");
    }

    // If there is a given unit
    if (! _unit.empty())
    {
        // add in the help
        help.append(" (unit = '");
        help.append(_unit);
        help.append("')");
    }

    // If there is a given minimum and maximum value
    if ((! _minValue.empty()) && (! _maxValue.empty()))
    {
        // add in the help
        help.append(" (range from '");
        help.append(_minValue);
        help.append("' to '");
        help.append(_maxValue);
        help.append("')");
    }
    else if (! _minValue.empty()) // If there is only a given minimum
    {
        // add in the help
        help.append(" (minimum value of '");
        help.append(_minValue);
        help.append("')");
    }
    else if (! _maxValue.empty()) // If there is only a given maximum
    {
        // add in the help
        help.append(" (maximum value of '");
        help.append(_maxValue);
        help.append("')");
    }

    // If there is a given description
    if (! _desc.empty())
    {
        // add in the help
        help.append("\n\t\t");
        help.append(_desc);
    }
    else
    {
        // add no description in the help
        help.append("\n\t\tNo description");
    }

    help.append("\n");

    return help;
}



/**
 * Return if the parameter has got a user value.
 *
 *  \returns mcsTRUE or mcsFALSE
 */
mcsLOGICAL cmdPARAM::IsDefined(void)
{
    logExtDbg("cmdPARAM::IsDefined()");
    if (_userValue.empty())
    {
        return mcsFALSE;
    }
    else
    {
        return mcsTRUE;
    }
}

/**
 *  Get the user value of the parameter.
 *
 *  \returns the string containing the user value.
 */
string cmdPARAM::GetUserValue(void)
{
    logExtDbg("cmdPARAM::GetUserValue()");
    return _userValue;
}

/**
 * Get the user value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetUserValue(mcsINT32 *value)
{
    logExtDbg("cmdPARAM::GetUserValue()");
    if (sscanf(_userValue.data(), "%d", value) != 1)
    {
        errAdd(cmdERR_INTEGER_VALUE, _userValue.data(), _name.data());
        return mcsFAILURE;
    }
    return mcsSUCCESS;
}

/**
 * Get the user value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetUserValue(mcsDOUBLE *value)
{
    logExtDbg("cmdPARAM::GetUserValue()");
    if (sscanf(_userValue.data(), "%lf", value) != 1)
    {
        errAdd(cmdERR_DOUBLE_VALUE, _userValue.data(), _name.data());
        return mcsFAILURE;
    }
    return mcsSUCCESS;
}

/**
 * Get the user value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetUserValue(mcsLOGICAL *value)
{
    logExtDbg("cmdPARAM::GetUserValue()");
    if ((_userValue.compare("1") == 0) || (_userValue.compare("true") == 0))
    {
        *value = mcsTRUE;
    }
    else if ((_userValue.compare("0") == 0) || (_userValue.compare("false")==0))
    {
        *value = mcsFALSE;
    }
    else
    {
        errAdd(cmdERR_LOGICAL_VALUE, _userValue.data(), _name.data());
        return mcsFAILURE;
    }
    return mcsSUCCESS;
}

/**
 * Get the user value.
 *
 * \param value the storage data pointer
 *
 *  \returns always mcsSUCCESS
 */
mcsCOMPL_STAT cmdPARAM::GetUserValue(char **value)
{
    logExtDbg("cmdPARAM::GetUserValue()");

    *value = (char *)_userValue.data();
    return mcsSUCCESS;
}

/**
 * Set the user value of the parameter. 
 *
 * \warning This method must be called only by cmdCOMMAND.
 * The value is extracted from the parameter line.
 *
 * \param value  the new user value.
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::SetUserValue(string value)
{
    logExtDbg("cmdPARAM::SetUserValue()");

    // Check value according to the parameter type
    if (CheckValueType(value) == mcsFAILURE)
    {
        return mcsFAILURE;
    }

    // Check value range
    if (CheckValueRange(value) == mcsFAILURE)
    {
        return mcsFAILURE;
    }

    _userValue=value;

    return mcsSUCCESS;
}



/**
 * Return if the parameter has got a default value.  
 *
 *  \returns mcsTRUE or mcsFALSE
 */
mcsLOGICAL cmdPARAM::HasDefaultValue(void)
{
    logExtDbg("cmdPARAM::HasDefaultValue()");
    if (_defaultValue.empty())
    {
        return mcsFALSE;
    }
    else
    {
        return mcsTRUE;
    }
}

/**
 *  Get the default value of the parameter.
 *
 *  \returns the string containing the user value.
 */
string cmdPARAM::GetDefaultValue(void)
{
    logExtDbg("cmdPARAM::GetDefaultValue()");
    return _defaultValue;
}

/**
 * Get the default value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetDefaultValue(mcsINT32 *value)
{
    logExtDbg("cmdPARAM::GetDefaultValue()");
    if (sscanf (_defaultValue.data(), "%d", value) != 1)
    {
        errAdd(cmdERR_INTEGER_VALUE, _defaultValue.data(), _name.data());
        return mcsFAILURE;
    }
    return mcsSUCCESS;    
}

/**
 * Get the default value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetDefaultValue(mcsDOUBLE *value)
{
    logExtDbg("cmdPARAM::GetDefaultValue()");
    if (sscanf (_defaultValue.data(), "%lf", value) != 1)
    {
        errAdd(cmdERR_DOUBLE_VALUE, _defaultValue.data(), _name.data());
        return mcsFAILURE;
    }
    return mcsSUCCESS;
}

/**
 * Get the default value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetDefaultValue(mcsLOGICAL *value)
{
    logExtDbg("cmdPARAM::GetDefaultValue()");
    if ((_defaultValue.compare("1") == 0) || (_defaultValue.compare("true") == 0))
    {
        *value = mcsTRUE;
    }
    else if ((_defaultValue.compare("0") == 0) || (_defaultValue.compare("false")==0))
    {
        *value = mcsFALSE;
    }
    else
    {
        errAdd(cmdERR_LOGICAL_VALUE, _userValue.data(), _name.data());
        return mcsFAILURE;
    }
    return mcsSUCCESS;
}

/**
 * Get the default value.
 *
 * \param value the storage data pointer
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::GetDefaultValue(char **value)
{
    logExtDbg("cmdPARAM::GetDefaultValue()");
    // cast as a char pointer  the return of .data() method of _defaultValue
    *value = (char*)_defaultValue.data();
    return mcsSUCCESS;
}

/**
 * Set the default value of the parameter.
 * \warning This method must be called only by cmdCOMMAND.
 *
 * The value is extracted from the cdf file.
 *
 * \param value  the new default value.
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::SetDefaultValue(string value)
{
    logExtDbg("cmdPARAM::SetDefaultValue()");

    // Check value according to the parameter type
    if (CheckValueType(value) == mcsFAILURE)
    {
        errAdd(cmdERR_DEFAULTVALUE_FORMAT, _name.data());
        return mcsFAILURE;
    }

    // Check value range
    if (CheckValueRange(value) == mcsFAILURE)
    {
        return mcsFAILURE;
    }

    _defaultValue=value;
    
    return mcsSUCCESS;
}




/**
 * Set the min value of the parameter. This method must be called only
 * by cmdCOMMAND. The value is extracted from the cdf file.
 *
 * \param value  the new min value.
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::SetMinValue(string value)
{
    logExtDbg("cmdPARAM::SetMinValue()");

    // Check value according to the parameter type
    if (CheckValueType(value) == mcsFAILURE)
    {
        errAdd(cmdERR_DEFAULTVALUE_FORMAT, _name.data());
        return mcsFAILURE;
    }

    _minValue=value;
    
    return mcsSUCCESS;
}

/**
 * Set the max value of the parameter. 
 * \warning This method must be called only by cmdCOMMAND.
 *
 * The value is extracted from the cdf file.
 *
 * \param value  the new max value.
 *
 *  \returns an MCS completion status code (mcsSUCCESS or mcsFAILURE)
 */
mcsCOMPL_STAT cmdPARAM::SetMaxValue(string value)
{
    logExtDbg("cmdPARAM::SetMaxValue()");

    // Check value according to the parameter type
    if (CheckValueType(value) == mcsFAILURE)
    {
        errAdd(cmdERR_DEFAULTVALUE_FORMAT, _name.data());
        return mcsFAILURE;
    }

    _maxValue=value;
    
    return mcsSUCCESS;
}



/*
 * Protected methods
 */
/**
 * Check the value, given as string, is consistent with parameter type.
 *
 * \param value parameter value.
 *
 *  \returns mcsSUCCESS the value is in conformity with parameter type,
 *  mcsFAILURE otherwise.
 */
mcsCOMPL_STAT cmdPARAM::CheckValueType(string value)
{
    logExtDbg("cmdPARAM::Method()");

    // if the type is string, no problem, return success
    if (_type == "string")
    {
        return mcsSUCCESS;
    }
    // if the type is integer
    else if (_type == "integer")
    {
        mcsINT32 iValue;
        // if it is possible to set to an integer value of the user
        // that's mean that the value is correct. Else (sscanf != 1),
        // return error
        if (sscanf (value.data(), "%d", &iValue) != 1)
        {
            errAdd(cmdERR_INTEGER_VALUE, value.data(), _name.data());
            return mcsFAILURE;
        }
        
        return mcsSUCCESS;
    }
    // if the type is double
    else if (_type == "double")
    {
        mcsDOUBLE dValue;
        // if it is possible to set to a double the value of the user
        // that's mean that the value is correct. Else (sscanf != 1),
        // return error
if (sscanf (value.data(), "%lf", &dValue) != 1)
        {
            errAdd(cmdERR_DOUBLE_VALUE, value.data(), _name.data());
            return mcsFAILURE;
        }
        
        return mcsSUCCESS;
    }
    // if the type is a logical
    else if (_type == "logical")
    {
        // Compare the user value to 1, 0, true or false
        if ((value.compare("1")     == 0) ||
            (value.compare("0")     == 0) ||
            (value.compare("true")  == 0) ||
            (value.compare("false") == 0))
        {
            errAdd(cmdERR_LOGICAL_VALUE, value.data(), _name.data());
            return mcsFAILURE;
        }

        return mcsSUCCESS;
    }
    return mcsSUCCESS;
}
 
/**
 * Check the value range.
 *
 * \param value parameter value.
 *
 *  \returns mcsFAILURE if the value is out of range, mcsSUCCESS otherwise.
 */
mcsCOMPL_STAT cmdPARAM::CheckValueRange(string value)
{
    logExtDbg("cmdPARAM::CheckValueRange()");

    // No check for logical parameter
    if (_type == "logical")
    {
        return mcsSUCCESS;
    }
    else if (_type == "string")
    {
        // Check min value
        if (_minValue.empty() == false)
        {
            if (value < _minValue)
            {
                errUserAdd(cmdERR_VALUE_OUT_OF_RANGE, value.data(),
                           _name.data(), "greater", _minValue.data());
                return mcsFAILURE;
            }
        }

        // Check max value
        if (_maxValue.empty() == false)
        {
            
            if (value > _minValue)
            {
                errUserAdd(cmdERR_VALUE_OUT_OF_RANGE, value.data(),
                           _name.data(), "less", _maxValue.data());
                return mcsFAILURE;
            }
        }
        
    }
    else if (_type == "integer")
    {
        mcsINT32 iValue;
        sscanf (value.data(), "%d", &iValue);
        
        // Check min value
        if (_minValue.empty() == false)
        {
            mcsINT32 minValue;
            sscanf (_minValue.data(), "%d", &minValue);
            if (iValue < minValue)
            {
                errUserAdd(cmdERR_VALUE_OUT_OF_RANGE, value.data(),
                           _name.data(), "greater", _minValue.data());
                return mcsFAILURE;
            }
        }

        // Check max value
        if (_maxValue.empty() == false)
        {
            mcsINT32 maxValue;
            sscanf (_maxValue.data(), "%d", &maxValue);
            
            if (iValue > maxValue)
            {
                errUserAdd(cmdERR_VALUE_OUT_OF_RANGE, value.data(),
                           _name.data(), "less", _maxValue.data());
                return mcsFAILURE;
            }
        }
        
    }
    else if (_type == "double")
    {
        mcsDOUBLE dValue;
        sscanf (value.data(), "%lf", &dValue);
        
        // Check min value
        if (_minValue.empty() == false)
        {
            mcsDOUBLE minValue;
            sscanf (_minValue.data(), "%lf", &minValue);
            if (dValue < minValue)
            {
                errUserAdd(cmdERR_VALUE_OUT_OF_RANGE, value.data(), 
                           _name.data(), "greater", _minValue.data());
                return mcsFAILURE;
            }
        }

        // Check max value
        if (_maxValue.empty() == false)
        {
            mcsDOUBLE maxValue;
            sscanf (_maxValue.data(), "%lf", &maxValue);
            
            if (dValue > maxValue)
            {
                errUserAdd(cmdERR_VALUE_OUT_OF_RANGE, value.data(),
                           _name.data(), "less", _maxValue.data());
                return mcsFAILURE;
            }
        }
        
    }
    return mcsSUCCESS;
}
 
/*
 * Private methods
 */

/*___oOo___*/