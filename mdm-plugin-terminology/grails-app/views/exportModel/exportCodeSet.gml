import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    layout '/codeSet/export.gml', codeSet: exportModel.modelExportMap.codeSet
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
