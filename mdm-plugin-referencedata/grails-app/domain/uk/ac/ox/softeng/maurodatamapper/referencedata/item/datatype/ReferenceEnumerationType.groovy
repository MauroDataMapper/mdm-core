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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.IndexedSiblingAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueValuesValidator
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j
import io.micronaut.core.order.Ordered

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceEnumerationType extends ReferenceDataType<ReferenceEnumerationType> implements IndexedSiblingAware {

    static hasMany = [
        referenceEnumerationValues: ReferenceEnumerationValue
    ]

    static constraints = {
        referenceEnumerationValues minSize: 1, validator: { val, obj -> new UniqueValuesValidator('key').isValid(val) }
    }

    static mapping = {
        referenceEnumerationValues cascade: 'all-delete-orphan'
    }

    ReferenceEnumerationType() {
        domainType = ReferenceEnumerationType.simpleName
    }

    @Override
    void updateChildIndexes(ModelItem updated, Integer oldIndex) {
        updateSiblingIndexes(updated, referenceEnumerationValues)
    }

    @Override
    def beforeValidate() {
        long st = System.currentTimeMillis()
        super.beforeValidate()
        if (referenceEnumerationValues) {
            // REVs might be new so sort them
            fullSortOfChildren(referenceEnumerationValues)
            referenceEnumerationValues.each {ev ->
                ev.createdBy = ev.createdBy ?: createdBy
                ev.beforeValidate()
            }
        }
        log.trace('DT before validate {} took {}', this.label, Utils.timeTaken(st))
    }

    ObjectDiff<ReferenceEnumerationType> diff(ReferenceEnumerationType otherDataType, String context) {
        diff(otherDataType, context, null, null)
    }

    ObjectDiff<ReferenceEnumerationType> diff(ReferenceEnumerationType otherDataType, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        ObjectDiff<ReferenceEnumerationType> base = DiffBuilder.catalogueItemDiffBuilder(ReferenceEnumerationType, this, otherDataType, lhsDiffCache, rhsDiffCache)


        if (!lhsDiffCache || !rhsDiffCache) {
            base.appendCollection(ReferenceEnumerationValue, 'referenceEnumerationValues', this.referenceEnumerationValues, otherDataType.referenceEnumerationValues)
        } else {
            base.appendCollection(ReferenceEnumerationValue, 'referenceEnumerationValues')
        }
        base
    }

    int countReferenceEnumerationValuesByKey(String key) {
        this.referenceEnumerationValues?.count {it.key == key} ?: 0
    }

    ReferenceEnumerationValue findReferenceEnumerationValueByKey(String key) {
        this.referenceEnumerationValues?.find {it.key == key}
    }

    ReferenceEnumerationType addToReferenceEnumerationValues(Map args) {
        addToReferenceEnumerationValues(new ReferenceEnumerationValue(args))
    }

    ReferenceEnumerationType addToReferenceEnumerationValues(String key, String value, User createdBy) {
        addToReferenceEnumerationValues(key: key, value: value, createdBy: createdBy.emailAddress)
    }

    ReferenceEnumerationType addToReferenceEnumerationValues(String key, String value, String category, User createdBy) {
        addToReferenceEnumerationValues(key: key, value: value, category: category, createdBy: createdBy.emailAddress)
    }

    ReferenceEnumerationType addToReferenceEnumerationValues(ReferenceEnumerationValue add) {
        ReferenceEnumerationValue valueToAdd = findReferenceEnumerationValueByKey(add.key)
        if (valueToAdd) {
            valueToAdd.value = add.value
            markDirty('referenceEnumerationValues', valueToAdd)
        } else {
            valueToAdd = add
            valueToAdd.referenceEnumerationType = this
            addTo('referenceEnumerationValues', valueToAdd)
        }
        updateChildIndexes(valueToAdd, Ordered.LOWEST_PRECEDENCE)
        this
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
}