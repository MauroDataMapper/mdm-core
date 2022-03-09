/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import grails.validation.Validateable
import groovy.transform.CompileStatic

@CompileStatic
abstract class Profile implements Comparable<Profile>, Validateable {

    List<ProfileSection> sections
    String domainType
    String label
    UUID id

    abstract Set<String> getKnownFields()

    @Override
    boolean validate() {
        sections.eachWithIndex {sec, i ->
            sec.validate()
            if (sec.hasErrors()) {
                sec.errors.fieldErrors.each {err ->
                    this.errors.rejectValue("sections[$i].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
        !hasErrors()
    }

    List<ProfileField> getAllFields() {
        sections.collectMany {it.fields}
    }
}