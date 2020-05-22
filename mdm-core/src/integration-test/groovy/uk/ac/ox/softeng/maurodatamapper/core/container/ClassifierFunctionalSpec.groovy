/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierController* Controller: classifier
 *  | POST   | /api/classifiers       | Action: save   |
 *  | GET    | /api/classifiers       | Action: index  |
 *  | DELETE | /api/classifiers/${id} | Action: delete |
 *  | PUT    | /api/classifiers/${id} | Action: update |
 *  | GET    | /api/classifiers/${id} | Action: show   |
 *
 */
@Integration
@Slf4j
class ClassifierFunctionalSpec extends ResourceFunctionalSpec<Classifier> {

    @Override
    String getResourcePath() {
        'classifiers'
    }

    Map getValidJson() {
        [label: 'Functional Testing Classifier']
    }

    Map getInvalidJson() {
        [label: null]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "availableActions": ["delete", "show", "update"],
  "id": "${json-unit.matches:id}",
  "label": "Functional Testing Classifier"
}'''
    }
}
