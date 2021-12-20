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

import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DynamicJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ItemsProfilesDataBinding
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfileProvided
import uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.ProfileProvidedCollection
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Transactional
import grails.web.databinding.DataBinder
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Transactional
class ProfileService implements DataBinder {

    @Autowired
    List<MultiFacetAwareService> multiFacetAwareServices

    @Autowired
    List<ModelService> modelServices

    @Autowired(required = false)
    Set<ProfileProviderService> profileProviderServices

    DataModelService dataModelService
    MetadataService metadataService
    ProfileSpecificationProfileService profileSpecificationProfileService
    SessionFactory sessionFactory

    Profile createProfile(ProfileProviderService profileProviderService, MultiFacetAware multiFacetAwareItem) {
        profileProviderService.createProfileFromEntity(multiFacetAwareItem)
    }

    ProfileProviderService findProfileProviderService(String profileNamespace, String profileName, String profileVersion = null) {

        if (profileVersion) {
            return getAllProfileProviderServices().find {
                it.namespace == profileNamespace &&
                it.getName() == profileName &&
                it.version == profileVersion
            }
        }
        getAllProfileProviderServices().findAll {
            it.namespace == profileNamespace &&
            it.getName() == profileName
        }.max()
    }

    MultiFacetAware storeProfile(ProfileProviderService profileProviderService, MultiFacetAware multiFacetAwareItem, Profile profileToStore, User user) {
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(multiFacetAwareItem.domainType)
        profileProviderService.storeProfileInEntity(multiFacetAwareItem, profileToStore, user.emailAddress, service.isMultiFacetAwareFinalised(multiFacetAwareItem))
        service.save(flush: true, validate: false, multiFacetAwareItem)
    }

    Profile validateProfile(ProfileProviderService profileProviderService, Profile submittedProfile) {
        Profile cleanProfile = profileProviderService.createCleanProfileFromProfile(submittedProfile)
        cleanProfile.validate()
        cleanProfile
    }

    void deleteProfile(ProfileProviderService profileProviderService, MultiFacetAware multiFacetAwareItem, User currentUser) {

        Set<Metadata> mds = profileProviderService.getAllProfileMetadataByMultiFacetAwareItemId(multiFacetAwareItem.id)

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

    PaginatedResultList<Profile> getModelsWithProfile(ProfileProviderService profileProviderService,
                                                      UserSecurityPolicyManager userSecurityPolicyManager,
                                                      String domainType,
                                                      Map pagination = [:]) {

        List<Model> models = getAllModelsWithProfile(profileProviderService, userSecurityPolicyManager, domainType)

        List<Profile> profiles = []
        profiles.addAll(models.collect {model ->
            profileProviderService.createProfileFromEntity(model)
        })
        new PaginatedResultList<>(profiles, pagination)
    }

    MultiFacetAwareService findServiceForMultiFacetAwareDomainType(String domainType) {
        MultiFacetAwareService service = multiFacetAwareServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('PS01', "No supporting service for ${domainType}")
        return service
    }

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        findServiceForMultiFacetAwareDomainType(domainType).get(multiFacetAwareItemId)
    }

    boolean isMultiFacetAwareFinalised(MultiFacetAware multiFacetAwareItem) {
        findServiceForMultiFacetAwareDomainType(multiFacetAwareItem.domainType).isMultiFacetAwareFinalised(multiFacetAwareItem)
    }

    Set<String> getUsedNamespaces(MultiFacetAware multiFacetAwareItem) {
        //MetadataService metadataService = grailsApplication.mainContext.getBean('metadataService')
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemId(multiFacetAwareItem.id)
        metadataList.collect {it.namespace} as Set
    }

    Set<ProfileProviderService> getUsedProfileServices(MultiFacetAware multiFacetAwareItem, boolean finalisedOnly = false) {
        Set<String> usedNamespaces = getUsedNamespaces(multiFacetAwareItem)

        getAllProfileProviderServices(finalisedOnly).findAll {
            (usedNamespaces.contains(it.getMetadataNamespace()) &&
             (it.profileApplicableForDomains().contains(multiFacetAwareItem.domainType) || it.profileApplicableForDomains().size() == 0))
        }
    }

    Set<ProfileProviderService> getUnusedProfileServices(MultiFacetAware multiFacetAwareItem, boolean finalisedOnly = false) {
        Set<ProfileProviderService> usedProfiles = getUsedProfileServices(multiFacetAwareItem, finalisedOnly)
        Set<ProfileProviderService> allProfiles = getAllProfileProviderServices(finalisedOnly).findAll {
            it.profileApplicableForDomains().size() == 0 ||
            it.profileApplicableForDomains().contains(multiFacetAwareItem.domainType)
        }
        Set<ProfileProviderService> unusedProfiles = new HashSet<ProfileProviderService>(allProfiles)
        unusedProfiles.removeAll(usedProfiles)
        unusedProfiles
    }

    ProfileProviderService createDynamicProfileServiceFromModel(DataModel dataModel) {

        return new DynamicJsonProfileProviderService(metadataService, dataModel)

    }

    /**
     * @param finalisedOnly If true then exclude dynamic profiles which are not finalised
     * @return
     */
    Set<ProfileProviderService> getAllProfileProviderServices(boolean finalisedOnly = false) {
        // First we'll get the ones we already know about...
        // (Except those that we've already disabled)
        Set<ProfileProviderService> allProfileServices = []
        allProfileServices.addAll(profileProviderServices.findAll {!it.disabled})

        // Now we get all the dynamic models
        List<DataModel> dynamicModels = dataModelService.findAllByMetadataNamespace(
            profileSpecificationProfileService.metadataNamespace)

        List<UUID> dynamicModelIds = dynamicModels.collect {it.id}


        // Now we do a quick check to make sure that none of those are dynamic, and refer to a model that has since been deleted
        allProfileServices.removeAll {profileProviderService ->
            if (profileProviderService.getDefiningDataModel() != null &&
                !dynamicModelIds.contains(profileProviderService.getDefiningDataModel())) {
                // Disable it for next time
                profileProviderService.disabled = true
                return true
            }
            return false
        }

        Set<UUID> alreadyKnownServiceDataModels = allProfileServices.findAll {
            it.getDefiningDataModel() != null
        }.collect {
            it.getDefiningDataModel()
        }

        // Now find any new profile models and create a service for them:
        List<DataModel> newDynamicModels = dynamicModels.findAll {dataModel ->
            (!alreadyKnownServiceDataModels.contains(dataModel.id)) &&
            !(finalisedOnly && !dataModel.finalised)
        }

        allProfileServices.addAll(
            newDynamicModels.collect {dataModel -> createDynamicProfileServiceFromModel(dataModel)}
        )

        return allProfileServices
    }

    Set<ProfileProviderService> getAllDynamicProfileProviderServices() {
        getAllProfileProviderServices().findAll {
            it.definingDataModel != null
        }
    }

    def getMany(UUID modelId, ItemsProfilesDataBinding itemsProfiles) {
        List<ProfileProvided> profiles = []

        itemsProfiles.multiFacetAwareItems.each { multiFacetAwareItemDataBinding ->
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

                itemsProfiles.profileProviderServices.each { profileProviderServiceDataBinding ->
                    ProfileProviderService profileProviderService = findProfileProviderService(
                        profileProviderServiceDataBinding.namespace,
                        profileProviderServiceDataBinding.name,
                        profileProviderServiceDataBinding.version)

                    if (profileProviderService) {
                        ProfileProvided profileProvided = new ProfileProvided()
                        profileProvided.profile = createProfile(profileProviderService, multiFacetAware)
                        profileProvided.profileProviderService = profileProviderService
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
    private handleMany(boolean validateOnly, ProfileProvidedCollection profileProvidedCollection, Model model,
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
                        validated.profile = validateProfile(profileProviderService, submittedInstance)
                        validated.profileProviderService = profileProviderService
                        handledInstances.add(validated)
                    } else {
                        boolean saveAllowed

                        if (profileProviderService.canBeEditedAfterFinalisation()) {
                            saveAllowed = userSecurityPolicyManager.userCanWriteSecuredResourceId(model.class, model.id, 'saveIgnoreFinalise')
                            log.debug("Profile can be edited after finalisation ${profileProviderService.namespace}.${profileProviderService.name} and saveAllowed is ${saveAllowed}")
                        } else {
                            saveAllowed = userSecurityPolicyManager.userCanCreateResourceId(Profile.class, null, model.class, model.id)
                            log.debug("Profile cannot be edited after finalisation ${profileProviderService.namespace}.${profileProviderService.name} and saveAllowed is ${saveAllowed}")
                        }

                        if (saveAllowed) {
                            ProfileProvided saved = new ProfileProvided()
                            MultiFacetAware profiled = storeProfile(profileProviderService, multiFacetAware, submittedInstance, user)
                            // Create the profile as the stored profile may only be segments of the profile and we now want to get everything
                            saved.profile = createProfile(profileProviderService, profiled)
                            saved.profileProviderService = profileProviderService
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
}
