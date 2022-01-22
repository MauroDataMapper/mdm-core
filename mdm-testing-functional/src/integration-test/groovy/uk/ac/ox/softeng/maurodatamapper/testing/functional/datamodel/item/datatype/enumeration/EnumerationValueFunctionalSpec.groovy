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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item.datatype.enumeration


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: enumerationValue
 *  |  POST    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValueController
 */
@Integration
@Slf4j
class EnumerationValueFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/" +
        "enumerationTypes/${getYesNoUnknownEnumerationTypeId()}/" +
        'enumerationValues'
    }

    @Override
    String getEditsPath() {
        'enumerationValues'
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getYesNoUnknownEnumerationTypeId() {
        EnumerationType.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), 'yesnounknown').get().id.toString()
    }

    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse UNPROCESSABLE_ENTITY, response
        assert response.body().total == 4
        assert response.body().errors.any {
            it.message == 'Property [enumerationType] of class [class uk.ac.ox.softeng.maurodatamapper.' +
            'datamodel.item.datatype.enumeration.EnumerationValue] cannot be null'
        }
        assert response.body().errors.any {
            it.message == 'Property [parent] of class [class uk.ac.ox.softeng.maurodatamapper.core.' +
            'facet.BreadcrumbTree] cannot be null'
        }
        assert response.body().errors.any {
            it.message == 'Property [path] of class [class uk.ac.ox.softeng.maurodatamapper.' +
            'datamodel.item.datatype.enumeration.EnumerationValue] cannot be null'
        }
        assert response.body().errors.any {
            it.message == 'Property [path] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree] cannot be null'
        }
    }

    @Override
    Map getValidJson() {
        [
            key  : 'O',
            value: 'Other'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            key  : 'Y',
            value: 'Affirmative'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            value: 'Optional'
        ]
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[EnumerationValue:O] changed properties \[value, description]/
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "index": 0,
      "id": "${json-unit.matches:id}",
      "category": null, 
      "value": "Yes",
      "key": "Y"
    },
    {
      "index": 1,
      "id": "${json-unit.matches:id}",
      "category": null, 
      "value": "No",
      "key": "N"
    },
    {
      "index": 2,
      "id": "${json-unit.matches:id}",
      "category": null, 
      "value": "Unknown",
      "key": "U"
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "index": 3,
  "id": "${json-unit.matches:id}",
  "category": null, 
  "value": "Other",
  "key": "O"
}'''
    }
}