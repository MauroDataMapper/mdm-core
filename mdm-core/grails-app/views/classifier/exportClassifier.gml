import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    Classifier classifier
}
Classifier cls = classifier as Classifier

'mdm:classifier' {
    if (cls.id) 'mdm:id'(cls.id)
    'mdm:label' {yield cls.label}
    if (cls.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(cls.lastUpdated))
    if (cls.description) 'mdm:description' {yield cls.description}
}
