package com.serratocreations.phovo.feature.connections.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun connectionsFeatureModule() = module {
    includes(ConnectionsFeatureModule().module, platformModule())
}

@Module
@ComponentScan("com.serratocreations.phovo.feature.connections")
class ConnectionsFeatureModule