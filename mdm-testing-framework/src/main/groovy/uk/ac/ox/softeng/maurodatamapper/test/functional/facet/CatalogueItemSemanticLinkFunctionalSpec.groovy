/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType

import grails.gorm.transactions.Transactional
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController* Controller: semanticLink
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks       | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks       | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemSemanticLinkFunctionalSpec extends CatalogueItemFacetFunctionalSpec<SemanticLink> {

    abstract String getTargetCatalogueItemId()

    abstract String getTargetCatalogueItemDomainType()

    abstract String getCatalogueItemDomainType()

    abstract String getTargetCatalogueItemJsonString()

    abstract String getSourceCatalogueItemJsonString()

    abstract String getSourceDataModelId()

    abstract String getDestinationDataModelId()

    @Transactional
    @RunOnce
    def cleanUpSemanticLinksBefore() {
        log.debug('Cleanup SemanticLinks before')
        SemanticLink.deleteAll(SemanticLink.list())
        sessionFactory.currentSession.flush()
    }

    String getCatalogueItemCopyPath() {
        "dataModels/${destinationDataModelId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}/${catalogueItemId}"
    }

    @Override
    String getFacetResourcePath() {
        'semanticLinks'
    }

    @Override
    Map getValidJson() {
        [
            linkType                           : SemanticLinkType.REFINES.label,
            targetMultiFacetAwareItemId        : getTargetCatalogueItemId(),
            targetMultiFacetAwareItemDomainType: getTargetCatalogueItemDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType                 : SemanticLinkType.REFINES.label,
            targetMultiFacetAwareItem: getCatalogueItemId().toString(),

        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            linkType: SemanticLinkType.DOES_NOT_REFINE.label
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "Refines",
  "unconfirmed":false,
  "domainType": "SemanticLink",
  "sourceMultiFacetAwareItem": ''' + getSourceCatalogueItemJsonString() + ''',
  "targetMultiFacetAwareItem": ''' + getTargetCatalogueItemJsonString() + '''
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().linkType == SemanticLinkType.DOES_NOT_REFINE.label
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
        // Count manually and automatically created semantic links
        assert response.body().count == 2
        assert response.body().items.size() == 2
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