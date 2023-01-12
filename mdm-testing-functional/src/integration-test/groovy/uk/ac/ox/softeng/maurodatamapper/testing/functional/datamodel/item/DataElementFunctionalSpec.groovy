/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndCopyingInDataModelsFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: dataElement
 *  |  GET     | /api/dataModels/${dataModelId}/dataElements                                   | Action: index
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${dataElementId}  |
 *  Action: copyDataElement
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementController
 */
@Integration
@Slf4j
class DataElementFunctionalSpec extends UserAccessAndCopyingInDataModelsFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/dataClasses/${getContentDataClassId()}/dataElements"
    }

    @Override
    String getCopyPath(String fromId) {
        "dataModels/${getSimpleDataModelId()}/dataClasses/${getSimpleDataClassId()}/${getCatalogueItemDomainType()}/" +
        "${getComplexDataModelId()}/${getContentDataClassId()}/${fromId}"
    }

    @Override
    String getAlternativePath(String id) {
        "dataModels/${getSimpleDataModelId()}/dataClasses/${getSimpleDataClassId()}/dataElements/$id"
    }

    @Override
    String getEditsPath() {
        'dataElements'
    }

    @Transactional
    @Override
    String getExpectedTargetId() {
        DataElement.byDataClassIdAndLabel(Utils.toUuid(getContentDataClassId()), 'ele1').get().id.toString()
    }

    @Override
    void cleanupCopiedItem(String id) {
        String dtId = getDataElementDataTypeId(id)
        assert dtId
        removeValidIdObject(id)
        loginAdmin()
        DELETE("dataModels/${getSimpleDataModelId()}/dataTypes/${dtId}", MAP_ARG, true)
        verifyResponse HttpStatus.NO_CONTENT, response
    }

    @Transactional
    String getDataElementDataTypeId(String id) {
        DataElement dataElement = DataElement.get(id)
        dataElement.dataTypeId.toString()
    }

    @Transactional
    String getContentDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), 'content').get().id.toString()
    }

    @Transactional
    String getStringDataTypeId() {
        PrimitiveType.byDataModelIdAndLabel(Utils.toUuid(getComplexDataModelId()), 'string').get().id.toString()
    }


    @Transactional
    String getSimpleDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(simpleDataModelId), 'simple').get().id.toString()
    }

    @Override
    Map getValidJson() {
        [
            label          : 'Functional Test DataElement',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : [id: getStringDataTypeId()]
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label          : UUID.randomUUID().toString(),
            maxMultiplicity: 2,
            minMultiplicity: 0
        ]
    }

    @Override
    Map getInvalidUpdateJson() {
        [
            dataType: UUID.randomUUID().toString()
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
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
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "PrimitiveType",
    "model": "${json-unit.matches:id}",
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
  "model": "${json-unit.matches:id}",
  "maxMultiplicity": 2,
  "id": "${json-unit.matches:id}",
  "label": "Functional Test DataElement",
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
  ],
   "availableActions": [
    "show"
  ]
}'''
    }

    @Override
    void verifyCopiedResponseBody(HttpResponse<Map> response) {
        Map body = response.body()

        assert body.id
        assert body.domainType == 'DataElement'
        assert body.label == 'ele1'
        assert body.model == getSimpleDataModelId()
        assert body.dataClass == getSimpleDataClassId()
        assert body.breadcrumbs
        assert body.breadcrumbs.size() == 2
        assert body.breadcrumbs[0].id == getSimpleDataModelId()
        assert body.breadcrumbs[0].label == 'Simple Test DataModel'
        assert body.breadcrumbs[0].domainType == 'DataModel'
        assert body.breadcrumbs[0].finalised == false
        assert body.breadcrumbs[1].id == getSimpleDataClassId()
        assert body.breadcrumbs[1].label == 'simple'
        assert body.breadcrumbs[1].domainType == 'DataClass'

        assert body.availableActions == getEditorModelItemAvailableActions().sort()
        assert body.lastUpdated
        assert body.maxMultiplicity == 20
        assert body.minMultiplicity == 0

        assert body.dataType
        assert body.dataType.domainType == 'PrimitiveType'
        assert body.dataType.label == 'string'
        assert body.dataType.breadcrumbs
        assert body.dataType.breadcrumbs.size() == 1
        assert body.dataType.breadcrumbs[0].id == getSimpleDataModelId()
        assert body.dataType.breadcrumbs[0].label == 'Simple Test DataModel'
        assert body.dataType.breadcrumbs[0].domainType == 'DataModel'
        assert body.dataType.breadcrumbs[0].finalised == false
    }

    void 'test getting all DataElements for a DataModel'() {
        given:
        String endpoint = "/api/dataModels/${getComplexDataModelId()}/dataElements"

        when: 'not logged in'
        def response = GET(endpoint, MAP_ARG, true)

        then:
        verifyResponse HttpStatus.NOT_FOUND, response

        when: 'logged in as reader'
        loginReader()
        response = GET(endpoint, MAP_ARG, true)

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().count == 3
        responseBody().items[0].domainType == 'DataElement'
        responseBody().items[1].domainType == 'DataElement'
        responseBody().items[2].domainType == 'DataElement'

        when: 'logged in as writer'
        loginEditor()
        response = GET(endpoint, MAP_ARG, true)

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().count == 3
        responseBody().items[0].domainType == 'DataElement'
        responseBody().items[1].domainType == 'DataElement'
        responseBody().items[2].domainType == 'DataElement'
    }

    /*
    void 'test getting all DataElements for a known DataType'() {
        given:
        String endpoint = "${apiPath}/" +
                          "dataModels/${testDataModel.id}/" +
                          "dataTypes/${DataType.findByLabel('string').id}/" +
                          "dataElements"
        def expectedJson = '''{
  "count": 1,
  "items": [
    {
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
    }
  ]
}'''

        when: 'not logged in'
        def response = restGet(endpoint)

        then:
        verifyResponse UNAUTHORIZED, response

        when: 'logged in as reader'
        loginUser(reader2)
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson

        when: 'logged in as writer'
        loginEditor()
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson
    }

    void 'test getting all DataElements for an unknown DataType'() {
        given:
        String endpoint = "${apiPath}/" +
                          "dataModels/${testDataModel.id}/" +
                          "dataTypes/${UUID.randomUUID().toString()}/" +
                          "dataElements"
        def expectedJson = '''{
  "count": 0,
  "items": [

  ]
}'''

        when: 'not logged in'
        def response = restGet(endpoint)

        then:
        verifyResponse UNAUTHORIZED, response

        when: 'logged in as reader'
        loginUser(reader2)
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson

        when: 'logged in as writer'
        loginEditor()
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson
    }

    void 'test creation of datatype alongside saving dataelement'() {
        given:
        loginEditor()
        def response

        when: 'The save action is executed with valid data'
        response = post('') {
            json {
                label = 'Functional Test DataElement'
                maxMultiplicity = 2
                minMultiplicity = 0
                dataType {
                    label = 'Function Test DataType'
                    domainType = DataType.PRIMITIVE_DOMAIN_TYPE
                }
            }
        }

        then: 'The response is correct'
        verifyResponse CREATED, response, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "editable": true,
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "PrimitiveType",
    "dataModel": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "Function Test DataType",
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
  "maxMultiplicity": 2,
  "id": "${json-unit.matches:id}",
  "label": "Functional Test DataElement",
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
}'''

    }

    void 'test creation of datatype alongside updating dataelement'() {
        given:
        String id = createNewItem()
        loginEditor()
        def response

        when: 'The save action is executed with valid data'
        response = put(id) {
            json {
                dataType {
                    label = 'Function Test DataType'
                    domainType = DataType.PRIMITIVE_DOMAIN_TYPE
                }
            }
        }

        then: 'The response is correct'
        verifyResponse OK, response, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "editable": true,
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "PrimitiveType",
    "dataModel": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "Function Test DataType",
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
  "maxMultiplicity": 2,
  "id": "${json-unit.matches:id}",
  "label": "Functional Test DataElement",
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
}'''

    }

    void 'test copying reference type dataelement'() {
        given:
        loginEditor()

        when: 'trying to copy valid'
        def response = post(getCopyEndpoint(true, 'parent', 'child'))

        then:
        verifyResponse CREATED, response, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "editable": true,
  "semanticLinks": [
    {
      "domainType": "CatalogueSemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "source": {
        "domainType": "DataElement",
        "dataModel": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child",
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
      },
      "target": {
        "domainType": "DataElement",
        "dataModel": "${json-unit.matches:id}",
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
    }
  ],
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "ReferenceType",
    "semanticLinks": [
      {
        "domainType": "CatalogueSemanticLink",
        "linkType": "Refines",
        "id": "${json-unit.matches:id}",
        "source": {
          "domainType": "ReferenceType",
          "dataModel": "${json-unit.matches:id}",
          "id": "${json-unit.matches:id}",
          "label": "child",
          "breadcrumbs": [
            {
              "domainType": "DataModel",
              "finalised": false,
              "id": "${json-unit.matches:id}",
              "label": "Simple Test DataModel"
            }
          ]
        },
        "target": {
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
          ]
        }
      }
    ],
    "dataModel": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "child",
    "breadcrumbs": [
      {
        "domainType": "DataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Simple Test DataModel"
      }
    ],
    "referenceClass": {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "child",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel"
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
      "label": "Simple Test DataModel"
    },
    {
      "domainType": "DataClass",
      "id": "${json-unit.matches:id}",
      "label": "simple"
    }
  ]
}'''
    }


    @Override
    void additionalCleanup() {

        DataClass toDataClass = DataClass.findByDataModelAndLabel(simpleTestDataModel, 'simple')
        DataElement.findAllByDataClass(toDataClass).each {
            dataElementService.delete it
        }
        DataType.findAllByDataModel(simpleTestDataModel).each {
            dataTypeService.delete it
        }

        dataTypeService.delete DataType.findByLabel('Function Test DataType')
        dataClassService.delete DataClass.findByDataModelAndLabel(simpleTestDataModel, 'child')
    }

*/

    /*


    void setupForLinkSuggestions() {
        loginEditor()
        DataType newDataType = simpleTestDataModel.findDataTypeByLabel("string")
        def response
        if (!newDataType) {
            response = post(apiPath + "/dataModels/${simpleTestDataModel.id}/dataTypes") {
                json {
                    domainType = 'PrimitiveType'
                    label = 'string'
                }
            }
            assert (response.statusCode.'2xxSuccessful')
            newDataType = simpleTestDataModel.findDataTypeByLabel("string")
        }
        DataClass targetDataClass = DataClass.findByDataModelAndLabel(simpleTestDataModel, "simple")

        response = post(apiPath + "/dataModels/${simpleTestDataModel.id}/dataClasses/${targetDataClass.id}/dataElements") {
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
        response = post(apiPath + "/dataModels/${simpleTestDataModel.id}/dataClasses/${targetDataClass.id}/dataElements") {
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
        adminService.rebuildHibernateSearchIndexes(new LuceneIndexParameters())
        logout()
    }

    void 'test get link suggestions for a data element'() {
        given:
        setupForLinkSuggestions()

        DataClass sourceDataClass = DataClass.findByLabel('content')
        DataElement sourceDataElement = DataElement.findByDataClassAndLabel(sourceDataClass, 'ele1')
        String endpoint = "${apiPath}/" +
                          "dataModels/${testDataModel.id}/" +
                          "dataClasses/${sourceDataClass.id}/" +
                          "dataElements/${sourceDataElement.id}/" +
                          "suggestLinks/${simpleTestDataModel.id}"

        String expectedJson = expectedLinkSuggestions(expectedLinkSuggestionResults())


        when: 'not logged in'
        def response = restGet(endpoint)

        then:
        verifyResponse UNAUTHORIZED, response

        when: 'logged in as reader'
        loginUser(reader2)
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson

        when: 'logged in as writer'
        loginEditor()
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson
    }


    void 'test get link suggestions for a data element with no data elements in the target'() {
        given:

        DataClass sourceDataClass = DataClass.findByLabel('content')
        DataElement sourceDataElement = DataElement.findByDataClassAndLabel(sourceDataClass, 'ele1')
        String endpoint = "${apiPath}/" +
                          "dataModels/${testDataModel.id}/" +
                          "dataClasses/${sourceDataClass.id}/" +
                          "dataElements/${sourceDataElement.id}/" +
                          "suggestLinks/${simpleTestDataModel.id}"

        String expectedJson = expectedLinkSuggestions("")

        when: 'not logged in'
        def response = restGet(endpoint)

        then:
        verifyResponse UNAUTHORIZED, response

        when: 'logged in as reader'
        loginUser(reader2)
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson

        when: 'logged in as writer'
        loginEditor()
        response = restGet(endpoint)

        then:
        verifyResponse OK, response, expectedJson
    }


    String expectedLinkSuggestions(String results) {
        '''{
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
  "results": [
    ''' + results + '''
  ]
}'''
    }

    String expectedLinkSuggestionResults() {
        '''    {
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
    }
'''
    }
*/

}
