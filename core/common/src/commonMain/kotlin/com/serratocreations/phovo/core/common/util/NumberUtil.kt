package com.serratocreations.phovo.core.common.util

expect fun Long.localize(): String

fun Int.localize() = this.toLong().localize()