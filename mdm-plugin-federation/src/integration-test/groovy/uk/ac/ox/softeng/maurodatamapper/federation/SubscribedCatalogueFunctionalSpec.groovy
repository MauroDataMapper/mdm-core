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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @see SubscribedCatalogueController* Controller: subscribedCatalogue
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
class SubscribedCatalogueFunctionalSpec extends ResourceFunctionalSpec<SubscribedCatalogue> {

    @Shared
    UUID finalisedSimpleDataModelId

    @Override
    String getResourcePath() {
        'admin/subscribedCatalogues'
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        Folder folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, testAuthority)
        finalisedSimpleDataModelId = BootstrapModels.buildAndSaveFinalisedSimpleDataModel(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec SubscribedCatalogueFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    Tuple<String> getNewerDataModelIds() {
        PUT("dataModels/${finalisedSimpleDataModelId}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId1 = response.body().id

        PUT("dataModels/${newerId1}/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerId1}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId2 = response.body().id

        PUT("dataModels/${newerId2}/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response

        new Tuple(newerId1, newerId2)
    }

    //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
    Map getValidJson() {
        [
            url          : "http://localhost:$serverPort".toString(),
            apiKey       : '67421316-66a5-4830-9156-b1ba77bba5d1',
            label        : 'Functional Test Label',
            description  : 'Functional Test Description',
            refreshPeriod: 7
        ]
    }

    Map getInvalidJson() {
        [
            url   : 'wibble',
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1'
        ]
    }

    //note: any-string on the Url is a workaround after the previous note
    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "${json-unit.any-string}",
  "label": 'Functional Test Label',
  "description": 'Functional Test Description',
  "refreshPeriod": 7,
  "apiKey": "67421316-66a5-4830-9156-b1ba77bba5d1"
}'''
    }

    String getExpectedOpenAccessShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "${json-unit.any-string}",
  "label": 'Functional Test Label',
  "description": 'Functional Test Description',
  "refreshPeriod": 7
}'''
    }

    String getExpectedOpenAccessIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "url": "${json-unit.any-string}",
      "label": "Functional Test Label",
      "description": "Functional Test Description",
      "refreshPeriod": 7
    }
  ]
}'''
    }

    void 'O01 : Test the open access index action'() {
        when:
        POST('', getValidJson())

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET('subscribedCatalogues', STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedOpenAccessIndexJson()

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'O02 : Test the open access show action'() {
        when:
        POST('', getValidJson())

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, getExpectedOpenAccessShowJson()

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'T01 : Test the testConnection action'() {
        when:
        POST('', getValidJson())

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
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'P01 : Test the publishedModels endpoint'() {
        given:
        POST('', getValidJson())
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
    "count": 1,
    "items": [
        {
            "dateCreated": "${json-unit.matches:offsetDateTime}",
            "datePublished": "${json-unit.matches:offsetDateTime}",
            "label": "Finalised Example Test DataModel",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "modelId": "${json-unit.matches:id}",
            "modelType": "DataModel",
            "title": "Finalised Example Test DataModel 1.0.0",
            "version": "1.0.0"
        }
    ]
}'''

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'N01 : Test the newerVersions endpoint (with no newer versions)'() {
        given:
        POST('', getValidJson())
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedSimpleDataModelId}/newerVersions", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "newerPublishedModels": []
}'''

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'N02 : Test the newerVersions endpoint (with newer versions)'() {
        given:
        getNewerDataModelIds()
        POST('', getValidJson())
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedSimpleDataModelId}/newerVersions", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "newerPublishedModels": [
        {
            "dateCreated": "${json-unit.matches:offsetDateTime}",
            "datePublished": "${json-unit.matches:offsetDateTime}",
            "label": "Finalised Example Test DataModel",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "modelId": "${json-unit.matches:id}",
            "modelType": "DataModel",
            "previousModelId": "${json-unit.matches:id}",
            "title": "Finalised Example Test DataModel 2.0.0",
            "version": "2.0.0"
        },
        {
            "dateCreated": "${json-unit.matches:offsetDateTime}",
            "datePublished": "${json-unit.matches:offsetDateTime}",
            "label": "Finalised Example Test DataModel",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "modelId": "${json-unit.matches:id}",
            "modelType": "DataModel",
            "previousModelId": "${json-unit.matches:id}",
            "title": "Finalised Example Test DataModel 3.0.0",
            "version": "3.0.0"
        }
    ]
}'''

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }
}
