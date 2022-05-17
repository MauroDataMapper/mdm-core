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
package uk.ac.ox.softeng.maurodatamapper.core.task

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @since 28/03/2022
 */
@Slf4j
class AsyncJobTask implements Callable<Boolean> {

    AsyncJobService asyncJobService
    String asyncJobName
    UUID asyncJobId
    Closure taskToExecute
    AtomicBoolean fullyCompleted
    AtomicBoolean hasStarted
    AtomicBoolean mainTaskCompleted

    AsyncJobTask(AsyncJobService asyncJobService, AsyncJob asyncJob, Closure taskToExecute) {
        this.asyncJobService = asyncJobService
        this.asyncJobId = asyncJob.id
        this.asyncJobName = asyncJob.jobName
        this.taskToExecute = taskToExecute
        this.fullyCompleted = new AtomicBoolean(false)
        this.hasStarted = new AtomicBoolean(false)
        this.mainTaskCompleted = new AtomicBoolean(false)
    }

    @Override
    Boolean call() {
        long st = System.currentTimeMillis()
        hasStarted.set true
        // Set the job as "running"
        performStep {
            log.info('Starting async task [{}]:{}', asyncJobName, asyncJobId)
            asyncJobService.runningJob(asyncJobId)
        }

        // Perform the required task
        boolean completed = performTask()

        if (completed) {
            log.debug('Task completed successfully')
            mainTaskCompleted.set(true)
            // Set the job as completed (interrupted thread will call this but not run it due to the check in performStep
            performStep {
                asyncJobService.completedJob(asyncJobId)
                log.info('Async task [{}]:{} completed in {}', asyncJobName, asyncJobId, Utils.timeTaken(st))
            }
        } else if (isInterrupted(false)) {
            performStep(true) {
                asyncJobService.cancelledJob(asyncJobId)
                log.info('Async task [{}]:{} cancelled in {}', asyncJobName, asyncJobId, Utils.timeTaken(st))
            }
        }
        fullyCompleted.set true
        true
    }

    boolean performTask() {
        try {
            log.debug('Performing async task')
            performStep(taskToExecute)
        } catch (ApiException apiException) {
            // Handle known exceptions
            log.error("Failed to complete async job ${asyncJobName} because ${apiException.message}")
            performStep {asyncJobService.failedJob(asyncJobId, apiException.message)}
            false
        } catch (Exception ex) {
            log.error("Unhandled failure to complete async job ${asyncJobName} because ${ex.message}", ex)
            performStep {asyncJobService.failedJob(asyncJobId, ex.message)}
            false
        }
    }

    boolean performStep(boolean ignoreInterrupt = false, Closure closure) {
        // Don't perform the step if the thread has been interrupted
        if (isInterrupted(ignoreInterrupt)) return false

        boolean stepFlushed = false
        // Perform each step in its own session and transaction to ensure thread interruption doesnt effect earlier steps
        AsyncJob.withNewSession {session ->
            AsyncJob.withNewTransaction {transactionStatus ->

                closure.call()

                // If the thread was interrupted then rollback the transaction
                if (isInterrupted(ignoreInterrupt)) {
                    log.warn('Interrupted step so transaction will be rolled back')
                    transactionStatus.setRollbackOnly()
                    stepFlushed = false
                } else {
                    // Otherwise make sure the session is flushed and cleared
                    session.flush()
                    session.clear()
                    stepFlushed = true
                }
            }
        }
        stepFlushed
    }

    boolean isInterrupted(boolean ignoreInterrupt) {
        !ignoreInterrupt && Thread.currentThread().interrupted
    }
}
