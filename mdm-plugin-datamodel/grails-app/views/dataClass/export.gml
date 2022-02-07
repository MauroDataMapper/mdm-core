import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

DataClass dc = dataClass as DataClass

'mdm:dataClass' {
    layout '/catalogueItem/_export.gml', catalogueItem: dc

    if (dc.maxMultiplicity != null) 'mdm:maxMultiplicity'(dc.maxMultiplicity)
    if (dc.minMultiplicity != null) 'mdm:minMultiplicity'(dc.minMultiplicity)
    if (dc.dataClasses) {
        'mdm:dataClasses' {
            dc.dataClasses.each {child ->
                layout '/dataClass/export.gml', dataClass: child
            }
        }
    }
    if (dc.dataElements) {
        'mdm:dataElements' {
            dc.dataElements.each {child ->
                layout '/dataElement/export.gml', dataElement: child
            }
        }
    }
    if (dc.summaryMetadata) {
        'mdm:summaryMetadata' {
            dc.summaryMetadata.each {sm ->
                layout '/summaryMetadata/export.gml', summaryMetadata: sm
            }
        }
    }
}