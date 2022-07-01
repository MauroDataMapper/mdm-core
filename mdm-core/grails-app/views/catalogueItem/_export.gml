import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

import org.springframework.core.Ordered

model {
    CatalogueItem catalogueItem
    Boolean addContents
}
CatalogueItem export = catalogueItem as CatalogueItem
Boolean add = addContents == null ? true : addContents


if (export.id) 'mdm:id'(export.id)
if (export instanceof Ordered) 'mdm:index'(((Ordered) export).order)
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
                layout '/classifier/exportClassifier.gml', classifier: cl
            }
        }
    }

    if (export.metadata) {
        'mdm:metadata' {
            export.metadata.each {md ->
                layout '/metadata/exportMetadata.gml', metadata: md
            }
        }
    }

    if (export.annotations) {
        'mdm:annotations' {
            export.annotations.each {ann ->
                layout '/annotation/exportAnnotation.gml', annotation: ann
            }
        }
    }
}