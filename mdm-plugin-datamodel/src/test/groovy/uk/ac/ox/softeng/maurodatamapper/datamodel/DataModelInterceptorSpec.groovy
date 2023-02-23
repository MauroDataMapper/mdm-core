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
package uk.ac.ox.softeng.maurodatamapper.datamodel


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.TieredAccessCheckResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

@Slf4j
class DataModelInterceptorSpec extends TieredAccessCheckResourceInterceptorUnitSpec<DataModelInterceptor>
    implements InterceptorUnitTest<DataModelInterceptor> {

    def setup() {
        log.debug('Setting up DataModelInterceptorSpec')
        mockDomains(Folder, DataModel, DataClass, DataElement, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue)
    }

    @Override
    String getControllerName() {
        'dataModel'
    }

    @Override
    void setParamsId(UUID id) {
        params.dataModelId = id
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
    void 'test access to changeFolder using folder: #folderIdStr datamodel: #dataModelIdStr is #allowedStr'() {

        given:
        params.dataModelId = dataModelId
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
        [folderId, dataModelId] << [[
                                        unknownId, readAccessId, noAccessId, writeAccessId
                                    ], [
                                        unknownId, readAccessId, noAccessId, writeAccessId
                                    ]].combinations()
        folderIdStr = folderId == unknownId ? 'unknownId' :
                      folderId == readAccessId ? 'readAccessId' :
                      folderId == noAccessId ? 'noAccessId' :
                      'writeAccessId'
        dataModelIdStr = dataModelId == unknownId ? 'unknownId' :
                         dataModelId == readAccessId ? 'readAccessId' :
                         dataModelId == noAccessId ? 'noAccessId' :
                         'writeAccessId'
        accepted = folderId == writeAccessId && dataModelId == writeAccessId
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = dataModelId == writeAccessId ?
                         folderId == writeAccessId ? HttpStatus.OK : folderId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                      : dataModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'test access to #action using datamodel: #dataModelIdStr other datamodel: #otherDataModelIdStr is #allowedStr'() {

        given:
        params.dataModelId = dataModelId
        params.otherModelId = otherDataModelId
        params.otherDataModelId = otherDataModelId

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
        [action, dataModelId, otherDataModelId] << [['diff', 'suggestLinks'], [
            unknownId, readAccessId, noAccessId, writeAccessId
        ], [
                                                        unknownId, readAccessId, noAccessId, writeAccessId
                                                    ]].combinations()
        otherDataModelIdStr = otherDataModelId == unknownId ? 'unknownId' :
                              otherDataModelId == readAccessId ? 'readAccessId' :
                              otherDataModelId == noAccessId ? 'noAccessId' :
                              'writeAccessId'
        dataModelIdStr = dataModelId == unknownId ? 'unknownId' :
                         dataModelId == readAccessId ? 'readAccessId' :
                         dataModelId == noAccessId ? 'noAccessId' :
                         'writeAccessId'
        accepted = otherDataModelId in [writeAccessId, readAccessId] && dataModelId in [writeAccessId, readAccessId]
        allowedStr = accepted ? 'allowed' : 'not allowed'
        expectedStatus = dataModelId == writeAccessId ?
                         otherDataModelId == writeAccessId ? HttpStatus.OK :
                         otherDataModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
                                                      : dataModelId == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND

    }
}
