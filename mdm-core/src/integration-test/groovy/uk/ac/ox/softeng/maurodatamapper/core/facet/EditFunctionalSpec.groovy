/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.EditController* Controller: edit
 *  | GET | /api/folders/${folderId}/edits | Action: index |
 *  | GET | /api/classifiers/${classifierId}/edits | Action: index |
 *  | GET | /api/edits/${resourceDomainType}/${resourceId} | Action: index |
 */
@Integration
@Slf4j
class EditFunctionalSpec extends BaseFunctionalSpec {

    String getResourcePath() {
        ''
    }

    Map getValidJson() {
        [:]
    }

    Map getInvalidJson() {
        [:]
    }

    String getEditsResourcePath() {
        'edits'
    }

    @Rollback
    void 'Test the index action'() {
        when: 'The index action is requested for unknown resource'
        GET("folder/${UUID.randomUUID()}/${editsResourcePath}")

        then: 'response is correct'
        response.status == HttpStatus.OK
        response.body() == [count: 0, items: []]

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'The index action is requested for known resource after a folder is created'
        def folderId = response.body().id
        GET("folder/${folderId}/${editsResourcePath}", STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "count": 1,
  "items": [
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "unlogged_user@mdm-core.com",
      "description": "[Folder:Functional Test Folder] created"
    }
  ]
}''')

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    @Rollback
    void 'Test the index action on folder endpoint'() {
        when: 'The index action is requested for unknown resource'
        GET("folders/${UUID.randomUUID()}/edits")

        then: 'response is correct'
        response.status == HttpStatus.OK
        response.body() == [count: 0, items: []]

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'The index action is requested for known resource after a folder is created'
        def folderId = response.body().id
        GET("folders/${folderId}/edits", STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "count": 1,
  "items": [
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "unlogged_user@mdm-core.com",
      "description": "[Folder:Functional Test Folder] created"
    }
  ]
}''')

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    @Rollback
    void 'Test the index action on classifier endpoint'() {
        when: 'The index action is requested for unknown resource'
        GET("classifiers/${UUID.randomUUID()}/edits")

        then: 'response is correct'
        response.status == HttpStatus.OK
        response.body() == [count: 0, items: []]

        when: 'creating new classifier'
        POST('classifiers', [label: 'Functional Test Classifier'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'The index action is requested for known resource after a classifier is created'
        def classifierId = response.body().id
        GET("classifiers/${classifierId}/edits", STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "count": 1,
  "items": [
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "unlogged_user@mdm-core.com",
      "description":  "[Classifier:Functional Test Classifier] created"
    }
  ]
}''')

        cleanup:
        DELETE("classifiers/$classifierId")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'Test the save action correctly persists an instance'() {
        when: 'The save action is executed with valid data'
        client.toBlocking().exchange(HttpRequest.POST(editsResourcePath, validJson), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void 'Test the update action correctly updates an instance'() {
        when: 'The update action is executed with valid data'
        String path = "${editsResourcePath}/1"
        client.toBlocking().exchange(HttpRequest.PUT(path, validJson), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void 'Test the show action correctly renders an instance'() {
        when: 'When the show action is called to retrieve a resource'
        def id = '1'
        String path = "${editsResourcePath}/${id}"
        client.toBlocking().exchange(HttpRequest.GET(path), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void 'Test the delete action correctly deletes an instance'() {
        when: 'When the delete action is executed on an unknown instance'
        def path = "${editsResourcePath}/99999"
        client.toBlocking().exchange(HttpRequest.DELETE(path))

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }
}
