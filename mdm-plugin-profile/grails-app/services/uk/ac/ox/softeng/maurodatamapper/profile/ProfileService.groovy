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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.core.GrailsApplication
import grails.databinding.DataBindingSource
import grails.gorm.transactions.Transactional
import grails.web.databinding.DataBindingUtils
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired

import javax.servlet.http.HttpServletRequest

@Transactional
class ProfileService {

    GrailsApplication grailsApplication

    @Autowired
    List<CatalogueItemService> catalogueItemServices

    @Autowired
    List<ModelService> modelServices

    @Autowired(required = false)
    Set<ProfileProviderService> profileProviderServices

    Profile createProfile(ProfileProviderService profileProviderService, CatalogueItem catalogueItem) {
        profileProviderService.createProfileFromEntity(catalogueItem)
    }

    ProfileProviderService findProfileProviderService(String profileNamespace, String profileName, String profileVersion = null) {

        if (profileVersion) {
            return profileProviderServices.find {
                it.namespace == profileNamespace &&
                it.name == profileName &&
                it.version == profileVersion
            }
        }
        profileProviderServices.findAll {
            it.namespace == profileNamespace &&
            it.name == profileName
        }.max()
    }

    void storeProfile(ProfileProviderService profileProviderService, CatalogueItem catalogueItem, HttpServletRequest request, User user) {

        Profile profile = profileProviderService.getNewProfile()
        //System.err.println(request.getJSON())
        List<ProfileSection> profileSections = []
        request.getJSON().sections.each { it ->
            ProfileSection profileSection = new ProfileSection(it)
            profileSection.fields = []
            it['fields'].each { field ->
                profileSection.fields.add(new ProfileField(field))
            }
            if(profileProviderService.isJsonProfileService()) {
                ((JsonProfile)profile).sections.add(profileSection)

            } else {
                profileSections.add(profileSection)
            }
        }

        if(!profileProviderService.isJsonProfileService()) {
            profile.fromSections(profileSections)
        }

            /*final DataBindingSource bindingSource = DataBindingUtils.createDataBindingSource(grailsApplication, profile.getClass(), request)
             bindingSource.propertyNames.each { propertyName ->
                 profile.setField(propertyName, bindingSource[propertyName])
             }

              */

        profileProviderService.storeProfileInEntity(catalogueItem, profile, user)
    }

    PaginatedResultList<Profile> getModelsWithProfile(ProfileProviderService profileProviderService,
                                                      UserSecurityPolicyManager userSecurityPolicyManager,
                                                      Map pagination = [:]) {
        List<Model> models = []

        modelServices.each { service ->
            models.addAll(service.findAllByMetadataNamespace(profileProviderService.metadataNamespace))
        }
        List<Profile> profiles = []
        models.each { model ->
            if (userSecurityPolicyManager.userCanReadSecuredResourceId(model.class, model.id) && !model.deleted) {
                Profile profile = profileProviderService.createProfileFromEntity(model)
                if (profile.simpleFilter(pagination)) {
                    profiles.add(profile)
                }
            }
        }
        new PaginatedResultList<>(profiles, pagination)
    }

    CatalogueItem findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        CatalogueItemService service = catalogueItemServices.find { it.handles(domainType) }
        if (!service) throw new ApiBadRequestException('CIAS02', "Facet retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }

    List<String> getUsedNamespaces(CatalogueItem catalogueItem) {
        catalogueItem.metadata.collect {it.namespace }
    }

    Set<ProfileProviderService> getUsedProfileServices(CatalogueItem catalogueItem) {
        List<String> usedNamespaces = getUsedNamespaces(catalogueItem)
        profileProviderServices.findAll {usedNamespaces.contains(it.getMetadataNamespace())}
    }

}
