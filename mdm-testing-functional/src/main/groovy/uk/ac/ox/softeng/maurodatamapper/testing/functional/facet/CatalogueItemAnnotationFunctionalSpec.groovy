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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet

import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 *  Controller: annotation
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: delete
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Transactional
@Slf4j
abstract class CatalogueItemAnnotationFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/annotations"
    }

    @Override
    Boolean getReaderCanCreate() {
        true
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test Annotation'
        ]
    }

    @Override
    Map getInvalidJson() {
        [:]
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0,"items": []}'
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "createdBy": "editor@test.com",
  "annotatorName": "editor User",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Annotation"
}'''
    }
}