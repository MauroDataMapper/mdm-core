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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.async

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessWithoutUpdatingFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import static io.micronaut.http.HttpStatus.FORBIDDEN

/**
 * @since 17/03/2022
 */
@Slf4j
@Integration
class AsyncJobFunctionalSpec extends UserAccessWithoutUpdatingFunctionalSpec {

    AsyncJobService asyncJobService

    @Override
    String getResourcePath() {
        'asyncJobs'
    }

    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withoutAvailableActions()
            .whereEditors {
                canIndex()
                cannotSee()
                cannotCreate()
                cannotDelete()
                cannotUpdate()
            }
            .whereAuthenticatedUsers {
                canIndex()
                cannotSee()
                cannotCreate()
                cannotDelete()
                cannotUpdate()
            }
            .whereReaders {
                canIndex()
                cannotSee()
                cannotCreate()
                cannotDelete()
                cannotUpdate()
            }
            .whereAuthors {
                canIndex()
                cannotSee()
                cannotCreate()
                cannotDelete()
                cannotUpdate()
            }
            .whereReviewers {
                canIndex()
                cannotSee()
                cannotCreate()
                cannotDelete()
                cannotUpdate()
            }
            .whereContainerAdmins {
                canIndex()
                cannotSee()
                cannotCreate()
                cannotDelete()
                cannotUpdate()
            }
            .whereAdmins {
                cannotUpdate()
                cannotCreate()
                canIndex()
                canSee()
                canDelete()
            }
    }

    @Override
    String getEditorIndexJson() {
        '{"count":0,"items":[]}'
    }

    @Override
    Map getValidJson() {
        return null
    }

    @Override
    Map getInvalidJson() {
        return null
    }


    @Override
    String getValidId() {
        getValidId(20000)
    }

    @Transactional
    String getValidId(long time) {
        asyncJobService.createAndSaveAsyncJob('Functional Test', userEmailAddresses.creator) {
            sleep(time) {e ->
                assert e in InterruptedException
                log.info('Sleep interrupted')
                true
            }
        }.id.toString()
    }

    @Transactional
    @Override
    void removeValidIdObject(String id) {
        asyncJobService.cancelRunningJob(id)
        asyncJobService.delete(id)
    }

    @Override
    void verifyNotAllowedResponse(HttpResponse response, String id) {
        verifyResponse FORBIDDEN, response
    }

    void verify03CannotCreateResponse(HttpResponse<Map> response, String name) {
        verifyResponse HttpStatus.NOT_FOUND, response
        assert response.body().path
    }

    @Override
    void verifyDeleteResponse(HttpResponse<Map> response) {
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().status == 'CANCELLED'
    }
}
