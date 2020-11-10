import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue

ReferenceEnumerationValue ev = referenceEnumerationValue as ReferenceEnumerationValue

'mdm:referenceEnumerationValue' {

    if (ev.id) 'mdm:id'(ev.id)
    'mdm:index' ev.order
    'mdm:key' {yield ev.key}
    'mdm:value' {yield ev.value}
    if (ev.category) 'mdm:category' {yield ev.category}

    if (ev.aliasesString) {
        'mdm:aliases' {
            ev.getAliases().each {al ->
                'mdm:alias' al
            }
        }
    }

    if (ev.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(ev.lastUpdated))

    if (ev.classifiers) {
        'mdm:classifiers' {
            ev.classifiers.each {cl ->
                layout '/classifier/ev.gml', classifier: cl, ns: 'mdm'
            }
        }
    }
    if (ev.metadata) {
        'mdm:metadata' {
            ev.metadata.each {md ->
                layout '/metadata/ev.gml', metadata: md, ns: 'mdm'
            }
        }
    }
    if (ev.annotations) {
        'mdm:annotations' {
            ev.annotations.each {ann ->
                layout '/annotation/ev.gml', annotation: ann, ns: 'mdm'
            }
        }
    }

}