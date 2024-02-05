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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.terminology.item

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: termRelationshipType
 *  |   POST   | /api/terminologies/${terminologyId}/termRelationshipTypes  | Action: save
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes  | Action: index
 *  |  DELETE  | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}  | Action: delete
 *  |   PUT    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}  | Action: update
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeController
 */
@Integration
@Slf4j
class TermRelationshipTypeFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "terminologies/${getComplexTerminologyId()}/termRelationshipTypes"
    }

    @Override
    String getEditsPath() {
        'termRelationshipTypes'
    }

    @Transactional
    String getComplexTerminologyId() {
        Terminology.findByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getSimpleTerminologyId() {
        Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME).id.toString()
    }

    @Override
    Map getValidJson() {
        [
            label       : 'is-part-of',
            displayLabel: 'Is Part Of'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label       : null,
            displayLabel: 'Is A'
        ]
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereEditorsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereAuthorsCanAction('comment', 'editDescription', 'show',)
            .whereReviewersCanAction('comment', 'show')
            .whereReadersCanAction('show')
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "TermRelationshipType",
  "label": "is-part-of",
  "path": "te:Complex Test Terminology$main|trt:is-part-of",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test Terminology",
      "domainType": "Terminology",
      "finalised": false
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "displayLabel": "Is Part Of",
  "parentalRelationship": false,
  "childRelationship": false
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationshipType",
      "label": "is-a",
      "path": "te:Complex Test Terminology$main|trt:is-a",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "displayLabel": "Is A"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationshipType",
      "label": "is-a-part-of",
      "path": "te:Complex Test Terminology$main|trt:is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "displayLabel": "Is A Part Of"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationshipType",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|trt:broaderThan",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "displayLabel": "Broader Than"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationshipType",
      "label": "narrowerThan",
      "path": "te:Complex Test Terminology$main|trt:narrowerThan",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "displayLabel": "Narrower Than"
    }
  ]
}'''
    }
}