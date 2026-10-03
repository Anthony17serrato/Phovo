package com.serratocreations.phovo.core.common.di

import com.serratocreations.phovo.core.common.performance.ProcessingCpuBudget
import com.serratocreations.phovo.core.common.performance.ProcessingPerformanceMode
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal expect fun getAndroidIosModules(): Module
internal actual fun getAndroidDesktopIosModules(): Module = module {
    includes(getAndroidIosModules())

    single<ProcessingCpuBudget> {
        ProcessingCpuBudget.forMode(
            mode = ProcessingPerformanceMode.High,
            availableProcessors = get(AVAILABLE_PROCESSOR_CORES)
        )
    }
}

val AVAILABLE_PROCESSOR_CORES = named("Available_Processor_Cores")