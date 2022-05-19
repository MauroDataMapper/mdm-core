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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.ADMIN

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueController* Controller: subscribedCatalogue
 *  | POST   | /api/admin/subscribedCatalogues                                          | Action: save            |
 *  | GET    | /api/admin/subscribedCatalogues                                          | Action: index           |
 *  | DELETE | /api/admin/subscribedCatalogues/${id}                                    | Action: delete          |
 *  | PUT    | /api/admin/subscribedCatalogues/${id}                                    | Action: update          |
 *  | GET    | /api/admin/subscribedCatalogues/${id}                                    | Action: show            |
 *  | GET    | /api/admin/subscribedCatalogues/${subscribedCatalogueId}/testConnection  | Action: testConnection  |
 *  | GET    | /api/subscribedCatalogues                                                | Action: index           |
 *  | GET    | /api/subscribedCatalogues/${id}                                          | Action: show            |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/testConnection        | Action: testConnection  |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/publishedModels       | Action: publishedModels |
 *
 */
@Integration
@Slf4j
class SubscribedCatalogueFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        'admin/subscribedCatalogues'
    }

    @Autowired
    DataModelJsonExporterService dataModelJsonExporterService

    String getValidId() {
        loginAdmin()
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()
        id
    }

    void removeValidIdObject(String id) {
        loginAdmin()
        DELETE(id)
        verifyResponse NO_CONTENT, response
        logout()
    }

    @Transactional
    String getFinalisedDataModelId() {
        DataModel.findByLabel('Finalised Example Test DataModel').id.toString()
    }

    Tuple2<String, String> getNewerDataModelIds() {
        loginAdmin()

        PUT("dataModels/${getFinalisedDataModelId()}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerPublicId = response.body().id

        PUT("dataModels/${newerPublicId}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerPublicId}/readByEveryone", [:], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerPublicId}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId = response.body().id

        PUT("dataModels/${newerId}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        logout()

        new Tuple(newerPublicId, newerId)
    }

    @Transactional
    void cleanUpRoles(String... ids) {
        log.info('Cleaning up roles and groups')
        log.debug('Cleaning up {} roles for ids {}', SecurableResourceGroupRole.count(), ids)
        SecurableResourceGroupRole.bySecurableResourceIds(ids.collect {Utils.toUuid(it)}).deleteAll()
        safeSessionFlush()
    }

    Map getValidJson() {
        [
            url                    : "http://localhost:$serverPort".toString(),
            apiKey                 : UUID.randomUUID().toString(),
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
    }

    Map getInvalidJson() {
        [
            url   : 'wibble',
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1'
        ]
    }

    Map getValidUpdateJson() {
        [
            description: 'Functional Test Description Updated'
        ]
    }

    String getExpectedShowJson() {
        """{
    "apiKey": "\${json-unit.matches:id}",
    "description": "Functional Test Description",
    "id": "\${json-unit.matches:id}",
    "label": "Functional Test Label",
    "subscribedCatalogueType": 'Mauro JSON',
    "refreshPeriod": 7,
    "url": "http://localhost:$serverPort"
}"""
    }

    String getExpectedOpenAccessShowJson() {
        """{
    "description": "Functional Test Description",
    "id": "\${json-unit.matches:id}",
    "label": "Functional Test Label",
    "subscribedCatalogueType": 'Mauro JSON',
    "refreshPeriod": 7,
    "url": "http://localhost:$serverPort"
}"""
    }

    String getExpectedIndexJson() {
        """{
    "count": 1,
    "items": [
        {
            "id": "\${json-unit.matches:id}",
            "url": "http://localhost:$serverPort",
            "label": "Functional Test Label",
            "subscribedCatalogueType": 'Mauro JSON',
            "description": "Functional Test Description",
            "refreshPeriod": 7
        }
    ]
}"""
    }

    /*
      * Logged in as editor testing
      */

    void 'E02 : Test the open access show and index actions render and admin actions are forbidden (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'When the admin show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse FORBIDDEN, response

        when: 'When the admin index action is called'
        GET('')

        then:
        verifyResponse FORBIDDEN, response

        when: 'When the open access show action is called to retrieve a resource'
        GET("subscribedCatalogues/$id", STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedOpenAccessShowJson()

        when: 'When the open access index action is called'
        GET('subscribedCatalogues', STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedIndexJson()

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged out testing
     */

    void 'L02 : Test the show and index actions do not render an instance for set user (not logged in)'() {
        given:
        String id = getValidId()

        when: 'When the admin show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse NOT_FOUND, response

        when: 'When the admin index action is called'
        GET('')

        then:
        verifyResponse NOT_FOUND, response

        when: 'When the open access show action is called to retrieve a resource'
        GET("subscribedCatalogues/$id", MAP_ARG, true)

        then:
        verifyResponse NOT_FOUND, response

        when: 'When the open access index action is called'
        GET('subscribedCatalogues', MAP_ARG, true)

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged out testing
     */

    void 'L02a : Test the index action for format opml responds when not logged in'() {
        given:
        String id = getValidId()

        //note: need to assign the GET return to response because this is not json
        when: 'When the index action is called for opml format'
        HttpResponse<String> response = GET('?format=opml', STRING_ARG)

        then:
        verifyResponse OK, response
        assert response.body().startsWith('<?xml version="1.0" encoding="UTF-8"?><opml version="2.0">')

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N02 : Test the open access show and index actions render and admin actions are forbidden (as no access/authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'When the admin show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse FORBIDDEN, response

        when: 'When the admin index action is called'
        GET('')

        then:
        verifyResponse FORBIDDEN, response

        when: 'When the open access show action is called to retrieve a resource'
        GET("subscribedCatalogues/$id", STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedOpenAccessShowJson()

        when: 'When the open access index action is called'
        GET('subscribedCatalogues', STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedIndexJson()

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R02 : Test the open access show and index actions render and admin actions are forbidden (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'When the admin show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse FORBIDDEN, response

        when: 'When the admin index action is called'
        GET('')

        then:
        verifyResponse FORBIDDEN, response

        when: 'When the open access show action is called to retrieve a resource'
        GET("subscribedCatalogues/$id", STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedOpenAccessShowJson()

        when: 'When the open access index action is called'
        GET('subscribedCatalogues', STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedIndexJson()

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged in as admin testing
     */

    void 'A02 : Test the show and index actions correctly render (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'When the admin show action is called to retrieve a resource'
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedShowJson()

        when: 'When the admin index action is called'
        GET('', STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedIndexJson()

        when: 'When the open access show action is called to retrieve a resource'
        GET("subscribedCatalogues/$id", STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedOpenAccessShowJson()

        when: 'When the open access index action is called'
        GET('subscribedCatalogues', STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedIndexJson()

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged in as editor testing
     */

    void 'E03a : Test the save action is forbidden (as editor)'() {
        given:
        loginEditor()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }

    void 'E03b : Test the save action is forbidden when using  PUT (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'E04 : Test the delete action is forbidden (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged out testing
     */

    void 'L03a : Test the save action is not found (as not logged in)'() {

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response
    }

    void 'L03b : Test the save action is not found when using  PUT (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'L04 : Test the delete action is not found (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */

    void 'N03a : Test the save action is forbidden (as authenticated)'() {
        given:
        loginAuthenticated()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }

    void 'N03b : Test the save action is forbidden when using  PUT (as authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'N04 : Test the delete action is forbidden (as authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /**
     * Testing when logged in as a reader only user
     */

    void 'R03a : Test the save action is forbidden (as reader)'() {
        given:
        loginReader()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }

    void 'R03b : Test the save action is forbidden when using  PUT (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'R04 : Test the delete action is forbidden (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A03a : Test the save action is ok (as admin)'() {
        given:
        loginAdmin()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with unreachable URL'
        Map invalidDomainMap = [
            url  : 'http://invalid.localhost:8080',
            label: 'Invalid Domain'
        ]
        POST('', invalidDomainMap)

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'A03b : Test the save action is OK when using PUT (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is not found'
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'A03c : Test that duplicate label is unprocessable (as admin)'() {
        given:
        loginAdmin()

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id

        when: 'The save action is executed again with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    void 'A04 : Test the delete action is ok (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is not correct'
        verifyResponse NO_CONTENT, response

        cleanup:
        cleanUpRoles(id)
    }

    /**
     * Test the publishedModels endpoint. This would be on a remote host, but in this functional test
     * we use the localhost. Test setup and execution is as follows:
     * 1. Login as Admin and create an API Key for Admin
     * 2. Subscribe to the local catalogue (in real life this would be remote), specifying the API key created above
     * 3. Get the local /publishedModels endpoint. In real life this would connect to /api/published/models on the remote,
     * but here we use the local.
     * 4. Cleanup
     */
    void 'A05 : Test the publishedModels endpoint'() {

        given:
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]

        when:
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey

        when:
        //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        verifyBaseJsonResponse(responseBody(), true)
        responseBody().items.size() == 3

        and:
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Finalised Example Test DataModel' && it.version == '1.0.0'}, 'DataModel', 'dataModels',
                                 getDataModelExporters())
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Simple Test CodeSet' && it.version == '1.0.0'}, 'CodeSet', 'codeSets', getCodeSetExporters())
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Complex Test CodeSet' && it.version == '1.0.0'}, 'CodeSet', 'codeSets', getCodeSetExporters())

        cleanup:
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A06 : Test the publishedModels endpoint (without API key)'() {
        given:
        loginAdmin()

        when:
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : '',
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        verifyBaseJsonResponse(responseBody(), false)

        cleanup:
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A07 : Test the newerModels endpoint (with no newer models)'() {
        given:
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        String finalisedDataModelId = getFinalisedDataModelId()
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), false)

        cleanup:
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A08 : Test the newerModels endpoint (with newer models and API key)'() {
        given:
        String finalisedDataModelId = getFinalisedDataModelId()
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), true)
        responseBody().newerPublishedModels.size() == 2

        and:
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'Finalised Example Test DataModel' && it.version == '2.0.0'}, 'DataModel', 'dataModels',
                                 getDataModelExporters(), true)
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'Finalised Example Test DataModel' && it.version == '3.0.0'}, 'DataModel', 'dataModels',
                                 getDataModelExporters(), true)

        cleanup:
        DELETE("dataModels/${newerId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/${newerPublicId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A09 : Test the newerModels endpoint (with newer models, without API key)'() {
        given:
        String finalisedDataModelId = getFinalisedDataModelId()
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2
        loginAdmin()
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : '',
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id
        PUT("dataModels/${finalisedDataModelId}/readByEveryone", [:], MAP_ARG, true)
        verifyResponse OK, response

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), true)
        responseBody().newerPublishedModels.size() == 1

        and:
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'Finalised Example Test DataModel' && it.version == '2.0.0'}, 'DataModel', 'dataModels',
                                 getDataModelExporters(), true)

        cleanup:
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$finalisedDataModelId/readByEveryone", MAP_ARG, true)
        verifyResponse OK, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A10 : Test the testConnection action'() {
        given:
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]

        when:
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey

        when:
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("${subscribedCatalogueId}/testConnection", STRING_ARG)

        then:
        verifyJsonResponse OK, null

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/testConnection", STRING_ARG, true)

        then:
        verifyJsonResponse OK, null

        cleanup:
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A11 : Test the testConnection action (without API key)'() {
        given:
        loginAdmin()

        when:
        Map subscriptionJson = [
            url                    : "http://localhost:$serverPort/".toString(),
            apiKey                 : '',
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Mauro JSON',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("${subscribedCatalogueId}/testConnection", STRING_ARG)

        then:
        verifyJsonResponse OK, null

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/testConnection", STRING_ARG, true)

        then:
        verifyJsonResponse OK, null

        cleanup:
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    private void verifyJsonPublishedModel(Map publishedModel, String modelType, String modelEndpoint, Map<String, String> exporters, boolean newerVersion = false) {
        assert publishedModel
        assert publishedModel.modelId ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.label
        assert Version.from(publishedModel.version)
        assert publishedModel.modelType == modelType
        assert OffsetDateTime.parse(publishedModel.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.dateCreated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert publishedModel.links.each {link ->
            assert link.contentType
            String exporterUrl = exporters.get(link.contentType)
            assert link.url ==~ /http:\/\/localhost:$serverPort\/api\/$modelEndpoint\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\/export\\/$exporterUrl/
        }
        if (newerVersion) assert publishedModel.previousModelId ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
    }

    private void verifyBaseJsonResponse(Map<String, Object> responseBody, boolean expectEntries) {
        if (expectEntries) {
            assert responseBody.items.size() > 0
            assert responseBody.items.size() == responseBody.count
        } else {
            assert responseBody.items.size() == 0
            assert responseBody.count == 0
        }
    }

    private void verifyBaseNewerVersionsJsonResponse(Map<String, Object> responseBody, boolean expectEntries) {
        assert OffsetDateTime.parse(responseBody.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        if (expectEntries) {
            assert responseBody.newerPublishedModels.size() > 0
        } else {
            assert responseBody.newerPublishedModels.size() == 0
        }
    }

    private static Map<String, String> getDataModelExporters() {
        [
            'application/mauro.datamodel+json': 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
            'application/mauro.datamodel+xml' : 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.1'
        ]
    }

    private static Map<String, String> getCodeSetExporters() {
        [
            'application/mauro.codeset+json': 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
            'application/mauro.codeset+xml' : 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetXmlExporterService/5.0'
        ]
    }
}
