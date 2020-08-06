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
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.ParameterizedType

@Slf4j
@GrailsCompileStatic
class SearchService extends AbstractCatalogueItemSearchService<CatalogueItem> {

    @Autowired(required = false)
    List<ContainerService> containerServices

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    List<CatalogueItemSearchDomainProvider> catalogueItemSearchDomainProviders

    PaginatedLuceneResult<CatalogueItem> findAllByFolderIdByLuceneSearch(UUID folderId, SearchParams searchParams, Map pagination = [:]) {
        findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch([folderId], searchParams, pagination)
    }

    PaginatedLuceneResult<CatalogueItem> findAllReadableByLuceneSearch(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                       SearchParams searchParams, Map pagination = [:]) {

        if (!containerServices && !modelServices) return new PaginatedLuceneResult<CatalogueItem>([], 0)

        List<UUID> readableIds = getAllReadableContainerIds(userSecurityPolicyManager) + getAllReadableModelIds(userSecurityPolicyManager)

        if (!readableIds) return new PaginatedLuceneResult<CatalogueItem>([], 0)

        findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch(readableIds, searchParams, pagination)
    }

    @Override
    Set<Class<CatalogueItem>> getDomainsToSearch() {
        catalogueItemSearchDomainProviders.collectMany {it.searchableCatalogueItemDomains}.toSet()
    }

    @Override
    Set<Class<SearchParamFilter>> getSearchParamFilters() {
        catalogueItemSearchDomainProviders.collectMany {it.searchParamFilters}.toSet()
    }

    List<UUID> getAllReadableContainerIds(UserSecurityPolicyManager userSecurityPolicyManager) {
        containerServices.collect {service ->
            ParameterizedType parameterizedType = (ParameterizedType) getClass().genericInterfaces.find {genericInterface ->
                genericInterface instanceof ParameterizedType &&
                ContainerService.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
            }

            Class<Container> containerClass = parameterizedType?.actualTypeArguments[0] as Class<Container>
            userSecurityPolicyManager.listReadableSecuredResourceIds(containerClass)
        }.findAll() as List<UUID>
    }

    List<UUID> getAllReadableModelIds(UserSecurityPolicyManager userSecurityPolicyManager) {
        modelServices.collect {service ->
            ParameterizedType parameterizedType = (ParameterizedType) getClass().genericInterfaces.find {genericInterface ->
                genericInterface instanceof ParameterizedType &&
                ModelService.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
            }

            Class<Model> modelClass = parameterizedType?.actualTypeArguments[0] as Class<Model>
            userSecurityPolicyManager.listReadableSecuredResourceIds(modelClass)
        }.findAll() as List<UUID>
    }
}
