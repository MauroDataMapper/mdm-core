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

    String getEditorGroupRoleName() {
        GroupRole.EDITOR_ROLE_NAME
    }

    List<String> getEditorAvailableActions() {
        ['show', 'update', 'delete']
    }

    List<String> getReaderAvailableActions() {
        ['show']
    }

    String getReader1JsonString() {
        '''{
      "firstName": "reader1",
      "lastName": "User",
      "emailAddress": "reader1@test.com",
      "needsToResetPassword": true,
      "disabled": false,
      "id": "\\\${json-unit.matches:id}",
      "userRole": "READER"
    },'''
    }

    String getReaderGroupJsonString() {
        '''{
      "createdBy": {
        "firstName": "editor",
        "lastName": "User",
        "emailAddress": "editor@test.com",
        "disabled": false,
        "id": "\\\${json-unit.matches:id}",
        "userRole": "EDITOR"
      },
      "id": "\\\${json-unit.matches:id}",
      "label": "readers"
    }'''
    }

    int getExpectedCountOfGroupsWithAccess() {
        1
    }

    /*
    * Logged in as editor testing
    */

    void 'E06 : test adding readable by everyone (as editor)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginEditor()

        when: 'getting the list of groups'
        GET("$id/securableResourceGroupRoles")

        then:
        verifyResponse(OK, response)
        responseBody().count == getExpectedCountOfGroupsWithAccess()

        when: 'logged in as user with write access add the read by everyone'
        PUT(endpoint, [:])

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == true

        when: 'getting the list of groups'
        GET("$id/securableResourceGroupRoles")

        then:
        verifyResponse(OK, response)
        responseBody().count == getExpectedCountOfGroupsWithAccess()

        cleanup:
        removeValidIdObject(id)
    }

    void 'E07 : test removing readable by everyone (as editor)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        response.body().readableByEveryone == true
        logout()

        when: 'logged in as user with write access'
        loginEditor()
        DELETE(endpoint)

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == false

        cleanup:
        removeValidIdObject(id)
    }

    void 'E08 : test accessing when readable by everyone (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with write access'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByEveryone == true
        response.body().availableActions == getEditorAvailableActions().sort()

        when: 'removing readable by everyone'
        loginEditor()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByEveryone == false
        response.body().availableActions == getEditorAvailableActions().sort()

        cleanup:
        removeValidIdObject(id)
    }

    /*
     * Logged out testing
     */

    void 'L06 : test adding readable by everyone (not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/readByEveryone", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'L07 : test removing readable by everyone (not logged in)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        DELETE(endpoint)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'L08 : test accessing when readable by everyone (not logged in)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        GET(id)

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == true
        response.body().availableActions == ['show']

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

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N06 : test adding readable by everyone (as no access/authenticated)'() {
        given:
        String id = getValidId()

        when: 'logged in as user with no access'
        loginAuthenticated()
        PUT("$id/readByEveryone", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N07 : test removing readable by everyone (as no access/authenticated)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with no access'
        loginAuthenticated()
        DELETE(endpoint)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'N08 : test accessing when readable by everyone (as no access/authenticated)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with no access'
        loginAuthenticated()
        GET(id)

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == true
        response.body().availableActions == ['show']

        when: 'removing readable by everyone'
        loginEditor()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        loginAuthenticated()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R06 : test adding readable by everyone (as reader)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"

        when: 'logged in as user with read access'
        loginReader()
        PUT(endpoint, [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'R07 : test removing readable by everyone (as reader)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with read access'
        loginReader()
        DELETE(endpoint)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'R08 : test accessing when readable by everyone (as reader)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with read access'
        loginReader()
        GET(id)

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == true
        response.body().availableActions == getReaderAvailableActions().sort()

        when: 'removing readable by everyone'
        loginEditor()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        loginReader()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByEveryone == false
        response.body().availableActions == getReaderAvailableActions().sort()


        cleanup:
        removeValidIdObject(id)
    }

    /*
    * Logged in as admin testing
    */

    void 'A06 : test adding readable by everyone (as admin)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"

        when: 'logged in as user with write access'
        loginAdmin()
        PUT(endpoint, [:])

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == true

        cleanup:
        removeValidIdObject(id)
    }

    void 'A07 : test removing readable by everyone (as admin)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByEveryone"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        response.body().readableByEveryone == true
        logout()

        when: 'logged in as user with write access'
        loginAdmin()
        DELETE(endpoint)

        then:
        verifyResponse(OK, response)
        response.body().readableByEveryone == false

        cleanup:
        removeValidIdObject(id)
    }

    void 'A08 : test accessing when readable by everyone (as admin)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with write access'
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByEveryone == true
        response.body().availableActions == getEditorAvailableActions().sort()

        when: 'removing readable by everyone'
        loginEditor()
        DELETE("$id/readByEveryone", [:])
        verifyResponse OK, response
        logout()

        and:
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByEveryone == false
        response.body().availableActions == getEditorAvailableActions().sort()

        cleanup:
        removeValidIdObject(id)
    }

    /*
    * Logged in as editor testing
    */

    void 'E09 : test adding readable by authenticated (as editor)'() {
        given:
        String id = getValidId()
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

    void 'E10 : test removing readable by authenticated (as editor)'() {
        given:
        String id = getValidId()
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

    void 'E11 : test accessing when readable by authenticated (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with write access'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == true
        response.body().availableActions == getEditorAvailableActions().sort()

        when: 'removing readable by everyone'
        loginEditor()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == false
        response.body().availableActions == getEditorAvailableActions().sort()

        cleanup:
        removeValidIdObject(id)
    }

    /*
     * Logged out testing
     */

    void 'L09 : test adding readable by authenticated (not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/readByAuthenticated", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'L10 : test removing readable by authenticated (not logged in)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        DELETE(endpoint)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'L11 : test accessing when readable by authenticated (not logged in)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        GET(id)

        then:
        verifyNotFound response, id

        when: 'removing readable by authenticated'
        loginEditor()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N09 : test adding readable by authenticated (as no access/authenticated)'() {
        given:
        String id = getValidId()

        when: 'logged in as user with no access'
        loginAuthenticated()
        PUT("$id/readByAuthenticated", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N10 : test removing readable by authenticated (as no access/authenticated)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with no access'
        loginAuthenticated()
        DELETE(endpoint)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'N11 : test accessing when readable by authenticated (as no access/authenticated)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with no access'
        loginAuthenticated()
        GET(id)

        then:
        verifyResponse(OK, response)
        response.body().readableByAuthenticatedUsers == true
        response.body().availableActions == ['show']

        when: 'removing readable by authenticated'
        loginEditor()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        loginAuthenticated()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R09 : test adding readable by authenticated (as reader)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"

        when: 'logged in as user with read access'
        loginReader()
        PUT(endpoint, [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'R10 : test removing readable by authenticated (as reader)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with read access'
        loginReader()
        DELETE(endpoint)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'R11 : test accessing when readable by authenticated (as reader)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with read access'
        loginReader()
        GET(id)

        then:
        verifyResponse(OK, response)
        response.body().readableByAuthenticatedUsers == true
        response.body().availableActions == getReaderAvailableActions().sort()

        when: 'removing readable by authenticated'
        loginEditor()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        loginReader()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == false
        response.body().availableActions == getReaderAvailableActions().sort()

        cleanup:
        removeValidIdObject(id)
    }

    /*
    * Logged in as admin testing
    */

    void 'A09 : test adding readable by authenticated (as admin)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"

        when: 'logged in as user with write access'
        loginAdmin()
        PUT(endpoint, [:])

        then:
        verifyResponse(OK, response)
        response.body().readableByAuthenticatedUsers == true

        cleanup:
        removeValidIdObject(id)
    }

    void 'A10 : test removing readable by authenticated (as admin)'() {
        given:
        String id = getValidId()
        def endpoint = "$id/readByAuthenticated"
        loginEditor()
        PUT(endpoint, [:])
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == true
        logout()

        when: 'logged in as user with write access'
        loginAdmin()
        DELETE(endpoint)

        then:
        verifyResponse(OK, response)
        response.body().readableByAuthenticatedUsers == false

        cleanup:
        removeValidIdObject(id)
    }

    void 'A11 : test accessing when readable by authenticated (as admin)'() {
        given:
        String id = getValidId()
        loginEditor()
        PUT("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        when: 'logged in as user with write access'
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == true
        response.body().availableActions == getEditorAvailableActions().sort()

        when: 'removing readable by authenticated'
        loginEditor()
        DELETE("$id/readByAuthenticated", [:])
        verifyResponse OK, response
        logout()

        and:
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().readableByAuthenticatedUsers == false
        response.body().availableActions == getEditorAvailableActions().sort()

        cleanup:
        removeValidIdObject(id)
    }

    /*
   * Logged in as editor testing
   */

    void 'E12 : test adding reader share (as editor)'() {
        given:
        String id = getValidId()
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
        response.body().availableActions == ['show']
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    void 'E13 : test removing reader share (as editor)'() {
        given:
        String id = getValidId()
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

    /*
   * Logged in as not logged in testing
   */

    void 'L12 : test adding reader share (not logged in)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'not logged in'
        POST(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'L13 : test removing reader share (not logged in)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'not logged in'
        DELETE(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader permission not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == ['show']
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    /*
  * Logged in as no access/authenticated user
  */

    void 'N12 : test adding reader share (no access/authenticated)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as user'
        loginAuthenticated()
        POST(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N13 : test removing reader share (no access/authenticated)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as user'
        loginAuthenticated()
        DELETE(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader permission not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == ['show']
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    /*
  * Logged in as reader
  */

    void 'R12 : test adding reader share (as reader)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as user'
        loginReader()
        POST(endpoint, [:])

        then:
        verifyForbidden response

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R13 : test removing reader share (as reader)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as user'
        loginReader()
        DELETE(endpoint, [:])

        then:
        verifyForbidden response

        when: 'getting item as new reader permission not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == ['show']
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    /*
   * Logged in as admin testing
   */

    void 'A12 : test adding reader share (as admin)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as admin'
        loginAdmin()
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
        response.body().availableActions == ['show']
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    void 'A13 : test removing reader share (as admin)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(GroupRole.READER_ROLE_NAME).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as admin'
        loginAdmin()
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

    /*
  * Logged in as editor testing
  */

    void 'E14 : test adding "editor" share (as editor)'() {
        given:
        String id = getValidId()
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
        response.body().availableActions == getEditorAvailableActions().sort()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    void 'E15 : test removing "editor" share (as editor)'() {
        given:
        String id = getValidId()
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

    /*
   * Logged in as not logged in testing
   */

    void 'L14 : test adding "editor" share (not logged in)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'not logged in'
        POST(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'L15 : test removing "editor" share (not logged in)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'not logged in'
        DELETE(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader access not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == getEditorAvailableActions().sort()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    /*
  * Logged in as no access/authenticated user
  */

    void 'N14 : test adding "editor" share (no access/authenticated)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as user'
        loginAuthenticated()
        POST(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N15 : test removing "editor" share (no access/authenticated)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as user'
        loginAuthenticated()
        DELETE(endpoint, [:])

        then:
        verifyNotFound response, id

        when: 'getting item as new reader access not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == getEditorAvailableActions().sort()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    /*
  * Logged in as reader
  */

    void 'R14 : test adding "editor" share (as reader)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as user'
        loginReader()
        POST(endpoint, [:])

        then:
        verifyForbidden response

        when: 'getting item as new reader'
        loginAuthenticated2()
        GET(id)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R15 : test removing "editor" share (as reader)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as user'
        loginReader()
        DELETE(endpoint, [:])

        then:
        verifyForbidden response

        when: 'getting item as new reader access not revoked'
        loginAuthenticated2()
        GET(id)

        then:
        verifyResponse OK, response
        response.body().availableActions == getEditorAvailableActions().sort()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    /*
   * Logged in as admin testing
   */

    void 'A14 : test adding "editor" share (as admin)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"

        when: 'logged in as admin'
        loginAdmin()
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
        response.body().availableActions == getEditorAvailableActions().sort()
        response.body().id == id

        cleanup:
        removeValidIdObject(id)
    }

    void 'A15 : test removing "editor" share (as admin)'() {
        given:
        String id = getValidId()
        String groupRoleId = getGroupRole(getEditorGroupRoleName()).id.toString()
        String readerGroupId = getUserGroup('extraGroup').id.toString()
        String endpoint = "$id/groupRoles/$groupRoleId/userGroups/$readerGroupId"
        loginEditor()
        POST(endpoint, [:])
        logout()

        when: 'logged in as admin'
        loginAdmin()
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
}
