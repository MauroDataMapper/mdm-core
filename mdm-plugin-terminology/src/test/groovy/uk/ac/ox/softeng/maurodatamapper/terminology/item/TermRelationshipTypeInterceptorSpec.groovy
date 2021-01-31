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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeInterceptor
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ContainedResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class TermRelationshipTypeInterceptorSpec extends ContainedResourceInterceptorUnitSpec
    implements InterceptorUnitTest<TermRelationshipTypeInterceptor> {

    def setup() {
        log.debug('Setting up TermRelationshipTypeInterceptorSpec')
        mockDomains(Folder, Terminology, Term, TermRelationship, TermRelationshipType, CodeSet)
    }

    @Override
    String getControllerName() {
        'termRelationshipType'
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
}
