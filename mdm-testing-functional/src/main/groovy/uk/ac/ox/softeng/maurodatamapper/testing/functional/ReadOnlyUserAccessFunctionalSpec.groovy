/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import org.spockframework.util.Assert

import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * Tests the below endpoints for editor/reader/not logged/admin/no access in access.
 * Tests editor first as we use the editor user to create new items, so we have to be able to "Edit" before we can test any further.
 * <pre>
 *  | GET    | /api/${resourcePath}       | Action: index  |
 *  | GET    | /api/${resourcePath}/${id} | Action: show   |
 * </pre>
 *
 * To change any expected permissions inside the tests override the getExpectedPermissions method
 *
 */
@Slf4j
abstract class ReadOnlyUserAccessFunctionalSpec extends FunctionalSpec {

    abstract String getEditorIndexJson()

    abstract String getValidId()

    abstract void removeValidIdObject(String id)

    @RunOnce
    def setup() {
        log.info('{}', expectations)
    }

    String getShowJson() {
        null
    }

    String getReaderIndexJson() {
        getEditorIndexJson()
    }

    String getAdminIndexJson() {
        getContainerAdminIndexJson()
    }

    String getContainerAdminIndexJson() {
        getEditorIndexJson()
    }


    void verify01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert responseBody().containsKey('count')
        assert responseBody().containsKey('items')
    }

    void verify02Response(HttpResponse<Map> response, String id, List<String> actions) {
        verifyResponse OK, response
        assert responseBody().id == id
        if (expectations.availableActionsProvided) assert responseBody().availableActions == actions
    }

    void untestedExpectation() {
        Assert.fail('Untested expectation')
    }

    /**
     * Testing when logged in as a reader only user
     */

    void 'CORE-#prefix-01 : Test the index action (as #name)'() {
        given:
        login(name)

        if (expectedJson) {
            when: 'The index action is requested'
            GET('', STRING_ARG)

            then: 'The response is correct'
            if (expectations.can(name, 'index')) verifyJsonResponse OK, expectedJson
            else assert jsonCapableResponse.status() == NOT_FOUND
        } else {
            when: 'The index action is requested'
            GET('')

            then: 'The response is correct'
            if (expectations.can(name, 'index')) verify01Response(response)
            else verifyNotFound(response, null)
        }

        where:
        prefix | name             | expectedJson
        'LO'   | 'Anonymous'      | null
        'NA'   | 'Authenticated'  | null
        'RE'   | 'Reader'         | getReaderIndexJson()
        'RV'   | 'Reviewer'       | getReaderIndexJson()
        'AU'   | 'Author'         | getReaderIndexJson()
        'ED'   | 'Editor'         | getEditorIndexJson()
        'CA'   | 'ContainerAdmin' | getContainerAdminIndexJson()
        'AD'   | 'Admin'          | getAdminIndexJson()
    }

    void 'CORE-#prefix-02 : Test the show action correctly renders an instance (as #name)'() {
        given:
        def id = getValidId()
        login(name)

        if (showJson && name == 'reader') {
            when: 'When the show action is called to retrieve a resource'
            GET("$id", STRING_ARG)

            then:
            if (expectations.can(name, 'see')) verifyJsonResponse OK, showJson
            else assert jsonCapableResponse.status() == NOT_FOUND
        }

        when: 'When the show action is called to retrieve a resource'
        GET("$id")

        then: 'The response is correct'
        if (expectations.noAccess) verifyNotFound response, id
        else if (expectations.can(name, 'see')) verify02Response(response, id, expectations.availableActions(name))
        else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | 'Anonymous'
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }
}
