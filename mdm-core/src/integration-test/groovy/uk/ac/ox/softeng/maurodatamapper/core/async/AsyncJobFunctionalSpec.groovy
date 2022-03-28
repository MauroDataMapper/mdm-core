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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.spockframework.util.Assert

/**
 * @since 15/03/2022
 */
@Slf4j
@Integration
class AsyncJobFunctionalSpec extends BaseFunctionalSpec {

    AsyncJobService asyncJobService

    @Override
    String getResourcePath() {
        'asyncJobs'
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check resource count is {}', 0)
        sessionFactory.currentSession.flush()
        if (AsyncJob.count() != 0) {
            log.error('{} {} resources left over from previous test', [AsyncJob.count(), AsyncJob.simpleName].toArray())
            Assert.fail('Resources left over')
        }
    }

    @Transactional
    String createNewItem(long time = 20000) {
        asyncJobService.createAndSaveAsyncJob('Functional Test', StandardEmailAddress.FUNCTIONAL_TEST) {
            sleep(time) {e ->
                assert e in InterruptedException
                log.info('Sleep interrupted')
                true
            }

        }.id.toString()
    }

    void 'R1 : Test the empty index action'() {
        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyResponse(HttpStatus.OK, response)
        assert response.body() == [count: 0, items: []]
    }

    void 'R3 : Test the index action with content'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem()

        when: 'List is called'
        GET('')

        then: 'We now list 1 due to public access'
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 1
        assert response.body().items.size() == 1
        assert response.body().items[0].id == id

        cleanup:
        asyncJobService.cancelRunningJob(id)
        deleteAsyncJob(id)
    }

    void 'R5a : Test the show action correctly renders an instance'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem()

        when: 'When the show action is called to retrieve a resource'
        sleep(1000)
        GET(id, STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "jobName": "Functional Test",
  "startedByUser": "functional-test@test.com",
  "dateTimeStarted": "${json-unit.matches:offsetDateTime}",
  "status": "RUNNING",
  "location": "${json-unit.any-string}"
}''')

        cleanup:
        asyncJobService.cancelRunningJob(id)
        deleteAsyncJob(id)
    }

    void 'R5b : Test the show action correctly renders an instance after completion'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(500)

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then: 'The response is correct'
        response.status == HttpStatus.OK
        responseBody().status == 'RUNNING'

        when: 'When the show action is called to retrieve a resource'
        sleep(1000)
        GET(id)

        then: 'The response is correct'
        response.status == HttpStatus.OK
        responseBody().status == 'COMPLETED'

        cleanup:
        deleteAsyncJob(id)
    }

    void 'R6 : Test the delete action correctly cancels not deletes an instance'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem()

        when: 'When the delete action is executed on an unknown instance'
        DELETE(UUID.randomUUID().toString())

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is correct'
        response.status == HttpStatus.OK
        responseBody().status == 'CANCELLED'

        cleanup:
        deleteAsyncJob(id)
    }

    @Transactional
    void deleteAsyncJob(String id) {
        asyncJobService.delete(id)
        assert asyncJobService.count() == 0
    }
}
