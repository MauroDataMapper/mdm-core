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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test.functional

import io.micronaut.http.HttpStatus
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemFacetFunctionalSpec

import groovy.util.logging.Slf4j

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
                    reportValue : '{\"Female\":20562,\"Male\":17407,\"Unknown\":604}'
                ],
                [
                    reportDate : '2020-02-29T07:31:57.519Z',
                    reportValue : '{\"Female\":19562,\"Male\":18407,\"Unknown\":704}'
                ],
                [
                    reportDate : '2020-03-29T07:31:57.519Z',
                    reportValue : '{\"Female\":18562,\"Male\":19407,\"Unknown\":804}'
                ],
                [
                    reportDate : '2020-04-29T07:31:57.519Z',
                    reportValue : '{\"Female\":17562,\"Male\":20407,\"Unknown\":904}'
                ]
            ]
        ]
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
}