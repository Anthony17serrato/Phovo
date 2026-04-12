package com.serratocreations.phovo.core.common.util

import com.serratocreations.phovo.core.logger.PhovoLogger
import kotlin.time.Clock

suspend fun <T> logTimeToComplete(apiTag: String, work: suspend () -> T): T {
    val startTime = Clock.System.now().toEpochMilliseconds()
    val result = work()
    val endTime = Clock.System.now().toEpochMilliseconds()
    PhovoLogger.withTag(apiTag).i {
        "logTimeToComplete completed in ${endTime-startTime} milliseconds"
    }
    return result
}