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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.time.OffsetDateTime

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceSummaryMetadataReport implements CreatorAware {

    UUID id
    OffsetDateTime reportDate
    String reportValue
    ReferenceSummaryMetadata summaryMetadata

    static belongsTo = [
            ReferenceSummaryMetadata
    ]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        reportValue blank: false
    }

    static mapping = {
        summaryMetadata index: 'summary_metadata_report_summary_metadata_idx'
        reportValue type: 'text'
    }

    @Override
    String getDomainType() {
        ReferenceSummaryMetadata.simpleName
    }

    String getEditLabel() {
        "Summary Metadata Report:${reportDate}"
    }

    static DetachedCriteria<ReferenceSummaryMetadataReport> bySummaryMetadataId(Serializable summaryMetadataId) {
        new DetachedCriteria<ReferenceSummaryMetadataReport>(ReferenceSummaryMetadataReport).
            eq('summaryMetadata.id', Utils.toUuid(summaryMetadataId)).join('summaryMetadata')
    }

    static DetachedCriteria<ReferenceSummaryMetadataReport> bySummaryMetadataIdAndId(Serializable summaryMetadataId, Serializable resourceId) {
        bySummaryMetadataId(summaryMetadataId).idEq(Utils.toUuid(resourceId))
    }
}