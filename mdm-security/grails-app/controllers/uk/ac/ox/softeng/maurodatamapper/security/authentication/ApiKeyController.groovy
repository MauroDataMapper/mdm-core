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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService

import grails.gorm.transactions.Transactional

class ApiKeyController extends EditLoggingController<ApiKey> {

    static allowedMethods = [patch: [], edit: [], create: [], update: [], show: []]

    static responseFormats = ['json', 'xml']

    ApiKeyService apiKeyService

    CatalogueUserService catalogueUserService

    ApiKeyController() {
        super(ApiKey)
    }

    @Transactional
    def refreshApiKey() {
        if (handleReadOnly()) return

        ApiKey instance = queryForResource(params.apiKeyId)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (!instance.refreshable) {
            instance.errors.rejectValue('refreshable', 'invalid.apikey.cannot.refresh.unrefreshable.message',
                                        'Cannot refresh ApiKey as it is not marked refreshable')
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: 'update' // STATUS CODE 422
            return
        }

        apiKeyService.refreshApiKey(instance, params.expiresInDays?.toLong())

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def disableApiKey() {
        if (handleReadOnly()) return

        ApiKey instance = queryForResource(params.apiKeyId)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        apiKeyService.disableApiKey(instance)

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def enableApiKey() {
        if (handleReadOnly()) return

        ApiKey instance = queryForResource(params.apiKeyId)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        apiKeyService.enableApiKey(instance)

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    protected ApiKey createResource() {
        ApiKey apiKey = super.createResource() as ApiKey
        catalogueUserService.get(params.catalogueUserId).addToApiKeys(apiKey)
        apiKey
    }

    @Override
    protected void serviceDeleteResource(ApiKey resource) {
        apiKeyService.delete(resource)
    }

    @Override
    protected List<ApiKey> listAllReadableResources(Map params) {
        apiKeyService.findAllByCatalogueUser(catalogueUserService.get(params.catalogueUserId), params)
    }
}
