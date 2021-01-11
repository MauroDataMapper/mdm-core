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
package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

abstract class JsonProfileProviderService extends ProfileProviderService<JsonProfile, CatalogueItem> {

    abstract String getJsonResourceFile()

    @Override
    JsonProfile createProfileFromEntity(CatalogueItem entity) {
        JsonProfile jsonProfile = EmptyJsonProfileFactory.instance.getEmptyProfile(this)
        jsonProfile.catalogueItemId = entity.id
        jsonProfile.catalogueItemDomainType = entity.domainType

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
    void storeProfileInEntity(CatalogueItem catalogueItem, JsonProfile jsonProfile, String userEmailAddress) {
        jsonProfile.sections.each {section ->
            section.fields.each {field ->
                if(field.currentValue && field.metadataPropertyName) {
                    catalogueItem.addToMetadata(metadataNamespace, field.metadataPropertyName, field.currentValue, userEmailAddress)
                } else if(!field.metadataPropertyName) {
                    log.error("No metadataPropertyName set for field: " + field.fieldName)
                }
            }
        }
        catalogueItem.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)


        metadataService.saveAll(catalogueItem.metadata)
    }

    @Override
    Set<String> getKnownMetadataKeys() {
        EmptyJsonProfileFactory.instance.getEmptyProfile(this).getKnownFields()
    }



}
