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
package uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager

import groovy.util.logging.Slf4j
import org.grails.web.util.GrailsApplicationAttributes

/**
 * @since 20/03/2020
 */
@Slf4j
abstract class VariableContainedResourceInterceptorSpec<T> extends ContainedResourceInterceptorUnitSpec<T> {

    abstract void setUnknownContainingItemDomainType()

    @Override
    String getExpectedExceptionCodeForNoContainingItem() {
        'MCI01'
    }

    void 'VCR1 : test execption thrown with unrecognised containing item'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        setResourceIdParameter(UUID.randomUUID().toString(), '')
        setUnknownContainingItemDomainType()

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == 'MCI02'
    }
}
