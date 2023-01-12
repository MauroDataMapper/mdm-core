/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import static io.micronaut.http.HttpStatus.CREATED

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
 * @see uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponentController
 */
@Integration
@Slf4j
class DataClassComponentFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${targetDataModelId}/dataFlows/${dataFlowId}/dataClassComponents"
    }

    @Override
    String getEditsPath() {
        'dataClassComponents'
    }

    @Transactional
    String getTargetDataModelId() {
        DataModel.findByLabel(BootstrapModels.TARGET_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getSourceDataModelId() {
        DataModel.findByLabel(BootstrapModels.SOURCE_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getDataFlowId() {
        DataFlow.findByLabel(BootstrapModels.DATAFLOW_NAME).id.toString()
    }

    @Transactional
    String getTargetDataClassId() {
        DataClass.findByLabel('tableE').id.toString()
    }

    @Transactional
    String getSourceDataClassId() {
        DataClass.findByLabel('tableC').id.toString()
    }

    @Transactional
    String getTargetDataClassId2() {
        DataClass.findByLabel('tableD').id.toString()
    }

    @Transactional
    String getSourceDataClassId2() {
        DataClass.findByLabel('tableB').id.toString()
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

    @Override
    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert responseBody().id
        assert responseBody().label == 'Functional Test DataClassComponent'
        assert responseBody().sourceDataClasses.size() == 1
        assert responseBody().sourceDataClasses.first().id == sourceDataClassId
        assert responseBody().targetDataClasses.size() == 1
        assert responseBody().targetDataClasses.first().id == targetDataClassId
        assert responseBody().dataFlow == dataFlowId
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
        verify03ValidResponseBody(response)
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
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClassComponent",
      "label": "aToD",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        }
      ],
      "dataFlow": "${json-unit.matches:id}",
      "sourceDataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataClass",
          "label": "tableA",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
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
          "label": "tableD",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            }
          ]
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClassComponent",
      "label": "bAndCToE",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        }
      ],
      "dataFlow": "${json-unit.matches:id}",
      "sourceDataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataClass",
          "label": "tableB",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            }
          ]
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataClass",
          "label": "tableC",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
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
          "label": "tableE",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            }
          ]
        }
      ]
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataClassComponent",
  "label": "Functional Test DataClassComponent",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
      "domainType": "DataModel",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Sample DataFlow",
      "domainType": "DataFlow"
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "dataFlow": "${json-unit.matches:id}",
  "sourceDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "tableC",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "SourceFlowDataModel",
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
      "label": "tableE",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    void 'L06 : test adding a source DataClass to DataClassComponent (as logged out)'() {
        given:
        def id = getValidId()

        when:
        logout()
        PUT("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N06 : test adding a source DataClass to DataClassComponent (as no access/authenticated)'() {
        given:
        def id = getValidId()

        when:
        loginAuthenticated()
        PUT("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'R06 : test adding a source DataClass to DataClassComponent (as reader)'() {
        given:
        def id = getValidId()

        when:
        loginReader()
        PUT("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E06 : test adding a source DataClass to DataClassComponent (as editor)'() {
        given:
        def id = getValidId()

        when:
        loginEditor()
        PUT("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 2
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId2.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'A06 : test adding a source DataClass to DataClassComponent (as admin)'() {
        given:
        def id = getValidId()

        when:
        loginAdmin()
        PUT("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 2
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId2.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'L07 : test adding a target DataClass to DataClassComponent (as logged out)'() {
        given:
        def id = getValidId()

        when:
        logout()
        PUT("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N07 : test adding a target DataClass to DataClassComponent (as no access/authenticated)'() {
        given:
        def id = getValidId()

        when:
        loginAuthenticated()
        PUT("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'R07 : test adding a target DataClass to DataClassComponent (as reader)'() {
        given:
        def id = getValidId()

        when:
        loginReader()
        PUT("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E07 : test adding a target DataClass to DataClassComponent (as editor)'() {
        given:
        def id = getValidId()

        when:
        loginEditor()
        PUT("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 2
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId2.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'A07 : test adding a target DataClass to DataClassComponent (as admin)'() {
        given:
        def id = getValidId()

        when:
        loginAdmin()
        PUT("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 2
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId2.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'L08 : test removing a source DataClass to DataClassComponent (as logged out)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/source/${sourceDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        logout()
        DELETE("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N08 : test removing a source DataClass to DataClassComponent (as no access/authenticaed)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/source/${sourceDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginAuthenticated()
        DELETE("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'R08 : test removing a source DataClass to DataClassComponent (as reader)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/source/${sourceDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginReader()
        DELETE("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E08 : test removing a source DataClass to DataClassComponent (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/source/${sourceDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginEditor()
        DELETE("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        !responseBody().sourceDataClasses.any {it.id == sourceDataClassId2.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'A08 : test removing a source DataClass to DataClassComponent (as admin)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/source/${sourceDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginAdmin()
        DELETE("${id}/source/${sourceDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        !responseBody().sourceDataClasses.any {it.id == sourceDataClassId2.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'L09 : test removing a target DataClass to DataClassComponent (as logged out)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/target/${targetDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        logout()
        DELETE("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N09 : test removing a target DataClass to DataClassComponent (as no access/authenticated)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/target/${targetDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginAuthenticated()
        DELETE("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'R09 : test removing a target DataClass to DataClassComponent (as reader)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/target/${targetDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginReader()
        DELETE("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E09 : test removing a target DataClass to DataClassComponent (as editor)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/target/${targetDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginEditor()
        DELETE("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}
        !responseBody().targetDataClasses.any {it.id == targetDataClassId2.toString()}

        cleanup:
        removeValidIdObject(id)
    }

    void 'A09 : test removing a target DataClass to DataClassComponent (as admin)'() {
        given:
        def id = getValidId()
        loginEditor()
        PUT("${id}/target/${targetDataClassId2}", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        loginAdmin()
        DELETE("${id}/target/${targetDataClassId2}", [:])

        then:
        verifyResponse HttpStatus.OK, response
        responseBody().sourceDataClasses.size() == 1
        responseBody().targetDataClasses.size() == 1
        responseBody().sourceDataClasses.any {it.id == sourceDataClassId.toString()}
        responseBody().targetDataClasses.any {it.id == targetDataClassId.toString()}
        !responseBody().targetDataClasses.any {it.id == targetDataClassId2.toString()}

        cleanup:
        removeValidIdObject(id)
    }
}