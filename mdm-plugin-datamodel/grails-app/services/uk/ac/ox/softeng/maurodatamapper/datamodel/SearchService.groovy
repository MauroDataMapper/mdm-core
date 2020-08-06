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

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.core.search.AbstractCatalogueItemSearchService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.search.searchparamfilter.DataModelTypeFilter
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult

class SearchService extends AbstractCatalogueItemSearchService {

    PaginatedLuceneResult<ModelItem> findAllByDataModelIdByLuceneSearch(UUID dataModelId, SearchParams searchParams, Map pagination = [:]) {
        findAllModelItemsByOwningIdsByLuceneSearch([dataModelId], searchParams, pagination)
    }

    PaginatedLuceneResult<ModelItem> findAllByDataClassIdByLuceneSearch(UUID dataClassId, SearchParams searchParams, Map pagination = [:]) {
        findAllModelItemsByOwningIdsByLuceneSearch([dataClassId], searchParams, pagination)
    }

    @Override
    List<Class<ModelItem>> getDomainsToSearch(SearchParams searchParams) {

        if (searchParams.domainTypes) {
            List<Class<ModelItem>> domainsToSearch = []

            if (DataClass.simpleName in searchParams.domainTypes) {
                domainsToSearch.add DataClass
            }
            if (DataElement.simpleName in searchParams.domainTypes) {
                domainsToSearch.add DataElement
            }
            if (ReferenceType.simpleName in searchParams.domainTypes) {
                domainsToSearch.add ReferenceType
            }
            if (EnumerationType.simpleName in searchParams.domainTypes) {
                domainsToSearch.add EnumerationType
            }
            if (PrimitiveType.simpleName in searchParams.domainTypes) {
                domainsToSearch.add PrimitiveType
            }
            if (EnumerationValue.simpleName in searchParams.domainTypes) {
                domainsToSearch.add EnumerationValue
            }

            return domainsToSearch
        }
        [DataClass, DataElement, ReferenceType, EnumerationType, PrimitiveType, EnumerationValue] as List<Class<ModelItem>>
    }

    @Override
    List<Class<SearchParamFilter>> getSearchParamFilters() {
        super.getSearchParamFilters() + [DataModelTypeFilter] as List<Class<SearchParamFilter>>
    }
}
