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
package uk.ac.ox.softeng.maurodatamapper.terminology.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
abstract class BaseImporterExporterSpec extends BaseTerminologyIntegrationSpec {

    @Shared
    Path resourcesPath

    @Shared
    UUID complexTerminologyId

    @Shared
    UUID simpleTerminologyId

    abstract ImporterProviderService getImporterService()
    abstract ExporterProviderService getExporterService()
    abstract void validateExportedModel(String testName, String exportedModel)

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up TerminologyServiceSpec unit')

        complexTerminologyId = complexTerminology.id
        simpleTerminologyId = simpleTerminology.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    String exportModel(UUID terminologyId) {
        ByteArrayOutputStream byteArrayOutputStream = exporterService.exportDomain(admin, terminologyId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    Terminology importAndConfirm(byte[] bytes) {
        def imported = importerService.importTerminology(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        check(imported)
        log.info('Saving imported model')
        assert terminologyService.saveWithBatching(imported)
        sessionFactory.currentSession.flush()
        assert terminologyService.count() == 3

        Terminology t = Terminology.listOrderByDateCreated().last()

        log.info('Confirming imported model')

        confirmTerminology(t)
        t
    }

    void confirmTerminology(terminology) {
        //assert dataModel
        //assert dataModel.label == 'National Minimum Data Set for Thoracic Surgery and Lung Cancer Surgery'
        //assert dataModel.modelType == DataModelType.DATA_STANDARD.label
        //assert dataModel.createdBy == admin.emailAddress
        //assert !dataModel.description
        //assert !dataModel.author
        //assert !dataModel.organisation
        //assert dataModel.breadcrumbTree
        //assert dataModel.breadcrumbTree.domainId == dataModel.id
        //assert dataModel.breadcrumbTree.label == dataModel.label
        assert true
    }

    void 'test that trying to export when specifying a null terminologyId fails with an exception'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'TEEP01'
    }

    void 'test exporting and reimporting the simple bootstrapped terminology'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String exported = exportModel(simpleTerminologyId)
        //log.debug(exported)

        then:
        validateExportedModel('simple', exported)

        //note: importing does not actually save
        when:
        Terminology imported = importerService.importTerminology(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier simple'

        when:
        imported.folder = testFolder
        ObjectDiff diff = terminologyService.diff(terminologyService.get(simpleTerminologyId), imported)

        then:
        diff.objectsAreIdentical()
    }

    void 'test exporting and reimporting the complex bootstrapped terminology'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String exported = exportModel(complexTerminologyId)
        log.debug(exported)

        then:
        validateExportedModel('complex', exported)

        //note: importing does not actually save
        when:
        Terminology imported = importerService.importTerminology(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 2

        when:
        imported.folder = testFolder
        ObjectDiff diff = terminologyService.diff(terminologyService.get(complexTerminologyId), imported)

        then:
        diff.objectsAreIdentical()
    }

}
