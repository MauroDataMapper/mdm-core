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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.TieredAccessCheckResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

@Slf4j
class TerminologyInterceptorSpec extends TieredAccessCheckResourceInterceptorUnitSpec<TerminologyInterceptor>
    implements InterceptorUnitTest<TerminologyInterceptor> {

    def setup() {
        log.debug('Setting up TerminologyInterceptorSpec')
        mockDomains(Folder, Terminology, Term, TermRelationship, TermRelationshipType, CodeSet)
    }

    @Override
    String getControllerName() {
        'terminology'
    }

    @Override
    void setParamsId(UUID id) {
        params.terminologyId = id
    }

    HttpStatus getSaveAllowedCode() {
        HttpStatus.FORBIDDEN
    }

    @Unroll
    void 'test access to changeFolder using folder: #folderIdStr terminology: #terminologyIdStr is #allowedStr'() {

        given:
        params.terminologyId = terminologyId
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
        [folderId, terminologyId] << [[
                                          unknownId, readAccessId, noAccessId, writeAccessId
                                      ], [
                                          unknownId, readAccessId, noAccessId, writeAccessId
                                      ]].combinations()
        folderIdStr = folderId == unknownId ? 'unknownId' :
                      folderId == readAccessId ? 'readAccessId' :
                      folderId == noAccessId ? 'noAccessId' :
                      'writeAccessId'
        terminologyIdStr = terminologyId == unknownId ? 'unknownId' :
                           terminologyId == readAccessId ? 'readAccessId' :
                           terminologyId == noAccessId ? 'noAccessId' :
                           'writeAccessId'
        accepted = folderId == writeAccessId && terminologyId == writeAccessId
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = terminologyId == writeAccessId ?
                         folderId == writeAccessId ? HttpStatus.OK : folderId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                        : terminologyId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'test access to #action using terminology: #terminologyIdStr other terminology: #otherModelIdStr is #allowedStr'() {

        given:
        params.terminologyId = terminologyId
        params.otherModelId = otherModelId

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
        response.status == (accepted ? HttpStatus.OK.code : HttpStatus.NOT_FOUND.code)

        where:
        [action, terminologyId, otherModelId] << [['diff'], [
            unknownId, readAccessId, noAccessId, writeAccessId
        ], [
                                                      unknownId, readAccessId, noAccessId, writeAccessId
                                                  ]].combinations()
        otherModelIdStr = otherModelId == unknownId ? 'unknownId' :
                          otherModelId == readAccessId ? 'readAccessId' :
                          otherModelId == noAccessId ? 'noAccessId' :
                          'writeAccessId'
        terminologyIdStr = terminologyId == unknownId ? 'unknownId' :
                           terminologyId == readAccessId ? 'readAccessId' :
                           terminologyId == noAccessId ? 'noAccessId' :
                           'writeAccessId'
        accepted = otherModelId in [writeAccessId, readAccessId] && terminologyId in [writeAccessId, readAccessId]
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = terminologyId == writeAccessId ?
                         otherModelId == writeAccessId ? HttpStatus.OK :
                         otherModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                        : terminologyId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND

    }
}
