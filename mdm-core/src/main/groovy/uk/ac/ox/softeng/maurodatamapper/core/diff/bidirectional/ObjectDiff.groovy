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
package uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional

import uk.ac.ox.softeng.maurodatamapper.core.api.exception.ApiDiffException
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.fieldDiff

/*
Always in relation to the lhs
 */

class ObjectDiff<O extends Diffable> extends BiDirectionalDiff<O> {

    List<FieldDiff> diffs

    String leftIdentifier
    String rightIdentifier
    String path

    ObjectDiff(Class<O> targetClass) {
        super(targetClass)
        diffs = []
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        ObjectDiff<O> objectDiff = (ObjectDiff<O>) o

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
        diffs?.sum { it.getNumberOfDiffs() } as Integer ?: 0
    }

    @Deprecated
    ObjectDiff<O> leftHandSide(String leftId, O lhs) {
        leftHandSide(lhs)
    }

    @Deprecated
    ObjectDiff<O> rightHandSide(String rightId, O rhs) {
        rightHandSide(rhs)
    }

    ObjectDiff<O> leftHandSide(O lhs) {
        super.leftHandSide(lhs)
        this.leftIdentifier = lhs.diffIdentifier
        this
    }

    ObjectDiff<O> rightHandSide(O rhs) {
        super.rightHandSide(rhs)
        this.rightIdentifier = rhs.diffIdentifier
        this
    }

    ObjectDiff<O> appendNumber(final String fieldName, final Number lhs, final Number rhs) throws ApiDiffException {
        append(fieldDiff(Number), fieldName, lhs, rhs)
    }

    ObjectDiff<O> appendBoolean(final String fieldName, final Boolean lhs, final Boolean rhs) throws ApiDiffException {
        append(fieldDiff(Boolean), fieldName, lhs, rhs)
    }

    ObjectDiff<O> appendString(final String fieldName, final String lhs, final String rhs) throws ApiDiffException {
        append(fieldDiff(String), fieldName, clean(lhs), clean(rhs))
    }

    ObjectDiff<O> appendOffsetDateTime(final String fieldName, final OffsetDateTime lhs, final OffsetDateTime rhs) throws ApiDiffException {
        append(fieldDiff(OffsetDateTime), fieldName, lhs, rhs)
    }

    def <K extends Diffable> ObjectDiff<O> appendList(Class<K> diffableClass, String fieldName,
                                                      Collection<K> lhs, Collection<K> rhs) throws ApiDiffException {

        validateFieldNameNotNull(fieldName)

        // If no lhs or rhs then nothing to compare
        if (!lhs && !rhs) return this

        ArrayDiff diff = arrayDiff(diffableClass)
            .fieldName(fieldName)
            .leftHandSide(lhs)
            .rightHandSide(rhs)


        // If no lhs then all rhs have been created/added
        if (!lhs) {
            return append(diff.created(rhs))
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
            return append(diff.deleted(lhs))
        }

        Collection<K> deleted = []
        Collection<ObjectDiff> modified = []

        // Assume all rhs have been created new
        List<K> created = new ArrayList<>(rhs)

        Map<String, K> lhsMap = lhs.collectEntries { [it.getDiffIdentifier(), it] }
        Map<String, K> rhsMap = rhs.collectEntries { [it.getDiffIdentifier(), it] }

        // Work through each lhs object and compare to rhs object
        lhsMap.each { di, lObj ->
            K rObj = rhsMap[di]
            if (rObj) {
                // If robj then it exists and has not been created
                created.remove(rObj)
                ObjectDiff od = lObj.diff(rObj)
                // If not equal then objects have been modified
                if (!od.objectsAreIdentical()) {
                    modified.add(od)
                }
            } else {
                // If no robj then object has been deleted from lhs
                deleted.add(lObj)
            }
        }

        if (created || deleted || modified) {
            append(diff.created(created)
                       .deleted(deleted)
                       .modified(modified))
        }
        this
    }

    def <K> ObjectDiff<O> append(FieldDiff<K> fieldDiff, String fieldName, K lhs, K rhs) {
        validateFieldNameNotNull(fieldName)
        if (lhs == null && rhs == null) {
            return this
        }
        if (lhs != rhs) {
            append(fieldDiff.fieldName(fieldName).leftHandSide(lhs).rightHandSide(rhs))
        }
        this
    }

    ObjectDiff<O> append(FieldDiff fieldDiff) {
        diffs.add(fieldDiff)
        this
    }

    FieldDiff find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff') Closure closure) {
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

    /**
     * @use DiffBuilder.objectDiff* @param objectClass
     * @return
     */
    @Deprecated
    static <K extends Diffable> ObjectDiff<K> builder(Class<K> objectClass) {
        new ObjectDiff<K>()
    }
}

