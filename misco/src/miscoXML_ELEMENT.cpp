/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: miscoXML_ELEMENT.cpp,v 1.7 2010-01-28 13:04:02 mella Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.6  2007/02/22 12:23:03  gzins
 * Fixed parameter documentation error
 *
 * Revision 1.5  2006/10/17 11:27:19  mella
 * Add typed attribute and content
 *
 * Revision 1.4  2006/10/17 08:20:53  mella
 * Use standard ToXml() instead of previous ToString()
 *
 * Revision 1.3  2006/10/16 11:41:07  swmgr
 * typo ...
 *
 * Revision 1.2  2006/10/16 11:34:57  mella
 * First functionnal revision
 *
 * Revision 1.1  2006/10/16 07:34:22  mella
 * Class miscoXML_ELEMENT created
 *
 ******************************************************************************/

/**
 * @file
 *  Definition of miscoXML_ELEMENT class.
 */

static char *rcsId __attribute__ ((unused)) = "@(#) $Id: miscoXML_ELEMENT.cpp,v 1.7 2010-01-28 13:04:02 mella Exp $"; 

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
#include "miscoXML_ELEMENT.h"
#include "miscoPrivate.h"

/**
 * Class constructor
 */
miscoXML_ELEMENT::miscoXML_ELEMENT(string name)
{
    _name=name;
}


/**
 * Class destructor
 */
miscoXML_ELEMENT::~miscoXML_ELEMENT()
{
}

/*
 * Public methods
 */

/**
 * Create one new attribute. If one attribute already exist, its content will be
 * replaced.
 * 
 * @param attributeName the attribute name
 * @param attributeValue the attribute value
 *
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddAttribute(string attributeName,
                                             string attributeValue)
{
    logTrace("miscoXML_ELEMENT::AddAttributeElement()");
    _attributes.erase(attributeName);
    _attributes.insert(make_pair(attributeName, attributeValue));
    return mcsSUCCESS;
}

/**
 * Create one new attribute. If one attribute already exist, its content will be
 * replaced.
 * 
 * @param attributeName the attribute name
 * @param attributeValue the attribute value as double
 * 
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddAttribute(string attributeName,
                                             mcsDOUBLE attributeValue)
{
    char buffer[128];
    logTrace("miscoXML_ELEMENT::AddAttributeElement()");
    sprintf(buffer,"%f",attributeValue);
    _attributes.erase(attributeName);
    _attributes.insert(make_pair(attributeName, buffer));
    return mcsSUCCESS;
}

/**
 * Create one new attribute. If one attribute already exist, its content will be
 * replaced.
 * 
 * @param attributeName the attribute name
 * @param attributeValue the attribute value as logical
 * 
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddAttribute(string attributeName,
                                             mcsLOGICAL attributeValue)
{
    logTrace("miscoXML_ELEMENT::AddAttributeElement()");
    _attributes.erase(attributeName);
    if(attributeValue==mcsTRUE)
    {   
        _attributes.insert(make_pair(attributeName, "true"));
    }
    else
    {
        _attributes.insert(make_pair(attributeName, "false"));
    }
    return mcsSUCCESS;
}

/**
 * Add the given element as child 
 * 
 * @param element new child to add
 * 
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddElement(miscoXML_ELEMENT * element)
{
    logTrace("miscoXML_ELEMENT::AddElement()");
    _elements.push_back(element);
    return mcsSUCCESS;
}

/**
 * Append the given string to the element's content. 
 * 
 * @param content new contetn to append.
 * 
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddContent(string content)
{
    logTrace("miscoXML_ELEMENT::AddContent()");
    _content.append(content);
    return mcsSUCCESS;
}

/**
 * Append true or false value to the content depending of the given argument.
 *
 * @param content mcsTRUE or mcsFALSE.
 *
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddContent(mcsLOGICAL content)
{
    logTrace("miscoXML_ELEMENT::AddContent()");
    if (content == mcsTRUE )
    {     
        _content.append("true");
    }
    else
    {
        _content.append("false");
    }
    return mcsSUCCESS;
}

/**
 * Append a numerical value to the content depending of the given argument.
 * 
 * @param content numerical value
 * 
 * @return always mcsSUCCESS
 */
mcsCOMPL_STAT miscoXML_ELEMENT::AddContent(mcsDOUBLE content)
{
    logTrace("miscoXML_ELEMENT::AddContent()");
    char buffer[128];
    sprintf(buffer,"%f",content);
    _content.append(buffer);
    return mcsSUCCESS;
}


/**
 * Return the xml stringified  representation of the element.
 * 
 * @return the xml representation.
 */
string miscoXML_ELEMENT::ToXml()
{
    logTrace("miscoXML_ELEMENT::ToXml()");

    string xmlStr ;
   
    // Append starting markup
    xmlStr.append("<");
    xmlStr.append(_name);  

    // Append attributes
    std::map<string, string>::iterator i = _attributes.begin();
    while( i != _attributes.end() )
    {
        // append only if value is not empty
        if(! i->second.empty() )
        {
            xmlStr.append(" ");
            xmlStr.append(i->first);
            xmlStr.append("=\"");
            xmlStr.append(i->second);
            xmlStr.append("\"");
        }
        i++;
    }
    xmlStr.append(">");
  
    // Append children elements content
    std::list<miscoXML_ELEMENT*>::iterator j = _elements.begin();
    while(j != _elements.end())
    {
        xmlStr.append((*j)->ToXml());
        j++;
    }
    
    // Append content
    xmlStr.append(_content);
    
    // Append closing markup
    xmlStr.append("</");
    xmlStr.append(_name);    
    xmlStr.append(">");
    
    return xmlStr;
}

/*
 * Protected methods
 */


/*
 * Private methods
 */


/*___oOo___*/