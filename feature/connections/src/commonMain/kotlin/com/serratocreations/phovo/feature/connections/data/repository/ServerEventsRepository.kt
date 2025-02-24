package com.serratocreations.phovo.feature.connections.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Singleton

@Singleton
class ServerEventsRepository {
    private val eventLogs = MutableStateFlow(emptyList<String>())

    fun addServerEventLog(eventLog: String) {
        eventLogs.update {
            (it + eventLog)
        }
    }

    fun serverEventLogsFlow() : Flow<List<String>> = eventLogs.asSharedFlow()
}