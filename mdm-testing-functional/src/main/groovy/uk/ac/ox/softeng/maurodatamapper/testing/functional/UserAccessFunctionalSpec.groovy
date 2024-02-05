/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import static io.micronaut.http.HttpStatus.NOT_FOUND
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

    Map getValidNonDescriptionUpdateJson() {
        [
            label: "Functional Test Updated Label ${getClass().simpleName}".toString()
        ]
    }

    Map getValidDescriptionOnlyUpdateJson() {
        [
            description: 'Functional Test Description Update'
        ]
    }

    Map getInvalidUpdateJson() {
        getInvalidJson()
    }

    Pattern getExpectedCreatedEditRegex() {
        ~/\[\w+( \w+)*:.+?] created/
    }

    Pattern getExpectedUpdateEditRegex() {
        ~/\[\w+( \w+)*:.+?] changed properties \[path, label]/
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

    void verifyValidDataUpdateResponse(HttpResponse<Map> response, String id, Map update) {
        verifyResponse OK, response
        assert responseBody().id == id
        update.each {k, v ->
            if (v instanceof Map) {
                v.each {k1, v1 ->
                    assert responseBody()[k][k1] == v1
                }
            } else {
                assert responseBody()[k] == v
            }
        }
    }

    /**
     * Testing when logged in as a author only user
     */
    void 'CORE-#prefix-05 : Test the update action correctly updates an instance (as #name)'() {
        given:
        def id = getValidId()
        login(name)

        when: 'The update action is called with invalid data'
        PUT("$id", invalidUpdateJson)

        then: 'The response is correct'
        if (expectations.can(name, 'update')) verifyResponse UNPROCESSABLE_ENTITY, response
        else if (expectations.can(name, 'see')) verifyForbidden(response)
        else verifyNotFound response, id

        when: 'The update action is called with valid data'
        PUT("$id", validDescriptionOnlyUpdateJson)

        then: 'The response is correct'
        if (expectations.can(name, 'editDescription') || expectations.can(name, 'update')) verifyValidDataUpdateResponse response, id, validDescriptionOnlyUpdateJson
        else if (expectations.can(name, 'see')) verifyForbidden(response)
        else verifyNotFound response, id

        when: 'The update action is called with valid data'
        PUT("$id", validNonDescriptionUpdateJson)

        then: 'The response is correct'
        if (expectations.can(name, 'update')) verifyValidDataUpdateResponse response, id, validNonDescriptionUpdateJson
        else if (expectations.can(name, 'see')) verifyForbidden(response)
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

    void 'CORE-X01 : Test getting edits after creation (as reader)'() {
        given:
        def id = getValidId()
        loginReader()

        when: 'getting initial edits'
        GET("${getEditsFullPath(id)}/edits?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        if (expectations.getHasEditsEndpoint()) {
            verifyResponse OK, response
            responseBody().count >= 1
            responseBody().items.first().createdBy == userEmailAddresses.creator
            (responseBody().items.first().description as String).matches(getExpectedCreatedEditRegex())
        } else {
            verifyResponse NOT_FOUND, response
        }


        cleanup:
        removeValidIdObject(id)
    }

    void 'CORE-X02 : Test getting edits after edit (as reader)'() {
        given:
        def id = getValidId()
        loginCreator()
        PUT("$id", validNonDescriptionUpdateJson)
        verifyResponse OK, response
        loginReader()

        when: 'getting edits after update'
        GET("${getEditsFullPath(id)}/edits?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        if (expectations.getHasEditsEndpoint()) {
            verifyResponse OK, response
            responseBody().count >= 2
            responseBody().items[1].createdBy == userEmailAddresses.creator
            (responseBody().items[1].description as String).matches(getExpectedCreatedEditRegex())
            responseBody().items[0].createdBy == userEmailAddresses.creator
            (responseBody().items[0].description as String).matches(getExpectedUpdateEditRegex())
        } else {
            verifyResponse NOT_FOUND, response
        }



        cleanup:
        removeValidIdObject(id)
    }

    void 'CORE-X03 : Test getting changelogs after creation (as reader)'() {
        given:
        def id = getValidId()
        loginReader()

        when: 'getting initial changelogs'
        GET("${getEditsFullPath(id)}/changelogs?sort=dateCreated&order=desc", MAP_ARG, true)

        then:
        if (expectations.getHasEditsEndpoint()) {
            verifyResponse OK, response
            responseBody().count == 0
        } else {
            verifyResponse NOT_FOUND, response
        }

        cleanup:
        removeValidIdObject(id)
    }

    void 'CORE-X04 : Test creating and getting a changelog (as editor)'() {
        given:
        def id = getValidId()
        loginCreator()

        when: 'creating a changelog'
        POST("${getEditsFullPath(id)}/changelogs", ['description': 'Functional Test Changelog'], MAP_ARG, true)

        then: 'the response is created'
        if (expectations.getHasEditsEndpoint()) {
            verifyResponse CREATED, response
        } else {
            verifyResponse NOT_FOUND, response
        }

        when: 'getting changelogs after creation'
        loginReader()
        GET("${getEditsFullPath(id)}/changelogs?sort=dateCreated&order=desc", MAP_ARG, true)

        then: 'the response is correct'
        String changelogId
        if (expectations.getHasEditsEndpoint()) {
            verifyResponse OK, response
            responseBody().count == 1
            responseBody().items[0].createdBy == userEmailAddresses.creator
            responseBody().items[0].description == 'Functional Test Changelog'
            responseBody().items[0].title == 'CHANGELOG'
            changelogId = responseBody().items[0].id
        } else {
            verifyResponse NOT_FOUND, response
        }

        cleanup:
        removeValidIdObject(id)
        if (changelogId) {
            removeChangelogUsingTransaction(changelogId)
        }

    }
}
