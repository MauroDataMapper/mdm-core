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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

/**
 * <pre>
 * Controller: classifier
 *  |  POST    | /api/classifiers/${classifierId}/classifiers        | Action: save
 *  |  GET     | /api/classifiers/${classifierId}/classifiers        | Action: index
 *  |  DELETE  | /api/classifiers/${classifierId}/classifiers/${id}  | Action: delete
 *  |  PUT     | /api/classifiers/${classifierId}/classifiers/${id}  | Action: update
 *  |  GET     | /api/classifiers/${classifierId}/classifiers/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierController
 */
@Integration
@Slf4j
class NestedClassifierFunctionalSpec extends UserAccessFunctionalSpec {

    @Transactional
    String getTestClassifierId() {
        Classifier.findByLabel('Functional Test Classifier').id.toString()
    }

    @Override
    String getResourcePath() {
        "classifiers/${getTestClassifierId()}/classifiers"
    }

    @Override
    String getEditsPath() {
        'classifiers'
    }

    Map getValidJson() {
        [
            label: 'Nested Functional Test Classifier',
        ]
    }

    Map getInvalidJson() {
        [
            label: null
        ]
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereEditorsCannotChangePermissions()
            .whereEditors {
                cannotCreate()
                cannotDelete()
                cannotEditDescription()
                cannotUpdate()
            }
            .whereAuthors {
                cannotEditDescription()
                cannotUpdate()
            }
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'softDelete', 'update')
            .whereEditorsCanAction('show')
    }

    Pattern getExpectedCreatedEditRegex() {
        ~/\[Classifier:Nested Functional Test Classifier] added as child of \[Classifier:Functional Test Classifier]/
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0,"items": []}'
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "id": "${json-unit.matches:id}",
  "domainType": "Classifier",
  "label": "Nested Functional Test Classifier",
  "path": "cl:Functional Test Classifier|cl:Nested Functional Test Classifier",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["show"]
}'''
    }
}