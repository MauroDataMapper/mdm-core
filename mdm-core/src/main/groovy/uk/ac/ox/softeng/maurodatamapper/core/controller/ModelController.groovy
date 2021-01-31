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
package uk.ac.ox.softeng.maurodatamapper.core.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.exporter.ExporterService
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CreateNewVersionData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.FinaliseData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeIntoData
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.artefact.Artefact
import grails.gorm.transactions.Transactional
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
@Artefact('Controller')
abstract class ModelController<T extends Model> extends CatalogueItemController<T> {

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService
    FolderService folderService
    AuthorityService authorityService
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    ExporterService exporterService
    ImporterService importerService

    final String alternateParamsIdKey

    ModelController(Class resource, String alternateParamsIdKey) {
        super(resource)
        this.alternateParamsIdKey = alternateParamsIdKey
    }

    ModelController(Class resource, boolean readOnly, String alternateParamsIdKey) {
        super(resource, readOnly)
        this.alternateParamsIdKey = alternateParamsIdKey
    }

    abstract protected ModelService getModelService()

    abstract Set<ExporterProviderService> getExporterProviderServices()

    abstract Set<ImporterProviderService> getImporterProviderServices()

    def exporterProviders() {
        respond exporterProviders: getExporterProviderServices()
    }

    def importerProviders() {
        respond importerProviders: getImporterProviderServices()
    }

    @Override
    def show() {
        params.id = params.id ?: params[alternateParamsIdKey]
        super.show()
    }

    @Override
    @Transactional
    def update() {

        if (handleReadOnly()) {
            return
        }

        T instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (instance.finalised) return forbidden('Cannot update a finalised Model')

        instance.properties = getObjectToBind()

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Override
    @Transactional
    def delete() {
        if (handleReadOnly()) {
            return
        }

        T instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (params.boolean('permanent')) {
            getModelService().permanentDeleteModel(instance)
            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.retrieveUserSecurityPolicyManager(currentUser.emailAddress)
            }
            request.withFormat {
                '*' { render status: NO_CONTENT } // NO CONTENT STATUS CODE
            }
            return
        }

        // Otherwise perform "soft delete"
        getModelService().softDeleteModel(instance)

        updateResource(instance)

        updateResponse(instance)
    }

    @Transactional
    def readByEveryone() {
        T instance = queryForResource(params[alternateParamsIdKey])

        if (!instance) return notFound(params[alternateParamsIdKey])

        instance.readableByEveryone = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByAuthenticated() {
        T instance = queryForResource(params[alternateParamsIdKey])

        if (!instance) return notFound(params[alternateParamsIdKey])

        instance.readableByAuthenticatedUsers = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def changeFolder() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params[alternateParamsIdKey])

        if (!instance) return notFound(params[alternateParamsIdKey])

        if (instance.deleted) return forbidden('Cannot change the folder of a deleted Terminology')

        Folder folder = folderService.get(params.folderId)
        if (!folder) return notFound(Folder, params.folderId)

        instance.folder = folder

        updateResource(instance)

        updateResponse(instance)
    }

    def diff() {
        T thisModel = queryForResource params[alternateParamsIdKey]
        T otherModel = queryForResource params.otherModelId

        if (!thisModel) return notFound(params[alternateParamsIdKey])
        if (!otherModel) return notFound(params.otherModelId)

        ObjectDiff diff = getModelService().getDiffForModels(thisModel, otherModel)
        respond diff
    }

    def commonAncestor() {
        T left = queryForResource params[alternateParamsIdKey]
        if (!left) return notFound(params[alternateParamsIdKey])

        T right = queryForResource params.otherModelId
        if (!right) return notFound(params.otherModelId)

        respond modelService.findCommonAncestorBetweenModels(left, right)
    }

    def latestFinalisedModel() {
        T source = queryForResource(params[alternateParamsIdKey])
        if (!source) return notFound(params[alternateParamsIdKey])

        respond modelService.findLatestFinalisedModelByLabel(source.label)
    }

    def latestModelVersion() {
        T source = queryForResource(params[alternateParamsIdKey])
        if (!source) return notFound(params[alternateParamsIdKey])

        respond modelService.getLatestModelVersionByLabel(source.label)
    }

    def mergeDiff() {

        T left = queryForResource params[alternateParamsIdKey]
        if (!left) return notFound(params[alternateParamsIdKey])

        T right = queryForResource params.otherModelId
        if (!right) return notFound(params.otherModelId)

        respond modelService.getMergeDiffForModels(left, right)
    }

    @Transactional
    def mergeInto(MergeIntoData mergeIntoData) {
        if (!mergeIntoData.validate()) {
            respond mergeIntoData.errors
            return
        }

        if (mergeIntoData.patch.rightId != params[alternateParamsIdKey]) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Source model id passed in request body does not match source model id in URI.')
        }
        if (mergeIntoData.patch.leftId != params.otherModelId) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Target model id passed in request body does not match target model id in URI.')
        }

        T left = queryForResource params[alternateParamsIdKey]
        if (!left) return notFound(params[alternateParamsIdKey])

        T right = queryForResource params.otherModelId
        if (!right) return notFound(params.otherModelId)

        T instance =
            modelService.mergeModelIntoModel(left, right, mergeIntoData.patch, currentUserSecurityPolicyManager) as T

        if (!validateResource(instance, 'update')) return

        if (mergeIntoData.deleteBranch) {
            if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(left.class, left.id)) {
                return forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(left.class, left.id))
            }
            modelService.permanentDeleteModel(left)
            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.retrieveUserSecurityPolicyManager(currentUser.emailAddress)
            }
        }

        updateResource(instance)

        updateResponse(instance)
    }

    def currentMainBranch() {
        T source = queryForResource(params[alternateParamsIdKey])
        if (!source) return notFound(params[alternateParamsIdKey])

        respond modelService.findCurrentMainBranchForModel(source)
    }

    def availableBranches() {
        T source = queryForResource(params[alternateParamsIdKey])
        if (!source) return notFound(params[alternateParamsIdKey])

        respond modelService.findAllAvailableBranchesByLabel(source.label)
    }

    @Transactional
    def finalise(FinaliseData finaliseData) {

        if (!finaliseData.validate()) {
            respond finaliseData.errors
            return
        }

        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params[alternateParamsIdKey])

        if (instance.branchName != VersionAwareConstraints.DEFAULT_BRANCH_NAME) return METHOD_NOT_ALLOWED

        instance = modelService.finaliseModel(instance, currentUser,
                                              finaliseData.version, finaliseData.versionChangeType,
                                              finaliseData.supersededBy ?: []) as T

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def newBranchModelVersion(CreateNewVersionData createNewVersionData) {

        createNewVersionData.label = 'newBranchModelVersion'

        if (!createNewVersionData.validate()) {
            respond createNewVersionData.errors
            return
        }

        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params[alternateParamsIdKey])

        T copy = getModelService().
            createNewBranchModelVersion(createNewVersionData.branchName, instance, currentUser, createNewVersionData.copyPermissions,
                                        currentUserSecurityPolicyManager) as T

        if (!validateResource(copy, 'create')) return

        T savedCopy = modelService.saveModelWithContent(copy) as T
        savedCopy.addCreatedEdit(currentUser)

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedCopy, currentUser, savedCopy.label)
        }

        saveResponse savedCopy
    }

    @Transactional
    def newDocumentationVersion(CreateNewVersionData createNewVersionData) {

        createNewVersionData.label = 'newDocumentationVersion'

        if (!createNewVersionData.validate()) {
            respond createNewVersionData.errors
            return
        }

        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params[alternateParamsIdKey])

        T copy = getModelService().
            createNewDocumentationVersion(instance, currentUser, createNewVersionData.copyPermissions, currentUserSecurityPolicyManager) as T

        if (!validateResource(copy, 'create')) return

        T savedCopy = modelService.saveModelWithContent(copy) as T
        savedCopy.addCreatedEdit(currentUser)

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedCopy, currentUser, savedCopy.label)
        }

        saveResponse savedCopy
    }

    @Transactional
    def newForkModel(CreateNewVersionData createNewVersionData) {

        if (createNewVersionData.hasErrors()) {
            respond createNewVersionData.errors
            return
        }

        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params[alternateParamsIdKey])

        if (!currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(resource, params[alternateParamsIdKey])) {
            createNewVersionData.copyPermissions = false
        }

        T copy = getModelService().createNewForkModel(createNewVersionData.label, instance, currentUser, createNewVersionData.copyPermissions,
                                                      currentUserSecurityPolicyManager) as T

        if (!validateResource(copy, 'create')) return

        T savedCopy = modelService.saveModelWithContent(copy) as T
        savedCopy.addCreatedEdit(currentUser)

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedCopy, currentUser, savedCopy.label)
        }

        saveResponse savedCopy
    }

    def exportModel() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(
            params.exporterNamespace, params.exporterName, params.exporterVersion
        )

        if (!exporter) {
            return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")
        }

        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params.dataModelId)
        log.info("Exporting Model using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomain(currentUser, exporter, params[alternateParamsIdKey] as String)
        log.info('Export complete')
        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Model could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "${instance.label}.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    def exportModels() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(params.exporterNamespace, params.exporterName,
                                                                                                      params.exporterVersion)
        if (!exporter) {
            return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")
        }

        // Default through to importing single model
        // This may result in errors due to file containing multiple models, but that should be handled
        if (!exporter.canExportMultipleDomains()) {
            params[alternateParamsIdKey] = params[multipleModelsParamsIdKey].first()
            return exportModel()
        }

        log.info("Exporting DataModel using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomains(currentUser, exporter, params[multipleModelsParamsIdKey])
        log.info('Export complete')

        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Models could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "${multipleModelsParamsIdKey}.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    @Transactional
    def importModel() throws ApiException {

        ModelImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(
            params.importerNamespace, params.importerName, params.importerVersion
        ) as ModelImporterProviderService
        if (!importer) {
            notFound(ImporterProviderService, "${params.importerNamespace}:${params.importerName}:${params.importerVersion}"
            )
            return
        }

        ModelImporterProviderServiceParameters importerProviderServiceParameters

        if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(
                importer, request as AbstractMultipartHttpServletRequest)
        } else {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(importer, request)
        }

        def errors = importerService.validateParameters(importerProviderServiceParameters as ModelImporterProviderServiceParameters,
                                                        importer.importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond errors
            return
        }

        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, importerProviderServiceParameters.folderId)) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, importerProviderServiceParameters.folderId)) {
                return notFound(Folder, importerProviderServiceParameters.folderId)
            }
            return forbiddenDueToPermissions()
        }
        Folder folder = folderService.get(importerProviderServiceParameters.folderId)

        T model = importerService.importModel(currentUser, importer, importerProviderServiceParameters)

        if (!model) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        model.folder = folder

        getModelService().validate(model)

        if (model.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond model.errors
            return
        }

        log.debug('No errors in imported model')

        Model savedModel = getModelService().saveModelWithContent(model)

        log.debug('Saved model')

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedModel, currentUser, savedModel.label)
        }

        log.info('Single Model Import complete')

        if (params.boolean('returnList')) {
            respond([savedModel], status: CREATED, view: 'index')
        } else {
            respond savedModel, status: CREATED, view: 'show'
        }
    }

    @Transactional
    def importModels() throws ApiException {
        ModelImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(
            params.importerNamespace, params.importerName, params.importerVersion
        ) as ModelImporterProviderService
        if (!importer) {
            notFound(ImporterProviderService, "${params.importerNamespace}:${params.importerName}:${params.importerVersion}"
            )
            return
        }

        // Default through to importing single model
        // This may result in errors due to file containing multiple models, but that should be handled
        if (!importer.canImportMultipleDomains()) {
            params.returnList = true
            return importModel()
        }

        ModelImporterProviderServiceParameters importerProviderServiceParameters

        if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(
                importer, request as AbstractMultipartHttpServletRequest)
        } else {
            importerProviderServiceParameters = importerService.extractImporterProviderServiceParameters(importer, request)
        }

        def errors = importerService.validateParameters(importerProviderServiceParameters, importer.importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond errors
            return
        }

        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Folder, importerProviderServiceParameters.folderId)) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, importerProviderServiceParameters.folderId)) {
                return forbiddenDueToPermissions()
            }
            return notFound(Folder, importerProviderServiceParameters.folderId)
        }
        Folder folder = folderService.get(importerProviderServiceParameters.folderId)

        List<T> result = importerService.importModels(currentUser, importer, importerProviderServiceParameters)

        if (!result) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        result.each { m ->
            m.folder = folder
            getModelService().validate(m)
        }

        if (result.any { it.hasErrors() }) {
            log.debug('Errors found in imported models')
            transactionStatus.setRollbackOnly()
            respond(getMultiErrorResponseMap(result), view: '/error', status: UNPROCESSABLE_ENTITY)
            return
        }

        log.debug('No errors in imported models')
        List<Model> savedModels = result.collect {
            Model saved = getModelService().saveModelWithContent(it)
            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(saved, currentUser, saved.label)
            }
            saved
        }
        log.debug('Saved all models')
        log.info('Multi-Model Import complete')

        respond savedModels, status: CREATED, view: 'index'

    }

    @Override
    protected T queryForResource(Serializable id) {
        getModelService().get(id)
    }

    @Override
    protected List<T> listAllReadableResources(Map params) {
        if (params.folderId) {
            return getModelService().findAllByFolderId(Utils.toUuid(params.folderId as String))
        }
        getModelService().findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected T createResource() {
        T model = super.createResource() as T
        model.folder = folderService.get(params.folderId)
        model.authority = authorityService.getDefaultAuthority()
        model
    }

    @Override
    void serviceDeleteResource(T resource) {
        throw new ApiNotYetImplementedException('MC01', 'serviceDeleteResource')
    }


    @Override
    protected void serviceInsertResource(T resource) {
        T model = getModelService().save(DEFAULT_SAVE_ARGS, resource) as T
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(model, currentUser, model.label)
        }
        model
    }

    @Override
    protected T updateResource(T resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        T model = super.updateResource(resource) as T
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(model,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        model
    }

    String getMultipleModelsParamsIdKey() {
        "${alternateParamsIdKey}s"
    }
}
