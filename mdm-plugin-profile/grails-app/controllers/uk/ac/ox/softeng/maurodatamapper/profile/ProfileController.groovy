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


import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class ProfileController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    SessionFactory sessionFactory

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

    def profiles() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params
            .multiFacetAwareId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }
        respond profileProviderServices: profileService.getUsedProfileServices(multiFacetAware)
    }

    def unusedProfiles() {
        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params
            .multiFacetAwareId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }
        respond profileProviderServices: profileService.getUnusedProfileServices(multiFacetAware)
    }

    def otherMetadata() {
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params.multiFacetAwareId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }
        Set<ProfileProviderService> usedProfiles = profileService.getUsedProfileServices(multiFacetAware)
        Set<String> profileNamespaces = usedProfiles.collect{it.metadataNamespace}
        respond metadataService.findAllByMultiFacetAwareItemIdAndNotNamespaces(multiFacetAware.id, profileNamespaces.asList(),params),
                view: "/metadata/index"

//        respond(view: "/metadata/index",
//                model: [metadataList: metadataService.findAllByMultiFacetAwareItemIdAndNotNamespaces(multiFacetAware.id, profileNamespaces.asList(),
//                                                                                                     params)])
    }

    @Transactional
    def delete() {
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params.multiFacetAwareId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        Set<Metadata> mds =
            multiFacetAware.metadata
            .findAll{ it.namespace == profileProviderService.metadataNamespace }

        mds.each {md ->
                //multiFacetAware.metadata.remove(md)
                metadataService.delete(md, true)
                metadataService.addDeletedEditToMultiFacetAwareItem(currentUser, md, params.multiFacetAwareDomainType, params.multiFacetAwareId)}
    }

    def show() {

        MultiFacetAware multiFacetAware = profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params
            .multiFacetAwareId)
        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        respond profileService.createProfile(profileProviderService, multiFacetAware)

    }

    @Transactional
    def save() {

        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params.multiFacetAwareId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        profileService.storeProfile(profileProviderService, multiFacetAware, request, currentUser)

        // Flush the profile before we create as the create method retrieves whatever is stored in the database
        sessionFactory.currentSession.flush()

        // Create the profile as the stored profile may only be segments of the profile and we now want to get everything
        respond profileService.createProfile(profileProviderService, multiFacetAware)
    }

    def validate() {
        log.debug("validating profile...")
        MultiFacetAware multiFacetAware =
            profileService.findMultiFacetAwareItemByDomainTypeAndId(params.multiFacetAwareDomainType, params.multiFacetAwareId)

        if (!multiFacetAware) {
            return notFound(params.multiFacetAwareClass, params.multiFacetAwareId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        respond profileService.validateProfile(profileProviderService, multiFacetAware, request, currentUser)
    }


    def listModelsInProfile() {
        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName,
                                                                                                  params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }
        PaginatedResultList<Profile> profiles =
                profileService.getModelsWithProfile(profileProviderService, currentUserSecurityPolicyManager, params.multiFacetAwareDomainType, params)
        respond profileList: profiles
    }

    def listValuesInProfile() {
        ProfileProviderService profileProviderService =
                profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)
        if (!profileProviderService) {
            return notFound(ProfileProviderService, getProfileProviderServiceId(params))
        }

        List<MetadataAware> profiledItems = profileProviderService.findAllProfiledItems(params.multiFacetAwareDomainType)
        List<MetadataAware> filteredProfiledItems = []
        profiledItems.each {profiledItem ->
            if(profiledItem instanceof Model
                    && currentUserSecurityPolicyManager.userCanReadSecuredResourceId(profiledItem.getClass(), profiledItem.id)) {
                    filteredProfiledItems.add(profiledItem)
            } else if (profiledItem instanceof ModelItem) {

                Model model = proxyHandler.unwrapIfProxy(profiledItem.getModel())
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
