import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

Annotation ann = annotation as Annotation

invokeMethod("$ns:annotation") {
    if (ann.id) invokeMethod("$ns:id", ann.id)
    invokeMethod("$ns:createdBy", ann.createdBy)
    if (ann.lastUpdated) invokeMethod("$ns:lastUpdated", OffsetDateTimeConverter.toString(ann.lastUpdated))

    if (!ann.parentAnnotation) invokeMethod("$ns:label", ann.label)
    if (ann.description) invokeMethod("$ns:description", ann.description)

    if (ann.childAnnotations) {
        invokeMethod("$ns:childAnnotations") {
            ann.childAnnotations.each {child ->
                layout "/annotation/export.gml", annotation: child, ns: ns
            }
        }
    }
}
