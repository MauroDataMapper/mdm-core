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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test.functional

import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemFacetFunctionalSpec

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 * <pre>
 *  Controller: summaryMetadata
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata          | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata          | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}    | Action: delete                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}    | Action: show                                 |
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataController
 */
@Slf4j
abstract class CatalogueItemSummaryMetadataFunctionalSpec extends CatalogueItemFacetFunctionalSpec<SummaryMetadata> {

    abstract String getSourceDataModelId()

    abstract String getDestinationDataModelId()

    String getCatalogueItemCopyPath() {
        "dataModels/${destinationDataModelId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}/${catalogueItemId}"
    }

    @Override
    String getFacetResourcePath() {
        'summaryMetadata'
    }

    @Override
    Map getValidJson() {
        [
            label              : 'Some interesting summary',
            summaryMetadataType: SummaryMetadataType.NUMBER
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            summaryMetadataType: 'object'
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
  "id": "${json-unit.matches:id}",
  "createdBy": "unlogged_user@mdm-core.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "label": "Some interesting summary",
  "summaryMetadataType": "NUMBER"
}'''
    }

    Map getSummaryMetadataReportJson() {
        [
            name                  : 'Sex Example',
            description           : 'Value Distribution',
            label                 : 'Some interesting summary',
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

    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        verifyResponse(HttpStatus.CREATED, response)
    }

    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        String copyId = response.body().id
        GET(getCopyResourcePath(copyId), MAP_ARG, true)
    }

    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        verifyResponse(HttpStatus.OK, response)
        // summary metadata should not be copied for DC/DT/DE
        assert response.body().count == 0
        assert response.body().items.size() == 0
    }

    void 'CIF01 : Test facet copied with catalogue item'() {
        given: 'Create new facet on catalogue item'
        def id = createNewItem(validJson)

        when: 'Copy catalogue item'
        POST(catalogueItemCopyPath, [:], MAP_ARG, true)

        then: 'Check successful copy'
        verifyCIF01SuccessfulCatalogueItemCopy(response)

        when: 'Retrieve the facets on the newly copied catalogue item'
        requestCIF01CopiedCatalogueItemFacet(response)

        then: 'Check our recent new facet was copied with the catalogue item'
        verifyCIF01CopiedFacetSuccessfully(response)

        cleanup: 'Remove facet from source catalogue item'
        cleanUpData(id)
    }

    void 'CISM01: test including summaryMetadataReport in summaryMetadata'() {
        when:
        def id = createNewItem(summaryMetadataReportJson)
        GET("${savePath}/${id}", STRING_ARG, true)

        then:
        verifyJsonResponse(HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "createdBy": "unlogged_user@mdm-core.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "label": "Some interesting summary",
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
        cleanUpData(id)
    }

    void 'CISM02: test filtering of summaryMetadata'() {
        given: 'Create new summaryMetadata items'
        def id1 = createNewItem([
            label              : 'Summary the first',
            summaryMetadataType: SummaryMetadataType.NUMBER
        ])
        def id2 = createNewItem([
            label              : 'Summary the second',
            summaryMetadataType: SummaryMetadataType.NUMBER
        ])

        when: 'No filter used'
        GET('')

        then: 'Check all summaryMetadata returned'
        verifyResponse(HttpStatus.OK, response)
        response.body().count == 2

        when: 'Filter by label'
        GET('?label=sec')

        then: 'Check filtered summaryMetadata returned'
        verifyResponse(HttpStatus.OK, response)
        response.body().count == 1
        response.body().items[0].id == id2

        cleanup:
        cleanUpData(id1)
        cleanUpData(id2)
    }

    void 'CISM03: test sorting of summaryMetadata'() {
        given: 'Create new summaryMetadata items'
        def id1 = createNewItem([
            label              : 'A summaryMetadata',
            summaryMetadataType: SummaryMetadataType.NUMBER
        ])
        def id2 = createNewItem([
            label              : 'B summaryMetadata',
            summaryMetadataType: SummaryMetadataType.NUMBER
        ])

        when: 'Default sorting by label'
        GET('')

        then: 'Check summaryMetadata returned in ascending label order'
        verifyResponse(HttpStatus.OK, response)
        response.body().count == 2
        response.body().items[0].id == id1
        response.body().items[1].id == id2

        when: 'Sort by label descending'
        GET('?sort=label&order=desc')

        then: 'Check summaryMetadata returned in descending label order'
        verifyResponse(HttpStatus.OK, response)
        response.body().count == 2
        response.body().items[0].id == id2
        response.body().items[1].id == id1

        cleanup:
        cleanUpData(id1)
        cleanUpData(id2)
    }
}