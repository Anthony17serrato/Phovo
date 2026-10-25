package com.serratocreations.phovo.core.workmanager

import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Long-running work that runs in-process while the app is in the foreground, the way
 * IosPhovoWorkManager runs it on its ordinary drain.
 *
 * androidx.work would promote long-running work to a foreground service straight away, and from
 * Android 16 that time counts against the app's job quota. While the app is open nothing needs
 * keeping alive, so the worker just runs here. If the user leaves mid run, AndroidPhovoWorkManager
 * hands the run to an androidx job, whose PhovoDelegatingWorker joins it from the foreground
 * service. The run itself never moves: it stays in [scope], so cancelling the androidx job when
 * the user comes back only cancels the join.
 */
internal class ForegroundRuns(
    defaultDispatcher: CoroutineDispatcher,
    private val logger: PhovoLogger
) {
    // Not the app scope, for the same reason as on iOS: a misbehaving worker should fail its own
    // work, not crash the app through the app scope's handler.
    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    class Run(
        val uniqueWorkName: String,
        val request: OneTimeWorkRequest,
        val worker: PhovoWorker,
        val result: Deferred<WorkResult>
    ) {
        /** Set once an androidx job has been enqueued to carry this run into the background. */
        @Volatile
        var handedOff: Boolean = false

        /** Set while a PhovoDelegatingWorker is joined to this run. */
        @Volatile
        var joined: Boolean = false
    }

    /** Runs in flight, and the outcome of each name's latest run, keyed by unique work name. */
    private val active = MutableStateFlow<Map<String, Run>>(emptyMap())
    private val outcomes = MutableStateFlow<Map<String, WorkInfo>>(emptyMap())

    fun active(uniqueWorkName: String): Run? = active.value[uniqueWorkName]

    fun allActive(): Collection<Run> = active.value.values

    /**
     * Starts [worker] for [request] under [uniqueWorkName]. The caller has already resolved the
     * existing work policy. [onFinished] runs once the run ends on its own, not when cancelled.
     */
    fun start(
        uniqueWorkName: String,
        request: OneTimeWorkRequest,
        worker: PhovoWorker,
        onFinished: (Run, WorkResult) -> Unit
    ): Run {
        val result = scope.async(start = CoroutineStart.LAZY) {
            try {
                worker.doWork()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Worker '${request.workerId}' threw in-process, failing '$uniqueWorkName'." }
                WorkResult.Failure
            }
        }
        val run = Run(uniqueWorkName, request, worker, result)
        active.update { it + (uniqueWorkName to run) }
        outcomes.update { it + (uniqueWorkName to run.info(WorkState.RUNNING)) }

        result.invokeOnCompletion { cause ->
            active.update { current -> if (current[uniqueWorkName] === run) current - uniqueWorkName else current }
            if (cause != null) return@invokeOnCompletion
            @OptIn(ExperimentalCoroutinesApi::class)
            val finished = result.getCompleted()
            onFinished(run, finished)
        }
        result.start()
        return run
    }

    /** Records the final state of [run], unless a newer run under the same name took over. */
    fun recordOutcome(run: Run, state: WorkState) {
        outcomes.update { current ->
            if (active.value[run.uniqueWorkName] != null) current
            else current + (run.uniqueWorkName to run.info(state))
        }
    }

    /** Cancels the in-process run under [uniqueWorkName], if any, recording it as cancelled. */
    fun cancel(uniqueWorkName: String) {
        val run = active.value[uniqueWorkName] ?: return
        run.result.cancel()
        active.update { current -> if (current[uniqueWorkName] === run) current - uniqueWorkName else current }
        outcomes.update { it + (uniqueWorkName to run.info(WorkState.CANCELLED)) }
    }

    /** Names of the in-process runs carrying [tag]. */
    fun namesWithTag(tag: String): List<String> =
        active.value.values.filter { tag in it.request.tags }.map { it.uniqueWorkName }

    /**
     * Drops what this registry knows about [uniqueWorkName], so androidx.work's own state shows
     * through again. Used when new work for the name goes to androidx.work instead.
     */
    fun forget(uniqueWorkName: String) {
        outcomes.update { it - uniqueWorkName }
    }

    /** This registry's view of [uniqueWorkName], or null when androidx.work's view applies. */
    fun infoFlow(uniqueWorkName: String): Flow<WorkInfo?> =
        outcomes.map { it[uniqueWorkName] }.distinctUntilChanged()

    private fun Run.info(state: WorkState) = WorkInfo(
        uniqueWorkName = uniqueWorkName,
        state = state,
        runAttemptCount = 0,
        tags = request.tags
    )
}
