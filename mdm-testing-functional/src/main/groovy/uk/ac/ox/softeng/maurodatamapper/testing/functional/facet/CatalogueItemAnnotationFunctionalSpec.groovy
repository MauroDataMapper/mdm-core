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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet

import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 *  Controller: annotation
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: delete
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Transactional
@Slf4j
abstract class CatalogueItemAnnotationFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/annotations"
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .withoutAvailableActions()
            .whereTestingUnsecuredResource()
            .whereAuthors {
                canCreate()
                canSee()
                canIndex()
            }
            .whereReviewers {
                canCreate()
                canSee()
                canIndex()
            }
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test Annotation'
        ]
    }

    @Override
    Map getInvalidJson() {
        [:]
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0,"items": []}'
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "createdBy": "creator@test.com",
  "createdByUser": {
    "name": "creator User",
    "id": "${json-unit.matches:id}"
  },
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Annotation"
}'''
    }
}