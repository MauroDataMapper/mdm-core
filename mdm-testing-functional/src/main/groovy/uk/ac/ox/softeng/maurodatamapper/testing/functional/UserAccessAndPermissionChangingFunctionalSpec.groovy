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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils

import grails.gorm.transactions.Transactional
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * Tests the permissions settings
 * <pre>
 *  | DELETE | /api/${resourcePath}/${id}/readByAuthenticated | Action: readByAuthenticated
 *  | PUT    | /api/${resourcePath}/${id}/readByAuthenticated | Action: readByAuthenticated
 *  | DELETE | /api/${resourcePath}/${id}/readByEveryone      | Action: readByEveryone
 *  | PUT    | /api/${resourcePath}/${id}/readByEveryone      | Action: readByEveryone
 *  | PUT    | /api/${resourcePath}/${id}                     | Action: update |
 *  | POST   | /api/${resourcePath}                           | Action: save   | [inherited test]
 *  | DELETE | /api/${resourcePath}/${id}                     | Action: delete | [inherited test]
 *  | GET    | /api/${resourcePath}                           | Action: index  | [inherited test]
 *  | GET    | /api/${resourcePath}/${id}                     | Action: show   | [inherited test]
 *
 *  |  DELETE  | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}  | Action: delete
 *  |  POST    | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups/${userGroupId}  | Action: save
 *  </pre>
 */
@Slf4j
abstract class UserAccessAndPermissionChangingFunctionalSpec extends UserAccessFunctionalSpec {

    @RunOnce
    @Transactional
    def setup() {
        log.info('Add group with new authenticated user')
        CatalogueUser authenticated2 = new CatalogueUser(emailAddress: userEmailAddresses.authenticated2,
                                                         firstName: 'authenticated2', lastName: 'User',
                                                         createdBy: userEmailAddresses.functionalTest,
                                                         tempPassword: SecurityUtils.generateRandomPassword())
        // To allow testing of reader group rights which reader2 is not in
        UserGroup group = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'extraGroup',
            ).addToGroupMembers(authenticated2)
        checkAndSave(messageSource, group)
    }

    @Transactional
    def cleanupSpec() {
        log.info('Removing group with new authenticated user')
        UserGroup.findByName('extraGroup').delete(flush: true)
        CatalogueUser.findByEmailAddress(userEmailAddresses.authenticated2).delete(flush: true)
    }

    @Override
    List<String> getPermanentGroupNames() {
        super.getPermanentGroupNames() + ['extraGroup']
    }

    int getExpectedCountOfGroupsWithAccess() {
        6
    }

    /*
    * Logged in as permission changing allowed user testing
    */

    void 'CORE-#prefix-06 : test adding readable by everyone [allowed] (as #name)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        login(name)

        when: 'getting the list of groups'
        GET("$id/securableResourceGroupRoles")

        then:
        verifyResponse(OK, response)
        responseBody().count == getExpectedCountOfGroupsWithAccess()

        when: 'logged in as user with write access add the read by everyone'
        PUT(endpoint, [:])

        then:
        verifyResponse(OK, response)
        responseBody().readableByEveryone == true

        when: 'getting the list of groups'
        GET("$id/securableResourceGroupRoles")

        then:
        verifyResponse(OK, response)
        responseBody().count == getExpectedCountOfGroupsWithAccess()

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-07 : test removing readable by everyone [allowed] (as container admin)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginCreator()
        PUT(endpoint, [:])
        verifyResponse OK, response
        responseBody().readableByEveryone == true
        logout()

        when: 'logged in as user with write access'
        login(name)
        DELETE(endpoint)

        then:
        verifyResponse(OK, response)
        responseBody().readableByEveryone == false

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-08 : test accessing when readable by everyone [allowed] (as container admin)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with write access'
        login(name)
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().readableByEveryone == true
        responseBody().availableActions == actions.sort()

        when: 'removing readable by everyone'
        loginCreator()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        login(name)
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().readableByEveryone == false
        responseBody().availableActions == actions.sort()

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | actions
        'CA'   | 'ContainerAdmin' | expectations.getContainerAdminAvailableActions()
        'AD'   | 'Admin'          | expectations.getContainerAdminAvailableActions()
        'ED'   | 'Editor'         | expectations.getEditorAvailableActions()
    }


    void 'CORE-#prefix-09 : test adding readable by authenticated [allowed] (as #name)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
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
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-10 : test removing readable by authenticated [allowed] (as #name)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"
        loginCreator()
        PUT(endpoint, [:])
        verifyResponse OK, response
        responseBody().readableByAuthenticatedUsers == true
        logout()

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
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-11 : test accessing when readable by authenticated [allowed] (as #name)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with write access'
        login(name)
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().readableByAuthenticatedUsers == true
        responseBody().availableActions == actions.sort()

        when: 'removing readable by everyone'
        loginCreator()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        login(name)
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().readableByAuthenticatedUsers == false
        responseBody().availableActions == actions.sort()

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | actions
        'CA'   | 'ContainerAdmin' | expectations.getContainerAdminAvailableActions()
        'AD'   | 'Admin'          | expectations.getContainerAdminAvailableActions()
        'ED'   | 'Editor'         | expectations.getEditorAvailableActions()
    }

    void 'CORE-#prefix-12 : test adding reader share [allowed] (as #name)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String groupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$groupId"

        when: 'logged in as writer'
        login(name)
        POST(endpoint, [:])

        then:
        verifyResponse CREATED, response
        responseBody().securableResourceId == id
        responseBody().userGroup.id == groupId
        responseBody().groupRole.id == groupRoleId

        when: 'list user groups'
        loginAdmin()
        GET("$id/groupRoles/$groupRoleId/userGroups")

        then:
        verifyResponse OK, response
        responseBody().items.find{it.id == groupId && it.name == "extraGroup"}

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == expectations.getReaderAvailableActions()
        responseBody().id == id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-13 : test removing reader share [allowed] (as #name)'() {
        if (name == 'Editor' && !expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String groupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$groupId"
        loginCreator()
        POST(endpoint, [:])
        logout()

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
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
        'ED'   | 'Editor'
    }

    /**
     * Testing when logged in as a not allowed to change permissions user
     */
    void 'CORE-#prefix-06 : test adding readable by everyone [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()

        when: 'logged in as user with read access'
        login(name)
        PUT("$id/readByEveryone", [:])

        then:
        if (canRead) verifyForbidden response
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
    }

    void 'CORE-#prefix-07 : test removing readable by everyone [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with read access'
        login(name)
        DELETE("$id/readByEveryone")

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-08 : test accessing when readable by everyone [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with read access'
        login(name)
        GET(id)

        then:
        verifyResponse(OK, response)
        responseBody().readableByEveryone == true
        responseBody().availableActions == actions.sort()

        when: 'removing readable by everyone'
        loginCreator()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        login(name)
        GET(id)

        then:
        if (canRead) {
            verifyResponse OK, response
            responseBody().readableByEveryone == false
            responseBody().availableActions == actions.sort()
        } else verifyNotFound response, id


        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead | actions
        'LO'   | null            | false   | expectations.getReaderAvailableActions()
        'NA'   | 'Authenticated' | false   | expectations.getReaderAvailableActions()
        'RE'   | 'Reader'        | true    | expectations.getReaderAvailableActions()
        'RV'   | 'Reviewer'      | true    | expectations.getReviewerAvailableActions()
        'AU'   | 'Author'        | true    | expectations.getAuthorAvailableActions()
        'ED'   | 'Editor'        | true    | expectations.getEditorAvailableActions()
    }

    void 'CORE-#prefix-09 : test adding readable by authenticated [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()

        when: 'logged in as user with no access'
        login(name)
        PUT("$id/readByAuthenticated", [:])

        then:
        if (canRead) verifyForbidden response
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

    void 'CORE-#prefix-10 : test removing readable by authenticated [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:

        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"
        loginCreator()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with no access'
        login(name)
        DELETE(endpoint)

        then:
        if (name) verifyForbidden response
        else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
        'RE'   | 'Reader'
        'RV'   | 'Reviewer'
        'AU'   | 'Author'
        'ED'   | 'Editor'
    }

    void 'CORE-#prefix-11 : test accessing when readable by authenticated [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        loginCreator()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with no access'
        login(name)
        GET(id)

        then:
        if (name) {
            verifyResponse(OK, response)
            responseBody().readableByAuthenticatedUsers == true
            responseBody().availableActions == actions.sort()
        } else verifyNotFound response, id

        when: 'removing readable by authenticated'
        loginCreator()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        login(name)
        GET(id)

        then:
        if (canRead) {
            verifyResponse(OK, response)
            responseBody().readableByAuthenticatedUsers == false
            responseBody().availableActions == actions.sort()
        } else verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name            | canRead | actions
        'LO'   | null            | false   | []
        'NA'   | 'Authenticated' | false   | expectations.getReaderAvailableActions()
        'RE'   | 'Reader'        | true    | expectations.getReaderAvailableActions()
        'RV'   | 'Reviewer'      | true    | expectations.getReviewerAvailableActions()
        'AU'   | 'Author'        | true    | expectations.getAuthorAvailableActions()
        'ED'   | 'Editor'        | true    | expectations.getEditorAvailableActions()
    }

    void 'CORE-#prefix-12 : test adding reader share [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as user'
        login(name)
        POST(endpoint, [:])

        then:
        if (canRead) verifyForbidden response
        else verifyNotFound response, id

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

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

    void 'CORE-#prefix-13 : test removing reader share [not allowed] (as #name)'() {
        if (name == 'Editor' && expectations.editorCanChangePermissions) return
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginCreator()
        POST(endpoint, [:])
        logout()

        when: 'logged in as user'
        login(name)
        DELETE(endpoint, [:])

        then:
        if (canRead) verifyForbidden response
        else verifyNotFound response, id

        when: 'getting item as new reader permission not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == expectations.readerAvailableActions
        responseBody().id == id

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
}
