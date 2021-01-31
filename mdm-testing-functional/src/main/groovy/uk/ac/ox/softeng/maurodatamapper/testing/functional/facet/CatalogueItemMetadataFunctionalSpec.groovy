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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

/**
 * <pre>
 * Controller: metadata
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
 */
@Slf4j
abstract class CatalogueItemMetadataFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/metadata"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}"
    }

    @Transactional
    @Override
    def cleanupSpec() {
        log.info('Removing functional test metadata')
        Metadata.byNamespaceAndKey('functional.test.namespace', 'ftk').deleteAll()
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
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Metadata:functional.test.namespace:ftk] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Metadata:functional.test.namespace:ftk] changed properties \[value]/
    }

    @Override
    Map getValidJson() {
        [
            namespace: 'functional.test.namespace',
            key      : 'ftk',
            value    : 'ftv'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            namespace: null,
            key      : 'ftk',
            value    : 'ftv'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            value: 'ftv.update'
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
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "namespace": "functional.test.namespace",
  "id": "${json-unit.matches:id}",
  "value": "ftv",
  "key": "ftk"
}'''
    }
}