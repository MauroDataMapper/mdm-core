/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.MergeIntoData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CreateNewVersionData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.FinaliseData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.search.SearchService
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

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

    def search() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())
        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        searchParams.crossValuesIntoParametersMap(params, 'label')

        PaginatedHibernateSearchResult<CatalogueItem> result =
            mdmCoreSearchService.findAllByFolderIdByHibernateSearch(params.versionedFolderId, searchParams, params)
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
                '*' {render status: NO_CONTENT} // NO CONTENT STATUS CODE
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

        if (instance.branchName != VersionAwareConstraints.DEFAULT_BRANCH_NAME) return forbidden('Cannot finalise a non-main branch')

        instance = versionedFolderService.finaliseFolder(instance, currentUser,
                                                         finaliseData.version,
                                                         finaliseData.versionChangeType,
                                                         finaliseData.getVersionTag())

        if (!validateResource(instance, 'update')) return

        if (finaliseData.changeNotice) {
            instance.addChangeNoticeEdit(currentUser, finaliseData.changeNotice)
        }

        Set<String> changedProperties = instance.getDirtyPropertyNames()

        updateResourceAndHierarchy(instance, changedProperties)
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

        if (!instance.finalised) return forbidden('Cannot create a new version of a non-finalised model')

        // Run as async job returns ACCEPTED and the async job which was created
        if (createNewVersionData.asynchronous) {

            AsyncJob asyncJob = versionedFolderService.asyncCreateNewBranchModelVersion(createNewVersionData.branchName, instance, currentUser,
                                                                                        createNewVersionData.copyPermissions,
                                                                                        currentUserSecurityPolicyManager)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        VersionedFolder copy = versionedFolderService.createNewBranchModelVersion(createNewVersionData.branchName, instance, currentUser,
                                                                                  createNewVersionData.copyPermissions,
                                                                                  currentUserSecurityPolicyManager)

        if (!validateResource(copy, 'create')) return

        saveResourceAndHierarchy(copy)

        saveResponse(copy)
    }

    @Transactional
    def newForkModel(CreateNewVersionData createNewVersionData) {

        if (createNewVersionData.hasErrors()) {
            respond createNewVersionData.errors
            return
        }

        VersionedFolder instance = queryForResource(params.versionedFolderId)

        if (!instance) return notFound(params.versionedFolderId)

        if (!instance.finalised) return forbidden('Cannot create a new version of a non-finalised model')

        if (!currentUserSecurityPolicyManager.userCanCreateSecuredResourceId(resource, params.versionedFolderId)) {
            createNewVersionData.copyPermissions = false
        }

        // Run as async job returns ACCEPTED and the async job which was created
        if (createNewVersionData.asynchronous) {
            AsyncJob asyncJob = versionedFolderService.asyncCreateNewForkModel(createNewVersionData.label,
                                                                               instance,
                                                                               currentUser,
                                                                               createNewVersionData.copyPermissions,
                                                                               currentUserSecurityPolicyManager)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        VersionedFolder copy = versionedFolderService.createNewForkModel(createNewVersionData.label,
                                                                         instance,
                                                                         currentUser,
                                                                         createNewVersionData.copyPermissions,
                                                                         currentUserSecurityPolicyManager) as VersionedFolder

        if (!validateResource(copy, 'create')) return

        saveResourceAndHierarchy(copy)

        saveResponse(copy)
    }

    @Transactional
    def newDocumentationVersion(CreateNewVersionData createNewVersionData) {

        createNewVersionData.label = 'newDocumentationVersion'

        if (!createNewVersionData.validate()) {
            respond createNewVersionData.errors
            return
        }

        VersionedFolder instance = queryForResource(params.versionedFolderId)

        if (!instance) return notFound(params.versionedFolderId)

        if (!instance.finalised) return forbidden('Cannot create a new version of a non-finalised model')


        // Run as async job returns ACCEPTED and the async job which was created
        if (createNewVersionData.asynchronous) {
            AsyncJob asyncJob = versionedFolderService.asyncCreateNewDocumentationVersion(instance,
                                                                                          currentUser,
                                                                                          createNewVersionData.copyPermissions,
                                                                                          currentUserSecurityPolicyManager)
            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        VersionedFolder copy = versionedFolderService.createNewDocumentationVersion(instance, currentUser, createNewVersionData.copyPermissions,
                                                                                    currentUserSecurityPolicyManager)

        if (!validateResource(copy, 'create')) return

        if (!validateResource(copy, 'create')) return

        saveResourceAndHierarchy(copy)

        saveResponse(copy)
    }


    def latestFinalisedModel() {
        VersionedFolder source = queryForResource(params.versionedFolderId)
        if (!source) return notFound(params.versionedFolderId)

        respond versionedFolderService.findLatestFinalisedFolderByLabel(source.label)
    }

    def latestModelVersion() {
        VersionedFolder source = queryForResource(params.versionedFolderId)
        if (!source) return notFound(params.versionedFolderId)

        respond versionedFolderService.getLatestModelVersionByLabel(source.label)
    }

    def currentMainBranch() {
        VersionedFolder source = queryForResource(params.versionedFolderId)
        if (!source) return notFound(params.versionedFolderId)

        respond versionedFolderService.findCurrentMainBranchForModel(source)
    }

    def availableBranches() {
        VersionedFolder source = queryForResource(params.versionedFolderId)
        if (!source) return notFound(params.versionedFolderId)

        respond versionedFolderService.findAllAvailableBranchesByLabel(source.label)
    }

    def modelVersionTree() {
        VersionedFolder instance = queryForResource(params.versionedFolderId)
        if (!instance) return notFound(params.versionedFolderId)

        VersionedFolder oldestAncestor = versionedFolderService.findOldestAncestor(instance)

        List<VersionTreeModel> versionTreeModelList = versionedFolderService.buildModelVersionTree(oldestAncestor, null,
                                                                                                   null, true,
                                                                                                   currentUserSecurityPolicyManager)
        respond versionTreeModelList
    }

    def commonAncestor() {
        VersionedFolder left = queryForResource(params.versionedFolderId)
        if (!left) return notFound(params.versionedFolderId)

        VersionedFolder right = queryForResource(params.otherVersionedFolderId)
        if (!right) return notFound(params.otherVersionedFolderId)

        respond versionedFolderService.findCommonAncestorBetweenModels(left, right)
    }

    def simpleModelVersionTree() {
        VersionedFolder instance = queryForResource(params.versionedFolderId)
        if (!instance) return notFound(params.versionedFolderId)

        VersionedFolder oldestAncestor = versionedFolderService.findOldestAncestor(instance) as VersionedFolder

        List<VersionTreeModel> versionTreeModelList = versionedFolderService.buildModelVersionTree(oldestAncestor, null, null, false,
                                                                                                   currentUserSecurityPolicyManager)

        respond versionTreeModelList.findAll {!it.newFork}
    }

    def diff() {
        VersionedFolder thisVersionedFolder = queryForResource params.versionedFolderId
        VersionedFolder otherVersionedFolder = queryForResource params.otherVersionedFolderId

        if (!thisVersionedFolder) return notFound(params.versionedFolderId)
        if (!otherVersionedFolder) return notFound(params.otherVersionedFolderId)

        ObjectDiff diff = versionedFolderService.getDiffForVersionedFolders(thisVersionedFolder, otherVersionedFolder)
        respond diff
    }

    def mergeDiff() {

        VersionedFolder source = queryForResource params.versionedFolderId
        if (!source) return notFound(params.versionedFolderId)

        VersionedFolder target = queryForResource params.otherVersionedFolderId
        if (!target) return notFound(params.otherVersionedFolderId)

        respond versionedFolderService.getMergeDiffForVersionedFolders(source, target)
    }

    @Transactional
    def mergeInto(MergeIntoData mergeIntoData) {
        if (!mergeIntoData.validate()) {
            respond mergeIntoData.errors
            return
        }

        if (mergeIntoData.patch.sourceId != params.versionedFolderId) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Source versioned folder id passed in request body does not match source versioned folder id in URI.')
        }
        if (mergeIntoData.patch.targetId != params.otherVersionedFolderId) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Target versioned folder id passed in request body does not match target versioned folder id in URI.')
        }

        VersionedFolder source = queryForResource params.versionedFolderId
        if (!source) return notFound(params.versionedFolderId)

        VersionedFolder target = queryForResource params.otherVersionedFolderId
        if (!target) return notFound(params.otherVersionedFolderId)

        VersionedFolder instance = versionedFolderService.mergeObjectPatchDataIntoVersionedFolder(mergeIntoData.patch, target, source, currentUserSecurityPolicyManager)

        if (!validateResource(instance, 'merge')) return

        if (mergeIntoData.deleteBranch) {
            if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(source.class, source.id)) {
                return forbiddenDueToPermissions(currentUserSecurityPolicyManager.userAvailableActions(source.class, source.id))
            }
            versionedFolderService.delete(source, true)
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

    @Override
    protected VersionedFolder queryForResource(Serializable id) {
        versionedFolderService.get(id)
    }

    @Override
    protected VersionedFolder createResource() {
        //Explicitly set the exclude map to empty so that the transient property VersionedFolder.userGroups is bound (if present) from the request
        VersionedFolder resource = super.createResource([exclude: []]) as VersionedFolder
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

    protected VersionedFolder saveResourceAndHierarchy(VersionedFolder resource) {
        versionedFolderService.saveFolderHierarchy(resource)
        saveResource(resource)
    }

    @Override
    protected VersionedFolder updateResource(VersionedFolder resource) {
        updateResource resource, resource.getDirtyPropertyNames().toSet()
    }

    protected VersionedFolder updateResource(VersionedFolder resource, Set<String> changedProperties) {
        VersionedFolder folder = super.updateResource(resource) as VersionedFolder
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(folder,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        folder
    }

    protected VersionedFolder updateResourceAndHierarchy(VersionedFolder resource, Set<String> changedProperties) {
        versionedFolderService.saveFolderHierarchy(resource)
        updateResource(resource, changedProperties)
    }

    @Override
    protected List<VersionedFolder> listAllReadableResources(Map params) {
        if (params.folderId) {
            return versionedFolderService.findAllByParentId(params.folderId, params)
        }
        versionedFolderService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(VersionedFolder resource) {
        throw new ApiNotYetImplementedException('MC01', 'serviceDeleteResource')
    }

    @Override
    @Transactional
    protected boolean validateResource(VersionedFolder instance, String view) {
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        if (view == 'update') versionedFolderService.shallowValidate(instance)
        else versionedFolderService.validate(instance)
        if (instance.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }

    protected VersionedFolder updateSecurity(VersionedFolder instance, Set<String> changedProperties) {
        modelServices.each {service ->
            Collection<Model> modelsInFolder = service.findAllByFolderId(instance.id)
            modelsInFolder.each {model ->
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
