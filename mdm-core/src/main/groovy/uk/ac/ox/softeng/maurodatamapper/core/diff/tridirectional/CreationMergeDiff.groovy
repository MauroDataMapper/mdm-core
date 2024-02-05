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

import groovy.transform.CompileStatic

@CompileStatic
class CreationMergeDiff<C extends Diffable> extends TriDirectionalDiff<C> implements Comparable<CreationMergeDiff> {

    CreationMergeDiff(Class<C> targetClass) {
        super(targetClass)
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    C getCreated() {
        super.getValue() as C
    }

    String getCreatedIdentifier() {
        value.getDiffIdentifier(fullyQualifiedObjectPath.toString())
    }

    Path getFullyQualifiedPath() {
        fullyQualifiedObjectPath.resolve(created.path)
    }

    boolean isSourceModificationAndTargetDeletion() {
        commonAncestor != null
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    CreationMergeDiff<C> whichCreated(C object) {
        withSource(object) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> insideFullyQualifiedObjectPath(Path fullyQualifiedObjectPath) {
        super.insideFullyQualifiedObjectPath(fullyQualifiedObjectPath) as CreationMergeDiff<C>
    }

    CreationMergeDiff<C> withCommonAncestor(C ca) {
        super.withCommonAncestor(ca) as CreationMergeDiff<C>
    }

    @Override
    CreationMergeDiff<C> asMergeConflict() {
        super.asMergeConflict() as CreationMergeDiff<C>
    }

    @Override
    String toString() {
        "Created :: ${createdIdentifier}"
    }

    @Override
    int compareTo(CreationMergeDiff that) {
        this.createdIdentifier <=> that.createdIdentifier
    }
}
