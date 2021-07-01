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
package uk.ac.ox.softeng.maurodatamapper.referencedata.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 08/04/2020
 */
@Integration
@Slf4j
class TreeItemFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')

        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Authority testAuthority = Authority.findByLabel('Test Authority')
        checkAndSave(testAuthority)

        ReferenceDataModel dataModel = new ReferenceDataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        otherDataModelId = new ReferenceDataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id

        Classifier classifier = new Classifier(label: 'Functional Test Classifier', createdBy: FUNCTIONAL_TEST).save(flush: true)
        new Classifier(label: 'Functional Test Classifier 2', createdBy: FUNCTIONAL_TEST).save(flush: true)
        dataModel.addToClassifiers(classifier).save(flush: true)

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(ReferenceDataModel, Folder, Classifier)
    }

    @Override
    String getResourcePath() {
        'tree'
    }

    void 'T01 : test folder tree'() {
        when:
        GET('folders', STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
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
        "domainType": "ReferenceDataModel",
        "label": "Functional Test DataModel",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "ReferenceDataModel"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataModel",
        "label": "Functional Test DataModel 2",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "ReferenceDataModel"
      }
    ]
  }
]''')
    }

    void 'T02 : test classifiers tree'() {
        when:
        GET('classifiers', STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "Functional Test Classifier",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "Functional Test Classifier 2",
    "hasChildren": false,
    "availableActions": [],
    "deleted": false
  }
]''')
    }

    void 'T03 : test tree for referencedatamodel with content'() {
        when:
        GET("folders/referenceDataModels/${dataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'T04 : test tree for datamodel with no content'() {
        when:
        GET("folders/referenceDataModels/${otherDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'T05 : test documentation superseded models arent shown in the tree'() {
        given: 'document superseded models created'
        // Create new model
        POST("folders/${folder.id}/referenceDataModels", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise model
        PUT("referenceDataModels/$firstId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new documentation version
        PUT("referenceDataModels/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
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
        localResponse.body().first().children.size() == 3

        when:
        List<Map> children = localResponse.body().first().children

        then:
        children.any { it.label == 'Functional Test DataModel' && !it.branchName && !it.modelVersion }
        children.any { it.label == 'Functional Test DataModel 2' && !it.branchName && !it.modelVersion }
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
        cleanUpData(secondId)
        cleanUpData(firstId)
    }

    void 'T06 : test model version superseded models arent shown in the tree'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/referenceDataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("referenceDataModels/$firstId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("referenceDataModels/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when: 'Getting tree'
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the finalised version and the new branch'
        localResponse.body().size() == 1
        localResponse.body().first().children.size() == 4

        when:
        List<Map> children = localResponse.body().first().children

        then:
        children.any { it.label == 'Functional Test DataModel' }
        children.any { it.label == 'Functional Test DataModel 2' }
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
        PUT("referenceDataModels/$secondId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response

        // 'Getting tree after finalisation'
        localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the second finalised version only'
        localResponse.body().size() == 1
        localResponse.body().first().children.size() == 3

        when:
        children = localResponse.body().first().children

        then:
        children.any { it.label == 'Functional Test DataModel' }
        children.any { it.label == 'Functional Test DataModel 2' }
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
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'T07 : test model version branches are shown in the tree'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/referenceDataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("referenceDataModels/$firstId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("referenceDataModels/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id
        // Create another new branch
        PUT("referenceDataModels/$firstId/newBranchModelVersion", [branchName: 'functionalTest'], MAP_ARG, true)
        verifyResponse CREATED, response
        String thirdId = response.body().id

        when: 'Getting tree'
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the finalised version and the new branches'
        localResponse.body().size() == 1
        localResponse.body().first().children.size() == 5

        when:
        List<Map> children = localResponse.body().first().children

        then:
        children.any { it.label == 'Functional Test DataModel' }
        children.any { it.label == 'Functional Test DataModel 2' }
        // New branch is in tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.modelVersion &&
            it.branchName == 'main' &&
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
        // Other new branch is in tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.modelVersion &&
            it.branchName == 'functionalTest' &&
            !it.finalised
        }

        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
        cleanUpData(thirdId)
    }

    void 'TA01 : test getting deleted models'() {
        given: 'finalised model is created'
        POST("folders/${folder.id}/referenceDataModels", [
            label: 'Functional Test Model deleted'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("referenceDataModels/$firstId/finalise", [versionChangeType: "Major"], MAP_ARG, true)

        expect:
        firstId

        when:
        DELETE("referenceDataModels/$firstId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().id == firstId
        response.body().deleted

        when:
        GET('admin/tree/folders/referenceDataModels/deleted', STRING_ARG, true)

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
        "domainType": "ReferenceDataModel",
        "label": "Functional Test Model deleted",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": true,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "ReferenceDataModel"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
    }

    void 'TA02 : test getting documentation superseded models when there are none'() {
        when: 'finalised model has not yet been created'
        GET('admin/tree/folders/referenceDataModels/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA03 : test getting documentation superseded models'() {
        given: 'document superseded models created'
        // Create new model
        POST("folders/${folder.id}/referenceDataModels", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise model
        PUT("referenceDataModels/$firstId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new documentation version
        PUT("referenceDataModels/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/referenceDataModels/documentationSuperseded', STRING_ARG, true)

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
        "domainType": "ReferenceDataModel",
        "label": "Functional Test Model doc superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "ReferenceDataModel"
      }
    ]
  }
]
'''

        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'TA04 : test getting model superseded models when there are none'() {
        when: 'finalised model has not yet been created'
        GET('admin/tree/folders/referenceDataModels/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA05 : test getting model version superseded models'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/referenceDataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("referenceDataModels/$firstId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("referenceDataModels/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id
        PUT("referenceDataModels/$secondId/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        verifyResponse OK, response

        expect:
        firstId
        secondId

        when: 'getting the admin modelSuperseded endpoint'
        GET('admin/tree/folders/referenceDataModels/modelSuperseded', STRING_ARG, true)

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
            "domainType": "ReferenceDataModel",
            "label": "Functional Test Model model superseded",
            "modelVersion": "1.0.0",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "finalised": true,
            "superseded": true,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "ReferenceDataModel"
          }
        ]
      }
    ]
    '''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    @Override
    void cleanUpData(String id) {
        cleanUpData(id, 'referenceDataModels')
    }

    void cleanUpData(String id, String endpoint) {
        if (!id) return
        DELETE("$endpoint/$id?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
