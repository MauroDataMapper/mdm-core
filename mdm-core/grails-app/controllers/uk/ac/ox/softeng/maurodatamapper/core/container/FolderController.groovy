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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.SearchService
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.NO_CONTENT

class FolderController extends EditLoggingController<Folder> {
    static responseFormats = ['json', 'xml']

    FolderService folderService
    SearchService mdmCoreSearchService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    FolderController() {
        super(Folder)
    }

    def search(SearchParams searchParams) {

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.searchTerm = searchParams.searchTerm ?: params.search
        params.max = params.max ?: searchParams.max ?: 10
        params.offset = params.offset ?: searchParams.offset ?: 0
        params.sort = params.sort ?: searchParams.sort ?: 'label'
        if (searchParams.order) {
            params.order = searchParams.order
        }

        PaginatedLuceneResult<CatalogueItem> result = mdmCoreSearchService.findAllByFolderIdByLuceneSearch(params.folderId, searchParams, params)
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

    @Override
    protected Folder queryForResource(Serializable id) {
        folderService.get(id)
    }

    @Override
    protected Folder createResource() {
        //Explicitly set the exclude map to empty so that the transient property Folder.userGroups is bound (if present) from the request
        Folder resource = super.createResource([exclude:[]]) as Folder
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
    protected List<Folder> listAllReadableResources(Map params) {
        if (params.folderId) {
            return folderService.findAllByParentFolderId(params.folderId, params)
        }

        folderService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(Folder resource) {
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(resource, currentUser)
        }
        folderService.delete(resource)
    }
}