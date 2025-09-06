package com.serratocreations.phovo.di

import com.serratocreations.phovo.core.common.di.getCoreCommonModule
import com.serratocreations.phovo.core.logger.KermitKoinLogger
import com.serratocreations.phovo.core.logger.PhovoLogger
import com.serratocreations.phovo.core.logger.getLoggerCommonModule
import com.serratocreations.phovo.feature.connections.di.getConnectionsFeatureModule
import com.serratocreations.phovo.feature.photos.di.getPhotosFeatureModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) = startKoin {
    config?.invoke(this)
    logger(KermitKoinLogger(PhovoLogger.withTag("koin")))
    modules(
        getCoreCommonModule(),
        getPhotosFeatureModule(),
        getLoggerCommonModule(),
        getConnectionsFeatureModule()
    )
}

// called by IOS in iOSApp.swift
fun initKoin() = initKoin {}