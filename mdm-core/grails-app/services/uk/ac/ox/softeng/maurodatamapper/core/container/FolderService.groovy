/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.api.exception.ApiDiffException
import uk.ac.ox.softeng.maurodatamapper.core.diff.CachedDiffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.CopyPassType
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayDiff

@Transactional
@Slf4j
class FolderService extends ContainerService<Folder> {

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService
    EditService editService
    MessageSource messageSource

    @Override
    boolean isContainerVirtual() {
        false
    }

    @Override
    String getContainerPropertyNameInModel() {
        'folder'
    }

    @Override
    List<Folder> getAll(Collection<UUID> containerIds) {
        Folder.getAll(containerIds).findAll().collect {unwrapIfProxy(it)}
    }

    @Override
    List<Folder> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm) {
        log.debug('Searching readable folders for search term in label')
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)
        Folder.treeLabelHibernateSearch(readableIds.collect { it.toString() }, searchTerm)
    }

    @Override
    List<Folder> findAllContainersInside(PathNode pathNode) {
        Folder.findAllContainedInFolderPathNode(pathNode)
    }

    @Override
    Folder findDomainByLabel(String label) {
        Folder.byNoParentFolder().eq('label', label).get()
    }

    @Override
    Folder findByParentIdAndLabel(UUID parentId, String label) {
        Folder.byParentFolderIdAndLabel(parentId, label.trim()).get()
    }

    @Override
    List<Folder> findAllByParentId(UUID parentId, Map pagination = [:]) {
        Folder.byParentFolderId(parentId).list(pagination)
    }

    @Override
    DetachedCriteria<Folder> getCriteriaByParent(Folder folder) {
        if (folder.parentFolder) return Folder.byParentFolderId(folder.parentFolder.id)
        return Folder.byNoParentFolder()
    }

    @Override
    List<Folder> findAllReadableByEveryone() {
        Folder.findAllByReadableByEveryone(true)
    }

    @Override
    List<Folder> findAllReadableByAuthenticatedUsers() {
        Folder.findAllByReadableByAuthenticatedUsers(true)
    }

    Folder get(Serializable id) {
        if (Utils.toUuid(id)) return Folder.get(id)
        if (id instanceof String) return findByPath(id)
        null
    }

    @Override
    List<Folder> list(Map pagination) {
        Folder.list(pagination)
    }

    @Override
    List<Folder> list() {
        Folder.list().collect { unwrapIfProxy(it) }
    }

    Long count() {
        Folder.count()
    }

    boolean exists(Serializable id) {
        Folder.countById(Utils.toUuid(id))
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(Folder folder) {
        folder?.deleted = true
    }

    void delete(Folder folder, boolean permanent, boolean flush = true) {
        if (!folder) {
            log.warn('Attempted to delete Folder which doesnt exist')
            return
        }
        if (permanent) {
            folder.childFolders.each {delete(it, permanent, false)}
            modelServices.each {it.deleteAllInContainer(folder)}
            folder.trackChanges()
            folder.delete(flush: flush)
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(folder, null)
            }
        } else {
            folder.childFolders.each {delete(it)}
            delete(folder)
        }
    }

    Folder validate(Folder folder) {
        folder.validate()
        folder
    }

    @Override
    Folder save(Map args, Folder folder) {
        Map saveArgs = new HashMap(args)
        if (args.flush) {
            saveArgs.remove('flush')
            (folder as GormEntity).save(saveArgs)
            sessionFactory.currentSession.flush()
        } else {
            (folder as GormEntity).save(args)
        }
        folder
    }

    Folder saveFolderHierarchy(Map args, Folder folder) {
        log.trace('Saving Folder Hierarchy')
        // Save parent folders first
        save(args ?: [validate: false, flush: false], folder)

        // Saves folders and the models but not the model contents
        folder.childFolders.each {cf ->
            saveFolderHierarchy(cf, validate: false, flush: false)
        }

        modelServices.each {service ->
            List<Model> models = service.findAllByContainerId(folder.id) as List<Model>
            models.each {m ->
                service.save(m, validate: false, flush: false)
            }
        }

        if (!args?.containsKey('flush') || args?.flush) {
            sessionFactory.currentSession.flush()
        }
        folder
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<Folder> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)
        ids ? Folder.findAllByIdInList(ids, pagination) : []
    }

    void generateDefaultFolderLabel(Folder folder) {
        generateDefaultLabel(folder, Folder.DEFAULT_FOLDER_LABEL)
    }

    @Override
    List<Folder> findAllWhereDirectParentOfModel(Model model) {
        List<Folder> folders = []
        Folder modelFolder = get(model.folder.id)
        folders << modelFolder
        folders.addAll(findAllWhereDirectParentOfContainer(modelFolder))
        folders
    }

    @Override
    List<Folder> findAllWhereDirectParentOfContainer(Folder folder) {
        List<Folder> folders = []
        if (folder.parentFolder) {
            folders << get(folder.parentFolder.id)
            folders.addAll(findAllWhereDirectParentOfContainer(folder.parentFolder))
        }
        folders
    }

    @Override
    List<Folder> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        Folder.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<Folder> findAllByMetadataNamespace(String namespace, Map pagination = [:]) {
        Folder.byMetadataNamespace(namespace).list(pagination)
    }

    List<Model> findAllModelsInFolder(Folder folder) {
        if (!modelServices) return []
        modelServices.collectMany {service ->
            service.findAllByFolderId(folder.id)
        } as List<Model>
    }

    def <T extends Folder> void loadModelsIntoFolderObjectDiff(ObjectDiff<T> diff, Folder leftHandSide, Folder rightHandSide, String context) {
        log.debug('Loading models into folder object diff for [{}] <> [{}] under context {}', leftHandSide.label, rightHandSide.label, context)
        List<Model> lhsModels = findAllModelsInFolder(leftHandSide)
        List<Model> rhsModels = findAllModelsInFolder(rightHandSide)
        log.debug('{} models on LHS <> {} models on RHS', lhsModels.size(), rhsModels.size())

        appendModelsToDiffList(diff, 'models', lhsModels, rhsModels, context)

        // Recurse into child folder diffs
        ArrayDiff<Folder> childFolderDiff = diff.diffs.find {it.fieldName == 'folders'} as ArrayDiff<Folder>

        Collection<Folder> lhsFolders = childFolderDiff.left
        Collection<Folder> rhsFolders = childFolderDiff.right
        log.debug('{} child folders on LHS <> {} child folders on RHS', lhsFolders.size(), rhsFolders.size())

        if (lhsFolders || rhsFolders) {
            log.debug('Loading child folder model content diffs')
            // Created folders wont have any need for a model diff as all models will be new
            // Deleted folders wont have any need for a model diff as all models will not exist
            lhsFolders.each {lhsFolder ->

                if (childFolderDiff.created.any {it.createdIdentifier == lhsFolder.diffIdentifier} ||
                    childFolderDiff.deleted.any {it.deletedIdentifier == lhsFolder.diffIdentifier}) {
                    return
                }

                boolean objectDiffAlreadyExists = true
                ObjectDiff<Folder> objectDiff = childFolderDiff.modified.find {it.leftIdentifier == lhsFolder.diffIdentifier}

                // If there are no diffs at the folder level then there wont be an object diff so we create an empty basic one to load the models into
                if (!objectDiff) {
                    objectDiffAlreadyExists = false
                    // There has to be a RHS otherwise the LHS would be in created or deleted in whcih case we've already returned from this loop
                    // There are no differences otherwise they'd be in the modified list so just create an empty diff with an empty array diff on the folders field
                    Folder rhsFolder = rhsFolders.find {it.diffIdentifier == lhsFolder.diffIdentifier}
                    objectDiff = DiffBuilder.objectDiff(Folder)
                        .leftHandSide(lhsFolder.id.toString(), lhsFolder)
                        .rightHandSide(rhsFolder.id.toString(), rhsFolder)
                        .append(arrayDiff(lhsFolder.childFolders.class)
                                    .fieldName('folders')
                                    .leftHandSide(lhsFolder.childFolders ?: [])
                                    .rightHandSide(rhsFolder.childFolders ?: []))
                }

                // Need to make sure the models are diffed properly as contained inside a versioned diff
                loadModelsIntoFolderObjectDiff(objectDiff.asVersionedDiff(), objectDiff.left, objectDiff.right, context)

                // If the object diff didnt exist and the one we created has diffs then make sure its added to the modified list
                if (!objectDiffAlreadyExists && objectDiff.getNumberOfDiffs()) {
                    childFolderDiff.modified << objectDiff
                }
            }
        } else {
            diff.diffs.remove(childFolderDiff)
        }
    }

    ModelService findModelServiceForModel(Model model) {
        ModelService modelService = modelServices.find {it.handles(model.class)}
        if (!modelService) throw new ApiInternalException('MSXX', "No model service to handle model [${model.domainType}]")
        modelService
    }

    Folder copyFolder(Folder original, Folder folderToCopyInto, User copier, boolean copyPermissions, String modelBranchName,
                      Version modelCopyDocVersion, boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        Folder copiedFolder = new Folder(deleted: false, parentFolder: folderToCopyInto)
        copyFolder(original, copiedFolder, original.label, copier, copyPermissions, modelBranchName, modelCopyDocVersion, throwErrors, false,
                   userSecurityPolicyManager)
    }

    Folder copyFolder(Folder original, Folder copiedFolder, String label, User copier, boolean copyPermissions, String modelBranchName,
                      Version modelCopyDocVersion, boolean throwErrors, boolean clearSession,
                      UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('Copying folder {}[{}]', original.id, original.label)
        long start = System.currentTimeMillis()
        copyFolderPass(CopyPassType.FIRST_PASS, original, copiedFolder, label, copier, copyPermissions, modelBranchName, modelCopyDocVersion,
                       throwErrors, userSecurityPolicyManager)
        sessionFactory.currentSession.flush()
        if (clearSession) sessionFactory.currentSession.clear()
        copyFolderPass(CopyPassType.SECOND_PASS, original, copiedFolder, label, copier, copyPermissions, modelBranchName, modelCopyDocVersion,
                       throwErrors, userSecurityPolicyManager)
        sessionFactory.currentSession.flush()
        if (clearSession) sessionFactory.currentSession.clear()
        copyFolderPass(CopyPassType.THIRD_PASS, original, copiedFolder, label, copier, copyPermissions, modelBranchName, modelCopyDocVersion,
                       throwErrors, userSecurityPolicyManager)
        log.debug('Folder copy complete in {}', Utils.timeTaken(start))
        get(copiedFolder.id)
    }

    Folder copyFolderPass(CopyPassType copyPassType, Folder original, Folder copiedFolder,
                          String label, User copier, boolean copyPermissions,
                          String modelBranchName,
                          Version modelCopyDocVersion, boolean throwErrors,
                          UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('{} performing copy folder pass for {}[{}]', copyPassType, original.id, original.label)
        long start = System.currentTimeMillis()
        if (copyPassType == CopyPassType.FIRST_PASS) {
            copiedFolder = copyBasicFolderInformation(original, copiedFolder, label, copier)

            if (copyPermissions) {
                if (throwErrors) {
                    throw new ApiNotYetImplementedException('MSXX', 'Folder permission copying')
                }
                log.warn('Permission copying is not yet implemented')

            }
            log.debug('Validating and saving copy')
            setFolderRefinesFolder(copiedFolder, original, copier)

            if (copiedFolder.validate()) {
                save(copiedFolder, flush: true, validate: false)
                editService.createAndSaveEdit(EditTitle.COPY, copiedFolder.id, copiedFolder.domainType,
                                              "Folder ${original.label} created as a copy of ${original.id}",
                                              copier
                )
                if (securityPolicyManagerService) {
                    userSecurityPolicyManager =
                        securityPolicyManagerService.addSecurityForSecurableResource(copiedFolder, userSecurityPolicyManager.user,
                                                                                     copiedFolder.label)
                }
            } else throw new ApiInvalidModelException('FS01', 'Copied Folder is invalid', copiedFolder.errors, messageSource)
        }
        copyFolderContents(original, copiedFolder, copier, copyPassType, copyPermissions, modelCopyDocVersion, modelBranchName, throwErrors,
                           userSecurityPolicyManager)

        log.debug('{} folder copy complete for {}[{}] in {}', copyPassType, original.id, original.label, Utils.timeTaken(start))
        copiedFolder
    }

    Folder copyBasicFolderInformation(Folder original, Folder copy, String label, User copier) {
        copy.createdBy = copier.emailAddress
        copy.label = label
        copy.description = original.description

        metadataService.findAllByMultiFacetAwareItemId(original.id).each {copy.addToMetadata(it.namespace, it.key, it.value, copier.emailAddress)}
        ruleService.findAllByMultiFacetAwareItemId(original.id).each {rule ->
            Rule copiedRule = new Rule(name: rule.name, description: rule.description, createdBy: copier.emailAddress)
            rule.ruleRepresentations.each {ruleRepresentation ->
                copiedRule.addToRuleRepresentations(language: ruleRepresentation.language,
                                                    representation: ruleRepresentation.representation,
                                                    createdBy: copier.emailAddress)
            }
            copy.addToRules(copiedRule)
        }

        semanticLinkService.findAllBySourceMultiFacetAwareItemId(original.id).each {link ->
            copy.addToSemanticLinks(createdBy: copier.emailAddress, linkType: link.linkType,
                                    targetMultiFacetAwareItemId: link.targetMultiFacetAwareItemId,
                                    targetMultiFacetAwareItemDomainType: link.targetMultiFacetAwareItemDomainType,
                                    unconfirmed: true)
        }

        copy
    }

    void copyFolderContents(Folder original, Folder folderCopy, User copier,
                            CopyPassType copyPassType,
                            boolean copyPermissions,
                            Version copyDocVersion,
                            String branchName,
                            boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {

        // If changing label then we need to prefix all the new models so the names dont introduce label conflicts as this situation arises in forking
        String labelSuffix = folderCopy.label == original.label ? '' : " (${folderCopy.label})"

        copyModelsInFolder(original, folderCopy, copier, copyPassType, labelSuffix, copyPermissions, copyDocVersion, branchName,
                           throwErrors, userSecurityPolicyManager)

        List<Folder> folders = findAllByParentId(original.id)
        log.debug('{} copying {} sub folders inside folder', copyPassType, folders.size())
        folders.each {childFolder ->
            Folder childCopy
            if (copyPassType == CopyPassType.FIRST_PASS) {
                childCopy = new Folder(parentFolder: folderCopy, deleted: false)
                folderCopy.addToChildFolders(childCopy)
            } else {
                childCopy = folderCopy.childFolders.find {it.label == childFolder.label}
                if (!childCopy) {
                    throw new ApiInternalException('FSXX', "${childCopy.label} does not exist inside ${folderCopy.label}")
                }
            }
            copyFolderPass(copyPassType, childFolder, childCopy, childFolder.label, copier, copyPermissions, branchName, copyDocVersion,
                           throwErrors, userSecurityPolicyManager)
        }
    }

    void copyModelsInFolder(Folder originalFolder, Folder copiedFolder, User copier,
                            CopyPassType copyPassType,
                            String labelSuffix,
                            boolean copyPermissions,
                            Version copyDocVersion,
                            String branchName,
                            boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        modelServices.each { service ->

            if (service.countByContainerId(originalFolder.id)) {

                List<Model> originalModels = service.findAllByContainerId(originalFolder.id) as List<Model>

                log.debug('{} copying {} {} models from original folder into copied folder', copyPassType, originalModels.size(),
                          service.getDomainClass().simpleName)

                originalModels.each { Model originalModel ->
                    Model workingModel = service.get(originalModel.id) as Model
                    switch (copyPassType) {
                        case CopyPassType.FIRST_PASS:
                            Folder workingFolder = get(copiedFolder.id)
                            // First pass copy/create all the models
                            // Any links across models will remain pointing to the original VF models
                            Model copiedModel = service.copyModel(workingModel, workingFolder, copier, copyPermissions,
                                                                  "${workingModel.label}${labelSuffix}",
                                                                  copyDocVersion, branchName, throwErrors,
                                                                  userSecurityPolicyManager)
                            log.debug('Validating and saving model copy {}', copiedModel.path)
                            service.validate(copiedModel)
                            if (copiedModel.hasErrors()) {
                                throw new ApiInvalidModelException('VFS02', 'Copied Model is invalid', copiedModel.errors, messageSource)
                            }
                            service.saveModelWithContent(copiedModel)
                            return copiedModel
                        case CopyPassType.SECOND_PASS:
                            // Second pass work through all the models and update the links across models
                            Model copiedModel = service.findByFolderIdAndLabel(copiedFolder.id, "${workingModel.label}${labelSuffix}")
                            if (!copiedModel) {
                                throw new ApiInternalException('FSXX',
                                                               "${workingModel.label}${labelSuffix} does not exist inside ${copiedFolder.label}")
                            }
                            return service.updateCopiedCrossModelLinks(copiedModel, workingModel)
                    }
                    null
                }
            }

            if (copyPassType == CopyPassType.THIRD_PASS) {
                // TODO is this necessary???
                // At the moment just make sure the session is flushed in the third pass, this makes sure all objects are the same
                service.findAllByContainerId(copiedFolder.id)
                sessionFactory.currentSession.flush()
            }
        }
    }

    void setFolderRefinesFolder(Folder source, Folder target, User catalogueUser) {
        source.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdBy: catalogueUser.emailAddress, targetMultiFacetAwareItem: target)
    }

    Path getFullPathForFolder(Folder folder) {
        if (folder.parentFolder) {
            Path parentPath = getFullPathForFolder(folder.parentFolder)
            return Path.from(parentPath, folder)
        }
        Path.from(folder)
    }

    /**
     * Custom handling for {@link uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff#appendList}.
     * Loads the models directly into the 1st level cache/memory before a diff is attempted.
     * Also clears the session after each diff to keep the cache clean and as small as possible
     *
     * @param folderDiff
     * @param fieldName
     * @param appendingLhs
     * @param appendingRhs
     * @param context
     * @param addIfEmpty
     * @return
     * @throws ApiDiffException
     */
    @SuppressFBWarnings('UPM_UNCALLED_PRIVATE_METHOD')
    private void appendModelsToDiffList(ObjectDiff folderDiff, String fieldName,
                                        Collection<Model> appendingLhs, Collection<Model> appendingRhs, String context = null) throws ApiDiffException {

        log.debug('Appending models to diff list for folder')
        ObjectDiff.validateFieldNameNotNull(fieldName)

        List<Model> diffableList = []

        // Just make sure all objects are unwrapped. Unlikely but it can happen
        Collection<Model> lhs = appendingLhs.collect {proxyHandler.unwrapIfProxy(it)} as Collection<Model>
        Collection<Model> rhs = appendingRhs.collect {proxyHandler.unwrapIfProxy(it)} as Collection<Model>

        ArrayDiff<Model> diff = arrayDiff(diffableList.class)
            .fieldName(fieldName)
            .leftHandSide(lhs ?: [])
            .rightHandSide(rhs ?: []) as ArrayDiff<Model>

        // If no lhs or rhs then nothing to compare
        if (!lhs && !rhs) {
            log.debug('No LHS or RHS so no diff')
            folderDiff
            return
        }

        // If no lhs then all rhs have been created/added
        if (!lhs) {
            log.debug('No LHS so adding RHS as created')
            folderDiff.append(diff.createdObjects(rhs))
            return
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
            log.debug('NO RHS so adding LHS as deleted')
            folderDiff.append(diff.deletedObjects(lhs))
            return
        }

        Collection<Model> deleted = []
        Collection<ObjectDiff> modified = []

        // Assume all rhs have been created new
        List<Model> created = new ArrayList<>(rhs)

        Map<String, Model> lhsMap = lhs.collectEntries {[it.getDiffIdentifier(context), it]}
        Map<String, Model> rhsMap = rhs.collectEntries {[it.getDiffIdentifier(context), it]}
        // This object diff is being performed on an object which has the concept of modelIdentifier, e.g branch name or version
        // If this is the case we want to make sure we ignore any versioning on sub contents as child versioning is controlled by the parent
        // This should only happen to models inside versioned folders, but we want to try and be more dynamic
        if (folderDiff.isVersionedDiff()) {
            log.debug('Versioned diff recollecting entries using paths')
            Path childPath = Path.from((MdmDomain) lhs.first())
            if (childPath.size() == 1 && childPath.first().modelIdentifier) {
                // child collection has versioning
                // recollect entries using the clean identifier rather than the full thing
                lhsMap = lhs.collectEntries {[Path.from(it.pathPrefix, it.getDiffIdentifier(context)).first().identifier, it]}
                rhsMap = rhs.collectEntries {[Path.from(it.pathPrefix, it.getDiffIdentifier(context)).first().identifier, it]}
            }
        }

        log.debug('Checking each LHS for diff against its RHS partner')
        // Work through each lhs object and compare to rhs object

        lhsMap.each {di, lObj ->
            Model rObj = rhsMap[di]
            if (rObj) {
                // If robj then it exists and has not been created
                created.remove(rObj)

                ModelService modelService = modelServices.find {it.handles(lObj.getClass())}
                CachedDiffable<Model> lObjCachedDiffable = modelService.loadEntireModelIntoDiffCache(lObj.id)
                CachedDiffable<Model> rObjCachedDiffable = modelService.loadEntireModelIntoDiffCache(rObj.id)

                long start = System.currentTimeMillis()
                ObjectDiff od = lObjCachedDiffable.diff(rObjCachedDiffable, context)

                sessionFactory.currentSession.clear()

                log.debug('Diff complete for [{}], session cleared, objects are identical [{}]. Took {}', di, od.objectsAreIdentical(), Utils.timeTaken(start))
                // If not equal then objects have been modified
                if (!od.objectsAreIdentical()) {
                    modified.add(od)
                }
            } else {
                log.debug('No RHS for lObj {} so adding lObj as deleted', di)
                // If no robj then object has been deleted from lhs
                deleted.add(lObj)
            }
        }

        if (created || deleted || modified) {
            log.debug('Adding diff to folder')
            folderDiff.append(diff.createdObjects(created)
                                  .deletedObjects(deleted)
                                  .withModifiedDiffs(modified))
        }
    }

    void createFolderDiffCaches(DiffCache diffCache, Map<UUID, List<Folder>> foldersMap,
                                Map<String, Map<UUID, List<Diffable>>> facetData, UUID folderId) {

        List<Folder> folders = foldersMap[folderId]
        diffCache.addField('folders', folders)

        folders.each {f ->
            DiffCache fDiffCache = createFolderDiffCache(diffCache, f, facetData)
            createFolderDiffCaches(fDiffCache, foldersMap, facetData, f.id)
            diffCache.addDiffCache(f.path, fDiffCache)
        }
    }

    DiffCache createFolderDiffCache(DiffCache parentCache, Folder folder,
                                    Map<String, Map<UUID, List<Diffable>>> facetData) {
        DiffCache fDiffCache = new DiffCache()
        addFacetDataToDiffCache(fDiffCache, facetData, folder.id)
        if (parentCache) parentCache.addDiffCache(folder.path, fDiffCache)
        fDiffCache
    }

    void checkImportedFolderAssociations(User importingUser, Folder folder) {
        folder.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingMultiFacetAware(folder)
        if (folder.childFolders) {
            folder.childFolders.each {it.parentFolder = folder}
            folder.childFolders.each {checkImportedFolderAssociations(importingUser, it)}
        }
        log.debug('Folder associations checked')
    }

    Folder addSecurity(Folder folder, User user) {
        log.debug('Adding security')
        if (securityPolicyManagerService) {
            securityPolicyManagerService.addSecurityForSecurableResource(folder, user, folder.label)
        }
        folder
    }
}
