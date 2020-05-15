package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.DynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @since 31/10/2019
 */
class PathAwareMappingContext extends DynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(PathAware, domainClass)
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        updateProperty(entity, 'path', [type: 'text'])
    }
}
