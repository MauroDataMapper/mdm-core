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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype


import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.IndexedSiblingAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueValuesValidator
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.rest.Resource
import groovy.util.logging.Slf4j

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class EnumerationType extends DataType<EnumerationType> implements IndexedSiblingAware {

    static hasMany = [
        enumerationValues: EnumerationValue
    ]

    static constraints = {
        enumerationValues minSize: 1, validator: {val, obj -> new UniqueValuesValidator('key').isValid(val)}
    }

    static mapping = {
        enumerationValues cascade: 'all-delete-orphan'
    }

    EnumerationType() {
        domainType = EnumerationType.simpleName
    }

    @Override
    def beforeValidate() {
        super.beforeValidate()
        if (enumerationValues) {
            enumerationValues.sort().eachWithIndex {ev, i ->
                ev.createdBy = ev.createdBy ?: createdBy
                if (ev.getOrder() != i) ev.idx = i
                ev.beforeValidate()
            }
        }
    }

    ObjectDiff<EnumerationType> diff(EnumerationType otherDataType) {
        catalogueItemDiffBuilder(EnumerationType, this, otherDataType)
            .appendList(EnumerationValue, 'enumerationValues', this.enumerationValues, otherDataType.enumerationValues)
    }

    int countEnumerationValuesByKey(String key) {
        enumerationValues?.count {it.key == key} ?: 0
    }

    EnumerationValue findEnumerationValueByKey(String key) {
        enumerationValues?.find {it.key == key}
    }

    EnumerationType addToEnumerationValues(Map args) {
        addToEnumerationValues(new EnumerationValue(args))
    }

    EnumerationType addToEnumerationValues(String key, String value, User createdBy) {
        addToEnumerationValues(key: key, value: value, createdBy: createdBy.emailAddress)
    }

    EnumerationType addToEnumerationValues(String key, String value, String category, User createdBy) {
        addToEnumerationValues(key: key, value: value, category: category, createdBy: createdBy.emailAddress)
    }

    EnumerationType addToEnumerationValues(EnumerationValue add) {
        EnumerationValue valueToAdd = findEnumerationValueByKey(add.key)
        if (valueToAdd) {
            valueToAdd.value = add.value
            markDirty('enumerationValues', valueToAdd)
        } else {
            valueToAdd = add
            valueToAdd.enumerationType = this
            addTo('enumerationValues', valueToAdd)
        }
        updateChildIndexes(valueToAdd)
        this
    }


    /*
     * Update the index property of the EnumerationValues which belong to this EnumerationType, and which are siblings of an updated EnumerationValue
     *
     * @param EnumerationValue updated An EnumerationValue, which belongs to this EnumerationType, and which has been updated.
     * @param int oldIndex             The index of a value before it was updated.
     */
    void updateChildIndexes(EnumerationValue updated, int oldIndex = Integer.MAX_VALUE) {
        updateSiblingIndexes(updated, enumerationValues, oldIndex)
    }
}