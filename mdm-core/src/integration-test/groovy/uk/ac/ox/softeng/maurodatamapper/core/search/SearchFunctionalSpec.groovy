package uk.ac.ox.softeng.maurodatamapper.core.search

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: search
 *  |   GET    | /api/catalogueItems/search  | Action: search
 *  |   POST   | /api/catalogueItems/search  | Action: search
 * </pre>
 *
 * In Core there will be nothing to search but we want to check we can at least run the endpoint on an empty system
 *
 * @see SearchController
 */
@Integration
@Slf4j
class SearchFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'catalogueItems/search'
    }

    void 'test searching for "qwerty" using GET'() {
        given:
        def term = 'test'

        when:
        GET("?searchTerm=${term}")

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'test searching for "qwerty" using POST'() {
        given:
        def term = 'test'

        when:
        POST('', [searchTerm: term, sort: 'label'])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }
}
