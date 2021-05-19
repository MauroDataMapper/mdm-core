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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

/**
 * <pre>
 * Controller: referenceDataElement
 *  |   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements  | Action: save
 *  |   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements  | Action: index
 *  |  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${id}  | Action: delete
 *  |   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${id}  | Action: update
 *  |   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataElements/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementController
 */
@Integration
@Slf4j
class ReferenceDataElementFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "referenceDataModels/${getSimpleReferenceDataModelId()}/referenceDataElements"
    }

    @Override
    String getEditsPath() {
        'referenceDataElements'
    }

    @Transactional
    String getSimpleReferenceDataModelId() {
        ReferenceDataModel.findByLabel(BootstrapModels.SIMPLE_REFERENCE_MODEL_NAME).id.toString()
    }

    @Transactional
    String getReferenceDataTypeId() {
        ReferencePrimitiveType.findByLabel("string").id.toString()
    }

    @Override
    Map getValidJson() {
        [
            label            : 'Functional Test DataElement',
            maxMultiplicity  : 2,
            minMultiplicity  : 0,
            referenceDataType: referenceDataTypeId
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label            : UUID.randomUUID().toString(),
            maxMultiplicity  : 2,
            minMultiplicity  : 0,
            referenceDataType: null
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'adding a description'
        ]
    }

    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getSimpleReferenceDataModelId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getSimpleReferenceDataModelId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getSimpleReferenceDataModelId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getSimpleReferenceDataModelId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getSimpleReferenceDataModelId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getSimpleReferenceDataModelId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    @Override
    void verifyE03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ReferenceDataElement",
  "label": "Functional Test DataElement",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Simple Reference Data Model",
      "domainType": "ReferenceDataModel",
      "finalised": false
    }
  ],
  "availableActions": [
    "show",
    "comment",
    "editDescription",
    "update",
    "save",
    "delete",
    "canAddRule"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "referenceDataType": {
    "id": "${json-unit.matches:id}",
    "domainType": "ReferencePrimitiveType",
    "label": "string",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Simple Reference Data Model",
        "domainType": "ReferenceDataModel",
        "finalised": false
      }
    ]
  },
  "maxMultiplicity": 2,
  "minMultiplicity": 0
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataElement",
      "label": "Organisation name",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Reference Data Model",
          "domainType": "ReferenceDataModel",
          "finalised": false
        }
      ],
      "referenceDataType": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferencePrimitiveType",
        "label": "string",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ]
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataElement",
      "label": "Organisation code",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Reference Data Model",
          "domainType": "ReferenceDataModel",
          "finalised": false
        }
      ],
      "referenceDataType": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferencePrimitiveType",
        "label": "string",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ]
      }
    }
  ]
}'''
    }
}
