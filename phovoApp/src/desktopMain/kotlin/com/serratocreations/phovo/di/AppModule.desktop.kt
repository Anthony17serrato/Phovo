package com.serratocreations.phovo.di

import com.serratocreations.phovo.AndroidDesktopIosWasmAppInitializer
import com.serratocreations.phovo.DesktopAppInitializer
import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getApplicationPlatformModulesFetcher(): ApplicationPlatformModuleFetcher =
    DesktopApplicationPlatformModuleFetcher()

class DesktopApplicationPlatformModuleFetcher: ApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule())
        factory<AndroidDesktopIosWasmAppInitializer> {
            DesktopAppInitializer(
                get(APPLICATION_SCOPE),
                get(),
                get(),
                get()
            )
        }
    }
}