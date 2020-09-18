import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

import java.time.OffsetDateTime

Terminology export = terminology as Terminology

'mdm:terminology' {
    layout '/catalogueItem/_export.gml', catalogueItem: export

    if (export.author) 'mdm:author' {yield export.author}
    if (export.organisation) 'mdm:organisation' {yield export.organisation}

    'mdm:documentationVersion' export.documentationVersion.toString()

    'mdm:finalised'(export.finalised)
     if (export.finalised) 'mdm:dateFinalised'(convertDate(export.dateFinalised))

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
        if (export.getAllTermRelationships()) {
            'mdm:termRelationships' {
                export.getAllTermRelationships().each {tr ->
                    layout '/termRelationship/export.gml', termRelationship: tr
                }
            }
        }
    }     

}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
