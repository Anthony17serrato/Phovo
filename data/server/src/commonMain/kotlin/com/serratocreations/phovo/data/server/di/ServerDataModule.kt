package com.serratocreations.phovo.data.server.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [ServerDataPlatformModule::class])
@ComponentScan("com.serratocreations.phovo.data.server")
class ServerDataModule

@Module
@ComponentScan("com.serratocreations.phovo.data.server")
internal expect class ServerDataPlatformModule()