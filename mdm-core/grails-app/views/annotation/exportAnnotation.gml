import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    Annotation annotation
}
Annotation ann = annotation as Annotation

'mdm:annotation' {
    if (ann.id) 'mdm:id'(ann.id)
    'mdm:createdBy'(ann.createdBy)
    if (ann.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(ann.lastUpdated))

    if (!ann.parentAnnotation) 'mdm:label'(ann.label)
    if (ann.description) 'mdm:description'(ann.description)

    if (ann.childAnnotations) {
        'mdm:childAnnotations' {
            ann.childAnnotations.each {child ->
                layout '/annotation/exportAnnotation.gml', annotation: child
            }
        }
    }
}
