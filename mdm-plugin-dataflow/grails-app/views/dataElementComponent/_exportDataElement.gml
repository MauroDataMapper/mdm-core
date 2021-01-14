import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

DataElement de = dataElement as DataElement

'mdm:dataElement' {
    'mdm:label' de.label
    //dataClass de.dataClass.label
    layout '/dataClassComponent/_exportDataClass.gml', dataClass: de.dataClass, ns: 'mdm'
    //dataType de.dataType.label
}
