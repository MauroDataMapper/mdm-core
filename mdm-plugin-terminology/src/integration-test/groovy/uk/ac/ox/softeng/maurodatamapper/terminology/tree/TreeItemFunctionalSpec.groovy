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
package uk.ac.ox.softeng.maurodatamapper.terminology.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
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
    UUID simpleTerminologyId

    @Shared
    UUID complexTerminologyId

    @Shared
    UUID codeSetId

    @Shared
    UUID otherCodeSetId

    @Shared
    Folder folder

    @Autowired
    TerminologyService terminologyService

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')

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

        complexTerminologyId = BootstrapModels.buildAndSaveComplexTerminology(messageSource, folder, terminologyService, testAuthority).id
        simpleTerminologyId = BootstrapModels.buildAndSaveSimpleTerminology(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(Terminology, CodeSet, Folder, Classifier)
    }

    @Transactional
    String getTermIdByCode(UUID terminologyId, String code) {
        Term.byTerminologyIdAndCode(terminologyId, code).get().id
    }

    @Override
    String getResourcePath() {
        'tree'
    }

    void 'T01 : test folder tree for terminology and codeset'() {
        String exp =
          '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false
  }
]'''
        String expChild = '''[
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Complex Test Terminology",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test CodeSet",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test CodeSet 2",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Terminology",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Terminology 2",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Simple Test Terminology",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      }
    ]'''
        when:
        GET('folders', STRING_ARG)

        then:
        verifyJsonResponse OK, exp

        when:
        GET("folders/${folder.id}", STRING_ARG)

        then:
        verifyJsonResponse OK, expChild

    }

    void 'T02 : test classifiers tree'() {
        when:
        GET('classifiers', STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "test classifier",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "test classifier simple",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "test classifier2",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false
  }
]''')
    }

    void 'T03 : test tree for terminology with no content'() {
        when:
        GET("folders/terminologies/${terminologyId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'T04a : test tree for terminology with content'() {
        when:
        GET("folders/terminologies/${complexTerminologyId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getCttTree())
    }

    void 'T04b : test term tree for terminology with content'() {
        when:
        GET("terminologies/${complexTerminologyId}/terms/tree", STRING_ARG, true)

        then:
        verifyJsonResponse(OK, getCttTree())
    }

    void 'T05a : test tree for term CTT00'() {
        given:
        String termId = getTermIdByCode(complexTerminologyId, 'CTT00')

        when:
        GET("folders/terms/${termId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getCtt00Tree())
    }

    void 'T05b : test term tree endpoint for term CTT00'() {
        given:
        String termId = getTermIdByCode(complexTerminologyId, 'CTT00')

        when:
        GET("terminologies/${complexTerminologyId}/terms/tree/${termId}", STRING_ARG, true)

        then:
        verifyJsonResponse(OK, getCtt00Tree())
    }

    void 'T06a : test tree for term CTT20'() {
        given:
        String termId = getTermIdByCode(complexTerminologyId, 'CTT20')

        when:
        GET("folders/terms/${termId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getCtt20Tree())
    }

    void 'T06b : test term tree endpoint for term CTT20'() {
        given:
        String termId = getTermIdByCode(complexTerminologyId, 'CTT20')

        when:
        GET("terminologies/${complexTerminologyId}/terms/tree/${termId}", STRING_ARG, true)

        then:
        verifyJsonResponse(OK, getCtt20Tree())
    }

    void 'T07 : test terminology documentation superseded models arent shown in the tree'() {
        given: 'document superseded models created'
        // Create new model
        POST("folders/${folder.id}/terminologies", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise model
        PUT("terminologies/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new documentation version
        PUT("terminologies/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId
        firstId != secondId

        when:
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then:
        localResponse.body().size() == 1
        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 7

        when:
        List<Map> children = localResponse.body()

        then:
        children.any {
            it.label == 'Functional Test Model doc superseded' &&
            it.documentationVersion == '2.0.0' &&
            !it.branchName &&
            !it.modelVersion &&
            !it.finalised
        }
        // Superseded model is not in the tree
        !children.any {
            it.label == 'Functional Test Model doc superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.branchName &&
            it.modelVersion == '1.0.0'
        }

        cleanup:
        cleanUpData(secondId, 'terminologies')
        cleanUpData(firstId, 'terminologies')
    }

    void 'T08 : terminology test model version superseded models arent shown in the tree'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/terminologies", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("terminologies/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("terminologies/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when: 'Getting tree'
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the finalised version and the new branch'
        localResponse.body().size() == 1

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 8

        when:
        List<Map> children = localResponse.body()

        then:
        // New branch is in tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.modelVersion &&
            !it.branchName &&
            !it.finalised
        }
        // Finalised model version is in the tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            it.modelVersion == '1.0.0' &&
            !it.branchName &&
            it.finalised
        }

        when: 'Finalise the branch'
        PUT("terminologies/$secondId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        // 'Getting tree after finalisation'
        localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the second finalised version only'
        localResponse.body().size() == 1

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 7

        when:
        children = localResponse.body()

        then:
        // Finalised model version is in the tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            it.modelVersion == '2.0.0' &&
            !it.branchName
        }
        // Superseded model is not in the tree
        !children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            it.modelVersion == '1.0.0' &&
            !it.branchName
        }

        cleanup:
        cleanUpData(firstId, 'terminologies')
        cleanUpData(secondId, 'terminologies')
    }

    void 'T09 : test tree for codesets with no content'() {
        when:
        GET("folders/codeSets/${codeSetId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'T10 : test codeset documentation superseded models arent shown in the tree'() {
        given: 'document superseded models created'
        // Create new model
        POST("folders/${folder.id}/codeSets", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise model
        PUT("codeSets/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new documentation version
        PUT("codeSets/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId
        firstId != secondId

        when:
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then:
        localResponse.body().size() == 1
        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 7

        when:
        List<Map> children = localResponse.body()

        then:
        children.any {
            it.label == 'Functional Test Model doc superseded' &&
            it.documentationVersion == '2.0.0' &&
            !it.branchName &&
            !it.modelVersion &&
            !it.finalised
        }
        // Superseded model is not in the tree
        !children.any {
            it.label == 'Functional Test Model doc superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.branchName &&
            it.modelVersion == '1.0.0'
        }

        cleanup:
        cleanUpData(secondId, 'codeSets')
        cleanUpData(firstId, 'codeSets')
    }

    void 'T11 : test codeset model version superseded models arent shown in the tree'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/codeSets", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("codeSets/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("codeSets/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when: 'Getting tree'
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the finalised version and the new branch'
        localResponse.body().size() == 1
        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 8

        when:
        List<Map> children = localResponse.body()

        then:
        // New branch is in tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.modelVersion &&
            !it.branchName &&
            !it.finalised
        }
        // Finalised model version is in the tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            it.modelVersion == '1.0.0' &&
            !it.branchName &&
            it.finalised
        }

        when: 'Finalise the branch'
        PUT("codeSets/$secondId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        // 'Getting tree after finalisation'
        localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the second finalised version only'
        localResponse.body().size() == 1

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 7

        when:
        children = localResponse.body()

        then:
        // Finalised model version is in the tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            it.modelVersion == '2.0.0' &&
            !it.branchName
        }
        // Superseded model is not in the tree
        !children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            it.modelVersion == '1.0.0' &&
            !it.branchName
        }

        cleanup:
        cleanUpData(firstId, 'codeSets')
        cleanUpData(secondId, 'codeSets')
    }

    void 'TA01 : test getting deleted terminologies'() {
        given: 'finalised model is created'
        POST("folders/${folder.id}/terminologies", [
            label: 'Functional Test Model deleted'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("terminologies/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)

        expect:
        firstId

        when:
        DELETE("terminologies/$firstId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().id == firstId
        response.body().deleted

        when:
        GET('admin/tree/folders/terminologies/deleted', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Model deleted",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": true,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId, 'terminologies')
    }

    void 'TA02 : test getting terminology superseded documentation when there are none'() {
        when: 'finalised terminology has not yet been created'
        GET('admin/tree/folders/terminologies/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA03 : test getting terminology superseded documentation'() {
        given: 'finalised terminology is created'
        POST("folders/${folder.id}/terminologies", [
            label: 'Functional Test Model documentation superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("terminologies/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
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
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Model documentation superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId, 'terminologies')
        cleanUpData(secondId, 'terminologies')
    }

    void 'TA04 : test getting terminology superseded model when there are none'() {
        when: 'finalised terminology has not yet been created'
        GET('admin/tree/folders/terminologies/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA05 : test getting terminology superseded models'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/terminologies", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("terminologies/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("terminologies/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id
        PUT("terminologies/$secondId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        expect:
        firstId
        secondId

        when: 'getting the admin modelSuperseded endpoint'
        GET('admin/tree/folders/terminologies/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "Terminology",
        "label": "Functional Test Model model superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Terminology"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId, 'terminologies')
        cleanUpData(secondId, 'terminologies')
    }

    void 'TA06 : test getting deleted codesets'() {
        given: 'finalised model is created'
        POST("folders/${folder.id}/codeSets", [
            label: 'Functional Test Model deleted'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("codeSets/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)

        expect:
        firstId

        when:
        DELETE("codeSets/$firstId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().id == firstId
        response.body().deleted

        when:
        GET('admin/tree/folders/codeSets/deleted', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test Model deleted",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": true,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId, 'codeSets')
    }

    void 'TA07 : test getting codeSets superseded documentation when there are none'() {
        when: 'finalised terminology has not yet been created'
        GET('admin/tree/folders/codeSets/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA08 : test getting codeSets superseded documentation'() {
        given: 'finalised codeSets is created'
        POST("folders/${folder.id}/codeSets", [
            label: 'Functional Test Model documentation superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("codeSets/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("codeSets/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
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
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test Model documentation superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId, 'codeSets')
        cleanUpData(secondId, 'codeSets')
    }

    void 'TA09 : test getting codeSets superseded model when there are none'() {
        when: 'finalised terminology has not yet been created'
        GET('admin/tree/folders/codeSets/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA10 : test getting codeSets superseded models'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/codeSets", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("codeSets/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("codeSets/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id
        PUT("codeSets/$secondId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        expect:
        firstId
        secondId

        when: 'getting the admin modelSuperseded endpoint'
        GET('admin/tree/folders/codeSets/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "CodeSet",
        "label": "Functional Test Model model superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "CodeSet"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId, 'codeSets')
        cleanUpData(secondId, 'codeSets')
    }

    void cleanUpData(String id, String endpoint) {
        if (!id) return
        DELETE("$endpoint/$id?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    String getCttTree() {
        '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT100: Complex Test Term 100",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT101",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT00: Complex Test Term 00",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]'''
    }

    String getCtt00Tree() {
        '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT1: Complex Test Term 1",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT2: Complex Test Term 2",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT3: Complex Test Term 3",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT4: Complex Test Term 4",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT5: Complex Test Term 5",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT6: Complex Test Term 6",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT7: Complex Test Term 7",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT8: Complex Test Term 8",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT9: Complex Test Term 9",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT10: Complex Test Term 10",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT20: Complex Test Term 20",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT30: Complex Test Term 30",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT40: Complex Test Term 40",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT50: Complex Test Term 50",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT60: Complex Test Term 60",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT70: Complex Test Term 70",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT80: Complex Test Term 80",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT90: Complex Test Term 90",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]'''
    }

    String getCtt20Tree() {
        '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT21: Complex Test Term 21",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT22: Complex Test Term 22",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT23: Complex Test Term 23",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT24: Complex Test Term 24",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT25: Complex Test Term 25",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT26: Complex Test Term 26",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT27: Complex Test Term 27",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT28: Complex Test Term 28",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT29: Complex Test Term 29",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]'''
    }
}
