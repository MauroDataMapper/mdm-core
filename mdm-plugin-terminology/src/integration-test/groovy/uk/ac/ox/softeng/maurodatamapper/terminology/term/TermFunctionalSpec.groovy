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
package uk.ac.ox.softeng.maurodatamapper.terminology.term


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.METHOD_NOT_ALLOWED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: term
 *  |   POST   | /api/terminologies/${terminologyId}/terms                                                                  | Action: save
 *  |   GET    | /api/terminologies/${terminologyId}/terms                                                                  | Action: index
 *  |  DELETE  | /api/terminologies/${terminologyId}/terms/${id}                                                            | Action: delete
 *  |   PUT    | /api/terminologies/${terminologyId}/terms/${id}                                                            | Action: update
 *  |   GET    | /api/terminologies/${terminologyId}/terms/${id}                                                            | Action: show
 *
 *  |   GET    | /api/codeSets/${codeSetId}/terms                                                                           | Action: index
 *
 *  |   GET    | /api/terminologies/${terminologyId}/terms/tree/${termId}?                                                  | Action: tree
 *
 *  |   GET    | /api/terminologies/${terminologyId}/terms/search                                                           | Action: search
 *  |   POST   | /api/terminologies/${terminologyId}/terms/search                                                           | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.TermController
 */
@Slf4j
@Integration
class TermFunctionalSpec extends ResourceFunctionalSpec<Term> {

    TerminologyService terminologyService

    @Shared
    UUID terminologyId

    @Shared
    UUID complexTerminologyId

    @Shared
    UUID simpleTerminologyId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Terminology terminology = new Terminology(label: 'Functional Test Terminology', createdBy: FUNCTIONAL_TEST,
                                                  folder: folder, authority: testAuthority).save(flush: true)
        terminologyId = terminology.id

        complexTerminologyId = BootstrapModels.buildAndSaveComplexTerminology(messageSource, folder, terminologyService, testAuthority).id
        simpleTerminologyId = BootstrapModels.buildAndSaveSimpleTerminology(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec TermFunctionalSpec')
        cleanUpResources(TermRelationship, Term, TermRelationshipType, Terminology, Folder)
    }

    @Override
    String getResourcePath() {
        "terminologies/${terminologyId}/terms"
    }

    @Override
    Map getValidJson() {
        [
            code      : 'FT01',
            definition: 'Functional Test 01'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            code      : 'FT01',
            definition: null
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Adding a description to the Term'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
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
  "availableActions": ["delete","show","update"],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "code": "FT01",
  "definition": "Functional Test 01"
}'''
    }

    @Transactional
    void 'T01 : test getting the tree for empty term'() {
        given:
        def id = Term.findByCode('CTT21').id

        when:
        GET("terminologies/${complexTerminologyId}/terms/tree/$id", MAP_ARG, true)

        then:
        verifyResponse OK, response
        !response.body()
    }

    void 'T02 : test getting the tree for unknown term'() {
        when:
        GET("terminologies/${complexTerminologyId}/terms/tree/${UUID.randomUUID().toString()}", MAP_ARG, true)

        then:
        verifyResponse NOT_FOUND, response
    }

    void 'T03 : test getting the tree for non-tree term'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("tree/$id")

        then:
        verifyResponse METHOD_NOT_ALLOWED, response
        response.body().message == 'Terminology does not support tree view'
        response.body().path == "/api/terminologies/${terminologyId}/terms/tree/${id}".toString()

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == NO_CONTENT
    }

    @Transactional
    void 'T04 : test getting the tree for tree terminology term'() {
        given:
        def id = Term.findByCode('CTT20').id

        when:
        GET("terminologies/${complexTerminologyId}/terms/tree/$id", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT21: Complex Test Term 21"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT22: Complex Test Term 22"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT23: Complex Test Term 23"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT24: Complex Test Term 24"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT25: Complex Test Term 25"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT26: Complex Test Term 26"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT27: Complex Test Term 27"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT28: Complex Test Term 28"
  },
  {
    "domainType": "Term",
    "modelId": "${json-unit.matches:id}",
    "hasChildren": false,
    "availableActions": [],
    "id": "${json-unit.matches:id}",
    "label": "CTT29: Complex Test Term 29"
  }
]'''
    }

    void 'T06 : test getting the tree for empty terminology'() {
        when:
        GET("/tree")

        then:
        verifyResponse METHOD_NOT_ALLOWED, response
        response.body().message == 'Terminology does not support tree view'
        response.body().path == "/api/terminologies/${terminologyId}/terms/tree".toString()
    }

    void 'T07 : test getting the tree for non-tree terminology'() {

        when:
        GET("terminologies/${simpleTerminologyId}/terms/tree", MAP_ARG, true)

        then:
        verifyResponse METHOD_NOT_ALLOWED, response
        response.body().message == 'Terminology does not support tree view'
        response.body().path == "/api/terminologies/${simpleTerminologyId}/terms/tree".toString()
    }

    void 'T08 : test getting the tree for tree terminology'() {

        when:
        GET("terminologies/${complexTerminologyId}/terms/tree", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT100: Complex Test Term 100",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT101",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT00: Complex Test Term 00",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]'''
    }

    @Transactional
    void 'T09 : test getting the tree for term with truncated label'() {
        given:
        def id = Term.findByCode('CTT101').id

        when:
        GET("terminologies/${complexTerminologyId}/terms/$id", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT101",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
        {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
        }
    ],
    "availableActions": ["delete","show","update"],
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "code": "CTT101",
    "definition": "CTT101",
    "description": "Example of truncated term label when code and definition are the same"
}'''
    }

    void 'S01 : Test searching terms'() {

        when:
        GET("terminologies/${complexTerminologyId}/terms/search?searchTerm=1*", STRING_ARG, true)

        then: 'The response is OK'
        verifyJsonResponse OK, '''{
  "count": 13,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT1: Complex Test Term 1",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "code": "CTT1",
      "definition": "Complex Test Term 1"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT10: Complex Test Term 10",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "code": "CTT10",
      "definition": "Complex Test Term 10"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT100: Complex Test Term 100",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "code": "CTT100",
      "definition": "Complex Test Term 100"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT101",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ],
      "code": "CTT101",
      "definition": "CTT101",
      "description": "Example of truncated term label when code and definition are the same"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT11: Complex Test Term 11",
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT12: Complex Test Term 12",
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT13: Complex Test Term 13",
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT14: Complex Test Term 14",
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT15: Complex Test Term 15",
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
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "CTT16: Complex Test Term 16",
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
  ]
}'''
    }
}
