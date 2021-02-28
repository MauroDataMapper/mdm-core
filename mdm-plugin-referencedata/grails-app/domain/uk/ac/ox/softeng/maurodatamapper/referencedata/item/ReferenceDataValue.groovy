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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.gorm.GormEntity

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceDataValue implements CreatorAware, Diffable<ReferenceDataValue> {

    public final static Integer BATCH_SIZE = 10000

    UUID id

    int rowNumber
    String value

    static belongsTo = [referenceDataModel: ReferenceDataModel, referenceDataElement: ReferenceDataElement]

    static constraints = {
        value blank: true, nullable: true
    }

    static mapping = {
        rowNumber type: 'integer'
        value type: 'text'
        referenceDataModel index: 'reference_data_value_reference_data_model_idx'
        referenceDataElement index: 'reference_data_value_reference_data_element_idx', cascade: 'save-update', fetch: 'join'
        model cascade: 'none'
    }  

    static mappedBy = [:]

    ReferenceDataValue() {
    }

    @Override
    String getDomainType() {
        ReferenceDataValue.simpleName
    }

    @Override
    String getDiffIdentifier() {
        this.id
    }

    ObjectDiff<ReferenceDataValue> diff(ReferenceDataValue otherValue) {
        catalogueItemDiffBuilder(ReferenceDataValue, this, otherValue)
            .appendInteger('rowNumber', this.rowNumber, otherValue.rowNumber)
            .appendString('value', this.value, otherValue.value)
    }

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelId(Serializable referenceDataModelId) {
        new DetachedCriteria<ReferenceDataValue>(ReferenceDataValue)
        .eq('referenceDataModel.id', Utils.toUuid(referenceDataModelId))
    }

    static DetachedCriteria<ReferenceDataValue> withFilter(DetachedCriteria<ReferenceDataValue> criteria, Map filters) {
        if (filters.domainType) criteria = criteria.ilike('domainType', "%${filters.domainType}%")
        criteria
    }    

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelIdAndRowNumber(Serializable referenceDataModelId, Integer fromRowNumber, Integer toRowNumber) {
        ReferenceDataValue.byReferenceDataModelId(referenceDataModelId)
        .ge('rowNumber', fromRowNumber)
        .lt('rowNumber', toRowNumber)
    }

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelIdAndRowNumberIn(Serializable referenceDataModelId, List rowNumbers) {
        ReferenceDataValue.byReferenceDataModelId(referenceDataModelId)
        .'in'('rowNumber', rowNumbers)
    }    

    static DetachedCriteria<ReferenceDataValue> countByReferenceDataModelId(Serializable referenceDataModelId) {
        ReferenceDataValue.byReferenceDataModelId(referenceDataModelId)
        .projections {
            countDistinct("rowNumber")
        }
    }

    static DetachedCriteria<ReferenceDataValue> distinctRowNumbersByReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch) {
        ReferenceDataValue.byReferenceDataModelIdAndValueIlike(referenceDataModelId, valueSearch)
        .projections {
            distinct("rowNumber")
        }
        .order("rowNumber", "asc")
    }

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch) {
        ReferenceDataValue.byReferenceDataModelId(referenceDataModelId)
        .ilike('value', "%${valueSearch}%")
    } 
}
