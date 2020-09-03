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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
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
class FolderService implements ContainerService<Folder> {

    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    boolean handles(Class clazz) {
        clazz == Folder
    }

    @Override
    boolean handles(String domainType) {
        domainType == Folder.simpleName
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
        Folder.getAll(containerIds)
    }

    @Override
    List<Folder> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm) {
        log.debug('Searching readable folders for search term in label')
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Folder)
        Folder.luceneTreeLabelSearch(readableIds.collect { it.toString() }, searchTerm)
    }

    @Override
    List<Folder> findAllContainersInside(UUID containerId) {
        Folder.findAllContainedInFolderId(containerId)
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
        if (id instanceof String) return findByFolderPath(id)
        null
    }

    List<Folder> list(Map pagination = [:]) {
        Folder.list(pagination)
    }

    Long count() {
        Folder.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(Folder folder) {
        folder?.deleted = true
        folder.save()
    }

    void delete(Folder folder, boolean permanent, boolean flush = true) {
        if (!folder) {
            log.warn('Attempted to delete Folder which doesnt exist')
            return
        }
        if (permanent) {
            folder.childFolders.each { delete(it, permanent, false) }
            modelServices.each { it.deleteAllInContainer(folder) }
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(folder, null)
            }
            folder.delete(flush: flush)
        } else {
            folder.childFolders.each { delete(it) }
            delete(folder)
        }
    }

    Folder findFolder(String label) {
        Folder.byNoParentFolder().eq('label', label).get()
    }

    Folder findFolder(Folder parentFolder, String label) {
        parentFolder.childFolders.find { it.label == label.trim() }
    }

    Folder findByFolderPath(String folderPath) {
        List<String> paths
        if (folderPath.contains('/')) paths = folderPath.split('/').findAll() ?: []
        else paths = folderPath.split('\\|').findAll() ?: []
        findByFolderPath(paths)
    }

    Folder findByFolderPath(List<String> pathLabels) {
        if (!pathLabels) return null
        if (pathLabels.size() == 1) {
            return findFolder(pathLabels[0])
        }

        String parentLabel = pathLabels.remove(0)
        Folder parent = findFolder(parentLabel)
        findByFolderPath(parent, pathLabels)
    }

    Folder findByFolderPath(Folder parentFolder, List<String> pathLabels) {
        if (pathLabels.size() == 1) {
            return findFolder(parentFolder, pathLabels[0])
        }

        String parentLabel = pathLabels.remove(0)
        Folder parent = findFolder(parentFolder, parentLabel)
        findByFolderPath(parent, pathLabels)
    }

    List<Folder> findAllByParentFolderId(UUID parentFolderId, Map pagination = [:]) {
        Folder.byParentFolderId(parentFolderId).list(pagination)
    }

    List<Folder> getFullPathFolders(Folder folder) {
        List<UUID> ids = folder.path.split('/').findAll().collect { Utils.toUuid(it) }
        List<Folder> folders = []
        if (ids) folders += Folder.getAll(ids)
        folders.add(folder)
        folders
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
        DetachedCriteria<Folder> criteria
        if (folder.parentFolder) criteria = Folder.byParentFolderId(folder.parentFolder.id)
        else criteria = Folder.byNoParentFolder()

        List<Folder> siblings = criteria
            .like('label', "${Folder.DEFAULT_FOLDER_LABEL}%")
            .sort('label')
            .list()

        if (!siblings) {
            folder.label = Folder.DEFAULT_FOLDER_LABEL
            return
        }

        String lastLabel = siblings.last().label
        int lastNum
        lastLabel.find(/${Folder.DEFAULT_FOLDER_LABEL}( \((\d+)\))?/) {
            lastNum = it[1] ? it[2].toInteger() : 0
        }

        folder.label = "${Folder.DEFAULT_FOLDER_LABEL} (${++lastNum})"
    }

}
