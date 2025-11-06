package com.serratocreations.phovo.core.common.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual fun Long.localize(): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
    }
    return formatter.stringFromNumber(NSNumber(longLong = this)) ?: this.toString()
}