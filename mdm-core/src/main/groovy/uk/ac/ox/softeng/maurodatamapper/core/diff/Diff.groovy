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

abstract class Diff<T> {

    T left
    T right

    Diff<T> leftHandSide(T lhs) {
        this.left = lhs
        this
    }

    Diff<T> rightHandSide(T rhs) {
        this.right = rhs
        this
    }

    boolean objectsAreIdentical() {
        !getNumberOfDiffs()
    }

    abstract Integer getNumberOfDiffs()
}
