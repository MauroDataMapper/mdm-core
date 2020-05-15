package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.CatalogueItemJoinTableDynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.container.CatalogueItemClassifierAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 31/10/2019
 */
class CatalogueItemClassifierAwareMappingContext extends CatalogueItemJoinTableDynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(CatalogueItemClassifierAware, domainClass)
    }

    @Override
    String getPropertyName() {
        'classifiers'
    }

    @Override
    String getCascade() {
        'none'
    }
}
