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
        JsonProfile jsonProfile = EmptyJsonProfileFactory.instance.getEmptyProfile(this)
        jsonProfile.profiledItemId = entity.id
        jsonProfile.profiledItemDomainType = entity.domainType
        jsonProfile.profiledItemLabel = entity.label

        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(entity.id, this.getMetadataNamespace())

        jsonProfile.sections.each {section ->
            section.fields.each {field ->
                Metadata matchingField = metadataList.find {it.key == field.metadataPropertyName}
                if (matchingField) {
                    field.currentValue = matchingField.value
                } else {
                    field.currentValue = ""
                }
                field.validate()
            }
        }
        jsonProfile
    }

    JsonProfile createNewEmptyJsonProfile() {
        EmptyJsonProfileFactory.instance.getEmptyProfile(this)
    }

    @Override
    void storeProfileInEntity(MultiFacetAware entity, JsonProfile jsonProfile, String userEmailAddress) {
        JsonProfile emptyJsonProfile = createNewEmptyJsonProfile()
        emptyJsonProfile.sections.each {section ->
            ProfileSection submittedSection = jsonProfile.sections.find {it.sectionName == section.sectionName}
            if (submittedSection) {
                section.fields.each {field ->
                    ProfileField submittedField = submittedSection.fields.find {it.fieldName == field.fieldName}

                    String newValue = submittedField.currentValue ?: ""
                    String key = field.getMetadataKeyForSaving(submittedSection.sectionName)
                    storeFieldInEntity(entity, newValue, key, userEmailAddress)
                }
            }
        }
        entity.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)
        Metadata.saveAll(entity.metadata)
    }

    void storeFieldInEntity(MultiFacetAware entity, String value, String key, String userEmailAddress) {

        if (value && value != "" && key ) {
            entity.addToMetadata(metadataNamespace, key, value, userEmailAddress)
        } else {
            Metadata md = entity.metadata.find {
                it.namespace == metadataNamespace && it.key == key
            }
            if (md) {
                metadataService.delete(md)
            }
        }
    }


    @Override
    Set<String> getKnownMetadataKeys() {
        EmptyJsonProfileFactory.instance.getEmptyProfile(this).getKnownFields()
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }


}
