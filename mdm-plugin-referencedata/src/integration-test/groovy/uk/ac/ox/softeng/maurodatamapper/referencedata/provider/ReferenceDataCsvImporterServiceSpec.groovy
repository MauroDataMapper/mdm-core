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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.ReferenceDataCsvImporterService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 25/04/2022
 */
@Slf4j
@Rollback
@Integration
class ReferenceDataCsvImporterServiceSpec extends BaseReferenceDataModelIntegrationSpec {

    @Shared
    Path resourcesPath

    @Shared
    ReferenceDataModelFileImporterProviderServiceParameters basicParameters

    ReferenceDataCsvImporterService referenceDataCsvImporterService

    @RunOnce
    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'csv')
        basicParameters = new ReferenceDataModelFileImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
        }
    }

    @Override
    void setupDomainData() {

    }

    ReferenceDataModel importAndConfirm() {
        ReferenceDataModel imported = referenceDataCsvImporterService.importDomain(admin, basicParameters)
        imported.folder = testFolder
        check(imported)
        log.info('Saving imported model')
        assert referenceDataModelService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        assert referenceDataModelService.count() == 1

        ReferenceDataModel referenceDataModel = referenceDataModelService.get(imported.id)

        log.info('Confirming imported model')

        confirmReferenceDataModel(referenceDataModel)
        referenceDataModel
    }


    void confirmReferenceDataModel(referenceDataModel) {
        assert referenceDataModel
        assert referenceDataModel.createdBy == admin.emailAddress
        assert referenceDataModel.breadcrumbTree
        assert referenceDataModel.breadcrumbTree.domainId == referenceDataModel.id
        assert referenceDataModel.breadcrumbTree.label == referenceDataModel.label
    }

    void loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.csv").toAbsolutePath()
        assert Files.exists(testFilePath)
        basicParameters.importFile = new FileParameter(fileContents: Files.readAllBytes(testFilePath),
                                                       fileName: testFilePath.fileName.toString())
    }

    void 'I01 : test importing simple csv'() {
        given:
        setupData()

        when:
        loadTestFile('simpleCSV')

        and:
        ReferenceDataModel rdm = importAndConfirm()

        then:
        rdm.label == 'simpleCSV.csv'
        rdm.documentationVersion.toString() == '1.0.0'
        !rdm.finalised
        rdm.authority.label == 'Test Authority'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        rdm.referenceDataTypes.size() == 1
        rdm.referenceDataElements.size() == 3
        rdm.referenceDataValues.size() == 30

    }

    void 'I02 : test importing big csv'() {
        given:
        setupData()

        when:
        loadTestFile('bigCSV')

        and:
        ReferenceDataModel rdm = importAndConfirm()

        then:
        rdm.label == 'bigCSV.csv'
        rdm.documentationVersion.toString() == '1.0.0'
        !rdm.finalised
        rdm.authority.label == 'Test Authority'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        rdm.referenceDataTypes.size() == 1
        rdm.referenceDataElements.size() == 10
        rdm.referenceDataValues.size() == 100800

    }

    void 'I03 : test importing other csv'() {
        given:
        setupData()

        when:
        loadTestFile('CHP.names.and.codes.SC.as.at.04_12')

        and:
        ReferenceDataModel rdm = importAndConfirm()

        then:
        rdm.label == 'CHP.names.and.codes.SC.as.at.04_12.csv'
        rdm.documentationVersion.toString() == '1.0.0'
        !rdm.finalised
        rdm.authority.label == 'Test Authority'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        rdm.referenceDataTypes.size() == 1
        rdm.referenceDataElements.size() == 3
        rdm.referenceDataValues.size() == 102

    }
}
