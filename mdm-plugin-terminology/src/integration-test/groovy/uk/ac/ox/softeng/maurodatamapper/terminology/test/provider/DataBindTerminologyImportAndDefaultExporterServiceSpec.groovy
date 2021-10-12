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
package uk.ac.ox.softeng.maurodatamapper.terminology.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.TerminologyExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.DataBindTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyImporterProviderServiceParameters

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll

import java.nio.charset.Charset

/**
 * @since 15/11/2017
 */
@Rollback
@Slf4j
abstract class DataBindTerminologyImportAndDefaultExporterServiceSpec<I extends DataBindTerminologyImporterProviderService, E extends TerminologyExporterProviderService>
    extends BaseImportExportTerminologySpec {

    @Autowired
    TerminologyService terminologyService

    @Shared
    TerminologyImporterProviderServiceParameters basicParameters

    def setupSpec() {
        basicParameters = new TerminologyImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
        }
    }

    abstract I getImporterService()

    abstract E getExporterService()

    abstract void validateExportedModel(String testName, String exportedModel)

    void validateExportedModels(String testName, String exportedModels) {
        validateExportedModel(testName, exportedModels)
    }

    Terminology importAndConfirm(byte[] bytes) {
        def imported = importerService.importTerminology(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        importerService.checkImport(admin, imported, basicParameters)
        check(imported)
        log.info('Saving imported model')
        assert terminologyService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        assert terminologyService.count() == 3

        Terminology tm = Terminology.listOrderByDateCreated().last()
        log.info('Confirming imported model')
        confirmTerminology(tm)
        tm
    }

    String exportModel(UUID dataModelId) {
        ByteArrayOutputStream byteArrayOutputStream = exporterService.exportDomain(admin, dataModelId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    String exportModels(List<UUID> terminologyIds) {
        new String(exporterService.exportDomains(admin, terminologyIds).toByteArray(), Charset.defaultCharset())
    }

    String importAndExport(byte[] bytes) {
        Terminology tm = importAndConfirm(bytes)
        assert tm, 'Must have a datamodel imported to be able to export'
        exportModel(tm.id)
    }

    static String replaceWithTestAuthority(String exported) {
        exported.replace(/Mauro Data Mapper/, 'Test Authority')
    }

    void 'E01 : test empty data import export'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'TEEP01'
    }

    @Unroll
    void 'E02 : test "#testName" data export'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String exported = importAndExport(loadTestFile(testName))

        then:
        validateExportedModel(testName, exported.replace(/Mauro Data Mapper/, 'Test Authority'))

        where:
        testName << [
            'simpleImport',
            'terminologyWithClassifiers',
            'terminologyWithAliases',
            'terminologyWithMetadata',
            'terminologyWithAnnotations',
            'complexImport'
        ]
    }

    void 'E03 : test export and import complex Terminology'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String exported = exportModel(complexTerminologyId)

        then:
        validateExportedModel('complexTerminology', exported)

        when:
        Terminology imported = importerService.importTerminology(admin, exported.bytes)

        then:
        assert imported

        when:
        imported.folder = testFolder
        ObjectDiff diff = terminologyService.getDiffForModels(terminologyService.get(complexTerminologyId), imported)

        then:
        if (!diff.objectsAreIdentical()) {
            log.error('{}', diff.toString())
        }
        // Rules are not exported/imported and therefore will exist as diffs
        diff.numberOfDiffs == 4
    }

    void 'E04 : test export and import simple Terminology'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String exported = exportModel(simpleTerminologyId)

        then:
        validateExportedModel('simpleTerminology', exported)

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
        ObjectDiff diff = terminologyService.getDiffForModels(terminologyService.get(simpleTerminologyId), imported)

        then:
        diff.objectsAreIdentical()
    }
}
