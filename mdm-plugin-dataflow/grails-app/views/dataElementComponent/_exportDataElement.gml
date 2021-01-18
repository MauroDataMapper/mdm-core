import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

DataElement de = dataElement as DataElement

'mdm:dataElement' {
    'mdm:label' de.label
    layout '/dataClassComponent/_exportDataClass.gml', dataClass: de.dataClass, ns: 'mdm'
}
