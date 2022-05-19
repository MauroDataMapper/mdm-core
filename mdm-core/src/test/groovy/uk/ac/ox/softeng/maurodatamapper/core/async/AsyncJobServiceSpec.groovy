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

import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.plugin.cache.GrailsCache
import grails.plugin.cache.GrailsConcurrentMapCacheManager
import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class AsyncJobServiceSpec extends BaseUnitSpec implements ServiceUnitTest<AsyncJobService> {

    def setup() {
        mockDomain(AsyncJob)
        service.grailsCacheManager = new GrailsConcurrentMapCacheManager()
    }

    def cleanup() {
    }

    void 'test job creation'() {
        when:
        AsyncJob asyncJob = service.createAndSaveAsyncJob('test', AnonymousUser.instance) {
            sleep(1000)
        }

        then:
        log.warn('checking async records while job runs')
        service.count() == 1
        service.get(asyncJob.id)
        !(service.grailsCacheManager.getCache(AsyncJobService.ASYNC_JOB_CACHE_KEY) as GrailsCache).getAllKeys().isEmpty()

        when:
        log.warn('Waiting for job to complete')
        service.getAsyncJobFuture(asyncJob).get()

        then:
        log.warn('Checking clean up has happened to async job record')
        service.count() == 1
        currentSession.flush()
        currentSession.clear()
        service.get(asyncJob.id).status == 'COMPLETED'
    }

    void 'test job cancellation'() {
        when:
        AsyncJob asyncJob = service.createAndSaveAsyncJob('test', AnonymousUser.instance) {
            sleep(30000)
        }

        then:
        log.warn('checking async records while job runs')
        service.count() == 1
        service.get(asyncJob.id)

        when:
        log.warn('cancelling job')
        service.cancelRunningJob(asyncJob)

        then:
        log.warn('Checking clean up has happened to async job record')
        service.count() == 1
        service.get(asyncJob.id).status == 'CANCELLED'
    }

    void 'test multijob cancellation'() {
        when:
        (1..100).each {
            service.createAndSaveAsyncJob('test:' + it, AnonymousUser.instance) {
                sleep(30000)
            }
        }

        then:
        log.warn('checking async records while job runs')
        service.count() == 100
        service.list().size() == 100
        (service.grailsCacheManager.getCache(AsyncJobService.ASYNC_JOB_CACHE_KEY) as GrailsCache).getAllKeys().size() == 100

        when:
        log.warn('cancelling job')
        service.cancelAllRunningJobs()

        then:
        log.warn('Checking clean up has happened to async job record')
        service.count() == 100
        service.list().every {it.status == 'CANCELLED'}
    }
}
