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
package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

import java.nio.charset.StandardCharsets

/**
 * @since 28/04/2021
 */
class DynamicJsonProfileProviderService extends JsonProfileProviderService {

    UUID dataModelId
    String dataModelLabel
    String dataModelDescription
    String dataModelVersion

    DynamicJsonProfileProviderService(MetadataService metadataService, DataModel dataModel) {
        this.metadataService = metadataService
        this.dataModelId = dataModel.id
        this.dataModelLabel = dataModel.label
        this.dataModelDescription = dataModel.description
        this.dataModelVersion = dataModel.modelVersionTag ?: dataModel.modelVersion ?: dataModel.branchName
    }

    @Override
    JsonProfile createProfileFromEntity(MultiFacetAware entity) {
        JsonProfile jsonProfile = new JsonProfile(getSections())
        jsonProfile.id = entity.id
        jsonProfile.domainType = entity.domainType
        jsonProfile.label = entity.label
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(entity.id, this.getMetadataNamespace())

        jsonProfile.sections.each {section ->
            section.fields.each {field ->
                Metadata matchingField = metadataList.find {it.key == field.getUniqueKey(section.name)}
                if (field.derived) {
                    try {
                        field.currentValue = entity."${field.derivedFrom}"
                    } catch (Exception ignored) {
                        // Currently gracefully handle a derived field which cant be found
                        log.warn('Could not set dervied field from {} for {}', field.derivedFrom, entity.label)
                    }
                } else if (matchingField) {
                    field.currentValue = matchingField.value
                } else {
                    field.currentValue = ""
                }
                field.validate()
            }
        }
        jsonProfile
    }

    @Override
    JsonProfile getNewProfile() {
        log.debug("Creating new profile from Data Model {}", getProfileDataModel())
        createProfileFromEntity(getProfileDataModel())
    }

    @Override
    Set<String> getKnownMetadataKeys() {
        Set<String> knownProperties = []
        getSections().each {section ->
            section.fields.each {field ->
                knownProperties.add(field.getUniqueKey(section.name))
            }
        }
        return knownProperties
    }

    @Override
    String getJsonResourceFile() {
        return null
    }

    @Override
    String getMetadataNamespace() {
        if (!getProfileDataModel()) {
            return null
        }
        Metadata md = getProfileDataModel().metadata.find {md ->
            md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
            md.key == "metadataNamespace"
        }
        if (md) {
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
            md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
            md.key == "domainsApplicable"
        }
        if (md) {
            return md.value.tokenize(";")
        } else {
            return []
        }
    }

    @Override
    Boolean canBeEditedAfterFinalisation() {
        Metadata md = getProfileDataModel().metadata.find {md ->
            md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
            md.key == "editableAfterFinalisation"
        }
        md ? md.value.toBoolean() : false
    }

    DataModel getProfileDataModel() {
        DataModel.findById(dataModelId)
    }

    @Override
    UUID getDefiningDataModel() {
        return dataModelId
    }

    @Override
    String getDefiningDataModelLabel() {
        return dataModelLabel
    }

    @Override
    String getDefiningDataModelDescription() {
        return dataModelDescription
    }


    List<ProfileSection> getSections() {
        DataModel dm = getProfileDataModel()
        dm.dataClasses.sort {it.order}.collect() {dataClass ->
            new ProfileSection(
                name: dataClass.label,
                description: dataClass.description,
                fields: dataClass.dataElements.sort {it.order}.collect {dataElement ->
                    new ProfileField(
                        fieldName: dataElement.label,
                        description: dataElement.description,
                        metadataPropertyName: dataElement.metadata.find {
                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                            it.key == "metadataPropertyName"
                        }?.value,
                        maxMultiplicity: dataElement.maxMultiplicity ?: 1,
                        minMultiplicity: dataElement.minMultiplicity ?: 0,
                        dataType: (dataElement.dataType instanceof EnumerationType) ? 'enumeration' : dataElement.dataType.label,
                        regularExpression: dataElement.metadata.find {
                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                            it.key == "regularExpression"
                        }?.value,
                        allowedValues: (dataElement.dataType instanceof EnumerationType) ?
                                       ((EnumerationType) dataElement.dataType).enumerationValues.collect {it.key} : [],
                        defaultValue: dataElement.metadata.find {
                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                            it.key == "defaultValue"
                        }?.value,
                        currentValue: '',
                        editableAfterFinalisation: {
                            Metadata md = dataElement.metadata.find {
                                it.namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.dataelement' &&
                                it.key == 'editableAfterFinalisation'
                            }
                            md ? md.value.toBoolean() : true
                        }()
                    )
                }
            )
        }
    }
}