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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.authentication.apikey

import uk.ac.ox.softeng.maurodatamapper.authentication.apikey.ApiKeyAuthenticationInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKeyService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpRequest
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

@Slf4j
@Integration
class ApiKeyAuthenticationFunctionalSpec extends FunctionalSpec {

    @Autowired
    ApiKeyService apiKeyService

    @Shared
    UUID adminApiKeyId


    @Shared
    UUID authApiKeyId

    String setApiKey = null

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        safeSessionFlush()
        assert CatalogueUser.count() == 10 // All users

        ApiKey apiKey = apiKeyService.createNewApiKeyForCatalogueUser(StandardEmailAddress.FUNCTIONAL_TEST,
                                                                      CatalogueUser.findByEmailAddress(StandardEmailAddress.ADMIN),
                                                                      2, true, 'functionalTest').save(flush: true)
        adminApiKeyId = apiKey.id

        apiKey = apiKeyService.createNewApiKeyForCatalogueUser(StandardEmailAddress.FUNCTIONAL_TEST,
                                                               CatalogueUser.findByEmailAddress(userEmailAddresses.authenticated),
                                                               2, true, 'functionalTest').save(flush: true)
        authApiKeyId = apiKey.id
    }

    @Transactional
    def cleanupSpec() {
        cleanUpResource(ApiKey)
    }

    @Override
    String getResourcePath() {
        ''
    }

    @Override
    MutableHttpRequest augmentRequest(MutableHttpRequest request) {
        switch (setApiKey) {
            case 'admin':
                request.header(ApiKeyAuthenticationInterceptor.API_KEY_HEADER, adminApiKeyId.toString())
                break
            case 'auth':
                request.header(ApiKeyAuthenticationInterceptor.API_KEY_HEADER, authApiKeyId.toString())
                break
        }
        request
    }

    void 'test accessing admin page without apikey is only possible when logged in as admin'() {
        when:
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.FORBIDDEN, response)

        when:
        loginAuthenticated()
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.FORBIDDEN, response)

        when:
        loginAdmin()
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.OK, response)
    }

    void 'test accessing admin page with apikey is possible'() {
        when:
        setApiKey = 'admin'
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.OK, response)
    }

    void 'test accessing admin page with apikey is possible when logged in as auth user'() {
        when:
        loginAuthenticated()
        setApiKey = 'admin'
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.OK, response)
    }

    void 'test accessing admin page with apikey for non-admin user is not possible when logged in as admin user'() {
        when:
        loginAdmin()
        setApiKey = 'auth'
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.FORBIDDEN, response)
    }
}
