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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceSummaryMetadataReport implements MdmDomain {

    static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern('yyyyMMddHHmmssSSSSSSX')

    UUID id
    OffsetDateTime reportDate
    String reportValue
    ReferenceSummaryMetadata summaryMetadata

    static belongsTo = [
        ReferenceSummaryMetadata
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
        ReferenceSummaryMetadataReport.simpleName
    }

    @Override
    String getPathPrefix() {
        'rsmr'
    }

    @Override
    String getPathIdentifier() {
        reportDate?.withOffsetSameInstant(ZoneOffset.UTC)?.format(PATH_FORMATTER)
    }

    String getEditLabel() {
        "Summary Metadata Report:${reportDate}"
    }

    static DetachedCriteria<ReferenceSummaryMetadataReport> byReferenceSummaryMetadataId(Serializable referenceSummaryMetadataId) {
        new DetachedCriteria<ReferenceSummaryMetadataReport>(ReferenceSummaryMetadataReport).
            eq('summaryMetadata.id', Utils.toUuid(referenceSummaryMetadataId)).join('summaryMetadata')
    }

    static DetachedCriteria<ReferenceSummaryMetadataReport> byReferenceSummaryMetadataIdAndId(Serializable referenceSummaryMetadataId, Serializable resourceId) {
        byReferenceSummaryMetadataId(referenceSummaryMetadataId).idEq(Utils.toUuid(resourceId))
    }
}