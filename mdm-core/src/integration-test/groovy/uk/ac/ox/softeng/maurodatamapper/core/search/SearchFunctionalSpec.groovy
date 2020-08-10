/*
 * Copyright 2020 University of Oxford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
