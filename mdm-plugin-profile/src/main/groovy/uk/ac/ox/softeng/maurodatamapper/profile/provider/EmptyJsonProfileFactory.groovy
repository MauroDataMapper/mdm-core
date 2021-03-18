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

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

import grails.io.IOUtils
import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets

@Singleton
class EmptyJsonProfileFactory {


    private Map<String, String> loadedProfileJsons = [:]

    private JsonSlurper jsonSlurper = new JsonSlurper()


    JsonProfile getEmptyProfile(JsonProfileProviderService jsonProfileProviderService) {

        String jsonStructure
        if(loadedProfileJsons[jsonProfileProviderService.metadataNamespace]) {
            jsonStructure = loadedProfileJsons[jsonProfileProviderService.metadataNamespace]
        } else {
            String resourceFile = jsonProfileProviderService.getJsonResourceFile()
            InputStream inputStream = jsonProfileProviderService.class.classLoader.getResourceAsStream(resourceFile)
            jsonStructure = IOUtils.toString(inputStream, StandardCharsets.UTF_8 as String)
            loadedProfileJsons[jsonProfileProviderService.metadataNamespace] = jsonStructure

        }
        def sectionList = jsonSlurper.parseText(jsonStructure)
        List<ProfileSection> profileSections = []
        sectionList.each { Map it ->
            ProfileSection profileSection = new ProfileSection(it)
            profileSections.add((ProfileSection) profileSection)
        }
        return new JsonProfile(profileSections)
    }
}
