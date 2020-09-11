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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.security


import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.FORBIDDEN

/**
 * <pre>
 * Controller: userGroup
 *  |  GET     | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups                 | Action: index
 *  |  DELETE  | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}  | Action: updateApplicationGroupRole
 *  |  PUT     | /api/admin/applicationGroupRoles/${applicationGroupRoleId}/userGroups/${userGroupId}  | Action: updateApplicationGroupRole
 *  |  DELETE  | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}                      | Action: alterMembers
 *  |  PUT     | /api/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}                      | Action: alterMembers
 *  |  POST    | /api/userGroups        | Action: save
 *  |  GET     | /api/userGroups        | Action: index
 *  |  DELETE  | /api/userGroups/${id}  | Action: delete
 *  |  PUT     | /api/userGroups/${id}  | Action: update
 *  |  GET     | /api/userGroups/${id}  | Action: show
 *  |  GET     | /api/${securableResourceDomainType}/${securableResourceId}/groupRoles/${groupRoleId}/userGroups         | Action: index
 *  |  DELETE  | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 *  |  PUT     | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers/${catalogueUserId}  | Action: alterMembers
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.security.UserGroupController
 */
@Integration
@Slf4j
class UserGroupFunctionalSpec extends UserAccessFunctionalSpec {

    GroupRoleService groupRoleService

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')

        CatalogueUser admin = CatalogueUser.findByEmailAddress(userEmailAddresses.admin)
        CatalogueUser editor = CatalogueUser.findByEmailAddress(userEmailAddresses.editor)
        CatalogueUser reader = CatalogueUser.findByEmailAddress(userEmailAddresses.reader)
        // For now to allow existing permissions tests the editor user needs to be group admin role level
        // This will give editor the "editing" rights to usergroups
        UserGroup groupAdmin = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'groupAdmins',
            applicationGroupRole: groupRoleService.getFromCache(GroupRole.GROUP_ADMIN_ROLE_NAME).groupRole)
            .addToGroupMembers(admin).addToGroupMembers(editor)
        checkAndSave(messageSource, groupAdmin)

        // For now to allow existing permissions tests the reader user needs to be container group admin role level
        // This will give editor the "reader" rights to usergroups
        UserGroup containerGroupAdmin = new UserGroup(
            createdBy: userEmailAddresses.functionalTest,
            name: 'containerGroupAdmins',
            applicationGroupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME).groupRole)
            .addToGroupMembers(admin).addToGroupMembers(editor).addToGroupMembers(reader)
        checkAndSave(messageSource, containerGroupAdmin)
    }

    @Transactional
    def cleanupSpec() {
        // Remove all installed required domains here
        UserGroup.findByName('groupAdmins').delete(flush: true)
        UserGroup.findByName('containerGroupAdmins').delete(flush: true)
    }

    @Transactional
    String getUserGroupId(String userGroupName) {
        UserGroup userGroup = UserGroup.findByName(userGroupName)
        userGroup.id
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @OnceBefore
    @Transactional
    @Override
    def addValidIdReaderGroup() {
        log.info('Not adding valid id reader group')
    }

    @Override
    String getResourcePath() {
        'userGroups'
    }

    @Override
    List<String> getPermanentGroupNames() {
        ['groupAdmins', 'containerGroupAdmins', 'administrators', 'readers', 'editors']
    }

    @Override
    Map getValidJson() {
        [
            name: 'testers'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            name: ''
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Testing peoples'
        ]
    }

    @Override
    Boolean getReaderCanCreate() {
        true
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 5,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "editors",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "administrators",
      "createdBy": "admin@maurodatamapper.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "groupAdmins",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "containerGroupAdmins",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    }
  ]
}'''
    }

    @Override
    String getReaderIndexJson() {
        '''{
  "count": 5,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "editors",
      "createdBy": "functional-test@test.com",
      "availableActions": ["show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "administrators",
      "createdBy": "admin@maurodatamapper.com",
      "availableActions": ["show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "groupAdmins",
      "createdBy": "functional-test@test.com",
      "availableActions": ["show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "containerGroupAdmins",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "show"]
    }
  ]
}'''
    }

    @Override
    String getAdminIndexJson() {
        '''{
  "count": 5,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "name": "readers",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "editors",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "administrators",
      "createdBy": "admin@maurodatamapper.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "groupAdmins",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "name": "containerGroupAdmins",
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","delete","show"]
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "name": "testers",
  "createdBy": "editor@test.com",
  "availableActions": [ "update","delete","show"]
}'''
    }

    void 'test getting members of a group'() {
        given:
        String id = getUserGroupId('readers')

        when: 'not logged in'
        GET("${id}/catalogueUsers")

        then:
        verifyNotFound response, id

        when: 'logged in as editor'
        loginEditor()
        GET("${id}/catalogueUsers", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 3,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "reader@test.com",
      "firstName": "reader",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com"
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "author@test.com",
      "firstName": "author",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com"
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "reviewer@test.com",
      "firstName": "reviewer",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com"
    }
  ]
}'''
    }

    void 'test adding a user to a group'() {
        given:
        def id = getValidId()
        String reviewerId = getUserByEmailAddress(userEmailAddresses.reviewer).id.toString()
        String authenticatedId = getUserByEmailAddress(userEmailAddresses.authenticated).id.toString()

        when: 'not logged in'
        PUT("${id}/catalogueUsers/${authenticatedId}", [:])

        then:
        verifyNotFound response, id

        when: 'logged in as editor who created the group'
        loginEditor()
        PUT("${id}/catalogueUsers/${authenticatedId}", [:], STRING_ARG)

        then:
        verifyJsonResponse OK, showJson

        when: 'listing edits for the group, expect to see 2 edits. One create and one PUT.'
        GET("$id/edits", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "[UserGroup:testers] created"
    },
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    }
  ]
}'''

        when: 'listing edits for the user who was added to the group'
        GET("${baseUrl}catalogueUsers/${authenticatedId}/edits", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
     {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    }
  ]
}'''

        when: 'logged in as user in group'
        loginAuthenticated()
        PUT("${id}/catalogueUsers/${reviewerId}", [:])

        then:
        verifyNotFound response, id

        when:
        GET("$id/catalogueUsers")

        then:
        verifyNotFound response, id

        when:
        loginEditor()
        GET("$id/catalogueUsers", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "authenticated@test.com",
      "firstName": "authenticated",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com"
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "editor@test.com",
      "firstName": "editor",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com"
    }
  ]
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'test removing a user from a group'() {
        given:
        def id = getValidId()
        String reviewerId = getUserByEmailAddress(userEmailAddresses.reviewer).id.toString()
        String authenticatedId = getUserByEmailAddress(userEmailAddresses.authenticated).id.toString()
        loginEditor()
        PUT("${id}/catalogueUsers/${authenticatedId}", [:])
        verifyResponse OK, response
        PUT("${id}/catalogueUsers/${reviewerId}", [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        DELETE("${id}/catalogueUsers/${authenticatedId}")

        then:
        verifyNotFound response, id

        when: 'logged in as editor'
        loginEditor()
        DELETE("${id}/catalogueUsers/${reviewerId}", STRING_ARG)

        then:
        verifyJsonResponse OK, showJson

        when: 'listing edits for the group expect to see 4 edits. One create, two PUTs and a DELETE'
        GET("$id/edits", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 4,
  "items": [
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "[UserGroup:testers] created"
    },
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    },
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    },
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    }
  ]
}'''
        when: 'listing edits for the user who was removed from the group'
        GET("${baseUrl}catalogueUsers/${reviewerId}/edits", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
     {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    },
    {
      "dateCreated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "${json-unit.any-string}"
    }
  ]
}'''

        when: 'logged in as user in group'
        loginEditor()
        PUT("${id}/catalogueUsers/${reviewerId}", [:])
        verifyResponse OK, response
        loginAuthenticated()
        DELETE("${id}/catalogueUsers/${reviewerId}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'test getting user groups with application level roles'() {
        given:
        GroupRole role = groupRoleService.getFromCache(GroupRole.GROUP_ADMIN_ROLE_NAME).groupRole

        when: 'not logged in'
        GET("admin/applicationGroupRoles/${role.id}/userGroups", MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as reader'
        loginReader()
        GET("admin/applicationGroupRoles/${role.id}/userGroups", MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as editor'
        loginEditor()
        GET("admin/applicationGroupRoles/${role.id}/userGroups", MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as admin'
        loginAdmin()
        GET("admin/applicationGroupRoles/${role.id}/userGroups", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().name == 'groupAdmins'
        response.body().items.first().createdBy == 'functional-test@test.com'
    }

    void 'test setting the application level role'() {
        given:
        GroupRole role = groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME).groupRole
        def id = getValidId()

        when: 'not logged in'
        PUT("admin/applicationGroupRoles/${role.id}/userGroups/${id}", [:], MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as reader'
        loginReader()
        PUT("admin/applicationGroupRoles/${role.id}/userGroups/${id}", [:], MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as editor'
        loginEditor()
        PUT("admin/applicationGroupRoles/${role.id}/userGroups/${id}", [:], MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as admin'
        loginAdmin()
        PUT("admin/applicationGroupRoles/${role.id}/userGroups/${id}", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("admin/applicationGroupRoles/${role.id}/userGroups", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().name == 'testers'
        response.body().items.first().createdBy == 'editor@test.com'

        cleanup:
        removeValidIdObject(id)
    }

    void 'test removing the application level role'() {
        given:
        GroupRole role = groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME).groupRole
        def id = getValidId()
        loginAdmin()
        PUT("admin/applicationGroupRoles/${role.id}/userGroups/${id}", [:], MAP_ARG, true)
        logout()

        when: 'not logged in'
        DELETE("admin/applicationGroupRoles/${role.id}/userGroups/${id}", MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as reader'
        loginReader()
        DELETE("admin/applicationGroupRoles/${role.id}/userGroups/${id}", MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as editor'
        loginEditor()
        DELETE("admin/applicationGroupRoles/${role.id}/userGroups/${id}", MAP_ARG, true)

        then:
        verifyForbidden response

        when: 'logged in as admin'
        loginAdmin()
        DELETE("admin/applicationGroupRoles/${role.id}/userGroups/${id}", MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("admin/applicationGroupRoles/${role.id}/userGroups", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 0

        cleanup:
        removeValidIdObject(id)
    }

    void 'A04 : Test when logged in as admin the delete action does not delete a user group marked as undeleteable'() {
        given:
        String id = getUserGroupId('administrators')
        loginAdmin()

        when: 'When the delete action is executed on the administrators user group, which is undeleteable'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }
}