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
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.profile.object.MapBasedProfile

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
@Deprecated
abstract class MapBasedDataModelProfileProviderService<P extends MapBasedProfile> extends DataModelProfileProviderService<P> {

    @Autowired
    DataModelService dataModelService

    @Autowired
    GrailsApplication grailsApplication

    abstract String getTitleFieldName()

    abstract String getDescriptionFieldName()

    abstract String getAuthorFieldName()

    abstract String getOrganisationFieldName()

    @Override
    P createProfileFromEntity(DataModel entity) {
        P profile = getNewProfile()

        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemId(entity.id)

        getKnownMetadataKeys().each { fieldName ->

            Metadata metadata = metadataList.find {
                it.namespace == metadataNamespace &&
                it.key == fieldName
            }
            if (metadata) {
                profile[fieldName] = metadata.value
            }
            if (fieldName == getTitleFieldName()) {
                profile[fieldName] = entity.label
            }
            if (fieldName == getDescriptionFieldName() && entity.description) {
                profile[fieldName] = entity.description
            }
            if (fieldName == getAuthorFieldName() && entity.author) {
                profile[fieldName] = entity.author
            }
            if (fieldName == getOrganisationFieldName() && entity.organisation) {
                profile[fieldName] = entity.organisation
            }
        }
        profile.id = entity.id.toString()
        profile
    }


    @Override
    void storeProfileInEntity(DataModel entity, P profile, String userEmailAddress, boolean isEntityFinalised = false) {
        profile.each { fieldName, value ->
            if (value) {
                entity.addToMetadata(metadataNamespace, fieldName, value.toString(), userEmailAddress)
                if (fieldName == getTitleFieldName()) {
                    entity.label = value.toString()
                }
                if (fieldName == getDescriptionFieldName()) {
                    entity.description = value.toString()
                }
                if (fieldName == getAuthorFieldName()) {
                    entity.author = value.toString()
                }
                if (fieldName == getOrganisationFieldName()) {
                    entity.organisation = value.toString()
                }
            }
        }
        entity.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)

        dataModelService.save(entity)
    }
}
