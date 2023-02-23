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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderInterceptor
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

class FolderInterceptorSpec extends ResourceInterceptorUnitSpec implements InterceptorUnitTest<FolderInterceptor> {

    def setup() {
        mockDomain(ApiProperty)
        ApiPropertyService apiPropertyService = new ApiPropertyService()
        this.getInterceptor().apiPropertyService = apiPropertyService
    }
    @Override
    String getControllerName() {
        'folder'
    }

    @Override
    boolean getNoAccessIndexAllowedState() {
        true
    }

    @Override
    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.OK
    }

    @Override
    HttpStatus getSaveAllowedCode() {
        HttpStatus.FORBIDDEN
    }

    @Unroll
    void 'test read/write access to #action is controlled for #type on nested folder'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        params.folderId = resourceId.toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action   | resourceId    || allowed | responseCode
        'index'  | unknownId     || false   | HttpStatus.NOT_FOUND
        'index'  | noAccessId    || false | HttpStatus.NOT_FOUND
        'index'  | readAccessId  || true  | null
        'index'  | writeAccessId || true  | null
        'show'   | unknownId     || false | HttpStatus.NOT_FOUND
        'show'   | noAccessId    || false | HttpStatus.NOT_FOUND
        'show'   | readAccessId  || true  | null
        'show'   | writeAccessId || true  | null
        'save'   | unknownId     || false | getSaveAllowedCode()
        'save'   | noAccessId    || false | getSaveAllowedCode()
        'save'   | readAccessId  || false | HttpStatus.FORBIDDEN
        'save'   | writeAccessId || true  | null
        'update' | unknownId     || false | HttpStatus.NOT_FOUND
        'update' | noAccessId    || false | HttpStatus.NOT_FOUND
        'update' | readAccessId  || false | HttpStatus.FORBIDDEN
        'update' | writeAccessId || true  | null
        'delete' | unknownId     || false | HttpStatus.NOT_FOUND
        'delete' | noAccessId    || false | HttpStatus.NOT_FOUND
        'delete' | readAccessId  || false | HttpStatus.FORBIDDEN
        'delete' | writeAccessId || true  | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }

    @Unroll
    void 'test access to search is controlled for #type on folder'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        params.folderId = resourceId.toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action   | resourceId    || allowed | responseCode
        'search' | unknownId     || false   | HttpStatus.NOT_FOUND
        'search' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'search' | readAccessId  || true    | null
        'search' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }

    @Unroll
    void 'test access to changeFolder using destination folder: #destinationFolderIdStr folder: #folderIdStr is #allowedStr'() {

        given:
        params.destinationFolderId = destinationFolderId
        params.folderId = folderId
        String action = 'changeFolder'

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
        interceptor.before() == accepted
        response.status == expectedStatus.code

        where:
        [destinationFolderId, folderId] << [[
                                        unknownId, readAccessId, noAccessId, writeAccessId
                                    ], [
                                        unknownId, readAccessId, noAccessId, writeAccessId
                                    ]].combinations()
        destinationFolderIdStr = destinationFolderId == unknownId ? 'unknownId' :
                      destinationFolderId == readAccessId ? 'readAccessId' :
                      destinationFolderId == noAccessId ? 'noAccessId' :
                      'writeAccessId'
        folderIdStr = folderId == unknownId ? 'unknownId' :
                         folderId == readAccessId ? 'readAccessId' :
                         folderId == noAccessId ? 'noAccessId' :
                         'writeAccessId'
        accepted = destinationFolderId == writeAccessId && folderId == writeAccessId
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = folderId == writeAccessId ?
                         destinationFolderId == writeAccessId ? HttpStatus.OK : destinationFolderId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                      : folderId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }
}
