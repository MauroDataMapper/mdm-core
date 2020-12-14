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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item.datatype


import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndCopyingInDataModelsAndModelImportFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: dataType
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}  | Action: copyDataType
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeController
 */
@Integration
@Slf4j
class DataTypeFunctionalSpec extends UserAccessAndCopyingInDataModelsAndModelImportFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/dataTypes"
    }

    @Override
    String getEditsPath() {
        'primitiveTypes'
    }

    String getCatalogueItemDomainType() {
        'dataTypes'
    }

    @Transactional
    @Override
    String getExpectedTargetId() {
        DataType.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), 'string').get().id.toString()
    }

    @Transactional
    String getReferenceDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(getComplexDataModelId()), 'parent').get().id.toString()
    }

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        DataType.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_SIMPLE_DATAMODEL_NAME).id, 'string').get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        DataType.simpleName
    } 
    
    @Override
    String getModelImportPath() {
        "dataModels/${getComplexDataModelId()}/modelImports"
    }

    @Transactional
    String getImportedDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_SIMPLE_DATAMODEL_NAME).id, 'simple').get().id.toString()
    }   

    @Transactional
    String getImportedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_SIMPLE_DATAMODEL_NAME).id.toString()
    }     

    @Transactional
    String getResourcePathForFinalisedSimpleDataModel() {
        "dataModels/${getImportedDataModelId()}/dataTypes"
    }     

    @Override
    String getExpectedModelImportJson() {
      '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Complex Test DataModel"
  },
  "importedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "PrimitiveType",
    "label": "string",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Finalised Simple Test DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  }
}'''
    }

    @Override
    Map getValidJson() {
        [
            domainType: 'PrimitiveType',
            label: 'Functional Data Type'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            domainType: 'PrimitiveType'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'describes a date only'
        ]
    }

    @Override
    Map getInvalidUpdateJson() {
        [
            label: 'string'
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 4,
  "items": [
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
      ]
    },
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
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        },
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "key": "N",
          "value": "No",
          "category": null
        }
      ]
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
  ]
}'''
    }

    //Same as getEditorIndexJson but with one extra imported DataType
    @Override
    String getEditorIndexJsonWithImported() {
        '''{
  "count": 5,
  "items": [
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
      ]
    },
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
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        },
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "key": "N",
          "value": "No",
          "category": null
        }
      ]
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "string",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Finalised Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": true
        }
      ]
    }
  ]
}'''
    }    

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "PrimitiveType",
  "label": "Functional Data Type",
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
    "delete",
    "update",
    "save",
    "show",
    "comment",
    "editDescription"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    @Override
    void verifyCopiedResponseBody(HttpResponse<Map> response) {
        Map body = responseBody()

        assert body.id
        assert body.domainType == 'PrimitiveType'
        assert body.label == 'string'
        assert body.model == getSimpleDataModelId()
        assert body.breadcrumbs
        assert body.breadcrumbs.size() == 1
        assert body.breadcrumbs.first().id == getSimpleDataModelId()
        assert body.breadcrumbs.first().label == 'Simple Test DataModel'
        assert body.breadcrumbs.first().domainType == 'DataModel'
        assert body.breadcrumbs.first().finalised == false

        assert body.availableActions == [
            'show', 'comment', 'editDescription', 'update', 'save', 'delete'
        ]
        assert body.lastUpdated
    }

    void "E03a: Test the save action correctly persists an instance for enumeration type (as editor)"() {
        given:
        loginEditor()
        Map validJson = [
            domainType       : 'EnumerationType',
            label            : 'Functional Enumeration Type',
            enumerationValues: [
                [key: 'a', value: 'wibble'],
                [key: 'b', value: 'wobble']
            ]
        ]

        when: "The save action is executed with valid data"
        POST('', validJson)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == 'EnumerationType'
        assert responseBody().label == 'Functional Enumeration Type'
        assert responseBody().enumerationValues.size() == 2

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "EnumerationType",
  "label": "Functional Enumeration Type",
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
    "show","comment","editDescription","update","save","delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "enumerationValues": [
    {
      "index": 0,
      "id": "${json-unit.matches:id}",
      "key": "a",
      "value": "wibble",
      "category": null
    },
    {
      "index": 1,
      "id": "${json-unit.matches:id}",
      "key": "b",
      "value": "wobble",
      "category": null
    }
  ]
}
'''

        cleanup:
        removeValidIdObject(id)
    }

    void "E03b: Test the save action correctly persists an instance for reference type (as editor)"() {
        given:
        loginEditor()


        when: "The save action is executed with valid data"
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'Functional Reference Type',
            referenceClass: getReferenceDataClassId()
        ])


        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == 'ReferenceType'
        assert responseBody().label == 'Functional Reference Type'
        assert responseBody().referenceClass.id == getReferenceDataClassId()

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ReferenceType",
  "label": "Functional Reference Type",
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
   "show","comment","editDescription","update","save","delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "referenceClass": {
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
    "maxMultiplicity": -1,
    "minMultiplicity": 1
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void "E03c: Test the save action correctly persists an instance for reference type when the DataClass is imported (as editor)"() {
        given:
        loginEditor()

        
        //TODO Currently this test fails because the ReferenceType is created. Add a check so that ReferenceType can only
        //be created on a DataClass which is directly owned by or imported into the DataModel
        //when: "The save action is executed using valid data with the imported DataClass which has not yet been imported"
        //POST('', [
        //    domainType    : 'ReferenceType',
        //    label         : 'Functional Reference Type on Imported DataClass',
        //    referenceClass: getImportedDataClassId()
        //])

        //then: "The response is correct"
        //verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response

        when: "A DataClass is imported into the DataModel"
        POST(getModelImportPath(), [
            importedCatalogueItemDomainType: "DataClass",
            importedCatalogueItemId: getImportedDataClassId()
        ], MAP_ARG, true)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String modelImportId = responseBody().id

        when: "The save action is executed using valid data with the imported DataClass"
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'Functional Reference Type on Imported DataClass',
            referenceClass: getImportedDataClassId()
        ])

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == 'ReferenceType'
        assert responseBody().label == 'Functional Reference Type on Imported DataClass'
        assert responseBody().referenceClass.id == getImportedDataClassId()

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ReferenceType",
  "label": "Functional Reference Type on Imported DataClass",
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
    "delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "referenceClass": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "simple",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Finalised Simple Test DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  }
}'''

        when: "List the DataTypes on the importing DataModel"
        GET("${getResourcePath()}", STRING_ARG, true)

        then: "The ReferenceType is included in the list"
        verifyJsonResponse HttpStatus.OK, '''{
  "count": 5,
  "items": [
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
      ]
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
      "enumerationValues": [
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
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
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceType",
      "label": "Functional Reference Type on Imported DataClass",
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
        "label": "simple",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Finalised Simple Test DataModel",
            "domainType": "DataModel",
            "finalised": true
          }
        ]
      }
    }
  ]
}'''

        when: "List the DataTypes on the DataModel from which the DataClass was imported"
        GET("${getResourcePathForFinalisedSimpleDataModel()}?imported=true", STRING_ARG, true)

        then: "The ReferenceType is not unintentionally included in the list"
        verifyJsonResponse HttpStatus.OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "string",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Finalised Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": true
        }
      ]
    }
  ]
}'''
        cleanup:     
        removeValidIdObject(id)
    }    

    void "E03d: Test the save action correctly persists an instance for model data type (as editor)"() {
        given:
        UUID modelId = UUID.randomUUID()
        loginEditor()
        Map validJson = [
            domainType             : 'ModelDataType',
            label                  : 'Functional ModelData Type',
            modelResourceId        : modelId,
            modelResourceDomainType: 'Terminology'
        ]

        when: "The save action is executed with valid data"
        POST('', validJson)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        responseBody().domainType == 'ModelDataType'
        responseBody().label == 'Functional ModelData Type'
        responseBody().modelResourceId == modelId.toString()
        responseBody().modelResourceDomainType == 'Terminology'

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ModelDataType",
  "label": "Functional ModelData Type",
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
    "show","comment","editDescription","update","save","delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "modelResourceId": "${json-unit.matches:id}",
  "modelResourceDomainType": "Terminology"
}
'''

        cleanup:
        removeValidIdObject(id)
    }

}