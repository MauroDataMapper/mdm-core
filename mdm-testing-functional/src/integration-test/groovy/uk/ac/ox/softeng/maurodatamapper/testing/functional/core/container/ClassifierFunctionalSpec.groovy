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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container


import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: classifier
 *  |  POST    | /api/classifiers        | Action: save
 *  |  GET     | /api/classifiers        | Action: index
 *  |  DELETE  | /api/classifiers/${id}  | Action: delete
 *  |  PUT     | /api/classifiers/${id}  | Action: update
 *  |  GET     | /api/classifiers/${id}  | Action: show
 *  |  DELETE  | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  PUT     | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/classifiers/${classifierId}/readByEveryone       | Action: readByEveryone
 *  |  PUT     | /api/classifiers/${classifierId}/readByEveryone       | Action: readByEveryone
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierController
 */
@Integration
@Slf4j
class ClassifierFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

    String getResourcePath() {
        'classifiers'
    }

    Map getValidJson() {
        [
            label: 'Functional Test Classifier 2',
        ]
    }

    Map getInvalidJson() {
        [
            label: 'Functional Test Classifier'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Just something for testing'
        ]
    }

    Pattern getExpectedCreatedEditRegex() {
        ~/\[Classifier:Functional Test Classifier 2] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Classifier:Functional Test Classifier 2] changed properties \[description]/
    }

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'update', 'save', 'softDelete', 'delete']
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
    }

    @Override
    Boolean getReaderCanCreate() {
        true
    }

    @Override
    Boolean getAuthenticatedUsersCanCreate() {
        true
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert response.body().count == 0
    }

    @Override
    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
        response.body().items.size() == 0
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Classifier",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}'''
    }

    @Override
    String getAdminIndexJson() {
        getEditorIndexJson()
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Classifier 2",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["show","update","save","softDelete","delete"]
}'''
    }
}