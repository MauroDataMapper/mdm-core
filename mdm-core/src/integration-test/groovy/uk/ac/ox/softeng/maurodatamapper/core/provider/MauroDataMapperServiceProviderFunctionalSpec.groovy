package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import io.micronaut.core.order.Ordered
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.OK

/**
 * @see MauroDataMapperServiceProviderController* Controller: mauroDataMapperServiceProvider
 *  | GET | /api/admin/providers/exporters   | Action: exporterProviders   |
 *  | GET | /api/admin/providers/emailers    | Action: emailProviders      |
 *  | GET | /api/admin/providers/dataLoaders | Action: dataLoaderProviders |
 *  | GET | /api/admin/providers/importers   | Action: importerProviders   |
 */
@Integration
class MauroDataMapperServiceProviderFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'admin/providers'
    }

    void 'test get exporters'() {
        when:
        GET('exporters', Argument.of(String))

        then:
        verifyJsonResponse(OK, '[]')
    }

    void 'test get emailers'() {
        when:
        GET('emailers', Argument.of(String))

        then:
        verifyJsonResponse(OK, '''[
  {
    "order": ''' + Ordered.LOWEST_PRECEDENCE + ''',
    "providerType": "Email",
    "knownMetadataKeys": [
      
    ],
    "displayName": "Basic Email Provider",
    "name": "BasicEmailProviderService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email",
    "allowsExtraMetadataKeys": true,
    "version": "${json-unit.matches:version}"
  }
]''')
    }

    void 'test get importers'() {
        when:
        GET('importers', Argument.of(String))

        then:
        verifyJsonResponse(OK, '[]')
    }

    void 'test get dataloaders'() {
        when:
        GET('dataLoaders', Argument.of(String))

        then:
        verifyJsonResponse(OK, '[]')
    }
}