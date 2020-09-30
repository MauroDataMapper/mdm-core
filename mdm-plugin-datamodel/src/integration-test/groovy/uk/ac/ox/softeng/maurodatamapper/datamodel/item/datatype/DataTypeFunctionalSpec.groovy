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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.functional.OrderedResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * @see DataTypeController* Controller: dataType
 *  | POST   | /api/dataModels/${dataModelId}/dataTypes       | Action: save   |
 *  | GET    | /api/dataModels/${dataModelId}/dataTypes       | Action: index  |
 *  | DELETE | /api/dataModels/${dataModelId}/dataTypes/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${dataModelId}/dataTypes/${id} | Action: update |
 *  | GET    | /api/dataModels/${dataModelId}/dataTypes/${id} | Action: show   |
 *
 *  | POST   | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId} | Action: copyDataType |
 */
@Integration
@Slf4j
class DataTypeFunctionalSpec extends OrderedResourceFunctionalSpec<DataType> {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    Folder folder

    @Shared
    UUID dataClassId

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
        otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id
        dataClassId = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST,
                                    dataModel: dataModel).save(flush: true).id
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataTypeFunctionalSpec')
        cleanUpResources(DataClass, DataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }

    @Override
    void cleanUpData() {
        if (dataModelId) {
            sleep(20)
            GET("dataModels/$otherDataModelId/dataTypes", MAP_ARG, true)
            def items = responseBody().items
            items.each { i ->
                DELETE("dataModels/$otherDataModelId/dataTypes/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
                sleep(20)
            }
            super.cleanUpData()
        }
    }

    @Override
    String getResourcePath() {
        "dataModels/${dataModelId}/dataTypes"
    }

    @Override
    Map getValidJson() {
        [
            domainType: 'PrimitiveType',
            label     : 'date'
        ]
    }


    @Override
    Map getValidLabelJson(String label, int index = -1) {
      if (index == -1) {
        [
            domainType: 'PrimitiveType',
            label     : label
        ]
      } else {
        [
            domainType: 'PrimitiveType',
            label     : label,
            index     : index
        ]
      }
    }    

    @Override
    Map getInvalidJson() {
        [
            label     : null,
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
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "PrimitiveType",
  "availableActions": ["delete","show","update"],
  "model": "${json-unit.matches:id}",
  "id": "${json-unit.matches:id}",
  "label": "date",
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataModel"
    }
  ]
}'''
    }


    void "Test the save action correctly persists an instance for enumeration type"() {
        when: "The save action is executed with valid data"
        POST('',
             [
                 domainType       : 'EnumerationType',
                 label            : 'functional enumeration',
                 enumerationValues: [
                     [key: 'a', value: 'wibble'],
                     [key: 'b', value: 'wobble']
                 ]
             ], STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "EnumerationType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "category": null,
          "value": "wibble",
          "key": "a"
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "category": null,
          "value": "wobble",
          "key": "b"
        }
      ],
      "id": "${json-unit.matches:id}",
      "label": "functional enumeration",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel"
        }
      ]
    }'''
    }

    void "Test the save action correctly persists an instance for reference type"() {

        when: "The save action is executed with valid data"
        POST('',
             [
                 domainType    : 'ReferenceType',
                 label         : 'functional dataclass reference',
                 referenceClass: dataClassId
             ]
             , STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ReferenceType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "functional dataclass reference",
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
    }'''
    }


    void "Test the save action correctly persists an instance for modeldata type"() {
        when: "The save action is executed with valid data"
        UUID modelId = UUID.randomUUID()
        POST('', [
            domainType             : 'ModelDataType',
            label                  : 'functional modeldata',
            modelResourceId        : modelId,
            modelResourceDomainType: 'Terminology'
        ], STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ModelDataType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "functional modeldata",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel"
        }
      ],
      "modelResourceId": "${json-unit.matches:id}",
      "modelResourceDomainType": "Terminology"
    }'''
    }

    void 'test copying primitive type from datamodel to other datamodel'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy valid'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()

        cleanup:
        cleanUpData(id)
    }

    void 'test copying enumeration type from datamodel to other datamodel'() {
        given:
        POST('', [
            domainType       : 'EnumerationType',
            label            : 'functional enumeration',
            enumerationValues: [
                [key: 'a', value: 'wibble'],
                [key: 'b', value: 'wobble']
            ]
        ])
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy valid'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        responseBody().id != id
        responseBody().label == 'functional enumeration'
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()

        cleanup:
        cleanUpData(id)
    }

    void 'test copying reference type from datamodel to other datamodel'() {
        given:
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'functional dataclass reference',
            referenceClass: dataClassId
        ])
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy valid'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(BAD_REQUEST, response)

        cleanup:
        cleanUpData(id)
    }

    void 'test copying modeldata type from datamodel to other datamodel'() {
        given:
        String modelId = UUID.randomUUID().toString()
        POST('', [
            domainType             : 'ModelDataType',
            label                  : 'functional modeldata',
            modelResourceId        : modelId,
            modelResourceDomainType: 'Terminology'
        ])
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy valid'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        responseBody().id != id
        responseBody().label == 'functional modeldata'
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()
        responseBody().modelResourceId == modelId
        responseBody().modelResourceDomainType == 'Terminology'

        cleanup:
        cleanUpData(id)
    }
}