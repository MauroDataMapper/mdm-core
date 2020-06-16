import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

Metadata md = metadata as Metadata

invokeMethod("$ns:metadata") {
    if (md.id) invokeMethod("$ns:id", md.id)
    invokeMethod("$ns:namespace") {yield md.namespace}
    invokeMethod("$ns:key") {yield md.key}
    invokeMethod("$ns:value") {yield md.value}
    if (md.lastUpdated) invokeMethod("$ns:lastUpdated", OffsetDateTimeConverter.toString(md.lastUpdated))
}
