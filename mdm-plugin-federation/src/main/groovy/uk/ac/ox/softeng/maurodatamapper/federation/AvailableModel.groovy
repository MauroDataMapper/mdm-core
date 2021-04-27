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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.util.Version

import java.time.OffsetDateTime


class AvailableModel {

    UUID id
    String modelLabel
    Version modelVersion
    String description
    String modelType
    OffsetDateTime lastUpdated

    void setLabel(String label) {
        String version = label.find(Version.VERSION_PATTERN)
        if (version) {
            modelVersion = Version.from(version)
            modelLabel = (label - version).trim()
        } else {
            modelLabel = label
        }
    }

    String getLabel() {
        "${modelLabel ?: ''} ${modelVersion ?: ''}".trim()
    }

    String getDescription() {
        description == label ? null : description
    }

    void setDescription(String description) {
        if (description && description != modelLabel) this.description = description
    }
}
