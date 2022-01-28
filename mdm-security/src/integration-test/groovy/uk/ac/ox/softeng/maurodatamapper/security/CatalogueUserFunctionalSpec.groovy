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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.UserSecurityPolicy
import uk.ac.ox.softeng.maurodatamapper.security.policy.UserSecurityPolicyService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.spockframework.util.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see CatalogueUserController* Controller: catalogueUser
 *  | POST   | /api/catalogueUsers/search                                 | Action: search |
 *  | GET    | /api/catalogueUsers/search/${searchTerm}?                  | Action: search |
 *  | POST   | /api/catalogueUsers/adminRegister                          | Action: adminRegister |
 *  | GET    | /api/catalogueUsers/pending                                | Action: pending |
 *  | GET    | /api/catalogueUsers/userExists/${emailAddress}             | Action: userExists |
 *  | PUT    | /api/catalogueUsers/${catalogueUserId}/adminPasswordReset  | Action: adminPasswordReset |
 *  | GET    | /api/catalogueUsers/${catalogueUserId}/resetPasswordLink   | Action: sendPasswordResetLink |
 *  | PUT    | /api/catalogueUsers/${catalogueUserId}/rejectRegistration  | Action: rejectRegistration |
 *  | PUT    | /api/catalogueUsers/${catalogueUserId}/approveRegistration | Action: approveRegistration |
 *  | PUT    | /api/catalogueUsers/${catalogueUserId}/changePassword      | Action: changePassword |
 *  | PUT    | /api/catalogueUsers/${catalogueUserId}/userPreferences     | Action: updateuserPreferences |
 *  | GET    | /api/catalogueUsers/${catalogueUserId}/userPreferences     | Action: userPreferences |
 *  | POST   | /api/catalogueUsers                                        | Action: save |
 *  | GET    | /api/catalogueUsers                                        | Action: index |
 *  | DELETE | /api/catalogueUsers/${id}                                  | Action: delete |
 *  | PUT    | /api/catalogueUsers/${id}                                  | Action: update |
 *  | GET    | /api/catalogueUsers/${id}                                  | Action: show |
 */
@Integration
@Slf4j

class CatalogueUserFunctionalSpec extends BaseFunctionalSpec implements SecurityUsers {

    @Autowired
    ApplicationContext applicationContext
    @Autowired
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService
    @Autowired
    GroupRoleService groupRoleService
    @Autowired
    UserSecurityPolicyService userSecurityPolicyService

    String getResourcePath() {
        ''
    }

    Map getValidJson(String emailId) {
        [
            emailAddress: "${emailId}@functional-test.com".toString(),
            password    : 'password',
            firstName   : 'a',
            lastName    : 'new user'
        ]
    }

    Map getInvalidJson() {
        [
            emailAddress: 'reader@test.com',
            password    : 'password',
            firstName   : 'a',
            lastName    : 'new user'
        ]
    }

    String adminRegisterNewUser() {
        POST('admin/catalogueUsers/adminRegister', getValidJson(UUID.randomUUID().toString()), MAP_ARG, true)
        verifyResponse CREATED, response
        assert response.body().id
        response.body().id
    }

    String selfRegisterNewUser(String emailId = UUID.randomUUID().toString()) {
        reconfigureDefaultUserPrivileges(false)
        POST('catalogueUsers', getValidJson(emailId))
        verifyResponse CREATED, response
        assert response.body().id
        reconfigureDefaultUserPrivileges(true)
        response.body().id
    }

    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        if (CatalogueUser.count() == 2) {
            implementSecurityUsers('functionalTest')
        }
        assert CatalogueUser.count() == 9
        reconfigureDefaultUserPrivileges(true)
    }

    @Transactional
    def cleanup() {
        reconfigureDefaultUserPrivileges(false)
    }

    @Transactional
    void reconfigureDefaultUserPrivileges(boolean accessGranted) {

        GroupBasedUserSecurityPolicyManager defaultUserSecurityPolicyManager = applicationContext.getBean(
            MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME, GroupBasedUserSecurityPolicyManager)
        defaultUserSecurityPolicyManager.lock()
        if (accessGranted) {
            VirtualGroupRole applicationLevelRole = groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME)
            defaultUserSecurityPolicyManager.withUpdatedUserPolicy(UserSecurityPolicy.builder()
                                                                       .forUser(CatalogueUser.findByEmailAddress(userEmailAddresses.admin))
                                                                       .withApplicationRoles([applicationLevelRole.groupRole] as HashSet)
                                                                       .withVirtualRoles(
                                                                           userSecurityPolicyService.buildCatalogueUserVirtualRoles(
                                                                               [applicationLevelRole.groupRole] as HashSet)
                                                                       )
            )
        } else {
            defaultUserSecurityPolicyManager.withUpdatedUserPolicy(
                userSecurityPolicyService.buildUserSecurityPolicy(UserSecurityPolicy.builder()
                                                                      .forUser(CatalogueUser.findByEmailAddress(UnloggedUser.instance.emailAddress))
                                                                      .withApplicationRoles([] as HashSet))
            )
        }
        groupBasedSecurityPolicyManagerService.storeUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
    }

    @Transactional
    def cleanupSpec() {
        CatalogueUser.list().findAll {
            !(it.emailAddress in [UnloggedUser.UNLOGGED_EMAIL_ADDRESS, StandardEmailAddress.ADMIN])
        }.each {it.delete(flush: true)}
        if (CatalogueUser.count() != 2) {
            Assert.fail("Resource Class ${CatalogueUser.simpleName} has not been emptied")
        }
    }

    @Transactional
    UUID getEditorId() {
        CatalogueUser.findByEmailAddress(userEmailAddresses.editor).id
    }

    @Transactional
    CatalogueUser getUser(String id) {
        CatalogueUser.get(id)
    }

    @Transactional
    void cleanupUser(String id) {
        (CatalogueUser.findByEmailAddress(id) ?: CatalogueUser.get(id)).delete(flush: true)
    }

    String getSelfRegisteredJson(String emailId) {
        '''{
  "firstName": "a",
  "lastName": "new user",
  "emailAddress": "''' + emailId + '''@functional-test.com",
  "createdBy": "''' + emailId + '''@functional-test.com",
  "availableActions": ["show"],
  "disabled": false,
  "id": "${json-unit.matches:id}",
  "creationMethod": "Standard",
  "pending": true
}'''
    }

    void "1 : Test the index action"() {

        when:
        GET('catalogueUsers', STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse OK, '''{
  "count": 9,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "container_admin@test.com",
      "firstName": "containerAdmin",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "authenticated@test.com",
      "firstName": "authenticated",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "reader@test.com",
      "firstName": "reader",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "admin@maurodatamapper.com",
      "firstName": "Admin",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "admin@maurodatamapper.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"],
      "organisation": "Oxford BRC Informatics",
      "jobTitle": "God",
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "administrators"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "unlogged_user@mdm-core.com",
      "firstName": "Unlogged",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "unlogged_user@mdm-core.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "pending@test.com",
      "firstName": "pending",
      "lastName": "User",
      "pending": true,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"],
      "organisation": "Oxford",
      "jobTitle": "tester"
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "reviewer@test.com",
      "firstName": "reviewer",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "editor@test.com",
      "firstName": "editor",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "author@test.com",
      "firstName": "author",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com",
      "creationMethod": "Standard",
      "availableActions": ["update","disable","show"]
    }
  ]
}'''
    }

    void "2 : Test the save/self registration action correctly persists an instance"() {
        given:
        reconfigureDefaultUserPrivileges(false)

        when: "The save action is executed with no content"
        POST('catalogueUsers', [:])

        then: "The response is correct"
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with invalid data"
        POST('catalogueUsers', invalidJson)

        then: "The response is correct"
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with valid data"
        String emailId = UUID.randomUUID().toString()
        POST('catalogueUsers', getValidJson(emailId), STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, getSelfRegisteredJson(emailId)

        cleanup:
        cleanupUser("${emailId}@functional-test.com")
    }

    void "3 : Test the admin registration action correctly persists an instance"() {
        given:
        String endpoint = 'admin/catalogueUsers/adminRegister'

        when: "The admin registration  action is executed with no content"
        POST(endpoint, [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with invalid data"
        POST(endpoint, invalidJson)

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with valid data"
        String emailId = UUID.randomUUID().toString()
        POST(endpoint, getValidJson(emailId), STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, '''{
  "firstName": "a",
  "lastName": "new user",
  "needsToResetPassword": true,
  "emailAddress": "''' + emailId + '''@functional-test.com",
  "availableActions": ["disable","show", "update"],
  "disabled": false,
  "id": "${json-unit.matches:id}",
  "createdBy": "admin@maurodatamapper.com",
  "creationMethod": "Standard",
  "pending": false
}'''

        cleanup:
        cleanupUser("${emailId}@functional-test.com")
    }

    void "4 : Test the update action correctly updates an instance"() {
        given:
        String id = adminRegisterNewUser()
        Map update = [firstName: 'hello']

        when:
        PUT("catalogueUsers/$id", update)

        then: "The response is correct"
        verifyResponse OK, response

        and:
        response.body().firstName == 'hello'

        cleanup:
        cleanupUser(id)
    }

    void "5 : Test the show action correctly renders an instance"() {
        given:
        String id = getEditorId().toString()

        when:
        GET("catalogueUsers/$id", STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse OK, '''{
  "firstName": "editor",
  "lastName": "User",
  "emailAddress": "editor@test.com",
  "availableActions": ["disable","show", "update"],
  "pending": false,
  "createdBy": "functional-test@test.com",
  "disabled": false,
  "id": "${json-unit.matches:id}",
  "creationMethod": "Standard"
}'''
    }

    void "6 : Test the delete action correctly deletes an instance"() {
        given:
        String id = adminRegisterNewUser()

        when: "When the delete action is executed on an unknown instance"
        DELETE("catalogueUsers/${UUID.randomUUID()}")

        then: "The response is correct"
        verifyResponse NOT_FOUND, response

        when: "When the delete action is executed on an existing instance"
        DELETE("catalogueUsers/$id")

        then: "The response is correct"
        verifyResponse OK, response

        and:
        response.body().disabled == true

        cleanup:
        cleanupUser(id)
    }

    void "7 : Test getting the user preferences"() {
        given:
        def id = getEditorId()

        when:
        GET("catalogueUsers/$id/userPreferences")

        then: "The response is correct"
        verifyResponse OK, response

    }

    void "8 : Test updating the user preferences"() {
        given:
        String id = adminRegisterNewUser()
        Map update = [
            something   : 'hello',
            anotherThing: 'wibble'
        ]

        when: 'logged in as user whose id it is'
        PUT("catalogueUsers/$id/userPreferences", update)

        then: "The response is correct"
        verifyResponse OK, response

        and:
        !response.body().userPreferences


        when: 'getting the preferences back'
        GET("catalogueUsers/$id/userPreferences", STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse OK, '''{"something":"hello","anotherThing":"wibble"}'''

        cleanup:
        cleanupUser(id)
    }

    void '9 : Test user exists'() {

        when: 'checking non-existent user'
        GET("admin/catalogueUsers/userExists/unknown@test.com", MAP_ARG)

        then:
        verifyResponse OK, response
        response.body().userExists == false

        when: 'checking existent user'
        GET("admin/catalogueUsers/userExists/${userEmailAddresses.editor}", MAP_ARG)

        then:
        verifyResponse OK, response
        response.body().userExists == true
    }

    void '10 : Test change password'() {
        given:
        String id = adminRegisterNewUser()
        def tempPassword = getUser(id).tempPassword
        def validChange = [
            oldPassword: tempPassword,
            newPassword: 'password'
        ]

        when: 'attempt to change using valid info'
        PUT("catalogueUsers/$id/changePassword", validChange)

        then:
        verifyResponse OK, response

        and:
        !response.body().needsToResetPassword

        when: 'using invalid password'
        PUT("catalogueUsers/$id/changePassword", validChange)

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response

        cleanup:
        cleanupUser(id)
    }

    void "11 : Test the approve registration works"() {
        given: 'user self registered'
        String id = selfRegisterNewUser()

        when:
        PUT("admin/catalogueUsers/$id/approveRegistration", [:])

        then:
        verifyResponse OK, response

        and:
        !response.body().pending

        cleanup:
        cleanupUser(id)
    }

    void "12 : Test the reject registration works"() {
        given: 'user self registered'
        String id = selfRegisterNewUser()

        when:
        PUT("admin/catalogueUsers/$id/rejectRegistration", [:])

        then:
        verifyResponse OK, response

        and:
        response.body().pending == true
        response.body().disabled == true

        cleanup:
        cleanupUser(id)
    }

    void '13 : Test getting list of pending users'() {
        given:
        String emailId = UUID.randomUUID().toString()
        String id = selfRegisterNewUser(emailId)
        PUT("admin/catalogueUsers/$id/rejectRegistration", [:])
        verifyResponse OK, response

        when:
        GET('admin/catalogueUsers/pending')

        then:
        verifyResponse OK, response

        and:
        response.body().count >= 2

        when:
        Map pending = response.body().items.find {it.emailAddress == userEmailAddresses.pending}
        Map newPending = response.body().items.find {it.emailAddress == "$emailId@functional-test.com".toString()}

        then:
        pending
        newPending

        and:
        pending.disabled == false
        newPending.disabled == true

        when: 'requesting the count of pending users'
        GET('admin/catalogueUsers/pendingCount')

        then:
        verifyResponse OK, response

        and:
        response.body().count >= 2

        when: 'requesting the disabled pending users'
        GET('admin/catalogueUsers/pending?disabled=true')

        then:
        verifyResponse OK, response
        response.body().count >= 1

        when:
        newPending = response.body().items.find { it.emailAddress == "$emailId@functional-test.com".toString() }

        then:
        newPending

        and:
        newPending.disabled == true

        cleanup:
        cleanupUser(id)

    }

    void '14 : Test searching'() {
        given:
        def expectedJson = '''{
      "count": 1,
      "items": [
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

        when: 'using get with term in query'
        GET('catalogueUsers/search?search=editor', STRING_ARG)

        then:
        verifyJsonResponse OK, expectedJson

        when: 'using post'
        POST('catalogueUsers/search', [search: 'editor'], STRING_ARG)

        then:
        verifyJsonResponse OK, expectedJson
    }

    void '15 : Test admin resetting password'() {
        given:
        String id = selfRegisterNewUser()

        expect:
        !getUser(id).tempPassword

        when: 'user resets'
        PUT("admin/catalogueUsers/$id/adminPasswordReset", [:])

        then:
        verifyResponse OK, response

        and:
        response.body().needsToResetPassword == true

        when: 'getting user info'
        CatalogueUser updated = getUser(id)

        then:
        updated.tempPassword
        updated.password

        cleanup:
        cleanupUser(id)
    }

    void '16 : Test user requesting reset link'() {
        given:
        String id = selfRegisterNewUser()
        def email = getUser(id).emailAddress
        reconfigureDefaultUserPrivileges(false)

        when: 'normal user forget own password'
        GET("catalogueUsers/resetPasswordLink/$email")

        then:
        verifyResponse OK, response

        and:
        !response.body()

        when: 'getting user info'
        CatalogueUser updated = getUser(id)

        then:
        !updated.tempPassword
        updated.password
        updated.resetToken

        cleanup:
        cleanupUser(id)
    }
}