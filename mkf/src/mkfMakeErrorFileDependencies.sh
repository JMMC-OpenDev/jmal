#! /bin/sh
#*******************************************************************************
# JMMC project
#
# "@(#) $Id: mkfMakeErrorFileDependencies.sh,v 1.1 2004-09-10 13:40:57 gzins Exp $" 
#
# who       when         what
# --------  --------     ----------------------------------------------
# gzins     26-Aug-2004  Adapted from vltMakeIndexFilesDependencies script

#************************************************************************
#   NAME
#   mkfMakeErrorFileDependencies - generate the error include file.
# 
#   SYNOPSIS
#
#   mkfMakeErrorFileDependencies
#
# 
#   DESCRIPTION
#   Utility used by mkfMakefile to generate the error include file.
#
#   It is not intended to be used as a standalone command.
#
#   FILES
#
#   ENVIRONMENT
#
#   RETURN VALUES
#
#   SEE ALSO 
#   mkfMakefile
#
#   BUGS    
#
#----------------------------------------------------------------------

# if Linux: disable the bash builtin command 'echo'.
if [ "`uname`" = "Linux" ]
then
    enable -n echo
fi

if [ "${1}" != "" ]
then
    echo "ERROR: mkfMakeErrorFileDependencies does not accept parameters" >&2
    exit 1
fi

echo "# Dependency file for error include file"
echo "# Created automatically by mkfMakeErrorFileDependencies -  `date '+%d.%m.%y %T'`"
echo "# DO NOT EDIT THIS FILE"

if [ "`which errXmlToH 2>/dev/null`" != "" ]
then
    XML2H=`which errXmlToH`
else
    if [ -f ./errXmlToH ]
    then
        XML2H="./errXmlToH"
    fi
fi

list=""
if [ -d ../errors -a  "`ls ../errors/*Errors.xml 2>/dev/null`" != "" ]
then
    if [ "$XML2H" == "" ]
    then
        echo "ERROR: mkfMakeErrorFileDependencies no errXmlToH in PATH" >&2
        exit 1
    fi

    if [ "`ls ../errors/*Errors.xml 2>/dev/null`" != "" ]
    then 
        for file in `ls ../errors/*Errors.xml 2>/dev/null`
        do
            if [ -s $file ]
            then 
                # Get the base file mane
                name=`basename $file .xml`
                list="$list $name"
            fi
        done

        # output the make-rules:
        target="do_ERRORS:"

        for name in $list
        do
            XML_FILE="../errors/${name}.xml"
            H_FILE="../include/${name}.h"

            target="$target $H_FILE"

            echo "$H_FILE : $XML_FILE"
            echo "	@echo \"== Generating error include file: $H_FILE\""
            echo "	-\$(AT) \$(RM) $H_FILE"
            echo "	-\$(AT) sh $XML2H $XML_FILE $H_FILE >/dev/null"
        done

        echo "$target"
    fi
fi 

# if either no good files or no directory have been found, create an empty target
if [ "$list" = "" ]
then 
    echo "do_ERRORS:"
    echo "	-\$(AT)echo \"\""        
fi


exit 0
#
# ___oOo___
