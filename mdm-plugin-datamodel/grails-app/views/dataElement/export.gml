import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

DataElement de = dataElement as DataElement

'mdm:dataElement' {
    layout '/catalogueItem/_export.gml', catalogueItem: de
    layout '/dataType/export.gml', dataType: de.dataType
    if (de.maxMultiplicity != null) 'mdm:maxMultiplicity'(de.maxMultiplicity)
    if (de.minMultiplicity != null) 'mdm:minMultiplicity'(de.minMultiplicity)
}
