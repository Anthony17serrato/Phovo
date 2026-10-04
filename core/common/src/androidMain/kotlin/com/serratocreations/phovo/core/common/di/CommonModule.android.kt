package com.serratocreations.phovo.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidIosModules(): Module = module {
    factory<Int>(AVAILABLE_PROCESSOR_CORES) {
        Runtime.getRuntime().availableProcessors()
    }
}