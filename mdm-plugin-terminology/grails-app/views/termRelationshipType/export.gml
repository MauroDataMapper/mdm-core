import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType

import java.time.OffsetDateTime

TermRelationshipType export = termRelationshipType as TermRelationshipType

'mdm:termRelationshipType' {
    'mdm:id'(export.id)
    'mdm:label' {yield export.label}

    if (export.description) 'mdm:description' {yield export.description}

    'mdm:displayLabel' {yield export.displayLabel}

    'mdm:lastUpdated'(convertDate(export.lastUpdated))

    'mdm:parentalRelationship' {yield export.parentalRelationship}
    'mdm:childRelationship' {yield export.childRelationship}
}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
