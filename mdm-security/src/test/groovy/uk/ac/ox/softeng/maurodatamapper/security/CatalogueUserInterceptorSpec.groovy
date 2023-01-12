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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager

import grails.testing.web.interceptor.InterceptorUnitTest
import grails.views.mvc.GenericGroovyTemplateViewResolver
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

@Slf4j
class CatalogueUserInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<CatalogueUserInterceptor> {

    String getControllerName() {
        'catalogueUser'
    }

    @Unroll
    void 'test access to group list members using #idStr is only available to readers'() {
        given:
        String action = 'index'
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
        interceptor.before() == (id in [writeAccessId, readAccessId])
        response.status == expectedStatus.code

        where:
        id << [[unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
        expectedStatus = id in [writeAccessId, readAccessId] ? HttpStatus.OK : HttpStatus.NOT_FOUND
    }

    /*
        ResourceInterceptorUnitSpec
    */
    static UUID unknownId = UUID.randomUUID()
    static UUID readAccessId = UUID.randomUUID()
    static UUID noAccessId = UUID.randomUUID()
    static UUID writeAccessId = UUID.randomUUID()
    static IdSecuredUserSecurityPolicyManager idSecuredUserSecurityPolicyManager
    static IdSecuredUserSecurityPolicyManager applicationAdminSecuredUserSecurityPolicyManager

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
        interceptor.before() == allowed


        where:
        action   || allowed
        'index'  || true
        'show'   || true
        'save'   || true
        'update' || true
        'delete' || true
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
        'save'   || true                      | HttpStatus.OK
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
        'save'   | unknownId     || false   | HttpStatus.METHOD_NOT_ALLOWED
        'save'   | noAccessId    || false   | HttpStatus.METHOD_NOT_ALLOWED
        'save'   | readAccessId  || false   | HttpStatus.METHOD_NOT_ALLOWED
        'save'   | writeAccessId || false   | HttpStatus.METHOD_NOT_ALLOWED
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

    /*
    TieredAccessCheckResourceInterceptorUnitSpec
     */

    void setParamsId(UUID id) {
        params.id = id
    }

    @Unroll
    void 'TR01 : test access to #action using #idStr is always available'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        where:
        [action, id] <<
        [interceptor.getPublicAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }

    @Unroll
    void 'TR02 : test access to #action using #userStr is only available to authenticated users'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(adminId)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(adminId)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.FORBIDDEN.code

        when:
        response.reset()
        setParamsId(adminId)
        params.currentUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(user, unknownId, noAccessId, readAccessId, writeAccessId)
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        setParamsId(adminId)
        interceptor.before() == (UnloggedUser.instance.emailAddress != user.emailAddress && user.emailAddress != 'pending@test.com')
        response.status == (UnloggedUser.instance.emailAddress != user.emailAddress
                                && user.emailAddress != 'pending@test.com' ? HttpStatus.OK.code : HttpStatus.FORBIDDEN.code)

        where:
        [action, user] << [interceptor.getAuthenticatedAccessMethods() ?: ['no-endpoints'],
                           [UnloggedUser.instance,
                            admin, editor, pending, reader1, reader2
                           ]].combinations()
        userStr = user.emailAddress
    }

    @Unroll
    void 'TR03 : test access to #action using #idStr is only available to readers'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.NOT_FOUND.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id in [writeAccessId, readAccessId])
        response.status == expectedStatus.code

        where:
        [action, id] << [interceptor.getReadAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
        expectedStatus = id in [writeAccessId, readAccessId] ? HttpStatus.OK : HttpStatus.NOT_FOUND
    }


    @Unroll
    void 'TR04 : test access to #action using #idStr is only available to creators'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.NOT_FOUND.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == expectedStatus.code

        where:
        [action, id] <<
        [interceptor.getCreateAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
        expectedStatus = id == writeAccessId ? HttpStatus.OK : id == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'TR05 : test access to #action using #idStr is only available to editors'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.NOT_FOUND.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == expectedStatus.code

        where:
        [action, id] << [interceptor.getEditAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
        expectedStatus = id == writeAccessId ? HttpStatus.OK : id == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'TR06 : test access to #action using #idStr is only available to deleters'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.NOT_FOUND.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before() == (id == writeAccessId)
        response.status == expectedStatus.code

        where:
        [action, id] <<
        [interceptor.getDeleteAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
        expectedStatus = id == writeAccessId ? HttpStatus.OK : id == readAccessId ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'TR07 : test access to #action using #idStr is only available to admins'() {
        given:
        if (action == 'no-endpoints') return

        when:
        setParamsId(id)
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.FORBIDDEN.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()
        response.status == HttpStatus.FORBIDDEN.code

        when:
        response.reset()
        setParamsId(id)
        params.currentUserSecurityPolicyManager = applicationAdminSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        where:
        [action, id] <<
        [interceptor.getApplicationAdminAccessMethods() ?: ['no-endpoints'], [unknownId, readAccessId, noAccessId, writeAccessId]].combinations()
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }
}
