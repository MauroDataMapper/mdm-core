import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue

ReferenceDataValue rdv = referenceDataValue as ReferenceDataValue

'mdm:referenceDataValue' {
    layout '/catalogueItem/_export.gml', catalogueItem: rdv

    'mdm:rowNumber'(rdv.rowNumber)
    'mdm:value'(rdv.value)
    
    layout '/referenceDataElement/export.gml', referenceDataElement: rdv.referenceDataElement
}