import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.path.Path

DataModel dm = dataModel as DataModel
Path path = path as Path

'mdm:id'(dm.id)
'mdm:label'(dm.label)
'mdm:path'(path.from(dm))
'mdm:type'(dm.modelType)
