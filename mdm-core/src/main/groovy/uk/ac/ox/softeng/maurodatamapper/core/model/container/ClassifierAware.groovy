package uk.ac.ox.softeng.maurodatamapper.core.model.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier

import groovy.transform.CompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 20/
 * 04/2018
 */
@SelfType(GormEntity)
@CompileStatic
trait ClassifierAware<K> {

    abstract Set<Classifier> getClassifiers()

    K addToClassifiers(Classifier classifier) {
        addTo('classifiers', classifier) as K
    }

    K addToClassifiers(Map args) {
        addTo('classifiers', args) as K
    }

    K removeFromClassifiers(Classifier classifier) {
        removeFrom('classifiers', classifier) as K
    }

    static abstract K findByIdJoinClassifiers(UUID id)
}
