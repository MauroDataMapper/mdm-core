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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService

import grails.gorm.transactions.Transactional
import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser

import java.time.LocalDate

@Transactional
class ApiKeyService implements AnonymisableService {
    CatalogueUserService catalogueUserService

    ApiKey get(Serializable id) {
        ApiKey.get(id)
    }

    List<ApiKey> list(Map pagination) {
        ApiKey.list(pagination)
    }

    Long count() {
        ApiKey.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(ApiKey apiKey) {
        apiKey.delete(flush: true)
    }

    boolean isApiKeyExpired(ApiKey apiKey) {
        apiKey.isExpired()
    }

    boolean isApiKeyExpired(Serializable id) {
        get(id)?.isExpired()
    }

    List<ApiKey> findAllByCatalogueUser(CatalogueUser catalogueUser, Map pagination = [:]) {
        ApiKey.findAllByCatalogueUser(catalogueUser, pagination)
    }

    ApiKey createNewApiKeyForCatalogueUser(String creatorEmailAddress, CatalogueUser catalogueUser, long expiresInDays,
                                           boolean refreshable = false,
                                           String name = ApiKey.DEFAULT_NAME) {
        ApiKey apiKey = new ApiKey(name: name, refreshable: refreshable, createdBy: creatorEmailAddress)
        apiKey.expiryDate = LocalDate.now().plusDays(expiresInDays)
        catalogueUser.addToApiKeys(apiKey)
        apiKey
    }

    ApiKey refreshApiKey(ApiKey apiKey, long expiresInDays) {
        apiKey.expiryDate = LocalDate.now().plusDays(expiresInDays)
        apiKey
    }

    ApiKey disableApiKey(ApiKey apiKey) {
        apiKey.disabled = true
        apiKey
    }

    ApiKey enableApiKey(ApiKey apiKey) {
        apiKey.disabled = false
        apiKey
    }

    void anonymise(String createdBy) {
        ApiKey.findAllByCreatedBy(createdBy).each { apiKey ->
            apiKey.createdBy = AnonymousUser.ANONYMOUS_EMAIL_ADDRESS
            apiKey.save(validate: false)
        }
    }
}
