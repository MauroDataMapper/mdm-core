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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softend.maurodatamapper.profile.databinding.ItemsProfilesDataBinding
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService

import grails.web.databinding.DataBinder
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.NO_CONTENT

@Slf4j
class ProfileController implements ResourcelessMdmController, DataBinder {
    static responseFormats = ['json', 'xml']

    ProfileService profileService

    MetadataService metadataService

    @Autowired
    SearchService mdmPluginProfileSearchService


    def profileProviders() {
        respond profileProviderServices: profileService.getAllProfileProviderServices()
    }

    def dynamicProfileProviders() {
        respond profileProviderServices: profileService.getAllDynamicProfileProviderServices()
    }

    def usedProfiles() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params
            .multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        respond profileProviderServices: profileService.getUsedProfileServices(multiFacetAware)
    }

    def unusedProfiles() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params
            .multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        respond profileProviderServices: profileService.getUnusedProfileServices(multiFacetAware)
    }

    def nonProfileMetadata() {
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        Set<ProfileProviderService> usedProfiles = profileService.getUsedProfileServices(multiFacetAware)
        Set<String> profileNamespaces = usedProfiles.collect {it.metadataNamespace}
        respond metadataService.findAllByMultiFacetAwareItemIdAndNotNamespaces(multiFacetAware.id, profileNamespaces.asList(), params),
                view: "/metadata/index"
    }

    @Transactional
    def delete() {
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        profileService.deleteProfile(profileProviderService, multiFacetAware, currentUser)

        request.withFormat {
            '*' {render status: NO_CONTENT} // NO CONTENT STATUS CODE
        }
    }

    def show() {

        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params
            .multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        respond profile: profileService.createProfile(profileProviderService, multiFacetAware), format: params.format
    }

    /**
     * The request must contain a collection of IDs of items which belong to the multi facet aware item, and a collection
     * of profile namespaces/names/version. The response returns all matching profiles for the requested items and profiles.
     */
    def itemsProfiles() {
        List<Profile> profiles = []
        // this multiFacetAware item is expected to be a model
        MultiFacetAware model = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params
                .multiFacetAwareItemId)
        if (!model || !(model instanceof Model)) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ItemsProfilesDataBinding itemsProfiles = new ItemsProfilesDataBinding()
        bindData itemsProfiles, request

        itemsProfiles.multiFacetAwareItems.each { multiFacetAwareItemDataBinding ->
            MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(
                    multiFacetAwareItemDataBinding.multiFacetAwareItemDomainType,
                    multiFacetAwareItemDataBinding.multiFacetAwareItemId)

            // In the interceptor we only checked access to the model
            // The body of the request (which was bound to itemsProfiles) could contain
            // multi facet aware items which do not belong to the checked model.
            // So here, only proceed if the multiFacetAware is a ModelItem and that ModelItem
            // belongs to the checked model.
            if (multiFacetAware &&
            multiFacetAware instanceof ModelItem &&
            multiFacetAware.model.id == model.id) {

                itemsProfiles.profileProviderServices.each { profileProviderServiceDataBinding ->
                    ProfileProviderService profileProviderService = profileService.findProfileProviderService(
                            profileProviderServiceDataBinding.namespace,
                            profileProviderServiceDataBinding.name,
                            profileProviderServiceDataBinding.version)

                    if (profileProviderService) {
                        profiles.add(profileService.createProfile(profileProviderService, multiFacetAware))
                    }
                }

            }
        }

        respond profileList: profiles, format: params.format, view: 'itemsProfiles'
    }

    @Transactional
    def save() {

        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        Profile instance = profileProviderService.getNewProfile()
        bindData(instance, request)

        MultiFacetAware profiled = profileService.storeProfile(profileProviderService, multiFacetAware, instance, currentUser)

        // Create the profile as the stored profile may only be segments of the profile and we now want to get everything
        respond profileService.createProfile(profileProviderService, profiled)
    }

    def validate() {
        log.debug("Validating profile")
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        Profile submittedInstance = profileProviderService.getNewProfile()
        bindData(submittedInstance, request)

        Profile validatedInstance = profileService.validateProfile(profileProviderService, submittedInstance)

        if (validatedInstance.hasErrors()) {
            respond validatedInstance.errors
            return
        }

        respond validatedInstance
    }


    def listModelsInProfile() {
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }
        PaginatedResultList<Profile> profiles =
            profileService.getModelsWithProfile(profileProviderService, currentUserSecurityPolicyManager, params.multiFacetAwareItemDomainType, params)
        respond profileList: profiles
    }

    def listValuesInProfile() {
        ProfileProviderService profileProviderService =
            profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }
        Map<String, Collection<String>> allValuesMap = profileProviderService.listAllValuesInProfile(params.multiFacetAwareItemDomainType,
                                                                                                     params.filter,
                                                                                                     currentUserSecurityPolicyManager)

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
