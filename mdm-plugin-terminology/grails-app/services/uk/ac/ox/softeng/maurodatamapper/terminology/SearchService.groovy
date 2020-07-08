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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifierFilterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifiersFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.plugins.hibernate.search.HibernateSearchApi

class SearchService {

    PaginatedLuceneResult<ModelItem> findAllByTerminologyIdByLuceneSearch(UUID terminologyId, SearchParams searchParams,
                                                                          Map pagination = [:]) {
        findAllByOwningIdsByLuceneSearch([terminologyId], searchParams, pagination)
    }

    PaginatedLuceneResult<ModelItem> findAllByOwningIdsByLuceneSearch(List<UUID> owningIds, SearchParams searchParams,
                                                                      Map pagination = [:]) {

        Closure additional = null

        List<Class<SearchParamFilter>> searchParamFilters = [
            UpdatedBeforeFilter,
            UpdatedAfterFilter,
            CreatedBeforeFilter,
            CreatedAfterFilter,
            ClassifiersFilter,
            ClassifierFilterFilter
        ]

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
        List<Class<ModelItem>> domainsToSearch = []

        if (searchParams.domainTypes) {
            if (Term.simpleName in searchParams.domainTypes) {
                domainsToSearch << Term
            }
            //            if (TermRelationship.simpleName in searchParams.domainTypes) {
            //                domainsToSearch << DataElement
            //            }
            //            if (TermRelationshipType.simpleName in searchParams.domainTypes) {
            //                domainsToSearch << ReferenceType
            //            }
        } else {
            domainsToSearch = [Term /*, TermRelationship, TermRelationshipType*/]
        }

        if (!domainsToSearch) {
            throw new ApiBadRequestException('SSXX', 'Owning IDs search attempted with filtered domains provided but no domains match this search ' +
                                                     'service')
        }

        long start = System.currentTimeMillis()

        List<ModelItem> modelItems

        if (searchParams.labelOnly) {
            log.debug('Performing lucene label search')
            modelItems = performLabelSearch(domainsToSearch, owningIds, searchParams.searchTerm, additional)
        } else {
            log.debug('Performing lucene standard search')
            modelItems = performStandardSearch(domainsToSearch, owningIds, searchParams.searchTerm, additional)
        }

        PaginatedLuceneResult<ModelItem> results = PaginatedLuceneResult.paginateFullResultSet(modelItems, pagination)

        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}")
        results
    }

    private List<ModelItem> performLabelSearch(List<Class<ModelItem>> domainsToSearch, List<UUID> allowedIds, String searchTerm,
                                               @DelegatesTo(HibernateSearchApi) Closure additional = null) {

        domainsToSearch.collect {domain ->
            domain.luceneLabelSearch(domain, searchTerm, allowedIds, [:], additional).results
        }.flatten().findAll()
    }

    private List<ModelItem> performStandardSearch(List<Class<ModelItem>> domainsToSearch, List<UUID> allowedIds, String searchTerm,
                                                  @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        domainsToSearch.collect {domain ->
            domain.luceneStandardSearch(domain, searchTerm, allowedIds, [:], additional).results
        }.flatten().findAll()
    }
}
