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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: ruleRepresentation
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentationController
 */
@Slf4j
abstract class CatalogueItemRuleRepresentationFunctionalSpec extends UserAccessFunctionalSpec {

    abstract CatalogueItem getModel()

    abstract CatalogueItem getCatalogueItem()

    abstract String getCatalogueItemDomainType()

    @Shared
    Rule rule

    String getModelId() {
        getModel().id.toString()
    }

    String getCatalogueItemId() {
        getCatalogueItem().id.toString()
    }

    @Transactional
    String getRuleId() {
        Rule.findByMultiFacetAwareItemIdAndName(getCatalogueItemId(), "Bootstrapped Functional Test Rule").id.toString()
    }

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/rules/${getRuleId()}/representations"
    }

    String getRuleResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/rules"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}"
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .withoutAvailableActions()
            .whereAuthors {
                cannotEditDescription()
            }
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[RuleRepresentation:sql on Rule Rule:Bootstrapped Functional Test Rule] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[RuleRepresentation:sql on Rule Rule:Bootstrapped Functional Test Rule] changed properties \[representation]/
    }

    @Override
    Map getValidJson() {
        [
            language       : "sql",
            representation : 'A > 0 AND A < 5'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            language       : null,
            representation : 'A > 0 AND A < 5'
        ]
    }

    @Override
    Map getValidNonDescriptionUpdateJson() {
        [
            representation: 'A > 0 AND A < 6'
        ]
    }

    @Override
    Map getValidDescriptionOnlyUpdateJson() {
        getValidNonDescriptionUpdateJson()
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
  "language": "sql",
  "representation": "A > 0 AND A < 5",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    String getRuleShowJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "name": "Bootstrapped Functional Test Rule",
      "description": "Functional Test Description",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "ruleRepresentations": [
        {
          "id": "${json-unit.matches:id}",
          "language": "sql",
          "representation": "A > 0 AND A < 5",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ]
    }
  ]
}'''
    }

    void 'A03a: Test the Rule endpoint also lists rule representations'() {
        given:
        loginAdmin()

        when:
        POST(getSavePath(), validJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String ruleRepresentationId = response.body().id

        when: 'Get rules'
        GET(getRuleResourcePath(), STRING_ARG, true)

        then:
        verifyJsonResponse OK, getRuleShowJson()

        cleanup:
        removeValidIdObject(ruleRepresentationId)
    }
}
