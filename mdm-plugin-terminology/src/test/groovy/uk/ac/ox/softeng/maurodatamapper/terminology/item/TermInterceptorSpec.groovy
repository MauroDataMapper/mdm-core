/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ContainedResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Unroll

@Slf4j
class TermInterceptorSpec extends ContainedResourceInterceptorUnitSpec implements InterceptorUnitTest<TermInterceptor> {

    def setup() {
        log.debug('Setting up TermInterceptorSpec')
        mockDomains(Folder, Terminology, Term, TermRelationship, TermRelationshipType, CodeSet)
    }

    @Override
    String getControllerName() {
        'term'
    }

    @Override
    void setAnyInitialParams() {
        params.terminologyId = UUID.randomUUID().toString()
    }

    @Override
    String getExpectedExceptionCodeForNoContainingItem() {
        'TSI01'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.terminologyId = id
    }

    @Unroll
    void 'T1 : test read/write access to index is controlled for #type codeSet'() {
        given:
        params.currentUserSecurityPolicyManager = idSecuredUserSecurityPolicyManager
        params.codeSetId = resourceId
        params.id = UUID.randomUUID().toString()

        when:
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
}