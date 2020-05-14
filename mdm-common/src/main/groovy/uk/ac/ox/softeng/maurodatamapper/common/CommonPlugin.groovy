package uk.ac.ox.softeng.maurodatamapper.common

import uk.ac.ox.softeng.maurodatamapper.provider.plugin.AbstractMauroDataMapperPlugin

/**
 * @since 17/08/2017
 */
class CommonPlugin extends AbstractMauroDataMapperPlugin {
    @Override
    String getName() {
        "Common"
    }

    @Override
    String getVersion() {
        '4.0.0-SNAPSHOT'
    }
}
