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
package uk.ac.ox.softeng.maurodatamapper.core.diff

import groovy.transform.CompileStatic

@CompileStatic
abstract class Diff<T> {

    T value

    Class<T> targetClass

    protected Diff(Class<T> targetClass) {
        this.targetClass = targetClass

    }

    abstract Integer getNumberOfDiffs()

    String getDiffType() {
        getClass().simpleName
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Diff<T> diff = (Diff<T>) o
        value == diff.value
    }

    boolean objectsAreIdentical() {
        !getNumberOfDiffs()
    }

    @Override
    int hashCode() {
        (value != null ? value.hashCode() : 0)
    }
}
