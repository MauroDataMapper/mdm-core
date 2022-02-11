/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.summarymetadata.ReferenceSummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.referencedata.gorm.constraint.validator.ReferenceSummaryMetadataLabelValidator
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceSummaryMetadata implements MultiFacetItemAware, InformationAware {

    public final static Integer BATCH_SIZE = 5000

    UUID id
    ReferenceSummaryMetadataType summaryMetadataType

    static hasMany = [
        summaryMetadataReports: ReferenceSummaryMetadataReport
    ]

    static transients = ['multiFacetAwareItem']

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.multiFacetAwareItem && !obj.multiFacetAwareItem.ident()) return true
            ['default.null.message']
        }
        label validator: { val, obj -> new ReferenceSummaryMetadataLabelValidator(obj).isValid(val) }
    }

    static mapping = {
        summaryMetadataReports cascade: 'all-delete-orphan'
    }

//    def beforeValidate(){
//        beforeValidateCheck()
//    }

    @Override
    def beforeInsert(){
        beforeInsertCheck()
    }

    @Override
    String getDomainType() {
        ReferenceSummaryMetadata.simpleName
    }

    @Override
    String getPathPrefix() {
        'rsm'
    }

    @Override
    String getPathIdentifier() {
        label
    }

    String toString() {
        "${getClass().getName()} : ${label} : ${id ?: '(unsaved)'}"
    }

    String getEditLabel() {
        "Reference Summary Metadata:${label}"
    }

    static DetachedCriteria<ReferenceSummaryMetadata> by() {
        new DetachedCriteria<ReferenceSummaryMetadata>(ReferenceSummaryMetadata)
    }

    static DetachedCriteria<ReferenceSummaryMetadata> byMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        new DetachedCriteria<ReferenceSummaryMetadata>(ReferenceSummaryMetadata).eq('multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
    }

    static DetachedCriteria<ReferenceSummaryMetadata> byMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        new DetachedCriteria<ReferenceSummaryMetadata>(ReferenceSummaryMetadata).inList('multiFacetAwareItemId', multiFacetAwareItemIds)
    }

    static DetachedCriteria<ReferenceSummaryMetadata> byMultiFacetAwareItemIdAndId(Serializable multiFacetAwareItemId, Serializable resourceId) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ReferenceSummaryMetadata> byLabel(String label) {
        new DetachedCriteria<ReferenceSummaryMetadata>(ReferenceSummaryMetadata).eq('label', label)
    }

    static DetachedCriteria<ReferenceSummaryMetadata> withFilter(DetachedCriteria<ReferenceSummaryMetadata> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.name}%")
        criteria
    }
}