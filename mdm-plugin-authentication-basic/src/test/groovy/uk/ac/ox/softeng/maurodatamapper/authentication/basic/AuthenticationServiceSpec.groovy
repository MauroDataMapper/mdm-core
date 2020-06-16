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
package uk.ac.ox.softeng.maurodatamapper.authentication.basic

import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class AuthenticationServiceSpec extends BaseUnitSpec implements ServiceUnitTest<AuthenticationService>, SecurityDefinition {

    def setup() {
        mockDomain(CatalogueUser)
        mockArtefact(CatalogueUserService)
        createModernSecurityUsers('unitTest')
        checkAndSave(admin)
        checkAndSave(editor)
        checkAndSave(pending)
        checkAndSave(reader)
        checkAndSave(authenticated)
    }

    void 'test authenticating user and password validation'() {
        expect: 'no user fails'
        !service.authenticateAndObtainUser(null, 'password')

        and: 'user doesnt exist fails'
        !service.authenticateAndObtainUser('newtester@email.com', 'wobble')

        and: 'user with temp password but wrong password'
        !service.authenticateAndObtainUser(reader.emailAddress, 'wobble')

        and: 'user with valid temp password'
        service.authenticateAndObtainUser(reader.emailAddress, reader.tempPassword)

        and: 'user with password but wrong password'
        !service.authenticateAndObtainUser(pending.emailAddress, 'wobble')

        and: 'user with password and password'
        service.authenticateAndObtainUser(pending.emailAddress, 'test password')

    }

    void 'test second user authentication'() {
        expect:
        service.authenticateAndObtainUser(admin.emailAddress, "password")
        !service.authenticateAndObtainUser(admin.emailAddress, "Password")
        !service.authenticateAndObtainUser(admin.emailAddress, "password1234")
        service.authenticateAndObtainUser(admin.emailAddress, "    password")
        service.authenticateAndObtainUser(admin.emailAddress, "password     ")
        !service.authenticateAndObtainUser(admin.emailAddress, "pass   word")
    }


}
