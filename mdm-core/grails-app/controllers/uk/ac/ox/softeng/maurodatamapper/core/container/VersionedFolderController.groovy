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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CreateNewVersionData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.FinaliseData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.SearchService
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED
import static org.springframework.http.HttpStatus.NO_CONTENT

class VersionedFolderController extends EditLoggingController<VersionedFolder> {
    static responseFormats = ['json', 'xml']

    FolderService folderService
    AuthorityService authorityService
    VersionedFolderService versionedFolderService
    SearchService mdmCoreSearchService

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    VersionedFolderController() {
        super(VersionedFolder)
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

        PaginatedLuceneResult<CatalogueItem> result =
            mdmCoreSearchService.findAllByFolderIdByLuceneSearch(params.versionedFolderId, searchParams, params)
        respond result
    }

    @Override
    @Transactional
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id ?: params.versionedFolderId)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id ?: params.versionedFolderId)
            return
        }

        if (params.boolean('permanent')) {
            versionedFolderService.delete(instance, true)

            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager = securityPolicyManagerService.retrieveUserSecurityPolicyManager(currentUser.emailAddress)
            }

            request.withFormat {
                '*' { render status: NO_CONTENT } // NO CONTENT STATUS CODE
            }
            return
        }

        // Otherwise perform "soft delete"
        versionedFolderService.delete(instance)
        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByEveryone() {
        VersionedFolder instance = queryForResource(params.versionedFolderId)

        if (!instance) return notFound(params.versionedFolderId)

        instance.readableByEveryone = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByAuthenticated() {
        VersionedFolder instance = queryForResource(params.versionedFolderId)

        if (!instance) return notFound(params.versionedFolderId)

        instance.readableByAuthenticatedUsers = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def finalise(FinaliseData finaliseData) {

        if (!finaliseData.validate()) {
            respond finaliseData.errors
            return
        }

        VersionedFolder instance = queryForResource(params.versionedFolderId)

        if (!instance) return notFound(params.versionedFolderId)

        if (instance.branchName != VersionAwareConstraints.DEFAULT_BRANCH_NAME) return METHOD_NOT_ALLOWED

        instance = versionedFolderService.finaliseFolder(instance, currentUser,
                                                         finaliseData.version,
                                                         finaliseData.versionChangeType,
                                                         finaliseData.getVersionTag())

        if (!validateResource(instance, 'update')) return

        if (finaliseData.changeNotice) {
            instance.addChangeNoticeEdit(currentUser, finaliseData.changeNotice)
        }

        Set<String> changedProperties = instance.getDirtyPropertyNames()

        updateResource(instance)
        updateSecurity(instance, changedProperties)
        updateResponse(instance)
    }

    @Transactional
    def newBranchModelVersion(CreateNewVersionData createNewVersionData) {

        createNewVersionData.label = 'newBranchModelVersion'

        if (!createNewVersionData.validate()) {
            respond createNewVersionData.errors
            return
        }

        VersionedFolder instance = queryForResource(params.versionedFolderId)

        if (!instance) return notFound(params.versionedFolderId)

        VersionedFolder copy = versionedFolderService.createNewBranchModelVersion(createNewVersionData.branchName, instance, currentUser,
                                                                                  createNewVersionData.copyPermissions,
                                                                                  currentUserSecurityPolicyManager)

        if (!validateResource(copy, 'create')) return

        VersionedFolder savedCopy = versionedFolderService.saveFolderWithContent(copy)
        savedCopy.addCreatedEdit(currentUser)

        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedCopy, currentUser, savedCopy.label)
        }

        saveResponse savedCopy
    }


    @Override
    protected VersionedFolder queryForResource(Serializable id) {
        versionedFolderService.get(id)
    }

    @Override
    protected VersionedFolder createResource() {
        //Explicitly set the exclude map to empty so that the transient property VersionedFolder.userGroups is bound (if present) from the request
        VersionedFolder resource = super.createResource([exclude: []]) as VersionedFolder
        if (params.versionedFolderId) {
            resource.parentFolder = versionedFolderService.get(params.versionedFolderId)
        }
        if (params.folderId) {
            resource.parentFolder = folderService.get(params.folderId)
        }
        resource.authority = authorityService.getDefaultAuthority()
        if (!resource.label) {
            versionedFolderService.generateDefaultFolderLabel(resource)
        }
        resource
    }

    @Override
    protected VersionedFolder saveResource(VersionedFolder resource) {
        VersionedFolder folder = super.saveResource(resource) as VersionedFolder
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(folder, currentUser, folder.label)
        }
        folder
    }

    @Override
    protected VersionedFolder updateResource(VersionedFolder resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        VersionedFolder folder = super.updateResource(resource) as VersionedFolder
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(folder,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        folder
    }

    @Override
    protected List<VersionedFolder> listAllReadableResources(Map params) {
        if (params.versionedFolderId) {
            return versionedFolderService.findAllByParentId(params.versionedFolderId, params)
        }

        versionedFolderService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(VersionedFolder resource) {
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(resource, currentUser)
        }
        versionedFolderService.delete(resource)
    }

    protected VersionedFolder updateSecurity(VersionedFolder instance, Set<String> changedProperties) {
        modelServices.each { service ->
            Collection<Model> modelsInFolder = service.findAllByFolderId(instance.id)
            modelsInFolder.each { model ->
                if (securityPolicyManagerService) {
                    currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(model as SecurableResource,
                                                                                                                       changedProperties,
                                                                                                                       currentUser)
                }
            }
        }
        instance
    }

}
