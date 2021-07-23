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
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

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

    Folder copyBasicFolderInformation(Folder original, Folder copy, User copier) {
        copy.createdBy = copier.emailAddress
        copy.label = original.label
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

    List<Model> findAllModelsInFolder(Folder folder) {
        if (!modelServices) return []
        modelServices.collectMany {service ->
            service.findAllByFolderId(folder.id)
        } as List<Model>
    }

    def <T extends Folder> void loadModelsIntoFolderObjectDiff(ObjectDiff<T> diff, Folder leftHandSide, Folder rightHandSide) {
        List<Model> thisModels = findAllModelsInFolder(leftHandSide)
        List<Model> thatModels = findAllModelsInFolder(rightHandSide)
        diff.appendList(Model, 'models', thisModels, thatModels)

        // Recurse into child folder diffs
        ArrayDiff<Folder> childFolderDiff = diff.diffs.find {it.fieldName == 'folders'}

        if (childFolderDiff) {
            // Created folders wont have any need for a model diff as all models will be new
            // Deleted folders wont have any need for a model diff as all models will not exist
            childFolderDiff.modified.each {childDiff ->
                loadModelsIntoFolderObjectDiff(childDiff, childDiff.left, childDiff.right)
            }
        }
    }

    ModelService findModelServiceForModel(Model model) {
        ModelService modelService = modelServices.find {it.handles(model.class)}
        if (!modelService) throw new ApiInternalException('MSXX', "No model service to handle model [${model.domainType}]")
        modelService
    }

    Folder copyFolder(Folder original, Folder folderToCopyInto, User copier, boolean copyPermissions, String modelBranchName, boolean throwErrors,
                      UserSecurityPolicyManager userSecurityPolicyManager) {
        Folder copiedFolder = new Folder(deleted: false, parentFolder: folderToCopyInto, createdBy: copier, description: original.description,
                                         label: original.label)

        metadataService.findAllByMultiFacetAwareItemId(original.id).each {copiedFolder.addToMetadata(it.namespace, it.key, it.value, copier.emailAddress)}
        ruleService.findAllByMultiFacetAwareItemId(original.id).each {rule ->
            Rule copiedRule = new Rule(name: rule.name, description: rule.description, createdBy: copier.emailAddress)
            rule.ruleRepresentations.each {ruleRepresentation ->
                copiedRule.addToRuleRepresentations(language: ruleRepresentation.language,
                                                    representation: ruleRepresentation.representation,
                                                    createdBy: copier.emailAddress)
            }
            copiedFolder.addToRules(copiedRule)
        }

        semanticLinkService.findAllBySourceMultiFacetAwareItemId(original.id).each {link ->
            copiedFolder.addToSemanticLinks(createdBy: copier.emailAddress, linkType: link.linkType,
                                            targetMultiFacetAwareItemId: link.targetMultiFacetAwareItemId,
                                            targetMultiFacetAwareItemDomainType: link.targetMultiFacetAwareItemDomainType,
                                            unconfirmed: true)
        }

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('MSXX', 'Folder permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        if (copiedFolder.validate()) {
            save(copiedFolder, validate: false)
            editService.createAndSaveEdit(EditTitle.COPY, copiedFolder.id, copiedFolder.domainType,
                                          "Folder ${original.label} created as a copy of ${original.id}",
                                          copier
            )
            if (securityPolicyManagerService) {
                userSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(copiedFolder, userSecurityPolicyManager.user,
                                                                                                         copiedFolder.label)
            }
        } else throw new ApiInvalidModelException('FS01', 'Copied Folder is invalid', copiedFolder.errors, messageSource)


        List<Model> models = findAllModelsInFolder(original)
        models.each {modelToCopy ->
            ModelService modelService = findModelServiceForModel(modelToCopy)
            modelService.copyModelAndValidateAndSave(modelToCopy, copiedFolder, userSecurityPolicyManager.user, true, modelToCopy.label, modelToCopy.documentationVersion,
                                                     modelBranchName, false, userSecurityPolicyManager)
        }

        List<Folder> childFolders = findAllByParentId(original.id, [:])
        childFolders.each {childFolderToCopy ->
            copiedFolder(childFolderToCopy, copiedFolder, copier, copyPermissions, childFolderToCopy.label, modelBranchName, throwErrors, userSecurityPolicyManager)

        }

        copiedFolder
    }
}
