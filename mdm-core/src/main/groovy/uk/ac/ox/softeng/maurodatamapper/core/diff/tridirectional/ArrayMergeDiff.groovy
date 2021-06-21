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

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable

import groovy.transform.CompileStatic

/**
 * Note the same object cannot exist in more than one of created, deleted, modified.
 * These collections are mutually exclusive
 */
@CompileStatic
class ArrayMergeDiff<C extends Diffable> extends FieldMergeDiff<Collection<C>> {

    Collection<CreationMergeDiff<C>> created
    Collection<DeletionMergeDiff<C>> deleted
    Collection<MergeDiff<C>> modified

    ArrayMergeDiff(Class<Collection<C>> targetArrayClass) {
        super(targetArrayClass)
        created = []
        deleted = []
        modified = []
    }

    ArrayMergeDiff<C> withModifiedMergeDiffs(Collection<MergeDiff<C>> modified) {
        this.modified = modified
        this
    }

    ArrayMergeDiff<C> withCreatedMergeDiffs(Collection<CreationMergeDiff<C>> created) {
        this.created = created
        this
    }

    ArrayMergeDiff<C> withDeletedMergeDiffs(Collection<DeletionMergeDiff<C>> deleted) {
        this.deleted = deleted
        this
    }

    @Override
    ArrayMergeDiff<C> forFieldName(String fieldName) {
        super.forFieldName(fieldName) as ArrayMergeDiff<C>
    }

    @Override
    ArrayMergeDiff<C> withSource(Collection<C> lhs) {
        super.withSource(lhs) as ArrayMergeDiff<C>
    }

    @Override
    ArrayMergeDiff<C> withTarget(Collection<C> rhs) {
        super.withTarget(rhs) as ArrayMergeDiff<C>
    }

    @Override
    ArrayMergeDiff<C> withCommonAncestor(Collection<C> ca) {
        super.withCommonAncestor(ca) as ArrayMergeDiff<C>
    }

    ArrayMergeDiff<C> asMergeConflict() {
        super.asMergeConflict() as ArrayMergeDiff<C>
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
}
