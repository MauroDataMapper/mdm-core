/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 *  Controller: annotation
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}  | Action: delete
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${annotationId}/annotations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Integration
@Slf4j
class NestedAnnotationFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    String getResourcePath() {
        "dataModels/${complexDataModelId}/annotations/${getParentCommentAnnotationId()}/annotations"
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getParentCommentAnnotationId() {
        Annotation.findByLabel('test annotation 1').id.toString()
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .withoutAvailableActions()
            .whereTestingUnsecuredResource()
            .whereReviewers {
                canCreate()
                canSee()
                canIndex()
            }
            .whereAuthors {
                canCreate()
                canSee()
                canIndex()
            }
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    Map getValidJson() {
        [
            description: 'Nested Functional Test Annotation'
        ]
    }

    Map getInvalidJson() {
        [:]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}
'''
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
  "description": "Nested Functional Test Annotation",
  "label": "test annotation 1 [1]"
}'''
    }
}