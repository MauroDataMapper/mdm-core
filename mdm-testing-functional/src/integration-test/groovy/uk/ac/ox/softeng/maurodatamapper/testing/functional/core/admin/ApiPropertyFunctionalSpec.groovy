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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.admin

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.junit.Assert

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Integration
@Slf4j
class ApiPropertyFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        'admin/properties'
    }

    Map getValidJson() {
        [key     : 'functional.test.key',
         value   : 'Some random thing',
         category: 'Functional Test']
    }

    Map getInvalidJson() {
        [key     : 'functional test key',
         value   : 'Some random thing',
         category: 'Functional Test']
    }

    Map getValidUpdateJson() {
        [key  : 'functional.test.key',
         value: 'Some different random thing']
    }

    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "key": "functional.test.key",
  "value": "Some random thing",
  "category": "Functional Test",
  "publiclyVisible": false,
  "lastUpdatedBy": "admin@maurodatamapper.com",
  "createdBy": "admin@maurodatamapper.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    String getPublicIndexJson() {
        '{"count":0,"items":[]}'
    }

    String getAdminIndexJson() {
        '''{
}'''
    }

    /**
     * Items are created by the editor user
     * This ensures that they dont have some possible weird admin protection
     * @return
     */
    String getValidId(Map jsonMap = validJson) {
        loginAdmin()
        POST('', jsonMap)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()
        id
    }

    void removeValidIdObject(String id) {
        removeValidIdObject(id, NO_CONTENT)
    }

    void removeValidIdObject(String id, HttpStatus expectedStatus) {
        if (!id) return
        log.info('Removing valid id {} using DELETE', id)
        loginAdmin()
        DELETE(id)
        verifyResponse expectedStatus, response
        logout()
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse UNPROCESSABLE_ENTITY, response
        assert response.body().total == 1
        assert response.body().errors.first().message
    }

    /*
   * Logged in as admin testing
   * This proves that admin users can mess with items created by other users
   */

    void 'A01 : Test the index action (as admin)'() {
        given:
        loginAdmin()

        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyResponse OK, response
        ApiPropertyEnum.values()
            .findAll {!(it in [ApiPropertyEnum.SITE_URL])}
            .each {ape ->
                Assert.assertTrue "${ape.key} should exist", responseBody().items.any {
                    it.key == ape.key &&
                    it.category == 'Email'
                }
            }

    }

    void 'A02 : Test the show action correctly renders an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'When the show action is called to retrieve a resource'
        GET("$id", STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse OK, showJson

        cleanup:
        removeValidIdObject(id)
    }

    /*
  * Logged in as admin testing
  * This proves that admin users can mess with items created by other users
  */

    void 'A03 : Test the save action correctly persists an instance (as admin)'() {
        given:
        loginAdmin()

        when:
        POST('', validJson)

        then:
        verifyResponse CREATED, response
        response.body().id

        when: 'Trying to save again using the same info'
        String id1 = response.body().id
        POST('', validJson)

        then:
        verifySameValidDataCreationResponse()
        String id2 = response.body()?.id

        cleanup:
        removeValidIdObject(id1)
        if (id2) {
            removeValidIdObject(id2) // not expecting anything, but just in case
        }
    }

    void 'A04 : Test the delete action correctly deletes an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${UUID.randomUUID()}")

        then: 'The response is correct'
        verifyResponse NOT_FOUND, response

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        verifyResponse NO_CONTENT, response

        cleanup:
        removeValidIdObject(id, NOT_FOUND)
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
        PUT("$id", invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

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

    void 'EXX : Test editor endpoints are all forbidden'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'LXX : Test not logged endpoints are all forbidden'() {
        given:
        def id = getValidId()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'NXX : Test logged in/authenticated endpoints are all forbidden'() {
        given:
        def id = getValidId()
        loginAuthenticated()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'RXX : Test reader endpoints are all forbidden'() {
        given:
        def id = getValidId()
        loginReader()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }


    def 'check public api property endpoint with no additional properties'() {
        when:
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody() == [count: 0, items: []]

    }

    def 'check public api property endpoint with public additional property'() {
        given:
        Map publicProperty = getValidJson()
        publicProperty.publiclyVisible = true
        String id = getValidId(publicProperty)

        when: 'not logged in'
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id


        when: 'reader'
        loginReader()
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        when: 'authenticated'
        loginAuthenticated()
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        when: 'editor'
        loginEditor()
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        cleanup:
        removeValidIdObject(id)

    }
}
