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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.authentication

import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * <pre>
 * Controller: apiKey
 * |   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/enable                    | Action: enableApiKey
 * |   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/disable                   | Action: disableApiKey
 * |   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/refresh/${expiresInDays}  | Action: refreshApiKey
 * |   POST   | /api/catalogueUsers/${catalogueUserId}/apiKeys                                       | Action: save
 * |   GET    | /api/catalogueUsers/${catalogueUserId}/apiKeys                                       | Action: index
 * |  DELETE  | /api/catalogueUsers/${catalogueUserId}/apiKeys/${id}                                 | Action: delete
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKeyController
 */
@Slf4j
@Integration
class ApiKeyFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    @Override
    String getResourcePath() {
        "catalogueUsers/${getUserByEmailAddress(userEmailAddresses.creator).id}/apiKeys"
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0, "items": []}'
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .whereNooneCanDoAnything()
            .whereTestingUnsecuredResource()
    }

    String getReaderIndexJson() {
        null
    }

    @Override
    Map getValidJson() {
        [name       : 'functionalTest',
         expiryDate : LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
         refreshable: true]
    }

    @Override
    Map getInvalidJson() {
        [name: 'functionalTest',]
    }

    @Override
    void verify01Response(HttpResponse<Map> response) {
        verifyResponse NOT_FOUND, response
        String id = getUserByEmailAddress(userEmailAddresses.creator).id.toString()
        assert responseBody().path == "/api/catalogueUsers/${id}/apiKeys/"
        assert responseBody().resource == 'CatalogueUser'
        assert responseBody().id == id
    }

    @Override
    void verifyNotFound(HttpResponse<Map> response, def id) {
        verifyResponse NOT_FOUND, response
        assert response.body().path
    }

    void verify04UnknownIdResponse(HttpResponse<Map> response, String name, String id) {
        verifyNotFound(response, id)
    }

    @Override
    void verify04NotAllowedToDeleteResponse(HttpResponse<Map> response, String name, String id) {
        verifyNotFound(response, id)
    }

    void 'C01 : Test disabling an ApiKey (as creator)'() {

        given:
        String id = getValidId()

        when:
        loginCreator()
        PUT("${id}/disable", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().disabled

        cleanup:
        removeValidIdObject(id)
    }

    void 'C02 : Test enabling a disabled ApiKey (as creator)'() {

        given:
        String id = getValidId()
        loginCreator()
        PUT("${id}/disable", [:])
        verifyResponse(HttpStatus.OK, response)
        logout()

        when:
        loginCreator()
        PUT("${id}/enable", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        !responseBody().disabled

        cleanup:
        removeValidIdObject(id)
    }

    void 'C03 : Test refreshing an expired refreshable ApiKey (as creator)'() {

        given:
        int refreshDays = 4
        loginCreator()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: true], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginCreator()
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().expiryDate == LocalDate.now().plusDays(refreshDays).format(DateTimeFormatter.ISO_LOCAL_DATE)

        cleanup:
        removeValidIdObject(id)
    }

    void 'C04 : Test refreshing an expired unrefreshable ApiKey (as creator)'() {

        given:
        int refreshDays = 4
        loginCreator()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: false], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginCreator()
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors[0].message == 'Cannot refresh ApiKey as it is not marked refreshable'

        cleanup:
        removeValidIdObject(id)
    }

    void '#prefix-01 : Test disabling an ApiKey (as #name)'() {

        given:
        String id = getValidId()

        when:
        login(name)
        PUT("${id}/disable", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
        'RD'   | 'Reader'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
    }

    void '#prefix-02 : Test enabling a disabled ApiKey (as #name)'() {

        given:
        String id = getValidId()
        loginCreator()
        PUT("${id}/disable", [:])
        verifyResponse(HttpStatus.OK, response)
        logout()

        when:
        PUT("${id}/enable", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
        'RD'   | 'Reader'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
    }

    void '#prefix-03 : Test refreshing an expired refreshable ApiKey (as #name)'() {

        given:
        int refreshDays = 4
        loginCreator()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: true], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
        'RD'   | 'Reader'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
    }

    void '#prefix-04 : Test refreshing an expired unrefreshable ApiKey (as #name)'() {

        given:
        int refreshDays = 4
        loginCreator()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: true], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
        'RD'   | 'Reader'
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
    }
}
