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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DataModelProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

class ProfileController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    SessionFactory sessionFactory

    ProfileService profileService

    @Autowired
    SearchService mdmPluginProfileSearchService

    def profileProviders() {
        respond providers: profileService.profileProviderServices
    }

    def profiles() {
        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        def usedProfiles = profileService.getUsedProfileServices(catalogueItem)
        render(view: "/profile/profileProviders", model: [providers: usedProfiles])
    }

    def show() {

        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)

        if (!catalogueItem) {
            return notFound(params.catalogueItemClass, params.catalogueItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        respond profileService.createProfile(profileProviderService, catalogueItem).getContents()

    }

    @Transactional
    def save() {

        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)

        if (!catalogueItem) {
            return notFound(params.catalogueItemClass, params.catalogueItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        profileService.storeProfile(profileProviderService, catalogueItem, request, currentUser)

        // Flush the profile before we create as the create method retrieves whatever is stored in the database
        sessionFactory.currentSession.flush()

        // Create the profile as the stored profile may only be segments of the profile and we now want to get everything
        respond profileService.createProfile(profileProviderService, catalogueItem)
    }

    def listModelsInProfile() {
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }
        PaginatedResultList<Profile> profiles = profileService.getModelsWithProfile(profileProviderService, currentUserSecurityPolicyManager, params)
        respond profileList: profiles
    }

    def listValuesInProfile() {
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        List<Profile> profiledResults
        if (params.search) {
            SearchParams searchParams = new SearchParams()
            searchParams.searchTerm = params.search
            searchParams.offset = 0
            searchParams.max = null
            searchParams.order = searchParams.order ?: 'asc'

            PaginatedLuceneResult<CatalogueItem> result = mdmPluginProfileSearchService.findAllReadableByLuceneSearch(
                currentUserSecurityPolicyManager, searchParams, params
            )

            Set<Model> foundModels = new HashSet<>()

            result.results.each { resultCatalogueItem ->
                if (Utils.parentClassIsAssignableFromChild(Model, resultCatalogueItem.class)) {
                    foundModels.add(resultCatalogueItem as Model)
                }
                if (Utils.parentClassIsAssignableFromChild(ModelItem, resultCatalogueItem.class)) {
                    foundModels.add((resultCatalogueItem as ModelItem).model)
                }
            }
            profiledResults = foundModels.collect { model ->
                profileProviderService.createProfileFromEntity(model)
            }

        } else {
            profiledResults = profileService.getModelsWithProfile(profileProviderService, currentUserSecurityPolicyManager, params)
        }

        respond excludes: 'knownFields', profileProviderService.getAllProfileFieldValues(params, profiledResults)
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        ProfileProviderService profileProviderService =
            profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                      params.profileVersion) as ProfileProviderService

        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        if (!(profileProviderService instanceof DataModelProfileProviderService)) {
            throw new ApiNotYetImplementedException('PCXX', 'Non-DataModel Based searching in profiles')
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        searchParams.offset = 0
        searchParams.max = null
        params.offset = 0
        params.max = null

        List<Profile> profileObjects = mdmPluginProfileSearchService.findAllDataModelProfileObjectsForProfileProviderByLuceneSearch(
            currentUserSecurityPolicyManager, profileProviderService, searchParams, params
        )

        respond profileList: profileObjects
    }

    protected String getProfileProviderServiceId(Map params) {
        String baseId = "${params.profileNamespace}:${params.profileName}"
        params.profileVersion ? "${baseId}:${params.profileVersion}" : baseId
    }
}
