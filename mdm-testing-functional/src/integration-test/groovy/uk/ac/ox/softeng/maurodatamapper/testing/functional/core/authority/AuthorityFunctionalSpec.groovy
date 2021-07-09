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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class AuthorityFunctionalSpec extends FunctionalSpec {

    /**
     * <pre>
     * Controller: authority
     *  |  GET     | /api/authorities          | Action: index
     *  |  GET     | /api/authorities/${id}    | Action: show
     * </pre>
     * @see uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityController
     */
    @Shared
        label = 'Mauro Data Mapper'

    @Override
    String getResourcePath() {
        "authorities"
    }

    def cleanup() {
    }

    void 'A01 : Test getting an authority while not logged in'() {

        when: 'making a get request'
        GET('/')
        then: 'expect an ok response with the default authority'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any { it.label == label }

        when: 'making a get request with an id, still not logged in'
        GET("/${getAuthorityId()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response

        when: 'making a get request with an id that does not exist'
        GET("/${UUID.randomUUID()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response

    }

    void 'A02 : Test getting an authority while authenticated'() {
        given:
        loginAuthenticated()

        when: 'making a get request'
        GET('/')
        then: 'expect an ok response with the default authority'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any { it.label == label }

        when: 'making a get request with an id while authenticated'
        GET("/${getAuthorityId()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response

        when: 'making a get request with an id that does while authenticated'
        GET("/${UUID.randomUUID()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response
    }

    void 'A03 : Test getting an authority as an Admin '() {
        given:
        loginAdmin()

        when: 'making a get request as an Admin'
        GET('/')
        then: 'expect an ok response with the default authority'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any { it.label == label }

//        when: 'making a get request with an id while Admin'
//        GET("/${getAuthorityId()}")
//        then: 'expect an ok response with the default authority'
//        verifyResponse OK, response
//        responseBody().count == 1
//        responseBody().items.any { it.label == label }

        when: 'making a get request with an id that does not exist as an Admin'
        GET("/${UUID.randomUUID()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response
    }

    void 'A04 : Test getting an authority as an Editor'() {
        given:
        loginEditor()

        when: 'making a get request as an Editor'
        GET('/')
        then: 'expect an ok response with the default authority'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any { it.label == label }

        when: 'making a get request with an id while Editor'
        GET("/${getAuthorityId()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response

        when: 'making a get request with an id that does not exist as an Editor'
        GET("/${UUID.randomUUID()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response
   
    }

    void 'A05 : Test getting an authority as a reader'() {
        given:
        loginReader()

        when: 'making a get request as an reader'
        GET('/')
        then: 'expect an ok response with the default authority'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any { it.label == label }

        when: 'making a get request with an id while reader'
        GET("/${getAuthorityId()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response

        when: 'making a get request with an id that does not exist as a reader'
        GET("/${UUID.randomUUID()}")
        then: 'expect a not found response'
        verifyResponse NOT_FOUND, response
    }

    @Transactional
    String getAuthorityId(String label = this.label) {
        def string = Authority.findByLabel(label).id.toString()
        return string


    }

}

