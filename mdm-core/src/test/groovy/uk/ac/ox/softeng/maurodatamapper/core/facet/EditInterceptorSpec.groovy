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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import grails.views.mvc.GenericGroovyTemplateViewResolver
import io.micronaut.http.HttpStatus
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes

class EditInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<EditInterceptor> {

    def setupSpec() {
        // The grails unit spec loads th composite view resolver but only with the gsp resolver
        // We need to add the jsonViewResolver
        // Weirdly the base spec does create the smart view resolvers so they are available as referenced beans
        defineBeans {
            jsonViewResolver(GenericGroovyTemplateViewResolver, ref('jsonSmartViewResolver'))
            "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)
        }
    }

    def setup() {
        mockDomains(Folder, Classifier)
    }

    void 'Test interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: 'edit')

        then: 'The interceptor does match'
        interceptor.doesMatch()

    }

    void 'test exception thrown with no resource'() {
        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI01'
    }

    void 'test execption thrown with unrecognised resource'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.resourceDomainType = 'dataModels'
        params.resourceDomainId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI02'
    }

    void 'test public access to index is allowed'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        params.resourceDomainType = 'folders'
        params.resourceDomainId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')

        then:
        interceptor.before()
    }

    void 'test noaccess to index is not allowed'() {
        given:
        params.currentUserSecurityPolicyManager = NoAccessSecurityPolicyManager.instance
        params.resourceDomainType = 'folders'
        params.resourceDomainId = UUID.randomUUID().toString()

        when:
        withRequest([controller: 'edit'])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')

        then:
        !interceptor.before()

        and:
        response.status == HttpStatus.FORBIDDEN.code

    }
}
