package com.serratocreations.phovo.di

import com.serratocreations.phovo.AndroidDesktopIosWasmAppInitializer
import com.serratocreations.phovo.core.common.di.getCoreCommonModule
import com.serratocreations.phovo.core.logger.KermitKoinLogger
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.logger.getLoggerCommonModule
import com.serratocreations.phovo.core.navigation.di.navigationModule
import com.serratocreations.phovo.feature.connections.di.getConnectionsFeatureModule
import com.serratocreations.phovo.feature.photos.di.getPhotosFeatureModule
import com.serratocreations.phovo.ui.viewmodel.ApplicationViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

fun initApplication(config: KoinAppDeclaration? = null) = startKoin {
    config?.invoke(this)
    allowOverride(false)
    logger(KermitKoinLogger(PhovoLogger.withTag("koin")))
    modules(
        getApplicationPlatformModulesFetcher().getModule()
    )

    val appInitializer = KoinPlatformTools.defaultContext().get().get<AndroidDesktopIosWasmAppInitializer>()
    appInitializer.initialize()
}

// called by IOS in iOSApp.swift
fun initApplication() = initApplication {}

expect fun getApplicationPlatformModulesFetcher(): ApplicationPlatformModuleFetcher

// Use kotlin language constructs to get all of the platform modules
abstract class ApplicationPlatformModuleFetcher {
    open fun getModule(): Module = module {
        // common dependency definitions
        includes(
            getCoreCommonModule(),
            getPhotosFeatureModule(),
            getLoggerCommonModule(),
            getConnectionsFeatureModule(),
            flavorModule,
            navigationModule
        )
        viewModelOf(::ApplicationViewModel)
    }
}