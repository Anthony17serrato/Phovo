package com.serratocreations.phovo.core.workmanager

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkStateTest {

    @Test
    fun `terminal states are finished`() {
        listOf(WorkState.SUCCEEDED, WorkState.FAILED, WorkState.CANCELLED).forEach { state ->
            assertTrue(state.isFinished, "$state should be finished")
        }
    }

    @Test
    fun `in flight states are not finished`() {
        listOf(WorkState.ENQUEUED, WorkState.RUNNING).forEach { state ->
            assertFalse(state.isFinished, "$state should not be finished")
        }
    }
}
