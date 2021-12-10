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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer.parameter.FolderFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
@CompileStatic
abstract class DataBindFolderImporterProviderService<P extends FolderFileImporterProviderServiceParameters> extends FolderImporterProviderService<P> {

    abstract Folder importFolder(User currentUser, byte[] content)

    abstract List<Folder> importFolders(User currentUser, byte[] content)

    @Override
    Class<P> getImporterProviderServiceParametersClass() {
        (Class<P>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindFolderImporterProviderService)
    }

    @Override
    Folder importDomain(User currentUser, FolderFileImporterProviderServiceParameters params) {
        checkImportParams(currentUser, params)
        importFolder(currentUser, params.importFile.fileContents)
    }

    @Override
    List<Folder> importDomains(User currentUser, FolderFileImporterProviderServiceParameters params) {
        checkImportParams(currentUser, params)
        importFolders(currentUser, params.importFile.fileContents)
    }

    private void checkImportParams(User currentUser, FolderFileImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import folder')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info("Importing ${params.importFile.fileName} as ${currentUser.emailAddress}")
    }
}
