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
 * Controller: referenceDataType
 *  |   POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes  | Action: save
 *  |   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes  | Action: index
 *  |  DELETE  | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${id}  | Action: delete
 *  |   PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${id}  | Action: update
 *  |   GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataTypes/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeController
 */
@Integration
@Slf4j
class ReferenceDataTypeFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "referenceDataModels/${getSimpleReferenceDataModelId()}/referenceDataTypes"
    }

    @Override
    String getEditsPath() {
        'referencePrimitiveType'
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
            domainType: 'ReferencePrimitiveType',
            label     : 'date'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label     : null,
            domainType: 'ReferencePrimitiveType'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'describes a date only'
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
  "domainType": "ReferencePrimitiveType",
  "label": "date",
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
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferencePrimitiveType",
      "label": "integer",
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
  ]
}'''
    }
}
