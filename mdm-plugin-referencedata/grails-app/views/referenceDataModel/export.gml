import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel

ReferenceDataModel rdm = referenceDataModel as ReferenceDataModel

'mdm:referenceDataModel' {
    layout '/catalogueItem/_export.gml', catalogueItem: rdm

    'mdm:type'(rdm.modelType)

    if (rdm.author) 'mdm:author' {yield rdm.author}
    if (rdm.organisation) 'mdm:organisation' {yield rdm.organisation}

    'mdm:documentationVersion' rdm.documentationVersion.toString()

    'mdm:finalised'(rdm.finalised)
    if (rdm.finalised) 'mdm:dateFinalised'(OffsetDateTimeConverter.toString(rdm.dateFinalised))

    layout '/authority/export.gml', authority: rdm.authority, ns: 'mdm'

    if (rdm.dataTypes) {
        'mdm:dataTypes' {
            rdm.dataTypes.each {dt ->
                layout '/referenceDataType/export.gml', referenceDataType: dt
            }
        }
    }
}