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
package uk.ac.ox.softeng.maurodatamapper.gorm

import grails.gorm.PagedResultList

class InMemoryPagedResultList<E> extends PagedResultList<E> {

    Map<String, Object> pagination

    InMemoryPagedResultList(List<E> results, Map<String, Object> pagination) {
        super(null)
        totalCount = results.size()
        this.pagination = pagination

        Integer max = pagination.max?.toInteger() ?: 10
        Integer offset = pagination.offset?.toInteger() ?: 0
        resultList = results.subList(Math.min(totalCount, offset), Math.min(totalCount, offset + max))
    }

    @Override
    protected void initialize() {
        // no-op, already initialized
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        InMemoryPagedResultList that = (InMemoryPagedResultList) o

        pagination == that.pagination
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (pagination != null ? pagination.hashCode() : 0)
        result
    }
}
