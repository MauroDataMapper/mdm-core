package uk.ac.ox.softeng.maurodatamapper.provider.plugin

import java.lang.Module as JModule

/**
 * @since 17/10/2019
 */
class JavaModule extends AbstractMauroDataMapperPlugin {

    JModule module

    @Override
    String getName() {
        module.name
    }

    @Override
    String getVersion() {
        module.descriptor.version().get()
    }

    @Override
    int getOrder() {
        LOWEST_PRECEDENCE
    }
}
