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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter


import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.DataFlowJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.dataflow.test.provider.DataBindImportAndDefaultExporterServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Assert

import java.nio.file.Files
import java.nio.file.Path

/**
 * @since 11/01/2021
 */
@Integration
@Rollback
@Slf4j
class DataFlowJsonExporterServiceSpec extends DataBindImportAndDefaultExporterServiceSpec<DataFlowJsonImporterService, DataFlowJsonExporterService>

    implements JsonComparer {

    DataFlowJsonImporterService dataFlowJsonImporterService
    DataFlowJsonExporterService dataFlowJsonExporterService

    @Override
    DataFlowJsonImporterService getDataFlowImporterService() {
        dataFlowJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    @Override
    DataFlowJsonExporterService getDataFlowExporterService() {
        dataFlowJsonExporterService
    }

    @Override
    void validateExportedModel(String testName, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.${importType}")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedModel.bytes)
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedModel)
    }
}
