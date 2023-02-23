/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.RuleController* Controller: rule
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentationController* Controller: ruleRepresentation
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/rules                                              | Action: save      |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules                                              | Action: index     |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}                                        | Action: delete    |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}                                        | Action: update    |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}                                        | Action: show      |
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}/representations                        | Action: save      |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}/representations                        | Action: index     |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}/representations/${representationId}    | Action: delete    |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}/representations/${representationId}    | Action: update    |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}/representations/${representationId}    | Action: show      |
 */
@Slf4j
abstract class CatalogueItemRuleFunctionalSpec extends CatalogueItemFacetFunctionalSpec<Rule> {

    abstract String getSourceCatalogueItemId()

    abstract String getDestinationCatalogueItemId()

    @Override
    String getFacetResourcePath() {
        'rules'
    }

    @Override
    Map getValidJson() {
        [
            name        : 'Functional Test Rule Name',
            description : 'Functional Test Rule Description'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            name: null,
            description : 'Functional Test Rule Description'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description : 'Functional Test Rule Description Updated'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "name": "Functional Test Rule Name",
  "description": "Functional Test Rule Description",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    String getRuleRepresentationResourcePath(String id) {
        "${getResourcePath()}/${id}/representations"
    }

    Map getValidRuleRepresentationJson() {
        [
            language         : 'sql',
            representation   : '0 < a < 25'
        ]
    }

    Map getInvalidRuleRepresentationJson() {
        [
            language         : null,
            representation   : '0 < a < 25'
        ]
    }

    Map getValidRuleRepresentationUpdateJson() {
        [
            representation   : '0 < a < 30'
        ]
    }

    String getExpectedRuleRepresentationShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "language": "sql",
  "representation": "0 < a < 25",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    String getExpectedRuleRepresentationUpdateJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "language": "sql",
  "representation": "0 < a < 30",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().value == 'Functional Test Rule Description Updated'
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