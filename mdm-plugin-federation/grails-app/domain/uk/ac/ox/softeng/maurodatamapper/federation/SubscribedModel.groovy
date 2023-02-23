/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria

import java.time.OffsetDateTime

class SubscribedModel implements MdmDomain, SecurableResource, EditHistoryAware {

    UUID id

    //The ID of the model on the remote (subscribed) catalogue
    String subscribedModelId
    String subscribedModelType
    //The folder that the model should be imported into
    UUID folderId
    //The last time that the model was last read from the remote (subscribed) catalogue
    OffsetDateTime lastRead
    //The ID of the model when imported into the local catalogue.
    UUID localModelId
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers

    static belongsTo = [subscribedCatalogue: SubscribedCatalogue]

    static constraints = {
        subscribedCatalogue nullable: false
        folderId nullable: false
        subscribedModelId nullable: false, unique: 'subscribedCatalogue' // Should prevent subscribing to the same modelId from same catalogue
        subscribedModelType nullable: true, blank: false
        lastRead nullable: true
        localModelId nullable: true
        path nullable: true
    }

    static mapping = {
        subscribedCatalogue index: 'subscribed_model_subscribed_catalogue_id'
    }

    SubscribedModel() {
        readableByAuthenticatedUsers = true
        readableByEveryone = false
    }

    @Override
    String getDomainType() {
        SubscribedModel.simpleName
    }

    @Override
    String getPathPrefix() {
        null
    }

    @Override
    String getPathIdentifier() {
        null
    }

    @Override
    String getEditLabel() {
        "SubscribedModel:${id}"
    }

    static DetachedCriteria<SubscribedModel> by() {
        new DetachedCriteria<SubscribedModel>(SubscribedModel)
    }

    static DetachedCriteria<SubscribedModel> bySubscribedCatalogueId(UUID subscribedCatalogueId) {
        by().eq('subscribedCatalogue.id', subscribedCatalogueId)
    }

    static DetachedCriteria<SubscribedModel> bySubscribedCatalogueIdAndSubscribedModelId(UUID subscribedCatalogueId, String subscribedModelId) {
        bySubscribedCatalogueId(subscribedCatalogueId).eq('subscribedModelId', subscribedModelId)
    }

    static DetachedCriteria<SubscribedModel> bySubscribedCatalogueIdAndId(UUID subscribedCatalogueId, UUID id) {
        bySubscribedCatalogueId(subscribedCatalogueId).idEq(id)
    }

    static DetachedCriteria<SubscribedModel> bySubscribedModelId(String subscribedModelId) {
        by()
            .eq('subscribedModelId', subscribedModelId)
    }
}
