import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet

import java.time.OffsetDateTime

CodeSet export = codeSet as CodeSet

'mdm:codeSet' {
    layout '/catalogueItem/_export.gml', catalogueItem: export

    if (export.author) 'mdm:author' {yield export.author}
    if (export.organisation) 'mdm:organisation' {yield export.organisation}

    'mdm:documentationVersion' export.documentationVersion.toString()

    'mdm:finalised'(export.finalised)
    if (export.finalised) 'mdm:dateFinalised'(convertDate(export.dateFinalised))
    if (export.modelVersion) 'mdm:modelVersion' export.modelVersion.toString()

    layout '/authority/export.gml', authority: export.authority, ns: 'mdm'

    if (export.terms) {
        'mdm:termPaths' {
            export.terms.each {t ->
                'mdm:termPath'(t.termPath())
            }
        }
    }

}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
