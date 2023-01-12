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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.DataBindTerminologyImportAndDefaultExporterServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Assert
import org.junit.jupiter.api.Tag

import java.nio.file.Files
import java.nio.file.Path

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
@Tag('non-parallel')
class TerminologyJsonExporterServiceSpec extends DataBindTerminologyImportAndDefaultExporterServiceSpec<TerminologyJsonImporterService, TerminologyJsonExporterService>
    implements JsonComparer {

    private static final String NO_TERMINOLOGY_IDS_TO_EXPORT_CODE = 'TEEP01'

    TerminologyJsonImporterService terminologyJsonImporterService
    TerminologyJsonExporterService terminologyJsonExporterService

    @Override
    String getImportType() {
        'json'
    }

    @Override
    TerminologyJsonImporterService getImporterService() {
        terminologyJsonImporterService
    }

    @Override
    TerminologyJsonExporterService getExporterService() {
        terminologyJsonExporterService
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

    void 'test multi-export invalid Terminologies'() {
        expect:
        exporterService.canExportMultipleDomains()

        when: 'given null'
        exportModels(null)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_TERMINOLOGY_IDS_TO_EXPORT_CODE

        when: 'given an empty list'
        exportModels([])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_TERMINOLOGY_IDS_TO_EXPORT_CODE

        when: 'given a null model'
        exportModels([null])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_TERMINOLOGY_IDS_TO_EXPORT_CODE

        when: 'given a single invalid model'
        exportModels([UUID.randomUUID()])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_TERMINOLOGY_IDS_TO_EXPORT_CODE

        when: 'given multiple invalid models'
        exportModels([UUID.randomUUID(), UUID.randomUUID()])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_TERMINOLOGY_IDS_TO_EXPORT_CODE
    }

    void 'test multi-export single Terminology'() {
        given:
        setupData()
        Terminology.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleTerminologyId])

        then:
        validateExportedModels('simpleTerminologyInList', replaceWithTestAuthority(exported))
    }

    void 'test multi-export multiple Terminologies'() {
        given:
        setupData()
        Terminology.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleTerminologyId, complexTerminologyId])

        then:
        validateExportedModels('simpleAndComplexTerminologies', replaceWithTestAuthority(exported))
    }

    void 'test multi-export Terminologies with invalid models'() {
        given:
        setupData()
        Terminology.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([UUID.randomUUID(), simpleTerminologyId])

        then:
        validateExportedModels('simpleTerminologyInList', replaceWithTestAuthority(exported))

        when:
        exported = exportModels([UUID.randomUUID(), simpleTerminologyId, UUID.randomUUID(), complexTerminologyId])

        then:
        validateExportedModels('simpleAndComplexTerminologies', replaceWithTestAuthority(exported))
    }

    void 'test multi-export Terminologies with duplicates'() {
        given:
        setupData()
        Terminology.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleTerminologyId, simpleTerminologyId])

        then:
        validateExportedModels('simpleTerminologyInList', replaceWithTestAuthority(exported))

        when:
        exported = exportModels([simpleTerminologyId, complexTerminologyId, complexTerminologyId, simpleTerminologyId])

        then:
        validateExportedModels('simpleAndComplexTerminologies', replaceWithTestAuthority(exported))
    }
}
