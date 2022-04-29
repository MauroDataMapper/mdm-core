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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController* Controller: metadata
 *  | GET    | /api/metadata/namespaces/${id}? | Action: namespaces  |
 */
@Integration
@Transactional
@Slf4j
class MetadataFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'metadata'
    }

    void 'test getting all metadata namespaces'() {

        when:
        GET('namespaces', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [

    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [

    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter"
  }
]'''
    }

    void 'test getting metadata namespaces for namespace'() {
        when: 'testing unknown namespace'
        GET('namespaces/functional.test.unknown.namespace', STRING_ARG)

        then:
        verifyJsonResponse OK, '[]'

        when: 'testing known namespace'
        GET('namespaces/uk.ac.ox.softeng.maurodatamapper.core.provider.email', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [

    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email"
  }
]'''
    }
}
