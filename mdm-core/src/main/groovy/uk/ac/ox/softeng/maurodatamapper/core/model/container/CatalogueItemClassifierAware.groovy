package uk.ac.ox.softeng.maurodatamapper.core.model.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 20/
 * 04/2018
 */
@SelfType(CatalogueItem)
@CompileStatic
trait CatalogueItemClassifierAware {

    abstract Set<Classifier> getClassifiers()

    CatalogueItem addToClassifiers(Classifier classifier) {
        addTo('classifiers', classifier)
    }

    CatalogueItem addToClassifiers(Map args) {
        addTo('classifiers', args)
    }

    CatalogueItem removeFromClassifiers(Classifier classifier) {
        removeFrom('classifiers', classifier)
    }

    static abstract CatalogueItem findByIdJoinClassifiers(UUID id)
}
