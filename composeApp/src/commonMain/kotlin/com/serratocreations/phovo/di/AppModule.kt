package com.serratocreations.phovo.di

import com.serratocreations.phovo.core.common.di.CoreCommonModule
import com.serratocreations.phovo.feature.connections.di.ConnectionsFeatureModule
import com.serratocreations.phovo.feature.photos.di.PhotosFeatureModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.*

fun initKoin(config: KoinAppDeclaration? = null) = startKoin {
    modules(AppModule().module)
    config?.invoke(this)
}

// called by IOS in iOSApp.swift
fun initKoin() = initKoin {}

@Module(includes = [PhotosFeatureModule::class, CoreCommonModule::class, ConnectionsFeatureModule::class])
@ComponentScan("com.serratocreations.phovo")
class AppModule