# :core:workmanager

A multiplatform background-work scheduler for Android and iOS, modelled on the androidx WorkManager
API. You write one worker in common code, enqueue it once, and each platform runs it with whatever
the OS gives it.

- **Android** delegates to `androidx.work`. Scheduling, persistence, constraint monitoring and
  backoff are WorkManager's own.
- **iOS** has no equivalent, so this module builds one on `BGTaskScheduler` over a work queue
  persisted in `NSUserDefaults`.

Work survives process death on both platforms.

## Writing a worker

Extend `PhovoWorker` and return a `WorkResult`.

```kotlin
class MediaSyncWorker(
    private val mediaRepository: LocalAndRemoteMediaRepository
) : PhovoWorker() {
    override suspend fun doWork(): WorkResult = try {
        mediaRepository.syncPendingMedia()
        WorkResult.Success
    } catch (e: IOException) {
        // Transient. Come back after the backoff delay.
        WorkResult.Retry
    }
}
```

`doWork` runs on a background dispatcher and is cancelled when the platform takes the execution
window back, so honour cancellation. Don't hold locks across a suspension point you can't be
interrupted in.

The three results:

| Result | What happens next |
| --- | --- |
| `Success` | One-time work is done. Periodic work is rescheduled for the next interval. |
| `Retry` | Re-runs after the backoff delay, with `runAttemptCount` incremented. |
| `Failure` | Terminal. Periodic work stops repeating, matching `androidx.work`. |

An exception thrown out of `doWork` is caught and treated as `Failure`. It won't crash the app.

## Registering a worker

A worker is named by a string id, not by its class. Work outlives the process that enqueued it, so
a record sitting on disk can only say *which* worker to build, and something has to know how to
build it. That's `WorkerRegistration`, contributed to Koin:

```kotlin
fun getPhotosDataModule(): Module = module {
    single { WorkerRegistration(MEDIA_SYNC_WORKER_ID) { MediaSyncWorker(get()) } }
}

const val MEDIA_SYNC_WORKER_ID = "media-sync"
```

Every registration in the graph is collected into a single `WorkerRegistry` by `getWorkManagerModule()`,
so you add one `single { ... }` and nothing else.

Ids are plain strings, so nothing stops two modules reaching for the same one. `WorkerRegistry`
rejects that at construction, which means at app startup, naming the offending id. Letting it pass
would have two workers silently sharing queue entries with the winner decided by whatever order
Koin collected the registrations in.

> **Worker ids are a persisted format.** Renaming one orphans any work already on disk. The orphaned
> record fails with a logged error rather than retrying forever, but the work it represented is lost.

## Enqueuing

Inject `PhovoWorkManager`. All work is unique work, keyed by a name you choose. There's no anonymous
enqueue, because anonymous work can't be reconciled after a restart.

### One-time

```kotlin
workManager.enqueueUniqueWork(
    uniqueWorkName = "media-sync-now",
    policy = ExistingWorkPolicy.REPLACE,
    request = OneTimeWorkRequest(
        workerId = MEDIA_SYNC_WORKER_ID,
        constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED),
        initialDelay = 30.seconds,
        backoffPolicy = BackoffPolicy.EXPONENTIAL,
        backoffDelay = 1.minutes,
        tags = setOf("sync")
    )
)
```

`ExistingWorkPolicy.REPLACE` cancels whatever is enqueued under that name and starts over. `KEEP`
leaves an unfinished existing job alone and drops the new request.

### Periodic

```kotlin
workManager.enqueueUniquePeriodicWork(
    uniqueWorkName = "media-sync-periodic",
    policy = ExistingPeriodicWorkPolicy.UPDATE,
    request = PeriodicWorkRequest(
        workerId = MEDIA_SYNC_WORKER_ID,
        repeatInterval = 6.hours,
        constraints = Constraints(
            requiredNetworkType = NetworkType.UNMETERED,
            requiresBatteryNotLow = true
        )
    )
)
```

`UPDATE` swaps in the new definition without resetting when the next run was already due, which
makes it safe to call on every app launch. That's the normal way to set up periodic work: enqueue it
from your initializer each time and let `UPDATE` deduplicate. `KEEP` drops the new request outright,
and `CANCEL_AND_REENQUEUE` restarts the schedule from now.

`repeatInterval` is clamped up to `PeriodicWorkRequest.MIN_PERIODIC_INTERVAL` (15 minutes). Treat it
as a floor rather than a schedule. Android won't run periodic work more often, and iOS decides for
itself when to hand out background time, which can be hours later or not at all.

`flexInterval` maps to WorkManager's flex window on Android and is ignored on iOS.

### Cancelling

```kotlin
workManager.cancelUniqueWork("media-sync-periodic")
workManager.cancelAllWorkByTag("sync")
```

A run already in flight is cancelled. On iOS, a cancel that lands while a worker is running wins:
whatever the worker returns afterwards is discarded.

### Expedited work

```kotlin
workManager.enqueueUniqueWork(
    uniqueWorkName = "media-sync-now",
    policy = ExistingWorkPolicy.REPLACE,
    request = OneTimeWorkRequest(
        workerId = MEDIA_SYNC_WORKER_ID,
        expedited = true,
        constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
    )
)
```

The promise both platforms keep is narrow and worth reading carefully: **expedited work starts
promptly while the app is in the foreground.** For an app the user has left, neither platform
promises anything.

What each one actually does:

| | Behaviour |
| --- | --- |
| Android, API 31+ | A real JobScheduler expedited job. Runs within moments, in the foreground or the background, until the app's expedited quota is spent. |
| Android, API ≤ 30 | Enqueued normally. Expedited work on those versions means a foreground service with a user-visible notification, which is the wrong trade for a silent sync. |
| iOS | Jumps the queue ahead of non-expedited work, and its `BGProcessingTaskRequest` is submitted with no `earliestBeginDate`. A suspended app still cannot be woken on demand. |

Two rules, enforced in `OneTimeWorkRequest`'s `init` block so you find out at the call site rather
than from an Android-only crash:

- Expedited work cannot have an `initialDelay`.
- Expedited work cannot ask for `requiresCharging` or `requiresBatteryNotLow`. Network constraints
  are fine.

Both come from `androidx.work`, which rejects the same combinations when it builds a request
(`"Expedited jobs cannot be delayed"`, `"Expedited jobs only support network and storage
constraints"`).

There is no expedited periodic work. `androidx.work` refuses it outright, so the option is not
offered here.

On the quota: Android uses `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST`, so exhausting the
quota makes the work run late instead of dropping it.

### Long-running work

For work that needs longer than the platform's ordinary execution window, such as backing up a
whole photo library:

```kotlin
workManager.enqueueUniqueWork(
    uniqueWorkName = "media-backup",
    policy = ExistingWorkPolicy.KEEP,
    request = OneTimeWorkRequest(
        workerId = MEDIA_SYNC_WORKER_ID,
        longRunning = LongRunningInfo(
            title = "Backing up photos",
            subtitle = "Preparing"
        ),
        constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED)
    )
)
```

Report progress as you go:

```kotlin
class MediaSyncWorker(private val repo: LocalAndRemoteMediaRepository) : PhovoWorker() {
    override suspend fun doWork(): WorkResult {
        val pending = repo.pendingMedia()
        pending.forEachIndexed { index, item ->
            repo.upload(item)
            setProgress(completed = index + 1L, total = pending.size.toLong())
        }
        return WorkResult.Success
    }
}
```

Both platforms grant a long background window only in exchange for showing the user what the app is
doing, which is why `LongRunningInfo` carries display text.

| | Behaviour | Ceiling |
| --- | --- | --- |
| Android | `setForeground` promotes the run to a `dataSync` foreground service with an ongoing notification. | The 10-minute JobScheduler deadline stops applying. Android 15+ budgets `dataSync` services to roughly 6 hours per day (`STOP_REASON_FOREGROUND_SERVICE_TIMEOUT`). |
| iOS 26+ | A `BGContinuedProcessingTaskRequest` with system progress UI. Starts immediately and keeps running after the user leaves the app. | System-managed. Tasks that look stalled are forcibly expired. |
| iOS below 26 | Falls back to the ordinary `BGProcessingTask` queue. | Whatever the OS grants, typically a few minutes. |

`setProgress` drives the Android notification and the iOS `NSProgress`. On iOS it is not optional in
practice: the scheduler may kill a continued processing task that appears to have stalled.

Rules, enforced in `OneTimeWorkRequest`'s `init`:

- It cannot have an `initialDelay`, because iOS ignores `earliestBeginDate` on a continued
  processing task and honouring the delay on Android alone would make the platforms disagree
  silently.
- Unlike expedited work, it may use power constraints.

### Combining it with `expedited`

Allowed, because the two ask for different things. `expedited` is about **when** the work starts,
`longRunning` about **how long** it may run once started.

| | With both set |
| --- | --- |
| Android | Schedules an expedited job that then promotes itself to a foreground service. Both apply. |
| iOS 26+ | Ignores `expedited`. A continued processing task already starts immediately and outranks the ordinary queue. |
| iOS < 26 | Honours both. Long-running work falls back to the ordinary queue, where expedited still moves it to the front. |

Legal does not mean advisable on Android. Expedited quota is finite and meant for short urgent
work, so spending it on something about to become a foreground service anyway is usually the wrong
trade. Work enqueued with no delay while the app is foregrounded starts within seconds regardless.

There is no long-running periodic work. `BGContinuedProcessingTaskRequest` must be submitted on
behalf of a foregrounded app, so this serves "the user tapped Back Up Now" rather than anything on a
schedule.

#### What this needed outside the module

On Android, the module ships its first `AndroidManifest.xml`: `FOREGROUND_SERVICE_DATA_SYNC` and
`POST_NOTIFICATIONS`, plus a `tools:node="merge"` override adding `foregroundServiceType="dataSync"`
to WorkManager's own `SystemForegroundService` declaration, which API 34+ rejects without it. **The
app must request `POST_NOTIFICATIONS` at run time**; the service runs either way, but the
notification stays invisible until it is granted.

On iOS, `Info.plist` gained a third permitted identifier. Apple requires continued-processing
identifiers to be a wildcard whose prefix starts with the bundle id, so it is shaped differently
from the other two:

```
com.serratocreations.phovo.Phovo.work.continued.*
```

Each submission uses a concrete identifier beneath it, derived from the unique work name so the
launch handler can find its record without keeping any mapping around.

The wildcard belongs in `Info.plist` and nowhere else. It grants permission to use identifiers
beneath it and is not itself registerable: passing it to `registerForTaskWithIdentifier` is rejected
with *"is not advertised in the application's Info.plist"*. Handlers are registered against the
concrete identifier, lazily, immediately before that request is submitted. That ordering is not
optional, because `submitTaskRequest` raises an Objective-C `NSInternalInconsistencyException` when
no handler exists, and a raised ObjC exception cannot be caught from Kotlin, so it terminates the
app. It also explains why Apple exempts these registrations from the "before the app finishes
launching" rule that every other `BGTask` follows.

Each identifier is registered once per process, tracked in `registeredContinuedIds`. Registering the
same identifier twice is documented to kill the app, and identifiers are deterministic, so
re-enqueuing the same unique work would otherwise do exactly that.

## Observing

```kotlin
workManager.getWorkInfoFlow("media-sync-periodic")
    .collect { info ->
        when (info?.state) {
            WorkState.RUNNING -> showSpinner()
            WorkState.FAILED -> showError(attempts = info.runAttemptCount)
            else -> Unit
        }
    }
```

The flow emits `null` when no work exists under that name. `WorkState.isFinished` covers `SUCCEEDED`,
`FAILED` and `CANCELLED`; periodic work never reaches a finished state while it's still scheduled.
`suspend fun getWorkInfo(name)` gives you the same snapshot once.

## Constraints

```kotlin
Constraints(
    requiredNetworkType = NetworkType.UNMETERED, // or NONE, CONNECTED
    requiresCharging = false,
    requiresBatteryNotLow = false
)
```

On Android these map straight onto `androidx.work.Constraints`.

On iOS, `IosConstraintMonitor` evaluates them itself before running anything. Network comes from
`NWPathMonitor`, where `UNMETERED` means a path that is satisfied and neither expensive (cellular or
hotspot) nor constrained (Low Data Mode). Charging and battery level come from `UIDevice`, and a
battery level of `-1`, which is what the simulator reports, counts as "not low" so constrained
workers aren't blocked forever during development.

## Backoff

`BackoffPolicy.delayForAttempt` lives in `commonMain` and is the single definition both platforms
agree on. Exponential doubles each attempt, linear scales by attempt count, and the result is clamped
to between 10 seconds and 5 hours, the same bounds `androidx.work` uses. Android computes this
internally; iOS calls the shared function directly.

## Platform notes

### Android

Every request is scheduled against one `CoroutineWorker`, `PhovoDelegatingWorker`, which reads the
worker id out of its input data and resolves the real worker from `WorkerRegistry` at run time. The
registry is pulled from the global Koin context rather than through a custom `WorkerFactory`.
`PhovoApplication` starts Koin at process start, so the graph is always up before WorkManager runs
anything, and this keeps the module free of a `Configuration.Provider` and of on-demand
initialization.

The module does ship an `AndroidManifest.xml`, but only for long-running work: the two foreground
service permissions, `POST_NOTIFICATIONS`, and the `foregroundServiceType` merge onto WorkManager's
`SystemForegroundService`. Nothing on the ordinary or expedited paths needs it.

Tags are prefixed with `phovo.work.tag.` internally so `cancelAllWorkByTag` can't collide with
WorkManager's own tags. The prefix is stripped back off in `WorkInfo`.

### iOS

Four pieces do the work:

1. `WorkRecordStore` keeps the queue as a JSON array in `NSUserDefaults`. It's unique work only, one
   record per name, so rewriting the array on each change costs less than the bookkeeping a real
   database would need, and it keeps Room and KSP out of this module. A record left in `RUNNING` by a
   process that died mid-run is put back to `ENQUEUED` on load.
2. `BGTaskScheduler` asks the OS for time. iOS allows one pending request per identifier, so the
   two ordinary identifiers are "drain whatever is due" handlers rather than one task per job, and
   a fresh request is submitted after every run.
3. A foreground drain on `UIApplicationDidBecomeActive`, because iOS grants background time
   unpredictably and close to never in the simulator. Without it the queue would look permanently
   stuck while you're developing.
4. `BGContinuedProcessingTask` on iOS 26+, for records enqueued with a `LongRunningInfo`. These get
   their own per-record request under a wildcard identifier, and the ordinary drain leaves them
   alone so the long window is the thing that runs them rather than the thirty-second one.

Every foreground drain holds a UIKit background task assertion
(`beginBackgroundTaskWithName`). iOS suspends a process a few seconds after the user leaves the
app, which would freeze a worker mid-run and throw away its progress. The assertion buys roughly
thirty seconds past that point, usually enough to finish the item in flight and checkpoint it. When
it expires, the drain is cancelled, and the existing `CancellationException` path puts the record
back to `ENQUEUED` without burning a retry attempt, exactly as a revoked `BGTask` window does. The
`BGTask` handlers drain without an assertion, since the task itself is already the execution window.

`BGContinuedProcessingTask` is iOS 26+ and the deployment target is 15.3. Kotlin/Native does not
model `@available`, so availability is checked at run time with
`NSProcessInfo.isOperatingSystemAtLeastVersion` and anything older falls through to the
`BGProcessingTask` queue. Everything else on iOS is short: `BGAppRefreshTask` is about thirty
seconds and `BGProcessingTask` a few undocumented minutes that want an idle device. Write workers
that checkpoint per item and resume, rather than workers that need one long window.

Pending continued-processing requests are cancelled by name, never through
`cancelAllTaskRequests`, which would take them down alongside the two ordinary identifiers.

`IosPhovoWorkManager.registerBackgroundTasks()` must run before the app finishes launching, or
`BGTaskScheduler` raises. It's called from `IosAppInitializer.initialize()`, which runs inside
`iOSApp.swift`'s `init()`. Continued processing handlers are the one exception Apple carves out,
and they register in the same call anyway.

All three identifiers have to be listed in the app's `Info.plist` under
`BGTaskSchedulerPermittedIdentifiers`, alongside `UIBackgroundModes` of `fetch` and `processing`:

- `com.serratocreations.phovo.work.processing`
- `com.serratocreations.phovo.work.refresh`
- `com.serratocreations.phovo.Phovo.work.continued.*` (long-running work, iOS 26+)

If registration fails, you get an error in the log and the foreground drain still runs everything.

Workers run on the module's own `SupervisorJob` scope, not the app scope. The app scope's exception
handler rethrows to crash the process, and a misbehaving worker should fail its own work instead.

## Testing

`commonTest` covers the platform-free logic: backoff maths, `WorkState.isFinished`,
`WorkerRegistry` resolution, and the `OneTimeWorkRequest` validation rules for expedited and
long-running work.

```bash
./gradlew :core:workmanager:iosSimulatorArm64Test :core:workmanager:testAndroidHostTest
```

To watch work actually run on a device or simulator, register a worker that just logs, enqueue it,
and check the Kermit output. On Android, `adb shell dumpsys jobscheduler | grep phovo` shows what
WorkManager scheduled. On iOS, the simulator won't fire a `BGTask` on its own; pause in the debugger
after launch and force it:

```
e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.serratocreations.phovo.work.processing"]
```

## Not supported

- **Input and output `Data`.** Workers take their dependencies from DI and their parameters from the
  unique work name. There's no key-value payload.
- **Chaining.** No `beginWith(...).then(...)`. There's no engine for it on iOS.
- **Long-running periodic work.** See above; iOS requires a foregrounded app to start one.
- **Desktop.** The module targets Android and iOS only, via the
  [`phovo.kmp.android.ios.library`](../../build-logic) convention plugin. Anything in `commonMain`
  here means Android plus iOS, so there is no `commonIosAndroid` source set.
