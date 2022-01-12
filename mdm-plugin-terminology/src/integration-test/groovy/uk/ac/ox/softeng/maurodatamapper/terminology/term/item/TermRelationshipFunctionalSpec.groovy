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
package uk.ac.ox.softeng.maurodatamapper.terminology.term.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

/**
 * <pre>
 * Controller: termRelationship
 *  |   POST   | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships                                            | Action: save
 *  |   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships                                            | Action: index
 *  |  DELETE  | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}                                      | Action: delete
 *  |   PUT    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}                                      | Action: update
 *  |   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}                                      | Action: show
 *
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships/${id}      | Action: show
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships            | Action: index
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeController
 */
@Integration
@Transactional
@Slf4j
class TermRelationshipFunctionalSpec extends ResourceFunctionalSpec<TermRelationship> {

    @Shared
    UUID terminologyId

    @Shared
    UUID termId

    @Shared
    UUID term2Id

    @Shared
    UUID termRelationshipTypeId

    @Shared
    Folder folder

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Terminology terminology = new Terminology(label: 'Functional Test Terminology', createdBy: FUNCTIONAL_TEST,
                                                  folder: folder, authority: testAuthority).save(flush: true)
        terminologyId = terminology.id
        Term term = new Term(code: 'FT01', definition: 'Functional Test 01', createdBy: FUNCTIONAL_TEST,
                             terminology: terminology).save(flush: true)
        termId = term.id
        Term term2 = new Term(code: 'FT02', definition: 'Functional Test 02', createdBy: FUNCTIONAL_TEST,
                              terminology: terminology).save(flush: true)
        term2Id = term2.id
        TermRelationshipType termRelationshipType = new TermRelationshipType(label: 'is-a', displayLabel: 'Is A', createdBy: FUNCTIONAL_TEST,
                                                                             terminology: terminology).save(flush: true)
        termRelationshipTypeId = termRelationshipType.id
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec TermFunctionalSpec')
        cleanUpResources(TermRelationship, Term, TermRelationshipType, Terminology, Folder)
    }

    @Override
    String getResourcePath() {
        "terminologies/${terminologyId}/terms/${termId}/termRelationships"
    }

    @Override
    Map getValidJson() {
        [
            relationshipType: termRelationshipTypeId.toString(),
            sourceTerm      : termId.toString(),
            targetTerm      : term2Id.toString()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            relationshipType: termRelationshipTypeId.toString(),
            sourceTerm      : termId.toString(),
            targetTerm      : termId.toString()
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            relationshipType: UUID.randomUUID().toString()
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "TermRelationship",
  "label": "is-a",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Terminology",
      "domainType": "Terminology",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "FT01: Functional Test 01",
      "domainType": "Term"
    }
  ],
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "relationshipType": {
    "id": "${json-unit.matches:id}",
    "domainType": "TermRelationshipType",
    "label": "is-a",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "displayLabel": "Is A"
  },
  "sourceTerm": {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "FT01: Functional Test 01",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "code": "FT01",
    "definition": "Functional Test 01"
  },
  "targetTerm": {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "FT02: Functional Test 02",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "code": "FT02",
    "definition": "Functional Test 02"
  }
}
'''
    }

    void 'test finding only relationships where the term is the source'() {
        given:
        String id = createNewItem(validJson)

        when: 'The index action is requested'
        GET('?type=source', STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse HttpStatus.OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "FT01: Functional Test 01",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "FT01: Functional Test 01",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "FT01",
        "definition": "Functional Test 01"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "FT02: Functional Test 02",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "FT02",
        "definition": "Functional Test 02"
      }
    }
  ]
}'''
        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test finding only relationships where the term is the target'() {
        given:
        String id = createNewItem(validJson)

        when: 'The index action is requested'
        GET('?type=target', STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse HttpStatus.OK, '''{
  "count": 0,
  "items": []
}'''
    }

    void 'test index through term relationship types'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships", STRING_ARG, true)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Functional Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "FT01: Functional Test 01",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "FT01: Functional Test 01",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "FT01",
        "definition": "Functional Test 01"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "FT02: Functional Test 02",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Functional Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "FT02",
        "definition": "Functional Test 02"
      }
    }
  ]
}
'''

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT

    }

    void 'test show through term relationship type'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("terminologies/${terminologyId}/termRelationshipTypes/${termRelationshipTypeId}/termRelationships/$id", STRING_ARG, true)

        then:
        verifyJsonResponse HttpStatus.OK, getExpectedShowJson()

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
    }
}