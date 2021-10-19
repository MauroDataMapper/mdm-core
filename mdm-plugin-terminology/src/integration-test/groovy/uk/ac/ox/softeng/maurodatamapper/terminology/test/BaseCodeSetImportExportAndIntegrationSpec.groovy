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
package uk.ac.ox.softeng.maurodatamapper.terminology.test

import uk.ac.ox.softeng.maurodatamapper.core.admin.AdminService
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.DataBindCodeSetImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 05/10/2020
 */
@Slf4j
abstract class BaseCodeSetImportExportAndIntegrationSpec<I extends DataBindCodeSetImporterProviderService, E extends CodeSetExporterProviderService>
    extends BaseIntegrationSpec {

    static final String NO_CODESET_IDS_TO_EXPORT_CODE = 'CSEP01'

    @Autowired
    CodeSetService codeSetService

    @Shared
    Path resourcesPath

    @Shared
    UUID simpleCodeSetId

    @Shared
    UUID complexCodeSetId

    @Shared
    UUID simpleTerminologyId

    @Shared
    CodeSetFileImporterProviderServiceParameters basicParameters

    Authority testAuthority
    AdminService adminService
    TerminologyService terminologyService
    CodeSet simpleCodeSet
    CodeSet complexCodeSet
    Terminology simpleTerminology

    Folder getTestFolder() {
        folder
    }

    def setupSpec() {
        basicParameters = new CodeSetFileImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
            propagateFromPreviousVersion = false
            importFile = null
        }
    }

    abstract I getImporterService()

    abstract E getExporterService()

    abstract String getImportType()

    abstract void validateExportedModel(String testName, String exportedModel)

    void validateExportedModels(String testName, String exportedModels) {
        validateExportedModel(testName, exportedModels)
    }

    void cleanupParameters() {
        basicParameters.tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
            propagateFromPreviousVersion = false
            importFile = null
        }
    }

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getImporterService()
    }

    @Override
    void preDomainDataSetup() {
        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        testAuthority = Authority.findByLabel('Test Authority')
        checkAndSave(testAuthority)
        simpleCodeSet = BootstrapModels.buildAndSaveSimpleCodeSet(messageSource, folder, testAuthority)
        complexCodeSet = BootstrapModels.buildAndSaveComplexCodeSet(messageSource, folder, terminologyService, testAuthority)
        simpleTerminology = Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME)
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up CodeSetServiceSpec unit')

        simpleCodeSetId = simpleCodeSet.id
        complexCodeSetId = complexCodeSet.id
        simpleTerminologyId = simpleTerminology.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    CodeSet importModel(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = null

        CodeSet imported = importerService.importCodeSet(admin, bytes)
        assert imported
        imported.folder = testFolder

        log.info('Checking imported model')
        importerService.checkImport(admin, imported, basicParameters)
        check(imported)

        log.info('Saving imported model')
        assert codeSetService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        // assert codeSetService.count() == 3
        log.debug('CodeSet saved')

        codeSetService.get(imported.id)
    }

    List<CodeSet> importModels(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = null

        List<CodeSet> imported = importerService.importCodeSets(admin, bytes)
        imported.each {
            assert it
            it.folder = testFolder

            log.info('Checking imported model')
            importerService.checkImport(admin, it, basicParameters)
            check(it)

            log.info('Saving imported model')
            assert codeSetService.saveModelWithContent(it)
            log.debug('CodeSet saved')
        }
        sessionFactory.currentSession.flush()
        log.debug('CodeSets saved')

        imported.collect { codeSetService.get(it.id) }
    }

    CodeSet importAndConfirm(byte[] bytes) {
        CodeSet codeSet = importModel(bytes)
        log.info('Confirming imported model')
        assert codeSet
        assert codeSet.createdBy == admin.emailAddress
        codeSet
    }

    String exportModel(UUID codeSetId) {
        new String(exporterService.exportDomain(admin, codeSetId).toByteArray(), Charset.defaultCharset())
    }

    String exportModels(List<UUID> codeSetIds) {
        new String(exporterService.exportDomains(admin, codeSetIds).toByteArray(), Charset.defaultCharset())
    }

    CodeSet clearExpectedDiffsFromImport(CodeSet codeSet) {
        codeSet.tap {
            finalised = true
            modelVersion = new Version(major: 1, minor: 0, patch: 0)
        }
    }

    List<CodeSet> clearExpectedDiffsFromModels(List<UUID> modelIds) {
        modelIds.collect {
            codeSetService.get(it).tap {
                dateFinalised = null
            }
        }
    }
}
