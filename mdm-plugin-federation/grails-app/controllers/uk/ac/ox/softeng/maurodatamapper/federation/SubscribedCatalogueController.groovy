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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationCredentials
import uk.ac.ox.softeng.maurodatamapper.federation.authentication.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.maurodatamapper.federation.rest.transport.SubscribedModelFederationParams
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.OK

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
    def show() {
        def resource = queryForResource(params.id ?: params.subscribedCatalogueId)
        resource ? respond(resource, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager],
                                      view : (params.openAccess ? 'show_min' : 'show')])
                 : notFound(params.id)
    }

    /**
     * Override save method to create nested credentials object.
     * @return
     */
    @Transactional
    @Override
    def save() {
        if (handleReadOnly()) return

        SubscribedCatalogue instance = createResource()
        subscribedCatalogueService.createAuthenticationCredentials(instance)

        if (response.isCommitted()) return

        if (!validateResource(instance, 'create')) return

        saveResource instance

        saveResponse instance
    }

    /**
     * Override update method to handle nested credentials object.
     * @return
     */
    @Transactional
    @Override
    def update() {
        if (handleReadOnly()) return

        SubscribedCatalogue instance = queryForResource(params.id)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        instance.properties = getObjectToBind()
        subscribedCatalogueService.updateAuthenticationCredentials(instance)

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    void serviceDeleteResource(SubscribedCatalogue resource) {
        subscribedCatalogueService.delete(resource)
    }

    List<String> types() {
        respond SubscribedCatalogueType.labels()
    }

    List<String> authenticationTypes() {
        respond SubscribedCatalogueAuthenticationType.labels()
    }

    /**
     * Read available models from the subscribed catalogue and return as json.
     *
     */
    def publishedModels() {
        SubscribedCatalogue subscribedCatalogue = queryForResource(params.subscribedCatalogueId)

        if (!subscribedCatalogue) {
            return notFound(SubscribedCatalogue, params.subscribedCatalogueId)
        }
        respond subscribedCatalogueService.listPublishedModels(subscribedCatalogue)
    }

    def newerVersions() {
        SubscribedCatalogue subscribedCatalogue = queryForResource(params.subscribedCatalogueId)
        if (!subscribedCatalogue) {
            return notFound(SubscribedCatalogue, params.subscribedCatalogueId)
        }

        respond subscribedCatalogueService.getNewerPublishedVersionsForPublishedModel(subscribedCatalogue, params.publishedModelId)
    }

    def testConnection() {
        SubscribedCatalogue subscribedCatalogue = queryForResource(params.subscribedCatalogueId)

        if (!subscribedCatalogue) {
            return notFound(SubscribedCatalogue, params.subscribedCatalogueId)
        }

        subscribedCatalogueService.verifyConnectionToSubscribedCatalogue(subscribedCatalogue)

        if (subscribedCatalogue.hasErrors()) {
            respond subscribedCatalogue.errors
        } else {
            respond null, status: OK
        }
    }

    @Override
    @Transactional
    protected boolean validateResource(SubscribedCatalogue instance, String view) {
        if (instance.hasErrors() || !instance.validate()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            // Dont try and test connection if the instance fails basic validation
            return false
        }

        // Validate nested credentials
        if (instance.subscribedCatalogueAuthenticationCredentials &&
            (instance.subscribedCatalogueAuthenticationCredentials.hasErrors() || !instance.subscribedCatalogueAuthenticationCredentials.validate())) {
            transactionStatus.setRollbackOnly()
            respond instance.subscribedCatalogueAuthenticationCredentials.errors, view: view // STATUS CODE 422
            return false
        }

        // If the instance is valid then confirm the connection is possible,
        // i.e. there is a catalogue at the URL and the ApiKey works
        //        subscribedCatalogueService.verifyConnectionToSubscribedCatalogue(instance)

        if (instance.hasErrors() || !instance.validate()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
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
        subscribedCatalogue
    }

    @Override
    protected SubscribedCatalogue queryForResource(Serializable id) {
        subscribedCatalogueService.get(id)
    }

    @Override
    protected List<SubscribedCatalogue> listAllReadableResources(Map params) {
        subscribedCatalogueService.list(params)
    }
}
