package com.serratocreations.phovo.data.server.di

import com.serratocreations.phovo.core.database.di.DatabaseCommonModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [ServerDataPlatformModule::class, DatabaseCommonModule::class])
@ComponentScan("com.serratocreations.phovo.data.server")
class ServerDataModule

@Module
@ComponentScan("com.serratocreations.phovo.data.server")
internal expect class ServerDataPlatformModule()