package uk.ac.ox.softeng.maurodatamapper.core.task

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

    AsyncJobFuture cancelIfRunningAndAwaitCompletion() {
        cancelIfRunning()
        awaitCompletion()
    }

    AsyncJobFuture awaitCompletion() {
        // Wait for cancellation to go through
        while (!isDone()) {
            sleep(1)
        }
        this
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
