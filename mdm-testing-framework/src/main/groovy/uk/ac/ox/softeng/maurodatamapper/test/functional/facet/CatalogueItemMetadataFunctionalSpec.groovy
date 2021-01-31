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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata

import grails.gorm.transactions.Transactional
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

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

    abstract String getSourceDataModelId()

    abstract String getDestinationDataModelId()

    @OnceBefore
    @Transactional
    def cleanUpMetadataBefore() {
        Metadata.deleteAll(Metadata.list())
        sessionFactory.currentSession.flush()
    }


    String getCatalogueItemCopyPath() {
        "dataModels/${destinationDataModelId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}/${catalogueItemId}"
    }

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

    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        verifyResponse(HttpStatus.CREATED, response)
    }

    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        String copyId = response.body().id
        GET(getCopyResourcePath(copyId), MAP_ARG, true)
    }

    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 1
        assert response.body().items.size() == 1
    }

    void 'CIF01 : Test facet copied with catalogue item'() {
        given: 'Create new facet on catalogue item'
        def id = createNewItem(validJson)

        when: 'Copy catalogue item'
        POST(catalogueItemCopyPath, [:], MAP_ARG, true)

        then: 'Check successful copy'
        verifyCIF01SuccessfulCatalogueItemCopy(response)

        when: 'Retrieve the facets on the newly copied catalogue item'
        requestCIF01CopiedCatalogueItemFacet(response)

        then: 'Check our recent new facet was copied with the catalogue item'
        verifyCIF01CopiedFacetSuccessfully(response)

        cleanup: 'Remove facet from source catalogue item'
        cleanUpData(id)
    }
}