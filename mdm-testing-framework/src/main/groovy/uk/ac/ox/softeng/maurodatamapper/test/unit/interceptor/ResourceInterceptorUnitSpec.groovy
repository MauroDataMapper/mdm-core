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
package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor

import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager

import grails.testing.web.GrailsWebUnitTest
import grails.views.mvc.GenericGroovyTemplateViewResolver
import grails.web.mapping.UrlMappingInfo
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

/**
 * @since 02/12/2019
 */
@Slf4j
abstract class ResourceInterceptorUnitSpec<T> extends BaseUnitSpec implements GrailsWebUnitTest {

    static UUID unknownId = UUID.randomUUID()
    static UUID readAccessId = UUID.randomUUID()
    static UUID noAccessId = UUID.randomUUID()
    static UUID writeAccessId = UUID.randomUUID()
    static IdSecuredUserSecurityPolicyManager idSecuredUserSecurityPolicyManager
    static IdSecuredUserSecurityPolicyManager applicationAdminSecuredUserSecurityPolicyManager

    abstract T getInterceptor()

    abstract UrlMappingInfo withRequest(Map<String, Object> arguments)

    abstract String getControllerName()

    void setResourceIdParameter(String id, String action) {
        if (action != 'index') params.id = id
    }

    void setAnyInitialParams() {
        // Default is no-op
    }

    boolean getNoAccessIndexAllowedState() {
        false
    }

    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.FORBIDDEN
    }

    HttpStatus getSaveAllowedCode() {
        HttpStatus.NOT_FOUND
    }

    HttpStatus getReadSaveAllowedCode() {
        HttpStatus.FORBIDDEN
    }

    def getPublicAccessUserSecurityPolicyManager() {
        PublicAccessSecurityPolicyManager.instance
    }

    def getNoAccessUserSecurityPolicyManager() {
        NoAccessSecurityPolicyManager.instance
    }

    def setupSpec() {
        log.debug('Setting up resource unit spec')
        unknownId = UUID.randomUUID()
        readAccessId = UUID.randomUUID()
        noAccessId = UUID.randomUUID()
        writeAccessId = UUID.randomUUID()

        idSecuredUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(editor, unknownId, noAccessId, readAccessId, writeAccessId)
        applicationAdminSecuredUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(admin, unknownId, noAccessId, readAccessId,
                                                                                                  writeAccessId)

        // The grails unit spec loads th composite view resolver but only with the gsp resolver
        // We need to add the jsonViewResolver
        // Weirdly the base spec does create the smart view resolvers so they are available as referenced beans
        defineBeans {
            jsonViewResolver(GenericGroovyTemplateViewResolver, ref('jsonSmartViewResolver'))
            "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)
        }
    }

    def setup() {
        log.debug('Setting up resource unit')
    }

    void 'R1 : Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: controllerName)

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    @Unroll
    void 'R2 : test public access to #action is allowed'() {
        given:
        setAnyInitialParams()
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager

        when:
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()

        where:
        action << [
            'index',
            'show',
            'save',
            'update',
            'delete'
        ]
    }

    @Unroll
    void 'R3 : test no access to #action is allowed (#allowed)'() {
        given:
        setAnyInitialParams()
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        response.status == responseCode.code

        where:
        action   || allowed                   | responseCode
        'index'  || noAccessIndexAllowedState | noAccessIndexAllowedCode
        'show'   || false                     | HttpStatus.NOT_FOUND
        'save'   || false                     | getSaveAllowedCode()
        'update' || false                     | HttpStatus.NOT_FOUND
        'delete' || false                     | HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'R4 : test read/write access to #action is controlled for #type resource'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        setResourceIdParameter(resourceId.toString(), action)
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == allowed

        and:
        !responseCode || response.status == responseCode.code

        where:
        action   | resourceId    || allowed | responseCode
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
        'update' | readAccessId || false | getReadSaveAllowedCode()
        'update' | writeAccessId || true    | null
        'delete' | unknownId     || false   | HttpStatus.NOT_FOUND
        'delete' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'delete' | readAccessId || false | getReadSaveAllowedCode()
        'delete' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }
}
