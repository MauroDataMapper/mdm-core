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

import uk.ac.ox.softeng.maurodatamapper.core.task.AsyncJobTask
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.plugin.cache.GrailsCache
import groovy.util.logging.Slf4j
import org.grails.plugin.cache.GrailsCacheManager
import org.hibernate.SessionFactory

import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Slf4j
@Transactional
class AsyncJobService implements MdmDomainService<AsyncJob> {

    public static final String ASYNC_JOB_CACHE_KEY = 'asyncJobCache'

    GrailsCacheManager grailsCacheManager
    ScheduledExecutorService executorService

    SessionFactory sessionFactory

    AsyncJobService() {
        executorService = Executors.newScheduledThreadPool(5)
    }

    AsyncJob save(Map args, AsyncJob domain) {
        domain.save(args)
    }

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
                                              status: 'CREATED'), flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('Creating job [{}]:{}', asyncJob.jobName, asyncJob.id)
        // We need to delay the start of the task long enough to ensure the current transaction creating the task has been completed
        // Otherwise the first step in the task will attempt to update a non-existent object
        long delay = 5
        TimeUnit timeUnit = TimeUnit.SECONDS
        log.debug('Submitting task with a {} {} delay', delay, timeUnit)
        Future future = executorService.schedule(new AsyncJobTask(this, asyncJob, taskToExecute), delay, timeUnit)
        getAsyncJobCache().put(asyncJob.id, future)
        asyncJob
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
        log.info('Cleanup job [{}]:{} as {}', asyncJob.jobName, asyncJob.id, cleanupStatus)
        getAsyncJobCache().evict(asyncJob.id)
        save(asyncJob.tap {
            it.status = cleanupStatus
            it.message = cleanupMessage
        }, flush: true, validate: false)
    }

    void cancelRunningJob(Serializable id) {
        AsyncJob asyncJob = get(id)
        if (!asyncJob) return
        cancelRunningJob(asyncJob)
    }

    void cancelRunningJob(AsyncJob asyncJob) {
        if (!asyncJob) return
        log.info('Cancelling running job [{}]:{}', asyncJob.jobName, asyncJob.id)
        Future future = getAsyncJobFuture(asyncJob.id)
        if (future) {
            if (!future.isDone()) future.cancel(true)
            // Wait for cancellation to go through
            while (!future.isDone()) {/* wait*/}
        }
        // Update database
        cancelledJob(asyncJob)
    }

    void cancelAllRunningJobs() {
        GrailsCache jobCache = getAsyncJobCache()
        Collection<String> jobIds = jobCache.getAllKeys() as Collection<String>
        if (jobIds) {
            log.info('Cancelling all {} running jobs {}', jobIds.size(), jobIds)
            List<AsyncJob> runningJobs = getAll(jobIds.collect {Utils.toUuid(it)}).findAll()
            // Call cancel on each job and wait for them to complete
            List<Future> futures = runningJobs.collect {
                Future future = getAsyncJobFuture(it.id)
                if (future && !future.isDone()) future.cancel(true)
                future
            }
            while (futures.every() {Future p -> !p.isDone()}) {/* wait*/}
            // Update the database
            runningJobs.each {cancelledJob(it)}
        }
    }

    Future getAsyncJobFuture(String id) {
        getAsyncJobFuture(Utils.toUuid(id))
    }

    Future getAsyncJobFuture(UUID id) {
        getAsyncJobCache().get(id, Future)
    }

    Future getAsyncJobFuture(AsyncJob asyncJob) {
        getAsyncJobFuture(asyncJob.id)
    }

    void shutdownAndAwaitTermination() {
        executorService.shutdown() // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    log.error("Pool did not terminate")
            }
        } catch (InterruptedException ex) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }

    private GrailsCache getAsyncJobCache() {
        grailsCacheManager.getCache(ASYNC_JOB_CACHE_KEY) as GrailsCache
    }
}
