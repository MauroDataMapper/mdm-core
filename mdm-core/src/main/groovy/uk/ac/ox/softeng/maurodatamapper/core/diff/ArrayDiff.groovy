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

class ArrayDiff<T extends Diffable> extends FieldDiff<Collection<T>> {

    Collection<T> created
    Collection<T> deleted
    Collection<ObjectDiff<T>> modified

    private ArrayDiff() {
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<T> created(Collection<T> created) {
        this.created = created
        this
    }

    ArrayDiff<T> deleted(Collection<T> deleted) {
        this.deleted = deleted
        this
    }

    ArrayDiff<T> modified(Collection<ObjectDiff<T>> modified) {
        this.modified = modified
        this
    }

    @Override
    ArrayDiff<T> fieldName(String fieldName) {
        super.fieldName(fieldName) as ArrayDiff<T>
    }

    @Override
    ArrayDiff<T> leftHandSide(Collection<T> lhs) {
        super.leftHandSide(lhs) as ArrayDiff<T>
    }

    @Override
    ArrayDiff<T> rightHandSide(Collection<T> rhs) {
        super.rightHandSide(rhs) as ArrayDiff<T>
    }

    @Override
    Integer getNumberOfDiffs() {
        created.size() + deleted.size() + ((modified.sum { it.getNumberOfDiffs() } ?: 0) as Integer)
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder(super.toString())

        if (created) {
            stringBuilder.append('\n  Created ::\n').append(created)
        }
        if (deleted) {
            stringBuilder.append('\n  Deleted ::\n').append(deleted)
        }
        if (modified) {
            stringBuilder.append('\n  Modified ::\n').append(modified)
        }
        stringBuilder.toString()
    }

    static <K extends Diffable> ArrayDiff<K> builder(Class<K> arrayClass) {
        new ArrayDiff<K>()
    }
}
