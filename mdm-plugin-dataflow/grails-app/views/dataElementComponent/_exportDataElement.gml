import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.path.Path

DataElement de = dataElement as DataElement

'mdm:dataElement' {
    'mdm:label' de.label
    'mdm:path' Path.toPathPrefix(de,'dm')

    layout '/dataClassComponent/_exportDataClass.gml', dataClass: de.dataClass, ns: 'mdm'
}
