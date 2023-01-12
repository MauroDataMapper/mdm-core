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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.gorm.InMemoryPagedResultList
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DefaultJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DynamicImportJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DynamicJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ItemsProfilesDataBinding
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfileProvided
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfileProvidedCollection
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfiledCatalogueItem
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.support.proxy.ProxyHandler
import grails.gorm.transactions.Transactional
import grails.web.databinding.DataBinder
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Transactional
//@GrailsCompileStatic
class ProfileService implements DataBinder {

    @Autowired
    List<MultiFacetAwareService> multiFacetAwareServices

    @Autowired
    List<ModelService> modelServices

    @Autowired(required = false)
    Set<ProfileProviderService> profileProviderServices

    DataModelService dataModelService
    MetadataService metadataService
    DefaultJsonProfileProviderService profileSpecificationProfileService
    SessionFactory sessionFactory
    ProxyHandler proxyHandler

    Profile createProfile(ProfileProviderService profileProviderService, MultiFacetAware multiFacetAwareItem) {
        profileProviderService.createProfileFromEntity(multiFacetAwareItem)
    }

    ProfileProviderService findProfileProviderService(String profileNamespace, String profileName, String profileVersion = null) {
        findProfileProviderService(getAllProfileProviderServices(true), profileNamespace, profileName, profileVersion)

    }

    ProfileProviderService findProfileProviderServiceForMultiFacetAwareItem(MultiFacetAware multiFacetAware, String profileNamespace, String profileName,
                                                                            String profileVersion = null) {
        List<ProfileProviderService> allAvailableProfileProvideServices = getAllAvailableProfileProviderServicesForMultiFacetAwareItem(multiFacetAware, false)
        findProfileProviderService(allAvailableProfileProvideServices, profileNamespace, profileName, profileVersion)
    }

    ProfileProviderService findProfileProviderService(List<ProfileProviderService> allAvailableProfileProvideServices,
                                                      String profileNamespace, String profileName, String profileVersion = null) {
        if (profileVersion) {
            return allAvailableProfileProvideServices.find {
                it.namespace == profileNamespace &&
                it.getName() in [profileName, Utils.safeUrlEncode(profileName)] &&
                it.version == profileVersion
            }
        }
        allAvailableProfileProvideServices.findAll {
            it.namespace == profileNamespace &&
            it.getName() in [profileName, Utils.safeUrlEncode(profileName)]
        }.max()
    }

    ProfileProviderService configureProfileProviderServiceForImportingOwner(ProfileProviderService profileProviderService, String importingOwnerDomainType,
                                                                            UUID importingOwnerId) {
        if (profileProviderService !instanceof DynamicImportJsonProfileProviderService) {
            throw new ApiBadRequestException('PC', 'Requesting import profile of a non-import type profile is not allowed')
        }
        if (!profileProviderService.importingId) {
            MdmDomain importingOwner = metadataService.findMultiFacetAwareItemByDomainTypeAndId(importingOwnerDomainType, importingOwnerId) as MdmDomain
            if (!importingOwner) return null

            return profileProviderService.generateDynamicProfileForImportingOwner(importingOwner, false)
        }
        profileProviderService.includeImportOwnerSection = false
        return profileProviderService
    }

    Profile storeProfile(ProfileProviderService profileProviderService, MultiFacetAware multiFacetAwareItem, Profile profileToStore, User user) {
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(multiFacetAwareItem.domainType)
        // Store profile should return the WHOLE profile after storing
        Profile fullProfile = profileProviderService.storeProfileInEntity(multiFacetAwareItem, profileToStore, user.emailAddress,
                                                                          service.isMultiFacetAwareFinalised(multiFacetAwareItem))
        service.save(flush: true, validate: false, multiFacetAwareItem) as MultiFacetAware
        fullProfile
    }

    Profile validateProfile(ProfileProviderService profileProviderService, Profile submittedProfile) {
        Profile cleanProfile = profileProviderService.createCleanProfileFromProfile(submittedProfile)
        cleanProfile.validate()
        cleanProfile
    }

    Profile validateProfileValues(ProfileProviderService profileProviderService, Profile submittedProfile) {
        Profile cleanProfile = profileProviderService.createCleanProfileFromProfile(submittedProfile)
        cleanProfile.validateCurrentValues()
        cleanProfile
    }

    void deleteProfile(ProfileProviderService profileProviderService, MultiFacetAware multiFacetAwareItem, User currentUser) {

        Collection<Metadata> mds = profileProviderService.getAllProfileMetadataByMultiFacetAwareItemId(multiFacetAwareItem.id)

        mds.each {md ->
            metadataService.delete(md)
            metadataService.addDeletedEditToMultiFacetAwareItem(currentUser, md, multiFacetAwareItem.domainType, multiFacetAwareItem.id)
        }
        sessionFactory.currentSession.flush()
    }


    List<Model> getAllModelsWithProfile(ProfileProviderService profileProviderService,
                                        UserSecurityPolicyManager userSecurityPolicyManager,
                                        String domainType) {

        List<Model> models = []
        ModelService service = modelServices.find {it.handles(domainType)}
        models.addAll(service.findAllByMetadataNamespace(profileProviderService.metadataNamespace))

        List<Model> validModels = models.findAll {model ->
            userSecurityPolicyManager.userCanReadSecuredResourceId(model.class, model.id) && !model.deleted
        }
        return validModels
    }

    InMemoryPagedResultList<Profile> getModelsWithProfile(ProfileProviderService profileProviderService,
                                                          UserSecurityPolicyManager userSecurityPolicyManager,
                                                          String domainType,
                                                          Map pagination = [:]) {

        List<Model> models = getAllModelsWithProfile(profileProviderService, userSecurityPolicyManager, domainType)

        List<Profile> profiles = []
        profiles.addAll(models.collect {model ->
            profileProviderService.createProfileFromEntity(model)
        })
        new InMemoryPagedResultList<>(profiles, pagination)
    }

    MultiFacetAwareService findServiceForMultiFacetAwareDomainType(String domainType) {
        MultiFacetAwareService service = multiFacetAwareServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('PS01', "No supporting service for ${domainType}")
        return service
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        findServiceForMultiFacetAwareDomainType(domainType).get(multiFacetAwareItemId) as MultiFacetAware
    }

    boolean isMultiFacetAwareFinalised(MultiFacetAware multiFacetAwareItem) {
        findServiceForMultiFacetAwareDomainType(multiFacetAwareItem.domainType).isMultiFacetAwareFinalised(multiFacetAwareItem)
    }

    Set<String> getUsedNamespaces(MultiFacetAware multiFacetAwareItem) {
        //MetadataService metadataService = grailsApplication.mainContext.getBean('metadataService')
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemId(multiFacetAwareItem.id)
        metadataList.collect {it.namespace} as Set
    }

    List<ProfileProviderService> getAllAvailableProfileProviderServicesForMultiFacetAwareItem(MultiFacetAware multiFacetAwareItem,
                                                                                              boolean latestVersionByMetadataNamespace) {
        List<ProfileProviderService> allProfileServices = getAllProfileProviderServices(latestVersionByMetadataNamespace).findAll {
            it.profileApplicableForDomains().contains(multiFacetAwareItem.domainType) || it.profileApplicableForDomains().size() == 0
        }
        allProfileServices.addAll(getAllDynamicImportProfileProviderServicesForMultiFacetAwareItem(multiFacetAwareItem))
        allProfileServices.sort()
    }

    List<ProfileProviderService> getUsedProfileServices(MultiFacetAware multiFacetAwareItem, boolean latestVersionByMetadataNamespace) {
        Set<String> usedNamespaces = getUsedNamespaces(multiFacetAwareItem)
        getAllAvailableProfileProviderServicesForMultiFacetAwareItem(multiFacetAwareItem, latestVersionByMetadataNamespace)
            .findAll {
                usedNamespaces.contains(it.getMetadataNamespace()) ||
                (it instanceof DynamicImportJsonProfileProviderService && it.importingId)
            }
    }

    List<ProfileProviderService> getUnusedProfileServices(MultiFacetAware multiFacetAwareItem, boolean latestVersionByMetadataNamespace) {
        Set<String> usedNamespaces = getUsedNamespaces(multiFacetAwareItem)
        getAllAvailableProfileProviderServicesForMultiFacetAwareItem(multiFacetAwareItem, latestVersionByMetadataNamespace)
            .findAll {
                !usedNamespaces.contains(it.getMetadataNamespace()) ||
                (it instanceof DynamicImportJsonProfileProviderService && !it.importingId)
            }
    }

    List<ProfileProviderService> getUsedImportProfileServices(MultiFacetAware multiFacetAwareItem, UUID importingOwnerId, boolean latestVersionByMetadataNamespace) {
        getUsedProfileServices(multiFacetAwareItem, latestVersionByMetadataNamespace).findAll {pps ->
            pps instanceof DynamicImportJsonProfileProviderService && pps.importingId == importingOwnerId
        }
    }

    List<ProfileProviderService> getUnusedImportProfileServices(MultiFacetAware multiFacetAwareItem, boolean latestVersionByMetadataNamespace) {
        getUnusedProfileServices(multiFacetAwareItem, latestVersionByMetadataNamespace).findAll {it instanceof DynamicImportJsonProfileProviderService}
    }

    /**
     * @param finalisedOnly If true then exclude dynamic profiles which are not finalised
     * @return
     */
    List<ProfileProviderService> getAllProfileProviderServices(boolean latestVersionByMetadataNamespace) {
        // Create a new set with the non-disabled autowired services added
        Set<ProfileProviderService> allProfileProviderServices = new HashSet<>(profileProviderServices.size())
        allProfileProviderServices.addAll(profileProviderServices.findAll {!it.disabled})
        // Get all the dynamic datamodel PPSs and add them
        allProfileProviderServices.addAll(getAllDynamicProfileProviderServices())

        if (latestVersionByMetadataNamespace) {
            return allProfileProviderServices
                .groupBy {new Tuple(it.namespace, it.name, it.metadataNamespace)}
                .collect {it.value.max()}
                .sort()
        }
        return allProfileProviderServices.sort()
    }

    List<ProfileProviderService> getAllDynamicProfileProviderServices() {
        // Now we get all the dynamic models
        List<DataModel> dynamicModels = dataModelService.findAllByMetadataNamespace(profileSpecificationProfileService.metadataNamespace)
        dynamicModels
            .findAll {it.finalised}
            .collect {new DynamicJsonProfileProviderService(proxyHandler, metadataService, sessionFactory, it)}
    }

    List<ProfileProviderService> getAllDynamicImportProfileProviderServicesForMultiFacetAwareItem(MultiFacetAware multiFacetAware) {
        List<Metadata> importMetadata = metadataService.findAllByMultiFacetAwareItemIdAndNamespaceLike(multiFacetAware.id,
                                                                                                       "${DynamicImportJsonProfileProviderService.IMPORT_NAMESPACE_PREFIX}.%",
                                                                                                       [:])
        Set<String> distinctNamespaces = importMetadata.collect {it.namespace}.toSet()
        List<DynamicImportJsonProfileProviderService> allImportJsonProfileProviderServices = profileProviderServices
            .findAll {
                it instanceof DynamicImportJsonProfileProviderService &&
                (it.profileApplicableForDomains().contains(multiFacetAware.domainType) || it.profileApplicableForDomains().size() == 0)
            } as List<DynamicImportJsonProfileProviderService>

        distinctNamespaces.collect {ns ->
            // Find the profile service that should be used for the namespace
            DynamicImportJsonProfileProviderService rootService = allImportJsonProfileProviderServices.find {it.matchesMetadataNamespace(ns)}
            // If none exists then the MD has been added erroneously or the PPS has been removed
            if (!rootService) return null
            rootService.generateDynamicProfileForImportingOwner(ns, importMetadata.findAll {it.namespace == ns})
        }.findAll().sort()
    }

    def getMany(UUID modelId, ItemsProfilesDataBinding itemsProfiles) {
        List<ProfileProvided> profiles = []

        itemsProfiles.multiFacetAwareItems.each {multiFacetAwareItemDataBinding ->
            MultiFacetAware multiFacetAware = findMultiFacetAwareItemByDomainTypeAndId(
                multiFacetAwareItemDataBinding.multiFacetAwareItemDomainType,
                multiFacetAwareItemDataBinding.multiFacetAwareItemId)

            // In the interceptor we only checked access to the model
            // The body of the request (which was bound to itemsProfiles) could contain
            // multi facet aware items which do not belong to the checked model.
            // So here, only proceed if the multiFacetAware is a ModelItem and that ModelItem
            // belongs to the checked model.
            if (multiFacetAware &&
                multiFacetAware instanceof ModelItem &&
                multiFacetAware.model.id == modelId) {

                itemsProfiles.profileProviderServices.each {profileProviderServiceDataBinding ->
                    ProfileProviderService profileProviderService = findProfileProviderService(
                        profileProviderServiceDataBinding.namespace,
                        profileProviderServiceDataBinding.name,
                        profileProviderServiceDataBinding.version)

                    if (profileProviderService) {
                        ProfileProvided profileProvided = new ProfileProvided()
                        profileProvided.profile = createProfile(profileProviderService, multiFacetAware)
                        profileProvided.profileProviderService = profileProviderService
                        profileProvided.multiFacetAware = multiFacetAware
                        profiles.add(profileProvided)
                    }
                }
            }
        }

        profiles
    }

    /**
     * Validate or save many profile instances
     * @param validateOnly
     * @param ProfileProvidedCollection bound data from the request
     * @param Model The model to which all the MultiFacetAwareItems specified in the request must belong
     * @param UserSecurityPolicyManager
     * @param User
     * @return List of handled (validated or saved) instances
     */
    List<ProfileProvided> handleMany(boolean validateOnly, ProfileProvidedCollection profileProvidedCollection, Model model,
                                     UserSecurityPolicyManager userSecurityPolicyManager, User user) {
        List<ProfileProvided> handledInstances = []

        profileProvidedCollection.profilesProvided.each {profileProvided ->

            ProfileProviderService profileProviderService = findProfileProviderService(
                profileProvided.profileProviderService.namespace,
                profileProvided.profileProviderService.name,
                profileProvided.profileProviderService.version)

            if (profileProviderService) {
                // Bind the Map profileProvided.profile to an object of the right subclass of Profile
                Profile submittedInstance = profileProviderService.getNewProfile()
                bindData submittedInstance, profileProvided.profile

                MultiFacetAware multiFacetAware = findMultiFacetAwareItemByDomainTypeAndId(submittedInstance.domainType, submittedInstance.id)

                // In the interceptor we only checked access to the model
                // The body of the request could contain
                // multi facet aware items which do not belong to the checked model.
                // So here, only proceed if the multiFacetAware is a ModelItem and that ModelItem
                // belongs to the checked model.
                if (multiFacetAware &&
                    multiFacetAware instanceof ModelItem &&
                    multiFacetAware.model.id == model.id) {

                    log.debug("Found allowed multiFacetAware ${multiFacetAware.model.id}")

                    if (validateOnly) {
                        ProfileProvided validated = new ProfileProvided()
                        validated.profile = validateProfileValues(profileProviderService, submittedInstance)
                        validated.profileProviderService = profileProviderService
                        validated.multiFacetAware = multiFacetAware
                        handledInstances.add(validated)
                    } else {
                        boolean saveAllowed

                        if (profileProviderService.canBeEditedAfterFinalisation()) {
                            saveAllowed = userSecurityPolicyManager.userCanWriteResourceId(Metadata, null, model.class, model.id, 'saveIgnoreFinalise')
                            log.debug(
                                "Profile can be edited after finalisation ${profileProviderService.namespace}.${profileProviderService.name} and saveAllowed is " +
                                "${saveAllowed}")
                        } else {
                            saveAllowed = userSecurityPolicyManager.userCanCreateResourceId(Metadata, null, model.class, model.id)
                            log.debug(
                                "Profile cannot be edited after finalisation ${profileProviderService.namespace}.${profileProviderService.name} and saveAllowed is " +
                                "${saveAllowed}")
                        }

                        if (saveAllowed) {
                            ProfileProvided saved = new ProfileProvided()
                            // Create the profile as the stored profile may only be segments of the profile and we now want to get everything
                            saved.profile = storeProfile(profileProviderService, multiFacetAware, submittedInstance, user)
                            saved.profileProviderService = profileProviderService
                            saved.multiFacetAware = multiFacetAware
                            handledInstances.add(saved)
                        } else {
                            log.debug("Save not allowed for multiFacetAware ${multiFacetAware.model.id}")
                        }
                    }
                }
            }
        }

        handledInstances
    }

    PaginatedHibernateSearchResult<ProfiledCatalogueItem> loadProfilesIntoCatalogueItems(ProfileProviderService profileProviderService,
                                                                                         PaginatedHibernateSearchResult<CatalogueItem> paginatedHibernateSearchResult) {
        List<CatalogueItem> catalogueItems = paginatedHibernateSearchResult.results
        List<ProfiledCatalogueItem> profiledCatalogueItems = catalogueItems.collect {ci ->
            new ProfiledCatalogueItem(catalogueItem: ci, profile: createProfile(profileProviderService, ci))
        } as List<ProfiledCatalogueItem>
        new PaginatedHibernateSearchResult<ProfiledCatalogueItem>(profiledCatalogueItems, paginatedHibernateSearchResult.count)
    }

    List<Metadata> findAllNonProfileMetadata(MultiFacetAware multiFacetAware, Map pagination) {
        List<ProfileProviderService> usedProfiles = getUsedProfileServices(multiFacetAware, false)
        List<String> profileNamespaces = usedProfiles.collect {it.metadataNamespace}
        metadataService.findAllByMultiFacetAwareItemIdAndNotNamespacesAndNamespaceNotLike(multiFacetAware.id,
                                                                                          profileNamespaces,
                                                                                          "${DynamicImportJsonProfileProviderService.IMPORT_NAMESPACE_PREFIX}.%",
                                                                                          pagination)
    }
}
