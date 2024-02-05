/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.security.role


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus

class UserGroupSecurableResourceGroupRoleInterceptorSpec extends ResourceInterceptorUnitSpec<SecurableResourceGroupRoleInterceptor>
    implements InterceptorUnitTest<SecurableResourceGroupRoleInterceptor>, SecurityUsers {

    UserGroup editors

    def setup() {
        mockDomains(CatalogueUser, UserGroup, Folder, BasicModel, BasicModelItem)
        implementSecurityUsers('unitTest')
        editors = new UserGroup(createdBy: userEmailAddresses.integrationTest, name: 'editors').addToGroupMembers(editor)
        checkAndSave(editors)
    }

    @Override
    String getControllerName() {
        'securableResourceGroupRole'
    }

    void setResourceIdParameter(String id, String action) {
        params.userGroupId = id
    }

    @Override
    void setAnyInitialParams() {
        params.userGroupId = UUID.randomUUID()
    }

    @Override
    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.NOT_FOUND
    }
}
