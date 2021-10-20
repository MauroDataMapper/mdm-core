import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

Object export = exportModel.modelExportMap.export

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    if (export instanceof List) {
        'mdm:terminologies' {
            export.each { layout '/terminology/export.gml', terminology: it }
        }
    } else {
        layout '/terminology/export.gml', terminology: export
    }
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
