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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetXmlExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetXmlImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseCodeSetIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlValidator

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.junit.Assert
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 18/09/2020
 */
@Integration
@Rollback
@Slf4j
class XmlCodeSetImporterExporterServiceSpec extends BaseCodeSetIntegrationSpec implements XmlValidator {

    @Shared
    Path resourcesPath

    @Shared
    UUID simpleTerminologyId

    @Shared
    UUID simpleCodeSetId

    CodeSetXmlImporterService codeSetXmlImporterService
    CodeSetXmlExporterService codeSetXmlExporterService

    @Shared
    CodeSetFileImporterProviderServiceParameters basicParameters

    def setupSpec() {
        basicParameters = new CodeSetFileImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
        }
    }

    String getImportType() {
        'xml'
    }

    CodeSetXmlImporterService getCodeSetImporterService() {
        codeSetXmlImporterService
    }

    CodeSetXmlExporterService getCodeSetExporterService() {
        codeSetXmlExporterService
    }

    void validateExportedModel(String testName, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.xml")
        if (!Files.exists(expectedPath)) {
            Files.writeString(expectedPath, (prettyPrint(exportedModel)))
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }
        validateAndCompareXml(Files.readString(expectedPath), exportedModel.replace(/Mauro Data Mapper/, 'Test Authority'), 'export',
                              codeSetXmlExporterService.version)
    }


    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getCodeSetImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up CodeSetServiceSpec unit')

        simpleTerminologyId = simpleTerminology.id
        simpleCodeSetId = simpleCodeSet.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    String exportModel(UUID codeSetId) {
        ByteArrayOutputStream byteArrayOutputStream = codeSetExporterService.exportDomain(admin, codeSetId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    CodeSet importAndConfirm(byte[] bytes) {
        CodeSet imported = codeSetImporterService.importCodeSet(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        codeSetImporterService.checkImport(admin, imported, basicParameters)
        check(imported)
        log.info('Saving imported model')
        assert codeSetService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        assert codeSetService.count() == 2

        CodeSet codeSet = codeSetService.get(imported.id)

        log.info('Confirming imported model')

        confirmCodeSet(codeSet)
        codeSet
    }


    void confirmCodeSet(codeSet) {
        assert codeSet
        assert codeSet.createdBy == admin.emailAddress
    }

    void 'test that trying to export when specifying a null codeSetId fails with an exception'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'CSEP01'
    }

    void 'test that trying to import multiple codeSets fails'() {
        given:
        setupData()

        expect:
        !codeSetImporterService.canImportMultipleDomains()

        when:
        codeSetImporterService.importCodeSets(admin, loadTestFile('codeSetSimple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('cannot import multiple CodeSets')
    }

    void 'test exporting and reimporting the simple bootstrapped codeset'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String exported = exportModel(simpleCodeSetId)

        then:
        validateExportedModel('bootstrappedSimpleCodeSet', exported)

        //note: importing does not actually save
        when:
        CodeSet imported = codeSetImporterService.importCodeSet(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier'

        when:
        imported.folder = testFolder
        ObjectDiff diff = codeSetService.getDiffForModels(codeSetService.get(simpleCodeSetId), imported)

        then:
        diff.objectsAreIdentical()
    }


    void 'test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        codeSetImporterService.importCodeSet(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)
    }

    void 'test simple data import'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimple'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        !cs.annotations
        !cs.metadata
        !cs.classifiers
        !cs.terms

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimple', exported)
    }

    void 'test simple data import finalised'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleFinalised'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.modelVersion.toString() == '6.3.1'
        cs.finalised == true
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        !cs.annotations
        !cs.metadata
        !cs.classifiers
        !cs.terms

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleFinalised', exported)
    }

    void 'test simple data import with aliases'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithAliases'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        cs.aliases.size() == 2
        'Alias 1' in cs.aliases
        'Alias 2' in cs.aliases
        !cs.annotations
        !cs.metadata
        !cs.classifiers
        !cs.terms

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleWithAliases', exported)
    }

    void 'test simple data import with annotations'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithAnnotations'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        cs.annotations.size() == 1
        !cs.metadata
        !cs.classifiers
        !cs.terms

        when:
        Annotation ann = cs.annotations[0]

        then:
        ann.description == 'test annotation 1 description'
        ann.label == 'test annotation 1 label'

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleWithAnnotations', exported)
    }

    void 'test simple data import with metadata'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithMetadata'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        !cs.annotations
        cs.metadata.size() == 3
        !cs.classifiers
        !cs.terms

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleWithMetadata', exported)
    }

    void 'test simple data import with classifiers'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithClassifiers'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        !cs.annotations
        !cs.metadata
        cs.classifiers.size() == 2
        !cs.terms

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleWithClassifiers', exported)
    }

    void 'test simple data import with known term'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithKnownTerm'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        !cs.annotations
        !cs.metadata
        !cs.classifiers
        cs.terms.size() == 1

        when:
        Term term0 = cs.terms[0]

        then:
        term0.label == 'STT01: Simple Test Term 01'

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleWithKnownTerm', exported)
    }

    void 'test simple data import with two known terms'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithTwoKnownTerms'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        cs.label == 'Simple CodeSet Import'
        cs.author == 'Test Author'
        cs.organisation == 'Test Organisation'
        cs.documentationVersion.toString() == '1.0.0'
        cs.finalised == false
        cs.authority.label == 'Test Authority'
        cs.authority.url == 'http://localhost'
        !cs.aliases
        !cs.annotations
        !cs.metadata
        !cs.classifiers
        cs.terms.size() == 2

        when:
        Term term0 = cs.terms[0]
        Term term1 = cs.terms[1]

        then:
        term0.label == "STT01: Simple Test Term 01" || term0.label == "STT02: Simple Test Term 02"
        term1.label == "STT01: Simple Test Term 01" || term1.label == "STT02: Simple Test Term 02"
        term0.label != term1.label

        when:
        String exported = exportModel(cs.id)

        then:
        validateExportedModel('codeSetSimpleWithTwoKnownTerms', exported)
    }

    void 'test simple data import with unknown term'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithUnknownTerm'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'CSS01'
    }

    void 'test simple data import with unknown terminology'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String data = new String(loadTestFile('codeSetSimpleWithUnknownTerminology'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'CSS01'
    }
}
