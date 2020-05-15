package uk.ac.ox.softeng.maurodatamapper.provider.plugin

/**
 * @since 17/10/2019
 */
class MdmGrailsPluginMauroDataMapperPlugin extends GrailsPluginMauroDataMapperPlugin {

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE + 100
    }

    boolean isMdmModule() {
        true
    }
}
