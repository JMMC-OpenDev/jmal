This folder contains all java libraries used by the JMAL module.

All of them should be listed in the modules's ApplicationData.xml


You should be able to use the following xmlstarlet tips to generate your jnlp
xml sel -t -m "//package" -c "./preceding-sibling::comment()[1]"  -e jar -a href -o "@SHARED@/" -v "./@jars" -b -b -n -n fr/jmmc/jmal/resource/ApplicationData.xml
by this way they will be properly be handled by the jmcsDeployJnlp script.


Runtime Libraries (JNLP) are described in the following files :
../src/fr/jmmc/jmal/resource/ApplicationData.xml 
../src/JmalLibs.jnlp

-rw-r--r--. 1 bourgesl laogsite  32404 21 févr. 09:27 jafama.jar
-rw-r--r--. 1 bourgesl laogsite  34234  7 sept.  2011 sptype.jar

