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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class TerminologySpec extends ModelSpec<Terminology> implements DomainUnitTest<Terminology> {

    def setup() {
        log.debug('Setting up TerminologySpec unit')
        mockDomains(Terminology, Term, TermRelationship, TermRelationshipType)
    }

    @Override
    void setValidDomainOtherValues() {
    }

    @Override
    void verifyDomainOtherConstraints(Terminology domain) {
    }

    @Override
    Terminology createValidDomain(String label) {
        new Terminology(folder: testFolder, label: label, createdBy: StandardEmailAddress.UNIT_TEST, authority: testAuthority)
    }

    @Override
    Terminology findById() {
        Terminology.findById(domain.id)
    }

    void 'test simple terminology valid'() {
        when:
        Terminology simple = BootstrapModels.buildAndSaveSimpleTerminology(messageSource, testFolder, testAuthority)

        then:
        check(simple)

        and:
        checkAndSave(simple)
    }

    void 'test complex terminology valid'() {
        when:
        Terminology complex = BootstrapModels.buildAndSaveComplexTerminology(messageSource, testFolder, null, testAuthority)

        then:
        check(complex)

        and:
        checkAndSave(complex)
    }

    void 'simple diff of label'() {
        when:
        def t1 = new Terminology(label: 'test model 1', folder: testFolder, authority: testAuthority)
        def t2 = new Terminology(label: 'test model 2', folder: testFolder, authority: testAuthority)
        Diff<Terminology> diff = t1.diff(t2, null)

        then:
        diff.getNumberOfDiffs() == 1

        when:
        t2.label = 'test model 1'
        diff = t1.diff(t2, null)

        then:
        diff.objectsAreIdentical()
    }
}
