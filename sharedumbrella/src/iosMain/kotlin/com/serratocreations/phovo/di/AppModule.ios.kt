package com.serratocreations.phovo.di

import com.serratocreations.phovo.AndroidDesktopIosWasmAppInitializer
import com.serratocreations.phovo.IosAppInitializer
import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getApplicationPlatformModulesFetcher(): ApplicationPlatformModuleFetcher =
    IosApplicationPlatformModuleFetcher()

class IosApplicationPlatformModuleFetcher: ApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule())
        factory<AndroidDesktopIosWasmAppInitializer> {
            IosAppInitializer(
                get(APPLICATION_SCOPE),
                get(),
                get()
            )
        }
    }
}