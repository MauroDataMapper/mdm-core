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

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.AbstractCatalogueItemSearchService
import uk.ac.ox.softeng.maurodatamapper.core.search.CatalogueItemSearchDomainProvider
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class SearchService extends AbstractCatalogueItemSearchService<ModelItem> implements CatalogueItemSearchDomainProvider {

    PaginatedLuceneResult<ModelItem> findAllByTerminologyIdByLuceneSearch(UUID terminologyId, SearchParams searchParams, Map pagination = [:]) {
        findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch([terminologyId], searchParams, pagination)
    }

    @Override
    Set<Class<ModelItem>> getDomainsToSearch() {
        [Term]
    }

    @Override
    Set<Class<CatalogueItem>> getSearchableCatalogueItemDomains() {
        [Term, Terminology] as HashSet<Class<CatalogueItem>>
    }
}
