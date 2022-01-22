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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.tree

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.testing.functional.tree.TreeItemFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: treeItem
 *  |  GET     | /api/tree/${containerDomainType}/search/${search}                               | Action: search
 *  |  GET     | /api/tree/${containerDomainType}                                                | Action: index
 *  |  GET     | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemController
 */
@Integration
@Slf4j
class FolderTreeItemFunctionalSpec extends TreeItemFunctionalSpec {

    @Override
    String getContainerDomainType() {
        'folders'
    }

    @Transactional
    String getFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Transactional
    String getParentFolderId() {
        Folder.findByLabel('Parent Functional Test Folder').id.toString()
    }

    void 'E07 : test focus on folder'() {
        when: 'logged in as normal user'
        loginEditor()
        GET(folderId, STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Complex Test DataModel",
    "hasChildren": true,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Data Standard"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Terminology",
    "label": "Complex Test Terminology",
    "hasChildren": true,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Terminology"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Finalised Example Test DataModel",
    "hasChildren": true,
    "availableActions": [
      "delete",
      "moveToContainer",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": true,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Data Standard",
    "modelVersion": "1.0.0"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceDataModel",
    "label": "Second Simple Reference Data Model",
    "hasChildren": false,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "ReferenceDataModel"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceDataModel",
    "label": "Simple Reference Data Model",
    "hasChildren": false,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "ReferenceDataModel"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "CodeSet",
    "label": "Simple Test CodeSet",
    "hasChildren": false,
    "availableActions": [
      "delete",
      "moveToContainer",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": true,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "CodeSet",
    "modelVersion": "1.0.0"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Simple Test DataModel",
    "hasChildren": true,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Data Standard"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Terminology",
    "label": "Simple Test Terminology",
    "hasChildren": false,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Terminology"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "SourceFlowDataModel",
    "hasChildren": true,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Data Asset"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "TargetFlowDataModel",
    "hasChildren": true,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "Data Asset"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "CodeSet",
    "label": "Unfinalised Simple Test CodeSet",
    "hasChildren": false,
    "availableActions": [
      "createModelItem",
      "delete",
      "moveToContainer",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "CodeSet"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "CodeSet",
    "label": "Complex Test CodeSet",
    "hasChildren": false,
    "availableActions": [
      "delete",
      "moveToContainer",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": true,
    "superseded": false,
    "documentationVersion": "1.0.0",
    "folder": "${json-unit.matches:id}",
    "type": "CodeSet",
    "modelVersion": "1.0.0"
  }
]'''
    }

    void 'AA : test available actions for folder inside versioned folder'() {

        given: 'create VF with F inside it'
        loginEditor()
        POST('versionedFolders', [label: 'Tree Testing Versioned Folder'], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String vfId = responseBody().id
        POST("versionedFolders/$vfId/folders", [label: 'Tree Testing Folder'], MAP_ARG, true)
        verifyResponse(CREATED, response)

        when:
        HttpResponse<List<Map>> localResponse = GET('', Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)

        when:
        Map vfTree = localResponse.body().find { it.id == vfId }

        then: 'Both items have no option to create a VF'
        vfTree
        vfTree.availableActions == [
            'createFolder',
            'createModel',
            'delete',
            'moveToFolder',
            'softDelete'
        ]
        vfTree.children.size() == 1
        vfTree.children.first().availableActions == [
            'createFolder',
            'createModel',
            'delete',
            'moveToFolder',
            'moveToVersionedFolder',
            'softDelete'
        ]

        cleanup:
        DELETE("versionedFolders/$vfId?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
    }

    void 'AN01 : test getting ancestors of class item'() {
        given:
        loginReader()

        when:
        GET("dataClasses/${getParentDataClassId()}/ancestors")

        then:
        verifyResponse(OK, response)

        responseBody().id == parentFolderId
        responseBody().hasChildren
        responseBody().children.size() == 1

        responseBody().children.first().id == folderId
        responseBody().children.first().hasChildren
        responseBody().children.first().children.size() == 1

        responseBody().children.first().children.first().id == complexDataModelId
        responseBody().children.first().children.first().hasChildren
        responseBody().children.first().children.first().children.size() == 1

        responseBody().children.first().children.first().children.first().id == parentDataClassId
    }

    void 'AN02 : test getting ancestors of DataModel item'() {
        given:
        loginReader()

        when:
        GET("dataModels/${getComplexDataModelId()}/ancestors")

        then:
        verifyResponse(OK, response)

        responseBody().id == parentFolderId
        responseBody().hasChildren
        responseBody().children.size() == 1

        responseBody().children.first().id == folderId
        responseBody().children.first().hasChildren
        responseBody().children.first().children.size() == 1

        responseBody().children.first().children.first().id == complexDataModelId
        responseBody().children.first().children.first().hasChildren
    }

    String getReaderTree() {
        '''[
   {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Parent Functional Test Folder",
    "hasChildren": true,
    "availableActions": [
      
    ],
    "deleted": false,
    "children": [
      {
  
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "parentFolder": "${json-unit.matches:id}",
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Complex Test Terminology",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Finalised Example Test DataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "modelVersion": "1.0.0"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataModel",
        "label": "Second Simple Reference Data Model",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "ReferenceDataModel"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataModel",
        "label": "Simple Reference Data Model",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "ReferenceDataModel"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Simple Test CodeSet",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet",
        "modelVersion": "1.0.0"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Simple Test DataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Simple Test Terminology",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "SourceFlowDataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Asset"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "TargetFlowDataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Asset"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Unfinalised Simple Test CodeSet",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Complex Test CodeSet",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet",
        "modelVersion": "1.0.0"
      }
      ]
      }
    ]
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder 2",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  }
]'''
    }

    @Override
    String getEditorTree() {
        '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Parent Functional Test Folder",
    "hasChildren": true,
    "availableActions": [
      "createFolder",
      "createModel",
      "createVersionedFolder",
      "delete",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder",
        "hasChildren": true,
        "availableActions": [
          "createFolder",
          "createModel",
          "createVersionedFolder",
          "delete",
          "moveToFolder",
          "softDelete"
        ],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}",
        "children": [
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Complex Test DataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "label": "Complex Test Terminology",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Terminology"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Finalised Example Test DataModel",
            "hasChildren": true,
            "availableActions": [
              "delete",
              "moveToContainer",
              "moveToFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": true,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard",
            "modelVersion": "1.0.0"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceDataModel",
            "label": "Second Simple Reference Data Model",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceDataModel",
            "label": "Simple Reference Data Model",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "label": "Simple Test CodeSet",
            "hasChildren": false,
            "availableActions": [
              "delete",
              "moveToContainer",
              "moveToFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": true,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "CodeSet",
            "modelVersion": "1.0.0"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Simple Test DataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "label": "Simple Test Terminology",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Terminology"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "SourceFlowDataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Asset"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "TargetFlowDataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Asset"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "label": "Unfinalised Simple Test CodeSet",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "CodeSet"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "label": "Complex Test CodeSet",
            "hasChildren": false,
            "availableActions": [
              "delete",
              "moveToContainer",
              "moveToFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": true,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "CodeSet",
            "modelVersion": "1.0.0"
          }
        ]
      }
    ]
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder",
    "hasChildren": false,
    "availableActions": [
      "createFolder",
      "createModel",
      "delete",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder 2",
    "hasChildren": false,
    "availableActions": [
      "createFolder",
      "createModel",
      "delete",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  }
]'''
    }

    @Override
    String getAdminTree() {
        '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Parent Functional Test Folder",
    "hasChildren": true,
    "availableActions": [
      "createFolder",
      "createModel",
      "createVersionedFolder",
      "delete",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder",
        "hasChildren": true,
        "availableActions": [
          "createFolder",
          "createModel",
          "createVersionedFolder",
          "delete",
          "moveToFolder",
          "softDelete"
        ],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}",
        "children": [
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Complex Test DataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "label": "Complex Test Terminology",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Terminology"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Finalised Example Test DataModel",
            "hasChildren": true,
            "availableActions": [
              "delete",
              "moveToContainer",
              "moveToFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": true,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard",
            "modelVersion": "1.0.0"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceDataModel",
            "label": "Second Simple Reference Data Model",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceDataModel",
            "label": "Simple Reference Data Model",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "label": "Simple Test CodeSet",
            "hasChildren": false,
            "availableActions": [
              "delete",
              "moveToContainer",
              "moveToFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": true,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "CodeSet",
            "modelVersion": "1.0.0"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Simple Test DataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "label": "Simple Test Terminology",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Terminology"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "SourceFlowDataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Asset"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "TargetFlowDataModel",
            "hasChildren": true,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Asset"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "label": "Unfinalised Simple Test CodeSet",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
              "delete",
              "moveToContainer",
              "moveToFolder",
              "moveToVersionedFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "CodeSet"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "label": "Complex Test CodeSet",
            "hasChildren": false,
            "availableActions": [
              "delete",
              "moveToContainer",
              "moveToFolder",
              "softDelete"
            ],
            "deleted": false,
            "finalised": true,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "CodeSet",
            "modelVersion": "1.0.0"
          }
        ]
      }
    ]
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder 2",
    "hasChildren": false,
    "availableActions": [
      "createFolder",
      "createModel",
      "createVersionedFolder",
      "delete",
      "moveToFolder",
      "moveToVersionedFolder",
      "softDelete"
    ],
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder",
    "hasChildren": false,
    "availableActions": [
      "createFolder",
      "createModel",
      "delete",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "VersionedFolder",
    "label": "Functional Test VersionedFolder 2",
    "hasChildren": false,
    "availableActions": [
      "createFolder",
      "createModel",
      "delete",
      "moveToFolder",
      "softDelete"
    ],
    "deleted": false,
    "finalised": false,
    "documentationVersion": "1.0.0",
    "branchName": "main"
  }
]'''
    }
}
