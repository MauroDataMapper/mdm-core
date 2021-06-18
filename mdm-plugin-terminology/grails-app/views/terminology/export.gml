import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship

import java.time.OffsetDateTime

Terminology export = terminology as Terminology

'mdm:terminology' {
    layout '/catalogueItem/_export.gml', catalogueItem: export

    if (export.author) 'mdm:author' {yield export.author}
    if (export.organisation) 'mdm:organisation' {yield export.organisation}

    'mdm:documentationVersion' export.documentationVersion.toString()

    'mdm:finalised'(export.finalised)
     if (export.finalised) 'mdm:dateFinalised'(convertDate(export.dateFinalised))
     if (export.modelVersion) 'mdm:modelVersion' export.modelVersion.toString()

     layout '/authority/export.gml', authority: export.authority, ns: 'mdm'

    if (export.termRelationshipTypes) {
        'mdm:termRelationshipTypes' {
            export.termRelationshipTypes.each {trt ->
                layout '/termRelationshipType/export.gml', termRelationshipType: trt
            }
        }
    }
    
    if (export.terms) {
        'mdm:terms' {
            export.terms.each {t ->
                layout '/term/export.gml', term: t
            }
        }
        List<TermRelationship> relationships = export.getAllTermRelationships().sort {a, b ->
            int r = a.sourceTerm.code <=> b.sourceTerm.code
            if (r == 0) r = a.targetTerm.code <=> b.targetTerm.code
            r
        }
        if (relationships) {
            'mdm:termRelationships' {
                relationships.each {tr ->
                    layout '/termRelationship/export.gml', termRelationship: tr
                }
            }
        }
    }     

}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
