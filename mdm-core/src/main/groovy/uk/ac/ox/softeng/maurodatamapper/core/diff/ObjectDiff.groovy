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
import org.apache.commons.lang3.NotImplementedException

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

    ObjectDiff<T> mergeDiff(ObjectDiff<T> left, ObjectDiff<T> right) {
        this.diffs = this.diffs.findAll {
            def fieldName = it.fieldName

            if (fieldName in left.diffs.fieldName) {
                if (fieldName in right.diffs.fieldName) {
                    if (it.class == FieldDiff) {
                        it.isMergeConflict = true
                        it.commonAncestorValue = right.diffs.find { it.fieldName == fieldName }.left
                        true
                    } else if (it.class == ArrayDiff) {
                        def leftArrayDiff = left.diffs.find { it.fieldName == fieldName }
                        def rightArrayDiff = right.diffs.find { it.fieldName == fieldName }
                        it.created = it.created.findAll {
                            def diffIdentifier = it.value.diffIdentifier
                            if (diffIdentifier in leftArrayDiff.created.value.diffIdentifier) {
                                // top created, left created
                                it.isMergeConflict = false
                                true
                            } else if (diffIdentifier in leftArrayDiff.modified.left.diffIdentifier) {
                                // top created, left modified
                                it.isMergeConflict = true
                                it.commonAncestorValue =
                                    left.diffs.find { it.fieldName == fieldName }.left.find { it.diffIdentifier == diffIdentifier }
                                true
                            } else {
                                false
                            }
                        }
                        it.deleted = it.deleted.findAll {
                            def diffIdentifier = it.value.diffIdentifier
                            if (diffIdentifier in rightArrayDiff.modified.left.diffIdentifier) {
                                // top deleted, right modified
                                it.isMergeConflict = true
                                it.commonAncestorValue =
                                    right.diffs.find { it.fieldName == fieldName }.left.find { it.diffIdentifier == diffIdentifier }
                                true
                            } else if (diffIdentifier in leftArrayDiff.deleted.value.diffIdentifier) {
                                // top deleted, right not modified, left deleted
                                it.isMergeConflict = false
                                true
                            } else {
                                false
                            }
                        }
                        it.modified = it.modified.findAll {
                            def diffIdentifier = it.right.diffIdentifier
                            if (diffIdentifier in leftArrayDiff.created.value.diffIdentifier) {
                                // top modified, right created, (left also created)
                                it.isMergeConflict = true
                                it.commonAncestorValue = null
                                true
                            } else if (diffIdentifier in leftArrayDiff.modified.left.diffIdentifier) {
                                if (diffIdentifier in rightArrayDiff.modified.left.diffIdentifier) {
                                    // top modified, left modified, right modified
                                    it.isMergeConflict = true
                                    it.commonAncestorValue =
                                        right.diffs.find { it.fieldName == fieldName }.left.find { it.diffIdentifier == diffIdentifier }
                                    true
                                } else {
                                    // top modified, left modified, right not modified
                                    it.isMergeConflict = false
                                    true
                                }
                            } else {
                                false
                            }
                        }
                    } else {
                        throw new NotImplementedException('ObjectDiff.mergeDiff only implemented for types in [FieldDiff, ArrayDiff]')
                    }
                } else {
                    it.isMergeConflict = false
                    true
                }
            } else {
                false
            }
        }

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

