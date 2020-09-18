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
package uk.ac.ox.softeng.maurodatamapper.core.search


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifierFilterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifiersFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.compiler.GrailsCompileStatic
import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

@Slf4j
@GrailsCompileStatic
abstract class AbstractCatalogueItemSearchService<K extends CatalogueItem> {

    abstract Set<Class<K>> getDomainsToSearch()

    Set<Class<SearchParamFilter>> getSearchParamFilters() {
        [
            UpdatedBeforeFilter,
            UpdatedAfterFilter,
            CreatedBeforeFilter,
            CreatedAfterFilter,
            ClassifiersFilter,
            ClassifierFilterFilter
        ] as HashSet<Class<SearchParamFilter>>
    }

    Set<Class<K>> getFilteredDomainsToSearch(SearchParams searchParams) {
        Set<Class<K>> allDomains = getDomainsToSearch()
        if (!searchParams.domainTypes) return allDomains

        allDomains.findAll { domainClass ->
            domainClass.simpleName in searchParams.domainTypes
        }
    }

    PaginatedLuceneResult<K> findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch(List<UUID> owningIds,
                                                                                  SearchParams searchParams,
                                                                                  boolean removeOwningIds,
                                                                                  Map pagination = [:],
                                                                                  @DelegatesTo(HibernateSearchApi) Closure customSearch = null) {
        Closure additional = null

        Set<Class<SearchParamFilter>> searchParamFilters = getSearchParamFilters()

        searchParamFilters.each { f ->
            SearchParamFilter filter = f.getDeclaredConstructor().newInstance()
            if (filter.doesApply(searchParams)) {
                if (additional) {
                    additional <<= filter.getClosure(searchParams)
                } else {
                    additional = filter.getClosure(searchParams)
                }
            }
        }
        Set<Class<K>> filteredDomainsToSearch = getFilteredDomainsToSearch(searchParams)

        if (!filteredDomainsToSearch) {
            return new PaginatedLuceneResult<K>(new ArrayList<K>(), 0)
        }

        long start = System.currentTimeMillis()

        List<K> items = performSearch(owningIds, searchParams.searchTerm, searchParams.labelOnly,
                                      filteredDomainsToSearch, additional, customSearch)

        if (removeOwningIds) {
            // Remove null entries and any which have an owning id, as we only want those inside the owners
            items = items.findAll { !(it.id in owningIds) }
        }

        PaginatedLuceneResult<K> results = PaginatedLuceneResult.paginateFullResultSet(items, pagination)

        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}")
        results
    }

    protected List<K> performSearch(List<UUID> owningIds,
                                    String searchTerm,
                                    Boolean labelOnly,
                                    Set<Class<K>> filteredDomainsToSearch,
                                    @DelegatesTo(HibernateSearchApi) Closure additional,
                                    @DelegatesTo(HibernateSearchApi) Closure customSearch) {
        if (customSearch) {
            log.debug('Performing lucene custom search')
            return performCustomSearch(filteredDomainsToSearch, owningIds, additional, customSearch)
        } else if (labelOnly) {
            log.debug('Performing lucene label search')
            return performLabelSearch(filteredDomainsToSearch, owningIds, searchTerm, additional)
        }

        log.debug('Performing lucene standard search')
        return performStandardSearch(filteredDomainsToSearch, owningIds, searchTerm, additional)
    }

    @CompileDynamic
    protected List<K> performLabelSearch(Set<Class<K>> domainsToSearch, List<UUID> owningIds, String searchTerm,
                                         @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        domainsToSearch.collect { domain ->
            log.debug('Domain searching {}', domain)
            domain.luceneLabelSearch(domain, searchTerm, owningIds, [:], additional).results
        }.flatten().findAll() as List<K>

    }

    @CompileDynamic
    protected List<K> performStandardSearch(Set<Class<K>> domainsToSearch, List<UUID> owningIds, String searchTerm,
                                            @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        domainsToSearch.collect { domain ->
            log.debug('Domain searching {}', domain)
            domain.luceneStandardSearch(domain, searchTerm, owningIds, [:], additional).results
        }.flatten().findAll() as List<K>
    }

    @CompileDynamic
    protected List<K> performCustomSearch(Set<Class<K>> domainsToSearch, List<UUID> owningIds,
                                          @DelegatesTo(HibernateSearchApi) Closure additional,
                                          @DelegatesTo(HibernateSearchApi) Closure customSearch) {
        domainsToSearch.collect { domain ->
            log.debug('Domain searching {}', domain)
            domain.luceneCustomSearch(domain, owningIds, [:], additional, customSearch).results
        }.flatten().findAll() as List<K>
    }
}
