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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.VersionLinkAwareService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormValidateable
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Transactional
@Slf4j
class VersionedFolderService extends ContainerService<VersionedFolder> implements VersionLinkAwareService<VersionedFolder> {

    protected static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    FolderService folderService
    EditService editService
    VersionLinkService versionLinkService
    MessageSource messageSource
    AuthorityService authorityService

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    boolean handles(Class clazz) {
        clazz == VersionedFolder
    }

    @Override
    boolean handles(String domainType) {
        domainType == VersionedFolder.simpleName
    }

    @Override
    Class<Folder> getContainerClass() {
        VersionedFolder
    }

    Class<VersionedFolder> getVersionLinkAwareClass() {
        VersionedFolder
    }

    @Override
    boolean isContainerVirtual() {
        folderService.isContainerVirtual()
    }

    @Override
    String getContainerPropertyNameInModel() {
        folderService.getContainerPropertyNameInModel()
    }

    @Override
    List<Folder> getAll(Collection<UUID> containerIds) {
        VersionedFolder.getAll(containerIds)
    }

    @Override
    List<VersionedFolder> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm) {
        log.debug('Searching readable folders for search term in label')
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)
        VersionedFolder.luceneTreeLabelSearch(readableIds.collect {it.toString()}, searchTerm)
    }

    @Override
    List<Container> findAllContainersInside(UUID containerId) {
        folderService.findAllContainersInside(containerId)
    }

    @Override
    VersionedFolder findDomainByLabel(String label) {
        VersionedFolder.byNoParentFolder().eq('label', label).get()
    }

    @Override
    VersionedFolder findDomainByParentIdAndLabel(UUID parentId, String label) {
        VersionedFolder.byParentFolderIdAndLabel(parentId, label.trim()).get()
    }

    @Override
    List<VersionedFolder> findAllByParentId(UUID parentId, Map pagination = [:]) {
        VersionedFolder.byParentFolderId(parentId).list(pagination)
    }

    @Override
    DetachedCriteria<VersionedFolder> getCriteriaByParent(VersionedFolder folder) {
        if (folder.parentFolder) return VersionedFolder.byParentFolderId(folder.parentFolder.id)
        return VersionedFolder.byNoParentFolder()
    }

    @Override
    List<VersionedFolder> findAllReadableByEveryone() {
        VersionedFolder.findAllByReadableByEveryone(true)
    }

    @Override
    List<VersionedFolder> findAllReadableByAuthenticatedUsers() {
        VersionedFolder.findAllByReadableByAuthenticatedUsers(true)
    }

    VersionedFolder finaliseFolder(VersionedFolder folder, User user, Version folderVersion, VersionChangeType versionChangeType,
                                   String versionTag) {
        log.debug('Finalising folder')
        long start = System.currentTimeMillis()

        folder.finalised = true
        folder.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

        folder.modelVersion = getNextFolderVersion(folder, folderVersion, versionChangeType)

        folder.modelVersionTag = versionTag

        // Recurse through contents to finalise everything
        finaliseFolderContents(folder, user, folder.modelVersion, folder.modelVersionTag)

        folder.addToAnnotations(createdBy: user.emailAddress, label: 'Finalised Versioned Folder',
                                description: "${folder.label} finalised by ${user.firstName} ${user.lastName} on " +
                                             "${OffsetDateTimeConverter.toString(folder.dateFinalised)}")

        editService.createAndSaveEdit(EditTitle.FINALISE, folder.id, folder.domainType,
                                      "${folder.label} finalised by ${user.firstName} ${user.lastName} on " +
                                      "${OffsetDateTimeConverter.toString(folder.dateFinalised)}",
                                      user)
        log.debug('Folder finalised took {}', Utils.timeTaken(start))
        folder
    }


    void finaliseFolderContents(Folder folder, User user, Version folderVersion, String folderVersionTag) {
        log.debug('Recusing into folder and finalising it and its contents')
        long start = System.currentTimeMillis()

        log.debug('Finalising models inside folder')
        modelServices.each {service ->
            Collection<Model> modelsInFolder = service.findAllByFolderId(folder.id)
            modelsInFolder.each {model ->
                service.finaliseModel(model as Model, user, folderVersion, null, folderVersionTag)
            }
        }

        List<Folder> folders = findAllByParentId(folder.id)
        log.debug('Finalising {} sub folders inside folder', folders.size())
        folders.each {childFolder ->
            finaliseFolderContents(childFolder, user, folderVersion, folderVersionTag)
        }

        log.debug('Folder contents finalisation took {}', Utils.timeTaken(start))
        folder
    }

    Version getParentModelVersion(VersionedFolder currentFolder) {
        VersionLink versionLink = versionLinkService.findBySourceModelIdAndLinkType(currentFolder.id, VersionLinkType.NEW_MODEL_VERSION_OF)
        if (!versionLink) return null
        VersionedFolder parent = get(versionLink.targetModelId)
        parent.modelVersion
    }

    Version getNextFolderVersion(VersionedFolder folder, Version requestedFolderVersion, VersionChangeType requestedVersionChangeType) {
        if (requestedFolderVersion) {
            // Prefer requested folder version
            return requestedFolderVersion
        }

        // We need to get the parent model version first so we can work out what to increment
        Version parentModelVersion = getParentModelVersion(folder)

        if (!parentModelVersion) {
            // No parent model then set the current version to 0 to allow the first finalisation to be defined using the versionChangeType
            parentModelVersion = Version.from('0.0.0')
        }

        if (requestedVersionChangeType) {
            // Someone requests a type change
            // Increment the parent version by that amount
            switch (requestedVersionChangeType) {
                case VersionChangeType.MAJOR:
                    return Version.nextMajorVersion(parentModelVersion)
                    break
                case VersionChangeType.MINOR:
                    return Version.nextMinorVersion(parentModelVersion)
                    break
                case VersionChangeType.PATCH:
                    return Version.nextPatchVersion(parentModelVersion)
                    break
            }
        }
        // If no requested version change type then just increment by the next major version
        Version.nextMajorVersion(parentModelVersion)
    }

    @Override
    List<Folder> findAllWhereDirectParentOfModel(Model model) {
        folderService.findAllWhereDirectParentOfModel(model)
    }

    @Override
    List<Folder> findAllWhereDirectParentOfContainer(VersionedFolder folder) {
        folderService.findAllWhereDirectParentOfContainer(folder)
    }

    @Override
    List<VersionedFolder> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        VersionedFolder.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<VersionedFolder> findAllByMetadataNamespace(String namespace, Map pagination = [:]) {
        VersionedFolder.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    List<VersionedFolder> findAllModelsByIdInList(List<UUID> ids, Map pagination) {
        if (!ids) return []
        VersionedFolder.byIdInList(ids).list(pagination) as List<VersionedFolder>
    }

    @Override
    List<UUID> getAllModelIds() {
        VersionedFolder.by().id().list() as List<UUID>
    }

    VersionedFolder get(Serializable id) {
        if (Utils.toUuid(id)) return VersionedFolder.get(id)
        if (id instanceof String) return findByPath(id)
        null
    }

    List<VersionedFolder> list(Map pagination = [:]) {
        VersionedFolder.list(pagination)
    }

    Long count() {
        VersionedFolder.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(VersionedFolder folder) {
        folderService.delete(folder)
    }

    void delete(VersionedFolder folder, boolean permanent, boolean flush = true) {
        folderService.delete(folder, permanent, flush)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<VersionedFolder> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(VersionedFolder)
        ids ? VersionedFolder.withFilter(VersionedFolder.byIdInList(ids), pagination).list(pagination) : []
    }

    void generateDefaultFolderLabel(VersionedFolder folder) {
        generateDefaultLabel(folder, Folder.DEFAULT_FOLDER_LABEL)
    }

    VersionedFolder createNewForkModel(String label, VersionedFolder folder, User user, boolean copyPermissions,
                                       UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        if (!newVersionCreationIsAllowed(folder)) return folder

        VersionedFolder newForkModel = copyModelAsNewForkModel(folder, user, copyPermissions, label,
                                                               additionalArguments.throwErrors as boolean,
                                                               userSecurityPolicyManager)
        setFolderIsNewForkModelOfFolder(newForkModel, folder, user)
        newForkModel
    }

    VersionedFolder createNewBranchModelVersion(String branchName, VersionedFolder folder, User user, boolean copyPermissions,
                                                UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        if (!newVersionCreationIsAllowed(folder)) return folder
        log.info('Creating a new branch model version of {}', folder.id)
        // Check if the branch name is already being used
        if (countAllByLabelAndBranchNameAndNotFinalised(folder.label, branchName) > 0) {
            (folder as GormValidateable).errors.reject('version.aware.label.branch.name.already.exists',
                                                       ['branchName', VersionedFolder, branchName, folder.label] as Object[],
                                                       'Property [{0}] of class [{1}] with value [{2}] already exists for label [{3}]')
            return folder
        }

        // We know at this point the versioned folder is finalised which means its branch name == main so we need to check no unfinalised main
        // branch exists
        boolean draftFolderOnMainBranchForLabel =
            countAllByLabelAndBranchNameAndNotFinalised(folder.label, VersionAwareConstraints.DEFAULT_BRANCH_NAME) > 0

        if (!draftFolderOnMainBranchForLabel) {
            log.info('Creating a new branch model version of {} with name {}', folder.id, VersionAwareConstraints.DEFAULT_BRANCH_NAME)
            VersionedFolder newMainBranchModelVersion = copyFolderAsNewBranchFolder(
                folder,
                user,
                copyPermissions,
                folder.label,
                VersionAwareConstraints.DEFAULT_BRANCH_NAME,
                additionalArguments.throwErrors as boolean,
                userSecurityPolicyManager)
            setFolderIsNewBranchModelVersionOfFolder(newMainBranchModelVersion, folder, user)

            // If the branch name isn't main and the main branch doesnt exist then we need to validate and save it
            // otherwise return the new folder
            if (branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME) {
                return newMainBranchModelVersion
            } else {
                if ((newMainBranchModelVersion as GormValidateable).validate()) {
                    save(newMainBranchModelVersion, validate: false)
                    if (securityPolicyManagerService) {
                        userSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(newMainBranchModelVersion, user,
                                                                                                                 newMainBranchModelVersion.label)
                    }
                } else throw new ApiInvalidModelException('VFSXX', 'Copied (newMainBranchModelVersion) Folder is invalid',
                                                          (newMainBranchModelVersion as GormValidateable).errors, messageSource)
            }
        }
        log.info('Creating a new branch model version of {} with name {}', folder.id, branchName)
        VersionedFolder newBranchModelVersion = copyFolderAsNewBranchFolder(
            folder,
            user,
            copyPermissions,
            folder.label,
            branchName,
            additionalArguments.throwErrors as boolean,
            userSecurityPolicyManager)

        setFolderIsNewBranchModelVersionOfFolder(newBranchModelVersion, folder, user)

        newBranchModelVersion
    }

    VersionedFolder createNewDocumentationVersion(VersionedFolder folder, User user, boolean copyPermissions,
                                                  UserSecurityPolicyManager userSecurityPolicyManager,
                                                  Map<String, Object> additionalArguments = [:]) {
        if (!newVersionCreationIsAllowed(folder)) return folder

        VersionedFolder newDocVersion = copyFolderAsNewDocumentationModel(folder,
                                                                          user,
                                                                          copyPermissions,
                                                                          folder.label,
                                                                          Version.nextMajorVersion(folder.documentationVersion),
                                                                          folder.branchName,
                                                                          additionalArguments.throwErrors as boolean,
                                                                          userSecurityPolicyManager,)
        setModelIsNewDocumentationVersionOfModel(newDocVersion, folder, user)
        newDocVersion
    }

    boolean newVersionCreationIsAllowed(VersionedFolder folder) {
        if (!folder.finalised) {
            (folder as GormValidateable).errors.reject('invalid.version.aware.new.version.not.finalised.message',
                                                       [folder.domainType, folder.label, folder.id] as Object[],
                                                       '{0} [{1}({2})] cannot have a new version as it is not finalised')
            return false
        }
        VersionedFolder superseding = findModelDocumentationSuperseding(folder)
        if (superseding) {
            (folder as GormValidateable).errors.reject('invalid.version.aware.new.version.superseded.message',
                                                       [folder.domainType, folder.label, folder.id, superseding.label, superseding.id] as Object[],
                                                       '{0} [{1}({2})] cannot have a new version as it has been superseded by [{3}({4})]')
            return false
        }
        true
    }

    VersionedFolder findModelDocumentationSuperseding(VersionedFolder versionedFolder) {
        VersionLink link = versionLinkService.findLatestLinkDocumentationSupersedingModelId(versionedFolder.domainType, versionedFolder.id)
        if (!link) return null
        link.multiFacetAwareItemId == versionedFolder.id ? get(link.targetModelId) : get(link.multiFacetAwareItemId)
    }

    int countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        VersionedFolder.countByLabelAndBranchNameAndFinalised(label, branchName, false)
    }

    VersionedFolder copyFolderAsNewBranchFolder(VersionedFolder original, User copier, boolean copyPermissions, String label, String branchName,
                                                boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyFolder(original, copier, copyPermissions, label, Version.from('1'), branchName, throwErrors, userSecurityPolicyManager)
    }

    VersionedFolder copyModelAsNewForkModel(VersionedFolder original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                                            UserSecurityPolicyManager userSecurityPolicyManager) {
        copyFolder(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager)
    }

    VersionedFolder copyFolderAsNewDocumentationModel(VersionedFolder original, User copier, boolean copyPermissions, String label,
                                                      Version copyDocVersion, String branchName, boolean throwErrors,
                                                      UserSecurityPolicyManager userSecurityPolicyManager) {
        copyFolder(original, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager)
    }

    VersionedFolder copyFolder(VersionedFolder original, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                               String branchName,
                               boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('Copying folder {}', original.id)
        Folder parentFolder = original.parentFolder ? proxyHandler.unwrapIfProxy(original.parentFolder) as Folder : null
        VersionedFolder folderCopy = new VersionedFolder(finalised: false, deleted: false,
                                                         documentationVersion: copyDocVersion,
                                                         parentFolder: parentFolder,
                                                         branchName: branchName)
        folderCopy = copyBasicFolderInformation(original, folderCopy, copier)
        folderCopy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('VFSXX', 'VersionedFolder permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setFolderRefinesFolder(folderCopy, original, copier)

        log.debug('Validating and saving copy')
        if (folderCopy.validate()) {
            save(folderCopy, validate: false)
            editService.createAndSaveEdit(EditTitle.COPY, folderCopy.id, folderCopy.domainType,
                                          "VersionedFolder ${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('VFS01', 'Copied VersionedFolder is invalid', folderCopy.errors, messageSource)

        folderCopy.trackChanges()

        copyFolderContents(original, copier, folderCopy, copyPermissions, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager)

        log.debug('Folder copy complete')
        folderCopy
    }

    VersionedFolder copyBasicFolderInformation(VersionedFolder original, VersionedFolder copy, User copier) {
        copy = folderService.copyBasicFolderInformation(original, copy, copier) as VersionedFolder
        copy.authority = authorityService.defaultAuthority
        copy
    }

    void copyFolderContents(Folder original, User copier, Folder folderCopy,
                            boolean copyPermissions,
                            Version copyDocVersion,
                            String branchName,
                            boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {

        // If changing label then we need to prefix all the new models so the names dont introduce label conflicts as this situation arises in forking
        String labelSuffix = folderCopy.label == original.label ? '' : " (${folderCopy.label})"

        log.debug('Copying models from original folder into copied folder')
        modelServices.each {service ->
            List<Model> originalModels = service.findAllByContainerId(original.id) as List<Model>
            List<Model> copiedModels = originalModels.collect {Model model ->

                service.copyModel(model, folderCopy, copier, copyPermissions,
                                  "${model.label}${labelSuffix}",
                                  copyDocVersion, branchName, throwErrors,
                                  userSecurityPolicyManager)
            }
            // We can't save until after all copied as the save clears the sessions
            copiedModels.each {copy ->
                log.debug('Validating and saving model copy')
                service.validate(copy)
                if (copy.hasErrors()) {
                    throw new ApiInvalidModelException('VFS02', 'Copied Model is invalid', copy.errors, messageSource)
                }
                service.saveModelWithContent(copy)
            }
        }

        List<Folder> folders = findAllByParentId(original.id)
        log.debug('Copying {} sub folders inside folder', folders.size())
        folders.each {childFolder ->
            Folder childCopy = new Folder(parentFolder: folderCopy, deleted: false)
            childCopy = folderService.copyBasicFolderInformation(childFolder, childCopy, copier)
            folderService.validate(childCopy)
            if (childCopy.hasErrors()) {
                throw new ApiInvalidModelException('VFS02', 'Copied Folder is invalid', childCopy.errors, messageSource)
            }
            folderService.save(flush: false, validate: false, childCopy)
            copyFolderContents(childFolder, copier, childCopy, copyPermissions, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager)
        }
    }

    void setFolderIsNewBranchModelVersionOfFolder(VersionedFolder newVersionedFolder, VersionedFolder oldVersionedFolder, User catalogueUser) {
        newVersionedFolder.addToVersionLinks(
            linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldVersionedFolder
        )
    }

    void setFolderIsNewForkModelOfFolder(VersionedFolder newFolder, VersionedFolder oldFolder, User catalogueUser) {
        newFolder.addToVersionLinks(
            linkType: VersionLinkType.NEW_FORK_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldFolder
        )
    }

    void setModelIsNewDocumentationVersionOfModel(VersionedFolder newFolder, VersionedFolder oldFolder, User catalogueUser) {
        newFolder.addToVersionLinks(
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldFolder
        )
    }

    void setFolderRefinesFolder(VersionedFolder source, VersionedFolder target, User catalogueUser) {
        source.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdBy: catalogueUser.emailAddress, targetMultiFacetAwareItem: target)
    }

    VersionedFolder findLatestFinalisedModelByLabel(String label) {
        VersionedFolder.byLabelAndBranchNameAndFinalisedAndLatestModelVersion(label, VersionAwareConstraints.DEFAULT_BRANCH_NAME).get()
    }

    Version getLatestModelVersionByLabel(String label) {
        findLatestFinalisedModelByLabel(label)?.modelVersion ?: Version.from('0.0.0')
    }

    List<VersionedFolder> findAllAvailableBranchesByLabel(String label) {
        VersionedFolder.byLabelAndNotFinalised(label).list() as List<VersionedFolder>
    }

    VersionedFolder findOldestAncestor(VersionedFolder versionedFolder) {
        // Look for model version or doc version only
        VersionLink versionLink = versionLinkService.findBySourceModelIdAndLinkType(versionedFolder.id, VersionLinkType.NEW_MODEL_VERSION_OF)
        versionLink = versionLink ?: versionLinkService.findBySourceModelIdAndLinkType(versionedFolder.id, VersionLinkType.NEW_DOCUMENTATION_VERSION_OF)

        // If no versionlink then we're at the oldest ancestor
        if (!versionLink) {
            return versionedFolder
        }
        // Check the parent for oldest ancestor
        VersionedFolder parentModel = get(versionLink.targetModelId)
        findOldestAncestor(parentModel)
    }

    List<VersionTreeModel> buildModelVersionTree(VersionedFolder instance, VersionLinkType versionLinkType,
                                                 VersionTreeModel parentVersionTreeModel, boolean includeForks,
                                                 UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!userSecurityPolicyManager.userCanReadSecuredResourceId(instance.class, instance.id)) return []

        VersionTreeModel rootVersionTreeModel = new VersionTreeModel(instance, versionLinkType, parentVersionTreeModel)
        List<VersionTreeModel> versionTreeModelList = [rootVersionTreeModel]

        if (versionLinkType == VersionLinkType.NEW_FORK_OF) return includeForks ? versionTreeModelList : []

        List<VersionLink> versionLinks = versionLinkService.findAllByTargetModelId(instance.id)

        versionLinks.each {link ->
            VersionedFolder linkedModel = get(link.multiFacetAwareItemId)
            versionTreeModelList.
                addAll(buildModelVersionTree(linkedModel, link.linkType, rootVersionTreeModel, includeForks, userSecurityPolicyManager))
        }
        versionTreeModelList.sort()
    }

    VersionedFolder findCommonAncestorBetweenModels(VersionedFolder leftModel, VersionedFolder rightModel) {

        if (leftModel.label != rightModel.label) {
            throw new ApiBadRequestException('VFS03', "VersionedFolder [${leftModel.id}] does not share its label with [${rightModel.id}] therefore they cannot have a " +
                                                      "common ancestor")
        }

        VersionedFolder finalisedLeftParent = getFinalisedParent(leftModel)
        VersionedFolder finalisedRightParent = getFinalisedParent(rightModel)

        if (!finalisedLeftParent) {
            throw new ApiBadRequestException('VFS01', "VersionedFolder [${leftModel.id}] has no finalised parent therefore cannot have a " +
                                                      "common ancestor with VersionedFolder [${rightModel.id}]")
        }

        if (!finalisedRightParent) {
            throw new ApiBadRequestException('VFS02', "VersionedFolder [${rightModel.id}] has no finalised parent therefore cannot have a " +
                                                      "common ancestor with VersionedFolder [${leftModel.id}]")
        }

        // Choose the finalised parent with the lowest model version
        finalisedLeftParent.modelVersion < finalisedRightParent.modelVersion ? finalisedLeftParent : finalisedRightParent
    }

    VersionedFolder getFinalisedParent(VersionedFolder versionedFolder) {
        if (versionedFolder.finalised) return versionedFolder
        get(VersionLinkService.findBySourceModelAndLinkType(versionedFolder, VersionLinkType.NEW_MODEL_VERSION_OF)?.targetModelId)
    }

    VersionedFolder findCurrentMainBranchForModel(VersionedFolder versionedFolder) {
        findCurrentMainBranchByLabel(versionedFolder.label)
    }

    VersionedFolder findCurrentMainBranchByLabel(String label) {
        VersionedFolder.byLabelAndBranchNameAndNotFinalised(label, VersionAwareConstraints.DEFAULT_BRANCH_NAME).get()
    }

    boolean hasVersionedFolderParent(Folder folder) {
        if (!folder.parentFolder) return false
        if (folder.parentFolder.instanceOf(VersionedFolder)) return true
        hasVersionedFolderParent(folder.parentFolder)
    }

    boolean doesMovePlaceVersionedFolderInsideVersionedFolder(Folder folderBeingMoved, Folder folderToMoveTo) {
        // Check up the tree
        if (isVersionedFolderFamily(folderBeingMoved) && isVersionedFolderFamily(folderToMoveTo)) return true
        if (isVersionedFolderFamily(folderToMoveTo)) {
            // If not up the tree then slower check going down the tree of the folder being moved to ensure it doesnt contain a VF
            // Only need to do this if the folder being moved into has a VF tree
            return doesDepthTreeContainVersionedFolder(folderBeingMoved)
        }
        false
    }

    boolean doesDepthTreeContainVersionedFolder(Folder folder) {
        folder.instanceOf(VersionedFolder) || folderService.findAllByParentId(folder.id).any {doesDepthTreeContainVersionedFolder(it)}
    }

    boolean isVersionedFolderFamily(Folder folder) {
        folder.instanceOf(VersionedFolder) || hasVersionedFolderParent(folder)
    }

    boolean doesDepthTreeContainFinalisedModel(Folder folder) {
        List<Model> models = folderService.findAllModelsInFolder(folder)
        models.any {it.finalised} || findAllByParentId(folder.id).any {doesDepthTreeContainFinalisedModel(it)}
    }
}
