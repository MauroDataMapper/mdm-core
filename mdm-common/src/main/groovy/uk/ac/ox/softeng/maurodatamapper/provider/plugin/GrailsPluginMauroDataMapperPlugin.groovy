package uk.ac.ox.softeng.maurodatamapper.provider.plugin

import grails.plugins.GrailsPlugin

/**
 * @since 17/10/2019
 */
class GrailsPluginMauroDataMapperPlugin extends AbstractMauroDataMapperPlugin {

    GrailsPlugin plugin

    @Override
    Closure doWithSpring() {
        null
    }

    @Override
    String getName() {
        plugin.name.replaceFirst(/^mdm/, '')
    }

    @Override
    String getVersion() {
        plugin.version
    }

    @Override
    int getOrder() {
        LOWEST_PRECEDENCE - 100
    }

    boolean isMdmModule() {
        false
    }
}