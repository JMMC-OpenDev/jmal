#!/bin/bash
#*******************************************************************************
# JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
#*******************************************************************************
#   NAME
#   mkfMakeTclLibDependencies - create the makefile to buid a Tcl/TK library
# 
#   SYNOPSIS
#
#        mkfMakeTclLibDependencies <libName> <objectList>
# 
#   DESCRIPTION
#   Utility used by mkfMakefile to create the makefile containing the
#   dependencies of a Tcl/Tk library. They are:
#          - each object file (scripts)
#          - the Makefile 
#
#     ../lib/lib<libName>.tcl: ... <obj-i>.tcl ...
#     <TAB>   -mkfMakeTclLib  <libName> $(<libName>_OBJECTS)
#
#   The rules is written to standard output.
#
#   <libName>     The name of the target. The file is ../lib/lib<libName>.tcl
#
#   <objectList>  The list of the script files in the src/ directory.
#                 (Without neither directory nor .tcl suffix)
#
#   FILES
#   $MCSROOT/include/mkfMakefile   
#
#   ENVIRONMENT
#
#   RETURN VALUES
#
#   SEE ALSO 
#   mkfMakefile, Makefile, (GNU) make
#
#   BUGS    
#

if [ $# -ne 2 ]
then
    echo "" >&2
    echo "Usage:  mkfMakeTclLibDependencies <libName> <object List>" >&2
    echo "" >&2
    exit 1
fi

libName=$1
objectList=$2

echo "# Dependency file for Tcl library: ${libName}"
echo "# Created automatically by mkfMakeTclLibDependencies -  `date '+%d.%m.%y %T'`"
echo "# DO NOT EDIT THIS FILE"

#
# define the dependency file dependent to the Makefile
echo "../object/${libName}.dat: Makefile"
echo ""

#
# define PHONY the target, so make does not try to make the target
# when no object are specified. (due to the fact that the same list of objects is
# used to build the list of exe both to be produced and to be installed).
echo ".PHONY: ${libName} "

#
# if the list of objects is not empty, the rule to build the Tcl/Tk-lib is written on output.
if [ "${objectList}" != "" ]
then
    #
    # prepare the list of all objects (full filename)
    for member in ${objectList}
    do
        oList="${oList} ${member}.tcl"
    done

    #
    # create a target with the <name> of the Tcl-lib (make <name>)
    echo "${libName}: ../lib/lib${libName}.tcl"
    echo ""

    #
    # output the rule to build the Tcl-lib file 
    # (for the timebeing, no dependency on the TCL libraries)
    echo "../lib/lib${libName}.tcl: ${oList} Makefile"
    echo "	@echo \"== Making TCL library: \$(@)\" "
    echo "	\$(AT)mkfMakeTclLib \"\$(TCL_CHECKER)\"  \"${libName}\" \"\$(${libName}_OBJECTS)\" \$(OUTPUT)"

else
    echo "# ${libName}_OBJECTS is not defined. Nothing to do here."
    echo "# Makefile should define the action for target  '${libName}:'"
fi

#
# ___oOo___
