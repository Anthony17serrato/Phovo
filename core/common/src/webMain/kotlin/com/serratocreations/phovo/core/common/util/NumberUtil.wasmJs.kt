package com.serratocreations.phovo.core.common.util

//import kotlinx.browser.window

actual fun Long.localize(): String {
    return TODO("TODO update this when web development resumes")
//    val locale = window.navigator.language
//    return localize(locale, this)
}

//@OptIn(ExperimentalWasmJsInterop::class)
//private fun localize(locale: String, valueToLocalize: Long): String =
//    js("""
//    new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(valueToLocalize)
//    """)