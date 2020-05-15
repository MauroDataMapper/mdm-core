package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.CatalogueItemJoinTableDynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.AnnotationAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 31/10/2019
 */
class AnnotationAwareMappingContext extends CatalogueItemJoinTableDynamicHibernateMappingContext {

    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(AnnotationAware, domainClass)
    }

    @Override
    String getPropertyName() {
        'annotations'
    }
}
