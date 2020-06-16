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
package uk.ac.ox.softeng.maurodatamapper.datamodel


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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.search.searchparamfilter.DataModelTypeFilter
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.plugins.hibernate.search.HibernateSearchApi

class SearchService {

    PaginatedLuceneResult<ModelItem> findAllByDataClassIdByLuceneSearch(UUID dataClassId, SearchParams searchParams,
                                                                        Map pagination = [:]) {
        findAllByOwningIdsByLuceneSearch([dataClassId], searchParams, pagination)
    }

    PaginatedLuceneResult<ModelItem> findAllByDataModelIdByLuceneSearch(UUID dataModelId, SearchParams searchParams,
                                                                        Map pagination = [:]) {
        findAllByOwningIdsByLuceneSearch([dataModelId], searchParams, pagination)
    }

    PaginatedLuceneResult<ModelItem> findAllByOwningIdsByLuceneSearch(List<UUID> owningIds, SearchParams searchParams,
                                                                      Map pagination = [:]) {

        Closure additional = null

        List<Class<SearchParamFilter>> searchParamFilters = [
            DataModelTypeFilter,
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
            if (DataClass.simpleName in searchParams.domainTypes) {
                domainsToSearch << DataClass
            }
            if (DataElement.simpleName in searchParams.domainTypes) {
                domainsToSearch << DataElement
            }
            if (ReferenceType.simpleName in searchParams.domainTypes) {
                domainsToSearch << ReferenceType
            }
            if (EnumerationType.simpleName in searchParams.domainTypes) {
                domainsToSearch << EnumerationType
            }
            if (PrimitiveType.simpleName in searchParams.domainTypes) {
                domainsToSearch << PrimitiveType
            }
            if (EnumerationValue.simpleName in searchParams.domainTypes) {
                domainsToSearch << EnumerationValue
            }

        } else {
            domainsToSearch = [DataClass, DataElement, ReferenceType, EnumerationType, PrimitiveType, EnumerationValue]
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
