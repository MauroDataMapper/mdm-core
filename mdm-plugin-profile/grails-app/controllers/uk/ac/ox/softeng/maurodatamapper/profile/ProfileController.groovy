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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
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

    MetadataService metadataService

    @Autowired
    SearchService mdmPluginProfileSearchService

    def profileProviders() {
        respond providers: profileService.profileProviderServices
    }

    def profiles() {
        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        if (!catalogueItem) {
            return notFound(params.catalogueItemClass, params.catalogueItemId)
        }
        def usedProfiles = profileService.getUsedProfileServices(catalogueItem)
        render(view: "/profile/profileProviders", model: [providers: usedProfiles])
    }

    def unusedProfiles() {
        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        if (!catalogueItem) {
            return notFound(params.catalogueItemClass, params.catalogueItemId)
        }
        Set<ProfileProviderService> usedProfiles = profileService.getUsedProfileServices(catalogueItem)
        Set<ProfileProviderService> allProfiles = profileService.getProfileProviderServices().findAll() {
            it.profileApplicableForDomains().size() == 0 ||
                it.profileApplicableForDomains().contains(params.catalogueItemDomainType)
        }
        Set<ProfileProviderService> unusedProfiles = new HashSet<ProfileProviderService>(allProfiles)
        unusedProfiles.removeAll(usedProfiles)
        render(view: "/profile/profileProviders", model: [providers: unusedProfiles])
    }

    def otherMetadata() {
        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        if (!catalogueItem) {
            return notFound(params.catalogueItemClass, params.catalogueItemId)
        }
        Set<ProfileProviderService> usedProfiles = profileService.getUsedProfileServices(catalogueItem)
        Set<String> profileNamespaces = usedProfiles.collect{it.metadataNamespace}
        render(view: "/metadata/index",
               model: [metadataList: metadataService.findAllByCatalogueItemIdAndNotNamespaces(catalogueItem.id, profileNamespaces.asList(), params)])
    }

    @Transactional
    def deleteProfile() {
        CatalogueItem catalogueItem = profileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        if (!catalogueItem) {
            return notFound(params.catalogueItemClass, params.catalogueItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }
        catalogueItem.metadata
            .findAll{ it.namespace == profileProviderService.metadataNamespace }
            .each {md ->
                metadataService.delete(md, true)
                metadataService.addDeletedEditToCatalogueItem(currentUser, md, params.catalogueItemDomainType, params.catalogueItemId)}
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

        respond profileService.createProfile(profileProviderService, catalogueItem)

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
        PaginatedResultList<Profile> profiles =
                profileService.getModelsWithProfile(profileProviderService, currentUserSecurityPolicyManager, params.catalogueItemDomainType, params)
        respond profileList: profiles
    }

    def listValuesInProfile() {
        ProfileProviderService profileProviderService =
                profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        List<MetadataAware> profiledItems = profileProviderService.findAllProfiledItems(params.catalogueItemDomainType)
        List<MetadataAware> filteredProfiledItems = []
        profiledItems.each {profiledItem ->
            if(profiledItem instanceof Model
                    && currentUserSecurityPolicyManager.userCanReadSecuredResourceId(profiledItem.getClass(), profiledItem.id)) {
                    filteredProfiledItems.add(profiledItem)
            } else if (profiledItem instanceof ModelItem) {

                CatalogueItem model = proxyHandler.unwrapIfProxy(profiledItem.getModel())
                if(currentUserSecurityPolicyManager.userCanReadResourceId(profiledItem.getClass(), profiledItem.id, model.getClass(), model.id)) {
                        filteredProfiledItems.add(profiledItem)
                    }
            }

        }
        Map<String, Collection<String>> allValuesMap = [:]
        profileProviderService.getKnownMetadataKeys().findAll{key -> (!params.filter || params.filter.contains(key))}.each { key ->
            Set<String> allValues = new HashSet<String>();
            filteredProfiledItems.each { profiledItem ->
                Metadata md = profiledItem.metadata.find {
                    it.namespace == profileProviderService.metadataNamespace &&
                            it.key == key }
                if(md) {
                    allValues.add(md.value)
                }
            }

            allValuesMap[key] = allValues
        }


        respond allValuesMap
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

/*        if (!(profileProviderService instanceof DataModelProfileProviderService)) {
            throw new ApiNotYetImplementedException('PCXX', 'Non-DataModel Based searching in profiles')
        }
*/
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
