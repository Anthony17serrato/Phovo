package com.serratocreations.phovo.di

import com.serratocreations.phovo.AndroidDesktopIosAppInitializer
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

fun initApplication(config: KoinAppDeclaration? = null, platformModule: Module = getApplicationPlatformModulesFetcher().getModule()) = startKoin {
    config?.invoke(this)

    logger(KermitKoinLogger(PhovoLogger.withTag("koin")))
    modules(
        platformModule
    )

    val appInitializer = KoinPlatformTools.defaultContext().get().get<AndroidDesktopIosAppInitializer>()
    appInitializer.initialize()
}

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