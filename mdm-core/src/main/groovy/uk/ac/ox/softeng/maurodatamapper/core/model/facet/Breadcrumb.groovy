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
package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * @since 06/02/2020
 */
@SuppressFBWarnings('RANGE_ARRAY_INDEX')
class Breadcrumb {

    UUID id
    String domainType
    String label
    Boolean finalised

    Breadcrumb(UUID id, String domainType, String label, Boolean finalised) {
        this.id = id
        this.domainType = domainType
        this.label = label
        this.finalised = finalised
    }

    Breadcrumb(String info) {
        this(info.split(/\|/))
    }

    Breadcrumb(String[] list) {
        id = Utils.toUuid(list[0])
        domainType = list[1]
        label = list[2]
        finalised = list[3] == 'null' ? null : list[3].toBoolean()
    }

    List<String> toList() {
        finalised != null ? [id.toString(), domainType, label, finalised?.toString()] : [id.toString(), domainType, label, null]
    }

    @Override
    String toString() {
        toList().join('|')
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Breadcrumb that = (Breadcrumb) o

        if (domainType != that.domainType) return false
        if (finalised != that.finalised) return false
        if (id != that.id) return false
        if (label != that.label) return false

        return true
    }

    int hashCode() {
        int result
        result = id.hashCode()
        result = 31 * result + domainType.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + (finalised != null ? finalised.hashCode() : 0)
        return result
    }
}
