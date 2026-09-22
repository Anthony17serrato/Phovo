package com.serratocreations.phovo.core.workmanager

import androidx.work.BackoffPolicy as AndroidxBackoffPolicy
import androidx.work.Constraints as AndroidxConstraints
import androidx.work.ExistingPeriodicWorkPolicy as AndroidxExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy as AndroidxExistingWorkPolicy
import androidx.work.NetworkType as AndroidxNetworkType
import androidx.work.WorkInfo as AndroidxWorkInfo

/** Tag prefix used so [PhovoWorkManager.cancelAllWorkByTag] cannot collide with WorkManager internals. */
internal const val PHOVO_TAG_PREFIX = "phovo.work.tag."

internal fun String.toAndroidxTag(): String = PHOVO_TAG_PREFIX + this

internal fun Set<String>.toAndroidxTags(): Set<String> = mapTo(mutableSetOf()) { it.toAndroidxTag() }

internal fun Set<String>.fromAndroidxTags(): Set<String> =
    filter { it.startsWith(PHOVO_TAG_PREFIX) }.mapTo(mutableSetOf()) { it.removePrefix(PHOVO_TAG_PREFIX) }

internal fun NetworkType.toAndroidx(): AndroidxNetworkType = when (this) {
    NetworkType.NONE -> AndroidxNetworkType.NOT_REQUIRED
    NetworkType.CONNECTED -> AndroidxNetworkType.CONNECTED
    NetworkType.UNMETERED -> AndroidxNetworkType.UNMETERED
}

internal fun Constraints.toAndroidx(): AndroidxConstraints = AndroidxConstraints.Builder()
    .setRequiredNetworkType(requiredNetworkType.toAndroidx())
    .setRequiresCharging(requiresCharging)
    .setRequiresBatteryNotLow(requiresBatteryNotLow)
    .build()

internal fun BackoffPolicy.toAndroidx(): AndroidxBackoffPolicy = when (this) {
    BackoffPolicy.EXPONENTIAL -> AndroidxBackoffPolicy.EXPONENTIAL
    BackoffPolicy.LINEAR -> AndroidxBackoffPolicy.LINEAR
}

internal fun ExistingWorkPolicy.toAndroidx(): AndroidxExistingWorkPolicy = when (this) {
    ExistingWorkPolicy.REPLACE -> AndroidxExistingWorkPolicy.REPLACE
    ExistingWorkPolicy.KEEP -> AndroidxExistingWorkPolicy.KEEP
}

internal fun ExistingPeriodicWorkPolicy.toAndroidx(): AndroidxExistingPeriodicWorkPolicy = when (this) {
    ExistingPeriodicWorkPolicy.UPDATE -> AndroidxExistingPeriodicWorkPolicy.UPDATE
    ExistingPeriodicWorkPolicy.KEEP -> AndroidxExistingPeriodicWorkPolicy.KEEP
    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE -> AndroidxExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
}

internal fun AndroidxWorkInfo.State.toPhovo(): WorkState = when (this) {
    AndroidxWorkInfo.State.ENQUEUED -> WorkState.ENQUEUED
    // BLOCKED only happens in chains, which this API does not expose, but map it rather than throw.
    AndroidxWorkInfo.State.BLOCKED -> WorkState.ENQUEUED
    AndroidxWorkInfo.State.RUNNING -> WorkState.RUNNING
    AndroidxWorkInfo.State.SUCCEEDED -> WorkState.SUCCEEDED
    AndroidxWorkInfo.State.FAILED -> WorkState.FAILED
    AndroidxWorkInfo.State.CANCELLED -> WorkState.CANCELLED
}

internal fun AndroidxWorkInfo.toPhovo(uniqueWorkName: String): WorkInfo = WorkInfo(
    uniqueWorkName = uniqueWorkName,
    state = state.toPhovo(),
    runAttemptCount = runAttemptCount,
    tags = tags.fromAndroidxTags()
)
