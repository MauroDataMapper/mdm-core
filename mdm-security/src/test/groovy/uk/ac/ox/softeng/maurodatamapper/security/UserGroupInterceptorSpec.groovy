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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

class UserGroupInterceptorSpec extends ResourceInterceptorUnitSpec implements InterceptorUnitTest<UserGroupInterceptor> {

    @Override
    String getControllerName() {
        'userGroup'
    }

    @Override
    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.NOT_FOUND
    }

    HttpStatus getSaveAllowedCode() {
        HttpStatus.FORBIDDEN
    }

    @Unroll
    void 'test access to #action using #idStr is only available to writers'() {
        given:
        params.userGroupId = id

        when:
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.NOT_FOUND.code

        when:
        response.reset()
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == expectedStatus.code

        where:
        [action, id] << [[
                             'alterMembers'
                         ], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
        expectedStatus = id == writeAccessId ? HttpStatus.OK : id == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }
}
