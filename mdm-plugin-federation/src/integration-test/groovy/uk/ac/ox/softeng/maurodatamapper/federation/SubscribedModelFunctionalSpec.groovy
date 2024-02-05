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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.ApiKeyAuthenticationCredentials
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Requires
import spock.lang.Shared

/**
 * @see SubscribedModelController* Controller: subscribedModel
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
// Requires a connection to the CD environment, running a version providing the /types endpoint
@Requires({
    String url = 'https://modelcatalogue.cs.ox.ac.uk/continuous-deployment/api/admin/subscribedCatalogues/types'
    HttpURLConnection connection = (url).toURL().openConnection() as HttpURLConnection
    connection.setRequestMethod('GET')
    connection.setRequestProperty('apiKey', 'f00616a7-c07f-48fe-b6cf-fb99f7076de5')
    connection.connect()
    connection.getResponseCode() == 200
})
class SubscribedModelFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    UUID subscribedCatalogueId

    @Shared
    UUID atomSubscribedCatalogueId

    @Shared
    UUID folderId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data for SubscribedModelFunctionalSpec')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert folderId

        subscribedCatalogueId = new SubscribedCatalogue(url: 'https://modelcatalogue.cs.ox.ac.uk/continuous-deployment',
                                                        apiKey: 'f00616a7-c07f-48fe-b6cf-fb99f7076de5',
                                                        label: 'Functional Test Subscribed Catalogue (Mauro JSON)',
                                                        subscribedCatalogueType: SubscribedCatalogueType.MAURO_JSON,
                                                        subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.API_KEY,
                                                        subscribedCatalogueAuthenticationCredentials:
                                                            new ApiKeyAuthenticationCredentials(apiKey: 'f00616a7-c07f-48fe-b6cf-fb99f7076de5'),
                                                        description: 'Functional Test Description',
                                                        refreshPeriod: 7,
                                                        createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert subscribedCatalogueId

        atomSubscribedCatalogueId = new SubscribedCatalogue(url: 'https://modelcatalogue.cs.ox.ac.uk/continuous-deployment/api/feeds/all',
                                                            apiKey: 'f00616a7-c07f-48fe-b6cf-fb99f7076de5',
                                                            label: 'Functional Test Subscribed Catalogue (Atom)',
                                                            subscribedCatalogueType: SubscribedCatalogueType.ATOM,
                                                            subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.API_KEY,
                                                            subscribedCatalogueAuthenticationCredentials:
                                                                new ApiKeyAuthenticationCredentials(apiKey: 'f00616a7-c07f-48fe-b6cf-fb99f7076de5'),
                                                            description: 'Functional Test Description',
                                                            refreshPeriod: 7,
                                                            createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert atomSubscribedCatalogueId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec SubscribedModelFunctionalSpec')
        cleanUpResources(Folder, SubscribedCatalogue)
    }

    @Override
    String getResourcePath() {
        "subscribedCatalogues/${getSubscribedCatalogueId()}/subscribedModels"
    }

    String getSavePath() {
        "${getResourcePath()}"
    }

    String getSavePathForAtom() {
        "subscribedCatalogues/${getAtomSubscribedCatalogueId()}/subscribedModels"
    }

    String createNewItem(Map model, String savePath = null) {
        POST(savePath ?: getSavePath(), model, MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        response.body().id
    }

    Map getValidJson() {
        [
            subscribedModel: [
                subscribedModelId: '427d1243-4f89-46e8-8f8f-8424890b5083',
                folderId         : getFolderId()
            ]
        ]
    }

    Map getValidJsonWithParams() {
        [
            subscribedModel        : [
                subscribedModelId: '427d1243-4f89-46e8-8f8f-8424890b5083',
                folderId         : getFolderId()
            ],
            contentType            : 'application/mauro.datamodel+xml',
            importerProviderService: [
                name     : 'DataModelXmlImporterService',
                namespace: 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer'
            ]
        ]
    }

    Map getValidJsonForAtom() {
        [
            subscribedModel: [
                subscribedModelId: 'urn:uuid:427d1243-4f89-46e8-8f8f-8424890b5083',
                folderId         : getFolderId()
            ]
        ]
    }

    Map getValidJsonForAtomWithParams() {
        [
            subscribedModel        : [
                subscribedModelId: 'urn:uuid:427d1243-4f89-46e8-8f8f-8424890b5083',
                folderId         : getFolderId()
            ],
            contentType            : 'application/mauro.datamodel+xml',
            importerProviderService: [
                name     : 'DataModelXmlImporterService',
                namespace: 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer'
            ]
        ]
    }

    Map getInvalidJson() {
        [
            subscribedModel: [
                subscribedModelId: null,
                folderId         : getFolderId()
            ]
        ]
    }

    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "subscribedModelId": "${json-unit.matches:id}",
  "folderId": "${json-unit.matches:id}",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": true,
  "federated": true,
  "localModelId": "${json-unit.matches:id}",
  "lastRead": "${json-unit.matches:offsetDateTime}"
}'''
    }

    void 'R1 : Test the empty index action'() {
        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyResponse(HttpStatus.OK, response)
        assert response.body() == [count: 0, items: []]
    }

    @Transactional
    void 'R2a : Test the save action correctly persists an instance (for #catalogueType)'() {
        given:
        String savePath
        Map validJson
        if (SubscribedCatalogueType.findForLabel(catalogueType) == SubscribedCatalogueType.MAURO_JSON) {
            savePath = getSavePath()
            validJson = getValidJson()
        } else {
            savePath = getSavePathForAtom()
            validJson = getValidJsonForAtom()
        }

        when: 'The save action is executed with no content'
        log.debug('No content save')
        POST(savePath, [:], MAP_ARG, true)

        then: 'The response is correct'
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
        response.body().errors.first().message ==
        'Property [subscribedModel] of class [class uk.ac.ox.softeng.maurodatamapper.federation.rest.transport.SubscribedModelFederationParams] cannot be null'

        when: 'The save action is executed with invalid data'
        log.debug('Invalid content save')
        POST(savePath, invalidJson, MAP_ARG, true)

        then: 'The response is correct'
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response
        response.body().total >= 1
        response.body().errors.size() == response.body().total

        when: 'The save action is executed with valid data'
        log.debug('Valid content save')
        createNewItem(validJson, savePath)

        then: 'The response is correct'
        verifyResponse HttpStatus.CREATED, response
        String id = response.body().id
        String localModelId = response.body().localModelId

        cleanup:
        DELETE(savePath + '/' + id, MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/${localModelId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT

        where:
        catalogueType << SubscribedCatalogueType.labels()
    }

    @Transactional
    void 'R2b : Test the save action with parameters correctly persists an instance (for #catalogueType)'() {
        given:
        String savePath
        Map validJson
        if (SubscribedCatalogueType.findForLabel(catalogueType) == SubscribedCatalogueType.MAURO_JSON) {
            savePath = getSavePath()
            validJson = getValidJsonWithParams()
        } else {
            savePath = getSavePathForAtom()
            validJson = getValidJsonForAtomWithParams()
        }

        when: 'The save action is executed with valid data with parameters'
        createNewItem(validJson, savePath)

        then: 'The response is correct'
        verifyResponse HttpStatus.CREATED, response
        String id = response.body().id
        String localModelId = response.body().localModelId

        cleanup:
        DELETE(savePath + '/' + id, MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/${localModelId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT

        where:
        catalogueType << SubscribedCatalogueType.labels()
    }

    void 'R3 : Test the index action with content'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'List is called'
        String id = response.body().id
        String localModelId = response.body().localModelId
        GET('')

        then: 'One subscribed model is listed'
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 1
        assert response.body().items.size() == 1
        assert response.body().items[0].id == id

        cleanup:
        DELETE(id)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/${localModelId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R4 : Test the show action correctly renders an instance'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'When the show action is called to retrieve a resource'
        String id = response.body().id
        String localModelId = response.body().localModelId
        GET(id, STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, getExpectedShowJson())

        cleanup:
        DELETE(id)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/${localModelId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'R5 : Test the delete action correctly deletes an instance'() {
        when: 'The save action is executed with valid data'
        createNewItem(validJson)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id
        String id = response.body().id
        String localModelId = response.body().localModelId

        when: 'When the delete action is executed on an unknown instance'
        DELETE(UUID.randomUUID().toString())

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT

        cleanup:
        DELETE("dataModels/${localModelId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
