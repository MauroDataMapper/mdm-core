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
package uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
class BootstrapModels {

    public static final String COMPLEX_TERMINOLOGY_NAME = 'Complex Test Terminology'
    public static final String COMPLEX_CODESET_NAME = 'Complex Test CodeSet'
    public static final String SIMPLE_TERMINOLOGY_NAME = 'Simple Test Terminology'
    public static final String SIMPLE_CODESET_NAME = 'Simple Test CodeSet'
    public static final String UNFINALISED_CODESET_NAME = 'Unfinalised Simple Test CodeSet'

    static Terminology buildAndSaveSimpleTerminology(MessageSource messageSource, Folder folder, Authority authority) {
        Terminology terminology = new Terminology(createdBy: DEVELOPMENT, label: SIMPLE_TERMINOLOGY_NAME, folder: folder,
                                                  author: 'Test Bootstrap', organisation: 'Oxford BRC', authority: authority)

        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier simple',
                                                             readableByAuthenticatedUsers: true)
        checkAndSave(messageSource, classifier)

        terminology.addToClassifiers(classifier)
        checkAndSave(messageSource, terminology)

        terminology.addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com/simple', key: 'mdk1', value: 'mdv1')
                   .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com', key: 'mdk2', value: 'mdv2')
                   .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com/simple', key: 'mdk2', value: 'mdv2')

        terminology.addToTerms(createdBy: DEVELOPMENT, code: 'STT01', definition: 'Simple Test Term 01')
        terminology.addToTerms(createdBy: DEVELOPMENT, code: 'STT02', definition: 'Simple Test Term 02')

        checkAndSave(messageSource, terminology)

        terminology
    }

    static Terminology buildAndSaveComplexTerminology(MessageSource messageSource, Folder folder, TerminologyService terminologyService,
                                                      Authority authority) {
        Terminology terminology = new Terminology(createdBy: DEVELOPMENT, label: COMPLEX_TERMINOLOGY_NAME, folder: folder,
                                                  author: 'Test Bootstrap', organisation: 'Oxford BRC', authority: authority)

        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier',
                                                             readableByAuthenticatedUsers: true)
        Classifier classifier1 = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier2',
                                                              readableByAuthenticatedUsers: true)
        checkAndSave(messageSource, classifier)
        checkAndSave(messageSource, classifier1)
        checkAndSave(messageSource, terminology)

        terminology.addToRules(name: 'Bootstrapped Functional Test Rule',
                               description: 'Functional Test Description',
                               createdBy: DEVELOPMENT)

        checkAndSave(messageSource, terminology)

        terminology.addToClassifiers(classifier)
                   .addToClassifiers(classifier1)
                   .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com/simple', key: 'mdk1', value: 'mdv1')
                   .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com', key: 'mdk2', value: 'mdv2')
                   .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com/simple', key: 'mdk2', value: 'mdv2')
                   .addToAnnotations(createdBy: DEVELOPMENT, label: 'test annotation 1')
                   .addToAnnotations(createdBy: DEVELOPMENT, label: 'test annotation 2', description: 'with description')

        Term top = new Term(createdBy: DEVELOPMENT, code: 'CTT00', definition: 'Complex Test Term 00',
                            description: 'This is a very important description', url: 'https://google.co.uk')
        terminology.addToTerms(top)
        List<Term> terms = [top]
        (1..101).each {
            Term t = new Term(createdBy: DEVELOPMENT, code: "CTT$it", definition: "Complex Test Term $it")
            terminology.addToTerms(t)
            terms += t
        }

        terms[101].tap {
            definition = code
            description = 'Example of truncated term label when code and definition are the same'
        }

        checkAndSave(messageSource, terminology)

        TermRelationshipType is = new TermRelationshipType(createdBy: DEVELOPMENT, label: 'is-a', displayLabel: 'Is A')
        TermRelationshipType isPartOf = new TermRelationshipType(createdBy: DEVELOPMENT, label: 'is-a-part-of', displayLabel: 'Is A Part Of',
                                                                 childRelationship: true)
        TermRelationshipType broaderThan = new TermRelationshipType(createdBy: DEVELOPMENT, label: 'broaderThan', displayLabel: 'Broader Than',
                                                                    parentalRelationship: true)
        TermRelationshipType narrowerThan = new TermRelationshipType(createdBy: DEVELOPMENT, label: 'narrowerThan', displayLabel: 'Narrower Than')

        terminology.addToTermRelationshipTypes(is)
                   .addToTermRelationshipTypes(isPartOf)
                   .addToTermRelationshipTypes(broaderThan)
                   .addToTermRelationshipTypes(narrowerThan)

        checkAndSave(messageSource, terminology)

        (1..99).each {
            Term source = terms[it]
            Term target = it % 10 == 0 ? top : terms[((it / 10) as Integer) * 10]
            TermRelationship relationship = new TermRelationship(createdBy: DEVELOPMENT, sourceTerm: source, targetTerm: target,
                                                                 relationshipType: isPartOf)
            source.addToSourceTermRelationships(relationship)
            target.addToTargetTermRelationships(relationship)
        }

        TermRelationship relationship = new TermRelationship(createdBy: DEVELOPMENT, sourceTerm: terms[100], targetTerm: top, relationshipType: is)
        terms[100].addToSourceTermRelationships(relationship)
        top.addToTargetTermRelationships(relationship)

        (2..9).each {
            relationship =
                new TermRelationship(createdBy: DEVELOPMENT, sourceTerm: terms[it], targetTerm: terms[it - 1], relationshipType: narrowerThan)
            terms[it].addToSourceTermRelationships(relationship)
            terms[it - 1].addToTargetTermRelationships(relationship)
        }
        (11..18).each {
            relationship =
                new TermRelationship(createdBy: DEVELOPMENT, sourceTerm: terms[it], targetTerm: terms[it + 1], relationshipType: broaderThan)
            terms[it].addToSourceTermRelationships(relationship)
            terms[it + 1].addToTargetTermRelationships(relationship)
        }

        checkAndSave(messageSource, terminology)
        if (terminologyService) {
            terminologyService.updateTermDepths(terminology, true)
        }
        checkAndSave(messageSource, terminology)

        narrowerThan.addToRules(name: 'Bootstrapped Functional Test Rule',
                                description: 'Functional Test Description',
                                createdBy: DEVELOPMENT)
        terminology.findTermByCode('CTT1').addToRules(name: 'Bootstrapped Functional Test Rule',
                                                      description: 'Functional Test Description',
                                                      createdBy: DEVELOPMENT)
        terminology.findTermByCode('CTT1').sourceTermRelationships[0]
            .addToRules(name: 'Bootstrapped Functional Test Rule',
                        description: 'Functional Test Description',
                        createdBy: DEVELOPMENT)

        checkAndSave(messageSource, terminology)

        terminology
    }

    static CodeSet buildAndSaveSimpleCodeSet(MessageSource messageSource, Folder folder, Authority authority) {

        CodeSet codeSet = new CodeSet(createdBy: DEVELOPMENT, label: SIMPLE_CODESET_NAME, folder: folder,
                                      author: 'Test Bootstrap', organisation: 'Oxford BRC', authority: authority)

        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier',
                                                             readableByAuthenticatedUsers: true)

        checkAndSave(messageSource, classifier)
        codeSet.addToClassifiers(classifier)
        checkAndSave(messageSource, codeSet)

        Terminology simpleTestTerminology = Terminology.findByLabel(SIMPLE_TERMINOLOGY_NAME)

        if (!simpleTestTerminology) {
            simpleTestTerminology = buildAndSaveSimpleTerminology(messageSource, folder, authority)
        }

        simpleTestTerminology.terms.each {
            codeSet.addToTerms(it)
        }

        codeSet.finalised = true
        //Truncate the dateFinalised to milliseconds to avoid a Diff failure when test exporting and reimporting a CodeSet
        codeSet.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
        codeSet.modelVersion = Version.from('1.0.0')

        checkAndSave(messageSource, codeSet)

        codeSet
    }

    static CodeSet buildAndSaveComplexCodeSet(MessageSource messageSource, Folder folder, TerminologyService terminologyService, Authority authority) {
        CodeSet codeSet = new CodeSet(createdBy: DEVELOPMENT, label: COMPLEX_CODESET_NAME, folder: folder, author: 'Test Bootstrap', organisation: 'Oxford BRC',
                                      authority: authority)
        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier', readableByAuthenticatedUsers: true)
        checkAndSave(messageSource, classifier)
        codeSet.addToClassifiers(classifier)
        checkAndSave(messageSource, codeSet)

        codeSet.addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com/simple', key: 'mdk1', value: 'mdv1')
               .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com', key: 'mdk2', value: 'mdv2')
               .addToMetadata(createdBy: DEVELOPMENT, namespace: 'terminology.test.com/simple', key: 'mdk2', value: 'mdv2')

        Terminology simpleTestTerminology = Terminology.findByLabel(SIMPLE_TERMINOLOGY_NAME) ?: buildAndSaveSimpleTerminology(messageSource, folder, authority)
        simpleTestTerminology.terms.each { codeSet.addToTerms(it) }

        Terminology complexTestTerminology = Terminology.findByLabel(COMPLEX_TERMINOLOGY_NAME) ?:
                                             buildAndSaveComplexTerminology(messageSource, folder, terminologyService, authority)
        complexTestTerminology.terms.each { codeSet.addToTerms(it) }

        codeSet.finalised = true
        // Truncate the dateFinalised to milliseconds to avoid a Diff failure when test exporting and reimporting a CodeSet
        codeSet.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
        codeSet.modelVersion = Version.from('1.0.0')

        checkAndSave(messageSource, codeSet)
        codeSet
    }

    static CodeSet buildAndSaveUnfinalisedCodeSet(MessageSource messageSource, Folder folder, Authority authority) {

        CodeSet codeSet = new CodeSet(createdBy: DEVELOPMENT, label: UNFINALISED_CODESET_NAME, folder: folder,
                                      author: 'Test Bootstrap', organisation: 'Oxford BRC', authority: authority)

        checkAndSave(messageSource, codeSet)

        Terminology simpleTestTerminology = Terminology.findByLabel(SIMPLE_TERMINOLOGY_NAME)

        if (!simpleTestTerminology) {
            simpleTestTerminology = buildAndSaveSimpleTerminology(messageSource, folder, authority)
        }

        simpleTestTerminology.terms.each {
            codeSet.addToTerms(it)
        }

        checkAndSave(messageSource, codeSet)

        codeSet.addToRules(name: 'Bootstrapped Functional Test Rule',
                           description: 'Functional Test Description',
                           createdBy: DEVELOPMENT)

        checkAndSave(messageSource, codeSet)

        codeSet
    }
}
