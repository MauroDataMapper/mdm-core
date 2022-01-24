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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet

import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: rule
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.RuleController
 */
@Slf4j
abstract class CatalogueItemRuleFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
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
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse UNPROCESSABLE_ENTITY, response
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Rule:Functional Test Rule Name] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Rule:.+?] changed properties \[path, name]/
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
            name       : null,
            description: 'Functional Test Rule Description'
        ]
    }

    Map getValidNonDescriptionUpdateJson() {
        [
            name: "Functional Test Updated Label ${getClass().simpleName}".toString()
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "name": "Bootstrapped Functional Test Rule",
      "description": "Functional Test Description",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "name": "Functional Test Rule Name",
  "description": "Functional Test Rule Description",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }
}