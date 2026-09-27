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

`ExistingWorkPolicy.REPLACE` cancels whatever is enqueued under that name and starts over, stopping
the worker if one is already running. That holds on both platforms. `KEEP` leaves an unfinished
existing job alone and drops the new request.

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

Cancelling stops a worker that's already running, on both platforms. On iOS it also ends the
work's continued processing task, the way cancelling on Android stops its foreground service, and
anything the stopped worker returns afterwards is discarded.

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
| iOS 26+ | Runs on the ordinary drain with no system UI while the app is open. As the user leaves, a `BGContinuedProcessingTaskRequest` carries the run into the background with a Live Activity. See *How it behaves on iOS* below. | System-managed. Tasks that stop moving are expired. |
| iOS below 26 | Falls back to the ordinary `BGProcessingTask` queue. | Whatever the OS grants, typically a few minutes. |

`setProgress` drives the Android notification and the iOS `NSProgress`.

On iOS, the module seeds `NSProgress` with a determinate `1 / 1000` the moment a continued
processing task starts, before the worker reports anything. With no progress at all, a backgrounded
task was killed within seconds on a test device. The seed claims one unit rather than zero because a
`completedUnitCount` of zero renders as an indefinite bar, even though `NSProgress` itself only calls
the both-zero case indeterminate. The seed covers the window before the first real `setProgress`,
which for a worker that scans a library before it knows any totals can be a long way in.

**The seed is a floor, not a substitute for reporting.** Apple's position ([WWDC25 session
227](https://developer.apple.com/videos/play/wwdc2025/227/)) is that tasks not reporting progress get
expired so the system can reclaim resources, and that work progressing slower than expected causes
the system to ask the person whether to continue. Report real numbers as you get them.

Device measurements, for calibration rather than as a contract:

| Charging | Progress reported | Outcome |
| --- | --- | --- |
| Yes | none | expired within seconds of backgrounding |
| Yes | one initial value, nothing after | survived for as long as it was left running |
| No | one initial value, nothing after | expired after roughly a minute |
| No | the same value, re-set every second | expired after roughly a minute |
| No | rising every second | survived over an hour, until the test stopped it |
| No | rising, then falling back, then rising again | survived |

The practical reading is that **movement** keeps a task alive. Re-setting an unchanged value counts
for nothing, while a value that falls back does count, which is what you get when a scan grows the
denominator faster than work completes. So report the real `completed / total` as often as either
changes, and don't clamp it to stop the bar going backwards.

Apple documents none of this. The class reference says only "run-time conditions" and "resource
constraints", and power state is never named as a trigger. Treat the table as evidence that the
policy is adaptive and unpublished, and do not build anything that depends on the specific numbers.

Note also that an expired continued processing task does not resume. Per Apple DTS on [this
thread](https://developer.apple.com/forums/thread/806668), the API extends foreground time rather
than granting background time, so the opportunity is lost until the app is foregrounded again. This
module survives that because expiry stops the run and returns the record to its own queue, without
burning a retry attempt, the same as Android stopping a foreground service.

### How it behaves on iOS

The continued processing task is treated as an implementation detail, the iOS stand-in for
Android's foreground service. It keeps the process alive and gives the current run a progress bar;
it has no behaviour of its own, and the work itself always runs on the module's ordinary drain.
Everything below exists to make iOS behave like Android.

| When | What happens |
| --- | --- |
| Work is enqueued with the app open | It runs on the drain. No Live Activity is shown while the app is open. |
| The user leaves (`willResignActive`) | A continued processing request is submitted for long-running work that is running or ready to run. The launch handler attaches the task to the run already in progress; nothing restarts. |
| The user comes back (`didBecomeActive`) | The task is ended, which dismisses the Live Activity. The run is the module's own coroutine rather than the task's, so it keeps going. Leaving again starts a fresh task for it. |
| The worker returns | The task is completed with `success = (result is WorkResult.Success)`. On success the bar is filled first. `Failure` and `Retry` complete it as unsuccessful. |
| `REPLACE`, or `CANCEL_AND_REENQUEUE` for periodic work | The old run is stopped and the replacement runs. The task belongs to the unique work name rather than to a run, so it stays up and passes to the replacement. |
| Cancel | The run is stopped and the task is ended. |
| The system expires the task | The run is stopped and the record goes back to the queue without burning an attempt. |
| The UIKit background assertion runs out, about 30 seconds after leaving | Ignored while a continued task is active, so it can't cut the run off. |

A few things that are less obvious:

- Returning to the app ends the task **as unsuccessful**. Ending it as successful was measured to
  leave the next request accepted but never launched.
- Every submission gets a fresh identifier, as Apple's long-running article asks. Even so, on a
  device roughly one request in nine was accepted and never launched. Coming back to the app
  withdraws any request in that state so it can't get stuck, and the next time the user leaves
  it's submitted again.
- `willResignActive` also fires when the user pulls down Control Center or Notification Center, or
  takes a call, without leaving the app. The Live Activity can appear briefly in those cases, and
  it's dismissed as soon as the app is active again.
- Apple's guidance is that continued processing should start from a user action. Here it starts
  for work the app already had running when the user leaves, which is exactly the situation the API
  exists for, but it's the argument you'd be making if App Review ever asks.

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
| iOS | Honours both. Expedited moves the work to the front of the drain, and on iOS 26+ a continued processing task carries it into the background when the user leaves. |

Legal does not mean advisable on Android. Expedited quota is finite and meant for short urgent
work, so spending it on something about to become a foreground service anyway is usually the wrong
trade. Work enqueued with no delay while the app is foregrounded starts within seconds regardless.

There is no long-running periodic work. `BGContinuedProcessingTaskRequest` can only be submitted on
behalf of a foregrounded app, so it can't be started on a schedule while the app is closed.

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

Each submission uses a fresh concrete identifier beneath it: the sanitised unique work name plus a
random suffix. The module keeps the mapping from identifier back to work name while the request is
outstanding.

The wildcard belongs in `Info.plist` and nowhere else. It grants permission to use identifiers
beneath it and is not itself registerable: passing it to `registerForTaskWithIdentifier` is rejected
with *"is not advertised in the application's Info.plist"*. Handlers are registered against the
concrete identifier, lazily, immediately before that request is submitted. That ordering is not
optional, because `submitTaskRequest` raises an Objective-C `NSInternalInconsistencyException` when
no handler exists, and a raised ObjC exception cannot be caught from Kotlin, so it terminates the
app. It also explains why Apple exempts these registrations from the "before the app finishes
launching" rule that every other `BGTask` follows.

Registering the same identifier twice is documented to kill the app. Fresh identifiers make that
unlikely, and `registeredContinuedIds` rules it out.

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
2. A single **drain** runs whatever is due, one record at a time. It's requested on every enqueue,
   cancel and return to the app, and requests are coalesced: while a drain is running, a new
   request only marks that one more pass is needed, so there's never more than one drain. iOS
   grants background time unpredictably and close to never in the simulator, so without the drain
   on app-active the queue would look permanently stuck while you're developing.
3. `BGTaskScheduler` asks the OS for time for queued work. iOS allows one pending request per
   identifier, so the two ordinary identifiers are "drain whatever is due" handlers rather than one
   task per job, and the requests are rebuilt whenever a drain finishes.
4. `BGContinuedProcessingTask` on iOS 26+, standing in for Android's foreground service. See *How
   it behaves on iOS* under long-running work.

Each worker runs in its own child job, so one record can be stopped (by `REPLACE`, a cancel, or its
continued task expiring) without taking down the rest of the drain. Every request also carries a
generation, which is how a run that was replaced mid-flight knows not to write its result over the
new request.

The drain holds a UIKit background task assertion (`beginBackgroundTaskWithName`), taken only once
it holds the lock, so it only ever covers real work. iOS suspends a process a few seconds after the
user leaves the app, which would freeze a worker mid-run and throw away its progress. The assertion
buys roughly thirty seconds past that point, usually enough to finish the item in flight and
checkpoint it. When it expires with no continued task active, the drain is cancelled and the record
goes back to `ENQUEUED` without burning a retry attempt, as a revoked `BGTask` window does. With a
continued task active, the expiry is ignored, since that task is what keeps the process alive. The
`BGTask` handlers drain without an assertion, because the task itself is already the execution
window.

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
which is what lets them be registered as the user leaves, just before each request is submitted.

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
