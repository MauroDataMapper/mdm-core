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
/*package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
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
import static io.micronaut.http.HttpStatus.OK*/

/**
 * @see ReferenceDataElementController* Controller: dataElement
 *  | POST   | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements       | Action: save   |
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements       | Action: index  |
 *  | DELETE | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements/${id} | Action: delete |
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements/${id} | Action: update |
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements/${id} | Action: show   |
 *
 *
 *  | POST   | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements/${otherReferenceDataModelId}/${otherDataClassId}/${dataElementId} |
 *  Action: copyDataElement |
 *
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/dataTypes/${dataTypeId}/dataElements          | Action: index  |
 *
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/dataClasses/${dataClassId}/dataElements/${dataElementId}/suggestLinks/${otherReferenceDataModelId} | Action:
 *  suggestLinks TODO
 *
 */
/*@Integration
@Slf4j
class ReferenceDataElementFunctionalSpec extends ResourceFunctionalSpec<ReferenceDataElement> {

    @Shared
    UUID referenceDataModelId

    @Shared
    UUID otherReferenceDataModelId

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

        ReferenceDataModel referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        referenceDataModelId = referenceDataModel.id
        ReferenceDataModel otherReferenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel 2', createdBy: FUNCTIONAL_TEST,
                                                 folder: folder, authority: testAuthority).save(flush: true)
        otherReferenceDataModelId = otherReferenceDataModel.id

        DataClass dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST,
                                            referenceDataModel: referenceDataModel).save(flush: true)
        dataClassId = dataClass.id
        DataClass otherDataClass = new DataClass(label: 'Functional Test DataClass 2', createdBy: FUNCTIONAL_TEST,
                                                 referenceDataModel: otherReferenceDataModel).save(flush: true)
        otherDataClassId = otherDataClass.id

        dataTypeId = new ReferencePrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                       referenceDataModel: referenceDataModel).save(flush: true).id

        differentDataTypeId = new ReferencePrimitiveType(label: 'text', createdBy: FUNCTIONAL_TEST,
                                                referenceDataModel: referenceDataModel).save(flush: true).id

        otherDataTypeId = new ReferencePrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                            referenceDataModel: otherReferenceDataModel).save(flush: true).id

        sessionFactory.currentSession.flush()

        assert referenceDataModelId
        assert otherReferenceDataModelId
        assert dataClassId
        assert dataTypeId
        assert otherDataTypeId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ReferenceDataModelFunctionalSpec')
        cleanUpResources(ReferenceDataType, ReferenceDataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }

    @Override
    void cleanUpData() {
        if (referenceDataModelId) {
            GET(getResourcePath(otherReferenceDataModelId, otherDataClassId), MAP_ARG, true)
            def items = response.body().items
            items.each {i ->
                DELETE("${getResourcePath(otherReferenceDataModelId, otherDataClassId)}/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
            }
            super.cleanUpData()
        }
    }

    @Override
    String getResourcePath() {
        getResourcePath(referenceDataModelId, dataClassId)
    }

    String getResourcePath(UUID referenceDataModelId, UUID dataClassId) {
        "referenceDataModels/${referenceDataModelId}/dataClasses/$dataClassId/dataElements"
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
        "domainType": "ReferenceDataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceDataModel"
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
      "domainType": "ReferenceDataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test ReferenceDataModel"
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

    /*void 'test copying from dataclass to other dataclass with existing datatype'() {
        given:
        POST('', validJson)
        String id = response.body().id

        expect:
        response.status() == CREATED
        id

        when: 'trying to copy non-existent'
        POST("${getResourcePath(otherReferenceDataModelId, otherDataClassId)}/$referenceDataModelId/$dataClassId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status() == NOT_FOUND

        when: 'trying to copy valid'
        POST("${getResourcePath(otherReferenceDataModelId, otherDataClassId)}/$referenceDataModelId/$dataClassId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherReferenceDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherReferenceDataModelId.toString()
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
        POST("${getResourcePath(otherReferenceDataModelId, otherDataClassId)}/$referenceDataModelId/$dataClassId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherReferenceDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherReferenceDataModelId.toString()
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
        GET("referenceDataModels/$referenceDataModelId/dataTypes/$dataTypeId/dataElements", MAP_ARG, true)

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
        "domainType": "ReferenceDataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceDataModel"
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
      "domainType": "ReferenceDataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test ReferenceDataModel"
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
        "domainType": "ReferenceDataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceDataModel"
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
      "domainType": "ReferenceDataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test ReferenceDataModel"
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
        "domainType": "ReferenceDataModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceDataModel"
      }
    ],
    "referenceClass": {
        "domainType": "DataClass",
        "model": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataClass",
        "breadcrumbs": [
          {
            "domainType": "ReferenceDataModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Functional Test ReferenceDataModel"
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
      "domainType": "ReferenceDataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test ReferenceDataModel"
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
        POST("${getResourcePath(otherReferenceDataModelId, otherDataClassId)}/$referenceDataModelId/$dataClassId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == 'Functional Test Reference Type DataElement'
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherReferenceDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherReferenceDataModelId.toString()
        response.body().breadcrumbs[1].id == otherDataClassId.toString()
        response.body().dataType.id != otherDataTypeId.toString()
        response.body().dataType.id != differentDataTypeId.toString()
        response.body().dataType.id != dtId
        response.body().dataType.label == 'Reference Test DataType'
    }


    /*
        void setupForLinkSuggestions() {
            loginEditor()
            DataType newDataType = simpleTestReferenceDataModel.findDataTypeByLabel("string")
            def response
            if (!newDataType) {
                response = post(apiPath + "/referenceDataModels/${simpleTestReferenceDataModel.id}/dataTypes") {
                    json {
                        domainType = 'PrimitiveType'
                        label = 'string'
                    }
                }
                assert (response.statusCode.'2xxSuccessful')
                newDataType = simpleTestReferenceDataModel.findDataTypeByLabel("string")
            }
            DataClass targetDataClass = DataClass.findByReferenceDataModelAndLabel(simpleTestReferenceDataModel, "simple")

            response = post(apiPath + "/referenceDataModels/${simpleTestReferenceDataModel.id}/dataClasses/${targetDataClass.id}/dataElements") {
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
            response = post(apiPath + "/referenceDataModels/${simpleTestReferenceDataModel.id}/dataClasses/${targetDataClass.id}/dataElements") {
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
                              "referenceDataModels/${testReferenceDataModel.id}/" +
                              "dataClasses/${sourceDataClass.id}/" +
                              "dataElements/${sourceDataElement.id}/" +
                              "suggestLinks/${simpleTestReferenceDataModel.id}"

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
                              "referenceDataModels/${testReferenceDataModel.id}/" +
                              "dataClasses/${sourceDataClass.id}/" +
                              "dataElements/${sourceDataElement.id}/" +
                              "suggestLinks/${simpleTestReferenceDataModel.id}"

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
          "referenceDataModel": "${json-unit.matches:id}",
          "id": "${json-unit.matches:id}",
          "label": "string",
          "breadcrumbs": [
            {
              "domainType": "ReferenceDataModel",
              "finalised": false,
              "id": "${json-unit.matches:id}",
              "label": "Complex Test ReferenceDataModel"
            }
          ]
        },
        "referenceDataModel": "${json-unit.matches:id}",
        "maxMultiplicity": 20,
        "id": "${json-unit.matches:id}",
        "label": "ele1",
        "minMultiplicity": 0,
        "breadcrumbs": [
          {
            "domainType": "ReferenceDataModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test ReferenceDataModel"
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
              "referenceDataModel": "${json-unit.matches:id}",
              "id": "${json-unit.matches:id}",
              "label": "string",
              "breadcrumbs": [
                {
                  "domainType": "ReferenceDataModel",
                  "finalised": false,
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Test ReferenceDataModel"
                }
              ]
            },
            "referenceDataModel": "${json-unit.matches:id}",
            "description": "most obvious match",
            "id": "${json-unit.matches:id}",
            "label": "ele1",
            "breadcrumbs": [
              {
                "domainType": "ReferenceDataModel",
                "finalised": false,
                "id": "${json-unit.matches:id}",
                "label": "Simple Test ReferenceDataModel"
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
              "referenceDataModel": "${json-unit.matches:id}",
              "id": "${json-unit.matches:id}",
              "label": "string",
              "breadcrumbs": [
                {
                  "domainType": "ReferenceDataModel",
                  "finalised": false,
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Test ReferenceDataModel"
                }
              ]
            },
            "referenceDataModel": "${json-unit.matches:id}",
            "description": "least obvious match",
            "id": "${json-unit.matches:id}",
            "label": "ele2",
            "breadcrumbs": [
              {
                "domainType": "ReferenceDataModel",
                "finalised": false,
                "id": "${json-unit.matches:id}",
                "label": "Simple Test ReferenceDataModel"
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
//}