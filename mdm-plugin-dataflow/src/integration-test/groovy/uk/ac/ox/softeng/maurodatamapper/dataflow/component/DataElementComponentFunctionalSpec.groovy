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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

/**
 * <pre>
 * Controller: dataElementComponent
 *  |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents | Action:
 *  save
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents | Action:
 *  index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id} |
 *  Action: delete
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id} |
 *  Action: update
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id} |
 *  Action: show
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/$
 *{dataElementComponentId}/${type}/${dataElementId} | Action: alterDataElements
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/$
 *{dataElementComponentId}/${type}/${dataElementId} | Action: alterDataElements
 * </pre>
 *
 * @see DataElementComponentController
 */
@Integration
@Slf4j
class DataElementComponentFunctionalSpec extends ResourceFunctionalSpec<DataElementComponent> {

    @Shared
    UUID sourceDataModelId

    @Shared
    UUID targetDataModelId

    @Shared
    UUID sourceDataElementId

    @Shared
    UUID targetDataElementId

    @Shared
    UUID sourceDataElementId2

    @Shared
    UUID targetDataElementId2

    @Shared
    UUID dataFlowId

    @Shared
    UUID dataClassComponentId

    @Shared
    Folder folder

    @RunOnce
    @Transactional
    def setup() {

        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        DataModel sourceDataModel = new DataModel(label: 'Functional Test DataModel Source', createdBy: FUNCTIONAL_TEST,
                                                  folder: folder, type: DataModelType.DATA_ASSET, authority: testAuthority).save(flush: true)
        sourceDataModelId = sourceDataModel.id
        DataModel targetDataModel = new DataModel(label: 'Functional Test DataModel Target', createdBy: FUNCTIONAL_TEST,
                                                  folder: folder, type: DataModelType.DATA_ASSET, authority: testAuthority).save(flush: true)
        targetDataModelId = targetDataModel.id

        DataClass sourceDataClass = new DataClass(label: 'Functional Test DataClass Source', createdBy: FUNCTIONAL_TEST,
                                                  dataModel: sourceDataModel).save(flush: true)
        DataClass targetDataClass = new DataClass(label: 'Functional Test DataClass Target', createdBy: FUNCTIONAL_TEST,
                                                  dataModel: targetDataModel).save(flush: true)

        DataType sourceDataType = new PrimitiveType(label: 'Functional Test DataType Source', createdBy: FUNCTIONAL_TEST,
                                                    dataModel: sourceDataModel).save(flush: true)

        DataType targetDataType = new PrimitiveType(label: 'Functional Test DataType Target', createdBy: FUNCTIONAL_TEST,
                                                    dataModel: targetDataModel).save(flush: true)

        sourceDataElementId = new DataElement(label: 'Functional Test DataElement Source', createdBy: FUNCTIONAL_TEST,
                                              dataClass: sourceDataClass, dataType: sourceDataType).save(flush: true).id

        targetDataElementId = new DataElement(label: 'Functional Test DataElement Target', createdBy: FUNCTIONAL_TEST,
                                              dataClass: targetDataClass, dataType: targetDataType).save(flush: true).id

        sourceDataElementId2 = new DataElement(label: 'Functional Test DataElement Source 2', createdBy: FUNCTIONAL_TEST,
                                               dataClass: sourceDataClass, dataType: sourceDataType).save(flush: true).id

        targetDataElementId2 = new DataElement(label: 'Functional Test DataElement Target 2', createdBy: FUNCTIONAL_TEST,
                                               dataClass: targetDataClass, dataType: targetDataType).save(flush: true).id

        DataFlow dataFlow = new DataFlow(label: 'Functional Test DataFlow', createdBy: FUNCTIONAL_TEST,
                                         source: sourceDataModel, target: targetDataModel).save(flush: true)
        dataFlowId = dataFlow.id

        dataClassComponentId = new DataClassComponent(label: 'Functional Test DataClassComponent', createdBy: FUNCTIONAL_TEST,
                                                      dataFlow: dataFlow,
                                                      sourceDataClasses: [sourceDataClass],
                                                      targetDataClasses: [targetDataClass]).save(flush: true).id

        sessionFactory.currentSession.flush()

        assert sourceDataModelId
        assert targetDataModelId
        assert sourceDataElementId
        assert targetDataElementId
        assert dataFlowId
        assert dataClassComponentId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(DataClassComponent, DataFlow, DataElement, DataType, DataClass, DataModel, Folder)
    }

    @Override
    String getResourcePath() {
        "dataModels/${targetDataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents"
    }

    @Override
    Map getValidJson() {
        [
            label             : 'Functional Test DataElementComponent',
            sourceDataElements: [sourceDataElementId],
            targetDataElements: [targetDataElementId]
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label             : 'Functional Test DataElementComponent',
            sourceDataElements: [],
            targetDataElements: []
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'a direct copy of the data from source to target'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataElementComponent",
  "label": "Functional Test DataElementComponent",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataModel Target",
      "domainType": "DataModel",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataFlow",
      "domainType": "DataFlow"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataClassComponent",
      "domainType": "DataClassComponent"
    }
  ],
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "dataClassComponent": "${json-unit.matches:id}",
  "sourceDataElements": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "Functional Test DataElement Source",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel Source",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataClass Source",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": "${json-unit.matches:id}"
    }
  ],
  "targetDataElements": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "Functional Test DataElement Target",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel Target",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataClass Target",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    void 'test adding a source DataElement to DataElementComponent'() {
        given:
        def id = createNewItem(validJson)

        when:
        PUT("${id}/source/${sourceDataElementId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataElements.size() == 2
        responseBody().targetDataElements.size() == 1
        responseBody().sourceDataElements.any {it.id == sourceDataElementId.toString()}
        responseBody().sourceDataElements.any {it.id == sourceDataElementId2.toString()}
        responseBody().targetDataElements.any {it.id == targetDataElementId.toString()}

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test adding a target DataElement to DataElementComponent'() {
        given:
        def id = createNewItem(validJson)

        when:
        PUT("${id}/target/${targetDataElementId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataElements.size() == 1
        responseBody().targetDataElements.size() == 2
        responseBody().sourceDataElements.any {it.id == sourceDataElementId.toString()}
        responseBody().targetDataElements.any {it.id == targetDataElementId.toString()}
        responseBody().targetDataElements.any {it.id == targetDataElementId2.toString()}
    }

    void 'test removing a source DataElement to DataElementComponent'() {
        given:
        def id = createNewItem(validJson)
        PUT("${id}/source/${sourceDataElementId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        DELETE("${id}/source/${sourceDataElementId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataElements.size() == 1
        responseBody().targetDataElements.size() == 1
        responseBody().sourceDataElements.any {it.id == sourceDataElementId.toString()}
        !responseBody().sourceDataElements.any {it.id == sourceDataElementId2.toString()}
        responseBody().targetDataElements.any {it.id == targetDataElementId.toString()}

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test removing a target DataElement to DataElementComponent'() {
        given:
        def id = createNewItem(validJson)
        PUT("${id}/target/${targetDataElementId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        DELETE("${id}/target/${targetDataElementId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataElements.size() == 1
        responseBody().targetDataElements.size() == 1
        responseBody().sourceDataElements.any {it.id == sourceDataElementId.toString()}
        responseBody().targetDataElements.any {it.id == targetDataElementId.toString()}
        !responseBody().targetDataElements.any {it.id == targetDataElementId2.toString()}
    }
}