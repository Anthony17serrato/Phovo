package com.serratocreations.phovo.di

import com.serratocreations.phovo.AndroidDesktopIosAppInitializer
import com.serratocreations.phovo.IosAppInitializer
import com.serratocreations.phovo.core.common.di.APPLICATION_SCOPE
import com.serratocreations.phovo.data.permissions.LocalNetworkPermissionDelegate
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

actual fun getApplicationPlatformModulesFetcher(): ApplicationPlatformModuleFetcher =
    IosApplicationPlatformModuleFetcher()

class IosApplicationPlatformModuleFetcher: IosAndroidApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule())
        factory<AndroidDesktopIosAppInitializer> {
            IosAppInitializer(
                get(APPLICATION_SCOPE),
                get(),
                get()
            )
        }
    }
}

// called by IOS in iOSApp.swift
@OptIn(ExperimentalObjCName::class)
fun initIosApplication(@ObjCName("_") localNetworkPermissionDelegate: LocalNetworkPermissionDelegate) = initApplication(
    config = null,
    platformModule = module {
        single { localNetworkPermissionDelegate }
        includes(getApplicationPlatformModulesFetcher().getModule())
    }
)