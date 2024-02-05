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
package uk.ac.ox.softeng.maurodatamapper.profile.domain


import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.json.JsonSlurper
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
    ENUMERATION('enumeration'),
    JSON('json')

    String label

    ProfileFieldDataType(String name) {
        this.label = name
    }

    String toString() {
        label
    }

    static ProfileFieldDataType findForLabel(String label) {
        values().find {it.label.equalsIgnoreCase(label)} ?: STRING
    }

    static ProfileFieldDataType findFor(String value) {
        value ? findForLabel(value) ?: valueOf(value) : null
    }

    static List<String> labels() {
        values().collect {it.label}.sort()
    }

    static ProfileFieldDataType findFromMap(def map) {
        map['dataType'] instanceof ProfileFieldDataType ? map['dataType'] as ProfileFieldDataType : findForLabel(map['dataType'] as String)
    }

    String validateString(String input) {
        switch (label) {
            case 'boolean':
                if (!input.equalsIgnoreCase('true') && !input.equalsIgnoreCase('false')) {
                    return 'Boolean'
                }
                return null
            case 'int':
                try {
                    Integer.parseInt(input)
                    return null
                } catch (Exception ignored) {
                    return 'Integer'
                }
            case 'decimal':
                try {
                    Double.parseDouble(input)
                    return null
                } catch (Exception ignored) {
                    return 'Decimal'
                }

            case 'date':
                try {
                    DateUtils.parseDateStrictly(input, ConfigurableProfileFieldTypes.instance.dateFormats)
                    return null
                } catch (Exception ignored) {
                    return 'Date'
                }

            case 'datetime':
                try {
                    DateUtils.parseDateStrictly(input, ConfigurableProfileFieldTypes.instance.dateTimeFormats)
                    return null
                } catch (Exception ignored) {
                    return 'DateTime'
                }

            case 'time':
                try {
                    DateUtils.parseDateStrictly(input, ConfigurableProfileFieldTypes.instance.timeFormats)
                    return null
                } catch (Exception ignored) {
                    return 'Time'
                }

            case 'model':
                try {
                    Utils.toUuid(input)
                    return null
                } catch (Exception ignored) {
                    return 'Model'
                }

            case 'folder':
                try {
                    Utils.toUuid(input)
                    return null
                } catch (Exception ignored) {
                    return 'Folder'
                }

            case 'json':
                try {
                    JsonSlurper jsonSlurper = new JsonSlurper()
                    jsonSlurper.parseText(input)
                    return null
                } catch (Exception ignored) {
                    return 'JSON'
                }
        }

        return null
    }

}