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

import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
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
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Transactional
@Slf4j
class VersionedFolderService extends ContainerService<VersionedFolder> implements VersionLinkAwareService<VersionedFolder> {

    FolderService folderService

    @Autowired(required = false)
    List<ModelService> modelServices

    EditService editService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    VersionLinkService versionLinkService

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

    VersionedFolder finaliseFolder(VersionedFolder folder, User user, Version folderVersion, VersionChangeType versionChangeType,
                                   String versionTag) {
        log.debug('Finalising folder')
        long start = System.currentTimeMillis()

        folder.finalised = true
        folder.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

        folder.modelVersion = getNextFolderVersion(folder, folderVersion, versionChangeType)

        folder.modelVersionTag = versionTag

        modelServices.each { service ->
            Collection<Model> modelsInFolder = service.findAllByFolderId(folder.id)
            modelsInFolder.each { model ->
                service.finaliseModel(model as Model, user, folder.modelVersion, null, folder.modelVersionTag)
            }
        }

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
