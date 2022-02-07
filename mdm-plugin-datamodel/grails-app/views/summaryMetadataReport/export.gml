import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport

SummaryMetadataReport smr = summaryMetadataReport as SummaryMetadataReport

'mdm:summaryMetadataReport' {
    if (smr.id) 'mdm:id' smr.id
    if (smr.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(smr.lastUpdated))
    if (smr.reportDate) 'mdm:reportDate'(OffsetDateTimeConverter.toString(smr.reportDate))
    'mdm:reportValue' {yield smr.reportValue}
}