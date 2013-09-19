<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml"/>
<xsl:template match="DOC">
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:dc="http://purl.org/dc/elements/1.1/"
  xmlns:dcterms="http://purl.org/dc/terms/" 
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" >
  <rdf:Description rdf:about="urn:x-current-document:/">
    <dcterms:language>
    <xsl:choose>
    <xsl:when test="./META_LANGUE[text()='D']">de</xsl:when>
    <xsl:when test="./META_LANGUE[text()='F']">fr</xsl:when>
    <xsl:when test="./META_LANGUE[text()='I']">it</xsl:when>
    <xsl:when test="./META_LANGUE[text()='E']">en</xsl:when>
    </xsl:choose>
    </dcterms:language>
    <dcterms:title>
    <xsl:value-of select="./META_OBJET"/>
    </dcterms:title>
    <dcterms:identifier>
    <xsl:value-of select="./META_NUMERO_DOSSIER"/>
    </dcterms:identifier>
    <dcterms:creator>
    <xsl:value-of select="./META_NOM_COUR"/>   
    </dcterms:creator>
    <dc:subject>
        <rdf:Description>
            <xsl:attribute name="rdf:about">http://example.bger.ch/terms/<xsl:value-of select="translate(./META_N_MATIERE, ' ','')"/></xsl:attribute>
            <rdfs:label>
                <xsl:value-of select="./META_N_MATIERE"/>
            </rdfs:label>
        </rdf:Description>     
    </dc:subject>
    <dc:area>
    <xsl:value-of select="./META_NOM_BRANCHE"/>     
    </dc:area>
    <dc:date>
    <xsl:value-of select="./META_DATE_DECISION"/>     
    </dc:date>    
  </rdf:Description>
</rdf:RDF>
</xsl:template>
</xsl:stylesheet>
