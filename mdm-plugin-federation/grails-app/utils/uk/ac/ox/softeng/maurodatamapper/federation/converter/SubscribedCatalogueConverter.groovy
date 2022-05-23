/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

interface SubscribedCatalogueConverter {
    static final String LINK_RELATIONSHIP_ALTERNATE = 'alternate'

    boolean handles(SubscribedCatalogueType type)

    Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue)

    default Authority getAuthority(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        getAuthorityAndPublishedModels(federationClient, subscribedCatalogue).v1
    }

    default List<PublishedModel> getPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue) {
        getAuthorityAndPublishedModels(federationClient, subscribedCatalogue).v2
    }
}