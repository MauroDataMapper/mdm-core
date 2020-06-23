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

import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemFacetFunctionalSpec

import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.time.OffsetDateTime

/**
 * <pre>
 *  Controller: summaryMetadata
 *  | POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports | Action: save
 *  | GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports      | Action: index
 *  | DELETE | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: delete
 *  | GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReportController
 */
@Slf4j
abstract class CatalogueItemSummaryMetadataReportFunctionalSpec extends CatalogueItemFacetFunctionalSpec<SummaryMetadataReport> {

    static final OffsetDateTime dateTime = OffsetDateTime.now()
    static final OffsetDateTimeConverter offsetDateTimeConverter = new OffsetDateTimeConverter()

    @Shared
    SummaryMetadata summaryMetadata

    @Override
    String getFacetResourcePath() {
        "summaryMetadata/${summaryMetadata.id}/summaryMetadataReports"
    }

    @Override
    Map getValidJson() {
        [
            reportValue: 'Some interesting report',
            reportDate : offsetDateTimeConverter.toString(dateTime)
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            reportDate: null
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            reportDate: offsetDateTimeConverter.toString(dateTime.plusDays(1))
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "reportDate": "${json-unit.matches:offsetDateTime}",
  "reportValue": "Some interesting report"
}'''
    }
}