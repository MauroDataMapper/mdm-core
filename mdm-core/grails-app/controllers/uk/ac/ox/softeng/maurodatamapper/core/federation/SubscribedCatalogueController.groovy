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
package uk.ac.ox.softeng.maurodatamapper.core.federation

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class SubscribedCatalogueController extends EditLoggingController<SubscribedCatalogue> {

    static responseFormats = ['json', 'xml', 'opml']

    SubscribedCatalogueService subscribedCatalogueService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    SubscribedCatalogueController() {
        super(SubscribedCatalogue)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond(listAllReadableResources(params))
    }

    @Override
    void serviceDeleteResource(SubscribedCatalogue resource) {
        subscribedCatalogueService.delete(resource)
    }
    
    /**
     * Read available models from the subscribed catalogue and return as json.
     *
     */
    def availableModels() {        
        SubscribedCatalogue subscribedCatalogue = queryForResource(params.subscribedCatalogueId)

        if (!subscribedCatalogue) {
            return notFound(SubscribedCatalogue, params.subscribedCatalogueId)
        }

        try {
            respond subscribedCatalogueService.listAvailableModels(subscribedCatalogue)
        } catch (Exception exception) {
            return errorResponse(UNPROCESSABLE_ENTITY, "Unable to get available models. Exception ${exception.toString()}")
        }

    }    

    @Override
    @Transactional
    protected SubscribedCatalogue saveResource(SubscribedCatalogue resource) {
        SubscribedCatalogue subscribedCatalogue = super.saveResource(resource) as SubscribedCatalogue
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(subscribedCatalogue,
                                                                                                            currentUser,
                                                                                                            subscribedCatalogue.url)
        }

        try {
            subscribedCatalogueService.listAvailableModels(subscribedCatalogue)
        } catch (Exception exception) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY,
                                 "Not able to retrieve list of models from the provided URL and API key. Exception ${exception.toString()}")
        }
        subscribedCatalogue
    }

    @Override
    @Transactional
    protected SubscribedCatalogue updateResource(SubscribedCatalogue resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        SubscribedCatalogue subscribedCatalogue = super.updateResource(resource) as SubscribedCatalogue
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(subscribedCatalogue,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }

        try {
            subscribedCatalogueService.listAvailableModels(subscribedCatalogue)
        } catch (Exception exception) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY,
                                 "Not able to retrieve list of models from the provided URL and API key. Exception ${exception.toString()}")
        }
        subscribedCatalogue
    }

    @Override
    protected SubscribedCatalogue queryForResource(Serializable id) {
        subscribedCatalogueService.get(id)
    }

    @Override
    protected List<SubscribedCatalogue> listAllReadableResources(Map params) {
        subscribedCatalogueService.list()
    }
}
