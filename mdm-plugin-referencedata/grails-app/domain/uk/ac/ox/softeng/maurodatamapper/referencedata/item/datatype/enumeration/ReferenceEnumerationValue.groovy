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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.gorm.constraint.validator.EnumerationValueKeyValidator
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.gorm.GormEntity

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
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        key blank: false, validator: {val, obj -> new EnumerationValueKeyValidator(obj).isValid(val)}
        value blank: false
        category nullable: true, blank: false
    }

    static mapping = {
        key type: 'text'
        value type: 'text'
        category type: 'text'
        enumerationType index: 'enumeration_value_enumeration_type_idx', fetch: 'join'
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
    GormEntity getPathParent() {
        enumerationType
    }

    @Override
    def beforeValidate() {
        label = key
        description = value
        beforeValidateModelItem()
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    @Override
    ReferenceDataModel getModel() {
        enumerationType?.model
    }

    @Override
    String getDiffIdentifier() {
        this.key
    }

    @Override
    Boolean hasChildren() {
        false
    }

    @Override
    void updateIndices(int index) {
        enumerationType.updateEnumerationValueIndexes(this)
    }

    ObjectDiff<ReferenceEnumerationValue> diff(ReferenceEnumerationValue otherEnumerationValue) {
        catalogueItemDiffBuilder(ReferenceEnumerationValue, this, otherEnumerationValue)
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

    static DetachedCriteria<ReferenceEnumerationValue> byEnumerationType(Serializable enumerationTypeId) {
        new DetachedCriteria<ReferenceEnumerationValue>(ReferenceEnumerationValue).eq('enumerationType.id', Utils.toUuid(enumerationTypeId))
    }

    static DetachedCriteria<ReferenceEnumerationValue> byIdAndEnumerationType(Serializable resourceId, Serializable enumerationTypeId) {
        byEnumerationType(enumerationTypeId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ReferenceEnumerationValue> byClassifierId(Serializable classifierId) {
        where {
            classifiers {
                eq 'id', Utils.toUuid(classifierId)
            }
        }
    }
}