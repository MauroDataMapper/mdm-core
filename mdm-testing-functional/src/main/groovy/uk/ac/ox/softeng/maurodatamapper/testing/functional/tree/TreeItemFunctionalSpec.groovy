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
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "parent"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "content"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
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
        "domainType": "DataClass",
        "hasChildren": true,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "parent"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "content"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "emptyclass"
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
        "domainType": "DataClass",
        "hasChildren": true,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "parent"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "content"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "modelId": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "emptyclass"
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
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Simple Test DataModel",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "SourceFlowDataModel",
        "hasChildren": false,
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
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Asset"
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
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Simple Test DataModel",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "SourceFlowDataModel",
        "hasChildren": false,
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
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Asset"
      }
    ]
  }
]
'''
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
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Simple Test DataModel",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "SourceFlowDataModel",
        "hasChildren": false,
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
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Asset"
      }
    ]
  }
]
'''
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
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": true,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "children": [
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataClass",
            "label": "emptyclass",
            "hasChildren": false,
            "modelId": "${json-unit.matches:id}"
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
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": true,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "children": [
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataClass",
            "label": "emptyclass",
            "hasChildren": false,
            "modelId": "${json-unit.matches:id}"
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
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Complex Test DataModel",
        "hasChildren": true,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "children": [
          {
            "id": "${json-unit.matches:id}",
            "domainType": "DataClass",
            "label": "emptyclass",
            "hasChildren": false,
            "modelId": "${json-unit.matches:id}"
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
    "label": "Functional Test Folder",
    "hasChildren": false,
    "deleted": false
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
    "label": "Functional Test Folder",
    "hasChildren": false,
    "deleted": false
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
    "label": "Functional Test Folder",
    "hasChildren": false,
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder 2",
    "hasChildren": false,
    "deleted": false
  }
]'''
    }


    /*
                void 'test call to tree using Terminology id'() {
                    given:
                    def id = complexTestTerminology.id

                    when: 'not logged in'
                    RestResponse response = restGet("$id")

                    then:
                    verifyResponse NOT_FOUND, response

                    when: 'logged in as reader 3 user'
                    loginUser(reader3)
                    response = restGet("$id")

                    then:
                    verifyResponse NOT_FOUND, response

                    when: 'logged in as normal user'
                    loginEditor()
                    response = restGet("$id")

                    then:
                    verifyResponse NOT_FOUND, response

                    when: 'logged in as admin user'
                    loginAdmin()
                    response = restGet("$id")

                    then:
                    verifyResponse NOT_FOUND, response
                }

             */
}
