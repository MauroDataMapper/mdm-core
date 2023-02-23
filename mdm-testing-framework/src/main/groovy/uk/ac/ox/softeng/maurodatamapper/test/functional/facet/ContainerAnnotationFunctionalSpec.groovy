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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet


import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * Where facet owner is a Container
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController* Controller: annotation
 |   POST   | /api/${containerDomainType}/${containerId}/annotations          | Action: save                                 |
 |   GET    | /api/${containerDomainType}/${containerId}/annotations          | Action: index                                |
 |  DELETE  | /api/${containerDomainType}/${containerId}/annotations/${id}    | Action: delete                               |
 |   GET    | /api/${containerDomainType}/${containerId}/annotations/${id}    | Action: show                                 |
 */
@Slf4j
abstract class ContainerAnnotationFunctionalSpec extends ContainerFacetFunctionalSpec<Annotation> {
    @Override
    String getFacetResourcePath() {
        'annotations'
    }

    @Override
    Map getValidJson() {
        [
                label      : 'Some interesting comment',
                description: 'Why are we writing these tests?'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
                description: 'A new annotation'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
                description: 'Attempting update'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "createdBy": "unlogged_user@mdm-core.com",
  "createdByUser": {
    "name": "Anonymous User",
    "id": "${json-unit.matches:id}"
  },
  "description": "Why are we writing these tests?",
  "id": "${json-unit.matches:id}",
  "label": "Some interesting comment"
}'''
    }

    @Override
    void verifyR4InvalidUpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }

    @Override
    void verifyR4UpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }
}
