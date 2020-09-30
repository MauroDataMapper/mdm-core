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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.referencedata.gorm.constraint.validator.SummaryMetadataLabelValidator
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class SummaryMetadata implements CatalogueItemAware, InformationAware, CreatorAware {

    public final static Integer BATCH_SIZE = 5000

    UUID id
    SummaryMetadataType summaryMetadataType

    static hasMany = [
        summaryMetadataReports: SummaryMetadataReport
    ]

    static transients = ['catalogueItem']

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        label validator: {val, obj -> new SummaryMetadataLabelValidator(obj).isValid(val)}
    }

    static mapping = {
        summaryMetadataReports cascade: 'all-delete-orphan'
    }

    @Override
    String getDomainType() {
        SummaryMetadata.simpleName
    }

    String toString() {
        "${getClass().getName()} : ${label} : ${id ?: '(unsaved)'}"
    }

    String getEditLabel() {
        "Summary Metadata:${label}"
    }

    static DetachedCriteria<SummaryMetadata> byCatalogueItemId(Serializable catalogueItemId) {
        new DetachedCriteria<SummaryMetadata>(SummaryMetadata).eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<SummaryMetadata> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<SummaryMetadata> byLabel(String label) {
        new DetachedCriteria<SummaryMetadata>(SummaryMetadata).eq('label', label)
    }

    static DetachedCriteria<SummaryMetadata> withFilter(DetachedCriteria<SummaryMetadata> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.name}%")
        criteria
    }
}