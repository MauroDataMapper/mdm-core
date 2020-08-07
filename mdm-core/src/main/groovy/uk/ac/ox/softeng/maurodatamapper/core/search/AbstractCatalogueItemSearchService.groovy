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

        allDomains.findAll {domainClass ->
            domainClass.simpleName in searchParams.domainTypes
        }
    }

    PaginatedLuceneResult<K> findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch(List<UUID> owningIds,
                                                                                  SearchParams searchParams,
                                                                                  Map pagination = [:]) {
        Closure additional = null

        Set<Class<SearchParamFilter>> searchParamFilters = getSearchParamFilters()

        searchParamFilters.each {f ->
            SearchParamFilter filter = f.getDeclaredConstructor().newInstance()
            if (filter.doesApply(searchParams)) {
                if (additional) {
                    additional <<= filter.getClosure(searchParams)
                } else {
                    additional = filter.getClosure(searchParams)
                }
            }
        }
        Set<Class<K>> domainsToSearch = getFilteredDomainsToSearch(searchParams)

        if (!domainsToSearch) {
            return new PaginatedLuceneResult<K>(new ArrayList<K>(), 0)
        }

        long start = System.currentTimeMillis()

        List<K> modelItems

        if (searchParams.labelOnly) {
            log.debug('Performing lucene label search')
            modelItems = performLabelSearch(domainsToSearch, owningIds, searchParams.searchTerm, additional)
        } else {
            log.debug('Performing lucene standard search')
            modelItems = performStandardSearch(domainsToSearch, owningIds, searchParams.searchTerm, additional)
        }

        PaginatedLuceneResult<K> results = PaginatedLuceneResult.paginateFullResultSet(modelItems, pagination)

        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}")
        results
    }

    @CompileDynamic
    protected List<K> performLabelSearch(Set<Class<K>> domainsToSearch, List<UUID> owningIds, String searchTerm,
                                         @DelegatesTo(HibernateSearchApi) Closure additional = null) {

        List<K> allFound = domainsToSearch.collect {domain ->
            domain.luceneLabelSearch(domain, searchTerm, owningIds, [:], additional).results
        }.flatten() as List<K>
        // Remove null entries and any which have an owning id, as we only want those inside the owners
        allFound.findAll {!(it.id in owningIds)}
    }

    @CompileDynamic
    protected List<K> performStandardSearch(Set<Class<K>> domainsToSearch, List<UUID> owningIds, String searchTerm,
                                            @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        List<K> allFound = domainsToSearch.collect {domain ->
            domain.luceneStandardSearch(domain, searchTerm, owningIds, [:], additional).results
        }.flatten() as List<K>
        // Remove null entries and any which have an owning id, as we only want those inside the owners
        allFound.findAll {!(it.id in owningIds)} as List<K>
    }
}
