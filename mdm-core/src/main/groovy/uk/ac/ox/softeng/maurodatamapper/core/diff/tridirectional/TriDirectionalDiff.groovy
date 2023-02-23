/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.BiDirectionalDiff
import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.transform.CompileStatic

@CompileStatic
abstract class TriDirectionalDiff<T> extends BiDirectionalDiff<T> {

    protected Path fullyQualifiedObjectPath
    private Boolean mergeConflict
    private T commonAncestor

    protected TriDirectionalDiff(Class<T> targetClass) {
        super(targetClass)
        mergeConflict = false
    }

    abstract Path getFullyQualifiedPath()

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        TriDirectionalDiff that = (TriDirectionalDiff) o

        if (commonAncestor != that.commonAncestor) return false
        if (fullyQualifiedObjectPath != that.fullyQualifiedObjectPath) return false
        mergeConflict == that.mergeConflict
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (fullyQualifiedObjectPath != null ? fullyQualifiedObjectPath.hashCode() : 0)
        result = 31 * result + (mergeConflict != null ? mergeConflict.hashCode() : 0)
        result = 31 * result + (commonAncestor != null ? commonAncestor.hashCode() : 0)
        result
    }

    TriDirectionalDiff<T> insideFullyQualifiedObjectPath(Path fullyQualifiedObjectPath) {
        this.fullyQualifiedObjectPath = fullyQualifiedObjectPath
        this
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    TriDirectionalDiff<T> withSource(T source) {
        this.left = source
        this
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    TriDirectionalDiff<T> withTarget(T target) {
        this.right = target
        this
    }

    TriDirectionalDiff<T> withCommonAncestor(T ca) {
        this.commonAncestor = ca
        this
    }

    TriDirectionalDiff<T> asMergeConflict() {
        this.mergeConflict = true
        this
    }

    Boolean isMergeConflict() {
        mergeConflict
    }

    T getCommonAncestor() {
        commonAncestor
    }

    T getTarget() {
        super.getRight()
    }

    T getSource() {
        super.getLeft()
    }

    Path getFullyQualifiedObjectPath() {
        fullyQualifiedObjectPath
    }

    @Override
    String toString() {
        "${source} --> ${target} [${commonAncestor}]"
    }
}
