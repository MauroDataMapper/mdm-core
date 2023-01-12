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
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult

class SearchController implements ResourcelessMdmController {

    static responseFormats = ['json', 'xml']

    SearchService mdmCoreSearchService

    def search() {

        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params, 'label')

        PaginatedHibernateSearchResult<CatalogueItem> results = mdmCoreSearchService.findAllReadableByHibernateSearch(currentUserSecurityPolicyManager, searchParams, params)
        respond results
    }

    def prefixLabelSearch() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params, null)

        CatalogueItem catalogueItem = mdmCoreSearchService.findCatalogueItem(params.catalogueItemDomainType, params.catalogueItemId)
        if (!catalogueItem) return notFound(params.catalogueItemClass, params.catalogueItemId)

        PaginatedHibernateSearchResult<CatalogueItem> results = mdmCoreSearchService.findAllReadableByHibernateSearchSortedByProximityToCatalogueItem(
            catalogueItem,
            currentUserSecurityPolicyManager, searchParams, params)
        respond results
    }
}
