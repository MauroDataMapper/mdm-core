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
package uk.ac.ox.softeng.maurodatamapper.core.container.test.provider

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer.FolderImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer.parameter.FolderFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
abstract class BaseFolderImporterServiceSpec extends BaseIntegrationSpec {

    @Autowired
    FolderService folderService

    @Shared
    Path resourcesPath

    @Shared
    FolderFileImporterProviderServiceParameters basicParameters

    abstract FolderImporterProviderService getImporterService()

    abstract String getImportType()

    Object setupSpec() {
        basicParameters = new FolderFileImporterProviderServiceParameters()
    }

    @Override
    Object setup() {
        basicParameters.importFile = null
    }

    @Override
    void setupDomainData() {
    }

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    Folder importFolder(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = new FileParameter(fileContents: bytes)

        Folder imported = importerService.importDomain(admin, basicParameters) as Folder
        check(imported)
        folderService.save(imported)
        // sessionFactory.currentSession.flush()
        log.debug('Folder saved')
        folderService.get(imported.id)
    }

    Folder importFolder(String filename) {
        importFolder(loadTestFile(filename))
    }
}
