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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.gorm.constraint.validator.ReferenceEnumerationValueKeyValidator
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceEnumerationValue implements ModelItem<ReferenceEnumerationValue, ReferenceDataModel> {

    UUID id

    String category
    String key
    String value

    static belongsTo = [referenceEnumerationType: ReferenceEnumerationType]

    static transients = ['aliases']

    static hasMany = [
        classifiers   : Classifier,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        referenceFiles: ReferenceFile,
        rules         : Rule
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        key blank: false, validator: { val, obj -> new ReferenceEnumerationValueKeyValidator(obj).isValid(val) }
        value blank: false
        category nullable: true, blank: false
    }

    static mapping = {
        key type: 'text'
        value type: 'text'
        category type: 'text'
        referenceEnumerationType index: 'enumeration_value_enumeration_type_idx', fetch: 'join'
        model cascade: 'none'
    }

    static mappedBy = [:]

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
    }

    ReferenceEnumerationValue() {
    }

    @Override
    String getDomainType() {
        ReferenceEnumerationValue.simpleName
    }

    @Override
    String getPathPrefix() {
        'rev'
    }

    @Override
    ReferenceEnumerationType getParent() {
        referenceEnumerationType
    }

    def beforeValidate() {
        label = key
        description = value
        beforeValidateModelItem()
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    @Override
    ReferenceDataModel getModel() {
        referenceEnumerationType?.model
    }

    @Override
    String getPathIdentifier() {
        this.key
    }

    @Override
    Boolean hasChildren() {
        false
    }

    @Override
    ReferenceEnumerationType getIndexedWithin() {
        referenceEnumerationType
    }

    @Override
    void updateIndices(Integer oldIndex) {
        // If adding to an existing ET then we should update indices
        // Otherwise this will have been done at DM or ET level
        if (referenceEnumerationType?.id) referenceEnumerationType.updateChildIndexes(this, oldIndex)
    }

    ObjectDiff<ReferenceEnumerationValue> diff(ReferenceEnumerationValue otherEnumerationValue, String context) {
        diff(otherEnumerationValue, context, null,null)
    }

    ObjectDiff<ReferenceEnumerationValue> diff(ReferenceEnumerationValue otherEnumerationValue, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(ReferenceEnumerationValue, this, otherEnumerationValue, lhsDiffCache, rhsDiffCache)
            .appendString('key', this.key, otherEnumerationValue.key)
            .appendString('value', this.value, otherEnumerationValue.value)
            .appendString('category', this.category, otherEnumerationValue.category)
    }

    void setKey(String key) {
        this.key = key
        this.label = key
    }

    void setValue(String value) {
        this.value = value
        this.description = value
    }

    static DetachedCriteria<ReferenceEnumerationValue> byReferenceEnumerationType(Serializable referenceEnumerationTypeId) {
        new DetachedCriteria<ReferenceEnumerationValue>(ReferenceEnumerationValue).eq('referenceEnumerationType.id', Utils.toUuid(referenceEnumerationTypeId))
    }

    static DetachedCriteria<ReferenceEnumerationValue> byIdAndReferenceEnumerationType(Serializable resourceId, Serializable referenceEnumerationTypeId) {
        byReferenceEnumerationType(referenceEnumerationTypeId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ReferenceEnumerationValue> byClassifierId(Serializable classifierId) {
        where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }
    static DetachedCriteria<ReferenceEnumerationValue> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ReferenceEnumerationValue> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<ReferenceEnumerationValue> byReferenceDataModelId(Serializable dataModelId) {
        new DetachedCriteria<ReferenceEnumerationValue>(ReferenceEnumerationValue).in('referenceEnumerationType', ReferenceEnumerationType.byReferenceDataModelId(dataModelId).id())
    }
}