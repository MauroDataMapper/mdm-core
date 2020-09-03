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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: dataModel
 *
 *  |  GET     | /api/dataModels        | Action: index
 *  |  DELETE  | /api/dataModels/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${id}  | Action: update
 *  |  GET     | /api/dataModels/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/dataModels                 | Action: save
 *
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *
 *  |  GET     | /api/dataModels/types  | Action: types
 *  |  GET     | /api/dataModels/${dataModelId}/hierarchy  | Action: hierarchy
 *  |  PUT     | /api/dataModels/${dataModelId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/dataModels/${dataModelId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/dataModels/${dataModelId}      | Action: changeFolder
 *  |  PUT     | /api/dataModels/${dataModelId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/dataModels/providers/defaultDataTypeProviders       | Action: defaultDataTypeProviders
 *  |  GET     | /api/dataModels/providers/importers                      | Action: importerProviders
 *  |  GET     | /api/dataModels/providers/exporters                      | Action: exporterProviders
 *  |  GET     | /api/dataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 *
 *  |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelController
 */
@Integration
@Slf4j
class DataModelFunctionalSpec extends ModelUserAccessAndPermissionChangingFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Transactional
    String getTestFolder2Id() {
        Folder.findByLabel('Functional Test Folder 2').id.toString()
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getSimpleDataModelId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
    }

    @Transactional
    String getDataModelFolderId(String id) {
        DataModel.get(id).folder.id.toString()
    }

    @Override
    String getResourcePath() {
        'dataModels'
    }

    @Override
    String getSavePath() {
        "folders/${getTestFolderId()}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test DataModel'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: 'Complex Test DataModel'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'This is a new testing DataModel'
        ]
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
    }

    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'delete', 'finalise']
    }

    List<String> getReaderAvailableActions() {
        ['show', 'comment']
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Data Standard:Functional Test DataModel] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Data Standard:Functional Test DataModel] changed properties \[description]/
    }

    @Override
    Boolean isDisabledNotDeleted() {
        true
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    String getModelType() {
        'DataModel'
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel",
      "type": "Data Standard",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "SourceFlowDataModel",
      "type": "Data Asset"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Complex Test DataModel",
      "type": "Data Standard",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "author": "admin person",
      "organisation": "brc"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "TargetFlowDataModel",
      "type": "Data Asset"
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Functional Test DataModel",
  "finalised": false,
  "type": "Data Standard",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "availableActions": [
    "delete",
    "softDelete",
    "update",
    "save",
    "show",
    "comment",
    "editDescription",
    "finalise"
  ],
  "branchName":"main"
}'''
    }

    void 'Test getting DataModel types'() {
        when: 'not logged in'
        GET('types')

        then:
        verifyForbidden response

        when: 'logged in'
        loginAuthenticated()
        GET('types')

        then:
        verifyResponse OK, response
        response.getBody(String).get() == '["Data Asset","Data Standard"]'

    }

    void 'L16 : test getting DataModel hierarchy (as not logged in)'() {
        given:
        String id = getSimpleDataModelId()

        when: 'not logged in'
        GET("${id}/hierarchy")

        then:
        verifyNotFound response, id
    }

    void 'N16 : test getting DataModel hierarchy (as authenticated/no access)'() {
        when: 'authenticated user'
        String id = getSimpleDataModelId()
        loginAuthenticated()
        GET("${id}/hierarchy")

        then:
        verifyNotFound response, id
    }

    void 'R16 : test getting DataModel hierarchy (as reader)'() {
        when: 'logged in as reader'
        String id = getSimpleDataModelId()
        loginReader()
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Simple Test DataModel",
  "availableActions": [
    "show",
    "comment"
  ],
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ],
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "dataTypes": [
    
  ],
  "childDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
    }
  ]
}'''
    }

    void 'E16a : test getting DataModel hierarchy (as editor)'() {
        when: 'logged in as editor'
        String id = getSimpleDataModelId()
        loginEditor()
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Simple Test DataModel",
  "availableActions": [
        "delete",
        "softDelete",
        "update",
        "save",
        "show",
        "comment",
        "editDescription",
        "finalise"
      ],
      "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ],
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "dataTypes": [
    
  ],
  "childDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "delete",
        "update",
        "save",
        "show",
        "comment",
        "editDescription",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
    }
  ]
}'''
    }

    void 'E16b : test getting complex DataModel hierarchy (as editor)'() {
        when: 'logged in as editor'
        String id = getComplexDataModelId()
        loginEditor()
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Complex Test DataModel",
  "availableActions": [
    "show",
    "comment",
    "editDescription",
    "update",
    "save",
    "softDelete",
    "delete",
    "finalise"
  ],
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ],
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "author": "admin person",
  "organisation": "brc",
  "dataTypes": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "EnumerationType",
      "label": "yesnounknown",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "key": "N",
          "value": "No",
          "category": null
        },
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "integer",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "string",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceType",
      "label": "child",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "referenceClass": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataClass",
        "label": "child",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "domainType": "DataModel",
            "finalised": false
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "parent",
            "domainType": "DataClass"
          }
        ],
        "parentDataClass": "${json-unit.matches:id}"
      }
    }
  ],
  "childDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "content",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "A dataclass with elements",
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "maxMultiplicity": 1,
      "minMultiplicity": 0,
      "dataClasses": [
        
      ],
      "dataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "ele1",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "content",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "show",
            "comment",
            "editDescription",
            "update",
            "save",
            "delete",
            "finalise"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "dataClass": "${json-unit.matches:id}",
          "dataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "PrimitiveType",
            "label": "string",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          "maxMultiplicity": 20,
          "minMultiplicity": 0
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "element2",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "content",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "show",
            "comment",
            "editDescription",
            "update",
            "save",
            "delete",
            "finalise"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "dataClass": "${json-unit.matches:id}",
          "dataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "PrimitiveType",
            "label": "integer",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          "maxMultiplicity": 1,
          "minMultiplicity": 1
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "emptyclass",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "dataclass with desc",
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "parent",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete",
        "finalise"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "maxMultiplicity": -1,
      "minMultiplicity": 1,
      "dataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataClass",
          "label": "child",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "parent",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "show",
            "comment",
            "editDescription",
            "update",
            "save",
            "delete",
            "finalise"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "parentDataClass": "${json-unit.matches:id}",
          "dataClasses": [
            
          ],
          "dataElements": [
            
          ],
          "parentDataClass": "${json-unit.matches:id}"
        }
      ],
      "dataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "child",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "parent",
              "domainType": "DataClass"
            }
          ],
          "availableActions": [
            "show",
            "comment",
            "editDescription",
            "update",
            "save",
            "delete",
            "finalise"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "dataClass": "${json-unit.matches:id}",
          "dataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceType",
            "label": "child",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ],
            "referenceClass": {
              "id": "${json-unit.matches:id}",
              "domainType": "DataClass",
              "label": "child",
              "model": "${json-unit.matches:id}",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                },
                {
                  "id": "${json-unit.matches:id}",
                  "label": "parent",
                  "domainType": "DataClass"
                }
              ],
              "parentDataClass": "${json-unit.matches:id}"
            }
          },
          "maxMultiplicity": 1,
          "minMultiplicity": 1
        }
      ]
    }
  ]
}'''
    }

    void 'Test finalising DataModel'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/finalise", [:])

        then:
        verifyNotFound response, id

        when: 'authenticated user'
        PUT("$id/finalise", [:])

        then:
        verifyNotFound response, id

        when: 'logged in as reader'
        loginReader()
        PUT("$id/finalise", [:])

        then:
        verifyForbidden response

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/finalise", [:])

        then:
        verifyResponse OK, response
        response.body().finalised == true
        response.body().dateFinalised
        response.body().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]

        when: 'log out and log back in again in as editor available actions are correct'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]

        when: 'log out and log back in again in as admin available actions are correct'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]

        cleanup:
        removeValidIdObject(id)
    }

    void 'L19 : test changing folder from DataModel context (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N19 : test changing folder from DataModel context (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R19 : test changing folder from DataModel context (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E19 : test changing folder from DataModel context (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the datamodel but not the folder 2'
        loginEditor()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'A19 : test changing folder from DataModel context (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyResponse OK, response

        and:
        getDataModelFolderId(id) == getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'L20 : test changing folder from Folder context (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N20 : test changing folder from Folder context (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R20 : test changing folder from Folder context (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E20 : test changing folder from Folder context (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the datamodel but not the folder 2'
        loginEditor()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'A20 : test changing folder from Folder context (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        and:
        getDataModelFolderId(id) == getTestFolder2Id()

        when: 'logged in as reader as no access to folder 2 or reader share'
        loginReader()
        GET(id)

        then:
        verifyNotFound response, id

        when: 'logged in as editor no access to folder 2 but has direct DM access'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)
    }


    void 'Test getting available DataModel default datatype providers'() {
        when: 'not logged in'
        GET("providers/defaultDataTypeProviders")

        then:
        verifyForbidden response

        when: 'logged in as authenticated user'
        loginAuthenticated()
        GET('providers/defaultDataTypeProviders', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "dataTypes": [
      {
        "domainType": "PrimitiveType",
        "description": "A piece of text",
        "label": "Text"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A whole number",
        "label": "Number"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A decimal number",
        "label": "Decimal"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A date",
        "label": "Date"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A date with a timestamp",
        "label": "DateTime"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A timestamp",
        "label": "Timestamp"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A true or false value",
        "label": "Boolean"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A time period in arbitrary units",
        "label": "Duration"
      }
    ],
    "displayName": "Basic Default DataTypes",
    "name": "DataTypeService",
    "version": "1.0.0"
  }
]'''
    }

    void 'Test getting available DataModel exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "JsonExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON DataModel Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [],
    "providerType": "DataModelExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": false
  },
  {
    "name": "XmlExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML DataModel Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [],
    "providerType": "DataModelExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": false
  }
]'''
    }

    void 'Test getting available DataModel importers'() {

        when: 'not logged in then inaccessible'
        GET('providers/importers')

        then:
        verifyForbidden response

        when: 'logged in'
        loginAuthenticated()
        GET('providers/importers', STRING_ARG)

        then: 'The response is Unauth'
        verifyJsonResponse OK, '''[
  {
    "name": "JsonImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON DataModel Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [],
    "providerType": "DataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "XmlImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML DataModel Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [],
    "providerType": "DataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": true
  }
]'''

    }

    void 'L21 : test diffing 2 DataModels (as not logged in)'() {

        when: 'not logged in'
        GET("${getComplexDataModelId()}/diff/${getSimpleDataModelId()}")

        then:
        verifyNotFound response, getComplexDataModelId()
    }

    void 'N21 : test diffing 2 DataModels (as authenticated/no access)'() {
        when:
        loginAuthenticated()
        GET("${getComplexDataModelId()}/diff/${getSimpleDataModelId()}")

        then:
        verifyNotFound response, getComplexDataModelId()
    }

    void 'R21A : test diffing 2 DataModels (as reader of LH model)'() {
        given:
        String id = getValidId()
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when: 'able to read right model only'
        loginReader()
        GET("${getComplexDataModelId()}/diff/${id}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R21B : test diffing 2 DataModels (as reader of RH model)'() {
        given:
        String id = getValidId()
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when:
        loginReader()
        GET("${id}/diff/${getComplexDataModelId()}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R21C : test diffing 2 DataModels (as reader of both models)'() {
        when:
        loginReader()
        GET("${getComplexDataModelId()}/diff/${getSimpleDataModelId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedDiffJson()
    }

    void 'L22 : test export a single DataModel (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N22 : test export a single DataModel (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R22 : test export a single DataModel (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    }
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L23 : test export multiple DataModels (json only exports first id) (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [dataModelIds: [id, getSimpleDataModelId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N23 : test export multiple DataModels (json only exports first id) (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [dataModelIds: [id, getSimpleDataModelId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R23 : test export multiple DataModels (json only exports first id) (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [dataModelIds: [id, getSimpleDataModelId()]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    }
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L24 : test import basic DataModel (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'N24 : test import basic DataModel (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, testFolderId

        cleanup:
        removeValidIdObject(id)
    }

    void 'R24 : test import basic DataModel (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E24A : test import basic DataModel (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test Import'
        response.body().items.first().id != id
        String id2 = response.body().items.first().id

        cleanup:
        removeValidIdObjectUsingTransaction(id2)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(id2, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }

    void 'E24B : test import basic DataModel as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test DataModel'
        response.body().items.first().id != id

        when:
        String newId = response.body().items.first().id
        GET("$newId/versionLinks")

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().domainType == 'VersionLink'
        response.body().items.first().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        response.body().items.first().sourceModel.id == newId
        response.body().items.first().targetModel.id == id
        response.body().items.first().sourceModel.domainType == response.body().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(newId)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(newId, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }


    void 'S01 : test searching for label "emptyclass" in complex model'() {
        given:
        def term = 'emptyclass'

        when: 'not logged in'
        GET("${getComplexDataModelId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getComplexDataModelId()}/search?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.size() == 1
        responseBody().items.first().label == 'emptyclass'
        responseBody().items.first().domainType == 'DataClass'
        responseBody().items.first().breadcrumbs.size() == 1
    }

    void 'S02 : test searching for label "emptyclass" in simple model'() {
        given:
        def term = 'emptyclass'

        when: 'not logged in'
        GET("${getSimpleDataModelId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getSimpleDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getSimpleDataModelId()}/search?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'S03 : test searching for label "c*" in complex model'() {
        given:
        def term = 'c*'

        when: 'not logged in'
        GET("${getComplexDataModelId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getComplexDataModelId()}/search?searchTerm=${term}&sort=domainType")

        then:
        verifyResponse OK, response
        responseBody().count == 4
        responseBody().items.size() == 4
        responseBody().items[0].label == 'child'
        responseBody().items[0].domainType == 'DataClass'
        responseBody().items[1].label == 'content'
        responseBody().items[1].domainType == 'DataClass'
        responseBody().items[2].label == 'child'
        responseBody().items[2].domainType == 'DataElement'
        responseBody().items[3].label == 'child'
        responseBody().items[3].domainType == 'ReferenceType'

    }

    void 'S04 : test searching for label "c*" in complex model using post'() {
        given:
        def term = 'c*'

        when: 'not logged in'
        POST("${getComplexDataModelId()}/search", [searchTerm: term, sort: 'label'])

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        POST("${getComplexDataModelId()}/search", [
            searchTerm : term,
            sort       : 'label',
            domainTypes: ['DataClass'],
        ])

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.size() == 2
        responseBody().items[0].label == 'child'
        responseBody().items[0].domainType == 'DataClass'
        responseBody().items[1].label == 'content'
        responseBody().items[1].domainType == 'DataClass'
    }

    /*
            void 'test deleting multiple models'() {
                given:
                def idstoDelete = []
                def response
                loginEditor()
                (1..4).each {n ->
                    response = post('') {
                        json {
                            folder = Folder.findByLabel('Test Folder').id.toString()
                            label = "Functional Test Model ${n}"
                        }
                    }
                    verifyResponse CREATED, response
                    idstoDelete << response.json.id
                }
                logout()

                when:
                loginEditor()
                response = delete('') {
                    json {
                        ids = idstoDelete
                    }
                }

                then:
                verifyUnauthorised response

                when:
                loginAdmin()
                response = delete('') {
                    json {
                        ids = idstoDelete
                        permanent = false
                    }
                }

                then:
                verifyResponse(OK, response, '''{
                          "count": 4,
                          "items": [
                            {
                              "deleted": true,
                              "domainType": "DataModel",
                              "documentationVersion": "1.0.0",
                              "id": "${json-unit.matches:id}",
                              "label": "Functional Test Model 1",
                              "type": "Data Standard"
                            },
                            {
                              "deleted": true,
                              "domainType": "DataModel",
                              "documentationVersion": "1.0.0",
                              "id": "${json-unit.matches:id}",
                              "label": "Functional Test Model 2",
                              "type": "Data Standard"
                            },
                            {
                              "deleted": true,
                              "domainType": "DataModel",
                              "documentationVersion": "1.0.0",
                              "id": "${json-unit.matches:id}",
                              "label": "Functional Test Model 3",
                              "type": "Data Standard"
                            },
                            {
                              "deleted": true,
                              "domainType": "DataModel",
                              "documentationVersion": "1.0.0",
                              "id": "${json-unit.matches:id}",
                              "label": "Functional Test Model 4",
                              "type": "Data Standard"
                            }
                          ]
                        }''')

                when:
                loginAdmin()
                response = delete('') {
                    json {
                        ids = idstoDelete
                    }
                }

                then:
                verifyResponse(NO_CONTENT, response)
            }

            /*

            void setupForLinkSuggestions() {
            loginEditor()
            DataType newDataType = simpleTestDataModel.findDataTypeByLabel("string")
            def response
            if (!newDataType) {
            response = post(apiPath + "/dataModels/${getSimpleDataModelId()}/dataTypes") {
            json {
            domainType = 'PrimitiveType'
            label = 'string'
            }
            }
            assert (response.statusCode.'2xxSuccessful')
            newDataType = simpleTestDataModel.findDataTypeByLabel("string")
            }
            DataClass targetDataClass = DataClass.findByDataModelAndLabel(simpleTestDataModel, "simple")

            response = post(apiPath + "/dataModels/${getSimpleDataModelId()}/dataClasses/${targetDataClass.id}/dataElements") {
            json {
            domainType = 'DataElement'
            label = 'ele1'
            description = 'most obvious match'
            dataType = {
            domainType = 'PrimitiveType'
            id = newDataType.id.toString()
            }

            }
            }
            assert (response.statusCode.'2xxSuccessful')
            response = post(apiPath + "/dataModels/${getSimpleDataModelId()}/dataClasses/${targetDataClass.id}/dataElements") {
            json {
            domainType = 'DataElement'
            label = 'ele2'
            description = 'least obvious match'
            dataType = {
            domainType = 'PrimitiveType'
            id = newDataType.id.toString()
            }

            }
            }
            assert (response.statusCode.'2xxSuccessful')
            adminService.rebuildLuceneIndexes(new LuceneIndexParameters())
            logout()
            }

            void 'test get link suggestions for a model'() {
            given:
            setupForLinkSuggestions()

            String endpoint = "${apiPath}/" +
                  "dataModels/${getComplexDataModelId()}/" +
                  "suggestLinks/${getSimpleDataModelId()}"


            String expectedJson = expectedLinkSuggestions(expectedLinkSuggestionResults())


            when: 'not logged in'
            GET(endpoint)

            then:
            verifyResponse UNAUTHORIZED, response

            when: 'logged in as reader'
            loginUser(reader2)
            GET(endpoint)

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as writer'
            loginEditor()
            GET(endpoint)

            then:
            verifyResponse OK, response, expectedJson
            }


            void 'test get link suggestions for a model with no data elements in the target'() {
            given:

            String endpoint = "${apiPath}/" +
                  "dataModels/${getComplexDataModelId()}/" +
                  "suggestLinks/${getSimpleDataModelId()}"

            String expectedJson = expectedLinkSuggestions(["", "", ""])

            when: 'not logged in'
            GET(endpoint)

            then:
            verifyResponse UNAUTHORIZED, response

            when: 'logged in as reader'
            loginUser(reader2)
            GET(endpoint)

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as writer'
            loginEditor()
            GET(endpoint)

            then:
            verifyResponse OK, response, expectedJson
            }

            String expectedLinkSuggestions(List<String> results) {
            '''{
              "links": [
                {
                  "sourceDataElement": {
                    "domainType": "DataElement",
                    "dataClass": "${json-unit.matches:id}",
                    "dataType": {
                      "domainType": "ReferenceType",
                      "dataModel": "${json-unit.matches:id}",
                      "id": "${json-unit.matches:id}",
                      "label": "child",
                      "breadcrumbs": [
                        {
                          "domainType": "DataModel",
                          "finalised": false,
                          "id": "${json-unit.matches:id}",
                          "label": "Complex Test DataModel"
                        }
                      ],
                      "referenceClass": {
                        "domainType": "DataClass",
                        "dataModel": "${json-unit.matches:id}",
                        "parentDataClass": "${json-unit.matches:id}",
                        "id": "${json-unit.matches:id}",
                        "label": "child",
                        "breadcrumbs": [
                          {
                            "domainType": "DataModel",
                            "finalised": false,
                            "id": "${json-unit.matches:id}",
                            "label": "Complex Test DataModel"
                          },
                          {
                            "domainType": "DataClass",
                            "id": "${json-unit.matches:id}",
                            "label": "parent"
                          }
                        ]
                      }
                    },
                    "dataModel": "${json-unit.matches:id}",
                    "maxMultiplicity": 1,
                    "id": "${json-unit.matches:id}",
                    "label": "child",
                    "minMultiplicity": 1,
                    "breadcrumbs": [
                      {
                        "domainType": "DataModel",
                        "finalised": false,
                        "id": "${json-unit.matches:id}",
                        "label": "Complex Test DataModel"
                      },
                      {
                        "domainType": "DataClass",
                        "id": "${json-unit.matches:id}",
                        "label": "parent"
                      }
                    ]
                  },
                  "results": [''' + results[2] + '''

                  ]
                },
                {
                  "sourceDataElement": {
                    "domainType": "DataElement",
                    "dataClass": "${json-unit.matches:id}",
                    "dataType": {
                      "domainType": "PrimitiveType",
                      "dataModel": "${json-unit.matches:id}",
                      "id": "${json-unit.matches:id}",
                      "label": "string",
                      "breadcrumbs": [
                        {
                          "domainType": "DataModel",
                          "finalised": false,
                          "id": "${json-unit.matches:id}",
                          "label": "Complex Test DataModel"
                        }
                      ]
                    },
                    "dataModel": "${json-unit.matches:id}",
                    "maxMultiplicity": 20,
                    "id": "${json-unit.matches:id}",
                    "label": "ele1",
                    "minMultiplicity": 0,
                    "breadcrumbs": [
                      {
                        "domainType": "DataModel",
                        "finalised": false,
                        "id": "${json-unit.matches:id}",
                        "label": "Complex Test DataModel"
                      },
                      {
                        "domainType": "DataClass",
                        "id": "${json-unit.matches:id}",
                        "label": "content"
                      }
                    ]
                  },
                  "results": [''' + results[0] + '''

                  ]
                },
                {
                  "sourceDataElement": {
                    "domainType": "DataElement",
                    "dataClass": "${json-unit.matches:id}",
                    "dataType": {
                      "domainType": "PrimitiveType",
                      "dataModel": "${json-unit.matches:id}",
                      "id": "${json-unit.matches:id}",
                      "label": "integer",
                      "breadcrumbs": [
                        {
                          "domainType": "DataModel",
                          "finalised": false,
                          "id": "${json-unit.matches:id}",
                          "label": "Complex Test DataModel"
                        }
                      ]
                    },
                    "dataModel": "${json-unit.matches:id}",
                    "maxMultiplicity": 1,
                    "id": "${json-unit.matches:id}",
                    "label": "element2",
                    "minMultiplicity": 1,
                    "breadcrumbs": [
                      {
                        "domainType": "DataModel",
                        "finalised": false,
                        "id": "${json-unit.matches:id}",
                        "label": "Complex Test DataModel"
                      },
                      {
                        "domainType": "DataClass",
                        "id": "${json-unit.matches:id}",
                        "label": "content"
                      }
                    ]
                  },
                  "results": [''' + results[1] + '''

                  ]
                }
              ]
            }'''
            }

            List<String> expectedLinkSuggestionResults() {
            ['''
            {
                "score": 0.70164835,
                "dataElement": {
                    "domainType": "DataElement",
                    "dataClass": "${json-unit.matches:id}",
                    "dataType": {
                        "domainType": "PrimitiveType",
                        "dataModel": "${json-unit.matches:id}",
                        "id": "${json-unit.matches:id}",
                        "label": "string",
                        "breadcrumbs": [
                            {
                                "domainType": "DataModel",
                                "finalised": false,
                                "id": "${json-unit.matches:id}",
                                "label": "Simple Test DataModel"
                            }
                        ]
                    },
                    "dataModel": "${json-unit.matches:id}",
                    "description": "most obvious match",
                    "id": "${json-unit.matches:id}",
                    "label": "ele1",
                    "breadcrumbs": [
                        {
                            "domainType": "DataModel",
                            "finalised": false,
                            "id": "${json-unit.matches:id}",
                            "label": "Simple Test DataModel"
                        },
                        {
                            "domainType": "DataClass",
                            "id": "${json-unit.matches:id}",
                            "label": "simple"
                        }
                    ]
                }
            },
            {
                "score": 0.35714078,
                "dataElement": {
                    "domainType": "DataElement",
                    "dataClass": "${json-unit.matches:id}",
                    "dataType": {
                        "domainType": "PrimitiveType",
                        "dataModel": "${json-unit.matches:id}",
                        "id": "${json-unit.matches:id}",
                        "label": "string",
                        "breadcrumbs": [
                            {
                                "domainType": "DataModel",
                                "finalised": false,
                                "id": "${json-unit.matches:id}",
                                "label": "Simple Test DataModel"
                            }
                        ]
                    },
                    "dataModel": "${json-unit.matches:id}",
                    "description": "least obvious match",
                    "id": "${json-unit.matches:id}",
                    "label": "ele2",
                    "breadcrumbs": [
                        {
                            "domainType": "DataModel",
                            "finalised": false,
                            "id": "${json-unit.matches:id}",
                            "label": "Simple Test DataModel"
                        },
                        {
                            "domainType": "DataClass",
                            "id": "${json-unit.matches:id}",
                            "label": "simple"
                        }
                    ]
                }
            }''', '''
            {
                "score": 0.05167392,
                "dataElement": {
                    "domainType": "DataElement",
                    "dataClass": "${json-unit.matches:id}",
                    "dataType": {
                        "domainType": "PrimitiveType",
                        "dataModel": "${json-unit.matches:id}",
                        "id": "${json-unit.matches:id}",
                        "label": "string",
                        "breadcrumbs": [
                            {
                                "domainType": "DataModel",
                                "finalised": false,
                                "id": "${json-unit.matches:id}",
                                "label": "Simple Test DataModel"
                            }
                        ]
                    },
                    "dataModel": "${json-unit.matches:id}",
                    "description": "least obvious match",
                    "id": "${json-unit.matches:id}",
                    "label": "ele2",
                    "breadcrumbs": [
                        {
                            "domainType": "DataModel",
                            "finalised": false,
                            "id": "${json-unit.matches:id}",
                            "label": "Simple Test DataModel"
                        },
                        {
                            "domainType": "DataClass",
                            "id": "${json-unit.matches:id}",
                            "label": "simple"
                        }
                    ]
                }
            }''', '''''']
            }

            @Override
            void additionalCleanup() {
            logger.info("Additional cleanup")
            DataClass toDataClass = DataClass.findByDataModelAndLabel(simpleTestDataModel, 'simple')
            DataElement.findAllByDataClass(toDataClass).each {
            dataElementService.delete it
            }
            DataType.findAllByDataModel(simpleTestDataModel).each {
            dataTypeService.delete it
            }
            }
    f
            */

    String getExpectedDiffJson() {
        '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Complex Test DataModel",
  "count": 21,
  "diffs": [
    {
      "label": {
        "left": "Complex Test DataModel",
        "right": "Simple Test DataModel"
      }
    },
    {
      "metadata": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com",
            "key": "mdk1",
            "value": "mdv1"
          },
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com/test",
            "key": "mdk1",
            "value": "mdv2"
          }
        ],
        "created": [
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com/simple",
            "key": "mdk1",
            "value": "mdv1"
          },
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com/simple",
            "key": "mdk2",
            "value": "mdv2"
          }
        ]
      }
    },
    {
      "annotations": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "test annotation 2"
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "test annotation 1"
          }
        ]
      }
    },
    {
      "author": {
        "left": "admin person",
        "right": null
      }
    },
    {
      "organisation": {
        "left": "brc",
        "right": null
      }
    },
    {
      "dataTypes": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "yesnounknown",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "integer",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "string",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "child",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          }
        ]
      }
    },
    {
      "dataClasses": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "emptyclass",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "child",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "parent",
                "domainType": "DataClass"
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "parent",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "content",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          }
        ],
        "created": [
          {
            "id": "${json-unit.matches:id}",
            "label": "simple",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Simple Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              }
            ]
          }
        ]
      }
    },
    {
      "dataElements": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "element2",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "content",
                "domainType": "DataClass"
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "child",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "parent",
                "domainType": "DataClass"
              }
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "ele1",
            "breadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "content",
                "domainType": "DataClass"
              }
            ]
          }
        ]
      }
    }
  ]
}'''
    }
}