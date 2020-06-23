/*
 * Copyright 2020 University of Oxford
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

import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 * Controller: summaryMetadata
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata        | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataController
 */
@Slf4j
abstract class CatalogueItemSummaryMetadataFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/summaryMetadata"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}"
    }

    @Transactional
    @Override
    def cleanupSpec() {
        log.info('Removing functional test summary metadata')
        SummaryMetadata.byLabel('Functional Test Summary Metadata').deleteAll()
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

    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Summary Metadata:Functional Test Summary Metadata] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Summary Metadata:Functional Test Summary Metadata] changed properties \[description]/
    }

    @Override
    Map getValidJson() {
        [
            label              : 'Functional Test Summary Metadata',
            summaryMetadataType: 'NUMBER'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            summaryMetadataType: 'object'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Attempting update'
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "createdBy": "editor@test.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "label": "Functional Test Summary Metadata",
  "summaryMetadataType": "NUMBER"
}'''
    }
}