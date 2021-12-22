import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

Object export = exportModel.exportMap.export

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    if (export instanceof List) {
        'mdm:dataModels' {
            export.each { layout '/dataModel/export.gml', dataModel: it }
        }
    } else {
        layout '/dataModel/export.gml', dataModel: export
    }
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
