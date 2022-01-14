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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Shared

import java.nio.file.Path
import java.time.LocalDate

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.ADMIN
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModelController* Controller: subscribedModel
 *  | POST   | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels                     | Action: save          |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels                     | Action: index         |
 *  | DELETE | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}               | Action: delete        |
 *  | PUT    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}               | Action: update        |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}               | Action: show          |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/subscribedModels/${id}/newerVersions | Action: newerVersions |
 *
 */
@Integration
@Slf4j
@Ignore("Depends on remote catalogue - to be run manually")
class SubscribedModelFunctionalSpec extends FunctionalSpec {

    @Shared
    UUID subscribedCatalogueId

    @Shared
    UUID adminApiKey

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data for SubscribedModelFunctionalSpec')
        sessionFactory.currentSession.flush()

        adminApiKey = new ApiKey(catalogueUser: getUserByEmailAddress(ADMIN),
                                 name: "Functional Test",
                                 expiryDate: LocalDate.now().plusDays(5),
                                 createdBy: FUNCTIONAL_TEST).save(flush: true).id


        subscribedCatalogueId = new SubscribedCatalogue(url: 'https://modelcatalogue.cs.ox.ac.uk/continuous-deployment',
                                                        apiKey: '720e60bc-3993-48d4-a17e-c3a13f037c7e',
                                                        label: 'Functional Test Label',
                                                        description: 'Functional Test Description',
                                                        refreshPeriod: 7,
                                                        createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert subscribedCatalogueId

    }

    @Transactional
    String getFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    def cleanupSpec() {
        log.debug('CleanupSpec SubscribedModelFunctionalSpec')
        cleanUpResources(SubscribedCatalogue, ApiKey)
    }

    @Shared
    Path resourcesPath

    @Override
    String getResourcePath() {
        "subscribedCatalogues/${getSubscribedCatalogueId()}/subscribedModels"
    }

    String getValidId() {
        loginAdmin()
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        logout()
        id
    }

    void removeValidIdObjects(String id, String localModelId = null) {
        loginAdmin()
        if (id) {
            DELETE(id)
            verifyResponse NO_CONTENT, response
        }
        if (localModelId) {
            DELETE("dataModels/${localModelId}?permanent=true", MAP_ARG, true)
            verifyResponse NO_CONTENT, response
        }
        logout()
    }

    String getSubscribedModelLocalModelId(String id) {
        loginAdmin()
        GET(id)
        String localModelId = responseBody().localModelId
        logout()
        localModelId
    }

    @Transactional
    void cleanUpRoles(String... ids) {
        log.info('Cleaning up roles and groups')
        log.debug('Cleaning up {} roles for ids {}', SecurableResourceGroupRole.count(), ids)
        SecurableResourceGroupRole.bySecurableResourceIds(ids.collect {Utils.toUuid(it)}).deleteAll()
        sessionFactory.currentSession.flush()
    }

    Map getValidJson() {
        [
            subscribedModelId  : '427d1243-4f89-46e8-8f8f-8424890b5083',
            folderId           : getFolderId(),
            subscribedModelType: 'DataModel'
        ]
    }

    Map getInvalidJson() {
        [
            subscribedModelId: null,
            folderId         : getFolderId()
        ]
    }

    /*
      * Logged in as editor testing
      */

    void 'E02 : Test the show and index action correctly renders an instance for set user (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response
        String localModelId = responseBody().localModelId

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /*
     * Logged out testing
     */

    void 'L02 : Test the show and index action does not render an instance for set user (not logged in)'() {
        given:
        String id = getValidId()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse NOT_FOUND, response

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        String localModelId = getSubscribedModelLocalModelId(id)
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N02 : Test the show and index action correctly renders an instance for set user (as no access/authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response
        String localModelId = responseBody().localModelId

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R02 : Test the show and index action correctly renders an instance for set user (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response
        String localModelId = responseBody().localModelId

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /*
     * Logged in as admin testing
     */

    void 'A02 : Test the show action correctly renders an instance for set user (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response
        String localModelId = responseBody().localModelId

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /*
     * Logged in as editor testing
     */

    void 'E03 : Test the save action is ok (as editor)'() {
        given:
        loginEditor()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id
        String localModelId = responseBody().localModelId

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    void 'E04 : Test the delete action is forbidden (as editor)'() {
        given:
        String id = getValidId()
        String localModelId = getSubscribedModelLocalModelId(id)
        loginEditor()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /*
     * Logged out testing
     */

    void 'L03 : Test the save action is not found (as not logged in)'() {
        given:

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

    void 'L04 : Test the delete action is not found (as not logged in)'() {
        given:
        String id = getValidId()
        String localModelId = getSubscribedModelLocalModelId(id)

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */

    void 'N03 : Test the save action is ok (as authenticated)'() {
        given:
        loginAuthenticated()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().total == 1
        responseBody().errors.contains([message: 'Invalid folderId for subscribed model, user does not have the necessary permissions'])
    }

    void 'N04 : Test the delete action is forbidden (as authenticated)'() {
        given:
        String id = getValidId()
        String localModelId = getSubscribedModelLocalModelId(id)
        loginAuthenticated()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /**
     * Testing when logged in as a reader only user
     */

    void 'R03a : Test the save action is forbidden (as reader)'() {
        given:
        loginReader()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().total == 1
        responseBody().errors.contains([message: 'Invalid folderId for subscribed model, user does not have the necessary permissions'])
    }

    void 'R04 : Test the delete action is forbidden (as reader)'() {
        given:
        String id = getValidId()
        String localModelId = getSubscribedModelLocalModelId(id)
        loginReader()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */

    void 'A03 : Test the save action is ok (as admin)'() {
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

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id
        String localModelId = responseBody().localModelId

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    void 'A04 : Test the delete action is ok (as admin)'() {
        given:
        String id = getValidId()
        String localModelId = getSubscribedModelLocalModelId(id)
        loginAdmin()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is correct'
        verifyResponse NO_CONTENT, response

        cleanup:
        removeValidIdObjects(null, localModelId)
        cleanUpRoles(id)
    }

    void 'A05 : Test the save action with attempted federation (as admin)'() {
        given:
        loginAdmin()

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id
        String localModelId = responseBody().localModelId

        when: 'The save action is executed with existing published model id'
        POST('', validJson)

        then: 'The response is unprocessable as this model is already subscribed'
        verifyResponse UNPROCESSABLE_ENTITY, response
        log.debug('responseBody().errors.first().message={}', responseBody().errors.first().message)
        responseBody().errors.first().message == 'Property [subscribedModelId] of class [class uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModel] with value [' +
            getValidJson().subscribedModelId + '] must be unique'

        cleanup:
        removeValidIdObjects(id, localModelId)
        cleanUpRoles(id, localModelId)
    }

    @Transactional
    String getPublishedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }
}
