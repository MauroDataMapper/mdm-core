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
package uk.ac.ox.softeng.maurodatamapper.referencedata.test.functional

import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemFacetFunctionalSpec

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.temporal.ChronoField

/**
 * <pre>
 *  Controller: summaryMetadata
 *  | POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports | Action: save
 *  | GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports      | Action: index
 *  | DELETE | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: delete
 *  | GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReportController
 */
@Slf4j
abstract class CatalogueItemReferenceSummaryMetadataReportFunctionalSpec extends CatalogueItemFacetFunctionalSpec<ReferenceSummaryMetadataReport> {

    // Need to make sure this never gets set to anything which is less than 3 digits when formatted as then the test fails
    static final OffsetDateTime dateTime = OffsetDateTime.now().with(ChronoField.MILLI_OF_SECOND, 414)
    static final OffsetDateTimeConverter offsetDateTimeConverter = new OffsetDateTimeConverter()

    abstract String getSourceDataModelId()

    abstract String getDestinationDataModelId()

    @Shared
    ReferenceSummaryMetadata summaryMetadata

    @Override
    String getFacetResourcePath() {
        "referenceSummaryMetadata/${summaryMetadata.id}/summaryMetadataReports"
    }

    String getCatalogueItemCopyPath() {
        "referenceDataModels/${destinationDataModelId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}/${catalogueItemId}"
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

    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        verifyResponse(HttpStatus.CREATED, response)
    }

    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        String copyId = response.body().id
        GET("${catalogueItemDomainResourcePath}/${copyId}/referenceSummaryMetadata", MAP_ARG, true)
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
}