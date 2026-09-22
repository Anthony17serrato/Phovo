package com.serratocreations.phovo.core.workmanager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The expedited rules come from androidx.work, which rejects these combinations when it builds a
 * request. Catching them in the shared constructor means an iOS-only developer finds out at the
 * call site instead of when the Android build runs.
 */
class OneTimeWorkRequestTest {

    @Test
    fun `expedited work rejects an initial delay`() {
        val error = assertFailsWith<IllegalArgumentException> {
            OneTimeWorkRequest(
                workerId = "media-sync",
                expedited = true,
                initialDelay = 5.minutes
            )
        }
        assertTrue("cannot be delayed" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `expedited work rejects a charging constraint`() {
        assertFailsWith<IllegalArgumentException> {
            OneTimeWorkRequest(
                workerId = "media-sync",
                expedited = true,
                constraints = Constraints(requiresCharging = true)
            )
        }
    }

    @Test
    fun `expedited work rejects a battery not low constraint`() {
        assertFailsWith<IllegalArgumentException> {
            OneTimeWorkRequest(
                workerId = "media-sync",
                expedited = true,
                constraints = Constraints(requiresBatteryNotLow = true)
            )
        }
    }

    @Test
    fun `expedited work allows a network constraint`() {
        val request = OneTimeWorkRequest(
            workerId = "media-sync",
            expedited = true,
            constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED)
        )
        assertTrue(request.expedited)
        assertEquals(NetworkType.UNMETERED, request.constraints.requiredNetworkType)
    }

    @Test
    fun `non expedited work may be delayed and require power`() {
        val request = OneTimeWorkRequest(
            workerId = "media-sync",
            initialDelay = 5.minutes,
            constraints = Constraints(requiresCharging = true, requiresBatteryNotLow = true)
        )
        assertEquals(5.minutes, request.initialDelay)
    }

    @Test
    fun `long-running work cannot also be expedited`() {
        val error = assertFailsWith<IllegalArgumentException> {
            OneTimeWorkRequest(
                workerId = "media-sync",
                expedited = true,
                longRunning = LongRunningInfo("Backing up photos", "0 of 340")
            )
        }
        assertTrue("should not also be" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `long-running work rejects an initial delay`() {
        // iOS ignores earliestBeginDate on a continued processing task, so honouring the delay on
        // Android only would make the two platforms silently disagree.
        assertFailsWith<IllegalArgumentException> {
            OneTimeWorkRequest(
                workerId = "media-sync",
                initialDelay = 5.minutes,
                longRunning = LongRunningInfo("Backing up photos", "0 of 340")
            )
        }
    }

    @Test
    fun `long-running work may require charging`() {
        // Only the expedited path carries androidx's network-and-storage-only restriction.
        val request = OneTimeWorkRequest(
            workerId = "media-sync",
            constraints = Constraints(requiresCharging = true),
            longRunning = LongRunningInfo("Backing up photos", "0 of 340")
        )
        assertEquals("Backing up photos", request.longRunning?.title)
    }

    @Test
    fun `long-running work needs a title the user can read`() {
        assertFailsWith<IllegalArgumentException> {
            LongRunningInfo(title = "   ", subtitle = "0 of 340")
        }
    }

    @Test
    fun `work is not long-running by default`() {
        assertEquals(null, OneTimeWorkRequest(workerId = "media-sync").longRunning)
    }

    @Test
    fun `work is not expedited by default`() {
        val request = OneTimeWorkRequest(workerId = "media-sync")
        assertTrue(!request.expedited)
        assertEquals(Duration.ZERO, request.initialDelay)
    }
}
