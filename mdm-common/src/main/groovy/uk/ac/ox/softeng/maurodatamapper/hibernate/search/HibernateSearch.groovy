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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdPathSecureFilterFactory
import uk.ac.ox.softeng.maurodatamapper.path.Path

import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.util.logging.Slf4j
import org.apache.lucene.search.BooleanQuery
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher

/**
 * @since 19/03/2020
 */
@Slf4j
class HibernateSearch {

    @SuppressWarnings('UnnecessaryQualifiedReference')
    static <T> PaginatedHibernateSearchResult<T> securedPaginatedList(Class<T> clazz,
                                                                      List<UUID> allowedIds,
                                                                      List<Path> allowedPaths,
                                                                      Map pagination,
                                                                      @DelegatesTo(HibernateSearchApi) Closure... closures) {
        if (!allowedIds) return new PaginatedHibernateSearchResult<T>([], 0)
        paginatedList(clazz, pagination) {

            for (Closure closure : closures.findAll()) {
                closure.setResolveStrategy(Closure.DELEGATE_FIRST)
                closure.setDelegate(delegate)
                closure.call()
            }

            filter IdPathSecureFilterFactory.createFilterPredicate(searchPredicateFactory, allowedIds, allowedPaths)
        }
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    static <T> PaginatedHibernateSearchResult<T> paginatedList(Class<T> clazz, Map pagination, @DelegatesTo(HibernateSearchApi) Closure closure) {

        if (!ClassPropertyFetcher.forClass(clazz).isReadableProperty('search')) {
            throw new ApiInternalException('L01', "Class ${clazz} is not configured for HS searching")
        }

        Integer max = pagination.max?.toInteger()
        Integer offsetAmount = pagination.offset?.toInteger()
        String sortKey = pagination.sort
        String order = pagination.order ?: 'asc'

        boolean distanceSort = sortKey && sortKey == 'distance'

        Closure paginatedClosure = HibernateSearchApi.defineSearchQuery {
            closure.setResolveStrategy(Closure.DELEGATE_FIRST)
            closure.setDelegate(delegate)
            closure.call()

            if (!distanceSort && max != null) maxResults max
            if (!distanceSort && offsetAmount != null) offset offsetAmount
            if (!distanceSort && sortKey) sort "${sortKey}_sort", order
        }

        try {
            if (distanceSort) {
                return PaginatedHibernateSearchResult.paginateFullResultSet(clazz.search().list(paginatedClosure), pagination)
            }
            return new PaginatedHibernateSearchResult<>(clazz.search().list(paginatedClosure), clazz.search().count(paginatedClosure))
        } catch (RuntimeException ex) {
            handleSearchException(clazz, closure, ex)
        }
    }

    static <T> PaginatedHibernateSearchResult<T> handleSearchException(Class<T> clazz, Closure closure, RuntimeException ex) {
        if (isTooManyClausesException(ex)) {
            log.warn('Initial search failed with {} exception and message [{}]', ex.class, ex.message)
            int oldQueries = getCurrentMaxClauseCount()
            int newQueries = oldQueries * 2
            increaseMaxClauseCount(newQueries)
            return paginatedList(clazz, [:], closure) as PaginatedHibernateSearchResult<T>
        }
        log.error('Search failed for unhandled reason', ex)
        throw ex
    }

    static boolean isTooManyClausesException(Throwable throwable) {
        if (!throwable) return null
        if (throwable instanceof BooleanQuery.TooManyClauses) return true
        if (!throwable.cause) return false
        isTooManyClausesException(throwable.cause)
    }

    static void increaseMaxClauseCount(int maxClauseCount) {
        log.info('Increasing org.apache.lucene.maxClauseCount to {}', maxClauseCount)
        System.setProperty('org.apache.lucene.maxClauseCount', Integer.toString(maxClauseCount))
        BooleanQuery.setMaxClauseCount(maxClauseCount)
    }

    static int getCurrentMaxClauseCount() {
        String defaultQueries = Integer.toString(BooleanQuery.getMaxClauseCount())
        Integer.parseInt(System.getProperty('org.apache.lucene.maxClauseCount', defaultQueries))
    }
}
