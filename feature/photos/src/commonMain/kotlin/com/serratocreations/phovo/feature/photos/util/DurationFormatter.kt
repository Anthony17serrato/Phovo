package com.serratocreations.phovo.feature.photos.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Duration.toFormattedDurationString(): String {
    val totalSeconds = inWholeSeconds
    val hours = totalSeconds.seconds.inWholeHours
    val minutes = (totalSeconds % 1.hours.inWholeSeconds).seconds.inWholeMinutes
    val seconds = totalSeconds % 1.minutes.inWholeSeconds

    return if (hours > 0)
        "${pad(hours, truncate0 = true)}:${pad(minutes)}:${pad(seconds)}"
    else
        "${pad(minutes, truncate0 = true)}:${pad(seconds)}"
}

private fun pad(value: Long, truncate0: Boolean = false): String =
    if (value < 10 && truncate0.not()) "0$value" else value.toString()