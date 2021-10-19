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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation.publish

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: publish
 *  | GET | /api/published/models                                   | Action: index         |
 *  | GET | /api/published/models/${publishedModelId}/newerVersions | Action: newerVersions |
 * </pre>
 *
 * @see uk.ac.ox.softeng.maurodatamapper.federation.publish.PublishController
 */
@Integration
@Slf4j
class PublishFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        'published'
    }

    @Transactional
    String getFinalisedDataModelId() {
        DataModel.findByLabel('Finalised Example Test DataModel').id.toString()
    }

    Tuple<String> getNewerDataModelIds() {
        loginAdmin()

        PUT("dataModels/${getFinalisedDataModelId()}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerPublicId = response.body().id

        PUT("dataModels/${newerPublicId}/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerPublicId}/readByEveryone", [:], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerPublicId}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId = response.body().id

        PUT("dataModels/${newerId}/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response

        logout()

        new Tuple(newerPublicId, newerId)
    }

    void 'L01 : Get published models when not logged in'() {

        when:
        GET('models')

        then: "The response is OK with no entries"
        verifyResponse(OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.isEmpty()
    }

    void 'R01 : Get published models when logged in as reader'() {

        given:
        loginReader()

        when:
        GET('models')

        then:
        verifyResponse(OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.size() == 3

        and:
        verifyEntry(responseBody().publishedModels.find { it.title == 'Simple Test CodeSet 1.0.0' }, 'CodeSet')
        verifyEntry(responseBody().publishedModels.find { it.title == 'Finalised Example Test DataModel 1.0.0' }, 'DataModel')
    }

    void 'L02 : Test the newerVersions endpoint with no newer versions (as not logged in)'() {
        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions")

        then:
        verifyResponse NOT_FOUND, response
    }

    void 'R02 : Test the newerVersions endpoint with no newer versions (as reader)'() {
        given:
        loginReader()

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "newerPublishedModels": []
}'''
    }

    void 'L03 : Test the newerVersions endpoint with newer versions (as not logged in)'() {
        given:
        def (String newerPublicId, String newerId) = getNewerDataModelIds()

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        loginAdmin()
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'R03 : Test the newerVersions endpoint with newer versions (as reader)'() {
        given:
        def (String newerPublicId, String newerId) = getNewerDataModelIds()
        loginReader()

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions", STRING_ARG)

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
        loginAdmin()
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    private void verifyEntry(Map publishedModel, String modelType) {
        assert publishedModel
        assert publishedModel.modelId.toString() ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.modelType == modelType
        assert publishedModel.datePublished
        assert publishedModel.lastUpdated
        assert publishedModel.dateCreated
    }
}
