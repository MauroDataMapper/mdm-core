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
package uk.ac.ox.softeng.maurodatamapper.core.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExport
import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExportService
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
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
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.MergeIntoData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CreateNewVersionData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.DeleteAllParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.FinaliseData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.artefact.Artefact
import grails.gorm.transactions.Transactional
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest

import static org.springframework.http.HttpStatus.CREATED
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
    VersionedFolderService versionedFolderService
    AsyncJobService asyncJobService
    DomainExportService domainExportService

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
        if (instance.deleted) return forbidden('Cannot update a deleted Model')

        instance.properties = getObjectToBind()

        instance = performAdditionalUpdates(instance)

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
                '*' {render status: NO_CONTENT} // NO CONTENT STATUS CODE
            }
            return
        }

        // Otherwise perform "soft delete"
        getModelService().softDeleteModel(instance)

        updateResource(instance)

        updateResponse(instance)
    }

    @Transactional
    def undoSoftDelete() {
        if (handleReadOnly()) {
            return
        }

        T instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        getModelService().undoSoftDeleteModel(instance)

        updateResource instance
        updateResponse instance
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

        if (versionedFolderService.isVersionedFolderFamily(folder) && instance.finalised) return forbidden('Cannot move a finalised model into a VersionedFolder')

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

        T source = queryForResource params[alternateParamsIdKey]
        if (!source) return notFound(params[alternateParamsIdKey])

        T target = queryForResource params.otherModelId
        if (!target) return notFound(params.otherModelId)

        respond modelService.getMergeDiffForModels(source, target)
    }

    @Transactional
    def mergeInto(MergeIntoData mergeIntoData) {
        if (!mergeIntoData.validate()) {
            respond mergeIntoData.errors
            return
        }

        if (mergeIntoData.patch.sourceId != params[alternateParamsIdKey]) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Source model id passed in request body does not match source model id in URI.')
        }
        if (mergeIntoData.patch.targetId != params.otherModelId) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Target model id passed in request body does not match target model id in URI.')
        }

        T sourceModel = queryForResource params[alternateParamsIdKey]
        if (!sourceModel) return notFound(params[alternateParamsIdKey])

        T targetModel = queryForResource params.otherModelId
        if (!targetModel) return notFound(params.otherModelId)

        T instance = modelService.mergeObjectPatchDataIntoModel(mergeIntoData.patch, targetModel, sourceModel, currentUserSecurityPolicyManager) as T

        if (!validateResource(instance, 'merge')) return

        if (mergeIntoData.deleteBranch) {
            if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(sourceModel.class, sourceModel.id)) {
                return forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(sourceModel.class, sourceModel.id))
            }
            modelService.permanentDeleteModel(sourceModel)
            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.retrieveUserSecurityPolicyManager(currentUser.emailAddress)
            }
        }

        if (mergeIntoData.changeNotice) {
            instance.addChangeNoticeEdit(currentUser, mergeIntoData.changeNotice)
        }

        updateResource(instance)

        updateResponse(instance)
    }

    def currentMainBranch() {
        T source = queryForResource(params[alternateParamsIdKey])
        if (!source) return notFound(params[alternateParamsIdKey])

        respond modelService.findCurrentMainBranchByLabel(source.label)
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

        if (instance.branchName != VersionAwareConstraints.DEFAULT_BRANCH_NAME) return forbidden('Cannot finalise a non-main branch')

        if (versionedFolderService.isVersionedFolderFamily(instance.folder)) return forbidden('Cannot finalise a model inside a VersionedFolder')

        instance = modelService.finaliseModel(instance,
                                              currentUser,
                                              finaliseData.version,
                                              finaliseData.versionChangeType,
                                              finaliseData.versionTag) as T

        if (!validateResource(instance, 'update')) return

        if (finaliseData.changeNotice) {
            instance.addChangeNoticeEdit(currentUser, finaliseData.changeNotice)
        }

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

        if (!instance.finalised) return forbidden('Cannot create a new version of a non-finalised model')
        if (versionedFolderService.isVersionedFolderFamily(instance.folder)) return forbidden('Cannot create a new branch model version of a model inside a VersionedFolder')

        // Run as async job returns ACCEPTED and the async job which was created
        if (createNewVersionData.asynchronous) {

            AsyncJob asyncJob = getModelService().asyncCreateNewBranchModelVersion(createNewVersionData.branchName, instance, currentUser,
                                                                                   createNewVersionData.copyPermissions,
                                                                                   currentUserSecurityPolicyManager)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

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

        if (!instance.finalised) return forbidden('Cannot create a new version of a non-finalised model')
        if (versionedFolderService.isVersionedFolderFamily(instance.folder)) return forbidden('Cannot create a new documentation version of a model inside a VersionedFolder')

        // Run as async job returns ACCEPTED and the async job which was created
        if (createNewVersionData.asynchronous) {
            AsyncJob asyncJob = getModelService().asyncCreateNewDocumentationVersion(instance,
                                                                                     currentUser,
                                                                                     createNewVersionData.copyPermissions,
                                                                                     currentUserSecurityPolicyManager)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

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

        if (!instance.finalised) return forbidden('Cannot create a new version of a non-finalised model')

        if (!currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(resource, params[alternateParamsIdKey])) {
            createNewVersionData.copyPermissions = false
        }

        // Run as async job returns ACCEPTED and the async job which was created
        if (createNewVersionData.asynchronous) {
            AsyncJob asyncJob = getModelService().asyncCreateNewForkModel(createNewVersionData.label,
                                                                          instance,
                                                                          currentUser,
                                                                          createNewVersionData.copyPermissions,
                                                                          currentUserSecurityPolicyManager)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
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

        if (!instance) return notFound(params[alternateParamsIdKey])

        // Extract body to map and add the params from the url
        Map exporterParameters = extractRequestBodyToMap()
        exporterParameters.putAll(params)

        // Run as async job returns ACCEPTED and the async job which was created
        if (exporterParameters.asynchronous) {
            AsyncJob asyncJob = exporterService.asyncExportDomain(currentUser, exporter, instance, exporterParameters)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        log.info("Exporting Model using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomain(currentUser, exporter, params[alternateParamsIdKey], exporterParameters)
        log.info('Export complete')
        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Model could not be exported')
        }

        // Cache the export
        DomainExport domainExport = domainExportService.createAndSaveNewDomainExport(exporter, instance, exporter.getFileName(instance), outputStream, currentUser)

        render(file: domainExport.exportData, fileName: domainExport.exportFileName, contentType: domainExport.exportContentType)
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

        // Extract body to map and add the params from the url
        Map exporterParameters = extractRequestBodyToMap()
        exporterParameters.putAll(params)

        List<T> domains = getModelService().getAll(exporterParameters[multipleModelsParamsIdKey])

        // Run as async job returns ACCEPTED and the async job which was created
        if (exporterParameters.asynchronous) {
            AsyncJob asyncJob = exporterService.asyncExportDomains(currentUser, exporter, domains, exporterParameters)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        log.info("Exporting DataModel using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomains(currentUser, exporter, exporterParameters[multipleModelsParamsIdKey], exporterParameters)
        log.info('Export complete')

        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Models could not be exported')
        }

        // Cache the export
        DomainExport domainExport = domainExportService.createAndSaveNewDomainExport(exporter, domains, "${UUID.randomUUID()}.${exporter.fileExtension}",
                                                                                     outputStream, currentUser)

        render(file: domainExport.exportData, fileName: domainExport.exportFileName, contentType: domainExport.exportContentType)
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

        if (!currentUserSecurityPolicyManager.userCanCreateResourceId(resource, null, Folder, importerProviderServiceParameters.folderId)) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, importerProviderServiceParameters.folderId)) {
                return notFound(Folder, importerProviderServiceParameters.folderId)
            }
            return forbiddenDueToPermissions()
        }
        Folder folder = folderService.get(importerProviderServiceParameters.folderId)

        // Run as async job returns ACCEPTED and the async job which was created
        if (importerProviderServiceParameters.asynchronous) {
            AsyncJob asyncJob = importerService.asyncImportDomain(currentUser, importer, importerProviderServiceParameters, getModelService(),
                                                                  folder)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        T model = importerService.importDomain(currentUser, importer, importerProviderServiceParameters)

        if (!model) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        Model savedModel

        if (!importerProviderServiceParameters.providerHasSavedModels) {
            if (versionedFolderService.isVersionedFolderFamily(folder) && model.finalised) {
                transactionStatus.setRollbackOnly()
                return forbidden('Cannot import a finalised model into a VersionedFolder')
            }

            model.folder = folder

            getModelService().validate(model)

            if (model.hasErrors()) {
                transactionStatus.setRollbackOnly()
                respond model.errors
                return
            }

            log.debug('No errors in imported model')

            savedModel = saveNewModelAndAddSecurity(model)
        } else {
            savedModel = addSecurity(model)
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

        println 'params class = ' + importerProviderServiceParameters.class

        def errors = importerService.validateParameters(importerProviderServiceParameters, importer.importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond errors
            return
        }

        if (!currentUserSecurityPolicyManager.userCanCreateResourceId(resource, null, Folder, importerProviderServiceParameters.folderId)) {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Folder, importerProviderServiceParameters.folderId)) {
                return notFound(Folder, importerProviderServiceParameters.folderId)
            }
            return forbiddenDueToPermissions()
        }
        Folder folder = folderService.get(importerProviderServiceParameters.folderId)

        // Run as async job returns ACCEPTED and the async job which was created
        if (importerProviderServiceParameters.asynchronous) {
            AsyncJob asyncJob = importerService.asyncImportDomains(currentUser, importer, importerProviderServiceParameters, getModelService(),
                                                                   folder)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        List<T> result = importerService.importDomains(currentUser, importer, importerProviderServiceParameters)

        if (!result) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        List<T> loadedModels
        if (!importerProviderServiceParameters.providerHasSavedModels) {
            if (versionedFolderService.isVersionedFolderFamily(folder) && result.any {it.finalised}) {
                transactionStatus.setRollbackOnly()
                return forbidden('Cannot import finalised models into a VersionedFolder')
            }

            result.each {m ->
                m.folder = folder
            }

            getModelService().validateMultipleModels(result)

            if (result.any {it.hasErrors()}) {
                log.debug('Errors found in imported models')
                transactionStatus.setRollbackOnly()
                respond(getMultiErrorResponseMap(result), view: '/error', status: UNPROCESSABLE_ENTITY)
                return
            }

            log.debug('No errors in imported models')
            List<T> savedModels = result.collect {
                saveNewModelAndAddSecurity(it)
            }

            log.debug('Saved all models')
            loadedModels = savedModels.collect {
                getModelService().get(it.id)
            } as List<T>
        } else {
            loadedModels = result.collect {
                addSecurity(it)
            }
        }

        log.info('Multi-Model Import complete')

        respond loadedModels, status: CREATED, view: 'index'
    }

    def modelVersionTree() {
        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params[alternateParamsIdKey])

        T oldestAncestor = modelService.findOldestAncestor(instance) as T

        List<VersionTreeModel> versionTreeModelList = modelService.buildModelVersionTree(oldestAncestor, null,
                                                                                         null, true, false,
                                                                                         currentUserSecurityPolicyManager)
        respond versionTreeModelList
    }

    def simpleModelVersionTree() {
        T instance = queryForResource params[alternateParamsIdKey]

        if (!instance) return notFound(params[alternateParamsIdKey])

        T oldestAncestor = modelService.findOldestAncestor(instance) as T

        boolean branchesOnly = params.boolean('branchesOnly', false)
        boolean forMerge = params.boolean('forMerge', false)

        List<VersionTreeModel> versionTreeModelList = modelService.buildModelVersionTree(oldestAncestor, null,
                                                                                         null, false,
                                                                                         branchesOnly || forMerge,
                                                                                         currentUserSecurityPolicyManager)
        respond versionTreeModelList.findAll {
            (!(it.newFork || (forMerge && it.id == instance.id.toString())))
        }
    }

    @Transactional
    def deleteAll() {
        // Command object binding is not performed when the HTTP method id DELETE
        DeleteAllParams deleteAllParams = new DeleteAllParams()
        bindData deleteAllParams, request.inputStream
        deleteAllParams.validate()

        if (deleteAllParams.hasErrors()) {
            respond deleteAllParams.errors, status: UNPROCESSABLE_ENTITY
            return
        }

        List<T> deleted = modelService.deleteAll(deleteAllParams.ids, deleteAllParams.permanent)

        if (deleted) {
            deleted.each {
                updateResource(it)
            }
            respond deleted, status: HttpStatus.OK, view: 'index'
        } else {
            render status: NO_CONTENT
        }
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
        Folder folder = folderService.get(params.folderId)
        // If the folder is inside/is a VF then we need to make sure the created DM uses the same branch name as the VF
        if (versionedFolderService.isVersionedFolderFamily(folder)) {
            model.branchName = versionedFolderService.getVersionedFolderParent(folder).branchName
        }
        model.folder = folder
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
    protected void serviceUpdateResource(T resource) {
        getModelService().save(DEFAULT_SAVE_ARGS, resource) as T
    }

    @Override
    protected T updateResource(T resource) {
        long start = System.currentTimeMillis()
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        T model = super.updateResource(resource) as T
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(model,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        log.debug('Updated resource took {}', Utils.timeTaken(start))
        model
    }

    @Override
    @Transactional
    protected boolean validateResource(T instance, String view) {
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        if (view == 'update') modelService.shallowValidate(instance)
        else modelService.validate(instance)
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }

    protected String getMultipleModelsParamsIdKey() {
        "${alternateParamsIdKey}s"
    }

    protected T saveNewModelAndAddSecurity(T model) {
        T savedModel = getModelService().saveModelWithContent(model) as T
        log.debug('Saved model')
        addSecurity(savedModel)
    }

    protected T addSecurity(T model) {
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(model, currentUser, model.label)
        }
        model
    }

    protected T performAdditionalUpdates(T model) {
        // default is no-op
        model
    }
}
