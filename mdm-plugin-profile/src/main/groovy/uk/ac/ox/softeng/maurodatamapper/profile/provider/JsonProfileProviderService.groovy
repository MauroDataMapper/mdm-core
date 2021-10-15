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
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

abstract class JsonProfileProviderService extends ProfileProviderService<JsonProfile, MultiFacetAware> {

    abstract String getJsonResourceFile()

    @Override
    JsonProfile createProfileFromEntity(MultiFacetAware entity) {
        JsonProfile jsonProfile = getNewProfile()
        jsonProfile.id = entity.id
        jsonProfile.domainType = entity.domainType
        jsonProfile.label = entity.label

        List<Metadata> metadataList = getAllProfileMetadataByMultiFacetAwareItemId(entity.id)
        jsonProfile.sections.each {section ->
            section.fields.each {field ->
                Metadata matchingField = metadataList.find {it.key == field.metadataPropertyName}
                // If field is derived then get the location its derived from
                if (field.derived) {
                    try {
                        field.currentValue = entity."${field.derivedFrom}"
                    } catch (Exception ignored) {
                        // Currently gracefully handle a derived field which cant be found
                        log.warn('Could not set derived field from {} for {}', field.derivedFrom, entity.label)
                    }
                } else if (matchingField) {
                    field.currentValue = matchingField.value
                }
                field.validate()
            }
        }
        updateUneditableFields(jsonProfile)
    }

    JsonProfile getNewProfile() {
        EmptyJsonProfileFactory.instance.getEmptyProfile(this)
    }

    @Override
    void storeProfileInEntity(MultiFacetAware entity, JsonProfile jsonProfile, String userEmailAddress, boolean isEntityFinalised = false) {
        JsonProfile emptyJsonProfile = getNewProfile()
        emptyJsonProfile.sections.each {section ->
            ProfileSection submittedSection = jsonProfile.sections.find {it.name == section.name}
            if (submittedSection) {
                section.fields.each {field ->
                    ProfileField submittedField = findFieldInSubmittedSection(submittedSection, section.name, field.getUniqueKey(section.name))
                    if (submittedField) {
                        // Dont allow derived or uneditable fields to be set
                        if (!field.derived && !field.uneditable && (!isEntityFinalised || field.editableAfterFinalisation)) {
                            String newValue = submittedField.currentValue ?: ''
                            String key = field.getUniqueKey(submittedSection.name)
                            storeFieldInEntity(entity, newValue, key, userEmailAddress)
                        }
                    }

                }
            }
        }
        entity.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)

        entity.findMetadataByNamespace(metadataNamespace).each {md ->
            metadataService.save(md)
        }
    }

    ProfileField findFieldInSubmittedSection(ProfileSection submittedSection, String sectionNameToSearch, String profileFieldNameToFind) {
        submittedSection.find {it.getUniqueKey(sectionNameToSearch) == profileFieldNameToFind}
    }

    void storeFieldInEntity(MultiFacetAware entity, String value, String key, String userEmailAddress) {
        if (!key) return
        if (value) {
            entity.addToMetadata(metadataNamespace, key, value, userEmailAddress)
        } else {
            Metadata md = entity.metadata.find {
                it.namespace == metadataNamespace && it.key == key
            }
            if (md) {
                entity.metadata.remove(md)
                metadataService.delete(md)
            }
        }
    }


    @Override
    Set<String> getKnownMetadataKeys() {
        getNewProfile().getKnownFields()
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    JsonProfile createCleanProfileFromProfile(JsonProfile submittedProfile) {
        JsonProfile cleanProfile = super.createCleanProfileFromProfile(submittedProfile) as JsonProfile
        cleanProfile.domainType = submittedProfile.domainType
        cleanProfile.id = submittedProfile.id
        cleanProfile.label = submittedProfile.label
        updateUneditableFields(cleanProfile)
    }

    /**
     * Method which can be overriden to set any uneditable fields.
     * Default behaviour is to do nothing.
     * @param jsonProfile
     * @return
     */
    JsonProfile updateUneditableFields(JsonProfile jsonProfile) {
        jsonProfile
    }

    void findAndSetProfileField(ProfileSection profileSection, String metadataPropertyName, String value, boolean replaceExistingValue = false) {
        ProfileField fieldToUpdate = profileSection.find {it.metadataPropertyName == metadataPropertyName}
        if (!fieldToUpdate.currentValue || replaceExistingValue) {
            fieldToUpdate.currentValue = value
        }
    }
}
