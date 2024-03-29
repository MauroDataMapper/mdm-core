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

import java.util.regex.Pattern

/**
 * <pre>
 * Controller: term
 *  |   POST   | /api/terminologies/${terminologyId}/terms  | Action: save
 *  |   GET    | /api/terminologies/${terminologyId}/terms  | Action: index
 *  |  DELETE  | /api/terminologies/${terminologyId}/terms/${id}  | Action: delete
 *  |   PUT    | /api/terminologies/${terminologyId}/terms/${id}  | Action: update
 *  |   GET    | /api/terminologies/${terminologyId}/terms/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.TermController
 */
@Integration
@Slf4j
class TermFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "terminologies/${getSimpleTerminologyId()}/terms"
    }

    @Override
    String getEditsPath() {
        'terms'
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
            code      : 'FTT01',
            definition: 'A new Term'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            code      : 'FTT01',
            definition: null
        ]
    }

    @Override
    Map getValidNonDescriptionUpdateJson() {
        [
            definition: 'Updated term'
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

    Pattern getExpectedUpdateEditRegex() {
        ~/\[\w+( \w+)*:.+?] changed properties \[definition, label]/
    }


    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "Term",
  "label": "FTT01: A new Term",
  "path": "te:Simple Test Terminology$main|tm:FTT01",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Simple Test Terminology",
      "domainType": "Terminology",
      "finalised": false
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "code": "FTT01",
  "definition": "A new Term"
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT01: Simple Test Term 01",
      "path": "te:Simple Test Terminology$main|tm:STT01",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "code": "STT01",
      "definition": "Simple Test Term 01"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT02: Simple Test Term 02",
      "path": "te:Simple Test Terminology$main|tm:STT02",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "code": "STT02",
      "definition": "Simple Test Term 02"
    }
  ]
}
'''
    }
}