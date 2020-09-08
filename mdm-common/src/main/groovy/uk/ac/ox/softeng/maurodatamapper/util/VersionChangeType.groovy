/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.util;

enum VersionChangeType {
    MAJOR('Major'),
    MINOR('Minor'),
    PATCH('Patch')

    String label

    VersionChangeType(String label) {
        this.label = label
    }

    static VersionChangeType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static VersionChangeType findFromMap(def map) {
        map['versionUpgradeType'] instanceof VersionChangeType ? map['versionUpgradeType'] as VersionChangeType : findForLabel(map['versionUpgradeType'] as String)
    }
}
