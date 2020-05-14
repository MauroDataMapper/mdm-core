package uk.ac.ox.softeng.maurodatamapper.hibernate.search

import grails.plugins.hibernate.search.config.SearchMappingEntityConfig
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

/**
 * @since 17/02/2020
 */
class CallableSearch {

    static call(Class searchClass, delegate) {
        List<Closure> constraints = ClassPropertyFetcher.getStaticPropertyValuesFromInheritanceHierarchy(searchClass,
                                                                                                         SearchMappingEntityConfig.INDEX_CONFIG_NAME,
                                                                                                         Closure)

        for (Closure c : constraints.findAll()) {
            c = (Closure<?>) c.clone();
            c.setResolveStrategy(Closure.DELEGATE_ONLY)
            c.setDelegate(delegate)
            c.call()
        }
    }
}
