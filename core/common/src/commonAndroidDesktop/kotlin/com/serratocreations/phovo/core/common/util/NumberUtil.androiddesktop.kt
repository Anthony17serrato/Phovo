package com.serratocreations.phovo.core.common.util

import java.text.NumberFormat
import java.util.Locale

actual fun Long.localize(): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.getDefault())
    return formatter.format(this)
}