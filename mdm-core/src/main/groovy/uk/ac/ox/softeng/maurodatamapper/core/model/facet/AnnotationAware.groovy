package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType

/**
 * @since 30/01/2020
 */
@SelfType(CatalogueItem)
@CompileStatic
trait AnnotationAware {
    abstract Set<Annotation> getAnnotations()

    CatalogueItem addToAnnotations(Annotation add) {
        add.setCatalogueItem(this as CatalogueItem)
        addTo('annotations', add)
    }

    CatalogueItem addToAnnotations(Map args) {
        addToAnnotations(new Annotation(args))
    }

    CatalogueItem removeFromAnnotations(Annotation annotations) {
        removeFrom('annotations', annotations)
    }
}
