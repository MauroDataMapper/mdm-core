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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test.functional

import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemFacetFunctionalSpec

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 *
 * @see ModelImportController* Controller: modelImport
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports         | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports         | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}   | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}   | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}   | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemModelImportFunctionalSpec extends CatalogueItemFacetFunctionalSpec<ModelImport> {

    abstract String getSourceDataModelId()

    abstract String getDestinationDataModelId()

    @Override
    String getFacetResourcePath() {
        'modelImports'
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

    @Override
    void verifyR4UpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }   

    @Override
    void verifyR4InvalidUpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
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