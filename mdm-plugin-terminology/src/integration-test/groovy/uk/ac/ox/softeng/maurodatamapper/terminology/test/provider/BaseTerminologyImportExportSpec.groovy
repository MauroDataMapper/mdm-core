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

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.DataBindTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 20/11/2017
 */
@Slf4j
abstract class BaseTerminologyImportExportSpec extends BaseTerminologyIntegrationSpec {

    @Autowired
    TerminologyService terminologyService

    @Shared
    Path resourcesPath

    @Shared
    UUID simpleTerminologyId

    @Shared
    UUID complexTerminologyId

    @Shared
    TerminologyFileImporterProviderServiceParameters basicParameters

    Object setupSpec() {
        basicParameters = new TerminologyFileImporterProviderServiceParameters()
    }

    Object setup() {
        basicParameters.tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
            propagateFromPreviousVersion = false
            importFile = null
        }
    }

    abstract DataBindTerminologyImporterProviderService getImporterService()

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType, 'terminology')
        assert getImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up TerminologyServiceSpec unit')

        simpleTerminologyId = simpleTerminology.id
        complexTerminologyId = complexTerminology.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    Terminology importModel(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = new FileParameter(fileContents: bytes)

        Terminology imported = importerService.importDomain(admin, basicParameters) as Terminology
        assert imported
        imported.folder = testFolder

        log.info('Checking imported model')
        check(imported)

        log.info('Saving imported model')
        assert terminologyService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        // assert terminologyService.count() == 3
        log.debug('Terminology saved')

        terminologyService.get(imported.id)
    }

    Terminology confirmTerminology(Terminology terminology) {
        assert terminology
        assert terminology.label == 'Imported Test Terminology'
        assert terminology.modelType == 'Terminology'
        assert terminology.createdBy == admin.emailAddress
        assert !terminology.description
        assert terminology.author == 'Test Author'
        assert terminology.organisation == 'Test Organisation'
        assert terminology.documentationVersion.toString() == '1.0.0'
        assert !terminology.finalised
        assert terminology.authority.label == 'Test Authority'
        assert terminology.authority.url == 'http://localhost'
        assert terminology.breadcrumbTree
        assert terminology.breadcrumbTree.domainId == terminology.id
        assert terminology.breadcrumbTree.label == terminology.label
        terminology
    }

    Terminology importAndConfirm(byte[] bytes) {
        importModel(bytes)
        Terminology tm = Terminology.listOrderByDateCreated().last()
        log.info('Confirming imported model')
        confirmTerminology(tm)
    }
}
