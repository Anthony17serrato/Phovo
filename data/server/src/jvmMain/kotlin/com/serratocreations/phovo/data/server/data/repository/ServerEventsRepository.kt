package com.serratocreations.phovo.data.server.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update

class ServerEventsRepository {
    private val eventLogs = MutableStateFlow(emptyList<String>())

    fun addServerEventLog(eventLog: String) {
        eventLogs.update {
            (it + eventLog)
        }
    }

    fun serverEventLogsFlow() : Flow<List<String>> = eventLogs.asSharedFlow()
}