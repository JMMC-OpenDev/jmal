#!/bin/bash
#*******************************************************************************
# JMMC project
#
# "@(#) $Id: mcsinsInstallMcs.sh,v 1.2 2004-12-08 18:43:49 swmgr Exp $"
#
# who       when         what
# --------  -----------  -------------------------------------------------------
# gzins     04-Dec-2004  Created
# gzins     08-Dec-2004  Moved from mkf module
#                        Added installation of env module
# gzins     08-Dec-2004  Added installation of gwt module
#
#*******************************************************************************
#   NAME 
#   mcsinsInstallMcs - Install/Update MCS modules 
# 
#   SYNOPSIS
#   mcsinsInstallMcs
# 
#   DESCRIPTION
#   This command retreives all the modules belonging to MCS from the CVS
#   repository and install them.
#
#   FILES
#
#   ENVIRONMENT
#
#   RETURN VALUES
#
#   CAUTIONS
#
#   EXAMPLES
#
#   SEE ALSO
#
#   BUGS     
#
#-------------------------------------------------------------------------------
#

# Get the current directory
dir=$PWD

# Propose the user to continue or abort
echo -e "\n-> All the MCS modules will be installed and updated from"
echo -e "   '$dir' directory\n"
echo -e "   Press enter to continue or ^C to abort "
read choice

# List of MCS modules
mcs_modules="mkf ctoo mcs log err misc modc modcpp fnd env cmd msg evh gwt"

# Retrieve modules from CVS repository
cd $dir
cvs co $mcs_modules
if [ $? != 0 ]
then
    echo -e "\nERROR: 'cvs co $mcs_modules' failed ... \n"; 
    exit 1;
fi

# Compile and install them
for mod in $mcs_modules; do
    cd $dir
    cd $mod/src 
    if [ $? != 0 ]
    then
        echo -e "\nERROR: 'cd $mod/src' failed ...\n";
        exit 1
    fi
    make all man install 
    if [ $? != 0 ]
    then
        echo -e "\nERROR: 'make all man install' in $mod failed ...\n";
        exit 1
    fi
    echo ""
done
#___oOo___
