/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

import grails.io.IOUtils
import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets

@Singleton
class EmptyJsonProfileFactory {

    private static final Map<String, String> loadedProfileJsonStructures = [:]

    private static final JsonSlurper jsonSlurper = new JsonSlurper()

    JsonProfile getEmptyProfile(JsonProfileProviderService jsonProfileProviderService) {
        String jsonStructure = getOrLoadProfileStructure(jsonProfileProviderService)

        def sectionList = jsonSlurper.parseText(jsonStructure)
        List<ProfileSection> profileSections = sectionList.collect {Map sectionMap ->
            List<Map> fields = (List<Map>) sectionMap.fields
            sectionMap.fields = []
            ProfileSection profileSection = new ProfileSection(sectionMap)
            profileSection.fields = fields.collect {Map field ->
                new ProfileField(field).tap {
                    // If the profile cannot be editted after finalisation then none of its fields should be
                    // however if the profile can be then by default all the fields will be set to true unless this imported json changes this
                    if (!jsonProfileProviderService.canBeEditedAfterFinalisation()) editableAfterFinalisation = false
                }
            }
            profileSection
        }
        new JsonProfile(profileSections)
    }

    private String getOrLoadProfileStructure(JsonProfileProviderService jsonProfileProviderService) {
        if (!loadedProfileJsonStructures[jsonProfileProviderService.getUniqueIdentifierKey()]) {
            loadedProfileJsonStructures[jsonProfileProviderService.getUniqueIdentifierKey()] = loadNewProfileStructure(jsonProfileProviderService)
        }
        loadedProfileJsonStructures[jsonProfileProviderService.getUniqueIdentifierKey()]
    }

    private String loadNewProfileStructure(JsonProfileProviderService jsonProfileProviderService) {
        String resourceFile = jsonProfileProviderService.getJsonResourceFile()
        InputStream inputStream = jsonProfileProviderService.class.classLoader.getResourceAsStream(resourceFile)
        IOUtils.toString(inputStream, StandardCharsets.UTF_8 as String)
    }
}
