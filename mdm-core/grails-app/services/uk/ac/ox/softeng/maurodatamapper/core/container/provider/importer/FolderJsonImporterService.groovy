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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter.FolderJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer.parameter.FolderFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.JsonImportMapping
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j

@Slf4j
class FolderJsonImporterService extends DataBindFolderImporterProviderService<FolderFileImporterProviderServiceParameters> implements JsonImportMapping {

    @Override
    String getDisplayName() {
        'JSON Folder Importer'
    }

    @Override
    String getVersion() {
        '1.0'
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.equalsIgnoreCase(FolderJsonExporterService.CONTENT_TYPE)
    }

    @Override
    Folder importFolder(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import folder')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        log.debug('Parsing in file content using JsonSlurper')
        Map folder = slurpAndClean(content).folder
        if (!folder) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as folder is not present')

        log.debug('Importing Folder map')
        bindMapToFolder(currentUser, new HashMap(folder))
    }

    @Override
    List<Folder> importFolders(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${name} cannot import multiple Folders")
    }
}
