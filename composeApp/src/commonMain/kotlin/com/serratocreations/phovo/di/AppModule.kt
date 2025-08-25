package com.serratocreations.phovo.di

import com.serratocreations.phovo.core.common.di.CoreCommonModule
import com.serratocreations.phovo.core.common.di.getCoreCommonModule
import com.serratocreations.phovo.core.logger.KermitKoinLogger
import com.serratocreations.phovo.core.logger.LoggerCommonModule
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.feature.connections.di.ConnectionsFeatureModule
import com.serratocreations.phovo.feature.photos.di.PhotosFeatureModule
import com.serratocreations.phovo.feature.photos.di.getPhotosFeatureModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.*

fun initKoin(config: KoinAppDeclaration? = null) = startKoin {
    config?.invoke(this)
    logger(KermitKoinLogger(PhovoLogger.withTag("koin")))
    modules(
        AppModule().module,
        getCoreCommonModule(),
        getPhotosFeatureModule()
    )
}

// called by IOS in iOSApp.swift
fun initKoin() = initKoin {}
// TODO: Migrate from annotations back to plain Koin
@Module(includes = [
    PhotosFeatureModule::class,
    CoreCommonModule::class,
    ConnectionsFeatureModule::class,
    LoggerCommonModule::class
])
@ComponentScan("com.serratocreations.phovo")
class AppModule