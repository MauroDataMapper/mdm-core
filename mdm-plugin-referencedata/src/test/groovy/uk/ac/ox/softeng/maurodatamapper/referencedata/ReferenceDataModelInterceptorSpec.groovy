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
package uk.ac.ox.softeng.maurodatamapper.referencedata


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelInterceptor
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.TieredAccessCheckResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

@Slf4j
class ReferenceDataModelInterceptorSpec extends TieredAccessCheckResourceInterceptorUnitSpec<ReferenceDataModelInterceptor>
    implements InterceptorUnitTest<ReferenceDataModelInterceptor> {

    def setup() {
        log.debug('Setting up ReferenceDataModelInterceptorSpec')
        mockDomains(Folder, ReferenceDataModel, ReferenceDataElement, ReferenceDataType, ReferencePrimitiveType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataValue)
    }

    @Override
    String getControllerName() {
        'referenceDataModel'
    }

    @Override
    void setParamsId(UUID id) {
        params.referenceDataModelId = id
    }

    HttpStatus getSaveAllowedCode() {
        HttpStatus.FORBIDDEN
    }

    boolean getNoAccessIndexAllowedState() {
        true
    }

    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.OK
    }

    @Unroll
    void 'test access to changeFolder using folder: #folderIdStr datamodel: #referenceDataModelIdStr is #allowedStr'() {

        given:
        params.referenceDataModelId = referenceDataModelId
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
        [folderId, referenceDataModelId] << [[
                                        unknownId, readAccessId, noAccessId, writeAccessId
                                    ], [
                                        unknownId, readAccessId, noAccessId, writeAccessId
                                    ]].combinations()
        folderIdStr = folderId == unknownId ? 'unknownId' :
                      folderId == readAccessId ? 'readAccessId' :
                      folderId == noAccessId ? 'noAccessId' :
                      'writeAccessId'
        referenceDataModelIdStr = referenceDataModelId == unknownId ? 'unknownId' :
                         referenceDataModelId == readAccessId ? 'readAccessId' :
                         referenceDataModelId == noAccessId ? 'noAccessId' :
                         'writeAccessId'
        accepted = folderId == writeAccessId && referenceDataModelId == writeAccessId
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = referenceDataModelId == writeAccessId ?
                         folderId == writeAccessId ? HttpStatus.OK : folderId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                      : referenceDataModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'test access to #action using datamodel: #referenceDataModelIdStr other datamodel: #otherDataModelIdStr is #allowedStr'() {

        given:
        params.referenceDataModelId = referenceDataModelId
        params.otherModelId = otherDataModelId

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
        [action, referenceDataModelId, otherDataModelId] << [['diff', 'suggestLinks'], [
            unknownId, readAccessId, noAccessId, writeAccessId
        ], [
                                                        unknownId, readAccessId, noAccessId, writeAccessId
                                                    ]].combinations()
        otherDataModelIdStr = otherDataModelId == unknownId ? 'unknownId' :
                              otherDataModelId == readAccessId ? 'readAccessId' :
                              otherDataModelId == noAccessId ? 'noAccessId' :
                              'writeAccessId'
        referenceDataModelIdStr = referenceDataModelId == unknownId ? 'unknownId' :
                         referenceDataModelId == readAccessId ? 'readAccessId' :
                         referenceDataModelId == noAccessId ? 'noAccessId' :
                         'writeAccessId'
        accepted = otherDataModelId in [writeAccessId, readAccessId] && referenceDataModelId in [writeAccessId, readAccessId]
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = referenceDataModelId == writeAccessId ?
                         otherDataModelId == writeAccessId ? HttpStatus.OK :
                         otherDataModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                      : referenceDataModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND

    }
}
