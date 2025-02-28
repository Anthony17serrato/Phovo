package com.serratocreations.phovo.di

import com.serratocreations.phovo.core.common.di.commonModule
import com.serratocreations.phovo.feature.connections.di.connectionsFeatureModule
import com.serratocreations.phovo.feature.photos.di.photosFeatureModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.*

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(AppModule().module, photosFeatureModule(), connectionsFeatureModule(), commonModule())
}

// called by IOS in iOSApp.swift
fun initKoin() = initKoin {}

@Module
@ComponentScan("com.serratocreations.phovo")
class AppModule