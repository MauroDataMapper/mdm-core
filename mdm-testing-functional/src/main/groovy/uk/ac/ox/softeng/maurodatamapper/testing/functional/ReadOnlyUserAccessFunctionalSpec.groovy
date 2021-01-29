/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Stepwise

import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * Tests the below endpoints for editor/reader/not logged/admin/no access in access.
 * Tests editor first as we use the editor user to create new items, so we have to be able to "Edit" before we can test any further.
 * <pre>
 *  | GET    | /api/${resourcePath}       | Action: index  |
 *  | GET    | /api/${resourcePath}/${id} | Action: show   |
 * </pre>
 */
@Stepwise
@Slf4j
abstract class ReadOnlyUserAccessFunctionalSpec extends FunctionalSpec {

    abstract String getEditorIndexJson()

    abstract String getShowJson()

    abstract String getValidId()

    abstract void removeValidIdObject(String id)

    String getReaderIndexJson() {
        getEditorIndexJson()
    }

    String getAdminIndexJson() {
        getEditorIndexJson()
    }

    Boolean getReaderCanSeeEditorCreatedItems() {
        false
    }

    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse NOT_FOUND, response
    }

    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse NOT_FOUND, response
    }

    void verifyE02Response(HttpResponse<Map> response, String id) {
        verifyResponse OK, response
        response.body().id == id
    }

    void verifyR01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert response.body().count
        assert response.body().items
    }

    void verifyR02Response(HttpResponse<Map> response, String id) {
        verifyResponse OK, response
        response.body().id == id
        response.body().availableActions = ['show']
    }

    void verifyA02Response(HttpResponse<Map> response, String id) {
        verifyE02Response(response, id)
    }

    /*
     * Logged in as editor testing
     */

    void 'E01 : Test the index action (as editor)'() {
        given:
        loginEditor()

        when: 'The index action is requested'
        GET('', STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse OK, editorIndexJson
    }

    void 'E02 : Test the show action correctly renders an instance (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()

        if (showJson) {
            when: 'When the show action is called to retrieve a resource'
            GET("$id", STRING_ARG)

            then:
            verifyJsonResponse OK, showJson
        } else {
            when: 'When the show action is called to retrieve a resource'
            GET("$id")

            then:
            verifyE02Response response, id
        }

        cleanup:
        removeValidIdObject(id)
    }

    /*
     * Logged out testing
     */

    void 'L01 : Test the index action (not logged in)'() {
        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyL01Response response
    }

    void 'L02 : Test the show action correctly renders an instance (not logged in)'() {
        given:
        def id = getValidId()

        when: 'When the show action is called to retrieve a resource'
        GET("$id")

        then: 'The response is correct'
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */

    void 'N01 : Test the index action (as no access/authenticated)'() {
        given:
        loginAuthenticated()

        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyN01Response(response)
    }

    void 'N02 : Test the show action correctly renders an instance (as no access/authenticated)'() {
        given:
        def id = getValidId()
        loginAuthenticated()

        when: 'When the show action is called to retrieve a resource'
        GET("$id")

        then: 'The response is correct'
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a reader only user
     */

    void 'R01 : Test the index action (as reader)'() {
        given:
        loginReader()

        if (getReaderIndexJson()) {
            when: 'The index action is requested'
            GET('', STRING_ARG)

            then: 'The response is correct'
            verifyJsonResponse OK, getReaderIndexJson()
        } else {
            when: 'The index action is requested'
            GET('')

            then: 'The response is correct'
            verifyR01Response(response)
        }
    }

    void 'R02 : Test the show action correctly renders an instance (as reader)'() {
        given:
        def id = getValidId()
        loginReader()

        when: 'When the show action is called to retrieve a resource'
        GET("$id")

        then: 'The response is correct'
        if (!readerCanSeeEditorCreatedItems) verifyNotFound response, id
        else {
            verifyR02Response response, id
        }

        cleanup:
        removeValidIdObject(id)
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A01 : Test the index action (as admin)'() {
        given:
        loginAdmin()

        when: 'The index action is requested'
        GET('', STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse OK, adminIndexJson
    }

    void 'A02 : Test the show action correctly renders an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        if (showJson) {
            when: 'When the show action is called to retrieve a resource'
            GET("$id", STRING_ARG)

            then: 'The response is correct'
            verifyJsonResponse OK, showJson
        } else {
            when: 'When the show action is called to retrieve a resource'
            GET("$id")

            then: 'The response is correct'
            verifyA02Response(response, id)
        }

        cleanup:
        removeValidIdObject(id)
    }
}
