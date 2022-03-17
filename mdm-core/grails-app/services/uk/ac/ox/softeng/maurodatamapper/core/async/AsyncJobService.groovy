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

import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.async.Promise
import grails.gorm.transactions.Transactional
import grails.plugin.cache.GrailsCache
import groovy.util.logging.Slf4j
import org.grails.plugin.cache.GrailsCacheManager

import java.time.OffsetDateTime
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

import static grails.async.Promises.onError
import static grails.async.Promises.task

@Slf4j
@Transactional
class AsyncJobService implements MdmDomainService<AsyncJob> {

    public static final String ASYNC_JOB_CACHE_KEY = 'asyncJobCache'

    GrailsCacheManager grailsCacheManager

    @Override
    AsyncJob get(Serializable id) {
        AsyncJob.get(id)
    }

    @Override
    List<AsyncJob> getAll(Collection<UUID> resourceIds) {
        AsyncJob.getAll(resourceIds)
    }

    @Override
    List<AsyncJob> list(Map pagination = [:]) {
        AsyncJob.list(pagination)
    }

    @Override
    Long count() {
        AsyncJob.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    @Override
    void delete(AsyncJob asyncJob) {
        asyncJob.delete(flush: true)
    }

    AsyncJob save(AsyncJob asyncJob) {
        asyncJob.save(flush: true)
    }

    @Override
    AsyncJob findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        return null
    }

    AsyncJob createAndSaveAsyncJob(String jobName, User startedByUser, Closure taskToExecute) {
        createAndSaveAsyncJob(jobName, startedByUser.emailAddress, taskToExecute)
    }

    AsyncJob createAndSaveAsyncJob(String jobName, String startedByUserEmailAddress, Closure taskToExecute) {
        AsyncJob asyncJob = save(new AsyncJob(jobName: jobName,
                                              startedByUser: startedByUserEmailAddress,
                                              dateTimeStarted: OffsetDateTime.now(),
                                              createdBy: startedByUserEmailAddress,
                                              status: 'CREATED'))

        Promise promise = task {
            long st = System.currentTimeMillis()
            log.debug('Starting async task {}:{}', jobName, asyncJob.id)
            taskToExecute.call()
            completedJob(asyncJob)
            log.info('Async task {}:{} completed in {}', jobName, asyncJob.id, Utils.timeTaken(st))
        }
        Promise error = onError([promise]) {Throwable err ->
            if (err !instanceof CancellationException) {
                Throwable reason = err
                if (err instanceof ExecutionException) {
                    reason = err.getCause()
                }
                log.error("Failed to complete async job ${jobName} because ${reason.message}")
                failedJob(asyncJob, reason.message)
            }
        }
        getAsyncJobCache().put(asyncJob.id, [task: promise, error: error])
        save(asyncJob.tap {it.status = 'RUNNING'})
    }

    List<AsyncJob> findAllByStartedByUser(String emailAddress, Map pagination) {
        AsyncJob.findAllByStartedByUser(emailAddress, pagination)
    }

    AsyncJob findByStartedByUserAndId(String emailAddress, UUID id) {
        AsyncJob.findByStartedByUserAndId(emailAddress, id)
    }

    void completedJob(AsyncJob asyncJob) {
        cleanupJob(asyncJob, 'COMPLETED', null)
    }

    void failedJob(AsyncJob asyncJob, String errorMessage) {
        cleanupJob(asyncJob, 'FAILED', errorMessage)
    }

    void cancelledJob(AsyncJob asyncJob) {
        cleanupJob(asyncJob, 'CANCELLED', null)
    }

    void cleanupJob(AsyncJob asyncJob, String cleanupStatus, String cleanupMessage) {
        log.debug('Cleanup job {}:{} as {}', asyncJob.jobName, asyncJob.id, cleanupStatus)
        getAsyncJobCache().evict(asyncJob.id)
        save(asyncJob.merge().tap {
            it.status = cleanupStatus
            it.message = cleanupMessage
        })
    }

    void cancelRunningJob(Serializable id) {
        AsyncJob asyncJob = get(id)
        if (!asyncJob) return
        cancelRunningJob(asyncJob)
    }

    void cancelRunningJob(AsyncJob asyncJob) {
        if (!asyncJob) return
        log.debug('Cancelling running job {}:{}', asyncJob.jobName, asyncJob.id)
        Map<String, Promise> promises = getAsyncJobPromises(asyncJob.id)
        if (promises) {
            if (!promises.task.isDone()) promises.task.cancel(true)
            // Wait for cancellation to go through
            while (!promises.task.isDone() && !promises.error.isDone()) {/* wait*/}
        }
        // Update database
        cancelledJob(asyncJob)
    }

    void cancelAllRunningJobs() {
        GrailsCache jobCache = getAsyncJobCache()
        Collection<String> jobIds = jobCache.getAllKeys() as Collection<String>
        if (jobIds) {
            log.debug('Cancelling {} running jobs', jobIds.size())
            List<AsyncJob> runningJobs = getAll(jobIds.collect {Utils.toUuid(it)}).findAll()
            // Call cancel on each job and wait for them to complete
            List<Promise> cancelPromises = runningJobs.collect {
                Map<String, Promise> promises = getAsyncJobPromises(it.id)
                if (promises && !promises.task.isDone()) promises.task.cancel(true)
                promises.error
            }
            while (cancelPromises.every() {Promise p -> !p.isDone()}) {/* wait*/}
            // Update the database
            runningJobs.each {cancelledJob(it)}
        }
    }

    Map<String, Promise> getAsyncJobPromises(String id) {
        getAsyncJobPromises(Utils.toUuid(id))
    }

    Promise getAsyncJobPromise(String id) {
        getAsyncJobPromise(Utils.toUuid(id))
    }

    Map<String, Promise> getAsyncJobPromises(UUID id) {
        getAsyncJobCache().get(id, Map<String, Promise>)
    }

    Promise getAsyncJobPromise(UUID id) {
        getAsyncJobPromises(id)?.task
    }

    Promise getAsyncJobPromise(AsyncJob asyncJob) {
        getAsyncJobPromise(asyncJob.id)
    }

    private GrailsCache getAsyncJobCache() {
        grailsCacheManager.getCache(ASYNC_JOB_CACHE_KEY) as GrailsCache
    }
}
