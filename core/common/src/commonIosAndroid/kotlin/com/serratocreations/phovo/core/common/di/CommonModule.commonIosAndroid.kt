package com.serratocreations.phovo.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidIosModules(): Module
internal actual fun getAndroidDesktopIosModules(): Module = module {
    includes(getAndroidIosModules())
}