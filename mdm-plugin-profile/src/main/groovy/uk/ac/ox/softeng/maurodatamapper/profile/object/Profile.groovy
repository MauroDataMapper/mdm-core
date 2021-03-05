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

import groovy.transform.CompileStatic

@CompileStatic
abstract class Profile implements Comparable<Profile> {

    abstract def getField(String fieldName)

    abstract void setField(String fieldName, Object value)

    abstract Set<String> getKnownFields()

    boolean simpleFilter(Map params) {
        boolean result = true
        getKnownFields().each { profileFieldName ->
            if (!params[profileFieldName]) {
                // no filter on this field... we don't care
            } else if (params[profileFieldName] instanceof String) {
                String filterField = params[profileFieldName]
                if (this[profileFieldName] != filterField) {
                    result = false
                }

            } else {
                // We've got a set of filters
                List<String> filters = new ArrayList<>((List<String>) params[profileFieldName])
                boolean found = false
                filters.each { filter ->
                    if (getField(profileFieldName) == filter) {
                        found = true
                    }
                }
                result &= found
            }
        }
        result
    }

    abstract List<ProfileSection> getSections()

    abstract void fromSections(List<ProfileSection> profileSections)

}