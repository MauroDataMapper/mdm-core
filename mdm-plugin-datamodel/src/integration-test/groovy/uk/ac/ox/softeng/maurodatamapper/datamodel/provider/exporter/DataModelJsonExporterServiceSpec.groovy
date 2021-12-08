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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindImportAndDefaultExporterServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Assert

import java.nio.file.Files
import java.nio.file.Path

/**
 * @since 18/11/2017
 */
@Integration
@Rollback
@Slf4j
class DataModelJsonExporterServiceSpec extends DataBindImportAndDefaultExporterServiceSpec<DataModelJsonImporterService, DataModelJsonExporterService>
    implements JsonComparer {

    private static final String NO_DATAMODEL_IDS_TO_EXPORT_CODE = 'DMEP01'
    private static final String SIMPLE_DATAMODEL_FILENAME = 'simpleDataModel'
    private static final String SIMPLE_AND_COMPLEX_DATAMODELS_FILENAME = 'simpleAndComplexDataModels'

    DataModelJsonImporterService dataModelJsonImporterService
    DataModelJsonExporterService dataModelJsonExporterService

    @Override
    DataModelJsonImporterService getImporterService() {
        dataModelJsonImporterService
    }

    @Override
    DataModelJsonExporterService getExporterService() {
        dataModelJsonExporterService
    }

    @Override
    String getImportType() {
        'json'
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

    void 'test multi-export invalid DataModels'() {
        expect:
        exporterService.canExportMultipleDomains()

        when: 'given null'
        exportModels(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == NO_DATAMODEL_IDS_TO_EXPORT_CODE

        when: 'given an empty list'
        exportModels([])

        then:
        exception = thrown(ApiInternalException)
        exception.errorCode == NO_DATAMODEL_IDS_TO_EXPORT_CODE

        when: 'given a null model'
        String exported = exportModels([null])

        then:
        exception = thrown(ApiInternalException)
        exception.errorCode == NO_DATAMODEL_IDS_TO_EXPORT_CODE

        when: 'given a single invalid model'
        exported = exportModels([UUID.randomUUID()])

        then:
        exception = thrown(ApiInternalException)
        exception.errorCode == NO_DATAMODEL_IDS_TO_EXPORT_CODE

        when: 'given multiple invalid models'
        exported = exportModels([UUID.randomUUID(), UUID.randomUUID()])

        then:
        exception = thrown(ApiInternalException)
        exception.errorCode == NO_DATAMODEL_IDS_TO_EXPORT_CODE
    }

    void 'test multi-export single DataModel'() {
        given:
        setupData()
        DataModel.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleDataModelId])

        then:
        validateExportedModels(SIMPLE_DATAMODEL_FILENAME, replaceWithTestAuthority(exported))
    }

    void 'test multi-export multiple DataModels'() {
        given:
        setupData()
        DataModel.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleDataModelId, complexDataModelId])

        then:
        validateExportedModels(SIMPLE_AND_COMPLEX_DATAMODELS_FILENAME, replaceWithTestAuthority(exported))
    }

    void 'test multi-export DataModels with invalid models'() {
        given:
        setupData()
        DataModel.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([UUID.randomUUID(), simpleDataModelId])

        then:
        validateExportedModels(SIMPLE_DATAMODEL_FILENAME, replaceWithTestAuthority(exported))

        when:
        exported = exportModels([UUID.randomUUID(), simpleDataModelId, UUID.randomUUID(), complexDataModelId])

        then:
        validateExportedModels(SIMPLE_AND_COMPLEX_DATAMODELS_FILENAME, replaceWithTestAuthority(exported))
    }

    void 'test multi-export DataModels with duplicates'() {
        given:
        setupData()
        DataModel.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleDataModelId, simpleDataModelId])

        then:
        validateExportedModels(SIMPLE_DATAMODEL_FILENAME, replaceWithTestAuthority(exported))

        when:
        exported = exportModels([simpleDataModelId, complexDataModelId, complexDataModelId, simpleDataModelId])

        then:
        validateExportedModels(SIMPLE_AND_COMPLEX_DATAMODELS_FILENAME, replaceWithTestAuthority(exported))
    }
}
