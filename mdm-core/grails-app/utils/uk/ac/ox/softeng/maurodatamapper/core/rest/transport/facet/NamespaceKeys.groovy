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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.facet

/**
 * @since 06/10/2017
 */
class NamespaceKeys implements Comparable<NamespaceKeys> {

    String namespace
    Boolean editable
    Collection<String> keys
    Boolean defaultNamespace = false

    @Override
    int compareTo(NamespaceKeys that) {
        this.namespace <=> that.namespace
    }


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        NamespaceKeys that = (NamespaceKeys) o

        if (namespace != that.namespace) return false

        return true
    }
}
