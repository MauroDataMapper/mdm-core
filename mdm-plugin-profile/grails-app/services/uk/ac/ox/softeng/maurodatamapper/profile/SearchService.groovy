/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.AbstractCatalogueItemSearchService
import uk.ac.ox.softeng.maurodatamapper.core.search.SearchService as CoreSearchService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.SearchService as DataModelSearchService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.gorm.InMemoryPagedResultList
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.beans.factory.annotation.Autowired

import java.util.function.Predicate

@Slf4j
class SearchService extends AbstractCatalogueItemSearchService<DataModel> {

    @Autowired
    CoreSearchService mdmCoreSearchService

    @Autowired
    DataModelSearchService mdmPluginDataModelSearchService

    @Autowired
    DataModelService dataModelService

    PaginatedHibernateSearchResult<CatalogueItem> findAllReadableCatalogueItemsByHibernateSearch(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                                 SearchParams searchParams,
                                                                                                 Map pagination = [:]) {
        mdmCoreSearchService.findAllReadableByHibernateSearch(userSecurityPolicyManager, searchParams, pagination)
    }

    PaginatedHibernateSearchResult<ModelItem> findAllModelItemsByMultifacetAwareItemByHibernateSearch(MultiFacetAware multiFacetAware, SearchParams searchParams,
                                                                                                      Map pagination = [:]) {
        if (multiFacetAware instanceof DataModel) {
            mdmPluginDataModelSearchService.findAllByDataModelIdByHibernateSearch(multiFacetAware.id, searchParams, pagination)
        } else if (multiFacetAware instanceof DataClass) {
            mdmPluginDataModelSearchService.findAllByDataClassIdByHibernateSearch(multiFacetAware.id, searchParams, pagination)
        } else {
            throw new ApiNotYetImplementedException('PSS', "${multiFacetAware.domainType} findAllByMultifacetAwareItemByHibernateSearch")
        }
    }

    InMemoryPagedResultList<Profile> findAllDataModelProfileObjectsForProfileProviderByHibernateSearch(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                                       ProfileProviderService dataModelProfileProviderService,
                                                                                                       SearchParams searchParams, Map pagination = [:]) {

        // Limit domain types to only those we know we care about
        if (searchParams.domainTypes) {
            searchParams.domainTypes.removeIf([test: {String s ->
                !(s in getDomainsToSearchInside())
            }] as Predicate)
        } else {
            searchParams.domainTypes = getDomainsToSearchInside().toList()
        }

        // Allow uninverting...why?
        System.setProperty('hibernate.search.index_uninverting_allowed', 'true')

        // Find all catalogue items which meet the initial search parameters
        // We dont want to paginate the results at all as we need ALL the results
        PaginatedHibernateSearchResult<CatalogueItem> result = mdmCoreSearchService.findAllReadableByHibernateSearch(userSecurityPolicyManager, searchParams, [:])
        log.debug('Results size: ' + result.count)
        if (result.isEmpty()) return new InMemoryPagedResultList<Profile>([], [:])

        Set<UUID> foundDataModelIds = [] as HashSet

        // Extract the DataModel ids for all the found catalogue items
        result.each {resultCatalogueItem ->
            if ((resultCatalogueItem as GormEntity).instanceOf(DataModel)) {
                foundDataModelIds.add(resultCatalogueItem.id)
            } else if ((resultCatalogueItem as GormEntity).instanceOf(DataClass)) {
                foundDataModelIds.add(((DataClass) resultCatalogueItem).modelId)
            } else if ((resultCatalogueItem as GormEntity).instanceOf(DataElement)) {
                foundDataModelIds.add(((DataElement) resultCatalogueItem).modelId)
            }
        }

        log.debug('DataModels size: ' + foundDataModelIds.size())

        // Set the sort parameter
        if (searchParams.sort) {
            searchParams.sort = "${dataModelProfileProviderService.metadataNamespace} | ${searchParams.sort}".toString()
        }
        searchParams.labelOnly = false

        PaginatedHibernateSearchResult<DataModel> filteredDataModels = filterSearchResultsByProfile(dataModelProfileProviderService,
                                                                                                    foundDataModelIds,
                                                                                                    searchParams,
                                                                                                    pagination)

        List<Profile> profiledResults = filteredDataModels.results.collect {dataModel ->
            dataModelProfileProviderService.createProfileFromEntity(dataModel)
        }

        log.debug('profiledResults size: ' + profiledResults.size())
        new InMemoryPagedResultList<>(profiledResults, pagination)
    }

    Set<String> getDomainsToSearchInside() {
        [DataModel, DataClass, DataElement].collect {it.simpleName}.toSet()
    }

    @Override
    Set<Class<DataModel>> getDomainsToSearch() {
        [DataModel] as HashSet<Class<DataModel>>
    }

    PaginatedHibernateSearchResult<DataModel> filterSearchResultsByProfile(ProfileProviderService dataModelProfileProviderService,
                                                                           Set<UUID> foundDataModelIds,
                                                                           SearchParams searchParams, Map<String, Object> pagination) {
        log.debug('Filtering found DataModel ids')
        // Execute the profile filtered search on the found datamodels

        Map<String, Object> filters = pagination.findAll {k, v ->
            k in dataModelProfileProviderService.getKnownMetadataKeys()
        }

        log.debug('Filtering on {}', filters.keySet())

        if (!filters) {
            return PaginatedHibernateSearchResult.paginateFullResultSet(dataModelService.getAll(foundDataModelIds), pagination)
        }

        PaginatedHibernateSearchResult<DataModel> filteredDataModels =
            findAllCatalogueItemsOfTypeByOwningIdsByHibernateSearch(foundDataModelIds.toList(),
                                                                    searchParams,
                                                                    false,
                                                                    pagination) {
                filters.each {metadataKey, filter ->
                    if (filter instanceof String) {
                        simpleQueryString(filter as String,
                                          "${dataModelProfileProviderService.metadataNamespace} | ${metadataKey}")
                    } else {
                        // We've got a list of filters
                        should {
                            ((List<String>) filter).each {filterValue ->
                                simpleQueryString(filterValue, "${dataModelProfileProviderService.metadataNamespace} | ${metadataKey}")
                            }
                        }
                    }
                }
            }

        log.debug('filteredDataModels size: {}', filteredDataModels.results.size())
        filteredDataModels
    }
}
