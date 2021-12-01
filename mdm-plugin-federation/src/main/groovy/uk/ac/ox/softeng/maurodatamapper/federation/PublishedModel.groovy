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

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.transform.Sortable

import java.time.OffsetDateTime

@Sortable(includes = ['modelLabel', 'modelVersion'])
class PublishedModel {

    UUID modelId
    String modelLabel
    Version modelVersion
    String description
    String modelType
    OffsetDateTime lastUpdated
    OffsetDateTime dateCreated
    OffsetDateTime datePublished
    String author
    UUID previousModelId

    PublishedModel() {
    }

    PublishedModel(Model model) {
        modelId = model.id
        modelLabel = model.label
        modelVersion = model.modelVersion
        modelType = model.domainType
        lastUpdated = model.lastUpdated
        dateCreated = model.dateCreated
        datePublished = model.dateFinalised
        author = model.author
        description = model.description
    }

    String getTitle() {
        "${modelLabel} ${modelVersion}"
    }

    void setTitle(String label) {
        String version = label.find(Version.VERSION_PATTERN)
        if (version) {
            modelVersion = Version.from(version)
            modelLabel = (label - version).trim()
        } else {
            modelLabel = label
        }
    }

    String getDescription() {
        description == title ? null : description
    }

    void setDescription(String description) {
        if (description && description != title) this.description = description
    }
}
