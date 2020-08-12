import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

Authority au = authority as Authority

invokeMethod("$ns:authority") {
    if (au.id) invokeMethod("$ns:id", au.id)
    invokeMethod("$ns:label") {yield au.label}
    invokeMethod("$ns:url") {yield au.url}
    if (au.lastUpdated) invokeMethod("$ns:lastUpdated", OffsetDateTimeConverter.toString(au.lastUpdated))
    if (au.description) invokeMethod("$ns:description") {yield au.description}
}
