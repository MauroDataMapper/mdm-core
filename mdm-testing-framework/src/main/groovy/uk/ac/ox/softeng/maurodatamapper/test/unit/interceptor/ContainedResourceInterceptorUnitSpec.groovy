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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

/**
 * @since 02/12/2019
 */
@Slf4j
abstract class ContainedResourceInterceptorUnitSpec<T> extends ResourceInterceptorUnitSpec<T> {

    abstract String getExpectedExceptionCodeForNoContainingItem()

    @Override
    void setResourceIdParameter(String id, String action) {
        setContainingResourceParameters(id)
        super.setResourceIdParameter(UUID.randomUUID().toString(), action)
    }

    abstract void setContainingResourceParameters(String id)

    HttpStatus getNoAccessIndexAllowedCode() {
        HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'CR1 : test read/write access to index is controlled for #type resource'() {
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
        action  | resourceId    || allowed | responseCode
        'index' | unknownId     || false   | HttpStatus.NOT_FOUND
        'index' | noAccessId    || false   | HttpStatus.NOT_FOUND
        'index' | readAccessId  || true    | null
        'index' | writeAccessId || true    | null

        type = resourceId == unknownId ? 'unknown' :
               resourceId == noAccessId ? 'no access' :
               resourceId == readAccessId ? 'read access' :
               resourceId == writeAccessId ? 'write access' : 'broken'
    }

    void 'CR2 : test exception thrown with no containing item'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager

        when:
        params.id = UUID.randomUUID().toString()
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        e.errorCode == expectedExceptionCodeForNoContainingItem
    }
}
