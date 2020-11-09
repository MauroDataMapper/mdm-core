import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    layout '/terminology/export.gml', terminology: exportModel.modelExportMap.terminology
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
