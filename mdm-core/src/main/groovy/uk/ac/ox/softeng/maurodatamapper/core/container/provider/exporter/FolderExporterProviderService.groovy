/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class FolderExporterProviderService extends ContainerExporterProviderService<Folder> {

    @Autowired
    FolderService folderService

    abstract ByteArrayOutputStream exportFolder(User currentUser, Folder folder, Map<String, Object> parameters) throws ApiException

    abstract ByteArrayOutputStream exportFolders(User currentUser, List<Folder> folders, Map<String, Object> parameters) throws ApiException

    @Override
    FolderService getContainerService() {
        folderService
    }

    @Override
    String getDomainType() {
        'Folder'
    }

    @Override
    String getProviderType() {
        "${domainType}${ProviderType.EXPORTER.name}"
    }

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId, Map<String, Object> parameters) throws ApiException {
        exportFolder(currentUser, retrieveExportableContainers([domainId]).pop(), parameters)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds, Map<String, Object> parameters) throws ApiException {
        exportFolders(currentUser, retrieveExportableContainers(domainIds), parameters)
    }
}
