import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

DataClass dc = dataClass as DataClass

'mdm:dataClass' {
    layout '/modelItem/_export_ordered.gml', modelItem: dc
    if (dc.summaryMetadata) {
        'mdm:summaryMetadata' {
            dc.summaryMetadata.each {sm ->
                layout '/summaryMetadata/export.gml', summaryMetadata: sm
            }
        }
    }

    if (dc.maxMultiplicity != null) 'mdm:maxMultiplicity'(dc.maxMultiplicity)
    if (dc.minMultiplicity != null) 'mdm:minMultiplicity'(dc.minMultiplicity)
    if (dc.dataClasses) {
        'mdm:dataClasses' {
            dc.dataClasses.sort().each {child ->
                layout '/dataClass/export.gml', dataClass: child
            }
        }
    }
    if (dc.dataElements) {
        'mdm:dataElements' {
            dc.dataElements.sort().each {child ->
                layout '/dataElement/export.gml', dataElement: child
            }
        }
    }
}