package com.serratocreations.phovo.di

import com.serratocreations.phovo.data.permissions.di.getPermissionsDataModule
import org.koin.core.module.Module
import org.koin.dsl.module

abstract class IosAndroidApplicationPlatformModuleFetcher: ApplicationPlatformModuleFetcher() {
    override fun getModule(): Module = module {
        includes(super.getModule(), getPermissionsDataModule())
    }
}