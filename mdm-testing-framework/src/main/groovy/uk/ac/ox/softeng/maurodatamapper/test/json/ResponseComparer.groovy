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
package uk.ac.ox.softeng.maurodatamapper.test.json

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import net.javacrumbs.jsonunit.core.Option
import org.junit.Assert


/**
 * @since 28/11/2017
 */
@Slf4j
trait ResponseComparer extends JsonComparer {

    HttpResponse<String> jsonCapableResponse

    void verifyJsonErrorResponse(HttpStatus expectedStatus, HttpClientResponseException thrown, expectedJson, Option... addtlOptions) {
        jsonCapableResponse = thrown.response as HttpResponse<String>
        verifyJsonResponse expectedStatus, expectedJson, addtlOptions
    }

    void verifyUnauthorised(HttpResponse response) {
        verifyResponse HttpStatus.UNAUTHORIZED, response
    }

    void verifyResponse(HttpStatus expected, HttpResponse response) {
        try {
            Assert.assertEquals('Failed Response code', expected, response.status())
        } catch (AssertionError error) {
            log.warn('', response.status().code, response.status().reason)
            log.error('Failed Response :: {}[{}]\nResponse Body\n{}\n', response.status().code, response.status().reason, response.body())
            throw error
        }
    }

    void verifyJsonResponse(HttpStatus expectedStatus, expectedJson, Option... addtlOptions) {

        verifyResponse expectedStatus, jsonCapableResponse
        if (expectedJson) internalCompareResponseBody(expectedJson, jsonCapableResponse.body() as String, addtlOptions)
        else {
            if (jsonCapableResponse.body()) log.error('Actual JSON\n{}\n', jsonCapableResponse.body())
            assert !jsonCapableResponse.body(), 'Should be no content in the response'
        }
    }

    private void internalCompareResponseBody(expectedJson, String actualJson, Option... addtlOptions) {
        assert actualJson != null
        if (expectedJson instanceof List) {
            verifyJson(expectedJson[0].toString(), actualJson, (expectedJson.findAll {it instanceof Option}.toArray() as Option[]))
        } else verifyJson(expectedJson.toString(), actualJson, addtlOptions)
    }
}
