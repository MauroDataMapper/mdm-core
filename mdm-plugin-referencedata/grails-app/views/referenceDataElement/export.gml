import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement

ReferenceDataElement rde = referenceDataElement as ReferenceDataElement

'mdm:referenceDataElement' {
    layout '/catalogueItem/_export.gml', catalogueItem: rde
    'mdm:columnNumber'(rde.columnNumber)
    layout '/referenceDataType/export.gml', referenceDataType: rde.referenceDataType
    if (rde.maxMultiplicity != null) 'mdm:maxMultiplicity'(rde.maxMultiplicity)
    if (rde.minMultiplicity != null) 'mdm:minMultiplicity'(rde.minMultiplicity)
}
