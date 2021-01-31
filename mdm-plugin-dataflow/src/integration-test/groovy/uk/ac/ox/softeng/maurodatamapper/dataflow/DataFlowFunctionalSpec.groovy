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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

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

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)


        sourceDataModelId = BootstrapModels.buildAndSaveSourceDataModel(messageSource, folder, testAuthority).id
        targetDataModelId = BootstrapModels.buildAndSaveTargetDataModel(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataFlowFunctionalSpec')
        cleanUpResources(DataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
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

    /*
    TODO importers/exporters

        void 'test exporting DataFlow'() {
            given:
            def id = getValidId()

            when: 'not logged in'
            def response = restGet("$id/export/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0")

            then:
            verifyUnauthorised response

            when: 'logged in as reader'
            loginUser(reader2)
            response = restGet("$id/export/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0")

            then: 'there are no dataflow importers or exporters available in core'
            verifyResponse NOT_FOUND, response

            when: 'logged in as writer'
            loginEditor()
            response = restGet("$id/export/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0")

            then: 'there are no dataflow importers or exporters available in core'
            verifyResponse NOT_FOUND, response
        }

        void 'test exporting DataFlows'() {
            given:
            def id = getValidId()

            when: 'not logged in'
            def response = post('export/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0') {
                json([id])
            }

            then:
            verifyUnauthorised response

            when: 'logged in as reader'
            loginUser(reader2)
            response = post('export/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0') {
                json([id])
            }

            then: 'there are no dataflow importers or exporters available in core'
            verifyResponse NOT_FOUND, response

            when: 'logged in as writer'
            loginEditor()
            response = post('export/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0') {
                json([id])
            }

            then: 'there are no dataflow importers or exporters available in core'
            verifyResponse NOT_FOUND, response
        }

        void 'test importing DataFlows'() {
            when: 'not logged in'
            def response = post('import/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0') {
                contentType MimeType.MULTIPART_FORM.name
                body = ''
            }

            then:
            verifyUnauthorised response

            when: 'logged in as reader'
            loginUser(reader2)
            response = post('import/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0') {
                contentType MimeType.MULTIPART_FORM.name
                body = ''
            }

            then:
            verifyUnauthorised response

            when: 'logged in as writer'
            loginEditor()
            response = post('import/ox.softeng.metadatacatalogue.plugins.excel/ExcelDataFlowExporterService/1.0.0') {
                contentType MimeType.MULTIPART_FORM.name
                body = ''
            }

            then: 'there are no dataflow importers or exporters available in core'
            verifyResponse NOT_FOUND, response
        }
    */
}