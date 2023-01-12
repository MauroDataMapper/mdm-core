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
package uk.ac.ox.softeng.maurodatamapper.core.search

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.HibernateSearch
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
//@GrailsCompileStatic
abstract class AbstractCatalogueItemSearchService<K extends CatalogueItem> {

    @Autowired
    PathService pathService

    @Autowired(required = false)
    Set<SearchParamFilter> searchParamFilters

    abstract Set<Class<K>> getDomainsToSearch()

    Set<Class<K>> getFilteredDomainsToSearch(SearchParams searchParams) {
        Set<Class<K>> allDomains = getDomainsToSearch()
        if (!searchParams.domainTypes) return allDomains

        allDomains.findAll {domainClass ->
            domainClass.simpleName in searchParams.domainTypes
        }
    }

    PaginatedHibernateSearchResult<K> findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch(List<UUID> owningIds,
                                                                                              SearchParams searchParams,
                                                                                              boolean removeOwningIds,
                                                                                              Map pagination = [:],
                                                                                              @DelegatesTo(HibernateSearchApi) Closure customSearch = null) {
        Closure additional = null

        int addtlClauseCount = 0
        if (searchParamFilters) {
            searchParamFilters.each {filter ->
                if (filter.doesApply(searchParams)) {
                    addtlClauseCount++
                    if (additional) {
                        additional <<= filter.getClosure(searchParams)
                    } else {
                        additional = filter.getClosure(searchParams)
                    }
                }
            }
        }
        Set<Class<K>> filteredDomainsToSearch = getFilteredDomainsToSearch(searchParams)

        if (!filteredDomainsToSearch) {
            return new PaginatedHibernateSearchResult<K>([], 0)
        }

        // Do an estimated increase to the clause count, this should make sure at least the number of owningIds and paths clauses dont cause an initial failure
        int currentMaxClauseCount = HibernateSearch.getCurrentMaxClauseCount()
        int estimatedClauseCount = (owningIds.size() * 2) + addtlClauseCount + 10
        if (currentMaxClauseCount <= estimatedClauseCount) {
            HibernateSearch.increaseMaxClauseCount(estimatedClauseCount)
        }

        long start = System.currentTimeMillis()
        PaginatedHibernateSearchResult<K> results

        // If only 1 domain to search then we can get HS to do all the pagination
        // This will be faster than doing it in memory
        // As geopoint distances in HS are in metres, for the time being we will do that in-memory
        if (filteredDomainsToSearch.size() == 1) {
            results = performSearch(owningIds, searchParams.searchTerm, searchParams.labelOnly,
                                    filteredDomainsToSearch.first(), pagination, additional, customSearch)
            if (removeOwningIds) {
                // Remove null entries and any which have an owning id, as we only want those inside the owners
                results.removeIf {it.id in owningIds}
            }
        } else {
            List<K> items = performSearch(owningIds, searchParams.searchTerm, searchParams.labelOnly,
                                          filteredDomainsToSearch, additional, customSearch)
            if (removeOwningIds) {
                // Remove null entries and any which have an owning id, as we only want those inside the owners
                items.removeIf {it.id in owningIds}
            }

            results = filteredDomainsToSearch.size() == 1 ?
                      items as PaginatedHibernateSearchResult<K> :
                      PaginatedHibernateSearchResult.paginateFullResultSet(items, pagination)
        }
        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}")
        results
    }

    List<K> performSearch(List<UUID> owningIds,
                          String searchTerm,
                          Boolean labelOnly,
                          Set<Class<K>> filteredDomainsToSearch,
                          @DelegatesTo(HibernateSearchApi) Closure additional,
                          @DelegatesTo(HibernateSearchApi) Closure customSearch) {
        if (customSearch) {
            log.debug('Performing hs custom search')
            return performCustomSearch(filteredDomainsToSearch, owningIds, additional, customSearch)
        } else if (labelOnly) {
            log.debug('Performing hs label search')
            return performLabelSearch(filteredDomainsToSearch, owningIds, searchTerm, additional)
        }

        log.debug('Performing hs standard search')
        performStandardSearch(filteredDomainsToSearch, owningIds, searchTerm, additional)
    }

    PaginatedHibernateSearchResult<K> performSearch(List<UUID> owningIds,
                                                    String searchTerm,
                                                    Boolean labelOnly,
                                                    Class<K> filteredDomainToSearch,
                                                    Map pagination,
                                                    @DelegatesTo(HibernateSearchApi) Closure additional,
                                                    @DelegatesTo(HibernateSearchApi) Closure customSearch) {
        List<Path> paths = pathService.findAllPathsForIds(owningIds)
        if (customSearch) {
            log.debug('Performing hs custom search')
            return performCustomSearch(filteredDomainToSearch, owningIds, paths, pagination, additional, customSearch)
        } else if (labelOnly) {
            log.debug('Performing hs label search')
            return performLabelSearch(filteredDomainToSearch, owningIds, paths, searchTerm, pagination, additional)
        }

        log.debug('Performing hs standard search')
        performStandardSearch(filteredDomainToSearch, owningIds, paths, searchTerm, pagination, additional)
    }

    protected List<K> performLabelSearch(Set<Class<K>> filteredDomainsToSearch, List<UUID> owningIds, String searchTerm,
                                         @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        List<Path> paths = pathService.findAllPathsForIds(owningIds)
        filteredDomainsToSearch.collect {domain ->
            performLabelSearch(domain, owningIds, paths, searchTerm, [:], additional).results
        }.flatten().findAll() as List<K>

    }

    protected List<K> performStandardSearch(Set<Class<K>> filteredDomainsToSearch, List<UUID> owningIds, String searchTerm,
                                            @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        List<Path> paths = pathService.findAllPathsForIds(owningIds)
        filteredDomainsToSearch.collect {domain ->
            performStandardSearch(domain, owningIds, paths, searchTerm, [:], additional).results
        }.flatten().findAll() as List<K>
    }

    protected List<K> performCustomSearch(Set<Class<K>> filteredDomainsToSearch, List<UUID> owningIds,
                                          @DelegatesTo(HibernateSearchApi) Closure additional,
                                          @DelegatesTo(HibernateSearchApi) Closure customSearch) {
        List<Path> paths = pathService.findAllPathsForIds(owningIds)
        filteredDomainsToSearch.collect {domain ->
            performCustomSearch(domain, owningIds, paths, [:], additional, customSearch).results
        }.flatten().findAll() as List<K>
    }

    protected PaginatedHibernateSearchResult<K> performLabelSearch(Class<K> domainToSearch, List<UUID> owningIds, List<Path> paths, String searchTerm,
                                                                   Map pagination, @DelegatesTo(HibernateSearchApi) Closure additional) {

        log.debug('Domain searching {}', domainToSearch)
        domainToSearch.labelHibernateSearch(domainToSearch, searchTerm, owningIds, paths, pagination, additional)

    }

    protected PaginatedHibernateSearchResult<K> performStandardSearch(Class<K> domainToSearch, List<UUID> owningIds, List<Path> paths, String searchTerm,
                                                                      Map pagination, @DelegatesTo(HibernateSearchApi) Closure additional) {
        log.debug('Domain searching {}', domainToSearch)
        domainToSearch.standardHibernateSearch(domainToSearch, searchTerm, owningIds, paths, pagination, additional)
    }

    protected PaginatedHibernateSearchResult<K> performCustomSearch(Class<K> domainToSearch, List<UUID> owningIds, List<Path> paths, Map pagination,
                                                                    @DelegatesTo(HibernateSearchApi) Closure additional,
                                                                    @DelegatesTo(HibernateSearchApi) Closure customSearch) {
        log.debug('Domain searching {}', domainToSearch)
        domainToSearch.customHibernateSearch(domainToSearch, owningIds, paths, pagination, additional, customSearch)
    }
}
