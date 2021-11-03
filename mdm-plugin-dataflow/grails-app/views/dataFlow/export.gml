import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow

DataFlow df = dataFlow as DataFlow


'mdm:dataFlow' {
    layout '/catalogueItem/_export.gml', catalogueItem: df
    'mdm:type'(df.modelType)

    if (df.source) {
        'mdm:source' {
            layout '/dataModel/_export.gml', dataModel: df.source
        }
    }

    if (df.target) {
        'mdm:target' {
            layout '/dataModel/_export.gml', dataModel: df.target
        }
    }    
    
    if (df.dataClassComponents) {
        'mdm:dataClassComponents' {
            df.dataClassComponents.sort{it.label}.each {dcc ->
                layout '/dataClassComponent/_export.gml', dataClassComponent: dcc 
            }
        }
    }
}
