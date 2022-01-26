/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.authority

import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * @since 31/08/2021
 */
@Rollback
@Integration
@Slf4j
class AuthorityFunctionalSpec extends ResourceFunctionalSpec<Authority> {

    @Override
    String getResourcePath() {
        'authorities'
    }

    @Transactional
    Authority getDefaultAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    int getExpectedInitialResourceCount() {
        1
    }

    @Override
    Map getValidJson() {
        [
            url  : 'https://functional-spec-authority.com',
            label: 'Functional Spec Authority'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            url  : getDefaultAuthority().url,
            label: getDefaultAuthority().label
        ]
    }

    void verifyR1EmptyIndexResponse() {
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().count == 1
        Authority authority = getDefaultAuthority()
        assert responseBody().items.first().id == authority.id.toString()
        assert responseBody().items.first().url == authority.url
        assert responseBody().items.first().label == authority.label
    }

    void verifyR3IndexResponse(String expectedId) {
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 2
        assert response.body().items.size() == 2
        assert response.body().items[1].id == expectedId
    }

    @Override
    void cleanUpData() {

    }

    void 'Test the show action correctly renders the default instance'() {
        given:
        Authority authority = getDefaultAuthority()
        String id = authority.id.toString()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then: 'The response is correct'
        verifyResponse(HttpStatus.OK, response)
        responseBody().id == id
        responseBody().url == authority.url
        responseBody().label == authority.label
        responseBody().defaultAuthority
        responseBody().readableByEveryone
        responseBody().readableByAuthenticatedUsers
        responseBody().availableActions.sort() == ["show",
                                                   "update"].sort()
    }

    void 'test trying to delete the default authority'() {
        given:
        Authority authority = getDefaultAuthority()
        String id = authority.id.toString()

        when:
        DELETE(id)

        then:
        verifyResponse(HttpStatus.FORBIDDEN, response)
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "https://functional-spec-authority.com",
  "label": "Functional Spec Authority",
  "defaultAuthority": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": [
    "delete",
    "show",
    "update"
  ]
}'''
    }
}
