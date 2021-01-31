import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    layout '/dataModel/export.gml', dataModel: exportModel.modelExportMap.dataModel
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
