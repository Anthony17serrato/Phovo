package com.serratocreations.phovo.di

import com.serratocreations.phovo.AndroidDesktopIosWasmAppInitializer
import com.serratocreations.phovo.WasmAppInitializer
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getApplicationPlatformModulesFetcher(): ApplicationPlatformModuleFetcher =
    WasmApplicationPlatformModuleFetcher()

class WasmApplicationPlatformModuleFetcher: ApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule())
        factory<AndroidDesktopIosWasmAppInitializer> { WasmAppInitializer() }
    }
}