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
package uk.ac.ox.softeng.maurodatamapper.terminology.term

import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.INTEGRATION_TEST

@Slf4j
@Integration
@Rollback
class TermServiceSpec extends BaseTerminologyIntegrationSpec {

    TermService termService

    @Override
    void setupDomainData() {

        Term term1 = new Term(code: 'IT01', definition: 'Integration Test 01', createdBy: INTEGRATION_TEST, terminology: simpleTerminology)
        Term term2 = new Term(code: 'IT02', definition: 'Integration Test 02', createdBy: INTEGRATION_TEST, terminology: simpleTerminology)
        Term term3 = new Term(code: 'IT03', definition: 'Integration Test 03', createdBy: INTEGRATION_TEST, terminology: simpleTerminology)

        checkAndSave(term1)
        checkAndSave(term2)
        checkAndSave(term3)

        id = term1.id
    }

    void "test get"() {
        setupData()

        expect:
        termService.get(id) != null
    }

    void "test list"() {
        setupData()

        when:
        List<Term> termList = termService.list(max: 3, offset: 101, sort: 'code')

        then:
        termList.size() == 3

        when:
        def tm1 = termList[0]
        def tm2 = termList[1]

        then:
        tm1.code == 'IT01'

        and:
        tm2.code == 'IT02'

    }

    void "test count"() {
        setupData()

        expect:
        termService.count() == 106
    }

    void "test delete"() {
        setupData()

        expect:
        termService.count() == 106

        when:
        termService.delete(id)
        sessionFactory.currentSession.flush()

        then:
        Term.count() == 105
    }

    void 'test findAllByTerminologyId'() {
        setupData()

        when:
        List<Term> terms = termService.findAllByTerminologyId(simpleTerminology.id)

        then:
        terms.size() == 5
    }

    void 'test findByTerminologyIdAndId'() {
        given:
        setupData()

        when:
        Term term = termService.findByTerminologyIdAndId(simpleTerminology.id, id)

        then:
        term
        term.code == 'IT01'
    }
}
