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

import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer.parameter.FolderImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType

import org.springframework.beans.factory.annotation.Autowired

abstract class FolderImporterProviderService<P extends FolderImporterProviderServiceParameters> extends ContainerImporterProviderService<Folder, P> {

    @Autowired
    AuthorityService authorityService

    @Autowired
    FolderService folderService

    @Override
    FolderService getContainerService() {
        folderService
    }

    @Override
    String getProviderType() {
        "Folder${ProviderType.IMPORTER.name}"
    }

    Folder checkImport(Folder folder) {
        folder.tap {
            createdBy = authorityService.defaultAuthority.createdBy
        }
    }
}
