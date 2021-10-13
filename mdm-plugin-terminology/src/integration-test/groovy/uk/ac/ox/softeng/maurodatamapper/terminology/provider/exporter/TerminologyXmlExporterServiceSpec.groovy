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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.TerminologyXmlExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyXmlImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.DataBindTerminologyImportAndDefaultExporterServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlValidator

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Assert
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.Assert.assertTrue

/**
 * @since 18/09/2020
 */
@Integration
@Rollback
@Slf4j
class TerminologyXmlExporterServiceSpec extends DataBindTerminologyImportAndDefaultExporterServiceSpec<TerminologyXmlImporterService, TerminologyXmlExporterService>
    implements XmlValidator {

    TerminologyXmlImporterService terminologyXmlImporterService
    TerminologyXmlExporterService terminologyXmlExporterService

    String getImportType() {
        'xml'
    }

    TerminologyXmlImporterService getImporterService() {
        terminologyXmlImporterService
    }

    TerminologyXmlExporterService getExporterService() {
        terminologyXmlExporterService
    }

    @Override
    void validateExportedModel(String testName, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.xml")
        if (!Files.exists(expectedPath)) {
            Files.writeString(expectedPath, (prettyPrint(exportedModel)))
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }
        validateAndCompareXml(Files.readString(expectedPath), exportedModel, 'export', exporterService.version)
    }

    @Unroll
    void 'test "#testName" xml files are valid'() {
        given:
        setupData()

        when:
        Path xmlPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.xml")
        if (!Files.exists(xmlPath)) {
            Assert.fail("Expected export file ${xmlPath} does not exist")
        }

        def xmlIsValid = validateXml('export', terminologyXmlExporterService.version, Files.readString(xmlPath))

        then:
        assertTrue failureReason, xmlIsValid

        where:
        testName << [
            'simpleImport',
            'terminologyWithAliases',
            'terminologyWithMetadata',
            'terminologyWithAnnotations',
            'complexImport'
        ]
    }
}
