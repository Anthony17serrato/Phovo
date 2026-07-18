package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.data.server.di.getServerDataModule
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun getAndroidDesktopIosModules(): Module

fun getConnectionsFeatureModule(): Module = module {
    includes(getServerDataModule(), getAndroidDesktopIosModules())

}