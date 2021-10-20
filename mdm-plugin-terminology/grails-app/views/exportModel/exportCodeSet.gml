import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

Object export = exportModel.modelExportMap.export

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    if (export instanceof List) {
        'mdm:codeSets' {
            export.each { layout '/codeSet/export.gml', codeSet: it }
        }
    } else {
        layout '/codeSet/export.gml', codeSet: export
    }
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}
