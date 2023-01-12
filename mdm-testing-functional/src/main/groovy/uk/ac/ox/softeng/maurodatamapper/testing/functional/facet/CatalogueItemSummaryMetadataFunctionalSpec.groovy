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

import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 * Controller: summaryMetadata
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata        | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataController
 */
@Slf4j
abstract class CatalogueItemSummaryMetadataFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/summaryMetadata"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}"
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .withoutAvailableActions()
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Summary Metadata:Functional Test Summary Metadata] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Summary Metadata:.+?] changed properties \[path, label]/
    }

    @Override
    Map getValidJson() {
        [
            label              : 'Functional Test Summary Metadata',
            summaryMetadataType: 'NUMBER'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            summaryMetadataType: 'object'
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "createdBy": "creator@test.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "label": "Functional Test Summary Metadata",
  "summaryMetadataType": "NUMBER"
}'''
    }

    Map getSummaryMetadataReportJson() {
        [
            name                  : 'Sex Example',
            description           : 'Value Distribution',
            label                 : 'Functional Test Summary Metadata',
            summaryMetadataType   : 'MAP',
            summaryMetadataReports: [
                [
                    reportDate : '2020-01-29T07:31:57.519Z',
                    reportValue: '{\"Female\":20562,\"Male\":17407,\"Unknown\":604}'
                ],
                [
                    reportDate : '2020-02-29T07:31:57.519Z',
                    reportValue: '{\"Female\":19562,\"Male\":18407,\"Unknown\":704}'
                ],
                [
                    reportDate : '2020-03-29T07:31:57.519Z',
                    reportValue: '{\"Female\":18562,\"Male\":19407,\"Unknown\":804}'
                ],
                [
                    reportDate : '2020-04-29T07:31:57.519Z',
                    reportValue: '{\"Female\":17562,\"Male\":20407,\"Unknown\":904}'
                ]
            ]
        ]
    }

    void 'CISM01: test including summaryMetadataReport in summaryMetadata'() {
        when:
        loginEditor()
        POST(savePath, summaryMetadataReportJson, MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        def id = response.body().id

        when:
        GET("${savePath}/${id}", STRING_ARG, true)

        then:
        verifyJsonResponse(HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "createdBy": "editor@test.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "label": "Functional Test Summary Metadata",
  "description": "Value Distribution",
  "summaryMetadataType": "MAP"
}''')
        when:
        GET("${savePath}/${id}/summaryMetadataReports", MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 4
        assert response.body().items.size() == 4

        cleanup:
        removeValidIdObject(id)
    }
}