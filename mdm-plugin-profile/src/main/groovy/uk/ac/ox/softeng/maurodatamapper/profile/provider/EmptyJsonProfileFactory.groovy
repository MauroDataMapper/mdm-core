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

import groovy.json.JsonSlurper

@Singleton
class EmptyJsonProfileFactory {


    private Map<String, List<ProfileSection>> loadedProfiles = [:]


    JsonProfile getEmptyProfile(JsonProfileProviderService jsonProfileProviderService) {
        List<ProfileSection> profileSections = []
        if(loadedProfiles[jsonProfileProviderService.metadataNamespace]) {
            profileSections.addAll(loadedProfiles[jsonProfileProviderService.metadataNamespace].collect { it.clone() })
        } else {
            List<ProfileSection> emptySections = []
            String resourceFile = jsonProfileProviderService.getJsonResourceFile()
            def sectionMap = new JsonSlurper().parse(jsonProfileProviderService.getClass().classLoader.getResource(resourceFile))
            sectionMap.each { Map it ->
                ProfileSection profileSection = new ProfileSection(it)
                emptySections.add(profileSection)
                profileSections.add(profileSection.clone())
            }
            loadedProfiles[jsonProfileProviderService.metadataNamespace] = profileSections

        }
        return new JsonProfile(profileSections)
    }




}
