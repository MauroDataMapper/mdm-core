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
package uk.ac.ox.softeng.maurodatamapper.terminology.term.item

import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.INTEGRATION_TEST

/**
 * @since 04/06/2018
 */
@Integration
@Rollback
@Slf4j
class TermRelationshipServiceSpec extends BaseTerminologyIntegrationSpec {

    TermRelationshipService termRelationshipService

    @Override
    void setupDomainData() {

        TermRelationshipType relationshipType1 = new TermRelationshipType(label: 'is-not-a', createdBy: INTEGRATION_TEST,
                                                                          terminology: simpleTerminology,
                                                                          displayLabel: 'Is Not A')
        TermRelationshipType relationshipType2 = new TermRelationshipType(label: 'is-not-narrower-than', createdBy: INTEGRATION_TEST,
                                                                          terminology: simpleTerminology, displayLabel: 'Is Not Narrower Than')
        Term term1 = new Term(code: 'IT01', definition: 'Integration Test 01', createdBy: INTEGRATION_TEST, terminology: simpleTerminology)
        Term term2 = new Term(code: 'IT02', definition: 'Integration Test 02', createdBy: INTEGRATION_TEST, terminology: simpleTerminology)
        Term term3 = new Term(code: 'IT03', definition: 'Integration Test 03', createdBy: INTEGRATION_TEST, terminology: simpleTerminology)

        checkAndSave(relationshipType1, relationshipType2, term1, term2, term3)

        TermRelationship relationship1 = new TermRelationship(sourceTerm: term1, targetTerm: term2, relationshipType: relationshipType1,
                                                              createdBy: INTEGRATION_TEST)
        TermRelationship relationship2 = new TermRelationship(sourceTerm: term1, targetTerm: term3, relationshipType: relationshipType2,
                                                              createdBy: INTEGRATION_TEST)
        TermRelationship relationship3 = new TermRelationship(sourceTerm: term2, targetTerm: term3, relationshipType: relationshipType1,
                                                              createdBy: INTEGRATION_TEST)

        checkAndSave(relationship1, relationship2, relationship3)

        id = relationship1.id
    }

    void "test get"() {
        setupData()

        expect:
        termRelationshipService.get(id) != null
    }

    void "test list"() {
        setupData()

        when:
        List<TermRelationship> termRelationships = termRelationshipService.list(max: 3, offset: 116, sort: 'dateCreated')

        then:
        termRelationships.size() == 3

    }

    void "test count"() {
        setupData()

        expect:
        termRelationshipService.count() == 119
    }

    void "test delete"() {
        setupData()

        expect:
        termRelationshipService.count() == 119

        when:
        termRelationshipService.delete(id)
        sessionFactory.currentSession.flush()

        then:
        TermRelationship.count() == 118
    }

    void 'test findAllByTermId'() {
        setupData()
        def id = Term.findByCode('IT01').id

        when:
        List<TermRelationship> termRelationships = termRelationshipService.findAllByTermId(id)

        then:
        termRelationships.size() == 2
    }

    void 'test findAllByTermIdAndType'() {
        given:
        setupData()
        def termId = Term.findByCode('IT02').id

        when:
        List<TermRelationship> termRelationships = termRelationshipService.findAllByTermIdAndType(termId, 'source')

        then:
        termRelationships.size() == 1
    }

    void 'test findAllByRelationshipTypeId'() {
        given:
        setupData()
        def relationshipTypeId = TermRelationshipType.findByLabel('is-not-a').id

        when:
        List<TermRelationship> termRelationships = termRelationshipService.findAllByRelationshipTypeId(relationshipTypeId)

        then:
        termRelationships.size() == 2
    }
}
