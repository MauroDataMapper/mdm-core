/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 04/06/2018
 */
@Integration
@Rollback
@Slf4j
class TermRelationshipTypeServiceSpec extends BaseTerminologyIntegrationSpec {

    TermRelationshipTypeService termRelationshipTypeService

    @Override
    void setupDomainData() {

        TermRelationshipType relationshipType1 = new TermRelationshipType(label: 'is-not-a', createdBy: StandardEmailAddress.INTEGRATION_TEST,
                                                                          terminology: simpleTerminology,
                                                                          displayLabel: 'Is Not A')
        TermRelationshipType relationshipType2 = new TermRelationshipType(label: 'is-not-narrower-than',
                                                                          createdBy: StandardEmailAddress.INTEGRATION_TEST,
                                                                          terminology: simpleTerminology, displayLabel: 'Is Not Narrower Than')
        Term term1 = new Term(code: 'IT01', definition: 'Integration Test 01', createdBy: StandardEmailAddress.INTEGRATION_TEST,
                              terminology: simpleTerminology)
        Term term2 = new Term(code: 'IT02', definition: 'Integration Test 02', createdBy: StandardEmailAddress.INTEGRATION_TEST,
                              terminology: simpleTerminology)
        Term term3 = new Term(code: 'IT03', definition: 'Integration Test 03', createdBy: StandardEmailAddress.INTEGRATION_TEST,
                              terminology: simpleTerminology)

        checkAndSave(relationshipType1, relationshipType2, term1, term2, term3)

        TermRelationship relationship1 = new TermRelationship(sourceTerm: term1, targetTerm: term2, relationshipType: relationshipType1,
                                                              createdBy: StandardEmailAddress.INTEGRATION_TEST)
        TermRelationship relationship2 = new TermRelationship(sourceTerm: term1, targetTerm: term3, relationshipType: relationshipType2,
                                                              createdBy: StandardEmailAddress.INTEGRATION_TEST)
        TermRelationship relationship3 = new TermRelationship(sourceTerm: term2, targetTerm: term3, relationshipType: relationshipType1,
                                                              createdBy: StandardEmailAddress.INTEGRATION_TEST)

        checkAndSave(relationship1, relationship2, relationship3)

        id = relationshipType1.id
    }

    void 'test get'() {
        setupData()

        expect:
        termRelationshipTypeService.get(id) != null
    }

    void 'test list'() {
        setupData()

        when:
        List<TermRelationshipType> termRelationshipTypes = termRelationshipTypeService.list(max: 2, offset: 4, sort: 'dateCreated')

        then:
        termRelationshipTypes.size() == 2

        when:
        def tm1 = termRelationshipTypes[0]
        def tm2 = termRelationshipTypes[1]

        then:
        tm1.label == 'is-not-a'

        and:
        tm2.label == 'is-not-narrower-than'

    }

    void 'test count'() {
        setupData()

        expect:
        termRelationshipTypeService.count() == 6
    }

    void 'test delete'() {
        setupData()

        expect:
        termRelationshipTypeService.count() == 6

        when:
        termRelationshipTypeService.delete(id)
        sessionFactory.currentSession.flush()

        then:
        TermRelationshipType.count() == 5
    }

    void 'test findAllByTerminologyId'() {
        setupData()

        when:
        List<TermRelationshipType> termRelationshipTypes = termRelationshipTypeService.findAllByTerminologyId(simpleTerminology.id)

        then:
        termRelationshipTypes.size() == 2
    }

    void 'test findByTerminologyIdAndId'() {
        given:
        setupData()

        when:
        TermRelationshipType termRelationshipType = termRelationshipTypeService.findByTerminologyIdAndId(simpleTerminology.id, id)

        then:
        termRelationshipType
        termRelationshipType.label == 'is-not-a'
    }
}
