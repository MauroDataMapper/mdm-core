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
package uk.ac.ox.softeng.maurodatamapper.federation.test

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
 *  | GET    | /api/subscribedCatalogues                                                | Action: types           |
 *  | GET    | /api/subscribedCatalogues                                                | Action: index           |
 *  | GET    | /api/subscribedCatalogues/${id}                                          | Action: show            |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/testConnection        | Action: testConnection  |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/publishedModels       | Action: publishedModels |
 *
 */
@Integration
@Slf4j
abstract class BaseSubscribedCatalogueFunctionalSpec extends ResourceFunctionalSpec<SubscribedCatalogue> {

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
        Folder folder = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST)
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

    abstract Map getValidJson()

    abstract Map getInvalidJson()

    abstract String getExpectedOpenAccessShowJson()

    abstract String getExpectedOpenAccessIndexJson()

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

    void 'T02 : Test getting SubscribedCatalogue types'() {
        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Atom",
          "Mauro JSON"
        ]'''
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
}
