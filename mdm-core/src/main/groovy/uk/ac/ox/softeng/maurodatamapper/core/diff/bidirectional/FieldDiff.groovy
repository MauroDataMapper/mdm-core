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

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff

class FieldDiff<F> extends BiDirectionalDiff<F> {

    String fieldName

    FieldDiff(Class<F> targetClass) {
        super(targetClass)
    }

    FieldDiff<F> fieldName(String fieldName) {
        this.fieldName = fieldName
        this
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        FieldDiff<F> fieldDiff = (FieldDiff<F>) o

        if (fieldName != fieldDiff.fieldName) return false

        return true
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    @Override
    FieldDiff<F> leftHandSide(F lhs) {
        super.leftHandSide(lhs) as FieldDiff<F>
    }

    @Override
    FieldDiff<F> rightHandSide(F rhs) {
        super.rightHandSide(rhs) as FieldDiff<F>
    }

    @Override
    FieldDiff<F> commonAncestor(F ca) {
        super.commonAncestor(ca) as FieldDiff<F>
    }

    @Override
    FieldDiff<F> asMergeConflict() {
        this.mergeConflict = true
        this
    }

    @Override
    String toString() {
        "${fieldName} :: ${left?.toString()} <> ${right?.toString()}"
    }

    static boolean isFieldDiff(Diff diff) {
        diff.diffType == FieldDiff.simpleName
    }
}
