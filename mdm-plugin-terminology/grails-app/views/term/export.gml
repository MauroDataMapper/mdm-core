import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import java.time.OffsetDateTime

Term export = term as Term

'mdm:term' {
    'mdm:id'(export.id)
    'mdm:code' {yield export.code}
    'mdm:definition' {yield export.definition}

    if (export.url) 'mdm:url' {yield export.url}
    if (export.description) 'mdm:description' {yield export.description}

    if(export.depth) 'mdm:depth' {yield export.depth}

    'mdm:lastUpdated'(convertDate(export.lastUpdated))


    if (export.aliasesString) 'mdm:aliases' {
        export.getAliases().each {al ->
            'mdm:alias' al
        }
    }


    if (export.classifiers) {
        'mdm:classifiers' {
            export.classifiers.each {cl ->
                layout '/classifier/export.gml', classifier: cl, ns: 'mdm'
            }
        }
    }
    if (export.metadata) {
        'mdm:metadata' {
            export.metadata.each {md ->
                layout '/metadata/export.gml', metadata: md, ns: 'mdm'
            }
        }
    }
    if (export.annotations) {
        'mdm:annotations' {
            export.annotations.each {ann ->
                layout '/annotation/export.gml', annotation: ann, ns: 'mdm'
            }
        }
    }

}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
