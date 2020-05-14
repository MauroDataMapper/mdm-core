package uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable

import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

/**
 * @since 17/02/2020
 */
class CallableConstraints {

    static call(Class constraintsClass, delegate) {
        List<Closure> constraints = ClassPropertyFetcher.getStaticPropertyValuesFromInheritanceHierarchy(constraintsClass,
                                                                                                         GormProperties.CONSTRAINTS, Closure)

        for (Closure c : constraints.findAll()) {
            c = (Closure<?>) c.clone();
            c.setResolveStrategy(Closure.DELEGATE_ONLY)
            c.setDelegate(delegate)
            c.call()
        }
    }
}
