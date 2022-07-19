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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceDataValue implements MdmDomain, Diffable<ReferenceDataValue> {

    public final static Integer BATCH_SIZE = 1000

    UUID id

    int rowNumber
    String value
    ReferenceDataModel referenceDataModel
    ReferenceDataElement referenceDataElement

    static belongsTo = [ReferenceDataModel, ReferenceDataElement]

    static constraints = {
        value blank: true, nullable: true
        rowNumber unique: 'referenceDataElement'
    }

    static mapping = {
        rowNumber type: 'integer'
        value type: 'text'
        referenceDataModel index: 'reference_data_value_reference_data_model_idx', cascadeValidate: 'none' //, cascade: 'none'
        referenceDataElement index: 'reference_data_value_reference_data_element_idx', cascade: 'none', fetch: 'join', cascadeValidate: 'dirty'
    }

    ReferenceDataValue() {
    }

    @Override
    String getDomainType() {
        ReferenceDataValue.simpleName
    }

    @Override
    String getPathPrefix() {
        'rdv'
    }

    @Override
    String getPathIdentifier() {
        rowNumber
    }

    String getEditLabel() {
        "$domainType:$value"
    }

    def beforeValidate() {
        checkPath() // get path to ensure its built
    }

    @Override
    Path buildPath() {
        // We only want to call the getpath method once
        Path parentPath = referenceDataElement?.getPath()
        parentPath ? Path.from(parentPath, pathPrefix, pathIdentifier) : null
    }

    ObjectDiff<ReferenceDataValue> diff(ReferenceDataValue otherValue, String context) {
        diff(otherValue, context, null, null)
    }

    @Override
    ObjectDiff<ReferenceDataValue> diff(ReferenceDataValue otherValue, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        String lhsId = this.id ?: "Left:Unsaved_${this.domainType}"
        String rhsId = otherValue.id ?: "Right:Unsaved_${otherValue.domainType}"
        DiffBuilder.objectDiff(ReferenceDataValue)
            .leftHandSide(lhsId, this)
            .rightHandSide(rhsId, otherValue)
            .withLeftHandSideCache(lhsDiffCache)
            .withRightHandSideCache(rhsDiffCache)
            .appendNumber('rowNumber', this.rowNumber, otherValue.rowNumber)
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
                countDistinct('rowNumber')
            }
    }

    static DetachedCriteria<ReferenceDataValue> distinctRowNumbersByReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch) {
        ReferenceDataValue.byReferenceDataModelIdAndValueIlike(referenceDataModelId, valueSearch)
            .projections {
                distinct('rowNumber')
            }
            .order('rowNumber', 'asc')
    }

    static DetachedCriteria<ReferenceDataValue> byReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch) {
        ReferenceDataValue.byReferenceDataModelId(referenceDataModelId)
            .ilike('value', "%${valueSearch}%")
    }

    static DetachedCriteria<ReferenceDataValue> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ReferenceDataValue> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}
