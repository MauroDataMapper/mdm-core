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
package uk.ac.ox.softeng.maurodatamapper.core.task

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Wrapping class to contain the Future and the task creating the future.
 * Required to allow knowledge about the state of the task.
 * As the task's thread can be interrupted but it needs to make additional changes or effects we need to
 * know what state the task is in.
 *
 * The state of the task is "not started", "running", "main task completed", "all parts completed".
 *
 * Task completion involves either that the task hasnt started and was cancelled before it did, or that its started and that the current transaction has been
 * rolled back and the correct state applied to the asyncjob domain related to the task
 *
 * We use the future to control interruption/cancellation of the task but then make sure the task is in the appropriate state before claiming its "done"
 * @since 17/05/2022
 */
@SuppressFBWarnings('LI_LAZY_INIT_STATIC')
class AsyncJobFuture implements Future<AsyncJobTask> {

    Future<Boolean> future
    AsyncJobTask task

    AsyncJobFuture(AsyncJobTask task, Future<Boolean> future) {
        this.future = future
        this.task = task
    }

    @Override
    boolean cancel(boolean mayInterruptIfRunning) {
        future.cancel(mayInterruptIfRunning)
    }

    @Override
    boolean isCancelled() {
        future.isCancelled()
    }

    @Override
    boolean isDone() {
        future.isDone() && !isTaskRunning()
    }

    @Override
    AsyncJobTask get() throws InterruptedException, ExecutionException {
        try {
            future.get()
        } catch (CancellationException ex) {
            awaitTaskCompletion()
            throw ex
        }
        task
    }

    @Override
    AsyncJobTask get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            future.get(timeout, unit)
        } catch (CancellationException ex) {
            awaitTaskCompletion()
            throw ex
        }
        task
    }

    AsyncJobFuture cancelIfRunning() {
        if (!isDone()) cancel(true)
        this
    }

    AsyncJobFuture awaitCompletion(long timeout, TimeUnit timeUnit) {
        // Wait for cancellation to go through
        long duration = 0
        while (!hasTimedOut(duration, timeout, timeUnit) && !isDone()) {
            duration++
            sleep(1)
        }
        this
    }

    boolean hasTimedOut(long duration, long timeout, TimeUnit timeUnit) {
        //Duration is in milliseconds
        long milliTimeout = timeUnit.toMillis(timeout)
        duration >= milliTimeout
    }

    boolean hasTaskStarted() {
        task.hasStarted.get()
    }

    boolean hasMainTaskCompleted() {
        isDone() || task.mainTaskCompleted.get()
    }

    private boolean isTaskRunning() {
        hasTaskStarted() && !task.fullyCompleted.get()
    }

    private void awaitTaskCompletion() {
        while (isTaskRunning()) {
            sleep(1)
        }
    }


}
