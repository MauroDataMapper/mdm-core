/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
 *  |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents | Action: save
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id} | Action: delete
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id} | Action: update
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${id} | Action: show
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}*   |
 *  Action: alterDataClasses
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/${type}/${dataClassId}*  |
 *  Action: alterDataClasses
 * </pre>
 *
 * @see DataClassComponentController
 */
@Integration
@Slf4j
class DataClassComponentFunctionalSpec extends ResourceFunctionalSpec<DataClassComponent> {

    @Shared
    UUID sourceDataModelId

    @Shared
    UUID targetDataModelId

    @Shared
    UUID sourceDataClassId

    @Shared
    UUID targetDataClassId

    @Shared
    UUID sourceDataClassId2

    @Shared
    UUID targetDataClassId2

    @Shared
    UUID dataFlowId

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

        sourceDataClassId = new DataClass(label: 'Functional Test DataClass Source', createdBy: FUNCTIONAL_TEST,
                                          dataModel: sourceDataModel).save(flush: true).id
        targetDataClassId = new DataClass(label: 'Functional Test DataClass Target', createdBy: FUNCTIONAL_TEST,
                                          dataModel: targetDataModel).save(flush: true).id

        sourceDataClassId2 = new DataClass(label: 'Functional Test DataClass Source 2', createdBy: FUNCTIONAL_TEST,
                                           dataModel: sourceDataModel).save(flush: true).id
        targetDataClassId2 = new DataClass(label: 'Functional Test DataClass Target 2', createdBy: FUNCTIONAL_TEST,
                                           dataModel: targetDataModel).save(flush: true).id

        DataFlow dataFlow = new DataFlow(label: 'Functional Test DataFlow', createdBy: FUNCTIONAL_TEST,
                                         source: sourceDataModel, target: targetDataModel).save(flush: true)
        dataFlowId = dataFlow.id

        sessionFactory.currentSession.flush()

        assert sourceDataModelId
        assert targetDataModelId
        assert sourceDataClassId
        assert targetDataClassId
        assert sourceDataClassId2
        assert targetDataClassId2
        assert dataFlowId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(DataFlow, DataClass, DataModel, Folder)
    }

    @Override
    String getResourcePath() {
        "dataModels/${targetDataModelId}/dataFlows/${dataFlowId}/dataClassComponents"
    }


    @Override
    Map getValidJson() {
        [
            label            : 'Functional Test DataClassComponent',
            sourceDataClasses: [sourceDataClassId],
            targetDataClasses: [targetDataClassId]
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label            : 'Functional Test DataClassComponent',
            sourceDataClasses: [],
            targetDataClasses: []
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
  "domainType": "DataClassComponent",
  "label": "Functional Test DataClassComponent",
  "path": "dm:Functional Test DataModel Target$main|df:Functional Test DataFlow|dcc:Functional Test DataClassComponent",
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
    }
  ],
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "dataFlow": "${json-unit.matches:id}",
  "sourceDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "Functional Test DataClass Source",
      "path": "dm:Functional Test DataModel Source$main|dc:Functional Test DataClass Source",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel Source",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ],
  "targetDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "Functional Test DataClass Target",
      "path": "dm:Functional Test DataModel Target$main|dc:Functional Test DataClass Target",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test DataModel Target",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    void 'test adding a source DataClass to DataClassComponent'() {
        given:
        def id = createNewItem(validJson)

        when:
        PUT("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 2
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId2.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test adding a target DataClass to DataClassComponent'() {
        given:
        def id = createNewItem(validJson)

        when:
        PUT("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 2
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId2.toString()}
    }

    void 'test removing a source DataClass to DataClassComponent'() {
        given:
        def id = createNewItem(validJson)
        PUT("${id}/source/${sourceDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        DELETE("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        !responseBody().sourceDataClasses.any {it.id == sourceDataClassId2.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test removing a target DataClass to DataClassComponent'() {
        given:
        def id = createNewItem(validJson)
        PUT("${id}/target/${targetDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        DELETE("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}
        !responseBody().targetDataClasses.any {it.id == targetDataClassId2.toString()}
    }
}