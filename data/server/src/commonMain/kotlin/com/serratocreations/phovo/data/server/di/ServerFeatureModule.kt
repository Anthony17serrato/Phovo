package com.serratocreations.phovo.data.server.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.*

fun serverFeatureModule() = module {
    includes(
        ServerFeatureModule().module,
        platformModule()
    )
}

@Module
@ComponentScan("com.serratocreations.phovo.data.server")
class ServerFeatureModule