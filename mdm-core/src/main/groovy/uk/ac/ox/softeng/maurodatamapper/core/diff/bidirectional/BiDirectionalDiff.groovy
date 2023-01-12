/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import groovy.transform.CompileStatic

@CompileStatic
abstract class BiDirectionalDiff<B> extends Diff<B> {

    B right

    protected BiDirectionalDiff(Class<B> targetClass) {
        super(targetClass)
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        BiDirectionalDiff<B> diff = (BiDirectionalDiff<B>) o

        if (left != diff.left) return false
        right == diff.right
    }

    BiDirectionalDiff<B> leftHandSide(B lhs) {
        this.left = lhs
        this
    }

    BiDirectionalDiff<B> rightHandSide(B rhs) {
        this.right = rhs
        this
    }

    void setLeft(B left) {
        this.value = left
    }

    B getLeft() {
        this.value
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (right != null ? right.hashCode() : 0)
        result
    }
}
