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
package uk.ac.ox.softeng.maurodatamapper.gorm

import grails.gorm.PagedResultList

class PaginatedResultList<E> extends PagedResultList<E> {

    Map<String, Object> pagination

    PaginatedResultList(List<E> results, Map<String, Object> pagination) {
        super(null)
        totalCount = results.size()
        this.pagination = pagination

        Integer max = pagination.max?: 10
        Integer offset = pagination.offset?: 0
        resultList = results.subList(Math.min(totalCount, offset), Math.min(totalCount, offset + max))
    }

    @Override
    protected void initialize() {
        // no-op, already initialized
    }
}
