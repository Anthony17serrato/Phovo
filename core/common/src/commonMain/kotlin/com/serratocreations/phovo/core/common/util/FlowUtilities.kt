package com.serratocreations.phovo.core.common.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.produceIn

/**
 * Merges [flows] into a single flow that emits in strict round-robin order:
 * one item from each flow in turn, in the order the flows were given.
 *
 * When a flow completes, it is dropped from the rotation and the remaining
 * flows split its turns, so the last active flow eventually gets every turn.
 * The resulting flow completes once all [flows] have completed.
 *
 * Unlike [merge], ordering is by turn, not by arrival: a slow flow holds up
 * the others while it is that flow's turn. Unlike [zip], items are not paired,
 * and collection does not stop when the shortest flow ends.
 *
 * Each upstream is collected concurrently, but through a rendezvous channel,
 * so it can run at most one item ahead of the downstream.
 *
 * An exception from any upstream cancels all the others and is rethrown
 * downstream. Cancelling the downstream cancels every upstream.
 *
 * ```
 * interleave(flowOf(1, 3, 5, 7, 9), flowOf(2, 4)) // 1, 2, 3, 4, 5, 7, 9
 * ```
 *
 * @param flows the flows to interleave; an empty argument list yields an empty flow.
 * @return a cold flow that emits the items of [flows] in round-robin order.
 */
fun <T> interleave(vararg flows: Flow<T>): Flow<T> = flow {
    coroutineScope {
        val active = flows
            .map { it.buffer(Channel.RENDEZVOUS).produceIn(this) }
            .toMutableList()
        var i = 0
        while (active.isNotEmpty()) {
            val result = active[i].receiveCatching()
            if (result.isSuccess) {
                emit(result.getOrThrow())
                i = (i + 1) % active.size
            } else {
                result.exceptionOrNull()?.let { throw it }
                active.removeAt(i)
                if (i >= active.size) i = 0
            }
        }
    }
}