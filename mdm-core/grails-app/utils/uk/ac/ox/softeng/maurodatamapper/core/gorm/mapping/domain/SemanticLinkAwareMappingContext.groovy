package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.CatalogueItemJoinTableDynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.SemanticLinkAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 31/10/2019
 */
class SemanticLinkAwareMappingContext extends CatalogueItemJoinTableDynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(SemanticLinkAware, domainClass)
    }

    @Override
    String getPropertyName() {
        'semanticLinks'
    }
}
