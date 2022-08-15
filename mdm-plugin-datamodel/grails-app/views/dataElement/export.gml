import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

DataElement de = dataElement as DataElement

'mdm:dataElement' {
    layout '/modelItem/_export_ordered.gml', modelItem: de
    if (de.summaryMetadata) {
        'mdm:summaryMetadata' {
            de.summaryMetadata.each {sm ->
                layout '/summaryMetadata/export.gml', summaryMetadata: sm
            }
        }
    }

    layout '/dataType/export.gml', dataType: de.dataType
    if (de.maxMultiplicity != null) 'mdm:maxMultiplicity'(de.maxMultiplicity)
    if (de.minMultiplicity != null) 'mdm:minMultiplicity'(de.minMultiplicity)
}
