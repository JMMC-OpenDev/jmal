#!/bin/bash
#*******************************************************************************
# JMMC project
#
# "@(#) $Id: mcscfgGenerateEnvList.sh,v 1.4 2005-12-06 07:08:33 gzins Exp $"
#
# History
# -------
# $Log: not supported by cvs2svn $
# Revision 1.3  2005/12/02 14:02:26  mella
# Force to remove previous envList file
#
# Revision 1.2  2005/12/02 12:54:09  mella
# Take into account the hostname
#
# Revision 1.1  2005/12/02 12:17:28  mella
# First revision
#
#*******************************************************************************

#/**
# @file
# Generate a MCS env list from the given general xml description file.
#
# @synopsis
# mcscfgGenerateEnvList <mcscfgEnvList.xml> <mcscfgEnvList> 
#
# @usedfiles
# @filename mcscfgGenerateEnvList :  transformation rules
#
# 
# */



if [ $# -ne 2 ]
then
    echo "Usage: $0 <mcsEnvList.xml> <mcsEnvList>"
    exit 1
fi

GIVENHOSTNAME=$(hostname)
XMLMCSENVLIST=$1
MCSENVLIST=$2

# remove previous file
rm -f $MCSENVLIST

# Search xslt file
if [ -e ../config/mcscfgGenerateEnvList.xsl ]
then
    XSLTFILE="../config/mcscfgGenerateEnvList.xsl"
#elif [ -e "$INTROOT/config/mcscfgGenerateEnvList.xsl" ]
#then
#    XSLTFILE="$INTROOT/config/mcscfgGenerateEnvList.xsl" ]
#elif [ -e "$MCSROOT/config/mcscfgGenerateEnvList.xsl" ]
#then
#    XSLTFILE="$MCSROOT/config/mcscfgGenerateEnvList.xsl" ]
else
    echo "Can't find file 'mcscfgGenerateEnvList.xsl'"
    exit 1
fi

echo "#  This file has been automatically generated on $(date)" >> $MCSENVLIST
echo "#  for hostname '$GIVENHOSTNAME' by $0" >> $MCSENVLIST
echo "#  to change some entry in this file , please:"  >> $MCSENVLIST
echo "#   - go into the mcscfg module"  >> $MCSENVLIST
echo "#   - edit $XMLMCSENVLIST"  >> $MCSENVLIST
echo "#   - execute make all install"  >> $MCSENVLIST
echo "#" >> $MCSENVLIST
echo "# !!!!!!!!!!!  DO NOT MANUALLY EDIT THIS FILE  !!!!!!!!!!!" >> $MCSENVLIST
echo "#" >> $MCSENVLIST
echo "# Please use ${0##*/} with your modified configuration file " >> $MCSENVLIST
echo "#" >> $MCSENVLIST
echo "#" >> $MCSENVLIST
echo "#" >> $MCSENVLIST

xsltproc --stringparam hostname "$GIVENHOSTNAME" $XSLTFILE "$1" >> $MCSENVLIST

#___oOo___
