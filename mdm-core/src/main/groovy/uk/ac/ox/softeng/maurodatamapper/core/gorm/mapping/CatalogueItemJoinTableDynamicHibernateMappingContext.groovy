package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping

import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @since 09/03/2020
 */
abstract class CatalogueItemJoinTableDynamicHibernateMappingContext extends JoinTableDynamicHibernateMappingContext {

    @Override
    String getJoinTableName(PersistentEntity entity) {
        "join_${getInverseSidePropertyName(entity)}_to_facet"
    }
}
