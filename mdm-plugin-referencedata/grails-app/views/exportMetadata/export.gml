import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

ExportMetadata em = exportMetadata as ExportMetadata

'exp:exportMetadata' {
    'exp:exportedBy' {yield em.exportedBy}
    'exp:exportedOn'(em.exportDate)
    'exp:exporter' {
        'exp:namespace'(em.namespace)
        'exp:name'(em.name)
        'exp:version'(em.version)
    }
}