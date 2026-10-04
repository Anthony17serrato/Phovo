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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import platform.Foundation.NSProgress
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.math.max
import kotlin.random.Random

/**
 * iOS [PhovoWorkManager].
 *
 * There is no iOS equivalent of WorkManager, so this builds one, aiming to behave as much like
 * androidx.work as iOS allows:
 *
 *  1. [WorkRecordStore] holds the queue on disk so work outlives the process.
 *  2. A single *drain* runs whatever is due, one record at a time. It is triggered whenever the
 *     queue changes or the app becomes active, and coalesced so there is never more than one.
 *  3. BGTaskScheduler asks the OS for background time for queued work. iOS allows one pending
 *     request per identifier, so both registered tasks are "drain whatever is due".
 *  4. For long-running work, a BGContinuedProcessingTask (iOS 26+) plays the part of Android's
 *     foreground service: started as the user leaves, ended when they return or the worker
 *     returns. See the continued processing region.
 *
 * Android semantics carried over: REPLACE and cancel stop the running worker; a worker's result
 * decides the continued task's outcome; a stopped worker goes back to the queue without burning a
 * retry attempt.
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
     * Continued processing requests submitted and not yet finished, keyed by unique work name, with
     * the concrete identifier each went in under. Every submission gets a fresh identifier; see
     * [newContinuedIdentifier].
     */
    private val continuedSubmissions = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * Concrete continued processing identifiers this process has registered a handler for.
     * Registering the same identifier twice is documented to kill the app.
     */
    private val registeredContinuedIds = MutableStateFlow<Set<String>>(emptySet())

    /** Why a run was stopped on purpose, as opposed to its execution window going away. */
    private enum class StopReason { REPLACED, CANCELLED, EXPIRED }

    /** A record currently executing, so it can be stopped on its own or given a progress surface. */
    private class InFlight(
        val worker: PhovoWorker,
        val job: Job,
        val generation: Long
    ) {
        @kotlin.concurrent.Volatile
        var stopReason: StopReason? = null
    }

    private val inFlight = MutableStateFlow<Map<String, InFlight>>(emptyMap())

    /**
     * The continued processing task keeping each unique work name alive in the background.
     *
     * It belongs to the name, not to a particular run. That is what lets it survive REPLACE: the
     * replacement run picks up the same task and its progress surface, the way an Android
     * foreground service outlives the worker instance it was started for.
     */
    private val activeContinued = MutableStateFlow<Map<String, BGContinuedProcessingTask>>(emptyMap())

    /** Progress reporter bound to each active continued task, handed to whichever run is current. */
    private val continuedReporters = MutableStateFlow<Map<String, WorkProgressReporter>>(emptyMap())

    /**
     * Coalescing state for the drain. [drainRequested] records that the queue may have changed;
     * [drainActive] is set while the single drain loop is running. At most one drain exists at a
     * time, so there is at most one UIKit background assertion held for it.
     */
    private val drainRequested = MutableStateFlow(false)
    private val drainActive = MutableStateFlow(false)

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
        // Continued processing handlers are deliberately not registered here. The wildcard in
        // Info.plist is a permission to use identifiers beneath it, not an identifier itself, and
        // registering the literal wildcard is rejected. Each concrete identifier is registered
        // just before its request is submitted, which is exactly why Apple exempts these
        // registrations from the "before the app finishes launching" rule.

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = {
                handBackContinuedTasks()
                requestDrain()
            }
        )

        // Long-running work runs on the ordinary drain while the app is open, so there is no Live
        // Activity then. As the user leaves, running or runnable long-running work gets a continued
        // processing task to carry it into the background. willResignActive is the last moment
        // the app still counts as foregrounded, which the request initializer requires.
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { submitContinuedRequests() }
        )

        requestDrain()
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
            earliestRunAtEpochMillis = nowEpochMillis() + request.initialDelay.inWholeMilliseconds,
            generation = newGeneration()
        )
        put(record)
        if (existing != null && policy == ExistingWorkPolicy.REPLACE) {
            // As androidx does: the running worker for the old request is stopped. Written after the
            // new record so the stopped run sees it has been superseded.
            stopInFlight(uniqueWorkName, StopReason.REPLACED)
        }
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
            runAttemptCount = if (keepNextRunTime) existing.runAttemptCount else 0,
            // UPDATE leaves a running worker alone, as androidx does, so it keeps its generation and
            // its result still lands on the updated record.
            generation = if (keepNextRunTime) existing.generation else newGeneration()
        )
        put(record)
        if (existing != null && policy == ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE) {
            stopInFlight(uniqueWorkName, StopReason.REPLACED)
        }
        onQueueChanged()
    }

    override fun cancelUniqueWork(uniqueWorkName: String) {
        store.update { records ->
            records.map { record ->
                if (record.uniqueWorkName == uniqueWorkName && !record.state.isFinished) {
                    record.copy(state = WorkState.CANCELLED)
                } else {
                    record
                }
            }
        }
        // As on Android, cancelling stops the running worker and its foreground surface.
        stopInFlight(uniqueWorkName, StopReason.CANCELLED)
        withdrawContinued(uniqueWorkName)
        onQueueChanged()
    }

    override fun cancelAllWorkByTag(tag: String) {
        val affected = store.records.value
            .filter { tag in it.tags && !it.state.isFinished }
            .map { it.uniqueWorkName }
        store.update { records ->
            records.map { record ->
                if (tag in record.tags && !record.state.isFinished) {
                    record.copy(state = WorkState.CANCELLED)
                } else {
                    record
                }
            }
        }
        affected.forEach { name ->
            stopInFlight(name, StopReason.CANCELLED)
            withdrawContinued(name)
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
     * Asks for the queue to be drained. Cheap and safe to call from anywhere, as often as you like.
     *
     * Requests are coalesced: if a drain is already running, this only flags that it should make
     * one more pass when it finishes, so nothing enqueued in the meantime is missed. Without this,
     * every enqueue, return to the app and launch handler started its own drain, and each took a
     * UIKit assertion while it queued behind the lock doing nothing.
     */
    private fun requestDrain() {
        drainRequested.value = true
        if (drainActive.compareAndSet(expect = false, update = true)) {
            scope.launch { runDrainLoop() }
        }
    }

    private suspend fun runDrainLoop() {
        var completedNormally = false
        try {
            while (drainRequested.compareAndSet(expect = true, update = false)) {
                drainWithAssertion()
            }
            completedNormally = true
        } finally {
            drainActive.value = false
            // A request landing between the last check and clearing drainActive would otherwise be
            // lost. Not after a cancellation, though: that is the background window closing, and
            // going again would only take another assertion the system is about to expire.
            if (completedNormally && drainRequested.value) requestDrain()
        }
        // Whatever is still queued needs a BGTaskScheduler request for when the app is gone.
        submitNextRequests()
    }

    /**
     * Drains the queue while holding a UIKit background task assertion.
     *
     * Without one, iOS suspends the process a few seconds after the user leaves the app, freezing
     * a worker mid run and throwing away its progress. The assertion buys roughly thirty seconds
     * past that point, which is usually enough to finish the item in flight and checkpoint it.
     *
     * The assertion is taken only once the drain holds the lock, so it only ever covers real work.
     * It must always be ended: holding one past its expiration gets the app killed by the
     * watchdog, so [end] runs from the expiration handler and from a finally block, and tolerates
     * being called twice.
     */
    private suspend fun drainWithAssertion() = drainMutex.withLock {
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
            if (activeContinued.value.isEmpty()) {
                // Cancelling puts the running record back to ENQUEUED without burning a retry, the
                // same way an expired BGTask window does.
                job?.cancel()
            } else {
                // A continued task is keeping the process alive, as a foreground service would on
                // Android, so the run must not be cut off along with the assertion. Measured on
                // device: the run carried on well past this point.
                logger.d { "Background assertion expired; a continued task is keeping the drain alive." }
            }
            end()
        }

        try {
            drainDueWorkLocked()
        } finally {
            end()
        }
    }

    /** Drains within a BGTask's own execution window, which stands in for the assertion. */
    private suspend fun drainDueWork() = drainMutex.withLock { drainDueWorkLocked() }

    /**
     * Runs every record that is due and whose constraints hold, one at a time, until the queue is
     * exhausted or the execution window is revoked. Callers hold [drainMutex], which is what stops
     * the foreground drain and a BGTask drain from running the same worker twice.
     */
    private suspend fun drainDueWorkLocked() {
        while (currentCoroutineContext().isActive) {
            val due = store.records.value.filter { record ->
                record.isDue(nowEpochMillis()) && constraintMonitor.isSatisfied(record.constraints)
            }
            // Expedited work jumps the queue. Within each group the order is insertion order,
            // which keeps a backlog draining oldest first.
            val next = due.firstOrNull { it.expedited } ?: due.firstOrNull() ?: break
            run(next)
        }
    }

    /**
     * Runs one record until it returns or something stops it.
     *
     * The worker runs in its own child job, so one record can be stopped (REPLACE, cancel, or its
     * continued task expiring) without taking the rest of the drain down, the way androidx stops one
     * worker without touching the others.
     */
    private suspend fun run(record: PersistedWorkRecord) {
        val name = record.uniqueWorkName
        val worker = registry.create(record.workerId)
        if (worker == null) {
            // The worker was renamed or deleted while work naming it was still on disk. Retrying
            // would spin forever, so fail it and let the record show why.
            logger.e { "No worker registered for id '${record.workerId}', failing '$name'." }
            put(record.copy(state = WorkState.FAILED))
            return
        }

        // Replaced between being picked and starting: leave it for the loop to pick up afresh.
        val claimed = store.find(name)
        if (claimed == null || claimed.generation != record.generation || claimed.state != WorkState.ENQUEUED) return
        put(claimed.copy(state = WorkState.RUNNING))
        logger.d { "Running '$name'." }

        var entry: InFlight? = null
        var result: WorkResult?
        try {
            result = coroutineScope {
                val child = async(start = CoroutineStart.LAZY) { worker.doWork() }
                val live = InFlight(worker, child, record.generation)
                entry = live
                inFlight.update { it + (name to live) }
                // Registered before reading here, and attachContinued writes before reading, so
                // whichever happens second sees the other and the run never misses its progress
                // surface.
                worker.progressReporter = continuedReporters.value[name]
                try {
                    child.await()
                } catch (stopped: CancellationException) {
                    // Only this record was stopped; the drain carries on. The reason is on the entry.
                    if (!isActive) throw stopped
                    null
                }
            }
        } catch (e: CancellationException) {
            // The drain's own window went away: a BGTask or the UIKit assertion expired. Same as a
            // system stop on Android: back to the queue without burning an attempt.
            restoreIfCurrent(record, WorkState.ENQUEUED)
            logger.d { "'$name' stopped: its execution window closed. Back to the queue." }
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Worker '${record.workerId}' threw, failing '$name'." }
            result = WorkResult.Failure
        } finally {
            worker.progressReporter = null
            inFlight.update { current -> if (current[name] === entry) current - name else current }
        }

        val finished = result
        if (finished != null) {
            val current = store.find(name)
            // A newer request under the same name, or a cancel, wins over this run's result.
            if (current != null && current.generation == record.generation && current.state != WorkState.CANCELLED) {
                put(current.nextState(finished))
                logger.d { "'$name' returned $finished." }
                // The worker returned, so its foreground surface goes, whatever it returned.
                activeContinued.value[name]?.let { finishContinued(name, it, finished is WorkResult.Success) }
            } else {
                logger.d { "'$name' returned $finished, discarded: replaced or cancelled meanwhile." }
            }
            return
        }

        when (entry?.stopReason) {
            // The replacement owns the record. Any continued task stays up and passes to the
            // replacement run, as a foreground service would.
            StopReason.REPLACED -> logger.d { "'$name' stopped: replaced. Any continued task passes to the replacement." }
            StopReason.CANCELLED -> logger.d { "'$name' stopped: cancelled." }
            StopReason.EXPIRED -> {
                restoreIfCurrent(record, WorkState.ENQUEUED)
                logger.d { "'$name' stopped: its continued task expired. Back to the queue." }
            }
            null -> {
                restoreIfCurrent(record, WorkState.ENQUEUED)
                logger.w { "'$name' stopped for no recorded reason. Back to the queue." }
            }
        }
    }

    /** Writes [state] back only if [record] is still the current request for its name. */
    private fun restoreIfCurrent(record: PersistedWorkRecord, state: WorkState) {
        val current = store.find(record.uniqueWorkName) ?: return
        if (current.generation == record.generation && current.state != WorkState.CANCELLED) {
            put(record.copy(state = state))
        }
    }

    private fun stopInFlight(uniqueWorkName: String, reason: StopReason) {
        val live = inFlight.value[uniqueWorkName] ?: return
        live.stopReason = reason
        live.job.cancel()
    }

    private fun newGeneration(): Long = Random.nextLong()

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
        // No continued processing request here, even for long-running work: that is submitted as
        // the user leaves, so the app shows no Live Activity while it is open.
        submitNextRequests()
        requestDrain()
    }

    // region continued processing (iOS 26+)
    //
    // The continued processing task is the iOS stand-in for an Android foreground service: an
    // implementation detail that keeps long-running work alive once the user leaves, with no
    // behaviour of its own. Work always runs on the drain; a continued task only keeps the process
    // alive and gives the current run a progress surface. It starts as the user leaves and ends
    // when they return, so there is no Live Activity while the app is open.

    /**
     * Submits a continued processing request for each long-running record that is running, or about
     * to, and has none yet. Called from willResignActive, the last moment the app still counts as
     * foregrounded, which the request initializer requires.
     */
    private fun submitContinuedRequests() {
        if (!supportsContinuedProcessing) return

        val candidates = store.records.value.filter { record ->
            record.longRunningTitle != null &&
                record.uniqueWorkName !in continuedSubmissions.value &&
                record.uniqueWorkName !in activeContinued.value &&
                (record.state == WorkState.RUNNING ||
                    (record.isDue(nowEpochMillis()) && constraintMonitor.isSatisfied(record.constraints)))
        }

        candidates.forEach { record ->
            val identifier = newContinuedIdentifier(record.uniqueWorkName)

            // Registering has to happen first. submitTaskRequest raises an Objective-C
            // NSInternalInconsistencyException when no handler exists for the identifier, and a
            // raised ObjC exception is not catchable from Kotlin, so it takes the app down.
            if (!ensureContinuedHandler(identifier)) return@forEach

            val request = BGContinuedProcessingTaskRequest(
                identifier = identifier,
                title = record.longRunningTitle.orEmpty(),
                subtitle = record.longRunningSubtitle.orEmpty()
            ).apply {
                // Queue rather than Fail: under load the request waits instead of being rejected.
                strategy = BGContinuedProcessingTaskRequestSubmissionStrategy
                    .BGContinuedProcessingTaskRequestSubmissionStrategyQueue
            }

            val accepted = submit(request)
            if (accepted) {
                continuedSubmissions.update { it + (record.uniqueWorkName to identifier) }
            }
        }
    }

    /**
     * Registers a launch handler for one concrete continued processing identifier, once per
     * process. Returns whether a handler is in place.
     */
    private fun ensureContinuedHandler(identifier: String): Boolean {
        if (identifier in registeredContinuedIds.value) return true

        val registered = BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = identifier,
            usingQueue = null
        ) { task ->
            val continued = task as? BGContinuedProcessingTask
            if (continued == null) {
                task?.setTaskCompletedWithSuccess(false)
            } else {
                handleContinuedProcessingTask(continued)
            }
        }

        if (registered) {
            registeredContinuedIds.update { it + identifier }
        } else {
            logger.e {
                "BGTaskScheduler refused to register '$identifier'. Check that " +
                    "'$CONTINUED_TASK_IDENTIFIER_WILDCARD' is in BGTaskSchedulerPermittedIdentifiers. " +
                    "Long-running work will run without background protection."
            }
        }
        return registered
    }

    private fun handleContinuedProcessingTask(task: BGContinuedProcessingTask) {
        val name = continuedSubmissions.value.entries.firstOrNull { it.value == task.identifier }?.key
        val record = name?.let { store.find(it) }
        if (name == null || record == null || record.state.isFinished) {
            // Finished, cancelled, or handed back while the request sat queued.
            name?.let { releaseContinued(it) }
            task.setTaskCompletedWithSuccess(false)
            return
        }

        attachContinued(name, task)
        if (inFlight.value[name] == null) {
            // Not running yet. The drain runs it like any other work; this task only keeps the
            // process alive while it does.
            requestDrain()
        }
    }

    /** Binds [task] to [name]: whichever run is current, now or after a REPLACE, reports into it. */
    private fun attachContinued(name: String, task: BGContinuedProcessingTask) {
        val progress = task.progress

        // Seed a determinate, non-zero value before the worker reports anything. Measured on
        // device: a completedUnitCount of zero renders as an indefinite bar, and a task whose
        // progress never moves is expired about a minute after the app is backgrounded.
        progress.totalUnitCount = SEED_PROGRESS_TOTAL
        progress.completedUnitCount = 1

        val reporter = WorkProgressReporter { completed, total -> progress.show(completed, total) }

        continuedReporters.update { it + (name to reporter) }
        activeContinued.update { it + (name to task) }
        inFlight.value[name]?.worker?.let { worker ->
            worker.progressReporter = reporter
            // Start from where the run already is. Anything it reported while the app was open
            // had nowhere to go, and it may not report again until its numbers next change.
            worker.lastReportedProgress?.let { last ->
                progress.show(last.completed, last.total)
            }
        }
        task.expirationHandler = { expireContinued(name, task) }
    }

    /**
     * Renders one progress report onto a continued task's bar.
     *
     * Movement is what keeps the task alive. Measured on device: repeating an identical value is
     * expired as fast as reporting nothing, while a value that falls back (a scan growing the
     * denominator) counts as movement.
     */
    private fun NSProgress.show(completed: Long, total: Long) {
        when {
            // Genuinely done.
            total > 0 && completed >= total -> {
                totalUnitCount = total
                completedUnitCount = total
            }
            // Too small a total to show "started but not done": keep the seed's look.
            total < 2 -> {
                totalUnitCount = SEED_PROGRESS_TOTAL
                completedUnitCount = 1
            }
            // Never zero (measured on device: zero renders as an indefinite bar), and never full,
            // so raising zero to one can't turn "not done" into a full bar.
            else -> {
                totalUnitCount = total
                completedUnitCount = completed.coerceIn(1, total - 1)
            }
        }
    }

    /** The system took the task back, like Android stopping a foreground service: back to the queue. */
    private fun expireContinued(name: String, task: BGContinuedProcessingTask) {
        logger.i { "Continued task for '$name' expired; the run goes back to the queue." }
        stopInFlight(name, StopReason.EXPIRED)
        finishContinued(name, task, success = false)
    }

    /**
     * Ends [task] only if it still fronts [name], so a task ended on one path is never ended twice.
     * The run it fronted, if any, is not touched.
     */
    private fun finishContinued(
        name: String,
        task: BGContinuedProcessingTask,
        success: Boolean
    ): Boolean {
        var owned = false
        activeContinued.update { current ->
            if (current[name] === task) {
                owned = true
                current - name
            } else {
                current
            }
        }
        if (!owned) return false

        val reporter = continuedReporters.value[name]
        continuedReporters.update { it - name }
        inFlight.value[name]?.worker?.let { worker ->
            if (worker.progressReporter === reporter) worker.progressReporter = null
        }
        releaseContinued(name)
        task.expirationHandler = null
        if (success) {
            // Apple's sample completes once progress reports finished; show a full bar, not
            // wherever the last report happened to leave it.
            val progress = task.progress
            progress.totalUnitCount = progress.totalUnitCount.coerceAtLeast(1)
            progress.completedUnitCount = progress.totalUnitCount
        }
        task.setTaskCompletedWithSuccess(success)
        return true
    }

    /**
     * The user is back in the app, so there is nothing for the Live Activity to add. Ending the task
     * dismisses it; the run is our coroutine, not the task's, so it keeps going. The next
     * willResignActive submits a fresh request that takes the run on again.
     *
     * Ended as unsuccessful: measured on device, ending it as successful left the next request for
     * that identifier accepted but never launched.
     */
    private fun handBackContinuedTasks() {
        activeContinued.value.forEach { (name, task) ->
            finishContinued(name, task, success = false)
        }

        // Accepted but never launched: withdraw, or the next willResignActive would skip the record.
        continuedSubmissions.value.forEach { (name, identifier) ->
            if (name !in activeContinued.value) {
                BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(identifier)
                releaseContinued(name)
            }
        }
    }

    /** Ends or withdraws any continued processing for [name], for work that is being cancelled. */
    private fun withdrawContinued(name: String) {
        if (!supportsContinuedProcessing) return
        activeContinued.value[name]?.let { finishContinued(name, it, success = false) }
        val identifier = continuedSubmissions.value[name] ?: return
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(identifier)
        releaseContinued(name)
    }

    private fun releaseContinued(name: String) {
        continuedSubmissions.update { it - name }
    }

    /**
     * A fresh concrete identifier for each submission, beneath the wildcard in Info.plist. Apple's
     * long-running article asks for the task-name part to be unique per job, and reusing one
     * identifier across cycles was measured to leave later submissions accepted but not launched.
     */
    private fun newContinuedIdentifier(uniqueWorkName: String): String {
        val sanitized = uniqueWorkName.map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")
        val nonce = Random.nextLong().toULong().toString(36)
        return "$CONTINUED_TASK_IDENTIFIER_PREFIX$sanitized-$nonce"
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
         *
         * This string belongs in Info.plist and nowhere else. It grants permission to use
         * identifiers beneath it; it is not itself registerable, and passing it to
         * registerForTaskWithIdentifier is rejected with "is not advertised in the application's
         * Info.plist". Handlers go on the concrete ids built by [continuedIdentifier].
         */
        const val CONTINUED_TASK_IDENTIFIER_PREFIX = "com.serratocreations.phovo.Phovo.work.continued."
        const val CONTINUED_TASK_IDENTIFIER_WILDCARD = CONTINUED_TASK_IDENTIFIER_PREFIX + "*"

        private const val MILLIS_PER_SECOND = 1000.0
        private const val BACKGROUND_ASSERTION_NAME = "phovo-work-drain"

        /** Nominal denominator for the pre-report seed. See where it is used. */
        private const val SEED_PROGRESS_TOTAL = 1000L

    }
}

