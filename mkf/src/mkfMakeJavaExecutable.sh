#! /bin/sh
#*******************************************************************************
# JMMC project
#
# "@(#) $Id: mkfMakeJavaExecutable.sh,v 1.5 2009-10-06 14:22:02 mella Exp $" 
#
# History
# -------
# $Log: not supported by cvs2svn $
# Revision 1.4  2007/02/15 09:19:33  mella
# Use now /home/users/mella/.java/logging.properties file if any
#
# Revision 1.3  2005/02/15 08:40:15  gzins
# Added CVS log as file modification history
#
# gzins     26-Aug-2004  Adapted from VLT
#
#************************************************************************
#   NAME
#   mkfMakeJavaExecutable - create the java program 
# 
#   SYNOPSIS
#   mkfMakeJavaExecutable <javaName> <class>
# 
#   DESCRIPTION
#   Utility used by mkfMakefile to create a Java program. The generated
#   program sets the class path, using the mkfMakeJavaClasspath, before
#   launching the Java application.
#   If one logging file exists, the java property java.util.logging.config.file
#   is set.
#   It is not intended to be used as a standalone command.
#
#   <javaName>     The name of the java program. The output is named
#                  ../bin/<javaName>
#   <class>        The name of the class to be invoked.
#   <args>         The list of args to be given when lauching Java application  
#
#   FILES
#   $MCSROOT/include/mkfMakefile   
#   $HOME/.java/logging.properties   
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
#----------------------------------------------------------------------

javaName=$1
class=$2
destDir=../bin/

echo -n > ${javaName}

addLine()
{
    echo -e "$*" >> ${javaName}
}
addLine "#!/bin/bash"
addLine "# Java program file: ${javaName}"
addLine "# Created automatically by mkfMakeJavaExecutable -  `date '+%d.%m.%y %T'`"
addLine "# DO NOT EDIT THIS FILE"
addLine ""
addLine "# Generate the class path for Java"
addLine "CLASSPATH=\`mkfMakeJavaClasspath\`"

addLine "if [ -n \"$HOME/.java/logging.properties\" ]"
addLine "then"
addLine "    java -Djava.util.logging.config.file=$HOME/.java/logging.properties -classpath \$CLASSPATH ${class} \"\$@\""
addLine "else"
addLine "    java -classpath \$CLASSPATH ${class} \"\$@\""
addLine "fi"

chmod +x ${javaName}

# ___oOo___