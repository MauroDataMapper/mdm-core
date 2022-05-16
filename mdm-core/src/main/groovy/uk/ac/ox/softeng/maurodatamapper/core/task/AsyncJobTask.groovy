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

/**
 * @since 28/03/2022
 */
@Slf4j
class AsyncJobTask implements Callable<AsyncJob> {

    AsyncJobService asyncJobService
    AsyncJob asyncJob
    Closure taskToExecute

    AsyncJobTask(AsyncJobService asyncJobService, AsyncJob asyncJob, Closure taskToExecute) {
        this.asyncJobService = asyncJobService
        this.asyncJob = asyncJob
        this.taskToExecute = taskToExecute
    }

    @Override
    AsyncJob call() {
        long st = System.currentTimeMillis()

        // Set the job as "running"
        performStep {
            log.info('Starting async task {}:{}', asyncJob.jobName, asyncJob.id)
            asyncJob.merge()
            asyncJob.status = 'RUNNING'
            asyncJobService.save(asyncJob)
        }

        // Perform the required task
        boolean completed = performTask()

        if (completed) {
            log.debug('Task completed successfully')
            // Set the job as completed (interrupted thread will call this but not run it due to the check in performStep
            performStep {
                asyncJobService.completedJob(asyncJob)
                log.info('Async task {}:{} completed in {}', asyncJob.jobName, asyncJob.id, Utils.timeTaken(st))
            }
        }
        asyncJob
    }

    boolean performTask() {
        try {
            log.debug('Performing async task')
            performStep taskToExecute
            true
        } catch (ApiException apiException) {
            // Handle known exceptions
            log.error("Failed to complete async job ${asyncJob.jobName} because ${apiException.message}")
            performStep {asyncJobService.failedJob(asyncJob, apiException.message)}
            false
        } catch (Exception ex) {
            log.error("Unhandled failure to complete async job ${asyncJob.jobName} because ${ex.message}", ex)
            performStep {asyncJobService.failedJob(asyncJob, ex.message)}
            false
        }
    }

    void performStep(Closure closure) {
        // Don't perform the step if the thread has been interrupted
        if (Thread.currentThread().interrupted) return


        // Perform each step in its own session and transaction to ensure thread interruption doesnt effect earlier steps
        AsyncJob.withNewSession {session ->
            AsyncJob.withNewTransaction {transactionStatus ->

                closure.call()

                // If the thread was interrupted then rollback the transaction
                if (Thread.currentThread().interrupted) {
                    transactionStatus.setRollbackOnly()
                } else {
                    // Otherwise make sure the session is flushed and cleared
                    session.flush()
                    session.clear()
                }
            }
        }

    }
}
