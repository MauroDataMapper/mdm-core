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

    String getFolderTreeResourcePath() {
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
        def response = GET(folderTreeResourcePath, Argument.of(List))

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
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '3 : test nested folders'() {
        given:
        String expParent = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false
  }
]'''
        String expChild = '''[
   {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
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
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expParent

        when: 'getting the folder tree'
        GET("$folderTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expChild


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
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '5 : test nested folders with a versioned folder'() {
        given:
        String expParent = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false
  }
]'''
        String expChildren = '''[
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
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expParent

        when: 'getting the folder tree'
        GET("$folderTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expChildren

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '6 : test drilling down into a folder'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false
  }
]'''
        String expChild = '''[
{
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
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
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        when: 'getting drill down'
        GET("$folderTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expChild

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
    "deleted": false
  }
]'''
        String expChild = '''[
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
        def vfId = responseBody().id
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        when: 'getting drill down'
        GET("$folderTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expChild

        when: 'getting drill down'
        GET("$folderTreeResourcePath/$vfId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''[]'''

        when: 'getting drill down using vf domain'
        GET("tree/versionedFolders/$vfId", STRING_ARG)

        then:
        verifyResponse HttpStatus.BAD_REQUEST, jsonCapableResponse

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '8 : test drilling down into a sub-subfolder'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false
  }
]'''
        String expChild = '''[
{
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
        }
]'''

        String expGrandChild = '''[
{
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder GrandChild",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
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

        when: 'creating new folder'
        def childId = response.body().id
        POST("folders/$childId/folders", [label: 'Functional Test Folder GrandChild'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        def grandChildId = response.body().id
        GET(folderTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        when: 'getting drill down'
        GET("$folderTreeResourcePath/$parentId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expChild

        when: 'getting drill down'
        GET("$folderTreeResourcePath/$childId", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expGrandChild

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '9 : test requesting tree using VersionedFolder domain with no VFs at the root'() {

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
        def vfId = responseBody().id
        verifyResponse HttpStatus.CREATED, response

        when: 'requesting VF tree'
        GET('tree/versionedFolders', STRING_ARG)

        then: 'theres no tree at the root for VFs'
        verifyResponse HttpStatus.BAD_REQUEST, jsonCapableResponse

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '10 : test requesting tree using VersionedFolder domain with a VF at the root'() {

        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test Versioned Folder",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  }
]'''

        when: 'creating new versioned folder'
        POST('versionedFolders', [label: 'Functional Test Versioned Folder'])

        then:
        def parentId = responseBody().id
        verifyResponse HttpStatus.CREATED, response


        when: 'creating new folder'
        POST("folders/$parentId/folders", [label: 'Functional Test Folder Child'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'requesting VF tree'
        GET('tree/versionedFolders', STRING_ARG)

        then:
        verifyResponse HttpStatus.BAD_REQUEST, jsonCapableResponse

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '11. test tree with VersionedFolders: exclude model superseded'() {
        given:
        boolean includeModelSuperseded = false
        String expectedChildren = '''[
        {
            "id": "${json-unit.matches:id}",
            "domainType": "VersionedFolder",
            "label": "Functional Versioned Folder",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "parentFolder": "${json-unit.matches:id}",
            "finalised": true,
            "documentationVersion": "1.0.0",
            "modelVersion": "2.0.0"
        }
    ]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new versioned folder'
        def parentId = response.body().id
        POST("folders/$parentId/versionedFolders", [label: 'Functional Versioned Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'finalise versioned folder for v1'
        def versionedFolderVer1Id = response.body().id
        PUT("versionedFolders/$versionedFolderVer1Id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse HttpStatus.OK, response

        when: 'create next model version of versioned folder'
        PUT("versionedFolders/$versionedFolderVer1Id/newBranchModelVersion", [:])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'finalise versioned folder for v2'
        def versionedFolderVer2Id = response.body().id
        PUT("versionedFolders/$versionedFolderVer2Id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse HttpStatus.OK, response

        when: 'getting folder tree'
        GET("$folderTreeResourcePath/$parentId?includeModelSuperseded=$includeModelSuperseded", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expectedChildren

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '12. test tree with VersionedFolders: include model superseded'() {
        given:
        boolean includeModelSuperseded = true
        String expectedChildren = '''[
        {
            "id": "${json-unit.matches:id}",
            "domainType": "VersionedFolder",
            "label": "Functional Versioned Folder",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "parentFolder": "${json-unit.matches:id}",
            "finalised": true,
            "documentationVersion": "1.0.0",
            "modelVersion": "1.0.0"
        },
        {
            "id": "${json-unit.matches:id}",
            "domainType": "VersionedFolder",
            "label": "Functional Versioned Folder",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "parentFolder": "${json-unit.matches:id}",
            "finalised": true,
            "documentationVersion": "1.0.0",
            "modelVersion": "2.0.0"
        }
    ]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new versioned folder'
        def parentId = response.body().id
        POST("folders/$parentId/versionedFolders", [label: 'Functional Versioned Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'finalise versioned folder for v1'
        def versionedFolderVer1Id = response.body().id
        PUT("versionedFolders/$versionedFolderVer1Id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse HttpStatus.OK, response

        when: 'create next model version of versioned folder'
        PUT("versionedFolders/$versionedFolderVer1Id/newBranchModelVersion", [:])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'finalise versioned folder for v2'
        def versionedFolderVer2Id = response.body().id
        PUT("versionedFolders/$versionedFolderVer2Id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse HttpStatus.OK, response

        when: 'getting folder tree'
        GET("$folderTreeResourcePath/$parentId?includeModelSuperseded=$includeModelSuperseded", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expectedChildren

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '13. test tree with VersionedFolders: exclude deleted'() {
        given:
        boolean includeDeleted = false
        String expectedChildren = '''[
        {
            "id": "${json-unit.matches:id}",
            "domainType": "VersionedFolder",
            "label": "Functional Versioned Folder",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "parentFolder": "${json-unit.matches:id}",
            "finalised": true,
            "documentationVersion": "1.0.0",
            "modelVersion": "1.0.0"
        }
    ]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new versioned folder'
        def parentId = response.body().id
        POST("folders/$parentId/versionedFolders", [label: 'Functional Versioned Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'finalise versioned folder for v1'
        def versionedFolderVer1Id = response.body().id
        PUT("versionedFolders/$versionedFolderVer1Id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse HttpStatus.OK, response

        when: 'create next model version of versioned folder'
        PUT("versionedFolders/$versionedFolderVer1Id/newBranchModelVersion", [:])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'mark draft versioned folder as deleted'
        def versionedFolderVer2Id = response.body().id
        DELETE("versionedFolders/$versionedFolderVer2Id?permanent=false")

        then:
        verifyResponse HttpStatus.OK, response

        when: 'getting folder tree'
        GET("$folderTreeResourcePath/$parentId?includeDeleted=$includeDeleted", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expectedChildren

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '14. test tree with VersionedFolders: include deleted'() {
        given:
        boolean includeDeleted = true
        String expectedChildren = '''[
        {
            "id": "${json-unit.matches:id}",
            "domainType": "VersionedFolder",
            "label": "Functional Versioned Folder",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "parentFolder": "${json-unit.matches:id}",
            "finalised": true,
            "documentationVersion": "1.0.0",
            "modelVersion": "1.0.0"
        },
        {
            "id": "${json-unit.matches:id}",
            "domainType": "VersionedFolder",
            "label": "Functional Versioned Folder",
            "hasChildren": false,
            "availableActions": [],
            "deleted": true,
            "parentFolder": "${json-unit.matches:id}",
            "finalised": false,
            "documentationVersion": "1.0.0",
            "branchName": "main"
        }
    ]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new versioned folder'
        def parentId = response.body().id
        POST("folders/$parentId/versionedFolders", [label: 'Functional Versioned Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'finalise versioned folder for v1'
        def versionedFolderVer1Id = response.body().id
        PUT("versionedFolders/$versionedFolderVer1Id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse HttpStatus.OK, response

        when: 'create next model version of versioned folder'
        PUT("versionedFolders/$versionedFolderVer1Id/newBranchModelVersion", [:])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'mark draft versioned folder as deleted'
        def versionedFolderVer2Id = response.body().id
        DELETE("versionedFolders/$versionedFolderVer2Id?permanent=false")

        then:
        verifyResponse HttpStatus.OK, response

        when: 'getting folder tree'
        GET("$folderTreeResourcePath/$parentId?includeDeleted=$includeDeleted", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, expectedChildren

        cleanup:
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
