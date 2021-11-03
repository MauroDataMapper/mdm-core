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
package uk.ac.ox.softeng.maurodatamapper.profile.object

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
@Deprecated
abstract class MapBasedProfile extends Profile {

    private Map<String, Object> contents

    MapBasedProfile() {
        contents = new HashMap<>()
    }

    Object getField(String fieldName) {
        contents[fieldName]
    }

    UUID getId() {
        contents.id as UUID
    }

    void setId(String id) {
        contents.id = Utils.toUuid(id)
    }

    Map<String, Object> each(@ClosureParams(value = FromString, options = ['Map<String,Object>', 'String,Object']) Closure closure) {
        contents.each(closure)
    }

    def propertyMissing(String name) {
        contents[name]
    }

    def propertyMissing(String name, def arg) {
        contents[name] = arg
    }

    void setField(String fieldName, def value) {
        contents[fieldName] = value
    }

    Map<String, Object> getMap() {
        contents
    }

    // Expect this to be overridden
    @Override
    List<ProfileSection> getSections() {
        ProfileSection profileSection = new ProfileSection(name: this.class.name, description: '')
        contents.sort {it.key }.each {
            ProfileField profileField = new ProfileField(fieldName: it.key, currentValue: it.value.toString(), metadataPropertyName: it.key)
            profileSection.fields.add(profileField)
        }
        return [profileSection]
    }
}
