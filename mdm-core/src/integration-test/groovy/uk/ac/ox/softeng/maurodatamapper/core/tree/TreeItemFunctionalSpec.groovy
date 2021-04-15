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
package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemController* Controller: treeItem
 *  | GET | /api/tree/search/${search} | Action: search |
 *  | GET | /api/tree/${id}?           | Action: index  |
 */
@Integration
@Slf4j
class TreeItemFunctionalSpec extends BaseFunctionalSpec {

    def setup() {

    }

    @Override
    String getResourcePath() {
        ''
    }

    String getOldTreeResourcePath() {
        'tree'
    }

    String getNewTreeResourcePath() {
        'tree/folders'
    }

    Map getValidJson() {
        [:]
    }

    Map getInvalidJson() {
        [:]
    }

    void '1 : Test the save action correctly persists an instance'() {
        when: 'The save action is executed with valid data'
        client.toBlocking().exchange(HttpRequest.POST(newTreeResourcePath, validJson), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void '2 : Test the update action correctly updates an instance'() {
        when: 'The update action is executed with valid data'
        String path = "$newTreeResourcePath/1"
        client.toBlocking().exchange(HttpRequest.PUT(path, validJson), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.BAD_REQUEST
    }

    void '3 : Test the delete action correctly deletes an instance'() {
        when: 'When the delete action is executed on an unknown instance'
        def path = "$newTreeResourcePath/99999"
        client.toBlocking().exchange(HttpRequest.DELETE(path))

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.BAD_REQUEST
    }

    void '4 : test call to full tree no containers'() {

        when: 'no containers new url'
        def response = GET(newTreeResourcePath, Argument.of(List))

        then: 'response is correct'
        response.status == HttpStatus.OK
        response.body() == []
    }

    void '5 : test single folder in existence'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": false,
    "deleted": false
  }
]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        def folderId = response.body().id
        GET(newTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '6 : test nested folders'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
        {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
        }
    ]
  }
]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new folder'
        def parentId = response.body().id
        POST("folders/$parentId/folders", [label: 'Functional Test Folder Child'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        def childId = response.body().id
        GET(newTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$parentId/folders/$childId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '7 : Test the show action for a folder id returns bad request'() {
        when: 'When the show action is called to retrieve a resource which doesnt exist'
        def id = '1'
        String path = "$newTreeResourcePath/${id}"
        client.toBlocking().exchange(HttpRequest.GET(path), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.BAD_REQUEST

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'When the show action is called to retrieve a folder'
        def folderId = response.body().id
        id = folderId
        GET("$newTreeResourcePath/${id}")

        then: 'The response is correct'
        verifyResponse(HttpStatus.BAD_REQUEST, response)

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '8 : test single versioned folder in existence'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test Folder",
    "hasChildren": false,
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  }
]'''

        when: 'creating new folder'
        POST('versionedFolders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        def folderId = response.body().id
        GET(newTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '9 : test nested folders with a versioned folder'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "VersionedFolder",
        "label": "Functional Test Versioned Folder Child",
        "hasChildren": false,
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}",
        "finalised": false,
        "documentationVersion": "1.0.0",
        "branchName": "main"
      }
    ]
  }
]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new folder'
        def parentId = response.body().id
        POST("folders/$parentId/folders", [label: 'Functional Test Folder Child'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new versioned folder'
        POST("folders/$parentId/versionedFolders", [label: 'Functional Test Versioned Folder Child'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        GET(newTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

}
