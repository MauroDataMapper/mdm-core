/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
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

    abstract String getModelType()

    abstract String getModelUrlType()

    abstract String getModelPrefix()

    abstract String getModelFolderId(String id)

    abstract String getLeftHandDiffModelId()

    abstract String getRightHandDiffModelId()

    abstract String getExpectedDiffJson()

    String getValidFinalisedId() {
        String id = getValidId()
        loginCreator()
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
        id
    }

    @Override
    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using permanent API call', id)
        loginCreator()
        DELETE("${id}?permanent=true")
        response.status() in [NO_CONTENT, NOT_FOUND]
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withSoftDeleteByDefault()
            .withInheritedAccessPermissions()
            .whereAuthenticatedUsers {
                canIndex()
            }
            .whereAnonymousUsers {
                canIndex()
            }
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'finalise', 'save', 'show', 'softDelete', 'update')
            .whereEditorsCanAction('comment', 'editDescription', 'finalise', 'save', 'show', 'softDelete', 'update')
            .whereAuthorsCanAction('comment', 'editDescription', 'show',)
            .whereReviewersCanAction('comment', 'show')
            .whereReadersCanAction('show')
    }

    @Override
    void verify03CannotCreateResponse(HttpResponse<Map> response, String name) {
        if (name in ['Anonymous', 'Authenticated']) verifyNotFound response, null
        else if ((expectations.can(name, 'see') && expectations.accessPermissionIsInherited) ||
                 (!expectations.can(name, 'create') && expectations.isSecuredResource)) verifyForbidden(response)
        else verifyNotFound response, null
    }

    @Override
    int getExpectedCountOfGroupsWithAccess() {
        1 // Just the creator's group as all other groups have inherited access from the folder
    }

    List<String> getFinalisedReaderAvailableActions() {
        ['show',
         'createNewVersions',
         'newForkModel',
         'finalisedReadActions'].sort()
    }

    List<String> getFinalisedReviewerAvailableActions() {
        ['comment',
         'show',
         'createNewVersions',
         'newForkModel',
         'finalisedReadActions'].sort()
    }


    List<String> getFinalisedEditorAvailableActions() {
        ['show',
         'createNewVersions',
         'newForkModel',
         'comment',
         'newModelVersion',
         'newDocumentationVersion',
         'newBranchModelVersion',
         'softDelete',
         'finalisedEditActions'].sort()
    }

    List<String> getFinalisedContainerAdminAvailableActions() {
        ['show',
         'createNewVersions',
         'newForkModel',
         'comment',
         'delete',
         'newModelVersion',
         'newDocumentationVersion',
         'newBranchModelVersion',
         'softDelete',
         'finalisedEditActions'].sort()
    }

    @Transactional
    String getTestVersionedFolderId() {
        Folder.findByLabel('Functional Test VersionedFolder').id.toString()
    }

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Transactional
    String getTestFolder2Id() {
        Folder.findByLabel('Functional Test Folder 2').id.toString()
    }


    void 'CORE-#prefix-08b : test accessing finalised when readable by everyone [not allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()
        loginCreator()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response

        when:
        login(name)
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().readableByEveryone == true
        responseBody().availableActions == actions

        when: 'removing readable by everyone'
        loginCreator()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response

        and:
        login(name)
        GET(id)

        then:
        if (canRead) {
            verifyResponse OK, response
            assert responseBody().readableByEveryone == false
            assert responseBody().availableActions == actions
        } else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead | actions
        'LO'   | null            | false   | ['finalisedReadActions', 'show'] // Authenticated users can fork
        'NA'   | 'Authenticated' | false   | getFinalisedReaderAvailableActions()
        'RE'   | 'Reader'        | true    | getFinalisedReaderAvailableActions()
        'RV'   | 'Reviewer'      | true    | getFinalisedReviewerAvailableActions()
        'AU'   | 'Author'        | true    | getFinalisedReviewerAvailableActions()
    }

    void 'CORE-#prefix-09b : test adding readable by authenticated once finalised [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()
        def endpoint = "$id/readByAuthenticated"

        when: 'logged in as user with write access'
        login(name)
        PUT(endpoint, [:])

        then:
        verifyResponse(OK, response)
        responseBody().readableByAuthenticatedUsers == true

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-10b : test removing readable by authenticated once finalised [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()
        def endpoint = "$id/readByAuthenticated"
        loginCreator()
        PUT(endpoint, [:])
        verifyResponse OK, response
        responseBody().readableByAuthenticatedUsers == true

        when: 'logged in as user with write access'
        login(name)
        DELETE(endpoint)

        then:
        verifyResponse(OK, response)
        responseBody().readableByAuthenticatedUsers == false

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-12b : test adding reader share once finalised (as #name)'() {
        given:
        String id = getValidFinalisedId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as writer'
        login(name)
        POST(endpoint, [:])

        then:
        verifyResponse CREATED, response
        responseBody().securableResourceId == id
        responseBody().userGroup.id == readerGroupId
        responseBody().groupRole.id == groupRoleId

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedReaderAvailableActions()
        responseBody().id == id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-13b : test removing reader share once finalised (as #name)'() {
        given:
        String id = getValidFinalisedId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginCreator()
        POST(endpoint, [:])

        when: 'logged in as writer'
        login(name)
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

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-14 : Test finalising Model [not allowed] (as #name)'() {
        given:
        String id = getValidId()

        when:
        login(name)
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        if (canRead) verifyForbidden response else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'CORE-#prefix-14 : Test finalising Model [allowed] (as #name)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor'
        login(name)
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == actions
        responseBody().modelVersion == '1.0.0'

        when: 'log out and log back in again in as editor available actions are correct'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions()

        when: 'log out and log back in again in as admin available actions are correct'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedContainerAdminAvailableActions()

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | getFinalisedEditorAvailableActions()
        'CA'   | 'ContainerAdmin' | getFinalisedContainerAdminAvailableActions()
        'AD'   | 'Admin'          | getFinalisedContainerAdminAvailableActions()
    }

    void 'CORE-#prefix-14b : Test finalising Model with a versionTag [allowed] (as #name)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor'
        login(name)
        PUT("$id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Release'])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == actions
        responseBody().modelVersion == '1.0.0'
        responseBody().modelVersionTag == 'Functional Test Release'

        when: 'log out and log back in again in as editor available actions are correct and modelVersionTag is set'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedEditorAvailableActions()
        responseBody().modelVersionTag == 'Functional Test Release'

        when: 'log out and log back in again in as admin available actions are correct and modelVersionTag is set'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == getFinalisedContainerAdminAvailableActions()
        responseBody().modelVersionTag == 'Functional Test Release'

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | getFinalisedEditorAvailableActions()
        'CA'   | 'ContainerAdmin' | getFinalisedContainerAdminAvailableActions()
        'AD'   | 'Admin'          | getFinalisedContainerAdminAvailableActions()

    }

    void 'CORE-#prefix-14c : Test attempting to finalise a model inside a versioned folder (as #name)'() {
        given:
        loginCreator()
        POST("folders/$testVersionedFolderId/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = responseBody().id

        when:
        login(name)
        PUT("$id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Release'])

        then:
        if (canRead) verifyForbidden response else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | canRead
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | true
        'RV'   | 'Reviewer'       | true
        'AU'   | 'Author'         | true
        'ED'   | 'Editor'         | true
        'CA'   | 'ContainerAdmin' | true
        'AD'   | 'Admin'          | true
    }

    void 'CORE-#prefix-14d : test getting a finalised model (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when:
        login(name)
        GET(id)

        then:
        if (canRead) {
            verifyResponse OK, response
            responseBody().availableActions == actions
        } else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | canRead | actions
        'LO'   | null             | false   | null
        'NA'   | 'Authenticated'  | false   | null
        'RE'   | 'Reader'         | true    | getFinalisedReaderAvailableActions()
        'RV'   | 'Reviewer'       | true    | getFinalisedReviewerAvailableActions()
        'AU'   | 'Author'         | true    | getFinalisedReviewerAvailableActions()
        'ED'   | 'Editor'         | true    | getFinalisedEditorAvailableActions()
        'CA'   | 'ContainerAdmin' | true    | getFinalisedContainerAdminAvailableActions()
        'AD'   | 'Admin'          | true    | getFinalisedContainerAdminAvailableActions()
    }

    void 'CORE-#prefix-15 : test creating a new fork model of a Model<T> [not allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when:
        login(name)
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    void 'CORE-#prefix-15 : test creating a new fork model of a Model<T> [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in'
        login(name)
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyResponse CREATED, response
        String forkId = responseBody().id
        responseBody().id != id
        responseBody().label == "Functional Test ${modelType} v2"
        responseBody().availableActions == actions

        when:
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

        where:
        prefix | name             | actions
        'RE'   | 'Reader'         | expectations.editorAvailableActions
        'RV'   | 'Reviewer'       | expectations.editorAvailableActions
        'AU'   | 'Author'         | expectations.editorAvailableActions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-16 : test creating a new documentation version of a Model<T> [not allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when:
        login(name)
        PUT("$id/newDocumentationVersion", [:])

        then:
        if (canRead) verifyForbidden response else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'CORE-#prefix-16 : test creating a new documentation version of a Model<T> [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        login(name)
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyResponse CREATED, response
        String docId = responseBody().id
        responseBody().id
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '2.0.0'
        responseBody().availableActions == actions

        when:
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

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-17 : test creating a new branch model version of a Model<T> [not allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when:
        login(name)
        PUT("$id/newBranchModelVersion", [:])

        then:
        if (canRead) verifyForbidden response else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'CORE-#prefix-17a : test creating a new model version of a Model<T> (no branch name) [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        login(name)
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response
        String branchId = responseBody().id
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        !responseBody().modelVersion
        // The creator gets editor level rights to a model
        responseBody().availableActions == actions

        when:
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

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-17b : test creating a new branch model version of a Model<T> [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        login(name)
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        String branchId = responseBody().id
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().availableActions == (actions + [ResourceActions.MERGE_INTO_ACTION]).sort() - [ResourceActions.FINALISE_ACTION]
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion

        when:
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
        //        log.debug(responseBody().toString())
        String mainBranchId = responseBody().items.find {
            it.label == validJson.label && !(it.id in [branchId, id])
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

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-17c : test creating a new model version of a Model<T> and finalising [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in'
        login(name)
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
        responseBody().availableActions == actions
        responseBody().modelVersion == '2.0.0'

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        cleanUpRoles(id, branchId)

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | finalisedEditorAvailableActions
        'CA'   | 'ContainerAdmin' | finalisedContainerAdminAvailableActions
        'AD'   | 'Admin'          | finalisedContainerAdminAvailableActions
    }

    void 'CORE-#prefix-17d : test creating a new branch model version of a Model<T> and trying to finalise [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in'
        login(name)
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])

        then:
        verifyResponse CREATED, response
        responseBody().availableActions == actions
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
        responseBody().availableActions == (actions + [ResourceActions.MERGE_INTO_ACTION] - [ResourceActions.FINALISE_ACTION]).sort()

        when:
        PUT("$branchId/finalise", [versionChangeType: 'Major'])

        then: 'No matter who you are, you cant finalise a non-main branch'
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

        where:
        prefix | name             | actions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-18 : test finding common ancestor of two Model<T> [not allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()
        loginCreator()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String mainBranchId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when: 'logged in'
        login(name)
        GET("$leftId/commonAncestor/$rightId")

        then:
        verifyNotFound(response, leftId)

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(leftId)
        removeValidIdObjectUsingTransaction(rightId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id, leftId, rightId, mainBranchId)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    void 'CORE-#prefix-18 : test finding common ancestor of two Model<T> [allowed] (as #name)'() {
        given:
        String id = getValidFinalisedId()
        loginCreator()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String mainBranchId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when: 'logged in'
        login(name)
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

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(leftId)
        removeValidIdObjectUsingTransaction(rightId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id, leftId, rightId, mainBranchId)

        where:
        prefix | name
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-19 : test finding latest version of a Model<T> [not allowed] (as #name)'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginCreator()
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

        when: 'logged in'
        login(name)
        GET("$newBranchId/latestFinalisedModel")

        then:
        verifyNotFound(response, newBranchId)

        when:
        GET("$latestDraftId/latestFinalisedModel")

        then:
        verifyNotFound(response, latestDraftId)

        when:
        GET("$newBranchId/latestModelVersion")

        then:
        verifyNotFound(response, newBranchId)

        when:
        GET("$latestDraftId/latestModelVersion")

        then:
        verifyNotFound(response, latestDraftId)

        when:
        GET('')

        then:
        verifyResponse OK, response
        responseBody().count == 0

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(expectedId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        cleanUpRoles(id, newBranchId, expectedId, latestDraftId)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    /**
     * The following only need read access to perform, so no need to check above that*/
    void 'CORE-RE-19 : test finding latest version of a Model<T>'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginCreator()
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

        when: 'logged in'
        loginReader()
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

        when:
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

    void 'CORE-#prefix-20 : test finding merge difference of two Model<T> [not allowed] (as #name)'() {
        if (expectations.mergingIsAvailable) return

        given:
        String id = getValidFinalisedId()
        loginCreator()
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
        login(name)
        GET("$leftId/mergeDiff/$rightId")

        then:
        verifyNotFound(response, leftId)

        when:
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyNotFound(response, leftId)

        when:
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyNotFound(response, rightId)

        cleanup:
        removeValidIdObjectUsingTransaction(mainId)
        removeValidIdObjectUsingTransaction(leftId)
        removeValidIdObjectUsingTransaction(rightId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(mainId, leftId, rightId, id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }


    void 'CORE-RE-20 : test finding merge difference of two Model<T>'() {
        if (expectations.mergingIsAvailable) return

        given:
        String id = getValidFinalisedId()
        loginCreator()
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
        loginReader()
        GET("$leftId/mergeDiff/$rightId")

        then:
        verifyResponse OK, response
        responseBody().targetId == rightId
        responseBody().sourceId == leftId

        when:
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == leftId

        when:
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().targetId == mainId
        responseBody().sourceId == rightId

        cleanup:
        removeValidIdObjectUsingTransaction(mainId)
        removeValidIdObjectUsingTransaction(leftId)
        removeValidIdObjectUsingTransaction(rightId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(mainId, leftId, rightId, id)
    }

    void 'CORE-#prefix-21 : test getting current draft model on main branch from side branch [not allowed] (as #name)'() {
        /*
        id (finalised) -- finalisedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginCreator()
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

        when: 'logged in'
        login(name)
        GET("$newBranchId/currentMainBranch")

        then:
        verifyNotFound(response, newBranchId)

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(finalisedId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        cleanUpRoles(id, newBranchId, finalisedId, latestDraftId)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    void 'CORE-RE-21 : test getting current draft model on main branch from side branch'() {
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

        when: 'logged in'
        loginReader()
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

    void 'CORE-#prefix-22 : test getting all draft models [not allowed] (as #name)'() {
        /*
        id (finalised) -- finalisedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginCreator()
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

        when: 'logged in'
        login(name)
        GET("$id/availableBranches")

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(newBranchId)
        removeValidIdObjectUsingTransaction(finalisedId)
        removeValidIdObjectUsingTransaction(latestDraftId)
        cleanUpRoles(id, newBranchId, finalisedId, latestDraftId)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }


    void 'CORE-RE-22 : test getting all draft models'() {
        /*
        id (finalised) -- finalisedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = getValidFinalisedId()
        loginCreator()
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

        when: 'logged in'
        loginReader()
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

    void 'CORE-#prefix-23 : test merging object diff into a draft main model [not allowed] (as #name)'() {
        given:
        // a source and draft main branch
        String id = getValidFinalisedId()
        loginCreator()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        login(name)
        PUT("$source/mergeInto/$target", [patch: [test: 'value']])

        then:
        if (canRead) verifyForbidden response else verifyNotFound response, source

        cleanup:
        removeValidIdObjectUsingTransaction(target)
        removeValidIdObjectUsingTransaction(source)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(target, source, id)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'CORE-#prefix-23 : test merging object diff into a draft main model [allowed] (as #name)'() {
        if (expectations.mergingIsAvailable) return

        given:
        // a source and draft main branch
        String id = getValidFinalisedId()
        loginCreator()
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id
        String label = responseBody().label
        String pathToChange = "${getModelPrefix()}:${label}\$source@description"

        when:
        def patchJson = [patch: [targetId: target,
                                 sourceId: source,
                                 label   : label,
                                 count   : 1,
                                 patches : [[fieldName          : 'description',
                                             path               : pathToChange,
                                             sourceValue        : 'modifiedDescriptionSource',
                                             targetValue        : null,
                                             commonAncestorValue: null,
                                             isMergeConflict    : false,
                                             type               : 'modification',]]]]
        // merging a patch
        login(name)
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

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
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
        loginCreator()

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
         fork     : fork, anotherFork: anotherFork, forkMain: forkMain]
    }

    void cleanupModelVersionTree(Map<String, String> data) {
        data.each {k, v -> removeValidIdObjectUsingTransaction(v)
        }
        cleanUpRoles(data.values())
    }

    void 'CORE-RE-24a : Test getting versionTreeModel at #tag'() {
        given:
        Map data = buildModelVersionTree()
        loginReader()

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

    void 'CORE-RE-24b : Test getting versionTreeModel at fork only shows the fork and its branch'() {
        given:
        Map data = buildModelVersionTree()
        String expectedJson = """[{
    "id": "${data.fork}",
    "label": "Functional Test Fork ${modelType}",
    "branch": null,
    "modelVersion": "0.1.0",
    "modelVersionTag": null,
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
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "isNewBranchModelVersion": true,
    "isNewDocumentationVersion": false,
    "isNewFork": false,
    "targets": [

    ]
  }]"""

        when: 'getting the tree'
        loginReader()
        GET("${data.fork}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'CORE-RE-24c : Test getting versionTreeModel at anotherFork only shows the fork'() {
        given:
        Map data = buildModelVersionTree()
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
        loginReader()
        GET("${data.anotherFork}/modelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'CORE-#prefix-25 : Test undoing a soft delete (as #name)'() {
        given: 'model is deleted'
        String id = getValidId()
        loginCreator()
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted
        logout()

        when:
        login(name)
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        if (canUndo) {
            verifyResponse(OK, response)
            !responseBody().deleted
        } else verifyForbidden response

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | canUndo
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | false
        'RV'   | 'Reviewer'       | false
        'AU'   | 'Author'         | false
        'ED'   | 'Editor'         | false
        'CA'   | 'ContainerAdmin' | false
        'AD'   | 'Admin'          | true
    }

    void 'CORE-#prefix-26 : Test getting simple versionTreeModel [not allowed] (as #name)'() {
        given:
        Map data = buildModelVersionTree()

        when: 'getting the tree'
        login(name)
        GET("${data.v1}/simpleModelVersionTree")

        then:
        verifyNotFound(response, data.v1)

        cleanup:
        cleanupModelVersionTree(data)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    void 'CORE-RE-26a : Test getting simple versionTreeModel at #tag'() {
        given:
        Map data = buildModelVersionTree()

        when: 'getting the tree'
        loginReader()
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

    void 'CORE-RE-26b : Test getting simple versionTreeModel at fork only shows the fork and its branch'() {
        given:
        Map data = buildModelVersionTree()
        String expectedJson = """[{
    "id": "${data.fork}",
    "branch": null,
    "modelVersion": "0.1.0",
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "V0.1.0"
  },
  {
    "id": "${data.forkMain}",
    "branch": "main",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V0.1.0)"
  }]"""

        when: 'getting the tree'
        loginReader()
        GET("${data.fork}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'CORE-RE-26c : Test getting simple versionTreeModel at anotherFork only shows the fork'() {
        given:
        Map data = buildModelVersionTree()
        String expectedJson = """[{
    "id": "${data.anotherFork}",
    "branch": "main",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "main"
  }]"""

        when: 'getting the tree'
        loginReader()
        GET("${data.anotherFork}/simpleModelVersionTree", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'CORE-RE-26d : Test getting simple versionTreeModel for merge shows only branches and not the selected model'() {
        given:
        Map data = buildModelVersionTree()
        String expectedJson = """[
  {
    "id": "${data.newBranch}",
    "branch": "newBranch",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "newBranch (V1.0.0)"
  },
  {
    "id": "${data.testBranch}",
    "branch": "testBranch",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "testBranch (V3.0.0)"
  },
  {
    "id": "${data.main}",
    "branch": "main",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V5.0.0)"
  },
    {
    "id": "${data.anotherBranch}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "anotherBranch (V5.0.0)"
  }
]"""

        when: 'getting the tree'
        loginReader()
        GET("${data.interestingBranch}/simpleModelVersionTree?forMerge=true", STRING_ARG)

        then:
        verifyResponse OK, jsonCapableResponse
        verifyJson(expectedJson, jsonCapableResponse.body(), false, true)

        cleanup:
        cleanupModelVersionTree(data)
    }

    void 'CORE-RE-26e : Test getting simple versionTreeModel with branches only shows only branches'() {
        given:
        Map data = buildModelVersionTree()
        loginEditor()
        String expectedJson = """[
  {
    "id": "${data.newBranch}",
    "branch": "newBranch",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "newBranch (V1.0.0)"
  },
  {
    "id": "${data.testBranch}",
    "branch": "testBranch",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "testBranch (V3.0.0)"
  },
  {
    "id": "${data.main}",
    "branch": "main",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "main (V5.0.0)"
  },
    {
    "id": "${data.anotherBranch}",
    "branch": "anotherBranch",
    "modelVersion": null,
    "modelVersionTag": null,
    "documentationVersion": "1.0.0",
    "displayName": "anotherBranch (V5.0.0)"
  },
  {
    "id": "${data.interestingBranch}",
    "branch": "interestingBranch",
    "modelVersion": null,
    "modelVersionTag": null,
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

    void 'CORE-#prefix-27a : Test available actions inside a VersionedFolder (as #name)'() {
        given:
        loginCreator()
        POST("folders/${testVersionedFolderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = responseBody().id
        logout()

        when:
        login(name)
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().availableActions == actions - ResourceActions.FINALISE_ACTION

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(id)

        where:
        prefix | name             | actions
        'RE'   | 'Reader'         | expectations.readerAvailableActions
        'RV'   | 'Reviewer'       | expectations.reviewerAvailableActions
        'AU'   | 'Author'         | expectations.authorAvailableActions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-27b : Test available actions inside a Folder inside a VersionedFolder (as #name)'() {
        given:
        loginCreator()
        POST("folders/${testVersionedFolderId}/folders/", [label: 'folder in versionedfolder'], MAP_ARG, true)
        verifyResponse CREATED, response
        String folderId = responseBody().id
        POST("folders/${folderId}/${getResourcePath()}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String id = responseBody().id
        logout()

        when:
        login(name)
        GET("folders/${testVersionedFolderId}", MAP_ARG, true)
        verifyResponse(OK, response)
        GET("folders/${folderId}", MAP_ARG, true)
        verifyResponse(OK, response)
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().availableActions == actions - ResourceActions.FINALISE_ACTION

        cleanup:
        loginCreator()
        DELETE("folders/${folderId}?permanent=true", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        cleanUpRoles(id)

        where:
        prefix | name             | actions
        'RE'   | 'Reader'         | expectations.readerAvailableActions
        'RV'   | 'Reviewer'       | expectations.reviewerAvailableActions
        'AU'   | 'Author'         | expectations.authorAvailableActions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'CORE-#prefix-28 : test changing folder from Model<T> context [not allowed] (as #name)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        login(name)
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        if (name in ['Editor', 'Author']) verifyNotFound response, getTestFolder2Id()
        else if (canRead) verifyForbidden response
        else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
        'ED'   | 'Editor'        | true

    }

    void 'CORE-#prefix-28 : test changing folder from Model<T> context [allowed] (as #name)'() {
        given:
        String id = getValidId()

        when: 'logged in'
        login(name)
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyResponse OK, response

        and:
        getModelFolderId(id) == getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'CORE-#prefix-29 : test changing folder from Folder context [not allowed] (as #name)'() {
        given:
        String id = getValidId()

        when:
        login(name)
        PUT("folders/${getTestFolder2Id()}/${modelUrlType}/$id", [:], MAP_ARG, true)

        then:
        if (name in ['Editor', 'Author']) verifyNotFound response, getTestFolder2Id()
        else if (canRead) verifyForbidden response
        else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
        'ED'   | 'Editor'        | true
    }

    void 'CORE-#prefix-29 : test changing folder from Folder context [allowed] (as #name)'() {
        given:
        String id = getValidId()
        loginCreator()
        // Add a reader share for the editors group directly to the DM
        addShare(id, 'editors', GroupRole.READER_ROLE_NAME)

        when: 'checking access before move logged in as reader'
        loginReader()
        GET(id)

        then:
        verifyResponse OK, response


        when: 'checking access before move logged in as editor'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response

        when: 'logged in as admin'
        login(name)
        PUT("folders/${getTestFolder2Id()}/${modelUrlType}/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        and:
        getModelFolderId(id) == getTestFolder2Id()

        when: 'logged in as reader who has no access to folder 2 and has lost inherited access to DM'
        loginReader()
        GET(id)

        then:
        verifyNotFound response, id

        when: 'logged in as editor no access to folder 2 but has direct DM access'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }


    void 'CORE-#prefix-30 : test diffing 2 Models<T> [not allowed] (as #name)'() {

        when:
        login(name)
        GET("${getLeftHandDiffModelId()}/diff/${getRightHandDiffModelId()}")

        then:
        verifyNotFound response, getLeftHandDiffModelId()

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    void 'CORE-RE-30a : test diffing 2 Models<T> (as reader of LH model)'() {
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when: 'able to read right model only'
        loginReader()
        GET("${getLeftHandDiffModelId()}/diff/${id}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'CORE-RE-30b : test diffing 2 Models<T> (as reader of RH model)'() {
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when:
        loginReader()
        GET("${id}/diff/${getLeftHandDiffModelId()}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'CORE-RE-30c : test diffing 2 Models<T> (as reader of both models)'() {
        when:
        loginReader()
        GET("${getLeftHandDiffModelId()}/diff/${getRightHandDiffModelId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedDiffJson()
    }

    String getExpectedModelTreeVersionString(Map data) {
        """[
  {
    "id": "${data.v1}",
    "label": "Functional Test ${modelType}",
    "branch": null,
    "modelVersion" : "1.0.0",
    "modelVersionTag": null,
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
