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
package uk.ac.ox.softeng.maurodatamapper.core.diff

import uk.ac.ox.softeng.maurodatamapper.core.api.exception.ApiDiffException

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import java.time.OffsetDateTime

class ObjectDiff<T extends Diffable> extends Diff<T> {

    List<FieldDiff> diffs

    String leftIdentifier
    String rightIdentifier

    private ObjectDiff() {
        diffs = []
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        ObjectDiff<T> objectDiff = (ObjectDiff<T>) o

        if (leftIdentifier != objectDiff.leftIdentifier) return false
        if (rightIdentifier != objectDiff.rightIdentifier) return false
        if (diffs != objectDiff.diffs) return false

        return true
    }

    @Override
    String toString() {
        int numberOfDiffs = getNumberOfDiffs()
        if (!numberOfDiffs) return "${leftIdentifier} == ${rightIdentifier}"
        "${leftIdentifier} <> ${rightIdentifier} :: ${numberOfDiffs} differences\n  ${diffs.collect { it.toString() }.join('\n  ')}"
    }

    @Override
    Integer getNumberOfDiffs() {
        diffs?.sum {it.getNumberOfDiffs()} as Integer ?: 0
    }

    ObjectDiff<T> leftHandSide(String leftId, T lhs) {
        leftHandSide(lhs)
        this.leftIdentifier = leftId
        this
    }

    ObjectDiff<T> rightHandSide(String rightId, T rhs) {
        rightHandSide(rhs)
        this.rightIdentifier = rightId
        this
    }

    ObjectDiff<T> appendNumber(final String fieldName, final Number lhs, final Number rhs) throws ApiDiffException {
        append(FieldDiff.builder(Number), fieldName, lhs, rhs)
    }

    ObjectDiff<T> appendBoolean(final String fieldName, final Boolean lhs, final Boolean rhs) throws ApiDiffException {
        append(FieldDiff.builder(Boolean), fieldName, lhs, rhs)
    }

    ObjectDiff<T> appendString(final String fieldName, final String lhs, final String rhs) throws ApiDiffException {
        append(FieldDiff.builder(String), fieldName, clean(lhs), clean(rhs))
    }

    ObjectDiff<T> appendOffsetDateTime(final String fieldName, final OffsetDateTime lhs, final OffsetDateTime rhs) throws ApiDiffException {
        append(FieldDiff.builder(OffsetDateTime), fieldName, lhs, rhs)
    }

    def <K extends Diffable> ObjectDiff<T> appendList(Class<K> diffableClass, String fieldName,
                                                      Collection<K> lhs, Collection<K> rhs)
        throws ApiDiffException {

        validateFieldNameNotNull(fieldName)

        // If no lhs or rhs then nothing to compare
        if (!lhs && !rhs) return this

        ArrayDiff diff = ArrayDiff.builder(diffableClass)
            .fieldName(fieldName)
            .leftHandSide(lhs)
            .rightHandSide(rhs)


        // If no lhs then all rhs have been created/added
        if (!lhs) {
            return append(diff.objectDiffs(rhs.collect { diffableClass.getDeclaredConstructor().newInstance().diff(it) }))
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
            return append(diff.objectDiffs(lhs.collect { it.diff(diffableClass.getDeclaredConstructor().newInstance()) }))
        }

        Collection<ObjectDiff> modified = []

        Map<String, K> lhsMap = lhs.collectEntries { [it.getDiffIdentifier(), it] }
        Map<String, K> rhsMap = rhs.collectEntries { [it.getDiffIdentifier(), it] }

        Set<String> uniqueDiffIdentifiers = ((lhsMap.keySet() as ArrayList) + (rhsMap.keySet() as ArrayList)) as Set

        uniqueDiffIdentifiers.each {
            K lObj = lhsMap[it] ?: diffableClass.getDeclaredConstructor().newInstance()
            K rObj = rhsMap[it] ?: diffableClass.getDeclaredConstructor().newInstance()
            ObjectDiff od = lObj.diff(rObj)
            // If not equal then objects have been modified
            if (!od.objectsAreIdentical()) {
                modified.add(od)
            }
        }

        if (modified) append(diff.objectDiffs(modified))
        this
    }

    def <K> ObjectDiff<T> append(FieldDiff<K> fieldDiff, String fieldName, K lhs, K rhs) {
        validateFieldNameNotNull(fieldName)
        if (lhs == null && rhs == null) {
            return this
        }
        if (lhs != rhs) {
            append(fieldDiff.fieldName(fieldName).leftHandSide(lhs).rightHandSide(rhs))
        }
        this
    }

    ObjectDiff<T> append(FieldDiff fieldDiff) {
        diffs.add(fieldDiff)
        this
    }

    FieldDiff find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.FieldDiff') Closure closure) {
        diffs.find closure
    }


    private static void validateFieldNameNotNull(final String fieldName) throws ApiDiffException {
        if (!fieldName) {
            throw new ApiDiffException('OD01', 'Field name cannot be null or blank')
        }
    }

    static String clean(String s) {
        s?.trim() ?: null
    }

    static <K extends Diffable> ObjectDiff<K> builder(Class<K> objectClass) {
        new ObjectDiff<K>()
    }
}

