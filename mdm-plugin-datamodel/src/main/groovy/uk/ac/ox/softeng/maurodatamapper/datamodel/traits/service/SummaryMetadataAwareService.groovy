/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

    /**
     * Copy the summary metadata and summary metadata reports from original to copy
     * @param original
     * @param copy
     * @param copier
     * @return
     */
    SummaryMetadataAware copySummaryMetadataFromOriginal(SummaryMetadataAware original, SummaryMetadataAware copy, User copier) {
        summaryMetadataService.findAllByMultiFacetAwareItemId(original.id).each {sm ->
            SummaryMetadata summaryMetadata = new SummaryMetadata(label: sm.label,
                description: sm.description,
                summaryMetadataType: sm.summaryMetadataType,
                createdBy: copier.emailAddress)

            sm.summaryMetadataReports.each {smr ->
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
