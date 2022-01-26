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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: dataFlow
 *  |   POST   | /api/dataModels/${dataModelId}/dataFlows       | Action: save
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows       | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${id} | Action: delete
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${id} | Action: update
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${id} | Action: show
 *
 *  |   GET    | /api/dataFlows/providers/importers  | Action: importerProviders
 *  |   GET    | /api/dataFlows/providers/exporters  | Action: exporterProviders
 *  |   POST   | /api/dataFlows/import/${importerNamespace}/${importerName}/${importerVersion} | Action: importModels
 *  |   POST   | /api/dataFlows/export/${exporterNamespace}/${exporterName}/${exporterVersion} | Action: exportModels
 *
 *  |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/import/${importerNamespace}/${importerName}/${importerVersion}  | Action:
 *  importModel
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action:
 *  exportDataFlow
 *
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/diagramLayout  | Action: updateDiagramLayout
 *
 *  </pre>
 * @see DataFlowController
 */
@Integration
@Slf4j
class DataFlowFunctionalSpec extends ResourceFunctionalSpec<DataFlow> {

    @Shared
    Folder folder

    @Shared
    UUID sourceDataModelId

    @Shared
    UUID targetDataModelId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        sourceDataModelId = BootstrapModels.buildAndSaveSourceDataModel(messageSource, folder, testAuthority).id
        targetDataModelId = BootstrapModels.buildAndSaveTargetDataModel(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataFlowFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    String getResourcePath() {
        "dataModels/${targetDataModelId}/dataFlows"
    }


    Map getValidJson() {
        [
            label : 'Functional Test DataFlow',
            source: sourceDataModelId
        ]
    }

    Map getInvalidJson() {
        [
            label : null,
            source: sourceDataModelId
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'This is nothing special'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataFlow",
  "label": "Functional Test DataFlow",
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
    "delete",
    "show",
    "update"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "source": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "SourceFlowDataModel",
    "type": "Data Asset",
    "branchName": "main",
    "documentationVersion": "1.0.0"
  },
  "target": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "TargetFlowDataModel",
    "type": "Data Asset",
    "branchName": "main",
    "documentationVersion": "1.0.0"
  },
  "diagramLayout": null
}'''
    }

    void 'test updating diagram layout'() {
        given:
        def id = createNewItem(validJson)

        when:
        PUT("$id/diagramLayout",
            [
                diagramLayout: 'this is how to draw the diagram'
            ])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().diagramLayout == 'this is how to draw the diagram'

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }


    void 'test getting DataFlow exporters'() {
        when:
        GET('/api/dataFlows/providers/exporters', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "DataFlowXmlExporterService",
    "version": "4.0",
    "displayName": "XML DataFlow Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "DataFlowExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": false
  },
  {
    "name": "DataFlowJsonExporterService",
    "version": "4.0",
    "displayName": "JSON DataFlow Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "DataFlowExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": false
  }
]'''
    }

    void 'test getting DataFlow importers'() {
        when:
        GET('/api/dataFlows/providers/importers', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowFileImporterProviderServiceParameters",
    "providerType": "DataFlowImporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "XML DataFlow Importer",
    "name": "DataFlowXmlImporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": true,
    "version": "${json-unit.matches:version}"
  },
  {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowFileImporterProviderServiceParameters",
    "providerType": "DataFlowImporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "JSON DataFlow Importer",
    "name": "DataFlowJsonImporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": false,
    "version": "${json-unit.matches:version}"
  }
]'''
    }

    void 'test export a single DataFlow'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "dataFlow": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataFlow",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Asset",
    "source": {
      "id": "${json-unit.matches:id}",
      "label": "SourceFlowDataModel",
      "path" : "dm:SourceFlowDataModel$main",
      "type": "Data Asset"
    },
    "target": {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
      "path" : "dm:TargetFlowDataModel$main",
      "type": "Data Asset"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter",
      "name": "DataFlowJsonExporterService",
      "version": "4.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test export multiple DataFlows when json only exports first id'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([
            label : 'Functional Test DataFlow 2',
            source: sourceDataModelId
        ])

        when:
        POST("export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0",
             [dataFlowIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "dataFlow": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataFlow",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Asset",
    "source": {
      "id": "${json-unit.matches:id}",
      "label": "SourceFlowDataModel",
      "path" : "dm:SourceFlowDataModel$main",
      "type": "Data Asset"
    },
    "target": {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
       "path" : "dm:TargetFlowDataModel$main",
      "type": "Data Asset"
    }
  },
  "exportMetadata": {
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter",
      "name": "DataFlowJsonExporterService",
      "version": "4.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }


    void 'test import the sample data flow'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter/DataFlowJsonExporterService/4.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST("import/uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer/DataFlowJsonImporterService/4.0", [
            modelName : 'Functional Test Import',
            importFile: [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataFlow",
      "label": "Functional Test Import",
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
        "type": "Data Asset",
        "branchName": "main",
        "documentationVersion": "1.0.0"
      },
      "target": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "TargetFlowDataModel",
        "type": "Data Asset",
        "branchName": "main",
        "documentationVersion": "1.0.0"
      },
      "diagramLayout": null
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

}