package uk.ac.ox.softeng.maurodatamapper.core.flyway


import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.PluginSchemaHibernateMappingContext

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer

@Slf4j
class CustomFlywayConfiguration implements FlywayConfigurationCustomizer {

    @Autowired
    Collection<PluginSchemaHibernateMappingContext> pluginSchemaHibernateMappingContexts

    @Override
    void customize(FluentConfiguration configuration) {

        log.info('Updating list of flyway schemas')
        List<String> schemas = pluginSchemaHibernateMappingContexts.sort().collect {it.schemaName}
        configuration.schemas(schemas.toArray() as String[])
        log.warn('Flyway now controls the following schemas: {}', schemas)
    }
}
