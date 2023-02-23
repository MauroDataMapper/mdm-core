/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.ExcludedIdsFilterFactory
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
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

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices


    PaginatedHibernateSearchResult<CatalogueItem> findAllByFolderIdByHibernateSearch(UUID folderId, SearchParams searchParams, Map pagination = [:]) {
        List<UUID> modelIds = getAllModelIdsInFolderId(folderId)
        findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch(modelIds, searchParams, false, pagination)
    }

    PaginatedHibernateSearchResult<CatalogueItem> findAllReadableByHibernateSearch(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                   SearchParams searchParams, Map pagination = [:]) {

        if (!modelServices) return new PaginatedHibernateSearchResult([], 0)
        Class<Model>[] classes = modelServices.collect {it.domainClass as Class<SecurableResource>}.toArray() as Class<Model>[]
        List<UUID> readableModelIds = userSecurityPolicyManager.listReadableSecuredResourceIds(classes)
        findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch(readableModelIds, searchParams, false, pagination)
    }

    PaginatedHibernateSearchResult<CatalogueItem> findAllReadableByHibernateSearchSortedByProximityToCatalogueItem(
        CatalogueItem catalogueItem,
        UserSecurityPolicyManager userSecurityPolicyManager,
        SearchParams searchParams,
        Map pagination = [:]) {

        Class<Model>[] classes = modelServices.collect {it.domainClass as Class<SecurableResource>}.toArray() as Class<Model>[]
        List<UUID> readableModelIds = userSecurityPolicyManager.listReadableSecuredResourceIds(classes)

        //distance('path_geopoint', catalogueItem.path.geoPoint)
        pagination.sort = 'distance'
        pagination.sortField = 'path_geopoint'
        pagination.center = catalogueItem.path.geoPoint
        pagination.remove('order')

        String searchTerm = searchParams.searchTerm ? searchParams.searchTerm : '*'
        if (!searchTerm.endsWith('*')) searchTerm = "${searchTerm}*"

        findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch(readableModelIds, searchParams, false, pagination) {
            simpleQueryString searchTerm, 'label_sort'
            filter(ExcludedIdsFilterFactory.createFilterPredicate(searchPredicateFactory, [catalogueItem.id]))
        }
    }

    @Override
    Set<Class<CatalogueItem>> getDomainsToSearch() {
        if (!catalogueItemSearchDomainProviders) return new HashSet<Class<CatalogueItem>>()
        List<Class<CatalogueItem>> allSearchableDomains = catalogueItemSearchDomainProviders.collectMany {provider ->
            provider.searchableCatalogueItemDomains
        }
        allSearchableDomains ? allSearchableDomains.toSet() : new HashSet<Class<CatalogueItem>>()
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

    CatalogueItem findCatalogueItem(String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemDomainType)}
        if (!service) throw new ApiBadRequestException('SS01', "No supporting service for ${catalogueItemDomainType}")
        service.get(catalogueItemId)
    }
}
