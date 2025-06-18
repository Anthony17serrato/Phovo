package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.data.server.di.ServerDataModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [ServerDataModule::class, ConnectionsFeaturePlatformModule::class])
@ComponentScan("com.serratocreations.phovo.feature.connections")
class ConnectionsFeatureModule

@Module
internal expect class ConnectionsFeaturePlatformModule()