package uk.ac.ox.softeng.maurodatamapper.provider.plugin

import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperProvider

/**
 * @since 16/08/2017
 */
interface MauroDataMapperPlugin extends MauroDataMapperProvider {
    Closure doWithSpring()
}
