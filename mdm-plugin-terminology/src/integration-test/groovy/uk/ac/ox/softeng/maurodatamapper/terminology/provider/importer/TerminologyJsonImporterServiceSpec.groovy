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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.DataBindTerminologyImporterProviderServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
class TerminologyJsonImporterServiceSpec extends DataBindTerminologyImporterProviderServiceSpec<TerminologyJsonImporterService> implements JsonComparer {

    private static final String CANNOT_IMPORT_EMPTY_CONTENT_CODE = 'JIS02'
    private static final String CANNOT_IMPORT_JSON_CODE = 'JIS03'

    TerminologyJsonImporterService terminologyJsonImporterService

    @Override
    TerminologyJsonImporterService getImporterService() {
        terminologyJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    void 'GH77 : test trimming issue'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndSave(loadTestFile('trimmingIssue'))

        then:
        terminology.termRelationshipTypes.size() == 1
        terminology.terms.size() == 2

        when:
        Term a = terminology.terms.find { it.code == 'A' }
        Term b = terminology.terms.find { it.code == 'B' }

        then:
        a
        a.definition == 'Alpha'
        a.depth == 1

        and:
        b
        b.definition == 'Beta'
        b.depth == 2

        and:
        a.sourceTermRelationships.size() == 0
        a.targetTermRelationships.size() == 1

        and:
        b.sourceTermRelationships.size() == 1
        b.targetTermRelationships.size() == 0
    }

    void 'PG01 test propagatingCatalogueItemElements'() {

        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        Terminology terminology = Terminology.findById(simpleTerminologyId)

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest',
                                                   createdBy: StandardEmailAddress.INTEGRATION_TEST)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value',
                                             createdBy: StandardEmailAddress.INTEGRATION_TEST)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: StandardEmailAddress.INTEGRATION_TEST)
            .addToRuleRepresentations(language: 'e', representation:
                'a+b', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: StandardEmailAddress.INTEGRATION_TEST,
                                                         targetMultiFacetAwareItem: Term.findByCode('STT01'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: StandardEmailAddress.INTEGRATION_TEST)

        terminology.addToAnnotations(testAnnotation)
        terminology.addToClassifiers(testClassifier)
        terminology.addToMetadata(testMetadata)
        terminology.addToRules(testRule)
        terminology.addToSemanticLinks(testSemanticLink)
        terminology.addToReferenceFiles(testReferenceFile)

        checkAndSave(testClassifier)
        checkAndSave(terminology)

        when:
        Terminology term = importAndSave(loadTestFile('simpleTerminology'))

        then:
        term.annotations.find { it.label == testAnnotation.label }
        term.classifiers.find { it.label == testClassifier.label }
        term.metadata.find { it.namespace == testMetadata.namespace }
        term.rules.find { it.name == testRule.name }
        term.semanticLinks.find { it.targetMultiFacetAwareItemId == testSemanticLink.targetMultiFacetAwareItemId }
        term.semanticLinks.find { it.multiFacetAwareItemDomainType == testSemanticLink.multiFacetAwareItemDomainType }
        term.referenceFiles.find { it.fileName == testReferenceFile.fileName }

        cleanup:
        cleanupParameters()
    }

    void 'PG02 : test propagating child content'() {

        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        Terminology terminology = Terminology.findById(complexTerminologyId)
        Term term = terminology.terms.find { it.code == 'CTT1' }

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest',
                                                   createdBy: StandardEmailAddress.INTEGRATION_TEST)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: StandardEmailAddress.INTEGRATION_TEST).save()
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value',
                                             createdBy: StandardEmailAddress.INTEGRATION_TEST)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: StandardEmailAddress.INTEGRATION_TEST)
            .addToRuleRepresentations(language: 'e', representation:
                'a+b', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: StandardEmailAddress.INTEGRATION_TEST,
                                                         targetMultiFacetAwareItem: Term.findByCode('STT01'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: StandardEmailAddress.INTEGRATION_TEST)

        term.addToAnnotations(testAnnotation)
        term.addToClassifiers(testClassifier)
        term.addToMetadata(testMetadata)
        term.addToRules(testRule)
        term.addToSemanticLinks(testSemanticLink)
        term.addToReferenceFiles(testReferenceFile)

        checkAndSave(term)

        term = terminology.terms.find { it.code == 'CTT2' }
        term.description = 'Some interesting thing we should preserve'

        checkAndSave(term)

        term = terminology.terms.find { it.code == 'CTT101' }
        term.description = 'Some interesting thing we should lose'

        checkAndSave(term)

        when:
        Terminology tm = importAndSave(loadTestFile('complexTerminology'))
        term = tm.terms.find { it.code == 'CTT1' }

        then:
        term.metadata.find { it.namespace == testMetadata.namespace }
        term.annotations.find { it.label == testAnnotation.label }
        term.classifiers.find { it.label == testClassifier.label }
        term.rules.find { it.name == testRule.name }
        term.semanticLinks.find { it.targetMultiFacetAwareItemId == testSemanticLink.targetMultiFacetAwareItemId }
        term.semanticLinks.find { it.multiFacetAwareItemDomainType == testSemanticLink.multiFacetAwareItemDomainType }
        term.referenceFiles.find { it.fileName == testReferenceFile.fileName }

        when:
        term = tm.terms.find { it.code == 'CTT2' }

        then:
        term.description == 'Some interesting thing we should preserve'

        when:
        term = tm.terms.find { it.code == 'CTT101' }

        then: 'description is not overwritten as it was included in the import'
        term.description == 'Example of truncated term label when code and definition are the same'

        cleanup:
        cleanupParameters()
    }

    void 'test multi-import invalid Terminology content'() {
        expect:
        importerService.canImportMultipleDomains()

        when: 'given empty content'
        importModels(''.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_EMPTY_CONTENT_CODE

        when: 'given an empty JSON map'
        importModels('{}'.bytes)

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given neither models list or model map (backwards compatibility)'
        importModels(loadTestFile('exportMetadataOnly'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given an empty model map (backwards compatibility)'
        importModels(loadTestFile('emptyTerminology'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given an empty models list'
        importModels(loadTestFile('emptyTerminologyList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE
    }

    void 'test multi-import invalid Terminologies'() {
        given:
        setupData()

        expect:
        importerService.canImportMultipleDomains()

        when: 'given an invalid model map (backwards compatibility)'
        importModels(loadTestFile('invalidTerminology'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given a single invalid model'
        importModels(loadTestFile('invalidTerminologyInList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given multiple invalid models'
        importModels(loadTestFile('invalidTerminologies'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        // when: 'not given export metadata'
        // importModels(loadTestFile('noExportMetadata'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import single Terminology (backwards compatibility)'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleTerminology'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import single Terminology'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleTerminologyInList'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import multiple Terminologies'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId, complexTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleAndComplexTerminologies'))

        then:
        imported
        imported.size() == 2

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies[0], imported[0])
        ObjectDiff complexDiff = terminologyService.getDiffForModels(terminologies[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import Terminologies with invalid models'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId, complexTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleAndInvalidTerminologies'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies[0], imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleComplexAndInvalidTerminologies'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = terminologyService.getDiffForModels(terminologies[0], imported[0])
        ObjectDiff complexDiff = terminologyService.getDiffForModels(terminologies[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import Terminologies with duplicates'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> dataModels = clearExpectedDiffsFromModels([simpleTerminologyId, complexTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleDuplicateTerminologies'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(dataModels[0], imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleAndComplexDuplicateTerminologies'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = terminologyService.getDiffForModels(dataModels[0], imported[0])
        ObjectDiff complexDiff = terminologyService.getDiffForModels(dataModels[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }
}
