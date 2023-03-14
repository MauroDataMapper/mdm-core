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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.gorm.InMemoryPagedResultList
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ItemsProfilesDataBinding
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfileProvided
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfileProvidedCollection

import grails.gorm.transactions.Transactional
import grails.web.databinding.DataBinder
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.NO_CONTENT

@Slf4j
class ProfileController implements ResourcelessMdmController, DataBinder {
    static responseFormats = ['json', 'xml']

    ProfileService profileService

    @Autowired
    SearchService mdmPluginProfileSearchService


    def profileProviders() {
        respond profileProviderServices: profileService.getAllProfileProviderServices(params.boolean('latestVersionByMetadataNamespace', false))
    }

    def profileProvider() {
        List<ProfileProviderService> allProviders = profileService.getAllProfileProviderServices(false)
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(allProviders, params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        respond profileProviderService: profileProviderService
    }

    def dynamicProfileProviders() {
        respond profileProviderServices: profileService.getAllDynamicProfileProviderServices()
    }

    def usedProfiles() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        if (params.importOnly) {
            return respond(profileProviderServices:
                               profileService
                                   .getUsedImportProfileServices(multiFacetAware, params.importingOwnerId, params.boolean('latestVersionByMetadataNamespace', false)))
        }
        respond profileProviderServices: profileService.getUsedProfileServices(multiFacetAware, params.boolean('latestVersionByMetadataNamespace', false))
    }

    def unusedProfiles() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        if (params.importOnly) {
            return respond(profileProviderServices: profileService.getUnusedImportProfileServices(multiFacetAware, params.boolean('latestVersionByMetadataNamespace', false)))
        }
        respond profileProviderServices: profileService.getUnusedProfileServices(multiFacetAware, params.boolean('latestVersionByMetadataNamespace', true))
    }

    def nonProfileMetadata() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        respond profileService.findAllNonProfileMetadata(multiFacetAware, params), view: '/metadata/index'
    }

    @Transactional
    def delete() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderServiceForMultiFacetAwareItem(
            multiFacetAware,
            params.profileNamespace,
            params.profileName,
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

        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderServiceForMultiFacetAwareItem(
            multiFacetAware,
            params.profileNamespace,
            params.profileName,
            params.profileVersion)

        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        if (params.importOnly) {
            profileProviderService = profileService.configureProfileProviderServiceForImportingOwner(profileProviderService,
                                                                                                     params.importingOwnerDomainType,
                                                                                                     params.importingOwnerId)
            if (!profileProviderService) {
                return notFound(params.importingOwnerClass, params.importingOwnerId)
            }
        }

        respond profile: profileService.createProfile(profileProviderService, multiFacetAware), format: params.format
    }

    def emptyProfile() {
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        respond profile: profileProviderService.getNewProfile(), format: params.format
    }


    /**
     * The request must contain a collection of IDs of items which belong to the multi facet aware item, and a collection
     * of profile namespaces/names/version. The response returns all matching profiles for the requested items and profiles.
     */
    def getMany(ItemsProfilesDataBinding itemsProfiles) {
        // this multiFacetAware item is expected to be a model
        MultiFacetAware model = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)
        if (!model) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        if (!(model instanceof Model)) {
            throw new ApiBadRequestException('PC01', 'Cannot use this endpoint on a item which is not a Model')
        }

        respond([view: 'many'], [profileProvidedList: profileService.getMany(model.id, itemsProfiles)])
    }

    @Transactional
    def save() {

        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderServiceForMultiFacetAwareItem(
            multiFacetAware,
            params.profileNamespace,
            params.profileName,
            params.profileVersion
        )
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        if (params.importOnly) {
            profileProviderService =
                profileService.configureProfileProviderServiceForImportingOwner(profileProviderService, params.importingOwnerDomainType, params.importingOwnerId)
            if (!profileProviderService) {
                return notFound(params.importingOwnerClass, params.importingOwnerId)
            }
        }

        Profile instance = profileProviderService.getNewProfile()
        bindData(instance, request)

        // Store profile should return the WHOLE profile after storing
        respond profileService.storeProfile(profileProviderService, multiFacetAware, instance, currentUser)
    }

    @Transactional
    def saveMany(ProfileProvidedCollection profileProvidedCollection) {
        handleMany(false, profileProvidedCollection)
    }

    def validate() {
        log.debug('Validating profile')
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderServiceForMultiFacetAwareItem(
            multiFacetAware,
            params.profileNamespace,
            params.profileName,
            params.profileVersion
        )
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        if (params.importOnly) {
            profileProviderService =
                profileService.configureProfileProviderServiceForImportingOwner(profileProviderService, params.importingOwnerDomainType, params.importingOwnerId)
            if (!profileProviderService) {
                return notFound(params.importingOwnerClass, params.importingOwnerId)
            }
        }

        Profile submittedInstance = profileProviderService.getNewProfile()
        bindData(submittedInstance, request)

        Profile validatedInstance = profileService.validateProfileValues(profileProviderService, submittedInstance)

        if (validatedInstance.hasErrors()) {
            respond validatedInstance.errors
            return
        }

        respond validatedInstance
    }

    def validateMany(ProfileProvidedCollection profileProvidedCollection) {
        handleMany(true, profileProvidedCollection)
    }

    /**
     * Validate or save many profile instances
     * @param validateOnly
     * @param ProfileProvidedCollection bound data from the request
     * @return
     */
    private handleMany(boolean validateOnly, ProfileProvidedCollection profileProvidedCollection) {
        log.debug('Handling many items profiles')

        // The multiFacetAware item referenced in the URI, is expected to be a model
        MultiFacetAware model = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

        if (!model) {
            return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
        }
        if (!(model instanceof Model)) {
            throw new ApiBadRequestException('PC02', 'Cannot use this endpoint on a item which is not a Model')
        }

        List<ProfileProvided> handled = profileService.handleMany(validateOnly, profileProvidedCollection, model, currentUserSecurityPolicyManager, currentUser)
        respond([view: 'many'], [profileProvidedList: handled])
    }

    def listModelsInProfile() {
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }
        InMemoryPagedResultList<Profile> profiles = profileService.getModelsWithProfile(profileProviderService, currentUserSecurityPolicyManager,
                                                                                        params.multiFacetAwareItemDomainType, params)
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

    def search() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }
        searchParams.crossValuesIntoParametersMap(params, 'label')

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)

        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        PaginatedHibernateSearchResult<CatalogueItem> results
        if (params.multiFacetAwareItemDomainType) {

            MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareItemDomainType, params.multiFacetAwareItemId)

            if (!multiFacetAware) {
                return notFound(params.multiFacetAwareItemClass, params.multiFacetAwareItemId)
            }
            results = mdmPluginProfileSearchService.findAllModelItemsByMultifacetAwareItemByHibernateSearch(multiFacetAware, searchParams, params)
        } else {
            results = mdmPluginProfileSearchService.findAllReadableCatalogueItemsByHibernateSearch(currentUserSecurityPolicyManager, searchParams, params)
        }
        respond profileService.loadProfilesIntoCatalogueItems(profileProviderService, results)
    }

    protected String getProfileProviderServiceId(Map params) {
        String baseId = "${params.profileNamespace}:${params.profileName}"
        params.profileVersion ? "${baseId}:${params.profileVersion}" : baseId
    }
}
