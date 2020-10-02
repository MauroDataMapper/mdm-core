import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement

ReferenceDataElement de = referenceDataElement as ReferenceDataElement

'mdm:referenceDataElement' {
    layout '/catalogueItem/_export.gml', catalogueItem: de
    layout '/referenceDataType/export.gml', referenceDataType: de.referenceDataType
    if (de.maxMultiplicity != null) 'mdm:maxMultiplicity'(de.maxMultiplicity)
    if (de.minMultiplicity != null) 'mdm:minMultiplicity'(de.minMultiplicity)
}
