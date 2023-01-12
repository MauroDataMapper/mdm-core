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
        !service.authenticateAndObtainUser(username: null, password: 'password')

        and: 'user doesnt exist fails'
        !service.authenticateAndObtainUser(username: 'newtester@email.com', password: 'wobble')

        and: 'user with temp password but wrong password'
        !service.authenticateAndObtainUser(username: reader.emailAddress, password: 'wobble')

        and: 'user with valid temp password'
        service.authenticateAndObtainUser(username: reader.emailAddress, password: reader.tempPassword)

        and: 'user with password but wrong password'
        !service.authenticateAndObtainUser(username: pending.emailAddress, password: 'wobble')

        and: 'user with password and password'
        service.authenticateAndObtainUser(username: pending.emailAddress, password: 'test password')

    }

    void 'test second user authentication'() {
        expect:
        service.authenticateAndObtainUser(username: admin.emailAddress, password: 'password')
        !service.authenticateAndObtainUser(username: admin.emailAddress, password: 'Password')
        !service.authenticateAndObtainUser(username: admin.emailAddress, password: 'password1234')
        service.authenticateAndObtainUser(username: admin.emailAddress, password: '    password')
        service.authenticateAndObtainUser(username: admin.emailAddress, password: 'password     ')
        !service.authenticateAndObtainUser(username: admin.emailAddress, password: 'pass   word')
    }


}
