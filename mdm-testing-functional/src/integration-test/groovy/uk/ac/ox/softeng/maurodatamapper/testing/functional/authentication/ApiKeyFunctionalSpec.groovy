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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.authentication


import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec

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
        "catalogueUsers/${getUserByEmailAddress(userEmailAddresses.editor).id}/apiKeys"
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0, "items": []}'
    }

    @Override
    String getReaderIndexJson() {
        null
    }

    @Override
    String getShowJson() {
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
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    Boolean getReaderCanSeeEditorCreatedItems() {
        false
    }

    @Override
    void verifyR01Response(HttpResponse<Map> response) {
        verifyResponse NOT_FOUND, response
        String id = getUserByEmailAddress(userEmailAddresses.editor).id.toString()
        assert responseBody().path == "/api/catalogueUsers/${id}/apiKeys/"
        assert responseBody().resource == 'CatalogueUser'
        assert responseBody().id == id
    }

    @Override
    void verifyE02Response(HttpResponse<Map> response, String id) {
        // Show is not allowed
        verifyResponse NOT_FOUND, response
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyR03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyR03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyR03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyR04KnownIdResponse(HttpResponse<Map> response, String id) {
        verifyNotFound(response, null)
    }

    @Override
    void verifyNotFound(HttpResponse<Map> response, def id) {
        verifyResponse NOT_FOUND, response
        assert response.body().path
    }

    void 'E05 : Test disabling an ApiKey (as editor)'() {

        given:
        String id = getValidId()

        when:
        loginEditor()
        PUT("${id}/disable", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().disabled

        cleanup:
        removeValidIdObject(id)
    }

    void 'E06 : Test enabling a disabled ApiKey (as editor)'() {

        given:
        String id = getValidId()
        loginEditor()
        PUT("${id}/disable", [:])
        verifyResponse(HttpStatus.OK, response)
        logout()

        when:
        loginEditor()
        PUT("${id}/enable", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        !responseBody().disabled

        cleanup:
        removeValidIdObject(id)
    }

    void 'E07 : Test refreshing an expired refreshable ApiKey (as editor)'() {

        given:
        int refreshDays = 4
        loginEditor()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: true], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginEditor()
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().expiryDate == LocalDate.now().plusDays(refreshDays).format(DateTimeFormatter.ISO_LOCAL_DATE)

        cleanup:
        removeValidIdObject(id)
    }

    void 'E08 : Test refreshing an expired unrefreshable ApiKey (as editor)'() {

        given:
        int refreshDays = 4
        loginEditor()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: false], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginEditor()
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors[0].message == 'Cannot refresh ApiKey as it is not marked refreshable'

        cleanup:
        removeValidIdObject(id)
    }

    void 'L05 : Test disabling an ApiKey (not logged in)'() {

        given:
        String id = getValidId()

        when:
        PUT("${id}/disable", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'L06 : Test enabling a disabled ApiKey (not logged in)'() {

        given:
        String id = getValidId()
        loginEditor()
        PUT("${id}/disable", [:])
        verifyResponse(HttpStatus.OK, response)
        logout()

        when:
        PUT("${id}/enable", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'L07 : Test refreshing an expired refreshable ApiKey (not logged in)'() {

        given:
        int refreshDays = 4
        loginEditor()
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
    }

    void 'L08 : Test refreshing an expired unrefreshable ApiKey (not logged in)'() {

        given:
        int refreshDays = 4
        loginEditor()
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
    }


    void 'N05 : Test disabling an ApiKey (as no access/authenticated)'() {

        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("${id}/disable", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N06 : Test enabling a disabled ApiKey (as no access/authenticated)'() {

        given:
        String id = getValidId()
        loginEditor()
        PUT("${id}/disable", [:])
        verifyResponse(HttpStatus.OK, response)
        logout()

        when:
        loginAuthenticated()
        PUT("${id}/enable", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N07 : Test refreshing an expired refreshable ApiKey (as no access/authenticated)'() {

        given:
        int refreshDays = 4
        loginEditor()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: true], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginAuthenticated()
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }

    void 'N08 : Test refreshing an expired unrefreshable ApiKey (as no access/authenticated)'() {

        given:
        int refreshDays = 4
        loginEditor()
        POST(getSavePath(), [name       : 'functionalTest',
                             expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                             refreshable: true], MAP_ARG, true)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()

        when:
        loginAuthenticated()
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyNotFound(response, id)

        cleanup:
        removeValidIdObject(id)
    }
}
