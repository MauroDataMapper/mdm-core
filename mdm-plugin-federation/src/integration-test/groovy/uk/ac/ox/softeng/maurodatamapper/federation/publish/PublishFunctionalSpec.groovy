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
package uk.ac.ox.softeng.maurodatamapper.federation.publish

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * @see PublishController* Controller: publish
 *  | GET | /api/published/models                                   | Action: index         |
 *  | GET | /api/published/models/${publishedModelId}/newerVersions | Action: newerVersions |
 *
 */
@Slf4j
@Integration
class PublishFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    String folderId

    @Shared
    String dataModelId

    @Override
    String getResourcePath() {
        'published'
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data for FeedFunctionalSpec')
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id.toString()
        assert folderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PublishFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    Tuple<String> getNewerDataModelIds() {
        PUT("dataModels/${dataModelId}/newBranchModelVersion", [:], MAP_ARG, true)
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

    void 'test getting published models'() {

        when:
        GET('models')

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.isEmpty()

    }

    void '2-test getting published models when model available'() {
        given:
        String publishedDateStr = '2021-06-28T12:36:37Z'
        POST("folders/${folderId}/dataModels", [
            label             : 'FunctionalTest DataModel',
            readableByEveryone: true,
            finalised         : true,
            dateFinalised     : publishedDateStr,
            description       : 'Some random desc',
            modelVersion      : '1.0.0'
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        dataModelId = responseBody().id

        when:
        GET('models')

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.size() == 1

        when:
        Map<String, Object> publishedModel = responseBody().publishedModels.first()

        then:
        publishedModel.modelId == dataModelId
        publishedModel.title == 'FunctionalTest DataModel 1.0.0'
        publishedModel.description == 'Some random desc'
        publishedModel.modelType == 'DataModel'
        publishedModel.datePublished == publishedDateStr
        publishedModel.lastUpdated
        publishedModel.dateCreated
    }

    void 'N01 : Test the newerVersions endpoint (with no newer versions)'() {
        when:
        GET("models/$dataModelId/newerVersions", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "newerPublishedModels": []
}'''
    }

    void 'N02 : Test the newerVersions endpoint (with newer versions)'() {
        given:
        getNewerDataModelIds()

        when:
        GET("models/$dataModelId/newerVersions", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "newerPublishedModels": [
        {
            "dateCreated": "${json-unit.matches:offsetDateTime}",
            "datePublished": "${json-unit.matches:offsetDateTime}",
            "description": "Some random desc",
            "label": "FunctionalTest DataModel",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "modelId": "${json-unit.matches:id}",
            "modelType": "DataModel",
            "previousModelId": "${json-unit.matches:id}",
            "title": "FunctionalTest DataModel 2.0.0",
            "version": "2.0.0"
        },
        {
            "dateCreated": "${json-unit.matches:offsetDateTime}",
            "datePublished": "${json-unit.matches:offsetDateTime}",
            "description": "Some random desc",
            "label": "FunctionalTest DataModel",
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "modelId": "${json-unit.matches:id}",
            "modelType": "DataModel",
            "previousModelId": "${json-unit.matches:id}",
            "title": "FunctionalTest DataModel 3.0.0",
            "version": "3.0.0"
        }
    ]
}'''
    }
}
