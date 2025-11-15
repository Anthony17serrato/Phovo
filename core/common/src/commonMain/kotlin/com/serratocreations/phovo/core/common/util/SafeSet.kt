package com.serratocreations.phovo.core.common.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SafeSet<T> {
    private val mutex = Mutex()
    private val data = mutableSetOf<T>()

    suspend fun add(value: T) = mutex.withLock {
        data.add(value)
    }

    suspend fun remove(value: T) = mutex.withLock {
        data.remove(value)
    }

    suspend fun contains(value: T): Boolean = mutex.withLock {
        data.contains(value)
    }

    suspend fun snapshot(): Set<T> = mutex.withLock {
        data.toSet()
    }
}