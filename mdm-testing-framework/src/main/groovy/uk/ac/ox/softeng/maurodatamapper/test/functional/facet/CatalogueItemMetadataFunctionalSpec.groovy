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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata

import groovy.util.logging.Slf4j

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController* Controller: metadata
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata         | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata         | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}   | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}   | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}   | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemMetadataFunctionalSpec extends CatalogueItemFacetFunctionalSpec<Metadata> {

    @Override
    String getFacetResourcePath() {
        'metadata'
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
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "namespace": "functional.test.namespace",
  "id": "${json-unit.matches:id}",
  "value": "ftv",
  "key": "ftk"
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().value == 'ftv.update'
    }
}