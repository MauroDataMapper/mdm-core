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
package uk.ac.ox.softeng.maurodatamapper.core.email

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException

import java.time.OffsetDateTime

@Integration
class EmailFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'admin/emails'
    }

    Map getValidJson() {
        [
            sentToEmailAddress: 'anewuser@test.com',
            subject           : 'test email',
            body              : 'this is just a test email',
            emailServiceUsed  : 'BasicEmailPluginService',
            dateTimeSent      : OffsetDateTime.now(),
            successfullySent  : true,
        ]
    }

    void "Test the index action"() {
        when: "The index action is requested"
        HttpResponse<Map> response = client.toBlocking().exchange(HttpRequest.GET(resourcePath), Map)

        then: "The response is correct"
        response.status == HttpStatus.OK
        response.body() == [count: 0, items: []]
    }

    void "Test the save action correctly persists an instance"() {
        when: "The save action is executed with valid data"
        client.toBlocking().exchange(HttpRequest.POST(resourcePath, validJson), Map)

        then: "The response is correct"
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void "Test the update action correctly updates an instance"() {
        when: "The update action is executed with valid data"
        String path = "${resourcePath}/1"
        client.toBlocking().exchange(HttpRequest.PUT(path, validJson), Map)

        then: "The response is correct"
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void "Test the show action correctly renders an instance"() {
        when: "When the show action is called to retrieve a resource"
        def id = '1'
        String path = "${resourcePath}/${id}"
        client.toBlocking().exchange(HttpRequest.GET(path), Map)

        then: "The response is correct"
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void "Test the delete action correctly deletes an instance"() {
        when: "When the delete action is executed on an unknown instance"
        def path = "${resourcePath}/99999"
        client.toBlocking().exchange(HttpRequest.DELETE(path))

        then: "The response is correct"
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }
}
