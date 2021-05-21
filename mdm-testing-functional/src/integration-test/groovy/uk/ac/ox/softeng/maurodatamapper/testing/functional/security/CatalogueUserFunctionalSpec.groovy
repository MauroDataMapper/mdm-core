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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.security


import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.METHOD_NOT_ALLOWED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 *  <pre>
 * Controller: catalogueUser
 *  |  POST    | /api/admin/catalogueUsers/adminRegister                           | Action: adminRegister
 *  |  GET     | /api/admin/catalogueUsers/pendingCount                            | Action: pendingCount
 *  |  GET     | /api/admin/catalogueUsers/pending                                 | Action: pending
 *  |  GET     | /api/admin/catalogueUsers/userExists/${emailAddress}              | Action: userExists
 *  |  PUT     | /api/admin/catalogueUsers/${catalogueUserId}/rejectRegistration   | Action: rejectRegistration
 *  |  PUT     | /api/admin/catalogueUsers/${catalogueUserId}/approveRegistration  | Action: approveRegistration
 *  |  PUT     | /api/admin/catalogueUsers/${catalogueUserId}/adminPasswordReset   | Action: adminPasswordReset
 *  |  GET     | /api/catalogueUsers/search                                        | Action: search
 *  |  POST    | /api/catalogueUsers/search                                        | Action: search
 *  |  GET     | /api/catalogueUsers/resetPasswordLink/${emailAddress}             | Action: sendPasswordResetLink
 *  |  PUT     | /api/catalogueUsers/${catalogueUserId}/changePassword             | Action: changePassword
 *  |  PUT     | /api/catalogueUsers/${catalogueUserId}/resetPassword              | Action: resetPassword
 *  |  PUT     | /api/catalogueUsers/${catalogueUserId}/userPreferences            | Action: updateUserPreferences
 *  |  GET     | /api/catalogueUsers/${catalogueUserId}/userPreferences            | Action: userPreferences
 *  |  GET     | /api/userGroups/${userGroupId}/catalogueUsers                     | Action: index
 *  |  POST    | /api/catalogueUsers                                               | Action: save
 *  |  GET     | /api/catalogueUsers                                               | Action: index
 *  |  DELETE  | /api/catalogueUsers/${id}                                         | Action: delete
 *  |  PUT     | /api/catalogueUsers/${id}                                         | Action: update
 *  |  GET     | /api/catalogueUsers/${id}                                         | Action: show
 *
 *  |  GET   | /api/${containerDomainType}/${containerId}/userGroups/${userGroupId}/catalogueUsers  | Action: index
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserController
 */
@Integration
@Slf4j
class CatalogueUserFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        ''
    }

    String getAdminEndpoint() {
        'admin/catalogueUsers'
    }

    String getEndpoint() {
        'catalogueUsers'
    }

    Map getValidJson() {
        [
            emailAddress: 'user@functional-test.com',
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
        loginAdmin()
        POST("$adminEndpoint/adminRegister", getValidJson(), MAP_ARG, true)
        verifyResponse CREATED, response
        assert response.body().id
        String id = response.body().id
        assert id
        logout()
        id
    }

    String selfRegisterNewUser() {
        logout()
        POST("$endpoint", getValidJson())
        verifyResponse CREATED, response
        assert response.body().id
        response.body().id
    }

    @Transactional
    void cleanupFunctionalSpecUser() {
        CatalogueUser user = getUserByEmailAddress('user@functional-test.com')
        user.delete(flush: true)
    }

    String adminRegisteredJson = '''{
  "firstName": "a",
  "lastName": "new user",
  "needsToResetPassword": true,
  "emailAddress": "user@functional-test.com",
  "availableActions": ["disable","show", "update"],
  "disabled": false,
  "id": "${json-unit.matches:id}",
  "pending": false,
   "createdBy": "admin@maurodatamapper.com"
}'''

    String selfRegisteredJson = '''{
  "firstName": "a",
  "lastName": "new user",
  "emailAddress": "user@functional-test.com",
  "availableActions": ["show"],
  "disabled": false,
  "id": "${json-unit.matches:id}",
  "pending": true,
  "createdBy": "user@functional-test.com"
}'''

    void "Test the index action"() {
        given: 'expect users have actually logged in'
        loginEditor()
        loginAuthenticated()
        loginReader()
        loginAdmin()
        logout()

        when: "The index action is requested unlogged in"
        GET("$endpoint")

        then: "The response is correct"
        verifyForbidden response

        when: "The index action is requested as admin"
        loginAdmin()
        GET("$endpoint?sort=emailAddress", STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse OK, '''{
  "count": 9,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "admin@maurodatamapper.com",
      "firstName": "Admin",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "admin@maurodatamapper.com",
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "lastLogin": "${json-unit.matches:offsetDateTime}",
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
      "emailAddress": "authenticated@test.com",
      "firstName": "authenticated",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "needsToResetPassword": true,
      "createdBy": "functional-test@test.com",
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "lastLogin": "${json-unit.matches:offsetDateTime}"
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
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "readers"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "container_admin@test.com",
      "firstName": "containerAdmin",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "editors"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "editor@test.com",
      "firstName": "editor",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "lastLogin": "${json-unit.matches:offsetDateTime}",
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "editors"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "pending@test.com",
      "firstName": "pending",
      "lastName": "User",
      "pending": true,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "organisation": "Oxford",
      "jobTitle": "tester"
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
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "lastLogin": "${json-unit.matches:offsetDateTime}",
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "readers"
        }
      ]
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
      "availableActions": [
        "update",
        "disable",
        "show"
      ],
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "readers"
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
      "availableActions": [
        "update",
        "disable",
        "show"
      ]
    }
  ]
}'''
    }

    void "Test the save/self registration action correctly persists an instance"() {
        when: "The save action is executed with no content"
        POST("$endpoint", [:])

        then: "The response is correct"
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with invalid data"
        POST("$endpoint", invalidJson)
        then: "The response is correct"
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with valid data"
        POST("$endpoint", validJson, STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, selfRegisteredJson

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the save/self registration action cannot be achieved when logged in"() {
        when: 'logged in as editor'
        loginEditor()
        POST("$endpoint", validJson)

        then:
        verifyResponse(METHOD_NOT_ALLOWED, response)

        when: 'logged in as admin'
        loginAdmin()
        POST("$endpoint", validJson)

        then:
        verifyResponse(METHOD_NOT_ALLOWED, response)
    }

    void "Test the admin registration action correctly persists an instance"() {

        when: "The admin registration action is called with valid data but not logged in"
        POST("$adminEndpoint/adminRegister", validJson)

        then:
        verifyForbidden response

        when: 'logged in as normal user'
        loginEditor()
        POST("$adminEndpoint/adminRegister", validJson)

        then:
        verifyForbidden response

        when: "The admin registration  action is executed with no content"
        loginAdmin()
        POST("$adminEndpoint/adminRegister", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "The save action is executed with invalid data"
        POST("$adminEndpoint/adminRegister", invalidJson)
        then:
        verifyResponse UNPROCESSABLE_ENTITY, response


        when: "The save action is executed with valid data"
        POST("$adminEndpoint/adminRegister", validJson, STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, adminRegisteredJson

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the update action correctly updates an instance"() {
        given:
        def id = adminRegisterNewUser()
        def update = [firstName: 'hello']

        when: "The update action is called with valid data but not logged in"
        PUT("$endpoint/$id", update)

        then:
        verifyNotFound response, id

        when: 'logged in as normal user'
        loginAuthenticated()
        PUT("$endpoint/$id", update)

        then:
        verifyNotFound response, id

        when: 'logged in as user whose id it is'
        loginUser(id)
        PUT("$endpoint/$id", update)

        then: "The response is correct"
        verifyResponse OK, response
        response.body().firstName == 'hello'

        when: 'logged in as admin'
        loginAdmin()
        PUT("$endpoint/$id", [firstName: 'byebye'])

        then: "The response is correct"
        verifyResponse OK, response
        response.body().firstName == 'byebye'

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the show action correctly renders an instance"() {
        given:
        String id = getUserByEmailAddress(userEmailAddresses.editor).id

        when: "When the show action is called to retrieve a resource unlogged in"
        GET("$endpoint/$id")

        then:
        verifyNotFound response, id

        when: 'logged in as normal user'
        loginAuthenticated()
        GET("$endpoint/$id")

        then:
        verifyNotFound response, id

        when: 'logged in as user whose id it is'
        loginEditor()
        GET("$endpoint/$id", STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse OK, '''{
      "id": "${json-unit.matches:id}",
      "emailAddress": "editor@test.com",
      "firstName": "editor",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "availableActions": ["disable","show", "update"],
      "lastLogin": "${json-unit.matches:offsetDateTime}",
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "editors"
        }
      ]
    }'''

        when: 'logged in as admin'
        loginAdmin()
        GET("$endpoint/$id", STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse OK, '''{
      "id": "${json-unit.matches:id}",
      "emailAddress": "editor@test.com",
      "firstName": "editor",
      "lastName": "User",
      "pending": false,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "availableActions": ["disable","show", "update"],
      "lastLogin": "${json-unit.matches:offsetDateTime}",
      "groups": [
        {
          "id": "${json-unit.matches:id}",
          "name": "editors"
        }
      ]
    }'''
    }

    void "Test the delete action correctly deletes an instance"() {
        given:
        String id = adminRegisterNewUser()

        when: "When the delete action is executed on an existing instance unlogged in"
        DELETE("$endpoint/$id")

        then: "The response is correct"
        verifyNotFound response, id

        when: "When the delete action is executed on an existing instance as normal user"
        loginEditor()
        DELETE("$endpoint/$id")

        then: "The response is correct"
        verifyForbidden response

        when: 'logged in as user whose id it is'
        loginUser(id)
        DELETE("$endpoint/$id")

        then: "The response is correct"
        verifyForbidden response

        when: "When the delete action is executed on an unknown instance as admin"
        loginAdmin()
        DELETE("${UUID.randomUUID()}")

        then: "The response is correct"
        verifyResponse NOT_FOUND, response

        when: "When the delete action is executed on an existing instance as admin"
        DELETE("$endpoint/$id")

        then: "The response is correct"
        verifyResponse OK, response
        response.body().disabled == true

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test getting the user preferences"() {
        given:
        String id = getUserByEmailAddress(userEmailAddresses.editor).id

        when: "Getting the user preferences but not logged in"
        GET("$endpoint/$id/userPreferences")

        then:
        verifyNotFound response, id

        when: 'logged in as normal user'
        loginAuthenticated()
        GET("$endpoint/$id/userPreferences")

        then:
        verifyNotFound response, id

        when: 'logged in as user whose id it is'
        loginEditor()
        GET("$endpoint/$id/userPreferences")

        then: "The response is correct"
        verifyResponse OK, response

        when: 'logged in as admin'
        loginAdmin()
        GET("$endpoint/$id/userPreferences")

        then: "The response is correct"
        verifyResponse OK, response
    }

    void "Test updating the user preferences"() {
        given:
        String id = adminRegisterNewUser()
        def update = [
            something   : 'hello',
            anotherThing: 'wibble'
        ]

        when: "Getting the user preferences but not logged in"
        PUT("$endpoint/$id/userPreferences", update)

        then:
        verifyNotFound response, id

        when: 'logged in as normal user'
        loginEditor()
        PUT("$endpoint/$id/userPreferences", update)

        then:
        verifyForbidden response

        when: 'logged in as user whose id it is'
        loginUser(id)
        PUT("$endpoint/$id/userPreferences", update)

        then: "The response is correct"
        verifyResponse OK, response

        and:
        !response.body().userPreferences

        when: 'getting the preferences back'
        GET("$endpoint/$id/userPreferences", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
   "something": "hello",
   "anotherThing": "wibble"
}'''

        when: 'logged in as admin'
        loginAdmin()
        PUT("$endpoint/$id/userPreferences", [
            something   : 'byebye',
            anotherThing: 'wibble'
        ])

        then: "The response is correct"
        verifyResponse OK, response

        and:
        response.body().id == id
        !response.body().userPreferences

        when: 'getting the preferences back as admin'
        GET("$endpoint/$id/userPreferences", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
   "something": "byebye",
   "anotherThing": "wibble"
}'''

        when: 'getting the preferences back as actual user'
        loginUser(id)
        GET("$endpoint/$id/userPreferences", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
   "something": "byebye",
   "anotherThing": "wibble"
}'''

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test user exists'() {

        when: 'not logged in checking existent user'
        GET("$adminEndpoint/userExists/${userEmailAddresses.editor}")

        then:
        verifyForbidden(response)

        when: 'logged in checking existent user'
        loginEditor()
        GET("$adminEndpoint/userExists/${userEmailAddresses.editor}")

        then:
        verifyForbidden(response)

        when: 'admin checking non-existent user'
        loginAdmin()
        GET("$adminEndpoint/userExists/unknown@test.com")

        then:
        verifyResponse OK, response
        response.body().userExists == false

        when: 'admin checking existent user'
        GET("$adminEndpoint/userExists/${userEmailAddresses.editor}")

        then:
        verifyResponse OK, response
        response.body().userExists == true
    }

    void 'Test change password'() {
        given:
        String id = adminRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def tempPassword = user.tempPassword
        def validChange = [
            oldPassword: tempPassword,
            newPassword: 'password'
        ]

        when: 'unlogged in attempt to change using valid info'
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyNotFound response, id

        when: 'logged in as other user'
        loginEditor()
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyForbidden response

        when: 'logged in as user'
        loginUser(id)
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyResponse OK, response
        !response.body().needsToResetPassword

        when: 'logged in as user using invalid password'
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'logged in as admin'
        loginAdmin()
        PUT("$endpoint/$id/changePassword",
            [oldPassword: 'password',
             newPassword: tempPassword
            ])

        then:
        verifyResponse OK, response
        !response.body().needsToResetPassword

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the approve registration works"() {
        given: 'user self registered'
        String id = selfRegisterNewUser()

        when: 'not logged in'
        PUT("$adminEndpoint/$id/approveRegistration", [:])

        then:
        verifyForbidden response

        when: 'logged in as normal user'
        loginEditor()
        PUT("$adminEndpoint/$id/approveRegistration", [:])

        then:
        verifyForbidden response

        when: 'logged in as admin user'
        loginAdmin()
        PUT("$adminEndpoint/$id/approveRegistration", [:])

        then:
        verifyResponse OK, response
        response.body().pending == false

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void "Test the reject registration works"() {
        given: 'user self registered'
        String id = selfRegisterNewUser()

        when: 'not logged in'
        PUT("$adminEndpoint/$id/rejectRegistration", [:])

        then:
        verifyForbidden response

        when: 'logged in as normal user'
        loginEditor()
        PUT("$adminEndpoint/$id/rejectRegistration", [:])

        then:
        verifyForbidden response

        when: 'logged in as admin user'
        loginAdmin()
        PUT("$adminEndpoint/$id/rejectRegistration", [:])

        then:
        verifyResponse OK, response
        response.body().pending == true
        response.body().disabled == true

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test getting list of pending users'() {
        given:
        String id = selfRegisterNewUser()
        loginAdmin()
        PUT("$adminEndpoint/$id/rejectRegistration", [:])
        verifyResponse OK, response
        logout()

        when: 'not logged in'
        GET("$adminEndpoint/pending")

        then:
        verifyForbidden response

        when: 'logged in as normal user'
        loginEditor()
        GET("$adminEndpoint/pending")

        then:
        verifyForbidden response

        when: 'logged in as admin user'
        loginAdmin()
        GET("$adminEndpoint/pending", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "pending@test.com",
      "firstName": "pending",
      "lastName": "User",
      "pending": true,
      "disabled": false,
      "createdBy": "functional-test@test.com",
      "availableActions": [ "update","disable","show"],
      "organisation": "Oxford",
      "jobTitle": "tester"
    },
    {
      "id": "${json-unit.matches:id}",
      "emailAddress": "user@functional-test.com",
      "firstName": "a",
      "lastName": "new user",
      "pending": true,
      "disabled": true,
      "createdBy": "user@functional-test.com",
      "availableActions": [ "update","disable","show"]
    }
  ]
}'''

        when: 'requesting the count of pending users'
        GET("$adminEndpoint/pendingCount")

        then:
        verifyResponse OK, response
        response.body().count == 2

        when: 'requesting the disabled pending users'
        GET("$adminEndpoint/pending?disabled=true", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
     {
      "id": "${json-unit.matches:id}",
      "emailAddress": "user@functional-test.com",
      "firstName": "a",
      "lastName": "new user",
      "pending": true,
      "disabled": true,
      "createdBy": "user@functional-test.com",
      "availableActions": [ "update","disable","show"]
    }
  ]
}'''
        cleanup:
        cleanupFunctionalSpecUser()

    }

    void 'Test searching'() {
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

        when: 'searching unlogged in'
        GET("$endpoint/search?searchTerm=editor")

        then:
        verifyForbidden response

        when: 'using get with term in query'
        loginAdmin()
        GET("$endpoint/search?searchTerm=editor", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedJson

        when: 'using post'
        POST("$endpoint/search", [searchTerm: 'editor'], STRING_ARG)

        then:
        verifyJsonResponse OK, expectedJson
    }

    void 'Test admin resetting password and user logs in using original'() {
        given:
        String id = selfRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def email = user.emailAddress

        expect:
        !user.tempPassword

        when: 'not logged in'
        PUT("$adminEndpoint/$id/adminPasswordReset", [:])

        then:
        verifyForbidden response

        when: 'normal user'
        loginEditor()
        PUT("$adminEndpoint/$id/adminPasswordReset", [:])

        then:
        verifyForbidden response

        when: 'actual user'
        loginUser(id)
        PUT("$adminEndpoint/$id/adminPasswordReset", [:])

        then:
        verifyForbidden response

        when: 'admin user resets'
        loginAdmin()
        PUT("$adminEndpoint/$id/adminPasswordReset", [:])

        then:
        verifyResponse OK, response

        and:
        response.body().needsToResetPassword == true

        when: 'getting user info'
        CatalogueUser updated = getUserById(id)

        then:
        updated.tempPassword
        updated.password

        when: 'user logs in using original password'
        def tempPassword = updated.tempPassword
        loginUser(email, 'password')

        then:
        !response.body().needsToResetPassword

        when:
        updated = getUserById(id)

        then:
        !updated.tempPassword
        updated.password

        when: 'logging in with the tempPassword'
        logout()
        POST('authentication/login', [
            username: email,
            password: tempPassword
        ])

        then:
        verifyUnauthorised response

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test admin resetting password and user logs in using reset password'() {
        given:
        String id = selfRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def email = user.emailAddress

        expect:
        !user.tempPassword

        when: 'admin user resets'
        loginAdmin()
        PUT("$adminEndpoint/$id/adminPasswordReset", [:])

        then:
        verifyResponse OK, response

        and:
        response.body().needsToResetPassword == true

        when: 'getting user info'
        CatalogueUser updated = getUserById(id)

        then:
        updated.tempPassword
        updated.password

        when: 'user logs in using temp password'
        loginUser(email, updated.tempPassword)

        then: 'temp password exists and no full password'
        response.body().needsToResetPassword == true

        when: 'trying to login using original password'
        logout()
        POST('authentication/login', [
            username: email,
            password: 'password'
        ])

        then:
        verifyUnauthorised response

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test user forgetting password and user logs in using original'() {
        given:
        String id = selfRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def email = user.emailAddress

        when: 'normal user logged in tries to forget own password'
        loginEditor()
        GET("$endpoint/resetPasswordLink/$email")

        then:
        verifyResponse METHOD_NOT_ALLOWED, response

        when: 'admin user logged in tries to forget user password'
        loginAdmin()
        GET("$endpoint/resetPasswordLink/$email")

        then:
        verifyResponse METHOD_NOT_ALLOWED, response

        when: 'not logged in tries to forget user password'
        logout()
        GET("$endpoint/resetPasswordLink/$email")

        then:
        verifyResponse OK, response

        and:
        !response.body()

        when: 'getting user info'
        CatalogueUser updated = getUserById(id)

        then:
        !updated.tempPassword
        updated.password
        updated.resetToken

        when: 'user logs in using original password'
        loginUser(email, 'password')
        updated = getUserById(id)

        then:
        !updated.tempPassword
        updated.password
        !updated.resetToken

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test user forgetting password and user logs in using reset token'() {
        given:
        String id = selfRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def email = user.emailAddress

        when: 'not logged in tries to forget user password'
        logout()
        GET("$endpoint/resetPasswordLink/$email")

        then:
        verifyResponse OK, response

        and:
        !response.body()

        when: 'getting user info'
        CatalogueUser updated = getUserById(id)

        then:
        !updated.tempPassword
        updated.password
        updated.resetToken

        when: 'user logs in using reset token'
        def token = updated.resetToken.toString()
        logout()
        POST('authentication/login', [
            username: email,
            password: token
        ])

        then: 'fails and nothing changes'
        verifyUnauthorised response

        when: 'getting user info'
        updated = getUserById(id)

        then:
        !updated.tempPassword
        updated.password
        updated.resetToken

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test change/reset password using reset token after logging in'() {
        given:
        String id = selfRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def email = user.emailAddress
        logout()
        GET("$endpoint/resetPasswordLink/$email")
        verifyResponse OK, response
        user = getUserById(id)
        def validChange = [
            resetToken : user.resetToken.toString(),
            newPassword: 'newpassword'
        ]

        when: 'not logged in using changePassword'
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyNotFound response, id

        when: 'logged in as other user using resetPassword'
        loginEditor()
        PUT("$endpoint/$id/resetPassword", validChange)

        then:
        verifyUnauthorised response

        when: 'logged in as other user using changePassword'
        loginEditor()
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyForbidden response

        when: 'logged in as user using resetPassword'
        loginUser(id)
        PUT("$endpoint/$id/resetPassword", validChange)

        then:
        verifyUnauthorised response

        when: 'logged in as user using changePassword'
        loginUser(id)
        PUT("$endpoint/$id/changePassword", validChange)

        then:
        verifyUnauthorised response

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test reset password using reset token without logging in'() {
        given: 'forgotten password'
        String id = selfRegisterNewUser()
        CatalogueUser user = getUserById(id)
        def email = user.emailAddress
        logout()
        GET("$endpoint/resetPasswordLink/$email")
        verifyResponse OK, response
        user = getUserById(id)
        def validChange = [
            resetToken : user.resetToken.toString(),
            newPassword: 'newpassword'
        ]

        when: 'unlogged in attempt to change using valid info after reset'
        PUT("$endpoint/$id/resetPassword", validChange)

        then:
        verifyResponse OK, response

        when: 'log in using old password'
        logout()
        POST('authentication/login', [
            username: email,
            password: 'password'
        ])

        then:
        verifyUnauthorised response

        when: 'log in using new password'
        logout()
        POST('authentication/login', [
            username: email,
            password: 'newpassword'
        ])

        then:
        verifyResponse(OK, response)

        cleanup:
        cleanupFunctionalSpecUser()
    }

    void 'Test exporting all Users to csv file (as admin)'() {
        given:
        loginAdmin()
        when:
        GET("$adminEndpoint/exportUsers")
        then:
        verifyResponse OK, response
    }

    void 'Test exporting all Users to csv file (as editor)'() {
        given:
        loginEditor()
        when:
        GET("$adminEndpoint/exportUsers")
        then:
        verifyForbidden response
    }
}