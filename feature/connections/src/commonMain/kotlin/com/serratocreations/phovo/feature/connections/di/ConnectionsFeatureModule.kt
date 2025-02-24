package com.serratocreations.phovo.feature.connections.di

import com.serratocreations.phovo.data.server.di.serverFeatureModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun connectionsFeatureModule() = module {
    includes(ConnectionsFeatureModule().module, platformModule(), serverFeatureModule())
}

@Module
@ComponentScan("com.serratocreations.phovo.feature.connections")
class ConnectionsFeatureModule