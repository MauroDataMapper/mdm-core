/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import groovy.util.logging.Slf4j

/**
 * @since 27/04/2018
 */
@Slf4j
class PaginatedHibernateSearchResult<K> {

    int count
    List<K> results

    PaginatedHibernateSearchResult(List<K> results, int count) {
        this.count = count
        this.results = results
    }

    boolean isEmpty() {
        results.isEmpty()
    }

    void each(@ClosureParams(value = FromString, options = ['K']) Closure closure) {
        results.each closure
    }

    static <D> PaginatedHibernateSearchResult<D> paginateFullResultSet(List<D> fullResultSet, Map pagination) {

        if (!pagination) return new PaginatedHibernateSearchResult<D>(fullResultSet, fullResultSet.size())

        Integer max = pagination.max?.toInteger()
        Integer offsetAmount = pagination.offset?.toInteger()
        String sortKey = pagination.sort ?: 'label'
        String order = pagination.order ?: 'asc'

        List<D> sortedList = fullResultSet.sort {a, b ->
            if (order == 'asc') {
                a."$sortKey" <=> b."${sortKey}"
            } else {
                b."$sortKey" <=> a."${sortKey}"
            }
        }

        List<D> offsetList = offsetAmount == null ? sortedList : sortedList.drop(offsetAmount)
        List<D> maxList = max == null ? offsetList : offsetList.take(max)
        new PaginatedHibernateSearchResult<D>(maxList, fullResultSet.size())
    }
}
