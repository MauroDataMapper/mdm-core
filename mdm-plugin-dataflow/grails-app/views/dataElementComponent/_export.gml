import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataElementComponent

DataElementComponent dec = dataElementComponent as DataElementComponent

'mdm:dataElementComponent' {
    layout '/catalogueItem/_export.gml', catalogueItem: dec

    definition(dec.definition)

    if (dec.sourceDataElements) {
        'mdm:sourceDataElements' {
            dec.sourceDataElements.each {de ->
                layout '/dataElementComponent/_exportDataElement.gml', dataElement: de
            }
        }
    }

    if (dec.targetDataElements) {
        'mdm:targetDataElements' {
            dec.targetDataElements.each {de ->
                layout '/dataElementComponent/_exportDataElement.gml', dataElement: de
            }
        }
    }    

}