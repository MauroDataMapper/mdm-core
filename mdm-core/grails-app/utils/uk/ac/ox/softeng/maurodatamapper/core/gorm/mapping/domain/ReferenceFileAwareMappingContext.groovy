package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.CatalogueItemJoinTableDynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.DynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.AnnotationAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @since 31/10/2019
 */
class ReferenceFileAwareMappingContext extends CatalogueItemJoinTableDynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(AnnotationAware, domainClass)
    }

    @Override
    String getPropertyName() {
        'referenceFiles'
    }
}
