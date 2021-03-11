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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.facet

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK


/**
 * <pre>
 * Controller: metadata
 *  |  GET     | /api/metadata/namespaces/${id}?  | Action: namespaces
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
 */
@Integration
@Slf4j
class MetadataFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        'metadata'
    }

    void 'test getting all metadata namespaces (as authenticated user)'() {
        given:
        loginAuthenticated()

        when:
        GET('namespaces', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1",
      "mdk2",
      "mdk3"
    ],
    "namespace": "referencedata.com"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk2"
    ],
    "namespace": "terminology.test.com"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1",
      "mdk2"
    ],
    "namespace": "terminology.test.com/simple"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1",
      "mdk2"
    ],
    "namespace": "test.com"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1",
      "mdk2"
    ],
    "namespace": "test.com/simple"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1"
    ],
    "namespace": "test.com/test"
  },
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
    "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter"
  },
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer"
  }
]'''
    }

    void 'test getting metadata namespaces for namespace (as authenticated user)'() {
        given:
        loginAuthenticated()

        when: 'testing unknown namespace'
        GET('namespaces/funtional.test.unknown.namespace')

        then:
        verifyResponse OK, response
        !response.body()

        when: 'testing known namespace'
        GET('namespaces/test.com', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1",
      "mdk2"
    ],
    "namespace": "test.com"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1",
      "mdk2"
    ],
    "namespace": "test.com/simple"
  },
  {
    "defaultNamespace": false,
    "editable": true,
    "keys": [
      "mdk1"
    ],
    "namespace": "test.com/test"
  }
]'''
    }

}