package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import groovy.xml.Namespace

class ExportModel {

    Namespace xmlNamespace
    Map modelExportMap
    ExportMetadata exportMetadata
    String modelExportTemplatePath
    String exportModelType
    Namespace modelXmlNamespace

    ExportModel(String version) {
        xmlNamespace = new Namespace("http://maurodatamapper.com/export/${version}", 'xmlns:exp')
    }

    Map getXmlNamespaces() {
        Map ns = [:]
        ns.put(xmlNamespace.prefix, xmlNamespace.uri)
        ns.put(modelXmlNamespace.prefix, modelXmlNamespace.uri)
        ns
    }
}

