package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping


import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.orm.hibernate.cfg.PropertyConfig

/**
 * @since 31/10/2019
 */
abstract class DynamicHibernateMappingContext {

    abstract boolean handlesDomainClass(Class domainClass)

    abstract Property updateDomainMapping(PersistentEntity entity)

    PropertyConfig updateProperty(PersistentEntity entity, String propertyName, Map updatedConfig) {
        PropertyConfig config = getPropertyConfig(entity, propertyName)
        PropertyConfig.configureExisting(config, updatedConfig)
    }

    PropertyConfig getPropertyConfig(PersistentEntity entity, String propertyName) {
        PersistentProperty pathProp = entity.getPropertyByName(propertyName)
        pathProp.mapping.mappedForm as PropertyConfig
    }
}
