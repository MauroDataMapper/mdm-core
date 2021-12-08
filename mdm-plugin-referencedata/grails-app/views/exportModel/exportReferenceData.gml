import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    layout '/referenceDataModel/export.gml', referenceDataModel: exportModel.modelExportMap.export.pop()
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
