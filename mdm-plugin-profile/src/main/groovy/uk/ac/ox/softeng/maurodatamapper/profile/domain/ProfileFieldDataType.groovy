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

import groovy.json.JsonSlurper
import org.apache.commons.collections.functors.ExceptionPredicate
import org.apache.commons.lang3.time.DateUtils

enum ProfileFieldDataType {
    BOOLEAN('boolean'),
    STRING('string'),
    TEXT('text'),
    INT('int'),
    DECIMAL('decimal'),
    DATE('date'),
    DATETIME('datetime'),
    TIME('time'),
    FOLDER('folder'),
    MODEL('model'),
    ENUMERATION('enumeration')
    JSON('json')

    String label

    ProfileFieldDataType(String name) {
        this.label = name
    }

    String toString() {
        label
    }

    static ProfileFieldDataType findForLabel(String label) {
        values().find { it.label.equalsIgnoreCase(label) }
    }

    static ProfileFieldDataType findFor(String value) {
        value ? findForLabel(value) ?: valueOf(value) : null
    }

    static List<String> labels() {
        values().collect { it.label }.sort()
    }

    static ProfileFieldDataType findFromMap(def map) {
        map['dataType'] instanceof ProfileFieldDataType ? map['dataType'] as ProfileFieldDataType : findForLabel(map['dataType'] as String)
    }

    String validateString(String input) {
        if (this.label == "boolean") {
            if(!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false")) {
                return "Field is not of type 'boolean'"
            }
        }
        if (this.label == "int") {
            try {
                Integer.parseInt(input)
            } catch(Exception ignored) {
                return "Field is not of type 'int'"
            }
        }
        if (this.label == "decimal") {
            try {
                Double.parseDouble(input)
            } catch(Exception ignored) {
                return "Field is not of type 'decimal'"
            }
        }
        if (this.label == "date") {
            try {
                DateUtils.parseDateStrictly(input, "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "MM-dd-yyyy", "yyyy/MM/dd")
            } catch(Exception ignored) {
                return "Field is not of type 'date'"
            }
        }
        if (this.label == "datetime") {
            try {
                DateUtils.parseDateStrictly(input, "dd/MM/yyyy'T'HH:mm:ss","dd-MM-yyyy'T'HH:mm:ss")
            } catch(Exception ignored) {
                return "Field is not of type 'datetime'"
            }
        }
        if (this.label == "time") {
            try {
                DateUtils.parseDateStrictly(input, "HH:mm:ss","HH:mm")
            } catch(Exception ignored) {
                return "Field is not of type 'datetime'"
            }
        }
        if (this.label == "model") {
            try {
                UUID.fromString(input)
            } catch(Exception ignored) {
                return "Field is not of type 'model'"
            }
        }
        if (this.label == "folder") {
            try {
                UUID.fromString(input)
            } catch(Exception ignored) {
                return "Field is not of type 'folder'"
            }
        }
        if (this.label == "json") {
            try {
                JsonSlurper jsonSlurper = new JsonSlurper()
                jsonSlurper.parseText(input)
            } catch(Exception ignored) {
                return "Field is not valid JSON for type 'json'"
            }
        }

        return null
    }

}