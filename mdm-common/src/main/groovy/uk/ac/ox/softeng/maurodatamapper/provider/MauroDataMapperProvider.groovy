package uk.ac.ox.softeng.maurodatamapper.provider

import io.micronaut.core.order.Ordered

/**
 * @since 17/08/2017
 */

interface MauroDataMapperProvider extends Ordered {

    String getName()

    String getVersion()
}
