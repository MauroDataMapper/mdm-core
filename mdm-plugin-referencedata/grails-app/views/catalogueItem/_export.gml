import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

CatalogueItem export = catalogueItem as CatalogueItem
Boolean add = addContents == null ? true : addContents


if (export.id) 'mdm:id'(export.id)
'mdm:label' {yield export.label}

if (add) {
    if (export.description) 'mdm:description' {yield export.description}

    if (export.aliasesString) {
        'mdm:aliases' {
            export.getAliases().each {al ->
                'mdm:alias' al
            }
        }
    }

    if (export.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(export.lastUpdated))

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