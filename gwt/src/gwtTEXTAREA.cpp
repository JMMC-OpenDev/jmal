/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: gwtTEXTAREA.cpp,v 1.3 2006-05-11 13:04:55 mella Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/02/04 10:01:47  mella
 * replace userchain by text for attribute value
 *
 * Revision 1.1  2005/01/27 18:09:35  gzins
 * Renamed .C to .cpp
 * Added CVS loh as modification history.
 *
 * mella     16-Sep-2004  Created
 * gzins     10-Dec-2004  Fixed doxygen warning
 *
 ******************************************************************************/

/**
 * \file
 * Definition of gwtTEXTAREA class.
 */

static char *rcsId __attribute__ ((unused)) ="@(#) $Id: gwtTEXTAREA.cpp,v 1.3 2006-05-11 13:04:55 mella Exp $";

/* 
 * System Headers 
 */
#include <iostream>
#include <sstream>
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
#include "gwtTEXTAREA.h"
#include "gwtPrivate.h"


/*
 * Class constructor
 */

/** 
 * Constructs a gwtTEXTAREA.
 */
gwtTEXTAREA::gwtTEXTAREA()
{
     logExtDbg("gwtTEXTAREA::gwtTEXTAREA()");
}

/** 
 * Constructs a gwtTEXTAREA.
 * \param text the text of the widget
 * \param rows the number of rows
 * \param columns the number of columns
 * \param help the help of the widget
 */
gwtTEXTAREA::gwtTEXTAREA(string text, int rows, int columns, string help)
{
     logExtDbg("gwtTEXTAREA::gwtTEXTAREA()");
     SetText(text);
     SetHelp(help);
     SetRows(rows);
     SetColumns(columns);
}

/*
 * Class destructor
 */
gwtTEXTAREA::~gwtTEXTAREA()
{
     logExtDbg("gwtTEXTAREA::~gwtTEXTAREA()");
}

/*
 * Public methods
 */

string gwtTEXTAREA::GetXmlBlock()
{
    logExtDbg("gwtTEXTAREA::GetXmlBlock()");
    string s;
    s.append("<SHOW");
    AppendXmlAttributes(s);
    s.append(">");
    s.append("</SHOW>");
    return s;
}

/**
 * Set the number of rows.
 *
 * \param rows the number of rows.
 *
 */
void gwtTEXTAREA::SetRows(int rows)
{
    logExtDbg("gwtTEXTAREA::SetRows()"); 
    ostringstream s;
    s << rows;
    SetXmlAttribute("rows",s.str()); 
}

/**
 * Set the number of columns.
 *
 * \param columns the number of columns.
 *
 */
void gwtTEXTAREA::SetColumns(int columns)
{
    logExtDbg("gwtTEXTAREA::SetColumns()"); 
    ostringstream s;
    s << columns;
    
    SetXmlAttribute("columns",s.str()); 
}

/**
 * Set the text value of the widget.
 *
 * \param text the text value of the widget.
 *
 * \todo try to make it dynamically changeable.
 */
void gwtTEXTAREA::SetText(string text)
{
    logExtDbg("gwtTEXTAREA::SetText()"); 
    SetXmlAttribute("text",text); 
}

/**
 * Get the text value of the widget.
 *
 * \return the text value of the widget.
 */
string gwtTEXTAREA::GetText()
{
    logExtDbg("gwtTEXTAREA::GetText()"); 

    return GetXmlAttribute("text"); 
}

/**
 * Assign a new value to the widget.
 *
 * \param value the new value of the widget assigned by the user. 
 *
 */
void gwtTEXTAREA::Changed(string value){
    logExtDbg("gwtTEXTAREA::Changed()"); 
    SetXmlAttribute("text",value); 
}

/*
 * Protected methods
 */
void gwtTEXTAREA::SetWidgetId(string id)
{
    gwtWIDGET::SetWidgetId(id);
    SetXmlAttribute("variable",id);
}



/*
 * Private methods
 */



/*___oOo___*/