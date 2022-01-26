/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.terminology.item.term

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class TermRelationshipSpec extends ModelItemSpec<TermRelationship> implements DomainUnitTest<TermRelationship> {

    Terminology terminology
    Term term1, term2
    TermRelationshipType relationshipType

    def setup() {
        mockDomains(Terminology, TermRelationship, TermRelationshipType, Folder, Term, Authority)

        Folder testFolder = new Folder(createdBy: StandardEmailAddress.UNIT_TEST, label: 'testFolder')
        checkAndSave(testFolder)

        term1 = new Term(code: 'TT01', definition: 'TT01', createdBy: StandardEmailAddress.UNIT_TEST)
        term2 = new Term(code: 'TT02', definition: 'TT01', createdBy: StandardEmailAddress.UNIT_TEST)
        relationshipType = new TermRelationshipType(label: 'child of', createdBy: StandardEmailAddress.UNIT_TEST, displayLabel: 'Child Of')

        terminology = new Terminology(label: 'test terminology', createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder,
                                      authority: testAuthority)
        terminology.addToTerms(term1).addToTerms(term2).addToTermRelationshipTypes(relationshipType)
        checkAndSave(terminology)
    }

    @Override
    Model getOwningModel() {
        terminology
    }

    @Override
    String getModelFieldName() {
        'sourceTerm'
    }

    @Override
    void wipeModel() {
        domain.breadcrumbTree = null
        domain.sourceTerm = null
    }

    @Override
    void setModel(TermRelationship domain, Model model) {
        domain.sourceTerm = term1
    }

    @Override
    TermRelationship createValidDomain(String label) {
        def domain = new TermRelationship(createdBy: StandardEmailAddress.UNIT_TEST, sourceTerm: term1, targetTerm: term2,
                                          relationshipType: relationshipType, label: label)
        domain
    }

    @Override
    void setValidDomainOtherValues() {
        domain.sourceTerm = term1
        domain.targetTerm = term2
        domain.relationshipType = relationshipType
    }

    @Override
    TermRelationship findById() {
        TermRelationship.findById(domain.id)
    }

    @Override
    void wipeBasicConstrained() {
        super.wipeBasicConstrained()
        domain.aliasesString = null
        domain.relationshipType = null
    }

    @Override
    void verifyDomainOtherConstraints(TermRelationship domain) {
        assert domain.targetTerm.id == term2.id
        assert domain.sourceTerm.id == term1.id
        assert domain.relationshipType.id == relationshipType.id
    }

    @Override
    int getExpectedConstrainedBlankErrors() {
        5
    }

    @Override
    int getExpectedConstrainedErrors() {
        3 // label, path, relationshiptype
    }

    @Override
    void verifyBlankConstraints() {
        assert domain.errors.getFieldError('description').code == 'blank'
        assert domain.errors.getFieldError('aliasesString').code == 'blank'
        // #4 == breadcrumbtree.label
    }

    @Override
    String getExpectedNewlineLabel() {
        'child of'
    }

    void 'test to make sure source and target cannot be the same'() {
        given:
        TermRelationshipType type2 = new TermRelationshipType(label: 'parent to', createdBy: StandardEmailAddress.UNIT_TEST)
        terminology.addToTermRelationshipTypes(type2)
        checkAndSave(terminology)

        when:
        domain.sourceTerm = term1
        domain.targetTerm = term1
        domain.relationshipType = type2
        domain.createdBy = editor
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)

    }
}
