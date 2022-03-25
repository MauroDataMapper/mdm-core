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
package uk.ac.ox.softeng.maurodatamapper.profile.domain

import grails.validation.Validateable
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

class ProfileSection implements Cloneable, Validateable {

    String name
    String description
    List<ProfileField> fields = []

    static constraints = {
        name blank: false
        description nullable: true, blank: false
        fields minSize: 1
    }

    @Override
    boolean validate(List fieldsToValidate, Map<String, Object> params, Closure<?>... adHocConstraintsClosures) {
        if (!params?.currentValuesOnly) {
            Validateable.super.validate null, params, null
        }
        fields.eachWithIndex {field, i ->
            if (params?.currentValuesOnly) {
                field.validate(['currentValue'], (Map<String, Object>) params)
            } else {
                field.validate((Map<String, Object>) params)
            }
            if (field.hasErrors()) {
                field.errors.fieldErrors.each {err ->
                    this.errors.rejectValue("fields[$i].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
        !hasErrors()
    }

    boolean validateCurrentValues() {
        Map<String, Object> params = [currentValuesOnly: (Object) true]
        validate(params)
    }

    void setSectionName(String name) {
        this.name = name
    }

    void setSectionDescription(String description) {
        this.description = description
    }

    ProfileField find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField') Closure closure) {
        fields.find closure
    }

    List<ProfileField> each(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField') Closure closure) {
        fields.each closure
    }

    Map<String, String> getFlatFieldMap() {
        fields.findAll {it.currentValue}.collectEntries {[it.fieldName, it.currentValue]}
    }
}
