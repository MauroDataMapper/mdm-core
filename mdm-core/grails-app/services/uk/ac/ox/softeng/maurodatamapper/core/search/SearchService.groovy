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
package uk.ac.ox.softeng.maurodatamapper.core.search

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.compiler.GrailsCompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.ParameterizedType

@Slf4j
@GrailsCompileStatic
class SearchService extends AbstractCatalogueItemSearchService<CatalogueItem> {

    FolderService folderService

    @Autowired(required = false)
    List<ContainerService> containerServices

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    List<CatalogueItemSearchDomainProvider> catalogueItemSearchDomainProviders

    PaginatedHibernateSearchResult<CatalogueItem> findAllByFolderIdByLuceneSearch(UUID folderId, SearchParams searchParams, Map pagination = [:]) {
        List<UUID> modelIds = getAllModelIdsInFolderId(folderId)
        findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch(modelIds, searchParams, false, pagination)
    }

    PaginatedHibernateSearchResult<CatalogueItem> findAllReadableByLuceneSearch(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                SearchParams searchParams, Map pagination = [:]) {

        if (!modelServices) return new PaginatedHibernateSearchResult<CatalogueItem>([], 0)

        List<UUID> readableFolderIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)

        if (!readableFolderIds) return new PaginatedHibernateSearchResult<CatalogueItem>([], 0)

        List<UUID> readableModelIds = readableFolderIds.collectMany {containerId -> getAllModelIdsInFolderId(containerId)}

        findAllCatalogueItemsOfTypeByOwningIdsByLuceneSearch(readableModelIds, searchParams, false, pagination)
    }

    @Override
    Set<Class<CatalogueItem>> getDomainsToSearch() {
        if (!catalogueItemSearchDomainProviders) return new HashSet<Class<CatalogueItem>>()
        List<Class<CatalogueItem>> allSearchableDomains = catalogueItemSearchDomainProviders.collectMany {provider ->
            provider.searchableCatalogueItemDomains
        }
        allSearchableDomains ? allSearchableDomains.toSet() : new HashSet<Class<CatalogueItem>>()
    }

    @Override
    Set<Class<SearchParamFilter>> getSearchParamFilters() {
        if (!catalogueItemSearchDomainProviders) return new HashSet<Class<SearchParamFilter>>()
        List<Class<SearchParamFilter>> allSearchParamFilters = catalogueItemSearchDomainProviders.collectMany {provider ->
            provider.searchParamFilters
        }
        allSearchParamFilters ? allSearchParamFilters.toSet() : new HashSet<Class<SearchParamFilter>>()
    }

    protected List<UUID> getAllReadableContainerIds(UserSecurityPolicyManager userSecurityPolicyManager) {
        containerServices.collect {service ->
            ParameterizedType parameterizedType = (ParameterizedType) service.getClass().genericInterfaces.find {genericInterface ->
                genericInterface instanceof ParameterizedType &&
                ContainerService.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
            }

            Class<Container> containerClass = parameterizedType?.actualTypeArguments[0] as Class<Container>
            userSecurityPolicyManager.listReadableSecuredResourceIds(containerClass)
        }.findAll() as List<UUID>
    }

    protected List<UUID> getAllReadableModelIds(UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!modelServices) return new ArrayList<UUID>()
        modelServices.collect {service ->
            ParameterizedType parameterizedType = (ParameterizedType) service.getClass().genericInterfaces.find {genericInterface ->
                genericInterface instanceof ParameterizedType &&
                ModelService.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
            }

            Class<Model> modelClass = parameterizedType?.actualTypeArguments[0] as Class<Model>
            userSecurityPolicyManager.listReadableSecuredResourceIds(modelClass)
        }.findAll() as List<UUID>
    }

    protected List<UUID> getAllModelIdsInFolderId(UUID folderId) {
        if (!modelServices) return new ArrayList<UUID>()
        List<UUID> containedFolderIds = folderService.findAllContainersInside(folderId).collect {it.id}
        containedFolderIds.add(folderId)
        modelServices.collectMany {service ->
            containedFolderIds.collectMany {fId ->
                (service.findAllByContainerId(fId) as List<Model>).collect {model -> model.id}
            }
        }
    }
}
