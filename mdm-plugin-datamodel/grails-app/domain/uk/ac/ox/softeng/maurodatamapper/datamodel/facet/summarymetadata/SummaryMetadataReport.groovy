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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Resource(readOnly = false, formats = ['json', 'xml'])
class SummaryMetadataReport implements MdmDomain {

    static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern('yyyyMMddHHmmssSSSSSSX')

    UUID id
    OffsetDateTime reportDate
    String reportValue
    SummaryMetadata summaryMetadata

    static belongsTo = [
        SummaryMetadata
    ]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        reportValue blank: false
    }

    static mapping = {
        summaryMetadata index: 'summary_metadata_report_summary_metadata_idx'
        reportValue type: 'text'
    }

    @Override
    String getDomainType() {
        SummaryMetadataReport.simpleName
    }

    @Override
    String getPathPrefix() {
        'smr'
    }

    @Override
    String getPathIdentifier() {
        getUTCReportDate()?.format(PATH_FORMATTER)
    }

    OffsetDateTime getUTCReportDate() {
        reportDate?.withOffsetSameInstant(ZoneOffset.UTC)
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