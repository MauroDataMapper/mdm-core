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


}