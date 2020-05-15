package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.DynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @since 31/10/2019
 */
class CreatorAwareMappingContext extends DynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(CreatorAware, domainClass)
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        String dType = entity.javaClass.simpleName
        updateProperty(entity, 'createdBy', [
            cascade: 'none',
            index  : "${entity.decapitalizedName}_created_by_idx".toString()
        ])
    }
}
