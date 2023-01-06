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
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
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
        GPathResult subscribedCatalogueModelsFeed = federationClient.getSubscribedCatalogueModelsFromAtomFeed()

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModelsFeed.author.name.text() ?: subscribedCatalogue.label,
                                                      url: subscribedCatalogueModelsFeed.author.uri.text() ?: subscribedCatalogue.url)

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {convertEntryToPublishedModel(it)}.sort {l, r ->
            r.lastUpdated <=> l.lastUpdated ?:
            l.modelLabel.compareToIgnoreCase(r.modelLabel) ?:
            l.modelLabel <=> r.modelLabel ?:
            l.modelId <=> r.modelId
        }

        return new Tuple2(subscribedAuthority, publishedModels)
    }

    @Override
    Tuple2<OffsetDateTime, List<PublishedModel>> getNewerPublishedVersionsForPublishedModel(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue,
                                                                                            String publishedModelId) {
        getNewerOrOlderPublishedVersionsForPublishedModel(federationClient, publishedModelId)
    }

    @Override
    Map<String, Object> getVersionLinksForPublishedModel(FederationClient federationClient, String urlModelType,
                                                         String publishedModelId) {
        List<PublishedModel> olderVersions = getNewerOrOlderPublishedVersionsForPublishedModel(federationClient, publishedModelId, true).v2

        Map<String, Object> versionLinks = [:]

        versionLinks.items = olderVersions.collect {PublishedModel pm ->
            [
                linkType   : VersionLinkType.NEW_MODEL_VERSION_OF.label,
                sourceModel: [id: publishedModelId],
                targetModel: [id: pm.modelId]
            ]
        }

        return versionLinks
    }

    private Tuple2<OffsetDateTime, List<PublishedModel>> getNewerOrOlderPublishedVersionsForPublishedModel(FederationClient federationClient, String publishedModelId,
                                                                                                           boolean older = false) {
        GPathResult subscribedCatalogueModelsFeed = federationClient.getSubscribedCatalogueModelsFromAtomFeed()

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {convertEntryToPublishedModel(it)}
        PublishedModel publishedModel = publishedModels.find {it.modelId == publishedModelId}

        List<PublishedModel> newerVersions =
            publishedModels
                .findAll {
                    it.modelLabel == publishedModel.modelLabel && ((!older && it.lastUpdated > publishedModel.lastUpdated) ||
                                                                   (older && it.lastUpdated < publishedModel.lastUpdated))
                }
                .sort {l, r ->
                    r.lastUpdated <=> l.lastUpdated ?:
                    l.modelLabel.compareToIgnoreCase(r.modelLabel) ?:
                    l.modelLabel <=> r.modelLabel ?:
                    l.modelId <=> r.modelId
                }

        OffsetDateTime lastUpdated = newerVersions.collect {it.lastUpdated}.max()

        return new Tuple2(lastUpdated, newerVersions)
    }

    private PublishedModel convertEntryToPublishedModel(GPathResult entry) {
        new PublishedModel().tap {
            modelId = entry.id
            modelLabel = entry.title
            if (entry.contentItemVersion.text()) modelVersionTag = entry.contentItemVersion.text()
            if (entry.updated.text()) lastUpdated = OffsetDateTime.parse(entry.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            if (entry.published.text()) datePublished = OffsetDateTime.parse(entry.published.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            author = entry.author.name ?: subscribedCatalogueModelsFeed.author.name
            description = entry.summary
            links = entry.link.collect {link ->
                new Link(LINK_RELATIONSHIP_ALTERNATE, link.@href.text()).tap {contentType = link.@type}
            }
        }
    }

}