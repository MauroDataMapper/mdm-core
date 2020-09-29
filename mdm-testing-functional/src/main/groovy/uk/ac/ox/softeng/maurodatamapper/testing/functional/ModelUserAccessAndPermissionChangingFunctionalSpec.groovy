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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import io.micronaut.core.type.Argument
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType
import spock.lang.PendingFeature

import java.net.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: model
 *
 *  |  GET     | /api/dataModels        | Action: index
 *  |  DELETE  | /api/dataModels/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${id}  | Action: update
 *  |  GET     | /api/dataModels/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/dataModels                 | Action: save
 *
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *
 *  |  GET     | /api/dataModels/types  | Action: types
 *  |  GET     | /api/dataModels/${dataModelId}/hierarchy  | Action: hierarchy
 *  |  PUT     | /api/dataModels/${dataModelId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/dataModels/${dataModelId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/dataModels/${dataModelId}      | Action: changeFolder
 *  |  PUT     | /api/dataModels/${dataModelId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/dataModels/providers/defaultDataTypeProviders       | Action: defaultDataTypeProviders
 *  |  GET     | /api/dataModels/providers/importers                      | Action: importerProviders
 *  |  GET     | /api/dataModels/providers/exporters                      | Action: exporterProviders
 *  |  GET     | /api/dataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 *
 *  |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
 */
@Integration
@Slf4j
abstract class ModelUserAccessAndPermissionChangingFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

    String getValidFinalisedId() {
        String id = getValidId()
        loginEditor()
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
        id
    }

    abstract String getModelType()

    @Override
    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using permanent API call', id)
        loginAdmin()
        DELETE("${id}?permanent=true")
        response.status() in [NO_CONTENT, NOT_FOUND]
    }

    boolean mergingIsNotAvailable() {
        false
    }

    void 'L16 : Test finalising Model (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N16 : Test finalising Model (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when: 'authenticated user'
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R16 : Test finalising Model (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/finalise", ["version": "3.9.0"])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E16 : Test finalising Model (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == [
            "show",
            "createNewVersions",
            "newForkModel",
            "comment",
            "newModelVersion",
            "newDocumentationVersion",
            "newBranchModelVersion",
            "softDelete",
            "delete"
        ]
        responseBody().modelVersion == '1.0.0'

        when: 'log out and log back in again in as editor available actions are correct'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == [
            "show",
            "createNewVersions",
            "newForkModel",
            "comment",
            "newModelVersion",
            "newDocumentationVersion",
            "newBranchModelVersion",
            "softDelete",
            "delete"
        ]

        when: 'log out and log back in again in as admin available actions are correct'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == [
            "show",
            "createNewVersions",
            "newForkModel",
            "comment",
            "newModelVersion",
            "newDocumentationVersion",
            "newBranchModelVersion",
            "softDelete",
            "delete"
        ]

        cleanup:
        removeValidIdObject(id)
    }

    void 'L17 : test creating a new fork model of a Model<T> (as not logged in)'() {
        given:
        String id = getValidFinalisedId()

        when: 'not logged in'
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N17 : test creating a new fork model of a Model<T> (as authenticated/no access)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginAuthenticated()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R17 : test creating a new fork model of a Model<T> (as reader)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == "Functional Test ${modelType} v2"

        when:
        String forkId = responseBody().id
        GET("$forkId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_FORK_OF.label
        responseBody().items.first().sourceModel.id == forkId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(forkId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(forkId, id)
    }

    void 'E17 : test creating a new fork model of a Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as writer'
        loginEditor()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == "Functional Test ${modelType} v2"

        when:
        String forkId = responseBody().id
        GET("$forkId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_FORK_OF.label
        responseBody().items.first().sourceModel.id == forkId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(forkId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(forkId, id)
    }

    void 'L18 : test creating a new documentation version of a Model<T> (as not logged in)'() {
        given:
        String id = getValidFinalisedId()

        when: 'not logged in'
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N18 : test creating a new documentation version of a Model<T> (as authenticated/no access)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginAuthenticated()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R18 : test creating a new documentation version of a Model<T> (as reader)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginReader()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E18 : test creating a new documentation version of a Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '2.0.0'

        when:
        String docId = responseBody().id
        GET("$docId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        responseBody().items.first().sourceModel.id == docId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(docId)
        cleanUpRoles(id, docId)
    }

    void 'L19 : test creating a new branch model version of a Model<T> (as not logged in)'() {
        given:
        String id = getValidFinalisedId()

        when: 'not logged in'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N19 : test creating a new branch model version of a Model<T> (as authenticated/no access)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginAuthenticated()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R19 : test creating a new branch model version of a Model<T> (as reader)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginReader()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E19a : test creating a new model version of a Model<T> (no branch name) (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        GET("$branchId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        responseBody().items.first().sourceModel.id == branchId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        cleanUpRoles(id, branchId)
    }

    void 'E19b : test creating a new branch model version of a Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        GET("$branchId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        responseBody().items.first().sourceModel.id == branchId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when:
        //get all so that if there are more than 10 items, we can be sure of finding the correct one in the when block below
        GET("?all=true")

        then:
        verifyResponse OK, response
        responseBody().count >= 3

        when:
        log.debug(responseBody().toString())
        String mainBranchId = responseBody().items.find {
            it.label == validJson.label &&
            !(it.id in [branchId, id])
        }?.id

        then:
        mainBranchId

        when:
        GET(mainBranchId)

        then:
        verifyResponse OK, response
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id, branchId, mainBranchId)
    }

    void 'E19c : test creating a new model version of a Model<T> and finalising (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when:
        String branchId = responseBody().id
        PUT("$branchId/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == [
            "show",
            "createNewVersions",
            "newForkModel",
            "comment",
            "newModelVersion",
            "newDocumentationVersion",
            "newBranchModelVersion",
            "softDelete",
            "delete"
        ]
        responseBody().modelVersion == '2.0.0'

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        cleanUpRoles(id, branchId)
    }

    @PendingFeature(reason = 'Finalise needs to be removed from available actions after model is finalised')
    void 'E19d : test creating a new branch model version of a Model<T> and trying to finalise(as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        String mainBranchId = responseBody().id

        when:
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        String branchId = responseBody().id
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion
        !responseBody().availableActions.contains('finalise') // TODO Functionality to satisfy this condition needs to be implemented

        when:
        PUT("$branchId/finalise", [versionChangeType: 'Major'])

        then:
        verifyForbidden response

        when:
        GET('')

        then:
        verifyResponse OK, response
        responseBody().count >= 3

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id, branchId, mainBranchId)
    }

    void 'E20 : test finding common ancestor of two Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when: 'logged in as editor'
        GET("$leftId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == validJson.label

        when:
        //get all so that if there are more than 10 items, we can be sure of finding the correct one in the when block below
        GET("?all=true")

        then:
        verifyResponse OK, response
        responseBody().count >= 3

        when:
        log.debug(responseBody().toString())
        String mainBranchId = responseBody().items.find {
            it.label == validJson.label &&
            !(it.id in [leftId, rightId, id])
        }?.id

        then:
        mainBranchId

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(leftId)
        removeValidIdObjectUsingTransaction(rightId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id, leftId, rightId, mainBranchId)
    }

    void 'E21 : test finding latest version of a Model<T> (as editor)'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when: 'logged in as editor'
        GET("$newBranchId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == validJson.label
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == validJson.label
        responseBody().modelVersion == '2.0.0'

        when: 'logged in as editor'
        GET("$newBranchId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '2.0.0'

        when:
        GET('')

        then:
        verifyResponse OK, response
        responseBody().count >= 4

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(expectedId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        cleanUpRoles(id, newBranchId, expectedId, latestDraftId)
    }

    void 'E22 : test finding merge difference of two Model<T> (as editor)'() {
        if (mergingIsNotAvailable()) {
            return
        }

        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when:
        GET("$leftId/mergeDiff/$rightId")

        then:
        verifyResponse OK, response
        responseBody().leftId == rightId
        responseBody().rightId == leftId

        when:
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().leftId == mainId
        responseBody().rightId == leftId

        when:
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().leftId == mainId
        responseBody().rightId == rightId

        cleanup:
        removeValidIdObjectUsingTransaction(mainId)
        removeValidIdObjectUsingTransaction(leftId)
        removeValidIdObjectUsingTransaction(rightId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(mainId, leftId, rightId, id)
    }

    void 'E23 : test getting current draft model on main branch from side branch (as editor)'() {
        /*
        id (finalised) -- finalisedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String finalisedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$finalisedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when: 'logged in as editor'
        GET("$newBranchId/currentMainBranch")

        then:
        verifyResponse OK, response
        responseBody().id == latestDraftId
        responseBody().label == validJson.label

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(finalisedId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        cleanUpRoles(id, newBranchId, finalisedId, latestDraftId)
    }

    void 'E24 : test getting all draft models (as editor)'() {
        /*
        id (finalised) -- finalisedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String finalisedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$finalisedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when: 'logged in as editor'
        GET("$id/availableBranches")

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.each { it.id in [newBranchId, latestDraftId] }
        responseBody().items.each { it.label == validJson.label }

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(finalisedId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        cleanUpRoles(id, newBranchId, finalisedId, latestDraftId)
    }

    void 'R25 : test merging object diff into a draft main model'() {
        given:
        // a source and draft main branch
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id
        logout()

        when:
        // attempt merge as reader
        loginReader()
        PUT("$source/mergeInto/$target", [patch: [test: 'value']])

        then:
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObjectUsingTransaction(target)
        removeValidIdObjectUsingTransaction(source)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(target, source, id)
    }

    void 'E25 : test merging object diff into a draft main model'() {
        if (mergingIsNotAvailable()) {
            return
        }

        given:
        // a source and draft main branch
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        def patchJson = [patch: [
            leftId : "$target" as String,
            rightId: "$source" as String,
            label  : "Functional Test Model",
            count  : 10,
            diffs  : [
                [
                    fieldName: "description",
                    value    : "modifiedDescriptionSource"
                ]
            ]
        ]
        ]
        // merging a patch
        PUT("$source/mergeInto/$target", patchJson)

        then:
        // success
        verifyResponse OK, response
        responseBody()
        responseBody().id == target
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        responseBody().description == 'modifiedDescriptionSource'

        when:
        patchJson.deleteBranch = true
        // merging patch and deleting source
        PUT("$source/mergeInto/$target", patchJson)

        then:
        verifyResponse OK, response
        responseBody()
        responseBody().id == target
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        responseBody().description == 'modifiedDescriptionSource'

        when:
        // trying to get source which should be deleted
        GET("$source")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObjectUsingTransaction(target)
        removeValidIdObjectUsingTransaction(source)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(target, source, id)
    }

    void 'Test getting versionTreeModel (as editor)'() {
        /*
        id (finalised) -- finalisedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])
        verifyResponse CREATED, response
        String forkId = responseBody().id
        PUT("$id/newBranchModelVersion", [ : ])
        verifyResponse CREATED, response
        String mainBranchId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$forkId/finalise", [versionChangeType: VersionChangeType.MINOR])
        verifyResponse OK, response
        PUT("$forkId/newDocumentationVersion",[ : ])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when: 'logged in as editor'
        io.micronaut.http.HttpResponse<List<Map>> localResponse = GET("$id/modelVersionTree", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body()
        localResponse.body().size() == 5


        Map sourceMap = localResponse.body().find { it.modelId == id }
        sourceMap
        sourceMap == [branchName             : "main",
                         label                  : "Functional Test " + getModelType(),
                         modelId                : id,
                         newBranchModelVersion  : false,
                         newDocumentationVersion: false,
                         newFork                : false,
                         targets                : [[
                                                            modelId: forkId,
                                                            description: VersionLinkType.NEW_FORK_OF.toString()
                                                   ],
                                                   [
                                                           modelId: mainBranchId,
                                                           description: VersionLinkType.NEW_MODEL_VERSION_OF.toString()
                                                   ],
                                                   [
                                                            modelId: newBranchId,
                                                            description: VersionLinkType.NEW_MODEL_VERSION_OF.toString()
                                                   ]]
                ]

        Map forkMap = localResponse.body().find { it.modelId == forkId }
        forkMap
        forkMap ==    [branchName            : "main",
                      label                  : "Functional Test " + getModelType() + " v2",
                      modelId                : forkId,
                      newBranchModelVersion  : false,
                      newDocumentationVersion: false,
                      newFork                : true,
                      targets                : [[
                                                        modelId: latestDraftId,
                                                        description: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.toString()
                                                ]]
                ]

        Map mainBranchMap = localResponse.body().find { it.modelId == mainBranchId }
        mainBranchMap
        mainBranchMap == [branchName             : "main",
                         label                  : "Functional Test " + getModelType(),
                         modelId                : mainBranchId,
                         newBranchModelVersion  : true,
                         newDocumentationVersion: false,
                         newFork                : false,
                         targets                : []]

        Map newBranchMap = localResponse.body().find { it.modelId == newBranchId }
        newBranchMap
        newBranchMap == [branchName             : "newBranch",
                         label                  : "Functional Test " + getModelType(),
                         modelId                : newBranchId,
                         newBranchModelVersion  : true,
                         newDocumentationVersion: false,
                         newFork                : false,
                         targets                : []]

        Map latestDraftMap = localResponse.body().find { it.modelId == latestDraftId }
        latestDraftMap
        latestDraftMap == [branchName           : "main",
                         label                  : "Functional Test " + getModelType() + " v2",
                         modelId                : latestDraftId,
                         newBranchModelVersion  : false,
                         newDocumentationVersion: true,
                         newFork                : false,
                         targets                : []]

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(forkId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id)
        cleanUpRoles(newBranchId)
        cleanUpRoles(forkId)
        cleanUpRoles(latestDraftId)
    }


}