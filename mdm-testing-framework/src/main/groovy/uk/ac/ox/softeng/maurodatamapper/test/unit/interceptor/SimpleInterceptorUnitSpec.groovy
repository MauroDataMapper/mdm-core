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
package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor


import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.artefact.Interceptor
import grails.views.mvc.GenericGroovyTemplateViewResolver
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

/**
 * Any class extending this class needs to add:
 *
 * @Shared
 * List<String> actions = []
 *
 * @since 03/12/2019
 */
@Slf4j
abstract class SimpleInterceptorUnitSpec extends BaseUnitSpec {

    def setupSpec() {
        log.debug('Setting up base unit beans')

        // The grails unit spec loads th composite view resolver but only with the gsp resolver
        // We need to add the jsonViewResolver
        // Weirdly the base spec does create the smart view resolvers so they are available as referenced beans
        defineBeans {
            jsonViewResolver(GenericGroovyTemplateViewResolver, ref('jsonSmartViewResolver'))
            "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)
        }

    }

    abstract Interceptor getInterceptor()

    abstract String getControllerName()

    def getPublicAccessUserSecurityPolicyManager() {
        PublicAccessSecurityPolicyManager.instance
    }

    def getNoAccessUserSecurityPolicyManager() {
        NoAccessSecurityPolicyManager.instance
    }

    void 'S1 : Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: controllerName)

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    @Unroll
    void 'S2 : test public access to #action is allowed'() {
        given:
        assert actions
        params.currentUserSecurityPolicyManager = publicAccessUserSecurityPolicyManager

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        interceptor.before()

        where:
        action << actions
    }

    @Unroll
    void 'S3 : test access to #action is not allowed'() {
        given:
        assert actions
        params.currentUserSecurityPolicyManager = noAccessUserSecurityPolicyManager

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, action)

        then:
        !interceptor.before()

        and:
        response.status == HttpStatus.FORBIDDEN.code
        response.json.additional

        where:
        action << actions
    }
}
