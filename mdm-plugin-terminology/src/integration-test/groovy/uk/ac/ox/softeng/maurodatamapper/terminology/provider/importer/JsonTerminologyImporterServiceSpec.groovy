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

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.DataBindTerminologyImporterProviderServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getDEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getDEVELOPMENT

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
class JsonTerminologyImporterServiceSpec extends DataBindTerminologyImporterProviderServiceSpec<TerminologyJsonImporterService>
    implements JsonComparer {

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

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest', createdBy: admin.emailAddress)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: admin.emailAddress)
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value', createdBy: admin.emailAddress)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: admin.emailAddress).addToRuleRepresentations(language: 'e', representation:
            'a+b', createdBy: admin.emailAddress)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: admin,
                                                         targetMultiFacetAwareItem: Term.findByCode('STT01'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: admin.emailAddress)

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

    }

    void 'PG02 test importing a Terminology and propagating existing information'() {

        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        Term term1 = new Term(createdBy: DEVELOPMENT, code: 'PPG01', definition: 'propagationTestTerm 1')
        Term term2 = new Term(createdBy: DEVELOPMENT, code: 'PPG02', definition: 'propagationTestTerm 02')
        Term term3 = new Term(createdBy: DEVELOPMENT, code: 'PPG03', definition: 'propagationTestTerm 03')
        simpleTerminology.addToTerms(term1)
            .addToTerms(term2)
            .addToTerms(term3)

        TermRelationshipType broaderThan = new TermRelationshipType(createdBy: DEVELOPMENT, label: 'broaderThan', displayLabel: 'Broader Than',
                                                                    parentalRelationship: true)
        TermRelationshipType narrowerThan = new TermRelationshipType(createdBy: DEVELOPMENT, label: 'narrowerThan', displayLabel: 'Narrower Than')

        simpleTerminology.addToTermRelationshipTypes(broaderThan)
            .addToTermRelationshipTypes(narrowerThan)

        TermRelationship relationshipNarrow = new TermRelationship(createdBy: DEVELOPMENT, sourceTerm: term1, targetTerm: term2, relationshipType:
            narrowerThan)
        TermRelationship relationshipBroader = new TermRelationship(createdBy: DEVELOPMENT, sourceTerm: term3, targetTerm: term1,
                                                                    relationshipType: broaderThan)
        term1.addToSourceTermRelationships(relationshipNarrow)
        term2.addToTargetTermRelationships(relationshipNarrow)

        term3.addToSourceTermRelationships(relationshipBroader)
        term1.addToTargetTermRelationships(relationshipBroader)

        checkAndSave(simpleTerminology)

        when:
        Terminology terminology = importAndSave(loadTestFile('simpleTerminology'))

        then:
        terminology.terms.size() == 5
        terminology.terms.count { it.code.matches('PPG(.*)') } == 3
        Term testTerm1 = terminology.terms.find { it.label == term1.label }
        testTerm1.sourceTermRelationships.find{it.relationshipType == relationshipNarrow.relationshipType}
        Term testTerm2 = terminology.terms.find { it.label == term2.label }
        testTerm2.targetTermRelationships.find{it.relationshipType == relationshipNarrow.relationshipType}
        terminology.termRelationshipTypes.find { it.label == broaderThan.label }
        terminology.termRelationshipTypes.find { it.label == narrowerThan.label }
    }
}
