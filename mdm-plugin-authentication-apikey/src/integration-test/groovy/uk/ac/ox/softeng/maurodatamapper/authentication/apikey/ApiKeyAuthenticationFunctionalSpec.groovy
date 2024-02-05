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
package uk.ac.ox.softeng.maurodatamapper.authentication.apikey

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKeyService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

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
class ApiKeyAuthenticationFunctionalSpec extends BaseFunctionalSpec implements SecurityDefinition {

    @Autowired
    ApiKeyService apiKeyService

    @Shared
    UUID apiKeyId

    Boolean setApiKey = false

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        assert CatalogueUser.count() == 2 // Unlogged user & admin user

        ApiKey apiKey = apiKeyService.createNewApiKeyForCatalogueUser(StandardEmailAddress.FUNCTIONAL_TEST,
                                                                      CatalogueUser.findByEmailAddress(StandardEmailAddress.ADMIN),
                                                                      2, true, 'functionalTest').save(flush: true)
        apiKeyId = apiKey.id
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
        if (setApiKey) request.header(ApiKeyAuthenticationInterceptor.API_KEY_HEADER, apiKeyId.toString())
        request
    }

    void 'test accessing admin page without apikey is not possible'() {
        when:
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.FORBIDDEN, response)
    }

    void 'test accessing admin page with apikey is possible'() {
        when:
        setApiKey = true
        GET('admin/status')

        then:
        verifyResponse(HttpStatus.OK, response)
    }
}
