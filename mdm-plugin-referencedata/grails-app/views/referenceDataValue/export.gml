import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue

ReferenceDataValue rdv = referenceDataValue as ReferenceDataValue

'mdm:referenceDataValue' {
    'mdm:id'(rdv.id)
    'mdm:rowNumber'(rdv.rowNumber)
    'mdm:value'(rdv.value)
    
    layout '/referenceDataElement/export.gml', referenceDataElement: rdv.referenceDataElement
}