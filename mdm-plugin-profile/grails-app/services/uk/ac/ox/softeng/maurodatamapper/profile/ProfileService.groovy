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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileFieldDataType
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.EmptyJsonProfileFactory
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.core.GrailsApplication
import grails.databinding.DataBindingSource
import grails.gorm.transactions.Transactional
import grails.web.databinding.DataBindingUtils
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired

import java.nio.charset.StandardCharsets
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
    @Autowired
    DataModelService dataModelService
    @Autowired
    ProfileSpecificationProfileService profileSpecificationProfileService


    Profile createProfile(ProfileProviderService profileProviderService, CatalogueItem catalogueItem) {
        profileProviderService.createProfileFromEntity(catalogueItem)
    }

    ProfileProviderService findProfileProviderService(String profileNamespace, String profileName, String profileVersion = null) {

        if (profileVersion) {
            return getStaticAndDynamicProfileServices().find {
                it.namespace == profileNamespace &&
                it.getName() == profileName &&
                it.version == profileVersion
            }
        }
        getStaticAndDynamicProfileServices().findAll {
            it.namespace == profileNamespace &&
            it.getName() == profileName
        }.max()
    }

    void storeProfile(ProfileProviderService profileProviderService, CatalogueItem catalogueItem, HttpServletRequest request, User user) {

        Profile profile = profileProviderService.getNewProfile()
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
                                                      String domainType,
                                                      Map pagination = [:]) {

        List<Model> models = []
        ModelService service = modelServices.find { it.handles(domainType) }
        models.addAll(service.findAllByMetadataNamespace(profileProviderService.metadataNamespace))

        List<Model> validModels = models.findAll {model ->
            userSecurityPolicyManager.userCanReadSecuredResourceId(model.class, model.id) && !model.deleted
        }
        List<Profile> profiles = []
        validModels.each {model ->
            profiles.add(profileProviderService.createProfileFromEntity(model))
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
        getStaticAndDynamicProfileServices().findAll {usedNamespaces.contains(it.getMetadataNamespace())}
    }

    Set<ProfileProviderService> getStaticAndDynamicProfileServices() {
        HashSet<ProfileProviderService> staticServices = profileProviderServices
        HashSet<ProfileProviderService> dynamicServices = getDynamicProfileServices()

        staticServices.addAll(dynamicServices)

        return staticServices
    }

    Set<ProfileProviderService> getDynamicProfileServices() {
        List<DataModel> dynamicModels = dataModelService.findAllByMetadataNamespace(
                profileSpecificationProfileService.metadataNamespace)

        Set<UUID> alreadyKnownServiceDataModels = profileProviderServices.findAll{
            it.getDefiningDataModel() != null
        }.collect{
            it.getDefiningDataModel()
        }

        List<DataModel> newDynamicModels = dynamicModels.findAll{dataModel ->
            !alreadyKnownServiceDataModels.contains(dataModel.id)
        }


        newDynamicModels.collect { dataModel ->

            UUID dataModelId = dataModel.id
            String dataModelLabel = dataModel.label
            String dataModelDescription = dataModel.description
            String dataModelVersion = dataModel.version

            return new ProfileProviderService<JsonProfile, CatalogueItem>() {

                //@Autowired(required = true)
                MetadataService metadataService = grailsApplication.mainContext.getBean('metadataService')

                @Override
                void storeProfileInEntity(CatalogueItem entity, JsonProfile profile, String userEmailAddress) {
                    JsonProfile emptyJsonProfile = new JsonProfile(getSections())

                    emptyJsonProfile.sections.each {section ->
                        ProfileSection submittedSection = profile.sections.find{it.sectionName == section.sectionName }
                        if(submittedSection) {
                            section.fields.each {field ->
                                ProfileField submittedField = submittedSection.fields.find {it.fieldName == field.fieldName }
                                if(submittedField) {
                                    if(submittedField.currentValue && submittedField.metadataPropertyName) {
                                        entity.addToMetadata(metadataNamespace, field.metadataPropertyName, submittedField.currentValue,
                                                userEmailAddress)
                                    } else if(!field.metadataPropertyName) {
                                        log.error("No metadataPropertyName set for field: " + field.fieldName)
                                    }
                                }
                            }
                        }
                    }
                    //entity.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)
                    Metadata.saveAll(entity.metadata)
                }

                @Override
                JsonProfile createProfileFromEntity(CatalogueItem entity) {
                    JsonProfile jsonProfile = new JsonProfile(getSections())
                    jsonProfile.catalogueItemId = entity.id
                    jsonProfile.catalogueItemDomainType = entity.domainType
                    jsonProfile.catalogueItemLabel = entity.label
                    List<Metadata> metadataList = metadataService.findAllByCatalogueItemIdAndNamespace(entity.id, this.getMetadataNamespace())

                    metadataList.each {}
                    jsonProfile.sections.each {section ->
                        section.fields.each { field ->
                            Metadata matchingField = metadataList.find {it.key == field.metadataPropertyName }
                            if(matchingField) {
                                field.currentValue = matchingField.value
                            } else {
                                field.currentValue = ""
                            }
                        }
                    }
                    jsonProfile
                }

                @Override
                String getMetadataNamespace() {
                    Metadata md = getProfileDataModel().metadata.find {md ->
                        md.namespace == "uk.ac.ox.softeng.profile" &&
                            md.key == "metadataNamespace"}
                    if(md) {
                        return md.value
                    } else {
                        log.error("Invalid namespace!!")
                        return "invalid.namespace"
                    }
                }

                @Override
                String getDisplayName() {
                    return dataModelLabel
                }

                @Override
                String getName() {
                    return URLEncoder.encode(dataModelLabel, StandardCharsets.UTF_8);
                }

                @Override
                String getVersion() {
                    return dataModelVersion
                }

                @Override
                List<String> profileApplicableForDomains() {
                    Metadata md = getProfileDataModel().metadata.find {md ->
                        md.namespace == "uk.ac.ox.softeng.profile" &&
                                md.key == "domainsApplicable"}
                    if(md) {
                        return md.value.tokenize(";")
                    } else {
                        return []
                    }
                }

                DataModel getProfileDataModel() {
                    DataModel.findById(dataModelId)
                }

                @Override
                UUID getDefiningDataModel() {
                    return dataModelId
                }

                List<ProfileSection> getSections() {
                    dataModel.dataClasses.sort { it.order }.collect() { dataClass ->
                        new ProfileSection(
                                sectionName: dataClass.label,
                                sectionDescription: dataClass.description,
                                fields: dataClass.dataElements.sort { it.order }.collect { dataElement ->
                                    new ProfileField(
                                            fieldName: dataElement.label,
                                            description: dataElement.description,
                                            metadataPropertyName: dataElement.metadata.find {
                                                it.namespace == "uk.ac.ox.softeng.profile.field" &&
                                                        it.key == "metadataPropertyName"
                                            }?.value,
                                            maxMultiplicity: dataElement.maxMultiplicity,
                                            minMultiplicity: dataElement.minMultiplicity,
                                            dataType: (dataElement.dataType instanceof EnumerationType) ? 'enumeration' : dataElement.dataType.label,
                                            allowedValues: (dataElement.dataType instanceof EnumerationType) ?
                                                    ((EnumerationType) dataElement.dataType).enumerationValues.collect { it.key } : [],
                                            currentValue: ""
                                    )
                                }
                        )
                    }
                }
            }
        }

    }

}
