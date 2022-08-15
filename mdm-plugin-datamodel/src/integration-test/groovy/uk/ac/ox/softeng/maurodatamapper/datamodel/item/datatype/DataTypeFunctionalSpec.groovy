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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.functional.OrderedResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

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

    @Shared
    UUID finalisedDataModelId

    @Shared
    UUID finalisedDataClassId

    @Shared
    UUID finalisedDataTypeId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id
        dataClassId = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST,
                                    dataModel: dataModel).save(flush: true).id

        DataModel finalisedDataModel = new DataModel(label: 'Functional Test DataModel 3', createdBy: FUNCTIONAL_TEST,
                                                     finalised: true, dateFinalised: OffsetDateTime.now(), modelVersion: Version.from('1'),
                                                     folder: folder, authority: testAuthority).save(flush: true)
        finalisedDataModelId = finalisedDataModel.id

        finalisedDataClassId = new DataClass(label: 'Functional Test DataClass (Importable)', createdBy: FUNCTIONAL_TEST,
                                             dataModel: finalisedDataModelId).save(flush: true).id

        finalisedDataTypeId = new PrimitiveType(label: 'Functional Test DataType (Importable)', createdBy: FUNCTIONAL_TEST,
                                                dataModel: finalisedDataModelId).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataTypeFunctionalSpec')
        cleanUpResources(DataClass, DataModel, Folder)
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


    Map getValidCopyEditJson() {
        [
            domainType: 'PrimitiveType',
            label     : 'CopyData'
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


    void 'Test the save action correctly persists an instance for enumeration type'() {
        when: 'The save action is executed with valid data'
        POST('',
             [
                 domainType       : 'EnumerationType',
                 label            : 'functional enumeration',
                 enumerationValues: [
                     [key: 'a', value: 'wibble'],
                     [key: 'b', value: 'wobble']
                 ]
             ], STRING_ARG)

        then: 'The response is correct'
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

    void 'RT01 : Test the save action correctly persists an instance for reference type'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 domainType    : 'ReferenceType',
                 label         : 'functional dataclass reference',
                 referenceClass: dataClassId
             ]
             , STRING_ARG)

        then: 'The response is correct'
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

    void 'RT02 : Test reference type saves without a label'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 domainType    : 'ReferenceType',
                 referenceClass: dataClassId
             ]
             , STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ReferenceType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "Reference to Functional Test DataClass",
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

    void 'RT03 : Test reference type saves without a label for a second time'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 domainType    : 'ReferenceType',
                 referenceClass: dataClassId
             ]
             , STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ReferenceType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "Reference to Functional Test DataClass",
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

        when: 'The save action is executed with valid data again'
        POST('',
             [
                 domainType    : 'ReferenceType',
                 referenceClass: dataClassId
             ]
             , STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ReferenceType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "Reference to Functional Test DataClass (1)",
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


    void 'MDT01 : Test the save action correctly persists an instance for modeldata type'() {
        when: 'The save action is executed with valid data'
        POST('', [
            domainType             : 'ModelDataType',
            label                  : 'functional modeldata',
            modelResourceId        : otherDataModelId,
            modelResourceDomainType: 'DataModel'
        ], STRING_ARG)

        then: 'The response is correct'
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
      "modelResourceDomainType": "DataModel"
    }'''
    }

    void 'MDT02 :  Test model data type saves without a label'() {
        when: 'The save action is executed with valid data'
        POST('', [
            domainType             : 'ModelDataType',
            modelResourceId        : otherDataModelId,
            modelResourceDomainType: 'DataModel'
        ], STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ModelDataType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "Reference to Functional Test DataModel 2",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel"
        }
      ],
      "modelResourceId": "${json-unit.matches:id}",
      "modelResourceDomainType": "DataModel"
    }'''
    }

    void 'CC01 : test copying primitive type from datamodel to other datamodel'() {
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

    void 'CC02 : test copying enumeration type from datamodel to other datamodel'() {
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

    void 'CC03 : test copying reference type from datamodel to other datamodel'() {
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

    void 'CC04 : test copying modeldata type from datamodel to other datamodel'() {
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

    void 'CC05 : test copying modeldata type from datamodel to other datamodel, altering the label property'() {
        given:
        POST('', getValidCopyEditJson())
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/${UUID.randomUUID()}",
             [copyLabel: 'DataType Label to change to'], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy invalid'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/${UUID.randomUUID()}",
             [copyLabel: null], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy valid, changing label'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id",
             [copyLabel: 'DataType Label to change to'], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        responseBody().id != id
        responseBody().label == 'DataType Label to change to'
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()

        cleanup:
        cleanUpData(id)
    }

    void 'CC06 : test copying enumeration type from datamodel to other datamodel, changing the label in the copy'() {
        given:
        POST('', [
            domainType       : 'EnumerationType',
            label            : 'Copy Test Enumeration',
            enumerationValues: [
                [key: 'a', value: 'Cat'],
                [key: 'b', value: 'Dog']
            ]
        ])
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy valid, changing label'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id",
             [copyLabel: 'DataType Label to change to'], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        responseBody().id != id
        responseBody().label == 'DataType Label to change to'
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()

        cleanup:
        cleanUpData(id)
    }

    void 'CC07 : test copying reference type from datamodel to other datamodel, changing the label property'() {
        given:
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'functional dataclass reference base label',
            referenceClass: dataClassId
        ])
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy valid, changing label'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id",
             [copyLabel: 'test changing label'], MAP_ARG, true)

        then:
        verifyResponse(BAD_REQUEST, response)

        cleanup:
        cleanUpData(id)
    }


    void 'CC08 : test copying modeldata type from datamodel to other datamodel, editing the label property'() {
        given:
        String modelId = UUID.randomUUID().toString()
        POST('', [
            domainType             : 'ModelDataType',
            label                  : 'functional modeldata pre change',
            modelResourceId        : modelId,
            modelResourceDomainType: 'Terminology'
        ])
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/${UUID.randomUUID()}",
             [copyLabel: 'post copy label'], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy valid'
        POST("dataModels/$otherDataModelId/dataTypes/$dataModelId/$id",
             [copyLabel: 'post copy label'], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        responseBody().id != id
        responseBody().label == 'post copy label'
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()
        responseBody().modelResourceId == modelId
        responseBody().modelResourceDomainType == 'Terminology'

        cleanup:
        cleanUpData(id)
    }

    void 'IMI01 : test creating reference datatype using imported dataclass'() {

        when: 'creating using DC outside model'
        POST('', [
            label         : 'Functional Test DataType',
            domainType    : 'ReferenceType',
            referenceClass: finalisedDataClassId
        ])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == 'DataClass assigned to DataType must belong to the same DataModel or be imported'

        when: 'importing dataclass id'
        PUT("dataModels/$dataModelId/dataClasses/$finalisedDataModelId/$finalisedDataClassId", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when: 'creating using the imported DT'
        POST('', [
            label         : 'Functional Test DataType',
            domainType    : 'ReferenceType',
            referenceClass: finalisedDataClassId
        ])

        then:
        verifyResponse CREATED, response
        responseBody().id
        responseBody().referenceClass.id == finalisedDataClassId.toString()

        cleanup:
        cleanUpData(responseBody().id)
    }

    void 'IMI02 : test ordering of datatypes with imported datatype'() {
        given: 'create datatypes with specified order different to label order'
        String eId = createNewItem(getValidLabelJson('Functional Test DataType E', 0))
        String dId = createNewItem(getValidLabelJson('Functional Test DataType D', 1))
        String cId = createNewItem(getValidLabelJson('Functional Test DataType C', 2))
        String bId = createNewItem(getValidLabelJson('Functional Test DataType B', 3))
        String aId = createNewItem(getValidLabelJson('Functional Test DataType A', 4))

        when:
        GET('')

        then: 'index order is default'
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataType E'
        response.body().items[1].label == 'Functional Test DataType D'
        response.body().items[2].label == 'Functional Test DataType C'
        response.body().items[3].label == 'Functional Test DataType B'
        response.body().items[4].label == 'Functional Test DataType A'

        when: 'add an imported dataelement'
        PUT("dataModels/$dataModelId/dataTypes/$finalisedDataModelId/$finalisedDataTypeId", [:], MAP_ARG, true)
        verifyResponse OK, response
        GET('')

        then: 'label order is used'
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataType (Importable)'
        response.body().items[1].label == 'Functional Test DataType A'
        response.body().items[2].label == 'Functional Test DataType B'
        response.body().items[3].label == 'Functional Test DataType C'
        response.body().items[4].label == 'Functional Test DataType D'
        response.body().items[5].label == 'Functional Test DataType E'

        and: 'sort by idx param is discarded'
        GET('?sort=idx')
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataType (Importable)'
        response.body().items[1].label == 'Functional Test DataType A'
        response.body().items[2].label == 'Functional Test DataType B'
        response.body().items[3].label == 'Functional Test DataType C'
        response.body().items[4].label == 'Functional Test DataType D'
        response.body().items[5].label == 'Functional Test DataType E'

        and: 'sort by date created works as expected'
        GET('?sort=dateCreated')
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataType (Importable)'
        response.body().items[1].label == 'Functional Test DataType E'
        response.body().items[2].label == 'Functional Test DataType D'
        response.body().items[3].label == 'Functional Test DataType C'
        response.body().items[4].label == 'Functional Test DataType B'
        response.body().items[5].label == 'Functional Test DataType A'

        when: 'imported data element is removed'
        DELETE("$finalisedDataModelId/$finalisedDataTypeId")
        verifyResponse OK, response

        then: 'index sorting is default again'
        GET('')
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataType E'
        response.body().items[1].label == 'Functional Test DataType D'
        response.body().items[2].label == 'Functional Test DataType C'
        response.body().items[3].label == 'Functional Test DataType B'
        response.body().items[4].label == 'Functional Test DataType A'

        cleanup:
        cleanUpData(eId)
        cleanUpData(dId)
        cleanUpData(cId)
        cleanUpData(bId)
        cleanUpData(aId)
    }
}