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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

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

    @Override
    void serviceDeleteResource(SubscribedModel resource) {
        subscribedModelService.delete(resource)
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

        Folder folder = folderService.get(subscribedModel.folderId)

        //Export the requested model from the SubscribedCatalogue
        log.debug("Exporting SubscribedModel ${params.subscribedModelId} as Json")
        String exportedJson =
            subscribedModelService.exportSubscribedModelFromSubscribedCatalogue(subscribedModelService.get(params.subscribedModelId))
        if (!exportedJson) {
            log.debug("No Json exported")
            request.withFormat {
                '*' {render status: NO_CONTENT} // NO CONTENT STATUS CODE
            }
            return
        }

        log.debug("exportedJson {}", exportedJson)


        //Get a ModelService to handle the domain type we are dealing with
        ModelService modelService = modelServices.find {it.handles(subscribedModel.subscribedModelType)}

        ModelImporterProviderService modelImporterProviderService = modelService.getJsonModelImporterProviderService()

        //Import the model
        ModelImporterProviderServiceParameters parameters =
            modelImporterProviderService.createNewImporterProviderServiceParameters() as ModelImporterProviderServiceParameters

        if (parameters.hasProperty('importFile')?.type != FileParameter) {
            throw new ApiInternalException('MSXX', "Assigned JSON importer ${modelImporterProviderService.class.simpleName} " +
                                                   "for model cannot import file content")
        }

        parameters.importFile = new FileParameter(fileContents: exportedJson.getBytes())
        parameters.folderId = folder.id
        parameters.finalised = true
        parameters.useDefaultAuthority = false

        Model model = modelImporterProviderService.importDomain(currentUser, parameters)

        if (!model) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        model.folder = folder

        modelService.validate(model)

        if (model.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond model.errors
            return
        }

        log.debug('No errors in imported model')

        Model savedModel = modelService.saveModelWithContent(model)
        log.debug('Saved model')
        if (securityPolicyManagerService) {
            log.debug("add security to saved model")
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedModel, currentUser, savedModel.label)
        }


        //Record the ID of the imported model against the subscription makes it easier to track version links later.
        subscribedModel.lastRead = OffsetDateTime.now()
        subscribedModel.localModelId = savedModel.id
        subscribedModelService.save(subscribedModel)
        log.info('Single Model Import complete')


        //Handle version linking
        Map versionLinks = subscribedModelService.getVersionLinks(modelService.getUrlResourceName(), subscribedModel)
        if (versionLinks) {
            log.debug("add version links")
            subscribedModelService.addVersionLinksToImportedModel(currentUser, versionLinks, modelService, subscribedModel)
        }

        //Respond with the subscribed model
        respond subscribedModel, status: OK, view: 'show'
    }

    @Override
    protected SubscribedModel createResource() {
        //Create the SubscribedModel
        SubscribedModel resource = super.createResource() as SubscribedModel

        //Create an association between the SubscribedCatalogue and SubscribedModel
        subscribedCatalogueService.get(params.subscribedCatalogueId)?.addToSubscribedModels(resource)

        resource
    }

    @Override
    protected SubscribedModel saveResource(SubscribedModel resource) {
        SubscribedModel subscribedModel = super.saveResource(resource) as SubscribedModel
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(subscribedModel,
                                                                                                            currentUser,
                                                                                                            subscribedModel.subscribedModelId.
                                                                                                                toString())
        }
        subscribedModel
    }

    @Override
    protected SubscribedModel queryForResource(Serializable id) {
        subscribedModelService.get(id)
    }

    @Override
    protected List<SubscribedModel> listAllReadableResources(Map params) {
        subscribedModelService.list()
    }
}
