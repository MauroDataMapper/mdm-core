import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

DataType dt = dataType as DataType

'mdm:dataType' {
    layout '/catalogueItem/_export.gml', catalogueItem: dt

    'mdm:domainType'(dt.domainType)

    if (dt.instanceOf(EnumerationType)) {
        EnumerationType et = dt as EnumerationType
        'mdm:enumerationValues' {
            et.enumerationValues.each {ev ->
                layout '/enumerationValue/export.gml', enumerationValue: ev
            }
        }
    } else if (dt.instanceOf(ReferenceType)) {
        ReferenceType rt = dt as ReferenceType
        'mdm:referenceClass' {
            layout '/dataClass/exportReference.gml', dataClass: rt.referenceClass
        }
    } else if (dt.instanceOf(PrimitiveType)) {
        PrimitiveType pt = dt as PrimitiveType
        if (pt.units) 'mdm:units' {yield pt.units}
    }
}