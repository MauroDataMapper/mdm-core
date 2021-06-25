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


import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.BiDirectionalDiff

import groovy.transform.CompileStatic

@CompileStatic
abstract class TriDirectionalDiff<T> extends BiDirectionalDiff<T> {

    protected String fullyQualifiedObjectPath
    private Boolean mergeConflict
    private T commonAncestor

    protected TriDirectionalDiff(Class<T> targetClass) {
        super(targetClass)
        mergeConflict = false
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        TriDirectionalDiff<T> diff = (TriDirectionalDiff<T>) o

        if (source != diff.source) return false
        if (target != diff.target) return false

        return true
    }

    TriDirectionalDiff<T> insideFullyQualifiedObjectPath(String fullyQualifiedObjectPath) {
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

    @Deprecated
    @Override
    T getRight() {
        super.getRight()
    }

    @Deprecated
    @Override
    void setRight(T right) {
        super.setRight(right)
    }

    @Deprecated
    @Override
    BiDirectionalDiff<T> leftHandSide(T lhs) {
        super.leftHandSide(lhs)
    }

    @Deprecated
    @Override
    BiDirectionalDiff<T> rightHandSide(T rhs) {
        super.rightHandSide(rhs)
    }

    @Deprecated
    @Override
    void setLeft(T left) {
        super.setLeft(left)
    }

    @Deprecated
    @Override
    T getLeft() {
        super.getLeft()
    }

    @Override
    String toString() {
        "${source} --> ${target} [${commonAncestor}]"
    }
}
