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
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.transform.CompileStatic

@CompileStatic
class DeletionMergeDiff<D extends Diffable> extends TriDirectionalDiff<D> implements Comparable<DeletionMergeDiff> {

    ObjectDiff<D> mergeModificationDiff

    DeletionMergeDiff(Class<D> targetClass) {
        super(targetClass)
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    D getDeleted() {
        super.getValue() as D
    }

    String getDeletedIdentifier() {
        value.getDiffIdentifier(fullyQualifiedObjectPath.toString())
    }

    boolean isSourceDeletionAndTargetModification() {
        mergeModificationDiff != null
    }

    Path getFullyQualifiedPath() {
        fullyQualifiedObjectPath.resolve(deleted.path)
    }

    DeletionMergeDiff<D> whichDeleted(D object) {
        this.value = object
        withCommonAncestor object
    }

    @Override
    DeletionMergeDiff<D> insideFullyQualifiedObjectPath(Path fullyQualifiedObjectPath) {
        super.insideFullyQualifiedObjectPath(fullyQualifiedObjectPath) as DeletionMergeDiff<D>
    }

    DeletionMergeDiff<D> withMergeModification(ObjectDiff<D> modifiedDiff) {
        this.mergeModificationDiff = modifiedDiff
        this
    }

    DeletionMergeDiff<D> withNoMergeModification() {
        this
    }

    DeletionMergeDiff<D> withCommonAncestor(D ca) {
        super.withCommonAncestor(ca) as DeletionMergeDiff<D>
    }

    @Override
    DeletionMergeDiff<D> asMergeConflict() {
        super.asMergeConflict() as DeletionMergeDiff<D>
    }

    @Override
    String toString() {
        String str = "Deleted :: ${getFullyQualifiedPath()}"
        mergeModificationDiff ? "${str}\n    >> Modified :: ${mergeModificationDiff}" : str
    }

    @Override
    int compareTo(DeletionMergeDiff that) {
        this.deletedIdentifier <=> that.deletedIdentifier
    }
}
