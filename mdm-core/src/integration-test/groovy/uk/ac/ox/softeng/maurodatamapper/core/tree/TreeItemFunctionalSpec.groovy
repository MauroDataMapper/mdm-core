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
import io.micronaut.http.HttpStatus

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

    void '1 : test call to full tree no containers'() {

        when: 'no containers new url'
        def response = GET(newTreeResourcePath, Argument.of(List))

        then: 'response is correct'
        response.status == HttpStatus.OK
        response.body() == []
    }

    void '2 : test single folder in existence'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": false,
    "availableActions": [],
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

    void '3 : test nested folders'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
        {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
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

    void '4 : test single versioned folder in existence'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test Folder",
    "hasChildren": false,
    "availableActions": [],
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

    void '5 : test nested folders with a versioned folder'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "VersionedFolder",
        "label": "Functional Test Versioned Folder Child",
        "hasChildren": false,
        "availableActions": [],
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

    void '6 :  test drilling down into a folder'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
        {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
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

        when: 'getting drill down'
        GET("$newTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''[{
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
        }]'''

        cleanup:
        DELETE("folders/$parentId/folders/$childId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '7 : test drilling down for a versioned folder'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "VersionedFolder",
        "label": "Functional Test Versioned Folder Child",
        "hasChildren": false,
        "availableActions": [],
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

        when: 'getting drill down'
        GET("$newTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''[
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "VersionedFolder",
        "label": "Functional Test Versioned Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}",
        "finalised": false,
        "documentationVersion": "1.0.0",
        "branchName": "main"
      }
    ]'''

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
