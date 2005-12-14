<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text" omit-xml-declaration="yes" indent="no"/>

    <xsl:variable name="autoGeneratedC"><xsl:text>#
#            
# This file has been automatically generated
#
</xsl:text>
</xsl:variable>

    <!-- ********************************************************* -->
    <!-- This TEMPLATE is the main part                            -->
    <!-- it calls one template for each cmd nodes                  -->
    <!-- INPUT : the module name                                   -->
    <!-- OUTPUT: the generated files  usind <MNEMO>.cfg             -->
    <!-- ********************************************************* -->
    <xsl:template match="/">
 
        <xsl:for-each select="/cmd">
            <xsl:document href="{concat(./mnemonic,'.cfg')}" method="text">
            <xsl:value-of select="$autoGeneratedC"/>
            <xsl:call-template name="cmdTest">
            </xsl:call-template>
        </xsl:document> 

        </xsl:for-each>
    </xsl:template>
        
    <!-- ********************************************************* -->
    <!-- This TEMPLATE is the main part to generate the test cfg   -->
    <!-- file from a given command node.                           -->
    <!-- ********************************************************* -->
    <xsl:template name="cmdTest">
#
# This is a sample test file for the command : <xsl:value-of select="mnemonic"/>
#

[DEFAULT]
command=<xsl:value-of select="mnemonic"/>

<xsl:for-each select="./params/param">
# parameter: <xsl:value-of select="./name"/>
# <xsl:value-of select="./desc"/>
<xsl:value-of select="'&#10;'"/>
    <xsl:if test="./@optional"># is optional<xsl:value-of select="'&#10;'"/></xsl:if>
    <xsl:if test="boolean(./defaultValue)"># has a default value:<xsl:value-of select="./defaultValue"/> <xsl:value-of select="'&#10;'"/> </xsl:if>
    <xsl:if test="not(./@optional)"> <xsl:value-of select="./name"/>=<xsl:value-of select="./defaultValue"/> </xsl:if>
    <xsl:if test="./@optional">#<xsl:value-of select="./name"/>=<xsl:value-of select="./defaultValue"/> <xsl:value-of select="'&#10;'"/>
    </xsl:if>
    <xsl:value-of select="'&#10;'"/>
</xsl:for-each>

</xsl:template>
    
</xsl:stylesheet>
