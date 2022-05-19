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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

    @RunOnce
    @Transactional
    def setup() {
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

    Tuple2<String, String> getNewerDataModelIds() {
        PUT("dataModels/${finalisedSimpleDataModelId}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId1 = response.body().id

        PUT("dataModels/${newerId1}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerId1}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId2 = response.body().id

        PUT("dataModels/${newerId2}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        new Tuple(newerId1, newerId2)
    }

    //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
    Map getValidJson() {
        [
            url                    : "http://localhost:$serverPort".toString(),
            apiKey                 : '67421316-66a5-4830-9156-b1ba77bba5d1',
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

    //note: any-string on the Url is a workaround after the previous note
    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "${json-unit.any-string}",
  "label": 'Functional Test Label',
  "subscribedCatalogueType": 'Mauro JSON',
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
  "subscribedCatalogueType": 'Mauro JSON',
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
      "subscribedCatalogueType": 'Mauro JSON',
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
        verifyResponse(CREATED, response)
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseJsonResponse(responseBody(), true)
        responseBody().items.size() == 1

        and:
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Finalised Example Test DataModel' && it.version == '1.0.0'}, 'DataModel', 'dataModels', getDataModelExporters())

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'N01 : Test the newerVersions endpoint (with no newer versions)'() {
        given:
        POST('', getValidJson())
        verifyResponse(CREATED, response)
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedSimpleDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), false)

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'N02 : Test the newerVersions endpoint (with newer versions)'() {
        given:
        Tuple tuple = getNewerDataModelIds()
        POST('', getValidJson())
        verifyResponse(CREATED, response)
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedSimpleDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), true)
        responseBody().newerPublishedModels.size() == 2

        and:
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'Finalised Example Test DataModel' && it.version == '2.0.0'}, 'DataModel', 'dataModels', getDataModelExporters(),
                                 true)
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'Finalised Example Test DataModel' && it.version == '3.0.0'}, 'DataModel', 'dataModels', getDataModelExporters(),
                                 true)

        cleanup:
        DELETE("dataModels/${tuple.v1}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/${tuple.v2}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
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
}
