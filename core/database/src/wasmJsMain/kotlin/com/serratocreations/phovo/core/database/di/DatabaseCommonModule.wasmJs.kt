package com.serratocreations.phovo.core.database.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DatabasePlatformModule::class])
@ComponentScan("com.serratocreations.phovo.core.database")
actual class DatabaseCommonModule actual constructor()

@Module
@ComponentScan("com.serratocreations.phovo.core.database")
actual class DatabasePlatformModule actual constructor()