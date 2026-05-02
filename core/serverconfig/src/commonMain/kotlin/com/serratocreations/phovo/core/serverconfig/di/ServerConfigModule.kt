package com.serratocreations.phovo.core.serverconfig.di

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosWasmModules(): Module

fun getServerConfigModule(): Module = module {
    includes(getAndroidDesktopIosWasmModules())

}