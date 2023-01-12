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
        Map<String, Object> subscribedCatalogueModels = federationClient.getSubscribedCatalogueModels(subscribedCatalogue.apiKey)

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModels.authority.label, url: subscribedCatalogueModels.authority.url)

        List<PublishedModel> publishedModels = (subscribedCatalogueModels.publishedModels as List<Map<String, Object>>).collect {pm ->
            new PublishedModel().tap {
                modelId = pm.modelId
                modelLabel = pm.label
                modelVersion = Version.from(pm.version)
                modelType = pm.modelType
                lastUpdated = OffsetDateTime.parse(pm.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                dateCreated = OffsetDateTime.parse(pm.dateCreated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                datePublished = OffsetDateTime.parse(pm.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                author = pm.author
                description = pm.description
                if (pm.links) links = pm.links.collect {link -> new Link(LINK_RELATIONSHIP_ALTERNATE, link.url).tap {contentType = link.contentType}}
            }
        }

        return new Tuple2(subscribedAuthority, publishedModels)
    }
}
