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
package uk.ac.ox.softeng.maurodatamapper.profile.domain

import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class ProfileField {

    String fieldName
    String metadataPropertyName
    String description
    Integer maxMultiplicity
    Integer minMultiplicity
    List<String> allowedValues
    String regularExpression

    List<String> validationErrors = []

    ProfileFieldDataType dataType

    String currentValue

    void setDataType(ProfileFieldDataType type) {
        dataType = type
    }

    void setDataType(String type) {
        dataType = ProfileFieldDataType.findForLabel(type)
    }

    void validate() {
        List<String> errors = []
        if(minMultiplicity > 0 && (!currentValue || currentValue == "")) {
            errors.add("This field is mandatory")
        }
        if(currentValue && currentValue == "") {
            if(allowedValues && !allowedValues.contains(currentValue)) {
                errors.add("This field does not take of the pre-specified values")
            }
            if(regularExpression && regularExpression != "") {
                if(!currentValue.matches(regularExpression)) {
                    errors.add("This field does not match the specified regular expression")
                }
            }
            String typeError = dataType.validateString(currentValue)
            if(typeError && typeError != "") {
                errors.add(typeError)
            }
        }

        validationErrors = errors
    }

}
