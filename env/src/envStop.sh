#!/bin/bash
#*******************************************************************************
# JMMC project
#
# "@(#) $Id: envStop.sh,v 1.6 2005-12-06 11:44:17 gzins Exp $"
#
# History
# -------
# $Log: not supported by cvs2svn $
# Revision 1.5  2005/02/28 14:25:00  lafrasse
# Reversed changelog order
#
# Revision 1.4  2005/02/13 17:26:51  gzins
# Minor changes in documentation
#
# Revision 1.3  2005/02/13 16:53:13  gzins
# Added CVS log as modification history
#
# lafrasse  25-Jan-2005  Added MCSENV label management (for the default MCSENV)
# lafrasse  21-Jan-2005  Created
#
#*******************************************************************************

#/**
# \file
# Stop the environment (the current environment or the one passed as argument).
#
# \synopsis
# \<envStop\> [\e \<MCS_environment_name\>]
#
# \param MCS_environment_name : the MCS environment to stop
#
# \n
# \env
# MCSENV variable defines the current MCS environment name.
# 
# */

# If we got more than 1 argument
if [ "$#" -gt 1 ]
then
    # Display the script usage
    echo -e "Usage: $0 [environment name]" 
    exit 1
else
    # If we got an enviromnent name
    if [ "$#" == 1 ]
    then 
        # Over-ride the user MCSENV environment with the given one
        MCSENV=$1
    fi
fi

# If MCSENV is defined
if [ "$MCSENV" != "" ]
then
    # Set LABEL accordinaly
    LABEL="$MCSENV"
else
    # Set LABEL to "default"
    LABEL="default"
fi

# Check whether the msgManager is already running or not
answer=`msgSendCommand msgManager PING "" 2>&1 > /dev/null`

# If the environment is running
if [ "$?" == 0 ]
then
    echo "Stopping '$LABEL' environment ..."
    procList="msgManager"
    for proc in ${procList}
    do
        answer=`msgSendCommand msgManager EXIT "" 2>&1 > /dev/null`
	sleep 1
	stat=`ps -f -u $USER | grep $proc | grep -v grep | wc -l`
    	if [ $stat == 0 ]
    	then
           echo "   '$proc' stopped" 
        else
           echo "   '$proc' failed" >&2
    	fi
    done
    echo "done."
else
    echo "'$LABEL' environment is not running"
fi

exit 0;

#___oOo___
