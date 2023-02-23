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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.search

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels as DataModelBootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: search
 *  |   GET    | /api/catalogueItems/search  | Action: search
 *  |   POST   | /api/catalogueItems/search  | Action: search
 * </pre>
 *
 * In Core there will be nothing to search but we want to check we can at least run the endpoint on an empty system
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.search.SearchController
 */
@Integration
@Slf4j
class SearchFunctionalSpec extends FunctionalSpec {

    AuthorityService authorityService

    @Override
    String getResourcePath() {
        'catalogueItems/search'
    }

    void 'L01 : test searching for "qwerty" using GET (not logged in)'() {
        given:
        def term = 'qwerty'

        when:
        GET("?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'L02 : test searching for "qwerty" using POST (not logged in)'() {
        given:
        def term = 'qwerty'

        when:
        POST('', [searchTerm: term, sort: 'label'])

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'N01 : test searching for "qwerty" using GET (no access/authenticated)'() {
        given:
        def term = 'qwerty'

        when:
        loginAuthenticated()
        GET("?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'N02 : test searching for "qwerty" using POST (no access/authenticated)'() {
        given:
        def term = 'qwerty'

        when:
        loginAuthenticated()
        POST('', [searchTerm: term, sort: 'label'])

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'R01 : test searching for "qwerty" using GET (as reader)'() {
        given:
        def term = 'qwerty'

        when:
        loginReader()
        GET("?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'R02 : test searching for "qwerty" using POST (as reader)'() {
        given:
        def term = 'qwerty'

        when:
        loginReader()
        POST('', [searchTerm: term, sort: 'label'])

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'R03 : test searching for "simple"'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        GET("?searchTerm=${term}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT01: Simple Test Term 01",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT02: Simple Test Term 02",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Simple Test Terminology"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    void 'R04 : test searching for "simple" using POST'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm: term, sort: 'label'], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT01: Simple Test Term 01",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Term",
      "label": "STT02: Simple Test Term 02",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Simple Test Terminology"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    void 'S05 : test searching for "simple" limited to DataModel'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm: term, sort: 'label', domainTypes: ['DataModel']])

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().label == BootstrapModels.SIMPLE_DATAMODEL_NAME
        responseBody().items.first().domainType == 'DataModel'
    }

    void 'S05b : test searching for "simple" limited to DataModel types'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm: term, sort: 'label', domainTypes: ['DataModel'], dataModelTypes: ['Data Standard']])

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().label == BootstrapModels.SIMPLE_DATAMODEL_NAME
        responseBody().items.first().domainType == 'DataModel'

        loginReader()
        POST('', [searchTerm: term, sort: 'label', domainTypes: ['DataModel'], dataModelTypes: ['Data Asset']])

        then:
        verifyResponse OK, response
        responseBody().count == 0
    }

    void 'S06 : test searching for "simple" using POST with pagination'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm: term, sort: 'label', max: 2, offset: 0], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    void 'S07 : test searching for "simple" using POST with pagination and offset'() {
        given:
        String term = 'simple'

        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm: term, sort: 'label', max: 2, offset: 2], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 7,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel"
    }
  ]
}'''
    }

    void 'S08 : test searching for "simple" using POST with pagination and classifier'() {
        given:
        String term = 'simple'
        Map ids = buildSearchData()


        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm : term,
                  classifiers: ['Environment A'],
                  sort       : 'label', max: 15], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "test simple A"
    }
  ]
}'''
        cleanup:
        loginAdmin()
        DELETE("dataModels/${ids.dm1Id}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/${ids.dm2Id}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        DELETE("classifiers/${ids.envA}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        DELETE("classifiers/${ids.envB}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
    }

    @Transactional
    Map<String, String> buildSearchData() {
        Map m = [:]
        Folder folder = Folder.findByLabel('Functional Test Folder')
        Authority authority = authorityService.getDefaultAuthority()
        m.dm1Id = DataModelBootstrapModels.buildAndSaveSimpleDataModelWithClassifier(messageSource, folder, 'test simple A', 'Environment A', authority).id.toString()
        m.dm2Id = DataModelBootstrapModels.buildAndSaveSimpleDataModelWithClassifier(messageSource, folder, 'test simple B', 'Environment B', authority).id.toString()

        m.envA = Classifier.byLabel('Environment A').get().id.toString()
        m.envB = Classifier.byLabel('Environment B').get().id.toString()
        m
    }
}
