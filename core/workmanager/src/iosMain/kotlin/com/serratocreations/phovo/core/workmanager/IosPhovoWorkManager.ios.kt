package com.serratocreations.phovo.core.workmanager

import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGContinuedProcessingTask
import platform.BackgroundTasks.BGContinuedProcessingTaskRequest
import platform.BackgroundTasks.BGContinuedProcessingTaskRequestSubmissionStrategy
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.math.max

/**
 * iOS [PhovoWorkManager].
 *
 * There is no iOS equivalent of WorkManager, so this builds one out of three parts:
 *
 *  1. [WorkRecordStore] holds the queue on disk so work outlives the process.
 *  2. BGTaskScheduler asks the OS for background time. iOS allows exactly one pending request per
 *     identifier, so both registered tasks are "drain whatever is due" rather than one task per
 *     job, and a new request is submitted after every run.
 *  3. A foreground drain on app-active, because iOS grants background time unpredictably and
 *     essentially never in the simulator. Without this the queue would look permanently stuck
 *     during development.
 *
 * [registerBackgroundTasks] must be called before the app finishes launching.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPhovoWorkManager internal constructor(
    private val store: WorkRecordStore,
    private val registry: WorkerRegistry,
    private val constraintMonitor: IosConstraintMonitor,
    defaultDispatcher: CoroutineDispatcher,
    private val logger: PhovoLogger
) : PhovoWorkManager {

    // Deliberately not the app scope: that scope's exception handler rethrows to crash the process,
    // and a misbehaving worker should fail its own work, not take the app down.
    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val drainMutex = Mutex()

    /**
     * Unique work names with a live BGContinuedProcessingTaskRequest. The ordinary drain leaves
     * these alone so the continued task is the thing that runs them, which is the whole point:
     * a continued task keeps going after the user leaves, while a foreground drain gets about
     * thirty seconds. A name only lands here once submission actually succeeded, so a rejected
     * request falls back to the ordinary queue.
     */
    private val continuedSubmissions = MutableStateFlow<Set<String>>(emptySet())

    /**
     * BGContinuedProcessingTask is iOS 26+. The deployment target is well below that, and
     * Kotlin/Native does not model @available, so this is checked at run time and everything
     * below falls back to the BGProcessingTask queue.
     */
    private val supportsContinuedProcessing: Boolean by lazy {
        NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
            cValue<NSOperatingSystemVersion> {
                majorVersion = 26
                minorVersion = 0
                patchVersion = 0
            }
        )
    }

    /**
     * Registers the BGTaskScheduler handlers and the foreground drain, then catches up on anything
     * already due. Must run before the app finishes launching, or BGTaskScheduler raises.
     */
    fun registerBackgroundTasks() {
        constraintMonitor.start()

        registerHandler(PROCESSING_TASK_IDENTIFIER)
        registerHandler(REFRESH_TASK_IDENTIFIER)
        if (supportsContinuedProcessing) {
            // Registered against the wildcard identifier from Info.plist; submissions use concrete
            // ids beneath it. Unlike every other BGTask, these registrations are explicitly exempt
            // from the "before the app finishes launching" rule.
            val registered = BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
                identifier = CONTINUED_TASK_IDENTIFIER_WILDCARD,
                usingQueue = null
            ) { task ->
                val continued = task as? BGContinuedProcessingTask
                if (continued == null) {
                    task?.setTaskCompletedWithSuccess(false)
                } else {
                    handleContinuedProcessingTask(continued)
                }
            }
            if (!registered) {
                logger.e {
                    "BGTaskScheduler refused '$CONTINUED_TASK_IDENTIFIER_WILDCARD'. Long-running " +
                        "work will fall back to the ordinary queue."
                }
            }
        }

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { scope.launch { drainWithAssertion() } }
        )

        scope.launch {
            drainWithAssertion()
            submitNextRequests()
        }
    }

    private fun registerHandler(identifier: String) {
        val registered = BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = identifier,
            usingQueue = null
        ) { task -> handleBackgroundTask(task) }

        if (!registered) {
            // Almost always a missing BGTaskSchedulerPermittedIdentifiers entry in Info.plist.
            logger.e { "BGTaskScheduler refused to register '$identifier'. Background work will only run in the foreground." }
        }
    }

    private fun handleBackgroundTask(task: BGTask?) {
        // No assertion here: the BGTask itself is the execution window, and its expirationHandler
        // plays the same role.
        val job = scope.launch { drainDueWork() }
        task?.expirationHandler = { job.cancel() }
        job.invokeOnCompletion { cause ->
            submitNextRequests()
            task?.setTaskCompletedWithSuccess(cause == null)
        }
    }

    override fun enqueueUniqueWork(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    ) {
        val existing = store.find(uniqueWorkName)
        val keepExisting = policy == ExistingWorkPolicy.KEEP &&
            existing != null &&
            !existing.state.isFinished
        if (keepExisting) return

        val record = PersistedWorkRecord(
            uniqueWorkName = uniqueWorkName,
            workerId = request.workerId,
            periodic = false,
            requiredNetworkType = request.constraints.requiredNetworkType,
            requiresCharging = request.constraints.requiresCharging,
            requiresBatteryNotLow = request.constraints.requiresBatteryNotLow,
            expedited = request.expedited,
            longRunningTitle = request.longRunning?.title,
            longRunningSubtitle = request.longRunning?.subtitle,
            backoffPolicy = request.backoffPolicy,
            backoffDelayMillis = request.backoffDelay.inWholeMilliseconds,
            tags = request.tags,
            earliestRunAtEpochMillis = nowEpochMillis() + request.initialDelay.inWholeMilliseconds
        )
        put(record)
        onQueueChanged()
    }

    override fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        policy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest
    ) {
        val existing = store.find(uniqueWorkName)
        if (policy == ExistingPeriodicWorkPolicy.KEEP && existing != null && !existing.state.isFinished) return

        val repeatInterval = request.repeatInterval.coerceAtLeast(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL)
        // UPDATE swaps in the new definition without disturbing when the next run was already due,
        // which is what makes it safe for a consumer to re enqueue on every app launch.
        val keepNextRunTime = policy == ExistingPeriodicWorkPolicy.UPDATE &&
            existing != null &&
            !existing.state.isFinished

        val record = PersistedWorkRecord(
            uniqueWorkName = uniqueWorkName,
            workerId = request.workerId,
            periodic = true,
            repeatIntervalMillis = repeatInterval.inWholeMilliseconds,
            requiredNetworkType = request.constraints.requiredNetworkType,
            requiresCharging = request.constraints.requiresCharging,
            requiresBatteryNotLow = request.constraints.requiresBatteryNotLow,
            backoffPolicy = request.backoffPolicy,
            backoffDelayMillis = request.backoffDelay.inWholeMilliseconds,
            tags = request.tags,
            earliestRunAtEpochMillis = if (keepNextRunTime) {
                existing.earliestRunAtEpochMillis
            } else {
                nowEpochMillis() + request.initialDelay.inWholeMilliseconds
            },
            runAttemptCount = if (keepNextRunTime) existing.runAttemptCount else 0
        )
        put(record)
        onQueueChanged()
    }

    override fun cancelUniqueWork(uniqueWorkName: String) {
        withdrawContinued(uniqueWorkName)
        store.update { records ->
            records.map { record ->
                if (record.uniqueWorkName == uniqueWorkName && !record.state.isFinished) {
                    record.copy(state = WorkState.CANCELLED)
                } else {
                    record
                }
            }
        }
        onQueueChanged()
    }

    override fun cancelAllWorkByTag(tag: String) {
        store.records.value
            .filter { tag in it.tags && !it.state.isFinished }
            .forEach { withdrawContinued(it.uniqueWorkName) }
        store.update { records ->
            records.map { record ->
                if (tag in record.tags && !record.state.isFinished) {
                    record.copy(state = WorkState.CANCELLED)
                } else {
                    record
                }
            }
        }
        onQueueChanged()
    }

    override fun getWorkInfoFlow(uniqueWorkName: String): Flow<WorkInfo?> = store.records
        .map { records -> records.firstOrNull { it.uniqueWorkName == uniqueWorkName }?.toWorkInfo() }
        .distinctUntilChanged()

    override suspend fun getWorkInfo(uniqueWorkName: String): WorkInfo? =
        store.find(uniqueWorkName)?.toWorkInfo()

    // region draining

    /**
     * Drains the queue while holding a UIKit background task assertion.
     *
     * Without one, iOS suspends the process a few seconds after the user leaves the app, freezing
     * a worker mid run and throwing away its progress. The assertion buys roughly thirty seconds
     * past that point, which is usually enough to finish the item in flight and checkpoint it.
     *
     * The assertion must always be ended. Holding one past its expiration gets the app killed by
     * the watchdog, so [end] runs from the expiration handler and from a finally block, and is
     * written to tolerate being called twice.
     */
    private suspend fun drainWithAssertion() {
        val app = UIApplication.sharedApplication
        val job = currentCoroutineContext()[Job]
        var taskId: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
        var ended = false

        fun end() {
            if (!ended && taskId != UIBackgroundTaskInvalid) {
                ended = true
                app.endBackgroundTask(taskId)
            }
        }

        taskId = app.beginBackgroundTaskWithName(BACKGROUND_ASSERTION_NAME) {
            // Invoked on the main thread, and the last chance before the watchdog steps in.
            // Cancelling puts the running record back to ENQUEUED without burning a retry, the
            // same way an expired BGTask window does.
            job?.cancel()
            end()
        }

        try {
            drainDueWork()
        } finally {
            end()
        }
    }

    /**
     * Runs every record that is due and whose constraints hold, one at a time, until the queue is
     * exhausted or the execution window is revoked. Serialised so the foreground and background
     * drains cannot run the same worker twice.
     */
    private suspend fun drainDueWork() = drainMutex.withLock {
        while (currentCoroutineContext().isActive) {
            val owned = continuedSubmissions.value
            val due = store.records.value.filter { record ->
                record.isDue(nowEpochMillis()) &&
                    constraintMonitor.isSatisfied(record.constraints) &&
                    record.uniqueWorkName !in owned
            }
            // Expedited work jumps the queue. Within each group the order is insertion order,
            // which keeps a backlog draining oldest first.
            val next = due.firstOrNull { it.expedited } ?: due.firstOrNull() ?: break
            run(next)
        }
    }

    private suspend fun run(
        record: PersistedWorkRecord,
        progressReporter: WorkProgressReporter? = null
    ) {
        val worker = registry.create(record.workerId)
        if (worker == null) {
            // The worker was renamed or deleted while work naming it was still on disk. Retrying
            // would spin forever, so fail it and let the record show why.
            logger.e { "No worker registered for id '${record.workerId}', failing '${record.uniqueWorkName}'." }
            put(record.copy(state = WorkState.FAILED))
            return
        }

        put(record.copy(state = WorkState.RUNNING))

        worker.progressReporter = progressReporter
        val result = try {
            worker.doWork()
        } catch (e: CancellationException) {
            // The window expired. Put the work back exactly as it was so the next window retries it
            // without burning an attempt.
            put(record.copy(state = WorkState.ENQUEUED))
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Worker '${record.workerId}' threw, failing '${record.uniqueWorkName}'." }
            WorkResult.Failure
        } finally {
            worker.progressReporter = null
        }

        // Re-read: a cancel may have landed while the worker was running, and it wins.
        val current = store.find(record.uniqueWorkName)
        if (current == null || current.state == WorkState.CANCELLED) return

        put(current.nextState(result))
    }

    private fun PersistedWorkRecord.nextState(result: WorkResult): PersistedWorkRecord = when (result) {
        is WorkResult.Success -> {
            val interval = repeatInterval
            if (periodic && interval != null) {
                copy(
                    state = WorkState.ENQUEUED,
                    runAttemptCount = 0,
                    earliestRunAtEpochMillis = nowEpochMillis() + interval.inWholeMilliseconds
                )
            } else {
                copy(state = WorkState.SUCCEEDED)
            }
        }

        is WorkResult.Retry -> {
            val attempts = runAttemptCount + 1
            copy(
                state = WorkState.ENQUEUED,
                runAttemptCount = attempts,
                earliestRunAtEpochMillis = nowEpochMillis() +
                    backoffPolicy.delayForAttempt(attempts, backoffDelay).inWholeMilliseconds
            )
        }

        // Matches androidx.work: a failure ends periodic work rather than waiting out the period.
        is WorkResult.Failure -> copy(state = WorkState.FAILED)
    }

    // endregion

    // region scheduling

    private fun put(record: PersistedWorkRecord) {
        store.update { records ->
            records.filterNot { it.uniqueWorkName == record.uniqueWorkName } + record
        }
    }

    private fun onQueueChanged() {
        submitContinuedRequests()
        submitNextRequests()
        scope.launch { drainWithAssertion() }
    }

    // region continued processing (iOS 26+)

    /**
     * Submits a BGContinuedProcessingTaskRequest for every pending long-running record that does
     * not already have one.
     *
     * The request must be made on behalf of a foregrounded app, and the scheduler ignores
     * earliestBeginDate in favour of now, so this is the "user asked for it and is watching"
     * path rather than anything periodic.
     */
    private fun submitContinuedRequests() {
        if (!supportsContinuedProcessing) return

        val pending = store.records.value.filter { record ->
            record.state == WorkState.ENQUEUED &&
                record.longRunningTitle != null &&
                record.uniqueWorkName !in continuedSubmissions.value
        }

        pending.forEach { record ->
            val request = BGContinuedProcessingTaskRequest(
                identifier = continuedIdentifier(record.uniqueWorkName),
                title = record.longRunningTitle.orEmpty(),
                subtitle = record.longRunningSubtitle.orEmpty()
            ).apply {
                // Queue rather than Fail: under load the request waits instead of being rejected.
                // Queued requests are dropped when the user swipes the app out of the switcher,
                // and the record stays ENQUEUED so the next launch picks it up.
                strategy = BGContinuedProcessingTaskRequestSubmissionStrategy
                    .BGContinuedProcessingTaskRequestSubmissionStrategyQueue
            }

            if (submit(request)) {
                continuedSubmissions.update { it + record.uniqueWorkName }
            } else {
                logger.w {
                    "Continued processing request for '${record.uniqueWorkName}' was rejected; " +
                        "it will run on the ordinary queue instead."
                }
            }
        }
    }

    private fun handleContinuedProcessingTask(task: BGContinuedProcessingTask) {
        val record = store.records.value.firstOrNull { candidate ->
            continuedIdentifier(candidate.uniqueWorkName) == task.identifier
        }

        if (record == null || record.state != WorkState.ENQUEUED) {
            // Cancelled, or already drained by something else while the request sat queued.
            releaseContinued(record?.uniqueWorkName)
            task.setTaskCompletedWithSuccess(true)
            return
        }

        val progress = task.progress
        val reporter = WorkProgressReporter { completed, total ->
            // iOS may forcibly expire a continued task that looks stalled, so this is not
            // decoration. Workers are expected to call setProgress as they go.
            progress.totalUnitCount = total
            progress.completedUnitCount = completed
        }

        val job = scope.launch {
            drainMutex.withLock { run(record, reporter) }
        }
        task.expirationHandler = { job.cancel() }
        job.invokeOnCompletion { cause ->
            releaseContinued(record.uniqueWorkName)
            task.setTaskCompletedWithSuccess(cause == null)
            // Whatever is left over goes back through the ordinary path.
            submitNextRequests()
            scope.launch { drainWithAssertion() }
        }
    }

    /** Withdraws a pending continued processing request, for work that is being cancelled. */
    private fun withdrawContinued(uniqueWorkName: String) {
        if (!supportsContinuedProcessing) return
        if (uniqueWorkName !in continuedSubmissions.value) return
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(
            continuedIdentifier(uniqueWorkName)
        )
        releaseContinued(uniqueWorkName)
    }

    private fun releaseContinued(uniqueWorkName: String?) {
        if (uniqueWorkName == null) return
        continuedSubmissions.update { it - uniqueWorkName }
    }

    /**
     * Derives this record's concrete continued-processing identifier.
     *
     * Apple requires the permitted identifier in Info.plist to be a wildcard prefixed with the
     * bundle id, with each submission using a concrete id beneath it. Deriving it from the unique
     * work name keeps the mapping stateless, so the launch handler can find its record by name
     * alone. The hash disambiguates names that sanitise to the same string.
     */
    private fun continuedIdentifier(uniqueWorkName: String): String {
        val sanitized = uniqueWorkName.map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")
        return CONTINUED_TASK_IDENTIFIER_PREFIX + sanitized + "-" + uniqueWorkName.hashCode().toUInt()
    }

    // endregion

    /**
     * Rebuilds the pending BGTaskScheduler requests from the current queue. Both identifiers drain
     * the same queue; submitting both just gives the OS two differently shaped opportunities to
     * hand us time, since app refresh windows are granted far more readily than processing ones.
     */
    private fun submitNextRequests() {
        val pending = store.records.value.filter { it.state == WorkState.ENQUEUED }
        // Cancel these two by name rather than calling cancelAllTaskRequests, which would also
        // drop any in-flight continued processing requests.
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(PROCESSING_TASK_IDENTIFIER)
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(REFRESH_TASK_IDENTIFIER)
        if (pending.isEmpty()) return

        // A nil earliestBeginDate tells iOS "whenever you can", which is the most we can ask for.
        // It is still the OS's decision; nothing here wakes a suspended app on demand.
        val beginDate = if (pending.any { it.expedited }) {
            null
        } else {
            val earliest = pending.minOf { it.earliestRunAtEpochMillis }
            NSDate.dateWithTimeIntervalSince1970(
                max(earliest, nowEpochMillis()).toDouble() / MILLIS_PER_SECOND
            )
        }

        val processing = BGProcessingTaskRequest(PROCESSING_TASK_IDENTIFIER).apply {
            earliestBeginDate = beginDate
            // The union of what the queue wants. A record whose own constraints are stricter is
            // still re checked by the constraint monitor before it runs.
            requiresNetworkConnectivity = pending.any { it.requiredNetworkType != NetworkType.NONE }
            requiresExternalPower = pending.any { it.requiresCharging }
        }
        submit(processing)

        val refresh = BGAppRefreshTaskRequest(REFRESH_TASK_IDENTIFIER).apply {
            earliestBeginDate = beginDate
        }
        submit(refresh)
    }

    private fun submit(request: BGTaskRequest): Boolean = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val submitted = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error.ptr)
        if (!submitted) {
            // Expected in the simulator and whenever Background App Refresh is switched off.
            // The foreground drain still runs everything, so this is not fatal.
            logger.w { "BGTaskScheduler rejected '${request.identifier}': ${error.value?.localizedDescription}" }
        }
        submitted
    }

    // endregion

    private fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()

    companion object {
        /**
         * Both identifiers must appear in the app's Info.plist under
         * BGTaskSchedulerPermittedIdentifiers or registration fails at launch.
         */
        const val PROCESSING_TASK_IDENTIFIER = "com.serratocreations.phovo.work.processing"
        const val REFRESH_TASK_IDENTIFIER = "com.serratocreations.phovo.work.refresh"

        /**
         * Continued processing requires the permitted identifier to be a wildcard whose prefix
         * starts with the app's bundle id, which is why this one is shaped differently from the
         * two above.
         */
        const val CONTINUED_TASK_IDENTIFIER_PREFIX = "com.serratocreations.phovo.Phovo.work.continued."
        const val CONTINUED_TASK_IDENTIFIER_WILDCARD = CONTINUED_TASK_IDENTIFIER_PREFIX + "*"

        private const val MILLIS_PER_SECOND = 1000.0
        private const val BACKGROUND_ASSERTION_NAME = "phovo-work-drain"
    }
}

