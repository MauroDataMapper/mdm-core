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
    if (rdm.modelVersion) 'mdm:modelVersion' rdm.modelVersion.toString()

    layout '/authority/export.gml', authority: rdm.authority, ns: 'mdm'

    if (rdm.referenceDataTypes) {
        'mdm:referenceDataTypes' {
            rdm.referenceDataTypes.each {rdt ->
                layout '/referenceDataType/export.gml', referenceDataType: rdt
            }
        }
    }

    if (rdm.referenceDataElements) {
        'mdm:referenceDataElements' {
            rdm.referenceDataElements.each {rde ->
                layout '/referenceDataElement/export.gml', referenceDataElement: rde
            }
        }
    }

    if (rdm.referenceDataValues) {
        'mdm:referenceDataValues' {
            //Sort by row number and reference data element lable, purely for reproduceability in testing
            rdm.referenceDataValues.sort{a,b -> 
            if (a.rowNumber == b.rowNumber) {
                a.referenceDataElement.label <=> b.referenceDataElement.label
            } else {
                a.rowNumber <=> b.rowNumber
            }}.each {rdv ->
                layout '/referenceDataValue/export.gml', referenceDataValue: rdv
            }
        }
    }
}
