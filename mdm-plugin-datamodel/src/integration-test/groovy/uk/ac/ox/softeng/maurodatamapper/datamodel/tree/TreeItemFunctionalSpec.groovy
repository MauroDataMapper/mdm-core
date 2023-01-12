/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
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
    UUID dataClassId

    @Shared
    Folder folder

    @Shared
    Folder importTestFolder

    @Shared
    UUID importingDataModelId

    @Shared
    UUID importingParentDataClassId

    @Shared
    UUID importedParentDataClassId

    @Shared
    UUID importedDataModelId

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @RunOnce
    @Transactional
    def setup() {
        try {
            log.debug('Check and setup test data')

            assert Classifier.count() == 0

            folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
            checkAndSave(folder)

            DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST, folder: folder, authority: testAuthority).save(flush: true)
            dataModelId = dataModel.id
            otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST, folder: folder, authority: testAuthority).save(flush: true).id

            PrimitiveType string = new PrimitiveType(createdBy: FUNCTIONAL_TEST, label: 'string')
            dataModel.addToDataTypes(string).save(flush: true)

            DataClass dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST, dataModel: dataModel)
                .addToDataElements(label: 'Functional Test DataElement', createdBy: FUNCTIONAL_TEST, dataType: string).save(flush: true)

            dataClassId = dataClass.id

            Classifier classifier = new Classifier(label: 'Functional Test Classifier', createdBy: FUNCTIONAL_TEST).save(flush: true)
            new Classifier(label: 'Functional Test Classifier 2', createdBy: FUNCTIONAL_TEST).save(flush: true)
            dataModel.addToClassifiers(classifier).save(flush: true)


            // Set up another folder with data models, classes and imports (see structure below)

            importTestFolder = new Folder(label: 'Functional Test Import Test Folder', createdBy: FUNCTIONAL_TEST)
            checkAndSave(importTestFolder)

            DataModel importingDataModel = new DataModel(label: 'Functional Test Importing DataModel', createdBy: FUNCTIONAL_TEST, folder: importTestFolder,
                                                         authority: testAuthority).save(flush: true)
            importingDataModelId = importingDataModel.id

            DataClass importingParentDataClass = new DataClass(label: 'Functional Test Importing Parent DataClass', createdBy: FUNCTIONAL_TEST, dataModel: importingDataModel)
                .save(flush: true)
            importingParentDataClassId = importingParentDataClass.id

            DataModel importedDataModel = new DataModel(label: 'Functional Test Imported DataModel', createdBy: FUNCTIONAL_TEST, folder: importTestFolder,
                                                        authority: testAuthority).save(flush: true)
            importedDataModelId = importedDataModel.id

            DataClass importedParentDataClass = new DataClass(label: 'Functional Test Imported Parent DataClass', createdBy: FUNCTIONAL_TEST, dataModel: importedDataModel)
            importedParentDataClassId = importedParentDataClass.id

            DataClass importedChildDataClass = new DataClass(label: 'Functional Test Imported Child DataClass', createdBy: FUNCTIONAL_TEST)

            // Folder: Functional Test Import Test Folder
            //      Data Model: Functional Test Importing DataModel
            //          Data Class: Functional Test Importing Parent DataClass (directly owned)
            //              Data Class: Functional Test Imported Child DataClass (imported)
            //          Data Class: Functional Test Imported Parent DataClass (imported)
            //      Data Model: Functional Test Imported DataModel
            //          Data Class: Functional Test Imported Parent DataClass (directly owned)
            //              Data Class: Functional Test Imported Child DataClass (directly owned)

            importingDataModel
                .addToDataClasses(importingParentDataClass)
            checkAndSave(importingDataModel)

            importedDataModel
                .addToDataClasses(importedParentDataClass)
                .addToDataClasses(importedChildDataClass)
            importedParentDataClass.addToDataClasses(importedChildDataClass)
            checkAndSave(importedDataModel)

            importingDataModel
                .addToImportedDataClasses(importedParentDataClass)
            importingParentDataClass.addToImportedDataClasses(importedChildDataClass)
            checkAndSave(importedDataModel)

            sessionFactory.currentSession.flush()
        } catch (Exception ex) {
            log.error(ex.message, ex)
            throw ex
        }
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(DataModel, Folder, Classifier, DataClass)
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
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Import Test Folder",
    "hasChildren": true,
    "availableActions": [],
    "deleted": false
  }
]''')
        when:
        GET("folders/${folder.id}", STRING_ARG)

        then:
        verifyJsonResponse(OK,  '''[
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test DataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test DataModel 2",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      }
    ]''')

        when:
        GET("folders/${importTestFolder.id}", STRING_ARG)

        then:
        verifyJsonResponse(OK,  '''[
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Imported DataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Importing DataModel",
        "hasChildren": true,
        "availableActions": [],
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
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

    void 'T03 : test tree for datamodel with content'() {
        when:
        GET("folders/dataModels/${dataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test DataClass",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]''')
    }

    void 'T04 : test tree for datamodel with no content'() {
        when:
        GET("folders/dataModels/${otherDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'T05 : test documentation superseded models arent shown in the tree'() {
        given: 'document superseded models created'
        // Create new model
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise model
        PUT("dataModels/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new documentation version
        PUT("dataModels/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId
        firstId != secondId

        when:
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then:
        localResponse.body().size() == 2

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 3

        when:
        List<Map> children = localResponse.body()

        then:
        children.any {it.label == 'Functional Test DataModel' && !it.branchName && !it.modelVersion}
        children.any {it.label == 'Functional Test DataModel 2' && !it.branchName && !it.modelVersion}
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
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("dataModels/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("dataModels/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when: 'Getting tree'
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the finalised version and the new branch'
        localResponse.body().size() == 2

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 4

        when:
        List<Map> children = localResponse.body()

        then:
        children.any {it.label == 'Functional Test DataModel'}
        children.any {it.label == 'Functional Test DataModel 2'}
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
        PUT("dataModels/$secondId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        // 'Getting tree after finalisation'
        localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the second finalised version only'
        localResponse.body().size() == 2

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 3

        when:
        children = localResponse.body()

        then:
        children.any {it.label == 'Functional Test DataModel'}
        children.any {it.label == 'Functional Test DataModel 2'}
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
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("dataModels/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("dataModels/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id
        // Create another new branch
        PUT("dataModels/$firstId/newBranchModelVersion", [branchName: 'functionalTest'], MAP_ARG, true)
        verifyResponse CREATED, response
        String thirdId = response.body().id

        when: 'Getting tree'
        HttpResponse<List> localResponse = GET('folders', Argument.of(List, Map))

        then: 'We should have the finalised version and the new branches'
        localResponse.body().size() == 2

        when:
        localResponse = GET("folders/${folder.id}", Argument.of(List, Map))

        then:
        localResponse.body().size() == 5

        when:
        List<Map> children = localResponse.body()

        then:
        children.any {it.label == 'Functional Test DataModel'}
        children.any {it.label == 'Functional Test DataModel 2'}
        // New branch is in tree
        children.any {
            it.label == 'Functional Test Model model superseded' &&
            it.documentationVersion == '1.0.0' &&
            !it.modelVersion &&
            it.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME &&
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
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model deleted'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("dataModels/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)

        expect:
        firstId

        when:
        DELETE("dataModels/$firstId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().id == firstId
        response.body().deleted

        when:
        GET('admin/tree/folders/dataModels/deleted', STRING_ARG, true)

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
        "domainType": "DataModel",
        "label": "Functional Test Model deleted",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": true,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
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
        GET('admin/tree/folders/dataModels/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA03 : test getting documentation superseded models'() {
        given: 'document superseded models created'
        // Create new model
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise model
        PUT("dataModels/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new documentation version
        PUT("dataModels/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/dataModels/documentationSuperseded', STRING_ARG, true)

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
        "domainType": "DataModel",
        "label": "Functional Test Model doc superseded",
        "modelVersion": "1.0.0",
        "hasChildren": false,
        "availableActions": [],
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard"
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
        GET('admin/tree/folders/dataModels/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[]'''
    }

    void 'TA05 : test getting model version superseded models'() {
        given: 'model version superseded is created'
        // Create model
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        // Finalise first model
        PUT("dataModels/$firstId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        // Create a new branch
        PUT("dataModels/$firstId/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id
        PUT("dataModels/$secondId/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        expect:
        firstId
        secondId

        when: 'getting the admin modelSuperseded endpoint'
        GET('admin/tree/folders/dataModels/modelSuperseded', STRING_ARG, true)

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
            "domainType": "DataModel",
            "label": "Functional Test Model model superseded",
            "modelVersion": "1.0.0",
            "hasChildren": false,
            "availableActions": [],
            "deleted": false,
            "finalised": true,
            "superseded": true,
            "documentationVersion": "1.0.0",
            "folder": "${json-unit.matches:id}",
            "type": "Data Standard"
          }
        ]
      }
    ]
    '''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'TMI01 : test tree for datamodel with imports, showing those imports'() {
        when:
        GET("folders/dataModels/${importingDataModelId}/?includeImported=true", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test Importing Parent DataClass",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test Imported Parent DataClass",
    "hasChildren": true,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}",
    "imported": true
  }
]''')
    }

    void 'TMI02 : test tree for datamodel with imports, not showing those imports'() {
        when:
        GET("folders/dataModels/${importingDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test Importing Parent DataClass",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]''')

        when:
        GET("folders/dataModels/${importingDataModelId}/?includeImported=false", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test Importing Parent DataClass",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}"
  }
]''')
    }

    void 'TMI03 : test tree for datamodel classes with imports, showing those imports'() {
        when:
        GET("folders/dataClasses/${importingParentDataClassId}/?includeImported=true", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test Imported Child DataClass",
    "hasChildren": false,
    "availableActions": [],
    "modelId": "${json-unit.matches:id}",
    "parentId": "${json-unit.matches:id}",
    "imported": true
  }
]''')
    }

    void 'TMI04 : test tree for datamodel classes with imports, not showing those imports'() {
        when:
        GET("folders/dataClasses/${importingParentDataClassId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')

        when:
        GET("folders/dataClasses/${importingParentDataClassId}/?includeImported=false", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'FTR : test full tree render'() {
        when:
        GET("full/dataModels/${dataModelId}")

        then:
        verifyResponse(OK, response)
        responseBody()
        responseBody().hasChildren
        responseBody().children.size() == 2
        responseBody().children.any { it.label == 'string' && !it.hasChildren }
        responseBody().children.any { it.label == 'Functional Test DataClass' && it.hasChildren }

        when:
        Map dataClassTree = responseBody().children.find { it.label == 'Functional Test DataClass' }

        then:
        dataClassTree.children.size() == 1
        dataClassTree.children.any { it.label == 'Functional Test DataElement' }
    }

    void 'AN01 : test getting ancestors of class item'() {

        when:
        GET("folders/dataClasses/${importingParentDataClassId}/ancestors")
        then:
        verifyResponse(OK, response)
        responseBody().id == importTestFolder.id.toString()
        responseBody().hasChildren
        responseBody().children.size() == 1

        responseBody().children.any({ it.id == importingDataModelId.toString() })
        responseBody().children.any({ it.hasChildren == true })

        responseBody().children.find { it.id == importingDataModelId.toString() }.children.any { it.id = importingParentDataClassId.toString() }
        responseBody().children.find { it.id == importingDataModelId.toString() }.children.any { it.hasChildren == false }

    }

    void 'AN02 : test getting ancestors of DataModel item'() {
        when:
        GET("folders/dataModels/${importingDataModelId}/ancestors")
        then:
        verifyResponse(OK, response)
        responseBody().id == importTestFolder.id.toString()
        responseBody().hasChildren
        responseBody().children.size() == 1
        responseBody().children.any({ it.id == importingDataModelId.toString() })
        responseBody().children.any({ it.hasChildren == true })
    }

    @Override
    void cleanUpData(String id) {
        cleanUpData(id, 'dataModels')
    }

    void cleanUpData(String id, String endpoint) {
        if (!id) return
        DELETE("$endpoint/$id?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
