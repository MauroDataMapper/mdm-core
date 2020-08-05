package uk.ac.ox.softeng.maurodatamapper.testing.functional.dataflow


import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
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
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTargetDataModelId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTargetDataModelId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTargetDataModelId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTargetDataModelId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTargetDataModelId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTargetDataModelId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
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
    Map getValidUpdateJson() {
        [
            description: 'This is nothing special'
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
        "type": "Data Asset"
      },
      "target": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "TargetFlowDataModel",
        "type": "Data Asset"
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
    "show",
    "comment",
    "editDescription",
    "update",
    "save",
    "delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "source": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "SourceFlowDataModel",
    "type": "Data Asset"
  },
  "target": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "TargetFlowDataModel",
    "type": "Data Asset"
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