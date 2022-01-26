import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType

import java.time.OffsetDateTime

TermRelationshipType export = termRelationshipType as TermRelationshipType

'mdm:termRelationshipType' {
    layout '/catalogueItem/_export.gml', catalogueItem: export
    'mdm:displayLabel' {yield export.displayLabel}
    'mdm:parentalRelationship' {yield export.parentalRelationship}
    'mdm:childRelationship' {yield export.childRelationship}
}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
