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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

/**
 * <pre>
 * Controller: termRelationshipType
 *  |   POST   | /api/terminologies/${terminologyId}/termRelationshipTypes                                                  | Action: save
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes                                                  | Action: index
 *  |  DELETE  | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}                                            | Action: delete
 *  |   PUT    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}                                            | Action: update
 *  |   GET    | /api/terminologies/${terminologyId}/termRelationshipTypes/${id}                                            | Action: show
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeController
 */
@Integration
@Transactional
@Slf4j
class TermRelationshipTypeFunctionalSpec extends ResourceFunctionalSpec<TermRelationshipType> {

    @Shared
    UUID terminologyId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        Authority testAuthority = Authority.findByLabel('Test Authority')
        checkAndSave(testAuthority)
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Terminology terminology = new Terminology(label: 'Functional Test Terminology', createdBy: FUNCTIONAL_TEST,
                                                  folder: folder, authority: testAuthority).save(flush: true)
        terminologyId = terminology.id
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec TermFunctionalSpec')
        cleanUpResources(TermRelationship, Term, TermRelationshipType, Terminology, Folder)
    }

    @Override
    String getResourcePath() {
        "terminologies/${terminologyId}/termRelationshipTypes"
    }

    @Override
    Map getValidJson() {
        [
            label       : 'is-a',
            displayLabel: 'Is A'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label       : null,
            displayLabel: 'Is A'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            displayLabel: 'Updating display label'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "TermRelationshipType",
  "label": "is-a",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Terminology",
      "domainType": "Terminology",
      "finalised": false
    }
  ],
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "displayLabel": "Is A",
  "parentalRelationship": false,
  "childRelationship": false
}'''
    }
}