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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

/**
 * @since 02/12/2019
 */
@Slf4j
class ClassifierInterceptorSpec extends ResourceInterceptorUnitSpec implements InterceptorUnitTest<ClassifierInterceptor> {

    def setup() {
        log.debug('Setting up classifier interceptor unit')
        mockDomain(BasicModel)
        mockDomain(ApiProperty)
        ApiPropertyService apiPropertyService = new ApiPropertyService()
        this.getInterceptor().apiPropertyService = apiPropertyService
    }

    @Override
    String getControllerName() {
        'classifier'
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
    void 'test read/write access to #action is controlled for #type on nested classifier'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        params.classifierId = resourceId.toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action   | resourceId    || allowed | responseCode
        'index'  | unknownId     || false   | HttpStatus.NOT_FOUND
        'index'  | noAccessId    || false   | HttpStatus.NOT_FOUND
        'index'  | readAccessId  || true    | null
        'index'  | writeAccessId || true    | null
        'show'   | unknownId     || false   | HttpStatus.NOT_FOUND
        'show'   | noAccessId    || false   | HttpStatus.NOT_FOUND
        'show'   | readAccessId  || true    | null
        'show'   | writeAccessId || true    | null
        'save'   | unknownId     || false   | getSaveAllowedCode()
        'save'   | noAccessId    || false   | getSaveAllowedCode()
        'save'   | readAccessId  || false   | HttpStatus.FORBIDDEN
        'save'   | writeAccessId || true    | null
        'update' | unknownId     || false   | HttpStatus.NOT_FOUND
        'update' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'update' | readAccessId  || false   | HttpStatus.FORBIDDEN
        'update' | writeAccessId || true    | null
        'delete' | unknownId     || false   | HttpStatus.NOT_FOUND
        'delete' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'delete' | readAccessId  || false   | HttpStatus.FORBIDDEN
        'delete' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }

    @Unroll
    void 'test read/write to #action classifiers is controlled for #type for a catalogue item'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        params.catalogueItemDomainType = 'basicModels'

        when:
        params.catalogueItemId = resourceId.toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action   | resourceId    || allowed | responseCode
        'index'  | unknownId     || false   | HttpStatus.FORBIDDEN
        'index'  | noAccessId    || false   | HttpStatus.FORBIDDEN
        'index'  | readAccessId  || true    | null
        'index'  | writeAccessId || true    | null
        'show'   | unknownId     || false   | HttpStatus.NOT_FOUND
        'show'   | noAccessId    || false   | HttpStatus.NOT_FOUND
        'show'   | readAccessId  || true    | null
        'show'   | writeAccessId || true    | null
        'save'   | unknownId     || false   | HttpStatus.NOT_FOUND
        'save'   | noAccessId    || false   | HttpStatus.NOT_FOUND
        'save'   | readAccessId  || false   | HttpStatus.FORBIDDEN
        'save'   | writeAccessId || true    | null
        'update' | unknownId     || false   | HttpStatus.METHOD_NOT_ALLOWED
        'update' | noAccessId    || false   | HttpStatus.METHOD_NOT_ALLOWED
        'update' | readAccessId  || false   | HttpStatus.METHOD_NOT_ALLOWED
        'update' | writeAccessId || false   | HttpStatus.METHOD_NOT_ALLOWED
        'delete' | unknownId     || false   | HttpStatus.NOT_FOUND
        'delete' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'delete' | readAccessId  || false   | HttpStatus.FORBIDDEN
        'delete' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }
}
