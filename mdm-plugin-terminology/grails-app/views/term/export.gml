import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import java.time.OffsetDateTime

Term export = term as Term

'mdm:term' {
    layout '/catalogueItem/_export.gml', catalogueItem: export
    'mdm:code' { yield export.code }
    'mdm:definition' { yield export.definition }
    if (export.url) 'mdm:url' { yield export.url }
    if (export.depth) 'mdm:depth' { yield export.depth }
}

static String convertDate(OffsetDateTime dateTime) {
    OffsetDateTimeConverter.toString(dateTime)
}
