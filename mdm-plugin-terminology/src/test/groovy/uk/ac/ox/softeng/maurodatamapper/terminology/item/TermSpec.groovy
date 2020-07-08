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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError
import spock.lang.Ignore

/**
 * @since 30/04/2018
 */
class TermSpec extends ModelItemSpec<Term> implements DomainUnitTest<Term> {

    Terminology terminology
    TermRelationshipType childOf
    TermRelationshipType parentTo

    def setup() {
        mockDomains(Terminology, TermRelationship, TermRelationshipType)
        terminology = new Terminology(label: 'test terminology', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder)
        childOf =
            new TermRelationshipType(label: 'child of', createdBy: StandardEmailAddress.UNIT_TEST, displayLabel: 'Child Of', childRelationship: true)
        parentTo = new TermRelationshipType(label: 'parent to', createdBy: StandardEmailAddress.UNIT_TEST, displayLabel: 'Parent To',
                                            parentalRelationship: true)
        terminology.addToTermRelationshipTypes(childOf).addToTermRelationshipTypes(parentTo)
        checkAndSave(terminology)
    }

    @Override
    Model getOwningModel() {
        terminology
    }

    @Override
    String getModelFieldName() {
        'terminology'
    }

    @Override
    void wipeModel() {
        domain.terminology = null
        domain.breadcrumbTree = null
    }

    @Override
    void wipeBasicConstrained() {
        super.wipeBasicConstrained()
        domain.code = null
        domain.definition = null
    }

    void setBasicConstrainedBlank() {
        domain.code = ''
        domain.definition = ''
        domain.description = ''
        domain.aliasesString = ''
    }

    @Override
    void setModel(Term domain, Model model) {
        domain.terminology = model as Terminology
    }

    @Override
    Term createValidDomain(String label) {
        def domain = new Term(code: label.find(/(\w+):?/) {it[1]}, createdBy: StandardEmailAddress.UNIT_TEST, definition: 'A part of TT',
                              label: label)
        terminology.addToTerms(domain)
        domain
    }

    @Override
    void setValidDomainOtherValues() {
        domain.code = 'TT01'
        domain.definition = 'A part of TT'
        terminology.addToTerms(domain)
    }

    @Override
    Term findById() {
        Term.findById(domain.id)
    }

    @Override
    void verifyDomainOtherConstraints(Term domain) {
        assert domain.code == 'TT01'
        assert domain.definition == 'A part of TT'
        assert domain.terminology.id == terminology.id
    }

    @Override
    void verifyBlankConstraints() {
        assert domain.errors.getFieldError('label').code == 'nullable'
        assert domain.errors.getFieldError('code').code == 'blank'
        assert domain.errors.getFieldError('definition').code == 'blank'
        assert domain.errors.getFieldError('description').code == 'blank'
        assert domain.errors.getFieldError('aliasesString').code == 'blank'
        // #4 == breadcrumbtree.label
    }

    @Override
    int getExpectedConstrainedErrors() {
        3 // code and definition and label
    }

    @Override
    int getExpectedConstrainedBlankErrors() {
        5
    }

    @Override
    int getExpectedBaseLevelOfDiffs() {
        1 // code
    }

    void 'test unique label naming with validation done at terminology level'() {
        given:
        setValidDomainValues()
        Terminology localTerminology = new Terminology(label: 'local test terminology', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder)
        terminology.removeFromTerms(domain)
        localTerminology.addToTerms(domain)

        expect: 'domain and terminology are currently valid'
        check(localTerminology)
        check(domain)

        when: 'adding term with same label to existing'
        localTerminology.addToTerms(code: domain.code, createdBy: StandardEmailAddress.UNIT_TEST, definition: 'New definition')
        check(localTerminology)

        then: 'terminology should not be valid as its never been saved'
        thrown(InternalSpockError)
        localTerminology.hasErrors()
        localTerminology.errors.allErrors.size() == 1
        localTerminology.errors.fieldErrors.any {'code' in it.arguments && it.field == 'terms' && it.code == 'invalid.unique.values.message'}
    }

    void 'test unique label naming with validation done at term level'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(terminology)

        when: 'adding term with same label to existing'
        terminology.addToTerms(code: domain.code, createdBy: StandardEmailAddress.UNIT_TEST, definition: 'New definition')
        check(terminology)

        then: 'terminology should not be valid'
        thrown(InternalSpockError)
        terminology.errors.allErrors.size() == 2
        terminology.errors.fieldErrors.any {it.field == 'terms[0].code' && it.code == 'default.not.unique.message' && it.rejectedValue == 'TT01'}
        terminology.errors.fieldErrors.any {it.field == 'terms[1].code' && it.code == 'default.not.unique.message' && it.rejectedValue == 'TT01'}
    }

    void 'test unique label naming across terminologies'() {
        given:
        setValidDomainValues()
        Terminology terminology2 = new Terminology(label: 'test terminology 2', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(terminology)
        checkAndSave(terminology2)

        when: 'adding term with same label to existing'
        terminology2.addToTerms(code: domain.code, createdBy: StandardEmailAddress.UNIT_TEST, definition: 'New definition')

        then: 'terminologies should be valid'
        checkAndSave(terminology)
        checkAndSave(terminology2)

        when: 'adding multiple terms with same label'
        terminology2.addToTerms(code: 'TT02', createdBy: StandardEmailAddress.UNIT_TEST, definition: 'New definition')
        terminology2.addToTerms(code: 'TT03', createdBy: StandardEmailAddress.UNIT_TEST, definition: 'New definition')
        terminology2.addToTerms(code: 'TT02', createdBy: StandardEmailAddress.UNIT_TEST, definition: 'New definition')


        then: 'terminology should be invalid'
        checkAndSave(terminology)

        when:
        checkAndSave(terminology2)

        then:
        thrown(InternalSpockError)
        terminology2.hasErrors()
    }

    void 'test unique relationships'() {
        given:
        setValidDomainValues()
        Term term2 = new Term(code: 'TT02', definition: 'TT02', createdBy: StandardEmailAddress.UNIT_TEST)
        terminology.addToTerms(term2)
        checkAndSave(terminology)

        when:
        domain.addToSourceTermRelationships(targetTerm: term2, relationshipType: childOf, createdBy: StandardEmailAddress.UNIT_TEST)
        domain.addToTargetTermRelationships(sourceTerm: term2, relationshipType: parentTo, createdBy: StandardEmailAddress.UNIT_TEST)

        then:
        checkAndSave(domain)
        TermRelationship.count() == 2

        when:
        term2.addToTargetTermRelationships(sourceTerm: domain, relationshipType: childOf, createdBy: StandardEmailAddress.UNIT_TEST)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()

    }

    @Ignore('To be moved into service level spec')
    void 'test no tree structure item'() {
        given:
        setValidDomainValues()

        expect:
        !domain.isParentTerm()
        !domain.isChildTerm()

        and:
        domain.hasNoHierarchy()

        and:
        domain.isRootTerm()
        domain.depth == Integer.MAX_VALUE
    }

    @Ignore('To be moved into service level spec')
    void 'test parent and child item from parental relationship'() {
        given:
        setValidDomainValues()
        Term term2 = new Term(code: 'TT02', definition: 'TT02', createdBy: StandardEmailAddress.UNIT_TEST)
        terminology.addToTerms(term2)
        checkAndSave(terminology)

        when:
        TermRelationship relationship = new TermRelationship(sourceTerm: domain, targetTerm: term2, relationshipType: parentTo,
                                                             createdBy: StandardEmailAddress.UNIT_TEST)
        domain.addToSourceTermRelationships(relationship)
        term2.addToTargetTermRelationships(relationship)

        then:
        checkAndSave(domain)
        checkAndSave(term2)

        and:
        domain.isParentTerm()
        !domain.isChildTerm()

        and:
        !domain.hasNoHierarchy()

        and:
        domain.isRootTerm()
        domain.depth == 1

        and:
        !term2.isParentTerm()
        term2.isChildTerm()

        and:
        !term2.hasNoHierarchy()

        and:
        !term2.isRootTerm()
        term2.depth == 2
    }

    @Ignore('To be moved into service level spec')
    void 'test parent and child item from child relationship'() {
        given:
        setValidDomainValues()
        Term term2 = new Term(code: 'TT02', definition: 'TT02', createdBy: StandardEmailAddress.UNIT_TEST)
        terminology.addToTerms(term2)
        checkAndSave(terminology)

        when:
        TermRelationship relationship = new TermRelationship(sourceTerm: term2, targetTerm: domain, relationshipType: childOf,
                                                             createdBy: StandardEmailAddress.UNIT_TEST)
        domain.addToTargetTermRelationships(relationship)
        term2.addToSourceTermRelationships(relationship)

        then:
        checkAndSave(domain)
        checkAndSave(term2)

        and:
        domain.isParentTerm()
        !domain.isChildTerm()

        and:
        !domain.hasNoHierarchy()

        and:
        domain.isRootTerm()
        domain.depth == 1

        and:
        !term2.isParentTerm()
        term2.isChildTerm()

        and:
        !term2.hasNoHierarchy()

        and:
        !term2.isRootTerm()
        term2.depth == 2
    }
}
