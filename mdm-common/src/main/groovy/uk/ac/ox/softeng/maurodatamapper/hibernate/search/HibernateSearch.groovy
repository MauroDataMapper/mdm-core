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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdPathSecureFilterFactory

import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.util.logging.Slf4j
import org.apache.lucene.search.BooleanQuery
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.hibernate.search.util.common.SearchException

/**
 * @since 19/03/2020
 */
@Slf4j
class HibernateSearch {

    @SuppressWarnings('UnnecessaryQualifiedReference')
    static <T> PaginatedHibernateSearchResult<T> securedPaginatedList(Class<T> clazz,
                                                                      List<UUID> allowedIds,
                                                                      Map pagination,
                                                                      @DelegatesTo(HibernateSearchApi) Closure... closures) {
        if (!allowedIds) return new PaginatedHibernateSearchResult<T>([], 0)
        paginatedList(clazz, pagination) {

            for (Closure closure : closures.findAll()) {
                closure.setResolveStrategy(Closure.DELEGATE_FIRST)
                closure.setDelegate(delegate)
                closure.call()
            }

            filter IdPathSecureFilterFactory.createFilterPredicate(searchPredicateFactory, allowedIds)
        }
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    static <T> PaginatedHibernateSearchResult<T> paginatedList(Class<T> clazz, Map pagination, @DelegatesTo(HibernateSearchApi) Closure closure) {

        if (!ClassPropertyFetcher.forClass(clazz).isReadableProperty('search')) {
            throw new ApiInternalException('L01', "Class ${clazz} is not configured for lucene searching")
        }

        Integer max = pagination.max?.toInteger()
        Integer offsetAmount = pagination.offset?.toInteger()
        String sortKey = pagination.sort
        String order = pagination.order ?: 'asc'

        Closure paginatedClosure = HibernateSearchApi.defineSearchQuery {
            closure.setResolveStrategy(Closure.DELEGATE_FIRST)
            closure.setDelegate(delegate)
            closure.call()

            if (max != null) maxResults max
            if (offsetAmount != null) offset offsetAmount

            if (sortKey) sort "${sortKey}_sort", order
        }

        try {
            return new PaginatedHibernateSearchResult<>(clazz.search().list(paginatedClosure), clazz.search().count(paginatedClosure))
        } catch (SearchException ex) {
            handleSearchException(clazz, closure, ex)
        }
    }

    static <T> PaginatedHibernateSearchResult<T> handleSearchException(Class<T> clazz, Closure closure, SearchException ex) {
        if (isTooManyClausesException(ex)) {
            log.warn('Initial search failed with {} exception and message [{}]', ex.class, ex.message)
            String defaultQueries = Integer.toString(BooleanQuery.getMaxClauseCount());
            int oldQueries = Integer.parseInt(System.getProperty("org.apache.lucene.maxClauseCount", defaultQueries))
            int newQueries = oldQueries * 2
            log.info('Too many clauses for query set to {}, increasing org.apache.lucene.maxClauseCount to {}', oldQueries, newQueries)
            System.setProperty("org.apache.lucene.maxClauseCount", Integer.toString(newQueries))
            BooleanQuery.setMaxClauseCount(newQueries)
            return paginatedList(clazz, [:], closure) as PaginatedHibernateSearchResult<T>
        } else {
            log.error('Search failed for unhandled reason', ex)
            throw ex
        }
    }

    static boolean isTooManyClausesException(Throwable throwable) {
        if (!throwable) return null
        if (throwable instanceof BooleanQuery.TooManyClauses) return true
        if (!throwable.cause) return false
        return isTooManyClausesException(throwable.cause)
    }

    @Deprecated
    static Closure defineAdditionalLuceneQuery(@DelegatesTo(HibernateSearchApi) closure) {
        closure
    }
}
