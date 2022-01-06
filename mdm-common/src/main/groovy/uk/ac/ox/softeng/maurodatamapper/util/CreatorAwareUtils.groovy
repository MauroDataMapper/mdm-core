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
package uk.ac.ox.softeng.maurodatamapper.util

import groovy.util.logging.Slf4j

import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

@Slf4j
class CreatorAwareUtils {

    /**
     * Set the value of a deep field on a domain. For example, if the domain is a DataModel,
     * and fieldName is dataType.label, then set the field domain.dataType.label
     *
     * This method is needed because when fieldName contains a . to represent the parts
     * of a deep field name, the simple domain."${fieldName}" does not work.
     *
     * For example, domain."dataType" would correctly return the dataType field of domain. But
     * to return the label of dataType, we need to do domain."dataType"."label" rather than domain."dataType.label".
     *
     * The method iterates the parts of the fieldName, until the penultimate part, and then sets the value.
     *
     * @param domain The domain containing a field to patch
     * @param fieldName The name of the field, with deep fields separated by .
     * @param value The value to set
     * @return
     */
    static patchCreatorAwareField(CreatorAware domain, String fieldName, value) {
        String[] splitFieldName = fieldName.split("\\.")
        int numSplits = splitFieldName.size()
        String lastSplit = splitFieldName[numSplits - 1]

        def field = domain

        for (Integer i = 0; i < numSplits - 1; i++) {
            field = field."${splitFieldName[i]}"
        }

        field."${lastSplit}" = value
    }
}
