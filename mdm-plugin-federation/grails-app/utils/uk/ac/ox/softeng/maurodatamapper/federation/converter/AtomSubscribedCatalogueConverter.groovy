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

import grails.rest.Link
import groovy.xml.slurpersupport.GPathResult

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AtomSubscribedCatalogueConverter implements SubscribedCatalogueConverter {
    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.ATOM
    }

    @Override
    Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        GPathResult subscribedCatalogueModelsFeed = federationClient.getSubscribedCatalogueModelsFromAtomFeed(subscribedCatalogue.apiKey)

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModelsFeed.author.name.text(), url: subscribedCatalogueModelsFeed.author.uri.text())

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {entry ->
            new PublishedModel().tap {
                modelId = entry.id
                modelLabel = entry.title
                if (entry.updated.text()) lastUpdated = OffsetDateTime.parse(entry.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                if (entry.published.text()) datePublished = OffsetDateTime.parse(entry.published.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                author = entry.author.name ?: subscribedCatalogueModelsFeed.author.name
                description = entry.summary
                links = entry.link.collect {link ->
                    new Link(LINK_RELATIONSHIP_ALTERNATE, link.@href.text()).tap {contentType = link.@type}
                }
            }
        }

        return new Tuple2(subscribedAuthority, publishedModels)
    }
}