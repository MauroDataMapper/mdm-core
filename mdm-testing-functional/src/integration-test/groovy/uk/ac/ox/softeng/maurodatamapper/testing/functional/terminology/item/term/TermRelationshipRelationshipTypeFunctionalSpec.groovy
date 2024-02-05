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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.terminology.item.term

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ReadOnlyUserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: termRelationship
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships        | Action: index
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipController
 */
@Integration
@Slf4j
class TermRelationshipRelationshipTypeFunctionalSpec extends ReadOnlyUserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "terminologies/${getComplexTerminologyId()}/termRelationshipTypes/${getRelationshipTypeId()}/termRelationships"
    }

    @Transactional
    String getComplexTerminologyId() {
        Terminology.findByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getRelationshipTypeId() {
        TermRelationshipType.byTerminologyIdAndLabel(Utils.toUuid(getComplexTerminologyId()), 'broaderThan').get().id.toString()
    }

    @Transactional
    @Override
    String getValidId() {
        TermRelationship.byRelationshipTypeId(Utils.toUuid(getRelationshipTypeId())).get().id.toString()
    }

    @Override
    void removeValidIdObject(String id) {
        //no-op dont remove
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
  "domainType": "TermRelationship",
  "label": "broaderThan",
  "path": "te:Complex Test Terminology$main|tm:CTT11|tr:CTT11.broaderThan.CTT12",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test Terminology",
      "domainType": "Terminology",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "CTT11: Complex Test Term 11",
      "domainType": "Term"
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "relationshipType": {
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
  "sourceTerm": {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT11: Complex Test Term 11",
    "path": "te:Complex Test Terminology$main|tm:CTT11",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "code": "CTT11",
    "definition": "Complex Test Term 11"
  },
  "targetTerm": {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT12: Complex Test Term 12",
    "path": "te:Complex Test Terminology$main|tm:CTT12",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "code": "CTT12",
    "definition": "Complex Test Term 12"
  }
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 8,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT13|tr:CTT13.broaderThan.CTT14",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT13: Complex Test Term 13",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT13: Complex Test Term 13",
        "path": "te:Complex Test Terminology$main|tm:CTT13",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT13",
        "definition": "Complex Test Term 13"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT14: Complex Test Term 14",
        "path": "te:Complex Test Terminology$main|tm:CTT14",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT14",
        "definition": "Complex Test Term 14"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT12|tr:CTT12.broaderThan.CTT13",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT12: Complex Test Term 12",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT12: Complex Test Term 12",
        "path": "te:Complex Test Terminology$main|tm:CTT12",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT12",
        "definition": "Complex Test Term 12"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT13: Complex Test Term 13",
        "path": "te:Complex Test Terminology$main|tm:CTT13",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT13",
        "definition": "Complex Test Term 13"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT11|tr:CTT11.broaderThan.CTT12",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT11: Complex Test Term 11",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT11: Complex Test Term 11",
        "path": "te:Complex Test Terminology$main|tm:CTT11",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT11",
        "definition": "Complex Test Term 11"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT12: Complex Test Term 12",
        "path": "te:Complex Test Terminology$main|tm:CTT12",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT12",
        "definition": "Complex Test Term 12"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT15|tr:CTT15.broaderThan.CTT16",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT15: Complex Test Term 15",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT15: Complex Test Term 15",
        "path": "te:Complex Test Terminology$main|tm:CTT15",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT15",
        "definition": "Complex Test Term 15"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT16: Complex Test Term 16",
        "path": "te:Complex Test Terminology$main|tm:CTT16",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT16",
        "definition": "Complex Test Term 16"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT14|tr:CTT14.broaderThan.CTT15",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT14: Complex Test Term 14",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT14: Complex Test Term 14",
        "path": "te:Complex Test Terminology$main|tm:CTT14",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT14",
        "definition": "Complex Test Term 14"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT15: Complex Test Term 15",
        "path": "te:Complex Test Terminology$main|tm:CTT15",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT15",
        "definition": "Complex Test Term 15"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT16|tr:CTT16.broaderThan.CTT17",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT16: Complex Test Term 16",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT16: Complex Test Term 16",
        "path": "te:Complex Test Terminology$main|tm:CTT16",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT16",
        "definition": "Complex Test Term 16"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT17: Complex Test Term 17",
        "path": "te:Complex Test Terminology$main|tm:CTT17",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT17",
        "definition": "Complex Test Term 17"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT18|tr:CTT18.broaderThan.CTT19",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT18: Complex Test Term 18",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT18: Complex Test Term 18",
        "path": "te:Complex Test Terminology$main|tm:CTT18",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT18",
        "definition": "Complex Test Term 18"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT19: Complex Test Term 19",
        "path": "te:Complex Test Terminology$main|tm:CTT19",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT19",
        "definition": "Complex Test Term 19"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "broaderThan",
      "path": "te:Complex Test Terminology$main|tm:CTT17|tr:CTT17.broaderThan.CTT18",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT17: Complex Test Term 17",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
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
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT17: Complex Test Term 17",
        "path": "te:Complex Test Terminology$main|tm:CTT17",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT17",
        "definition": "Complex Test Term 17"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT18: Complex Test Term 18",
        "path": "te:Complex Test Terminology$main|tm:CTT18",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT18",
        "definition": "Complex Test Term 18"
      }
    }
  ]
}'''
    }
}