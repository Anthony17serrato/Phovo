package com.serratocreations.phovo.core.common.di

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.common.performance.ProcessingPerformanceMode
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getAndroidDesktopIosModules(): Module = module {
    // TODO Read the mode from the user's persisted client config once it is a setting
    single<ProcessingPerformanceMode> { ProcessingPerformanceMode.DEFAULT }

    single<ProcessingCpuBudget> {
        ProcessingCpuBudget.forMode(
            mode = get(),
            availableProcessors = Runtime.getRuntime().availableProcessors()
        )
    }
}