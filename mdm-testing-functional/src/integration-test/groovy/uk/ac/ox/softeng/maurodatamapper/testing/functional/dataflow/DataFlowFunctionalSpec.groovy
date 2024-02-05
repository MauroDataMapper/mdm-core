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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.dataflow

import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: dataFlow
 *  |   POST   | /api/dataModels/${dataModelId}/dataFlows       | Action: save
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows       | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${id} | Action: delete
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${id} | Action: update
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${id} | Action: show
 *
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/diagramLayout  | Action: updateDiagramLayout
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowController
 */
@Integration
@Slf4j
class DataFlowFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${targetDataModelId}/dataFlows"
    }

    @Override
    String getEditsPath() {
        'dataFlows'
    }

    @Transactional
    String getTargetDataModelId() {
        DataModel.findByLabel(BootstrapModels.TARGET_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getSourceDataModelId() {
        DataModel.findByLabel(BootstrapModels.SOURCE_DATAMODEL_NAME).id.toString()
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereEditorsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereAuthorsCanAction('comment', 'editDescription', 'show',)
            .whereReviewersCanAction('comment', 'show')
            .whereReadersCanAction('show')
    }

    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using permanent API call', id)
        loginAdmin()
        DELETE("${id}?permanent=true")
        response.status() in [HttpStatus.NO_CONTENT, HttpStatus.NOT_FOUND]
    }

    String getAdditionalValidId(Map additionalValidJson) {
        loginEditor()
        POST(getSavePath(), additionalValidJson, MAP_ARG, true)
        verifyResponse HttpStatus.CREATED, response
        String id = response.body().id
        addAccessShares(id)
        logout()
        id
    }

    Map getValidJson() {
        [
            label : 'Functional Test DataFlow',
            source: [
                id: sourceDataModelId
            ]
        ]
    }

    Map getInvalidJson() {
        [
            label : null,
            source: [
                id: sourceDataModelId
            ]
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataFlow",
      "label": "Sample DataFlow",
      "path": "dm:TargetFlowDataModel$main|df:Sample DataFlow",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "source": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "SourceFlowDataModel",
        "path": "dm:SourceFlowDataModel$main",
        "type": "Data Asset",
        "branchName": "main",
        "documentationVersion": "1.0.0"
      },
      "target": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "TargetFlowDataModel",
        "path": "dm:TargetFlowDataModel$main",
        "type": "Data Asset",
        "branchName": "main",
        "documentationVersion": "1.0.0"
      },
      "diagramLayout": null
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataFlow",
  "label": "Functional Test DataFlow",
  "path": "dm:TargetFlowDataModel$main|df:Functional Test DataFlow",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
      "domainType": "DataModel",
      "finalised": false
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "source": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "SourceFlowDataModel",
    "path": "dm:SourceFlowDataModel$main",
    "type": "Data Asset",
    "branchName": "main",
    "documentationVersion": "1.0.0"
  },
  "target": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "TargetFlowDataModel",
    "path": "dm:TargetFlowDataModel$main",
    "type": "Data Asset",
    "branchName": "main",
    "documentationVersion": "1.0.0"
  },
  "diagramLayout": null
}'''
    }

    void 'L06 : test updating diagram layout (not logged in)'() {
        given:
        String id = getValidId()

        when:
        PUT("$id/diagramLayout", [diagramLayout: 'this is how to draw the diagram'])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N06 : test updating diagram layout (as no access/authenticated)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("$id/diagramLayout", [diagramLayout: 'this is how to draw the diagram'])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'R06 : test updating diagram layout (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        PUT("$id/diagramLayout", [diagramLayout: 'this is how to draw the diagram'])

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E06 : test updating diagram layout (as editor)'() {
        given:
        String id = getValidId()

        when:
        loginEditor()
        PUT("$id/diagramLayout", [diagramLayout: 'this is how to draw the diagram'])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().diagramLayout == 'this is how to draw the diagram'

        cleanup:
        removeValidIdObject(id)
    }

    void 'A06 : test updating diagram layout (as admin)'() {
        given:
        String id = getValidId()

        when:
        loginAdmin()
        PUT("$id/diagramLayout", [diagramLayout: 'this is how to draw the diagram'])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().diagramLayout == 'this is how to draw the diagram'

        cleanup:
        removeValidIdObject(id)
    }


    void 'L34 : test export a single DataFlow (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N34 : test export a single DataFlow (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R34 : test export a single DataFlow (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "dataFlow": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataFlow",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Asset",
    "source": {
      "id": "${json-unit.matches:id}",
      "label": "SourceFlowDataModel",
      "path": "dm:SourceFlowDataModel$main",
      "type": "Data Asset"
    },
    "target": {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
      "path": "dm:TargetFlowDataModel$main",
      "type": "Data Asset"
    }
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter",
      "name": "DataFlowJsonExporterService",
      "version": "4.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L35 : test export multiple Flows (json only exports first id) (as not logged in)'() {
        given:
        String id = getValidId()
        String id2 = getAdditionalValidId([
            label : 'Functional Test DataFlow 2',
            source: sourceDataModelId
        ])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0',
             [dataFlowIds: [id, id2]]
        )

        then:
        //no id set when multiple ones sent in the POST
        verifyNotFound response, null

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(id2)
    }

    void 'N35 : test export multiple Flows (json only exports first id) (as authenticated/no access)'() {
        given:
        String id = getValidId()
        String id2 = getAdditionalValidId([
            label : 'Functional Test DataFlow 2',
            source: sourceDataModelId
        ])

        when:
        loginAuthenticated()
        POST('export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0',
             [dataFlowIds: [id, id2]]
        )

        then:
        //no id set when multiple ones sent in the POST
        verifyNotFound response, null

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(id2)
    }

    void 'R35 : test export multiple Flows (json only exports first id) (as reader)'() {
        given:
        String id = getValidId()
        String id2 = getAdditionalValidId([
            label : 'Functional Test DataFlow 2',
            source: sourceDataModelId
        ])

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0',
             [dataFlowIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "dataFlow": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataFlow",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Asset",
    "source": {
      "id": "${json-unit.matches:id}",
      "label": "SourceFlowDataModel",
      "path": "dm:SourceFlowDataModel$main",
      "type": "Data Asset"
    },
    "target": {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
      "path": "dm:TargetFlowDataModel$main",
      "type": "Data Asset"
    }
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter",
      "name": "DataFlowJsonExporterService",
      "version": "4.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(id2)
    }


    void 'L36 : test import basic DataFlow (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)
        verifyResponse HttpStatus.OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer/DataFlowJsonImporterService/4.0', [
            modelName                      : 'Functional Test Import',
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, targetDataModelId

        cleanup:
        removeValidIdObject(id)
    }

    void 'N36 : test import basic DataFlow (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)
        verifyResponse HttpStatus.OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer/DataFlowJsonImporterService/4.0', [
            modelName                      : 'Functional Test Import',
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, targetDataModelId

        cleanup:
        removeValidIdObject(id)
    }

    void 'R36 : test import basic DataFlow (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)
        verifyResponse HttpStatus.OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer/DataFlowJsonImporterService/4.0', [
            modelName                      : 'Functional Test Import',
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        //Can read the DM but importing a DF is forbidden
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }


    void 'E36A : test import basic DataFlow (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)
        verifyResponse HttpStatus.OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer/DataFlowJsonImporterService/4.0', [
            modelName                      : 'Functional Test Import',
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])


        then:
        verifyResponse HttpStatus.CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test Import'
        response.body().items.first().id != id
        String id2 = response.body().items.first().id

        cleanup:
        removeValidIdObjectUsingTransaction(id2)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(id2, HttpStatus.NOT_FOUND)
        removeValidIdObject(id, HttpStatus.NOT_FOUND)
    }

}