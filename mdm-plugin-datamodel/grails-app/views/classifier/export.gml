import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

Classifier cls = classifier as Classifier

invokeMethod("$ns:classifier") {
    if (cls.id) invokeMethod("$ns:id", cls.id)
    invokeMethod("$ns:label") {yield cls.label}
    if (cls.lastUpdated) invokeMethod("$ns:lastUpdated", OffsetDateTimeConverter.toString(cls.lastUpdated))
    if (cls.description) invokeMethod("$ns:description") {yield cls.description}
}
