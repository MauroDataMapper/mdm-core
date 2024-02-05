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
package uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service


import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.SelfType

/**
 * @since 09/04/2021
 */
@SelfType(MultiFacetAwareService)
trait SummaryMetadataAwareService {

    abstract SummaryMetadataService getSummaryMetadataService()

    void removeSummaryMetadataFromMultiFacetAware(UUID multiFacetAwareId, SummaryMetadata summaryMetadata) {
        removeFacetFromDomain(multiFacetAwareId, summaryMetadata.id, 'summaryMetadata')
    }

    CopyInformation cacheSummaryMetadataInformationForCopy(List<UUID> originalIds, CopyInformation cachedInformation) {
        if (!cachedInformation || !originalIds || originalIds.size() == 1) return cachedInformation
        List<SummaryMetadata> md = SummaryMetadata
            .byMultiFacetAwareItemIdInList(originalIds)
            .join('summaryMetadataReports')
            .list()
        cachedInformation.preloadedFacets.summaryMetadata = new TreeMap(md.groupBy { it.multiFacetAwareItemId })
        cachedInformation
    }

    /**
     * Copy the summary metadata and summary metadata reports from original to copy
     * @param original
     * @param copy
     * @param copier
     * @return
     */
    SummaryMetadataAware copySummaryMetadataFromOriginal(SummaryMetadataAware original, SummaryMetadataAware copy, User copier,
                                                         CopyInformation copyInformation) {
        // Allow facets to be preloaded from the db and passed in via the copy information
        // Facets loaded in this way could be more than just those belonging to the item being copied so we need to extract only those relevant
        List<SummaryMetadata> summaryMetadataList
        if (copyInformation) {
            summaryMetadataList = copyInformation.hasFacetData('summaryMetadata') ?
                                  copyInformation.extractPreloadedFacetsForTypeAndId(SummaryMetadata, 'summaryMetadata', original.id) :
                                  summaryMetadataService.findAllByMultiFacetAwareItemId(original.id)
        } else {
            summaryMetadataList = summaryMetadataService.findAllByMultiFacetAwareItemId(original.id)
        }
        summaryMetadataList.each { sm ->
            SummaryMetadata summaryMetadata = new SummaryMetadata(label: sm.label,
                                                                  description: sm.description,
                                                                  summaryMetadataType: sm.summaryMetadataType,
                                                                  createdBy: copier.emailAddress)

            sm.summaryMetadataReports.each { smr ->
                summaryMetadata.addToSummaryMetadataReports(reportDate: smr.reportDate,
                                                            reportValue: smr.reportValue,
                                                            createdBy: copier.emailAddress
                )
            }
            copy.addToSummaryMetadata(summaryMetadata)
        }

        copy
    }
}
