import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent

DataClassComponent dcc = dataClassComponent as DataClassComponent

'mdm:dataClassComponent' {
    layout '/catalogueItem/_export.gml', catalogueItem: dcc

    //label(dcc.label)
    definition(dcc.definition)
    
    if (dcc.sourceDataClasses) {
        'mdm:sourceDataClasses' {
            dcc.sourceDataClasses.each {dc ->
                layout '/dataClassComponent/_exportDataClass.gml', dataClass: dc
            }
        }
    }

    if (dcc.targetDataClasses) {
        'mdm:targetDataClasses' {
            dcc.targetDataClasses.each {dc ->
                layout '/dataClassComponent/_exportDataClass.gml', dataClass: dc
            }
        }
    }    

    if (dcc.dataElementComponents) {
        'mdm:dataElementComponents' {
            dcc.dataElementComponents.each {de ->
                layout '/dataElementComponent/_export.gml', dataElementComponent: de
            }
        }
    }
}