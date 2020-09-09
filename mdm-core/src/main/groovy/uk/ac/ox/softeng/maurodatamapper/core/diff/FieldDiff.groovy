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

class FieldDiff<T> extends Diff<T> {

    String fieldName

    FieldDiff() {
    }

    FieldDiff<T> fieldName(String fieldName) {
        this.fieldName = fieldName
        this
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        FieldDiff<T> fieldDiff = (FieldDiff<T>) o

        if (fieldName != fieldDiff.fieldName) return false

        return true
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    @Override
    FieldDiff<T> leftHandSide(T lhs) {
        super.leftHandSide(lhs) as FieldDiff<T>
    }

    @Override
    FieldDiff<T> rightHandSide(T rhs) {
        super.rightHandSide(rhs) as FieldDiff<T>
    }

    @Override
    String toString() {
        "${fieldName} :: ${left?.toString()} <> ${right?.toString()}"
    }

    static <K> FieldDiff<K> builder(Class<K> fieldClass) {
        new FieldDiff<K>()
    }
}
