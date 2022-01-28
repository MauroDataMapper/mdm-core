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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.tree


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: treeItem
 *  |  GET     | /api/tree/${containerDomainType}/search/${searchTerm}                           | Action: search
 *  |  GET     | /api/tree/${containerDomainType}                                                | Action: index
 *  |  GET     | /api/tree/${containerDomainType}/${catalogueItemDomainType}/${catalogueItemId}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemController
 */
@Slf4j
abstract class TreeItemFunctionalSpec extends FunctionalSpec {

    abstract String getContainerDomainType()

    abstract String getReaderTree()

    abstract String getEditorTree()

    abstract String getAdminTree()

    @Override
    String getResourcePath() {
        "tree/${getContainerDomainType()}"
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getParentDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(getComplexDataModelId()), 'parent').get().id.toString()
    }

    void 'L01 : test call to full tree (not logged in)'() {

        when: 'not logged in'
        GET('')

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'N01 : test call to full tree (as authenticated/no access)'() {
        when: 'logged in '
        loginAuthenticated()
        GET('')

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'R01 : test call to full tree (as reader)'() {
        when: 'logged in '
        loginReader()
        GET('', STRING_ARG)

        then:
        verifyJsonResponse OK, getReaderTree()
    }

    void 'E01 : test call to full tree (as editor)'() {
        when: 'logged in as normal user'
        loginEditor()
        GET('', STRING_ARG)

        then:
        verifyJsonResponse OK, getEditorTree()
    }

    void 'A01 : test call to full tree (as admin)'() {
        when: 'logged in as admin user'
        loginAdmin()
        GET('', STRING_ARG)

        then:
        verifyJsonResponse OK, getAdminTree()
    }

    void 'L02 : test call to tree using DataModel id (as not logged in)'() {

        when: 'not logged in'
        GET("dataModels/${getComplexDataModelId()}")

        then:
        verifyNotFound response, getComplexDataModelId()
    }

    void 'N02 : test call to tree using DataModel id (as authenticated/no access)'() {
        when:
        loginAuthenticated()
        GET("dataModels/${getComplexDataModelId()}")

        then:
        verifyNotFound response, getComplexDataModelId()
    }

    void 'R02 : test call to tree using DataModel id (as reader)'() {
        when: 'logged in as reader 3 user'
        loginReader()
        GET("dataModels/${getComplexDataModelId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
      {
        "domainType": "DataClass",
        "hasChildren": true,
        "availableActions": [],
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "parent"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "availableActions": [],
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "content"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "availableActions": [],
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "emptyclass"
      }
    ]'''
    }

    void 'E02 : test call to tree using DataModel id (as editor)'() {
        when: 'logged in as normal user'
        loginEditor()
        GET("dataModels/${getComplexDataModelId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "emptyclass",
    "hasChildren": false,
    "availableActions": [
      "createModelItem"
    ],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "content",
    "hasChildren": false,
    "availableActions": [
      "createModelItem"
    ],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "parent",
    "hasChildren": true,
    "availableActions": [
      "createModelItem"
    ],
    "modelId": "${json-unit.matches:id}"
  }
]'''
    }

    void 'A02 : test call to tree using DataModel id (as admin)'() {
        when: 'logged in as admin user'
        loginAdmin()
        GET("dataModels/${getComplexDataModelId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "emptyclass",
    "hasChildren": false,
    "availableActions": [
      "createModelItem",
      "delete"
    ],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "content",
    "hasChildren": false,
    "availableActions": [
      "createModelItem",
      "delete"
    ],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "parent",
    "hasChildren": true,
    "availableActions": [
      "createModelItem",
      "delete"
    ],
    "modelId": "${json-unit.matches:id}"
  }
]'''
    }

    void 'L03 : test call to tree using DataClass id (as not logged in)'() {

        when: 'not logged in'
        GET("dataClasses/${getParentDataClassId()}")

        then:
        verifyNotFound response, getParentDataClassId()
    }

    void 'N03 : test call to tree using DataClass id (as authenticated/no access)'() {
        when:
        loginAuthenticated()
        GET("dataClasses/${getParentDataClassId()}")

        then:
        verifyNotFound response, getParentDataClassId()
    }

    void 'R03 : test call to tree using DataClass id (as reader)'() {
        when: 'logged in as reader 3'
        loginReader()
        GET("dataClasses/${getParentDataClassId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "availableActions": [],
        "modelId": "${json-unit.matches:id}",
        "parentId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child"
      }
    ]'''
    }

    void 'E03 : test call to tree using DataClass id (as editor)'() {
        when:
        loginEditor()
        GET("dataClasses/${getParentDataClassId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "availableActions": ["createModelItem"],
        "modelId": "${json-unit.matches:id}",
        "parentId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child"
      }
    ]'''
    }

    void 'A03 : test call to tree using DataClass id (as admin)'() {
        when: 'logged in as admin user'
        loginAdmin()
        GET("dataClasses/${getParentDataClassId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "availableActions": ["createModelItem","delete"],
        "modelId": "${json-unit.matches:id}",
        "parentId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child"
      }
    ]'''
    }

    void 'L04 : test searching for "model" (as not logged in)'() {

        when: 'not logged in'
        GET('search/model')

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'N04 : test searching for "model" (as authenticated/no access)'() {

        when:
        loginAuthenticated()
        GET('search/model')

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'R04 : test searching for "model" (as reader)'() {
        when: 'logged in as reader '
        loginReader()
        GET('search/model', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
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
        "availableActions": [
          
        ],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}",
        "children": [
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Complex Test DataModel",
            "hasChildren": false,
            "availableActions": [
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Finalised Example Test DataModel",
            "hasChildren": false,
            "availableActions": [
              
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
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceDataModel",
            "label": "Simple Reference Data Model",
            "hasChildren": false,
            "availableActions": [
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Simple Test DataModel",
            "hasChildren": false,
            "availableActions": [
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "SourceFlowDataModel",
            "hasChildren": false,
            "availableActions": [
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Asset",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "TargetFlowDataModel",
            "hasChildren": false,
            "availableActions": [
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Asset",
            "branchName": "main"
          }
        ]
      }
    ]
  }
]
'''
    }

    void 'E04 : test searching for "model" (as editor)'() {
        when: 'logged in as normal user'
        loginEditor()
        GET('search/model', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Parent Functional Test Folder",
    "hasChildren": true,
    "availableActions": [
      "createFolder",
      "createModel",
      "createVersionedFolder",
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
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
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
            "type": "Data Standard",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Finalised Example Test DataModel",
            "hasChildren": false,
            "availableActions": [
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
            "type": "ReferenceDataModel",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceDataModel",
            "label": "Simple Reference Data Model",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
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
            "type": "ReferenceDataModel",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Simple Test DataModel",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
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
            "type": "Data Standard",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "SourceFlowDataModel",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
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
            "type": "Data Asset",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "TargetFlowDataModel",
            "hasChildren": false,
            "availableActions": [
              "createModelItem",
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
            "type": "Data Asset",
            "branchName": "main"
          }
        ]
      }
    ]
  }
]'''
    }

    void 'A04 : test searching for "model" (as admin)'() {
        when: 'logged in as admin user'
        loginAdmin()
        GET('search/model', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
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
            "type": "Data Standard",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Finalised Example Test DataModel",
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
            "type": "ReferenceDataModel",
            "branchName": "main"
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
            "type": "ReferenceDataModel",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Simple Test DataModel",
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
            "type": "Data Standard",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "SourceFlowDataModel",
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
            "type": "Data Asset",
            "branchName": "main"
          },
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "TargetFlowDataModel",
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
            "type": "Data Asset",
            "branchName": "main"
          }
        ]
      }
    ]
  }
]'''
    }

    void 'L05 : test searching for "emptyclass" (as not logged in)'() {

        when: 'not logged in'
        GET("search/emptyclass")

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'N05 : test searching for "emptyclass" (as authenticated/no access)'() {

        when:
        loginAuthenticated()
        GET("search/emptyclass")

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'R05 : test searching for "emptyclass" (as reader)'() {
        when: 'logged in as reader'
        loginReader()
        GET("search/emptyclass", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
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
        "availableActions": [
          
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
              
            ],
            "deleted": false,
            "finalised": false,
            "superseded": false,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard",
            "branchName": "main",
            "children": [
              {
                "id": "${json-unit.matches:id}",
                "domainType": "DataClass",
                "label": "emptyclass",
                "hasChildren": false,
                "availableActions": [
                  
                ],
                "modelId": "${json-unit.matches:id}"
              }
            ]
          }
        ]
      }
    ]
  }
]'''
    }

    void 'E05 : test searching for "emptyclass" (as editor)'() {
        when: 'logged in as normal user'
        loginEditor()
        GET("search/emptyclass", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Parent Functional Test Folder",
    "hasChildren": true,
    "availableActions": [
      "createFolder",
      "createModel",
      "createVersionedFolder",
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
            "type": "Data Standard",
            "branchName": "main",
            "children": [
              {
                "id": "${json-unit.matches:id}",
                "domainType": "DataClass",
                "label": "emptyclass",
                "hasChildren": false,
                "availableActions": [
                  "createModelItem"
                ],
                "modelId": "${json-unit.matches:id}"
              }
            ]
          }
        ]
      }
    ]
  }
]'''
    }

    void 'A05 : test searching for "emptyclass" (as admin)'() {
        when: 'logged in as admin user'
        loginAdmin()
        GET("search/emptyclass", STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
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
            "type": "Data Standard",
            "branchName": "main",
            "children": [
              {
                "id": "${json-unit.matches:id}",
                "domainType": "DataClass",
                "label": "emptyclass",
                "hasChildren": false,
                "availableActions": [
                  "createModelItem",
                  "delete"
                ],
                "modelId": "${json-unit.matches:id}"
              }
            ]
          }
        ]
      }
    ]
  }
]'''
    }


    void 'L06 : test call to full tree folders only (not logged in)'() {

        when: 'not logged in'
        GET('?foldersOnly=true')

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'N06 : test call to full tree folders only (as authenticated/no access)'() {
        when: 'logged in '
        loginAuthenticated()
        GET('?foldersOnly=true')

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'R06 : test call to full tree folders only (as reader)'() {
        when: 'logged in '
        loginReader()
        GET('?foldersOnly=true', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
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
        "hasChildren": false,
        "availableActions": [
          
        ],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
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

    void 'E06 : test call to full tree folders only (as editor)'() {
        when: 'logged in as normal user'
        loginEditor()
        GET('?foldersOnly=true', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Parent Functional Test Folder",
    "hasChildren": true,
    "availableActions": [
      "createFolder",
      "createModel",
      "createVersionedFolder",
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
        "hasChildren": false,
        "availableActions": [
          "createFolder",
          "createModel",
          "createVersionedFolder",
          "moveToFolder",
          "softDelete"
        ],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
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

    void 'A06 : test call to full tree folders only (as admin)'() {
        when: 'logged in as admin user'
        loginAdmin()
        GET('?foldersOnly=true', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
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
        "hasChildren": false,
        "availableActions": [
          "createFolder",
          "createModel",
          "createVersionedFolder",
          "delete",
          "moveToFolder",
          "softDelete"
        ],
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
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
