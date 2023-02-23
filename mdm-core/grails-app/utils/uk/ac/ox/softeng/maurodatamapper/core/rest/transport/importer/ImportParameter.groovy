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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer

import io.micronaut.core.order.Ordered

/**
 * @since 22/01/2018
 */
class ImportParameter implements Comparable<ImportParameter>, Ordered {

    String name
    String type
    String description
    String displayName
    boolean optional = false
    int order

    @Override
    int compareTo(ImportParameter that) {
        int res = this.type == Boolean.simpleName && that.type != Boolean.simpleName ? 1 : 0
        if (res == 0) res = that.type == Boolean.simpleName && this.type != Boolean.simpleName ? -1 : 0
        if (res == 0) res = this.optional <=> that.optional
        if (res == 0) res = this.order <=> that.order
        if (res == 0) res = this.displayName <=> that.displayName
        res
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ImportParameter that = (ImportParameter) o

        name == that.name
    }

    int hashCode() {
        name != null ? name.hashCode() : 0
    }

    String toString() {
        "$name:$order"
    }
}
