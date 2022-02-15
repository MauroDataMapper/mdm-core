package uk.ac.ox.softeng.maurodatamapper.federation


import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseUrlMappingsReportSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 14/02/2022
 */
@Integration
@Slf4j
class UrlMappingsSpec extends BaseUrlMappingsReportSpec {

    @Override
    String getKnownControllerInPlugin() {
        'subscribedCatalogue'
    }
}
