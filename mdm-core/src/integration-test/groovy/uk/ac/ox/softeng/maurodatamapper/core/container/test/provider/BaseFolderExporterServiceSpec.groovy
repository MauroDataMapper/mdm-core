/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.container.test.provider

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter.FolderExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

abstract class BaseFolderExporterServiceSpec<E extends FolderExporterProviderService> extends BaseIntegrationSpec {

    static final String NO_FOLDER_IDS_TO_EXPORT_CODE = 'CIPS01'

    @Autowired
    FolderService folderService

    @Shared
    Path resourcesPath

    @Shared
    UUID folderId

    abstract E getExporterService()

    abstract String getImportType()

    abstract void validateExportedFolder(String testName, String exportedFolder)

    @RunOnce
    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
    }

    @Override
    void setupDomainData() {
        Folder folder = new Folder(label: 'Test Folder', createdBy: admin.emailAddress)
        checkAndSave(folder)
        folderId = folder.id
    }

    String exportFolder(UUID folderId) {
        new String(exporterService.exportDomain(admin, folderId).toByteArray(), Charset.defaultCharset())
    }
}
