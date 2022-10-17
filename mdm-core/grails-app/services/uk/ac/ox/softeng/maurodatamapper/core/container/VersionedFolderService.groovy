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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.diff.CachedDiffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.MergeDiffService
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.ObjectPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.VersionLinkAwareService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.JoinTable
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.function.Predicate

@Transactional
@Slf4j
class VersionedFolderService extends ContainerService<VersionedFolder> implements VersionLinkAwareService<VersionedFolder> {

    FolderService folderService
    EditService editService
    VersionLinkService versionLinkService
    MessageSource messageSource
    AuthorityService authorityService
    PathService pathService
    MergeDiffService mergeDiffService
    AsyncJobService asyncJobService

    @Autowired(required = false)
    Set<MdmDomainService> domainServices

    @Autowired(required = false)
    Set<MultiFacetItemAwareService> multiFacetItemAwareServices

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    Set<ModelItemService> modelItemServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

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

    Set<MdmDomainService> getDomainServices() {
        domainServices.add(this)
        domainServices
    }

    Path getFullContextPathForFolder(Folder folder) {
        getFullContextPathForFolder(folder,
                                    Utils.parentClassIsAssignableFromChild(VersionedFolder, folder.class))
    }

    Path getFullContextPathForFolder(Folder folder, boolean foundVersionedFolder) {
        if (folder.parentFolder && !foundVersionedFolder) {
            Path parentPath = getFullContextPathForFolder(folder.parentFolder,
                                                          Utils.parentClassIsAssignableFromChild(VersionedFolder, folder.parentFolder.class))
            return Path.from(parentPath, folder)
        }
        Path.from(folder)
    }

    @Override
    List<Folder> getAll(Collection<UUID> containerIds) {
        VersionedFolder.getAll(containerIds).findAll().collect {unwrapIfProxy(it)}
    }

    @Override
    List<VersionedFolder> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm) {
        log.debug('Searching readable folders for search term in label')
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)
        VersionedFolder.treeLabelHibernateSearch(readableIds.collect {it.toString()}, searchTerm)
    }

    @Override
    List<Container> findAllContainersInside(PathNode pathNode) {
        folderService.findAllContainersInside(pathNode)
    }

    @Override
    VersionedFolder findDomainByLabel(String label) {
        VersionedFolder.byNoParentFolder().eq('label', label).get()
    }

    @Override
    VersionedFolder findByParentIdAndLabel(UUID parentId, String label) {
        VersionedFolder.byParentFolderIdAndLabel(parentId, label.trim()).get()
    }

    @Override
    VersionedFolder findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        String[] split = pathIdentifier.split(PathNode.ESCAPED_MODEL_PATH_IDENTIFIER_SEPARATOR)
        String label = split[0]
        boolean finalisedOnly = pathParams.finalised ? pathParams.finalised.toBoolean() : false

        // A specific identity of the model has been requested so make sure we limit to that
        if (split.size() == 2) {
            String identity = split[1]
            DetachedCriteria criteria = VersionedFolder.byParentFolderId(parentId).eq('label', split[0])

            // Try the search by modelVersion or branchName and no modelVersion
            // This will return the requested model or the latest non-finalised main branch
            if (Version.isVersionable(identity)) {
                criteria.eq('modelVersion', Version.from(identity))
            } else {
                // Need to make sure that if the main branch is requested we return the one without a modelVersion
                criteria.eq('branchName', identity)
                    .isNull('modelVersion')
            }
            return criteria.get() as VersionedFolder
        }

        // If no identity part then we can just get the latest model by the label
        finalisedOnly ? findLatestFinalisedModelByLabel(label) : findLatestModelByLabel(label)
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

        // During testing its very important that we dont disable constraints otherwise we may miss an invalid model,
        // The disabling is done to provide a speed up during saving which is not necessary during test
        if (Environment.current != Environment.TEST) {
            log.debug('Disabling database constraints')
            GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
        }

        folder.finalised = true
        folder.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

        folder.modelVersion = getNextFolderVersion(folder, folderVersion, versionChangeType)

        folder.modelVersionTag = versionTag

        // Recurse through contents to finalise everything
        finaliseFolderModels(folder, user, folder.modelVersion, folder.modelVersionTag)

        folder.addToAnnotations(createdBy: user.emailAddress, label: 'Finalised Versioned Folder',
                                description: "${folder.label} finalised by ${user.firstName} ${user.lastName} on " +
                                             "${OffsetDateTimeConverter.toString(folder.dateFinalised)}")

        editService.createAndSaveEdit(EditTitle.FINALISE, folder.id, folder.domainType,
                                      "${folder.label} finalised by ${user.firstName} ${user.lastName} on " + "${OffsetDateTimeConverter.toString(folder.dateFinalised)}",
                                      user)

        if (Environment.current != Environment.TEST) {
            log.debug('Enabling database constraints')
            GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
        }

        log.debug('Folder finalised took {}', Utils.timeTaken(start))
        folder
    }


    void finaliseFolderModels(VersionedFolder folder, User user, Version folderVersion, String folderVersionTag) {
        log.debug('Finalising models contained within folder at all levels')
        long start = System.currentTimeMillis()

        Set<UUID> foldersInside = collectAllFoldersIdsInsideFolder(folder.id)
        foldersInside.add(folder.id)
        log.debug('Found {} total folders inside (and including) VF', foldersInside.size())

        log.debug('Finalising models inside folders')
        modelServices.each {service ->
            long st = System.currentTimeMillis()
            Collection<Model> modelsInFolder = service.findAllByFolderIdInList(foldersInside)
            log.debug('Found {} {} inside VF', modelsInFolder.size(), service.getDomainClass().simpleName)
            modelsInFolder.each {model -> service.finaliseModel(model as Model, user, folderVersion, null, folderVersionTag)}
            log.debug('Finalisation of {} models took {}', modelsInFolder.size(), Utils.timeTaken(st))
        }

        log.debug('Folder contents finalisation took {}', Utils.timeTaken(start))
    }

    Set<UUID> collectAllFoldersIdsInsideFolder(UUID folderId) {
        Set<UUID> folderIds = new HashSet<>()
        List<Folder> folders = folderService.findAllByParentId(folderId)
        folderIds.addAll(folders.collect {it.id})
        folderIds.addAll(folders.collectMany {collectAllFoldersIdsInsideFolder(it.id)})
        folderIds
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

    @Override
    List<VersionedFolder> list(Map pagination) {
        VersionedFolder.list(pagination)
    }

    @Override
    List<VersionedFolder> list() {
        VersionedFolder.list().collect {unwrapIfProxy(it)}
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

        if (permanent) {
            // delete version links which point to this VF
            versionLinkService.findAllByTargetModelId(folder.id).each {
                versionLinkService.delete(it, flush)
            }
        }
    }

    @Override
    VersionedFolder save(Map args, VersionedFolder folder) {
        folderService.save(args, folder) as VersionedFolder
    }

    VersionedFolder saveFolderHierarchy(Folder folder) {
        folderService.saveFolderHierarchy([:], folder) as VersionedFolder
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

    AsyncJob asyncCreateNewForkModel(String label, VersionedFolder folder, User user, boolean copyPermissions,
                                     UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        asyncJobService.createAndSaveAsyncJob("Create new documentation model of ${folder.path}",
                                              userSecurityPolicyManager.user.emailAddress) {
            folder.attach()
            folder.authority.attach()
            VersionedFolder fork = createNewForkModel(label, folder, user, copyPermissions,
                                                      userSecurityPolicyManager, additionalArguments)
            fullValidateAndSaveOfModel(fork, user)
        }
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

    AsyncJob asyncCreateNewBranchModelVersion(String branchName, VersionedFolder folder, User user, boolean copyPermissions,
                                              UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        asyncJobService.createAndSaveAsyncJob("Branch model ${folder.path} as ${branchName}",
                                              userSecurityPolicyManager.user.emailAddress) {
            folder.attach()
            folder.authority.attach()
            VersionedFolder copy = createNewBranchModelVersion(branchName, folder, user, copyPermissions,
                                                               userSecurityPolicyManager, additionalArguments)

            fullValidateAndSaveOfModel(copy, user)
        }
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
            VersionedFolder newMainBranchModelVersion = copyFolderAsNewBranchFolder(folder,
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
        VersionedFolder newBranchModelVersion = copyFolderAsNewBranchFolder(folder,
                                                                            user,
                                                                            copyPermissions,
                                                                            folder.label,
                                                                            branchName,
                                                                            additionalArguments.throwErrors as boolean,
                                                                            userSecurityPolicyManager)

        setFolderIsNewBranchModelVersionOfFolder(newBranchModelVersion, folder, user)

        newBranchModelVersion
    }

    AsyncJob asyncCreateNewDocumentationVersion(VersionedFolder folder, User user, boolean copyPermissions,
                                                UserSecurityPolicyManager userSecurityPolicyManager, Map<String, Object> additionalArguments = [:]) {
        asyncJobService.createAndSaveAsyncJob("Create new documentation model of ${folder.path}",
                                              userSecurityPolicyManager.user.emailAddress) {
            folder.attach()
            folder.authority.attach()
            VersionedFolder doc = createNewDocumentationVersion(folder, user, copyPermissions,
                                                                userSecurityPolicyManager, additionalArguments)
            fullValidateAndSaveOfModel(doc, user)
        }
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
        copyVersionedFolder(original, copier, copyPermissions, label, Version.from('1'), branchName, throwErrors, userSecurityPolicyManager)
    }

    VersionedFolder copyModelAsNewForkModel(VersionedFolder original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                                            UserSecurityPolicyManager userSecurityPolicyManager) {
        copyVersionedFolder(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager)
    }

    VersionedFolder copyFolderAsNewDocumentationModel(VersionedFolder original, User copier, boolean copyPermissions, String label,
                                                      Version copyDocVersion, String branchName, boolean throwErrors,
                                                      UserSecurityPolicyManager userSecurityPolicyManager) {
        copyVersionedFolder(original, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager)
    }

    VersionedFolder copyVersionedFolder(VersionedFolder original, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                                        String branchName,
                                        boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('Copying folder {}', original.id)
        Folder parentFolder = original.parentFolder ? proxyHandler.unwrapIfProxy(original.parentFolder) as Folder : null
        VersionedFolder folderCopy = new VersionedFolder(finalised: false, deleted: false,
                                                         documentationVersion: copyDocVersion,
                                                         parentFolder: parentFolder,
                                                         branchName: branchName,
                                                         authority: authorityService.defaultAuthority)
        folderService.copyFolder(original, folderCopy, label, copier, copyPermissions, branchName, copyDocVersion, throwErrors, true,
                                 userSecurityPolicyManager) as VersionedFolder
    }

    void setFolderIsNewBranchModelVersionOfFolder(VersionedFolder newVersionedFolder, VersionedFolder oldVersionedFolder, User catalogueUser) {
        newVersionedFolder.addToVersionLinks(linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
                                             createdBy: catalogueUser.emailAddress,
                                             targetModel: oldVersionedFolder)
    }

    void setFolderIsNewForkModelOfFolder(VersionedFolder newFolder, VersionedFolder oldFolder, User catalogueUser) {
        newFolder.addToVersionLinks(linkType: VersionLinkType.NEW_FORK_OF,
                                    createdBy: catalogueUser.emailAddress,
                                    targetModel: oldFolder)
    }

    void setModelIsNewDocumentationVersionOfModel(VersionedFolder newFolder, VersionedFolder oldFolder, User catalogueUser) {
        newFolder.addToVersionLinks(linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
                                    createdBy: catalogueUser.emailAddress,
                                    targetModel: oldFolder)
    }

    VersionedFolder findLatestModelByLabel(String label) {
        findCurrentMainBranchByLabel(label) ?: findLatestFinalisedModelByLabel(label)
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
            throw new ApiBadRequestException('VFS03',
                                             "VersionedFolder [${leftModel.id}] does not share its label with [${rightModel.id}] therefore they " +
                                             'cannot have a common ancestor')
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

    VersionedFolder getVersionedFolderParent(Folder folder) {
        if (folder.instanceOf(VersionedFolder)) return proxyHandler.unwrapIfProxy(folder) as VersionedFolder
        getVersionedFolderParent(folder.parentFolder)
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

    ObjectDiff<VersionedFolder> getDiffForVersionedFolders(VersionedFolder thisVersionedFolder, VersionedFolder otherVersionedFolder, String contentContext = 'none') {
        log.debug('Obtaining diff for {} <> {}', thisVersionedFolder.diffIdentifier, otherVersionedFolder.diffIdentifier)

        CachedDiffable<VersionedFolder> thisCachedDiffable = loadEntireVersionedFolderIntoDiffCache(thisVersionedFolder.id)
        CachedDiffable<VersionedFolder> otherCachedDiffable = loadEntireVersionedFolderIntoDiffCache(otherVersionedFolder.id)
        getDiffForVersionedFolders(thisCachedDiffable, otherCachedDiffable, contentContext)
    }

    ObjectDiff<VersionedFolder> getDiffForVersionedFolders(CachedDiffable<VersionedFolder> thisCachedDiffable, CachedDiffable<VersionedFolder> otherCachedDiffable,
                                                           String contentContext = 'none') {
        ObjectDiff<VersionedFolder> coreDiff = thisCachedDiffable.diff(otherCachedDiffable, 'none')
        folderService.loadModelsIntoFolderObjectDiff(coreDiff, thisCachedDiffable.diffable, otherCachedDiffable.diffable, contentContext)
        coreDiff
    }

    @Transactional(readOnly = true)
    MergeDiff<VersionedFolder> getMergeDiffForVersionedFolders(VersionedFolder sourceVersionedFolder, VersionedFolder targetVersionedFolder) {
        log.debug('Generate mergediff source {} to target {}', sourceVersionedFolder.path, targetVersionedFolder.path)
        long start = System.currentTimeMillis()
        VersionedFolder commonAncestor = findCommonAncestorBetweenModels(sourceVersionedFolder, targetVersionedFolder)

        String caModelIdentifier = commonAncestor.modelVersion ?: commonAncestor.branchName
        String sourceModelIdentifier = sourceVersionedFolder.modelVersion ?: sourceVersionedFolder.branchName
        String targetModelIdentifier = targetVersionedFolder.modelVersion ?: targetVersionedFolder.branchName

        CachedDiffable<VersionedFolder> sourceCachedDiffable = loadEntireVersionedFolderIntoDiffCache(sourceVersionedFolder.id)
        CachedDiffable<VersionedFolder> targetCachedDiffable = loadEntireVersionedFolderIntoDiffCache(targetVersionedFolder.id)
        CachedDiffable<VersionedFolder> caCachedDiffable = loadEntireVersionedFolderIntoDiffCache(commonAncestor.id)

        /*
        Context is needed to allow CodeSet term comparisons
        We need to ensure that created/deleted terms are correctly identified, when performing the diffs between the below we end up with a list of all the terms from CA
        being marked as deleted and all the terms from the source/target as being created. This is due to the diffIdentifier for a Term being set from the terminology
        path. This is therefore technically correct, however its not useful for us as it incorrectly marks the terms. What we need is to identify the terms which
        have actually been created and actually been deleted, ignoring the modelIdentifier of the terminology.
        This ignoring can be done by passing in the possible modelIdentifiers to the Terms and then removing them.
        However we may have CS which looks at a terminology outside of the the VF which adds or removes Terms from another version of the same Terminology model.
        This context solution will handle that issue as only finalised Terminologies can be used for CS outside of a VF which means a comparsion of 1.0.0|source of a VF
        which uses a 1.0.0|2.0.0 external T the terms will still be correctly identified.
         */
        ObjectDiff<VersionedFolder> caDiffSource = getDiffForVersionedFolders(caCachedDiffable, sourceCachedDiffable, "${caModelIdentifier}|${sourceModelIdentifier}")
        ObjectDiff<VersionedFolder> caDiffTarget = getDiffForVersionedFolders(caCachedDiffable, targetCachedDiffable, "${caModelIdentifier}|${targetModelIdentifier}")

        removeBranchNameDiff(caDiffSource)
        removeBranchNameDiff(caDiffTarget)

        MergeDiff<VersionedFolder> mergeDiff = mergeDiffService.generateMergeDiff(DiffBuilder
                                                                                      .mergeDiff(VersionedFolder)
                                                                                      .forMergingDiffable(sourceVersionedFolder)
                                                                                      .intoDiffable(targetVersionedFolder)
                                                                                      .havingCommonAncestor(commonAncestor)
                                                                                      .withCommonAncestorDiffedAgainstSource(caDiffSource)
                                                                                      .withCommonAncestorDiffedAgainstTarget(caDiffTarget))
            .flatten()
            .clean {
                Path diffPath = it.fullyQualifiedPath
                PathNode lastNode = diffPath.last()
                // Strip out term property nodes defined inside codeset paths
                // TODO come up with an agnostic way of doing this
                lastNode.isPropertyNode() && lastNode.prefix == 'tm' && diffPath.any {it.prefix == 'cs'}
            }
        log.debug('MergeDiff completed, took {}', Utils.timeTaken(start))
        mergeDiff
    }

    void removeBranchNameDiff(ObjectDiff diff) {

        Predicate branchNamePredicate = [test: {FieldDiff fieldDiff -> fieldDiff.fieldName == 'branchName'}] as Predicate

        diff.diffs.removeIf(branchNamePredicate)

        ArrayDiff modelsDiff = diff.diffs.find {it.fieldName == 'models'}
        if (modelsDiff) {
            modelsDiff.modified.each {md -> md.diffs.removeIf(branchNamePredicate)}
        }

        ArrayDiff folderDiff = diff.diffs.find {it.fieldName == 'folders'}
        if (folderDiff) {
            folderDiff.modified.each {fd -> removeBranchNameDiff(fd)}
        }
    }

    VersionedFolder mergeObjectPatchDataIntoVersionedFolder(ObjectPatchData objectPatchData, VersionedFolder targetVersionedFolder,
                                                            VersionedFolder sourceVersionedFolder,
                                                            UserSecurityPolicyManager userSecurityPolicyManager) {


        if (!objectPatchData.hasPatches()) {
            log.debug('No patch data to merge into {}', targetVersionedFolder.id)
            return targetVersionedFolder
        }
        log.debug('Merging patch data into {}', targetVersionedFolder.id)

        getSortedFieldPatchDataForMerging(objectPatchData).each {fieldPatch ->
            // Flush and clear the session before each patch
            // This ensures all "retrieved" objects are properly loaded into the session and that all objects are stored correctly
            // This will also keep the session small so kep speed high
            sessionFactory.currentSession.flush()
            sessionFactory.currentSession.clear()
            // Load the target VF after the session has been cleared
            VersionedFolder target = get(targetVersionedFolder.id)
            switch (fieldPatch.type) {
                case 'creation':
                    return processCreationPatchIntoVersionedFolder(fieldPatch, target, get(sourceVersionedFolder.id),
                                                                   userSecurityPolicyManager)
                case 'deletion':
                    return processDeletionPatchIntoVersionedFolder(fieldPatch, target)
                case 'modification':
                    return processModificationPatchIntoVersionedFolder(fieldPatch, target)
                default:
                    log.warn('Unknown field patch type [{}]', fieldPatch.type)
            }
        }
        sessionFactory.currentSession.flush()
        get(targetVersionedFolder.id)
    }

    List<FieldPatchData> getSortedFieldPatchDataForMerging(ObjectPatchData objectPatchData) {
        /*
          We have to process modifications in after everything else incase the modifications require something to have been created
          Process creations before deletions, that way any deletions will automatically take care of any links to potentially created objects
           */
        objectPatchData.patches.sort {l, r ->
            if (l.type == r.type) return getSortResultForFieldPatchPath(l.path, r.path)
            l <=> r
        }
    }

    int getSortResultForFieldPatchPath(Path leftPath, Path rightPath) {
        // Allow each service to try sorting the patches
        // As a non-match or "care" will return 0 we can keep going or return 0
        for (ModelService service : modelServices) {
            int result = service.getSortResultForFieldPatchPath(leftPath, rightPath)
            if (result) return result
        }
        0
    }

    void processCreationPatchIntoVersionedFolder(FieldPatchData creationPatch, VersionedFolder targetVersionedFolder,
                                                 VersionedFolder sourceVersionedFolder,
                                                 UserSecurityPolicyManager userSecurityPolicyManager) {
        MdmDomain domainInTarget = pathService.findResourceByPathFromRootResource(targetVersionedFolder, creationPatch.relativePathToRoot,
                                                                                  getModelIdentifier(targetVersionedFolder))
        MdmDomain domainToCopy = domainInTarget ?: pathService.findResourceByPathFromRootResource(sourceVersionedFolder, creationPatch.path)
        if (!domainToCopy) {
            log.warn('Could not process creation patch into versioned folder at path [{}] as no such path exists in the source', creationPatch.path)
            return
        }
        log.debug('Creating [{}]', creationPatch.path.toString(getModelIdentifier(targetVersionedFolder)))
        // Potential creations are folders, models, modelItems or facets
        if (Utils.parentClassIsAssignableFromChild(Folder, domainToCopy.class)) {
            processCreationPatchOfFolder(domainToCopy as Folder, targetVersionedFolder, creationPatch.relativePathToRoot.parent,
                                         userSecurityPolicyManager)
        }
        if (Utils.parentClassIsAssignableFromChild(Model, domainToCopy.class)) {
            processCreationPatchOfModel(domainToCopy as Model, targetVersionedFolder, creationPatch.relativePathToRoot.parent,
                                        userSecurityPolicyManager)
        }
        if (Utils.parentClassIsAssignableFromChild(ModelItem, domainToCopy.class)) {
            processCreationPatchOfModelItem(domainToCopy as ModelItem, targetVersionedFolder, creationPatch.relativePathToRoot,
                                            userSecurityPolicyManager)
        }
        if (Utils.parentClassIsAssignableFromChild(MultiFacetItemAware, domainToCopy.class)) {
            processCreationPatchOfFacet(domainToCopy as MultiFacetItemAware, targetVersionedFolder, creationPatch.relativePathToRoot.parent)
        }
    }

    void processDeletionPatchIntoVersionedFolder(FieldPatchData deletionPatch, VersionedFolder targetVersionedFolder) {
        MdmDomain domain =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, deletionPatch.relativePathToRoot,
                                                           getModelIdentifier(targetVersionedFolder))
        if (!domain) {
            log.warn('Could not process deletion patch from versioned folder at path [{}] as no such path exists in the target',
                     deletionPatch.relativePathToRoot)
            return
        }
        log.debug('Deleting [{}]', deletionPatch.path.toString(getModelIdentifier(targetVersionedFolder)))

        // Potential deletions are folders, models, modelItems or facets
        if (Utils.parentClassIsAssignableFromChild(Folder, domain.class)) {
            processDeletionPatchOfFolder(domain as Folder)
        }
        if (Utils.parentClassIsAssignableFromChild(Model, domain.class)) {
            processDeletionPatchOfModel(domain as Model)
        }
        if (Utils.parentClassIsAssignableFromChild(ModelItem, domain.class)) {
            processDeletionPatchOfModelItem(domain as ModelItem, targetVersionedFolder, deletionPatch.relativePathToRoot)
        }
        if (Utils.parentClassIsAssignableFromChild(MultiFacetItemAware, domain.class)) {
            processDeletionPatchOfFacet(domain as MultiFacetItemAware, targetVersionedFolder, deletionPatch.relativePathToRoot)
        }
    }

    void processModificationPatchIntoVersionedFolder(FieldPatchData modificationPatch, VersionedFolder targetVersionedFolder) {
        MdmDomain domain =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, modificationPatch.relativePathToRoot,
                                                           getModelIdentifier(targetVersionedFolder))
        if (!domain) {
            log.warn('Could not process modification patch into model at path [{}] as no such path exists in the target',
                     modificationPatch.relativePathToRoot)
            return
        }
        String fieldName = modificationPatch.fieldName
        log.debug('Modifying [{}] in [{}]', fieldName, modificationPatch.path.toString(getModelIdentifier(targetVersionedFolder)))

        MdmDomainService domainService = getDomainServices().find {it.handles(domain.class)}
        if (!domainService) throw new ApiInternalException('MSXX', "No domain service to handle modification of [${domain.domainType}]")

        // If the domainService provides a special handler for modifying this field then use it,
        // otherwise just set the value directly
        if (!domainService.handlesModificationPatchOfField(modificationPatch, targetVersionedFolder, domain, fieldName)) {
            domain."${fieldName}" = modificationPatch.sourceValue
        }

        // Use the domain service validation to ensure proper object validation
        domainService.validate(domain)
        if (domain.hasErrors()) throw new ApiInvalidModelException('MS01', 'Modified domain is invalid', domain.errors, messageSource)
        domainService.save(domain, flush: false, validate: false)
    }

    void processDeletionPatchOfFolder(Folder folder) {
        log.debug('Deleting Folder from VersionedFolder')
        folderService.delete(folder, true, false)
    }

    void processDeletionPatchOfModel(Model model) {
        ModelService modelService = folderService.findModelServiceForModel(model)
        log.debug('Deleting Model from VersionedFolder')
        modelService.delete(model, true, false)
    }

    void processDeletionPatchOfModelItem(ModelItem modelItem, VersionedFolder targetVersionedFolder, Path relativePathToRemoveFrom) {
        Map<String, Object> modelInformation =
            findModelInformationForModelItemMergePatch(targetVersionedFolder, relativePathToRemoveFrom, modelItem.domainType)

        (modelInformation.modelService as ModelService).processDeletionPatchOfModelItem(modelItem, modelInformation.targetModel as Model, relativePathToRemoveFrom)
    }

    MultiFacetAware processDeletionPatchOfFacet(MultiFacetItemAware multiFacetItemAware, VersionedFolder targetVersionedFolder, Path path) {
        MultiFacetItemAwareService multiFacetItemAwareService = multiFacetItemAwareServices.find {it.handles(multiFacetItemAware.class)}
        if (!multiFacetItemAwareService) throw new ApiInternalException('MSXX',
                                                                        "No domain service to handle deletion of [${multiFacetItemAware.domainType}]")
        log.debug('Deleting Facet from path [{}]', path)
        multiFacetItemAwareService.delete(multiFacetItemAware, false)

        MultiFacetAware multiFacetAwareItem =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, path.getParent(),
                                                           getModelIdentifier(targetVersionedFolder)) as MultiFacetAware
        switch (multiFacetItemAware.domainType) {
            case Metadata.simpleName:
                multiFacetAwareItem.metadata.remove(multiFacetItemAware)
                break
            case Annotation.simpleName:
                multiFacetAwareItem.annotations.remove(multiFacetItemAware)
                break
            case Rule.simpleName:
                multiFacetAwareItem.rules.remove(multiFacetItemAware)
                break
            case SemanticLink.simpleName:
                multiFacetAwareItem.semanticLinks.remove(multiFacetItemAware)
                break
            case ReferenceFile.simpleName:
                multiFacetAwareItem.referenceFiles.remove(multiFacetItemAware)
                break
            case VersionLink.simpleName:
                (multiFacetAwareItem as Model).versionLinks.remove(multiFacetItemAware)
                break
        }
        multiFacetAwareItem
    }

    void processCreationPatchOfFolder(Folder folderToCopy, VersionedFolder targetVersionedFolder, Path relativeParentPathToCopyTo,
                                      UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('Creating Folder into VersionedFolder at [{}]', relativeParentPathToCopyTo)
        Folder parentFolder =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, relativeParentPathToCopyTo,
                                                           getModelIdentifier(targetVersionedFolder)) as Folder
        folderService.copyFolder(folderToCopy, parentFolder, userSecurityPolicyManager.user, true, targetVersionedFolder.branchName,
                                 targetVersionedFolder.documentationVersion, false, userSecurityPolicyManager)
    }

    void processCreationPatchOfModel(Model modelToCopy, VersionedFolder targetVersionedFolder, Path relativeParentPathToCopyTo,
                                     UserSecurityPolicyManager userSecurityPolicyManager) {
        ModelService modelService = folderService.findModelServiceForModel(modelToCopy)
        log.debug('Creating Model into VersionedFolder at [{}]', relativeParentPathToCopyTo)
        Folder parentFolder =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, relativeParentPathToCopyTo,
                                                           getModelIdentifier(targetVersionedFolder)) as Folder
        modelService.copyModelAndValidateAndSave(modelToCopy, parentFolder, userSecurityPolicyManager.user, true, modelToCopy.label,
                                                 modelToCopy.documentationVersion,
                                                 targetVersionedFolder.branchName, false, userSecurityPolicyManager)
    }

    void processCreationPatchOfModelItem(ModelItem modelItemToCopy, VersionedFolder targetVersionedFolder, Path relativePathToCopyTo,
                                         UserSecurityPolicyManager userSecurityPolicyManager) {

        Map<String, Object> modelInformation =
            findModelInformationForModelItemMergePatch(targetVersionedFolder, relativePathToCopyTo, modelItemToCopy.domainType)
        (modelInformation.modelService as ModelService).processCreationPatchOfModelItem(modelItemToCopy,
                                                                                        modelInformation.targetModel as Model,
                                                                                        (modelInformation.modelItemToModelAbsolutePath as Path),
                                                                                        userSecurityPolicyManager,
                                                                                        true)
    }

    void processCreationPatchOfFacet(MultiFacetItemAware multiFacetItemAwareToCopy, VersionedFolder targetVersionedFolder, Path parentPathToCopyTo) {
        MultiFacetItemAwareService multiFacetItemAwareService = multiFacetItemAwareServices.find {it.handles(multiFacetItemAwareToCopy.class)}
        if (!multiFacetItemAwareService) {
            throw new ApiInternalException('MSXX',
                                           "No domain service to handle creation of [${multiFacetItemAwareToCopy.domainType}]")
        }
        log.debug('Creating Facet into VersionedFolder at [{}]', parentPathToCopyTo)

        MultiFacetAware parentToCopyInto =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, parentPathToCopyTo,
                                                           getModelIdentifier(targetVersionedFolder)) as MultiFacetAware
        MultiFacetItemAware copy = multiFacetItemAwareService.copy(multiFacetItemAwareToCopy, parentToCopyInto)

        if (!copy.validate()) throw new ApiInvalidModelException('MS01', 'Copied Facet is invalid', copy.errors, messageSource)

        multiFacetItemAwareService.save(copy, flush: false, validate: false)
    }

    @Override
    PersistentEntity getPersistentEntity() {
        grailsApplication.mappingContext.getPersistentEntity(Folder.name)
    }

    @Override
    JoinTable getJoinTable(PersistentEntity persistentEntity, String facetProperty) {
        if (facetProperty == 'versionLinks') {
            PropertyConfig propertyConfig = grailsApplication
                .mappingContext
                .getPersistentEntity(VersionedFolder.name)
                .getPropertyByName(facetProperty)
                .mapping
                .mappedForm as PropertyConfig
            return propertyConfig.joinTable
        } else super.getJoinTable(persistentEntity, facetProperty)
    }

    VersionedFolder validate(VersionedFolder folder) {
        folderService.validate(folder) as VersionedFolder
    }

    VersionedFolder shallowValidate(VersionedFolder versionedFolder) {
        log.debug('Shallow validating VersionedFolder')
        long st = System.currentTimeMillis()
        versionedFolder.validate(deepValidate: false)
        log.debug('Validated VersionedFolder in {}', Utils.timeTaken(st))
        versionedFolder
    }

    private Map<String, Object> findModelInformationForModelItemMergePatch(VersionedFolder targetVersionedFolder, Path relativePathToMergeTo,
                                                                           String modelItemDomainType) {
        ModelService modelService
        Path modelItemToModelAbsolutePath
        Path modelRelativeToTargetPath

        relativePathToMergeTo.each {node ->
            if (!modelService) {
                // Build up the path to the model
                if (!modelRelativeToTargetPath) modelRelativeToTargetPath = Path.from(node)
                else modelRelativeToTargetPath.addToPathNodes(node)

                modelService = modelServices.find {s -> s.handlesPathPrefix(node.prefix)}
            }
            // Dont use else as we want to make sure the model node is added to the absolute path therefore as soon as the modelservice is found we
            // should add the node
            if (modelService) {
                // Build up the path from the model to the modelitem
                // Make sure we repoint the path to the target model so we can use the model service code to do the copy
                if (!modelItemToModelAbsolutePath) modelItemToModelAbsolutePath = Path.from(node).tap {
                    it.first().modelIdentifier = getModelIdentifier(targetVersionedFolder)
                } else {
                    modelItemToModelAbsolutePath.addToPathNodes(node)
                }
            }
        }
        if (!modelService) throw new ApiInternalException('MSXX', "No model service to handle creation of model item [${modelItemDomainType}]")

        Model targetModel =
            pathService.findResourceByPathFromRootResource(targetVersionedFolder, modelRelativeToTargetPath,
                                                           getModelIdentifier(targetVersionedFolder)) as Model

        [
            targetModel                 : targetModel,
            modelService                : modelService,
            modelItemToModelAbsolutePath: modelItemToModelAbsolutePath
        ]
    }

    static String getModelIdentifier(VersionedFolder versionedFolder) {
        Path.from(versionedFolder).first().getModelIdentifier()
    }

    @Override
    boolean isMultiFacetAwareFinalised(VersionedFolder multiFacetAwareItem) {
        multiFacetAwareItem.finalised
    }

    CachedDiffable<VersionedFolder> loadEntireVersionedFolderIntoDiffCache(UUID folderId) {
        long start = System.currentTimeMillis()
        VersionedFolder loadedFolder = get(folderId)
        log.debug('Loading entire folder [{}] into memory', loadedFolder.path)

        // Load direct content

        log.trace('Loading Folder')
        List<Folder> folders = getAllFoldersInside(loadedFolder)
        Map<UUID, List<Folder>> foldersMap = folders.groupBy {it.parentFolder.id}

        log.trace('Loading Facets')
        List<UUID> allIds = Utils.gatherIds(Collections.singleton(folderId),
                                            folders.collect {it.id})

        Map<String, Map<UUID, List<Diffable>>> facetData = loadAllDiffableFacetsIntoMemoryByIds(allIds)

        DiffCache diffCache = folderService.createFolderDiffCache(null, loadedFolder, facetData)
        folderService.createFolderDiffCaches(diffCache, foldersMap, facetData, folderId)

        log.debug('Folder loaded into memory, took {}', Utils.timeTaken(start))
        new CachedDiffable(loadedFolder, diffCache)
    }

    private List<Folder> getAllFoldersInside(Folder folder) {
        List<Folder> folders = []
        folders.addAll(folder.childFolders)
        folders.addAll(folder.childFolders.collectMany {getAllFoldersInside(it)})
        folders
    }

    VersionedFolder fullValidateAndSaveOfModel(VersionedFolder folder, User user) {
        log.debug('Full validate and save of folder')
        if (folder.hasErrors()) {
            throw new ApiInvalidModelException('MSXX', 'Invalid model', folder.errors, messageSource)
        }
        validate(folder)
        if (folder.hasErrors()) {
            throw new ApiInvalidModelException('MSXX', 'Invalid model', folder.errors, messageSource)
        }
        saveAndAddSecurity(folder, user)
    }

    VersionedFolder saveAndAddSecurity(VersionedFolder folder, User user) {
        log.debug('Saving and adding security')
        VersionedFolder savedCopy = saveFolderHierarchy(folder)
        savedCopy.addCreatedEdit(user)
        if (securityPolicyManagerService) {
            securityPolicyManagerService.addSecurityForSecurableResource(savedCopy, user, savedCopy.label)
        }
        savedCopy
    }

    List<VersionedFolder> filterAllReadableContainers(Collection<VersionedFolder> containers, boolean includeDocumentSuperseded,
                                                      boolean includeModelSuperseded, boolean includeDeleted) {
        List<UUID> ids = containers.findAll {includeDeleted ? true : !it.deleted}.collect {it.id}
        List<UUID> constrainedIds
        // The list of ids are ALL the readable ids by the user, no matter the model status
        if (includeDocumentSuperseded && includeModelSuperseded) {
            constrainedIds = ids
        } else if (includeModelSuperseded) {
            constrainedIds = findAllExcludingDocumentSupersededIds(ids)
        } else if (includeDocumentSuperseded) {
            constrainedIds = findAllExcludingModelSupersededIds(ids)
        } else {
            constrainedIds = findAllExcludingDocumentAndModelSupersededIds(ids)
        }
        if (!constrainedIds) return []

        containers.findAll {it.id in constrainedIds}
    }
}
