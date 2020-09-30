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

import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.time.OffsetDateTime

@Resource(readOnly = false, formats = ['json', 'xml'])
class SummaryMetadataReport implements CreatorAware {

    UUID id
    OffsetDateTime reportDate
    String reportValue
    SummaryMetadata summaryMetadata

    static belongsTo = [
        SummaryMetadata
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
        SummaryMetadata.simpleName
    }

    String getEditLabel() {
        "Summary Metadata Report:${reportDate}"
    }

    static DetachedCriteria<SummaryMetadataReport> bySummaryMetadataId(Serializable summaryMetadataId) {
        new DetachedCriteria<SummaryMetadataReport>(SummaryMetadataReport).
            eq('summaryMetadata.id', Utils.toUuid(summaryMetadataId)).join('summaryMetadata')
    }

    static DetachedCriteria<SummaryMetadataReport> bySummaryMetadataIdAndId(Serializable summaryMetadataId, Serializable resourceId) {
        bySummaryMetadataId(summaryMetadataId).idEq(Utils.toUuid(resourceId))
    }
}