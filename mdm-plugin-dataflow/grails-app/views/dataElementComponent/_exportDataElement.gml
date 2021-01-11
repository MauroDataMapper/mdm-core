import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

DataElement de = dataElement as DataElement

'mdm:dataElement' {
    label de.label
    dataClass de.dataClass.label
    dataType de.dataType.label
}
