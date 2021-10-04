import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

List<Object> exportList = exportModel.modelExportMap.export

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    if (exportList.size() > 1) {
        'mdm:codeSets' {
            exportList.each {
                layout '/codeSet/export.gml', codeSet: it
            }
        }
    } else {
        layout '/codeSet/export.gml', codeSet: exportList.pop()
    }
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
