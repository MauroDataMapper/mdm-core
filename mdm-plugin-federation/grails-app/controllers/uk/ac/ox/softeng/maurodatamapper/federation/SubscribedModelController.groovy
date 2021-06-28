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
package uk.ac.ox.softeng.maurodatamapper.federation


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors

@Slf4j
class SubscribedModelController extends EditLoggingController<SubscribedModel> {

    static responseFormats = ['json', 'xml']

    FolderService folderService

    @Autowired(required = false)
    List<ModelService> modelServices

    SubscribedCatalogueService subscribedCatalogueService
    SubscribedModelService subscribedModelService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    SubscribedModelController() {
        super(SubscribedModel)
    }

    @Transactional
    @Override
    def save() {
        if (handleReadOnly()) return

        def instance = createResource()

        if (response.isCommitted()) return

        if (!validateResource(instance, 'create')) return

        if (params.boolean('federate', true)) {
            def federationResult = subscribedModelService.federateSubscribedModel(instance, currentUserSecurityPolicyManager)
            if (federationResult instanceof Errors) {
                transactionStatus.setRollbackOnly()
                respond federationResult, view: 'create' // STATUS CODE 422
                return
            }
        }

        saveResource instance

        saveResponse instance
    }

    /**
     * Federate the specified SubscribedModel by
     * 1. Get the SubscribedModel, else respond not found
     * 2. Export as Json the model from the remote catalogue, responding no content if the result is empty
     * 3. Import the json into the local catalogue, responding unprocessable if this doesn't work
     * 4. Look up version links from the remote and save this locally
     * @return
     */
    @Transactional
    def federate() {
        log.debug("Getting SubscribedModel ${params.subscribedModelId}")
        SubscribedModel subscribedModel = subscribedModelService.get(params.subscribedModelId)
        if (!subscribedModel) {
            log.debug("SubscribedModel not found")
            return notFound(SubscribedModel, params.subscribedModelId)
        }

        //Check we can import into the requested folder, and get the folder
        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, subscribedModel.folderId)) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, subscribedModel.folderId)) {
                return notFound(Folder, subscribedModel.folderId)
            }
            return forbiddenDueToPermissions()
        }

        def federationResult = subscribedModelService.federateSubscribedModel(subscribedModel, currentUserSecurityPolicyManager)
        if (federationResult instanceof Errors) {
            transactionStatus.setRollbackOnly()
            respond federationResult, view: 'update' // STATUS CODE 422
        }

        updateResource(subscribedModel)
        updateResponse(subscribedModel)

    }

    @Override
    void serviceDeleteResource(SubscribedModel resource) {
        subscribedModelService.delete(resource)
    }

    @Override
    protected SubscribedModel createResource() {
        //Create the SubscribedModel
        SubscribedModel resource = super.createResource() as SubscribedModel

        //Create an association between the SubscribedCatalogue and SubscribedModel
        resource.subscribedCatalogue = subscribedCatalogueService.get(params.subscribedCatalogueId)

        resource
    }

    @Override
    protected SubscribedModel saveResource(SubscribedModel resource) {
        SubscribedModel subscribedModel = super.saveResource(resource) as SubscribedModel
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(
                subscribedModel,
                currentUser,
                subscribedModel.subscribedModelId.toString())
        }
        subscribedModel
    }

    @Override
    @Transactional
    protected boolean validateResource(SubscribedModel instance, String view) {
        // Make sure any existing errors are returned first..such as parse errors
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }

        instance.validate()

        //Check we can import into the requested folder, and get the folder
        if (instance.folderId && !currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, instance.folderId)) {
            instance.errors.rejectValue('folderId', 'invalid.subscribedmodel.folderid.no.permissions',
                                        'Invalid folderId for subscribed model, user does not have the necessary permissions')
        }

        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }

    @Override
    protected SubscribedModel queryForResource(Serializable id) {
        subscribedModelService.findBySubscribedCatalogueIdAndId(params.subscribedCatalogueId, id)
    }

    @Override
    protected List<SubscribedModel> listAllReadableResources(Map params) {
        subscribedModelService.findAllBySubscribedCatalogueId(params.subscribedCatalogueId, params)
    }
}
