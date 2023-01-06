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

import java.time.OffsetDateTime

trait SubscribedCatalogueConverter {
    static final String LINK_RELATIONSHIP_ALTERNATE = 'alternate'

    abstract boolean handles(SubscribedCatalogueType type)

    abstract Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue)

    abstract Tuple2<OffsetDateTime, List<PublishedModel>> getNewerPublishedVersionsForPublishedModel(FederationClient federationClient,
                                                                                                     SubscribedCatalogue subscribedCatalogue, String publishedModelId)

    abstract Map<String, Object> getVersionLinksForPublishedModel(FederationClient client, String urlModelType, String publishedModelId)

    Authority getAuthority(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        getAuthorityAndPublishedModels(federationClient, subscribedCatalogue).v1
    }

    List<PublishedModel> getPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        getAuthorityAndPublishedModels(federationClient, subscribedCatalogue).v2
    }
}