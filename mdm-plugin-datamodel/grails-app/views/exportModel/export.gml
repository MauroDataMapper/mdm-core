import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

List<Object> exportList = exportModel.modelExportMap.export

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    if (exportList.size() > 1) {
        'mdm:dataModels' {
            exportList.each {
                layout '/dataModel/export.gml', dataModel: it
            }
        }
    } else {
        layout '/dataModel/export.gml', dataModel: exportList.pop()
    }
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
