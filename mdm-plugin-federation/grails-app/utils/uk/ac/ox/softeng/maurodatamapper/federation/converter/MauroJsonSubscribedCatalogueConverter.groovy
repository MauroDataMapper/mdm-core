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
package uk.ac.ox.softeng.maurodatamapper.federation.converter

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.federation.web.FederationClient
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.rest.Link

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MauroJsonSubscribedCatalogueConverter implements SubscribedCatalogueConverter {
    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.MAURO_JSON
    }

    @Override
    Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        Map<String, Object> subscribedCatalogueModels = federationClient.getSubscribedCatalogueModels()

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModels.authority.label, url: subscribedCatalogueModels.authority.url)

        List<PublishedModel> publishedModels = (subscribedCatalogueModels.publishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}

        return new Tuple2(subscribedAuthority, publishedModels)
    }

    @Override
    Tuple2<OffsetDateTime, List<PublishedModel>> getNewerPublishedVersionsForPublishedModel(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue,
                                                                                            String publishedModelId) {
        Map<String, Object> newerPublishedVersions = federationClient.getNewerPublishedVersionsForPublishedModel(publishedModelId)

        OffsetDateTime lastUpdated = OffsetDateTime.parse(newerPublishedVersions.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        List<PublishedModel> newerVersions = (newerPublishedVersions.newerPublishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}

        return new Tuple2(lastUpdated, newerVersions)
    }

    @Override
    Map<String, Object> getVersionLinksForPublishedModel(FederationClient federationClient, String urlModelType,
                                                         String publishedModelId) {
        if (urlModelType) {
            return federationClient.getVersionLinksForModel(urlModelType, publishedModelId)
        } else {
            return [:]
        }
    }

    private PublishedModel convertEntryToPublishedModel(Map<String, Object> entry) {
        new PublishedModel().tap {
            modelId = entry.modelId
            modelLabel = entry.label
            modelVersion = Version.from(entry.version)
            modelVersionTag = entry.modelVersionTag
            modelType = entry.modelType
            lastUpdated = OffsetDateTime.parse(entry.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            dateCreated = OffsetDateTime.parse(entry.dateCreated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            datePublished = OffsetDateTime.parse(entry.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            author = entry.author
            description = entry.description
            if (entry.links) links = entry.links.collect {link -> new Link(LINK_RELATIONSHIP_ALTERNATE, link.url).tap {contentType = link.contentType}}
        }
    }
}
