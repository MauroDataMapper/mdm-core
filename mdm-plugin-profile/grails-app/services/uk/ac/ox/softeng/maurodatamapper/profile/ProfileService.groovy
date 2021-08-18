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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DynamicJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Transactional
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

@Transactional
class ProfileService {

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
        profileProviderService.storeProfileInEntity(multiFacetAwareItem, profileToStore, user)
        MultiFacetAwareService service = multiFacetAwareServices.find {it.handles(multiFacetAwareItem.domainType)}
        if (!service) throw new ApiBadRequestException('CIAS02', "Facet retrieval for catalogue item [${multiFacetAwareItem.domainType}] with no supporting service")
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

    MultiFacetAware findMultiFacetAwareItemByDomainTypeAndId(String domainType, UUID multiFacetAwareItemId) {
        MultiFacetAwareService service = multiFacetAwareServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CIAS02', "Facet retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(multiFacetAwareItemId)
    }

    Set<String> getUsedNamespaces(MultiFacetAware multiFacetAwareItem) {
        //MetadataService metadataService = grailsApplication.mainContext.getBean('metadataService')
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemId(multiFacetAwareItem.id)
        metadataList.collect {it.namespace} as Set
    }

    Set<ProfileProviderService> getUsedProfileServices(MultiFacetAware multiFacetAwareItem) {
        Set<String> usedNamespaces = getUsedNamespaces(multiFacetAwareItem)

        getAllProfileProviderServices().findAll {
            (usedNamespaces.contains(it.getMetadataNamespace()) &&
             it.profileApplicableForDomains().contains(multiFacetAwareItem.domainType))
        }
    }

    Set<ProfileProviderService> getUnusedProfileServices(MultiFacetAware multiFacetAwareItem) {
        Set<ProfileProviderService> usedProfiles = getUsedProfileServices(multiFacetAwareItem)
        Set<ProfileProviderService> allProfiles = getAllProfileProviderServices().findAll {
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

    Set<ProfileProviderService> getAllProfileProviderServices() {
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
            !alreadyKnownServiceDataModels.contains(dataModel.id)
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

}
