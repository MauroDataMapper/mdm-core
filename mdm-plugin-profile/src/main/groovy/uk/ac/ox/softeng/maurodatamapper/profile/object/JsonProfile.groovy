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

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection

import groovy.util.logging.Slf4j

@Slf4j
class JsonProfile extends Profile {

    List<ProfileSection> sections
    String domainType
    String label
    UUID id

    // Empty constructor used for deserialization from Json
    JsonProfile() {
        super()
        sections = []
    }

    JsonProfile(List<ProfileSection> sections) {
        super()
        this.sections = sections
    }

    static constraints = {
        label blank: false
        domainType blank: false
        sections minSize: 1
    }

    @Override
    Set<String> getKnownFields() {
        List<String> fields = []
        sections.each {section ->
            section.fields.each {field ->
                if (field.metadataPropertyName) {
                    fields.add(field.metadataPropertyName)
                } else {
                    log.info("No metadataPropertyName set for field: " + field.fieldName)
                    fields.add("${section.name}/${field.fieldName}")
                }
            }
        }
        return fields
    }

    @Override
    int compareTo(Profile o) {
        return 0
    }
}
