import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

model {
    DataModel dataModel
}

DataModel dm = dataModel as DataModel

'mdm:dataModel' {
    layout '/catalogueItem/_export.gml', catalogueItem: dm

    if (dm.author) 'mdm:author' {yield dm.author}
    if (dm.organisation) 'mdm:organisation' {yield dm.organisation}

    'mdm:documentationVersion' dm.documentationVersion.toString()

    'mdm:finalised'(dm.finalised)
    if (dm.finalised) 'mdm:dateFinalised'(OffsetDateTimeConverter.toString(dm.dateFinalised))
    if (dm.modelVersion) 'mdm:modelVersion' dm.modelVersion.toString()
    else 'mdm:branchName' dm.branchName

    layout '/authority/exportAuthority.gml', authority: dm.authority

    'mdm:type'(dm.modelType)
    if (dm.dataTypes) {
        'mdm:dataTypes' {
            dm.dataTypes.each {dt ->
                layout '/dataType/export.gml', dataType: dt
            }
        }
    }
    if (dm.childDataClasses) {
        'mdm:childDataClasses' {
            dm.childDataClasses.each {dc ->
                layout '/dataClass/export.gml', dataClass: dc
            }
        }
    }
}