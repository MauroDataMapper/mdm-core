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
package uk.ac.ox.softeng.maurodatamapper.core.facet

/**
 * @since 12/02/2018
 */
enum VersionLinkType {
    NEW_FORK_OF('New Fork Of'),
    NEW_DOCUMENTATION_VERSION_OF('New Documentation Version Of'),
    NEW_MODEL_VERSION_OF('New Model Version Of')

    String label

    VersionLinkType(String label) {
        this.label = label
    }

    static VersionLinkType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static VersionLinkType findFromMap(def map) {
        map['linkType'] instanceof VersionLinkType ? map['linkType'] as VersionLinkType : findForLabel(map['linkType'] as String)
    }
}