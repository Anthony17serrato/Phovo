package com.serratocreations.phovo.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual fun getAndroidIosModules(): Module = module {
    factory<Int>(AVAILABLE_PROCESSOR_CORES) {
        Platform.getAvailableProcessors()
    }
}