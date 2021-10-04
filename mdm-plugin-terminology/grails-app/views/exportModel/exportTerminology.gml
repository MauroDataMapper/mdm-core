import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

List<Object> exportList = exportModel.modelExportMap.export

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    if (exportList.size() > 1) {
        'mdm:terminologies' {
            exportList.each {
                layout '/terminology/export.gml', terminology: it
            }
        }
    } else {
        layout '/terminology/export.gml', terminology: exportList.pop()
    }
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
