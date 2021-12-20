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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
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
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

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
    boolean handles(Class clazz) {
        clazz == Folder
    }

    @Override
    boolean handles(String domainType) {
        domainType == Folder.simpleName
    }

    @Override
    Class<Folder> getContainerClass() {
        Folder
    }

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
        Folder.luceneTreeLabelSearch(readableIds.collect {it.toString()}, searchTerm)
    }

    @Override
    List<Folder> findAllContainersInside(UUID containerId) {
        Folder.findAllContainedInFolderId(containerId)
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
        Folder.list().collect {unwrapIfProxy(it)}
    }

    Long count() {
        Folder.count()
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
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(folder, null)
            }
            folder.trackChanges()
            folder.delete(flush: flush)
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
        // If inserting then we will need to update all the facets with the CIs "id" after insert
        // If updating then we dont need to do this as the ID has already been done
        boolean inserting = !(folder as GormEntity).ident() ?: args.insert
        Map saveArgs = new HashMap(args)
        if (args.flush) {
            saveArgs.remove('flush')
            (folder as GormEntity).save(saveArgs)
            if (inserting) updateFacetsAfterInsertingMultiFacetAware(folder)
            sessionFactory.currentSession.flush()
        } else {
            (folder as GormEntity).save(args)
            if (inserting) updateFacetsAfterInsertingMultiFacetAware(folder)
        }
        folder
    }

    Folder saveFolderHierarchy(Map args, Folder folder) {
        log.trace('Saving Folder Hierarchy')
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

        save(args ?: [validate: false, flush: true], folder)
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

    @Deprecated
    Folder findFolder(String label) {
        findDomainByLabel(label)
    }

    @Deprecated
    Folder findFolder(Folder parentFolder, String label) {
        findByParentIdAndLabel(parentFolder.id, label)
    }

    @Deprecated
    Folder findByFolderPath(String folderPath) {
        findByPath(folderPath)
    }

    @Deprecated
    Folder findByFolderPath(List<String> pathLabels) {
        findByPath(pathLabels)
    }

    @Deprecated
    Folder findByFolderPath(Folder parentFolder, List<String> pathLabels) {
        findByPath(parentFolder, pathLabels)
    }

    @Deprecated
    List<Folder> findAllByParentFolderId(UUID parentFolderId, Map pagination = [:]) {
        findAllByParentId(parentFolderId, pagination)
    }

    @Deprecated
    List<Folder> getFullPathFolders(Folder folder) {
        getFullPathDomains(folder)
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
        log.debug("{} models on LHS <> {} models on RHS", lhsModels.size(), rhsModels.size())
        diff.appendList(Model, 'models', lhsModels, rhsModels, context)

        // Recurse into child folder diffs
        ArrayDiff<Folder> childFolderDiff = diff.diffs.find {it.fieldName == 'folders'} as ArrayDiff<Folder>

        Collection<Folder> lhsFolders = childFolderDiff.left
        Collection<Folder> rhsFolders = childFolderDiff.right
        log.debug("{} child folders on LHS <> {} child folders on RHS", lhsFolders.size(), rhsFolders.size())

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
        copyFolder(original, copiedFolder, original.label, copier, copyPermissions, modelBranchName, modelCopyDocVersion, throwErrors,
                   userSecurityPolicyManager)
    }

    Folder copyFolder(Folder original, Folder copiedFolder, String label, User copier, boolean copyPermissions, String modelBranchName,
                      Version modelCopyDocVersion, boolean throwErrors,
                      UserSecurityPolicyManager userSecurityPolicyManager) {
        log.debug('Copying folder {}[{}]', original.id, original.label)
        copyFolderPass(CopyPassType.FIRST_PASS, original, copiedFolder, label, copier, copyPermissions, modelBranchName, modelCopyDocVersion,
                       throwErrors, userSecurityPolicyManager)
        copyFolderPass(CopyPassType.SECOND_PASS, original, copiedFolder, label, copier, copyPermissions, modelBranchName, modelCopyDocVersion,
                       throwErrors, userSecurityPolicyManager)
        copyFolderPass(CopyPassType.THIRD_PASS, original, copiedFolder, label, copier, copyPermissions, modelBranchName, modelCopyDocVersion,
                       throwErrors, userSecurityPolicyManager)
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

        log.debug('{} folder copy complete in {}', copyPassType, Utils.timeTaken(start))
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

        log.debug('{} copying models from original folder into copied folder', copyPassType)
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
        modelServices.each {service ->
            List<Model> originalModels = service.findAllByContainerId(originalFolder.id) as List<Model>

            originalModels.each {Model originalModel ->
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
                        log.debug('Validating and saving model copy')
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
                            throw new ApiInternalException('FSXX', "${workingModel.label}${labelSuffix} does not exist inside ${copiedFolder.label}")
                        }
                        return service.updateCopiedCrossModelLinks(copiedModel, workingModel)
                }
                null
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

}
