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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.core.task.AsyncJobFuture
import uk.ac.ox.softeng.maurodatamapper.core.task.AsyncJobTask
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.gorm.transactions.Transactional
import grails.plugin.cache.GrailsCache
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
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
@SuppressFBWarnings('LI_LAZY_INIT_STATIC')
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

    List<AsyncJob> listWithFilter(Map filter, Map pagination) {
        AsyncJob.withFilter(AsyncJob.by(), filter).list(pagination)
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
    AsyncJob findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        return null
    }

    AsyncJob createAndSaveAsyncJob(String jobName, User startedByUser, @ClosureParams(value = SimpleType, options = 'java.util.UUID') Closure taskToExecute) {
        createAndSaveAsyncJob(jobName, startedByUser.emailAddress, taskToExecute)
    }

    AsyncJob createAndSaveAsyncJob(String jobName, String startedByUserEmailAddress, @ClosureParams(value = SimpleType, options = 'java.util.UUID') Closure taskToExecute) {
        AsyncJob asyncJob = save(new AsyncJob(jobName: jobName,
                                              startedByUser: startedByUserEmailAddress,
                                              dateTimeStarted: OffsetDateTime.now(),
                                              createdBy: startedByUserEmailAddress,
                                              status: 'CREATED'), flush: true)
        sessionFactory?.currentSession?.flush()
        sessionFactory?.currentSession?.clear()
        log.info('Creating job [{}]:{}', asyncJob.jobName, asyncJob.id)
        // We need to delay the start of the task long enough to ensure the current transaction creating the task has been completed
        // Otherwise the first step in the task will attempt to update a non-existent object
        long delay = 5
        TimeUnit timeUnit = TimeUnit.SECONDS
        log.debug('Submitting task with a {} {} delay', delay, timeUnit)
        AsyncJobTask task = new AsyncJobTask(this, asyncJob, taskToExecute)
        Future<Boolean> future = executorService.schedule(task, delay, timeUnit)
        getAsyncJobCache().put(asyncJob.id, new AsyncJobFuture(task, future))
        asyncJob
    }

    List<AsyncJob> findAllByStartedByUser(String emailAddress, Map filter, Map pagination) {
        AsyncJob.withFilter(AsyncJob.byStartedByUser(emailAddress), filter).list(pagination)
    }

    AsyncJob findByStartedByUserAndId(String emailAddress, UUID id) {
        AsyncJob.findByStartedByUserAndId(emailAddress, id)
    }

    void runningJob(UUID id) {
        setJobStatus(id, 'RUNNING', null)
    }

    void completedJob(UUID id) {
        setJobStatus(id, 'COMPLETED', null)
    }

    void failedJob(UUID id, String errorMessage) {
        setJobStatus(id, 'FAILED', errorMessage)
    }

    void cancelledJob(UUID id) {
        setJobStatus(id, 'CANCELLED', null)
    }

    void cancellingJob(UUID id) {
        setJobStatus(id, 'CANCELLING', null)
    }

    void setJobStatus(UUID id, String status, String statusMessage) {
        AsyncJob job = get(id)
        if (!job) {
            log.warn('Attempting to set status {} on non-existent job {} with message [{}]', status, id, statusMessage)
            return
        }
        log.info('Set job [{}]:{} as {}', job.jobName, job.id, status)
        save(job.tap {
            it.status = status
            if (statusMessage) it.message = statusMessage
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
        AsyncJobFuture asyncJobFuture = getAsyncJobFuture(asyncJob.id)
        if (asyncJobFuture) {
            if (asyncJobFuture.hasMainTaskCompleted()) {
                log.error('Cannot cancel task as it has already completed')
                return
            }
            if (asyncJobFuture.hasTaskStarted()) cancellingJob(asyncJob.id)
            else cancelledJob(asyncJob.id)
            asyncJobFuture.cancelIfRunning()
        }
    }

    void cancelAllRunningJobs(long timeout, TimeUnit timeUnit) {
        Collection<String> jobIds = getAllRunningJobs()
        if (jobIds) {
            log.info('Cancelling all {} running jobs {}', jobIds.size(), jobIds)
            List<UUID> runningJobIds = jobIds.collect {Utils.toUuid(it)}
            // Call cancel on each job and wait for them to complete
            List<AsyncJobFuture> futures = runningJobIds.collect {asyncJobId ->
                cancellingJob(asyncJobId)
                getAsyncJobFuture(asyncJobId).cancelIfRunning()
            }
            futures.every {ajf -> ajf.awaitCompletion(timeout, timeUnit)}
            // Update the database
            runningJobIds.each {cancelledJob(it)}
        }
    }

    Collection<String> getAllRunningJobs() {
        GrailsCache jobCache = getAsyncJobCache()
        Collection<String> jobIds = jobCache.getAllKeys() as Collection<String>
        if (!jobIds) return []
        jobIds.findAll {id ->
            !jobCache.get(id, AsyncJobFuture).isDone()
        }
    }

    AsyncJobFuture getAsyncJobFuture(String id) {
        getAsyncJobFuture(Utils.toUuid(id))
    }

    AsyncJobFuture getAsyncJobFuture(UUID id) {
        getAsyncJobCache().get(id, AsyncJobFuture)
    }

    AsyncJobFuture getAsyncJobFuture(AsyncJob asyncJob) {
        getAsyncJobFuture(asyncJob.id)
    }

    void shutdownAndAwaitTermination(long timeout, TimeUnit timeUnit) {
        Utils.shutdownAndAwaitTermination(executorService, timeout, timeUnit)
    }

    private GrailsCache getAsyncJobCache() {
        grailsCacheManager.getCache(ASYNC_JOB_CACHE_KEY) as GrailsCache
    }
}
