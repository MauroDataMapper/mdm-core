/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import grails.rest.Link
import java.time.OffsetDateTime
import java.util.regex.Pattern

class PublishedModel implements Comparable<PublishedModel> {

    String modelId
    String modelLabel
    Version modelVersion
    String modelVersionTag
    String description
    String modelType
    OffsetDateTime lastUpdated
    OffsetDateTime dateCreated
    OffsetDateTime datePublished
    String author
    UUID previousModelId
    List<Link> links

    PublishedModel() {
    }

    PublishedModel(Model model) {
        modelId = model.id
        modelLabel = model.label
        modelVersion = model.modelVersion
        modelVersionTag = model.modelVersionTag
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

    String getDescription() {
        description == title ? null : description
    }

    void setDescription(String description) {
        if (description && description != title) this.description = description
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        PublishedModel that = (PublishedModel) o

        if (author != that.author) return false
        if (dateCreated != that.dateCreated) return false
        if (datePublished != that.datePublished) return false
        if (description != that.description) return false
        if (lastUpdated != that.lastUpdated) return false
        if (modelId != that.modelId) return false
        if (modelLabel != that.modelLabel) return false
        if (modelType != that.modelType) return false
        if (modelVersion != that.modelVersion) return false
        if (modelVersionTag != that.modelVersionTag) return false
        previousModelId == that.previousModelId
    }

    @Override
    int hashCode() {
        int result
        result = modelId.hashCode()
        result = 31 * result + modelLabel.hashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + (modelVersionTag != null ? modelVersionTag.hashCode() : 0)
        result = 31 * result + (description != null ? description.hashCode() : 0)
        result = 31 * result + modelType.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + dateCreated.hashCode()
        result = 31 * result + datePublished.hashCode()
        result = 31 * result + (author != null ? author.hashCode() : 0)
        result = 31 * result + (previousModelId != null ? previousModelId.hashCode() : 0)
        result
    }

    @Override
    int compareTo(PublishedModel that) {
        int res = this.modelLabel <=> that.modelLabel
        if (res == 0) res = this.modelVersion <=> that.modelVersion
        res
    }
}
