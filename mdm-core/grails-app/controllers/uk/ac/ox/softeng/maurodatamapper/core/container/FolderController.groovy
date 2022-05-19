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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.exporter.ExporterService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.SearchService
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import grails.web.http.HttpHeaders
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class FolderController extends EditLoggingController<Folder> {
    static responseFormats = ['json', 'xml']

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    FolderService folderService
    SearchService mdmCoreSearchService
    VersionedFolderService versionedFolderService
    ExporterService exporterService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    FolderController() {
        super(Folder)
    }

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond folderList: listAllResources(params), userSecurityPolicyManager: currentUserSecurityPolicyManager
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        resource ? respond(folder: resource, userSecurityPolicyManager: currentUserSecurityPolicyManager) : notFound(params.id)
    }

    def search() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params, 'label')

        PaginatedHibernateSearchResult<CatalogueItem> result =
            mdmCoreSearchService.findAllByFolderIdByHibernateSearch(params.folderId, searchParams, params)
        respond result
    }

    @Override
    @Transactional
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id ?: params.folderId)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id ?: params.folderId)
            return
        }

        if (params.boolean('permanent')) {
            folderService.delete(instance, true)

            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.retrieveUserSecurityPolicyManager(currentUser.emailAddress)
            }

            request.withFormat {
                '*' { render status: NO_CONTENT } // NO CONTENT STATUS CODE
            }
            return
        }

        // Otherwise perform "soft delete"
        folderService.delete(instance)

        if (!validateResource(instance, 'update')) return

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByEveryone() {
        Folder instance = queryForResource(params.folderId)

        if (!instance) return notFound(params.folderId)

        instance.readableByEveryone = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByAuthenticated() {
        Folder instance = queryForResource(params.folderId)

        if (!instance) return notFound(params.folderId)

        instance.readableByAuthenticatedUsers = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def changeFolder() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.folderId)

        if (!instance) return notFound(params.folderId)

        if (instance.deleted) return forbidden('Cannot change the folder of a deleted Folder')

        Folder folder
        if (params.destinationFolderId == 'root') {
            folder = null
        } else {
            folder = folderService.get(params.destinationFolderId)
            if (!folder) return notFound(Folder, params.destinationFolderId)

            if (versionedFolderService.doesMovePlaceVersionedFolderInsideVersionedFolder(instance, folder)) {
                return forbidden('Cannot put a VersionedFolder inside a VersionedFolder')
            }
        }

        instance.parentFolder = folder

        updateResource(instance)

        updateResponse(instance)
    }

    def exportFolder() {
        ExporterProviderService exporter = mauroDataMapperServiceProviderService.findExporterProvider(params.exporterNamespace, params.exporterName, params.exporterVersion)
        if (!exporter) return notFound(ExporterProviderService, "${params.exporterNamespace}:${params.exporterName}:${params.exporterVersion}")

        Folder instance = queryForResource(params.folderId)
        if (!instance) return notFound(params.folderId)

        // Extract body to map and add the params from the url
        Map exporterParameters = extractRequestBodyToMap()
        exporterParameters.putAll(params)

        // Run as async job returns ACCEPTED and the async job which was created
        if (exporterParameters.asynchronous) {
            AsyncJob asyncJob = exporterService.asyncExportDomain(currentUser, exporter, instance, exporterParameters)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        log.info("Exporting Folder using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = exporterService.exportDomain(currentUser, exporter, params.folderId, exporterParameters)
        if (!outputStream) return errorResponse(UNPROCESSABLE_ENTITY, 'Folder could not be exported')
        log.info('Export complete')

        render(file: outputStream.toByteArray(), fileName: "${instance.label}.${exporter.fileExtension}", contentType: exporter.fileType)
    }

    @Override
    protected Folder queryForResource(Serializable id) {
        folderService.get(id)
    }

    @Override
    protected Folder createResource() {
        //Explicitly set the exclude map to empty so that the transient property Folder.userGroups is bound (if present) from the request
        Folder resource = super.createResource([exclude: []]) as Folder
        if (params.folderId) {
            resource.parentFolder = folderService.get(params.folderId)
        }

        if (!resource.label) {
            folderService.generateDefaultFolderLabel(resource)
        }
        resource
    }

    @Override
    protected Folder saveResource(Folder resource) {
        Folder folder = super.saveResource(resource) as Folder
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(folder, currentUser, folder.label)
        }
        folder
    }

    @Override
    protected Folder updateResource(Folder resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        Folder folder = super.updateResource(resource) as Folder
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(folder,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        folder
    }

    @Override
    protected void updateResponse(Folder instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: OK, view: 'update', model: [userSecurityPolicyManager: currentUserSecurityPolicyManager, folder: instance]]
            }

        }
    }

    @Override
    protected void saveResponse(Folder instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: CREATED, view: 'show', model: [userSecurityPolicyManager: currentUserSecurityPolicyManager, folder: instance]]
            }
        }
    }

    @Override
    protected List<Folder> listAllReadableResources(Map params) {
        if (params.folderId) {
            return folderService.findAllByParentId(params.folderId, params)
        }

        folderService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(Folder resource) {
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(resource, currentUser, resource.label)
        }
        folderService.delete(resource)
    }

    @Transactional
    protected boolean validateResource(Folder instance, String view) {
        if (instance.parentFolder) {
            instance.parentFolder = proxyHandler.unwrapIfProxy(instance.parentFolder) as Folder
        }
        if (instance.hasErrors() || !instance.validate()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }
}
