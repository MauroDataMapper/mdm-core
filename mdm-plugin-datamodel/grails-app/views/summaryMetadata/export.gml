import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata

model {
    SummaryMetadata summaryMetadata
}

SummaryMetadata sm = summaryMetadata as SummaryMetadata

'mdm:summaryMetadata' {
    if (sm.id) 'mdm:id' sm.id
    if (sm.createdBy) 'mdm:createdBy' {yield sm.createdBy}
    if (sm.lastUpdated) 'mdm:lastUpdated' sm.lastUpdated
    'mdm:label' {yield sm.label}
    if (sm.description) 'mdm:description' {yield sm.description}
    'mdm:summaryMetadataType' sm.summaryMetadataType
    if (sm.summaryMetadataReports) {
        'mdm:summaryMetadataReports' {
            sm.summaryMetadataReports.each {report ->
                layout '/summaryMetadataReport/export.gml', summaryMetadataReport: report
            }
        }
    }
}