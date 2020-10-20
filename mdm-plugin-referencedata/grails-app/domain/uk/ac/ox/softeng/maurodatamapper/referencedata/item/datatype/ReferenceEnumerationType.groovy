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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype


import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueValuesValidator
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.rest.Resource
import groovy.util.logging.Slf4j

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceEnumerationType extends ReferenceDataType<ReferenceEnumerationType> {

    static hasMany = [
            referenceEnumerationValues: ReferenceEnumerationValue
    ]

    static constraints = {
        referenceEnumerationValues minSize: 1, validator: { val, obj -> new UniqueValuesValidator('key').isValid(val)}
    }

    static mapping = {
        referenceEnumerationValues cascade: 'all-delete-orphan'
    }

    ReferenceEnumerationType() {
        domainType = ReferenceEnumerationType.simpleName
    }

    @Override
    def beforeValidate() {
        super.beforeValidate()
        if (this.referenceEnumerationValues) {
            this.referenceEnumerationValues.sort().eachWithIndex { ev, i ->
                ev.createdBy = ev.createdBy ?: createdBy
                if (ev.getOrder() != i) ev.idx = i
                ev.beforeValidate()
            }
        }
    }

    ObjectDiff<ReferenceEnumerationType> diff(ReferenceEnumerationType otherDataType) {
        catalogueItemDiffBuilder(ReferenceEnumerationType, this, otherDataType)
            .appendList(ReferenceEnumerationValue, 'referenceEnumerationValues', this.referenceEnumerationValues, otherDataType.referenceEnumerationValues)
    }

    int countEnumerationValuesByKey(String key) {
        this.referenceEnumerationValues?.count {it.key == key} ?: 0
    }

    ReferenceEnumerationValue findEnumerationValueByKey(String key) {
        this.referenceEnumerationValues?.find {it.key == key}
    }

    ReferenceEnumerationType addToEnumerationValues(Map args) {
        addToReferenceEnumerationValues(new ReferenceEnumerationValue(args))
    }

    ReferenceEnumerationType addToEnumerationValues(String key, String value, User createdBy) {
        addToReferenceEnumerationValues(key: key, value: value, createdBy: createdBy.emailAddress)
    }

    ReferenceEnumerationType addToEnumerationValues(String key, String value, String category, User createdBy) {
        addToReferenceEnumerationValues(key: key, value: value, category: category, createdBy: createdBy.emailAddress)
    }

    ReferenceEnumerationType addToEnumerationValues(ReferenceEnumerationValue add) {
        ReferenceEnumerationValue valueToAdd = findEnumerationValueByKey(add.key)
        if (valueToAdd) {
            valueToAdd.value = add.value
            markDirty('referenceEnumerationValues', valueToAdd)
        } else {
            valueToAdd = add
            valueToAdd.enumerationType = this
            addTo('referenceEnumerationValues', valueToAdd)
        }
        updateEnumerationValueIndexes(valueToAdd)
        this
    }

    void updateEnumerationValueIndexes(ReferenceEnumerationValue updated) {
        List<ReferenceEnumerationValue> sorted = this.referenceEnumerationValues.sort()
        int updatedIndex = updated.getOrder()
        int maxIndex = sorted.size() - 1
        sorted.eachWithIndex { ReferenceEnumerationValue ev, int i ->
            //EV is the updated one, skipping any changes
            if (ev == updated) {
                // Make sure updated value is not ordered larger than the actual size of the collection
                if (ev.getOrder() > maxIndex) {
                    ev.idx = maxIndex
                }
                return
            }

            // Make sure all values have trackChanges turned on
            if (!ev.isDirty()) ev.trackChanges()

            log.trace('Before >> EV {} has order {} sorted to {}', ev.key, ev.order)
            // Reorder the index which matches the one we just added
            if (i == updatedIndex) {
                if (i == maxIndex) {
                    // If at end of list then move the current value back one to ensure the updated value is at then end of the list
                    ev.idx = i - 1
                } else {
                    // Otherwise alphabetical sorting has placed the elements in the wrong order so shift the value by 1
                    ev.idx = i + 1
                }
            } else if (ev.getOrder() != i) {
                // Sorting has got the order right so make sure the idx is set correctly
                ev.idx = i
            }
            log.trace('After >> EV {} has order {} (Dirty: {})', ev.key, i, ev.isDirty())
        }
    }
}