/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.OrderedResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see DataElementController* Controller: dataElement
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
class DataElementFunctionalSpec extends OrderedResourceFunctionalSpec<DataElement> {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    UUID dataClassId

    @Shared
    UUID secondDataClassId

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

    @Shared
    UUID finalisedDataModelId

    @Shared
    UUID finalisedDataTypeId

    @Shared
    UUID finalisedDataClassId

    @Shared
    UUID finalisedDataElementId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        DataModel otherDataModel = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                                 folder: folder, authority: testAuthority).save(flush: true)
        otherDataModelId = otherDataModel.id

        DataClass dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST,
                                            dataModel: dataModel).save(flush: true)
        dataClassId = dataClass.id
        secondDataClassId = new DataClass(label: 'Functional Test DataClass 3', createdBy: FUNCTIONAL_TEST,
                                          dataModel: dataModel).save(flush: true).id

        DataClass otherDataClass = new DataClass(label: 'Functional Test DataClass 2', createdBy: FUNCTIONAL_TEST,
                                                 dataModel: otherDataModel).save(flush: true)
        otherDataClassId = otherDataClass.id

        dataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                       dataModel: dataModel).save(flush: true).id

        differentDataTypeId = new PrimitiveType(label: 'text', createdBy: FUNCTIONAL_TEST,
                                                dataModel: dataModel).save(flush: true).id

        otherDataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                            dataModel: otherDataModel).save(flush: true).id


        DataModel finalisedDataModel = new DataModel(label: 'Functional Test DataModel 3', createdBy: FUNCTIONAL_TEST,
                                                     finalised: true, dateFinalised: OffsetDateTime.now(), modelVersion: Version.from('1'),
                                                     folder: folder, authority: testAuthority).save(flush: true)
        finalisedDataModelId = finalisedDataModel.id

        DataType finalisedDataType = new PrimitiveType(label: 'a finalised datatype', createdBy: FUNCTIONAL_TEST,
                                                       dataModel: finalisedDataModel).save(flush: true)
        finalisedDataTypeId = finalisedDataType.id

        DataClass finalisedDataClass = new DataClass(label: 'Functional Test DataClass 4', createdBy: FUNCTIONAL_TEST,
                                                     dataModel: finalisedDataModel).save(flush: true)
        finalisedDataClassId = finalisedDataClass.id

        DataElement finalisedDataElement = new DataElement(label: 'Functional Test DataElement', createdBy: FUNCTIONAL_TEST,
                                                           dataModel: finalisedDataModel, dataClass: finalisedDataClass, dataType: finalisedDataType).save(flush: true)
        finalisedDataElementId = finalisedDataElement.id


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
        cleanUpResources(DataType, DataClass, DataModel, Folder)
    }

    @Override
    void cleanUpData() {
        if (dataModelId) {
            sleep(20)
            GET(getResourcePath(otherDataModelId, otherDataClassId), MAP_ARG, true)
            def items = response.body().items
            items.each {i ->
                DELETE("${getResourcePath(otherDataModelId, otherDataClassId)}/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
                sleep(20)
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
    Map getValidLabelJson(String label, int index = -1) {
        if (index == -1) {
            [
                label          : label,
                maxMultiplicity: 2,
                minMultiplicity: 0,
                dataType       : dataTypeId.toString()
            ]
        } else {
            [
                label          : label,
                maxMultiplicity: 2,
                minMultiplicity: 0,
                dataType       : dataTypeId.toString(),
                index          : index
            ]
        }
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

    void 'CC01 test copying from dataclass to other dataclass with existing datatype'() {
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

    void 'CC02 test copying from dataclass to other dataclass with unknown datatype'() {
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

    @Transactional
    void cleanupCreatedDataTypes(String label) {
        DataType.findByLabel(label).delete()
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
                     domainType: DataType.PRIMITIVE_DOMAIN_TYPE
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

        cleanup:
        cleanupCreatedDataTypes('Functional Test DataType')

    }

    void 'test finding existing DataType alongside saving DataElement'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 label          : 'Functional Test DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label     : 'Functional Test DataType',
                     domainType: DataType.PRIMITIVE_DOMAIN_TYPE
                 ]
             ])

        then:
        verifyResponse(CREATED, response)
        responseBody().dataType.id

        when: 'posting again with the same DT type'
        String dtId = responseBody().dataType.id
        POST('',
             [
                 label          : 'Functional Test DataElement 2',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label     : 'Functional Test DataType',
                     domainType: DataType.PRIMITIVE_DOMAIN_TYPE
                 ]
             ])

        then:
        verifyResponse(CREATED, response)
        responseBody().dataType.id == dtId

        cleanup:
        cleanupCreatedDataTypes('Functional Test DataType')

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
                domainType: DataType.PRIMITIVE_DOMAIN_TYPE
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

        cleanup:
        cleanupCreatedDataTypes('Functional Test DataType 2')

    }

    void 'RT01 : test creation of Reference DataType alongside saving DataElement'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 label          : 'Functional Test DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label         : 'Functional Test DataType 3',
                     domainType    : DataType.REFERENCE_DOMAIN_TYPE,
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

        cleanup:
        cleanupCreatedDataTypes('Functional Test DataType 3')

    }

    void 'RT02 : test creation of Reference DataType alongside saving DataElement with no label'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 label          : 'Functional Test DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     domainType    : DataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: dataClassId
                 ]
             ])

        then: 'The response is correct'
        verifyResponse CREATED, response
        responseBody().dataType.label == 'Reference to Functional Test DataClass'

        cleanup:
        cleanupCreatedDataTypes('Reference to Functional Test DataClass')

    }

    void 'RT03 : test creation of Reference DataType alongside saving DataElement with no label and prexisting'() {

        when: 'The save action is executed with valid data'
        POST('',
             [
                 label          : 'Functional Test DataElement',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     domainType    : DataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: dataClassId
                 ]
             ])

        then: 'The response is correct'
        verifyResponse CREATED, response
        responseBody().dataType.label == 'Reference to Functional Test DataClass'


        when: 'The save action is executed with valid data again'
        String rdtId = responseBody().dataType.id
        POST('',
             [
                 label          : 'Functional Test DataElement 2',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     domainType    : DataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: dataClassId
                 ]
             ])

        then: 'The response is correct'
        verifyResponse CREATED, response
        responseBody().dataType.label == 'Reference to Functional Test DataClass'
        responseBody().dataType.id == rdtId

        cleanup:
        cleanupCreatedDataTypes('Reference to Functional Test DataClass')

    }

    void 'CC03 test copying reference type DataElement'() {
        given:
        POST('',
             [
                 label          : 'Functional Test Reference Type DataElement To Copy',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label         : 'Reference Test DataType To Copy',
                     domainType    : DataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: secondDataClassId.toString() //Originally tested the dataClassId which is what this DE is added to making an
                     // infinite loop. this is not realistic
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
        response.body().label == 'Functional Test Reference Type DataElement To Copy'
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherDataModelId.toString()
        response.body().breadcrumbs[1].id == otherDataClassId.toString()
        response.body().dataType.id != otherDataTypeId.toString()
        response.body().dataType.id != differentDataTypeId.toString()
        response.body().dataType.id != dtId
        response.body().dataType.label == 'Reference Test DataType To Copy'
    }

    @Rollback
    void 'CC04 Test Copy data element, relabeling new element'() {
        given:
        POST('', validJson)
        String id = response.body().id

        expect:
        response.status() == CREATED
        id

        when: 'trying to copy non-existent'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/${UUID.randomUUID()}",
             [copyLabel: 'newCopyLabel'], MAP_ARG, true)

        then:
        response.status() == NOT_FOUND

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/$id",
             [copyLabel: 'newCopyLabel'], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == 'newCopyLabel'
    }

    @Rollback
    void 'CC05 Testing copying DataClass to unknown datatype, relabeling in the process'() {
        given:
        POST('', [
            label          : 'Functional Test DataElement for relabeling',
            maxMultiplicity: 1,
            minMultiplicity: 1,
            dataType       : differentDataTypeId.toString()
        ])
        String id = response.body().id

        expect:
        response.status() == CREATED
        id

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/$id",
             [copyLabel: 'newCopyLabel'], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == 'newCopyLabel'

    }

    @Rollback
    void 'CC06 test copying reference type DataElement, relabeling the copy'() {
        given:
        POST('',
             [
                 label          : 'Functional Test Reference Type DataElement To Copy',
                 maxMultiplicity: 2,
                 minMultiplicity: 1,
                 dataType       : [
                     label         : 'Copy DataType',
                     domainType    : DataType.REFERENCE_DOMAIN_TYPE,
                     referenceClass: secondDataClassId.toString()
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
        POST("${getResourcePath(otherDataModelId, otherDataClassId)}/$dataModelId/$dataClassId/$id",
             [copyLabel: 'newCopyLabel'], MAP_ARG, true)

        then:
        response.status() == CREATED
        response.body().id != id
        response.body().label == 'newCopyLabel'
        response.body().dataType.label == 'Copy DataType'

    }

    void 'IMI01 : test creating using imported datatype'() {

        when: 'creating using DT outside model'
        POST('', [
            label   : 'Functional Test DataElement',
            dataType: finalisedDataTypeId
        ])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == 'DataType assigned to DataElement must belong to the same DataModel or be imported'

        when: 'importing datatype id'
        PUT("dataModels/$dataModelId/dataTypes/$finalisedDataModelId/$finalisedDataTypeId", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when: 'creating using the imported DT'
        POST('', [
            label   : 'Functional Test DataElement',
            dataType: finalisedDataTypeId
        ])

        then:
        verifyResponse CREATED, response
        responseBody().id
        responseBody().dataType.id == finalisedDataTypeId.toString()

        cleanup:
        cleanUpData(responseBody().id)
    }

    void 'IMI02 : test ordering of DataElements with imported DataElement'() {
        given: 'create dataelements with specified order different to label order'
        String eId = createNewItem(getValidLabelJson('Functional Test DataElement E', 0))
        String dId = createNewItem(getValidLabelJson('Functional Test DataElement D', 1))
        String cId = createNewItem(getValidLabelJson('Functional Test DataElement C', 2))
        String bId = createNewItem(getValidLabelJson('Functional Test DataElement B', 3))
        String aId = createNewItem(getValidLabelJson('Functional Test DataElement A', 4))

        when:
        GET('')

        then: 'index order is default'
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataElement E'
        response.body().items[1].label == 'Functional Test DataElement D'
        response.body().items[2].label == 'Functional Test DataElement C'
        response.body().items[3].label == 'Functional Test DataElement B'
        response.body().items[4].label == 'Functional Test DataElement A'

        when: 'add an imported dataelement'
        PUT("dataModels/$dataModelId/dataClasses/$dataClassId/dataElements/$finalisedDataModelId/$finalisedDataClassId/$finalisedDataElementId", [:], MAP_ARG, true)
        verifyResponse OK, response
        GET('')

        then: 'label order is used'
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataElement'
        response.body().items[1].label == 'Functional Test DataElement A'
        response.body().items[2].label == 'Functional Test DataElement B'
        response.body().items[3].label == 'Functional Test DataElement C'
        response.body().items[4].label == 'Functional Test DataElement D'
        response.body().items[5].label == 'Functional Test DataElement E'

        and: 'sort by idx param is discarded'
        GET('?sort=idx')
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataElement'
        response.body().items[1].label == 'Functional Test DataElement A'
        response.body().items[2].label == 'Functional Test DataElement B'
        response.body().items[3].label == 'Functional Test DataElement C'
        response.body().items[4].label == 'Functional Test DataElement D'
        response.body().items[5].label == 'Functional Test DataElement E'

        and: 'sort by date created works as expected'
        GET('?sort=dateCreated')
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataElement'
        response.body().items[1].label == 'Functional Test DataElement E'
        response.body().items[2].label == 'Functional Test DataElement D'
        response.body().items[3].label == 'Functional Test DataElement C'
        response.body().items[4].label == 'Functional Test DataElement B'
        response.body().items[5].label == 'Functional Test DataElement A'

        when: 'imported data element is removed'
        DELETE("$finalisedDataModelId/$finalisedDataClassId/$finalisedDataElementId")
        verifyResponse OK, response

        then: 'index sorting is default again'
        GET('')
        verifyResponse OK, response
        response.body().items[0].label == 'Functional Test DataElement E'
        response.body().items[1].label == 'Functional Test DataElement D'
        response.body().items[2].label == 'Functional Test DataElement C'
        response.body().items[3].label == 'Functional Test DataElement B'
        response.body().items[4].label == 'Functional Test DataElement A'

        cleanup:
        cleanUpData(eId)
        cleanUpData(dId)
        cleanUpData(cId)
        cleanUpData(bId)
        cleanUpData(aId)
    }

    Tuple2<String, String> setupForLinkSuggestions() {
        String el1 = createNewItem([
            label          : 'ele1',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : dataTypeId.toString(),
            description    : 'most obvious match'
        ])

        String el2 = createNewItem([
            label          : 'element',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : dataTypeId.toString(),
            description    : 'least obvious match'
        ])

        Tuple.of(el1, el2)
    }

    void 'LINK01 : test get link suggestions for a data element'() {
        given:
        Tuple ids = setupForLinkSuggestions()
        POST(getResourcePath(otherDataModelId, otherDataClassId), [
            label          : 'ele1',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : otherDataTypeId.toString(),
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        String el3 = responseBody().id
        String endpoint = "dataModels/${otherDataModelId}/" +
                          "dataClasses/${otherDataClassId}/" +
                          "dataElements/${el3}/" +
                          "suggestLinks/${dataModelId}"

        String expectedJson = expectedLinkSuggestions(expectedLinkSuggestionResults())

        when:
        GET(endpoint, STRING_ARG, true)

        then:
        verifyJsonResponse OK, expectedJson

        cleanup:
        DELETE("${getResourcePath(otherDataModelId, otherDataClassId)}/${el3}", MAP_ARG, true)
        verifyResponse(HttpStatus.NO_CONTENT, response)
        cleanUpData(ids.v1)
        cleanUpData(ids.v2)
    }

    void 'LINK02 : test get link suggestions for a data element with no data elements in the target'() {
        given:
        POST(getResourcePath(otherDataModelId, otherDataClassId), [
            label          : 'ele1',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : otherDataTypeId.toString(),
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        String el3 = responseBody().id
        String endpoint = "dataModels/${otherDataModelId}/" +
                          "dataClasses/${otherDataClassId}/" +
                          "dataElements/${el3}/" +
                          "suggestLinks/${dataModelId}"

        String expectedJson = expectedLinkSuggestions('')

        when:
        GET(endpoint, STRING_ARG, true)

        then:
        verifyJsonResponse OK, expectedJson

        cleanup:
        DELETE("${getResourcePath(otherDataModelId, otherDataClassId)}/${el3}", MAP_ARG, true)
        verifyResponse(HttpStatus.NO_CONTENT, response)
    }


    String expectedLinkSuggestions(String results) {
        '''{
  "sourceDataElement": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataElement",
    "label": "ele1",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel 2",
        "domainType": "DataModel",
        "finalised": false
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataClass 2",
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
          "label": "Functional Test DataModel 2",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    },
    "maxMultiplicity": 2,
    "minMultiplicity": 0
  },
  "results": [''' + results + ''']
}'''
    }

    String expectedLinkSuggestionResults() {
        '''{
      "dataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataElement",
        "label": "ele1",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test DataModel",
            "domainType": "DataModel",
            "finalised": false
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test DataClass",
            "domainType": "DataClass"
          }
        ],
        "description": "most obvious match",
        "dataClass": "${json-unit.matches:id}",
        "dataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "PrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Functional Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            }
          ]
        },
        "maxMultiplicity": 2,
        "minMultiplicity": 0
      },
      "score": "${json-unit.any-number}"
    },
    {
      "dataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataElement",
        "label": "element",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test DataModel",
            "domainType": "DataModel",
            "finalised": false
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test DataClass",
            "domainType": "DataClass"
          }
        ],
        "description": "least obvious match",
        "dataClass": "${json-unit.matches:id}",
        "dataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "PrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Functional Test DataModel",
              "domainType": "DataModel",
              "finalised": false
            }
          ]
        },
        "maxMultiplicity": 2,
        "minMultiplicity": 0
      },
      "score": "${json-unit.any-number}"
    }'''
    }

}