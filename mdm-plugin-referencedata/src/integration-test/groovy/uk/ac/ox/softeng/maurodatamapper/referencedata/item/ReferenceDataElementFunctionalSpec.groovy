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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * @see ReferenceDataElementController* Controller: dataElement
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements       | Action: save   |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements       | Action: index  |
 *  | DELETE | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id} | Action: update |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${id} | Action: show   |
 *
 *
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${otherDataModelId}/${otherDataClassId}/${dataElementId} |
 *  Action: copyDataElement |
 *
 *  | GET    | /api/dataModels/${dataModelId}/dataTypes/${dataTypeId}/dataElements          | Action: index  |
 *
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataElements/${dataElementId}/suggestLinks/${otherDataModelId} | Action:
 *  suggestLinks TODO
 *
 */
@Integration
@Slf4j
class ReferenceDataElementFunctionalSpec extends ResourceFunctionalSpec<ReferenceDataElement> {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    UUID dataClassId

    @Shared
    UUID otherDataClassId

    @Shared
    UUID dataTypeId

    @Shared
    UUID differentDataTypeId

    @Shared
    UUID otherDataTypeId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)

        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        DataModel otherDataModel = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                                 folder: folder, authority: testAuthority).save(flush: true)
        otherDataModelId = otherDataModel.id

        DataClass dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST,
                                            dataModel: dataModel).save(flush: true)
        dataClassId = dataClass.id
        DataClass otherDataClass = new DataClass(label: 'Functional Test DataClass 2', createdBy: FUNCTIONAL_TEST,
                                                 dataModel: otherDataModel).save(flush: true)
        otherDataClassId = otherDataClass.id

        dataTypeId = new ReferencePrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                       dataModel: dataModel).save(flush: true).id

        differentDataTypeId = new ReferencePrimitiveType(label: 'text', createdBy: FUNCTIONAL_TEST,
                                                dataModel: dataModel).save(flush: true).id

        otherDataTypeId = new ReferencePrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                            dataModel: otherDataModel).save(flush: true).id

        sessionFactory.currentSession.flush()

        assert dataModelId
        assert otherDataModelId
        assert dataClassId
        assert dataTypeId
        assert otherDataTypeId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(ReferenceDataType, DataClass, DataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }

    @Override
    void cleanUpData() {
        if (dataModelId) {
            GET(getResourcePath(otherDataModelId, otherDataClassId), MAP_ARG, true)
            def items = response.body().items
            items.each {i ->
                DELETE("${getResourcePath(otherDataModelId, otherDataClassId)}/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
            }
            super.cleanUpData()
        }
    }

    @Override
    String getResourcePath() {
        getResourcePath(dataModelId, dataClassId)
    }

    String getResourcePath(UUID dataModelId, UUID dataClassId) {
        "dataModels/${dataModelId}/dataClasses/$dataClassId/dataElements"
    }

    @Override
    Map getValidJson() {
        [
            label          : 'Functional Test DataElement',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : dataTypeId.toString()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label          : UUID.randomUUID().toString(),
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : null
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'adding a description'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "availableActions": ["delete","show","update"],
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
        "label": "Functional Test DataModel"
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
      "label": "Functional Test DataModel"
    },
    {
      "domainType": "DataClass",
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataClass"
    }
  ]
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().description == 'adding a description'
    }

    void 'test copying from dataclass to other dataclass with existing datatype'() {
        given:
        POST('', validJson)
        String id = response.body().id

        expect:
        response.status() == CREATED
        id

        when: 'trying to copy non-existent'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status() == NOT_FOUND

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherDataModelId.toString()
        response.body().breadcrumbs[1].id == otherDataClassId.toString()
        response.body().dataType.id == otherDataTypeId.toString()
    }

    void 'test copying from dataclass to other dataclass with unknown datatype'() {
        given:
        POST('', [
            label          : 'Functional Test DataElement',
            maxMultiplicity: 1,
            minMultiplicity: 1,
            dataType       : differentDataTypeId.toString()
        ])
        String id = response.body().id

        expect:
        response.status() == CREATED
        id

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherDataModelId.toString()
        response.body().breadcrumbs[1].id == otherDataClassId.toString()
        response.body().dataType.id != otherDataTypeId.toString()
        response.body().dataType.id != differentDataTypeId.toString()
        response.body().dataType.label == 'text'
    }

    void 'test getting all DataElements for a known DataType'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id

        when:
        GET("dataModels/$dataModelId/dataTypes/$dataTypeId/dataElements", MAP_ARG, true)

        then:
        response.status() == OK
        response.body().count == 1
        response.body().items.size() == 1
        response.body().items[0].id == id
        response.body().items[0].label == validJson.label
    }

    void 'test creation of DataType alongside saving DataElement'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 label          : 'Functional Test DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label     : 'Functional Test DataType',
                     domainType: ReferenceDataType.PRIMITIVE_DOMAIN_TYPE
                 ]
             ], STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "availableActions": ["delete","show","update"],
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "PrimitiveType",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataType",
    "breadcrumbs": [
      {
        "domainType": "DataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel"
      }
    ]
  },
  "model": "${json-unit.matches:id}",
  "maxMultiplicity": 2,
  "id": "${json-unit.matches:id}",
  "label": "Functional Test DataElement",
  "minMultiplicity": 1,
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataModel"
    },
    {
      "domainType": "DataClass",
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataClass"
    }
  ]
}'''

    }

    void 'test creation of DataType alongside updating DataElement'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id

        when: 'The update action is executed with valid data'
        PUT(id, [
            dataType: [
                label     : 'Functional Test DataType 2',
                domainType: ReferenceDataType.PRIMITIVE_DOMAIN_TYPE
            ]
        ], STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse OK, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "availableActions": ["delete","show","update"],
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "PrimitiveType",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataType 2",
    "breadcrumbs": [
      {
        "domainType": "DataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel"
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
      "label": "Functional Test DataModel"
    },
    {
      "domainType": "DataClass",
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataClass"
    }
  ]
}'''

    }

    void 'test creation of Reference DataType alongside saving DataElement'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 label          : 'Functional Test DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label         : 'Functional Test DataType 3',
                     domainType    : ReferenceDataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: dataClassId
                 ]
             ], STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataElement",
  "availableActions": ["delete","show","update"],
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "domainType": "ReferenceType",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataType 3",
    "breadcrumbs": [
      {
        "domainType": "DataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel"
      }
    ],
    "referenceClass": {
        "domainType": "DataClass",
        "model": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataClass",
        "breadcrumbs": [
          {
            "domainType": "DataModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Functional Test DataModel"
          }
        ]
      }
  },
  "model": "${json-unit.matches:id}",
  "maxMultiplicity": 2,
  "id": "${json-unit.matches:id}",
  "label": "Functional Test DataElement",
  "minMultiplicity": 1,
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataModel"
    },
    {
      "domainType": "DataClass",
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataClass"
    }
  ]
}'''

    }

    void 'test copying reference type DataElement'() {
        given:
        POST('',
             [
                 label          : 'Functional Test Reference Type DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label         : 'Reference Test DataType',
                     domainType    : ReferenceDataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: dataClassId.toString()
                 ]
             ])
        verifyResponse CREATED, response
        String id = response.body().id
        String dtId = response.body().dataType.id

        expect:
        response.status() == CREATED
        id
        dtId

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == 'Functional Test Reference Type DataElement'
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherDataModelId.toString()
        response.body().breadcrumbs[1].id == otherDataClassId.toString()
        response.body().dataType.id != otherDataTypeId.toString()
        response.body().dataType.id != differentDataTypeId.toString()
        response.body().dataType.id != dtId
        response.body().dataType.label == 'Reference Test DataType'
    }


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
            adminService.rebuildLuceneIndexes(new LuceneIndexParameters())
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