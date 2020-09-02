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
package uk.ac.ox.softeng.maurodatamapper.terminology.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: treeItem
 *
 * |   GET   | /api/admin/tree/folders/terminologies  | Action: modelSuperseded
 * |   GET   | /api/admin/tree/folders/terminologies  | Action: documentationSuperseded
 * |   GET   | /api/admin/tree/folders/codeSets       | Action: modelSuperseded
 * |   GET   | /api/admin/tree/folders/codeSets       | Action: documentationSuperseded
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemController
 * </pre>
 */
@Integration
@Slf4j
class TreeItemFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    UUID terminologyId

    @Shared
    UUID otherTerminologyId

    @Shared
    UUID codeSetId

    @Shared
    UUID otherCodeSetId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')

        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)

        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        Terminology terminology = new Terminology(label: 'Functional Test Terminology', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        terminologyId = terminology.id
        otherTerminologyId = new Terminology(label: 'Functional Test Terminology 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id

        CodeSet codeSet = new CodeSet(label: 'Functional Test CodeSet', createdBy: FUNCTIONAL_TEST,
                folder: folder, authority: testAuthority).save(flush: true)
        codeSetId = codeSet.id
        otherCodeSetId = new CodeSet(label: 'Functional Test CodeSet 2', createdBy: FUNCTIONAL_TEST,
                folder: folder, authority: testAuthority).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(Terminology, CodeSet, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }

    @Override
    String getResourcePath() {
        'tree'
    }

    void 'test folder tree for terminology and codeset'() {
        when:
        GET('folders', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
            {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test CodeSet",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet",
        "branchName": "main"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test CodeSet 2",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet",
        "branchName": "main"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Terminology",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology",
        "branchName": "main"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Terminology 2",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology",
        "branchName": "main"
      }
    ]
  }
]'''
    }

    void 'test getting terminology superseded model when there are none'() {
        when: 'finalised terminology has not yet been created'
        GET('admin/tree/folders/terminologies/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'test getting terminology superseded models'() {
        given: 'finalised terminology is created'
        POST("folders/${folder.id}/terminologies", [
                label: 'Functional Test Model terminology superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("terminologies/$firstId/finalise", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$firstId/newModelVersion", [label: 'Functional Test Terminology reader'], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/terminologies/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Model terminology superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology",
        "branchName":"main"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'test getting terminology superseded documentation when there are none'() {
        when: 'finalised terminology has not yet been created'
        GET('admin/tree/folders/terminologies/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'test getting terminology superseded documentation'() {
        given: 'finalised terminology is created'
        POST("folders/${folder.id}/terminologies", [
                label: 'Functional Test Model terminology documentation superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("terminologies/$firstId/finalise", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$firstId/newDocumentationVersion", [label: 'Functional Test Terminology reader'], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/terminologies/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Model terminology documentation superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology",
        "branchName":"main"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'test getting codeset superseded model when there are none'() {
        when: 'finalised codeset has not yet been created'
        GET('admin/tree/folders/codeSets/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'test getting codeset superseded models'() {
        given: 'finalised codeset is created'
        POST("folders/${folder.id}/codeSets", [
                label: 'Functional Test Model codeset superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("codeSets/$firstId/finalise", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("codeSets/$firstId/newModelVersion", [label: 'Functional Test CodeSet reader'], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/codeSets/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test Model codeset superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet",
        "branchName":"main"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'test getting codeset superseded documentation when there are none'() {
        when: 'finalised codeset has not yet been created'
        GET('admin/tree/folders/codeSets/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'test getting codeset superseded documentation'() {
        given: 'finalised codeset is created'
        POST("folders/${folder.id}/codeSets", [
                label: 'Functional Test Model codeset documentation superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("codeSets/$firstId/finalise", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("codeSets/$firstId/newDocumentationVersion", [label: 'Functional Test CodeSet reader'], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/codeSets/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test Model codeset documentation superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet",
        "branchName":"main"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }
}
