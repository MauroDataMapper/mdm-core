package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.PropertyConfig

/**
 * @since 09/03/2020
 */
abstract class JoinTableDynamicHibernateMappingContext extends DynamicHibernateMappingContext {

    abstract String getPropertyName()

    String getInverseSidePropertyName(PersistentEntity entity) {
        entity.decapitalizedName
    }

    String getCascade() {
        'all-delete-orphan'
    }

    String getJoinTableName(PersistentEntity entity) {
        "join_${getInverseSidePropertyName(entity)}_to_${propertyName}"
    }

    String getJoinTableKey(PersistentEntity entity) {
        "${entity.decapitalizedName}_id"
    }

    Map getJoinTableMap(PersistentEntity entity) {
        [name: getJoinTableName(entity),
         key : getJoinTableKey(entity)]
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        PropertyConfig property = updateProperty(entity, propertyName, [cascade: cascade])
        property.joinTable(getJoinTableMap(entity))
        property

    }
}
