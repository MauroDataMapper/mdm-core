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
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseCodeSetIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

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
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
class CodeSetJsonImporterExporterServiceSpec extends BaseCodeSetIntegrationSpec implements JsonComparer {

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

    CodeSetJsonImporterService codeSetJsonImporterService
    CodeSetJsonExporterService codeSetJsonExporterService

    def setupSpec() {
        basicParameters = new CodeSetFileImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
        }
    }

    CodeSetJsonImporterService getCodeSetImporterService() {
        codeSetJsonImporterService
    }

    CodeSetJsonExporterService getCodeSetExporterService() {
        codeSetJsonExporterService
    }

    String getImportType() {
        'json'
    }

    void cleanupParameters() {
        basicParameters.tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
            propagateFromPreviousVersion = false
        }
    }

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

    void validateExportedModels(String testName, String exportedModels) {
        validateExportedModel(testName, exportedModels)
    }

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getCodeSetImporterService()
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

    String exportModel(UUID codeSetId) {
        ByteArrayOutputStream byteArrayOutputStream = codeSetExporterService.exportDomain(admin, codeSetId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    String exportModels(List<UUID> codeSetIds) {
        new String(codeSetExporterService.exportDomains(admin, codeSetIds).toByteArray(), Charset.defaultCharset())
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
        assert codeSetService.count() == 3

        CodeSet codeSet = codeSetService.get(imported.id)

        log.info('Confirming imported model')

        confirmCodeSet(codeSet)
        codeSet
    }

    CodeSet importModel(byte[] bytes) {
        CodeSet imported = codeSetImporterService.importCodeSet(admin, bytes)
        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        codeSetImporterService.checkImport(admin, imported, basicParameters)
        check(imported)
        assert codeSetService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()

        CodeSet codeSet = codeSetService.get(imported.id)
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

    void 'PG01 test propagatingCatalogueItemElements'() {

        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        CodeSet codeSet = CodeSet.findById(simpleCodeSetId)

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest', createdBy: admin.emailAddress)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: admin.emailAddress)
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value', createdBy: admin.emailAddress)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: admin.emailAddress).addToRuleRepresentations(language: 'e', representation:
            'a+b', createdBy: admin.emailAddress)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: admin,
                                                         targetMultiFacetAwareItem: Term.findByCode('STT01'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: admin.emailAddress)

        codeSet.addToAnnotations(testAnnotation)
        codeSet.addToClassifiers(testClassifier)
        codeSet.addToMetadata(testMetadata)
        codeSet.addToRules(testRule)
        codeSet.addToSemanticLinks(testSemanticLink)
        codeSet.addToReferenceFiles(testReferenceFile)

        checkAndSave(testClassifier)
        checkAndSave(codeSet)

        when:
        //propagatedBootstrappedSimpleCodeSet is simply not finalised
        CodeSet cs = importModel(loadTestFile('propagatedBootstrappedSimpleCodeSet'))

        then:
        cs.annotations.find {it.label == testAnnotation.label}
        cs.classifiers.find {it.label == testClassifier.label}
        cs.metadata.find {it.namespace == testMetadata.namespace}
        cs.rules.find {it.name == testRule.name}
        cs.semanticLinks.find {it.targetMultiFacetAwareItemId == testSemanticLink.targetMultiFacetAwareItemId}
        cs.semanticLinks.find {it.multiFacetAwareItemDomainType == testSemanticLink.multiFacetAwareItemDomainType}
        cs.referenceFiles.find {it.fileName == testReferenceFile.fileName}

        cleanup:
        cleanupParameters()
    }

    void 'test import multiple CodeSets'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 2
        codeSetImporterService.canImportMultipleDomains()

        when:
        List<CodeSet> codeSets = codeSetImporterService.importCodeSets(admin, loadTestFile('simpleAndComplexCodeSets'))

        then:
        codeSets
        codeSets.size() == 2

        when:
        ObjectDiff simpleDiff = codeSetService.getDiffForModels(simpleCodeSet, codeSets[0])
        ObjectDiff complexDiff = codeSetService.getDiffForModels(complexCodeSet, codeSets[1])

        then:
        !simpleDiff.objectsAreIdentical() // Expected diff failure on dateFinalised
        !complexDiff.objectsAreIdentical()

        simpleDiff.numberOfDiffs == 1
        simpleDiff.diffs.find {it.fieldName == 'dateFinalised'}

        complexDiff.numberOfDiffs == 1
        complexDiff.diffs.find {it.fieldName == 'dateFinalised'}
    }

    void 'test export multiple CodeSets'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 2
        codeSetExporterService.canExportMultipleDomains()

        when:
        String exported = exportModels([simpleCodeSetId, complexCodeSetId])

        then:
        validateExportedModels('simpleAndComplexCodeSets', exported.replace(/Mauro Data Mapper/, 'Test Authority'))
    }
}
