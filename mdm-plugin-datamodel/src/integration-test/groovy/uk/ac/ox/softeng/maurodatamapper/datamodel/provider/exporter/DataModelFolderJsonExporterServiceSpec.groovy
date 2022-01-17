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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter.FolderJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.BaseDataModelFolderExporterServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Assert

import java.nio.file.Files
import java.nio.file.Path

@Integration
@Rollback
@Slf4j
class DataModelFolderJsonExporterServiceSpec extends BaseDataModelFolderExporterServiceSpec<FolderJsonExporterService> implements JsonComparer {

    private static final String NO_DATAMODEL_IDS_TO_EXPORT_CODE = 'DMEP01'

    FolderJsonExporterService folderJsonExporterService

    @Override
    FolderJsonExporterService getExporterService() {
        folderJsonExporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    @Override
    Object setup() {
        setupData()
    }

    Object cleanup() {
        folderService.delete(folderService.get(folderId), true)
    }

    @Override
    void validateExportedFolder(String testName, String exportedFolder) {
        assert exportedFolder, 'There must be an exported Folder string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.${importType}")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedFolder.bytes)
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedFolder.replace(/Mauro Data Mapper/, 'Test Authority'))
    }

    void 'F01 : test export Folder with DataModels'() {
        when:
        buildSimpleDataModel(folderService.get(folderId))

        then:
        validateExportedFolder('folderIncDataModel', exportFolder(folderId))
    }
}
