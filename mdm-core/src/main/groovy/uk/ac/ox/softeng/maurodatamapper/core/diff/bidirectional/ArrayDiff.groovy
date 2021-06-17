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
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.creationDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.deletionDiff

/**
 * Note the same object cannot exist in more than one of created, deleted, modified.
 * These collections are mutually exclusive
 */
class ArrayDiff<C extends Diffable> extends FieldDiff<Collection<C>> {

    Collection<CreationDiff<C>> created
    Collection<DeletionDiff<C>> deleted
    Collection<ObjectDiff<C>> modified

    ArrayDiff(Class<Collection<C>> targetClass) {
        super(targetClass)
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<C> created(Collection<C> created) {
        this.created = created.collect { creationDiff(it.class as Class<C>).created(it) }
        this
    }

    ArrayDiff<C> deleted(Collection<C> deleted) {
        this.deleted = deleted.collect { deletionDiff(it.class as Class<C>).deleted(it) }
        this
    }

    ArrayDiff<C> modified(Collection<ObjectDiff<C>> modified) {
        this.modified = modified
        this
    }

    ArrayDiff<C> withCreatedDiffs(Collection<CreationDiff<C>> created) {
        this.created = created
        this
    }

    ArrayDiff<C> withDeletedDiffs(Collection<DeletionDiff<C>> deleted) {
        this.deleted = deleted
        this
    }

    @Override
    ArrayDiff<C> fieldName(String fieldName) {
        super.fieldName(fieldName) as ArrayDiff<C>
    }

    @Override
    ArrayDiff<C> leftHandSide(Collection<C> lhs) {
        super.leftHandSide(lhs) as ArrayDiff<C>
    }

    @Override
    ArrayDiff<C> rightHandSide(Collection<C> rhs) {
        super.rightHandSide(rhs) as ArrayDiff<C>
    }

    @Override
    ArrayDiff<C> commonAncestor(Collection<C> ca) {
        super.commonAncestor(ca) as ArrayDiff<C>
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

    static boolean isArrayDiff(Diff diff) {
        diff.diffType == ArrayDiff.simpleName
    }
}
