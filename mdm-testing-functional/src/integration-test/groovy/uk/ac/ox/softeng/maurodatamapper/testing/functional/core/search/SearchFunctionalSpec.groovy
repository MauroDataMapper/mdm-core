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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.search

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels as DataModelBootstrapModels

import io.micronaut.http.HttpStatus

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
      "label": "Second Simple Reference Data Model"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceDataModel",
      "label": "Simple Reference Data Model"
    }
  ]
}'''
    }

    void 'S08 : test searching for "simple" using POST with pagination and classifier'() {
        given:
        String term = 'simple'
        def dm1Id, dm2Id

        Folder.withNewTransaction {
            Folder folder = Folder.findByLabel('Functional Test Folder')
            Authority authority = authorityService.getDefaultAuthority()
            dm1Id = DataModelBootstrapModels.buildAndSaveSimpleDataModelWithClassifier(messageSource, folder, 'test simple A', 'Environment A', authority).id
            dm2Id = DataModelBootstrapModels.buildAndSaveSimpleDataModelWithClassifier(messageSource, folder, 'test simple B', 'Environment B', authority).id
        }

        when: 'logged in as reader user'
        loginReader()
        POST('', [searchTerm: term,
                  classifiers: ['Environment A'],
                  sort: 'label', max: 15], STRING_ARG)

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
        DELETE("/api/dataModels/${dm1Id}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("/api/dataModels/${dm2Id}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

}
