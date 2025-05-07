package com.serratocreations.phovo.core.database.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [DatabasePlatformModule::class])
@ComponentScan("com.serratocreations.phovo.core.database")
expect class DatabaseCommonModule()

@Module
@ComponentScan("com.serratocreations.phovo.core.database")
expect class DatabasePlatformModule()