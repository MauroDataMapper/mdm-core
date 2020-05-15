package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping


import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.PluginSchemaHibernateMappingContext

import groovy.util.logging.Slf4j

/**
 * Maps all domains in the mdm-core plugin into the core schema
 * @since 01/11/2019
 */
@Slf4j
class CoreSchemaMappingContext extends PluginSchemaHibernateMappingContext {


    @Override
    String getPluginName() {
        'mdmCore'
    }

    @Override
    String getSchemaName() {
        'core'
    }
}
