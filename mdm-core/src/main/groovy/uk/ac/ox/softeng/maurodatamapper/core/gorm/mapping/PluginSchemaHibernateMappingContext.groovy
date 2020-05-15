package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping

import grails.plugins.metadata.GrailsPlugin
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.Mapping

/**
 * @since 04/11/2019
 */
abstract class PluginSchemaHibernateMappingContext extends DynamicHibernateMappingContext {

    abstract String getPluginName()

    abstract String getSchemaName()

    @Override
    boolean handlesDomainClass(Class domainClass) {
        GrailsPlugin annotation = domainClass.getAnnotation(GrailsPlugin) as GrailsPlugin
        annotation.name() == pluginName
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        Mapping mapping = entity.mapping.mappedForm as Mapping
        mapping.table([schema: schemaName])
        null
    }
}
