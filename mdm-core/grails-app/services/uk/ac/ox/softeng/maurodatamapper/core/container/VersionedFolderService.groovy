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

import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class VersionedFolderService extends ContainerService<VersionedFolder> {

    FolderService folderService

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
        VersionedFolder.luceneTreeLabelSearch(readableIds.collect { it.toString() }, searchTerm)
    }

    @Override
    List<VersionedFolder> findAllContainersInside(UUID containerId) {
        VersionedFolder.findAllContainedInFolderId(containerId)
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
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)
        ids ? VersionedFolder.findAllByIdInList(ids, pagination) : []
    }

    void generateDefaultFolderLabel(VersionedFolder folder) {
        generateDefaultLabel(folder, Folder.DEFAULT_FOLDER_LABEL)
    }

    VersionedFolder save(VersionedFolder folder) {
        folder.save()
    }
}
