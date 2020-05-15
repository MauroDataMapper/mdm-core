package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.DynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @since 31/10/2019
 */
class BreadcrumbTreeMappingContext extends DynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(ModelItem, domainClass)
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        updateProperty(entity, 'breadcrumbTree', [fetch: 'join'])
    }
}
