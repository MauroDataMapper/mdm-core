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
package uk.ac.ox.softeng.maurodatamapper.dataflow.test.provider


import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.test.BaseDataFlowIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 11/01/2021
 */
@Slf4j
abstract class BaseImportExportSpec extends BaseDataFlowIntegrationSpec {

    @Shared
    Path resourcesPath

    @Shared
    UUID dataFlowId

    abstract ImporterProviderService getDataFlowImporterService()

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getDataFlowImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up DataFlowServiceSpec unit')

        dataFlowId = dataFlow.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    void confirmDataFlow(DataFlow dataFlow) {
        assert dataFlow
        assert dataFlow.label == 'My DataFlow label'
        assert dataFlow.modelType == DataModelType.DATA_ASSET.label
        assert dataFlow.createdBy == admin.emailAddress
        assert dataFlow.breadcrumbTree
        assert dataFlow.breadcrumbTree.domainId == dataFlow.id
        assert dataFlow.breadcrumbTree.label == dataFlow.label
    }
}
