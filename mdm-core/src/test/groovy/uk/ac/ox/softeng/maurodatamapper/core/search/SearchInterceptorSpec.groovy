/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.search

import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.IdSecuredUserSecurityPolicyManager

import grails.testing.web.interceptor.InterceptorUnitTest
import grails.views.mvc.GenericGroovyTemplateViewResolver
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes

@Slf4j
class SearchInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<SearchInterceptor> {

    static UUID unknownId = UUID.randomUUID()
    static UUID readAccessId = UUID.randomUUID()
    static UUID noAccessId = UUID.randomUUID()
    static UUID writeAccessId = UUID.randomUUID()
    static IdSecuredUserSecurityPolicyManager idSecuredUserSecurityPolicyManager

    def setupSpec() {
        log.debug('Setting up base unit beans')

        // The grails unit spec loads th composite view resolver but only with the gsp resolver
        // We need to add the jsonViewResolver
        // Weirdly the base spec does create the smart view resolvers so they are available as referenced beans
        defineBeans {
            jsonViewResolver(GenericGroovyTemplateViewResolver, ref('jsonSmartViewResolver'))
            "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)
        }

        unknownId = UUID.randomUUID()
        readAccessId = UUID.randomUUID()
        noAccessId = UUID.randomUUID()
        writeAccessId = UUID.randomUUID()

        idSecuredUserSecurityPolicyManager = new IdSecuredUserSecurityPolicyManager(editor, unknownId, noAccessId, readAccessId, writeAccessId)

    }

    String getControllerName() {
        'search'
    }

    def getPublicAccessUserSecurityPolicyManager() {
        PublicAccessSecurityPolicyManager.instance
    }

    def getNoAccessUserSecurityPolicyManager() {
        NoAccessSecurityPolicyManager.instance
    }

    void 'Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: controllerName)

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    void 'test access to search using #idStr is always available'() {
        given:
        String action = 'search'

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
        interceptor.before()
        response.status == HttpStatus.OK.code

        when:
        response.reset()
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        withRequest(controller: controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()
        response.status == HttpStatus.OK.code

        where:
        id << [unknownId, readAccessId, noAccessId, writeAccessId]
        idStr = id == unknownId ? 'unknownId' :
                id == readAccessId ? 'readAccessId' :
                id == noAccessId ? 'noAccessId' :
                'writeAccessId'
    }
}
