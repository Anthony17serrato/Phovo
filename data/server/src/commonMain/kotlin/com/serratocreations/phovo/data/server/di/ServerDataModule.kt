package com.serratocreations.phovo.data.server.di

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosWasmModules(): Module

fun getServerDataModule(): Module = module {
    includes(getAndroidDesktopIosWasmModules())

}