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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.test

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationType
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
 *  | GET    | /api/subscribedCatalogues                                                | Action: types           |
 *  | GET    | /api/subscribedCatalogues                                                | Action: index           |
 *  | GET    | /api/subscribedCatalogues/${id}                                          | Action: show            |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/testConnection        | Action: testConnection  |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/publishedModels       | Action: publishedModels |
 *
 */
@Integration
@Slf4j
abstract class BaseSubscribedCatalogueFunctionalSpec extends FunctionalSpec {

    abstract Map getValidJson()

    abstract Map getInvalidJson()

    abstract Map getValidUpdateJson()

    abstract String getExpectedShowJson()

    abstract String getExpectedOpenAccessShowJson()

    abstract String getExpectedIndexJson()

    abstract SubscribedCatalogueType getSubscribedCatalogueType()

    abstract String getSubscribedCatalogueUrl()

    @Override
    String getResourcePath() {
        'admin/subscribedCatalogues'
    }

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

    /*
      * Logged in as editor testing
      */

    void 'E01 : Test the open access show and index actions render and admin actions are forbidden (as editor)'() {
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

    void 'E02 : Test the types action renders (as editor)'() {
        given:
        loginEditor()

        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Atom",
          "Mauro JSON"
        ]'''
    }

    /*
     * Logged out testing
     */

    void 'L01 : Test the show and index actions do not render an instance for set user (not logged in)'() {
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

    void 'L01O : Test the index action for format opml responds when not logged in'() {
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

    void 'L02 : Test the types action does not render (when not logged in)'() {
        when:
        GET('types')

        then:
        verifyResponse NOT_FOUND, response
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N01 : Test the open access show and index actions render and admin actions are forbidden (as no access/authenticated)'() {
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

    void 'N02 : Test the types action renders (as no access/authenticated)'() {
        given:
        loginAuthenticated()

        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Atom",
          "Mauro JSON"
        ]'''
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R01 : Test the open access show and index actions render and admin actions are forbidden (as reader)'() {
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

    void 'R02 : Test the types action renders (as reader)'() {
        given:
        loginReader()

        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Atom",
          "Mauro JSON"
        ]'''
    }

    /*
     * Logged in as admin testing
     */

    void 'A01 : Test the show and index actions correctly render (as admin)'() {
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

    void 'A02 : Test the types action renders (as admin)'() {
        given:
        loginAdmin()

        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Atom",
          "Mauro JSON"
        ]'''
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

    void 'E04b : Test the save action is forbidden when using PUT (as editor)'() {
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

    void 'E05 : Test the delete action is forbidden (as editor)'() {
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

    void 'L03b : Test the save action is not found when using PUT (as not logged in)'() {
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

    void 'A05 : Test the testConnection action'() {
        given:
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]

        when:
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(StandardEmailAddress.ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey

        when:
        Map subscriptionJson = [
            url                                  : subscribedCatalogueUrl,
            apiKey                               : apiKey,
            label                                : 'Functional Test Label',
            subscribedCatalogueType              : subscribedCatalogueType.label,
            subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.API_KEY.label,
            description                          : 'Functional Test Description',
            refreshPeriod                        : 7
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
        DELETE("catalogueUsers/${getUserByEmailAddress(StandardEmailAddress.ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A06 : Test the testConnection action (without API key)'() {
        given:
        loginAdmin()

        when:
        Map subscriptionJson = [
            url                                  : subscribedCatalogueUrl,
            label                                : 'Functional Test Label',
            subscribedCatalogueType              : subscribedCatalogueType.label,
            subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.NO_AUTHENTICATION.label,
            description                          : 'Functional Test Description',
            refreshPeriod                        : 7
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

    protected void verifyBaseJsonResponse(Map<String, Object> responseBody, boolean expectEntries) {
        if (expectEntries) {
            assert responseBody.items.size() > 0
            assert responseBody.items.size() == responseBody.count
        } else {
            assert responseBody.items.size() == 0
            assert responseBody.count == 0
        }
    }

    protected void verifyBaseNewerVersionsJsonResponse(Map<String, Object> responseBody, boolean expectEntries) {
        assert OffsetDateTime.parse(responseBody.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        if (expectEntries) {
            assert responseBody.newerPublishedModels.size() > 0
        } else {
            assert responseBody.newerPublishedModels.size() == 0
        }
    }

    protected static Map<String, String> getDataModelExporters() {
        [
            'application/mauro.datamodel+json': 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.2',
            'application/mauro.datamodel+xml' : 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.2'
        ]
    }

    protected static Map<String, String> getCodeSetExporters() {
        [
            'application/mauro.codeset+json': 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
            'application/mauro.codeset+xml' : 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetXmlExporterService/5.0'
        ]
    }
}
