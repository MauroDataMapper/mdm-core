/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetXmlExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetXmlImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseCodeSetIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlValidator
import uk.ac.ox.softeng.maurodatamapper.version.Version

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.junit.Assert
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
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
@Tag('non-parallel')
class CodeSetXmlImporterExporterServiceSpec extends BaseCodeSetIntegrationSpec implements XmlValidator {

    private static final String CANNOT_IMPORT_EMPTY_CONTENT_CODE = 'XTIS02'
    private static final String NO_CODESET_TO_IMPORT_CODE = 'FBIP03'
    private static final String NO_CODESET_IDS_TO_EXPORT_CODE = 'CSEP01'

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

    CodeSetXmlImporterService getImporterService() {
        codeSetXmlImporterService
    }

    CodeSetXmlExporterService getExporterService() {
        codeSetXmlExporterService
    }

    String getImportType() {
        'xml'
    }

    void validateExportedModel(String testName, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.xml")
        if (!Files.exists(expectedPath)) {
            Files.writeString(expectedPath, (prettyPrintXml(exportedModel)))
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }
        validateAndCompareXml(Files.readString(expectedPath), exportedModel, 'export',
                              codeSetXmlExporterService.version)
    }

    void validateExportedModels(String testName, String exportedModels) {
        validateExportedModel(testName, exportedModels)
    }

    @RunOnce
    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType, 'codeset')
        assert getImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up CodeSetXmlImporterExporterServiceSpec')

        simpleCodeSetId = simpleCodeSet.id
        complexCodeSetId = complexCodeSet.id
        simpleTerminologyId = simpleTerminology.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    String exportModel(UUID codeSetId) {
        ByteArrayOutputStream byteArrayOutputStream = exporterService.exportDomain(admin, codeSetId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    String exportModels(List<UUID> codeSetIds) {
        new String(exporterService.exportDomains(admin, codeSetIds).toByteArray(), Charset.defaultCharset())
    }

    CodeSet importAndConfirm(byte[] bytes) {
        CodeSet imported = importerService.importCodeSet(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        importerService.checkImport(admin, imported, basicParameters)
        check(imported)
        log.info('Saving imported model')
        assert codeSetService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        assert codeSetService.count() == 3

        CodeSet codeSet = codeSetService.get(imported.id)

        log.info('Confirming imported model')

        confirmCodeSet(codeSet)
        codeSet
    }

    List<CodeSet> importModels(byte[] bytes) {
        List<CodeSet> imported = importerService.importCodeSets(admin, bytes)
        imported.each {
            it.folder = testFolder
            importerService.checkImport(admin, it, basicParameters)
            check(it)
            assert codeSetService.saveModelWithContent(it)
        }
        sessionFactory.currentSession.flush()
        imported.collect { codeSetService.get(it.id) }
    }

    void confirmCodeSet(codeSet) {
        assert codeSet
        assert codeSet.createdBy == admin.emailAddress
    }

    CodeSet clearExpectedDiffsFromImport(CodeSet codeSet) {
        codeSet.tap {
            finalised = true
            modelVersion = new Version(major: 1, minor: 0, patch: 0)
        }
    }

    List<CodeSet> clearExpectedDiffsFromModels(List<UUID> modelIds) {
        // Rules are not imported/exported and will therefore exist as diffs
        Closure<Boolean> removeRule = {it.rules?.removeIf {rule -> rule.name == 'Bootstrapped Functional Test Rule'}}
        List<CodeSet> codeSets = modelIds.collect {
            CodeSet codeSet = codeSetService.get(it)
            removeRule(codeSet)
            codeSet.dateFinalised = null
            codeSet
        }
        sessionFactory.currentSession.clear()
        codeSets
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

    void 'test exporting and reimporting the simple bootstrapped codeset'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 2

        when:
        String exported = exportModel(simpleCodeSetId)

        then:
        validateExportedModel('bootstrappedSimpleCodeSet', exported)

        //note: importing does not actually save
        when:
        CodeSet imported = importerService.importCodeSet(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier'

        when:
        imported.folder = testFolder
        ObjectDiff diff = codeSetService.get(simpleCodeSetId).diff(imported, 'none', null, null)

        then:
        diff.objectsAreIdentical()
    }

    void 'test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importCodeSet(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)
    }

    void 'test simple data import'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

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
        term0.label == 'STT01: Simple Test Term 01' || term0.label == 'STT02: Simple Test Term 02'
        term1.label == 'STT01: Simple Test Term 01' || term1.label == 'STT02: Simple Test Term 02'
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
        CodeSet.count() == 2

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
        CodeSet.count() == 2

        when:
        String data = new String(loadTestFile('codeSetSimpleWithUnknownTerminology'))

        and:
        CodeSet cs = importAndConfirm(data.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'CSS01'
    }

    void 'test multi-import invalid CodeSet content'() {
        expect:
        importerService.canImportMultipleDomains()

        when: 'given empty content'
        importModels(''.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_EMPTY_CONTENT_CODE

        when: 'given neither models list or model map (backwards compatibility)'
        importModels(loadTestFile('exportMetadataOnly'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given an empty model map (backwards compatibility)'
        importModels(loadTestFile('emptyCodeSet'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        // when: 'given an empty models list'
        // importModels(loadTestFile('emptyCodeSetList'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import invalid CodeSets'() {
        given:
        setupData()

        expect:
        importerService.canImportMultipleDomains()

        when: 'given an invalid model map (backwards compatibility)'
        importModels(loadTestFile('invalidCodeSet'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given a single invalid model'
        importModels(loadTestFile('invalidCodeSetInList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given multiple invalid models'
        importModels(loadTestFile('invalidCodeSets'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        // when: 'not given export metadata'
        // importModels(loadTestFile('noExportMetadata'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import single CodeSet (backwards compatibility)'() {
        given:
        setupData()
        CodeSet.count() == 2
        List<CodeSet> codeSets = clearExpectedDiffsFromModels([simpleCodeSetId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<CodeSet> imported = importModels(loadTestFile('bootstrappedSimpleCodeSet')).each {
            clearExpectedDiffsFromImport(it)
        }

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = codeSets.pop().diff(imported.pop(), 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import single CodeSet'() {
        given:
        setupData()
        CodeSet.count() == 2
        List<CodeSet> codeSets = clearExpectedDiffsFromModels([simpleCodeSetId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<CodeSet> imported = importModels(loadTestFile('simpleCodeSetInList')).each {
            clearExpectedDiffsFromImport(it)
        }

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = codeSets.pop().diff(imported.pop(), 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import multiple CodeSets'() {
        given:
        setupData()
        CodeSet.count() == 2
        List<CodeSet> codeSets = clearExpectedDiffsFromModels([simpleCodeSetId, complexCodeSetId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<CodeSet> imported = importModels(loadTestFile('simpleAndComplexCodeSets')).each {
            clearExpectedDiffsFromImport(it)
        }

        then:
        imported
        imported.size() == 2

        when:
        ObjectDiff simpleDiff = codeSets[0].diff(imported[0], 'none', null, null)
        ObjectDiff complexDiff = codeSets[1].diff(imported[1], 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import CodeSets with invalid models'() {
        given:
        setupData()
        CodeSet.count() == 2
        List<CodeSet> codeSets = clearExpectedDiffsFromModels([simpleCodeSetId, complexCodeSetId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<CodeSet> imported = importModels(loadTestFile('simpleAndInvalidCodeSets')).each {
            clearExpectedDiffsFromImport(it)
        }

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = codeSets[0].diff(imported.pop(), 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleComplexAndInvalidCodeSets')).each {
            clearExpectedDiffsFromImport(it)
        }

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = codeSets[0].diff(imported[0], 'none', null, null)
        ObjectDiff complexDiff = codeSets[1].diff(imported[1], 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-export invalid CodeSets'() {
        expect:
        exporterService.canExportMultipleDomains()

        when: 'given null'
        exportModels(null)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_CODESET_IDS_TO_EXPORT_CODE

        when: 'given an empty list'
        exportModels([])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_CODESET_IDS_TO_EXPORT_CODE

        when: 'given a null model'
        exportModels([null])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_CODESET_IDS_TO_EXPORT_CODE

        when: 'given a single invalid model'
        exportModels([UUID.randomUUID()])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_CODESET_IDS_TO_EXPORT_CODE

        when: 'given multiple invalid models'
        exportModels([UUID.randomUUID(), UUID.randomUUID()])

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_CODESET_IDS_TO_EXPORT_CODE
    }

    void 'test multi-export single CodeSet'() {
        given:
        setupData()
        CodeSet.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleCodeSetId])

        then:
        validateExportedModels('simpleCodeSetInList', replaceWithTestAuthority(exported))
    }

    void 'test multi-export multiple CodeSets'() {
        given:
        setupData()
        CodeSet.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleCodeSetId, complexCodeSetId])

        then:
        validateExportedModels('simpleAndComplexCodeSets', replaceWithTestAuthority(exported))
    }

    void 'test multi-export CodeSets with invalid models'() {
        given:
        setupData()
        CodeSet.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([UUID.randomUUID(), simpleCodeSetId])

        then:
        validateExportedModels('simpleCodeSetInList', replaceWithTestAuthority(exported))

        when:
        exported = exportModels([UUID.randomUUID(), simpleCodeSetId, UUID.randomUUID(), complexCodeSetId])

        then:
        validateExportedModels('simpleAndComplexCodeSets', replaceWithTestAuthority(exported))
    }

    void 'test multi-export CodeSets with duplicates'() {
        given:
        setupData()
        CodeSet.count() == 2

        expect:
        exporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleCodeSetId, simpleCodeSetId])

        then:
        validateExportedModels('simpleCodeSetInList', replaceWithTestAuthority(exported))

        when:
        exported = exportModels([simpleCodeSetId, complexCodeSetId, complexCodeSetId, simpleCodeSetId])

        then:
        validateExportedModels('simpleAndComplexCodeSets', replaceWithTestAuthority(exported))
    }
}
