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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Unroll

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
 *  |  GET     | /api/dataModels/${dataModelId}/diff/${otherModelId}  | Action: diff
 *
 *  |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 *
 *  |   POST   | /api/dataModels/${dataModelId}/undoDelete  | Action: undoDelete
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
 */
@Integration
@Slf4j
abstract class ModelUserAccessPermissionChangingAndVersioningFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

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

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'finalise', 'delete'].sort()
    }

    List<String> getReaderAvailableActions() {
        ['show', 'comment'].sort()
    }

    List<String> getFinalisedReaderAvailableActions() {
        [
            'show',
            'createNewVersions',
            'newForkModel',
            'postFinalisedReadable'
        ].sort()
    }

    List<String> getFinalisedEditorAvailableActions() {
        [
            'show',
            'createNewVersions',
            'newForkModel',
            'comment',
            'newModelVersion',
            'newDocumentationVersion',
            'newBranchModelVersion',
            'softDelete',
            'delete',
            'postFinalisedReadable'

        ].sort()
    }

    @Transactional
    String getTestVersionedFolderId() {
        Folder.findByLabel('Functional Test VersionedFolder').id.toString()
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
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()
        responseBody().modelVersion == '1.0.0'

        when: 'log out and log back in again in as editor available actions are correct'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()

        when: 'log out and log back in again in as admin available actions are correct'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()

        cleanup:
        removeValidIdObject(id)
    }

    void 'E16b : Test finalising Model (as editor) with a versionTag'() {
        given:
        String id = getValidId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Release'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()
        responseBody().modelVersion == '1.0.0'
        responseBody().modelVersionTag == 'Functional Test Release'

        when: 'log out and log back in again in as editor available actions are correct and modelVersionTag is set'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()
        responseBody().modelVersionTag == 'Functional Test Release'

        when: 'log out and log back in again in as admin available actions are correct and modelVersionTag is set'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()
        responseBody().modelVersionTag == 'Functional Test Release'

        cleanup:
        removeValidIdObject(id)
    }

    void 'E16c : Test attempting to finalise a model inside a versioned folder (as editor)'() {
        given:
        loginEditor()
        POST('versionedFolders', [label: 'Functional Test Versioned Folder',], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String versionedFolderId = responseBody().id
        POST("folders/$versionedFolderId/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = responseBody().id

        when:
        loginEditor()
        PUT("$id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Release'])

        then:
        verifyResponse(FORBIDDEN, response)

        cleanup:
        DELETE("folders/$versionedFolderId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
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
        responseBody().availableActions == getEditorAvailableActions().sort() - 'finalise'
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
        responseBody().availableActions == getFinalisedEditorAvailableActions().sort()
        responseBody().modelVersion == '2.0.0'

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        cleanUpRoles(id, branchId)
    }

    void 'E19d : test creating a new branch model version of a Model<T> and trying to finalise(as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        responseBody().availableActions == getEditorAvailableActions().sort()
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
        responseBody().availableActions == (getEditorAvailableActions() - [ResourceActions.FINALISE_ACTION]).sort()

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
        responseBody().items.each {it.id in [newBranchId, latestDraftId]}
        responseBody().items.each {it.label == validJson.label}

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

    Map<String, String> buildModelVersionTree() {
        /*
                                                   /- anotherFork
      v1 --------------------------- v2 -- v3  -- v4 --------------- v5 --- main
        \\_ newBranch (v1)                  \_ testBranch (v3)          \__ anotherBranch (v5)
         \_ fork ---- main                                               \_ interestingBranch (v5)
      */
        // V1
        String v1 = getValidFinalisedId()
        loginEditor()

        // Fork and finalise fork
        PUT("$v1/newForkModel", [label: "Functional Test Fork ${modelType}" as String])
        verifyResponse CREATED, response
        String fork = responseBody().id
        PUT("$fork/finalise", [versionChangeType: VersionChangeType.MINOR])
        verifyResponse OK, response

        // Fork main branch
        PUT("$fork/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String forkMain = responseBody().id

        // V2 main branch
        PUT("$v1/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v2 = responseBody().id

        // newBranch from v1 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        PUT("$v1/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranch = responseBody().id

        // Finalise the main branch to v2
        PUT("$v2/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response

        // V3 main branch
        PUT("$v2/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v3 = responseBody().id

        // Finalise the main branch to v3
        PUT("$v3/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response

        // V4 main branch
        PUT("$v3/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v4 = responseBody().id

        // testBranch from v3 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        PUT("$v3/newBranchModelVersion", [branchName: 'testBranch'])
        verifyResponse CREATED, response
        String testBranch = responseBody().id

        // Finalise main branch to v4
        PUT("$v4/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response

        // Fork from v4
        PUT("$v4/newForkModel", [label: "Functional Test AnotherFork ${modelType}" as String])
        verifyResponse CREATED, response
        String anotherFork = responseBody().id

        // V5 and finalise
        PUT("$v4/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String v5 = responseBody().id
        PUT("$v5/finalise", [versionChangeType: VersionChangeType.MAJOR])
        verifyResponse OK, response

        // Main branch
        PUT("$v5/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String main = responseBody().id

        // Another branch
        PUT("$v5/newBranchModelVersion", [branchName: 'anotherBranch'])
        verifyResponse CREATED, response
        String anotherBranch = responseBody().id

        // Interesting branch
        PUT("$v5/newBranchModelVersion", [branchName: 'interestingBranch'])
        verifyResponse CREATED, response
        String interestingBranch = responseBody().id

        logout()
        [v1       : v1, v2: v2, v3: v3, v4: v4, v5: v5,
         newBranch: newBranch, testBranch: testBranch, main: main, anotherBranch: anotherBranch, interestingBranch: interestingBranch,
         fork     : fork, anotherFork: anotherFork, forkMain: forkMain
        ]
    }

    void cleanupModelVersionTree(Map<String, String> data) {
        data.each {k, v ->
            removeValidIdObjectUsingTransaction(v)
        }
        cleanUpRoles(data.values())
    }

    @Unroll
    void 'E26a : Test getting versionTreeModel at #tag (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()

        when: 'getting the tree'
        GET("${data[tag]}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(getExpectedModelTreeVersionString(data), jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)

        // For all versions and branches we should get the same tree, no matter where it was requested
        // But it should NOT include the branch of the fork
        where:
        tag << ['v1', 'v2', 'v3', 'v4', 'v5',
                'newBranch', 'testBranch', 'main', 'anotherBranch', 'interestingBranch']
    }

    void 'E26b : Test getting versionTreeModel at fork only shows the fork and its branch'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.fork}",
    "label": "Functional Test Fork ${modelType}",
    "branch": null,
    "modelVersion": "0.1.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
     {
        "id": "${data.forkMain}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.forkMain}",
    "label": "Functional Test Fork ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  }]"""

        when: 'getting the tree'
        GET("${data.fork}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'E26c : Test getting versionTreeModel at anotherFork only shows the fork'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.anotherFork}",
    "label": "Functional Test AnotherFork ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": []
  }]"""

        when: 'getting the tree'
        GET("${data.anotherFork}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'L27 : Test undoing a soft delete (as not logged in)'() {
        given: 'model is deleted'
        String id = getValidId()
        loginEditor()
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        logout()
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N27 : Test undoing a soft delete (as authenticated/no access)'() {
        given: 'model is deleted'
        String id = getValidId()
        loginEditor()
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        loginAuthenticated()
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'R27 : Test undoing a soft delete (as reader)'() {
        given: 'model is deleted'
        String id = getValidId()
        loginEditor()
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        loginReader()
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E27 : Test undoing a soft delete (as editor)'() {
        given: 'model is deleted'
        String id = getValidId()
        loginEditor()
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        loginEditor()
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'A27 : Test undoing a soft delete (as admin)'() {
        given: 'model is deleted'
        String id = getValidId()
        loginEditor()
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        loginAdmin()
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        !responseBody().deleted

        cleanup:
        removeValidIdObject(id)
    }


    @Unroll
    void 'E28a : Test getting simple versionTreeModel at #tag (as editor)'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()

        when: 'getting the tree'
        GET("${data[tag]}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(getExpectedSimpleModelTreeVersionString(data), jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)

        // For all versions and branches we should get the same tree, no matter where it was requested
        // But it should NOT include the branch of the fork
        where:
        tag << ['v1', 'v2', 'v3', 'v4', 'v5',
                'newBranch', 'testBranch', 'main', 'anotherBranch', 'interestingBranch']
    }

    void 'E28b : Test getting simple versionTreeModel at fork only shows the fork and its branch'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.fork}",
    "branch": null,
    "modelVersion": "0.1.0",
    "documentationVersion": "1.0.0",
    "displayName": "V0.1.0"
  },
  {
    "id": "${data.forkMain}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V0.1.0)"
  }]"""

        when: 'getting the tree'
        GET("${data.fork}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'E28c : Test getting simple versionTreeModel at anotherFork only shows the fork'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[{
    "id": "${data.anotherFork}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main"
  }]"""

        when: 'getting the tree'
        GET("${data.anotherFork}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'E28d : Test getting simple versionTreeModel for merge shows only branches and not the selected model'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[
  {
    "id": "${data.newBranch}",
    "branch": "newBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "newBranch (V1.0.0)"
  },
  {
    "id": "${data.testBranch}",
    "branch": "testBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "testBranch (V3.0.0)"
  },
  {
    "id": "${data.main}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V5.0.0)"
  },
    {
    "id": "${data.anotherBranch}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "anotherBranch (V5.0.0)"
  }
]"""

        when: 'getting the tree'
        GET("${data.interestingBranch}/simpleModelVersionTree?forMerge=true", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'E28e : Test getting simple versionTreeModel with branches only shows only branches'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[
  {
    "id": "${data.newBranch}",
    "branch": "newBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "newBranch (V1.0.0)"
  },
  {
    "id": "${data.testBranch}",
    "branch": "testBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "testBranch (V3.0.0)"
  },
  {
    "id": "${data.main}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V5.0.0)"
  },
    {
    "id": "${data.anotherBranch}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "anotherBranch (V5.0.0)"
  },
  {
    "id": "${data.interestingBranch}",
    "branch": "interestingBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
   "displayName": "interestingBranch (V5.0.0)"
  }
]"""

        when: 'getting the tree'
        GET("${data.interestingBranch}/simpleModelVersionTree?branchesOnly=true", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'R29a : Test available actions inside a VersionedFolder (as reader)'() {
        given:
        loginEditor()
        POST("folders/${testVersionedFolderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginReader()
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().availableActions == getReaderAvailableActions()

        cleanup:
        loginEditor()
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(id)
    }

    void 'R29b : Test available actions inside a Folder inside a VersionedFolder (as reader)'() {
        given:
        loginEditor()
        POST("folders/${testVersionedFolderId}/folders/", [label: 'folder in versionedfolder'], MAP_ARG, true)
        verifyResponse CREATED, response
        String folderId = response.body().id
        POST("folders/${folderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginReader()
        GET("folders/${testVersionedFolderId}", MAP_ARG, true)
        verifyResponse(OK, response)
        GET("folders/${folderId}", MAP_ARG, true)
        verifyResponse(OK, response)
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().availableActions == getReaderAvailableActions()

        cleanup:
        loginEditor()
        DELETE("folders/${folderId}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        cleanUpRoles(id)
    }

    void 'E29a : Test available actions inside a VersionedFolder (as editor)'() {
        given:
        loginEditor()
        POST("folders/${testVersionedFolderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginEditor()
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().availableActions == getEditorAvailableActions() - 'finalise'

        cleanup:
        loginEditor()
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(id)
    }

    void 'E29b : Test available actions inside a Folder inside a VersionedFolder (as editor)'() {
        given:
        loginEditor()
        POST("folders/${testVersionedFolderId}/folders/", [label: 'folder in versionedfolder'], MAP_ARG, true)
        verifyResponse CREATED, response
        String folderId = response.body().id
        POST("folders/${folderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginEditor()
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().availableActions == getEditorAvailableActions() - 'finalise'

        cleanup:
        loginEditor()
        DELETE("folders/${folderId}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        cleanUpRoles(id)
    }

    void 'L08b : test accessing finalised when readable by everyone (not logged in)'() {
        given:
        String id = getValidFinalisedId()
        loginEditor()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        GET(id)

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == true
        response.body().availableActions == ['postFinalisedReadable', 'show']

        when: 'removing readable by everyone'
        loginEditor()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'E09b : test adding readable by authenticated once finalised (as editor)'() {
        given:
        String id = getValidFinalisedId()
        def endpoint = "$id/readByAuthenticated"

        when: 'logged in as user with write access'
        loginEditor()
        PUT(endpoint, [:])

        then:
        verifyResponse(OK, response)
        response.body().readableByAuthenticatedUsers == true

        cleanup:
        removeValidIdObject(id)
    }

    void 'E10b : test removing readable by authenticated once finalised (as editor)'() {
        given:
        String id = getValidFinalisedId()
        def endpoint = "$id/readByAuthenticated"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == true
        logout()

        when: 'logged in as user with write access'
        loginEditor()
        DELETE(endpoint)

        then:
        verifyResponse(OK, response)
        response.body().readableByAuthenticatedUsers == false

        cleanup:
        removeValidIdObject(id)
    }

    void 'E12b : test adding reader share once finalised (as editor)'() {
        given:
        String id = getValidFinalisedId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as writer'
        loginEditor()
        POST(endpoint, [:])

        then:
        verifyResponse CREATED, response
        response.body().securableResourceId == id
        response.body().userGroup.id == readerGroupId
        response.body().groupRole.id == groupRoleId

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == getFinalisedReaderAvailableActions()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    void 'E13b : test removing reader share once finalised (as editor)'() {
        given:
        String id = getValidFinalisedId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as writer'
        loginEditor()
        DELETE(endpoint, [:])

        then:
        verifyResponse NO_CONTENT, response

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'E14b : test adding "editor" share once finalised (as editor)'() {
        given:
        String id = getValidFinalisedId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as writer'
        loginEditor()
        POST(endpoint, [:])

        then:
        verifyResponse CREATED, response
        response.body().securableResourceId == id
        response.body().userGroup.id == readerGroupId
        response.body().groupRole.id == groupRoleId

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == getFinalisedEditorAvailableActions()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    void 'E15b : test removing "editor" share once finalised (as editor)'() {
        given:
        String id = getValidFinalisedId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as writer'
        loginEditor()
        DELETE(endpoint, [:])

        then:
        verifyResponse NO_CONTENT, response

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    String getExpectedModelTreeVersionString(Map data) {
        """[
  {
    "id": "${data.v1}",
    "label": "Functional Test ${modelType}",
    "branch": null,
    "modelVersion" : "1.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
     {
        "id": "${data.fork}",
        "description": "New Fork Of"
      },
      {
        "id": "${data.v2}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.newBranch}",
        "description": "New Model Version Of"
      }
      
    ]
  },
  {
    "id": "${data.newBranch}",
    "label": "Functional Test ${modelType}",
    "branch": "newBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
  {
    "id": "${data.fork}",
    "label": "Functional Test Fork ${modelType}",
    "branch": null,
    "modelVersion": "0.1.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": true,
    "targets": [
      
    ]
  },
  {
    "id": "${data.v2}",
    "label": "Functional Test ${modelType}",
    "branch": null,
    "modelVersion" : "2.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      {
        "id": "${data.v3}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.v3}",
    "label": "Functional Test ${modelType}",
    "branch": null,
    "modelVersion" : "3.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      {
        "id": "${data.v4}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.testBranch}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.testBranch}",
    "label": "Functional Test ${modelType}",
    "branch": "testBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
  {
   "id": "${data.v4}",
    "label": "Functional Test ${modelType}",
    "branch": null,
    "modelVersion" : "4.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      {
        "id": "${data.anotherFork}",
        "description": "New Fork Of"
      },
      {
        "id": "${data.v5}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.anotherFork}",
    "label": "Functional Test AnotherFork ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": false,
    "isNewDocumentationVersion": false,
    "isNewFork": true,
    "targets": [
      
    ]
  },
  {
    "id": "${data.v5}",
    "label": "Functional Test ${modelType}",
    "branch": null,
    "modelVersion" : "5.0.0",
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
    {
        "id": "${data.main}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.anotherBranch}",
        "description": "New Model Version Of"
      },
      {
        "id": "${data.interestingBranch}",
        "description": "New Model Version Of"
      }
    ]
  },
  {
    "id": "${data.main}",
    "label": "Functional Test ${modelType}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
    {
    "id": "${data.anotherBranch}",
    "label": "Functional Test ${modelType}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  },
  {
    "id": "${data.interestingBranch}",
    "label": "Functional Test ${modelType}",
    "branch": "interestingBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [
      
    ]
  }
]"""
    }

    String getExpectedSimpleModelTreeVersionString(Map data) {
        """[
  {
    "id": "${data.v1}",
    "branch": null,
    "modelVersion" : "1.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V1.0.0"
  },
  {
    "id": "${data.newBranch}",
    "branch": "newBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "newBranch (V1.0.0)"
  },
  {
    "id": "${data.v2}",
    "branch": null,
    "modelVersion" : "2.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V2.0.0"
  },
  {
    "id": "${data.v3}",
    "branch": null,
    "modelVersion" : "3.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V3.0.0"
  },
  {
    "id": "${data.testBranch}",
    "branch": "testBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "testBranch (V3.0.0)"
  },
  {
    "id": "${data.v4}",
    "branch": null,
    "modelVersion" : "4.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V4.0.0"
  },
  {
    "id": "${data.v5}",
    "branch": null,
    "modelVersion" : "5.0.0",
    "documentationVersion": "1.0.0",
    "displayName": "V5.0.0"
  },
  {
    "id": "${data.main}",
    "branch": "main",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V5.0.0)"
  },
    {
    "id": "${data.anotherBranch}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
    "displayName": "anotherBranch (V5.0.0)"
  },
  {
    "id": "${data.interestingBranch}",
    "branch": "interestingBranch",
    "modelVersion": null,
    "documentationVersion": "1.0.0",
   "displayName": "interestingBranch (V5.0.0)"
  }
]"""
    }
}
