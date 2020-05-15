package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterController* Controller: importer
 *  | GET | /api/importer/parameters/${ns}?/${name}?/${version}? | Action: parameters |
 */
@Integration
class ImporterFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'importer'
    }

    void 'test importer parameters'() {

        when:
        GET('parameters/ox.softeng.maurodatamapper.core.spi.json/JsonImporterService/1.1', Argument.of(String))

        then:
        verifyJsonResponse(NOT_FOUND, '''{
  "path": "/api/importer/parameters/ox.softeng.maurodatamapper.core.spi.json/JsonImporterService/1.1",
  "resource": "ImporterProviderService",
  "id": "ox.softeng.maurodatamapper.core.spi.json:JsonImporterService:1.1"
}''')
    }
}
