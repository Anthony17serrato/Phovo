package com.serratocreations.phovo.core.common

expect fun getPlatform(): Platform

enum class Platform {
    Desktop,
    Wasm,
    Ios,
    Android
}