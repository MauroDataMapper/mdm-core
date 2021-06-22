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
package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional


import groovy.transform.CompileStatic

@CompileStatic
class FieldMergeDiff<F> extends TriDirectionalDiff<F> {

    String fieldName

    FieldMergeDiff(Class<F> targetClass) {
        super(targetClass)
    }

    FieldMergeDiff<F> forFieldName(String fieldName) {
        this.fieldName = fieldName
        this
    }

    @Override
    FieldMergeDiff<F> withSource(F lhs) {
        super.withSource(lhs) as FieldMergeDiff<F>
    }

    @Override
    FieldMergeDiff<F> withTarget(F rhs) {
        super.withTarget(rhs) as FieldMergeDiff<F>
    }

    @Override
    FieldMergeDiff<F> withCommonAncestor(F ca) {
        super.withCommonAncestor(ca) as FieldMergeDiff<F>
    }

    FieldMergeDiff<F> asMergeConflict() {
        super.asMergeConflict() as FieldMergeDiff<F>
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        FieldMergeDiff<F> fieldDiff = (FieldMergeDiff<F>) o

        if (fieldName != fieldDiff.fieldName) return false

        return true
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    boolean hasDiff() {
        source != target
    }

    @Override
    String toString() {
        "${fieldName} :: ${source} <> ${target} :: ${commonAncestor} "
    }
}
