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
package uk.ac.ox.softeng.maurodatamapper.profile.object

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.validation.Validateable
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import groovy.transform.stc.SimpleType

@SuppressFBWarnings('NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE')
abstract class Profile implements Comparable<Profile>, Validateable {

    List<ProfileSection> sections
    String domainType
    String label
    UUID id
    Closure customSectionsValidation

    Profile() {
        sections = []
    }

    Profile(List<ProfileSection> sections) {
        super()
        this.sections = sections
    }

    static constraints = {
        id nullable: false
        label nullable: false, blank: false
        sections minSize: 1
        customSectionsValidation nullable: true
    }

    abstract Set<String> getKnownFields()

    List<ProfileSection> getSections() {
        // Maintain original order the sections were added in
        sections.eachWithIndex {entry, i ->
            if (!entry.order) entry.order = i
        }
        sections.sort()
    }

    @Override
    boolean validate(List fieldsToValidate, Map<String, Object> params, Closure<?>... adHocConstraintsClosures) {
        if (!params?.currentValuesOnly) {
            Validateable.super.validate null, params, null
        }
        List<ProfileSection> sortedSections = getSections()
        for (i in 0..<sortedSections.size()) {

            ProfileSection sec = sortedSections[i]
            sec.validate((Map<String, Object>) params)
            if (sec.hasErrors()) {
                sec.errors.fieldErrors.each {err ->
                    this.errors.rejectValue("sections[${i}].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
        customSectionsValidation?.call(sections, this.errors)
        !hasErrors()
    }

    boolean validateCurrentValues() {
        validate(currentValuesOnly: (Object) true)
    }

    List<ProfileField> getAllFields() {
        sections.collectMany {it.fields}
    }

    Profile addToSections(@DelegatesTo(value = ProfileSection, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        sections.add(new ProfileSection().tap(closure))
        this
    }

    Profile customSectionsValidation(@ClosureParams(value = FromString,
        options = ['java.util.List<uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection>,org.springframework.validation.Errors']) Closure closure) {
        customSectionsValidation = closure
        this
    }

    ProfileSection find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection') Closure closure) {
        sections.find closure
    }

    List<ProfileSection> each(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection') Closure closure) {
        sections.each closure
    }
}