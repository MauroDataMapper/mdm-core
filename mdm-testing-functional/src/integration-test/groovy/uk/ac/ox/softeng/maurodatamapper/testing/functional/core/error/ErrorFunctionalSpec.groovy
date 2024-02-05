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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.error

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR

@Integration
@Slf4j
class ErrorFunctionalSpec extends FunctionalSpec {

    @Shared String errorApiKeyId

    String getResourcePath() {
        ''
    }

    void 'AuthError01 : Login with invalid request gives stacktrace'() {
        when:
        POST('authentication/login', ' ', MAP_ARG, true)

        then:
        verifyResponse(INTERNAL_SERVER_ERROR, response)
        responseBody().status == 500
        responseBody().reason == 'Internal Server Error'

        and: 'stacktrace info is shown'
        responseBody().errorCode == 'UEX--'
        responseBody().message
        responseBody().exception
        responseBody().exception.message
        responseBody().exception.stacktrace.size() > 0
    }

    void 'AuthError02 : Login with invalid request and stacktrace disabled'() {
        given:
        loginAdmin()
        POST('admin/properties', [key: 'security.hide.exception', value: 'true', publiclyVisible: false, category: 'security'])
        verifyResponse(CREATED, response)
        errorApiKeyId = responseBody().id
        logout()

        when:
        POST('authentication/login', ' ', MAP_ARG, true)

        then:
        verifyResponse(INTERNAL_SERVER_ERROR, response)
        responseBody().status == 500
        responseBody().reason == 'Internal Server Error'

        and: 'stacktrace info is not shown'
        !responseBody().errorCode
        !responseBody().message
        !responseBody().exception
    }

    void 'AuthError03 : Login with invalid request and stacktrace re-enabled'() {
        given:
        loginAdmin()
        PUT("admin/properties/$errorApiKeyId", [value: 'false'])
        verifyResponse(OK, response)
        logout()

        when:
        POST('authentication/login', ' ', MAP_ARG, true)

        then:
        verifyResponse(INTERNAL_SERVER_ERROR, response)
        responseBody().status == 500
        responseBody().reason == 'Internal Server Error'

        and: 'stacktrace info is shown'
        responseBody().errorCode == 'UEX--'
        responseBody().message
        responseBody().exception
        responseBody().exception.message
        responseBody().exception.stacktrace.size() > 0

        when:
        loginAdmin()
        DELETE("admin/properties/$errorApiKeyId")
        verifyResponse(NO_CONTENT, response)
        logout()
        POST('authentication/login', ' ', MAP_ARG, true)

        then:
        verifyResponse(INTERNAL_SERVER_ERROR, response)
        responseBody().status == 500
        responseBody().reason == 'Internal Server Error'

        and: 'stacktrace info is shown'
        responseBody().errorCode == 'UEX--'
        responseBody().message
        responseBody().exception
        responseBody().exception.message
        responseBody().exception.stacktrace.size() > 0
    }
}
