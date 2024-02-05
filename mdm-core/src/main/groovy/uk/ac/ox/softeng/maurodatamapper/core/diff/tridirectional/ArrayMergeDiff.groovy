/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.path.Path

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.transform.CompileStatic

/**
 * Note the same object cannot exist in more than one of created, deleted, modified.
 * These collections are mutually exclusive
 */
@CompileStatic
@SuppressFBWarnings(value = 'EQ_DOESNT_OVERRIDE_EQUALS')
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

    ArrayMergeDiff<C> insideFullyQualifiedObjectPath(Path fullyQualifiedObjectPath) {
        super.insideFullyQualifiedObjectPath(fullyQualifiedObjectPath) as ArrayMergeDiff<C>
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
    ArrayMergeDiff<C> getValidOnly() {
        super.getValidOnly() as ArrayMergeDiff<C>
    }

    @Override
    boolean hasDiff() {
        getNumberOfDiffs() != 0
    }

    @Override
    Integer getNumberOfDiffs() {
        created.size() + deleted.size() + ((modified.sum { it.getNumberOfDiffs() } ?: 0) as Integer)
    }

    List<TriDirectionalDiff> getFlattenedDiffs() {
        List<TriDirectionalDiff> flattenedDiffs = new ArrayList<>(numberOfDiffs)
        flattenedDiffs.addAll(created.sort())
        flattenedDiffs.addAll(deleted.sort())
        flattenedDiffs.addAll(modified.sort().collectMany { it.getFlattenedDiffs() })
        flattenedDiffs
    }

    @Override
    String toString() {
        String diffIdentifier = source?.first()?.diffIdentifier ?: target?.first()?.diffIdentifier ?: commonAncestor?.first()?.diffIdentifier
        StringBuilder stringBuilder = new StringBuilder(
            "${diffIdentifier}.${fieldName} :: ${source.size()} <> ${target.size()} :: ${commonAncestor.size()}")

        if (created) {
            stringBuilder.append('\n').append(created.join('\n'))
        }
        if (deleted) {
            stringBuilder.append('\n').append(deleted.join('\n'))
        }
        if (modified) {
            stringBuilder.append('\n  Modified ::\n').append(modified.join('\n'))
        }
        stringBuilder.toString()
    }
}
