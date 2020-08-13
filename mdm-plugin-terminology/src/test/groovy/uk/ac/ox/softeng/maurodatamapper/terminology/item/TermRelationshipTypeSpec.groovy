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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

class TermRelationshipTypeSpec extends ModelItemSpec<TermRelationshipType> implements DomainUnitTest<TermRelationshipType> {

    Terminology terminology
    Folder testFolder

    def setup() {
        mockDomains(Terminology, TermRelationship, TermRelationshipType, Folder, Term)
        testFolder = new Folder(createdBy: StandardEmailAddress.UNIT_TEST, label: 'testFolder')
        checkAndSave(testFolder)

        terminology =
            new Terminology(label: 'test terminology', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder, authority: testAuthority)
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
    void setModel(TermRelationshipType domain, Model model) {
        domain.terminology = model as Terminology
    }

    @Override
    TermRelationshipType createValidDomain(String label) {
        def domain = new TermRelationshipType(createdBy: StandardEmailAddress.UNIT_TEST, label: label, displayLabel: 'Child Of')
        terminology.addToTermRelationshipTypes(domain)
        domain
    }

    @Override
    void setValidDomainOtherValues() {
        domain.displayLabel = 'Child Of'
        terminology.addToTermRelationshipTypes(domain)
    }

    @Override
    TermRelationshipType findById() {
        TermRelationshipType.findById(domain.id)
    }

    @Override
    void verifyDomainOtherConstraints(TermRelationshipType domain) {
        assert domain.label == 'test'
        assert domain.terminology.id == terminology.id
        assert domain.displayLabel == 'Child Of'
    }

    void 'test unique label naming'() {
        given:
        setValidDomainValues()

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(terminology)

        when: 'adding type with same label to existing'
        terminology.
            addToTermRelationshipTypes(label: domain.label, createdBy: StandardEmailAddress.UNIT_TEST, displayLabel: 'Another Term Relationship')
        checkAndSave(terminology)

        then: 'terminology should not be valid'
        thrown(InternalSpockError)
        terminology.errors.allErrors.size() == 2
        terminology.errors.fieldErrors.any {it.field.contains('label') && it.code.contains('unique')}
    }

    void 'test unique label naming across terminologies'() {
        given:
        setValidDomainValues()
        Terminology terminology2 = new Terminology(label: 'test terminology 2', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder,
                                                   authority: testAuthority)

        expect: 'domain is currently valid'
        checkAndSave(domain)
        checkAndSave(terminology)
        checkAndSave(terminology2)

        when: 'adding type with same label to existing'
        terminology2.
            addToTermRelationshipTypes(label: domain.label, createdBy: StandardEmailAddress.UNIT_TEST, displayLabel: 'Another Term Relationship')

        then: 'terminologies should be valid'
        checkAndSave(terminology)
        checkAndSave(terminology2)

        when: 'adding multiple types with same label'
        terminology2.addToTerms(label: 'parent of', createdBy: StandardEmailAddress.UNIT_TEST)
        terminology2.addToTerms(label: 'child to', createdBy: StandardEmailAddress.UNIT_TEST)
        terminology2.addToTerms(label: 'parent of', createdBy: StandardEmailAddress.UNIT_TEST)


        then: 'datamodel should be invalid'
        checkAndSave(terminology)

        when:
        checkAndSave(terminology2)

        then:
        thrown(InternalSpockError)
    }
}
