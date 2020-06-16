/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.datamodel


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.core.exporter.ExporterService
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.CreateNewVersionData
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DeleteAllParams
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.FinaliseData
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

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
class DataModelController extends CatalogueItemController<DataModel> {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [
        export                       : 'GET', tree: 'GET', types: 'GET', finalise: 'PUT',
        createNewDocumentationVersion: 'PUT', createNewVersion: 'PUT'
    ]

    DataModelService dataModelService
    FolderService folderService
    SearchService searchService

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ExporterService exporterService
    ImporterService importerService

    @Autowired
    Set<DefaultDataTypeProvider> defaultDataTypeProviders

    @Autowired(required = false)
    Set<DataModelExporterProviderService> dataModelExporterProviderServices

    @Autowired(required = false)
    Set<DataModelImporterProviderService> dataModelImporterProviderServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    DataModelController() {
        super(DataModel)
    }

    def types() {
        respond DataModelType.labels()
    }

    def exporterProviders() {
        respond exporterProviders: dataModelExporterProviderServices
    }

    def importerProviders() {
        respond importerProviders: dataModelImporterProviderServices
    }

    def defaultDataTypeProviders() {
        respond providers: defaultDataTypeProviders
    }

    def hierarchy() {
        params.deep = true
        show()
    }

    @Override
    def show() {
        params.id = params.id ?: params.dataModelId
        super.show()
    }

    @Transactional
    @Override
    def update() {
        log.trace('Update')
        if (handleReadOnly()) return

        DataModel instance = queryForResource(params.id)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (instance.finalised) return forbidden('Cannot update a finalised DataModel')

        instance.properties = getObjectToBind()

        instance = checkForAndAddDefaultDataTypes instance

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

        DataModel instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (params.boolean('permanent')) {
            dataModelService.delete(instance, true)
            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.removeSecurityForSecurableResource(instance, currentUser)
            }
            request.withFormat {
                '*' {render status: NO_CONTENT} // NO CONTENT STATUS CODE
            }
            return
        }

        // Otherwise perform "soft delete"
        dataModelService.delete(instance)

        updateResource(instance)

        updateResponse(instance)
    }

    @Transactional
    def readByEveryone() {
        DataModel instance = queryForResource(params.dataModelId)

        if (!instance) return notFound(params.dataModelId)

        instance.readableByEveryone = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByAuthenticated() {
        DataModel instance = queryForResource(params.dataModelId)

        if (!instance) return notFound(params.dataModelId)

        instance.readableByAuthenticatedUsers = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def changeFolder() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.dataModelId)

        if (!instance) return notFound(params.dataModelId)

        if (instance.deleted) return forbidden('Cannot change the folder of a deleted DataModel')

        Folder folder = folderService.get(params.folderId)
        if (!folder) return notFound(Folder, params.folderId)

        instance.folder = folder

        updateResource(instance)

        updateResponse(instance)
    }

    def diff() {
        DataModel thisDataModel = queryForResource params.dataModelId
        DataModel otherDataModel = queryForResource params.otherDataModelId

        if (!thisDataModel) return notFound(params.dataModelId)
        if (!otherDataModel) return notFound(params.otherDataModelId)

        respond dataModelService.diff(thisDataModel, otherDataModel)
    }

    @Transactional
    def finalise(FinaliseData finaliseData) {

        DataModel instance = queryForResource params.dataModelId

        if (!instance) return notFound(params.dataModelId)

        instance = dataModelService.finaliseDataModel(instance, currentUser, finaliseData.supersededBy ?: [])

        if (!validateResource(instance, 'update')) return

        updateResource instance

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(instance,
                                                                                                               ['finalised'] as HashSet,
                                                                                                               currentUser)
        }

        updateResponse instance
    }

    @Transactional
    def newDocumentationVersion(CreateNewVersionData createNewVersionData) {

        createNewVersionData.label = 'newDocumentationVersion'

        if (!createNewVersionData.validate()) {
            respond createNewVersionData.errors
            return
        }

        DataModel instance = queryForResource params.dataModelId

        if (!instance) return notFound(params.dataModelId)

        DataModel copy = dataModelService.createNewDocumentationVersion(instance, currentUser, createNewVersionData.copyPermissions,
                                                                        createNewVersionData.moveDataFlows)

        if (!validateResource(copy, 'create')) return

        params.editable = true
        saveResource copy

        saveResponse copy
    }

    @Transactional
    def newModelVersion(CreateNewVersionData createNewVersionData) {

        if (createNewVersionData.hasErrors()) {
            respond createNewVersionData.errors
            return
        }

        DataModel instance = queryForResource params.dataModelId

        if (!instance) return notFound(params.dataModelId)

        if (!currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(DataModel, params.dataModelId)) {
            createNewVersionData.copyPermissions = false
        }

        try {
            DataModel copy = dataModelService.createNewModelVersion(createNewVersionData.label, instance, currentUser,
                                                                    createNewVersionData.copyPermissions,
                                                                    createNewVersionData.copyDataFlows)

            if (!validateResource(copy, 'create')) return

            params.editable = true
            saveResource copy

            saveResponse copy

        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }
    }

    def exportDataModel() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(
            params.exporterNamespace, params.exporterName, params.exporterVersion
        )

        if (!exporter) return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}"
        )


        DataModel instance = queryForResource params.dataModelId

        if (!instance) return notFound(params.dataModelId)
        log.info("Exporting DataModel using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomain(currentUser, exporter, params.dataModelId as String)
        log.info('Export complete')
        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'DataModel could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "${instance.label}.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    def exportDataModels() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(params.exporterNamespace, params.exporterName,
                                                                                                      params.exporterVersion)
        if (!exporter) return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}"
        )

        // Default through to importing single model
        // This may result in errors due to file containing multiple models, but that should be handled
        if (!exporter.canExportMultipleDomains()) {
            params.dataModelId = params.dataModelIds.first()
            return exportDataModel()
        }

        log.info("Exporting DataModel using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomains(currentUser, exporter, params.dataModelIds)
        log.info('Export complete')

        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'DataModels could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: "DataModels.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    @Transactional
    def importDataModel() throws ApiException {

        DataModelImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(
            params.importerNamespace, params.importerName, params.importerVersion
        ) as DataModelImporterProviderService
        if (!importer) {
            notFound(DataModelImporterProviderService, "${params.importerNamespace}:${params.importerName}:${params.importerVersion}"
            )
            return
        }

        DataModelImporterProviderServiceParameters importerProviderServiceParameters

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
                return notFound(Folder, importerProviderServiceParameters.folderId)
            }
            return forbiddenDueToPermissions()
        }
        Folder folder = folderService.get(importerProviderServiceParameters.folderId)

        DataModel dataModel = importerService.importModel(currentUser, importer, importerProviderServiceParameters)

        if (!dataModel) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        dataModel.folder = folder

        dataModelService.validate(dataModel)

        if (dataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataModel.errors
            return
        }

        log.debug('No errors in imported model')

        dataModelService.saveWithBatching(dataModel)

        log.debug('Saved model')

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(dataModel, currentUser, dataModel.label)
        }

        log.info('Single DataModel Import complete')

        if (params.boolean('returnList')) {
            respond([dataModel], status: CREATED, view: 'index')
        } else {
            respond dataModel, status: CREATED, view: 'show'
        }
    }

    @Transactional
    def importDataModels() throws ApiException {
        DataModelImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(
            params.importerNamespace, params.importerName, params.importerVersion
        ) as DataModelImporterProviderService
        if (!importer) {
            notFound(DataModelImporterProviderService, "${params.importerNamespace}:${params.importerName}:${params.importerVersion}"
            )
            return
        }

        // Default through to importing single model
        // This may result in errors due to file containing multiple models, but that should be handled
        if (!importer.canImportMultipleDomains()) {
            params.returnList = true
            return importDataModel()
        }

        DataModelImporterProviderServiceParameters importerProviderServiceParameters

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

        List<DataModel> result = importerService.importModels(currentUser, importer, importerProviderServiceParameters)

        if (!result) {
            transactionStatus.setRollbackOnly()
            return errorResponse(UNPROCESSABLE_ENTITY, 'No model imported')
        }

        result.each {dm ->
            dm.folder = folder
            dataModelService.validate(dm)
        }

        if (result.any {it.hasErrors()}) {
            log.debug('Errors found in imported models')
            transactionStatus.setRollbackOnly()
            respond(getMultiErrorResponseMap(result), view: '/error', status: UNPROCESSABLE_ENTITY)
            return
        }

        log.debug('No errors in imported models')
        result.each {
            dataModelService.saveWithBatching(it)
            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(it, currentUser, it.label)
            }
        }
        log.debug('Saved all models')
        log.info('Multi-DataModel Import complete')

        respond result, status: CREATED, view: 'index'

    }

    @Transactional
    def deleteAllUnusedDataClasses() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = queryForResource params.dataModelId

        if (!dataModel) return notFound(params.dataModelId)

        dataModelService.deleteAllUnusedDataClasses(dataModel)

        render status: NO_CONTENT // NO CONTENT STATUS CODE
    }

    @Transactional
    def deleteAllUnusedDataTypes() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = queryForResource params.dataModelId

        if (!dataModel) return notFound(params.dataModelId)

        dataModelService.deleteAllUnusedDataTypes(dataModel)

        render status: NO_CONTENT // NO CONTENT STATUS CODE
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

        List<DataModel> deleted = dataModelService.deleteAll(deleteAllParams.ids, deleteAllParams.permanent)

        if (deleted) {
            deleted.each {
                updateResource(it)
            }
            respond deleted, status: HttpStatus.OK, view: 'index'
        } else {
            render status: NO_CONTENT
        }
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        params.max = params.max ?: searchParams.max ?: 10
        params.offset = params.offset ?: searchParams.offset ?: 0

        PaginatedLuceneResult<ModelItem> result = searchService.findAllByDataModelIdByLuceneSearch(params.dataModelId,
                                                                                                   searchParams, params)
        respond result
    }

    def suggestLinks() {
        DataModel dataModel = queryForResource params.dataModelId
        DataModel otherDataModel = queryForResource params.otherDataModelId

        if (!dataModel) return notFound(params.dataModelId)
        if (!otherDataModel) return notFound(params.otherDataModelId)

        int maxResults = params.max ?: 5

        respond dataModelService.suggestLinksBetweenModels(dataModel, otherDataModel, maxResults)
    }

    @Override
    protected DataModel queryForResource(Serializable id) {
        DataModel dataModel = super.queryForResource(id)
        if (dataModel && dataModel.finalised) params.editable = false
        dataModel
    }

    @Override
    protected List<DataModel> listAllReadableResources(Map params) {
        if (params.folderId) {
            return dataModelService.findAllByFolderId(Utils.toUuid(params.folderId))
        }
        dataModelService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected DataModel createResource() {
        DataModel dataModel = super.createResource() as DataModel
        dataModel.folder = folderService.get(params.folderId)
        checkForAndAddDefaultDataTypes dataModel
    }

    @Override
    void serviceDeleteResource(DataModel resource) {
        throw new ApiNotYetImplementedException('DMC01', 'serviceDeleteResource')
    }

    @Override
    protected void serviceInsertResource(DataModel resource) {
        DataModel dataModel = dataModelService.save(resource)
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(dataModel, currentUser, dataModel.label)
        }
        dataModel
    }

    @Override
    protected DataModel updateResource(DataModel resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        DataModel dataModel = super.updateResource(resource) as DataModel
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(dataModel,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        dataModel
    }

    protected DataModel checkForAndAddDefaultDataTypes(DataModel resource) {
        if (params.defaultDataTypeProvider) {
            DefaultDataTypeProvider provider = defaultDataTypeProviders.find {it.displayName == params.defaultDataTypeProvider}
            if (provider) {
                log.debug("Adding ${provider.displayName} default DataTypes")
                return provider.addDefaultListOfDataTypesToDataModel(resource)
            }
        }
        resource
    }

    /*
        def getProfile() {
            DataModel dataModel = queryForResource params.dataModelId
            System.err.println("Getting dataModel profile: " + dataModel.label)
            Profile p
            if(params.profileName == "dcat") {
                p = new DcatProfile()
            }
            else {
                return errorResponse(UNPROCESSABLE_ENTITY, 'No profile matching that name')
            }
            Object t = p.getProfile(dataModel)
            render contentType: p.getContentType(), text: p.renderProfile(t)
        }

        def setProfile() {
            DataModel dataModel = queryForResource params.dataModelId
            System.err.println("Setting dataModel profile: " + dataModel.label)
            Profile p
            if(params.profileName == "dcat") {
                p = new DcatProfile()
            }
            else {
                return errorResponse(UNPROCESSABLE_ENTITY, 'No profile matching that name')
            }
            System.err.println(request.getJSON())

            def t = p.readProfileFromBody(request.getJSON())

            dataModelService.setProfile((Dcat) t, (DcatProfile) p, dataModel, currentUser)

            render contentType: p.getContentType(), text: p.renderProfile(t)

        }*/


}
