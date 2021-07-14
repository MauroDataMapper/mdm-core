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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * Tests the below endpoints for editor/reader/not logged/admin in access
 * <pre>
 *  | PUT    | /api/${resourcePath}/${id} | Action: update |
 *  | POST   | /api/${resourcePath}       | Action: save   | [inherited test]
 *  | DELETE | /api/${resourcePath}/${id} | Action: delete | [inherited test]
 *  | GET    | /api/${resourcePath}       | Action: index  | [inherited test]
 *  | GET    | /api/${resourcePath}/${id} | Action: show   | [inherited test]
 *
 *  Controller: edit
 *  |  GET     | /api/${resourceDomainType}/${resourceId}/edits  | Action: index
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.EditController
 */
@Slf4j
abstract class UserAccessFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    abstract Map getValidUpdateJson()

    Map getInvalidUpdateJson() {
        getInvalidJson()
    }

    void verifyInvalidUpdateResponse(HttpResponse response) {
        verifyResponse UNPROCESSABLE_ENTITY, response
    }

    Pattern getExpectedCreatedEditRegex() {
        ~/\[\w+:.+?] created/
    }

    Pattern getExpectedUpdateEditRegex() {
        ~/\[\w+:.+?] changed properties \[description]/
    }

    String getEditsPath() {
        resourcePath
    }

    String getEditsFullPath(String id) {
        "${getEditsPath()}/${id}"
    }

    @Transactional
    void removeChangelogUsingTransaction(String id) {
        log.info('Removing changelog id {} using transaction', id)
        Edit changelog = Edit.get(id)
        changelog.delete(flush: true)
    }    

    /*
     * Logged in as editor testing
     */

    void 'E05 : Test the update action correctly updates an instance (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'The update action is called with invalid data'
        PUT("$id", invalidUpdateJson)

        then: 'The response is correct'
        verifyInvalidUpdateResponse(response)

        when: 'The update action is called with valid data'
        PUT("$id", validUpdateJson)

        then: 'The response is correct'
        verifyResponse OK, response
        response.body().id == id
        validUpdateJson.each {k, v ->
            if (v instanceof Map) {
                v.each {k1, v1 ->
                    assert response.body()[k][k1] == v1
                }
            } else {
                assert response.body()[k] == v
            }
        }

        cleanup:
        removeValidIdObject(id)
    }

    /*
     * Logged out testing
     */

    void 'L05 : Test the update action correctly updates an instance (not logged in)'() {
        given:
        def id = getValidId()

        when: 'The update action is called with invalid data'
        PUT("$id", invalidUpdateJson)

        then: 'The response is correct'
        verifyNotFound response, id

        when: 'The update action is called with valid data'
        PUT("$id", validUpdateJson)

        then: 'The response is correct'
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N05 : Test the update action correctly updates an instance (as no access/authenticated)'() {
        given:
        def id = getValidId()
        loginAuthenticated()

        when: 'The update action is called with invalid data'
        PUT("$id", invalidUpdateJson)

        then: 'The response is correct'
        verifyNotFound response, id

        when: 'The update action is called with valid data'
        PUT("$id", validUpdateJson)

        then: 'The response is correct'
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R05 : Test the update action correctly updates an instance (as reader)'() {
        given:
        def id = getValidId()
        loginReader()

        when: 'The update action is called with invalid data'
        PUT("$id", invalidUpdateJson)

        then: 'The response is correct'
        verifyForbidden response

        when: 'The update action is called with valid data'
        PUT("$id", validUpdateJson)

        then: 'The response is correct'
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A05 : Test the update action correctly updates an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'The update action is called with invalid data'
        PUT("$id", invalidUpdateJson)

        then: 'The response is correct'
        verifyInvalidUpdateResponse(response)

        when: 'The update action is called with valid data'
        PUT("$id", validUpdateJson)

        then: 'The response is correct'
        verifyResponse OK, response
        response.body().id == id
        validUpdateJson.each {k, v ->
            if (v instanceof Map) {
                v.each {k1, v1 ->
                    assert response.body()[k][k1] == v1
                }
            } else {
                assert response.body()[k] == v
            }
        }

        cleanup:
        removeValidIdObject(id)
    }

    void 'X01 : Test getting edits after creation (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'getting initial edits'
        GET("${getEditsFullPath(id)}/edits?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count >= 1
        response.body().items.first().createdBy == userEmailAddresses.editor
        (response.body().items.first().description as String).matches(getExpectedCreatedEditRegex())

        cleanup:
        removeValidIdObject(id)
    }

    void 'X02 : Test getting edits after edit (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("$id", validUpdateJson)
        verifyResponse OK, response

        when: 'getting edits after update'
        GET("${getEditsFullPath(id)}/edits?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count >= 2
        response.body().items[1].createdBy == userEmailAddresses.editor
        (response.body().items[1].description as String).matches(getExpectedCreatedEditRegex())
        response.body().items[0].createdBy == userEmailAddresses.editor
        (response.body().items[0].description as String).matches(getExpectedUpdateEditRegex())


        cleanup:
        removeValidIdObject(id)
    }

    void 'X03 : Test getting changelogs after creation (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'getting initial changelogs'
        GET("${getEditsFullPath(id)}/changelogs?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 0

        cleanup:
        removeValidIdObject(id)
    }

    void 'X04 : Test creating and getting a changelog (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'creating a changelog'
        POST("${getEditsFullPath(id)}/changelogs", ["description": "Functional Test Changelog"], MAP_ARG, true)

        then: 'the response is created'
        verifyResponse CREATED, response

        when: 'getting changelogs after creation'
        GET("${getEditsFullPath(id)}/changelogs?sort=dateCreated&order=desc", MAP_ARG, true)

        then: 'the response is correct'
        verifyResponse OK, response
        response.body().count == 1
        response.body().items[0].createdBy == userEmailAddresses.editor
        response.body().items[0].description == "Functional Test Changelog"
        response.body().items[0].title == "CHANGELOG"
        def changelogId = response.body().items[0].id

        cleanup:
        removeValidIdObject(id)
        removeChangelogUsingTransaction(changelogId)
    }    
}
