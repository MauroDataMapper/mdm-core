import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType

ReferenceDataType dt = referenceDataType as ReferenceDataType

'mdm:referenceDataType' {
    layout '/modelItem/_export_ordered.gml', modelItem: dt

    'mdm:domainType'(dt.domainType)

    if (dt.instanceOf(ReferenceEnumerationType)) {
        ReferenceEnumerationType et = dt as ReferenceEnumerationType
        'mdm:enumerationValues' {
            et.referenceEnumerationValues.sort().each {ev ->
                layout '/referenceEnumerationValue/export.gml', referenceEnumerationValue: ev
            }
        }
    } else if (dt.instanceOf(ReferencePrimitiveType)) {
        ReferencePrimitiveType pt = dt as ReferencePrimitiveType
        if (pt.units) 'mdm:units' {yield pt.units}
    }
}